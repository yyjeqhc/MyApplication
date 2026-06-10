package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem
import com.example.myapplication.ui.detail.AdDetailScreen
import com.example.myapplication.ui.feed.FeedScreen
import com.example.myapplication.ui.search.SearchScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.FeedViewModel
import kotlinx.coroutines.launch

/**
 * 应用主 Activity
 * 管理全局状态和页面切换
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdApp()
                }
            }
        }
    }
}

/**
 * 广告应用主组件
 * 管理广告列表状态和页面切换逻辑
 */
@Composable
fun AdApp(
    viewModel: FeedViewModel = viewModel()
) {
    // 从 ViewModel 获取 UI 状态
    val uiState by viewModel.uiState.collectAsState()

    // 当前选中的广告 ID（用于详情页展示）
    var selectedAdId by remember { mutableStateOf<String?>(null) }

    // 搜索页状态，独立于信息流列表，避免影响频道缓存和滚动位置
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<AdItem>()) }
    var hasSearched by remember { mutableStateOf(false) }
    var detailRefreshKey by remember { mutableIntStateOf(0) }
    var playingFeedVideoAdId by remember { mutableStateOf<String?>(null) }
    var autoPlayingFeedVideoAdId by remember { mutableStateOf<String?>(null) }
    var manualPausedFeedVideoAdIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var detailAutoPlayVideoAdId by remember { mutableStateOf<String?>(null) }
    var videoPlaybackPositions by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var videoDurations by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var videoSeekPositions by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var videoSeekRequestIds by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    // 每个频道独立保存滚动状态，避免 Tab 切换后丢失位置
    val featuredListState = rememberLazyListState()
    val ecommerceListState = rememberLazyListState()
    val localListState = rememberLazyListState()
    val currentListState = when (uiState.selectedChannel) {
        AdChannel.FEATURED -> featuredListState
        AdChannel.ECOMMERCE -> ecommerceListState
        AdChannel.LOCAL -> localListState
    }

    val coroutineScope = rememberCoroutineScope()

    fun runSearch(query: String) {
        val trimmedQuery = query.trim()
        searchQuery = query
        hasSearched = true
        searchResults = if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            viewModel.searchAds(trimmedQuery)
        }
    }

    fun refreshSearchResults() {
        if (hasSearched && searchQuery.isNotBlank()) {
            searchResults = viewModel.searchAds(searchQuery.trim())
        }
    }

    // 根据 selectedAdId 获取最新的广告对象
    val selectedAd = remember(uiState.ads, searchResults, selectedAdId, detailRefreshKey) {
        selectedAdId?.let { id ->
            viewModel.getAdById(id)
                ?: uiState.ads.find { it.id == id }
                ?: searchResults.find { it.id == id }
        }
    }

    // 点击广告卡片回调
    fun resetFeedVideoPreview(adId: String) {
        videoPlaybackPositions = videoPlaybackPositions + (adId to 0L)
        videoSeekPositions = videoSeekPositions + (adId to 0L)
        videoSeekRequestIds = videoSeekRequestIds + (
            adId to ((videoSeekRequestIds[adId] ?: 0L) + 1L)
        )
    }

    val onAdClick: (String) -> Unit = { adId ->
        detailAutoPlayVideoAdId = if (playingFeedVideoAdId == adId) adId else null
        playingFeedVideoAdId = null
        autoPlayingFeedVideoAdId = null
        videoSeekPositions = videoSeekPositions - adId
        videoSeekRequestIds = videoSeekRequestIds - adId
        viewModel.recordClick(adId)
        refreshSearchResults()
        detailRefreshKey++
        selectedAdId = adId
    }

    // 返回列表回调
    val onBack: () -> Unit = {
        selectedAdId = null
    }

    // 页面切换逻辑
    if (selectedAd != null) {
        // 显示详情页
        AdDetailScreen(
            ad = selectedAd,
            initialVideoPositionMs = videoPlaybackPositions[selectedAd.id] ?: 0L,
            initialVideoDurationMs = videoDurations[selectedAd.id] ?: 0L,
            videoSeekPositionMs = videoSeekPositions[selectedAd.id] ?: 0L,
            videoSeekRequestId = videoSeekRequestIds[selectedAd.id] ?: 0L,
            autoPlayVideo = detailAutoPlayVideoAdId == selectedAd.id,
            onVideoPlaybackUpdate = { positionMs, durationMs ->
                videoPlaybackPositions = videoPlaybackPositions + (selectedAd.id to positionMs)
                if (durationMs > 0L) {
                    videoDurations = videoDurations + (selectedAd.id to durationMs)
                }
            },
            onVideoPlaybackEnded = {
                val durationMs = videoDurations[selectedAd.id] ?: 0L
                if (durationMs > 0L) {
                    videoPlaybackPositions = videoPlaybackPositions + (selectedAd.id to durationMs)
                }
            },
            onVideoSeek = { positionMs ->
                videoPlaybackPositions = videoPlaybackPositions + (selectedAd.id to positionMs)
                videoSeekPositions = videoSeekPositions + (selectedAd.id to positionMs)
                videoSeekRequestIds = videoSeekRequestIds + (
                    selectedAd.id to ((videoSeekRequestIds[selectedAd.id] ?: 0L) + 1L)
                )
            },
            onBack = onBack,
            onLikeClick = {
                viewModel.toggleLike(selectedAd.id)
                refreshSearchResults()
                detailRefreshKey++
            },
            onFavoriteClick = {
                viewModel.toggleFavorite(selectedAd.id)
                refreshSearchResults()
                detailRefreshKey++
            },
            onShareClick = {
                viewModel.recordShare(selectedAd.id)
                refreshSearchResults()
                detailRefreshKey++
            },
            onCtaClick = {
                // CTA 只做用户反馈，不计入列表点击统计，避免污染 CTR。
            },
            onTagClick = { tag ->
                viewModel.selectTag(tag)
                isSearchVisible = false
                selectedAdId = null
                coroutineScope.launch {
                    currentListState.animateScrollToItem(0)
                }
            }
        )
    } else if (isSearchVisible) {
        SearchScreen(
            query = searchQuery,
            results = searchResults,
            hasSearched = hasSearched,
            onQueryChange = { searchQuery = it },
            onSearch = { runSearch(it) },
            onBack = { isSearchVisible = false },
            onAdClick = onAdClick,
            onLikeClick = {
                viewModel.toggleLike(it)
                refreshSearchResults()
            },
            onFavoriteClick = {
                viewModel.toggleFavorite(it)
                refreshSearchResults()
            },
            onShareClick = {
                viewModel.recordShare(it)
                refreshSearchResults()
            },
            onTagClick = {
                viewModel.selectTag(it)
                isSearchVisible = false
                coroutineScope.launch {
                    currentListState.animateScrollToItem(0)
                }
            }
        )
    } else {
        // 显示信息流页面
        FeedScreen(
            uiState = uiState,
            playingVideoAdId = playingFeedVideoAdId,
            manualPausedVideoAdIds = manualPausedFeedVideoAdIds,
            videoPlaybackPositions = videoPlaybackPositions,
            videoDurations = videoDurations,
            videoSeekPositions = videoSeekPositions,
            videoSeekRequestIds = videoSeekRequestIds,
            listStateForChannel = { channel ->
                when (channel) {
                    AdChannel.FEATURED -> featuredListState
                    AdChannel.ECOMMERCE -> ecommerceListState
                    AdChannel.LOCAL -> localListState
                }
            },
            onAdClick = onAdClick,
            onLikeClick = {
                viewModel.toggleLike(it)
                refreshSearchResults()
            },
            onFavoriteClick = {
                viewModel.toggleFavorite(it)
                refreshSearchResults()
            },
            onShareClick = {
                viewModel.recordShare(it)
                refreshSearchResults()
            },
            onVideoPlayToggle = { adId ->
                if (playingFeedVideoAdId == adId) {
                    playingFeedVideoAdId = null
                    if (autoPlayingFeedVideoAdId == adId) {
                        resetFeedVideoPreview(adId)
                    }
                    autoPlayingFeedVideoAdId = null
                    manualPausedFeedVideoAdIds = manualPausedFeedVideoAdIds + adId
                } else {
                    playingFeedVideoAdId = adId
                    autoPlayingFeedVideoAdId = null
                    manualPausedFeedVideoAdIds = manualPausedFeedVideoAdIds - adId
                }
            },
            onVideoAutoPlay = { adId ->
                if (adId !in manualPausedFeedVideoAdIds) {
                    resetFeedVideoPreview(adId)
                    playingFeedVideoAdId = adId
                    autoPlayingFeedVideoAdId = adId
                }
            },
            onVideoAutoPause = { adId ->
                if (playingFeedVideoAdId == adId) {
                    playingFeedVideoAdId = null
                }
                if (autoPlayingFeedVideoAdId == adId) {
                    autoPlayingFeedVideoAdId = null
                    resetFeedVideoPreview(adId)
                }
            },
            onVisibleFeedVideoIdsChanged = { visibleVideoIds ->
                val visibleVideoIdSet = visibleVideoIds.toSet()
                manualPausedFeedVideoAdIds = manualPausedFeedVideoAdIds.intersect(visibleVideoIdSet)
            },
            onVideoPlaybackUpdate = { adId, positionMs, durationMs ->
                videoPlaybackPositions = videoPlaybackPositions + (adId to positionMs)
                if (durationMs > 0L) {
                    videoDurations = videoDurations + (adId to durationMs)
                }
            },
            onVideoPlaybackEnded = { adId ->
                if (playingFeedVideoAdId == adId) {
                    playingFeedVideoAdId = null
                }
                if (autoPlayingFeedVideoAdId == adId) {
                    autoPlayingFeedVideoAdId = null
                    resetFeedVideoPreview(adId)
                } else {
                    val durationMs = videoDurations[adId] ?: 0L
                    if (durationMs > 0L) {
                        videoPlaybackPositions = videoPlaybackPositions + (adId to durationMs)
                    }
                }
            },
            onVideoSeek = { adId, positionMs ->
                if (autoPlayingFeedVideoAdId == adId) {
                    autoPlayingFeedVideoAdId = null
                }
                videoPlaybackPositions = videoPlaybackPositions + (adId to positionMs)
                videoSeekPositions = videoSeekPositions + (adId to positionMs)
                videoSeekRequestIds = videoSeekRequestIds + (
                    adId to ((videoSeekRequestIds[adId] ?: 0L) + 1L)
                )
            },
            onTagClick = {
                viewModel.selectTag(it)
                coroutineScope.launch {
                    currentListState.animateScrollToItem(0)
                }
            },
            onClearTagFilter = {
                viewModel.clearTagFilter()
                coroutineScope.launch {
                    currentListState.animateScrollToItem(0)
                }
            },
            onChannelSelect = { viewModel.selectChannel(it) },
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMore() },
            onVisibleAdsChanged = { viewModel.recordExposures(it) },
            onRetry = { viewModel.retry() },
            onClearError = { viewModel.clearError() },
            onFeedbackShown = { viewModel.clearFeedbackMessage() },
            onSearchClick = { isSearchVisible = true },
            onSimulateNormal = { viewModel.simulateNormal() },
            onSimulateEmpty = { viewModel.simulateEmptyState() },
            onSimulateError = { viewModel.simulateErrorState() },
            onResetPagination = { viewModel.resetPagination() },
            onClearLocalAnalytics = { viewModel.clearLocalAnalytics() }
        )
    }
}
