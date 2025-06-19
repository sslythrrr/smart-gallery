package com.sslythrrr.galeri.ui.media

import android.net.Uri

data class Media(
    val id: Long,
    val title: String,
    val uri: Uri,
    val type: MediaType,
    val albumId: Long?,
    val albumName: String?,
    val dateAdded: Long,
    val dateTaken: Long,
    val duration: Long = 0,
    val size: Long = 0,
    val relativePath: String = "",
    val isFavorite: Boolean? = null,
    val isArchive: Boolean? = null,
    val isDeleted: Boolean? = null,
    val locationName: String? = null,
    val formattedSize: String? = null,
    val formattedDate: String? = null
)

enum class MediaType {
    IMAGE, VIDEO
}

data class Album(
    val id: Long,
    val name: String,
    val uri: Uri,
    val mediaCount: Int,
    val type: MediaType = MediaType.IMAGE,
    val latestMediaDate: Long,
)