package com.sslythrrr.voe.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.reflect.TypeToken
import com.sslythrrr.voe.ui.media.Media
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.JsonToken

class UriTypeAdapter : TypeAdapter<Uri>() {
    override fun write(out: JsonWriter, value: Uri?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    override fun read(`in`: JsonReader): Uri? {
        return if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            null
        } else {
            Uri.parse(`in`.nextString())
        }
    }
}

class TrashRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: TrashRepository? = null
        private const val TRASH_PREFS = "trash_media_prefs_v4"
        private const val KEY_TRASHED_MAP = "trashed_map_v4"

        fun getInstance(context: Context): TrashRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrashRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .create()

    private val sharedPreferences = context.getSharedPreferences(TRASH_PREFS, Context.MODE_PRIVATE)

    private val _trashedMediaFlow = MutableStateFlow<List<Media>>(emptyList())
    val trashedMediaFlow = _trashedMediaFlow.asStateFlow()

    init {
        _trashedMediaFlow.value = getTrashedMediaList()
    }

    private fun getTrashedMap(): Map<String, String> {
        val json = sharedPreferences.getString(KEY_TRASHED_MAP, "{}")
        Log.d("TrashRepository", "Raw JSON from SharedPrefs: $json")
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            Log.e("TrashRepository", "Error parsing trash map: $e")
            emptyMap()
        }
    }

    private fun getTrashedMediaList(): List<Media> {
        val result = getTrashedMap().values.mapNotNull { json ->
            try {
                gson.fromJson(json, Media::class.java)
            } catch (e: Exception) {
                Log.e("TrashRepository", "Error parsing media JSON: $e")
                null
            }
        }.sortedByDescending { it.dateAdded }
        Log.d("TrashRepository", "getTrashedMediaList: ${result.size} items")
        return result
    }

    fun addToTrash(media: Media) {
        val mediaWithTimestamp = media.copy(dateAdded = System.currentTimeMillis())
        val mediaJson = gson.toJson(mediaWithTimestamp)
        val currentTrash = getTrashedMap().toMutableMap()
        currentTrash[media.uri.toString()] = mediaJson
        saveTrash(currentTrash)
        _trashedMediaFlow.value = getTrashedMediaList()
        // Debug log
        Log.d("TrashRepository", "Added to trash: ${media.title}, Total trashed: ${_trashedMediaFlow.value.size}")
    }

    fun restoreFromTrash(media: Media) {
        val currentTrash = getTrashedMap().toMutableMap()
        currentTrash.remove(media.uri.toString())
        saveTrash(currentTrash)
        _trashedMediaFlow.value = getTrashedMediaList() // Ini sudah benar
    }

    fun getTrashedUrisSet(): Set<String> {
        return getTrashedMap().keys
    }

    private fun saveTrash(map: Map<String, String>) {
        sharedPreferences.edit {
            putString(KEY_TRASHED_MAP, gson.toJson(map))
        }
    }

    fun debugTrashContents() {
        val json = sharedPreferences.getString(KEY_TRASHED_MAP, "{}")
        Log.d("TrashRepository", "Raw SharedPrefs content: $json")

        val map = getTrashedMap()
        Log.d("TrashRepository", "Parsed map size: ${map.size}")
        map.forEach { (key, value) ->
            Log.d("TrashRepository", "Key: $key, Value: $value")
        }
    }
}