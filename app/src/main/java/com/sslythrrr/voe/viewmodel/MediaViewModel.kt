package com.sslythrrr.voe.viewmodel
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
import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.sslythrrr.voe.data.AppDatabase
import com.sslythrrr.voe.ui.media.Album
import com.sslythrrr.voe.ui.media.Media
import com.sslythrrr.voe.worker.LocationWorker
import com.sslythrrr.voe.worker.MediaScanWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import com.sslythrrr.voe.data.entity.DeteksiGambar
import androidx.core.net.toUri
import android.content.SharedPreferences
import com.sslythrrr.voe.worker.LabelDetectorWorker
import android.app.Application
import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import com.sslythrrr.voe.ui.paging.StaticListPagingSource
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.use
import com.sslythrrr.voe.ui.utils.CacheManager
import com.sslythrrr.voe.ui.screens.mainscreen.formatSize
import android.app.PendingIntent
import android.os.Build
import android.util.Size
import kotlinx.coroutines.flow.flowOf
import android.media.MediaScannerConnection
import com.sslythrrr.voe.Constants
import java.io.File

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    // UI
    private val _mediaPager = MutableStateFlow<Flow<PagingData<UiModel>>?>(null)
    val mediaPager: StateFlow<Flow<PagingData<UiModel>>?> = _mediaPager.asStateFlow()
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()
    private val _currentAlbum = MutableStateFlow<Album?>(null)
    val currentAlbum: StateFlow<Album?> = _currentAlbum.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _aiAlbums = MutableStateFlow<List<Album>>(emptyList())
    val aiAlbums: StateFlow<List<Album>> = _aiAlbums.asStateFlow()

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

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private val _refreshTrigger = MutableStateFlow(0)

    private val _pagedMedia = MutableStateFlow<List<Media>>(emptyList())
    val pagedMedia: StateFlow<List<Media>> = _pagedMedia.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _navigateBackSignal = MutableSharedFlow<Unit>()
    val navigateBackSignal = _navigateBackSignal.asSharedFlow()

    private val _initialScanProgress = MutableStateFlow(0)
    val initialScanProgress: StateFlow<Int> = _initialScanProgress.asStateFlow()

    private val _initialScanTotalItems = MutableStateFlow(0)
    val initialScanTotalItems: StateFlow<Int> = _initialScanTotalItems.asStateFlow()

    private val _initialScanProcessedItems = MutableStateFlow(0)
    val initialScanProcessedItems: StateFlow<Int> = _initialScanProcessedItems.asStateFlow()

    private val _isInitialScanComplete = MutableStateFlow(false)
    val isInitialScanComplete: StateFlow<Boolean> = _isInitialScanComplete.asStateFlow()

    private val _showAiAnalysisMenu = MutableStateFlow(false)
    val showAiAnalysisMenu: StateFlow<Boolean> = _showAiAnalysisMenu.asStateFlow()

    private val _showAiPrompts = MutableStateFlow(false)
    val showAiPrompts: StateFlow<Boolean> = _showAiPrompts.asStateFlow()

    private val _duplicateMedia = MutableStateFlow<List<Media>>(emptyList())
    val duplicateMedia: StateFlow<List<Media>> = _duplicateMedia.asStateFlow()

    private val _cleanableCacheSize = MutableStateFlow("0 B")
    val cleanableCacheSize: StateFlow<String> = _cleanableCacheSize.asStateFlow()

    private val _collections = MutableStateFlow<List<String>>(emptyList())
    val collections: StateFlow<List<String>> = _collections.asStateFlow()
    private val _navigateToPage = MutableSharedFlow<Int>()

    fun deleteCollection(context: Context, collectionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()
            val allMediaWithCollections = dao.getAllMediaWithTag("")

            allMediaWithCollections.forEach { scannedImage ->
                val collections = scannedImage.collections?.split(',')
                    ?.map { it.trim() }?.toMutableList() ?: return@forEach
                if (collections.contains(collectionName)) {
                    collections.remove(collectionName)
                    val newCollectionsString = if (collections.isEmpty()) null else collections.joinToString(",")
                    dao.updateCollections(scannedImage.uri, newCollectionsString)
                }
            }
            loadCollections(context)
        }
    }

    fun createDeleteRequestIntent(context: Context, mediaList: List<Media>): PendingIntent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = mediaList.map { it.uri }
            return MediaStore.createDeleteRequest(context.contentResolver, uris)
        }
        return null
    }

    fun removeMediaEntriesFromDatabase(mediaList: List<Media>) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(getApplication()).scannedImageDao()
            val urisToDelete = mediaList.map { it.uri.toString() }
            dao.deletePermanentlyByUri(urisToDelete)
        }
    }

    fun removeMediaFromCollection(context: Context, mediaList: List<Media>, collectionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()
            mediaList.forEach { media ->
                val scannedImage = dao.getMediaByUri(media.uri.toString())
                scannedImage?.let { image ->
                    val currentCollections = image.collections
                    if (currentCollections.isNullOrBlank()) {
                        return@let
                    }
                    val collectionList = currentCollections.split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toMutableList()

                    if (collectionList.remove(collectionName)) {
                        val newCollectionsString = if (collectionList.isEmpty()) null else collectionList.joinToString(",")
                        dao.updateCollections(image.uri, newCollectionsString)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                loadCollections(context)
                loadMediaForCollection(context, collectionName)
                clearSelection()
            }
        }
    }

    fun loadCollections(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()
            val tags = dao.getAllCollectionTags()
            val allCollections = tags
                .flatMap { it.split(',') }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
            _collections.value = allCollections
        }
    }

    fun addMediaToCollection(context: Context, mediaList: List<Media>, collectionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()
            mediaList.forEach { media ->
                val scannedImage = dao.getMediaByUri(media.uri.toString())
                val existingCollections = scannedImage?.collections?.split(',')?.map { it.trim() }?.toMutableSet() ?: mutableSetOf()
                existingCollections.add(collectionName)
                dao.updateCollections(media.uri.toString(), existingCollections.joinToString(","))
            }
            loadCollections(context)
        }
    }

    fun loadMediaForCollection(context: Context, collectionTag: String) {
        val dao = AppDatabase.getInstance(context).scannedImageDao()
        val pager = Pager(
            config = PagingConfig(pageSize = 60),
            pagingSourceFactory = { dao.getMediaForCollectionPagingSource(collectionTag) }
        ).flow.map { pagingDataDeteksiGambar: PagingData<DeteksiGambar> ->
            pagingDataDeteksiGambar.map { scannedImage ->
                UiModel.MediaItem(scannedImage.toMedia()) as UiModel
            }
        }.cachedIn(viewModelScope)

        _mediaPager.value = pager
    }

    fun calculateCacheSize(context: Context) {
        viewModelScope.launch {
            val sizeBytes = CacheManager.getCacheSize(context)
            _cleanableCacheSize.value = formatSize(sizeBytes)
        }
    }

    fun performCacheCleanup(context: Context) {
        viewModelScope.launch {
            CacheManager.clearCache(context)
            calculateCacheSize(context)
        }
    }

    fun loadDuplicateMedia(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val dao = AppDatabase.getInstance(context).scannedImageDao()
                val duplicatesFromDb = dao.getDuplicateMedia()
                _duplicateMedia.value = duplicatesFromDb.map { it.toMedia() }
            }
            _isLoading.value = false
        }
    }

    fun markInitialScanCompleted(context: Context) {
        getAppPrefs(context).edit {
            putBoolean("initial_scan_completed", true)
        }
        viewModelScope.launch {
            val scanStatusDao = AppDatabase.getInstance(context).scanStatusDao()
            val objStatus = scanStatusDao.getStatusForWorker("OBJECT_DETECTOR")
            val txtStatus = scanStatusDao.getStatusForWorker("TEXT_RECOGNIZER")
            if (objStatus == null || txtStatus == null) {
                _showAiPrompts.value = true
            }
        }
    }

    fun aiPromptsShown() {
        _showAiPrompts.value = false
    }

    private fun getAppPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    fun hasInitialScanCompleted(context: Context): Boolean {
        return getAppPrefs(context).getBoolean("initial_scan_completed", false)
    }

    fun DeteksiGambar.toMedia(): Media {
        return Media(
            id = this.uri.hashCode().toLong(),
            title = this.nama,
            uri = this.uri.toUri(),
            type = this.type,
            albumId = this.album.hashCode().toLong(),
            albumName = this.album,
            dateTaken = this.tanggal,
            dateAdded = this.tanggal,
            size = this.ukuran,
            relativePath = this.path,
            isFavorite = this.isFavorite,
            fileHash = this.fileHash
        )
    }

    fun toggleFavorite(media: Media, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()
            dao.updateFavoriteStatus(media.uri.toString(), !media.isFavorite)
        }
    }

    fun selectAllFavorites(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()
            val allFavorites = dao.getAllFavorites().map { it.toMedia() }
            withContext(Dispatchers.Main) {
                _selectedMedia.value = allFavorites.toSet()
            }
        }
    }

    fun unfavoriteSelection(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()
            val urisToUnfavorite = _selectedMedia.value.map { it.uri.toString() }

            urisToUnfavorite.forEach { uri ->
                dao.updateFavoriteStatus(uri, false)
            }

            withContext(Dispatchers.Main) {
                clearSelection()
                _refreshTrigger.value++
            }
        }
    }

    fun loadFavoriteMedia(context: Context) {
        val dao = AppDatabase.getInstance(context).scannedImageDao()
        val pager = Pager(
            config = PagingConfig(pageSize = 60),
            pagingSourceFactory = { dao.getFavoriteMediaPagingSource() }
        ).flow.map { pagingDataDeteksiGambar: PagingData<DeteksiGambar> ->
            val pagingDataUiModel = pagingDataDeteksiGambar.map { scannedImage ->
                UiModel.MediaItem(scannedImage.toMedia())
            }
            pagingDataUiModel.insertSeparators<UiModel.MediaItem, UiModel> { _, _ ->
                null
            }
        }.cachedIn(viewModelScope)

        _mediaPager.value = pager
    }

    fun moveMediaToTrash(mediaList: List<Media>, context: Context, andThen: suspend () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()
            val uris = mediaList.map { it.uri.toString() }
            dao.updateTrashedStatus(uris, true, System.currentTimeMillis())
            loadAlbums(context)
            withContext(Dispatchers.Main) {
                andThen.invoke()
            }
        }
    }

    suspend fun restoreMediaFromTrash(mediaList: List<Media>, context: Context) {
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()
            val uris = mediaList.map { it.uri.toString() }
            dao.updateTrashedStatus(uris, false, null)
        }
    }

    fun loadTrashedMedia(context: Context) {
        val dao = AppDatabase.getInstance(context).scannedImageDao()
        val pager = Pager(
            config = PagingConfig(pageSize = 60),
            pagingSourceFactory = { dao.getTrashedMediaPagingSource() }
        ).flow.map { pagingDataDeteksiGambar: PagingData<DeteksiGambar> ->
            val pagingDataUiModel = pagingDataDeteksiGambar.map { scannedImage ->
                UiModel.MediaItem(scannedImage.toMedia())
            }
            pagingDataUiModel.insertSeparators<UiModel.MediaItem, UiModel> { _, _ ->
                null
            }
        }.cachedIn(viewModelScope)

        _mediaPager.value = pager
    }

    fun sendNavigateBackSignal() {
        viewModelScope.launch {
            _navigateBackSignal.emit(Unit)
        }
    }

    fun deleteMediaPermanently(context: Context, mediaList: List<Media>): PendingIntent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = mediaList.map { it.uri }
            return MediaStore.createDeleteRequest(context.contentResolver, uris)
        }
        return null
    }

    fun refreshAllData(context: Context) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadAlbums(context)
                loadAiAlbums(context)
                _refreshTrigger.value++
                delay(1000)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setMediaPagerFromList(mediaList: List<Media>) {
        val pagerFlow = Pager(
            config = PagingConfig(pageSize = 60),
            pagingSourceFactory = { StaticListPagingSource(mediaList) }
        ).flow.map { pagingData: PagingData<Media> ->
            pagingData.map { media: Media ->
                UiModel.MediaItem(media) as UiModel
            }
        }.cachedIn(viewModelScope)

        _mediaPager.value = pagerFlow
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
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )
    }

    private fun queryImages(
        context: Context,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    ): List<Media> {
        val mediaList = mutableListOf<Media>()
        val selection = MediaStore.Images.Media.DATA + " LIKE ?"
        val selectionArgs = arrayOf("%${Constants.TARGET_DIRECTORY}%")
        context.contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                IMAGE_PROJECTION,
                null,
                null,
                sortOrder
            )
            ?.use { cursor -> mediaList.addAll(processImageCursor(cursor)) }

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
            val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))

            mediaList.add(
                Media(
                    id = id,
                    title = title,
                    uri = contentUri,
                    albumId = bucketId,
                    albumName = bucketName,
                    dateTaken = dateTaken,
                    dateAdded =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                        ),
                    size = size,
                    relativePath = relativePath,
                    width = width,
                    height = height,
                    type = title.substringAfterLast('.', "").lowercase(),
                )
            )
        }

        return mediaList
    }

    private data class ThumbnailRequest(
        val mediaId: Long,
        val mediaUri: Uri,
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

    private fun getLatLongFromExif(context: Context, uri: Uri): Pair<Double, Double>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                exif.latLong?.let { Pair(it[0], it[1]) }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDate(timestamp: Long): Triple<Int, String, Int> {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val monthNumber = calendar.get(Calendar.MONTH) + 1
        return Triple(
            calendar.get(Calendar.YEAR),
            when (monthNumber) {
                1 -> "Januari"; 2 -> "Februari"; 3 -> "Maret"; 4 -> "April"
                5 -> "Mei"; 6 -> "Juni"; 7 -> "Juli"; 8 -> "Agustus"
                9 -> "September"; 10 -> "Oktober"; 11 -> "November"; 12 -> "Desember"
                else -> "Tidak Diketahui"
            },
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun performInitialScan(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isInitialScanComplete.value = false
            val imageDao = AppDatabase.getInstance(context).scannedImageDao()

            val allMediaOnDevice = fetchMedia(context)
            val scannedUris = imageDao.getAllScannedUris().toSet()
            val mediaToScan = allMediaOnDevice.filter { it.uri.toString() !in scannedUris }

            if (mediaToScan.isEmpty()) {
                markInitialScanCompleted(context)
                _isInitialScanComplete.value = true
                startLocationWorker(context, showNotification = true)
                return@launch
            }

            _initialScanTotalItems.value = mediaToScan.size
            _initialScanProcessedItems.value = 0
            val batchSize = 100

            mediaToScan.chunked(batchSize).forEach { batch ->
                val deteksiGambarList = batch.mapNotNull { media ->
                    try {
                        val (year, month, day) = formatDate(media.dateTaken)
                        val latLng = getLatLongFromExif(context, media.uri)
                        val fileHash = getFileHash(context, media.uri)

                        DeteksiGambar(
                            uri = media.uri.toString(),
                            path = media.relativePath,
                            nama = media.title,
                            ukuran = media.size,
                            album = media.albumName ?: "Unknown",
                            type = media.title.substringAfterLast('.', "").lowercase(),
                            resolusi = "${media.width}x${media.height}",
                            tanggal = media.dateTaken,
                            tahun = year,
                            bulan = month,
                            hari = day,
                            latitude = latLng?.first,
                            longitude = latLng?.second,
                            fileHash = fileHash
                        )
                    } catch (_: Exception) {
                        null
                    }
                }

                if (deteksiGambarList.isNotEmpty()) {
                    imageDao.insertAll(deteksiGambarList)
                }

                val newProcessedCount = _initialScanProcessedItems.value + batch.size
                _initialScanProcessedItems.value = newProcessedCount
                _initialScanProgress.value = (newProcessedCount * 100 / mediaToScan.size)
            }

            markInitialScanCompleted(context)
            _isInitialScanComplete.value = true
            startLocationWorker(context, showNotification = true)
        }
    }

    init {
        Log.d("MediaViewModel", "Using $optimalThreads threads for thumbnail processing")
        viewModelScope.launch {
            scrollStateChannel.receiveAsFlow().collectLatest { scrolling ->
                isActivelyScrolling.set(scrolling)
            }
        }
        checkAiWorkerStatus(getApplication<Application>().applicationContext)
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
                    startScanning(context)
                    loadMedia(context)
                }
            }
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver!!
        )
    }

    fun unregisterContentObservers(context: Context) {
        mediaObserver?.let { context.contentResolver.unregisterContentObserver(it) }
    }

    fun setCurrentMediaPager(mediaList: List<Media>) {
        val uiModelList = mediaList.map { UiModel.MediaItem(it) }
        _mediaPager.value = flowOf(PagingData.from(uiModelList))
    }

    fun loadMedia(context: Context) {
        loadJob?.cancel()
        _isLoading.value = true
        val dao = AppDatabase.getInstance(context).scannedImageDao()

        viewModelScope.launch {
            _refreshTrigger.collectLatest {
                val newPager = Pager(
                    config = PagingConfig(pageSize = 60, enablePlaceholders = false),
                    pagingSourceFactory = {
                        val albumName = _currentAlbum.value?.name
                        if (albumName != null && albumName != "Semua Media") {
                            dao.getMediaPagingSourceByAlbum(albumName)
                        } else {
                            dao.getAllMediaPagingSource()
                        }
                    }
                ).flow.map { pagingData: PagingData<DeteksiGambar> ->
                    pagingData.map { scannedImage ->
                        scannedImage.toMedia()
                    }
                }.map { pagingDataMedia ->
                    pagingDataMedia.map { media ->
                        UiModel.MediaItem(media)
                    }.insertSeparators { before, after ->
                        val beforeMedia = before?.media
                        val afterMedia = after?.media
                        if (afterMedia == null) return@insertSeparators null
                        if (beforeMedia == null) return@insertSeparators UiModel.SeparatorItem(formatDateForHeader(afterMedia.dateTaken))

                        val beforeDate = Calendar.getInstance().apply { timeInMillis = beforeMedia.dateTaken }
                        val afterDate = Calendar.getInstance().apply { timeInMillis = afterMedia.dateTaken }
                        if (!isSameDay(beforeDate, afterDate)) {
                            UiModel.SeparatorItem(formatDateForHeader(afterMedia.dateTaken))
                        } else {
                            null
                        }
                    }
                }.cachedIn(viewModelScope)

                _mediaPager.value = newPager

                if (_currentAlbum.value == null) {
                    loadAlbums(context)
                }
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDateForHeader(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        calendar.timeInMillis = timestamp

        return when {
            isSameDay(calendar, today) -> "Hari Ini"
            isSameDay(calendar, yesterday) -> "Kemarin"
            else -> SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(timestamp))
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
                val allMedia = AppDatabase.getInstance(context).scannedImageDao()
                    .getAllNonTrashedMedia().map { it.toMedia() }

                if (allMedia.isEmpty()) {
                    _albums.value = emptyList()
                    return@launch
                }

                val albumsMap = allMedia
                    .groupBy { it.albumName ?: "Unknown" }
                    .mapNotNull { (albumName, mediaInAlbum) ->
                        val latestMedia = mediaInAlbum.maxByOrNull { it.dateTaken }
                        if (latestMedia != null) {
                            val coverUri = latestMedia.uri

                            Album(
                                id = latestMedia.albumId ?: albumName.hashCode().toLong(),
                                name = albumName,
                                uri = coverUri,
                                mediaCount = mediaInAlbum.size,
                                type = latestMedia.type,
                                latestMediaDate = latestMedia.dateTaken
                            )
                        } else {
                            null
                        }
                    }

                val totalCount = allMedia.size
                val latestAlbumForMaster = albumsMap.maxByOrNull { it.latestMediaDate }

                val masterAlbum = Album(
                    id = -1L,
                    name = "Semua Media",
                    uri = latestAlbumForMaster?.uri ?: Uri.EMPTY,
                    mediaCount = totalCount,
                    latestMediaDate = latestAlbumForMaster?.latestMediaDate ?: 0L,
                    type = latestAlbumForMaster?.type ?: ""
                )

                val sortedAlbums = listOf(masterAlbum) + albumsMap.sortedByDescending { it.latestMediaDate }
                _albums.value = sortedAlbums

            } catch (e: Exception) {
                Log.e("ViewModelDebug", "Error fatal di loadAlbums: ${e.message}", e)
                _albums.value = emptyList()
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

    private fun fetchImages(context: Context, mediaList: MutableList<Media>) {
        mediaList.addAll(queryImages(context))
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

    internal suspend fun fetchMediaAlbum(context: Context, albumId: Long): List<Media> {
        return withContext(Dispatchers.IO) {
            val mediaList = mutableListOf<Media>()
            loadImages(context, mediaList, albumId)
            mediaList.sortedByDescending { it.dateTaken }
        }
    }

    suspend fun fetchMedia(context: Context): List<Media> {
        return withContext(Dispatchers.IO) {
            val mediaList = mutableListOf<Media>()
            fetchImages(context, mediaList)
            mediaList.sortedByDescending { it.dateTaken }
        }
    }

    fun loadAiAlbums(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val objectDao = AppDatabase.getInstance(context).detectedObjectDao()
                val topLabels = objectDao.getTopLabels()

                val aiAlbumList = topLabels.mapNotNull { labelCount ->
                    val thumbnailImage = objectDao.getImagesWithLabel(labelCount.label).firstOrNull()
                    thumbnailImage?.let {
                        Album(
                            id = labelCount.label.hashCode().toLong(),
                            name = labelCount.label.replaceFirstChar { it.uppercase() },
                            uri = it.uri.toUri(),
                            mediaCount = labelCount.count,
                            latestMediaDate = it.tanggal,
                            type = it.type,
                        )
                    }
                }
                _aiAlbums.value = aiAlbumList
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Error loading AI albums: ${e.message}", e)
            }
        }
    }

    fun loadMediaForAiLabel(context: Context, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val objectDao = AppDatabase.getInstance(context).detectedObjectDao()
                val imagesFromDb = objectDao.getImagesWithLabel(label)

                val mediaList = imagesFromDb.map { scannedImage ->
                    Media(
                        id = scannedImage.uri.hashCode().toLong(),
                        title = scannedImage.nama,
                        uri = scannedImage.uri.toUri(),
                        albumId = scannedImage.album.hashCode().toLong(),
                        albumName = scannedImage.album,
                        dateTaken = scannedImage.tanggal,
                        dateAdded = scannedImage.tanggal,
                        size = scannedImage.ukuran,
                        relativePath = scannedImage.path,
                        type = scannedImage.type,
                    )
                }
                _pagedMedia.value = mediaList

            } catch (e: Exception) {
                Log.e("MediaViewModel", "Error loading media for AI label: $label", e)
            }
        }
    }

    fun startScanning(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val initialScanConstraints =
            Constraints.Builder()
                .setRequiresDeviceIdle(false)
                .setRequiresBatteryNotLow(false)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(false)
                .setRequiresStorageNotLow(true)
                .build()

        val initialMediaScanWork =
            OneTimeWorkRequestBuilder<MediaScanWorker>()
                .setConstraints(initialScanConstraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("media_scanner_work")
                .setInputData(workDataOf("needs_notification" to true))
                .build()

        val locationWork = OneTimeWorkRequestBuilder<LocationWorker>()
            .setConstraints(initialScanConstraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("location_worker")
            .setInputData(workDataOf("needs_notification" to true))
            .build()
        /*
                val objectDetectionWork = OneTimeWorkRequestBuilder<LabelDetectorWorker>()
                    .setConstraints(initialScanConstraints)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS * 2,
                        TimeUnit.MILLISECONDS
                    )
                    .addTag("object_detector_work")
                    .setInputData(workDataOf("needs_notification" to true))
                    .build()

                val textRecognitionWork = OneTimeWorkRequestBuilder<TextRecognizerWorker>()
                    .setConstraints(initialScanConstraints)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS * 2,
                        TimeUnit.MILLISECONDS
                    )
                    .addTag("text_recognizer_work")
                    .setInputData(workDataOf("needs_notification" to true))
                    .build()
        */
        val reactiveConstraints = Constraints.Builder()
            .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
            .build()

        val reactiveMediaScanWork = OneTimeWorkRequestBuilder<MediaScanWorker>()
            .setConstraints(reactiveConstraints)
            .addTag("reactive_media_scan")
            .setInputData(workDataOf("needs_notification" to true))
            .build()

        val reactiveLocationWork = OneTimeWorkRequestBuilder<LocationWorker>()
            .setInputData(workDataOf("needs_notification" to true))
            .build()

        val reactiveObjectDetectionWork = OneTimeWorkRequestBuilder<LabelDetectorWorker>()
            .setInputData(workDataOf("needs_notification" to true))
            .build()

        workManager
            .beginUniqueWork(
                "reactive_full_scan",
                //ExistingWorkPolicy.APPEND_OR_REPLACE,
                ExistingWorkPolicy.REPLACE,
                reactiveMediaScanWork
            )
            .then(reactiveLocationWork)
            .then(reactiveObjectDetectionWork)
            .enqueue()
    }
    fun selectingMedia(mediaList: List<Media>) {
        _selectedMedia.update { currentSelection ->
            currentSelection + mediaList
        }
        if (_selectedMedia.value.isNotEmpty()) {
            _isSelectionMode.value = true
        }
    }
    fun startObjectDetection(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val objectDetectionWork = OneTimeWorkRequestBuilder<LabelDetectorWorker>()
            .setInputData(workDataOf("needs_notification" to true))
            .build()
        workManager.enqueueUniqueWork(
            "object_detector_work_manual",
            ExistingWorkPolicy.KEEP,
            objectDetectionWork
        )
    }

    fun startLocationWorker(context: Context, showNotification: Boolean = false) {
        val workManager = WorkManager.getInstance(context)
        val work = OneTimeWorkRequestBuilder<LocationWorker>()
            .setInputData(workDataOf("needs_notification" to showNotification))
            .addTag("location_worker")
            .build()

        workManager.enqueueUniqueWork(
            "location_worker_unique",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            work
        )
    }

    fun scanDirectoryForNewMedia(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetDirectory = File(Constants.TARGET_DIRECTORY)
            if (targetDirectory.exists() && targetDirectory.isDirectory) {
                val filesToScan = targetDirectory.listFiles()?.map { it.absolutePath }?.toTypedArray()
                if (filesToScan != null && filesToScan.isNotEmpty()) {
                    MediaScannerConnection.scanFile(
                        context,
                        filesToScan,
                        null
                    ) { path, uri ->
                        Log.d("MediaScanner", "Scanned $path -> uri: $uri")
                    }
                }
            }
        }
    }

    fun checkAiWorkerStatus(context: Context) {
        viewModelScope.launch {
            val scanStatusDao = AppDatabase.getInstance(context).scanStatusDao()
            val objStatusNull = scanStatusDao.getStatusForWorker("LabelDetectorWorker") == null
            val txtStatusNull = scanStatusDao.getStatusForWorker("TextRecognizerWorker") == null

            _showAiAnalysisMenu.value = objStatusNull || txtStatusNull
        }
    }

    fun syncDatabaseWithMediaStore(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).scannedImageDao()

            val urisInDb = dao.getAllScannedUris().toSet()
            if (urisInDb.isEmpty()) return@launch

            val urisOnDevice = mutableSetOf<String>()
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(imageUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    urisOnDevice.add(ContentUris.withAppendedId(imageUri, id).toString())
                }
            }
            val ghostUris = urisInDb.subtract(urisOnDevice).toList()

            if (ghostUris.isNotEmpty()) {
                Log.d("SyncDB", "Menghapus ${ghostUris.size} media hantu.")
                dao.deletePermanentlyByUri(ghostUris)

                withContext(Dispatchers.Main) {
                    _refreshTrigger.value++
                }
            } else {
                Log.d("SyncDB", "Database sudah sinkron.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        searchJob?.cancel()
        scrollStateJob?.cancel()
    }
}

sealed class UiModel {
    data class MediaItem(val media: Media) : UiModel()
    data class SeparatorItem(val date: String) : UiModel()
}

private fun getFileHash(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val md = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var read: Int
            while (inputStream.read(buffer).also { read = it } > 0) {
                md.update(buffer, 0, read)
            }
            val digest = md.digest()
            val sb = StringBuilder()
            for (b in digest) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        }
    } catch (_: Exception) {
        null
    }
}