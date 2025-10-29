package com.sslythrrr.voe.worker

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sslythrrr.voe.data.AppDatabase
import com.sslythrrr.voe.data.entity.DeteksiLabel
import com.sslythrrr.voe.data.entity.ScanStatus
import com.sslythrrr.voe.ml.LabelDetector
import com.sslythrrr.voe.ui.utils.Notification
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class LabelDetectorWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val tag = "Deteksi Label"
    private val notificationId = Notification.OBJECT_DETECTOR_NOTIFICATION_ID
    private val imageDao = AppDatabase.getInstance(context).scannedImageDao()
    private val objectDao = AppDatabase.getInstance(context).detectedObjectDao()
    private val scanStatusDao = AppDatabase.getInstance(context).scanStatusDao()
    private val labelDetector = LabelDetector(context)
    private val batchSize = 20
    private val workerName = "OBJECT_DETECTOR"

    private fun getCurrentProgress(): Pair<Int, Int> {
        val status = scanStatusDao.getScanStatus(workerName)
        return if (status != null && status.status == "RUNNING") {
            Pair(status.totalItems, status.processedItems)
        } else {
            Pair(0, 0)
        }
    }

    override suspend fun doWork(): Result {
        Log.d(tag, "🔥 Object Detector Worker dimulai! (Attempt: $runAttemptCount)")

        val needsNotification = inputData.getBoolean("needs_notification", false)
        if (needsNotification) {
            Notification.createNotificationChannel(applicationContext)
        }

        try {
            labelDetector.initialize()
            val scannedPaths = imageDao. getAllImageUris()
            val processedPaths = objectDao.getAllProcessedPaths().toSet()
            val pathsToProcess = scannedPaths.filter { it !in processedPaths }
            val (previousTotal, previousProcessed) = getCurrentProgress()
            val isResuming = previousTotal > 0 && previousProcessed > 0 && previousProcessed < previousTotal

            if (isResuming) {
                Log.d(tag, "🔄 RESUME: Melanjutkan dari $previousProcessed/$previousTotal")
                val actualProcessedPaths = objectDao.getAllProcessedPaths().toSet()
                val remainingPaths = scannedPaths.filter { it !in actualProcessedPaths }

                Log.d(tag, "📊 Sisa yang perlu diproses: ${remainingPaths.size}")

                if (remainingPaths.isEmpty()) {
                    Log.d(tag, "✅ Ternyata sudah selesai semua!")
                    updateScanStatus(scannedPaths.size, scannedPaths.size, "COMPLETED")
                    return Result.success()
                }
            }

            if (pathsToProcess.isEmpty()) {
                Log.d(tag, "✅ Tidak ada gambar yang perlu diproses")
                updateScanStatus(scannedPaths.size, scannedPaths.size, "COMPLETED")
                return Result.success()
            }

            Log.d(tag, "📋 Total gambar: ${scannedPaths.size}, Belum diproses: ${pathsToProcess.size}")
            updateScanStatus(scannedPaths.size, scannedPaths.size - pathsToProcess.size, "RUNNING")

            pathsToProcess.chunked(batchSize).forEachIndexed { index, batch ->
                val deteksiLabels = coroutineScope {
                    batch.map { path ->
                        async {
                            val realPath = getPathFromUri(path) ?: path
                            try {
                                val objects = labelDetector.detectObjects(realPath, path)
                                if (objects.isNotEmpty()) {
                                    Log.d(tag, "✅ Terdeteksi ${objects.size} objek pada $realPath")
                                } else {
                                    Log.d(tag, "⚠️ Tidak ada objek pada $realPath")
                                }
                                objects
                            } catch (e: Exception) {
                                Log.e(tag, "Error deteksi objek: $realPath", e)
                                emptyList<DeteksiLabel>()
                            }
                        }
                    }.awaitAll().flatten()
                }

                if (deteksiLabels.isNotEmpty()) {
                    saveToDatabase(deteksiLabels)
                }

                val batchProcessed = minOf((index + 1) * batchSize, pathsToProcess.size)
                val totalProcessed = (scannedPaths.size - pathsToProcess.size) + batchProcessed
                val progressPercent = (totalProcessed * 100) / scannedPaths.size

                updateScanStatus(scannedPaths.size, totalProcessed, "RUNNING")
                setProgress(workDataOf("progress" to progressPercent))

                Log.d(tag, "📊 Progress: $totalProcessed/${scannedPaths.size} ($progressPercent%)")

                if (needsNotification) {
                    Notification.updateProgressNotification(
                        applicationContext,
                        notificationId,
                        "Deteksi Label",
                        "Memproses $totalProcessed dari ${scannedPaths.size} gambar",
                        progressPercent,
                        100
                    )
                }
            }
            updateScanStatus(scannedPaths.size, scannedPaths.size, "COMPLETED")
            Log.d(tag, "✅ Object detection selesai, ${pathsToProcess.size} gambar diproses.")

            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Deteksi Label Selesai",
                    "${pathsToProcess.size} gambar telah diproses"
                )
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "❌ Error during object detection", e)
            updateScanStatus(0, 0, "FAILED")

            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Deteksi Label Gagal",
                    "Terjadi kesalahan saat memproses gambar"
                )
            }

            return Result.failure()
        }
    }

    private fun getPathFromUri(uriString: String): String? {
        return try {
            val uri = uriString.toUri()

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

    private fun saveToDatabase(deteksiLabels: List<DeteksiLabel>) {
        try {
            AppDatabase.getInstance(applicationContext).runInTransaction {
                objectDao.insertAll(deteksiLabels)
            }
            Log.d(tag, "✅ Database berhasil diupdate: ${deteksiLabels.size} objek")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error menyimpan ke database", e)
            e.printStackTrace()
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