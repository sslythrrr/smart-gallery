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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.sslythrrr.galeri.ui.theme.GoldAccent
import com.sslythrrr.galeri.ui.theme.LightBackground
import com.sslythrrr.galeri.ui.theme.TextGray
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    context: Context,
    viewModel: MediaViewModel,
    onLoadComplete: () -> Unit,
    isDarkTheme: Boolean
) {
    val allMedia by viewModel.mediaPager.collectAsState()
    val isLoading = remember { mutableStateOf(true) }
    val isFirstInstall = remember { mutableStateOf(true) }
    val loadingText =
        listOf("setup model", "memuat media", "menghasilkan thumbnail", "menyiapkan aplikasi")
    val textIndex = remember { mutableIntStateOf(0) }

    //val backgroundColor = if (isDarkTheme) DarkBackground else LightBackground
    val backgroundColor = LightBackground

    LaunchedEffect(allMedia) {
        //if (allMedia.isNotEmpty()) {
        isFirstInstall.value = viewModel.isFirstInstall(context)
        delay(250)
        isLoading.value = false
        if (isFirstInstall.value) {
            delay(2000)
            viewModel.markFirstInstall(context)
        }
        onLoadComplete()
        //}

    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            textIndex.intValue = (textIndex.intValue + 1) % loadingText.size
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
                //painter = if (isDarkTheme) painterResource(R.drawable.ss_d) else painterResource(R.drawable.ss_l),
                painter = painterResource(R.drawable.ss_l),
                contentDescription = "App Logo",
                modifier = Modifier.size(80.dp),
                tint = Color.Unspecified,
            )

            if (isFirstInstall.value) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = loadingText[textIndex.intValue],
                    color = TextGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.width(150.dp),
                    color = GoldAccent,
                    trackColor = BlueAccent,
                    strokeCap = StrokeCap.Butt,
                )
                Spacer(modifier = Modifier.padding(bottom = 100.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    //color = if (isDarkTheme) GoldAccent else BlueAccent,
                    color = GoldAccent,
                    //trackColor = if (isDarkTheme) SurfaceLight else SurfaceDark,
                    trackColor = BlueAccent,
                    strokeCap = StrokeCap.Butt
                )
                Spacer(modifier = Modifier.padding(bottom = 80.dp))
            }
        }
    }
}