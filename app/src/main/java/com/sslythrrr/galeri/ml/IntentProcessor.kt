package com.sslythrrr.galeri.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.google.gson.Gson
import java.nio.LongBuffer
import kotlin.math.exp

data class IntentMetadata(
    val labellist: List<String>,
    val label2id: Map<String, Int>,
    val id2label: Map<String, String>,
    val maxlength: Int,
    val modeltype: String,
    val vocabsize: Int
)

data class IntentResult(
    val intent: String,
    val confidence: Double
)

class IntentOnnxProcessor(private val context: Context) {
    private var ortSession: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var metadata: IntentMetadata? = null
    private var vocab: Map<String, Int>? = null

    fun initialize(): Boolean {
        return try {
            println("🔧 Initializing ONNX Intent Processor...")

            // Initialize ONNX Runtime
            ortEnvironment = OrtEnvironment.getEnvironment()

            // Load model
            val modelBytes = context.assets.open("distilbert_intent_smartgallery.onnx").readBytes()
            ortSession = ortEnvironment!!.createSession(modelBytes)

            // Load metadata
            metadata = loadMetadata("model_metadata_intent.json")

            // Load vocabulary (shared with NER)
            vocab = loadVocabulary()

            println("✅ ONNX Intent Processor initialized successfully")
            println("📊 Vocab size: ${vocab?.size}")
            println("🎯 Intent labels: ${metadata?.labellist?.size}")

            true
        } catch (e: Exception) {
            println("❌ Failed to initialize ONNX Intent: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun processQuery(query: String): IntentResult {
        val tokens = tokenize(query)
        val inputIds = convertTokensToIds(tokens)
        val attentionMask = createAttentionMask(inputIds)

        val predictions = runInference(inputIds, attentionMask)
        return convertPredictionsToIntent(predictions)
    }

    private fun loadMetadata(filename: String): IntentMetadata {
        val json = context.assets.open(filename).bufferedReader().use { it.readText() }
        return Gson().fromJson(json, IntentMetadata::class.java)
    }

    private fun loadVocabulary(): Map<String, Int> {
        val vocab = mutableMapOf<String, Int>()
        context.assets.open("vocab.txt").bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, line ->
                val token = line.trim()
                if (token.isNotEmpty()) {
                    vocab[token] = index
                }
            }
        }
        return vocab
    }

    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val normalizedText = text.trim().lowercase()
        val words = normalizedText.split(Regex("\\s+"))

        for (word in words) {
            if (word.isEmpty()) continue
            val cleanWord = word.replace(Regex("[.,!?;:()\\[\\]{}\"']"), " ")
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
            tokens.addAll(cleanWord)
        }
        return tokens
    }

    private fun convertTokensToIds(tokens: List<String>): LongArray {
        val maxLen = metadata?.maxlength ?: 128
        val ids = LongArray(maxLen)
        val vocab = this.vocab ?: return ids

        // CLS token
        ids[0] = (vocab["[CLS]"] ?: 101).toLong()
        var currentPos = 1

        for (token in tokens) {
            if (currentPos >= maxLen - 1) break

            var tokenId = vocab[token]
            if (tokenId == null) {
                tokenId = vocab["##$token"]
            }
            if (tokenId == null) {
                val subwords = tokenizeSubwords(token, vocab)
                for (subword in subwords) {
                    if (currentPos >= maxLen - 1) break
                    ids[currentPos] = (vocab[subword] ?: vocab["[UNK]"] ?: 100).toLong()
                    currentPos++
                }
            } else {
                ids[currentPos] = tokenId.toLong()
                currentPos++
            }
        }

        // SEP token
        if (currentPos < maxLen) {
            ids[currentPos] = (vocab["[SEP]"] ?: 102).toLong()
        }

        return ids
    }

    private fun tokenizeSubwords(word: String, vocab: Map<String, Int>): List<String> {
        val subwords = mutableListOf<String>()
        var remainingWord = word

        while (remainingWord.isNotEmpty()) {
            var longestMatch = ""
            var matchFound = false

            for (i in remainingWord.length downTo 1) {
                val candidate = if (subwords.isEmpty()) {
                    remainingWord.substring(0, i)
                } else {
                    "##" + remainingWord.substring(0, i)
                }

                if (vocab.containsKey(candidate)) {
                    longestMatch = candidate
                    matchFound = true
                    break
                }
            }

            if (matchFound) {
                subwords.add(longestMatch)
                val prefixLength = if (longestMatch.startsWith("##")) {
                    longestMatch.length - 2
                } else {
                    longestMatch.length
                }
                remainingWord = remainingWord.substring(prefixLength)
            } else {
                subwords.add("[UNK]")
                remainingWord = remainingWord.substring(1)
            }
        }
        return subwords
    }

    private fun createAttentionMask(inputIds: LongArray): LongArray {
        return LongArray(inputIds.size) { if (inputIds[it] != 0L) 1L else 0L }
    }

    private fun runInference(inputIds: LongArray, attentionMask: LongArray): FloatArray {
        val ortSession = this.ortSession ?: return FloatArray(0)
        val ortEnvironment = this.ortEnvironment ?: return FloatArray(0)

        try {
            // Create input tensors
            val inputShape = longArrayOf(1, inputIds.size.toLong())

            val inputIdsTensor = OnnxTensor.createTensor(
                ortEnvironment,
                LongBuffer.wrap(inputIds),
                inputShape
            )

            val attentionMaskTensor = OnnxTensor.createTensor(
                ortEnvironment,
                LongBuffer.wrap(attentionMask),
                inputShape
            )

            // Run inference
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )

            val result = ortSession.run(inputs)

            // Get logits - untuk intent classification, output shape [1, num_classes]
            val logits = result[0].value as Array<FloatArray>
            val predictions = logits[0] // ambil batch pertama

            // Cleanup
            inputIdsTensor.close()
            attentionMaskTensor.close()
            result.close()

            return predictions

        } catch (e: Exception) {
            println("❌ Intent inference error: ${e.message}")
            e.printStackTrace()
            return FloatArray(metadata?.labellist?.size ?: 0)
        }
    }

    private fun convertPredictionsToIntent(predictions: FloatArray): IntentResult {
        if (predictions.isEmpty()) {
            return IntentResult("unknown", 0.0)
        }

        // Find the index with highest confidence
        var maxIdx = 0
        var maxVal = Float.NEGATIVE_INFINITY

        for (i in predictions.indices) {
            if (predictions[i] > maxVal) {
                maxVal = predictions[i]
                maxIdx = i
            }
        }

        // Apply softmax to get confidence score
        val expSum = predictions.sumOf { exp(it.toDouble()) }
        val confidence = exp(maxVal.toDouble()) / expSum

        val intent = metadata?.id2label?.get(maxIdx.toString()) ?: "unknown"

        return IntentResult(intent, confidence)
    }

    fun close() {
        ortSession?.close()
        ortEnvironment?.close()
    }
}