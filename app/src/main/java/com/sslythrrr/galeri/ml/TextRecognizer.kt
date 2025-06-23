package com.sslythrrr.galeri.ml
//Experimental
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

class TextRecognizerHelper() {
    private val tag = "TextRecognizerHelper"
    private val textRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val confidenceThreshold = 0.65f
    private val processedCache = mutableMapOf<String, List<DetectedText>>()
    private val maxCacheSize = 50

    suspend fun detectTexts(path: String, uri: String): List<DetectedText> {
        processedCache[path]?.let { return it }
        return try {
            Log.d(tag, "Memulai text recognition pada: $path")

            val options = BitmapFactory.Options().apply {
                inSampleSize = 16
                inPreferredConfig = Bitmap.Config.RGB_565
                inJustDecodeBounds = false
            }
            val bitmap = BitmapFactory.decodeFile(path, options)
            if (bitmap == null) {
                Log.e(tag, "❌ Gagal membuka gambar: $path")
                return emptyList()
            }

            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()

            if (result.text.isBlank()) {
                Log.d(tag, "⚠️ Tidak ada teks terdeteksi pada gambar: $path")
                return emptyList()
            }

            val detectedTexts = parseTextRecognitionResult(result, uri)
            Log.d(tag, "✅ Berhasil mendeteksi ${detectedTexts.size} teks dari gambar: $path")

            if (processedCache.size >= maxCacheSize) {
                processedCache.clear()
            }
            processedCache[path] = detectedTexts

            bitmap.recycle()
            detectedTexts
        } catch (e: Exception) {
            Log.e(tag, "❌ Error saat mendeteksi teks", e)
            emptyList()
        }
    }

    private fun parseTextRecognitionResult(result: Text, uri: String): List<DetectedText> {
        val detectedTexts = mutableListOf<DetectedText>()

        if (result.text.isNotBlank()) {
            detectedTexts.add(
                DetectedText(
                    id = 0,
                    uri = uri,
                    text = result.text.trim(),
                    type = "FULL_TEXT",
                    confidence = 1.0f,
                    boundingBox = null
                )
            )
        }

        // Proses setiap blok teks
        for (block in result.textBlocks) {
            // Tambahkan blok teks jika cukup signifikan (minimal 3 karakter)
            if (block.text.length >= 3) {
                detectedTexts.add(
                    DetectedText(
                        id = 0,
                        uri = uri,
                        text = block.text.trim(),
                        type = "BLOCK",
                        confidence = calculateConfidence(),
                        boundingBox = block.boundingBox?.flattenToString()
                    )
                )
            }

            // Proses baris teks yang signifikan (kata kunci potensial)
            for (line in block.lines) {
                if (isSignificantText(line.text)) {
                    detectedTexts.add(
                        DetectedText(
                            id = 0,
                            uri = uri,
                            text = line.text.trim(),
                            type = "LINE",
                            confidence = calculateConfidence(),
                            boundingBox = line.boundingBox?.flattenToString()
                        )
                    )
                }

                // Tambahkan elemen yang mungkin kata kunci penting
                for (element in line.elements) {
                    if (isKeywordCandidate(element.text)) {
                        detectedTexts.add(
                            DetectedText(
                                id = 0,
                                uri = uri,
                                text = element.text.trim(),
                                type = "ELEMENT",
                                confidence = calculateConfidence(),
                                boundingBox = element.boundingBox?.flattenToString()
                            )
                        )
                    }
                }
            }
        }

        // Filter berdasarkan confidence dan batasi jumlah teks yang disimpan
        return detectedTexts
            .filter { it.confidence >= confidenceThreshold }
            .distinctBy { it.text.lowercase() }
            .sortedByDescending { it.confidence }
            .take(20) // Batasi jumlah teks yang disimpan per gambar
    }

    private fun calculateConfidence(): Float = 0.8f

    private fun isSignificantText(text: String): Boolean {
        val trimmed = text.trim()
        // Teks dianggap signifikan jika panjangnya minimal 4 karakter dan bukan hanya angka
        return trimmed.length >= 4 && !trimmed.all { it.isDigit() }
    }

    private fun isKeywordCandidate(text: String): Boolean {
        val trimmed = text.trim()
        // Kandidat keyword: minimal 4 karakter, tidak hanya angka, tidak ada karakter khusus
        return trimmed.length >= 4 &&
                !trimmed.all { it.isDigit() } &&
                trimmed.all { it.isLetterOrDigit() || it.isWhitespace() }
    }

    fun release() {
        textRecognizer.close()
        Log.d(tag, "Text recognizer berhasil dilepas")
    }
}