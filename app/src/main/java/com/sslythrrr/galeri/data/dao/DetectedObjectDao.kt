package com.sslythrrr.galeri.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sslythrrr.galeri.data.entity.DetectedObject
import com.sslythrrr.galeri.data.entity.ScannedImage

@Dao
interface DetectedObjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(detectedObject: DetectedObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(detectedObject: List<DetectedObject>)

    @Query("SELECT * FROM detected_objects WHERE uri = :uri")
    fun getObjectsForImage(uri: String): List<DetectedObject>

    @Query("SELECT DISTINCT label FROM detected_objects")
    fun getAllDetectedLabels(): List<String>

    @Query("SELECT DISTINCT uri FROM detected_objects")
    fun getAllProcessedPaths(): List<String>

    @Query("SELECT * FROM scanned_images " +
            "JOIN detected_objects " +
            "ON detected_objects.uri = scanned_images.uri " +
            "WHERE label LIKE '%' || :label || '%'")
    fun getImagesByLabel(label: String): List<ScannedImage>
}

