package com.sslythrrr.voe.worker
//r
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sslythrrr.voe.Constants
import com.sslythrrr.voe.data.AppDatabase
import com.sslythrrr.voe.data.entity.DeteksiGambar
import com.sslythrrr.voe.ui.utils.Notification
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class MediaScanWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    private val tag = "Deteksi Gambar"
    private val imageDao = AppDatabase.getInstance(context).scannedImageDao()
    private val batchSize = 20
    private val notificationId = Notification.MEDIA_SCAN_NOTIFICATION_ID

    private data class MediaInfo(
        val id: Long,
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

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasInitialScanCompleted = prefs.getBoolean("initial_scan_completed", false)
        if (!hasInitialScanCompleted) {
            Log.d(tag, "⏭️ Pemindaian awal belum selesai, skip worker ini.")
            return Result.success()
        }

        val needsNotification = inputData.getBoolean("needs_notification", false)
        if (needsNotification) {
            Notification.createNotificationChannel(applicationContext)
            Notification.showProgressNotification(
                applicationContext,
                notificationId,
                "Memindai Media",
                "Memulai proses pemindaian...",
                0,
                100
            )
        }

        try {
            val contentResolver = applicationContext.contentResolver
            val scannedUris = imageDao.getAllScannedUris().toSet()
            val mediaToScan = scanAllMedia(contentResolver, scannedUris)

            if (mediaToScan.isEmpty()) {
                Log.d(tag, "✅ Tidak ada media baru untuk dipindai.")
                if (needsNotification) {
                    Notification.cancelNotification(applicationContext, notificationId)
                }
                return Result.success()
            }

            processMediaBatches(mediaToScan, needsNotification)

            if (needsNotification && mediaToScan.isNotEmpty()) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Pemindaian Media Selesai",
                    "${mediaToScan.size} media baru telah dipindai 🖼️📹"
                )
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "❌ Error selama pemindaian", e)
            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Pemindaian Gagal",
                    "Terjadi kesalahan saat memindai media"
                )
            }
            return Result.failure()
        }
    }

    private fun scanAllMedia(
        contentResolver: ContentResolver,
        scannedUris: Set<String>
    ): List<MediaInfo> {
        val result = mutableListOf<MediaInfo>()
        scanImages(contentResolver, scannedUris, result)
        return result
    }

    private fun scanImages(
        contentResolver: ContentResolver,
        scannedUris: Set<String>,
        result: MutableList<MediaInfo>
    ) {
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
        val selection = MediaStore.Images.Media.DATA + " LIKE ?"
        val selectionArgs = arrayOf("%${Constants.TARGET_DIRECTORY}%")
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, /*selection*/null, /*selectionArgs*/null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                if (!scannedUris.contains(uri.toString())) {
                    val fileName = cursor.getString(nameColumn)
                    val relativePath = cursor.getString(pathColumn) ?: ""
                    val baseStorage = Environment.getExternalStorageDirectory().absolutePath
                    val path = "$baseStorage/$relativePath$fileName"
                    val dateTaken = if (cursor.isNull(dateTakenColumn)) cursor.getLong(dateAddedColumn) * 1000 else cursor.getLong(dateTakenColumn)

                    result.add(
                        MediaInfo(
                            id = id,
                            uri = uri,
                            path = path,
                            name = fileName,
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
    }

    private suspend fun processMediaBatches(
        mediaList: List<MediaInfo>,
        needsNotification: Boolean = false
    ) = coroutineScope {
        var processedCount = 0
        mediaList.chunked(batchSize).forEach { batch ->
            val scannedMediaList = batch.map { mediaInfo ->
                async(Dispatchers.IO) {
                    try {
                        val (year, month, day) = formatDate(mediaInfo.date)
                        val latLng = getLatLongFromExif(applicationContext, mediaInfo.uri)
                        val fileHash = getFileHash(applicationContext, mediaInfo.uri)

                        DeteksiGambar(
                            uri = mediaInfo.uri.toString(),
                            path = mediaInfo.path,
                            nama = mediaInfo.name,
                            ukuran = mediaInfo.size,
                            type = mediaInfo.name.substringAfterLast('.', "").lowercase(),
                            album = mediaInfo.album,
                            resolusi = "${mediaInfo.width}x${mediaInfo.height}",
                            tanggal = mediaInfo.date,
                            tahun = year,
                            bulan = month,
                            hari = day,
                            latitude = latLng?.first,
                            longitude = latLng?.second,
                            fileHash = fileHash
                        )
                    } catch (e: Exception) {
                        Log.e(tag, "Error memproses metadata untuk: ${mediaInfo.uri}", e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            if (scannedMediaList.isNotEmpty()) {
                saveToDatabase(scannedMediaList)
            }

            processedCount += batch.size
            val progressPercent = processedCount * 100 / mediaList.size
            setProgress(workDataOf("progress" to progressPercent))

            if (needsNotification) {
                Notification.updateProgressNotification(
                    applicationContext,
                    notificationId,
                    "Memindai Media",
                    "Memproses $processedCount dari ${mediaList.size} media",
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

    private suspend fun saveToDatabase(deteksiGambars: List<DeteksiGambar>) {
        try {
            imageDao.insertAll(deteksiGambars)
            Log.d(tag, "✅ Database berhasil diupdate dengan ${deteksiGambars.size} media baru")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error menyimpan ke database", e)
        }
    }

    private fun formatDate(timestamp: Long): Triple<Int, String, Int> {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val monthNumber = calendar.get(Calendar.MONTH) + 1
        return Triple(
            calendar.get(Calendar.YEAR),
            getMonthName(monthNumber),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Januari"; 2 -> "Februari"; 3 -> "Maret"; 4 -> "April"
            5 -> "Mei"; 6 -> "Juni"; 7 -> "Juli"; 8 -> "Agustus"
            9 -> "September"; 10 -> "Oktober"; 11 -> "November"; 12 -> "Desember"
            else -> "Tidak Diketahui"
        }
    }
}

private fun getFileHash(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val md = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)
        var read: Int
        while (inputStream.read(buffer).also { read = it } > 0) {
            md.update(buffer, 0, read)
        }
        val digest = md.digest()
        val sb = StringBuilder()
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        sb.toString()
    } catch (e: Exception) {
        null
    }
}