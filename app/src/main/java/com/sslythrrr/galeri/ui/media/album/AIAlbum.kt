package com.sslythrrr.galeri.ui.media.album

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sslythrrr.galeri.ui.theme.BlueAccent
import com.sslythrrr.galeri.ui.theme.GoldAccent
import com.sslythrrr.galeri.ui.theme.SurfaceDark
import com.sslythrrr.galeri.ui.theme.TextBlack
import com.sslythrrr.galeri.ui.theme.TextGrayDark
import com.sslythrrr.galeri.ui.theme.TextLightGray
import com.sslythrrr.galeri.ui.theme.TextWhite

@Composable
fun AlbumCarousel(
    modifier: Modifier = Modifier
) {
    val themedAlbums = listOf(
        "Pantai", "Gunung", "Makanan", "Mobil", "Keluarga", "Arsitektur", "Hewan Peliharaan"
    )

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.padding(top = 4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(themedAlbums) { theme ->
                ThemedAlbumItem(
                    theme = theme,
                    onClick = { /* Call onAlbumClick with actual album when implemented */ }
                )
            }
        }
    }
}

@Composable
private fun ThemedAlbumItem(
    theme: String,
    onClick: () -> Unit
) {
    val backgroundGradient = getThemeDarkGradient(theme)

    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFF111111),
        border = BorderStroke(1.dp, SurfaceDark.copy(alpha = 0.2f)),
        elevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundGradient)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = getThemeIcon(theme),
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(36.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.BottomCenter)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = theme,
                    color = TextLightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

private fun getThemeDarkGradient(theme: String): Brush {
    return when (theme) {
        "Pantai" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF0A3A64),
                Color(0xFF051A31)
            )
        )

        "Gunung" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF1A4521),
                Color(0xFF071D0A)
            )
        )

        "Mobil" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF383838),
                Color(0xFF121212)
            )
        )

        "Makanan" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF7C2C13),
                Color(0xFF3D0D09)
            )
        )

        "Keluarga" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF7D5912),
                Color(0xFF3D2A09)
            )
        )

        "Arsitektur" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF2E201B),
                Color(0xFF120C0A)
            )
        )

        "Hewan Peliharaan" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF481359),
                Color(0xFF240938)
            )
        )

        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF252525),
                Color(0xFF101010)
            )
        )
    }
}

private fun getThemeIcon(theme: String): ImageVector {
    return when (theme) {
        "Pantai" -> Icons.Default.BeachAccess
        "Gunung" -> Icons.Default.Terrain
        "Mobil" -> Icons.Default.DirectionsCar
        "Makanan" -> Icons.Default.Restaurant
        "Keluarga" -> Icons.Default.People
        "Arsitektur" -> Icons.Default.Apartment
        "Hewan Peliharaan" -> Icons.Default.Pets
        else -> Icons.Default.PhotoAlbum
    }
}

@Composable
fun CollapsibleAlbumSection(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .padding(start = 5.dp)
                .clickable { isExpanded = !isExpanded }
        ) {
            if (isExpanded) {
                Icon(
                    imageVector = Icons.Default.ArrowLeft,
                    contentDescription = null,
                    tint = if (isDarkTheme) GoldAccent else BlueAccent,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Album AI",
                    color = if (isDarkTheme) TextWhite else TextBlack,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowRight,
                    contentDescription = null,
                    tint = if (isDarkTheme) TextLightGray else TextGrayDark,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Album AI",
                    color = if (isDarkTheme) TextLightGray else TextGrayDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { -it },
            exit = fadeOut(animationSpec = tween(300)) +
                    slideOutHorizontally(animationSpec = tween(300)) { -it }
        ) {
            AlbumCarousel(modifier = Modifier.fillMaxWidth())
        }
    }
}