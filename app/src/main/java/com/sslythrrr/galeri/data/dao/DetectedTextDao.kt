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

    @Query("SELECT * FROM detected_texts WHERE uri = :uri")
    fun getTextsForImage(uri: String): List<DetectedText>

    @Query("SELECT * FROM detected_texts WHERE text LIKE '%' || :query || '%'")
    fun searchTexts(query: String): List<DetectedText>

    @Query("SELECT DISTINCT scanned_images.path FROM scanned_images " +
            "JOIN detected_texts ON scanned_images.uri = detected_texts.uri " +
            "WHERE detected_texts.text LIKE '%' || :query || '%'")
    fun searchImagesByText(query: String): List<String>

    @Query("DELETE FROM detected_texts WHERE uri = :uri")
    fun deleteTextsForImage(uri: String)

    @Query("SELECT DISTINCT uri FROM detected_texts")
    fun getAllProcessedPaths(): List<String>
}