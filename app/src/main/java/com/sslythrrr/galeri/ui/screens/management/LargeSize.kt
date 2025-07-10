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
import androidx.core.net.toUri
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
import androidx.paging.filter
import com.sslythrrr.galeri.data.AppDatabase
import com.sslythrrr.galeri.data.entity.ScannedImage
import com.sslythrrr.galeri.ui.media.MediaGridLegacy
import com.sslythrrr.galeri.ui.media.MediaType
import com.sslythrrr.galeri.viewmodel.UiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Composable
fun LargeSizeMediaScreen(
    context: Context,
    onBack: () -> Unit,
    onMediaClick: (Media) -> Unit,
    viewModel: MediaViewModel,
    isDarkTheme: Boolean
) {
    var isLoading by remember { mutableStateOf(true) }
    var largeMediaList by remember { mutableStateOf<List<Media>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        // Ambil data dari database yang sudah punya thumbnail path
        val allMediaFromDb = withContext(Dispatchers.IO) {
             AppDatabase.getInstance(context).scannedImageDao().getAllScannedImages()
        }
        largeMediaList = allMediaFromDb.filter {
            val sizeInMB = it.ukuran / (1024.0 * 1024.0)
            sizeInMB >= 10
        }.map { scannedImage ->
            // Ubah dari ScannedImage (DB) ke Media (UI)
            scannedImage.toMedia()
        }
        isLoading = false
    }
    val pagedMedia by viewModel.pagedMedia.collectAsState()
    val sections = sizeSection(pagedMedia)

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()

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
                        isDarkTheme = isDarkTheme,
                        onAddToCollection = {
                            viewModel.loadCollections(context)
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDarkTheme) DarkBackground else LightBackground)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (largeMediaList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada media berukuran besar.")
                }
            } else {
                // Kelompokkan list menjadi sections
                val sections = sizeSection(largeMediaList)
                // Gunakan MediaGridLegacy yang menerima List<SectionItem>
                MediaGridLegacy(
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

private fun ScannedImage.toMedia(): Media {
    return Media(
        id = this.uri.hashCode().toLong(),
        title = this.nama,
        uri = this.uri.toUri(),
        type = if (this.type.startsWith("video")) MediaType.VIDEO else MediaType.IMAGE,
        albumId = this.album.hashCode().toLong(),
        albumName = this.album,
        dateTaken = this.tanggal,
        dateAdded = this.tanggal,
        size = this.ukuran,
        relativePath = this.path,
        thumbnailPath = this.thumbnailPath, // <-- Path thumbnail dibawa
        isFavorite = this.isFavorite,
        width = this.resolusi.substringBefore("x").toIntOrNull() ?: 0,
        height = this.resolusi.substringAfter("x").toIntOrNull() ?: 0
    )
}