package com.mengzhen.app.ui.components.home

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mengzhen.app.ui.theme.BrandDimThemed
import com.mengzhen.app.ui.theme.BrandGlowThemed
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==================== HeroBackdrop：SVG 流动线条 + 发光节点 ====================

/** Web 端 heroNodes（viewBox 1200x800 坐标系） */
private data class HeroNode(val x: Float, val y: Float, val r: Float, val delayMs: Int)

private val heroNodes = listOf(
    HeroNode(200f, 300f, 4f, 0),
    HeroNode(400f, 200f, 3f, 200),
    HeroNode(600f, 400f, 5f, 400),
    HeroNode(800f, 250f, 3f, 600),
    HeroNode(1000f, 350f, 4f, 800),
    HeroNode(300f, 500f, 3f, 1000),
    HeroNode(700f, 550f, 4f, 1200),
    HeroNode(500f, 600f, 3f, 1400),
)

/** 二次贝塞尔转 Compose Path（Web SVG 的 T 指令 = 前控制点反射） */
private fun heroPath1() = Path().apply {
    moveTo(100f, 400f)
    quadraticTo(300f, 200f, 500f, 350f)
    quadraticTo(700f, 500f, 900f, 400f) // T：控制点 (700,500) = 2*(500,350)-(300,200)
}

private fun heroPath2() = Path().apply {
    moveTo(200f, 500f)
    quadraticTo(400f, 600f, 600f, 450f)
    quadraticTo(800f, 300f, 1100f, 300f) // T：控制点 (800,300) = 2*(600,450)-(400,600)
}

private fun heroPath3() = Path().apply {
    moveTo(50f, 300f)
    quadraticTo(250f, 100f, 450f, 250f)
    quadraticTo(650f, 400f, 850f, 150f) // T：控制点 (650,400) = 2*(450,250)-(250,100)
}

/**
 * HeroBackdrop —— 复刻 Web 端 page.tsx HeroBackdrop（SVG viewBox 1200x800，opacity 0.3，slice 裁剪）
 * - 3 条虚线流动路径（dash 20s / dash-reverse 25s / dash 20s+delay）
 * - 8 个发光节点（光晕 pulse-slow 3s + 实心 glow 2s）
 */
@Composable
fun HeroBackdrop(modifier: Modifier = Modifier) {
    val brandGlow = BrandGlowThemed
    val brandDim = BrandDimThemed

    val transition = rememberInfiniteTransition(label = "heroBackdrop")
    // dash：stroke-dashoffset 0 → -100（20s linear）
    val dashPhase by transition.animateFloat(
        0f, -100f,
        infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart), label = "dash"
    )
    // dash-reverse：0 → 100（25s）
    val dashPhaseReverse by transition.animateFloat(
        0f, 100f,
        infiniteRepeatable(tween(25_000, easing = LinearEasing), RepeatMode.Restart), label = "dashReverse"
    )
    // pulse-slow：0.4 ↔ 0.8（3s ease-in-out）
    val pulse by transition.animateFloat(
        0.4f, 0.8f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )
    // glow：实心点亮度 0.7 ↔ 1.0（2s，drop-shadow 2px↔8px 近似）
    val glowAlpha by transition.animateFloat(
        0.7f, 1f,
        infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )

    val path1 = remember { heroPath1() }
    val path2 = remember { heroPath2() }
    val path3 = remember { heroPath3() }

    Canvas(modifier = modifier) {
        // viewBox 1200x800 slice：等比放大至覆盖，居中裁剪
        val scale = maxOf(size.width / 1200f, size.height / 800f)
        val dx = (size.width - 1200f * scale) / 2f
        val dy = (size.height - 800f * scale) / 2f

        drawContext.canvas.save()
        drawContext.canvas.translate(dx, dy)
        drawContext.canvas.scale(scale, scale)

        val lineBrush = Brush.linearGradient(
            0f to brandDim.copy(alpha = 0f),
            0.5f to brandGlow.copy(alpha = 0.8f),
            1f to brandDim.copy(alpha = 0f),
            start = Offset(0f, 0f),
            end = Offset(1200f, 0f),
        )
        val opacity = 0.3f

        // 路径 1：strokeWidth 1.5，dash 8,4，dash 动画
        drawPath(
            path1, brush = lineBrush, alpha = opacity,
            style = Stroke(
                width = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), dashPhase),
            ),
        )
        // 路径 2：strokeWidth 1，dash 6,6，dash-reverse（delay 1s 近似忽略，相位错开即可）
        drawPath(
            path2, brush = lineBrush, alpha = opacity,
            style = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), dashPhaseReverse),
            ),
        )
        // 路径 3：strokeWidth 0.8，dash 4,8，brand-dim 纯色，dash 动画
        drawPath(
            path3, color = brandDim, alpha = opacity,
            style = Stroke(
                width = 0.8f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), dashPhase),
            ),
        )

        // 发光节点：光晕 r*3（radial gradient brand-glow 0.6→0，pulse）+ 实心 r（glow）
        heroNodes.forEach { node ->
            val glowBrush = Brush.radialGradient(
                0f to brandGlow.copy(alpha = 0.6f * pulse),
                1f to brandGlow.copy(alpha = 0f),
                center = Offset(node.x, node.y),
                radius = node.r * 3f * (1f + (pulse - 0.4f) / 0.4f * 0.2f), // scale 1↔1.2
            )
            drawCircle(brush = glowBrush, radius = node.r * 3f * 1.2f, center = Offset(node.x, node.y), alpha = opacity)
            drawCircle(color = brandGlow.copy(alpha = glowAlpha), radius = node.r, center = Offset(node.x, node.y), alpha = opacity)
        }

        drawContext.canvas.restore()
    }
}

