package com.sslythrrr.galeri.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import com.sslythrrr.galeri.data.entity.DetectedObject
import com.sslythrrr.galeri.data.utils.Labels
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ObjectDetector(private val context: Context) {
    private val tag = "ObjectDetector"
    private val modelFilename = "ssdmobilenetv2.tflite"
    private val inputSize = 320
    private val confidenceThreshold = 0.5f

    private lateinit var tflite: Interpreter
    private lateinit var outputShapes: Array<IntArray>
    private var isInitialized = false
    private var reusableByteBuffer: ByteBuffer? = null
    private var reusableOutputBoxes = Array(1) { Array(12804) { FloatArray(4) } }
    private var reusableOutputClasses = Array(1) { Array(12804) { FloatArray(91) } }

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

    fun detectObjects(imagePath: String): List<DetectedObject> {
        synchronized(lock) {
            if (!isInitialized) {
                Log.e(tag, "Object detector belum diinisialisasi!")
                return emptyList()
            }

            try {
                val bitmap = loadAndResizeImage(imagePath)
                if (bitmap == null) {
                    Log.e(tag, "❌ Gagal membuka gambar: $imagePath")
                    return emptyList()
                }

                val inputBuffer = convertBitmapToByteBuffer(bitmap)
                val outputBoxes = reusableOutputBoxes
                val outputClasses = reusableOutputClasses

                val outputMap = mapOf(
                    6 to outputBoxes,
                    7 to outputClasses
                )

                Log.d(tag, "Running inference pada gambar: $imagePath")
                tflite.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

                return parseDetectionResults(
                    boxes = outputBoxes[0],
                    classes = outputClasses[0],
                    imagePath = imagePath
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
        val compatList = CompatibilityList()

        if (compatList.isDelegateSupportedOnThisDevice) {
            val gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate)
        } else {
            options.setNumThreads(2)
        }

        tflite = Interpreter(loadModelFile(modelFile), options)
        outputShapes = Array(tflite.outputTensorCount) { i ->
            tflite.getOutputTensor(i).shape()
        }

        Log.d(tag, "Model dimuat. Jumlah outputTensor: ${tflite.outputTensorCount}")
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
            reusableByteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3)
            reusableByteBuffer!!.order(ByteOrder.nativeOrder())
        }
        val byteBuffer = reusableByteBuffer!!
        byteBuffer.clear()

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            byteBuffer.put(((pixel shr 16) and 0xFF).toByte())
            byteBuffer.put(((pixel shr 8) and 0xFF).toByte())
            byteBuffer.put((pixel and 0xFF).toByte())
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    private fun parseDetectionResults(
        boxes: Array<FloatArray>,
        classes: Array<FloatArray>,
        imagePath: String
    ): List<DetectedObject> {
        val results = mutableListOf<DetectedObject>()

        for (i in boxes.indices) {
            var maxScore = 0f
            var maxClassId = 0

            for (classId in 1 until classes[i].size) {
                val score = classes[i][classId]
                if (score > maxScore) {
                    maxScore = score
                    maxClassId = classId
                }
            }

            if (maxScore >= confidenceThreshold) {
                val label = Labels.getLabel(maxClassId - 1)

                Log.d(
                    tag,
                    "Deteksi terproses: class=${maxClassId - 1}, label=$label, score=$maxScore"
                )

                if (label != "Unknown") {
                    results.add(
                        DetectedObject(
                            imagePath = imagePath,
                            label = label,
                            confidence = maxScore
                        )
                    )
                }
            }
        }
        return results.sortedByDescending { it.confidence }.take(5)
    }

    private fun loadAndResizeImage(imagePath: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            options.inSampleSize = calculateInSampleSize(options, inputSize, inputSize)
            options.inJustDecodeBounds = false
            options.inMutable = true // untuk reuse di GPU delegate, dll

            val bitmap = BitmapFactory.decodeFile(imagePath, options) ?: return null
            bitmap.scale(inputSize, inputSize, false)
        } catch (e: Exception) {
            Log.e(tag, "❌ Gagal decode gambar: $imagePath", e)
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