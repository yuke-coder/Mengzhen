package com.mengzhen.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ChatGptSpinnerSegments = listOf(
    Offset(9.5f, 2.9375f) to Offset(9.5f, 5.5625f),
    Offset(9.5f, 13.4375f) to Offset(9.5f, 16.0625f),
    Offset(2.9375f, 9.5f) to Offset(5.5625f, 9.5f),
    Offset(13.4375f, 9.5f) to Offset(16.0625f, 9.5f),
    Offset(4.86011f, 4.85961f) to Offset(6.71627f, 6.71577f),
    Offset(12.2847f, 12.2842f) to Offset(14.1409f, 14.1404f),
    Offset(4.86011f, 14.1404f) to Offset(6.71627f, 12.2842f),
    Offset(12.2847f, 6.71577f) to Offset(14.1409f, 4.85961f),
)

@Composable
fun ChatGptLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = LocalContentColor.current,
    loadingDescription: String = "正在加载",
) {
    val transition = rememberInfiniteTransition(label = "chatgpt-loading-spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = loadingDescription
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .then(Modifier.size(size)),
    ) {
        val scale = this.size.minDimension / 19f
        val origin = Offset(
            x = (this.size.width - 19f * scale) / 2f,
            y = (this.size.height - 19f * scale) / 2f,
        )

        rotate(degrees = rotation, pivot = center) {
            ChatGptSpinnerSegments.forEach { (start, end) ->
                drawLine(
                    color = color,
                    start = origin + start * scale,
                    end = origin + end * scale,
                    strokeWidth = 1.875f * scale,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
