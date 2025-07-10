package com.sslythrrr.galeri.ui.screens
//b
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sslythrrr.galeri.R
import com.sslythrrr.galeri.ui.theme.BlueAccent
import com.sslythrrr.galeri.ui.theme.DarkBackground
import com.sslythrrr.galeri.ui.theme.GoldAccent
import com.sslythrrr.galeri.ui.theme.LightBackground
import com.sslythrrr.galeri.ui.theme.SurfaceDark
import com.sslythrrr.galeri.ui.theme.SurfaceLight
import com.sslythrrr.galeri.ui.theme.TextBlack
import com.sslythrrr.galeri.ui.theme.TextGray
import com.sslythrrr.galeri.ui.theme.TextWhite
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    context: Context,
    viewModel: MediaViewModel,
    onLoadComplete: () -> Unit,
    isDarkTheme: Boolean
) {
    val backgroundColor = if (isDarkTheme) DarkBackground else LightBackground
    val isScanCompleted by viewModel.isInitialScanComplete.collectAsState()
    val hasAlreadyCompleted = remember { viewModel.hasInitialScanCompleted(context) }
    val progress by viewModel.initialScanProgress.collectAsState()
    val processedItems by viewModel.initialScanProcessedItems.collectAsState()
    val totalItems by viewModel.initialScanTotalItems.collectAsState()
    LaunchedEffect(key1 = Unit) {
        if (hasAlreadyCompleted) {
            delay(1500)
            onLoadComplete()
        } else {
            viewModel.performInitialScan(context)
        }
    }
    LaunchedEffect(isScanCompleted) {
        if (isScanCompleted) {
            onLoadComplete()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = if (isDarkTheme) painterResource(R.drawable.albumaid) else painterResource(R.drawable.albumail),
                contentDescription = "App Logo",
                modifier = Modifier.size(96.dp),
                tint = Color.Unspecified,
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (!hasAlreadyCompleted) {
                Text(
                    text = "Menyiapkan Galeri...",
                    color = if (isDarkTheme) TextWhite else TextBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$progress%",
                    color = if (isDarkTheme) GoldAccent else BlueAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.width(200.dp),
                    color = if (isDarkTheme) GoldAccent else BlueAccent,
                    trackColor = if (isDarkTheme) SurfaceDark else SurfaceLight,
                    strokeCap = StrokeCap.Round,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Memindai $processedItems dari $totalItems media",
                    color = TextGray,
                    fontSize = 14.sp,
                )
            } else {
                CircularProgressIndicator(
                    color = if (isDarkTheme) GoldAccent else BlueAccent
                )
            }
            Spacer(modifier = Modifier.padding(bottom = 100.dp))
        }
    }
}