package com.example.myapplication.model

/**
 * 信息流页面 UI 状态
 * 管理列表的各种状态：加载中、刷新、加载更多、空状态、错误状态
 */
data class FeedUiState(
    /** 广告列表 */
    val ads: List<AdItem> = emptyList(),

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
    val errorMessage: String? = null
)

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
