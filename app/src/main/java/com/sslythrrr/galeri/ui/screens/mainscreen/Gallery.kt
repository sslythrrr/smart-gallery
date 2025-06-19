package com.sslythrrr.galeri.ui.screens.mainscreen
//v
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import com.sslythrrr.galeri.ui.components.SectionHeader
import com.sslythrrr.galeri.ui.media.Album
import com.sslythrrr.galeri.ui.media.Media
import com.sslythrrr.galeri.ui.media.MediaItem
import com.sslythrrr.galeri.ui.media.MediaThumbnail
import com.sslythrrr.galeri.ui.media.album.AlbumItem
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged


@Composable
fun GalleryScreen(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Media>?,
    albums: List<Album>,
    onMediaClick: (Media) -> Unit,
    onLongClick: ((Media) -> Unit)? = null,
    onAlbumClick: (Album) -> Unit,
    onSeeAllClick: () -> Unit,
    selectedMedia: Set<Media>,
    isDarkTheme: Boolean,
    viewModel: MediaViewModel
) {
    val albumPairs = remember(albums) { albums.chunked(2) }
    val recentMediaState = remember { mutableStateOf<List<Media>>(emptyList()) }

    LaunchedEffect(lazyPagingItems) {
        snapshotFlow { lazyPagingItems?.itemSnapshotList?.items ?: emptyList() }
            .distinctUntilChanged()
            .collectLatest { items ->
                if (viewModel.currentAlbum.value == null) {
                    recentMediaState.value = items.take(6)
                    viewModel.setPagedMedia(items)
                }
            }
    }
    val recentMedia = recentMediaState.value

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        modifier = modifier
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = "Terbaru", isDarkTheme = isDarkTheme)
        }
        items(
            count = minOf(recentMedia.size, 5),
            key = { index -> recentMedia[index].id }
        ) { index ->
            val media = recentMedia[index]
            MediaItem(
                media = media,
                onClick = onMediaClick,
                onLongClick = onLongClick,
                isSelected = selectedMedia.contains(media),
                modifier = Modifier.padding(1.dp),
                isDarkTheme = isDarkTheme
            )
        }
        item {
            LihatSemua(
                onClick = onSeeAllClick,
                isDarkTheme = isDarkTheme,
                backgroundMedia = recentMedia.getOrNull(5),
                modifier = Modifier.padding(1.dp)
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Semua Album", isDarkTheme = isDarkTheme)
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
                    .padding(horizontal = 4.dp)
            ) {
                AlbumItem(
                    album = albumPair[0],
                    onClick = onAlbumClick,
                    modifier = Modifier.weight(1f),
                    isDarkTheme = isDarkTheme
                )
                if (albumPair.size > 1) {
                    AlbumItem(
                        album = albumPair[1],
                        onClick = onAlbumClick,
                        modifier = Modifier.weight(1f),
                        isDarkTheme = isDarkTheme
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
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
                mediaType = media.type,
                placeholderColor = if (isDarkTheme) Color.DarkGray else Color.LightGray,
                modifier = Modifier
                    .matchParentSize()
                    .blur(radius = 4.dp)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.6f))
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

        // Overlay content (icon + text)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = "Lihat Semua",
                tint = Color.White, // Selalu putih agar kontras dengan background
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Lihat\nSemua",
                color = Color.White, // Selalu putih agar kontras
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}