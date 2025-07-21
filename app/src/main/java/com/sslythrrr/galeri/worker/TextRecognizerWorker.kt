package com.sslythrrr.galeri.worker

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sslythrrr.galeri.data.AppDatabase
import com.sslythrrr.galeri.data.entity.DetectedText
import com.sslythrrr.galeri.data.entity.ScanStatus
import com.sslythrrr.galeri.ml.TextRecognizerHelper
import com.sslythrrr.galeri.ui.utils.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class TextRecognizerWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val tag = "TextRecognizerWorker"
    private val notificationId = Notification.TEXT_RECOGNIZER_NOTIFICATION_ID
    private val imageDao = AppDatabase.getInstance(context).scannedImageDao()
    private val textDao = AppDatabase.getInstance(context).detectedTextDao()
    private val scanStatusDao = AppDatabase.getInstance(context).scanStatusDao()
    private val textRecognizer = TextRecognizerHelper()
    private val batchSize = 20
    private val workerName = "TEXT_RECOGNIZER"

    private fun canRetryWork(): Boolean {
        val status = scanStatusDao.getScanStatus(workerName)
        return status?.status != "COMPLETED"
    }

    override suspend fun doWork(): Result {
        Log.d(tag, "🔥 Text Recognizer Worker dimulai!")

        if (!canRetryWork() && runAttemptCount > 0) {
            Log.d(tag, "⏭️ Pekerjaan sudah selesai, tidak perlu retry")
            return Result.success()
        }

        val needsNotification = inputData.getBoolean("needs_notification", false)
        if (needsNotification) {
            Notification.createNotificationChannel(applicationContext)
            Notification.showProgressNotification(
                applicationContext,
                notificationId,
                "Deteksi Teks",
                "Memulai pengenalan teks...",
                0,
                100
            )
        }

        try {
            // Ambil semua path gambar yang sudah discan metadata-nya
            val scannedPaths = imageDao. getAllImageUris()

            // Ambil semua path gambar yang sudah dikenali teksnya
            val processedPaths = textDao.getAllProcessedPaths().toSet()

            // Filter gambar yang belum diproses
            val pathsToProcess = scannedPaths
                .filter { it !in processedPaths }
            //.filter { textRecognizer.isImageLikelyToContainText(it) }

            // Check if resuming from previous run
            val existingStatus = scanStatusDao.getScanStatus(workerName)
            val isResuming = existingStatus?.status == "RUNNING"

            if (isResuming) {
                Log.d(tag, "🔄 Melanjutkan text recognition dari sebelumnya...")
                if (needsNotification) {
                    Notification.showProgressNotification(
                        applicationContext,
                        notificationId,
                        "Pengenalan Teks",
                        "Melanjutkan pengenalan teks...",
                        existingStatus.processedItems * 100 / existingStatus.totalItems,
                        100
                    )
                }
            }

            Log.d(tag, "Ditemukan ${pathsToProcess.size} gambar untuk diproses text recognition")

            // Update status awal
            updateScanStatus(pathsToProcess.size, 0, "RUNNING")

            // Proses batch per batch
            pathsToProcess.chunked(batchSize).forEachIndexed { index, batch ->
                val detectedTexts = withContext(Dispatchers.Default) {
                    batch.map { uri ->
                        async {
                            try {
                                val realPath = getPathFromUri(uri) ?: uri
                                textRecognizer.detectTexts(realPath, uri)
                            } catch (e: Exception) {
                                Log.e(tag, "Error recognizing text for image: $uri", e)
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()
                }

                if (detectedTexts.isNotEmpty()) {
                    saveToDatabase(detectedTexts)
                }

                // Update progress
                val processedCount = (index + 1) * batchSize
                val actualProcessed = minOf(processedCount, pathsToProcess.size)
                val progressPercent = actualProcessed * 100 / pathsToProcess.size

                updateScanStatus(pathsToProcess.size, actualProcessed, "RUNNING")
                setProgress(workDataOf("progress" to progressPercent))

                if (needsNotification) {
                    Notification.updateProgressNotification(
                        applicationContext,
                        notificationId,
                        "Pengenalan Teks",
                        "Memproses $actualProcessed dari ${pathsToProcess.size} gambar",
                        progressPercent,
                        100
                    )
                }
            }

            textRecognizer.release()

            // Update final status
            updateScanStatus(
                pathsToProcess.size,
                pathsToProcess.size,
                "COMPLETED"
            )
            Log.d(tag, "✅ Text recognition selesai, ${pathsToProcess.size} gambar diproses.")

            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Pengenalan Teks Selesai",
                    "${pathsToProcess.size} gambar telah diproses"
                )
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "❌ Error during text recognition", e)
            updateScanStatus(0, 0, "FAILED")

            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Pengenalan Teks Gagal",
                    "Terjadi kesalahan saat memproses gambar"
                )
            }

            return Result.failure()
        }
    }

    private fun getPathFromUri(uriString: String): String? {
        return try {
            val uri = uriString.toUri()

            // Jika sudah berupa path langsung
            if (uriString.startsWith("/")) {
                return uriString
            }

            val projection = arrayOf(MediaStore.Images.Media.DATA)
            applicationContext.contentResolver.query(uri, projection, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        cursor.getString(columnIndex)
                    } else null
                }
        } catch (e: Exception) {
            Log.w(tag, "Gagal konversi URI ke path: $uriString - ${e.message}")
            null
        }
    }

    private suspend fun saveToDatabase(detectedTexts: List<DetectedText>) {
        try {
            withContext(Dispatchers.IO) {
                // Batch insert yang lebih efisien
                detectedTexts.chunked(100).forEach { chunk ->
                    AppDatabase.getInstance(applicationContext).runInTransaction {
                        textDao.insertAll(chunk)
                    }
                }
            }
            Log.d(tag, "✅ Database berhasil diupdate: ${detectedTexts.size} teks")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error menyimpan ke database", e)
        }
    }

    private fun updateScanStatus(total: Int, processed: Int, status: String) {
        try {
            val scanStatus = ScanStatus(
                workerName = workerName,
                totalItems = total,
                processedItems = processed,
                status = status,
                lastUpdated = System.currentTimeMillis()
            )
            scanStatusDao.insertOrUpdate(scanStatus)
        } catch (e: Exception) {
            Log.e(tag, "Error updating scan status", e)
        }
    }
}