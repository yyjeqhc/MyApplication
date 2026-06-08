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

/**
 * Demo 控制面板
 * 用于快速切换不同状态进行测试
 */
@Composable
fun DemoControlPanel(
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onSimulateNormal: () -> Unit,
    onSimulateEmpty: () -> Unit,
    onSimulateError: () -> Unit,
    onResetPagination: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 切换按钮
        TextButton(
            onClick = onToggleVisibility,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isVisible) "▼ 隐藏控制面板" else "▶ 显示控制面板",
                fontSize = 12.sp
            )
        }

        // 控制面板内容
        if (isVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎮 Demo 控制面板",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "快速切换页面状态进行测试",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

                    // 说明文字
                    Text(
                        text = "💡 正常：显示广告列表\n📭 空态：无数据状态\n😞 错误：模拟加载失败\n🔄 重置：恢复初始数据和分页",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
