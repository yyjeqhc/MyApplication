package com.example.myapplication.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 左侧小图占位
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
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
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "图片",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧文字信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 标题
                    Text(
                        text = ad.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 副标题
                    Text(
                        text = ad.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 标签
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ad.tags.take(2).forEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = tag,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.height(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCount(ad.likeCount),
                        style = MaterialTheme.typography.bodySmall,
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "收藏",
                        style = MaterialTheme.typography.bodySmall,
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "分享",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 格式化数字显示
 */
private fun formatCount(count: Int): String {
    return when {
        count >= 10000 -> "${count / 10000}万"
        count >= 1000 -> "${count / 1000}k"
        else -> count.toString()
    }
}
