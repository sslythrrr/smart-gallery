@file:OptIn(ExperimentalMaterial3Api::class)

package com.sslythrrr.galeri.ui.screens.management

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.media.MediaGrid
import com.sslythrrr.galeri.ui.media.SectionItem
import com.sslythrrr.galeri.ui.screens.SelectionTopBar
import com.sslythrrr.galeri.ui.theme.BlueAccent
import com.sslythrrr.galeri.ui.theme.DarkBackground
import com.sslythrrr.galeri.ui.theme.GoldAccent
import com.sslythrrr.galeri.ui.theme.LightBackground
import com.sslythrrr.galeri.ui.theme.SurfaceDark
import com.sslythrrr.galeri.ui.theme.SurfaceLight
import com.sslythrrr.galeri.ui.theme.TextBlack
import com.sslythrrr.galeri.ui.theme.TextGrayDark
import com.sslythrrr.galeri.ui.theme.TextLightGray
import com.sslythrrr.galeri.ui.theme.TextWhite
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LargeSizeMediaScreen(
    context: Context,
    onBack: () -> Unit,
    onMediaClick: (Media) -> Unit,
    viewModel: MediaViewModel,
    isDarkTheme: Boolean
) {
    val mediaPager by viewModel.mediaPager.collectAsState()
    val lazyPagingItems = mediaPager?.flow?.collectAsLazyPagingItems()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.loadAllMedia(context)
        delay(50)
    }

    LaunchedEffect(lazyPagingItems) {
        lazyPagingItems?.let { pagingItems ->
            snapshotFlow { pagingItems.itemSnapshotList.items }
                .collectLatest { items ->
                    // Filter media dengan ukuran >= 10MB
                    val filteredItems = items.filter { media ->
                        val sizeInMB = media.size / (1024 * 1024).toDouble()
                        sizeInMB >= 10
                    }
                    viewModel.setPagedMedia(filteredItems)
                    if (filteredItems.isNotEmpty()) {
                        delay(200)
                        isLoading = false
                    } else {
                        isLoading = false
                    }
                }
        }
    }

    val pagedMedia by viewModel.pagedMedia.collectAsState()
    val sections = sizeSection(pagedMedia)

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val context = LocalContext.current

    val handleMediaLongClick: (Media) -> Unit = { media ->
        if (!isSelectionMode) {
            viewModel.selectionMode(true)
        }
        viewModel.selectingMedia(media)
    }

    val handleMediaClick: (Media) -> Unit = { media ->
        if (isSelectionMode) {
            viewModel.selectingMedia(media)
        } else {
            onMediaClick(media)
        }
    }

    val shareSelectedMedia = {
        val uris = selectedMedia.map { it.uri }.toList()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan ke"))
    }

    val confirmDelete = {
        // Implementasi dialog konfirmasi hapus media
    }

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    fadeIn() + slideInVertically { -it } togetherWith
                            fadeOut() + slideOutVertically { -it }
                }
            ) { inSelectionMode ->
                if (inSelectionMode) {
                    SelectionTopBar(
                        selectedCount = selectedMedia.size,
                        onSelectAll = { viewModel.selectMedia(context) },
                        onClearSelection = { viewModel.clearSelection() },
                        onDelete = confirmDelete,
                        onShare = shareSelectedMedia,
                        isDarkTheme = isDarkTheme
                    )
                } else {
                    TopAppBar(
                        modifier = Modifier.height(48.dp),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (isDarkTheme) SurfaceDark else SurfaceLight
                        ),
                        windowInsets = WindowInsets(0),
                        title = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    "Media Berukuran Besar",
                                    color = if (isDarkTheme) TextWhite else TextBlack,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Back",
                                    tint = if (isDarkTheme) GoldAccent else BlueAccent
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) DarkBackground else LightBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = if (isDarkTheme) GoldAccent else BlueAccent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Memuat media berukuran besar...",
                        color = if (isDarkTheme) TextLightGray else TextGrayDark,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            if (sections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDarkTheme) DarkBackground else LightBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.FilePresent,
                            contentDescription = null,
                            tint = if (isDarkTheme) TextLightGray else TextGrayDark,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Tidak ada media berukuran besar",
                            color = if (isDarkTheme) TextLightGray else TextGrayDark,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(if (isDarkTheme) DarkBackground else LightBackground)
                ) {
                    MediaGrid(
                        sections = sections,
                        onMediaClick = handleMediaClick,
                        isDarkTheme = isDarkTheme,
                        selectedMedia = selectedMedia,
                        onLongClick = handleMediaLongClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

fun sizeSection(mediaList: List<Media>): List<SectionItem> {
    val grouped = mediaList.groupBy { media ->
        val sizeInBytes = media.size
        val sizeInMB = sizeInBytes / (1024 * 1024).toDouble()
        val sizeInGB = sizeInMB / 1024.0

        when {
            sizeInGB > 2 -> "> 2GB"
            sizeInGB >= 1 -> "1GB - 2GB"
            sizeInMB >= 500 -> "500MB - 1GB"
            sizeInMB >= 100 -> "100MB - 500MB"
            sizeInMB >= 10 -> "10MB - 100MB"
            else -> null
        }
    }.filterKeys { it != null }

    // Urutkan berdasarkan prioritas ukuran (terbesar dulu)
    val orderedKeys = listOf("> 2GB", "1GB - 2GB", "500MB - 1GB", "100MB - 500MB", "10MB - 100MB")
    val sections = mutableListOf<Pair<String, List<Media>>>()

    orderedKeys.forEach { key ->
        grouped[key]?.let { mediaList ->
            sections.add(key to mediaList.sortedByDescending { it.size })
        }
    }

    return sections.flatMap { (title, media) ->
        listOf(SectionItem.Header(title)) + media.map { SectionItem.MediaItem(it) }
    }
}