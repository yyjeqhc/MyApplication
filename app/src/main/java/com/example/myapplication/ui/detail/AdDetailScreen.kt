package com.example.myapplication.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.AdCardType
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem
import com.example.myapplication.ui.common.AssetImage
import com.example.myapplication.ui.common.LocalVideoPlayer
import com.example.myapplication.ui.common.VideoProgressBar
import com.example.myapplication.ui.common.showSingleToast
import com.example.myapplication.ui.feed.formatCount
import java.util.Locale

/**
 * 广告详情页
 * 展示广告的完整信息和互动功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdDetailScreen(
    ad: AdItem,
    initialVideoPositionMs: Long = 0L,
    initialVideoDurationMs: Long = 0L,
    videoSeekPositionMs: Long = 0L,
    videoSeekRequestId: Long = 0L,
    autoPlayVideo: Boolean = false,
    onVideoPlaybackUpdate: (Long, Long) -> Unit = { _, _ -> },
    onVideoPlaybackEnded: () -> Unit = {},
    onVideoSeek: (Long) -> Unit = {},
    onBack: () -> Unit,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onCtaClick: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val detailCopy = detailCopyFor(ad)
    val reasonText = detailReasonTextFor(ad)
    val summaryText = detailSummaryTextFor(ad)

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "广告详情",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            AdDetailBottomBar(
                ad = ad,
                onLikeClick = onLikeClick,
                onFavoriteClick = onFavoriteClick,
                onShareClick = {
                    onShareClick()
                    showSingleToast(context, "已记录分享")
                },
                onCtaClick = {
                    onCtaClick()
                    showSingleToast(context, "外部跳转功能开发中")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f))
                .verticalScroll(rememberScrollState())
        ) {
            DetailMediaHero(
                ad = ad,
                initialVideoPositionMs = initialVideoPositionMs,
                initialVideoDurationMs = initialVideoDurationMs,
                videoSeekPositionMs = videoSeekPositionMs,
                videoSeekRequestId = videoSeekRequestId,
                autoPlayVideo = autoPlayVideo,
                onVideoPlaybackUpdate = onVideoPlaybackUpdate,
                onVideoPlaybackEnded = onVideoPlaybackEnded,
                onVideoSeek = onVideoSeek
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MainInfoSection(
                        ad = ad,
                        summaryText = summaryText
                    )

                    if (reasonText.isNotEmpty()) {
                        RecommendationReasonSection(
                            title = detailCopy.reasonTitle,
                            text = reasonText
                        )
                    }

                    if (ad.category.isNotEmpty() || ad.scene.isNotEmpty() || ad.targetAudience.isNotEmpty()) {
                        AiInsightSection(
                            ad = ad,
                            title = detailCopy.insightTitle,
                            audienceLabel = detailCopy.audienceLabel
                        )
                    }

                    if (summaryText.isNotEmpty()) {
                        DescriptionSection(
                            title = detailCopy.descriptionTitle,
                            text = summaryText
                        )
                    }

                    StatisticsSection(ad = ad)
                }
            }
        }
    }
}

/**
 * 详情页固定底部操作栏
 */
