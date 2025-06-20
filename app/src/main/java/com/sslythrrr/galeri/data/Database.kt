package com.sslythrrr.galeri.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sslythrrr.galeri.data.dao.DetectedObjectDao
import com.sslythrrr.galeri.data.dao.DetectedTextDao
import com.sslythrrr.galeri.data.dao.ScanStatusDao
import com.sslythrrr.galeri.data.dao.ScannedImageDao
import com.sslythrrr.galeri.data.entity.DetectedObject
import com.sslythrrr.galeri.data.entity.DetectedText
import com.sslythrrr.galeri.data.entity.ScanStatus
import com.sslythrrr.galeri.data.entity.ScannedImage

@Database(
    entities = [ScannedImage::class, DetectedObject::class, DetectedText::class, ScanStatus::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedImageDao(): ScannedImageDao
    abstract fun detectedObjectDao(): DetectedObjectDao
    abstract fun detectedTextDao(): DetectedTextDao
    abstract fun scanStatusDao(): ScanStatusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val USE_PREPOPULATED_DB = true // false utk prod

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "gallery.db"
                )

                if (USE_PREPOPULATED_DB) {
                    builder.createFromAsset("gallery.db")
                }

                val instance = builder.fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}