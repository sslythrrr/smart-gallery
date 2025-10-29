package com.sslythrrr.voe.data.dao
//v
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sslythrrr.voe.data.DataRelations
import com.sslythrrr.voe.data.entity.DeteksiGambar
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedImageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(image: DeteksiGambar)

    @Query("SELECT * FROM deteksi_gambar where is_deleted = 0 ORDER BY tanggal DESC")
    fun getAllScannedImages(): List<DeteksiGambar>

    @Query("SELECT uri FROM deteksi_gambar where is_deleted = 0")
    fun getAllImageUris(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(images: List<DeteksiGambar>): List<Long>

    @Query("SELECT uri FROM deteksi_gambar")
    fun getAllScannedUris(): List<String>

    @Query("SELECT * FROM deteksi_gambar WHERE album LIKE :albumName AND is_deleted = 0 ORDER BY tanggal DESC")
    fun getMediaPagingSourceByAlbum(albumName: String): PagingSource<Int, DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE is_deleted = 0 ORDER BY tanggal DESC")
    fun getAllMediaPagingSource(): PagingSource<Int, DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE is_deleted = 1 ORDER BY delete_timestamp DESC")
    fun getTrashedMediaPagingSource(): PagingSource<Int, DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE is_favorite = 1 AND is_deleted = 0 ORDER BY tanggal DESC")
    fun getFavoriteMediaPagingSource(): PagingSource<Int, DeteksiGambar>

    @Query("UPDATE deteksi_gambar SET is_favorite = :isFavorite WHERE uri = :uri")
    suspend fun updateFavoriteStatus(uri: String, isFavorite: Boolean)

    @Query("SELECT * FROM deteksi_gambar WHERE is_favorite = 1 AND is_deleted = 0")
    fun getAllFavorites(): List<DeteksiGambar>

    @Query("DELETE FROM deteksi_gambar WHERE uri IN (:uris)")
    suspend fun deletePermanentlyByUri(uris: List<String>)

    @Query("SELECT * FROM deteksi_gambar WHERE is_deleted = 0")
    fun getAllNonTrashedMedia(): List<DeteksiGambar>

    @Query("UPDATE deteksi_gambar SET is_deleted = :isTrashed, delete_timestamp = :timestamp WHERE uri IN (:uris)")
    suspend fun updateTrashedStatus(uris: List<String>, isTrashed: Boolean, timestamp: Long?)

    @Query("DELETE FROM deteksi_gambar WHERE is_deleted = 1 AND delete_timestamp <= :cutoffTimestamp")
    suspend fun deleteOldTrashedMedia(cutoffTimestamp: Long)

    @Query("SELECT * FROM deteksi_gambar WHERE format LIKE 'video/%' AND is_deleted = 0 ORDER BY tanggal DESC")
    fun getVideoPagingSource(): PagingSource<Int, DeteksiGambar>

    @Query("SELECT COUNT(uri) FROM deteksi_gambar")
    suspend fun countImages(): Int

    @Query(
        "SELECT * FROM deteksi_gambar WHERE nama LIKE '%' || :query || '%' OR " +
                "uri LIKE '%' || :query || '%' OR " +
                "album LIKE '%' || :query || '%' OR " +
                "tanggal LIKE '%' || :query || '%'"
    )
    fun searchImages(query: String): Flow<List<DeteksiGambar>>

    @Query("SELECT * FROM deteksi_gambar WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND fetch_lokasi = 0 AND is_deleted = 0 LIMIT :limit")
    suspend fun getImagesNeedingLocation(limit: Int): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND (lokasi IS NULL OR lokasi = '') AND retry_lokasi < 3")
    suspend fun getImagesForLocationRetry(): List<DeteksiGambar>

    @Query("UPDATE deteksi_gambar SET lokasi = :location, fetch_lokasi = 1 WHERE uri = :uri")
    suspend fun updateImageLocation(uri: String, location: String)

    @Query("UPDATE deteksi_gambar SET fetch_lokasi = 1, retry_lokasi = retry_lokasi + 1 WHERE uri = :uri")
    suspend fun markLocationFetchFailed(uri: String)

    @Query("UPDATE deteksi_gambar SET fetch_lokasi = 0 WHERE uri IN (:uris)")
    suspend fun resetLocationFetchStatus(uris: List<String>)

    @Query("SELECT * FROM deteksi_gambar Where is_deleted = 0  ORDER BY tanggal DESC")
    fun getAllActiveImages(): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE album LIKE '%' || :album || '%' AND is_deleted = 0")
    fun getImagesByAlbum(album: String): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE nama LIKE '%' || :name || '%' AND is_deleted = 0")
    fun getImagesByName(name: String): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE format LIKE '%' || :format || '%' AND is_deleted = 0 AND format LIKE 'image/%'")
    fun getImagesByFormat(format: String): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE lokasi LIKE '%' || :location || '%' AND is_deleted = 0")
    fun getImagesByLocation(location: String): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE (tahun = :year) AND is_deleted = 0")
    fun getImagesByYear(year: Int): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE bulan LIKE '%' || :month || '%' AND is_deleted = 0")
    fun getImagesByMonth(month: String): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE (hari = :day) AND is_deleted = 0")
    fun getImagesByDay(day: Int): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE tanggal BETWEEN :startDate AND :endDate AND is_deleted = 0")
    fun getImagesByDateRange(startDate: Long, endDate: Long): List<DeteksiGambar>

    @Transaction
    @Query("SELECT * FROM deteksi_gambar WHERE uri = :path AND is_deleted = 0")
    fun getIndexedImageWithDetails(path: String): DataRelations

    @Query("""
    SELECT * FROM deteksi_gambar
    WHERE file_hash IN (
        SELECT file_hash FROM deteksi_gambar
        WHERE file_hash IS NOT NULL
        GROUP BY file_hash
        HAVING COUNT(uri) > 1
    ) AND is_deleted = 0
    ORDER BY file_hash, tanggal DESC
""")
    fun getDuplicateMedia(): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE koleksi LIKE '%' || :collectionTag || '%'")
    suspend fun getAllMediaWithTag(collectionTag: String): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE uri = :uri LIMIT 1")
    suspend fun getMediaByUri(uri: String): DeteksiGambar?

    @Query("UPDATE deteksi_gambar SET koleksi = :collections WHERE uri = :uri")
    suspend fun updateCollections(uri: String, collections: String?)

    @Query("SELECT DISTINCT koleksi FROM deteksi_gambar WHERE koleksi IS NOT NULL AND koleksi != ''")
    suspend fun getAllCollectionTags(): List<String>

    @Query("SELECT * FROM deteksi_gambar WHERE koleksi LIKE '%' || :collectionTag || '%' AND is_deleted = 0 ORDER BY tanggal DESC")
    fun getMediaForCollectionPagingSource(collectionTag: String): PagingSource<Int, DeteksiGambar>

    @Transaction
    @Query("SELECT * FROM deteksi_gambar")
    fun getAllIndexedImagesWithDetails(): List<DataRelations>

    @Query("SELECT * FROM deteksi_gambar WHERE path LIKE '%' || :path || '%' AND is_deleted = 0")
    fun getImagesByPath(path: String): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE ukuran >= :minSize AND ukuran <= :maxSize AND is_deleted = 0")
    fun getImagesBySize(minSize: Long, maxSize: Long): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE resolusi LIKE '%' || :resolution || '%' AND is_deleted = 0")
    fun getImagesByResolution(resolution: String): List<DeteksiGambar>

    @Query("SELECT COUNT(uri) FROM deteksi_gambar WHERE is_deleted = 0")
    suspend fun countAllMedia(): Int

    @Query("SELECT COUNT(uri) FROM deteksi_gambar WHERE format LIKE 'image/%' AND is_deleted = 0")
    suspend fun countAllImages(): Int

    @Query("SELECT COUNT(uri) FROM deteksi_gambar WHERE format LIKE 'video/%' AND is_deleted = 0")
    suspend fun countAllVideos(): Int

    // TAMBAHKAN DI DALAM interface ScannedImageDao
    @Query("SELECT * FROM deteksi_gambar WHERE album LIKE '%' || :query || '%' AND is_deleted = 0")
    suspend fun searchAlbumByName(query: String): List<DeteksiGambar>

    @Query("SELECT * FROM deteksi_gambar WHERE koleksi LIKE '%' || :query || '%' AND is_deleted = 0")
    suspend fun searchCollectionByName(query: String): List<DeteksiGambar>
}
