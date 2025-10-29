package com.sslythrrr.voe.data

import androidx.room.Embedded
import androidx.room.Relation
import com.sslythrrr.voe.data.entity.DeteksiLabel
import com.sslythrrr.voe.data.entity.DeteksiGambar

data class DataRelations(
    @Embedded val image: DeteksiGambar,
    @Relation(
        parentColumn = "uri",
        entityColumn = "uri",
        entity = DeteksiLabel::class
    )
    val deteksiLabels: List<DeteksiLabel>,
)