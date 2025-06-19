package com.sslythrrr.galeri.ui.media
//mt10
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.sslythrrr.galeri.ui.theme.SurfaceLight

@Composable
fun MediaThumbnail(
    uri: Uri,
    mediaType: MediaType,
    placeholderColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.background(placeholderColor)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .size(200, 200)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .placeholder(placeholderColor.toArgb().toDrawable())
                .error(placeholderColor.toArgb().toDrawable())
                .apply {
                    if (mediaType == MediaType.VIDEO) {
                        decoderFactory(VideoFrameDecoder.Factory())
                    }
                }
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
        MediaThumbnail(
            uri = media.uri,
            mediaType = media.type,
            placeholderColor = placeholderColor,
            modifier = Modifier.fillMaxSize()
        )

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