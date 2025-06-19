package com.sslythrrr.galeri.data.dao
//v
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sslythrrr.galeri.data.entity.ScannedImage
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedImageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(image: ScannedImage)

    @Query("SELECT * FROM scanned_images ORDER BY tanggal DESC")
    fun getAllScannedImages(): List<ScannedImage>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(images: List<ScannedImage>): List<Long>

    @Query("SELECT uri FROM scanned_images")
    fun getAllScannedUris(): List<String>

    @Query(
        "SELECT * FROM scanned_images WHERE nama LIKE '%' || :query || '%' OR " +
                "uri LIKE '%' || :query || '%' OR " +
                "album LIKE '%' || :query || '%' OR " +
                "tanggal LIKE '%' || :query || '%'"
    )
    fun searchImages(query: String): Flow<List<ScannedImage>>

    @Query("SELECT * FROM scanned_images WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND location_fetched = 0 LIMIT :limit")
    suspend fun getImagesNeedingLocation(limit: Int): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND (location IS NULL OR location = '') AND location_retry_count < 3")
    suspend fun getImagesForLocationRetry(): List<ScannedImage>

    @Query("UPDATE scanned_images SET location = :location, location_fetched = 1 WHERE uri = :uri")
    suspend fun updateImageLocation(uri: String, location: String)

    @Query("UPDATE scanned_images SET location_fetched = 1, location_retry_count = location_retry_count + 1 WHERE uri = :uri")
    suspend fun markLocationFetchFailed(uri: String)

    @Query("UPDATE scanned_images SET location_fetched = 0 WHERE uri IN (:uris)")
    suspend fun resetLocationFetchStatus(uris: List<String>)

    @Query("SELECT * FROM scanned_images WHERE is_deleted = 0 ORDER BY tanggal DESC")
    fun getAllActiveImages(): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE is_deleted = 0 AND is_favorite = 1 ORDER BY tanggal DESC")
    fun getFavoriteImages(): Flow<List<ScannedImage>>

    @Query("SELECT * FROM scanned_images WHERE is_deleted = 0 AND is_archive = 1 ORDER BY tanggal DESC")
    fun getArchivedImages(): Flow<List<ScannedImage>>

    @Query("SELECT * FROM scanned_images WHERE is_deleted = 1 AND deleted_at > :thirtyDaysAgo ORDER BY deleted_at DESC")
    fun getRecentlyDeleted(thirtyDaysAgo: Long): Flow<List<ScannedImage>>

    @Query("UPDATE scanned_images SET is_favorite = :isFavorite WHERE uri = :uri")
    suspend fun updateFavoriteStatus(uri: String, isFavorite: Boolean)

    @Query("UPDATE scanned_images SET is_archive = :isArchive WHERE uri = :uri")
    suspend fun updateArchiveStatus(uri: String, isArchive: Boolean)

    @Query("UPDATE scanned_images SET is_deleted = :isDeleted, deleted_at = :deletedAt WHERE uri = :uri")
    suspend fun updateDeleteStatus(uri: String, isDeleted: Boolean, deletedAt: Long?)

    @Query("DELETE FROM scanned_images WHERE is_deleted = 1 AND deleted_at < :thirtyDaysAgo")
    suspend fun permanentlyDeleteOldItems(thirtyDaysAgo: Long)
}
