package com.sslythrrr.voe.ui.screens.mainscreen

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilePresent
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sslythrrr.voe.ui.theme.TextWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Style
import androidx.compose.runtime.collectAsState
import com.sslythrrr.voe.viewmodel.MediaViewModel

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
    viewModel: MediaViewModel
) {
    val context = LocalContext.current
    var storageInfo by remember { mutableStateOf<StorageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Modern color scheme
    val backgroundColor = if (isDarkTheme) Color(0xFF0A0A0A) else Color(0xFFFAFAFA)
    val surfaceColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFf7f2f2)
    val primaryColor = if (isDarkTheme) Color(0xFF6366F1) else Color(0xFF4F46E5)
    val textPrimary = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF111827)
    val textSecondary = if (isDarkTheme) Color(0xFFB3B3B3) else Color(0xFF6B7280)
    val borderColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE5E7EB)
    val dangerColor = Color(0xFFEF4444)
    val storageColor = if (isDarkTheme) Color(0xFF121111) else TextWhite

    LaunchedEffect(key1 = Unit) {
        isLoading = true
        viewModel.calculateCacheSize(context)

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Storage Overview Section
            StorageSection(
                isLoading = isLoading,
                storageInfo = storageInfo,
                usagePercentage = usagePercentage,
                storageColor = storageColor,
                primaryColor = primaryColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                borderColor = borderColor
            )
            ManagementToolsSection(
                navController = navController,
                onCardClick = onCardClick,
                surfaceColor = surfaceColor,
                primaryColor = primaryColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun StorageSection(
    isLoading: Boolean,
    storageInfo: StorageInfo?,
    usagePercentage: Float,
    storageColor: Color,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isLoading) {
            ModernLoadingStorageCard(
                modifier = Modifier.fillMaxWidth(),
                storageColor = storageColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                borderColor = borderColor
            )
        } else {
            ModernStorageCard(
                modifier = Modifier.fillMaxWidth(),
                storageInfo = storageInfo ?: StorageInfo(),
                usagePercentage = usagePercentage,
                storageColor = storageColor,
                primaryColor = primaryColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun ManagementToolsSection(
    navController: NavController,
    onCardClick: (String) -> Unit,
    surfaceColor: Color,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernMenuCard(
                    icon = Icons.Default.Style,
                    title = "Koleksi",
                    subtitle = "Album virtual",
                    surfaceColor = surfaceColor,
                    iconColor = Color(0xFF6366F1),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { navController.navigate("collections") }
                )
                ModernMenuCard(
                    icon = Icons.Default.FilePresent,
                    title = "Media Besar",
                    subtitle = "Grup ukuran",
                    surfaceColor = surfaceColor,
                    iconColor = Color(0xFF6366F1),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { navController.navigate("largeSizeMedia") }
                )
                ModernMenuCard(
                    icon = Icons.Default.History,
                    title = "Riwayat",
                    subtitle = "Pencarian lalu",
                    surfaceColor = surfaceColor,
                    iconColor = Color(0xFF6366F1),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { navController.navigate("searchHistory") }
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernMenuCard(
                    icon = Icons.Default.Delete,
                    title = "Sampah",
                    subtitle = "Media buangan",
                    surfaceColor = surfaceColor,
                    iconColor = Color(0xFFEF4444),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { navController.navigate("trash") }
                )
                ModernMenuCard(
                    icon = Icons.Default.Favorite,
                    title = "Media Favorit",
                    subtitle = "Koleksi pilihan",
                    surfaceColor = surfaceColor,
                    iconColor = Color(0xFF6366F1),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { navController.navigate("favoriteMedia") }
                )
                ModernMenuCard(
                    icon = Icons.Default.ContentCopy,
                    title = "Media Duplikat",
                    subtitle = "Lihat duplikat",
                    surfaceColor = surfaceColor,
                    iconColor = Color(0xFF6366F1),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onClick = { navController.navigate("duplicateMedia")  }
                )
            }
        }
    }
}

@Composable
private fun ModernLoadingStorageCard(
    modifier: Modifier = Modifier,
    storageColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = storageColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Penyimpanan Digunakan",
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(borderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    borderColor.copy(alpha = 0.3f),
                                    borderColor.copy(alpha = 0.8f),
                                    borderColor.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .shimmerEffect()
                )
            }

            Text(
                text = "Menghitung ruang penyimpanan...",
                color = textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(25.dp)
            ) {
                LegendItem(
                    color = borderColor,
                    label = "Media",
                    value = "—",
                    textPrimary = textPrimary,
                    modifier = Modifier.weight(1f)
                )
                LegendItem(
                    color = borderColor,
                    label = "Lainnya",
                    value = "—",
                    textPrimary = textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernStorageCard(
    modifier: Modifier = Modifier,
    storageInfo: StorageInfo,
    usagePercentage: Float,
    storageColor: Color,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val mediaPercentage = if (storageInfo.totalSpace > 0) {
        storageInfo.mediaSize.toFloat() / storageInfo.totalSpace.toFloat()
    } else 0f

    val otherPercentage = if (storageInfo.totalSpace > 0) {
        storageInfo.otherSize.toFloat() / storageInfo.totalSpace.toFloat()
    } else 0f

    val otherColor = Color(0xFF94A3B8)
    val trackColor = Color(0xFF334155).copy(alpha = 0.2f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = storageColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Penyimpanan Digunakan",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${(usagePercentage * 100).toInt()}%",
                    color = primaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            ModernProgressBar(
                segments = listOf(
                    ProgressSegment(mediaPercentage, primaryColor),
                    ProgressSegment(otherPercentage, otherColor)
                ),
                trackColor = trackColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatSize(storageInfo.totalSpace - storageInfo.freeSpace)} dari ${
                        formatSize(
                            storageInfo.totalSpace
                        )
                    }",
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Sisa: ${formatSize(storageInfo.freeSpace)}",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LegendItem(
                    color = primaryColor,
                    label = "Media",
                    value = formatSize(storageInfo.mediaSize),
                    textPrimary = textPrimary,
                    modifier = Modifier.weight(1f)
                )

                LegendItem(
                    color = otherColor,
                    label = "Lainnya",
                    value = formatSize(storageInfo.otherSize),
                    textPrimary = textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernProgressBar(
    segments: List<ProgressSegment>,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(trackColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            var startX = 0f

            segments.forEach { segment ->
                val segmentWidth = canvasWidth * segment.ratio
                drawRoundRect(
                    color = segment.color,
                    topLeft = Offset(startX, 0f),
                    size = Size(segmentWidth, canvasHeight),
                )
                startX += segmentWidth
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: String,
    textPrimary: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(6.dp))
        )
        Text(
            text = "$label : $value",
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ModernMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    surfaceColor: Color,
    iconColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(34.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    color = textSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// [Fungsi shimmerEffect(), getBasicStorageInfo(), getMediaSizeViaContentProvider(),
//  getCachedStorageInfo(), cacheStorageInfo(), formatSize() tetap sama seperti sebelumnya]

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