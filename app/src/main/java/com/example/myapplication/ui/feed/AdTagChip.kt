package com.example.myapplication.ui.feed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdTagChip(
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = tag,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier.height(30.dp),
        shape = RoundedCornerShape(13.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
            labelColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        border = null
    )
}

@Composable
fun AdTagRow(
    tags: List<String>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxTags: Int = tags.size
) {
    val visibleTags = tags.take(maxTags)
    val overflowCount = (tags.size - visibleTags.size).coerceAtLeast(0)

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleTags.forEach { tag ->
            AdTagChip(
                tag = tag,
                onClick = { onTagClick(tag) }
            )
        }
        if (overflowCount > 0) {
            TagOverflowChip(count = overflowCount)
        }
    }
}

@Composable
private fun TagOverflowChip(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 10000 -> "${count / 10000}万"
        count >= 1000 -> "${count / 1000}k"
        else -> count.toString()
    }
}

@Composable
fun AiSummaryText(
    summary: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2
) {
    Text(
        text = summary,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 20.sp
    )
}

@Composable
fun FeedActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selected: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "feedActionScale"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(
                color = if (selected) {
                    tint.copy(alpha = 0.10f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LikeActionButton(
    liked: Boolean,
    likeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FeedActionButton(
        icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        text = formatCount(likeCount),
        onClick = onClick,
        modifier = modifier,
        tint = if (liked) Color(0xFFE5484D) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
        selected = liked
    )
}

@Composable
fun FavoriteActionButton(
    favorited: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FeedActionButton(
        icon = if (favorited) Icons.Default.Star else Icons.Default.StarBorder,
        text = "收藏",
        onClick = onClick,
        modifier = modifier,
        tint = if (favorited) Color(0xFFE3A008) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
        selected = favorited
    )
}

@Composable
fun ShareActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FeedActionButton(
        icon = Icons.Default.Share,
        text = "分享",
        onClick = onClick,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
    )
}
