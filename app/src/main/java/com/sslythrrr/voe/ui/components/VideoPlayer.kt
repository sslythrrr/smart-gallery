package com.sslythrrr.voe.ui.components

import android.graphics.Color
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sslythrrr.voe.ui.media.Media

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    media: Media,
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
    autoPlay: Boolean = false,
    onPlayerReady: ((Player) -> Unit)? = null,
    // Parameter baru:
    onPlayStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(media.uri)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = autoPlay // Tetap false initially
            }
    }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                onPlayStateChanged(isPlaying)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (autoPlay) exoPlayer.play()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    LaunchedEffect(media.uri) {
        if (exoPlayer.currentMediaItem?.localConfiguration?.uri != media.uri) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(media.uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = autoPlay
        }
    }

    LaunchedEffect(Unit) {
        onPlayerReady?.invoke(exoPlayer)
    }
    LaunchedEffect(showControls) {
        if (showControls) {
            // Ketika showControls true, artinya user mau play
            exoPlayer.play()
        }
    }

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                val playerView = PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = showControls
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setKeepContentOnPlayerReset(true)
                    controllerAutoShow = true
                    setShutterBackgroundColor(Color.TRANSPARENT)
                }
                addView(playerView)
            }
        },
        modifier = modifier,
        update = { frameLayout ->
            val playerView = frameLayout.getChildAt(0) as? PlayerView
            playerView?.apply {
                useController = showControls
                player = exoPlayer
            }
        }
    )
}