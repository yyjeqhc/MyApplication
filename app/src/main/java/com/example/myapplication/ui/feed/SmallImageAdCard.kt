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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

/**
 * 小图广告卡片
 * 左侧小图占位 + 右侧文字信息布局
 */
@Composable
fun SmallImageAdCard(
    ad: AdItem,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isPressed by cardInteractionSource.collectIsPressedAsState()
    val cardElevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 3.dp,
        label = "smallCardElevation"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "smallCardAlpha"
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
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 左侧小图媒体
                Box(
                    modifier = Modifier
                        .width(112.dp)
                        .height(118.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
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
                                            Color(0xFF10B981),
                                            Color(0xFF34D399),
                                            Color(0xFF6EE7B7)
                                        )
                                    )
                                )
                        )
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "图片",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                        )
                    }

                    // 广告标识
                    if (ad.isAd) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "广告",
                                color = Color.White,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧文字信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 品牌名称
                    if (ad.brandName.isNotEmpty()) {
                        Text(
                            text = ad.brandName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    // 标题
                    Text(
                        text = ad.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // AI 摘要
                    AiSummaryText(
                        summary = ad.aiSummary.ifBlank { ad.subtitle },
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    // 智能标签
                    AdTagRow(
                        tags = ad.tags,
                        onTagClick = onTagClick,
                        maxTags = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
