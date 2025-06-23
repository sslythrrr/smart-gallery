package com.sslythrrr.galeri.data.dao
//v
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sslythrrr.galeri.data.DataRelations
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

    @Query("SELECT * FROM scanned_images WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND fetch_lokasi = 0 LIMIT :limit")
    suspend fun getImagesNeedingLocation(limit: Int): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND (lokasi IS NULL OR lokasi = '') AND retry_lokasi < 3")
    suspend fun getImagesForLocationRetry(): List<ScannedImage>

    @Query("UPDATE scanned_images SET lokasi = :location, fetch_lokasi = 1 WHERE uri = :uri")
    suspend fun updateImageLocation(uri: String, location: String)

    @Query("UPDATE scanned_images SET fetch_lokasi = 1, retry_lokasi = retry_lokasi + 1 WHERE uri = :uri")
    suspend fun markLocationFetchFailed(uri: String)

    @Query("UPDATE scanned_images SET fetch_lokasi = 0 WHERE uri IN (:uris)")
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

    @Query("SELECT * FROM scanned_images WHERE album LIKE '%' || :album || '%' AND is_deleted = 0")
    fun getImagesByAlbum(album: String): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE nama LIKE '%' || :name || '%' AND is_deleted = 0")
    fun getImagesByName(name: String): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE format LIKE '%' || :format || '%' AND is_deleted = 0")
    fun getImagesByFormat(format: String): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE lokasi LIKE '%' || :location || '%' AND is_deleted = 0")
    fun getImagesByLocation(location: String): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE (tahun = :year) AND is_deleted = 0")
    fun getImagesByYear(year: Int): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE bulan LIKE '%' || :month || '%' AND is_deleted = 0")
    fun getImagesByMonth(month: String): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE (hari = :day) AND is_deleted = 0")
    fun getImagesByDay(day: Int): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE tanggal BETWEEN :startDate AND :endDate AND is_deleted = 0")
    fun getImagesByDateRange(startDate: Long, endDate: Long): List<ScannedImage>

    @Transaction
    @Query("SELECT * FROM scanned_images WHERE uri = :path")
    fun getIndexedImageWithDetails(path: String): DataRelations

    @Transaction
    @Query("SELECT * FROM scanned_images")
    fun getAllIndexedImagesWithDetails(): List<DataRelations>

    @Query("SELECT * FROM scanned_images WHERE path LIKE '%' || :path || '%' AND is_deleted = 0")
    fun getImagesByPath(path: String): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE ukuran >= :minSize AND ukuran <= :maxSize AND is_deleted = 0")
    fun getImagesBySize(minSize: Long, maxSize: Long): List<ScannedImage>

    @Query("SELECT * FROM scanned_images WHERE resolusi LIKE '%' || :resolution || '%' AND is_deleted = 0")
    fun getImagesByResolution(resolution: String): List<ScannedImage>
}
