package com.sslythrrr.voe.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "deteksi_label",
    foreignKeys = [ForeignKey(
        entity = DeteksiGambar::class,
        parentColumns = ["uri"],
        childColumns = ["uri"],
        onDelete = ForeignKey.CASCADE
    )])
data class DeteksiLabel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "confidence") val confidence: Float
)
