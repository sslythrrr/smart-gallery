package com.sslythrrr.galeri.ml
//Experimental
import android.content.Context
import com.google.gson.Gson
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
    private var interpreter: Interpreter? = null
    private var metadata: NerMetadata? = null
    private var vocab: Map<String, Int>? = null

    fun initialize(): Boolean {
        return try {
            println("🔧 Initializing ONNX NER Processor...")

            val modelBytes = context.assets.open("distilbert_ner.tflite").readBytes()
            val modelByteBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            modelByteBuffer.put(modelBytes)
            interpreter = Interpreter(modelByteBuffer)

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

    // NEW: Tambahkan di fungsi processQuery untuk debugging
    fun processQuery(query: String): NerResult {
        println("🔍 Processing query: '$query'")
        val tokens = tokenize(query)
        println("🔤 Tokens: $tokens")

        val inputIds = convertTokensToIds(tokens)
        println("🔢 Input IDs: ${inputIds.take(10)}...")

        val attentionMask = createAttentionMask(inputIds)
        val predictions = runInference(inputIds, attentionMask)
        println("🎯 Raw predictions: ${predictions.take(10)}...")

        val labels = convertPredictionsToLabels(predictions)
        println("🏷️ Labels: $labels")

        val entities = extractEntities(tokens, labels)
        println("📝 Entities: $entities")

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
        val interpreter = this.interpreter ?: return IntArray(0)

        try {
            // Prepare input buffers
            val batchSize = 1
            val sequenceLength = inputIds.size

            val inputIdsBuffer = ByteBuffer.allocateDirect(4 * batchSize * sequenceLength)
            inputIdsBuffer.order(ByteOrder.nativeOrder())

            val attentionMaskBuffer = ByteBuffer.allocateDirect(4 * batchSize * sequenceLength)
            attentionMaskBuffer.order(ByteOrder.nativeOrder())

            inputIdsBuffer.rewind()
            attentionMaskBuffer.rewind()

            // Fill input buffers
            for (i in inputIds.indices) {
                inputIdsBuffer.putInt(inputIds[i].toInt())
                attentionMaskBuffer.putInt(attentionMask[i].toInt())
            }

            // Prepare output buffer
            val numLabels = metadata?.label_list?.size ?: 9
            val outputBuffer = ByteBuffer.allocateDirect(4 * batchSize * sequenceLength * numLabels)
            outputBuffer.order(ByteOrder.nativeOrder())

            // Run inference
            val inputMap = HashMap<Int, Any>()
            interpreter.getInputTensor(0)?.let {
                val name = it.name()
                if (name == "input_ids") {
                    inputMap[0] = inputIdsBuffer
                    inputMap[1] = attentionMaskBuffer
                } else {
                    inputMap[0] = attentionMaskBuffer
                    inputMap[1] = inputIdsBuffer
                }
            }


            val outputs = mapOf(0 to outputBuffer)

            interpreter.runForMultipleInputsOutputs(arrayOf(inputMap[0]!!, inputMap[1]!!), outputs)


            // Process output
            outputBuffer.rewind()
            val predictions = IntArray(sequenceLength)
            val logits = Array(sequenceLength) { FloatArray(numLabels) }

// Ambil semua nilai dulu dari buffer
            for (i in 0 until sequenceLength) {
                for (j in 0 until numLabels) {
                    logits[i][j] = outputBuffer.getFloat()
                }
            }

// Cari index dengan nilai tertinggi per token
            for (i in 0 until sequenceLength) {
                val tokenLogits = logits[i]
                predictions[i] = tokenLogits.indices.maxByOrNull { tokenLogits[it] } ?: 0
            }


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

        // Kita asumsikan posisi token diisi mulai dari label[1]
        for ((tokenIndex, token) in tokens.withIndex()) {
            val labelIndex = tokenIndex + 1  // geser karena token ke-0 = token pertama, label ke-0 = [CLS]
            if (labelIndex >= labels.size) continue

            val label = labels[labelIndex]
            if (label.startsWith("B-")) {
                val type = label.removePrefix("B-")
                entities.getOrPut(type) { mutableListOf() }.add(token)
            }
        }

        return entities
    }


    fun close() {
        interpreter?.close()
    }
}