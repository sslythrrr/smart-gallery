package com.sslythrrr.galeri.worker
//r
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sslythrrr.galeri.data.AppDatabase
import com.sslythrrr.galeri.data.entity.ScannedImage
import com.sslythrrr.galeri.ui.utils.Notification
import java.util.Calendar

class MediaScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    private val tag = "MediaScanWorker"
    private val imageDao = AppDatabase.getInstance(context).scannedImageDao()
    private val batchSize = 500
    private val notificationId = Notification.MEDIA_SCAN_NOTIFICATION_ID

    override suspend fun doWork(): Result {
        Log.d(tag, "🔥 Media Scan Worker dimulai!")

        val needsNotification = inputData.getBoolean("needs_notification", false)
        if (needsNotification) {
            Notification.createNotificationChannel(applicationContext)
            Notification.showProgressNotification(
                applicationContext,
                notificationId,
                "Memindai Gambar",
                "Memulai proses pemindaian...",
                0,
                100
            )
        }

        try {
            val contentResolver = applicationContext.contentResolver
            val scannedUris = imageDao.getAllScannedUris().toSet()
            val imagesToScan = imageScanning(contentResolver, scannedUris)

            if (imagesToScan.isEmpty() && needsNotification) {
                Notification.cancelNotification(applicationContext, notificationId)
                return Result.success()
            }

            processImageBatches(imagesToScan, needsNotification)

            if (needsNotification && imagesToScan.isNotEmpty()) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Pemindaian Gambar Selesai",
                    "${imagesToScan.size} gambar telah dipindai \uD83E\uDD96"
                )
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "❌ Error during scan", e)

            val needsNotification = inputData.getBoolean("needs_notification", false)
            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Pemindaian Gambar Gagal",
                    "Terjadi kesalahan saat memindai gambar"
                )
            }

            return Result.failure()
        }
    }

    private fun imageScanning(
        contentResolver: ContentResolver,
        scannedUris: Set<String>
    ): List<ImageInfo> {
        val result = mutableListOf<ImageInfo>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val albumColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                if (!scannedUris.contains(uri.toString())) {
                    val dateTaken = if (cursor.isNull(dateTakenColumn)) {
                        cursor.getLong(dateAddedColumn) * 1000
                    } else {
                        cursor.getLong(dateTakenColumn)
                    }
                    result.add(
                        ImageInfo(
                            uri = uri,
                            path = cursor.getString(pathColumn),
                            name = cursor.getString(nameColumn),
                            size = cursor.getLong(sizeColumn),
                            type = cursor.getString(typeColumn),
                            album = cursor.getString(albumColumn),
                            width = cursor.getInt(widthColumn),
                            height = cursor.getInt(heightColumn),
                            date = dateTaken
                        )
                    )
                }
            }
        }
        return result
    }

    private suspend fun processImageBatches(
        images: List<ImageInfo>,
        needsNotification: Boolean = false
    ) {
        val totalBatches = images.chunked(batchSize)
        var processedImages = 0

        totalBatches.forEach { batch ->
            val scannedImages = mutableListOf<ScannedImage>()

            for (imageInfo in batch) {
                try {
                    val (year, month) = formatDate(imageInfo.date)
                    val latLng = getLatLongFromExif(applicationContext, imageInfo.uri)

                    val image = ScannedImage(
                        uri = imageInfo.uri.toString(),
                        path = imageInfo.path,
                        nama = imageInfo.name,
                        ukuran = imageInfo.size,
                        type = imageInfo.type,
                        album = imageInfo.album,
                        resolusi = "${imageInfo.width}x${imageInfo.height}",
                        tanggal = imageInfo.date,
                        year = year,
                        month = month,
                        day = Calendar.getInstance().apply { timeInMillis = imageInfo.date }
                            .get(Calendar.DAY_OF_MONTH),
                        latitude = latLng?.first,
                        longitude = latLng?.second,
                        isFavorite = false,
                        isArchive = false,
                        isDeleted = false,
                        deletedAt = null
                    )
                    scannedImages.add(image)

                } catch (e: Exception) {
                    Log.e(tag, "Error processing image metadata: ${imageInfo.uri}", e)
                }
            }

            if (scannedImages.isNotEmpty()) {
                saveToDatabase(scannedImages)
            }

            processedImages += batch.size
            val progressPercent = processedImages * 100 / images.size

            setProgress(workDataOf("progress" to progressPercent))

            if (needsNotification) {
                Notification.updateProgressNotification(
                    applicationContext,
                    notificationId,
                    "Media Scanning",
                    "Memproses $processedImages dari ${images.size} gambar",
                    progressPercent,
                    100
                )
            }
        }
    }

    private fun getLatLongFromExif(context: Context, uri: Uri): Pair<Double, Double>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                exif.latLong?.let { Pair(it[0].toDouble(), it[1].toDouble()) }
            }
        } catch (e: Exception) {
            Log.e("EXIF", "Gagal ambil lokasi EXIF dari $uri", e)
            null
        }
    }


    private suspend fun saveToDatabase(scannedImages: List<ScannedImage>) {
        try {
            val insertedIds = imageDao.insertAll(scannedImages)
            Log.d(tag, "✅ Database berhasil diupdate: ${insertedIds.size} gambar")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error menyimpan ke database", e)
        }
    }


    data class ImageInfo(
        val uri: Uri,
        val path: String,
        val name: String,
        val size: Long,
        val type: String,
        val album: String,
        val width: Int,
        val height: Int,
        val date: Long
    )

    private fun formatDate(timestamp: Long): Pair<Int, Int> {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return Pair(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }
}