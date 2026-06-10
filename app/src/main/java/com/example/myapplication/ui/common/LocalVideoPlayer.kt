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
import androidx.media3.common.C
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
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private const val LOCAL_VIDEO_PLAYER_TAG = "LocalVideoPlayer"
private const val POSITION_UPDATE_INTERVAL_MS = 500L

@OptIn(UnstableApi::class)
@Composable
fun LocalVideoPlayer(
    assetPath: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    initialPositionMs: Long = 0L,
    autoPlay: Boolean = isPlaying,
    onBufferingChanged: (Boolean) -> Unit = {},
    onReadyChanged: (Boolean) -> Unit = {},
    onReady: (Long) -> Unit = {},
    onPositionChanged: (Long, Long) -> Unit = { _, _ -> },
    onPlaybackEnded: () -> Unit = {},
    onFirstFrameRendered: () -> Unit = {},
    onPlaybackError: () -> Unit = {}
) {
    val context = LocalContext.current
    val latestOnBufferingChanged = rememberUpdatedState(onBufferingChanged)
    val latestOnReadyChanged = rememberUpdatedState(onReadyChanged)
    val latestOnReady = rememberUpdatedState(onReady)
    val latestOnPositionChanged = rememberUpdatedState(onPositionChanged)
    val latestOnPlaybackEnded = rememberUpdatedState(onPlaybackEnded)
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
        val safeInitialPositionMs = max(0L, initialPositionMs)

        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = autoPlay
            volume = 0f
            setMediaSource(mediaSource)
            if (safeInitialPositionMs > 0L) {
                seekTo(safeInitialPositionMs)
            }
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
                val durationMs = player.duration.sanitizedDuration()
                Log.d(
                    LOCAL_VIDEO_PLAYER_TAG,
                    "state=$playbackState ready=$isReady buffering=$isBuffering assetPath=$normalizedAssetPath uri=$videoUri"
                )
                latestOnBufferingChanged.value(isBuffering)
                latestOnReadyChanged.value(isReady)
                if (isReady) {
                    latestOnReady.value(durationMs)
                    latestOnPositionChanged.value(player.currentPosition.coercePosition(durationMs), durationMs)
                } else if (playbackState == Player.STATE_ENDED) {
                    latestOnPositionChanged.value(0L, durationMs)
                    latestOnPlaybackEnded.value()
                }
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
            val durationMs = player.duration.sanitizedDuration()
            latestOnPositionChanged.value(player.currentPosition.coercePosition(durationMs), durationMs)
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

    LaunchedEffect(player) {
        while (true) {
            val durationMs = player.duration.sanitizedDuration()
            latestOnPositionChanged.value(player.currentPosition.coercePosition(durationMs), durationMs)
            delay(POSITION_UPDATE_INTERVAL_MS)
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

private fun Long.sanitizedDuration(): Long {
    return if (this == C.TIME_UNSET || this < 0L) 0L else this
}

private fun Long.coercePosition(durationMs: Long): Long {
    return if (durationMs > 0L) min(max(0L, this), durationMs) else max(0L, this)
}
