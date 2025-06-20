package com.sslythrrr.galeri.data

import androidx.room.Embedded
import androidx.room.Relation
import com.sslythrrr.galeri.data.entity.DetectedObject
import com.sslythrrr.galeri.data.entity.DetectedText
import com.sslythrrr.galeri.data.entity.ScannedImage

data class DataRelations(
    @Embedded val image: ScannedImage,
    @Relation(
        parentColumn = "path",
        entityColumn = "path",
        entity = DetectedObject::class
    )
    val detectedObjects: List<DetectedObject>,
    @Relation(
        parentColumn = "path",
        entityColumn = "path",
        entity = DetectedText::class
    )
    val detectedTexts: List<DetectedText>
)