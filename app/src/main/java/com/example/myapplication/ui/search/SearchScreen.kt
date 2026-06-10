package com.example.myapplication.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.AdCardType
import com.example.myapplication.model.AdItem
import com.example.myapplication.ui.common.showSingleToast
import com.example.myapplication.ui.feed.AdFeedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    results: List<AdItem>,
    hasSearched: Boolean,
    searchResultListState: LazyListState,
    videoPlaybackPositions: Map<String, Long>,
    videoDurations: Map<String, Long>,
    videoSeekPositions: Map<String, Long>,
    videoSeekRequestIds: Map<String, Long>,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onAdClick: (String, Boolean) -> Unit,
    onLikeClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onVideoPlaybackUpdate: (String, Long, Long) -> Unit,
    onVideoPlaybackEnded: (String) -> Unit,
    onVideoSeek: (String, Long) -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun submitSearch() {
        if (query.isNotBlank()) {
            onSearch(query)
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "搜索广告",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
        ) {
            SearchInputBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = ::submitSearch
            )

            when {
                !hasSearched -> SearchStartState(
                    onSuggestionClick = { suggestion ->
                        onQueryChange(suggestion)
                        onSearch(suggestion)
                    }
                )
                results.isEmpty() -> SearchEmptyState(query = query)
                else -> SearchResultList(
                    results = results,
                    listState = searchResultListState,
                    videoPlaybackPositions = videoPlaybackPositions,
                    videoDurations = videoDurations,
                    videoSeekPositions = videoSeekPositions,
                    videoSeekRequestIds = videoSeekRequestIds,
                    onAdClick = onAdClick,
                    onLikeClick = onLikeClick,
                    onFavoriteClick = onFavoriteClick,
                    onTagClick = onTagClick,
                    onShareClick = { adId ->
                        onShareClick(adId)
                        showSingleToast(context, "已记录分享")
                    },
                    onVideoPlaybackUpdate = onVideoPlaybackUpdate,
                    onVideoPlaybackEnded = onVideoPlaybackEnded,
                    onVideoSeek = onVideoSeek
                )
            }
        }
    }
}

@Composable
private fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = {
                Text("搜索你想看的广告...")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索"
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch()
                }
            )
        )

        Button(
            onClick = onSearch,
            enabled = query.isNotBlank(),
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("搜索")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchStartState(
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "试试这样搜索",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "可以输入标签、频道、品牌或一句自然语言需求。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "学生党 运动",
                "本地 咖啡 优惠",
                "数码 游戏",
                "亲子旅游活动",
                "性价比 通勤用品"
            ).forEach { suggestion ->
                AssistChip(
                    onClick = { onSuggestionClick(suggestion) },
                    label = {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = null
                )
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    query: String
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
                text = "没有找到相关广告",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "换个关键词试试：$query",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchResultList(
    results: List<AdItem>,
    listState: LazyListState,
    videoPlaybackPositions: Map<String, Long>,
    videoDurations: Map<String, Long>,
    videoSeekPositions: Map<String, Long>,
    videoSeekRequestIds: Map<String, Long>,
    onAdClick: (String, Boolean) -> Unit,
    onLikeClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onVideoPlaybackUpdate: (String, Long, Long) -> Unit,
    onVideoPlaybackEnded: (String) -> Unit,
    onVideoSeek: (String, Long) -> Unit
) {
    val context = LocalContext.current
    var playingVideoAdId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(results) {
        if (playingVideoAdId != null && results.none { it.id == playingVideoAdId }) {
            playingVideoAdId = null
        }
    }

    LaunchedEffect(listState, results) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { item -> item.key as? String }
                .filter { key -> key != "search_result_count" }
                .toSet()
        }.collect { visibleAdIds ->
            playingVideoAdId?.let { playingId ->
                if (playingId !in visibleAdIds) {
                    playingVideoAdId = null
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            top = 4.dp,
            end = 14.dp,
            bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "search_result_count") {
            Text(
                text = "找到 ${results.size} 条相关广告",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        items(
            items = results,
            key = { it.id }
        ) { ad ->
            AdFeedCard(
                ad = ad,
                onClick = {
                    onAdClick(
                        ad.id,
                        playingVideoAdId == ad.id
                    )
                },
                onLikeClick = { onLikeClick(ad.id) },
                onFavoriteClick = { onFavoriteClick(ad.id) },
                onShareClick = { onShareClick(ad.id) },
                onTagClick = onTagClick,
                onCtaClick = { showSingleToast(context, "${ad.ctaText}功能开发中") },
                isVideoPlaying = ad.cardType == AdCardType.VIDEO && playingVideoAdId == ad.id,
                videoPositionMs = videoPlaybackPositions[ad.id] ?: 0L,
                videoDurationMs = videoDurations[ad.id] ?: 0L,
                videoSeekPositionMs = videoSeekPositions[ad.id] ?: 0L,
                videoSeekRequestId = videoSeekRequestIds[ad.id] ?: 0L,
                onVideoClick = {
                    playingVideoAdId = if (playingVideoAdId == ad.id) null else ad.id
                },
                onVideoPlaybackUpdate = { positionMs, durationMs ->
                    onVideoPlaybackUpdate(ad.id, positionMs, durationMs)
                },
                onVideoPlaybackEnded = {
                    if (playingVideoAdId == ad.id) {
                        playingVideoAdId = null
                    }
                    onVideoPlaybackEnded(ad.id)
                },
                onVideoSeek = { positionMs ->
                    onVideoSeek(ad.id, positionMs)
                },
                onVideoError = {
                    if (playingVideoAdId == ad.id) {
                        playingVideoAdId = null
                    }
                }
            )
        }
    }
}
