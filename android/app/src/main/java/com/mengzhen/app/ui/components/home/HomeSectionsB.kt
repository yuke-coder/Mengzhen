package com.mengzhen.app.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mengzhen.app.R
import com.mengzhen.app.ui.theme.*
import kotlinx.coroutines.launch

// ==================== §3 展示模式切换 + 全功能免费 ====================

/** DisplayModeSection —— 复刻 page.tsx #display-mode：2 主题大卡 + 8 免费权益卡 */
@Composable
fun DisplayModeSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        RevealGroup {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "随心切换",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                Text(
                    "展示模式",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    style = TextStyle(brush = brandTextGradient()),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "适配不同使用环境，自动切换最佳视觉体验",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // 2 主题大卡（grid-cols-2）
        RevealGroup(delayBase = 100) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // 日间模式
                Box(Modifier.weight(1f)) {
                    ThemeModeCard(
                        diffuseIndex = 34,
                        borderColor = Color(0xFFF59E0B),
                        iconBg = Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFFCD34D))),
                        icon = { Icon(Icons.Default.LightMode, null, Modifier.size(20.dp), tint = Color(0xFFB45309)) },
                        title = "日间模式",
                        titleColor = Color(0xFFB45309),
                        desc = "明亮清晰，适合白天",
                        descColor = Color(0xCCB45309),
                    ) {
                        scope.launch { ThemeModeStore.setMode(context, ThemeMode.LIGHT) }
                    }
                }
                // 夜间模式
                Box(Modifier.weight(1f)) {
                    ThemeModeCard(
                        diffuseIndex = 35,
                        borderColor = Color(0xFF6366F1),
                        iconBg = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF9333EA))),
                        icon = { Icon(Icons.Default.Nightlight, null, Modifier.size(20.dp), tint = Color.White) },
                        title = "夜间模式",
                        titleColor = Color(0xFF818CF8),
                        desc = "柔和护眼，适合夜晚",
                        descColor = Color(0xCC818CF8),
                    ) {
                        scope.launch { ThemeModeStore.setMode(context, ThemeMode.DARK) }
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        // 全功能免费
        RevealGroup {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "全功能免费",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    style = TextStyle(brush = brandTextGradient()),
                )
                Text(
                    "使用权益",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "无需付费、无需订阅、无任何限制，尽情享受完整功能",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // 8 免费卡（grid-cols-2）
        val freeCards = listOf(
            FreeCardData(Icons.Default.CardGiftcard, "全部功能免费", "无付费门槛", Color(0xFF22C55E)),
            FreeCardData(Icons.Default.Schedule, "全自动定时", "到点自动播放", Color(0xFF3B82F6)),
            FreeCardData(Icons.Default.Bolt, "无广告弹窗", "纯净体验", Color(0xFFA855F7)),
            FreeCardData(Icons.Default.Shield, "隐私零收集", "数据安全", Color(0xFFF59E0B)),
            FreeCardData(Icons.Default.Smartphone, "全平台通用", "多设备同步", Color(0xFF06B6D4)),
            FreeCardData(Icons.Default.Lock, "密码安全加密", "银行级保障", Color(0xFFEC4899)),
            FreeCardData(Icons.Default.Bolt, "极速加载", "原生应用", Color(0xFF10B981)),
            FreeCardData(Icons.Default.WorkspacePremium, "专属音频库", "云端存储", Color(0xFF6366F1)),
        )
        RevealGroup(delayBase = 100) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                (0 until freeCards.size step 2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FreeCard(36 + row, freeCards[row], Modifier.weight(1f))
                        FreeCard(36 + row + 1, freeCards[row + 1], Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

/** 主题模式大卡 */
@Composable
private fun ThemeModeCard(
    diffuseIndex: Int,
    borderColor: Color,
    iconBg: Brush,
    icon: @Composable () -> Unit,
    title: String,
    titleColor: Color,
    desc: String,
    descColor: Color,
    onClick: () -> Unit,
) {
    DiffuseCard(diffuseIndex, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Column(Modifier.padding(end = 44.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 12.sp, color = descColor, maxLines = 1)
            }
        }
    }
}

private data class FreeCardData(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val color: Color,
)

/** 免费权益小卡 */
@Composable
private fun FreeCard(diffuseIndex: Int, data: FreeCardData, modifier: Modifier = Modifier) {
    DiffuseCard(diffuseIndex, modifier = modifier) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(data.color.copy(alpha = 0.3f), data.color.copy(alpha = 0.15f))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(data.icon, null, Modifier.size(24.dp), tint = data.color)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                data.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                data.desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ==================== §4 困扰 PainCard ====================

/** PainPointsSection —— 复刻 page.tsx「你是否也遇到过这些困扰」4 卡 + Sparkles pill */
@Composable
fun PainPointsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        RevealGroup {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "你是否也遇到过",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                Text(
                    "这些困扰",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    style = TextStyle(brush = brandTextGradient()),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "梦枕帮你轻松解决",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
        Spacer(Modifier.height(40.dp))

        val pains = listOf(
            PainData(Icons.Default.Upload, "音频难上传", "想用专属音频助眠，却找不到支持私人音频文件的应用", Color(0xFF14B8A6)),
            PainData(Icons.Default.Schedule, "定时不智能", "普通定时器无法自动停止，半夜醒来还得手动关闭", Color(0xFF3B82F6)),
            PainData(Icons.AutoMirrored.Filled.VolumeUp, "启停太突兀", "音频突然播放或停止，音量骤变极易惊醒浅眠的你", Color(0xFF8B5CF6)),
            PainData(Icons.Default.Bolt, "操作太繁琐", "现有工具功能分散，全流程自动化难以实现", Color(0xFFF59E0B)),
        )
        RevealGroup(delayBase = 100) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                pains.forEachIndexed { i, pain ->
                    DiffuseCard(44 + i, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(pain.color.copy(alpha = 0.2f), pain.color.copy(alpha = 0.1f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(pain.icon, null, Modifier.size(28.dp), tint = BrandGlowThemed)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "${pain.title}？",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                pain.desc,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        RevealGroup(delayBase = 300) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(BrandStartThemed.copy(alpha = 0.1f), BrandEndThemed.copy(alpha = 0.1f))
                        )
                    )
                    .border(1.dp, BrandStartThemed.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp), tint = BrandGlowThemed)
                Text(
                    "上传音频·自定义定时·淡入淡出·全自动运行",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

private data class PainData(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val color: Color,
)

// ==================== §5 助眠能力（TemplateSelector 15 卡） ====================

private val templateCardColors = listOf(
    listOf("#9ED2BE", "#7BC4A8", "#5BB892"),
    listOf("#A5C4E4", "#7BA8D4", "#528CC4"),
    listOf("#F5D0A9", "#F0B878", "#E8A04E"),
    listOf("#E8B4D4", "#D48CB8", "#C0649C"),
    listOf("#B8E0B8", "#90C890", "#68B068"),
    listOf("#D4C4F0", "#B8A4E0", "#9C84D0"),
    listOf("#FFB88C", "#FF9860", "#FF7838"),
    listOf("#FFC4C4", "#FF9898", "#FF6C6C"),
    listOf("#C4E0F0", "#98C4E0", "#6CA8D0"),
    listOf("#E0C4F0", "#C498E0", "#A86CD0"),
    listOf("#F0D4B8", "#E0B890", "#D09C68"),
    listOf("#B8D4F0", "#90B8E0", "#689CD0"),
    listOf("#BFE6FF", "#86C8F2", "#4AA8DF"),
    listOf("#D7F5C8", "#A9DF8E", "#7CC762"),
    listOf("#FFD6A5", "#FFB86B", "#F4973A"),
    listOf("#CFC7FF", "#AFA2F2", "#8674E6"),
)

private data class TemplateCardData(
    val title: String,
    val desc: String,
    val icon: ImageVector,
)

// 文案词表：PWA辅助 → 原生辅助（配合媒体通知和锁屏控制）
private val templateCards = listOf(
    TemplateCardData("私人音频", "导入你熟悉的助眠声音", Icons.Default.Headphones),
    TemplateCardData("多格式上传", "适配 mp3、wav、ogg 等格式", Icons.Default.Upload),
    TemplateCardData("本地持久", "游客音频可存入浏览器本地", Icons.Default.Storage),
    TemplateCardData("云端同步", "登录后保存到云端音频库", Icons.Default.Cloud),
    TemplateCardData("精准定时", "按年月日时分秒创建任务", Icons.Default.EventRepeat),
    TemplateCardData("播放时长", "控制每次播放持续多久", Icons.Default.Schedule),
    TemplateCardData("淡入淡出", "减少突然启停带来的惊醒", Icons.AutoMirrored.Filled.VolumeUp),
    TemplateCardData("重复规则", "一次、每天、工作日、节假日", Icons.Default.Repeat),
    TemplateCardData("任务列表", "查看播放状态并随时编辑", Icons.AutoMirrored.Filled.QueueMusic),
    TemplateCardData("原生辅助", "配合媒体通知和锁屏控制", Icons.Default.Smartphone),
    TemplateCardData("状态恢复", "页面回到前台后同步任务", Icons.Default.Storage),
    TemplateCardData("安静执行", "围绕夜间使用减少打扰", Icons.Default.VerifiedUser),
    TemplateCardData("试听预览", "上传后先试听再加入任务", Icons.Default.AudioFile),
    TemplateCardData("醒后续播", "半夜醒来一键接着播放", Icons.Default.PlayCircle),
    TemplateCardData("参数微调", "音量与渐变秒数可细调", Icons.Default.Tune),
)

/** TemplatesSection —— 复刻 page.tsx #templates「助眠能力」+ TemplateSelector（grid-cols-2，纯展示） */
@Composable
fun TemplatesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        RevealGroup {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "助眠能力",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp, // tracking-wide
                    style = TextStyle(brush = brandTextGradient()),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "围绕夜间自动播放，把关键细节拆成可感知的功能卡片",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        RevealGroup(delayBase = 100) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                (0 until templateCards.size step 2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TemplateCard(row, templateCards[row], Modifier.weight(1f))
                        if (row + 1 < templateCards.size) {
                            TemplateCard(row + 1, templateCards[row + 1], Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** 功能小卡（渐变 icon 方块 + 标题 + 描述，hover 效果不复刻） */
@Composable
private fun TemplateCard(index: Int, data: TemplateCardData, modifier: Modifier = Modifier) {
    val (from, _, to) = templateCardColors[index % templateCardColors.size].map { Color(android.graphics.Color.parseColor(it)) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Column {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(from.copy(alpha = 0.5f), to), start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset.Infinite)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(data.icon, null, Modifier.size(20.dp), tint = Color.Black.copy(alpha = 0.55f))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                data.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                data.desc,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
            )
        }
    }
}

// ==================== §6 简单三步 ====================

/** ThreeStepsSection —— 复刻 page.tsx「简单三步」3 卡（h-200px，顶部渐变色条） */
@Composable
fun ThreeStepsSection() {
    val steps = listOf(
        StepData("01", Icons.Default.Upload, "上传音频", "导入助眠音乐、白噪音或自己的录音文件", BrandStartThemed),
        StepData("02", Icons.Default.Schedule, "设置任务", "选择开始时间、播放时长、音量和淡入淡出", BrandMidThemed),
        StepData("03", Icons.Default.Headphones, "自动播放", "让任务在夜里按计划执行，减少手动操作", BrandEndThemed),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        RevealGroup {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "简单",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                )
                Text(
                    "三步",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    style = TextStyle(brush = brandTextGradient()),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "从音频到任务，几步完成夜间自动播放",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(Modifier.height(48.dp))
        steps.forEachIndexed { i, step ->
            RevealGroup(delayBase = i * 120) {
                DiffuseCard(48 + i, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        // 顶部渐变色条
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(step.color.copy(alpha = 0.6f), Color.Transparent)
                                    )
                                )
                        )
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(step.color.copy(alpha = 0.2f), Color.Transparent)
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(step.icon, null, Modifier.size(28.dp), tint = BrandGlowThemed)
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                step.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                step.desc,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.height(56.dp))
    }
}

private data class StepData(
    val num: String,
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val color: Color,
)

// ==================== §7 开始设置（底部 CTA） ====================

/**
 * StartSection —— 复刻 page.tsx #start「开始设置」
 * 含 bottomCta（FloatingBar 可见性锚点，onCtaPositioned 上报位置）
 */
@Composable
fun StartSection(
    onStart: () -> Unit,
    onCtaPositioned: ((Boolean) -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        RevealGroup {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "开始",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                )
                Text(
                    "设置",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    style = TextStyle(brush = brandTextGradient()),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "上传音频并创建任务，让梦枕按你的节奏播放",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                // 无需登录 pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(BrandStartThemed.copy(alpha = 0.15f), BrandEndThemed.copy(alpha = 0.1f))
                            )
                        )
                        .border(1.dp, BrandStartThemed.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(BrandStartThemed))
                    Text("无需登录·开箱即用", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BrandStartThemed)
                }
                Spacer(Modifier.height(32.dp))
                // 大按钮「开始设置」（bottomCta 锚点：上报视口可见性给 FloatingBar）
                Box(
                    modifier = Modifier
                        .viewportVisibilityTracker(onCtaPositioned)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(BrandStartThemed, BrandEndThemed)
                            )
                        )
                        .clickable(onClick = onStart)
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = R.drawable.logo,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "开始设置",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(56.dp))
    }
}

// ==================== §8 Footer ====================

/** HomeFooter —— 复刻 page.tsx footer（logo + 渐变"梦枕" + 简介行，词表替换 PWA→原生应用） */
@Composable
fun HomeFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(vertical = 32.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = R.drawable.logo,
                contentDescription = "梦枕",
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "梦枕",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(brush = brandTextGradient()),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "深夜助眠播放器·原生应用·自定义音频",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
