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
fun MediaGrid(
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
        columns = GridCells.Fixed(4),
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
                    is SectionItem.Header -> GridItemSpan(currentLineSpan = 4)
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

fun dateSection(mediaList: List<Media>): List<SectionItem> {
    val calendar = Calendar.getInstance()
    val today =
        calendar
            .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis

    val yesterday =
        calendar
            .apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis

    val grouped =
        mediaList.groupBy { media ->
            val mediaCalendar = Calendar.getInstance().apply { timeInMillis = media.dateTaken }
            mediaCalendar.set(Calendar.HOUR_OF_DAY, 0)
            mediaCalendar.set(Calendar.MINUTE, 0)
            mediaCalendar.set(Calendar.SECOND, 0)
            mediaCalendar.set(Calendar.MILLISECOND, 0)
            val mediaDate = mediaCalendar.timeInMillis

            when (mediaDate) {
                today -> "Hari Ini"
                yesterday -> "Kemarin"
                else -> SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(mediaDate))
            }
        }
    val sections = mutableListOf<Pair<String, List<Media>>>()
    grouped["Hari Ini"]?.let { todayMedia -> sections.add("Hari Ini" to todayMedia) }

    grouped["Kemarin"]?.let { yesterdayMedia -> sections.add("Kemarin" to yesterdayMedia) }
    sections.addAll(
        grouped
            .filterKeys { it != "Hari Ini" && it != "Kemarin" }
            .toList()
            .sortedByDescending { (_, list) -> list.first().dateTaken }
    )

    return sections.flatMap { (title, media) ->
        listOf(SectionItem.Header(title)) + media.map { SectionItem.MediaItem(it) }
    }
}

sealed class SectionItem {
    data class Header(val title: String) : SectionItem()
    data class MediaItem(val media: Media) : SectionItem()
}
