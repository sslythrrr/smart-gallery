package com.sslythrrr.voe.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sslythrrr.voe.ui.theme.BlueAccent
import com.sslythrrr.voe.ui.theme.CardDark
import com.sslythrrr.voe.ui.theme.DarkBackground
import com.sslythrrr.voe.ui.theme.GoldAccent
import com.sslythrrr.voe.ui.theme.GoldAccentDark
import com.sslythrrr.voe.ui.theme.LightBackground
import com.sslythrrr.voe.ui.theme.SurfaceDark
import com.sslythrrr.voe.ui.theme.SurfaceLight
import com.sslythrrr.voe.ui.theme.TextBlack
import com.sslythrrr.voe.ui.theme.TextGray
import com.sslythrrr.voe.ui.theme.TextGrayDark
import com.sslythrrr.voe.ui.theme.TextWhite

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onAboutClick: () -> Unit,
    onContactClick: () -> Unit
) {
    val scrollState = rememberScrollState()

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
                text = "Pengaturan",
                color = if (isDarkTheme) TextWhite else TextBlack,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 16.dp)
        ) {
            ToggleSetting(
                icon = Icons.Default.DarkMode,
                title = "Mode Gelap",
                subtitle = if (isDarkTheme) "Aktif" else "Nonaktif",
                isChecked = isDarkTheme,
                onCheckedChange = { onThemeChange(it) },
                isDarkTheme = isDarkTheme
            )
            ClickableSetting(
                icon = Icons.Default.Language,
                title = "Bahasa",
                subtitle = "Bahasa Indonesia",
                onClick = { /* Language selector */ },
                isDarkTheme = isDarkTheme
            )
            ClickableSetting(
                icon = Icons.Default.Contacts,
                title = "Hubungi",
                subtitle = "Media sosial, dan email",
                onClick = { onContactClick() },
                isDarkTheme = isDarkTheme
            )
            ClickableSetting(
                icon = Icons.Default.Info,
                title = "Tentang",
                subtitle = "Versi, fitur, dan informasi lainnya",
                onClick = { onAboutClick() },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
fun ToggleSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkTheme) CardDark else SurfaceLight)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDarkTheme) GoldAccent else BlueAccent,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    color = if (isDarkTheme) TextWhite else TextBlack,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )

                Text(
                    text = subtitle,
                    color = if (isDarkTheme) TextGray else TextGrayDark,
                    fontSize = 14.sp
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GoldAccent,
                checkedTrackColor = GoldAccentDark,
                uncheckedThumbColor = if (isDarkTheme) TextGray else TextGrayDark,
                uncheckedTrackColor = if (isDarkTheme) SurfaceDark else SurfaceLight
            )
        )
    }
}

@Composable
fun ClickableSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkTheme) CardDark else SurfaceLight)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isDestructive) Color.Red else if (isDarkTheme) GoldAccent else BlueAccent,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                color = if (isDestructive) Color.Red else if (isDarkTheme) TextWhite else TextBlack,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )

            Text(
                text = subtitle,
                color = if (isDarkTheme) TextGray else TextGrayDark,
                fontSize = 14.sp
            )
        }
    }
}

