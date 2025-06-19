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

    @Query("SELECT * FROM detected_texts WHERE path = :imagePath")
    fun getTextsForImage(imagePath: String): List<DetectedText>

    @Query("SELECT * FROM detected_texts WHERE text LIKE '%' || :query || '%'")
    fun searchTexts(query: String): List<DetectedText>

    @Query("SELECT DISTINCT path FROM detected_texts WHERE text LIKE '%' || :query || '%'")
    fun searchImagesByText(query: String): List<String>

    @Query("DELETE FROM detected_texts WHERE path = :imagePath")
    fun deleteTextsForImage(imagePath: String)

    @Query("SELECT DISTINCT path FROM detected_texts")
    fun getAllProcessedPaths(): List<String>
}