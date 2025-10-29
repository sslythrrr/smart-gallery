package com.sslythrrr.voe.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sslythrrr.voe.ui.theme.BlueAccent
import com.sslythrrr.voe.ui.theme.GoldAccent
import com.sslythrrr.voe.ui.theme.SurfaceDark
import com.sslythrrr.voe.ui.theme.SurfaceLight
import com.sslythrrr.voe.ui.theme.TextGray
import com.sslythrrr.voe.ui.theme.TextGrayDark

@Composable
fun BottomNavigationBar(
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    isDarkTheme: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(if (isDarkTheme) SurfaceDark else SurfaceLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextOnlyNavItem(
                selected = currentPage == 0,
                label = "Pencarian",
                onClick = { onPageSelected(0) },
                isDarkTheme = isDarkTheme
            )

            TextOnlyNavItem(
                selected = currentPage == 1,
                label = "Galeri",
                onClick = { onPageSelected(1) },
                isDarkTheme = isDarkTheme
            )

            TextOnlyNavItem(
                selected = currentPage == 2,
                label = "Kelola",
                onClick = { onPageSelected(2) },
                isDarkTheme = isDarkTheme
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isDarkTheme) GoldAccent.copy(alpha = 0.7f) else BlueAccent.copy(
                                alpha = 0.7f
                            ),
                            if (isDarkTheme) GoldAccent else BlueAccent,
                            if (isDarkTheme) GoldAccent.copy(alpha = 0.7f) else BlueAccent.copy(
                                alpha = 0.7f
                            ),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun TextOnlyNavItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) if (isDarkTheme) GoldAccent else BlueAccent else if (isDarkTheme) TextGray else TextGrayDark,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        val width by animateDpAsState(
            targetValue = if (selected) 36.dp else 0.dp,
            animationSpec = tween(300),
            label = "indicatorWidth"
        )

        Box(
            modifier = Modifier
                .width(width)
                .height(2.dp)
                .background(
                    color = if (isDarkTheme) GoldAccent else BlueAccent,
                    shape = RoundedCornerShape(1.dp)
                )
        )
    }
}