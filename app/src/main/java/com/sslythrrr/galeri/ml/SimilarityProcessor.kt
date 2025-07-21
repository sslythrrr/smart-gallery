package com.sslythrrr.galeri.ml

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

typealias SimilarityResult = List<String>

class SimilarityProcessor(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var labelEmbeddings: Map<String, List<Float>>? = null
//abc
    private val modelFilename = "similarity.tflite"
    private val embeddingsFilename = "label.json"
    private val maxLength = 128
    private var vocabMap: Map<String, Int>? = null
    private val vocabFilename = "vocab.txt"
    // INI ADALAH FUNGSI UNTUK MEMUAT MODEL YANG PALING AMAN
    private fun loadModelFile(filename: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(filename)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        fileInputStream.close()
        assetFileDescriptor.close()
        return mappedByteBuffer
    }

    fun initialize(): Boolean {
        return try {
            println("🚨 JURUS PAMUNGKAS: Inisialisasi Similarity Processor...")
            val modelByteBuffer = loadModelFile(modelFilename)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
                // MATIKAN SEMUA DELEGATE untuk menghindari crash
                setUseXNNPACK(false)
                setUseNNAPI(false)
            }
            interpreter = Interpreter(modelByteBuffer, options)
            println("✅ Interpreter dibuat. Info model:")
            println("Input count: ${interpreter!!.inputTensorCount}")
            println("Output count: ${interpreter!!.outputTensorCount}")

// Print input details
            for (i in 0 until interpreter!!.inputTensorCount) {
                val tensor = interpreter!!.getInputTensor(i)
                println("Input $i: name=${tensor.name()}, shape=${tensor.shape().contentToString()}, dtype=${tensor.dataType()}")
            }

// Print output details
            for (i in 0 until interpreter!!.outputTensorCount) {
                val tensor = interpreter!!.getOutputTensor(i)
                println("Output $i: name=${tensor.name()}, shape=${tensor.shape().contentToString()}, dtype=${tensor.dataType()}")
            }
            println("✅ Interpreter dibuat. Memulai alokasi manual...")

            try {
                interpreter!!.allocateTensors()
                println("✅ Tensor berhasil dialokasikan!")
            } catch (e: IllegalStateException) {
                println("❌ GAGAL allocateTensors - IllegalState: ${e.message}")
                throw e
            } catch (e: RuntimeException) {
                println("❌ GAGAL allocateTensors - Runtime: ${e.message}")
                throw e
            } catch (e: Exception) {
                println("❌ GAGAL allocateTensors - Generic: ${e.message}")
                throw e
            }

            println("✅ Tensor berhasil dialokasikan!")

            labelEmbeddings = loadLabelEmbeddings(embeddingsFilename)
            vocabMap = loadVocab()
            println("✅ Similarity Processor (Jurus Pamungkas) SIAP!")
            true
        } catch (e: Exception) {
            println("❌ GAGAL TOTAL di Jurus Pamungkas: ${e.message}")
            e.printStackTrace()
            // Pastikan interpreter ditutup jika inisialisasi gagal
            interpreter?.close()
            interpreter = null
            false
        }
    }

    // GANTI seluruh fungsi loadLabelEmbeddings:
    private fun loadLabelEmbeddings(filename: String): Map<String, List<Float>> {
        val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }

        // Parse sebagai raw object dulu
        val rawType = object : TypeToken<Map<String, Any>>() {}.type
        val rawMap: Map<String, Any> = Gson().fromJson(jsonString, rawType)

        val result = mutableMapOf<String, List<Float>>()

        for ((label, value) in rawMap) {
            try {
                val floatList = when (value) {
                    is List<*> -> {
                        if (value.isNotEmpty() && value.first() is List<*>) {
                            // Nested list - ambil yang pertama
                            @Suppress("UNCHECKED_CAST")
                            val innerList = value.first() as List<*>
                            innerList.mapNotNull { (it as? Number)?.toFloat() }
                        } else {
                            // Flat list
                            value.mapNotNull { (it as? Number)?.toFloat() }
                        }
                    }
                    else -> {
                        println("⚠️ Unexpected value type for '$label': ${value?.javaClass}")
                        emptyList()
                    }
                }

                result[label] = floatList
                println("✅ Loaded '$label': ${floatList.size} dimensions")

            } catch (e: Exception) {
                println("❌ Failed to load '$label': ${e.message}")
                result[label] = emptyList()
            }
        }

        return result
    }
    private fun loadVocab(): Map<String, Int> {
        return try {
            val vocab = mutableMapOf<String, Int>()
            context.assets.open(vocabFilename).bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, token ->
                    vocab[token.trim()] = index
                }
            }
            println("✅ Vocab loaded: ${vocab.size} tokens")
            vocab
        } catch (e: Exception) {
            println("❌ Vocab loading failed: ${e.message}")
            emptyMap()
        }
    }
    fun calculateSimilarity(query: String, top: Int = 3, threshold: Float = 0.85f): SimilarityResult {
        if (interpreter == null || labelEmbeddings == null) {
            println("⚠️ Processor belum siap.")
            return emptyList()
        }

        try {
            val queryEmbedding = getEmbedding(query)
            if (queryEmbedding.isEmpty()) return emptyList()

            val similarities = labelEmbeddings!!.mapValues { (_, labelEmbedding) ->
                cosineSimilarity(queryEmbedding, labelEmbedding.toFloatArray())
            }

            return similarities.entries
                .sortedByDescending { it.value }
                .filter { it.value >= threshold }
                .take(top)
                .map { it.key }

        } catch (e: Exception) {
            println("❌ Error saat kalkulasi similarity: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun getEmbedding(text: String): FloatArray {
        // Tokenisasi sederhana (sementara)
        val tokens = text.lowercase().split(" ")
        val inputIds = IntArray(maxLength) { vocabMap?.get("[PAD]") ?: 2 }
        val attentionMask = IntArray(maxLength) { 0 }

        // [CLS] token
        inputIds[0] = vocabMap?.get("[CLS]") ?: 3
        attentionMask[0] = 1

        var pos = 1
        for (word in tokens) {
            if (pos >= maxLength - 1) break
            inputIds[pos] = vocabMap?.get(word) ?: vocabMap?.get("[UNK]") ?: 0
            attentionMask[pos] = 1
            pos++
        }

        // [SEP] token
        if (pos < maxLength) {
            inputIds[pos] = vocabMap?.get("[SEP]") ?: 1
            attentionMask[pos] = 1
        }

        // Buat ByteBuffer yang benar
        val inputBuffer1 = ByteBuffer.allocateDirect(maxLength * 4).order(ByteOrder.nativeOrder())
        val inputBuffer2 = ByteBuffer.allocateDirect(maxLength * 4).order(ByteOrder.nativeOrder())

        for (i in 0 until maxLength) {
            inputBuffer1.putInt(inputIds[i])
            inputBuffer2.putInt(attentionMask[i])
        }

        inputBuffer1.rewind()
        inputBuffer2.rewind()

        // Output array
        val outputShape = intArrayOf(1, maxLength, 768)
        val modelOutput = Array(1) { Array(maxLength) { FloatArray(768) } }
        val outputs = mapOf(0 to modelOutput)

        try {
            interpreter!!.runForMultipleInputsOutputs(arrayOf(inputBuffer1, inputBuffer2), outputs)

            // Mean pooling - ambil rata-rata dari token yang aktif
            val embedding = FloatArray(768)
            var tokenCount = 0

            for (i in 0 until maxLength) {
                if (attentionMask[i] == 1) {
                    for (j in 0 until 768) {
                        embedding[j] += modelOutput[0][i][j]
                    }
                    tokenCount++
                }
            }

            // Rata-ratakan
            if (tokenCount > 0) {
                for (i in 0 until 768) {
                    embedding[i] /= tokenCount
                }
            }

            return embedding

        } catch (e: Exception) {
            println("❌ Error inference: ${e.message}")
            return floatArrayOf()
        }
    }

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.isEmpty() || vec2.isEmpty() || vec1.size != vec2.size) return 0.0f
        val dotProduct = vec1.zip(vec2).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()
        val normA = sqrt(vec1.sumOf { (it * it).toDouble() }).toFloat()
        val normB = sqrt(vec2.sumOf { (it * it).toDouble() }).toFloat()
        return if (normA > 0 && normB > 0) dotProduct / (normA * normB) else 0.0f
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}