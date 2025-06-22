package com.sslythrrr.galeri.worker

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sslythrrr.galeri.data.AppDatabase
import com.sslythrrr.galeri.data.entity.DetectedObject
import com.sslythrrr.galeri.data.entity.ScanStatus
import com.sslythrrr.galeri.ml.ObjectDetector
import com.sslythrrr.galeri.ui.utils.Notification
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ObjectDetectorWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val tag = "ObjectDetectorWorker"
    private val notificationId = Notification.OBJECT_DETECTOR_NOTIFICATION_ID
    private val imageDao = AppDatabase.getInstance(context).scannedImageDao()
    private val objectDao = AppDatabase.getInstance(context).detectedObjectDao()
    private val scanStatusDao = AppDatabase.getInstance(context).scanStatusDao()
    private val objectDetector = ObjectDetector(context)
    private val batchSize = 50

    private fun shouldSkipWork(): Boolean {
        val status = scanStatusDao.getScanStatus("OBJECT_DETECTOR")
        return status?.status == "COMPLETED"
    }

    private fun getCurrentProgress(): Pair<Int, Int> {
        val status = scanStatusDao.getScanStatus("OBJECT_DETECTOR")
        return if (status != null && status.status == "RUNNING") {
            Pair(status.totalItems, status.processedItems)
        } else {
            Pair(0, 0)
        }
    }

    override suspend fun doWork(): Result {
        Log.d(tag, "🔥 Object Detector Worker dimulai! (Attempt: $runAttemptCount)")

        if (shouldSkipWork()) {
            Log.d(tag, "⏭️ Pekerjaan sudah selesai sebelumnya, skip")
            return Result.success()
        }

        val needsNotification = inputData.getBoolean("needs_notification", false)
        if (needsNotification) {
            Notification.createNotificationChannel(applicationContext)
        }

        try {
            objectDetector.initialize()

            // Ambil semua path gambar
            val scannedPaths = imageDao.getAllScannedUris()
            val processedPaths = objectDao.getAllProcessedPaths().toSet()
            val pathsToProcess = scannedPaths.filter { it !in processedPaths }

            // Cek progress sebelumnya
            val (previousTotal, previousProcessed) = getCurrentProgress()
            val isResuming = previousTotal > 0 && previousProcessed > 0 && previousProcessed < previousTotal

            if (isResuming) {
                Log.d(tag, "🔄 RESUME: Melanjutkan dari $previousProcessed/$previousTotal")
                // Hitung ulang paths yang belum diproses
                val actualProcessedPaths = objectDao.getAllProcessedPaths().toSet()
                val remainingPaths = scannedPaths.filter { it !in actualProcessedPaths }

                Log.d(tag, "📊 Sisa yang perlu diproses: ${remainingPaths.size}")

                if (remainingPaths.isEmpty()) {
                    Log.d(tag, "✅ Ternyata sudah selesai semua!")
                    updateScanStatus("OBJECT_DETECTOR", scannedPaths.size, scannedPaths.size, "COMPLETED")
                    return Result.success()
                }
            }

            if (pathsToProcess.isEmpty()) {
                Log.d(tag, "✅ Tidak ada gambar yang perlu diproses")
                updateScanStatus("OBJECT_DETECTOR", scannedPaths.size, scannedPaths.size, "COMPLETED")
                return Result.success()
            }

            Log.d(tag, "📋 Total gambar: ${scannedPaths.size}, Belum diproses: ${pathsToProcess.size}")

            // Update status dengan data yang benar
            updateScanStatus("OBJECT_DETECTOR", scannedPaths.size, scannedPaths.size - pathsToProcess.size, "RUNNING")

            // Proses batch per batch
            pathsToProcess.chunked(batchSize).forEachIndexed { index, batch ->
                val detectedObjects = coroutineScope {
                    batch.map { path ->
                        async {
                            val realPath = getPathFromUri(path) ?: path
                            try {
                                val objects = objectDetector.detectObjects(realPath)
                                if (objects.isNotEmpty()) {
                                    Log.d(tag, "✅ Terdeteksi ${objects.size} objek pada $realPath")
                                } else {
                                    Log.d(tag, "⚠️ Tidak ada objek pada $realPath")
                                }
                                objects
                            } catch (e: Exception) {
                                Log.e(tag, "Error deteksi objek: $realPath", e)
                                emptyList<DetectedObject>()
                            }
                        }
                    }.awaitAll().flatten()
                }

                if (detectedObjects.isNotEmpty()) {
                    saveToDatabase(detectedObjects)
                }

                val batchProcessed = minOf((index + 1) * batchSize, pathsToProcess.size)
                val totalProcessed = (scannedPaths.size - pathsToProcess.size) + batchProcessed
                val progressPercent = (totalProcessed * 100) / scannedPaths.size

                updateScanStatus("OBJECT_DETECTOR", scannedPaths.size, totalProcessed, "RUNNING")
                setProgress(workDataOf("progress" to progressPercent))

                Log.d(tag, "📊 Progress: $totalProcessed/${scannedPaths.size} ($progressPercent%)")

                if (needsNotification) {
                    Notification.updateProgressNotification(
                        applicationContext,
                        notificationId,
                        "Object Detection",
                        "Memproses $totalProcessed dari ${scannedPaths.size} gambar",
                        progressPercent,
                        100
                    )
                }
            }
            updateScanStatus("OBJECT_DETECTOR", scannedPaths.size, scannedPaths.size, "COMPLETED")
            Log.d(tag, "✅ Object detection selesai, ${pathsToProcess.size} gambar diproses.")

            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Object Detection Selesai",
                    "${pathsToProcess.size} gambar telah diproses"
                )
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "❌ Error during object detection", e)
            updateScanStatus("OBJECT_DETECTOR", 0, 0, "FAILED")

            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Object Detection Gagal",
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

    private fun saveToDatabase(detectedObjects: List<DetectedObject>) {
        try {
            AppDatabase.getInstance(applicationContext).runInTransaction {
                objectDao.insertAll(detectedObjects)
                detectedObjects.forEach {
                    Log.d(
                        tag,
                        "Disimpan objek terdeteksi: ${it.label} (${it.confidence}) untuk ${it.imagePath}"
                    )
                }
            }
            Log.d(tag, "✅ Database berhasil diupdate: ${detectedObjects.size} objek")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error menyimpan ke database", e)
            e.printStackTrace()
        }
    }

    private fun updateScanStatus(workerName: String, total: Int, processed: Int, status: String) {
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