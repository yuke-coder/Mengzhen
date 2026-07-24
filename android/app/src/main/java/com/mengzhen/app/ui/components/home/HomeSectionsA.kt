package com.mengzhen.app.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mengzhen.app.ui.theme.BrandGlowThemed

// ==================== 共用小件 ====================

/** 小节 pill 标签（核心优势/核心价值 等） */
@Composable
private fun SectionPill(text: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(BrandGlowThemed.copy(alpha = 0.1f))
            .border(1.dp, BrandGlowThemed.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = BrandGlowThemed, modifier = Modifier.size(14.dp))
        } else {
            Box(Modifier.size(6.dp).clip(CircleShape).background(BrandGlowThemed))
        }
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BrandGlowThemed)
    }
}

/** 大标题（前景色 + 渐变行） */
@Composable
private fun SectionTitle(
    plain: String,
    gradient: String,
    plainSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    subtitle: String? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            plain,
            fontSize = plainSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
        Text(
            gradient,
            fontSize = plainSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            style = TextStyle(brush = brandTextGradient()),
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                subtitle,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 小板块标题（text-2xl） */
@Composable
private fun SubSectionTitle(title: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

/** 特性小卡（icon 方块 + 标题 + 描述），homeLiftCardClass 结构 */
@Composable
private fun FeatureMiniCard(
    diffuseIndex: Int,
    icon: ImageVector,
    title: String,
    desc: String,
    iconTint: Color = BrandGlowThemed,
    iconBox: Boolean = true,
) {
    DiffuseCard(diffuseIndex, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconBox) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, Modifier.size(20.dp), tint = iconTint)
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                desc,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/** 两行一列简易网格（grid-cols-2 移动端保持 2 列用） */
@Composable
private fun TwoColumnGrid(
    count: Int,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 10.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 10.dp,
    itemContent: @Composable BoxScope.(Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
        (0 until count step 2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
                Box(Modifier.weight(1f)) { itemContent(row) }
                if (row + 1 < count) {
                    Box(Modifier.weight(1f)) { itemContent(row + 1) }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ==================== §1 Features 大区块 ====================

/**
 * FeaturesSection —— 复刻 page.tsx #features（py-32）
 * 标题 + 4 痛点卡 + 3 价值卡 + 6 配置卡 + 4 后台卡 + 桌面 pill + 3 存储卡 + 极简定位 + 2 安全卡
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FeaturesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))

        // 标题组
        RevealGroup {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SectionPill("核心优势")
                Spacer(Modifier.height(24.dp))
                SectionTitle(
                    plain = "专为中国浅眠人群",
                    gradient = "匠心打造",
                    subtitle = "深度适配睡眠浅、对音量突变敏感、半夜易醒的用户群体",
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        // 4 痛点卡（单列）
        val pains = listOf(
            Triple(Icons.Default.Nightlight, "睡眠深度较浅", "对音量突变敏感，音频启停稍有不慎便会彻底惊醒，难以再次入睡"),
            Triple(Icons.Default.LightMode, "夜间易中途觉醒", "入睡效率良好，但夜间频繁中途醒来，需要柔和音频辅助接续睡眠"),
            Triple(Icons.AutoMirrored.Filled.VolumeUp, "音量突变惊醒", "精准音量渐入渐出自定义，彻底规避音频启停音量骤变惊醒用户的问题"),
            Triple(Icons.Default.Schedule, "深夜操作困难", "半夜醒来后睡意朦胧，不愿手动操作手机，一键预设定时播放完美适配"),
        )
        pains.forEachIndexed { i, (icon, title, desc) ->
            DiffuseCard(i, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        com.mengzhen.app.ui.theme.BrandStartThemed.copy(alpha = 0.2f),
                                        com.mengzhen.app.ui.theme.BrandEndThemed.copy(alpha = 0.1f),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, Modifier.size(24.dp), tint = BrandGlowThemed)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            desc,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(48.dp))

        // 接续睡眠 3 卡
        SubSectionTitle("夜间觉醒后接续睡眠", "不求辅助入眠，只为中途觉醒后快速重新入睡")
        Spacer(Modifier.height(24.dp))
        val continueSleep = listOf(
            Triple(Icons.Default.Refresh, "觉醒自动续播", "夜间醒来后，柔和音频无缝衔接，帮助快速重新入睡"),
            Triple(Icons.AutoMirrored.Filled.VolumeUp, "零突变音量", "全程音量渐入渐出，彻底规避惊醒风险，营造柔和睡眠氛围"),
            Triple(Icons.Default.Nightlight, "黑屏后台播放", "锁屏休眠持续播放，不干扰睡眠，支持定时自动停止"),
        )
        continueSleep.forEachIndexed { i, (icon, title, desc) ->
            FeatureMiniCard(4 + i, icon, title, desc)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(36.dp))

        // 个性化音频配置 6 卡
        SubSectionTitle("个性化音频配置", "精细化音量控制，适配个人听觉耐受度")
        Spacer(Modifier.height(24.dp))
        val audioConfig = listOf(
            Triple(Icons.Default.MusicNote, "全格式兼容", "MP3、WAV、FLAC 等全主流音频格式"),
            Triple(Icons.Default.Headphones, "在线试听", "上传后实时预览，快速筛选适配音频"),
            Triple(Icons.AutoMirrored.Filled.VolumeUp, "小数级音量", "0-100% 精细化分级，支持 0.1 微调"),
            Triple(Icons.Default.Layers, "播放列表", "自定义音频播放顺序，编排专属播放列表"),
            Triple(Icons.Default.Tune, "全面自定义", "定时、渐变音量、播放规则全部可调"),
            Triple(Icons.Default.DateRange, "周期定时", "每日/工作日重复定时，适配长期规律睡眠"),
        )
        audioConfig.forEachIndexed { i, (icon, title, desc) ->
            FeatureMiniCard(7 + i, icon, title, desc)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(36.dp))

        // 后台稳定播放 4 卡
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                com.mengzhen.app.ui.theme.BrandStartThemed.copy(alpha = 0.2f),
                                com.mengzhen.app.ui.theme.BrandEndThemed.copy(alpha = 0.1f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Smartphone, null, Modifier.size(32.dp), tint = BrandGlowThemed)
            }
            Spacer(Modifier.height(16.dp))
            SubSectionTitle("后台稳定播放", "夜间锁屏休眠持续播放，不中断接续睡眠")
        }
        Spacer(Modifier.height(24.dp))
        val backgroundPlay = listOf(
            Triple(Icons.Default.Bolt, "后台唤醒", "锁屏休眠仍可定时唤醒正常播放"),
            Triple(Icons.Default.BatterySaver, "电池优化", "忽略电池优化引导，提升休眠稳定性"),
            Triple(Icons.Default.Refresh, "异常兜底", "系统杀进程后可自动重试唤醒"),
            Triple(Icons.Default.WifiOff, "离线模式", "断网网络不佳时定时播放正常"),
        )
        backgroundPlay.forEachIndexed { i, (icon, title, desc) ->
            DiffuseCard(13 + i, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandGlowThemed.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, Modifier.size(20.dp), tint = BrandGlowThemed)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        desc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 原生提示 pill（词表替换：PWA 语境 → 原生语境）
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(BrandGlowThemed.copy(alpha = 0.1f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Monitor, null, Modifier.size(16.dp), tint = BrandGlowThemed)
            Text(
                "已是原生 App，系统级后台稳定播放",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }

        Spacer(Modifier.height(48.dp))

        // 分层数据存储 3 卡
        SubSectionTitle("分层数据存储方案", "兼顾云端便捷性与本地隐私安全")
        Spacer(Modifier.height(24.dp))
        val storage = listOf(
            StorageCardData(Icons.Default.Storage, "云端数据库", "音频文件统一存入云端数据库，跨设备同步无缝使用", Color(0xFF38BDF8), Color(0xFF0EA5E9)),
            StorageCardData(Icons.Default.Cookie, "本地持久化", "Cookie 本地存储配置信息，响应速度快、隐私性强", Color(0xFFFBBF24), Color(0xFFF59E0B)),
            StorageCardData(Icons.Default.Shield, "自主可控", "支持云端备份开关，自主选择是否同步播放配置", Color(0xFF34D399), Color(0xFF10B981)),
        )
        storage.forEachIndexed { i, data ->
            DiffuseCard(17 + i, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp)) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(data.colorA.copy(alpha = 0.2f), data.colorB.copy(alpha = 0.1f)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(data.icon, null, Modifier.size(24.dp), tint = data.colorB)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        data.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        data.desc,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(36.dp))

        // 极简定位
        SubSectionTitle("纯粹极简的产品定位", "梦枕不争夺注意力，只在深夜替你完成播放这件事。")
        Spacer(Modifier.height(24.dp))
        DiffuseCard(22, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(1.dp, BrandGlowThemed.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text("留下必要，删掉噪音", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BrandGlowThemed)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "不把睡眠变成报表、课程或社区，只把助眠音频按时送到耳边。",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 30.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(24.dp))
                // 边界标签
                val boundaries = listOf("无广告", "无订阅", "无推荐流", "无社交分享", "无睡眠监测", "无多余弹窗")
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    boundaries.forEach { text ->
                        Text(
                            text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val principles = listOf(
            Triple(Icons.Default.MusicNote, "私人音频", "不做内容流，不推曲库，只播放你自己选择的声音。"),
            Triple(Icons.Default.Schedule, "自动任务", "设好时间、时长和音量渐变，夜里按计划安静执行。"),
            Triple(Icons.Default.Shield, "少即是安", "只保留音频和任务所需数据，不追踪睡眠，也不制造打扰。"),
        )
        principles.forEachIndexed { i, (icon, title, desc) ->
            DiffuseCard(23 + i, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BrandGlowThemed.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, Modifier.size(20.dp), tint = BrandGlowThemed)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            desc,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(36.dp))

        // 安全保障 2 卡
        SubSectionTitle("全方位隐私安全保障", "用户数据完全可控")
        Spacer(Modifier.height(24.dp))
        SecurityCard(
            26, Icons.Default.Shield, "银行级密码安全", "bcrypt 哈希算法加密", Color(0xFFA78BFA),
            listOf("不可逆加密处理，杜绝明文泄露", "独立随机盐值混合加密", "抵御彩虹表攻击、暴力破解"),
        )
        Spacer(Modifier.height(16.dp))
        SecurityCard(
            27, Icons.Default.Lock, "数据完全可控", "无第三方快捷登录", Color(0xFF22D3EE),
            listOf("不收集睡眠数据、不追踪使用行为", "音频素材自主上传管理", "无多余数据上报，仅存用户主动数据"),
        )

        Spacer(Modifier.height(56.dp))
    }
}

private data class StorageCardData(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val colorA: Color,
    val colorB: Color,
)

/** 安全卡（icon + 标题 + 副标 + 3 bullet） */
@Composable
private fun SecurityCard(
    diffuseIndex: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    bullets: List<String>,
) {
    DiffuseCard(diffuseIndex, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.2f), accent.copy(alpha = 0.1f)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, Modifier.size(24.dp), tint = accent)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    )
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.height(16.dp))
            bullets.forEach { text ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                    Spacer(Modifier.width(8.dp))
                    Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ==================== §2 用户群体 ====================

private data class AudienceData(
    val icon: ImageVector,
    val badge: String,
    val badgeColor: Color,
    val title: String,
    val desc: String,
)

/** AudienceSection —— 复刻 page.tsx「专为浅眠人群设计·接续睡眠」6 卡 */
@Composable
fun AudienceSection() {
    val audiences = listOf(
        AudienceData(Icons.Default.Nightlight, "核心用户", BrandGlowThemed, "浅眠 / 神经衰弱人群",
            "长期睡眠浅、半夜频繁惊醒、对音量突变极度敏感，需要柔和渐变音量 + 全自动定时 + 后台稳定播放"),
        AudienceData(Icons.Default.Work, "职场首选", Color(0xFFF59E0B), "高压都市上班族",
            "职场压力大、入睡困难，原生构建轻量流畅，全自动定时关闭，厌恶广告付费与臃肿APP"),
        AudienceData(Icons.Default.School, "校园适配", Color(0xFFA855F7), "住校学生群体",
            "宿舍环境嘈杂、集体作息受限，自定义专属助眠音频、音量柔和不吵室友，无冗余社交广告"),
        AudienceData(Icons.Default.Favorite, "新手爸妈", Color(0xFFEC4899), "产后宝妈 / 新手父母",
            "睡眠碎片化、夜间频繁惊醒，没有精力手动开关，全自动预设播放、后台静默运行、解放双手"),
        AudienceData(Icons.Default.Psychology, "个性化", Color(0xFF0EA5E9), "情绪性失眠 / 焦虑人群",
            "依赖个人专属音频助眠（冥想音、雨声、私人歌单），拒绝平台推送、商业化干扰"),
        AudienceData(Icons.Default.Group, "易上手", Color(0xFF10B981), "中老年浅眠用户",
            "睡眠周期短、半夜易醒，产品操作极简、全自动定时、无复杂功能，适配低门槛使用习惯"),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RevealGroup {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "专为浅眠人群设计",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "接续睡眠",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    style = TextStyle(brush = brandTextGradient()),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(48.dp))
        audiences.forEachIndexed { i, data ->
            DiffuseCard(28 + i, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(data.icon, null, Modifier.size(32.dp), tint = data.badgeColor)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        data.badge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = data.badgeColor,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(data.badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        data.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        data.desc,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
