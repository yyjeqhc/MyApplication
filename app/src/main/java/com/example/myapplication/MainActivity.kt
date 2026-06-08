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
import com.example.myapplication.ui.detail.AdDetailScreen
import com.example.myapplication.ui.feed.FeedScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.FeedViewModel

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

    // 列表滚动状态（提升到此处，避免返回时丢失）
    val listState = rememberLazyListState()

    // 根据 selectedAdId 获取最新的广告对象
    val selectedAd = remember(uiState.ads, selectedAdId) {
        selectedAdId?.let { id ->
            uiState.ads.find { it.id == id }
        }
    }

    // 点击广告卡片回调
    val onAdClick: (String) -> Unit = { adId ->
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
            onLikeClick = { viewModel.toggleLike(selectedAd.id) },
            onFavoriteClick = { viewModel.toggleFavorite(selectedAd.id) },
            onTagClick = { tag ->
                viewModel.selectTag(tag)
                selectedAdId = null
            }
        )
    } else {
        // 显示信息流页面
        FeedScreen(
            uiState = uiState,
            listState = listState,
            onAdClick = onAdClick,
            onLikeClick = { viewModel.toggleLike(it) },
            onFavoriteClick = { viewModel.toggleFavorite(it) },
            onTagClick = { viewModel.selectTag(it) },
            onClearTagFilter = { viewModel.clearTagFilter() },
            onChannelSelect = { viewModel.selectChannel(it) },
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMore() },
            onRetry = { viewModel.retry() },
            onClearError = { viewModel.clearError() },
            onSimulateNormal = { viewModel.simulateNormal() },
            onSimulateEmpty = { viewModel.simulateEmptyState() },
            onSimulateError = { viewModel.simulateErrorState() },
            onResetPagination = { viewModel.resetPagination() }
        )
    }
}
