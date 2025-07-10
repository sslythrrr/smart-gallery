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
// GANTI MediaGrid DENGAN MediaGridLegacy
import com.sslythrrr.galeri.ui.media.MediaGridLegacy
import com.sslythrrr.galeri.ui.media.dateSection
import com.sslythrrr.galeri.ui.theme.*
import com.sslythrrr.galeri.viewmodel.MediaViewModel

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

    LaunchedEffect(albumName) {
        isLoading = true
        viewModel.loadMediaForAiLabel(context, albumName.lowercase())
        isLoading = false
    }

    Scaffold(
        topBar = {
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
                // PANGGIL MediaGridLegacy DI SINI
                MediaGridLegacy(
                    sections = sections,
                    onMediaClick = onMediaClick,
                    isDarkTheme = isDarkTheme,
                )
            }
        }
    }
}