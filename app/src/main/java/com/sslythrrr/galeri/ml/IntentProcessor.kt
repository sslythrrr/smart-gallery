package com.sslythrrr.galeri.ml
//Experimental
import android.content.Context
import com.google.gson.Gson
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class IntentMetadata(
    val label_list: List<String>,
    val label2id: Map<String, Int>,
    val id2label: Map<String, String>,
    val max_length: Int,
    val model_type: String,
    val vocab_size: Int
)

data class IntentResult(
    val intent: String,
    val confidence: Double
)

class IntentOnnxProcessor(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var metadata: IntentMetadata? = null
    private var vocab: Map<String, Int>? = null

    fun initialize(): Boolean {
        return try {
            println("🔧 Initializing TFLite Intent Processor...")

            val modelBytes = context.assets.open("distilbert_intent.tflite").readBytes()
            val modelByteBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            modelByteBuffer.put(modelBytes)
            interpreter = Interpreter(modelByteBuffer)

            metadata = loadMetadata("model_metadata_intent.json")
            vocab = loadVocabulary()

            println("✅ TFLite Intent Processor initialized successfully")
            true
        } catch (e: Exception) {
            println("❌ Failed to initialize TFLite Intent: ${e.message}")
            false
        }
    }

    fun processQuery(query: String): IntentResult {
        println("🔍 intent query: '$query'")
        val tokens = tokenize(query)
        println("🔤 Tokens intent: $tokens")
        val inputIds = convertTokensToIds(tokens)
        println("🔢 Input IDs: ${inputIds.take(10)}...")
        val attentionMask = createAttentionMask(inputIds)

        val predictions = runInference(inputIds, attentionMask)
        println("🎯 Raw predictions: ${predictions.take(10)}...")
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
        val maxLen = metadata?.max_length ?: 128
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
        val maxLen = metadata?.max_length ?: 128
        val inputIdsBuffer = ByteBuffer.allocateDirect(4 * maxLen).order(ByteOrder.nativeOrder())
        val attentionMaskBuffer = ByteBuffer.allocateDirect(4 * maxLen).order(ByteOrder.nativeOrder())

        for (i in 0 until maxLen) {
            inputIdsBuffer.putInt(inputIds[i].toInt())
            attentionMaskBuffer.putInt(attentionMask[i].toInt())
        }

        inputIdsBuffer.rewind()
        attentionMaskBuffer.rewind()

        val output = Array(1) { FloatArray(metadata?.label_list?.size ?: 0) }

        val inputs = arrayOf<Any?>(null, null)
        for (i in 0 until 2) {
            val name = interpreter!!.getInputTensor(i).name()
            if (name.contains("input_ids")) {
                inputs[i] = inputIdsBuffer
            } else if (name.contains("attention_mask")) {
                inputs[i] = attentionMaskBuffer
            }
        }

        interpreter!!.runForMultipleInputsOutputs(inputs, mapOf(0 to output))

        return output[0]
    }

    private fun convertPredictionsToIntent(predictions: FloatArray): IntentResult {
        if (predictions.isEmpty()) return IntentResult("unknown", 0.0)

        val maxIdx = predictions.indices.maxByOrNull { predictions[it] } ?: 0
        val maxVal = predictions[maxIdx]

        val expSum = predictions.sumOf { kotlin.math.exp(it.toDouble()) }
        val confidence = kotlin.math.exp(maxVal.toDouble()) / expSum

        val label = metadata?.id2label?.get(maxIdx.toString()) ?: "unknown"
        return IntentResult(label, confidence)
    }

    fun close() {
        interpreter?.close()
    }
}