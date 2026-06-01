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
import com.example.myapplication.data.MockAdRepository
import com.example.myapplication.ui.detail.AdDetailScreen
import com.example.myapplication.ui.feed.FeedScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

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
fun AdApp() {
    // 广告列表状态（从 MockRepository 获取）
    var adList by remember { mutableStateOf(MockAdRepository.ads) }

    // 当前选中的广告 ID（用于详情页展示）
    var selectedAdId by remember { mutableStateOf<String?>(null) }

    // 列表滚动状态（提升到此处，避免返回时丢失）
    val listState = rememberLazyListState()

    // 根据 selectedAdId 获取最新的广告对象
    val selectedAd = remember(adList, selectedAdId) {
        selectedAdId?.let { id ->
            adList.find { it.id == id }
        }
    }

    // 点赞回调
    val onLikeClick: (String) -> Unit = { adId ->
        MockAdRepository.toggleLike(adId)
        adList = MockAdRepository.ads
    }

    // 收藏回调
    val onFavoriteClick: (String) -> Unit = { adId ->
        MockAdRepository.toggleFavorite(adId)
        adList = MockAdRepository.ads
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
            onLikeClick = { onLikeClick(selectedAd.id) },
            onFavoriteClick = { onFavoriteClick(selectedAd.id) }
        )
    } else {
        // 显示信息流页面
        FeedScreen(
            adList = adList,
            listState = listState,
            onAdClick = onAdClick,
            onLikeClick = onLikeClick,
            onFavoriteClick = onFavoriteClick
        )
    }
}
