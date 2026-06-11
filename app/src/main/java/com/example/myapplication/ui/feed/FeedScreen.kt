package com.example.myapplication.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.AdCardType
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem
import com.example.myapplication.model.FeedListState
import com.example.myapplication.model.FeedUiState
import com.example.myapplication.ui.common.showSingleToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

private const val SHOW_DEBUG_PANEL = false
private const val REFRESH_FEEDBACK_DURATION_MS = 900L
private const val FEED_VIDEO_AUTOPLAY_DELAY_MS = 1500L

/**
 * 信息流主页面
 * 包含顶部标题栏、频道 Tab、Demo 控制面板和广告列表
 * 支持下拉刷新和上拉加载更多
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    uiState: FeedUiState,
    playingVideoAdId: String?,
    manualPausedVideoAdIds: Set<String>,
    videoPlaybackPositions: Map<String, Long>,
    videoDurations: Map<String, Long>,
    videoSeekPositions: Map<String, Long>,
    videoSeekRequestIds: Map<String, Long>,
    listStateForChannel: (AdChannel) -> LazyListState,
    onAdClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onVideoPlayToggle: (String) -> Unit,
    onVideoAutoPlay: (String) -> Unit,
    onVideoAutoPause: (String) -> Unit,
    onVisibleFeedVideoIdsChanged: (List<String>) -> Unit,
    onVideoPlaybackUpdate: (String, Long, Long) -> Unit,
    onVideoPlaybackEnded: (String) -> Unit,
    onVideoSeek: (String, Long) -> Unit,
    onTagClick: (String) -> Unit,
    onClearTagFilter: () -> Unit,
    onChannelSelect: (AdChannel) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onVisibleAdsChanged: (List<String>) -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
    onFeedbackShown: () -> Unit,
    onSearchClick: () -> Unit,
    onSimulateNormal: () -> Unit,
    onSimulateEmpty: () -> Unit,
    onSimulateError: () -> Unit,
    onResetPagination: () -> Unit,
    onClearLocalAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 频道列表
    val channels = remember { AdChannel.entries }
    val selectedPage = channels.indexOf(uiState.selectedChannel).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedPage,
        pageCount = { channels.size }
    )
    val activeTabIndex by remember {
        derivedStateOf { pagerState.targetPage.coerceIn(0, channels.lastIndex) }
    }
    val indicatorPagePosition by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, channels.lastIndex.toFloat())
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val latestSelectedChannel by rememberUpdatedState(uiState.selectedChannel)
    val latestOnChannelSelect by rememberUpdatedState(onChannelSelect)
    val selectedListState = listStateForChannel(uiState.selectedChannel)
    val latestSelectedListState by rememberUpdatedState(selectedListState)
    val latestPlayingVideoAdId by rememberUpdatedState(playingVideoAdId)
    val latestManualPausedVideoAdIds by rememberUpdatedState(manualPausedVideoAdIds)
    val latestOnVideoPlayToggle by rememberUpdatedState(onVideoPlayToggle)
    val latestOnVideoAutoPlay by rememberUpdatedState(onVideoAutoPlay)
    val latestOnVideoAutoPause by rememberUpdatedState(onVideoAutoPause)
    val latestOnVisibleFeedVideoIdsChanged by rememberUpdatedState(onVisibleFeedVideoIdsChanged)

    // 控制面板显示状态
    var isControlPanelVisible by remember { mutableStateOf(false) }
    var refreshFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var refreshFeedbackToken by remember { mutableIntStateOf(0) }

    val visibleAds = remember(uiState.ads, uiState.selectedTag) { uiState.filteredAds }
    val visibleAdIds = remember(visibleAds) { visibleAds.map { it.id } }
    val visibleAdIdSet = remember(visibleAdIds) { visibleAdIds.toSet() }
    val adsById = remember(visibleAds) { visibleAds.associateBy { it.id } }

    // 监听滚动位置，触发加载更多
    val shouldLoadMore = remember(
        selectedListState,
        uiState.listState,
        uiState.isRefreshing,
        uiState.isLoadingMore,
        uiState.hasMore,
        visibleAds.size
    ) {
        derivedStateOf {
            val lastVisibleItem = selectedListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = selectedListState.layoutInfo.totalItemsCount
            uiState.listState is FeedListState.Success &&
                !uiState.isRefreshing &&
                totalItems > 0 &&
                visibleAds.isNotEmpty() &&
                lastVisibleItem >= totalItems - 3 &&
                !uiState.isLoadingMore &&
                uiState.hasMore
        }
    }

    // 触发加载更多
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    LaunchedEffect(uiState.selectedChannel) {
        latestPlayingVideoAdId?.let(latestOnVideoAutoPause)
        if (pagerState.currentPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                channels.getOrNull(page)?.let { channel ->
                    if (channel != latestSelectedChannel) {
                        latestOnChannelSelect(channel)
                    }
                }
            }
    }

    // 简化曝光口径：广告 id 第一次进入 LazyColumn 可见范围即计 1 次曝光；
    // 不做 50% 可见和 1 秒停留判断，去重逻辑由 ViewModel/Repository 维护。
    LaunchedEffect(selectedListState, visibleAdIds) {
        snapshotFlow {
            selectedListState.layoutInfo.visibleItemsInfo
                .mapNotNull { it.key as? String }
                .filter { it in visibleAdIdSet }
                .distinct()
        }
            .distinctUntilChanged()
            .collect { visibleIds ->
                if (visibleIds.isNotEmpty()) {
                    onVisibleAdsChanged(visibleIds)
                }
                val visibleVideoIds = visibleIds.filter { adsById[it]?.cardType == AdCardType.VIDEO }
                latestOnVisibleFeedVideoIdsChanged(visibleVideoIds)
                latestPlayingVideoAdId?.let { playingId ->
                    if (playingId !in visibleIds) {
                        latestOnVideoAutoPause(playingId)
                    }
                }
            }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (uiState.isRefreshing) {
            latestPlayingVideoAdId?.let(latestOnVideoAutoPause)
        }
    }

    LaunchedEffect(selectedListState, visibleAdIds, adsById) {
        snapshotFlow {
            selectFeedVideoAutoplayCandidate(
                listState = selectedListState,
                adsById = adsById,
                manualPausedVideoAdIds = latestManualPausedVideoAdIds
            )
        }
            .distinctUntilChanged()
            .collect { candidateAdId ->
                if (candidateAdId == null) return@collect
                delay(FEED_VIDEO_AUTOPLAY_DELAY_MS)
                val stableCandidateAdId = selectFeedVideoAutoplayCandidate(
                    listState = selectedListState,
                    adsById = adsById,
                    manualPausedVideoAdIds = latestManualPausedVideoAdIds
                )
                if (
                    stableCandidateAdId == candidateAdId &&
                    latestPlayingVideoAdId != candidateAdId &&
                    candidateAdId !in latestManualPausedVideoAdIds
                ) {
                    latestOnVideoAutoPlay(candidateAdId)
                }
            }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { message ->
            refreshFeedbackMessage = message
            refreshFeedbackToken += 1
            onFeedbackShown()
        }
    }

    LaunchedEffect(refreshFeedbackToken) {
        if (refreshFeedbackToken > 0) {
            delay(REFRESH_FEEDBACK_DURATION_MS)
            refreshFeedbackMessage = null
        }
    }

    LaunchedEffect(uiState.refreshCompletedEventId) {
        if (uiState.refreshCompletedEventId > 0) {
            latestSelectedListState.scrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
    ) {
        // 顶部标题栏：避开系统状态栏，保持标题、副标题与 Tab 的稳定间距
        Surface(
            modifier = Modifier.statusBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI 广告推荐",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "精选可互动广告内容",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 频道 Tab
        PrimaryTabRow(
            selectedTabIndex = activeTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = pagerTabIndicatorOffset(
                        pagePosition = indicatorPagePosition
                    ),
                    width = Dp.Unspecified
                )
            }
        ) {
            channels.forEachIndexed { index, channel ->
                Tab(
                    selected = activeTabIndex == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier.height(38.dp),
                    text = {
                        Text(
                            text = channel.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (activeTabIndex == index) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                )
            }
        }

        SearchEntry(onClick = onSearchClick)
        RefreshFeedbackBanner(message = refreshFeedbackMessage)

        // Demo 控制面板
        if (SHOW_DEBUG_PANEL) {
            DemoControlPanel(
                isVisible = isControlPanelVisible,
                statsOverview = uiState.statsOverview,
                onToggleVisibility = { isControlPanelVisible = !isControlPanelVisible },
                onSimulateNormal = onSimulateNormal,
                onSimulateEmpty = onSimulateEmpty,
                onSimulateError = onSimulateError,
                onResetPagination = onResetPagination,
                onClearLocalAnalytics = onClearLocalAnalytics
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val pageChannel = channels[page]
            if (pageChannel == uiState.selectedChannel) {
                FeedPageContent(
                    listStateStatus = uiState.listState,
                    visibleAds = visibleAds,
                    selectedTag = uiState.selectedTag,
                    isRefreshing = uiState.isRefreshing,
                    isLoadingMore = uiState.isLoadingMore,
                    hasMore = uiState.hasMore,
                    listState = listStateForChannel(pageChannel),
                    onAdClick = onAdClick,
                    onLikeClick = onLikeClick,
                    onFavoriteClick = onFavoriteClick,
                    onShareClick = {
                        onShareClick(it)
                        showSingleToast(context, "已记录分享")
                    },
                    onTagClick = onTagClick,
                    onClearTagFilter = onClearTagFilter,
                    playingVideoAdId = playingVideoAdId,
                    videoPlaybackPositions = videoPlaybackPositions,
                    videoDurations = videoDurations,
                    videoSeekPositions = videoSeekPositions,
                    videoSeekRequestIds = videoSeekRequestIds,
                    onVideoClick = onVideoPlayToggle,
                    onVideoPlaybackUpdate = onVideoPlaybackUpdate,
                    onVideoPlaybackEnded = onVideoPlaybackEnded,
                    onVideoSeek = onVideoSeek,
                    onRefresh = onRefresh,
                    onRetry = onRetry
                )
            } else if (uiState.channelSnapshots.containsKey(pageChannel)) {
                val snapshot = uiState.channelSnapshots.getValue(pageChannel)
                FeedPageContent(
                    listStateStatus = snapshot.listState,
                    visibleAds = snapshot.ads,
                    selectedTag = null,
                    isRefreshing = false,
                    isLoadingMore = false,
                    hasMore = snapshot.hasMore,
                    listState = listStateForChannel(pageChannel),
                    onAdClick = onAdClick,
                    onLikeClick = onLikeClick,
                    onFavoriteClick = onFavoriteClick,
                    onShareClick = {
                        onShareClick(it)
                        showSingleToast(context, "已记录分享")
                    },
                    onTagClick = onTagClick,
                    onClearTagFilter = onClearTagFilter,
                    playingVideoAdId = null,
                    videoPlaybackPositions = videoPlaybackPositions,
                    videoDurations = videoDurations,
                    videoSeekPositions = videoSeekPositions,
                    videoSeekRequestIds = videoSeekRequestIds,
                    onVideoClick = {},
                    onVideoPlaybackUpdate = onVideoPlaybackUpdate,
                    onVideoPlaybackEnded = onVideoPlaybackEnded,
                    onVideoSeek = onVideoSeek,
                    onRefresh = {},
                    onRetry = onRetry
                )
            } else {
                SkeletonLoadingState()
            }
        }

        // 错误提示（Snackbar 风格）
        uiState.errorMessage?.let { message ->
            LaunchedEffect(message) {
                // 自动清除错误信息
                delay(3000)
                onClearError()
            }
        }
    }
}

private fun selectFeedVideoAutoplayCandidate(
    listState: LazyListState,
    adsById: Map<String, AdItem>,
    manualPausedVideoAdIds: Set<String>
): String? {
    val layoutInfo = listState.layoutInfo
    if (layoutInfo.visibleItemsInfo.isEmpty()) return null

    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return layoutInfo.visibleItemsInfo
        .mapNotNull { item ->
            item.feedVideoCandidateDistance(
                adsById = adsById,
                manualPausedVideoAdIds = manualPausedVideoAdIds,
                viewportCenter = viewportCenter
            )
        }
        .minByOrNull { it.distanceToViewportCenter }
        ?.adId
}

private data class FeedVideoCandidate(
    val adId: String,
    val distanceToViewportCenter: Int
)

private fun LazyListItemInfo.feedVideoCandidateDistance(
    adsById: Map<String, AdItem>,
    manualPausedVideoAdIds: Set<String>,
    viewportCenter: Int
): FeedVideoCandidate? {
    val adId = key as? String ?: return null
    val ad = adsById[adId] ?: return null
    if (ad.cardType != AdCardType.VIDEO || ad.videoAsset.isBlank() || adId in manualPausedVideoAdIds) {
        return null
    }
    val itemCenter = offset + size / 2
    return FeedVideoCandidate(
        adId = adId,
        distanceToViewportCenter = kotlin.math.abs(itemCenter - viewportCenter)
    )
}

private fun TabIndicatorScope.pagerTabIndicatorOffset(
    pagePosition: Float
): Modifier {
    return Modifier.tabIndicatorLayout { measurable, constraints, tabPositions ->
        if (tabPositions.isEmpty()) {
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            return@tabIndicatorLayout layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.placeRelative(0, constraints.maxHeight - placeable.height)
            }
        }

        val safePosition = pagePosition.coerceIn(0f, tabPositions.lastIndex.toFloat())
        val startIndex = floor(safePosition).toInt().coerceIn(0, tabPositions.lastIndex)
        val endIndex = ceil(safePosition).toInt().coerceIn(0, tabPositions.lastIndex)
        val fraction = (safePosition - startIndex).coerceIn(0f, 1f)
        val startPosition = tabPositions[startIndex]
        val endPosition = tabPositions[endIndex]
        val indicatorWidth = lerpDp(
            start = startPosition.contentWidth,
            stop = endPosition.contentWidth,
            fraction = fraction
        )
        val indicatorCenter = lerpDp(
            start = startPosition.left + startPosition.width / 2,
            stop = endPosition.left + endPosition.width / 2,
            fraction = fraction
        )
        val indicatorLeft = indicatorCenter - indicatorWidth / 2
        val widthPx = indicatorWidth.roundToPx()
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = widthPx,
                maxWidth = widthPx,
                minHeight = 0
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(
                x = indicatorLeft.roundToPx(),
                y = constraints.maxHeight - placeable.height
            )
        }
    }
}

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp {
    return start + (stop - start) * fraction
}

@Composable
private fun RefreshFeedbackBanner(
    message: String?
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedPageContent(
    listStateStatus: FeedListState,
    visibleAds: List<AdItem>,
    selectedTag: String?,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    listState: LazyListState,
    onAdClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onClearTagFilter: () -> Unit,
    playingVideoAdId: String?,
    videoPlaybackPositions: Map<String, Long>,
    videoDurations: Map<String, Long>,
    videoSeekPositions: Map<String, Long>,
    videoSeekRequestIds: Map<String, Long>,
    onVideoClick: (String) -> Unit,
    onVideoPlaybackUpdate: (String, Long, Long) -> Unit,
    onVideoPlaybackEnded: (String) -> Unit,
    onVideoSeek: (String, Long) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val bottomContentPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

    when (listStateStatus) {
        is FeedListState.Loading -> {
            SkeletonLoadingState()
        }
        is FeedListState.Error -> {
            ErrorState(
                message = listStateStatus.message,
                onRetry = onRetry
            )
        }
        is FeedListState.Empty -> {
            EmptyState()
        }
        is FeedListState.Success -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = bottomContentPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    selectedTag?.let { tag ->
                        item(key = "tag_filter") {
                            ActiveTagFilterBar(
                                tag = tag,
                                resultCount = visibleAds.size,
                                onClear = onClearTagFilter
                            )
                        }
                    }

                    if (isRefreshing) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "正在刷新...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(
                        items = visibleAds,
                        key = { it.id },
                        contentType = { it.cardType }
                    ) { ad ->
                        AdFeedCard(
                            ad = ad,
                            onClick = { onAdClick(ad.id) },
                            onLikeClick = { onLikeClick(ad.id) },
                            onFavoriteClick = { onFavoriteClick(ad.id) },
                            onShareClick = { onShareClick(ad.id) },
                            onTagClick = onTagClick,
                            onCtaClick = { showSingleToast(context, "${ad.ctaText}功能开发中") },
                            isVideoPlaying = ad.cardType == AdCardType.VIDEO && playingVideoAdId == ad.id,
                            videoPositionMs = videoPlaybackPositions[ad.id] ?: 0L,
                            videoDurationMs = videoDurations[ad.id] ?: 0L,
                            videoSeekPositionMs = videoSeekPositions[ad.id] ?: 0L,
                            videoSeekRequestId = videoSeekRequestIds[ad.id] ?: 0L,
                            onVideoClick = { onVideoClick(ad.id) },
                            onVideoPlaybackUpdate = { positionMs, durationMs ->
                                onVideoPlaybackUpdate(ad.id, positionMs, durationMs)
                            },
                            onVideoPlaybackEnded = { onVideoPlaybackEnded(ad.id) },
                            onVideoSeek = { positionMs -> onVideoSeek(ad.id, positionMs) },
                            onVideoError = {
                                if (playingVideoAdId == ad.id) {
                                    onVideoClick(ad.id)
                                }
                            }
                        )
                    }

                    if (selectedTag != null && visibleAds.isEmpty()) {
                        item(key = "empty_filter") {
                            EmptyFilterState(onClear = onClearTagFilter)
                        }
                    }

                    item {
                        LoadMoreIndicator(
                            isLoading = isLoadingMore,
                            hasMore = hasMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEntry(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .height(38.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "搜索你想看的广告...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 当前标签筛选提示
 */
@Composable
private fun ActiveTagFilterBar(
    tag: String,
    resultCount: Int,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "筛选",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$resultCount 条",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("清除筛选")
            }
        }
    }
}

/**
 * 标签筛选空结果
 */
@Composable
private fun EmptyFilterState(
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "没有找到相关广告",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "换个标签或清除筛选再看看",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onClear) {
            Text("清除筛选")
        }
    }
}

/**
 * 骨架屏加载状态
 */
@Composable
private fun SkeletonLoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            top = 12.dp,
            end = 14.dp,
            bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) { index ->
            if (index % 2 == 0) {
                SkeletonCard()
            } else {
                SmallImageSkeletonCard()
            }
        }
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无推荐内容",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "下拉刷新试试",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 错误状态
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

/**
 * 加载更多指示器
 */
@Composable
private fun LoadMoreIndicator(
    isLoading: Boolean,
    hasMore: Boolean
) {
    if (isLoading) {
        // 加载中
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "正在加载更多",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (!hasMore) {
        // 没有更多数据
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "没有更多了",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
