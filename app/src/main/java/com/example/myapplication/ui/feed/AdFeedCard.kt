package com.example.myapplication.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myapplication.model.AdCardType
import com.example.myapplication.model.AdItem

/**
 * 广告卡片容器
 * 根据卡片类型选择对应的卡片组件进行渲染
 */
@Composable
fun AdFeedCard(
    ad: AdItem,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onTagClick: (String) -> Unit,
    isVideoPlaying: Boolean = false,
    videoPositionMs: Long = 0L,
    videoDurationMs: Long = 0L,
    onVideoClick: () -> Unit = {},
    onVideoPlaybackUpdate: (Long, Long) -> Unit = { _, _ -> },
    onVideoPlaybackEnded: () -> Unit = {},
    onVideoError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (ad.cardType) {
        AdCardType.LARGE_IMAGE -> {
            LargeImageAdCard(
                ad = ad,
                onClick = onClick,
                onLikeClick = onLikeClick,
                onFavoriteClick = onFavoriteClick,
                onShareClick = onShareClick,
                onTagClick = onTagClick,
                modifier = modifier
            )
        }
        AdCardType.SMALL_IMAGE -> {
            SmallImageAdCard(
                ad = ad,
                onClick = onClick,
                onLikeClick = onLikeClick,
                onFavoriteClick = onFavoriteClick,
                onShareClick = onShareClick,
                onTagClick = onTagClick,
                modifier = modifier
            )
        }
        AdCardType.VIDEO -> {
            VideoAdCard(
                ad = ad,
                onClick = onClick,
                onLikeClick = onLikeClick,
                onFavoriteClick = onFavoriteClick,
                onShareClick = onShareClick,
                onTagClick = onTagClick,
                isPlaying = isVideoPlaying,
                initialPositionMs = videoPositionMs,
                videoPositionMs = videoPositionMs,
                videoDurationMs = videoDurationMs,
                onVideoClick = onVideoClick,
                onVideoPlaybackUpdate = onVideoPlaybackUpdate,
                onVideoPlaybackEnded = onVideoPlaybackEnded,
                onVideoError = onVideoError,
                modifier = modifier
            )
        }
    }
}
