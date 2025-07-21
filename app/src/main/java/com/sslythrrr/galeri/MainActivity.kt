package com.sslythrrr.galeri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sslythrrr.galeri.navigation.Navigation
import com.sslythrrr.galeri.ui.theme.SmartGalleryTheme
import com.sslythrrr.galeri.ui.utils.Notification
import com.sslythrrr.galeri.ui.utils.PermissionUtils
import com.sslythrrr.galeri.viewmodel.ChatbotViewModel
import com.sslythrrr.galeri.viewmodel.MediaViewModel
import com.sslythrrr.galeri.viewmodel.ThemeViewModel
import com.sslythrrr.galeri.viewmodel.factory.MediaFactory
import com.sslythrrr.galeri.viewmodel.factory.ThemeFactory
import com.sslythrrr.galeri.worker.LocationRetryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var mediaViewModel: MediaViewModel
    private val viewModel: MediaViewModel by viewModels {
        MediaFactory(application)
    }
    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeFactory(applicationContext)
    }
    private lateinit var hasPermissionsState: MutableState<Boolean>
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            viewModel.loadMedia(this)
            viewModel.startScanning(this@MainActivity)
            hasPermissionsState.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasPermissionsState = mutableStateOf(PermissionUtils.hasRequiredPermissions(this))
        if (!hasPermissionsState.value) {
            requestPermissions()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            LocationRetryManager.checkAndRetryLocationFetch(this@MainActivity)
        }

        setContent {
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

            SmartGalleryTheme(
                darkTheme = isDarkTheme
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .systemBarsPadding()
                ) {
                    LaunchedEffect(key1 = Unit) {
                        if (hasPermissionsState.value) {
                            viewModel.loadMedia(this@MainActivity)
                            viewModel.startScanning(this@MainActivity)
                        }
                    }
                    if (hasPermissionsState.value) {
                        val chatbotViewModel: ChatbotViewModel = viewModel()
                        Navigation(viewModel, themeViewModel, chatbotViewModel = chatbotViewModel)
                    }
                }
            }
        }


        viewModel.registerContentObservers(applicationContext)
        viewModel.loadMedia(applicationContext)

        Notification.createNotificationChannel(applicationContext)
    }

    override fun onStart() {
        super.onStart()
        if (PermissionUtils.hasRequiredPermissions(this)) {
            viewModel.registerContentObservers(this)
            viewModel.syncDatabaseWithMediaStore(this)
            viewModel.startScanning(this)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.unregisterContentObservers(this)
    }

    override fun onDestroy() {
        mediaViewModel.unregisterContentObservers(applicationContext)
        super.onDestroy()
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(PermissionUtils.getRequiredPermissions())
    }
}