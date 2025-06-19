package com.sslythrrr.galeri.data.entity
//v
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_images")
data class ScannedImage(
    @PrimaryKey
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "nama") val nama: String,
    @ColumnInfo(name = "ukuran") val ukuran: Long,
    @ColumnInfo(name = "format") val type: String,
    @ColumnInfo(name = "album") val album: String,
    @ColumnInfo(name = "resolusi") val resolusi: String,
    @ColumnInfo(name = "tanggal") val tanggal: Long,
    @ColumnInfo(name = "year") val year: Int = 0,
    @ColumnInfo(name = "month") val month: Int = 0,
    @ColumnInfo(name = "day") val day: Int = 0,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "location") val location: String? = null,
    @ColumnInfo(name = "location_fetched") val locationFetched: Boolean = false,
    @ColumnInfo(name = "location_retry_count") val locationRetryCount: Int = 0,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_archive") val isArchive: Boolean = false,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)
