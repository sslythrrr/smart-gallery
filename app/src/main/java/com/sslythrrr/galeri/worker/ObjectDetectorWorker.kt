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
    private val batchSize = 15

    private fun canRetryWork(): Boolean {
        val status = scanStatusDao.getScanStatus("OBJECT_DETECTOR")
        return status?.status != "COMPLETED"
    }

    private fun shouldSkipWork(): Boolean {
        val status = scanStatusDao.getScanStatus("OBJECT_DETECTOR")
        return status?.status == "COMPLETED" && runAttemptCount == 0
    }

    override suspend fun doWork(): Result {
        Log.d(tag, "🔥 Object Detector Worker dimulai!")
        if (shouldSkipWork()) {
            Log.d(tag, "⏭️ Pekerjaan sudah selesai sebelumnya, skip")
            return Result.success()
        }

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
                "Object Detection",
                "Memulai deteksi objek...",
                0,
                100
            )
        }

        try {
            objectDetector.initialize()

            val existingStatus = scanStatusDao.getScanStatus("OBJECT_DETECTOR")
            if (existingStatus?.status == "FAILED") {
                updateScanStatus("OBJECT_DETECTOR", 0, 0, "PENDING")
            }

            // Ambil semua path gambar yang sudah discan metadata-nya
            val scannedPaths = imageDao.getAllScannedUris()

            // Ambil semua path gambar yang sudah dideteksi objeknya
            val processedPaths = objectDao.getAllProcessedPaths().toSet()

            // Filter gambar yang belum diproses
            val pathsToProcess = scannedPaths.filter { it !in processedPaths }

            // Check if resuming from previous run
            val isResuming = existingStatus?.status == "RUNNING"

            if (isResuming) {
                Log.d(tag, "🔄 Melanjutkan object detection dari sebelumnya...")
                if (needsNotification) {
                    Notification.showProgressNotification(
                        applicationContext,
                        notificationId,
                        "Object Detection",
                        "Melanjutkan deteksi objek...",
                        existingStatus.processedItems * 100 / existingStatus.totalItems,
                        100
                    )
                }
            }

            Log.d(tag, "Ditemukan ${pathsToProcess.size} gambar untuk diproses object detection")

            // Update status awal
            updateScanStatus("OBJECT_DETECTOR", pathsToProcess.size, 0, "RUNNING")

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

                // Update progress
                val processedCount = (index + 1) * batchSize

                val actualProcessed = minOf(processedCount, pathsToProcess.size)
                val progressPercent = (actualProcessed * 100) / pathsToProcess.size
                updateScanStatus("OBJECT_DETECTOR", pathsToProcess.size, actualProcessed, "RUNNING")
                setProgress(workDataOf("progress" to progressPercent))

                if (needsNotification) {
                    Notification.updateProgressNotification(
                        applicationContext,
                        notificationId,
                        "Object Detection",
                        "Memproses $actualProcessed dari ${pathsToProcess.size} gambar",
                        progressPercent,
                        100
                    )
                }
            }
            // Update final status
            updateScanStatus(
                "OBJECT_DETECTOR",
                pathsToProcess.size,
                pathsToProcess.size,
                "COMPLETED"
            )
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