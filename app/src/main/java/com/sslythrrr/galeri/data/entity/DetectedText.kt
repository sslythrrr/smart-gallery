package com.sslythrrr.galeri.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detected_texts")
data class DetectedText(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "path") val imagePath: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "bounding_box") val boundingBox: String?
)