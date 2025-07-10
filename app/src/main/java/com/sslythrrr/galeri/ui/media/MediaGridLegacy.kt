package com.sslythrrr.galeri.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sslythrrr.galeri.ui.components.SectionHeader
import com.sslythrrr.galeri.ui.theme.DarkBackground
import com.sslythrrr.galeri.ui.theme.LightBackground
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MediaGridLegacy(
    modifier: Modifier = Modifier,
    sections: List<SectionItem>,
    onMediaClick: (Media) -> Unit,
    isDarkTheme: Boolean,
    selectedMedia: Set<Media> = emptySet(),
    onLongClick: ((Media) -> Unit)? = null
) {
    val lazyGridState = rememberLazyGridState()

    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(1.dp),
        modifier = modifier.background(if (isDarkTheme) DarkBackground else LightBackground)
    ) {
        items(
            items = sections,
            key = { item ->
                when (item) {
                    is SectionItem.Header -> "header_${item.title}"
                    is SectionItem.MediaItem -> "media_${item.media.id}"
                }
            },
            span = { item ->
                when (item) {
                    is SectionItem.Header -> GridItemSpan(currentLineSpan = 3)
                    is SectionItem.MediaItem -> GridItemSpan(1)
                }
            }
        ) { item ->
            when (item) {
                is SectionItem.Header -> {
                    SectionHeader(title = item.title, isDarkTheme = isDarkTheme)
                }

                is SectionItem.MediaItem -> {
                    MediaItem(
                        media = item.media,
                        onClick = onMediaClick,
                        modifier = Modifier.padding(1.dp),
                        isDarkTheme = isDarkTheme,
                        isSelected = selectedMedia.contains(item.media),
                        onLongClick = onLongClick
                    )
                }
            }
        }
    }
}