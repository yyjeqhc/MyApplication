package com.example.myapplication.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

@Composable
fun VideoProgressBar(
    progress: Float,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.9f),
    trackColor: Color = Color.White.copy(alpha = 0.22f)
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val safeProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = tween(durationMillis = 220),
        label = "videoProgress"
    )
    val displayedProgress = if (isDragging) dragProgress else animatedProgress
    val canSeek = durationMs > 0L

    fun progressToPosition(targetProgress: Float): Long {
        return (targetProgress.coerceIn(0f, 1f) * durationMs).roundToLong()
            .coerceIn(0L, durationMs)
    }

    Box(
        modifier = modifier
            .height(18.dp)
            .then(
                if (canSeek) {
                    Modifier.pointerInput(durationMs) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            dragProgress = (down.position.x / width).coerceIn(0f, 1f)
                            isDragging = true

                            drag(down.id) { change ->
                                change.consume()
                                dragProgress = (change.position.x / width).coerceIn(0f, 1f)
                            }

                            onSeek(progressToPosition(dragProgress))
                            isDragging = false
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(trackColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(displayedProgress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(color)
        )
    }
}
