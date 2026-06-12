package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.AiSearchRepository
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
private data class SavedListPosition(
    val channel: AdChannel,
    val anchorAdId: String?,
    val index: Int,
    val offset: Int
)

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
    var searchResultAdIds by remember { mutableStateOf(emptyList<String>()) }
    var hasSearched by remember { mutableStateOf(false) }
    var isAiSearchLoading by remember { mutableStateOf(false) }
    var aiSearchMessage by remember { mutableStateOf("") }
    var aiSuggestedRefinements by remember { mutableStateOf(emptyList<String>()) }
    var aiClarifyQuestion by remember { mutableStateOf("") }
    var completedSearchQuery by remember { mutableStateOf("") }
    var latestSearchRequestId by remember { mutableStateOf(0L) }
    var detailRefreshKey by remember { mutableIntStateOf(0) }

    // Video state is intentionally kept at the app level so feed, search, and detail share one timeline per ad.
    // playingFeedVideoAdId is the currently playing feed card; autoPlayingFeedVideoAdId marks the subset started by
    // feed autoplay preview; manualPausedFeedVideoAdIds only blocks autoplay while that card remains visible.
    // detailAutoPlayVideoAdId carries the user's play intent into detail: paused or seek-only videos keep position
    // but do not auto-play. Position/duration are keyed by adId. Seek uses request ids because seeking to the same
    // millisecond twice must still be observable by LocalVideoPlayer across recompositions.
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
    val searchResultListState = rememberLazyListState()
    val currentListState = when (uiState.selectedChannel) {
        AdChannel.FEATURED -> featuredListState
        AdChannel.ECOMMERCE -> ecommerceListState
        AdChannel.LOCAL -> localListState
    }
    var tagFilterReturnPosition by remember { mutableStateOf<SavedListPosition?>(null) }
    var pendingTagFilterRestorePosition by remember { mutableStateOf<SavedListPosition?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun clearSearchState(clearQuery: Boolean = false) {
        latestSearchRequestId++
        if (clearQuery) {
            searchQuery = ""
        }
        searchResults = emptyList()
        searchResultAdIds = emptyList()
        hasSearched = false
        isAiSearchLoading = false
        aiSearchMessage = ""
        aiSuggestedRefinements = emptyList()
        aiClarifyQuestion = ""
        completedSearchQuery = ""
    }

    fun updateSearchQuery(newQuery: String) {
        val previousQuery = searchQuery.trim()
        searchQuery = newQuery
        if (isAiSearchLoading && newQuery.trim() != previousQuery) {
            latestSearchRequestId++
            searchResults = emptyList()
            searchResultAdIds = emptyList()
            hasSearched = false
            isAiSearchLoading = false
            aiSearchMessage = ""
            aiSuggestedRefinements = emptyList()
            aiClarifyQuestion = ""
            completedSearchQuery = ""
            return
        }

        val hasDirtyCompletedSearch = completedSearchQuery.isNotBlank() &&
            newQuery.trim() != completedSearchQuery
        if (hasDirtyCompletedSearch && !isAiSearchLoading) {
            searchResults = emptyList()
            searchResultAdIds = emptyList()
            hasSearched = false
            aiSearchMessage = ""
            aiSuggestedRefinements = emptyList()
            aiClarifyQuestion = ""
        }
    }

    fun saveTagFilterReturnPosition() {
        val adIds = uiState.ads.mapTo(mutableSetOf()) { it.id }
        val anchorItem = currentListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> (item.key as? String) in adIds }
        val anchorAdId = anchorItem?.key as? String
        val anchorOffset = anchorItem?.let { item ->
            (currentListState.layoutInfo.viewportStartOffset - item.offset).coerceAtLeast(0)
        } ?: currentListState.firstVisibleItemScrollOffset
        tagFilterReturnPosition = SavedListPosition(
            channel = uiState.selectedChannel,
            anchorAdId = anchorAdId,
            index = currentListState.firstVisibleItemIndex,
            offset = anchorOffset
        )
    }

    fun runSearch(query: String) {
        val trimmedQuery = query.trim()
        searchQuery = query
        hasSearched = true
        searchResults = emptyList()
        searchResultAdIds = emptyList()
        aiSearchMessage = ""
        aiSuggestedRefinements = emptyList()
        aiClarifyQuestion = ""
        completedSearchQuery = ""

        if (trimmedQuery.isBlank()) {
            clearSearchState(clearQuery = false)
            return
        }

        val requestId = latestSearchRequestId + 1L
        latestSearchRequestId = requestId
        isAiSearchLoading = true

        coroutineScope.launch {
            val aiResult = AiSearchRepository.aiSearch(
                query = trimmedQuery,
                currentChannel = uiState.selectedChannel,
                limit = 12
            )
            if (latestSearchRequestId != requestId) return@launch

            aiResult
                .onSuccess { response ->
                    if (response.type == "clarify") {
                        searchResultAdIds = emptyList()
                        searchResults = emptyList()
                        aiClarifyQuestion = response.question
                        aiSuggestedRefinements = response.suggestedRefinements
                        completedSearchQuery = trimmedQuery
                    } else {
                        searchResultAdIds = response.matchedAdIds
                        searchResults = response.matchedAdIds.mapNotNull { adId ->
                            viewModel.getAdById(adId)
                        }
                        aiSearchMessage = response.explanation
                        aiSuggestedRefinements = response.suggestedRefinements
                        completedSearchQuery = trimmedQuery
                    }
                }
                .onFailure {
                    val fallbackResults = viewModel.searchAds(trimmedQuery)
                    searchResults = fallbackResults
                    searchResultAdIds = fallbackResults.map { ad -> ad.id }
                    aiSearchMessage = "AI 搜索暂时不可用，已使用本地搜索结果。"
                    aiSuggestedRefinements = emptyList()
                    aiClarifyQuestion = ""
                    completedSearchQuery = trimmedQuery
                }

            isAiSearchLoading = false
            coroutineScope.launch {
                searchResultListState.scrollToItem(0)
            }
        }
    }

    fun refreshSearchResults() {
        if (hasSearched && searchQuery.isNotBlank()) {
            searchResults = if (searchResultAdIds.isEmpty()) {
                emptyList()
            } else {
                searchResultAdIds.mapNotNull { adId -> viewModel.getAdById(adId) }
            }
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

    BackHandler(
        enabled = selectedAd == null && !isSearchVisible && uiState.selectedTag != null
    ) {
        pendingTagFilterRestorePosition = tagFilterReturnPosition
        tagFilterReturnPosition = null
        viewModel.clearTagFilter()
    }

    LaunchedEffect(
        uiState.selectedTag,
        uiState.selectedChannel,
        uiState.ads,
        pendingTagFilterRestorePosition
    ) {
        val returnPosition = pendingTagFilterRestorePosition
        if (uiState.selectedTag == null && returnPosition != null) {
            pendingTagFilterRestorePosition = null
            if (returnPosition.channel == uiState.selectedChannel && uiState.ads.isNotEmpty()) {
                val anchorIndex = returnPosition.anchorAdId?.let { anchorAdId ->
                    uiState.ads.indexOfFirst { ad -> ad.id == anchorAdId }.takeIf { it >= 0 }
                }
                val safeIndex = anchorIndex ?: returnPosition.index.coerceIn(0, uiState.ads.lastIndex)
                currentListState.scrollToItem(
                    index = safeIndex,
                    scrollOffset = returnPosition.offset.coerceAtLeast(0)
                )
            }
        }
    }

    // Feed autoplay is only a lightweight preview. When that preview ends or leaves the screen, it may reset to 0
    // so the next impression starts cleanly. User-driven play, seek, or navigation to detail must not use this path.
    fun resetFeedVideoPreview(adId: String) {
        videoPlaybackPositions = videoPlaybackPositions + (adId to 0L)
        videoSeekPositions = videoSeekPositions + (adId to 0L)
        videoSeekRequestIds = videoSeekRequestIds + (
            adId to ((videoSeekRequestIds[adId] ?: 0L) + 1L)
        )
    }

    fun openAdDetail(adId: String, shouldAutoPlayInDetail: Boolean) {
        // Keep videoPlaybackPositions[adId] as the source of truth for detail initial position.
        // Clear only pending seek requests for this ad: a stale feed preview reset request, e.g. seek-to-0,
        // would otherwise run after detail initialization and override the preserved resume position.
        detailAutoPlayVideoAdId = if (shouldAutoPlayInDetail) adId else null
        playingFeedVideoAdId = null
        autoPlayingFeedVideoAdId = null
        videoSeekPositions = videoSeekPositions - adId
        videoSeekRequestIds = videoSeekRequestIds - adId
        viewModel.recordClick(adId)
        refreshSearchResults()
        detailRefreshKey++
        selectedAdId = adId
    }

    val onAdClick: (String) -> Unit = { adId ->
        openAdDetail(
            adId = adId,
            shouldAutoPlayInDetail = playingFeedVideoAdId == adId
        )
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
                saveTagFilterReturnPosition()
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
            isAiSearchLoading = isAiSearchLoading,
            aiSearchMessage = aiSearchMessage,
            aiSuggestedRefinements = aiSuggestedRefinements,
            aiClarifyQuestion = aiClarifyQuestion,
            searchResultListState = searchResultListState,
            videoPlaybackPositions = videoPlaybackPositions,
            videoDurations = videoDurations,
            videoSeekPositions = videoSeekPositions,
            videoSeekRequestIds = videoSeekRequestIds,
            onQueryChange = { updateSearchQuery(it) },
            onSearch = { runSearch(it) },
            onRefinementClick = { runSearch(it) },
            onBack = {
                isSearchVisible = false
                clearSearchState(clearQuery = true)
            },
            // Search videos do not autoplay in the results list, but they share global position/duration/seek state
            // with feed and detail. The Boolean records whether the user was actively playing before navigation.
            onAdClick = { adId, shouldAutoPlayInDetail ->
                openAdDetail(
                    adId = adId,
                    shouldAutoPlayInDetail = shouldAutoPlayInDetail
                )
            },
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
            onVideoPlaybackUpdate = { adId, positionMs, durationMs ->
                videoPlaybackPositions = videoPlaybackPositions + (adId to positionMs)
                if (durationMs > 0L) {
                    videoDurations = videoDurations + (adId to durationMs)
                }
            },
            onVideoPlaybackEnded = { adId ->
                val durationMs = videoDurations[adId] ?: 0L
                if (durationMs > 0L) {
                    videoPlaybackPositions = videoPlaybackPositions + (adId to durationMs)
                }
            },
            onVideoSeek = { adId, positionMs ->
                videoPlaybackPositions = videoPlaybackPositions + (adId to positionMs)
                videoSeekPositions = videoSeekPositions + (adId to positionMs)
                videoSeekRequestIds = videoSeekRequestIds + (
                    adId to ((videoSeekRequestIds[adId] ?: 0L) + 1L)
                )
            },
            onTagClick = {
                saveTagFilterReturnPosition()
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
                saveTagFilterReturnPosition()
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
