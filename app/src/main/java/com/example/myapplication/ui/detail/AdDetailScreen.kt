package com.example.myapplication.ui.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.example.myapplication.model.AdItem
import com.example.myapplication.ui.feed.AdTagRow
import com.example.myapplication.ui.feed.formatCount

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
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            AdDetailBottomBar(
                ad = ad,
                onLikeClick = onLikeClick,
                onFavoriteClick = onFavoriteClick,
                onShareClick = {
                    Toast.makeText(context, "分享功能开发中", Toast.LENGTH_SHORT).show()
                },
                onCtaClick = {
                    Toast.makeText(context, "${ad.ctaText}功能开发中", Toast.LENGTH_SHORT).show()
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
            AdDetailHeader(ad = ad)

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
                    AiSummarySection(ad = ad)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 推荐理由
                if (ad.recommendationReason.isNotEmpty()) {
                    RecommendationReasonSection(ad = ad)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // AI 洞察
                if (ad.category.isNotEmpty() || ad.scene.isNotEmpty() || ad.targetAudience.isNotEmpty()) {
                    AiInsightSection(ad = ad)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 广告摘要
                Text(
                    text = "广告摘要",
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

                // 数据统计区域
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
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 智能摘要",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = ad.aiSummary,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp
            )
        }
    }
}

/**
 * 推荐理由区域
 */
@Composable
private fun RecommendationReasonSection(
    ad: AdItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "推荐理由",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = ad.recommendationReason,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 洞察",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InsightRow(label = "品类", value = ad.category)
            InsightRow(label = "场景", value = ad.scene)
            InsightRow(label = "受众", value = ad.targetAudience)
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
 * 统计数据区域
 */
@Composable
private fun StatisticsSection(
    ad: AdItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 数据统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 第一行：曝光、点击、CTR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    count = ad.exposureCount,
                    label = "曝光"
                )
                StatItem(
                    count = ad.clickCount,
                    label = "点击"
                )
                StatItem(
                    count = calculateCTR(ad.clickCount, ad.exposureCount),
                    label = "CTR",
                    isPercentage = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 第二行：点赞、收藏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    count = ad.likeCount,
                    label = "点赞"
                )
                StatItem(
                    count = if (ad.favorited) 1 else 0,
                    label = "收藏状态",
                    isText = true,
                    textValue = if (ad.favorited) "已收藏" else "未收藏"
                )
            }
        }
    }
}

/**
 * 统计数据项
 */
@Composable
private fun StatItem(
    count: Int,
    label: String,
    isPercentage: Boolean = false,
    isText: Boolean = false,
    textValue: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                isText -> textValue
                isPercentage -> "${count}%"
                else -> formatCount(count)
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 计算 CTR（点击率）
 */
private fun calculateCTR(clicks: Int, impressions: Int): Int {
    return if (impressions > 0) {
        ((clicks.toFloat() / impressions.toFloat()) * 100).toInt()
    } else {
        0
    }
}

/**
 * 详情页头图区域
 * 根据卡片类型显示不同的头图样式
 */
@Composable
private fun AdDetailHeader(
    ad: AdItem,
    modifier: Modifier = Modifier
) {
    when (ad.cardType) {
        AdCardType.LARGE_IMAGE -> {
            // 大图样式
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6),
                                Color(0xFFA78BFA)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🖼️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "大图广告",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
        AdCardType.SMALL_IMAGE -> {
            // 小图样式（居中显示）
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF10B981),
                                Color(0xFF34D399),
                                Color(0xFF6EE7B7)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "图片",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "小图广告",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
        AdCardType.VIDEO -> {
            // 视频样式
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1F2937),
                                Color(0xFF374151),
                                Color(0xFF4B5563)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 播放按钮
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "播放",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🎬 视频广告",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "时长: 00:30",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
