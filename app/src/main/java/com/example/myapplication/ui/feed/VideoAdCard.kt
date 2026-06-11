package com.example.myapplication.ui.feed

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.AdItem
import com.example.myapplication.ui.common.AssetImage
import com.example.myapplication.ui.common.LocalVideoPlayer
import com.example.myapplication.ui.common.VideoProgressBar

/**
 * 视频广告卡片
 * 深色背景 + 播放按钮 + 标题信息
 */
@Composable
fun VideoAdCard(
    ad: AdItem,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onCtaClick: () -> Unit,
    isPlaying: Boolean = false,
    initialPositionMs: Long = 0L,
    videoPositionMs: Long = 0L,
    videoDurationMs: Long = 0L,
    videoSeekPositionMs: Long = 0L,
    videoSeekRequestId: Long = 0L,
    onVideoClick: () -> Unit = {},
    onVideoPlaybackUpdate: (Long, Long) -> Unit = { _, _ -> },
    onVideoPlaybackEnded: () -> Unit = {},
    onVideoSeek: (Long) -> Unit = {},
    onVideoError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var hasPlaybackError by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var hasRequestedPlayback by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var isVideoBuffering by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var isVideoReady by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    val canPlayVideo = ad.videoAsset.isNotBlank() && !hasPlaybackError
    val isActivelyPlaying = canPlayVideo && isPlaying
    val shouldShowVideo = canPlayVideo && hasRequestedPlayback
    val shouldShowCover = !hasRenderedFirstFrame || hasPlaybackError || !shouldShowVideo
    val shouldShowLoading = canPlayVideo && isPlaying && !hasRenderedFirstFrame && (isVideoBuffering || !isVideoReady)
    val progress = remember(videoPositionMs, videoDurationMs) {
        if (videoDurationMs > 0L) {
            (videoPositionMs.toFloat() / videoDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    val durationLabel = remember(videoDurationMs, ad.videoDuration) {
        formatVideoDuration(videoDurationMs).ifBlank { ad.videoDuration }
    }
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardElevation by animateDpAsState(
        targetValue = if (isCardPressed) 1.dp else 4.dp,
        label = "videoCardElevation"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isCardPressed) 0.97f else 1f,
        label = "videoCardAlpha"
    )
    val mediaClickInteractionSource = remember { MutableInteractionSource() }
    val playButtonInteractionSource = remember { MutableInteractionSource() }

    fun handlePlayButtonClick() {
        if (canPlayVideo) {
            hasRequestedPlayback = true
            onVideoClick()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            hasRequestedPlayback = true
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = cardAlpha }
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        Column {
            // 视频封面区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(208.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                if (shouldShowVideo) {
                    LocalVideoPlayer(
                        assetPath = ad.videoAsset,
                        isPlaying = isPlaying,
                        initialPositionMs = initialPositionMs,
                        autoPlay = isPlaying,
                        seekToPositionMs = videoSeekPositionMs,
                        seekRequestId = videoSeekRequestId,
                        modifier = Modifier.matchParentSize(),
                        onBufferingChanged = { isVideoBuffering = it },
                        onReadyChanged = { isVideoReady = it },
                        onReady = { durationMs ->
                            onVideoPlaybackUpdate(videoPositionMs, durationMs)
                        },
                        onPositionChanged = onVideoPlaybackUpdate,
                        onPlaybackEnded = onVideoPlaybackEnded,
                        onFirstFrameRendered = { hasRenderedFirstFrame = true },
                        onPlaybackError = {
                            hasPlaybackError = true
                            hasRequestedPlayback = false
                            isVideoBuffering = false
                            isVideoReady = false
                            hasRenderedFirstFrame = false
                            onVideoError()
                        }
                    )
                }

                if (shouldShowCover) {
                    AssetImage(
                        assetPath = ad.imageAsset,
                        contentDescription = ad.title,
                        modifier = Modifier.matchParentSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF1F2937),
                                            Color(0xFF374151),
                                            Color(0xFF4B5563)
                                        )
                                    )
                                )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Color.Black.copy(
                                alpha = if (shouldShowCover) 0.22f else 0.08f
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = mediaClickInteractionSource,
                            indication = null,
                            onClick = onClick
                        )
                )

                // 视频图标占位
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 播放按钮
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isActivelyPlaying) 0.12f else 0.18f))
                            .clickable(
                                interactionSource = playButtonInteractionSource,
                                indication = null,
                                onClick = ::handlePlayButtonClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (shouldShowLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isActivelyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isActivelyPlaying) "暂停" else "播放",
                                tint = Color.White,
                                modifier = Modifier.size(if (isActivelyPlaying) 24.dp else 30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "视频广告",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                // 广告标识
                if (ad.isAd) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.42f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "广告",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }

                if (durationLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = durationLabel,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                if (ad.videoAsset.isNotBlank()) {
                    VideoProgressBar(
                        progress = progress,
                        durationMs = videoDurationMs,
                        onSeek = onVideoSeek,
                        compact = true,
                        isActive = isActivelyPlaying,
                        showWhenIdle = false,
                        color = Color.White.copy(alpha = 0.72f),
                        trackColor = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }
            }

            // 内容区域
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 品牌名称
                if (ad.brandName.isNotEmpty()) {
                    Text(
                        text = ad.brandName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 标题
                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // AI 摘要
                AiSummaryText(
                    summary = ad.aiSummary.ifBlank { ad.subtitle },
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 标签与 CTA 放在同一行，保持视频下方信息区紧凑
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdTagRow(
                        tags = ad.tags,
                        onTagClick = onTagClick,
                        maxTags = 3,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onCtaClick,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = ad.ctaText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 互动按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LikeActionButton(
                        liked = ad.liked,
                        likeCount = ad.likeCount,
                        onClick = onLikeClick
                    )

                    FavoriteActionButton(
                        favorited = ad.favorited,
                        onClick = onFavoriteClick
                    )

                    ShareActionButton(onClick = onShareClick)
                }
            }
        }
    }
}

private fun formatVideoDuration(durationMs: Long): String {
    if (durationMs <= 0L) return ""

    val totalSeconds = durationMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
