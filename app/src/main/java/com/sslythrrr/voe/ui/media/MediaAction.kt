package com.sslythrrr.voe.ui.media

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sslythrrr.voe.ui.screens.Image
import com.sslythrrr.voe.ui.theme.BlueAccent
import com.sslythrrr.voe.ui.theme.GoldAccent
import com.sslythrrr.voe.ui.theme.SurfaceDark
import com.sslythrrr.voe.ui.theme.SurfaceLight
import com.sslythrrr.voe.ui.theme.TextBlack
import com.sslythrrr.voe.ui.theme.TextWhite
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaAction(
    media: Media?,
    onSingleTap: () -> Unit,
    isPagerCurrentPage: Boolean,
    pagerState: PagerState,
    filteredMediaList: List<Media>
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            media == null -> Text("Media not found", color = TextWhite)

            else -> Image(
                uri = media.uri,
                contentDescription = media.title,
                onSingleTap = onSingleTap,
                isPagerCurrentPage = isPagerCurrentPage,
                pagerState = pagerState
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaNavbar(
    media: Media?,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    if (media == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDarkTheme) SurfaceDark else SurfaceLight,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Detail Informasi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) GoldAccent else BlueAccent,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (isDarkTheme) GoldAccent else BlueAccent
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = if (isDarkTheme) GoldAccent.copy(alpha = 0.5f) else BlueAccent.copy(alpha = 0.5f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                InfoRow(label = "Judul", value = media.title, isDarkTheme = isDarkTheme)

                val sizeInMB = media.size / 1000000.0
                val df = DecimalFormat("#.###")
                val formattedSize = df.format(sizeInMB) + " MB"
                InfoRow(label = "Ukuran", value = formattedSize, isDarkTheme = isDarkTheme)

                InfoRow(
                    label = "Album",
                    value = media.albumName ?: "N/A",
                    isDarkTheme = isDarkTheme
                )
                InfoRow(label = "Lokasi", value = media.relativePath, isDarkTheme = isDarkTheme)

                val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
                val formattedDate = if (media.dateTaken > 0) {
                    dateFormat.format(Date(media.dateTaken))
                } else {
                    "Tidak diketahui"
                }
                InfoRow(label = "Tanggal", value = formattedDate, isDarkTheme = isDarkTheme)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isDarkTheme: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isDarkTheme) TextWhite.copy(alpha = 0.8f) else TextBlack.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(80.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            color = if (isDarkTheme) TextWhite else TextBlack,
            fontWeight = FontWeight.Thin,
            overflow = TextOverflow.Ellipsis
        )
    }
}