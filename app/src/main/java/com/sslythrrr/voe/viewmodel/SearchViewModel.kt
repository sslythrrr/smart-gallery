package com.sslythrrr.voe.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sslythrrr.voe.data.AppDatabase
import com.sslythrrr.voe.data.dao.DetectedObjectDao
import com.sslythrrr.voe.data.dao.ScannedImageDao
import com.sslythrrr.voe.data.dao.SearchHistoryDao
import com.sslythrrr.voe.data.entity.DeteksiGambar
import com.sslythrrr.voe.data.entity.SearchHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sslythrrr.voe.BuildConfig
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val images: List<DeteksiGambar> = emptyList(),
    val showAllImagesButton: Boolean = false
)

class SearchViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    private val _allFilteredImages = MutableStateFlow<List<DeteksiGambar>>(emptyList())
    val allFilteredImages: StateFlow<List<DeteksiGambar>> = _allFilteredImages.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val geminiAPIkey = BuildConfig.GEMINI_API_KEY
    private val geminiAPIURL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiAPIkey"


    private var detectedObjectDao: DetectedObjectDao? = null
    private var scannedImageDao: ScannedImageDao? = null
    private var searchHistoryDao: SearchHistoryDao? = null

    private val geminiPrompt = """
Kamu adalah asisten pencarian cerdas untuk aplikasi galeri foto bernama 'Vue of Eden'.

Tugasmu:

1. Memahami maksud pengguna dan menentukan INTENT dari query.

2. Jika intent adalah pencarian foto → hasilkan output berupa JSON sesuai format.

3. Jika intent adalah ambiguous (kata umum, deskripsi kreatif, atau tidak match ke label) → berikan respon interaktif untuk meminta klarifikasi atau menawarkan alternatif.

4. Jika intent adalah salam, ucapan terima kasih, permintaan bantuan, atau ngobrol ringan → jawab secara natural dan singkat tanpa JSON.

---

### A. Aturan Mode JSON

Jika query termasuk pencarian jelas (memiliki kata yang bisa dipetakan ke salah satu 'Fields' atau label):

- Output HARUS berupa JSON murni, tanpa teks tambahan.

- Jika sebuah field tidak disebutkan dalam query, jangan masukkan ke JSON.

- Gunakan pengetahuan umum untuk sinonim atau konsep.

- Kunci JSON harus sama persis dengan 'Fields'. Nilai adalah array of string.

Fields yang bisa dicari di database:

- nama: Nama file gambar.

- tahun: Tahun pengambilan gambar (angka).

- bulan: Nama bulan dalam Bahasa Indonesia (contoh: "januari").

- hari: Hari dalam bulan (angka).

- label: Objek yang ada di dalam gambar.

- lokasi: Lokasi geografis pengambilan gambar.

- album: Nama album folder.

- koleksi: Nama koleksi virtual buatan pengguna.

Contoh:

- "foto mobil di bandung tahun 2023" → {"label": ["mobil"], "lokasi": ["bandung"], "tahun": ["2023"]}

- "pemandangan di bali" → {"label": ["pantai", "pegunungan", "taman", "alam"], "lokasi": ["bali"]}

- "momen makan-makan pas agustus kemarin" → {"label": ["makanan"], "bulan": ["agustus"]}

---

### B. Mode Klarifikasi & Alternatif

Jika query ambigu:

- Cek apakah kata tersebut bisa jadi album atau koleksi di database (jika info ada).

- Jika tidak ditemukan di label:

  - Tawarkan kategori serupa.

  - Ajukan pertanyaan klarifikasi untuk mempersempit pencarian.

- Jika deskripsi kreatif (contoh: "sesuatu yang bergerak dan terbuat dari besi"):

  - Gunakan pengetahuan umum untuk memetakan ke label yang mungkin.

  - Jika ada lebih dari 1 kemungkinan → tawarkan pilihan ke pengguna.

Format respon untuk mode ini: Jawaban natural (tidak dalam JSON).

---

### C. Mode Non-Pencarian

Jika intent adalah:

- Salam → Balas salam singkat + tawarkan bantuan pencarian.

- Ucapan terima kasih → Balas ramah singkat.

- Bantuan penggunaan → Jelaskan cara pakai aplikasi sesuai konteks.

- Small talk → Balas singkat, nyambung, tapi tidak berlarut-larut.

---

### D. Deteksi Intent (Prioritas)

1. Bantuan (kata: "cara", "gimana", "tolong", "bantuan").

2. Ucapan terima kasih (kata: "terima kasih", "makasih", "thanks").

3. Salam (kata: "halo", "hai", "selamat pagi/siang/malam").

4. Small talk (kata: "apa kabar", "lagi apa", "ngapain").

5. Pencarian jelas (label match).

6. Ambigu (tidak match tapi masih bisa diinterpretasi).

---

### E. Daftar Label yang tersedia:

  "alat_elektronik",
  "aplikasi",
  "barcode",
  "bekerja",
  "belajar",
  "berenang",
  "berkendara",
  "bermain",
  "bermain_game",
  "berpose",
  "berwarna",
  "blur",
  "buku",
  "chat",
  "dapur",
  "dokumen",
  "foto_keluarga",
  "gelap",
  "hewan",
  "ilustrasi",
  "jalan",
  "kantor",
  "kelas",
  "mainan",
  "makan",
  "makanan",
  "mal",
  "meja_kursi",
  "meme",
  "mobil",
  "monokrom",
  "motor",
  "ocr_worthy",
  "olahraga",
  "pantai",
  "pegunungan",
  "rumah",
  "screenshot",
  "selfie",
  "sepeda",
  "taman",
  "tanaman",
  "tempat_ibadah",
  "terang",
  "tidur"

---

Sekarang, proses query pengguna sesuai aturan di atas dan dengan penggunaan kalimat yang rapih (jangan gunakan elemen khusus).
"""

    private var connectivityManager: ConnectivityManager? = null

    private fun isNetworkAvailable(): Boolean {
        return try {
            connectivityManager?.let { cm ->
                val network = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(network) ?: return false
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } == true
        } catch (_: Exception) {
            false
        }
    }

    init {
        val welcomeMessage = ChatMessage(
            text = "Hai, mau cari gambar apa hari ini?",
            isUser = false
        )
        _messages.value = listOf(welcomeMessage)
    }

    fun initializeProcessors(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getInstance(context)
                detectedObjectDao = database.detectedObjectDao()
                scannedImageDao = database.scannedImageDao()
                searchHistoryDao = database.searchHistoryDao()
            } catch (e: Exception) {
                e.printStackTrace()
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
                if (message.contains("hitung", ignoreCase = true) &&
                    (message.contains("media", ignoreCase = true) || message.contains("gambar", ignoreCase = true) || message.contains("foto", ignoreCase = true))) {
                    handleCountMedia()
                } else {
                    val geminiResponse = callGeminiAPI(message)
                    processGeminiResponse(geminiResponse, message)
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("limit", ignoreCase = true) == true ||
                            e.message?.contains("quota", ignoreCase = true) == true ||
                            e.message?.contains("rate", ignoreCase = true) == true -> {
                        "Maaf, sedang kena limit API Gemini. Coba lagi dalam beberapa menit"
                    }
                    e.message?.contains("network", ignoreCase = true) == true ||
                            e.message?.contains("connection", ignoreCase = true) == true ||
                            e.message?.contains("hostname", ignoreCase = true) == true ||
                            e.message?.contains("resolve", ignoreCase = true) == true -> {
                        "Koneksi internet bermasalah"
                    }
                    e.message?.contains("Tidak ada koneksi") == true -> {
                        "Tidak ada koneksi internet"
                    }
                    else -> {
                        "Waduh, ada error: ${e.message}"
                    }
                }
                val botMessage = ChatMessage(errorMessage, isUser = false)
                _messages.value = _messages.value + botMessage
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun callGeminiAPI(query: String): String {
        return withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable()) {
                    throw Exception("Tidak ada koneksi internet")
                }

                val url = URL(geminiAPIURL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "$geminiPrompt\n\nQuery pengguna: \"$query\"")
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.3)
                        put("topK", 40)
                        put("topP", 0.95)
                        put("maxOutputTokens", 1024)
                    })
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).getString("text")
                        }
                    }
                } else {
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        val errorResponse = errorStream.bufferedReader().readText()
                        throw Exception("API Error ($responseCode): $errorResponse")
                    } else {
                        throw Exception("HTTP Error: $responseCode")
                    }
                }
                throw Exception("Tidak ada respon dari Gemini")
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private suspend fun processGeminiResponse(response: String, originalQuery: String) {
        try {
            val jsonRegex = """\{.*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matchResult = jsonRegex.find(response)
            val jsonString = matchResult?.value

            if (jsonString != null) {
                val jsonObject = JSONObject(jsonString)
                val searchCriteria = parseSearchCriteria(jsonObject)
                val filteredImages = filterImagesByCriteria(searchCriteria)

                if (filteredImages.isNotEmpty()) {
                    searchHistoryDao?.insert(SearchHistory(query = originalQuery))
                    setAllFilteredImages(filteredImages)

                    val criteriaDesc = createCriteriaDescription(searchCriteria)
                    val template = responseTemplate.random()
                    val resultText = String.format(template, filteredImages.size, criteriaDesc)

                    val botMessage = ChatMessage(
                        text = resultText,
                        isUser = false,
                        images = filteredImages.take(5),
                        showAllImagesButton = filteredImages.size > 5
                    )
                    _messages.value = _messages.value + botMessage
                } else {
                    val botMessage = ChatMessage("Tidak ditemukan gambar yang cocok dengan pencarian '$originalQuery'", isUser = false)
                    _messages.value = _messages.value + botMessage
                }
            } else {
                val botMessage = ChatMessage(response.trim(), isUser = false)
                _messages.value = _messages.value + botMessage
            }
        } catch (_: Exception) {
            val botMessage = ChatMessage("Maaf, ada kesalahan dalam memproses respon", isUser = false)
            _messages.value = _messages.value + botMessage
        }
    }

    private suspend fun handleCountMedia() {
        try {
            val total = scannedImageDao?.countAllMedia() ?: 0
            val photos = scannedImageDao?.countAllImages() ?: 0
            val videos = scannedImageDao?.countAllVideos() ?: 0

            val responseText = "Tentu! Saat ini ada total $total media di Vue of Eden-mu, terdiri dari $photos foto dan $videos video! ✨"
            val botMessage = ChatMessage(responseText, isUser = false)
            _messages.value = _messages.value + botMessage
        } catch (_: Exception) {
            val botMessage = ChatMessage("Gagal menghitung media", isUser = false)
            _messages.value = _messages.value + botMessage
        }
    }

    private fun parseSearchCriteria(jsonObject: JSONObject): Map<String, List<String>> {
        val criteria = mutableMapOf<String, List<String>>()

        val validFields = listOf("nama", "tahun", "bulan", "hari", "label", "lokasi", "album", "koleksi")

        for (field in validFields) {
            if (jsonObject.has(field)) {
                val jsonArray = jsonObject.getJSONArray(field)
                val values = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    values.add(jsonArray.getString(i))
                }
                if (values.isNotEmpty()) {
                    criteria[field] = values
                }
            }
        }

        return criteria
    }

    private fun createCriteriaDescription(criteria: Map<String, List<String>>): String {
        val descriptions = mutableListOf<String>()
        criteria.forEach { (type, values) ->
            when (type) {
                "nama" -> descriptions.add("nama ${values.joinToString(", ")}")
                "tahun" -> descriptions.add("tahun ${values.joinToString(", ")}")
                "bulan" -> descriptions.add("bulan ${values.joinToString(", ")}")
                "hari" -> descriptions.add("hari ${values.joinToString(", ")}")
                "label" -> descriptions.add("label ${values.joinToString(", ")}")
                "lokasi" -> descriptions.add("lokasi ${values.joinToString(", ")}")
                "album" -> descriptions.add("album ${values.joinToString(", ")}")
                "koleksi" -> descriptions.add("koleksi ${values.joinToString(", ")}")
            }
        }
        return descriptions.joinToString(" dan ")
    }

    private suspend fun filterImagesByCriteria(criteria: Map<String, List<String>>): List<DeteksiGambar> {
        return withContext(Dispatchers.IO) {
            try {
                if (criteria.isEmpty()) {
                    return@withContext emptyList()
                }

                var resultImages: Set<DeteksiGambar>? = null

                for ((type, values) in criteria) {
                    if (values.isNotEmpty()) {
                        val currentImagesForThisType = mutableSetOf<DeteksiGambar>()
                        for (value in values) {
                            when (type) {
                                "nama" -> currentImagesForThisType.addAll(scannedImageDao?.getImagesByName(value) ?: emptyList())
                                "tahun" -> {
                                    val year = value.toIntOrNull()
                                    if (year != null) {
                                        currentImagesForThisType.addAll(scannedImageDao?.getImagesByYear(year) ?: emptyList())
                                    }
                                }
                                "bulan" -> currentImagesForThisType.addAll(scannedImageDao?.getImagesByMonth(value) ?: emptyList())
                                "hari" -> {
                                    val day = value.toIntOrNull()
                                    if (day != null) {
                                        currentImagesForThisType.addAll(scannedImageDao?.getImagesByDay(day) ?: emptyList())
                                    }
                                }
                                "label" -> currentImagesForThisType.addAll(detectedObjectDao?.getImagesByLabel(value) ?: emptyList())
                                "lokasi" -> currentImagesForThisType.addAll(scannedImageDao?.getImagesByLocation(value) ?: emptyList())
                                "album" -> currentImagesForThisType.addAll(scannedImageDao?.getImagesByAlbum(value) ?: emptyList())
                                "koleksi" -> currentImagesForThisType.addAll(scannedImageDao?.getAllMediaWithTag(value) ?: emptyList())
                            }
                        }

                        resultImages = resultImages?.intersect(currentImagesForThisType)
                            ?: currentImagesForThisType

                        if (resultImages.isEmpty() == true) {
                            return@withContext emptyList()
                        }
                    }
                }
                return@withContext resultImages?.toList()?.sortedByDescending { it.tanggal } ?: emptyList()
            } catch (_: Exception) {
                return@withContext emptyList()
            }
        }
    }

    private val responseTemplate = listOf(
        "Ketemu %d gambar yang cocok dengan: %s",
        "%d gambar ditemukan dengan kriteria: %s",
        "Berhasil menemukan %d gambar sesuai '%s'",
        "Ada %d gambar di galeri yang cocok dengan '%s'",
        "Dapat %d hasil pencarian untuk '%s'",
        "Berikut %d gambar yang sesuai dengan '%s'",
        "Menemukan %d gambar pas buat '%s'",
        "Ada %d foto yang sesuai sama '%s'",
        "Ditemukan %d gambar cocok buat '%s'",
        "Total %d gambar cocok dengan: %s"
    )

    fun setAllFilteredImages(images: List<DeteksiGambar>) {
        _allFilteredImages.value = images
    }

    fun resendMessage(query: String) {
        sendMessage(query)
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}