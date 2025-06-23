package com.sslythrrr.galeri.ml
//Experimental
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import com.sslythrrr.galeri.data.entity.DetectedObject
import com.sslythrrr.galeri.data.utils.Labels
import org.tensorflow.lite.Interpreter

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ObjectDetector(private val context: Context) {
    private val tag = "ObjectDetector"
    private val modelFilename = "yolo11_cls.tflite"
    private val inputSize = 224
    private val confidenceThreshold = 0.5f

    private lateinit var tflite: Interpreter
    private lateinit var outputShapes: Array<IntArray>
    private var isInitialized = false
    private var reusableByteBuffer: ByteBuffer? = null
    private var reusableOutputClasses = Array(1) { FloatArray(1000) }

    private val lock = Any()

    fun initialize() {
        if (isInitialized) return

        try {
            setupTFLiteInterpreter()
            isInitialized = true
            Log.d(tag, "Object detector berhasil diinisialisasi")
        } catch (e: Exception) {
            Log.e(tag, "❌ Gagal menginisialisasi object detector", e)
            throw e
        }
    }

    fun detectObjects(path: String, uri: String): List<DetectedObject> {
        synchronized(lock) {
            if (!isInitialized) {
                Log.e(tag, "Object detector belum diinisialisasi!")
                return emptyList()
            }

            try {
                val bitmap = loadAndResizeImage(path)
                if (bitmap == null) {
                    Log.e(tag, "❌ Gagal membuka gambar: $path")
                    return emptyList()
                }

                val inputBuffer = convertBitmapToByteBuffer(bitmap)
                val outputClasses = reusableOutputClasses

                Log.d(tag, "Running inference pada gambar: $path")
                tflite.run(inputBuffer, outputClasses)

                return parseClassificationResults(
                    classes = outputClasses[0],
                    uri = uri
                )

            } catch (e: Exception) {
                Log.e(tag, "❌ Error detecting objects", e)
                e.printStackTrace()
                return emptyList()
            }
        }
    }

    private fun setupTFLiteInterpreter() {
        val modelFile = File(context.filesDir, modelFilename)
        if (!modelFile.exists()) {
            throw IllegalStateException("Model TFLite tidak ditemukan di: ${modelFile.absolutePath}")
        }

        val options = Interpreter.Options()
        options.setNumThreads(2)


        tflite = Interpreter(loadModelFile(modelFile), options)
        outputShapes = Array(tflite.outputTensorCount) { i ->
            tflite.getOutputTensor(i).shape()
        }

        Log.d(tag, "Model dimuat. Jumlah outputTensor: ${tflite.outputTensorCount}")
        val inputTensor = tflite.getInputTensor(0)
        val outputTensor = tflite.getOutputTensor(0)
        Log.d(tag, "Input shape: ${inputTensor.shape().contentToString()}")
        Log.d(tag, "Output shape: ${outputTensor.shape().contentToString()}")
        Log.d(tag, "Input dataType: ${inputTensor.dataType()}")
        Log.d(tag, "Expected input bytes: ${inputTensor.numBytes()}")
        Log.d(tag, "Our buffer size: ${1 * inputSize * inputSize * 3 * 4}")
    }


    private fun loadModelFile(modelFile: File): MappedByteBuffer {
        val fileDescriptor = FileInputStream(modelFile).fd
        val fileChannel = FileInputStream(fileDescriptor).channel
        val startOffset = 0L
        val declaredLength = fileChannel.size()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        if (reusableByteBuffer == null) {
            reusableByteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
            reusableByteBuffer!!.order(ByteOrder.nativeOrder())
        }
        val byteBuffer = reusableByteBuffer!!
        byteBuffer.clear()

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    private fun parseClassificationResults(
        classes: FloatArray,
        uri: String
    ): List<DetectedObject> {
        val results = mutableListOf<DetectedObject>()

        // Ambil top 5 predictions
        val classWithScores = classes.mapIndexed { index, score ->
            Pair(index, score)
        }.sortedByDescending { it.second }.take(5)

        for ((classId, score) in classWithScores) {
            if (score >= confidenceThreshold) {
                val label = Labels.getLabel(classId)

                Log.d(tag, "Klasifikasi: class=$classId, label=$label, score=$score")

                if (label != "Tidak Diketahui") {
                    results.add(
                        DetectedObject(
                            id = 0,
                            uri = uri,
                            label = label,
                            confidence = score
                        )
                    )
                }
            }
        }
        return results
    }

    private fun loadAndResizeImage(path: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, inputSize, inputSize)
            options.inJustDecodeBounds = false
            options.inMutable = true // untuk reuse di GPU delegate, dll

            val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
            bitmap.scale(inputSize, inputSize, false)
        } catch (e: Exception) {
            Log.e(tag, "❌ Gagal decode gambar: $path", e)
            null
            null
        }
    }


    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}