package com.sslythrrr.galeri.ml
//Experimental
import android.content.Context
import com.google.gson.Gson
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer

data class NerMetadata(
    val label_list: List<String>,
    val label2id: Map<String, Int>,
    val id2label: Map<String, String>,
    val max_length: Int,
    val model_type: String,
    val vocab_size: Int
)

data class NerResult(
    val tokens: List<String>,
    val labels: List<String>,
    val entities: Map<String, List<String>>
)

class NerOnnxProcessor(private val context: Context) {
    private var ortSession: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var metadata: NerMetadata? = null
    private var vocab: Map<String, Int>? = null

    fun initialize(): Boolean {
        return try {
            println("🔧 Initializing ONNX NER Processor...")

            // Initialize ONNX Runtime
            ortEnvironment = OrtEnvironment.getEnvironment()

            // Load model
            val modelBytes = context.assets.open("distilbert_ner_smartgallery.onnx").readBytes()
            ortSession = ortEnvironment!!.createSession(modelBytes)

            // Load metadata
            metadata = loadMetadata("model_metadata_ner.json")

            // Load vocabulary
            vocab = loadVocabulary()

            println("✅ ONNX NER Processor initialized successfully")
            println("📊 Vocab size: ${vocab?.size}")
            println("🏷️ Labels: ${metadata?.label_list?.size}")

            true
        } catch (e: Exception) {
            println("❌ Failed to initialize ONNX NER: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun processQuery(query: String): NerResult {
        val tokens = tokenize(query)
        val inputIds = convertTokensToIds(tokens)
        val attentionMask = createAttentionMask(inputIds)

        val predictions = runInference(inputIds, attentionMask)
        val labels = convertPredictionsToLabels(predictions)
        val entities = extractEntities(tokens, labels)

        return NerResult(tokens, labels, entities)
    }

    private fun loadMetadata(filename: String): NerMetadata {
        val json = context.assets.open(filename).bufferedReader().use { it.readText() }
        return Gson().fromJson(json, NerMetadata::class.java)
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

    private fun runInference(inputIds: LongArray, attentionMask: LongArray): IntArray {
        val ortSession = this.ortSession ?: return IntArray(0)
        val ortEnvironment = this.ortEnvironment ?: return IntArray(0)

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

            // Get logits
            val logits = result[0].value as Array<Array<FloatArray>>
            val predictions = IntArray(inputIds.size)

            // Convert logits to predictions
            for (i in predictions.indices) {
                var maxIdx = 0
                var maxVal = Float.NEGATIVE_INFINITY

                for (j in logits[0][i].indices) {
                    if (logits[0][i][j] > maxVal) {
                        maxVal = logits[0][i][j]
                        maxIdx = j
                    }
                }
                predictions[i] = maxIdx
            }

            // Cleanup
            inputIdsTensor.close()
            attentionMaskTensor.close()
            result.close()

            return predictions

        } catch (e: Exception) {
            println("❌ Inference error: ${e.message}")
            e.printStackTrace()
            return IntArray(inputIds.size)
        }
    }

    private fun convertPredictionsToLabels(predictions: IntArray): List<String> {
        val id2label = metadata?.id2label ?: return emptyList()
        return predictions.map { id2label[it.toString()] ?: "O" }
    }

    private fun extractEntities(tokens: List<String>, labels: List<String>): Map<String, List<String>> {
        val entities = mutableMapOf<String, MutableList<String>>()

        for (i in tokens.indices) {
            if (i + 1 < labels.size) {
                val label = labels[i + 1]
                if (label.startsWith("B-")) {
                    val entityType = label.substring(2)
                    if (!entities.containsKey(entityType)) {
                        entities[entityType] = mutableListOf()
                    }
                    entities[entityType]?.add(tokens[i])
                }
            }
        }
        return entities
    }

    fun close() {
        ortSession?.close()
        ortEnvironment?.close()
    }
}