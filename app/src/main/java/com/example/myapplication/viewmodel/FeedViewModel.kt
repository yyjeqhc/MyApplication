package com.example.myapplication.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.MockAdRepository
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem
import com.example.myapplication.model.AdStatsOverview
import com.example.myapplication.model.FeedChannelSnapshot
import com.example.myapplication.model.FeedListState
import com.example.myapplication.model.FeedUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 信息流 ViewModel
 * 管理页面状态、刷新、加载更多等业务逻辑
 */
class FeedViewModel(
    application: Application
) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "FeedViewModel"
    }

    /** UI 状态 */
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    /** 模拟错误开关（用于测试错误状态） */
    private var simulateError = false

    /** 模拟空数据开关 */
    private var simulateEmpty = false

    /** 每页数据量 */
    private val pageSize = 6

    /** 每个频道当前已加载的数据缓存 */
    private val channelCache = mutableMapOf<AdChannel, ChannelFeedCache>()

    /** 每个频道的 mock 刷新轮次，用于让下拉刷新产生稳定但可见的重排效果。 */
    private val channelRefreshRounds = mutableMapOf<AdChannel, Int>()

    /** 防止首屏后的频道预加载重复启动。 */
    private var hasPreloadedOtherChannels = false

    /** Repository 初始化完成前，避免 UI 交互误触发 fallback 数据。 */
    private var repositoryReady = false

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                MockAdRepository.initialize(application.applicationContext)
            }
            repositoryReady = true
            Log.i(
                TAG,
                "Repository ready. fromAssets=${MockAdRepository.isLoadedFromAssets()}, " +
                    "total=${MockAdRepository.ads.size}, " +
                    AdChannel.entries.joinToString { "${it.name}=${MockAdRepository.getChannelCount(it)}" }
            )
            _uiState.update {
                it.copy(statsOverview = currentStatsOverview())
            }

            // 首次加载数据
            loadInitialData()
        }
    }

    /**
     * 首次加载数据
     */
    private fun loadInitialData() {
        if (!repositoryReady) return

        viewModelScope.launch {
            val channel = _uiState.value.selectedChannel
            val cached = channelCache[channel]
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        ads = cached.ads,
                        listState = cached.toListState(),
                        currentPage = cached.currentPage,
                        hasMore = cached.hasMore,
                        errorMessage = null,
                        isRefreshing = false,
                        isLoadingMore = false,
                        channelSnapshots = currentChannelSnapshots(),
                        statsOverview = currentStatsOverview()
                    )
                }
                return@launch
            }

            if (_uiState.value.selectedChannel != channel) return@launch

            if (simulateError) {
                _uiState.update {
                    it.copy(
                        listState = FeedListState.Error("加载失败，请重试"),
                        errorMessage = "加载失败，请重试"
                    )
                }
                return@launch
            }

            val page = loadPage(channel, page = 1)
            saveChannelCache(channel, page.ads, currentPage = page.currentPage, hasMore = page.hasMore)

            _uiState.update {
                it.copy(
                    ads = page.ads,
                    listState = if (page.ads.isEmpty()) FeedListState.Empty else FeedListState.Success,
                    currentPage = page.currentPage,
                    hasMore = page.hasMore,
                    errorMessage = null,
                    channelSnapshots = currentChannelSnapshots(),
                    statsOverview = currentStatsOverview()
                )
            }
            preloadOtherChannelsAfterFirstPage(channel)
        }
    }

    /**
     * 切换频道
     */
    fun selectChannel(channel: AdChannel) {
        if (channel == _uiState.value.selectedChannel) return
        if (!repositoryReady) {
            _uiState.update {
                it.copy(
                    selectedChannel = channel,
                    selectedTag = null,
                    listState = FeedListState.Loading,
                    errorMessage = null,
                    isRefreshing = false,
                    isLoadingMore = false
                )
            }
            return
        }

        if (simulateError) {
            _uiState.update {
                it.copy(
                    selectedChannel = channel,
                    selectedTag = null,
                    listState = FeedListState.Error("加载失败，请重试"),
                    errorMessage = "加载失败，请重试",
                    isRefreshing = false,
                    isLoadingMore = false
                )
            }
            return
        }

        val cached = channelCache[channel]
        if (cached != null) {
            _uiState.update {
                it.copy(
                    selectedChannel = channel,
                    selectedTag = null,
                    ads = cached.ads,
                    currentPage = cached.currentPage,
                    hasMore = cached.hasMore,
                    listState = cached.toListState(),
                    errorMessage = null,
                    isRefreshing = false,
                    isLoadingMore = false,
                    channelSnapshots = currentChannelSnapshots(),
                    statsOverview = currentStatsOverview()
                )
            }
            return
        }

        val page = loadPage(channel, page = 1)
        saveChannelCache(channel, page.ads, currentPage = page.currentPage, hasMore = page.hasMore)

        _uiState.update {
            it.copy(
                selectedChannel = channel,
                selectedTag = null,
                ads = page.ads,
                currentPage = page.currentPage,
                hasMore = page.hasMore,
                listState = if (page.ads.isEmpty()) FeedListState.Empty else FeedListState.Success,
                isRefreshing = false,
                isLoadingMore = false,
                errorMessage = null,
                channelSnapshots = currentChannelSnapshots(),
                statsOverview = currentStatsOverview()
            )
        }
    }

    /**
     * 按智能标签筛选当前频道广告
     */
    fun selectTag(tag: String) {
        _uiState.update {
            it.copy(selectedTag = tag)
        }
    }

    /**
     * 清除智能标签筛选
     */
    fun clearTagFilter() {
        _uiState.update {
            it.copy(selectedTag = null)
        }
    }

    /**
     * 下拉刷新
     */
    fun refresh() {
        if (!repositoryReady) return
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            val channel = _uiState.value.selectedChannel
            _uiState.update { it.copy(isRefreshing = true) }

            if (_uiState.value.selectedChannel != channel) return@launch

            if (simulateError) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = "刷新失败"
                    )
                }
                return@launch
            }

            advanceRefreshRound(channel)
            val page = loadFirstPageForRefresh(
                channel = channel,
                selectedTag = _uiState.value.selectedTag
            )
            saveChannelCache(channel, page.ads, currentPage = page.currentPage, hasMore = page.hasMore)

            _uiState.update {
                it.copy(
                    ads = page.ads,
                    isRefreshing = false,
                    currentPage = page.currentPage,
                    hasMore = page.hasMore,
                    listState = if (page.ads.isEmpty()) FeedListState.Empty else FeedListState.Success,
                    errorMessage = null,
                    feedbackMessage = if (page.ads.isEmpty()) null else "已刷新推荐内容",
                    refreshCompletedEventId = it.refreshCompletedEventId + 1,
                    channelSnapshots = currentChannelSnapshots(),
                    statsOverview = currentStatsOverview()
                )
            }
        }
    }

    /**
     * 加载更多
     */
    fun loadMore() {
        if (!repositoryReady) return
        val currentState = _uiState.value
        if (
            currentState.isLoadingMore ||
            !currentState.hasMore ||
            (currentState.selectedTag != null && currentState.filteredAds.isEmpty())
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            if (simulateError) {
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = "加载更多失败"
                    )
                }
                return@launch
            }

            val channel = currentState.selectedChannel
            val currentPage = currentState.currentPage
            if (_uiState.value.selectedChannel != channel) return@launch

            val page = loadPage(channel, currentPage + 1)
            val mergedAds = mergeAdsById(_uiState.value.ads, page.ads)
            saveChannelCache(channel, mergedAds, page.currentPage, page.hasMore)

            _uiState.update {
                it.copy(
                    ads = mergedAds,
                    isLoadingMore = false,
                    currentPage = page.currentPage,
                    hasMore = page.hasMore,
                    errorMessage = null,
                    channelSnapshots = currentChannelSnapshots(),
                    statsOverview = currentStatsOverview()
                )
            }
        }
    }

    /**
     * 批量记录广告曝光。
     */
    fun recordExposures(adIds: Collection<String>) {
        if (!repositoryReady) return
        val updatedAds = adIds.mapNotNull { adId ->
            MockAdRepository.recordExposure(adId)
        }
        syncAds(updatedAds)
    }

    /**
     * 记录广告点击。
     */
    fun recordClick(adId: String) {
        if (!repositoryReady) return
        MockAdRepository.recordClick(adId)?.let { updatedAd ->
            syncAd(updatedAd)
        }
    }

    /**
     * 记录广告分享。
     */
    fun recordShare(adId: String) {
        if (!repositoryReady) return
        MockAdRepository.recordShare(adId)?.let { updatedAd ->
            syncAd(updatedAd)
        }
    }

    /**
     * 切换点赞状态
     */
    fun toggleLike(adId: String) {
        if (!repositoryReady) return
        MockAdRepository.toggleLike(adId)

        MockAdRepository.getAdById(adId)?.let { updatedAd ->
            syncAd(updatedAd)
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(adId: String) {
        if (!repositoryReady) return
        MockAdRepository.toggleFavorite(adId)

        MockAdRepository.getAdById(adId)?.let { updatedAd ->
            syncAd(updatedAd)
        }
    }

    /**
     * 对话式搜索的本地规则入口。
     */
    fun searchAds(query: String): List<AdItem> {
        if (!repositoryReady) return emptyList()
        return MockAdRepository.searchAds(query)
    }

    /**
     * 从完整本地数据集中取最新广告状态，供搜索详情页使用。
     */
    fun getAdById(adId: String): AdItem? {
        if (!repositoryReady) return null
        return MockAdRepository.getAdById(adId)
    }

    /**
     * 切换错误模拟（用于测试）
     */
    fun toggleErrorSimulation() {
        simulateError = !simulateError
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 清除一次性轻量反馈。
     */
    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    /**
     * 重试加载（从错误状态恢复）
     */
    fun retry() {
        if (!repositoryReady) return
        _uiState.update { it.copy(errorMessage = null) }
        loadInitialData()
    }

    // ==================== Demo 控制面板方法 ====================

    /**
     * 模拟正常状态
     */
    fun simulateNormal() {
        simulateError = false
        simulateEmpty = false
        channelCache.clear()
        channelRefreshRounds.clear()
        hasPreloadedOtherChannels = false
        _uiState.update {
            it.copy(channelSnapshots = emptyMap())
        }
        loadInitialData()
    }

    /**
     * 模拟空状态
     */
    fun simulateEmptyState() {
        simulateError = false
        simulateEmpty = true
        saveChannelCache(_uiState.value.selectedChannel, emptyList(), currentPage = 1, hasMore = false)
        _uiState.update {
            it.copy(
                ads = emptyList(),
                selectedTag = null,
                listState = FeedListState.Empty,
                currentPage = 1,
                hasMore = false,
                errorMessage = null,
                channelSnapshots = currentChannelSnapshots(),
                statsOverview = currentStatsOverview()
            )
        }
    }

    /**
     * 模拟错误状态
     */
    fun simulateErrorState() {
        simulateError = true
        simulateEmpty = false
        _uiState.update {
            it.copy(
                selectedTag = null,
                listState = FeedListState.Error("模拟的加载错误"),
                errorMessage = "模拟的加载错误"
            )
        }
    }

    /**
     * 重置分页数据
     */
    fun resetPagination() {
        simulateError = false
        simulateEmpty = false
        MockAdRepository.reset(getApplication<Application>().applicationContext)
        channelCache.clear()
        channelRefreshRounds.clear()
        hasPreloadedOtherChannels = false
        _uiState.update {
            it.copy(
                channelSnapshots = emptyMap(),
                statsOverview = currentStatsOverview()
            )
        }
        loadInitialData()
    }

    /**
     * 清空本机持久化统计，并刷新当前 UI 到 JSON 初始统计。
     */
    fun clearLocalAnalytics() {
        simulateError = false
        simulateEmpty = false
        MockAdRepository.clearLocalAnalytics(getApplication<Application>().applicationContext)
        channelCache.clear()
        channelRefreshRounds.clear()
        hasPreloadedOtherChannels = false
        _uiState.update {
            it.copy(
                channelSnapshots = emptyMap(),
                statsOverview = currentStatsOverview()
            )
        }
        loadInitialData()
    }

    private fun loadPage(channel: AdChannel, page: Int): MockAdRepository.PagedAds {
        return if (simulateEmpty) {
            MockAdRepository.PagedAds(
                ads = emptyList(),
                currentPage = 1,
                hasMore = false,
                totalCount = 0
            )
        } else {
            val orderedAds = getOrderedAdsForChannel(channel)
            val safePage = page.coerceAtLeast(1)
            val fromIndex = (safePage - 1) * pageSize
            if (fromIndex >= orderedAds.size) {
                return MockAdRepository.PagedAds(
                    ads = emptyList(),
                    currentPage = safePage,
                    hasMore = false,
                    totalCount = orderedAds.size
                )
            }

            val toIndex = (fromIndex + pageSize).coerceAtMost(orderedAds.size)
            MockAdRepository.PagedAds(
                ads = orderedAds.subList(fromIndex, toIndex),
                currentPage = safePage,
                hasMore = toIndex < orderedAds.size,
                totalCount = orderedAds.size
            )
        }
    }

    private fun preloadOtherChannelsAfterFirstPage(currentChannel: AdChannel) {
        if (hasPreloadedOtherChannels || simulateError || simulateEmpty) return
        hasPreloadedOtherChannels = true

        viewModelScope.launch {
            AdChannel.entries
                .filter { channel -> channel != currentChannel && channel !in channelCache }
                .forEach { channel ->
                    val page = loadPage(channel, page = 1)
                    if (channel !in channelCache) {
                        saveChannelCache(
                            channel = channel,
                            ads = page.ads,
                            currentPage = page.currentPage,
                            hasMore = page.hasMore
                        )
                    }
                }

            _uiState.update {
                it.copy(
                    channelSnapshots = currentChannelSnapshots(),
                    statsOverview = currentStatsOverview()
                )
            }
        }
    }

    private fun loadFirstPageForRefresh(
        channel: AdChannel,
        selectedTag: String?
    ): MockAdRepository.PagedAds {
        var loadedPage = loadPage(channel, page = 1)
        if (selectedTag.isNullOrBlank() || loadedPage.ads.any { selectedTag in it.tags }) {
            return loadedPage
        }

        var loadedAds = loadedPage.ads
        while (loadedPage.hasMore && loadedAds.none { selectedTag in it.tags }) {
            loadedPage = loadPage(channel, page = loadedPage.currentPage + 1)
            loadedAds = mergeAdsById(loadedAds, loadedPage.ads)
        }

        return loadedPage.copy(ads = loadedAds)
    }

    private fun advanceRefreshRound(channel: AdChannel) {
        channelRefreshRounds[channel] = (channelRefreshRounds[channel] ?: 0) + 1
    }

    private fun getOrderedAdsForChannel(channel: AdChannel): List<AdItem> {
        val channelAds = MockAdRepository.ads.filter { it.channel == channel }
        val refreshRound = channelRefreshRounds[channel] ?: 0
        if (refreshRound <= 0 || channelAds.size <= 1) return channelAds

        val offsetStep = channel.ordinal + 2
        val rawOffset = refreshRound * offsetStep % channelAds.size
        val offset = if (rawOffset == 0) offsetStep.coerceAtMost(channelAds.lastIndex) else rawOffset
        val rotatedAds = channelAds.drop(offset) + channelAds.take(offset)

        return if (refreshRound % 2 == 0) {
            rotatedAds
        } else {
            rotatedAds.chunked(pageSize).flatMap { it.reversed() }
        }
    }

    private fun mergeAdsById(
        currentAds: List<AdItem>,
        nextAds: List<AdItem>
    ): List<AdItem> {
        if (nextAds.isEmpty()) return currentAds
        val existingIds = currentAds.mapTo(mutableSetOf()) { it.id }
        return currentAds + nextAds.filter { existingIds.add(it.id) }
    }

    private fun syncAd(updatedAd: AdItem) {
        syncAds(listOf(updatedAd))
    }

    private fun syncAds(updatedAds: Collection<AdItem>) {
        val updatedById = updatedAds.associateBy { it.id }

        if (updatedById.isNotEmpty()) {
            channelCache.keys.toList().forEach { channel ->
                val cached = channelCache[channel] ?: return@forEach
                channelCache[channel] = cached.copy(
                    ads = cached.ads.replaceAds(updatedById)
                )
            }
        }

        _uiState.update { state ->
            state.copy(
                ads = if (updatedById.isEmpty()) state.ads else state.ads.replaceAds(updatedById),
                channelSnapshots = currentChannelSnapshots(),
                statsOverview = currentStatsOverview()
            )
        }
    }

    private fun List<AdItem>.replaceAds(updatedById: Map<String, AdItem>): List<AdItem> {
        var changed = false
        val replacedAds = map { ad ->
            updatedById[ad.id]?.also { changed = true } ?: ad
        }
        return if (changed) replacedAds else this
    }

    private fun currentStatsOverview(): AdStatsOverview {
        return if (repositoryReady) {
            MockAdRepository.getStatsOverview()
        } else {
            AdStatsOverview()
        }
    }

    private fun saveChannelCache(
        channel: AdChannel,
        ads: List<AdItem>,
        currentPage: Int,
        hasMore: Boolean
    ) {
        channelCache[channel] = ChannelFeedCache(
            ads = ads,
            currentPage = currentPage,
            hasMore = hasMore
        )
    }

    private fun currentChannelSnapshots(): Map<AdChannel, FeedChannelSnapshot> {
        return channelCache.mapValues { (_, cache) ->
            FeedChannelSnapshot(
                ads = cache.ads,
                listState = cache.toListState(),
                currentPage = cache.currentPage,
                hasMore = cache.hasMore
            )
        }
    }

    private fun ChannelFeedCache.toListState(): FeedListState {
        return if (ads.isEmpty()) FeedListState.Empty else FeedListState.Success
    }

    private data class ChannelFeedCache(
        val ads: List<AdItem>,
        val currentPage: Int,
        val hasMore: Boolean
    )
}
