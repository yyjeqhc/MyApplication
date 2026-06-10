package com.example.myapplication.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.AdItem
import com.example.myapplication.ui.common.AssetImage
import com.example.myapplication.ui.common.LocalVideoPlayer

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
    isPlaying: Boolean = false,
    initialPositionMs: Long = 0L,
    videoPositionMs: Long = 0L,
    videoDurationMs: Long = 0L,
    onVideoClick: () -> Unit = {},
    onVideoPlaybackUpdate: (Long, Long) -> Unit = { _, _ -> },
    onVideoPlaybackEnded: () -> Unit = {},
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // 视频封面区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            ) {
                if (shouldShowVideo) {
                    LocalVideoPlayer(
                        assetPath = ad.videoAsset,
                        isPlaying = isPlaying,
                        initialPositionMs = initialPositionMs,
                        autoPlay = isPlaying,
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

                // 时长标签
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
                        text = ad.videoDuration.ifBlank { "00:30" },
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                if (ad.videoAsset.isNotBlank()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        trackColor = Color.White.copy(alpha = 0.22f),
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }

            // 内容区域
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // 品牌名称
                if (ad.brandName.isNotEmpty()) {
                    Text(
                        text = ad.brandName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 标题
                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // AI 摘要
                AiSummaryText(
                    summary = ad.aiSummary.ifBlank { ad.subtitle },
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 智能标签
                AdTagRow(
                    tags = ad.tags,
                    onTagClick = onTagClick,
                    maxTags = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CTA 按钮
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        text = ad.ctaText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 互动按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 点赞
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLikeClick() }
                    ) {
                        Icon(
                            imageVector = if (ad.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "点赞",
                            tint = if (ad.liked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCount(ad.likeCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 收藏
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onFavoriteClick() }
                    ) {
                        Icon(
                            imageVector = if (ad.favorited) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "收藏",
                            tint = if (ad.favorited) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "收藏",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 分享
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onShareClick() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "分享",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
