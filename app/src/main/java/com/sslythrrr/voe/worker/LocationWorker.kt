package com.sslythrrr.voe.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sslythrrr.voe.data.AppDatabase
import com.sslythrrr.voe.ui.utils.Notification
import com.sslythrrr.voe.data.entity.DeteksiGambar
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class LocationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val tag = "LocationWorker"
    private val imageDao = AppDatabase.getInstance(context).scannedImageDao()
    private val batchSize = 10
    private val notificationId = Notification.LOCATION_FETCH_NOTIFICATION_ID

    override suspend fun doWork(): Result {
        Log.d(tag, "🌍 Location Worker dimulai!")

        if (!isNetworkAvailable()) {
            Log.d(tag, "❌ Tidak ada koneksi internet, skip location fetch")
            return Result.success()
        }

        val needsNotification = inputData.getBoolean("needs_notification", false)
        if (needsNotification) {
            Notification.showProgressNotification(
                applicationContext,
                notificationId,
                "Mengambil Lokasi",
                "Memulai proses pencarian lokasi...",
                0,
                100
            )
        }

        try {
            val imagesToProcess = imageDao.getImagesNeedingLocation(1000)

            if (imagesToProcess.isEmpty()) {
                Log.d(tag, "✅ Tidak ada gambar yang perlu diproses")
                if (needsNotification) {
                    Notification.cancelNotification(applicationContext, notificationId)
                }
                return Result.success()
            }

            processLocationBatches(imagesToProcess, needsNotification)

            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Pencarian Lokasi Selesai",
                    "${imagesToProcess.size} lokasi telah diproses 📍"
                )
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "❌ Error during location fetch", e)

            if (needsNotification) {
                Notification.finishNotification(
                    applicationContext,
                    notificationId,
                    "Pencarian Lokasi Gagal",
                    "Terjadi kesalahan saat mencari lokasi"
                )
            }

            return Result.failure()
        }
    }

    private suspend fun processLocationBatches(
        images: List<DeteksiGambar>,
        needsNotification: Boolean
    ) {
        val totalBatches = images.chunked(batchSize)
        var processedImages = 0

        totalBatches.forEach { batch ->
            for (image in batch) {
                try {
                    if (image.latitude != null && image.longitude != null) {
                        val location = reverseGeocode(image.latitude, image.longitude)
                        if (location != null) {
                            imageDao.updateImageLocation(image.uri, location)
                            Log.d(tag, "✅ Location updated: ${image.nama} -> $location")
                        } else {
                            imageDao.markLocationFetchFailed(image.uri)
                            Log.d(tag, "❌ Location fetch failed: ${image.nama}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error processing location for: ${image.uri}", e)
                    imageDao.markLocationFetchFailed(image.uri)
                }

                // Rate limiting - 1 request per second
                delay(1000)
            }

            processedImages += batch.size
            val progressPercent = processedImages * 100 / images.size

            setProgress(workDataOf("progress" to progressPercent))

            if (needsNotification) {
                Notification.updateProgressNotification(
                    applicationContext,
                    notificationId,
                    "Mencari Lokasi",
                    "Memproses $processedImages dari ${images.size} gambar",
                    progressPercent,
                    100
                )
            }
        }
    }

    private fun reverseGeocode(latitude: Double, longitude: Double): String? {
        return try {
            val url =
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&addressdetails=1"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "GaleriApp/1.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                parseLocationFromResponse(response)
            } else {
                Log.e(tag, "HTTP Error: ${connection.responseCode}")
                null
            }
        } catch (e: IOException) {
            Log.e(tag, "Network error during geocoding", e)
            null
        } catch (e: Exception) {
            Log.e(tag, "Error during geocoding", e)
            null
        }
    }

    private fun parseLocationFromResponse(response: String): String? {
        return try {
            val json = JSONObject(response)
            val address = json.optJSONObject("address")

            if (address != null) {
                val parts = mutableListOf<String>()

                // 1. ROAD/STREET (optional - sering kosong di rural area)
                val road = address.optString("road").takeIf { it.isNotEmpty() }

                // 2. VILLAGE/HAMLET (level terkecil)
                val village = address.optString("village").takeIf { it.isNotEmpty() }
                    ?: address.optString("hamlet").takeIf { it.isNotEmpty() }
                    ?: address.optString("suburb").takeIf { it.isNotEmpty() }

                // 3. CITY/TOWN (level kota/kabupaten)
                val city = address.optString("city").takeIf { it.isNotEmpty() }
                    ?: address.optString("town").takeIf { it.isNotEmpty() }
                    ?: address.optString("county").takeIf { it.isNotEmpty() }

                // 4. STATE/PROVINCE (wajib ada)
                val state = address.optString("state").takeIf { it.isNotEmpty() }

                // 5. COUNTRY (untuk future international)
                val country = address.optString("country").takeIf { it.isNotEmpty() }

                // Build location string dengan prioritas
                // Format: [Village], [City], [State][, Country jika bukan Indonesia]

                road?.let { parts.add(it) }
                village?.let { parts.add(it) }
                city?.let { parts.add(it) }
                state?.let { parts.add(it) }
                country?.let { parts.add(it) }

                // Fallback: kalau masih kosong, ambil display_name
                if (parts.isEmpty()) {
                    val displayName = json.optString("display_name")
                    if (displayName.isNotEmpty()) {
                        // Ambil 3 bagian pertama dari display_name
                        return displayName.split(",").take(3).joinToString(",").trim()
                    }
                }
                parts.joinToString(", ")
            } else {
                Log.w(tag, "Tidak ada alamat")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing location response: $response", e)
            null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

object LocationRetryManager {
    fun checkAndRetryLocationFetch(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val retryWork = OneTimeWorkRequestBuilder<LocationRetryWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("location_retry")
            .build()

        workManager.enqueueUniqueWork(
            "location_retry",
            ExistingWorkPolicy.REPLACE,
            retryWork
        )
    }
}

class LocationRetryWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val imageDao = AppDatabase.getInstance(context).scannedImageDao()

    override suspend fun doWork(): Result {
        val imagesToRetry = imageDao.getImagesForLocationRetry()

        if (imagesToRetry.isNotEmpty()) {
            // Reset status untuk retry
            imageDao.resetLocationFetchStatus(imagesToRetry.map { it.uri })

            // Jalankan LocationWorker
            val locationWork = OneTimeWorkRequestBuilder<LocationWorker>()
                .setInputData(workDataOf("needs_notification" to false))
                .build()

            WorkManager.getInstance(applicationContext)
                .enqueue(locationWork)
        }

        return Result.success()
    }
}