package com.sslythrrr.galeri.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detected_objects")
data class DetectedObject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "path") val imagePath: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "confidence") val confidence: Float
)
