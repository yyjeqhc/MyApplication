package com.example.myapplication.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.AdStatsOverview
import java.util.Locale

/**
 * Demo 控制面板
 * 用于快速切换不同状态进行测试
 */
@Composable
fun DemoControlPanel(
    isVisible: Boolean,
    statsOverview: AdStatsOverview,
    onToggleVisibility: () -> Unit,
    onSimulateNormal: () -> Unit,
    onSimulateEmpty: () -> Unit,
    onSimulateError: () -> Unit,
    onResetPagination: () -> Unit,
    onClearLocalAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 切换按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onToggleVisibility,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                )
            ) {
                Text(
                    text = if (isVisible) "隐藏调试" else "调试",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // 控制面板内容
        if (isVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "调试面板",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "用于验收空态、错误态、分页状态和本地统计",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    StatsOverviewGrid(statsOverview = statsOverview)

                    Spacer(modifier = Modifier.height(4.dp))

                    // 状态切换按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSimulateNormal,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("正常", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onSimulateEmpty,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("空态", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onSimulateError,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("错误", fontSize = 12.sp)
                        }
                    }

                    // 重置按钮
                    OutlinedButton(
                        onClick = onResetPagination,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("重置分页数据", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onClearLocalAnalytics,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清空本地统计", fontSize = 12.sp)
                    }

                    // 说明文字
                    Text(
                        text = "正常：显示广告列表\n空态：无数据状态\n错误：模拟加载失败\n重置：重新加载分页并清空本会话曝光去重\n清空本地统计：恢复 JSON 初始统计",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsOverviewGrid(
    statsOverview: AdStatsOverview,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OverviewStatItem(
                label = "总曝光",
                value = formatCount(statsOverview.totalExposureCount),
                modifier = Modifier.weight(1f)
            )
            OverviewStatItem(
                label = "总点击",
                value = formatCount(statsOverview.totalClickCount),
                modifier = Modifier.weight(1f)
            )
            OverviewStatItem(
                label = "CTR",
                value = String.format(Locale.US, "%.1f%%", statsOverview.ctrPercent),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OverviewStatItem(
                label = "总点赞",
                value = formatCount(statsOverview.totalLikeCount),
                modifier = Modifier.weight(1f)
            )
            OverviewStatItem(
                label = "收藏",
                value = formatCount(statsOverview.totalFavoriteCount),
                modifier = Modifier.weight(1f)
            )
            OverviewStatItem(
                label = "分享",
                value = formatCount(statsOverview.totalShareCount),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OverviewStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
