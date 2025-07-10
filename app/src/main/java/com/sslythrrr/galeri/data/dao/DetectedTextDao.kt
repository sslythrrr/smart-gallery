package com.sslythrrr.galeri.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sslythrrr.galeri.data.entity.DetectedText

@Dao
interface DetectedTextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(detectedText: DetectedText): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(detectedTexts: List<DetectedText>)

    @Query("SELECT * FROM deteksi_teks WHERE uri = :uri")
    fun getTextsForImage(uri: String): List<DetectedText>

    @Query("SELECT * FROM deteksi_teks WHERE text LIKE '%' || :query || '%'")
    fun searchTexts(query: String): List<DetectedText>

    @Query("SELECT DISTINCT deteksi_gambar.path FROM deteksi_gambar " +
            "JOIN deteksi_teks ON deteksi_gambar.uri = deteksi_teks.uri " +
            "WHERE deteksi_teks.text LIKE '%' || :query || '%'")
    fun searchImagesByText(query: String): List<String>

    @Query("DELETE FROM deteksi_teks WHERE uri = :uri")
    fun deleteTextsForImage(uri: String)

    @Query("SELECT DISTINCT uri FROM deteksi_teks")
    fun getAllProcessedPaths(): List<String>
}