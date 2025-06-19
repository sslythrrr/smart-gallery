package com.sslythrrr.galeri.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sslythrrr.galeri.data.entity.DetectedObject

@Dao
interface DetectedObjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(detectedObject: DetectedObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(detectedObject: List<DetectedObject>)

    @Query("SELECT * FROM detected_objects WHERE path = :imagePath")
    fun getObjectsForImage(imagePath: String): List<DetectedObject>

    @Query("SELECT DISTINCT label FROM detected_objects")
    fun getAllDetectedLabels(): List<String>

    @Query("SELECT DISTINCT path FROM detected_objects")
    fun getAllProcessedPaths(): List<String>

    @Query("SELECT * FROM detected_objects WHERE label LIKE '%' || :label || '%'")
    fun getImagesByLabel(label: String): List<DetectedObject>
}

