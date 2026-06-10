package com.example.myapplication.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.myapplication.ui.common.showSingleToast
import com.example.myapplication.ui.feed.AdTagRow
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
                        fontWeight = FontWeight.Bold
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
                    showSingleToast(context, "${ad.ctaText}功能开发中")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            DetailMediaHero(ad = ad)

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 品牌名称和广告标识
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ad.brandName.isNotEmpty()) {
                        Text(
                            text = ad.brandName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (ad.isAd) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "广告",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 标题
                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 副标题
                Text(
                    text = ad.subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 智能标签
                AdTagRow(
                    tags = ad.tags,
                    onTagClick = onTagClick
                )

                Spacer(modifier = Modifier.height(24.dp))

                // AI 摘要区域
                if (ad.aiSummary.isNotEmpty()) {
                    AiSummarySection(
                        ad = ad,
                        title = detailCopy.aiSummaryTitle
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 推荐理由
                if (reasonText.isNotEmpty()) {
                    RecommendationReasonSection(
                        title = detailCopy.reasonTitle,
                        text = reasonText
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // AI 洞察
                if (ad.category.isNotEmpty() || ad.scene.isNotEmpty() || ad.targetAudience.isNotEmpty()) {
                    AiInsightSection(
                        ad = ad,
                        title = detailCopy.insightTitle,
                        audienceLabel = detailCopy.audienceLabel
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 广告摘要
                Text(
                    text = detailCopy.descriptionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = ad.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 互动数据区域
                StatisticsSection(ad = ad)
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
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
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
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = ad.ctaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * AI 摘要区域
 */
@Composable
private fun AiSummarySection(
    ad: AdItem,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = ad.aiSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 23.sp
            )
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "推荐",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 23.sp
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = "人群",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = "互动数据",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        },
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
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
            Spacer(modifier = Modifier.width(6.dp))
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
    val aiSummaryTitle: String,
    val reasonTitle: String,
    val insightTitle: String,
    val audienceLabel: String,
    val descriptionTitle: String
)

private fun detailReasonTextFor(ad: AdItem): String {
    return ad.subtitle.ifBlank { ad.aiSummary }.trim()
}

private fun detailCopyFor(ad: AdItem): DetailCopy {
    return when {
        ad.cardType == AdCardType.VIDEO -> DetailCopy(
            aiSummaryTitle = "视频看点",
            reasonTitle = "观看理由",
            insightTitle = "适合人群",
            audienceLabel = "受众",
            descriptionTitle = "视频内容摘要"
        )
        ad.channel == AdChannel.LOCAL -> DetailCopy(
            aiSummaryTitle = "服务亮点",
            reasonTitle = "到店理由",
            insightTitle = "适合人群",
            audienceLabel = "人群",
            descriptionTitle = "广告摘要"
        )
        else -> DetailCopy(
            aiSummaryTitle = "商品亮点",
            reasonTitle = "推荐理由",
            insightTitle = "适合场景",
            audienceLabel = "受众",
            descriptionTitle = "广告摘要"
        )
    }
}

/**
 * 详情页媒体区，根据广告类型展示不同的视觉重点。
 */
@Composable
private fun DetailMediaHero(
    ad: AdItem,
    modifier: Modifier = Modifier
) {
    when (ad.cardType) {
        AdCardType.VIDEO -> VideoDetailHero(ad = ad, modifier = modifier)
        AdCardType.LARGE_IMAGE -> LargeImageDetailHero(ad = ad, modifier = modifier)
        AdCardType.SMALL_IMAGE -> SmallImageDetailHero(ad = ad, modifier = modifier)
    }
}

@Composable
private fun VideoDetailHero(
    ad: AdItem,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var hasRequestedPlayback by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    var hasPlaybackError by remember(ad.id, ad.videoAsset) { mutableStateOf(false) }
    val canPlayVideo = ad.videoAsset.isNotBlank() && !hasPlaybackError
    val shouldShowVideo = canPlayVideo && hasRequestedPlayback

    LaunchedEffect(hasPlaybackError) {
        if (hasPlaybackError) {
            isPlaying = false
            hasRequestedPlayback = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable {
                if (canPlayVideo) {
                    hasRequestedPlayback = true
                    isPlaying = !isPlaying
                }
            }
    ) {
        if (shouldShowVideo) {
            LocalVideoPlayer(
                assetPath = ad.videoAsset,
                isPlaying = isPlaying,
                modifier = Modifier.matchParentSize(),
                onPlaybackError = {
                    hasPlaybackError = true
                }
            )
        } else {
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
                .background(Color.Black.copy(alpha = 0.28f))
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
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(end = 84.dp)
        ) {
            Text(
                text = ad.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ad.aiSummary.ifBlank { ad.subtitle },
                color = Color.White.copy(alpha = 0.76f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        DetailHeroBadge(
            text = ad.videoDuration.ifBlank { "00:30" },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun LargeImageDetailHero(
    ad: AdItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.62f)
                        )
                    )
                )
        )

        DetailHeroBadge(
            text = "图文广告",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = ad.brandName.ifBlank { ad.category },
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ad.subtitle,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
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
            .height(190.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF059669),
                        Color(0xFF14B8A6),
                        Color(0xFF0EA5E9)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetImage(
                assetPath = ad.imageAsset,
                contentDescription = ad.title,
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "图文图片",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                DetailHeroBadge(text = "图文广告")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = ad.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ad.aiSummary.ifBlank { ad.subtitle },
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
