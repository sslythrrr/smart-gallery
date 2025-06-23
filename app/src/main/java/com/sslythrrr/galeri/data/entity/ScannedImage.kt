package com.sslythrrr.galeri.data.entity
//v
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_images")
data class ScannedImage(
    @PrimaryKey
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "nama") val nama: String,
    @ColumnInfo(name = "ukuran") val ukuran: Long,
    @ColumnInfo(name = "format") val type: String,
    @ColumnInfo(name = "album") val album: String,
    @ColumnInfo(name = "resolusi") val resolusi: String,
    @ColumnInfo(name = "tanggal") val tanggal: Long,
    @ColumnInfo(name = "tahun") val tahun: Int = 0,
    @ColumnInfo(name = "bulan") val bulan: String = "",
    @ColumnInfo(name = "hari") val hari: Int = 0,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "lokasi") val lokasi: String? = null,
    @ColumnInfo(name = "fetch_lokasi") val fetchLokasi: Boolean = false,
    @ColumnInfo(name = "retry_lokasi") val retryLokasi: Int = 0,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "is_archive") val isArchive: Boolean = false,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)
