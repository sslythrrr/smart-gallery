package com.sslythrrr.voe.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sslythrrr.voe.data.entity.ScanStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(scanStatus: ScanStatus): Long

    @Query("SELECT * FROM status_pemindaian WHERE worker_name = :workerName")
    fun getScanStatus(workerName: String): ScanStatus?

    @Query("SELECT * FROM status_pemindaian")
    fun getAllScanStatus(): List<ScanStatus>

    @Query("SELECT * FROM status_pemindaian")
    fun observeAllScanStatus(): Flow<List<ScanStatus>>

    @Query("SELECT status FROM status_pemindaian WHERE worker_name = :workerName LIMIT 1")
    suspend fun getStatusForWorker(workerName: String): String?

    /* @Query("SELECT * FROM status_pemindaian WHERE worker_name = :workerName")
     suspend fun getScanStatus(workerName: String): ScanStatus?*/

    // Tambahkan method baru:
    @Query("SELECT * FROM status_pemindaian WHERE worker_name = :workerName")
    suspend fun getScanStatusSuspend(workerName: String): ScanStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSuspend(scanStatus: ScanStatus): Long

    @Query("UPDATE status_pemindaian SET status = :status WHERE worker_name = :workerName")
    suspend fun updateWorkerStatus(workerName: String, status: String)
}