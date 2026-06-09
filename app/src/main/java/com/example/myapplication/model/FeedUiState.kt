package com.example.myapplication.model

/**
 * 信息流页面 UI 状态
 * 管理列表的各种状态：加载中、刷新、加载更多、空状态、错误状态
 */
data class FeedUiState(
    /** 广告列表 */
    val ads: List<AdItem> = emptyList(),

    /** 当前选中的标签筛选条件 */
    val selectedTag: String? = null,

    /** 当前选中的频道 */
    val selectedChannel: AdChannel = AdChannel.FEATURED,

    /** 列表状态 */
    val listState: FeedListState = FeedListState.Loading,

    /** 是否正在刷新 */
    val isRefreshing: Boolean = false,

    /** 是否正在加载更多 */
    val isLoadingMore: Boolean = false,

    /** 是否还有更多数据 */
    val hasMore: Boolean = true,

    /** 当前页码（用于模拟分页） */
    val currentPage: Int = 1,

    /** 错误信息（null 表示无错误） */
    val errorMessage: String? = null,

    /** 一次性轻量提示信息（null 表示无提示） */
    val feedbackMessage: String? = null,

    /** 下拉刷新完成事件 id，用于 UI 一次性滚回顶部 */
    val refreshCompletedEventId: Int = 0,

    /** 各频道已加载快照，用于 Pager 直接展示缓存页 */
    val channelSnapshots: Map<AdChannel, FeedChannelSnapshot> = emptyMap(),

    /** 全局本地统计总览（调试面板默认隐藏） */
    val statsOverview: AdStatsOverview = AdStatsOverview()
) {
    /** 当前筛选后实际展示的广告列表 */
    val filteredAds: List<AdItem>
        get() = selectedTag?.let { tag ->
            ads.filter { ad -> tag in ad.tags }
        } ?: ads
}

data class FeedChannelSnapshot(
    val ads: List<AdItem> = emptyList(),
    val listState: FeedListState = FeedListState.Loading,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)

/**
 * 本地埋点总览，仅基于当前 App 进程内的 mock 数据计算。
 */
data class AdStatsOverview(
    val totalExposureCount: Int = 0,
    val totalClickCount: Int = 0,
    val totalLikeCount: Int = 0,
    val totalFavoriteCount: Int = 0,
    val totalShareCount: Int = 0
) {
    val ctrPercent: Float
        get() = if (totalExposureCount > 0) {
            totalClickCount.toFloat() / totalExposureCount.toFloat() * 100f
        } else {
            0f
        }
}

/**
 * 列表状态枚举
 */
sealed class FeedListState {
    /** 首次加载中 */
    data object Loading : FeedListState()

    /** 正常显示列表 */
    data object Success : FeedListState()

    /** 空状态（无数据） */
    data object Empty : FeedListState()

    /** 错误状态 */
    data class Error(val message: String) : FeedListState()
}
