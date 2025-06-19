package com.sslythrrr.galeri.ui.screens.mainscreen

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sslythrrr.galeri.ui.theme.DarkBackground
import com.sslythrrr.galeri.ui.theme.LightBackground
import com.sslythrrr.galeri.ui.theme.SearchBarBackground
import com.sslythrrr.galeri.ui.theme.TextWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

data class StorageInfo(
    val totalSpace: Long = 0,
    val freeSpace: Long = 0,
    val mediaSize: Long = 0,
    val otherSize: Long = 0
)

data class ProgressSegment(
    val ratio: Float,
    val color: Color
)

private const val PREFS_NAME = "storage_prefs"
private const val KEY_TOTAL_SPACE = "total_space"
private const val KEY_FREE_SPACE = "free_space"
private const val KEY_MEDIA_SIZE = "media_size"
private const val KEY_OTHER_SIZE = "other_size"
private const val KEY_LAST_UPDATE = "last_update"
private const val CACHE_EXPIRY_MS = 3600000L

@Composable
fun Management(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    onCardClick: (String) -> Unit = {},
    navController: NavController,
) {
    val context = LocalContext.current
    var storageInfo by remember { mutableStateOf<StorageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val backgroundColor = if (isDarkTheme) DarkBackground else LightBackground
    val cardColor = if (isDarkTheme) SearchBarBackground else Color.White
    val textColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF121212)
    val secondaryTextColor = if (isDarkTheme) Color(0xFFAAAAAA) else Color(0xFF666666)
    val accentColor = if (isDarkTheme) Color(0xFFFFD700) else Color(0xFF2196F3)
    val borderColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)
    val progressTrackColor = if (isDarkTheme) Color(0xFF333333) else Color(0xFFE0E0E0)

    LaunchedEffect(key1 = Unit) {
        isLoading = true

        val cachedInfo = getCachedStorageInfo(context)
        if (cachedInfo != null) {
            storageInfo = cachedInfo
            isLoading = false
        }

        val freshInfo = withContext(Dispatchers.IO) {
            val basicInfo = getBasicStorageInfo()

            if (cachedInfo == null) {
                storageInfo = basicInfo
                isLoading = false
            }

            val mediaSize = async { getMediaSizeViaContentProvider(context) }

            val completeInfo = StorageInfo(
                totalSpace = basicInfo.totalSpace,
                freeSpace = basicInfo.freeSpace,
                mediaSize = mediaSize.await(),
                otherSize = basicInfo.totalSpace - basicInfo.freeSpace - mediaSize.await()
            )

            cacheStorageInfo(context, completeInfo)
            completeInfo
        }

        storageInfo = freshInfo
        isLoading = false
    }

    val usedSpace = storageInfo?.let { it.totalSpace - it.freeSpace } ?: 0L
    val usagePercentage = storageInfo?.let {
        if (it.totalSpace > 0) usedSpace.toFloat() / it.totalSpace.toFloat() else 0f
    } ?: 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    LoadingStorageCard(
                        modifier = Modifier.weight(3f),
                        cardColor = cardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        borderColor = borderColor,
                        storageColor = cardColor
                    )
                } else {
                    StorageUsageCard(
                        modifier = Modifier.weight(3f),
                        storageInfo = storageInfo ?: StorageInfo(),
                        usagePercentage = usagePercentage,
                        storageColor = cardColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        progressTrackColor = progressTrackColor
                    )
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onCardClick("Sampah") },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF4D4D)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sampah",
                            tint = TextWhite,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sampah",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ActionItem(
                        icon = Icons.Default.CleaningServices,
                        title = "Bersihkan Media",
                        subtitle = "45 berkas dapat dibersihkan",
                        iconTint = accentColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onClick = { onCardClick("Bersihkan Media") }
                    )

                    Divider(color = borderColor)

                    ActionItem(
                        icon = Icons.Default.ContentCopy,
                        title = "Cek Duplikat",
                        iconTint = accentColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onClick = { onCardClick("Cek Duplikat") }
                    )

                    Divider(color = borderColor)

                    ActionItem(
                        icon = Icons.Default.FilePresent,
                        title = "Media Berukuran Besar",
                        iconTint = accentColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onClick = {
                            navController.navigate("largeSizeMedia")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuCard(
                        icon = Icons.Default.VideoFile,
                        title = "Berkas Video",
                        backgroundColor = cardColor,
                        iconTint = accentColor,
                        textColor = textColor,
                        borderColor = borderColor,
                        onClick = { navController.navigate("videoFiles") }
                    )

                    MenuCard(
                        icon = Icons.Default.Description,
                        title = "Media Dokumen",
                        backgroundColor = cardColor,
                        iconTint = accentColor,
                        textColor = textColor,
                        borderColor = borderColor,
                        onClick = { onCardClick("Media Dokumen") }
                    )

                    MenuCard(
                        icon = Icons.Default.Archive,
                        title = "Media Arsip",
                        backgroundColor = cardColor,
                        iconTint = accentColor,
                        textColor = textColor,
                        borderColor = borderColor,
                        onClick = { onCardClick("Media Arsip") }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuCard(
                        icon = Icons.Default.Create,
                        title = "Buat Koleksi",
                        backgroundColor = cardColor,
                        iconTint = accentColor,
                        textColor = textColor,
                        borderColor = borderColor,
                        onClick = { onCardClick("Buat Koleksi") }
                    )

                    MenuCard(
                        icon = Icons.Default.Star,
                        title = "Media Favorit",
                        backgroundColor = cardColor,
                        iconTint = accentColor,
                        textColor = textColor,
                        borderColor = borderColor,
                        onClick = { navController.navigate("favoriteMedia") }
                    )

                    MenuCard(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = "Riwayat AI",
                        backgroundColor = cardColor,
                        iconTint = accentColor,
                        textColor = textColor,
                        borderColor = borderColor,
                        onClick = { onCardClick("Riwayat AI") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LoadingStorageCard(
    modifier: Modifier = Modifier,
    cardColor: Color,
    textColor: Color,
    storageColor: Color,
    secondaryTextColor: Color,
    borderColor: Color
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Penyimpanan Digunakan",
                    color = storageColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(borderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    borderColor.copy(alpha = 0.4f),
                                    borderColor.copy(alpha = 0.7f),
                                    borderColor.copy(alpha = 0.4f)
                                )
                            )
                        )
                        .shimmerEffect()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Menghitung ruang penyimpanan...",
                color = secondaryTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Divider(color = borderColor)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(borderColor, RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Media",
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(borderColor, RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lainnya",
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StorageUsageCard(
    modifier: Modifier = Modifier,
    storageInfo: StorageInfo,
    usagePercentage: Float,
    storageColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    borderColor: Color,
    progressTrackColor: Color
) {
    val mediaPercentage = if (storageInfo.totalSpace > 0) {
        storageInfo.mediaSize.toFloat() / storageInfo.totalSpace.toFloat()
    } else {
        0f
    }

    val otherPercentage = if (storageInfo.totalSpace > 0) {
        storageInfo.otherSize.toFloat() / storageInfo.totalSpace.toFloat()
    } else {
        0f
    }

    val otherStorageColor = Color(0xFF8E8E8E)

    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = storageColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Penyimpanan Digunakan",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${(usagePercentage * 100).toInt()}%",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MultiSegmentProgressBar(
                segments = listOf(
                    ProgressSegment(mediaPercentage, accentColor),
                    ProgressSegment(otherPercentage, otherStorageColor),
                    ProgressSegment(1f - mediaPercentage - otherPercentage, progressTrackColor)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatSize(storageInfo.totalSpace - storageInfo.freeSpace)} dari ${
                        formatSize(
                            storageInfo.totalSpace
                        )
                    }",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Sisa: ${formatSize(storageInfo.freeSpace)}",
                    color = secondaryTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider(color = borderColor)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accentColor, RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Media",
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatSize(storageInfo.mediaSize),
                        color = secondaryTextColor,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(otherStorageColor, RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lainnya",
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatSize(storageInfo.otherSize),
                        color = secondaryTextColor,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MultiSegmentProgressBar(
    segments: List<ProgressSegment>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            var startX = 0f

            segments.forEach { segment ->
                val segmentWidth = canvasWidth * segment.ratio
                drawRect(
                    color = segment.color,
                    topLeft = Offset(startX, 0f),
                    size = Size(segmentWidth, size.height)
                )
                startX += segmentWidth
            }
        }
    }
}

@Composable
fun Divider(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

@Composable
fun ActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color,
    textColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = secondaryTextColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun MenuCard(
    icon: ImageVector,
    title: String,
    backgroundColor: Color,
    iconTint: Color,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var transition by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    LaunchedEffect(Unit) {
        transition = true
    }

    graphicsLayer(alpha = if (transition) alpha else 0f)
}

private fun getBasicStorageInfo(): StorageInfo {
    return try {
        val externalStoragePath = Environment.getExternalStorageDirectory().path
        val stat = StatFs(externalStoragePath)

        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalSpace = totalBlocks * blockSize
        val freeSpace = availableBlocks * blockSize

        StorageInfo(
            totalSpace = totalSpace,
            freeSpace = freeSpace,
            mediaSize = 0L,
            otherSize = 0L
        )
    } catch (_: Exception) {
        StorageInfo(
            totalSpace = 128L * 1024L * 1024L * 1024L,
            freeSpace = 86L * 1024L * 1024L * 1024L,
            mediaSize = 30L * 1024L * 1024L * 1024L,
            otherSize = 12L * 1024L * 1024L * 1024L
        )
    }
}

private suspend fun getMediaSizeViaContentProvider(context: Context): Long {
    return withContext(Dispatchers.IO) {
        var mediaSize = 0L
        try {
            val projection = arrayOf(MediaStore.MediaColumns.SIZE)
            val resolver = context.contentResolver

            val mediaTypes = listOf(
                Pair(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null),
                Pair(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null),
                Pair(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null),
                Pair(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null)
            )

            for ((uri, selection) in mediaTypes) {
                resolver.query(uri, projection, selection, null, null)?.use { cursor ->
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    while (cursor.moveToNext()) {
                        val size = cursor.getLong(sizeColumn)
                        mediaSize += size
                    }
                }
            }
            mediaSize
        } catch (_: Exception) {
            30L * 1024L * 1024L * 1024L
        }
    }
}

private fun getCachedStorageInfo(context: Context): StorageInfo? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)
    val currentTime = System.currentTimeMillis()

    if (currentTime - lastUpdate > CACHE_EXPIRY_MS) {
        return null
    }

    return StorageInfo(
        totalSpace = prefs.getLong(KEY_TOTAL_SPACE, 0),
        freeSpace = prefs.getLong(KEY_FREE_SPACE, 0),
        mediaSize = prefs.getLong(KEY_MEDIA_SIZE, 0),
        otherSize = prefs.getLong(KEY_OTHER_SIZE, 0)
    )
}

private fun cacheStorageInfo(context: Context, info: StorageInfo) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
        putLong(KEY_TOTAL_SPACE, info.totalSpace)
        putLong(KEY_FREE_SPACE, info.freeSpace)
        putLong(KEY_MEDIA_SIZE, info.mediaSize)
        putLong(KEY_OTHER_SIZE, info.otherSize)
        putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
        apply()
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()

    return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + units[digitGroups]
}