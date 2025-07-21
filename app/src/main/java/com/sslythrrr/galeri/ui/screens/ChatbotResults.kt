package com.sslythrrr.galeri.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sslythrrr.galeri.ui.media.MediaItem
import com.sslythrrr.galeri.ui.theme.*
import com.sslythrrr.galeri.viewmodel.ChatbotViewModel
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.sslythrrr.galeri.ui.components.AddToCollectionDialog
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotResults(
    onBack: () -> Unit,
    onImageClick: (Media) -> Unit,
    chatbotViewModel: ChatbotViewModel,
    mediaViewModel: MediaViewModel,
    isDarkTheme: Boolean
) {
    val allFilteredImages by chatbotViewModel.allFilteredImages.collectAsState()
    val context = LocalContext.current

    val isSelectionMode by mediaViewModel.isSelectionMode.collectAsState()
    val selectedMedia by mediaViewModel.selectedMedia.collectAsState()

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    val collections by mediaViewModel.collections.collectAsState()

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Pindahkan ke Sampah?") },
            text = { Text("Item ini akan dihapus permanen setelah 7 hari.") },
            confirmButton = {
                Button(
                    onClick = {
                        mediaViewModel.moveMediaToTrash(selectedMedia.toList(), context) {}
                        showDeleteConfirmation = false
                        mediaViewModel.clearSelection()
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
                mediaViewModel.addMediaToCollection(context, selectedMedia.toList(), collectionName)
                mediaViewModel.clearSelection()
                showCollectionDialog = false
            },
            onNewCollection = { collectionName ->
                mediaViewModel.addMediaToCollection(context, selectedMedia.toList(), collectionName)
                mediaViewModel.clearSelection()
                showCollectionDialog = false
            }
        )
    }

    val handleMediaLongClick: (Media) -> Unit = { media ->
        if (!isSelectionMode) {
            mediaViewModel.selectionMode(true)
        }
        mediaViewModel.selectingMedia(media)
    }

    val handleMediaClick: (Media) -> Unit = { media ->
        if (isSelectionMode) {
            mediaViewModel.selectingMedia(media)
        } else {
            onImageClick(media)
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
        mediaViewModel.clearSelection()
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
                        onSelectAll = {
                            val allMedia = allFilteredImages.map { mediaViewModel.run { it.toMedia() } }
                            mediaViewModel.selectingMedia(allMedia)
                        },
                        onClearSelection = { mediaViewModel.clearSelection() },
                        onDelete = { showDeleteConfirmation = true },
                        onShare = shareSelectedMedia,
                        isDarkTheme = isDarkTheme,
                        onAddToCollection = {
                            mediaViewModel.loadCollections(context)
                            showCollectionDialog = true
                        }
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                "Hasil Pencarian",
                                color = if (isDarkTheme) TextWhite else TextBlack,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Back",
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDarkTheme) DarkBackground else LightBackground)
                .padding(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(allFilteredImages, key = { it.uri }) { scannedImage ->
                val media = mediaViewModel.run { scannedImage.toMedia() }
                MediaItem(
                    media = media,
                    onClick = handleMediaClick,
                    isDarkTheme = isDarkTheme,
                    isSelected = selectedMedia.contains(media),
                    onLongClick = handleMediaLongClick
                )
            }
        }
    }
}
