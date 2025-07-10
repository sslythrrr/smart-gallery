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
import com.sslythrrr.galeri.ui.screens.mainscreen.formatSize
import com.sslythrrr.galeri.viewmodel.UiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Composable
fun DuplicateMediaScreen(
    context: Context,
    onBack: () -> Unit,
    onMediaClick: (Media) -> Unit,
    viewModel: MediaViewModel,
    isDarkTheme: Boolean
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val duplicateMediaList by viewModel.duplicateMedia.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadDuplicateMedia(context)
    }

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
                                    "Media Duplikat",
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
            } else if (duplicateMediaList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada media duplikat ditemukan")
                }
            } else {
                // Kelompokkan list menjadi sections duplikat
                val sections = duplicateSection(duplicateMediaList) // Panggil fungsi baru
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


fun duplicateSection(mediaList: List<Media>): List<SectionItem> {
    // Kelompokkan media berdasarkan hash-nya, abaikan jika hash-nya null
    val groupedByHash = mediaList.filter { !it.fileHash.isNullOrEmpty() }.groupBy { it.fileHash }

    return groupedByHash.values.flatMap { mediaItems ->
        // Buat header untuk setiap grup duplikat
        val headerTitle = "Duplikat (${mediaItems.size} item, ${formatSize(mediaItems.first().size)})"
        // Buat section header diikuti oleh item-item medianya
        listOf(SectionItem.Header(headerTitle)) + mediaItems.map { SectionItem.MediaItem(it) }
    }
}