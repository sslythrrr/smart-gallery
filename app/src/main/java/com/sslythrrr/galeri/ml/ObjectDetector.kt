package com.sslythrrr.galeri.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import com.sslythrrr.galeri.data.entity.DetectedObject
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ObjectDetector(private val context: Context) {
    private val tag = "ObjectDetector"
    private val modelFilename = "14juli25.tflite"
    private val vocabFilename = "14juli25.json"
    private val inputSize = 224
    private val confidenceThreshold = 0.8f

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
                numThreads = 4 }
            tflite = Interpreter(modelBuffer, options)
            loadVocabulary(vocabFilename)
            isInitialized = true
            Log.d(tag, "✅✅✅ Object Detector (UNTUK MODEL BARU) Berhasil Diinisialisasi ✅✅✅")
        } catch (e: Exception) {
            Log.e(tag, "❌ Gagal total saat inisialisasi", e)
        }
    }

    fun detectObjects(path: String, uri: String): List<DetectedObject> {
        synchronized(lock) {
            if (!isInitialized) return emptyList()
            try {
                val bitmap = loadAndResizeImage(path)
                if (bitmap == null) {
                    Log.e(tag, "❌ GAGAL MEMUAT BITMAP DARI PATH: $path.")
                    return emptyList()
                }

                // INI BAGIAN YANG DIPERBAIKI SESUAI ERROR LOG
                val inputBuffer = convertBitmapToByteBuffer(bitmap)
                inputBuffer.rewind()

                // Model baru mengeluarkan output UINT8 juga
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
                    key.toInt() to idxToTagJsonObject.getString(key)
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

    // --- PERBAIKAN FINAL BERDASARKAN CRASH LOG ---
    // Mengubah buffer menjadi UINT8 (1 byte per channel)
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
            // Langsung memasukkan nilai R, G, B sebagai byte [0, 255]
            byteBuffer.put(((pixelValue shr 16) and 0xFF).toByte())
            byteBuffer.put(((pixelValue shr 8) and 0xFF).toByte())
            byteBuffer.put((pixelValue and 0xFF).toByte())
        }
        return byteBuffer
    }

    private fun parseQuantizedResults(output: ByteArray, uri: String): List<DetectedObject> {
        val results = mutableListOf<DetectedObject>()
        // Kita gunakan 'forEachIndexed' yang pasti ada di ByteArray
        output.forEachIndexed { index, byteValue ->
            // Ubah byte [-128, 127] menjadi probabilitas [0, 1]
            val probability = (byteValue.toInt() and 0xFF) / 255.0f
            if (probability >= confidenceThreshold) {
                results.add(
                    DetectedObject(
                        uri = uri,
                        label = idxToTagMap[index] ?: "Unknown",
                        confidence = probability
                    )
                )
            }
        }
        // Lakukan pengurutan setelah semua hasil terkumpul
        return results.sortedByDescending { it.confidence }
    }

    private fun loadAndResizeImage(path: String): Bitmap? = try {
        BitmapFactory.decodeFile(path)?.scale(inputSize, inputSize, true)
    } catch (e: Exception) { null }
}