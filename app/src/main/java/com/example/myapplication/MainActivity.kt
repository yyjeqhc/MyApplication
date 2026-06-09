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
    val onAdClick: (String) -> Unit = { adId ->
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
            listState = currentListState,
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
            onSearchClick = { isSearchVisible = true },
            onSimulateNormal = { viewModel.simulateNormal() },
            onSimulateEmpty = { viewModel.simulateEmptyState() },
            onSimulateError = { viewModel.simulateErrorState() },
            onResetPagination = { viewModel.resetPagination() },
            onClearLocalAnalytics = { viewModel.clearLocalAnalytics() }
        )
    }
}
