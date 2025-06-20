package com.sslythrrr.galeri.viewmodel
//wf
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sslythrrr.galeri.data.AppDatabase
import com.sslythrrr.galeri.data.dao.DetectedObjectDao
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
    val images: List<String> = emptyList(), // Tambah field ini
    val showAllImagesButton: Boolean = false // Tambah field ini
)

class ChatbotViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Tambahkan state untuk error dan status NER
    private val _nerStatus = MutableStateFlow("Belum diinisialisasi")
    private val _intentStatus = MutableStateFlow("Intent belum diinisialisasi")

    private var nerProcessor: NerOnnxProcessor? = null
    private var isNerInitialized = false
    private var intentProcessor: IntentOnnxProcessor? = null
    private var isIntentInitialized = false

    private var detectedObjectDao: DetectedObjectDao? = null

    // Ganti fungsi yang ada dengan ini
    fun initializeProcessors(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Initialize database dao
                val database =
                    AppDatabase.getInstance(context) // Sesuaikan dengan nama database class Anda
                detectedObjectDao = database.detectedObjectDao()

                // Initialize Intent Processor
                _intentStatus.value = "Menginisialisasi Intent..."
                intentProcessor = IntentOnnxProcessor(context)
                isIntentInitialized = intentProcessor!!.initialize()

                if (isIntentInitialized) {
                    _intentStatus.value = "Intent siap digunakan"
                } else {
                    _intentStatus.value = "Intent gagal diinisialisasi"
                    intentProcessor = null
                }

                // Initialize NER Processor
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

        // Add user message
        val userMessage = ChatMessage(message, isUser = true)
        _messages.value = _messages.value + userMessage

        // Process message
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
                        "❌ Error processing: ${e.message}\n\n${generateFallbackResponse(message)}"
                    }
                } else {
                    val statusInfo = buildString {
                        append("🔧 Status:\n")
                        append("Intent: ${_intentStatus.value}\n")
                        append("NER: ${_nerStatus.value}\n\n")
                    }
                    statusInfo + generateFallbackResponse(message)
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

    private fun generateFallbackResponse(message: String): String {
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
            responseBuilder.append("Analisis Intent:\n")
            responseBuilder.append("🎯 Intent: ${intentResult.intent} (${
                "%.2f".format(
                    Locale.US,
                    intentResult.confidence * 100
                )
            }%)\n")
        }
        if (nerResult != null) {
            responseBuilder.append("Analisis NER:\n")
            val entities = nerResult.entities

            if (entities.isNotEmpty()) {
                entities.forEach { (entityType, values) ->
                    when (entityType) {
                        "label" -> responseBuilder.append("🏷️ Objek: ${values.joinToString(", ")}\n")
                        "text" -> responseBuilder.append("📝 Teks: ${values.joinToString(", ")}\n")
                        "album" -> responseBuilder.append("📁 Album: ${values.joinToString(", ")}\n")
                        "date" -> responseBuilder.append("📅 Tanggal: ${values.joinToString(", ")}\n")
                        "name" -> responseBuilder.append("📛 Nama: ${values.joinToString(", ")}\n")
                        "type" -> responseBuilder.append("🔧 Tipe: ${values.joinToString(", ")}\n")
                    }
                    responseBuilder.append("\n")
                }
            } else {
                responseBuilder.append("Tidak ditemukan entitas khusus.\n\n")
            }
        }
        if (intentResult?.intent == "cari_gambar") {
            val entities = nerResult?.entities
            if (entities != null && entities.containsKey("label")) {
                val objectLabels = entities["label"] ?: emptyList()

                if (objectLabels.isNotEmpty()) {
                    // Filter gambar berdasarkan objek yang terdeteksi
                    val filteredImages = filterImagesByObjects(objectLabels)
                    setAllFilteredImages(filteredImages)

                    if (filteredImages.isNotEmpty()) {
                        val labelText = objectLabels.joinToString(", ")
                        val template = responseTemplate.random()
                        val resultText = String.format(template, filteredImages.size, labelText)
                        responseBuilder.append(resultText)


                        // Tampilkan gambar sesuai jumlah yang ditemukan
                        when (filteredImages.size) {
                            0 -> {
                                responseBuilder.append(
                                    "Tidak ditemukan gambar dengan objek ${
                                        objectLabels.joinToString(
                                            ", "
                                        )
                                    }"
                                )
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
                                    images = filteredImages.take(3), // Ubah dari 2 ke 3
                                    showAllImagesButton = filteredImages.size > 3 // Ubah dari 2 ke 3
                                )
                                _messages.value = _messages.value + botMessage
                                return responseBuilder.toString()
                            }
                        }
                    } else {
                        responseBuilder.append(
                            "❌ Tidak ditemukan gambar dengan objek: ${
                                objectLabels.joinToString(
                                    ", "
                                )
                            }"
                        )
                    }
                } else {
                    responseBuilder.append("❓ Tidak ada objek spesifik yang terdeteksi untuk pencarian.")
                }
            } else {
                responseBuilder.append("❓ Silakan sebutkan objek yang ingin Anda cari, misalnya: 'cari gambar kucing' atau 'cari foto mobil'")
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

    private suspend fun filterImagesByObjects(objectLabels: List<String>): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val dao = detectedObjectDao
                if (dao == null) {
                    return@withContext emptyList<String>()
                }

                val filteredPaths = mutableSetOf<String>()

                objectLabels.forEach { label ->
                    // Gunakan query yang sudah dimodifikasi
                    val detectedObjects = dao.getImagesByLabel(label)
                    detectedObjects.forEach { obj ->
                        filteredPaths.add(obj.imagePath)
                    }
                }

                // Urutkan berdasarkan waktu modifikasi (terbaru dulu)
                filteredPaths.toList().sortedByDescending { path ->
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

}