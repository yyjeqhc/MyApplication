package com.example.myapplication.ui.feed

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.AdChannel
import com.example.myapplication.model.FeedListState
import com.example.myapplication.model.FeedUiState

private const val SHOW_DEBUG_PANEL = false

/**
 * 信息流主页面
 * 包含顶部标题栏、频道 Tab、Demo 控制面板和广告列表
 * 支持下拉刷新和上拉加载更多
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    uiState: FeedUiState,
    listState: LazyListState,
    onAdClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onClearTagFilter: () -> Unit,
    onChannelSelect: (AdChannel) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
    onSearchClick: () -> Unit,
    onSimulateNormal: () -> Unit,
    onSimulateEmpty: () -> Unit,
    onSimulateError: () -> Unit,
    onResetPagination: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 频道列表
    val channels = remember { AdChannel.entries }

    // 控制面板显示状态
    var isControlPanelVisible by remember { mutableStateOf(false) }

    val visibleAds = uiState.filteredAds

    // 监听滚动位置，触发加载更多
    val shouldLoadMore = remember(
        listState,
        uiState.isLoadingMore,
        uiState.hasMore,
        visibleAds.size
    ) {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            visibleAds.isNotEmpty() &&
                lastVisibleItem >= totalItems - 3 &&
                !uiState.isLoadingMore &&
                uiState.hasMore
        }
    }

    // 触发加载更多
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
    ) {
        // 顶部标题栏
        TopAppBar(
            title = {
                Text(
                    text = "AI 广告推荐",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // 频道 Tab
        PrimaryTabRow(
            selectedTabIndex = channels.indexOf(uiState.selectedChannel),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            channels.forEach { channel ->
                Tab(
                    selected = uiState.selectedChannel == channel,
                    onClick = { onChannelSelect(channel) },
                    text = {
                        Text(
                            text = channel.displayName,
                            fontWeight = if (uiState.selectedChannel == channel) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                )
            }
        }

        SearchEntry(onClick = onSearchClick)

        // Demo 控制面板
        if (SHOW_DEBUG_PANEL) {
            DemoControlPanel(
                isVisible = isControlPanelVisible,
                onToggleVisibility = { isControlPanelVisible = !isControlPanelVisible },
                onSimulateNormal = onSimulateNormal,
                onSimulateEmpty = onSimulateEmpty,
                onSimulateError = onSimulateError,
                onResetPagination = onResetPagination
            )
        }

        // 内容区域
        when (uiState.listState) {
            is FeedListState.Loading -> {
                // 首次加载中 - 显示骨架屏
                SkeletonLoadingState()
            }
            is FeedListState.Error -> {
                // 错误状态
                ErrorState(
                    message = uiState.listState.message,
                    onRetry = onRetry
                )
            }
            is FeedListState.Empty -> {
                // 空状态
                EmptyState()
            }
            is FeedListState.Success -> {
                // 正常列表（支持下拉刷新）
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 刷新指示器
                    if (uiState.isRefreshing) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }

                    // 列表内容
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            top = 12.dp,
                            end = 14.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 当前标签筛选条件
                        uiState.selectedTag?.let { tag ->
                            item(key = "tag_filter") {
                                ActiveTagFilterBar(
                                    tag = tag,
                                    resultCount = visibleAds.size,
                                    onClear = onClearTagFilter
                                )
                            }
                        }

                        // 刷新提示
                        if (uiState.isRefreshing) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "正在刷新...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // 广告列表
                        items(
                            items = visibleAds,
                            key = { it.id }
                        ) { ad ->
                            AdFeedCard(
                                ad = ad,
                                onClick = { onAdClick(ad.id) },
                                onLikeClick = { onLikeClick(ad.id) },
                                onFavoriteClick = { onFavoriteClick(ad.id) },
                                onShareClick = {
                                    Toast.makeText(context, "分享功能开发中", Toast.LENGTH_SHORT).show()
                                },
                                onTagClick = onTagClick
                            )
                        }

                        if (uiState.selectedTag != null && visibleAds.isEmpty()) {
                            item(key = "empty_filter") {
                                EmptyFilterState(onClear = onClearTagFilter)
                            }
                        }

                        // 底部加载更多状态
                        item {
                            LoadMoreIndicator(
                                isLoading = uiState.isLoadingMore,
                                hasMore = uiState.hasMore
                            )
                        }
                    }
                }
            }
        }

        // 错误提示（Snackbar 风格）
        uiState.errorMessage?.let { message ->
            LaunchedEffect(message) {
                // 自动清除错误信息
                kotlinx.coroutines.delay(3000)
                onClearError()
            }
        }
    }
}

@Composable
private fun SearchEntry(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "搜索你想看的广告...",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 当前标签筛选提示
 */
@Composable
private fun ActiveTagFilterBar(
    tag: String,
    resultCount: Int,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "筛选",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$resultCount 条",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("清除筛选")
            }
        }
    }
}

/**
 * 标签筛选空结果
 */
@Composable
private fun EmptyFilterState(
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "没有找到相关广告",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "换个标签或清除筛选再看看",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onClear) {
            Text("清除筛选")
        }
    }
}

/**
 * 骨架屏加载状态
 */
@Composable
private fun SkeletonLoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            top = 12.dp,
            end = 14.dp,
            bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) { index ->
            if (index % 2 == 0) {
                SkeletonCard()
            } else {
                SmallImageSkeletonCard()
            }
        }
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无推荐内容",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "下拉刷新试试",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 错误状态
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

/**
 * 加载更多指示器
 */
@Composable
private fun LoadMoreIndicator(
    isLoading: Boolean,
    hasMore: Boolean
) {
    if (isLoading) {
        // 加载中
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "正在加载更多",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (!hasMore) {
        // 没有更多数据
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "没有更多了",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
