package com.sslythrrr.voe.ui.screens.mainscreen
//v
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import com.sslythrrr.voe.ui.components.SectionHeader
import com.sslythrrr.voe.ui.media.Album
import com.sslythrrr.voe.ui.media.Media
import com.sslythrrr.voe.ui.media.MediaThumbnail
import com.sslythrrr.voe.ui.media.album.AlbumItem
import com.sslythrrr.voe.viewmodel.MediaViewModel
import com.sslythrrr.voe.viewmodel.UiModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<UiModel>?,
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    isDarkTheme: Boolean,
    viewModel: MediaViewModel,
    navController: NavController
) {
    val albumPairs = remember(albums) { albums.chunked(2) }
    val recentMediaState = remember { mutableStateOf<Media?>(null) }
    val aiAlbums by viewModel.aiAlbums.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadAiAlbums(context)
    }
    LaunchedEffect(lazyPagingItems?.itemCount) {
        if (lazyPagingItems != null && lazyPagingItems.itemCount > 0) {
            val firstMediaItem =
                (0 until lazyPagingItems.itemCount).firstNotNullOfOrNull { lazyPagingItems.peek(it) as? UiModel.MediaItem }
            recentMediaState.value = firstMediaItem?.media
        }
    }
    val backgroundMedia = recentMediaState.value
    val recentMedia = recentMediaState.value
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshAllData(context) },
        state = pullRefreshState
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            modifier = modifier
        ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionHeader(title = "Album AI", isDarkTheme = isDarkTheme)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (aiAlbums.isEmpty()) {
                            item {
                                EmptyAiAlbumPlaceholder(isDarkTheme = isDarkTheme)
                            }
                        } else {
                            items(count = aiAlbums.size) { index ->
                                val album = aiAlbums[index]
                                AlbumItem(
                                    album = album,
                                    onClick = {
                                        navController.navigate("aiAlbumDetail/${album.name}")
                                    },
                                    modifier = Modifier.size(120.dp),
                                    isDarkTheme = isDarkTheme,
                                    isAiAlbum = true
                                )
                            }
                        }
                    }
                }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = "Semua Album", isDarkTheme = isDarkTheme)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            items(
                count = albumPairs.size,
                span = { GridItemSpan(maxLineSpan) }
            ) { index ->
                val albumPair = albumPairs[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                ) {
                    if (albumPair[0].id == -1L) {
                        LihatSemua(
                            onClick = { onAlbumClick(albumPair[0]) },
                            isDarkTheme = isDarkTheme,
                            backgroundMedia = backgroundMedia,
                            modifier = Modifier
                                .weight(1f)
                                .padding(1.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp,
                                        bottomEnd = 8.dp,
                                        bottomStart = 8.dp
                                    )
                                )
                        )
                    } else {
                        AlbumItem(
                            album = albumPair[0],
                            onClick = onAlbumClick,
                            modifier = Modifier.weight(1f),
                            isDarkTheme = isDarkTheme
                        )
                    }
                    if (albumPair.size > 1) {
                        if (albumPair[1].id == -1L) {
                            LihatSemua(
                                onClick = { onAlbumClick(albumPair[1]) },
                                isDarkTheme = isDarkTheme,
                                backgroundMedia = backgroundMedia,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(1.dp)
                            )
                        } else {
                            AlbumItem(
                                album = albumPair[1],
                                onClick = onAlbumClick,
                                modifier = Modifier.weight(1f),
                                isDarkTheme = isDarkTheme
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}


@Composable
fun LihatSemua(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    backgroundMedia: Media? = null
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        backgroundMedia?.let { media ->
            MediaThumbnail(
                uri = media.uri,
                placeholderColor = if (isDarkTheme) Color.DarkGray else Color.LightGray,
                modifier = Modifier
                    .matchParentSize()
                    .blur(radius = 4.dp)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(if (isDarkTheme) Color.Black.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.5f))
            )
        }
        if (backgroundMedia == null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = if (isDarkTheme) Color.Black else Color.White
                    )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = "Lihat Semua",
                tint = Color.White,
                modifier = Modifier.size(53.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Lihat\nSemua",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun EmptyAiAlbumPlaceholder(isDarkTheme: Boolean) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (isDarkTheme) Color(0xFFFFD700) else Color(0xFF2196F3),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isDarkTheme) Color.Black.copy(alpha = 0.3f)
                else Color.Gray.copy(alpha = 0.1f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy, // atau icon AI lainnya
                contentDescription = "AI Album Placeholder",
                tint = if (isDarkTheme) Color.Gray else Color.DarkGray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Memuat Album AI\nRefresh berkala\nTarik kebawah",
                color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                lineHeight = 14.sp,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}