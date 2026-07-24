package com.mengzhen.app.ui.components.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mengzhen.app.R
import com.mengzhen.app.ui.theme.BrandEndThemed
import com.mengzhen.app.ui.theme.BrandGlowThemed
import com.mengzhen.app.ui.theme.BrandStartThemed

/** Web 端 special-button 主色 rgb(40,184,148) */
private val InstallGreen = Color(0xFF28B894)

/** Web 端箭头方块底色 rgb(25,26,35) */
private val ArrowBlockBg = Color(0xFF191A23)

// ==================== HeroBadge：用户认证 · 全自动流程 ====================

/**
 * HeroBadge（mobile）—— 复刻 Web 端 page.tsx HeroBadge
 * pill：brand 渐变底 + border + 脉搏点 + "用户认证" + 分隔线 + "全自动流程"（serif）
 */
@Composable
fun HeroBadge(modifier: Modifier = Modifier) {
    val brandStart = BrandStartThemed
    val transition = rememberInfiniteTransition(label = "badgePulse")
    // animate-pulse：opacity 1 → 0.5 → 1（2s）
    val pulseAlpha by transition.animateFloat(
        1f, 0.5f,
        infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(brandStart.copy(alpha = 0.2f), BrandEndThemed.copy(alpha = 0.15f))
                )
            )
            .border(1.dp, brandStart.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(brandStart.copy(alpha = pulseAlpha))
        )
        Text(
            "用户认证",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = brandStart,
            letterSpacing = 0.5.sp, // tracking-wide
        )
        Box(
            Modifier
                .width(1.dp)
                .height(14.dp)
                .background(brandStart.copy(alpha = 0.3f))
        )
        Text(
            "全自动流程",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = brandStart.copy(alpha = 0.6f),
            fontFamily = FontFamily.Serif, // Web 端 Georgia/KaiTi serif 栈
            letterSpacing = 0.5.sp,
        )
    }
}

// ==================== 权限设置按钮（Web InstallButton 样式，文案与逻辑已改） ====================

/** 箭头 SVG path（viewBox 30x30，Web 端 install-button 同款对角箭头；H/V 指令已展开为 lineTo） */
private fun arrowPath() = Path().apply {
    moveTo(3.7546f, 29.5444f)
    lineTo(0.632812f, 26.4936f)
    lineTo(23.4077f, 3.29306f)
    lineTo(24.188f, 4.71205f)
    lineTo(4.74788f, 4.71205f)   // H4.74788
    lineTo(4.74788f, 0.455078f)  // V0.455078
    lineTo(26.4584f, 0.455078f)  // H26.4584
    lineTo(29.3674f, 3.36401f)
    lineTo(29.3674f, 25.0746f)   // V25.0746
    lineTo(25.1105f, 25.0746f)   // H25.1105
    lineTo(25.1105f, 5.63439f)   // V5.63439
    lineTo(26.5294f, 6.41485f)
    lineTo(3.7546f, 29.5444f)
    close()
}

/**
 * 权限设置按钮 —— 保留 Web 端 InstallButton 全部样式（special-button 移动端规格），
 * 文案「安装梦枕」→「权限设置」，点击逻辑 → 权限设置页（喜马拉雅式）。
 * 规格：h-40dp / rounded 4dp / bg #28B894 / padding 2-8-2-2 / gap 6dp / logo 36dp / 黑字 13sp Bold / 箭头方块 28dp #191A23
 */
@Composable
fun PermissionSetupButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val arrow = remember { arrowPath() }
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(InstallGreen)
            .clickable(onClick = onClick)
            .padding(start = 2.dp, top = 2.dp, end = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = R.drawable.logo,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit,
        )
        Text(
            "权限设置",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = 0.6.sp,
        )
        // 箭头方块（28x28 深色底 + 白色对角箭头 14dp）
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ArrowBlockBg),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(14.dp)) {
                val scale = size.width / 30f
                drawContext.canvas.save()
                drawContext.canvas.scale(scale, scale)
                drawPath(arrow, Color.White)
                drawContext.canvas.restore()
            }
        }
    }
}

// ==================== HeroCta：免费体验（outline-button） ====================

/**
 * HeroCta —— 复刻 Web 端 outline-button 移动端规格
 * h-40dp / px-16dp / border 2dp #28B894 / rounded 4dp / 文字 #28B894 14sp Bold
 */
@Composable
fun HeroCta(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(2.dp, InstallGreen, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "免费体验",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = InstallGreen,
            letterSpacing = 0.6.sp,
        )
    }
}

// ==================== HeroChips：三个特性 pill ====================

private data class HeroChipData(val icon: ImageVector, val text: String)

private val heroChips = listOf(
    HeroChipData(Icons.Default.MusicNote, "多格式音频适配"),
    HeroChipData(Icons.Default.Schedule, "时段自定义配置"),
    HeroChipData(Icons.AutoMirrored.Filled.VolumeUp, "精细化音量管控"),
)

/**
 * HeroChips（mobile）—— 复刻 Web 端 page.tsx HeroChips
 * 横排 nowrap gap-6dp，pill：rounded-full border-border/50，icon 12dp brand-glow + 12sp 文字
 */
@Composable
fun HeroChips(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heroChips.forEach { chip ->
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    chip.icon,
                    contentDescription = null,
                    tint = BrandGlowThemed,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    chip.text,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}