// ==================== HeroTitle：SVG 渐变文字逐字动画 ====================

private val HeroTitleGradientColors = listOf(
    0f to Color(0xFF5EEDA0),
    0.35f to Color(0xFF40C78A),
    0.5f to Color(0xFF60C4A0),
    0.65f to Color(0xFF9055E0),
    1f to Color(0xFFA855F7),
)

/**
 * HeroTitle —— 复刻 Web 端 hero-title.tsx（animated 模式）
 * - viewBox 600x300 容器 w-[22rem]（352dp）显示 → 76px 字体 ≈ 45sp
 * - 两行「星河入眠」「伴你梦枕」，5 色对角渐变（整行连续渐变）
 * - 逐字 reveal：opacity 0→1 + translateY 20px→0，0.6s ease-out，行内字延迟 60ms 递增
 *
 * 实现：Canvas + nativeCanvas（Paint + LinearGradient shader 统一坐标系，保证跨字连续渐变）
 */
@Composable
fun HeroTitle(modifier: Modifier = Modifier) {
    val line1 = "星河入眠"
    val line2 = "伴你梦枕"
    // Web 端逐字延迟：行 1 从 200ms 起 60ms 递增；行 2 从 500ms 起
    val delays1 = remember { line1.indices.map { 200 + it * 60 } }
    val delays2 = remember { line2.indices.map { 500 + it * 60 } }

    // 每字 reveal 进度（0→1），独立动画
    val progress1 = line1.map { delayMs -> remember { Animatable(0f) } }
    val progress2 = line2.map { delayMs -> remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        line1.indices.forEach { i ->
            launch {
                delay(delays1[i].toLong())
                progress1[i].animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
        }
        line2.indices.forEach { i ->
            launch {
                delay(delays2[i].toLong())
                progress2[i].animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
        }
    }

    val density = LocalDensity.current
    val fontSizePx = with(density) { 45.dp.toPx() } // 76/600×352dp ≈ 44.6dp
    val lineHeightPx = fontSizePx * 1.2f

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        Canvas(
            modifier = Modifier
                .width(maxWidth)
                .height(with(density) { (lineHeightPx * 2f).toDp() })
        ) {
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = fontSizePx
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                // 整体对角渐变（0,0 → 100%,100%，统一坐标系）
                shader = android.graphics.LinearGradient(
                    0f, 0f, widthPx, lineHeightPx * 2f,
                    HeroTitleGradientColors.map { it.second.toArgb() }.toIntArray(),
                    HeroTitleGradientColors.map { it.first }.toFloatArray(),
                    android.graphics.Shader.TileMode.CLAMP,
                )
            }
            drawContext.canvas.nativeCanvas.apply {
                listOf(line1 to progress1, line2 to progress2).forEachIndexed { row, (text, progresses) ->
                    val textWidth = paint.measureText(text)
                    var x = (size.width - textWidth) / 2f
                    val baseline = lineHeightPx * (row + 1) - paint.descent()
                    text.forEachIndexed { i, ch ->
                        val p = progresses[i].value
                        paint.alpha = (p * 255).toInt()
                        val yOffset = (1f - p) * 20f * density.density
                        drawText(ch.toString(), x, baseline + yOffset, paint)
                        x += paint.measureText(ch.toString())
                    }
                }
            }
        }
    }
}

// ==================== WordReveal：副标题逐字渐显 ====================

/**
 * WordReveal —— 复刻 Web 端 page.tsx WordReveal
 * - 按 separator「·」分段，段内逐字 reveal（opacity+translateY(20px)+scale(0.9)，blur 忽略）
 * - 段间分隔符 brand-glow/50
 * - 每字延迟：delayBase + 段序 × wordDelay + 字序 × 40ms
 */
@Composable
fun WordReveal(
    text: String,
    modifier: Modifier = Modifier,
    delayBase: Int = 0,
    wordDelay: Int = 150,
    fontSize: TextUnit = 12.sp,
    color: Color = Color.Unspecified,
) {
    val words = text.split("·").filter { it.isNotBlank() }
    // 与 Web 端一致的延迟公式：delayBase + 段序 × wordDelay + 字序 × 40ms（分隔符 = 段末字延迟位）
    val flatChars = remember(words, delayBase, wordDelay) {
        buildList {
            words.forEachIndexed { wi, word ->
                word.forEachIndexed { j, ch ->
                    add(Triple(ch, false, delayBase + wi * wordDelay + j * 40))
                }
                if (wi < words.lastIndex) add(Triple('·', true, delayBase + wi * wordDelay + word.length * 40))
            }
        }
    }
    val progresses = flatChars.map { remember { Animatable(0f) } }
    LaunchedEffect(Unit) {
        flatChars.forEachIndexed { i, (_, _, d) ->
            launch {
                delay(d.toLong())
                progresses[i].animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
        }
    }

    val brandGlow = BrandGlowThemed
    val baseColor = if (color == Color.Unspecified)
        androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    else color
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        flatChars.forEachIndexed { i, (ch, isSep, _) ->
            val p = progresses[i].value
            androidx.compose.material3.Text(
                ch.toString(),
                fontSize = fontSize,
                color = if (isSep) brandGlow.copy(alpha = 0.5f * p) else baseColor.copy(alpha = baseColor.alpha * p),
                modifier = Modifier
                    .offset(y = ((1f - p) * 8f).dp)
                    .padding(horizontal = if (isSep) 8.dp else 0.dp),
            )
        }
    }
}