@Composable
private fun AdDetailBottomBar(
    ad: AdItem,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = onLikeClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (ad.liked) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (ad.liked) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            ) {
                Icon(
                    imageVector = if (ad.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (ad.liked) "取消点赞" else "点赞"
                )
            }

            FilledTonalIconButton(
                onClick = onFavoriteClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (ad.favorited) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (ad.favorited) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            ) {
                Icon(
                    imageVector = if (ad.favorited) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (ad.favorited) "取消收藏" else "收藏"
                )
            }

            OutlinedIconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "分享"
                )
            }

            Button(
                onClick = onCtaClick,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = ad.ctaText.ifBlank { "了解更多" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MainInfoSection(
    ad: AdItem,
    summaryText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ad.brandName.isNotBlank()) {
                Text(
                    text = ad.brandName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (ad.isAd) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = "广告",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = ad.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 30.sp
        )

        if (summaryText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 23.sp
            )
        }

        if (ad.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            DetailTagRow(tags = ad.tags)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTagRow(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * 推荐理由区域
 */
@Composable
private fun RecommendationReasonSection(
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "推荐",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * AI 洞察区域
 */
@Composable
private fun AiInsightSection(
    ad: AdItem,
    title: String,
    audienceLabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "人群",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            InsightRow(label = "品类", value = ad.category)
            InsightRow(label = "场景", value = ad.scene)
            InsightRow(label = audienceLabel, value = ad.targetAudience)
        }
    }
}

@Composable
private fun InsightRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    if (value.isBlank()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(44.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            lineHeight = 21.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DescriptionSection(
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * 轻量互动数据区域
 */
@Composable
private fun StatisticsSection(
    ad: AdItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "互动表现",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InteractionStatChip(
                    label = "曝光",
                    value = formatCount(ad.exposureCount),
                    modifier = Modifier.weight(1f)
                )
                InteractionStatChip(
                    label = "点击",
                    value = formatCount(ad.clickCount),
                    modifier = Modifier.weight(1f)
                )
                InteractionStatChip(
                    label = "CTR",
                    value = formatCTR(ad.clickCount, ad.exposureCount),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InteractionStatChip(
                    label = "点赞",
                    value = formatCount(ad.likeCount),
                    modifier = Modifier.weight(1f)
                )
                InteractionStatChip(
                    label = "收藏",
                    value = if (ad.favorited) "已收藏" else "未收藏",
                    highlighted = ad.favorited,
                    modifier = Modifier.weight(1f)
                )
                InteractionStatChip(
                    label = "分享",
                    value = formatCount(ad.shareCount),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 轻量互动指标 chip
 */
@Composable
private fun InteractionStatChip(
    label: String,
    value: String,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        },
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 格式化 CTR（点击率）
 */
private fun formatCTR(clicks: Int, impressions: Int): String {
    return when {
        impressions <= 0 -> "—"
        clicks < 0 -> "—"
        clicks > impressions -> "测试中"
        else -> String.format(Locale.US, "%.1f%%", clicks.toFloat() / impressions.toFloat() * 100f)
    }
}

private data class DetailCopy(
    val reasonTitle: String,
    val insightTitle: String,
    val audienceLabel: String,
    val descriptionTitle: String
)

private fun detailReasonTextFor(ad: AdItem): String {
    return ad.recommendationReason
        .ifBlank { ad.aiSummary }
        .ifBlank { ad.subtitle }
        .trim()
}

private fun detailSummaryTextFor(ad: AdItem): String {
    return ad.subtitle
        .ifBlank { ad.summary }
        .ifBlank { ad.aiSummary }
        .trim()
}

private fun detailCopyFor(ad: AdItem): DetailCopy {
    return when {
        ad.cardType == AdCardType.VIDEO -> DetailCopy(
            reasonTitle = "核心亮点",
            insightTitle = "适用场景",
            audienceLabel = "受众",
            descriptionTitle = "视频看点"
        )
        ad.channel == AdChannel.LOCAL -> DetailCopy(
            reasonTitle = "核心亮点",
            insightTitle = "适用场景",
            audienceLabel = "人群",
            descriptionTitle = "内容摘要"
        )
        else -> DetailCopy(
            reasonTitle = "核心亮点",
            insightTitle = "适用场景",
            audienceLabel = "受众",
            descriptionTitle = "内容摘要"
        )
    }
}

/**
 * 详情页媒体区，根据广告类型展示不同的视觉重点。
 */
@Composable
private fun DetailMediaHero(
    ad: AdItem,
    initialVideoPositionMs: Long = 0L,
    initialVideoDurationMs: Long = 0L,
    videoSeekPositionMs: Long = 0L,
    videoSeekRequestId: Long = 0L,
    autoPlayVideo: Boolean = false,
    onVideoPlaybackUpdate: (Long, Long) -> Unit = { _, _ -> },
    onVideoPlaybackEnded: () -> Unit = {},
    onVideoSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (ad.cardType) {
        AdCardType.VIDEO -> VideoDetailHero(
            ad = ad,
            initialPositionMs = initialVideoPositionMs,
            initialDurationMs = initialVideoDurationMs,
            seekToPositionMs = videoSeekPositionMs,
            seekRequestId = videoSeekRequestId,
            autoPlay = autoPlayVideo,
            onVideoPlaybackUpdate = onVideoPlaybackUpdate,
            onVideoPlaybackEnded = onVideoPlaybackEnded,
            onVideoSeek = onVideoSeek,
            modifier = modifier
        )
        AdCardType.LARGE_IMAGE -> LargeImageDetailHero(ad = ad, modifier = modifier)
        AdCardType.SMALL_IMAGE -> SmallImageDetailHero(ad = ad, modifier = modifier)
    }
}

@Composable
private fun VideoDetailHero(
    ad: AdItem,
    initialPositionMs: Long = 0L,
    initialDurationMs: Long = 0L,
    seekToPositionMs: Long = 0L,
    seekRequestId: Long = 0L,
    autoPlay: Boolean = false,
    onVideoPlaybackUpdate: (Long, Long) -> Unit = { _, _ -> },
    onVideoPlaybackEnded: () -> Unit = {},
    onVideoSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPlaying by remember(ad.id, ad.videoAsset) { mutableStateOf(autoPlay) }
    var hasRequestedPlayback by remember(ad.id, ad.videoAsset) { mutableStateOf(autoPlay || initialPositionMs > 0L) }
    var hasPlaybackError by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var isVideoBuffering by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var isVideoReady by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var hasRenderedFirstFrame by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var currentPositionMs by remember(ad.id, ad.videoAsset) { mutableStateOf(initialPositionMs) }
    var currentDurationMs by remember(ad.id, ad.videoAsset) { mutableStateOf(initialDurationMs) }
    val canPlayVideo = ad.videoAsset.isNotBlank() && !hasPlaybackError
    val shouldShowVideo = canPlayVideo && hasRequestedPlayback
    val shouldShowCover = !hasRenderedFirstFrame || hasPlaybackError || !shouldShowVideo
    val shouldShowLoading = canPlayVideo && isPlaying && !hasRenderedFirstFrame && (isVideoBuffering || !isVideoReady)
    val progress = remember(currentPositionMs, currentDurationMs) {
        if (currentDurationMs > 0L) {
            (currentPositionMs.toFloat() / currentDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    val durationLabel = remember(currentDurationMs, ad.videoDuration) {
        formatVideoDuration(currentDurationMs).ifBlank { ad.videoDuration }
    }
    val playButtonInteractionSource = remember { MutableInteractionSource() }

    fun handlePlayButtonClick() {
        if (canPlayVideo) {
            hasRequestedPlayback = true
            isPlaying = !isPlaying
        }
    }

    LaunchedEffect(hasPlaybackError) {
        if (hasPlaybackError) {
            isPlaying = false
            hasRequestedPlayback = false
            isVideoBuffering = false
            isVideoReady = false
            hasRenderedFirstFrame = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clipToBounds()
    ) {
        if (shouldShowVideo) {
            LocalVideoPlayer(
                assetPath = ad.videoAsset,
                isPlaying = isPlaying,
                initialPositionMs = initialPositionMs,
                autoPlay = autoPlay,
                seekToPositionMs = seekToPositionMs,
                seekRequestId = seekRequestId,
                modifier = Modifier.matchParentSize(),
                onBufferingChanged = { isVideoBuffering = it },
                onReadyChanged = { isVideoReady = it },
                onReady = { durationMs ->
                    currentDurationMs = durationMs
                    onVideoPlaybackUpdate(currentPositionMs, durationMs)
                },
                onPositionChanged = { positionMs, durationMs ->
                    currentPositionMs = positionMs
                    if (durationMs > 0L) {
                        currentDurationMs = durationMs
                    }
                    onVideoPlaybackUpdate(positionMs, durationMs)
                },
                onPlaybackEnded = {
                    if (currentDurationMs > 0L) {
                        currentPositionMs = currentDurationMs
                    }
                    isPlaying = false
                    onVideoPlaybackEnded()
                },
                onFirstFrameRendered = { hasRenderedFirstFrame = true },
                onPlaybackError = {
                    hasPlaybackError = true
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
                                    Color(0xFF111827),
                                    Color(0xFF263244),
                                    Color(0xFF111827)
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
                        alpha = if (shouldShowCover) 0.28f else 0.08f
                    )
                )
        )

        DetailHeroBadge(
            text = "视频广告",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(82.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (isPlaying) 0.12f else 0.18f))
                .clickable(
                    interactionSource = playButtonInteractionSource,
                    indication = null,
                    onClick = ::handlePlayButtonClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (shouldShowLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(if (isPlaying) 38.dp else 48.dp)
                )
            }
        }

        if (durationLabel.isNotBlank()) {
            DetailHeroBadge(
                text = durationLabel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }

        if (ad.videoAsset.isNotBlank()) {
            VideoProgressBar(
                progress = progress,
                durationMs = currentDurationMs,
                onSeek = { positionMs ->
                    currentPositionMs = positionMs
                    onVideoSeek(positionMs)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
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

@Composable
private fun LargeImageDetailHero(
    ad: AdItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(286.dp)
    ) {
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
                                Color(0xFF4F46E5),
                                Color(0xFF7C3AED),
                                Color(0xFFDB2777)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(112.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "商品图片",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        DetailHeroBadge(
            text = "图文广告",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

@Composable
private fun SmallImageDetailHero(
    ad: AdItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clipToBounds()
    ) {
        AssetImage(
            assetPath = ad.imageAsset,
            contentDescription = ad.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF059669),
                                Color(0xFF14B8A6),
                                Color(0xFF0EA5E9)
                            )
                        )
                    )
            )
        }

        DetailHeroBadge(
            text = "图文广告",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

@Composable
private fun DetailHeroBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.42f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
