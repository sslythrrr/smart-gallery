package com.sslythrrr.galeri.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sslythrrr.galeri.data.entity.DetectedObject
import com.sslythrrr.galeri.data.entity.ScannedImage

data class LabelCount(val label: String, val count: Int)

@Dao
interface DetectedObjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(detectedObject: DetectedObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(detectedObject: List<DetectedObject>)

    @Query("SELECT * FROM deteksi_label WHERE uri = :uri")
    fun getObjectsForImage(uri: String): List<DetectedObject>

    @Query("SELECT DISTINCT label FROM deteksi_label")
    fun getAllDetectedLabels(): List<String>

    @Query("SELECT DISTINCT uri FROM deteksi_label")
    fun getAllProcessedPaths(): List<String>

    @Query("SELECT * FROM deteksi_gambar " +
            "JOIN deteksi_label " +
            "ON deteksi_label.uri = deteksi_gambar.uri " +
            "WHERE label LIKE '%' || :label || '%'")
    fun getImagesByLabel(label: String): List<ScannedImage>

    @Query("""
        SELECT label, COUNT(uri) as count FROM deteksi_label
        WHERE confidence >= :confidenceThreshold
        GROUP BY label
        ORDER BY count DESC
    """)
    suspend fun getTopLabels(confidenceThreshold: Float = 0.4f): List<LabelCount>

    @Query("""
        SELECT s.* FROM deteksi_gambar s
        INNER JOIN deteksi_label o ON s.uri = o.uri
        WHERE o.label = :label AND o.confidence >= :confidenceThreshold
        ORDER BY s.tanggal DESC
    """)
    suspend fun getImagesWithLabel(label: String, confidenceThreshold: Float = 0.4f): List<ScannedImage>
}