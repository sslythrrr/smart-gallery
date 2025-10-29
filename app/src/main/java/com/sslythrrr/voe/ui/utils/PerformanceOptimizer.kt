package com.sslythrrr.voe.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import okhttp3.OkHttpClient

/**
 * Optimisasi performa untuk aplikasi galeri
 */
object PerformanceOptimizer {
    
    /**
     * Membuat ImageLoader yang dioptimisasi untuk performa tinggi
     */
    @Composable
    fun rememberOptimizedImageLoader(): ImageLoader {
        val context = LocalContext.current
        
        return remember {
            ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder(context)
                        .maxSizePercent(0.25) // Gunakan 25% dari available memory
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02) // 2% dari storage untuk cache
                        .build()
                }
                .components {
                    add(VideoFrameDecoder.Factory()) // Untuk thumbnail video
                }
                .okHttpClient {
                    OkHttpClient.Builder()
                        .build()
                }
                .respectCacheHeaders(false)
                .allowHardware(true) // Gunakan hardware acceleration
                .crossfade(true)
                .crossfade(150) // Animasi crossfade singkat
                .build()
        }
    }
    
    /**
     * Optimasi untuk smooth scrolling
     */
    @Composable
    fun EnableHardwareAcceleration() {
        val view = LocalView.current
        
        DisposableEffect(view) {
            // Enable hardware acceleration untuk smooth scrolling
            view.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            
            onDispose {
                view.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
            }
        }
    }
    
    /**
     * Optimasi status bar dan navigation bar
     */
    @Composable
    fun OptimizeSystemBars() {
        val view = LocalView.current
        val context = LocalContext.current
        
        DisposableEffect(view) {
            val window = (context as? androidx.activity.ComponentActivity)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                // Optimasi untuk performa
                it.statusBarColor = android.graphics.Color.TRANSPARENT
                it.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
            
            onDispose { }
        }
    }
}

/**
 * Cache policy yang dioptimisasi untuk galeri
 */
fun getOptimizedCachePolicy(): CachePolicy {
    return CachePolicy.ENABLED
}

/**
 * Konfigurasi optimal untuk thumbnail
 */
data class ThumbnailConfig(
    val size: Int = 300, // Ukuran thumbnail optimal
    val quality: Int = 85, // Kualitas kompresi
    val format: String = "JPEG" // Format optimal
)
