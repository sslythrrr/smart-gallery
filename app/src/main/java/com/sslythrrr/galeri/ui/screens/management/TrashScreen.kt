package com.sslythrrr.galeri.ui.screens.management

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.media.MediaItem
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import com.sslythrrr.galeri.viewmodel.UiModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    viewModel: MediaViewModel,
    onBack: () -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val pagerFlow by viewModel.mediaPager.collectAsState()
    val lazyPagingItems = pagerFlow?.collectAsLazyPagingItems()

    // State untuk memunculkan BottomSheet dan item yang dipilih
    var selectedMediaForAction by remember { mutableStateOf<Media?>(null) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // State untuk dialog konfirmasi hapus permanen
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Memuat data sampah saat layar pertama kali dibuka
    LaunchedEffect(Unit) {
        viewModel.loadTrashedMedia(context)
    }

    val onRestoreClick: (Media) -> Unit = { media ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            viewModel.restoreMediaFromTrash(listOf(media), context)
            // JANGAN panggil loadMedia. Cukup refresh Pager yang ada.
            lazyPagingItems?.refresh()
            selectedMediaForAction = null
        }
    }

    val onDeletePermanentClick: (Media) -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            showDeleteConfirmDialog = true
        }
    }

    // Jika ada media yang dipilih, tampilkan bottom sheet
    if (selectedMediaForAction != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMediaForAction = null },
            sheetState = sheetState,
        ) {
            // Konten BottomSheet
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("Pulihkan") },
                    leadingContent = { Icon(Icons.Default.Restore, contentDescription = "Pulihkan") },
                    modifier = Modifier.clickable { onRestoreClick(selectedMediaForAction!!) }
                )
                ListItem(
                    headlineContent = { Text("Hapus Permanen", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = "Hapus Permanen", tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { onDeletePermanentClick(selectedMediaForAction!!) }
                )
            }
        }
    }

    // Dialog konfirmasi untuk hapus permanen
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                selectedMediaForAction = null
            },
            title = { Text("Hapus Permanen?") },
            text = { Text("Tindakan ini tidak dapat diurungkan. Media akan dihapus selamanya dari perangkat Anda.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedMediaForAction?.let {
                            viewModel.deleteMediaPermanently(context, listOf(it))
                            lazyPagingItems?.refresh()
                        }
                        showDeleteConfirmDialog = false
                        selectedMediaForAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    selectedMediaForAction = null
                }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sampah") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.loadMedia(context)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Handle Back press agar kembali ke galeri utama
        BackHandler {
            viewModel.loadMedia(context)
            onBack()
        }

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (lazyPagingItems == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (lazyPagingItems.itemCount == 0) {
                Text("Sampah kosong", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(1.dp)
                ) {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = { index ->
                            val item = lazyPagingItems.peek(index)
                            (item as? UiModel.MediaItem)?.media?.id ?: "item_$index"
                        }
                    ) { index ->
                        val uiModel = lazyPagingItems[index]
                        if (uiModel is UiModel.MediaItem) {
                            MediaItem(
                                media = uiModel.media,
                                onClick = { /* Klik biasa bisa untuk preview nanti */ },
                                isDarkTheme = isDarkTheme,
                                onLongClick = {
                                    selectedMediaForAction = uiModel.media
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}