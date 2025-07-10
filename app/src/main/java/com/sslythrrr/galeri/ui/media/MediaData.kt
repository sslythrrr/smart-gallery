package com.sslythrrr.galeri.ui.media

import android.net.Uri
import androidx.compose.runtime.Stable

@Stable
data class Media(
    val id: Long,
    val title: String,
    val uri: Uri,
    val type: MediaType,
    val albumId: Long?,
    val albumName: String?,
    val dateAdded: Long,
    val dateTaken: Long,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Long = 0,
    val size: Long = 0,
    val relativePath: String = "",
    val isFavorite: Boolean = false,
    val isArchive: Boolean? = null,
    val isDeleted: Boolean? = null,
    val locationName: String? = null,
    val formattedSize: String? = null,
    val formattedDate: String? = null,
    val thumbnailPath: String? = null,
    val fileHash: String? = null
)

enum class MediaType {
    IMAGE, VIDEO
}

@Stable
data class Album(
    val id: Long,
    val name: String,
    val uri: Uri,
    val mediaCount: Int,
    val type: MediaType = MediaType.IMAGE,
    val latestMediaDate: Long,
)