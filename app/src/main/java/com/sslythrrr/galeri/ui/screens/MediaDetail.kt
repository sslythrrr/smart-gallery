package com.sslythrrr.galeri.ui.screens
//r
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.filter
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.media.MediaAction
import com.sslythrrr.galeri.ui.media.MediaNavbar
import com.sslythrrr.galeri.ui.media.MediaType
import com.sslythrrr.galeri.ui.theme.BlueAccent
import com.sslythrrr.galeri.ui.theme.DarkBackground
import com.sslythrrr.galeri.ui.theme.GoldAccent
import com.sslythrrr.galeri.ui.theme.LightBackground
import com.sslythrrr.galeri.ui.theme.SurfaceDark
import com.sslythrrr.galeri.ui.theme.SurfaceLight
import com.sslythrrr.galeri.ui.theme.TextBlack
import com.sslythrrr.galeri.ui.theme.TextGray
import com.sslythrrr.galeri.ui.theme.TextGrayDark
import com.sslythrrr.galeri.ui.theme.TextWhite
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import com.sslythrrr.galeri.viewmodel.UiModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    initialMediaId: Long, // BARU: Menerima ID media awal
    onBack: () -> Unit,
    onShare: (Media) -> Unit = {},
    isDarkTheme: Boolean,
    albumId: String? = null,
    viewModel: MediaViewModel
) {
    val originalPagerFlow = viewModel.mediaPager.collectAsState().value

    // Buat flow baru yang HANYA berisi MediaItem, membuang SeparatorItem
    val mediaOnlyPagerFlow = remember(originalPagerFlow) {
        originalPagerFlow?.map { pagingData ->
            pagingData.filter { it is UiModel.MediaItem }
        }
    }

    val lazyPagingItems = mediaOnlyPagerFlow?.collectAsLazyPagingItems()

    var initialPage by remember { mutableIntStateOf(0) }
    var isInitialPageFound by remember { mutableStateOf(false) }

    // Efek untuk mencari halaman awal setelah item dimuat
    LaunchedEffect(lazyPagingItems?.itemCount) {
        if (lazyPagingItems != null && !isInitialPageFound) {
            val itemCount = lazyPagingItems.itemCount
            for (i in 0 until itemCount) {
                val item = lazyPagingItems.peek(i)
                if (item is UiModel.MediaItem && item.media.id == initialMediaId) {
                    initialPage = i
                    isInitialPageFound = true
                    break
                }
            }
        }
    }

    // Tampilkan loading jika Pager belum siap atau halaman awal belum ketemu
    if (lazyPagingItems == null || !isInitialPageFound && lazyPagingItems.itemCount > 0) {
        Box(modifier = Modifier.fillMaxSize().background(if(isDarkTheme) DarkBackground else LightBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = if(isDarkTheme) GoldAccent else BlueAccent)
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = initialPage) {
        lazyPagingItems.itemCount
    }

    val currentMedia by remember(pagerState.currentPage) {
        derivedStateOf {
            if (lazyPagingItems.itemCount > 0) {
                (lazyPagingItems[pagerState.currentPage] as? UiModel.MediaItem)?.media
            } else {
                null
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }
    val isVideo = currentMedia?.type == MediaType.VIDEO
    var isVideoPlaying by remember { mutableStateOf(false) }
    var showVideoControls by remember { mutableStateOf(false) }
    val shouldShowControls = when {
        isVideo -> !isVideoPlaying // Show custom controls hanya kalau video tidak playing
        else -> showControls // Image tetap pakai logic lama
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.navigateBackSignal.collect {
            onBack()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> isResumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showInfoDialog) {
        MediaNavbar(
            media = currentMedia,
            onDismiss = { showInfoDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) DarkBackground else LightBackground.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
            }
    ) {
        HorizontalPager(
            state = pagerState,
            key = lazyPagingItems.itemKey { (it as UiModel.MediaItem).media.id },
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val uiModel = lazyPagingItems[pageIndex]
            if (uiModel is UiModel.MediaItem) {
                MediaAction(
                    media = uiModel.media,
                    isResumed = isResumed && pageIndex == pagerState.currentPage,
                    onSingleTap = {
                        if (uiModel.media.type != MediaType.VIDEO) {
                            showControls = !showControls
                        } else {
                            if (!isVideoPlaying) {
                                showControls = !showControls
                            }
                        }
                    },
                    // Anda mungkin perlu menyesuaikan parameter MediaAction ini
                    // karena filteredMediaList sudah tidak ada.
                    // Untuk isPagerCurrentPage, pagerState, dll. bisa disesuaikan atau dihapus
                    // jika tidak lagi relevan dengan PagingData
                    isPagerCurrentPage = pageIndex == pagerState.currentPage,
                    pagerState = pagerState,
                    filteredMediaList = emptyList(), // Hapus dependensi ini
                    isVideoPlaying = isVideoPlaying,
                    onVideoPlayStateChanged = { playing ->
                        isVideoPlaying = playing
                        showVideoControls = playing
                    },
                    showVideoControls = showVideoControls
                )
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            isZoomed = false
        }

        AnimatedVisibility(
            visible = shouldShowControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) SurfaceDark.copy(alpha = 0.6f) else SurfaceLight.copy(
                        alpha = 0.9f
                    ),
                    titleContentColor = if (isDarkTheme) TextWhite else TextBlack.copy(alpha = 0.9f)
                ),
                title = {
                    Column {
                        Text(
                            text = currentMedia?.title ?: "",
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (albumId != null) {
                            Text(
                                text = currentMedia?.albumName ?: "",
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isDarkTheme) TextGray else TextGrayDark
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkTheme) GoldAccent else BlueAccent
                        )
                    }
                },
                actions = {
                    currentMedia?.let { media ->
                        IconButton(
                            onClick = {
                                viewModel.toggleFavorite(media, context)
                            }
                        ) {
                            Icon(
                                imageVector = if (media.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (media.isFavorite) Color.Red else (if (isDarkTheme) GoldAccent else BlueAccent)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = shouldShowControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomAppBar(
                containerColor = if (isDarkTheme) SurfaceDark.copy(alpha = 0.6f) else SurfaceLight.copy(
                    alpha = 0.9f
                ),
                contentColor = TextWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    currentMedia?.let { media ->
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (media.type == MediaType.IMAGE) "image/*" else "video/*"
                                putExtra(Intent.EXTRA_STREAM, media.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Bagikan ${media.title}"
                                )
                            )
                            onShare(media)
                        }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                tint = if (isDarkTheme) SurfaceLight else SurfaceDark
                            )
                        }
                        var showDeleteConfirmation by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Media",
                                tint = if (isDarkTheme) Color.White else Color.Black
                            )
                        }
                        if (showDeleteConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmation = false },
                                title = { Text("Pindahkan ke Sampah?") },
                                text = { Text("Item ini akan dihapus permanen setelah 7 hari. Anda dapat memulihkannya dari sampah.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.moveMediaToTrash(listOf(media), context) {
                                                onBack()
                                            }
                                            showDeleteConfirmation = false
                                            viewModel.clearSelection()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Text("Ya, Pindahkan")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showDeleteConfirmation = false }) {
                                        Text("Batal")
                                    }
                                }
                            )
                        }

                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Info",
                                tint = if (isDarkTheme) SurfaceLight else SurfaceDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Image(
    uri: Any,
    contentDescription: String?,
    onSingleTap: () -> Unit,
    isPagerCurrentPage: Boolean,
    pagerState: PagerState,
    filteredMediaList: List<Media> // Kita akan sesuaikan pemanggilannya
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var totalDragAmount by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        val newOffset = offset + pan
                        val maxX = (scale - 1) * size.width / 2
                        val maxY = (scale - 1) * size.height / 2
                        offset = Offset(
                            x = newOffset.x.coerceIn(-maxX, maxX),
                            y = newOffset.y.coerceIn(-maxY, maxY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragAmount = 0f },
                    onDragEnd = {
                        // Logika swipe manual Anda yang sudah benar
                        if (scale <= 1f && isPagerCurrentPage) {
                            val itemCount = pagerState.pageCount
                            if (totalDragAmount > 100 && pagerState.currentPage > 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            } else if (totalDragAmount < -100 && pagerState.currentPage < itemCount - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        // === INI KUNCI PERBAIKANNYA ===
                        if (scale > 1f) {
                            // Jika di-zoom, konsumsi gestur untuk pan
                            change.consume()
                            val newOffsetX = offset.x + dragAmount
                            val maxX = (scale - 1) * size.width / 2
                            offset = Offset(x = newOffsetX.coerceIn(-maxX, maxX), y = offset.y)
                        } else {
                            // Jika tidak di-zoom, biarkan gestur untuk swipe manual
                            totalDragAmount += dragAmount
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 3f
                        offset = Offset.Zero
                    },
                    onTap = { onSingleTap() }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

