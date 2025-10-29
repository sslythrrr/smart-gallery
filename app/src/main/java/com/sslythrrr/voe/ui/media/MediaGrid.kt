package com.sslythrrr.voe.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.sslythrrr.voe.ui.components.SectionHeader
import com.sslythrrr.voe.ui.theme.DarkBackground
import com.sslythrrr.voe.ui.theme.LightBackground
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.sslythrrr.voe.viewmodel.UiModel

@Composable
fun MediaGrid(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<UiModel>,
    onMediaClick: (Media) -> Unit,
    isDarkTheme: Boolean,
    selectedMedia: Set<Media> = emptySet(),
    onLongClick: ((Media) -> Unit)? = null
) {
    val lazyGridState = rememberLazyGridState()
    
    // Optimasi: Memoize background color
    val backgroundColor = remember(isDarkTheme) { 
        if (isDarkTheme) DarkBackground else LightBackground 
    }

    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(1.dp),
        modifier = modifier.background(backgroundColor)
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { item ->
                when (item) {
                    is UiModel.MediaItem -> "media_${item.media.id}"
                    is UiModel.SeparatorItem -> "separator_${item.date}"
                }
            },
            span = { index ->
                val item = lazyPagingItems.peek(index)
                if (item is UiModel.SeparatorItem) {
                    GridItemSpan(maxLineSpan) // Header memakan satu baris penuh
                } else {
                    GridItemSpan(1) // Item media hanya 1 kolom
                }
            }
        ) { index ->
            val item = lazyPagingItems[index]
            item?.let {
                when (it) {
                    is UiModel.MediaItem -> {
                        MediaItem(
                            media = it.media,
                            onClick = onMediaClick,
                            modifier = Modifier.padding(1.dp),
                            isDarkTheme = isDarkTheme,
                            isSelected = selectedMedia.contains(it.media),
                            onLongClick = onLongClick
                        )
                    }
                    is UiModel.SeparatorItem -> {
                        SectionHeader(
                            title = it.date,
                            isDarkTheme = isDarkTheme
                        )
                    }
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
