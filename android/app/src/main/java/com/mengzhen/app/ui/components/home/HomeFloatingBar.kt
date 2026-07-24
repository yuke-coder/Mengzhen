package com.mengzhen.app.ui.components.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mengzhen.app.R

/**
 * FloatingBar —— 复刻 Web 端 page.tsx FloatingBar（移动端）
 * - 显示条件：Hero 按钮与底部 CTA 均滚出视口（由外部传入 visible）
 * - 显隐动画：translate-y-4 + opacity（0.3s ease-out）
 * - 卡片：rounded-2xl 毛玻璃（半透明近似）+ border + 左 logo 渐变名 + 右「免费体验」渐变按钮
 */
@Composable
fun HomeFloatingBar(
    visible: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offsetY by animateDpAsState(
        if (visible) 0.dp else 16.dp,
        tween(300), label = "floatingBarY",
    )
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        tween(300), label = "floatingBarA",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .alpha(alpha)
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = R.drawable.logo,
                    contentDescription = "梦枕",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "梦枕",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    style = TextStyle(brush = brandTextGradient()),
                )
            }
            // 「免费体验」渐变按钮（brand 渐变底 + 白字 + chevron）
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                com.mengzhen.app.ui.theme.BrandStartThemed,
                                com.mengzhen.app.ui.theme.BrandEndThemed,
                            )
                        )
                    )
                    .clickable(onClick = onStart)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "免费体验",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
