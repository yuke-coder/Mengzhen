package com.mengzhen.app.ui.components.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mengzhen.app.ui.theme.BrandEnd
import com.mengzhen.app.ui.theme.BrandStart

/** 警告琥珀色（token：亮 #F59E0B / 暗 #FBBF24） */
val AmberWarning: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFF59E0B)

/** 「必要」文字 Chip（设计 §4.1：Primary 12% 底 + Primary 文字，12sp / Medium，圆角 6dp） */
@Composable
fun RequiredBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            "必要",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** WhyNeedCard 为什么需要（§4.3：品牌渐变 135° 低透明度底，无边框） */
@Composable
fun WhyNeedCard(text: String, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        BrandStart.copy(alpha = if (dark) 0.14f else 0.12f),
                        BrandEnd.copy(alpha = if (dark) 0.10f else 0.08f),
                    )
                )
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                fontSize = 14.sp,
                lineHeight = 22.4.sp, // 1.6 行高
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
            )
        }
    }
}

/** NoticeCard 注意事项（§4.7：琥珀 8%/12% 底 + 左侧 4dp 实条，无边框） */
@Composable
fun NoticeCard(text: String, modifier: Modifier = Modifier) {
    val amber = AmberWarning
    val dark = isSystemInDarkTheme()
    Box(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(amber.copy(alpha = if (dark) 0.12f else 0.08f))
                .padding(start = 18.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text,
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp, // 1.5 行高
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )
            }
        }
        // 左侧 4dp 琥珀实条
        Box(
            Modifier
                .width(4.dp)
                .matchParentSize()
                .background(amber)
        )
    }
}

/** AlternateCard 备选路径（§4.8：surface 底） */
@Composable
fun AlternateCard(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Text(
            "其他入口",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text,
            fontSize = 14.sp,
            lineHeight = 22.4.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
        )
    }
}

/** StatusBanner 状态回检横条（§4.9：Primary 10% 底，无边框） */
@Composable
fun StatusBanner(unfinishedCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "还有 $unfinishedCount 项必要设置未完成，完成后夜间播放才有保障",
            fontSize = 13.sp,
            lineHeight = 19.5.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
        )
    }
}

/** 单个路径 Chip（§4.6：Primary 10% 底，圆角 8dp，无边框） */
@Composable
fun PathChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** PathChip 路径行（§4.6：FlowRow + chevron 分隔） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PathChipRow(segments: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        segments.forEachIndexed { index, segment ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                PathChip(segment)
            }
        }
    }
}

/** FallbackTip 兜底提示（§4.10） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FallbackTip(searchKeyword: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "找不到对应入口？",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "在「设置」顶部搜索框输入 ",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PathChip(searchKeyword)
            Text(
                " 可直达；没有这一项就跳过，属正常",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 应急出口（§4.11：右对齐 TextButton，零打扰返回） */
@Composable
fun EmergencyExit(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onClick) {
            Text(
                "太困了？先点这里继续播放，明天再设置",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
