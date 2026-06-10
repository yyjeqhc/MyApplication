package com.example.myapplication.ui.common

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun LocalVideoPlayer(
    assetPath: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onPlaybackError: () -> Unit = {}
) {
    val context = LocalContext.current
    val normalizedAssetPath = remember(assetPath) { assetPath.trim().removePrefix("/") }
    val player = remember(normalizedAssetPath) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            volume = 0f
            setMediaItem(MediaItem.fromUri("asset:///$normalizedAssetPath"))
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                player.pause()
                onPlaybackError()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isPlaying) {
        player.volume = 0f
        if (isPlaying) {
            player.play()
        } else {
            player.pause()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.player = player
            }
        },
        update = { playerView ->
            playerView.player = player
        }
    )
}
