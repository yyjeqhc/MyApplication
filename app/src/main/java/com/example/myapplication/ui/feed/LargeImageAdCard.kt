package com.example.myapplication.ui.feed

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.AdItem
import com.example.myapplication.ui.common.AssetImage

/**
 * 大图广告卡片
 * 全宽大图展示，底部叠加渐变蒙层显示信息
 */
@Composable
fun LargeImageAdCard(
    ad: AdItem,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isPressed by cardInteractionSource.collectIsPressedAsState()
    val cardElevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        label = "largeCardElevation"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "largeCardAlpha"
    )

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
            // 大图媒体区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
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
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFA78BFA)
                                    )
                                )
                            )
                    )
                    Text(
                        text = "广告封面",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center)
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

                // 渐变蒙层
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }

            // 内容区域
            Column(
                modifier = Modifier.padding(start = 13.dp, top = 10.dp, end = 13.dp, bottom = 10.dp)
            ) {
                // 品牌名称
                if (ad.brandName.isNotEmpty()) {
                    Text(
                        text = ad.brandName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // 标题
                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.titleLarge,
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

                // 标签与 CTA 放在同一行，避免按钮孤立撑高卡片
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
                    TextButton(
                        onClick = onCtaClick,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = ad.ctaText.ifBlank { "了解详情" },
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
