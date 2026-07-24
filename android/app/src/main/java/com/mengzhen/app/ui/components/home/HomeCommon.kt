package com.mengzhen.app.ui.components.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.mengzhen.app.ui.theme.LocalIsDarkTheme

// ==================== 多色弥散卡（home-diffuse-card） ====================

/** Web 端 diffuseShapes 的 angle/x/y（6 组轮转，transform/radius 变形忽略） */
private val diffuseAnchors = listOf(
    Triple(82f, 0.24f, 0.38f),
    Triple(218f, 0.76f, 0.28f),
    Triple(312f, 0.30f, 0.82f),
    Triple(154f, 0.82f, 0.74f),
    Triple(18f, 0.58f, 0.18f),
    Triple(268f, 0.18f, 0.66f),
)

private fun diffuseHue(i: Int, n: Int) = ((152 + i * 37 + n * 28) % 360).toFloat()

/** cardDiffuse(i) 同款 7 色生成：hsl(h 72% 62~77% / 0.4~0.56) */
private fun diffuseColors(i: Int): List<Color> = (0 until 7).map { n ->
    Color.hsl(
        diffuseHue(i, n), 0.72f, 0.62f + ((i + n) % 4) * 0.05f,
        alpha = 0.4f + ((i + n) % 3) * 0.08f,
    )
}

/** conic 色标：transparent 0-6%, a 11%, b 18%, transparent 27%, c 36%, d 44%, transparent 53%, e 63%, f 71%, transparent 80%, g 89%, transparent */
private fun diffuseStops(colors: List<Color>): Array<Pair<Float, Color>> = arrayOf(
    0f to Color.Transparent, 0.06f to Color.Transparent,
    0.11f to colors[0], 0.18f to colors[1], 0.27f to Color.Transparent,
    0.36f to colors[2], 0.44f to colors[3], 0.53f to Color.Transparent,
    0.63f to colors[4], 0.71f to colors[5], 0.80f to Color.Transparent,
    0.89f to colors[6], 1f to Color.Transparent,
)

/**
 * 多色弥散卡 —— 复刻 Web 端 home-diffuse-card（cardDiffuse）
 * 卡片底层铺 conic-gradient 弥散光斑（放大 190% + blur 30dp，亮 opacity 0.28 / 暗 0.20）
 * hover 效果（位移/边框变亮/弥散增强）为桌面端专属，不复刻。
 */
@Composable
fun DiffuseCard(
    diffuseIndex: Int,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = LocalIsDarkTheme.current
    val (angle, cx, cy) = diffuseAnchors[diffuseIndex % diffuseAnchors.size]
    val stops = remember(diffuseIndex) { diffuseStops(diffuseColors(diffuseIndex)) }

    Box(modifier = modifier.clip(shape)) {
        // 弥散背景层：inset -45%（190% 放大）+ blur 30px + opacity 0.28/dark 0.2
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = 1.9f; scaleY = 1.9f }
                .blur(30.dp)
                .alpha(if (dark) 0.2f else 0.28f)
        ) {
            val center = Offset(size.width * cx, size.height * cy)
            // CSS conic from angle（12 点钟起）→ Compose sweep（3 点钟起）：绕中心旋转 angle-90
            drawContext.canvas.save()
            drawContext.transform.rotate(angle - 90f, pivot = center)
            drawRect(brush = Brush.sweepGradient(colorStops = stops, center = center))
            drawContext.canvas.restore()
        }
        // 内容层（border-border/50 + 内容）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape),
            content = content,
        )
    }
}

// ==================== RevealGroup：滚动入场动画 ====================

/**
 * 视口可见性追踪 Modifier（复刻 Web useScrollVisibility）
 * 元素 bounds 与视口相交时回调 true，离开时回调 false。FloatingBar 锚点用。
 */
@Composable
fun Modifier.viewportVisibilityTracker(
    onVisibleChanged: ((Boolean) -> Unit)?,
): Modifier {
    if (onVisibleChanged == null) return this
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val viewportHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    return this.then(
        Modifier.onGloballyPositioned { coords ->
            val bounds = coords.boundsInRoot()
            onVisibleChanged(bounds.top < viewportHeightPx && bounds.bottom > 0f)
        }
    )
}

/**
 * RevealGroup —— 复刻 Web 端 page.tsx RevealGroup
 * 进入视口后：opacity 0→1 + translateY 16px→0，0.5s ease，delayBase 延迟。
 * （LazyColumn 按需组合即天然懒加载，组合时触发近似 IntersectionObserver threshold 0.08；
 *   Web 端子项级联 80ms 延迟在移动端单列布局下感知极弱，统一整体动画）
 */
@Composable
fun RevealGroup(
    delayBase: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayBase.toLong())
        progress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    Column(
        modifier = modifier
            .alpha(progress.value)
            .graphicsLayer { translationY = (1f - progress.value) * 16f * density },
        content = content,
    )
}

// ==================== 区块标题（渐变文字） ====================

/** 品牌渐变文字（bg-gradient-to-r from-brand-start via-brand-mid to-brand-end bg-clip-text） */
@Composable
fun brandTextGradient(): Brush = Brush.horizontalGradient(
    listOf(
        com.mengzhen.app.ui.theme.BrandStartThemed,
        com.mengzhen.app.ui.theme.BrandMidThemed,
        com.mengzhen.app.ui.theme.BrandEndThemed,
    )
)
