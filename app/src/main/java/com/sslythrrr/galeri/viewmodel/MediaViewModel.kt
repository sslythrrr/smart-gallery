package com.sslythrrr.galeri.viewmodel
// mt10
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.sslythrrr.galeri.repository.FavoriteRepository
import com.sslythrrr.galeri.ui.media.Album
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.media.MediaType
import com.sslythrrr.galeri.ui.paging.MediaPagingSource
import com.sslythrrr.galeri.worker.LocationWorker
import com.sslythrrr.galeri.worker.MediaScanWorker
import com.sslythrrr.galeri.worker.ObjectDetectorWorker
import com.sslythrrr.galeri.worker.TextRecognizerWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MediaViewModel() : ViewModel() {
    fun initContext(context: Context) {
        context.applicationContext
    }

    // UI
    private var _mediaPager = MutableStateFlow<Pager<Int, Media>?>(null)
    val mediaPager: StateFlow<Pager<Int, Media>?> = _mediaPager.asStateFlow()
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()
    private val _currentAlbum = MutableStateFlow<Album?>(null)
    val currentAlbum: StateFlow<Album?> = _currentAlbum.asStateFlow()
    private val _isLoading = MutableStateFlow(false)

    // Control
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    private val _selectedMedia = MutableStateFlow<Set<Media>>(emptySet())
    val selectedMedia: StateFlow<Set<Media>> = _selectedMedia.asStateFlow()
    internal var isActivelyScrolling = AtomicBoolean(false)
    private val scrollStateChannel = Channel<Boolean>(Channel.CONFLATED)
    private var scrollStateJob: Job? = null

    // Config
    val numProcessors = Runtime.getRuntime().availableProcessors()
    val optimalThreads = (numProcessors / 2).coerceAtLeast(2)

    @OptIn(ExperimentalCoroutinesApi::class)

    // Observer
    private var mediaObserver: ContentObserver? = null
    private var videoObserver: ContentObserver? = null

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    private val _pagedMedia = MutableStateFlow<List<Media>>(emptyList())
    val pagedMedia: StateFlow<List<Media>> = _pagedMedia.asStateFlow()

    private lateinit var favoriteRepository: FavoriteRepository
    private val _favoriteMedia = MutableStateFlow<List<Media>>(emptyList())
    val favoriteMedia: StateFlow<List<Media>> = _favoriteMedia.asStateFlow()

    fun initializeFavoriteRepository(context: Context) {
        favoriteRepository = FavoriteRepository.getInstance(context)
    }

    fun toggleFavorite(mediaId: Long) {
        favoriteRepository.toggleFavorite(mediaId)
    }

    @Composable
    fun isFavorite(mediaId: Long): Boolean {
        return if (::favoriteRepository.isInitialized) {
            val favoriteIds = favoriteRepository.favoriteMediaIds.collectAsState()
            favoriteIds.value.contains(mediaId)
        } else false
    }

    suspend fun loadFavoriteMedia(context: Context) {
        if (::favoriteRepository.isInitialized) {
            val allMedia = fetchMedia(context)
            val favoriteIds = favoriteRepository.getFavoriteMediaIds()
            _favoriteMedia.value = allMedia.filter { favoriteIds.contains(it.id) }
                .map { it.copy(isFavorite = true) }
        }
    }

    fun selectFavoriteMedia() {
        _selectedMedia.value = favoriteMedia.value.toSet()
    }

    companion object {
        val IMAGE_PROJECTION =
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.RELATIVE_PATH
            )

        val VIDEO_PROJECTION =
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.RELATIVE_PATH
            )
    }

    private fun queryImages(
        context: Context,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    ): List<Media> {
        val mediaList = mutableListOf<Media>()

        context.contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                IMAGE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )
            ?.use { cursor -> mediaList.addAll(processImageCursor(cursor)) }

        return mediaList
    }

    private fun queryVideos(
        context: Context,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String = "${MediaStore.Video.Media.DATE_TAKEN} DESC"
    ): List<Media> {
        val mediaList = mutableListOf<Media>()

        context.contentResolver
            .query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                VIDEO_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )
            ?.use { cursor -> mediaList.addAll(processVideoCursor(cursor)) }

        return mediaList
    }

    private fun processImageCursor(cursor: Cursor): List<Media> {
        val mediaList = mutableListOf<Media>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            val title =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                    ?: ""
            val dateTaken =
                if (
                    cursor.isNull(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
                ) {
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    ) * 1000
                } else {
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
                }
            val bucketId =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
            val bucketName =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                ) ?: "Unknown Album"
            val contentUri =
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
            val relativePath =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                ) ?: ""

            mediaList.add(
                Media(
                    id = id,
                    title = title,
                    uri = contentUri,
                    type = MediaType.IMAGE,
                    albumId = bucketId,
                    albumName = bucketName,
                    dateTaken = dateTaken,
                    dateAdded =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                        ),
                    size = size,
                    relativePath = relativePath
                )
            )
        }

        return mediaList
    }

    private fun processVideoCursor(cursor: Cursor): List<Media> {
        val mediaList = mutableListOf<Media>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
            val title =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                    ?: ""
            val dateTaken =
                if (
                    cursor.isNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN))
                ) {
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    ) * 1000
                } else {
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN))
                }
            val bucketId =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
            val bucketName =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                ) ?: "Unknown Album"
            val duration =
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
            val contentUri =
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
            val relativePath =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH))
                    ?: ""

            mediaList.add(
                Media(
                    id = id,
                    title = title,
                    uri = contentUri,
                    type = MediaType.VIDEO,
                    albumId = bucketId,
                    albumName = bucketName,
                    dateTaken = dateTaken,
                    dateAdded =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                        ),
                    duration = duration,
                    size = size,
                    relativePath = relativePath
                )
            )
        }

        return mediaList
    }

    private data class ThumbnailRequest(
        val mediaId: Long,
        val mediaUri: Uri,
        val mediaType: MediaType,
        val priority: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : Comparable<ThumbnailRequest> {
        override fun compareTo(other: ThumbnailRequest): Int {
            return if (this.priority != other.priority) {
                other.priority - this.priority
            } else {
                (this.timestamp - other.timestamp).toInt()
            }
        }
    }

    fun setPagedMedia(media: List<Media>) {
        _pagedMedia.value = media
    }

    fun isFirstInstall(context: Context): Boolean {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("is_first_install", true)
    }

    fun markFirstInstall(context: Context) {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit { putBoolean("is_first_install", false) }
    }

    init {
        Log.d("MediaViewModel", "Using $optimalThreads threads for thumbnail processing")
        viewModelScope.launch {
            scrollStateChannel.receiveAsFlow().collectLatest { scrolling ->
                isActivelyScrolling.set(scrolling)
            }
        }
    }

    fun selectionMode(enable: Boolean) {
        if (!enable) {
            _selectedMedia.value = emptySet()
        }
        _isSelectionMode.value = enable
    }

    fun selectingMedia(media: Media) {
        _selectedMedia.update { currentSelection ->
            if (currentSelection.contains(media)) {
                currentSelection - media
            } else {
                currentSelection + media
            }
        }
        if (_selectedMedia.value.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun clearSelection() {
        _selectedMedia.value = emptySet()
        _isSelectionMode.value = false
    }

    fun registerContentObservers(context: Context) {
        val handler = Handler(Looper.getMainLooper())
        mediaObserver =
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    loadMedia(context)
                }
            }
        videoObserver =
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    loadMedia(context)
                }
            }
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver!!
        )
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            videoObserver!!
        )
    }

    fun unregisterContentObservers(context: Context) {
        mediaObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        videoObserver?.let { context.contentResolver.unregisterContentObserver(it) }
    }

    fun loadMedia(context: Context) {
        loadJob?.cancel()
        _isLoading.value = true

        val pager =
            Pager(
                config =
                    PagingConfig(pageSize = 50, enablePlaceholders = false, prefetchDistance = 10),
                pagingSourceFactory = { MediaPagingSource(context, _currentAlbum.value?.id) }
            )

        _mediaPager.value = pager
        _isLoading.value = false
        loadAlbums(context)
    }

    private fun updateAlbums(
        albumsMap: MutableMap<Long, Album>,
        bucketId: Long,
        bucketName: String,
        thumbnailUri: Uri,
        mediaType: MediaType,
        dateTaken: Long
    ) {
        val existingAlbum = albumsMap[bucketId]
        if (existingAlbum == null) {
            albumsMap[bucketId] =
                Album(
                    id = bucketId,
                    name = bucketName,
                    uri = thumbnailUri,
                    mediaCount = 1,
                    type = mediaType,
                    latestMediaDate = dateTaken
                )
        } else {
            val isNewer = dateTaken > existingAlbum.latestMediaDate
            val newThumbnailUri = if (isNewer) thumbnailUri else existingAlbum.uri
            val newType =
                if (mediaType == MediaType.IMAGE || existingAlbum.type == MediaType.IMAGE) {
                    MediaType.IMAGE
                } else {
                    MediaType.VIDEO
                }

            albumsMap[bucketId] =
                Album(
                    id = bucketId,
                    name = bucketName,
                    uri = newThumbnailUri,
                    mediaCount = existingAlbum.mediaCount + 1,
                    type = newType,
                    latestMediaDate = maxOf(existingAlbum.latestMediaDate, dateTaken)
                )
        }
    }

    fun setCurrentAlbum(album: Album?, context: Context, shouldLoadMedia: Boolean = true) {
        if (shouldLoadMedia) {
            _currentAlbum.value = album
            loadMedia(context)
        } else {
            _currentAlbum.value = album
        }
    }

    private fun loadAlbums(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val albumsMap = mutableMapOf<Long, Album>()

                imagesAlbum(context, albumsMap)
                videosAlbum(context, albumsMap)

                _albums.update { albumsMap.values.sortedByDescending { it.latestMediaDate } }
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Error loading albums: ${e.message}", e)
            }
        }
    }

    private fun imagesAlbum(context: Context, albumsMap: MutableMap<Long, Album>) {
        val projection =
            arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED
            )

        context.contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            )
            ?.use { cursor ->
                val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateTakenColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val bucketId = cursor.getLong(bucketIdColumn)
                    val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown Album"
                    val mediaId = cursor.getLong(idColumn)
                    val dateTaken =
                        if (cursor.isNull(dateTakenColumn)) {
                            cursor.getLong(dateAddedColumn) * 1000
                        } else {
                            cursor.getLong(dateTakenColumn)
                        }

                    val thumbnailUri =
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            mediaId
                        )

                    updateAlbums(
                        albumsMap,
                        bucketId,
                        bucketName,
                        thumbnailUri,
                        MediaType.IMAGE,
                        dateTaken
                    )
                }
            }
    }

    private fun videosAlbum(context: Context, albumsMap: MutableMap<Long, Album>) {
        val projection =
            arrayOf(
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DATE_ADDED
            )

        context.contentResolver
            .query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_TAKEN} DESC"
            )
            ?.use { cursor ->
                val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val dateTakenColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
                val dateAddedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val bucketId = cursor.getLong(bucketIdColumn)
                    val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown Album"
                    val mediaId = cursor.getLong(idColumn)
                    val dateTaken =
                        if (cursor.isNull(dateTakenColumn)) {
                            cursor.getLong(dateAddedColumn) * 1000
                        } else {
                            cursor.getLong(dateTakenColumn)
                        }

                    val thumbnailUri =
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            mediaId
                        )

                    updateAlbums(
                        albumsMap,
                        bucketId,
                        bucketName,
                        thumbnailUri,
                        MediaType.VIDEO,
                        dateTaken
                    )
                }
            }
    }

    private fun loadImages(context: Context, mediaList: MutableList<Media>, albumId: Long) {
        mediaList.addAll(
            queryImages(
                context,
                selection = "${MediaStore.Images.Media.BUCKET_ID} = ?",
                selectionArgs = arrayOf(albumId.toString())
            )
        )
    }

    private fun loadVideos(context: Context, mediaList: MutableList<Media>, albumId: Long) {
        mediaList.addAll(
            queryVideos(
                context,
                selection = "${MediaStore.Video.Media.BUCKET_ID} = ?",
                selectionArgs = arrayOf(albumId.toString())
            )
        )
    }

    private fun fetchImages(context: Context, mediaList: MutableList<Media>) {
        mediaList.addAll(queryImages(context))
    }

    private fun fetchVideos(context: Context, mediaList: MutableList<Media>) {
        mediaList.addAll(queryVideos(context))
    }

    fun selectMedia(context: Context) {
        viewModelScope.launch {
            try {
                val currentPagerValue = _mediaPager.value
                if (currentPagerValue != null) {
                    val allMediaInCurrentAlbum =
                        if (_currentAlbum.value != null) {
                            fetchMediaAlbum(context, _currentAlbum.value!!.id)
                        } else {
                            fetchMedia(context)
                        }
                    _selectedMedia.value = allMediaInCurrentAlbum.toSet()
                    _isSelectionMode.value = true
                }
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Error selecting all media: ${e.message}", e)
            }
        }
    }

    fun selectVideoMedia(context: Context) {
        viewModelScope.launch {
            try {
                val currentPagerValue = _mediaPager.value
                if (currentPagerValue != null) {
                    val allMediaInCurrentAlbum =
                        if (_currentAlbum.value != null) {
                            fetchMediaAlbum(context, _currentAlbum.value!!.id)
                        } else {
                            fetchMedia(context)
                        }
                    _selectedMedia.value =
                        allMediaInCurrentAlbum.filter { it.type == MediaType.VIDEO }.toSet()
                    _isSelectionMode.value = true
                }
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Error selecting all media: ${e.message}", e)
            }
        }
    }

    internal suspend fun fetchMediaAlbum(context: Context, albumId: Long): List<Media> {
        return withContext(Dispatchers.IO) {
            val mediaList = mutableListOf<Media>()
            loadImages(context, mediaList, albumId)
            loadVideos(context, mediaList, albumId)
            mediaList.sortedByDescending { it.dateTaken }
        }
    }

    suspend fun fetchMedia(context: Context): List<Media> {
        return withContext(Dispatchers.IO) {
            val mediaList = mutableListOf<Media>()
            fetchImages(context, mediaList)
            fetchVideos(context, mediaList)
            mediaList.sortedByDescending { it.dateTaken }
        }
    }

    fun startScanning(context: Context) {
        val constraints =
            Constraints.Builder()
                .setRequiresDeviceIdle(false)
                .setRequiresBatteryNotLow(false)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(false)
                .setRequiresStorageNotLow(true)
                .build()

        val mediaScanWork =
            OneTimeWorkRequestBuilder<MediaScanWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("media_scanner_work")
                .setInputData(workDataOf("needs_notification" to true))
                .build()

        val locationWork = OneTimeWorkRequestBuilder<LocationWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("location_worker")
            .setInputData(workDataOf("needs_notification" to true))
            .build()

        val objectDetectionWork = OneTimeWorkRequestBuilder<ObjectDetectorWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS * 2,
                TimeUnit.MILLISECONDS
            )
            .addTag("object_detector_work")
            .setInputData(workDataOf("needs_notification" to true))
            .build()
        /*
                // Text Recognizer Worker (setelah media scan selesai)
                val textRecognitionWork = OneTimeWorkRequestBuilder<TextRecognizerWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS * 2,
                        TimeUnit.MILLISECONDS
                    )
                    .addTag("text_recognizer_work")
                    .setInputData(workDataOf("needs_notification" to true))
                    .build()
        */
        WorkManager.getInstance(context)
            .beginUniqueWork(
                "start_scanning",
                ExistingWorkPolicy.KEEP,
                mediaScanWork
            )
            .then(listOf(objectDetectionWork,/*textRecognitionWork,*/ locationWork))
            //.then(listOf(locationWork))
            .enqueue()
    }

    fun loadAllMedia(context: Context) {
        _mediaPager.value = Pager(
            config = PagingConfig(pageSize = 50),
            pagingSourceFactory = {
                MediaPagingSource(context = context)
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        searchJob?.cancel()
        scrollStateJob?.cancel()
    }
}