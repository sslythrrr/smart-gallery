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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    mediaList: List<Media>,
    initialMediaPosition: Int,
    onBack: () -> Unit,
    onShare: (Media) -> Unit = {},
    onEdit: (Media) -> Unit = {},
    isDarkTheme: Boolean,
    albumId: String? = null,
    onDelete: (Media) -> Unit = {},
    viewModel: MediaViewModel
) {
    val filteredMediaList = remember(mediaList, albumId) {
        if (albumId != null) {
            mediaList.filter { it.albumId?.toString() == albumId }
        } else {
            mediaList.sortedByDescending { it.dateTaken }
        }
    }

    val currentMediaIndex = remember(mediaList, initialMediaPosition, albumId) {
        if (albumId != null) {
            val mediaToFind = mediaList.getOrNull(initialMediaPosition)
            filteredMediaList.indexOfFirst { it.id == mediaToFind?.id }.takeIf { it >= 0 } ?: 0
        } else {
            initialMediaPosition.coerceIn(0, filteredMediaList.size - 1)
        }
    }

    val pagerState = rememberPagerState(initialPage = currentMediaIndex) { filteredMediaList.size }
    val currentMedia by remember(pagerState.currentPage) {
        derivedStateOf { filteredMediaList.getOrNull(pagerState.currentPage) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
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

    if (showDeleteConfirmation && currentMedia != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Hapus", color = if (isDarkTheme) TextWhite else TextBlack) },
            text = {
                Text(
                    "Anda yakin ingin menghapus media ini?",
                    color = if (isDarkTheme) TextWhite else TextBlack
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentMedia?.let { media ->
                            // Soft delete instead of permanent delete
                            onDelete(media) // parameter baru yang perlu ditambah
                        }
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Pindah ke Sampah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Batal", color = if (isDarkTheme) TextGray else TextGrayDark)
                }
            },
            containerColor = if (isDarkTheme) SurfaceDark else SurfaceLight,
            tonalElevation = 8.dp
        )
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
        if (filteredMediaList.isEmpty()) {
            Text(
                "No media found",
                modifier = Modifier.align(Alignment.Center),
                color = if (isDarkTheme) TextWhite else TextBlack
            )
        } else {
            HorizontalPager(
                userScrollEnabled = true,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val media = filteredMediaList.getOrNull(page)
                MediaAction(
                    media = media,
                    isResumed = isResumed && page == pagerState.currentPage,
                    onSingleTap = {
                        if (!isVideo) {
                            showControls = !showControls
                        } else {
                            // Untuk video, single tap toggle custom controls saat pause
                            if (!isVideoPlaying) {
                                showControls = !showControls
                            }
                        }
                    },
                    isPagerCurrentPage = page == pagerState.currentPage,
                    pagerState = pagerState,
                    filteredMediaList = filteredMediaList,
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
                                viewModel.toggleFavorite(media.id)
                            }
                        ) {
                            Icon(
                                imageVector = if (viewModel.isFavorite(media.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (viewModel.isFavorite(media.id)) Color.Red else (if (isDarkTheme) GoldAccent else BlueAccent)
                            )
                        }
                        IconButton(
                            onClick = {
                                // TODO: Implement toggle archive
                                // viewModel.toggleArchive(media.path)
                            }
                        ) {
                            Icon(
                                imageVector = if (media.isArchive == true) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = "Archive",
                                tint = if (isDarkTheme) GoldAccent else BlueAccent
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

                        IconButton(onClick = { onEdit(media) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = if (isDarkTheme) SurfaceLight else SurfaceDark
                            )
                        }

                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = if (isDarkTheme) SurfaceLight else SurfaceDark
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
    filteredMediaList: List<Media>
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var totalDragAmount by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    /* LaunchedEffect(scale) {
         onZoomChange(scale > 1f)
     }*/

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    scale = (scale * gestureZoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        val newOffset = offset + pan
                        val maxX = (scale - 1) * size.width / 2
                        val maxY = (scale - 1) * size.height / 2
                        offset = Offset(
                            x = newOffset.x.coerceIn(-maxX, maxX),
                            y = newOffset.y.coerceIn(-maxY, maxY)
                        )
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragAmount = 0f },
                    onDragEnd = {
                        if (scale <= 1f && isPagerCurrentPage) {
                            if (totalDragAmount > 100 && pagerState.currentPage > 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            } else if (totalDragAmount < -100 && pagerState.currentPage < filteredMediaList.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (scale > 1f) {
                            val newOffsetX = offset.x + dragAmount
                            val maxX = (scale - 1) * size.width / 2
                            offset = Offset(
                                x = newOffsetX.coerceIn(-maxX, maxX),
                                y = offset.y
                            )
                        } else {
                            totalDragAmount += dragAmount
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        scale = if (scale == 1f) 3f else 1f
                        offset = Offset.Zero
                    },
                    onTap = { onSingleTap() }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
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

