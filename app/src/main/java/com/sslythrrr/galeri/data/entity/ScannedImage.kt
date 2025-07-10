package com.sslythrrr.galeri.data.entity
//v
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deteksi_gambar")
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
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String? = null,
    @ColumnInfo(name = "file_hash", index = true) val fileHash: String? = null,
    @ColumnInfo(name = "is_trashed", defaultValue = "0") val isTrashed: Boolean = false,
    @ColumnInfo(name = "trashed_timestamp") val trashedTimestamp: Long? = null,
    @ColumnInfo(name = "is_favorite", defaultValue = "0") val isFavorite: Boolean = false,
    @ColumnInfo(name = "collections") val collections: String? = null
)
