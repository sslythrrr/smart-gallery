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
import com.sslythrrr.galeri.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotResults(
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    viewModel: ChatbotViewModel,
    isDarkTheme: Boolean
) {
    val allFilteredImages by viewModel.allFilteredImages.collectAsState()
    // Kita butuh akses ke MediaViewModel untuk menggunakan fungsi konverter .toMedia()
    val mediaViewModel: MediaViewModel = viewModel()

    Scaffold(
        topBar = {
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
            // Looping List<ScannedImage>
            items(allFilteredImages, key = { it.uri }) { scannedImage ->
                // 1. Ubah ScannedImage menjadi objek Media yang lengkap
                val media = mediaViewModel.run { scannedImage.toMedia() }

                // 2. Gunakan MediaItem yang sudah pintar menangani thumbnail
                MediaItem(
                    media = media,
                    onClick = { onImageClick(media.uri.toString()) },
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}
