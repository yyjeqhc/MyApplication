package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.MockAdRepository
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem
import com.example.myapplication.model.FeedListState
import com.example.myapplication.model.FeedUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 信息流 ViewModel
 * 管理页面状态、刷新、加载更多等业务逻辑
 */
class FeedViewModel : ViewModel() {

    /** UI 状态 */
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    /** 模拟错误开关（用于测试错误状态） */
    private var simulateError = false

    /** 每页数据量 */
    private val pageSize = 6

    init {
        // 首次加载数据
        loadInitialData()
    }

    /**
     * 首次加载数据
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(listState = FeedListState.Loading) }

            // 模拟网络延迟
            delay(800)

            if (simulateError) {
                _uiState.update {
                    it.copy(
                        listState = FeedListState.Error("加载失败，请重试"),
                        errorMessage = "加载失败，请重试"
                    )
                }
                return@launch
            }

            val channel = _uiState.value.selectedChannel
            val ads = MockAdRepository.getAdsByChannel(channel)

            _uiState.update {
                it.copy(
                    ads = ads,
                    listState = if (ads.isEmpty()) FeedListState.Empty else FeedListState.Success,
                    currentPage = 1,
                    hasMore = ads.size >= pageSize,
                    errorMessage = null
                )
            }
        }
    }

    /**
     * 切换频道
     */
    fun selectChannel(channel: AdChannel) {
        if (channel == _uiState.value.selectedChannel) return

        _uiState.update {
            it.copy(
                selectedChannel = channel,
                ads = emptyList(),
                currentPage = 1,
                hasMore = true,
                errorMessage = null
            )
        }

        // 加载新频道数据
        loadInitialData()
    }

    /**
     * 下拉刷新
     */
    fun refresh() {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            // 模拟网络延迟
            delay(1000)

            if (simulateError) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = "刷新失败"
                    )
                }
                return@launch
            }

            val channel = _uiState.value.selectedChannel
            // 刷新时重新打乱数据（模拟新数据）
            val ads = MockAdRepository.getAdsByChannel(channel).shuffled()

            _uiState.update {
                it.copy(
                    ads = ads,
                    isRefreshing = false,
                    currentPage = 1,
                    hasMore = ads.size >= pageSize,
                    listState = if (ads.isEmpty()) FeedListState.Empty else FeedListState.Success,
                    errorMessage = null
                )
            }
        }
    }

    /**
     * 加载更多
     */
    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            // 模拟网络延迟
            delay(800)

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
            // 模拟生成更多数据（实际项目中会请求下一页）
            val moreAds = generateMoreAds(channel, currentPage + 1)

            _uiState.update {
                it.copy(
                    ads = it.ads + moreAds,
                    isLoadingMore = false,
                    currentPage = currentPage + 1,
                    hasMore = moreAds.size >= pageSize && currentPage < 5, // 最多加载5页
                    errorMessage = null
                )
            }
        }
    }

    /**
     * 切换点赞状态
     */
    fun toggleLike(adId: String) {
        MockAdRepository.toggleLike(adId)

        _uiState.update { state ->
            state.copy(
                ads = state.ads.map { ad ->
                    if (ad.id == adId) {
                        MockAdRepository.getAdById(adId) ?: ad
                    } else {
                        ad
                    }
                }
            )
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(adId: String) {
        MockAdRepository.toggleFavorite(adId)

        _uiState.update { state ->
            state.copy(
                ads = state.ads.map { ad ->
                    if (ad.id == adId) {
                        MockAdRepository.getAdById(adId) ?: ad
                    } else {
                        ad
                    }
                }
            )
        }
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
     * 重试加载（从错误状态恢复）
     */
    fun retry() {
        _uiState.update { it.copy(errorMessage = null) }
        loadInitialData()
    }

    /**
     * 模拟生成更多广告数据
     */
    private fun generateMoreAds(channel: AdChannel, page: Int): List<AdItem> {
        val baseAds = MockAdRepository.getAdsByChannel(channel)
        val startIndex = (page - 1) * pageSize

        // 基于现有数据生成变体
        return baseAds.take(pageSize).mapIndexed { index, ad ->
            ad.copy(
                id = "${channel.name.lowercase()}_page${page}_${index}",
                title = "${ad.title} (${page})",
                likeCount = ad.likeCount + (page * 100),
                exposureCount = ad.exposureCount + (page * 1000)
            )
        }
    }
}
