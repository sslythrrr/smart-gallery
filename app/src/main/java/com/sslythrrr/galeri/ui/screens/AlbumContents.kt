package com.sslythrrr.galeri.ui.screens
//v
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import com.sslythrrr.galeri.ui.media.Album
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.media.MediaGrid
import com.sslythrrr.galeri.ui.media.dateSection
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.paging.compose.collectAsLazyPagingItems
import com.sslythrrr.galeri.ui.components.AddToCollectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: Album?,
    isCollection: Boolean,
    onBack: () -> Unit,
    onMediaClick: (Media) -> Unit,
    viewModel: MediaViewModel,
    isDarkTheme: Boolean
) {
    // KITA HAPUS SEMUA LOGIKA LAMA YANG MENGGUNAKAN `pagedMedia` dan `sections`

    // BARU: Langsung kumpulkan data Pager dari ViewModel sebagai LazyPagingItems
    val lazyPagingItems = viewModel.mediaPager.collectAsState().value?.collectAsLazyPagingItems()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val context = LocalContext.current

    var showCollectionDialog by remember { mutableStateOf(false) }
    val collections by viewModel.collections.collectAsState()

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
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Pindahkan ke Sampah?") },
            text = { Text("Item ini akan dihapus permanen setelah 7 hari. Anda dapat memulihkannya dari sampah.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveMediaToTrash(selectedMedia.toList(), context){
                            viewModel.sendNavigateBackSignal()
                        }
                        showDeleteConfirmation = false
                        viewModel.clearSelection()
                        lazyPagingItems?.refresh()
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
    var collectionToDelete by remember { mutableStateOf<String?>(null) }
    if (collectionToDelete != null) {
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("Hapus Koleksi") },
            text = { Text("Yakin ingin menghapus koleksi '${collectionToDelete}'? Foto di dalamnya tidak akan ikut terhapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCollection(context, collectionToDelete!!)
                        collectionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
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
                        onDelete = {
                            if (isCollection) {
                                album?.name?.let { collectionName ->
                                    viewModel.removeMediaFromCollection(context, selectedMedia.toList(), collectionName)
                                }
                            } else {
                                showDeleteConfirmation = true
                            }
                        },
                        onShare = shareSelectedMedia,
                        isDarkTheme = isDarkTheme,
                        isSelectionInCollection = isCollection,
                        onAddToCollection = {
                            viewModel.loadCollections(context)
                            showCollectionDialog = true
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
                                    album?.name ?: "",
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
                if (lazyPagingItems != null) {
                    MediaGrid(
                        lazyPagingItems = lazyPagingItems,
                        onMediaClick = handleMediaClick,
                        isDarkTheme = isDarkTheme,
                        selectedMedia = selectedMedia,
                        onLongClick = handleMediaLongClick,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
        }
    }
}