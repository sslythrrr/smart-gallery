package com.sslythrrr.galeri.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sslythrrr.galeri.data.entity.ScanStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(scanStatus: ScanStatus): Long

    @Query("SELECT * FROM scan_status WHERE worker_name = :workerName")
    fun getScanStatus(workerName: String): ScanStatus?

    @Query("SELECT * FROM scan_status")
    fun getAllScanStatus(): List<ScanStatus>

    @Query("SELECT * FROM scan_status")
    fun observeAllScanStatus(): Flow<List<ScanStatus>>

    /* @Query("SELECT * FROM scan_status WHERE worker_name = :workerName")
     suspend fun getScanStatus(workerName: String): ScanStatus?*/

    // Tambahkan method baru:
    @Query("SELECT * FROM scan_status WHERE worker_name = :workerName")
    suspend fun getScanStatusSuspend(workerName: String): ScanStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSuspend(scanStatus: ScanStatus): Long

    @Query("UPDATE scan_status SET status = :status WHERE worker_name = :workerName")
    suspend fun updateWorkerStatus(workerName: String, status: String)
}