package com.sslythrrr.galeri.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sslythrrr.galeri.data.AppDatabase
import com.sslythrrr.galeri.data.dao.DetectedObjectDao
import com.sslythrrr.galeri.data.dao.DetectedTextDao
import com.sslythrrr.galeri.data.dao.ScannedImageDao
import com.sslythrrr.galeri.data.dao.SearchHistoryDao
import com.sslythrrr.galeri.data.entity.ScannedImage
import com.sslythrrr.galeri.data.entity.SearchHistory
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
import java.util.Locale
import com.sslythrrr.galeri.ml.SimilarityProcessor

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val images: List<ScannedImage> = emptyList(),
    val showAllImagesButton: Boolean = false
)

class ChatbotViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    private val _allFilteredImages = MutableStateFlow<List<ScannedImage>>(emptyList())
    val allFilteredImages: StateFlow<List<ScannedImage>> = _allFilteredImages.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val intentTreshold = 0.985
    private val _nerStatus = MutableStateFlow("Belum diinisialisasi")
    private val _intentStatus = MutableStateFlow("Intent belum diinisialisasi")

    private var nerProcessor: NerOnnxProcessor? = null
    private var isNerInitialized = false
    private var intentProcessor: IntentOnnxProcessor? = null
    private var isIntentInitialized = false
    private var similarityProcessor: SimilarityProcessor? = null
    private var isSimilarityInitialized = false
    private var detectedObjectDao: DetectedObjectDao? = null
    private var detectedTextDao: DetectedTextDao? = null
    private var scannedImageDao: ScannedImageDao? = null
    private var searchHistoryDao: SearchHistoryDao? = null

    val validLabels = setOf(
        "abu-abu", "bersepeda", "hewan", "merah", "perak",
        "aktivitas", "biru", "hijau", "minuman", "peralatan",
        "alam", "chat", "hitam", "mobil", "pink",
        "aplikasi", "elektronik", "hutan", "motor", "putih",
        "bangunan", "emas", "kuning", "olahraga", "tanaman",
        "bank", "furnitur", "makanan", "orang", "tas",
        "berenang", "video game", "media sosial", "pakaian", "transportasi",
        "berlari", "gunung", "memancing", "pantai", "ungu"
    )

    val labelAlias = mapOf(
        "kucing" to "hewan", "anjing" to "hewan", "sapi" to "hewan",
        "burung" to "hewan", "ular" to "hewan", "ikan" to "hewan",
        "kuda" to "hewan", "harimau" to "hewan", "macan" to "hewan",
        "kerbau" to "hewan", "gajah" to "hewan",
        "nasi" to "makanan", "mie" to "makanan", "roti" to "makanan",
        "kue" to "makanan", "sate" to "makanan", "burger" to "makanan",
        "pizza" to "makanan", "ayam goreng" to "makanan",
        "kopi" to "minuman", "teh" to "minuman", "air mineral" to "minuman",
        "susu" to "minuman", "jus" to "minuman", "soda" to "minuman",
        "kereta" to "transportasi", "pesawat" to "transportasi",
        "kapal" to "transportasi", "mobil sport" to "mobil",
        "truk" to "mobil", "angkot" to "mobil",
        "baju" to "pakaian", "celana" to "pakaian", "jaket" to "pakaian",
        "kaos" to "pakaian", "topi" to "pakaian",
        "abu" to "abu-abu", "silver" to "perak", "gold" to "emas",
        "ungu muda" to "ungu", "ungu tua" to "ungu",
        "laut" to "pantai", "danau" to "alam", "sungai" to "alam",
        "pohon" to "tanaman", "daun" to "tanaman", "kebun" to "tanaman",
        "hutan hujan" to "hutan",
        "lari" to "berlari", "berenang di kolam" to "berenang",
        "memancing ikan" to "memancing", "bersepeda di taman" to "bersepeda",
        "basket" to "olahraga",
        "hp" to "elektronik", "smartphone" to "elektronik",
        "laptop" to "elektronik", "tv" to "elektronik",
        "kamera" to "elektronik", "console" to "video game",
        "whatsapp" to "chat", "telegram" to "chat", "line" to "chat",
        "instagram" to "media sosial", "tiktok" to "media sosial",
        "facebook" to "media sosial", "twitter" to "media sosial",
        "sofa" to "furnitur", "kursi" to "furnitur", "meja" to "furnitur",
        "lemari" to "furnitur",
        "ransel" to "tas", "tas selempang" to "tas", "tas sekolah" to "tas"
    )

    private fun mapNERAliases(entities: Map<String, List<String>>): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        for ((type, values) in entities) {
            if (type == "label") {
                for (value in values) {
                    val normalized = value.lowercase()
                    val mapped = labelAlias[normalized]
                    if (mapped != null && validLabels.contains(mapped)) {
                        result.getOrPut("label") { mutableListOf() }.add(mapped)
                    } else if (validLabels.contains(normalized)) {
                        result.getOrPut("label") { mutableListOf() }.add(normalized)
                    }
                }
            } else {
                result.getOrPut(type) { mutableListOf() }.addAll(values)
            }
        }
        return result
    }

    fun initializeProcessors(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val database =
                    AppDatabase.getInstance(context)
                detectedObjectDao = database.detectedObjectDao()
                detectedTextDao = database.detectedTextDao()
                scannedImageDao = database.scannedImageDao()
                searchHistoryDao = database.searchHistoryDao()

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

                similarityProcessor = SimilarityProcessor(context)
                isSimilarityInitialized = similarityProcessor!!.initialize()
                println("✅ [DEBUG INIT] Status Inisialisasi Similarity: $isSimilarityInitialized")
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
                        generateResponse(intentResult, nerResult, message)
                    } catch (e: Exception) {
                        val errorMessage = ChatMessage(
                            "Waduh, ada error nih: ${e.message}",
                            isUser = false
                        )
                        _messages.value = _messages.value + errorMessage
                        e.printStackTrace()
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
                "Coba gunakan perintah cari gambar"
            }
            else -> {
                "NLP sedang tidak tersedia. Silakan coba lagi nanti."
            }
        }
    }

    private val salamAwalResponses = listOf(
        "Halo! Ada yang bisa aku bantu?",
        "Hai, mau cari gambar apa hari ini?",
        "Selamat datang! Yuk mulai cari foto yang kamu butuhin",
        "Apa yang bisa aku bantu hari ini?"
    )

    private val terimaKasihResponses = listOf(
        "Sama-sama, senang bisa bantu",
        "Kapan pun kamu butuh bantuan, tinggal bilang ya!",
        "Oke, kalau ada yang lain langsung aja yaa",
        "Sip! Kalau ada yang mau dicari lagi, tinggal bilang"
    )

    private suspend fun generateResponse(
        intentResult: IntentResult?,
        nerResult: NerResult?,
        originalQuery: String
    ): String {
        val responseBuilder = StringBuilder()
        /*if (intentResult != null && intentResult.confidence >= intentTreshold) { // Tambah pengecekan di sini
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
                        "nama_gambar" -> responseBuilder.append("📛 Nama: ${values.joinToString(", ")}")
                        "tahun" -> responseBuilder.append("📅 Tahun: ${values.joinToString(", ")}")
                        "bulan" -> responseBuilder.append("📆 Bulan: ${values.joinToString(", ")}")
                        "hari" -> responseBuilder.append("📅 Hari: ${values.joinToString(", ")}")
                        "label" -> responseBuilder.append("🏷️ Label: ${values.joinToString(", ")}")
                        "teks" -> responseBuilder.append("📝 Teks: ${values.joinToString(", ")}")
                        "format" -> responseBuilder.append("🔧 Format: ${values.joinToString(", ")}")
                        "album" -> responseBuilder.append("📁 Album: ${values.joinToString(", ")}")
                        "lokasi" -> responseBuilder.append("📍 Lokasi: ${values.joinToString(", ")}")
                        "koleksi" -> responseBuilder.append("📚 Koleksi: ${values.joinToString(", ")}")
                    }
                    responseBuilder.append("\n")
                }
            } else {
                responseBuilder.append("Tidak ditemukan entitas khusus.\n\n")
            }
        }
*/
        when (intentResult?.takeIf { it.confidence >= intentTreshold }?.intent) {
            "salam_awal" -> {
                responseBuilder.append(salamAwalResponses.random())
            }

            "ucapan_terima_kasih" -> {
                responseBuilder.append(terimaKasihResponses.random())
            }

            "bantuan" -> {
                responseBuilder.append(generateHelpMessage())
            }

            "hitung_media" -> {
                val total = scannedImageDao!!.countAllMedia()
                val photos = scannedImageDao!!.countAllImages()
                val videos = scannedImageDao!!.countAllVideos()
                responseBuilder.append("Tentu! Saat ini ada total $total media di Piece of Eden-mu, terdiri dari $photos foto dan $videos video! ✨")
            }

            "cari_gambar" -> {
                val entities = nerResult?.entities
                if (entities != null && entities.isNotEmpty()) {
                    val mappedEntities = mapNERAliases(entities)
                    val filteredImages = filterByNER(mappedEntities)
                    setAllFilteredImages(filteredImages)

                    if (filteredImages.isNotEmpty()) {
                        searchHistoryDao?.insert(SearchHistory(query = originalQuery))
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
                        // NER jalan tapi nggak nemu gambar, fallback ke B-Plan
                        val bPlanMessage = executeBPlan(originalQuery)
                        _messages.value = _messages.value + bPlanMessage
                        return "" // return string kosong karena pesan sudah dikirim
                    }
                } else {
                    // NER nggak nemu entitas, langsung fallback ke B-Plan
                    val bPlanMessage = executeBPlan(originalQuery)
                    _messages.value = _messages.value + bPlanMessage
                    return "" // return string kosong karena pesan sudah dikirim
                }
            }

            else -> {
                // Intent tidak terdeteksi atau confidence rendah, langsung ke B-Plan
                val bPlanMessage = executeBPlan(originalQuery)
                _messages.value = _messages.value + bPlanMessage
                return "" // return string kosong karena pesan sudah dikirim
            }
        }
        val botMessage = ChatMessage(
            text = responseBuilder.toString(),
            isUser = false
        )
        _messages.value = _messages.value + botMessage
        return responseBuilder.toString()
    }

    private suspend fun executeBPlan(query: String): ChatMessage {
        val albumResults = scannedImageDao?.searchAlbumByName(query) ?: emptyList()
        val collectionResults = scannedImageDao?.searchCollectionByName(query) ?: emptyList()
        val combinedResults = (albumResults + collectionResults).distinctBy { it.uri }

        if (combinedResults.isNotEmpty()) {
            searchHistoryDao?.insert(SearchHistory(query = query))
            return createImageResponseMessage(combinedResults, "album atau koleksi '$query'")
        }

        // Tahap 2: Kalau nggak ketemu, baru pakai Similarity Model
        if (isSimilarityInitialized) {
            val similarLabels = similarityProcessor?.calculateSimilarity(query, top = 2, threshold = 0.85f) ?: emptyList()

            // --- TAMBAHKAN INI UNTUK DEBUGGING ---
            println("👀 [DEBUG SIMILARITY] Query: '$query'")
            println("👀 [DEBUG SIMILARITY] Label Terpilih (Threshold > 0.85): $similarLabels")
            // ------------------------------------

            if (similarLabels.isNotEmpty()) {
                val imageResults = mutableListOf<ScannedImage>()
                similarLabels.forEach { label ->
                    val imagesFromLabel = detectedObjectDao?.getImagesByLabel(label) ?: emptyList()
                    imageResults.addAll(imagesFromLabel)
                }
                val distinctImages = imageResults.distinctBy { it.uri }

                if (distinctImages.isNotEmpty()) {
                    searchHistoryDao?.insert(SearchHistory(query = query))
                    val labelNames = similarLabels.joinToString(" atau ")
                    return createImageResponseMessage(distinctImages, "gambar yang mirip dengan '$labelNames'")
                }
            }

        }

        // Tahap 3: Final, kalau semua gagal
        return ChatMessage("Waduh, aku udah coba cari kemana-mana tapi nggak nemu gambar yang cocok untuk '$query'. Coba kata kunci lain yuk?", isUser = false)
    }

    private fun createImageResponseMessage(images: List<ScannedImage>, criteria: String): ChatMessage {
        setAllFilteredImages(images)
        val template = responseTemplate.random()
        val text = String.format(template, images.size, criteria)

        return ChatMessage(
            text = text,
            isUser = false,
            images = images.take(5),
            showAllImagesButton = images.size > 5
        )
    }

    private fun generateHelpMessage(): String {
        return """
• Cari gambar berdasarkan:
  - Label (contoh: mobil, makanan, hewan)
  - Teks dalam gambar
  - Album
  - Tanggal (tahun, bulan, hari)
  - Lokasi
  - Koleksi

• Cek jumlah media:
  - Ketik: hitung semua media

• Respons umum:
  - halo / hai
  - terima kasih

""".trimIndent()
    }


    private val responseTemplate = listOf(
        "Ketemu %d gambar yang cocok dengan: %s",
        "%d gambar ditemukan dengan kriteria: %s",
        "Berhasil nemu %d gambar sesuai '%s'",
        "Ada %d gambar di galeri yang cocok dengan '%s'",
        "Dapat %d hasil pencarian untuk '%s'",
        "Berikut %d gambar yang sesuai dengan '%s'",
        "Nemu %d gambar pas buat '%s'",
        "Ada %d foto yang sesuai sama '%s'",
        "Ditemukan %d gambar cocok buat '%s'",
        "Total %d gambar cocok dengan: %s"
    )


    fun setAllFilteredImages(images: List<ScannedImage>) {
        _allFilteredImages.value = images
    }

    private fun entityDescription(entities: Map<String, List<String>>): String {
        val descriptions = mutableListOf<String>()
        entities.forEach { (type, values) ->
            when (type) {
                "nama_gambar" -> descriptions.add("nama ${values.joinToString(", ")}")
                "tahun" -> descriptions.add("tahun ${values.joinToString(", ")}")
                "bulan" -> descriptions.add("bulan ${values.joinToString(", ")}")
                "hari" -> descriptions.add("hari ${values.joinToString(", ")}")
                "label" -> descriptions.add("label ${values.joinToString(", ")}")
                "teks" -> descriptions.add("teks '${values.joinToString(", ")}'")
                "format" -> descriptions.add("format ${values.joinToString(", ")}")
                "album" -> descriptions.add("album ${values.joinToString(", ")}")
                "lokasi" -> descriptions.add("lokasi di ${values.joinToString(", ")}")
                "koleksi" -> descriptions.add("koleksi ${values.joinToString(", ")}")
            }
        }
        return descriptions.joinToString(" dan ")
    }

    private suspend fun filterByNER(entities: Map<String, List<String>>): List<ScannedImage> {
        return withContext(Dispatchers.IO) {
            try {
                if (entities.isEmpty()) {
                    return@withContext emptyList()
                }

                var resultImages: Set<ScannedImage>? = null

                for ((type, values) in entities) {
                    if (values.isNotEmpty()) {
                        val currentImagesForThisType = mutableSetOf<ScannedImage>()
                        for (value in values) {
                            when (type) {
                                "nama_gambar" -> currentImagesForThisType.addAll(scannedImageDao!!.getImagesByName(value))
                                "tahun" -> currentImagesForThisType.addAll(scannedImageDao!!.getImagesByYear(value.toInt()))
                                "bulan" -> currentImagesForThisType.addAll(scannedImageDao!!.getImagesByMonth(value))
                                "hari" -> currentImagesForThisType.addAll(scannedImageDao!!.getImagesByDay(value.toInt()))
                                "label" -> currentImagesForThisType.addAll(detectedObjectDao!!.getImagesByLabel(value))
                                "teks" -> currentImagesForThisType.addAll(detectedTextDao!!.searchImagesByContainedText(value))
                                "format" -> currentImagesForThisType.addAll(scannedImageDao!!.getImagesByFormat(value))
                                "album" -> currentImagesForThisType.addAll(scannedImageDao!!.getImagesByAlbum(value))
                                "lokasi" -> currentImagesForThisType.addAll(scannedImageDao!!.getImagesByLocation(value))
                                "koleksi" -> currentImagesForThisType.addAll(scannedImageDao!!.getAllMediaWithTag(value))
                            }
                        }

                        resultImages = resultImages?.intersect(currentImagesForThisType)
                            ?: currentImagesForThisType

                        if (resultImages.isEmpty()) {
                            return@withContext emptyList()
                        }
                    }
                }
                return@withContext resultImages?.toList()?.sortedByDescending { it.tanggal } ?: emptyList()
            } catch (e: Exception) {
                println("❌ Error filtering images: ${e.message}")
                return@withContext emptyList()
            }
        }
    }
    fun resendMessage(query: String) {
        sendMessage(query)
    }
    fun clearMessages() {
        _messages.value = emptyList()
    }
}