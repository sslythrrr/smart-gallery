package com.sslythrrr.galeri.ui.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CacheManager {
    suspend fun getCacheSize(context: Context): Long = withContext(Dispatchers.IO) {
        context.cacheDir?.listFiles()?.sumOf { file ->
            if (file.name != "thumbs") {
                getDirectorySize(file)
            } else {
                0L
            }
        } ?: 0L
    }
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        context.cacheDir?.listFiles()?.forEach { file ->
            if (file.name != "thumbs") {
                deleteDirectory(file)
            }
        }
    }
    private fun getDirectorySize(file: File): Long {
        return file.walkTopDown().sumOf { it.length() }
    }
    private fun deleteDirectory(file: File) {
        file.walkBottomUp().forEach {
            it.delete()
        }
    }
}