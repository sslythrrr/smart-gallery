package com.sslythrrr.voe.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sslythrrr.voe.data.dao.DetectedObjectDao
import com.sslythrrr.voe.data.dao.ScanStatusDao
import com.sslythrrr.voe.data.dao.ScannedImageDao
import com.sslythrrr.voe.data.dao.SearchHistoryDao
import com.sslythrrr.voe.data.entity.DeteksiLabel
import com.sslythrrr.voe.data.entity.ScanStatus
import com.sslythrrr.voe.data.entity.DeteksiGambar
import com.sslythrrr.voe.data.entity.SearchHistory

@Database(
    entities = [DeteksiGambar::class, DeteksiLabel::class, ScanStatus::class, SearchHistory::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedImageDao(): ScannedImageDao
    abstract fun detectedObjectDao(): DetectedObjectDao
    abstract fun scanStatusDao(): ScanStatusDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val USE_PREPOPULATED_DB = true// false utk prod

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "gallery.db"
                )

                if (USE_PREPOPULATED_DB) {
                    builder.createFromAsset("voe.db")
                }

                val instance = builder.fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}