package com.example.myapplication.ui.feed

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.AdItem

/**
 * 信息流主页面
 * 包含顶部标题栏、频道 Tab 和广告列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    adList: List<AdItem>,
    listState: LazyListState,
    onAdClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 当前选中的频道
    var selectedChannel by remember { mutableStateOf(AdChannel.FEATURED) }

    // 频道列表
    val channels = remember { AdChannel.entries }

    // 根据选中频道筛选广告
    val filteredAds = remember(adList, selectedChannel) {
        adList.filter { it.channel == selectedChannel }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部标题栏
        TopAppBar(
            title = {
                Text(
                    text = "AI 广告推荐",
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // 频道 Tab
        TabRow(
            selectedTabIndex = channels.indexOf(selectedChannel),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            channels.forEach { channel ->
                Tab(
                    selected = selectedChannel == channel,
                    onClick = { selectedChannel = channel },
                    text = {
                        Text(
                            text = channel.displayName,
                            fontWeight = if (selectedChannel == channel) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                )
            }
        }

        // 广告列表
        if (filteredAds.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无广告",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = filteredAds,
                    key = { it.id }
                ) { ad ->
                    AdFeedCard(
                        ad = ad,
                        onClick = { onAdClick(ad.id) },
                        onLikeClick = { onLikeClick(ad.id) },
                        onFavoriteClick = { onFavoriteClick(ad.id) },
                        onShareClick = {
                            Toast.makeText(context, "分享功能开发中", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
