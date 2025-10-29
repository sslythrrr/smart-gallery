package com.sslythrrr.voe.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

class FavoriteRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: FavoriteRepository? = null

        fun getInstance(context: Context): FavoriteRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FavoriteRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("favorite_media", Context.MODE_PRIVATE)

    private val _favoriteMediaIds = MutableStateFlow<Set<Long>>(getFavoriteIds())
    val favoriteMediaIds: StateFlow<Set<Long>> = _favoriteMediaIds.asStateFlow()

    private fun getFavoriteIds(): Set<Long> {
        val favoritesString = sharedPreferences.getString("favorites", "") ?: ""
        return if (favoritesString.isNotEmpty()) {
            favoritesString.split(",").mapNotNull { it.toLongOrNull() }.toSet()
        } else {
            emptySet()
        }
    }

    fun toggleFavorite(mediaId: Long) {
        val currentFavorites = _favoriteMediaIds.value.toMutableSet()
        if (currentFavorites.contains(mediaId)) {
            currentFavorites.remove(mediaId)
        } else {
            currentFavorites.add(mediaId)
        }

        saveFavorites(currentFavorites)
        _favoriteMediaIds.value = currentFavorites
    }

    private fun saveFavorites(favorites: Set<Long>) {
        val favoritesString = favorites.joinToString(",")
        sharedPreferences.edit {
            putString("favorites", favoritesString)
        }
    }

    fun getFavoriteMediaIds(): Set<Long> {
        return _favoriteMediaIds.value
    }
}