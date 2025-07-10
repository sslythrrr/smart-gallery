package com.sslythrrr.galeri.ui.screens.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sslythrrr.galeri.data.entity.SearchHistory
import com.sslythrrr.galeri.viewmodel.HistoryViewModel
import com.sslythrrr.galeri.viewmodel.factory.HistoryFactory
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import com.sslythrrr.galeri.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHistoryScreen(
    onBackPressed: () -> Unit,
    onHistoryClick: (String) -> Unit,
    isDarkTheme: Boolean
) {
    val application = LocalContext.current.applicationContext as Application
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryFactory(application)
    )

    val historyList by historyViewModel.searchHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Pencarian", color = if(isDarkTheme) TextWhite else TextBlack) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = if(isDarkTheme) GoldAccent else BlueAccent)
                    }
                },
                actions = {
                    IconButton(onClick = { historyViewModel.clearAllHistory() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Bersihkan Riwayat", tint = if(isDarkTheme) GoldAccent else BlueAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) SurfaceDark else SurfaceLight
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(if (isDarkTheme) DarkBackground else LightBackground)
        ) {
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada riwayat pencarian.", color = if (isDarkTheme) TextGray else TextGrayDark)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(historyList, key = { it.id }) { historyItem ->
                        HistoryItem(
                            history = historyItem,
                            onClick = { onHistoryClick(historyItem.query) },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    history: SearchHistory,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = "History Icon",
            modifier = Modifier.size(24.dp),
            tint = if (isDarkTheme) TextGray else TextGrayDark
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = history.query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isDarkTheme) TextLightGray else TextBlack,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}