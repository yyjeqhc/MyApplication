package com.example.myapplication.ui.common

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.AssetDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.myapplication.R

private const val LOCAL_VIDEO_PLAYER_TAG = "LocalVideoPlayer"

@OptIn(UnstableApi::class)
@Composable
fun LocalVideoPlayer(
    assetPath: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onBufferingChanged: (Boolean) -> Unit = {},
    onReadyChanged: (Boolean) -> Unit = {},
    onFirstFrameRendered: () -> Unit = {},
    onPlaybackError: () -> Unit = {}
) {
    val context = LocalContext.current
    val latestOnBufferingChanged = rememberUpdatedState(onBufferingChanged)
    val latestOnReadyChanged = rememberUpdatedState(onReadyChanged)
    val latestOnFirstFrameRendered = rememberUpdatedState(onFirstFrameRendered)
    val latestOnPlaybackError = rememberUpdatedState(onPlaybackError)
    val normalizedAssetPath = remember(assetPath) { assetPath.trim().removePrefix("/") }
    val videoUri = remember(normalizedAssetPath) {
        Uri.parse("asset:///$normalizedAssetPath")
    }
    val player = remember(normalizedAssetPath) {
        val dataSourceFactory = DataSource.Factory {
            AssetDataSource(context)
        }
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUri))

        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            volume = 0f
            setMediaSource(mediaSource)
            prepare()
        }
    }

    DisposableEffect(player) {
        Log.d(
            LOCAL_VIDEO_PLAYER_TAG,
            "prepare assetPath=$normalizedAssetPath uri=$videoUri"
        )
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val isReady = playbackState == Player.STATE_READY
                Log.d(
                    LOCAL_VIDEO_PLAYER_TAG,
                    "state=$playbackState ready=$isReady buffering=$isBuffering assetPath=$normalizedAssetPath uri=$videoUri"
                )
                latestOnBufferingChanged.value(isBuffering)
                latestOnReadyChanged.value(isReady)
            }

            override fun onRenderedFirstFrame() {
                Log.d(
                    LOCAL_VIDEO_PLAYER_TAG,
                    "first frame assetPath=$normalizedAssetPath uri=$videoUri"
                )
                latestOnFirstFrameRendered.value()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(
                    LOCAL_VIDEO_PLAYER_TAG,
                    "player error assetPath=$normalizedAssetPath uri=$videoUri message=${error.message}",
                    error
                )
                player.pause()
                latestOnPlaybackError.value()
            }
        }
        player.addListener(listener)
        onDispose {
            Log.d(
                LOCAL_VIDEO_PLAYER_TAG,
                "release assetPath=$normalizedAssetPath uri=$videoUri"
            )
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isPlaying) {
        player.volume = 0f
        Log.d(
            LOCAL_VIDEO_PLAYER_TAG,
            "set isPlaying=$isPlaying assetPath=$normalizedAssetPath uri=$videoUri"
        )
        if (isPlaying) {
            player.play()
        } else {
            player.pause()
        }
    }

    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { viewContext ->
            (LayoutInflater.from(viewContext)
                .inflate(R.layout.local_video_player_view, null, false) as PlayerView).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                clipChildren = true
                clipToPadding = true
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
