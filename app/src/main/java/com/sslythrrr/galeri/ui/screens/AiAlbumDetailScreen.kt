package com.sslythrrr.galeri.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.media.MediaGridLegacy
import com.sslythrrr.galeri.ui.media.dateSection
import com.sslythrrr.galeri.ui.theme.*
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.sslythrrr.galeri.ui.components.AddToCollectionDialog
import com.sslythrrr.galeri.ui.screens.SelectionTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAlbumDetailScreen(
    albumName: String,
    viewModel: MediaViewModel,
    onBack: () -> Unit,
    onMediaClick: (Media) -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val pagedMedia by viewModel.pagedMedia.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    val collections by viewModel.collections.collectAsState()


    LaunchedEffect(albumName) {
        isLoading = true
        viewModel.loadMediaForAiLabel(context, albumName.lowercase())
        isLoading = false
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Pindahkan ke Sampah?") },
            text = { Text("Item ini akan dihapus permanen setelah 7 hari.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveMediaToTrash(selectedMedia.toList(), context) {
                            // Kosongkan callback jika tidak ada aksi lanjutan
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

    if (showCollectionDialog) {
        AddToCollectionDialog(
            collections = collections,
            onDismiss = { showCollectionDialog = false },
            onCollectionSelected = { collectionName ->
                viewModel.addMediaToCollection(context, selectedMedia.toList(), collectionName)
                viewModel.clearSelection()
                showCollectionDialog = false
            },
            onNewCollection = { collectionName ->
                viewModel.addMediaToCollection(context, selectedMedia.toList(), collectionName)
                viewModel.clearSelection()
                showCollectionDialog = false
            }
        )
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
                        onSelectAll = { viewModel.selectingMedia(pagedMedia) },
                        onClearSelection = { viewModel.clearSelection() },
                        onDelete = { showDeleteConfirmation = true },
                        onShare = shareSelectedMedia,
                        isDarkTheme = isDarkTheme,
                        onAddToCollection = {
                            viewModel.loadCollections(context)
                            showCollectionDialog = true
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text(albumName, color = if (isDarkTheme) TextWhite else TextBlack) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Kembali",
                                    tint = if (isDarkTheme) GoldAccent else BlueAccent
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (isDarkTheme) SurfaceDark else SurfaceLight
                        )
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
                    CircularProgressIndicator(color = if(isDarkTheme) GoldAccent else BlueAccent)
                }
            } else {
                val sections = dateSection(pagedMedia)
                MediaGridLegacy(
                    sections = sections,
                    onMediaClick = handleMediaClick,
                    isDarkTheme = isDarkTheme,
                    selectedMedia = selectedMedia,
                    onLongClick = handleMediaLongClick
                )
            }
        }
    }
}