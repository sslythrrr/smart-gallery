package com.sslythrrr.galeri.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sslythrrr.galeri.R
import com.sslythrrr.galeri.ui.theme.DarkBackground
import com.sslythrrr.galeri.ui.theme.LightBackground
import com.sslythrrr.galeri.ui.theme.SurfaceDark
import com.sslythrrr.galeri.ui.theme.SurfaceLight
import com.sslythrrr.galeri.ui.theme.TextBlack
import com.sslythrrr.galeri.ui.theme.TextGrayDark
import com.sslythrrr.galeri.ui.theme.TextLightGray
import com.sslythrrr.galeri.ui.theme.TextWhite


@Composable
fun ContactScreen(
    isDarkTheme: Boolean,
    onBackPressed: () -> Unit
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) DarkBackground else LightBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(if (isDarkTheme) SurfaceDark else SurfaceLight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = if (isDarkTheme) TextWhite else TextBlack,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBackPressed() }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Kontak",
                color = if (isDarkTheme) TextWhite else TextBlack,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 8.dp)
        ) {
            SocialMediaLink(
                iconRes = R.drawable.gm,
                title = "an.tubagusp@gmail.com",
                onClick = { uriHandler.openUri("mailto:an.tubagusp@gmail.com") },
                isDarkTheme = isDarkTheme
            )
            SocialMediaLink(
                iconRes = R.drawable.li,
                title = "Tubagus Panji Anugrah",
                onClick = { uriHandler.openUri("https://www.linkedin.com/in/panji-anugrah/") },
                isDarkTheme = isDarkTheme
            )
            SocialMediaLink(
                iconRes = R.drawable.gh,
                title = "Sslythrrr",
                onClick = { uriHandler.openUri("https://github.com/sslythrrr") },
                isDarkTheme = isDarkTheme
            )
            SocialMediaLink(
                iconRes = R.drawable.ig,
                title = "Panji Anugrah",
                onClick = { uriHandler.openUri("https://instagram.com/tubaguspn") },
                isDarkTheme = isDarkTheme
            )
        }
    }
}
@Composable
fun SocialMediaLink(
    @DrawableRes iconRes: Int,
    title: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkTheme) DarkBackground else LightBackground)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(30.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = if (isDarkTheme) TextLightGray else TextGrayDark,
                fontWeight = FontWeight.Light,
                fontSize = 16.sp
            )
        }
    }
}