package com.sslythrrr.galeri.data

import androidx.room.Embedded
import androidx.room.Relation
import com.sslythrrr.galeri.data.entity.DetectedObject
import com.sslythrrr.galeri.data.entity.DetectedText
import com.sslythrrr.galeri.data.entity.ScannedImage

data class DataRelations(
    @Embedded val image: ScannedImage,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
        entity = DetectedObject::class
    )
    val detectedObjects: List<DetectedObject>,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
        entity = DetectedText::class
    )
    val detectedTexts: List<DetectedText>
)