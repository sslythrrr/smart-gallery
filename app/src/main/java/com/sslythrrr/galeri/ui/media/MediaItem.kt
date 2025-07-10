package com.sslythrrr.galeri.ui.media
//mt10
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.sslythrrr.galeri.ui.theme.SurfaceLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItem(
    media: Media,
    onClick: (Media) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    isSelected: Boolean = false,
    onLongClick: ((Media) -> Unit)? = null
) {
    val placeholderColor = if (isDarkTheme) Color.DarkGray else Color.LightGray
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = { onClick(media) },
                onLongClick = { onLongClick?.invoke(media) }
            )
            .background(placeholderColor)
    ) {
        val displayUri = if (media.type == MediaType.VIDEO && !media.thumbnailPath.isNullOrEmpty()) {
            File(media.thumbnailPath).toUri()
        } else {
            media.uri
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(displayUri)
                .crossfade(true)
                .size(256) // Ukuran thumbnail bisa kita standarkan
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .placeholder(placeholderColor.toArgb().toDrawable())
                .error(placeholderColor.toArgb().toDrawable())
                .build(),
            contentDescription = media.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 0.1.dp,
                    color = if (isDarkTheme) Color.Black else Color.White,
                )
        )

        if (media.type == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color(0x80000000))
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(color = Color.Blue, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.1f)
            ) {
                if (media.type == MediaType.VIDEO) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video",
                            tint = SurfaceLight,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MediaThumbnail(
    uri: Uri,
    mediaType: MediaType,
    placeholderColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.background(placeholderColor)) {
        val displayUri = remember(context, uri, mediaType) {
            if (mediaType == MediaType.VIDEO) {
                rememberVideoThumbnailUri(context, uri.hashCode().toLong(), uri)
            } else {
                uri
            }
        }
        LaunchedEffect(uri, mediaType) {
            if (mediaType == MediaType.VIDEO) {
                generateVideoThumbnailIfNeeded(context, uri.hashCode().toLong(), uri)
            }
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(displayUri)
                .crossfade(true)
                .size(200, 200)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .placeholder(placeholderColor.toArgb().toDrawable())
                .error(placeholderColor.toArgb().toDrawable())
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (mediaType == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            )
        }
    }
}

private fun rememberVideoThumbnailUri(context: Context, mediaId: Long, videoUri: Uri): Uri {
    val cacheDir = File(context.filesDir, "thumbs")
    val file = File(cacheDir, "video_thumb_$mediaId.png")

    return if (file.exists()) file.toUri() else videoUri
}

private suspend fun generateVideoThumbnailIfNeeded(context: Context, mediaId: Long, videoUri: Uri) {
    withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, "thumbs")
        val file = File(cacheDir, "video_thumb_$mediaId.png")

        if (!file.exists()) {
            try {
                cacheDir.mkdirs()
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()

                frame?.let {
                    FileOutputStream(file).use { out ->
                        it.compress(Bitmap.CompressFormat.PNG, 90, out)
                    }
                }
            } catch (e: Exception) {
                Log.e("ThumbnailHelper", "Gagal buat thumbnail video: ${e.message}")
            }
        }
    }
}