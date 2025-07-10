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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.media.MediaGrid
import com.sslythrrr.galeri.ui.screens.SelectionTopBar
import com.sslythrrr.galeri.ui.theme.*
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import com.sslythrrr.galeri.viewmodel.UiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteMediaScreen(
    context: Context,
    onBack: () -> Unit,
    onMediaClick: (Media) -> Unit,
    viewModel: MediaViewModel,
    isDarkTheme: Boolean
) {
    val pagerFlow by viewModel.mediaPager.collectAsState()
    val lazyPagingItems = pagerFlow?.collectAsLazyPagingItems()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFavoriteMedia(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelection()
        }
    }

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
                        onSelectAll = { viewModel.selectAllFavorites(context) },
                        onClearSelection = { viewModel.clearSelection() },
                        // Tombol "Delete" di sini akan berfungsi sebagai "Unfavorite"
                        onDelete = {
                            viewModel.unfavoriteSelection(context)
                            lazyPagingItems?.refresh()
                        },
                        onShare = { shareSelectedMedia() },
                        isDarkTheme = isDarkTheme,
                        onAddToCollection = {
                            viewModel.loadCollections(context)
                        }
                    )
                } else {
                    TopAppBar(
                        modifier = Modifier.height(48.dp),
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDarkTheme) SurfaceDark else SurfaceLight),
                        windowInsets = WindowInsets(0),
                        title = { Text("Media Favorit", color = if(isDarkTheme) TextWhite else TextBlack, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = {
                                viewModel.loadMedia(context)
                                onBack()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = if (isDarkTheme) GoldAccent else BlueAccent)
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        BackHandler {
            viewModel.loadMedia(context)
            onBack()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDarkTheme) DarkBackground else LightBackground)
        ) {
            if (lazyPagingItems == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (lazyPagingItems.itemCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada media favorit.")
                }
            }
            else {
                MediaGrid(
                    lazyPagingItems = lazyPagingItems,
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