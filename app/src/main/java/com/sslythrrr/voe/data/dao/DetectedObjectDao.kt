package com.sslythrrr.voe.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sslythrrr.voe.data.entity.DeteksiLabel
import com.sslythrrr.voe.data.entity.DeteksiGambar

data class LabelCount(val label: String, val count: Int)

@Dao
interface DetectedObjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(deteksiLabel: DeteksiLabel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(deteksiLabel: List<DeteksiLabel>)

    @Query("SELECT * FROM deteksi_label WHERE uri = :uri")
    fun getObjectsForImage(uri: String): List<DeteksiLabel>

    @Query("SELECT DISTINCT label FROM deteksi_label")
    fun getAllDetectedLabels(): List<String>

    @Query("SELECT DISTINCT uri FROM deteksi_label")
    fun getAllProcessedPaths(): List<String>

    @Query("""
        SELECT * FROM deteksi_gambar
            JOIN deteksi_label
            ON deteksi_label.uri = deteksi_gambar.uri
            WHERE label LIKE '%' || :label || '%'
            AND is_deleted = 0""")
    fun getImagesByLabel(label: String): List<DeteksiGambar>

    @Query("""
        SELECT  o.label, COUNT(o.uri) as count FROM deteksi_label o
        Join deteksi_gambar  s ON s.uri = o.uri
        WHERE  o.confidence >= :confidenceThreshold
        AND s.is_deleted = 0
        GROUP BY o.label
        ORDER BY count DESC
    """)
    suspend fun getTopLabels(confidenceThreshold: Float = 0.5f): List<LabelCount>

    @Query("""
        SELECT s.* FROM deteksi_gambar s
        INNER JOIN deteksi_label o ON s.uri = o.uri
        WHERE o.label = :label AND o.confidence >= :confidenceThreshold AND s.is_deleted = 0
        ORDER BY s.tanggal DESC
    """)
    suspend fun getImagesWithLabel(label: String, confidenceThreshold: Float = 0.50f): List<DeteksiGambar>
}