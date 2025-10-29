package com.sslythrrr.voe.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import com.sslythrrr.voe.data.entity.DeteksiLabel
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class LabelDetector(private val context: Context) {
    private val tag = "LabelDetector"
    private val modelFilename = "mobilenetv3.tflite"
    private val vocabFilename = "vocab.json"
    private val inputSize = 224
    private val confidenceThreshold = 0.01f

    private lateinit var tflite: Interpreter
    private var isInitialized = false
    private val lock = Any()

    private lateinit var idxToTagMap: Map<Int, String>
    private var vocabSize: Int = 0
    private var reusableByteBuffer: ByteBuffer? = null

    fun initialize() {
        if (isInitialized) return
        try {
            val modelBuffer = loadModelFileFromAssets(modelFilename)
            val options = Interpreter.Options().apply {
                setUseXNNPACK(false)
                setNumThreads(4)
            }
            tflite = Interpreter(modelBuffer, options)
            loadVocabulary(vocabFilename)
            isInitialized = true
            Log.d(tag, "✅✅✅ Object Detector (UNTUK MODEL BARU) Berhasil Diinisialisasi ✅✅✅")
        } catch (e: Exception) {
            Log.e(tag, "❌ Gagal total saat inisialisasi", e)
        }
    }

    fun detectObjects(path: String, uri: String): List<DeteksiLabel> {
        synchronized(lock) {
            if (!isInitialized) return emptyList()
            try {
                val bitmap = loadAndResizeImage(path)
                if (bitmap == null) {
                    Log.e(tag, "❌ GAGAL MEMUAT BITMAP DARI PATH: $path.")
                    return emptyList()
                }

                val inputBuffer = convertBitmapToByteBuffer(bitmap)
                inputBuffer.rewind()

                val outputBuffer = Array(1) { ByteArray(vocabSize) }
                tflite.run(inputBuffer, outputBuffer)

                val results = parseQuantizedResults(outputBuffer[0], uri)
                if (results.isNotEmpty()) {
                    Log.i(tag, "🎯 Berhasil! ${results.size} objek terdeteksi di $uri: ${results.joinToString { it.label }}")
                }
                return results
            } catch (e: Exception) {
                Log.e(tag, "❌ Error tidak terduga saat deteksi objek di $path", e)
                return emptyList()
            }
        }
    }

    private fun loadVocabulary(filename: String) {
        try {
            context.assets.open(filename).bufferedReader().use {
                val jsonObject = JSONObject(it.readText())
                val idxToTagJsonObject = jsonObject.getJSONObject("idx_to_tag")
                idxToTagMap = idxToTagJsonObject.keys().asSequence().map { key ->
                    idxToTagJsonObject.getInt(key) to key
                }.toMap()
                vocabSize = idxToTagMap.size
            }
        } catch (e: IOException) {
            throw e
        }
    }

    private fun loadModelFileFromAssets(modelFilename: String): MappedByteBuffer {
        context.assets.openFd(modelFilename).use { assetFileDescriptor ->
            FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength)
            }
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        if (reusableByteBuffer == null) {
            // Ukuran sekarang 1 * 224 * 224 * 3 * 1 (untuk byte) = 150528
            reusableByteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3)
            reusableByteBuffer!!.order(ByteOrder.nativeOrder())
        }
        val byteBuffer = reusableByteBuffer!!
        byteBuffer.clear()

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            byteBuffer.put(((pixelValue shr 16) and 0xFF).toByte())
            byteBuffer.put(((pixelValue shr 8) and 0xFF).toByte())
            byteBuffer.put((pixelValue and 0xFF).toByte())
        }
        return byteBuffer
    }

    private fun parseQuantizedResults(output: ByteArray, uri: String): List<DeteksiLabel> {
        if (output.isEmpty()) return emptyList()

        var maxIndex = -1
        var maxValue = -1
        output.forEachIndexed { index, byteValue ->
            val value = byteValue.toInt() and 0xFF
            if (value > maxValue) {
                maxValue = value
                maxIndex = index
            }
        }

        val probability = maxValue / 255.0f
        if (probability >= confidenceThreshold) {
            val label = idxToTagMap[maxIndex] ?: "Unknown"
            return listOf(
                DeteksiLabel(
                    uri = uri,
                    label = label,
                    confidence = probability
                )
            )
        }
        return emptyList()
    }

    private fun loadAndResizeImage(path: String): Bitmap? = try {
        BitmapFactory.decodeFile(path)?.scale(inputSize, inputSize, true)
    } catch (e: Exception) { null }
}