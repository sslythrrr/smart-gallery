package com.sslythrrr.galeri.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_status")
data class ScanStatus(
    @PrimaryKey @ColumnInfo(name = "worker_name") val workerName: String,
    @ColumnInfo(name = "total_items") val totalItems: Int = 0,
    @ColumnInfo(name = "processed_items") val processedItems: Int = 0,
    @ColumnInfo(name = "status") val status: String = "PENDING", // PENDING, RUNNING, COMPLETED, FAILED
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)