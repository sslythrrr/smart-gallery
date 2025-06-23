package com.sslythrrr.galeri.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sslythrrr.galeri.data.AppDatabase
import com.sslythrrr.galeri.data.dao.DetectedObjectDao
import com.sslythrrr.galeri.data.dao.DetectedTextDao
import com.sslythrrr.galeri.data.dao.ScannedImageDao
import com.sslythrrr.galeri.ml.IntentOnnxProcessor
import com.sslythrrr.galeri.ml.IntentResult
import com.sslythrrr.galeri.ml.NerOnnxProcessor
import com.sslythrrr.galeri.ml.NerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val images: List<String> = emptyList(),
    val showAllImagesButton: Boolean = false
)

class ChatbotViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _nerStatus = MutableStateFlow("Belum diinisialisasi")
    private val _intentStatus = MutableStateFlow("Intent belum diinisialisasi")

    private var nerProcessor: NerOnnxProcessor? = null
    private var isNerInitialized = false
    private var intentProcessor: IntentOnnxProcessor? = null
    private var isIntentInitialized = false

    private var detectedObjectDao: DetectedObjectDao? = null
    private var detectedTextDao: DetectedTextDao? = null
    private var scannedImageDao: ScannedImageDao? = null

    fun initializeProcessors(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val database =
                    AppDatabase.getInstance(context)
                detectedObjectDao = database.detectedObjectDao()
                detectedTextDao = database.detectedTextDao()
                scannedImageDao = database.scannedImageDao()

                _intentStatus.value = "Menginisialisasi Intent..."
                intentProcessor = IntentOnnxProcessor(context)
                isIntentInitialized = intentProcessor!!.initialize()

                if (isIntentInitialized) {
                    _intentStatus.value = "Intent siap digunakan"
                } else {
                    _intentStatus.value = "Intent gagal diinisialisasi"
                    intentProcessor = null
                }

                _nerStatus.value = "Menginisialisasi NER..."
                nerProcessor = NerOnnxProcessor(context)
                isNerInitialized = nerProcessor!!.initialize()

                if (isNerInitialized) {
                    _nerStatus.value = "NER siap digunakan"
                } else {
                    _nerStatus.value = "NER gagal diinisialisasi"
                    nerProcessor = null
                }
            } catch (e: Exception) {
                _intentStatus.value = "Error Intent: ${e.message}"
                _nerStatus.value = "Error NER: ${e.message}"
                intentProcessor = null
                nerProcessor = null
                isIntentInitialized = false
                isNerInitialized = false
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        val userMessage = ChatMessage(message, isUser = true)
        _messages.value = _messages.value + userMessage
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            try {
                if (isIntentInitialized && isNerInitialized &&
                    intentProcessor != null && nerProcessor != null
                ) {
                    try {
                        val intentResult = intentProcessor?.processQuery(message)
                        val nerResult = nerProcessor?.processQuery(message)
                        generateResponse(intentResult, nerResult)
                    } catch (e: Exception) {
                        "❌ Error processing: ${e.message}\n\n${fallbackResponse(message)}"
                    }
                } else {
                    val statusInfo = buildString {
                        append("🔧 Status:\n")
                        append("Intent: ${_intentStatus.value}\n")
                        append("NER: ${_nerStatus.value}\n\n")
                    }
                    statusInfo + fallbackResponse(message)
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    "❌ Maaf, terjadi kesalahan: ${e.message}",
                    isUser = false
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fallbackResponse(message: String): String {
        return when {
            message.contains("foto", ignoreCase = true) ||
                    message.contains("gambar", ignoreCase = true) -> {
                "🖼️ Untuk pencarian foto/gambar, silakan gunakan fitur pencarian visual di halaman utama."
            }

            message.contains("video", ignoreCase = true) -> {
                "🎥 Untuk pencarian video, gunakan filter berdasarkan tipe file di galeri."
            }

            message.contains("album", ignoreCase = true) -> {
                "📁 Anda dapat mengorganisir media ke dalam album melalui menu pengaturan."
            }

            else -> {
                "🤖 AI processor sedang tidak tersedia. Silakan coba lagi nanti atau gunakan fitur pencarian manual."
            }
        }
    }

    private suspend fun generateResponse(
        intentResult: IntentResult?,
        nerResult: NerResult?
    ): String {
        val responseBuilder = StringBuilder()
        if (intentResult != null) {
            responseBuilder.append("Intent:\n")
            responseBuilder.append(
                "🎯 ${intentResult.intent} (${
                    "%.2f".format(
                        Locale.US,
                        intentResult.confidence * 100
                    )
                }%)\n"
            )
        }
        if (nerResult != null) {
            responseBuilder.append("NER:\n")
            val entities = nerResult.entities

            if (entities.isNotEmpty()) {
                entities.forEach { (entityType, values) ->
                    when (entityType) {
                        "label" -> responseBuilder.append("🏷️ Objek: ${values.joinToString(", ")}")
                        "text" -> responseBuilder.append("📝 Teks: ${values.joinToString(", ")}")
                        "album" -> responseBuilder.append("📁 Album: ${values.joinToString(", ")}")
                        "date" -> responseBuilder.append("📅 Tanggal: ${values.joinToString(", ")}")
                        "name" -> responseBuilder.append("📛 Nama: ${values.joinToString(", ")}")
                        "type" -> responseBuilder.append("🔧 Tipe: ${values.joinToString(", ")}")
                        "path" -> responseBuilder.append("📂 Path: ${values.joinToString(", ")}")
                        "size" -> responseBuilder.append("📏 Ukuran: ${values.joinToString(", ")}")
                        "resolution" -> responseBuilder.append("🖼️ Resolusi: ${values.joinToString(", ")}")
                        "month" -> responseBuilder.append("📆 Bulan: ${values.joinToString(", ")}")
                        "day" -> responseBuilder.append("📅 Hari: ${values.joinToString(", ")}")
                        "location" -> responseBuilder.append("📍 Lokasi: ${values.joinToString(", ")}")
                    }
                    responseBuilder.append("\n")
                }
            } else {
                responseBuilder.append("Tidak ditemukan entitas khusus.\n\n")
            }
        }
        if (intentResult?.intent == "cari_gambar") {
            val entities = nerResult?.entities
            if (entities != null && entities.isNotEmpty()) {
                val filteredImages = filterByNER(entities)
                setAllFilteredImages(filteredImages)

                if (filteredImages.isNotEmpty()) {
                    val entityDescription = entityDescription(entities)
                    responseBuilder.append("\n")
                    val template = responseTemplate.random()
                    val resultText = String.format(template, filteredImages.size, entityDescription)
                    responseBuilder.append(resultText)

                    when (filteredImages.size) {
                        0 -> {
                            responseBuilder.append("Tidak ditemukan gambar dengan kriteria: $entityDescription")
                        }

                        1 -> {
                            val botMessage = ChatMessage(
                                text = responseBuilder.toString(),
                                isUser = false,
                                images = filteredImages.take(1),
                                showAllImagesButton = false
                            )
                            _messages.value = _messages.value + botMessage
                            return responseBuilder.toString()
                        }

                        else -> {
                            val botMessage = ChatMessage(
                                text = responseBuilder.toString(),
                                isUser = false,
                                images = filteredImages.take(3),
                                showAllImagesButton = filteredImages.size > 3
                            )
                            _messages.value = _messages.value + botMessage
                            return responseBuilder.toString()
                        }
                    }
                } else {
                    val entityDescription = entityDescription(entities)
                    responseBuilder.append("❌ Tidak ditemukan gambar dengan kriteria: $entityDescription")
                }
            } else {
                responseBuilder.append("❓ Silakan sebutkan kriteria pencarian yang lebih spesifik, misalnya: 'cari gambar kucing di album liburan' atau 'cari foto dari tahun 2023'")
            }
        }
        val botMessage = ChatMessage(
            text = responseBuilder.toString(),
            isUser = false
        )
        _messages.value = _messages.value + botMessage

        return responseBuilder.toString()
    }

    private val responseTemplate = listOf(
        "Nih, ada %d gambar yang cocok sama kata \"%s\"~",
        "Ditemukan %d gambar dengan objek '%s'. Silakan dicek ya~",
        "Ketemu %d gambar sesuai dengan '%s'",
        "Ada %d gambar berisi '%s' di galeri kamu",
        "Aku berhasil nemuin %d gambar bertema '%s'",
        "Scan selesai! %d gambar cocok dengan: %s",
        "Galeri kamu punya %d gambar yang berkaitan dengan '%s'",
        "%d gambar cocok dengan pencarian kamu: '%s'",
        "Hasil pencarian menunjukkan %d gambar untuk '%s'",
        "Dapat %d gambar yang sesuai sama '%s'"
    )


    private val _allFilteredImages = MutableStateFlow<List<String>>(emptyList())
    val allFilteredImages: StateFlow<List<String>> = _allFilteredImages.asStateFlow()

    fun setAllFilteredImages(images: List<String>) {
        _allFilteredImages.value = images
    }

    private fun entityDescription(entities: Map<String, List<String>>): String {
        val descriptions = mutableListOf<String>()
        entities.forEach { (type, values) ->
            when (type) {
                "label" -> descriptions.add("objek ${values.joinToString(", ")}")
                "text" -> descriptions.add("teks ${values.joinToString(", ")}")
                "album" -> descriptions.add("album ${values.joinToString(", ")}")
                "date" -> descriptions.add("tahun ${values.joinToString(", ")}")
                "name" -> descriptions.add("nama ${values.joinToString(", ")}")
                "type" -> descriptions.add("format ${values.joinToString(", ")}")
                "path" -> descriptions.add("path ${values.joinToString(", ")}")
                "size" -> descriptions.add("ukuran ${values.joinToString(", ")}")
                "resolution" -> descriptions.add("resolusi ${values.joinToString(", ")}")
                "month" -> descriptions.add("bulan ${values.joinToString(", ")}")
                "day" -> descriptions.add("hari ${values.joinToString(", ")}")
                "location" -> descriptions.add("lokasi ${values.joinToString(", ")}")
            }
        }
        return descriptions.joinToString(" dan ")
    }

    private suspend fun filterByNER(entities: Map<String, List<String>>): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val detectedObjectDao = detectedObjectDao
                val detectedTextDao = detectedTextDao
                val scannedImageDao = scannedImageDao

                if (detectedObjectDao == null || detectedTextDao == null || scannedImageDao == null) {
                    return@withContext emptyList<String>()
                }

                val allMatchingPaths = mutableSetOf<String>()
                var isFirstEntity = true

                entities.forEach { (entityType, values) ->
                    val currentPaths = mutableSetOf<String>()

                    values.forEach { value ->
                        when (entityType) {
                            "label" -> {
                                val objects = detectedObjectDao.getImagesByLabel(value)
                                objects.forEach { obj -> currentPaths.add(obj.path) }
                            }

                            "text" -> {
                                val textPaths = detectedTextDao.searchImagesByText(value)
                                currentPaths.addAll(textPaths)
                            }

                            "album" -> {
                                val images = scannedImageDao.getImagesByAlbum(value)
                                images.forEach { img -> currentPaths.add(img.path) }
                            }

                            "date" -> {
                                val year = value.toInt()
                                val images = scannedImageDao.getImagesByYear(year)
                                images.forEach { img -> currentPaths.add(img.path) }
                            }

                            "name" -> {
                                val images = scannedImageDao.getImagesByName(value)
                                images.forEach { img -> currentPaths.add(img.path) }
                            }

                            "type" -> {
                                val images = scannedImageDao.getImagesByFormat(value)
                                images.forEach { img -> currentPaths.add(img.path) }
                            }

                            "location" -> {
                                val images = scannedImageDao.getImagesByLocation(value)
                                images.forEach { img -> currentPaths.add(img.path) }
                            }

                            "month" -> {
                                val images = scannedImageDao.getImagesByMonth(value)
                                images.forEach { img -> currentPaths.add(img.path) }
                            }

                            "day" -> {
                                val day = value.toInt()
                                val images = scannedImageDao.getImagesByDay(day)
                                images.forEach { img -> currentPaths.add(img.path) }
                            }
                            "path" -> {
                                val images = scannedImageDao.getImagesByPath(value)
                                images.forEach { img -> currentPaths.add(img.path) }
                            }

                            "resolution" -> {
                                val images = scannedImageDao.getImagesByResolution(value)
                                images.forEach { img -> currentPaths.add(img.path) }
                            }
                        }
                    }
                    if (isFirstEntity) {
                        allMatchingPaths.addAll(currentPaths)
                        isFirstEntity = false
                    } else {
                        allMatchingPaths.retainAll(currentPaths)
                    }
                    if (allMatchingPaths.isEmpty()) {
                        return@withContext emptyList<String>()
                    }
                }
                allMatchingPaths.toList().sortedByDescending { path ->
                    try {
                        File(path).lastModified()
                    } catch (_: Exception) {
                        0L
                    }
                }
            } catch (e: Exception) {
                println("❌ Error filtering images: ${e.message}")
                emptyList()
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}