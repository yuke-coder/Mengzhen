package com.mengzhen.app.ui.components.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import kotlin.math.sqrt

/**
 * 动态背景 —— 像素级复刻 Web 端 dynamic-background.tsx
 *
 * - 暗色：星空（radial-gradient(ellipse at bottom, #1B2735 0%, #090A0F 100%）
 *   + 三层星星（700×1dp 50s / 200×2dp 100s / 100×3dp 150s，线性上移 2000px 无缝循环）
 *   星星坐标用 Web 端同款确定性种子公式生成，位置与 Web 完全一致
 * - 亮色：滚动网格（白底 + 50dp tile 浅灰线，0.92s 从 (50,50) 滚到 (0,0) 无限循环）
 *   网格线颜色解码自 Web 端 base64 PNG：横线 #E9E9E8、竖线 #EDEDEC
 */
@Composable
fun DynamicBackground(modifier: Modifier = Modifier) {
    if (LocalIsDarkTheme.current) {
        StarryBackground(modifier)
    } else {
        GridBackground(modifier)
    }
}

// ==================== 暗色 · 星空 ====================

/** Web 端 generateStars 同款种子公式：x/y ∈ [0, 2000) */
private fun generateStars(count: Int, seed: Int): List<Offset> =
    (0 until count).map { i ->
        val x = ((seed * (i + 1) * 9301 + 49297) % 233280) / 233280f * 2000f
        val y = ((seed * (i + 1) * 49297 + 9301) % 233280) / 233280f * 2000f
        Offset(x, y)
    }

private val StarSpaceSize = 2000f

@Composable
private fun StarryBackground(modifier: Modifier) {
    val stars1 = remember { generateStars(700, 1) }
    val stars2 = remember { generateStars(200, 2) }
    val stars3 = remember { generateStars(100, 3) }

    val transition = rememberInfiniteTransition(label = "stars")
    val offset1 by transition.animateFloat(
        0f, StarSpaceSize,
        androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(50_000, easing = LinearEasing), RepeatMode.Restart
        ), label = "stars1"
    )
    val offset2 by transition.animateFloat(
        0f, StarSpaceSize,
        infiniteRepeatable(tween(100_000, easing = LinearEasing), RepeatMode.Restart), label = "stars2"
    )
    val offset3 by transition.animateFloat(
        0f, StarSpaceSize,
        infiniteRepeatable(tween(150_000, easing = LinearEasing), RepeatMode.Restart), label = "stars3"
    )

    val density = LocalDensity.current
    val starSize1 = with(density) { 1.dp.toPx() }
    val starSize2 = with(density) { 2.dp.toPx() }
    val starSize3 = with(density) { 3.dp.toPx() }

    Canvas(modifier = modifier) {
        // 底色：radial-gradient(ellipse at bottom, #1B2735 0%, #090A0F 100%)
        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0xFF1B2735), 1f to Color(0xFF090A0F),
                center = Offset(size.width / 2f, size.height),
                radius = sqrt(size.width * size.width / 4f + size.height * size.height),
            )
        )
        drawStarLayer(stars1, offset1, starSize1)
        drawStarLayer(stars2, offset2, starSize2)
        drawStarLayer(stars3, offset3, starSize3)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarLayer(
    stars: List<Offset>,
    offset: Float,
    strokeWidth: Float,
) {
    // animStar：整体上移，mod 2000 无缝循环（等价 Web 端 ::after 在 2000px 处克隆拼接）
    val points = stars.map { star -> Offset(star.x, (star.y - offset).mod(StarSpaceSize)) }
    drawPoints(
        points = points,
        pointMode = PointMode.Points,
        color = Color.White,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

// ==================== 亮色 · 滚动网格 ====================

private val GridLineHorizontal = Color(0xFFE9E9E8) // 解码自 Web 端网格 PNG 顶边线
private val GridLineVertical = Color(0xFFEDEDEC)   // 解码自 Web 端网格 PNG 左边线

@Composable
private fun GridBackground(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "grid")
    // bg-scrolling-reverse 0.92s：background-position 从 (50,50) → (0,0)
    val progress by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(920, easing = LinearEasing), RepeatMode.Restart), label = "gridScroll"
    )
    val density = LocalDensity.current
    val tile = with(density) { 50.dp.toPx() }
    val stroke = with(density) { 1.dp.toPx() }

    Canvas(modifier = modifier) {
        drawRect(Color.White)
        val offset = (1f - progress) * tile
        var x = offset - tile
        while (x < size.width) {
            drawLine(GridLineVertical, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
            x += tile
        }
        var y = offset - tile
        while (y < size.height) {
            drawLine(GridLineHorizontal, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            y += tile
        }
    }
}
