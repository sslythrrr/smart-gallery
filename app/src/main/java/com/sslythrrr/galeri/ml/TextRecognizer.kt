package com.sslythrrr.galeri.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sslythrrr.galeri.data.entity.DetectedText
import kotlinx.coroutines.tasks.await

class TextRecognizerHelper {

    companion object {
        private const val TAG = "Deteksi Teks"
        private const val CONFIDENCE_THRESHOLD = 0.75f
        private const val MIN_TEXT_LENGTH = 3
        private const val MAX_CACHE_SIZE = 100
        private const val MAX_RESULTS_PER_IMAGE = 10
    }

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val processedCache = mutableMapOf<String, List<DetectedText>>()

    suspend fun detectTexts(path: String, uri: String): List<DetectedText> {
        processedCache[path]?.let {
            Log.d(TAG, "Cache hit untuk path: $path")
            return it
        }

        return try {
            Log.d(TAG, "Memulai text recognition pada: $path")

            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Penjelasan: Naikkan inSampleSize untuk mengurangi konsumsi memori, seringkali tidak mengurangi akurasi secara signifikan.
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeFile(path, options) ?: run {
                Log.e(TAG, "❌ Gagal membuka bitmap dari path: $path")
                return emptyList()
            }

            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()
            bitmap.recycle() // Penjelasan: Recycle bitmap sesegera mungkin setelah tidak digunakan.

            if (result.text.isBlank()) {
                Log.d(TAG, "⚠️ Tidak ada teks terdeteksi pada gambar: $path")
                return emptyList()
            }

            val detectedTexts = parseAndFilterResults(result, uri)
            Log.d(TAG, "✅ Berhasil mendeteksi dan memfilter ${detectedTexts.size} teks dari gambar: $path")

            // Manajemen Cache
            if (processedCache.size >= MAX_CACHE_SIZE) {
                processedCache.keys.firstOrNull()?.let { processedCache.remove(it) } // Hapus yang paling lama
            }
            processedCache[path] = detectedTexts

            detectedTexts
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saat mendeteksi teks", e)
            emptyList()
        }
    }
    private fun parseAndFilterResults(result: Text, uri: String): List<DetectedText> {
        val candidates = mutableListOf<DetectedText>()

        result.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                if (line.confidence >= CONFIDENCE_THRESHOLD && isMeaningfulText(line.text)) {
                    candidates.add(
                        DetectedText(
                            id = 0,
                            uri = uri,
                            text = line.text.trim(),
                            confidence = line.confidence
                        )
                    )
                }
            }
        }

        return candidates
            .asSequence()
            .map { it.copy(text = it.text.lowercase()) }
            .distinctBy { it.text }
            .sortedByDescending { it.confidence }
            .take(MAX_RESULTS_PER_IMAGE)
            .toList()
    }
    private fun isMeaningfulText(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.length >= MIN_TEXT_LENGTH &&
                !trimmed.all { it.isDigit() } &&
                trimmed.all { it.isLetterOrDigit() || it.isWhitespace() }
    }

    fun release() {
        textRecognizer.close()
        Log.d(TAG, "Text recognizer berhasil dilepas.")
    }
}