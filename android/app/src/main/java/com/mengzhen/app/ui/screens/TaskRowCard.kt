package com.mengzhen.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mengzhen.app.audio.PlaybackSnapshot
import com.mengzhen.app.audio.PlaybackTransportState
import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.TaskPhase
import com.mengzhen.app.data.model.TaskRepeatType
import com.mengzhen.app.data.model.TaskStatus
import com.mengzhen.app.data.model.effectiveScheduledStopDurationSeconds
import com.mengzhen.app.data.model.hasActiveSchedule
import com.mengzhen.app.data.model.hasConfiguredStop

// ============================================================
// Design Tokens
// ============================================================

private object TaskRowTokens {
    // Dark mode
    val darkBg0 = Color(0xFF0F0D0B)
    val darkBg1 = Color(0xFF1A1611)
    val darkBg2 = Color(0xFF221E18)
    val darkBg3 = Color(0xFF2C2720)
    val darkT1 = Color(0xFFF2EDE2)
    val darkT2 = Color(0xFFA89E8E)
    val darkT3 = Color(0xFF8A8070)
    val darkBrand = Color(0xFF2BC496)
    val darkBrandHi = Color(0xFF7EEDC4)
    val darkBrandGlow = Color(0x4D2BC496) // 30% alpha
    val darkAmber = Color(0xFFE8A04C)
    val darkAmberHi = Color(0xFFF2BE7E)
    val darkAmberGlow = Color(0x38E8A04C)
    val darkDone = Color(0xFF7CAA98)
    val darkClosed = Color(0xFF6B6256)

    // Light mode
    val lightBg0 = Color(0xFFFAF7F2)
    val lightBg1 = Color(0xFFF3EDE4)
    val lightBg2 = Color(0xFFEBE3D6)
    val lightBg3 = Color(0xFFE0D5C4)
    val lightT1 = Color(0xFF2A2520)
    val lightT2 = Color(0xFF6B5F52)
    val lightT3 = Color(0xFFA89E8E)
    val lightBrand = Color(0xFF5BB892)
    val lightBrandHi = Color(0xFF2A8B68)
    val lightBrandGlow = Color(0x385BB892)
    val lightAmber = Color(0xFFD4892E)
    val lightAmberHi = Color(0xFFB06F1E)
    val lightAmberGlow = Color(0x29D4892E)
    val lightDone = Color(0xFF5E8C76)
    val lightClosed = Color(0xFFB5A99A)

    val cardRadius = 13.dp
    val cardRadiusOpen = 18.dp
    val cardPadding = 14.dp
    val cardMinHeight = 50.dp
    val cardGap = 11.dp
    val gridGap = 1.dp
    val gridRadius = 9.dp
    val gridCellPadding = 12.dp
}

// ============================================================
// Card visual state derived from task + playback
// ============================================================

enum class TaskRowVisualState {
    EXECUTING_PLAY,   // brand green + wave + pulse
    EXECUTING_FADE,   // amber + wave + pulse
    PENDING,          // gray dot
    COMPLETED,        // done green
    CANCELLED,        // closed gray
}

data class TaskRowData(
    val title: String,
    val startTimeText: String,
    val closeTimeText: String,
    val durationText: String,
    val repeatText: String,
    val volumePercent: Int,
    val fadeInSeconds: Int,
    val fadeOutSeconds: Int,
    val phaseLabel: String,
    val visualState: TaskRowVisualState,
    val defaultExpanded: Boolean = false,
    val scheduleEnabled: Boolean = true,
    val taskId: String = "",
)

fun ScheduledTask.toRowData(playback: PlaybackSnapshot): TaskRowData {
    val runtimeActive = playback.taskId == id &&
        playback.transportState != PlaybackTransportState.IDLE &&
        !playback.isTerminal

    val visualState = when {
        runtimeActive && playback.phase == TaskPhase.FADING_IN -> TaskRowVisualState.EXECUTING_FADE
        runtimeActive && playback.phase == TaskPhase.FADING_OUT -> TaskRowVisualState.EXECUTING_FADE
        runtimeActive && playback.transportState == PlaybackTransportState.PLAYING -> TaskRowVisualState.EXECUTING_PLAY
        runtimeActive && playback.transportState == PlaybackTransportState.PREPARING -> TaskRowVisualState.EXECUTING_PLAY
        status == TaskStatus.COMPLETED -> TaskRowVisualState.COMPLETED
        status == TaskStatus.CANCELLED || !hasActiveSchedule() -> TaskRowVisualState.CANCELLED
        else -> TaskRowVisualState.PENDING
    }

    val phaseLabel = when (visualState) {
        TaskRowVisualState.EXECUTING_PLAY -> when (playback.phase) {
            TaskPhase.FADING_IN -> "正在音量渐强"
            TaskPhase.FADING_OUT -> "正在音量渐弱"
            else -> "正在播放"
        }
        TaskRowVisualState.EXECUTING_FADE -> if (playback.phase == TaskPhase.FADING_IN) "正在音量渐强" else "正在音量渐弱"
        TaskRowVisualState.PENDING -> "等待执行"
        TaskRowVisualState.COMPLETED -> "已完成"
        TaskRowVisualState.CANCELLED -> if (status == TaskStatus.CANCELLED) "已关闭" else "未开启"
    }

    val startEnabled = hasActiveSchedule()
    val startTimeText = if (startEnabled) {
        "%02d:%02d:%02d".format(startTime.hour, startTime.minute, startTime.second)
    } else {
        "--:--:--"
    }

    val stopSeconds = if (hasConfiguredStop()) {
        effectiveScheduledStopDurationSeconds().takeIf { it > 0 }
    } else null

    val closeTimeText = if (stopSeconds != null && startEnabled) {
        val startMs = nextExecuteAt ?: startTime.toEpochMillis()
        val closeMs = startMs + stopSeconds * 1000L
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = closeMs }
        "%02d:%02d:%02d".format(
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
            cal.get(java.util.Calendar.SECOND),
        )
    } else if (runtimeActive && playback.endsAt > 0) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = playback.endsAt }
        "%02d:%02d:%02d".format(
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
            cal.get(java.util.Calendar.SECOND),
        )
    } else {
        "--:--:--"
    }

    val durationText = stopSeconds?.let { secs ->
        if (secs >= 3600) "${secs / 3600}h ${(secs % 3600) / 60}min"
        else "${secs / 60}min"
    } ?: "自然播完"

    val days = repeatDays ?: when (repeatType) {
        TaskRepeatType.ONCE -> 0
        TaskRepeatType.DAILY -> 127
        TaskRepeatType.WORKDAY -> 31
        TaskRepeatType.HOLIDAY -> 96
    }
    val repeatText = when (days) {
        0 -> "仅一次"
        31 -> "工作日"
        96 -> "周末"
        127 -> "每天"
        else -> "自定义"
    }

    val volPct = if (visualState == TaskRowVisualState.CANCELLED) 0 else volume

    return TaskRowData(
        title = name.ifBlank { "未命名任务" },
        startTimeText = startTimeText,
        closeTimeText = closeTimeText,
        durationText = durationText,
        repeatText = repeatText,
        volumePercent = volPct,
        fadeInSeconds = fadeInDuration,
        fadeOutSeconds = fadeOutDuration,
        phaseLabel = phaseLabel,
        visualState = visualState,
        defaultExpanded = visualState == TaskRowVisualState.EXECUTING_PLAY ||
            visualState == TaskRowVisualState.EXECUTING_FADE,
        scheduleEnabled = startEnabled,
        taskId = id,
    )
}

// ============================================================
// Token resolver for current theme
// ============================================================

private data class RowPalette(
    val bg0: Color, val bg1: Color, val bg2: Color, val bg3: Color,
    val t1: Color, val t2: Color, val t3: Color,
    val accent: Color, val accentHi: Color, val accentGlow: Color,
    val dotColor: Color,
    val chipBg: Color, val chipText: Color,
    val stagText: Color,
)

@Composable
private fun rememberPalette(isDark: Boolean, state: TaskRowVisualState): RowPalette {
    val p = if (isDark) {
        RowPalette(
            bg0 = TaskRowTokens.darkBg0, bg1 = TaskRowTokens.darkBg1,
            bg2 = TaskRowTokens.darkBg2, bg3 = TaskRowTokens.darkBg3,
            t1 = TaskRowTokens.darkT1, t2 = TaskRowTokens.darkT2, t3 = TaskRowTokens.darkT3,
            accent = TaskRowTokens.darkBrand, accentHi = TaskRowTokens.darkBrandHi,
            accentGlow = TaskRowTokens.darkBrandGlow,
            dotColor = TaskRowTokens.darkBrand, chipBg = Color(0x2243C196),
            chipText = TaskRowTokens.darkBrandHi, stagText = TaskRowTokens.darkBrandHi,
        )
    } else {
        RowPalette(
            bg0 = TaskRowTokens.lightBg0, bg1 = TaskRowTokens.lightBg1,
            bg2 = TaskRowTokens.lightBg2, bg3 = TaskRowTokens.lightBg3,
            t1 = TaskRowTokens.lightT1, t2 = TaskRowTokens.lightT2, t3 = TaskRowTokens.lightT3,
            accent = TaskRowTokens.lightBrand, accentHi = TaskRowTokens.lightBrandHi,
            accentGlow = TaskRowTokens.lightBrandGlow,
            dotColor = TaskRowTokens.lightBrand, chipBg = Color(0x225BB892),
            chipText = TaskRowTokens.lightBrandHi, stagText = TaskRowTokens.lightBrandHi,
        )
    }

    return remember(isDark, state) {
        when (state) {
            TaskRowVisualState.EXECUTING_PLAY -> p
            TaskRowVisualState.EXECUTING_FADE -> p.copy(
                accent = if (isDark) TaskRowTokens.darkAmber else TaskRowTokens.lightAmber,
                accentHi = if (isDark) TaskRowTokens.darkAmberHi else TaskRowTokens.lightAmberHi,
                accentGlow = if (isDark) TaskRowTokens.darkAmberGlow else TaskRowTokens.lightAmberGlow,
                dotColor = if (isDark) TaskRowTokens.darkAmber else TaskRowTokens.lightAmber,
                chipBg = if (isDark) Color(0x22E8A04C) else Color(0x22D4892E),
                chipText = if (isDark) TaskRowTokens.darkAmberHi else TaskRowTokens.lightAmberHi,
                stagText = if (isDark) TaskRowTokens.darkAmberHi else TaskRowTokens.lightAmberHi,
            )
            TaskRowVisualState.PENDING -> p.copy(
                dotColor = p.t3, chipBg = Color(0x1AA89E8E), chipText = p.t2, stagText = p.t3,
            )
            TaskRowVisualState.COMPLETED -> p.copy(
                dotColor = if (isDark) TaskRowTokens.darkDone else TaskRowTokens.lightDone,
                accentGlow = if (isDark) Color(0x297CAA98) else Color(0x245E8C76),
                chipBg = if (isDark) Color(0x207CAA98) else Color(0x1C5E8C76),
                chipText = if (isDark) TaskRowTokens.darkDone else TaskRowTokens.lightDone,
                stagText = p.t3,
            )
            TaskRowVisualState.CANCELLED -> p.copy(
                dotColor = if (isDark) TaskRowTokens.darkClosed else TaskRowTokens.lightClosed,
                chipBg = Color(0x14A89E8E), chipText = p.t3, stagText = if (isDark) TaskRowTokens.darkClosed else TaskRowTokens.lightClosed,
            )
        }
    }
}

// ============================================================
// Main Composable — flat structure, minimal nesting
// ============================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRowCard(
    data: TaskRowData,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggle: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(data.defaultExpanded) }
    val palette = rememberPalette(isDark, data.visualState)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(100),
        label = "press-scale",
    )

    val radius = if (expanded) TaskRowTokens.cardRadiusOpen else TaskRowTokens.cardRadius

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .scale(scale)
            .clip(RoundedCornerShape(radius))
            .background(palette.bg1)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    expanded = !expanded
                    onClick()
                },
                onLongClick = onLongClick,
            )
    ) {
        // Glow bar (left edge) for executing states
        if (data.visualState == TaskRowVisualState.EXECUTING_PLAY ||
            data.visualState == TaskRowVisualState.EXECUTING_FADE
        ) {
            GlowBar(palette)
        }

        Column(
            modifier = Modifier.padding(TaskRowTokens.cardPadding),
        ) {
            // Line 1: dot + title + wave + chip + switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TaskRowTokens.cardGap),
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatusDot(data.visualState, palette)

                Text(
                    text = data.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (data.visualState == TaskRowVisualState.CANCELLED ||
                        data.visualState == TaskRowVisualState.COMPLETED
                    ) palette.t2 else palette.t1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (data.visualState == TaskRowVisualState.EXECUTING_PLAY ||
                    data.visualState == TaskRowVisualState.EXECUTING_FADE
                ) {
                    WaveBars(palette)
                }

                Chip(data.phaseLabel, palette)

                if (onToggle != null) {
                    Switch(
                        checked = data.scheduleEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = palette.accent,
                            checkedThumbColor = palette.accentHi,
                            uncheckedTrackColor = palette.t3.copy(alpha = 0.3f),
                            uncheckedThumbColor = palette.t2,
                        ),
                        modifier = Modifier.scale(0.8f),
                    )
                }
            }

            // Line 2: time range on its own line
            Text(
                text = "${data.startTimeText} → ${data.closeTimeText}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (data.visualState == TaskRowVisualState.CANCELLED ||
                    data.visualState == TaskRowVisualState.COMPLETED
                ) palette.t3 else palette.t2,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 19.dp, top = 1.dp),
            )

            // Expanded section
            if (expanded) {
                Spacer(Modifier.height(13.dp))

                // Status label
                Text(
                    text = "● ${data.phaseLabel}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.stagText,
                    letterSpacing = 0.5.sp,
                )

                Spacer(Modifier.height(11.dp))

                // Time grid 2x2 — flat Row + Column, no wrapper
                TimeGrid(data, palette)

                Spacer(Modifier.height(11.dp))

                // Footer: volume + fades — flat Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VolumeBars(data.volumePercent, palette)
                    FadeTags(data.fadeInSeconds, data.fadeOutSeconds, palette)
                }
            }
        }
    }
}

// ============================================================
// Sub-components — each is a single Composable, no extra nesting
// ============================================================

@Composable
private fun GlowBar(palette: RowPalette) {
    val transition = rememberInfiniteTransition(label = "glow")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow-alpha",
    )
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(44.dp)
            .background(
                color = palette.accent.copy(alpha = alpha),
                shape = RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp),
            ),
    )
}

@Composable
private fun StatusDot(state: TaskRowVisualState, palette: RowPalette) {
    if (state == TaskRowVisualState.EXECUTING_PLAY ||
        state == TaskRowVisualState.EXECUTING_FADE
    ) {
        val transition = rememberInfiniteTransition(label = "dot")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot-scale",
        )
        Box(
            modifier = Modifier
                .size((8 * scale).dp)
                .clip(CircleShape)
                .background(palette.dotColor),
        )
    } else {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(palette.dotColor),
        )
    }
}

@Composable
private fun WaveBars(palette: RowPalette) {
    val transition = rememberInfiniteTransition(label = "wave")
    val bars = listOf(0f, 0.15f, 0.3f, 0.45f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(12.dp),
    ) {
        bars.forEach { delay ->
            val progress by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(350, delayMillis = (delay * 1000).toInt(), easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar-$delay",
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height((12 * progress).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(palette.accentHi),
            )
        }
    }
}

@Composable
private fun Chip(label: String, palette: RowPalette) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.chipBg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = palette.chipText,
        )
    }
}

@Composable
private fun Chevron(expanded: Boolean, palette: RowPalette) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300),
        label = "chev",
    )
    // Simple arrow drawn with rotation
    Text(
        text = "›",
        fontSize = 16.sp,
        color = palette.t3,
        modifier = Modifier,
    )
}

@Composable
private fun TimeGrid(data: TaskRowData, palette: RowPalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TaskRowTokens.gridRadius))
            .background(palette.bg2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TaskRowTokens.gridGap),
        ) {
            TimeCell("开始", data.startTimeText, palette, Modifier.weight(1f))
            TimeCell("关闭", data.closeTimeText, palette, Modifier.weight(1f))
        }
        Spacer(Modifier.height(TaskRowTokens.gridGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TaskRowTokens.gridGap),
        ) {
            TimeCell("时长", data.durationText, palette, Modifier.weight(1f), isLarge = true)
            TimeCell("重复", data.repeatText, palette, Modifier.weight(1f), isText = true)
        }
    }
}

@Composable
private fun TimeCell(
    label: String,
    value: String,
    palette: RowPalette,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    isText: Boolean = false,
) {
    Column(
        modifier = modifier
            .background(palette.bg1)
            .padding(TaskRowTokens.gridCellPadding),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            color = palette.t3,
        )
        Text(
            text = value,
            fontSize = if (isLarge) 14.sp else 13.5.sp,
            fontWeight = if (isLarge) FontWeight.Bold else FontWeight.SemiBold,
            color = palette.t1,
            fontFamily = if (isText) FontFamily.Default else FontFamily.Monospace,
        )
    }
}

@Composable
private fun VolumeBars(percent: Int, palette: RowPalette) {
    val barCount = 7
    val filledBars = (percent * barCount / 100).coerceIn(0, barCount)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = "音量",
            fontSize = 11.sp,
            color = palette.t2,
        )
        Spacer(Modifier.width(5.dp))
        for (i in 0 until barCount) {
            val height = (4 + i * 1.5f).dp
            val isFilled = i < filledBars
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isFilled) palette.accent else palette.t3.copy(alpha = 0.4f)),
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = "$percent",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.t1,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun FadeTags(fadeIn: Int, fadeOut: Int, palette: RowPalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        FadeTag("渐入", fadeIn, palette)
        Text("·", fontSize = 11.sp, color = palette.t2)
        FadeTag("渐出", fadeOut, palette)
    }
}

@Composable
private fun FadeTag(label: String, seconds: Int, palette: RowPalette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = palette.t2,
        )
        Spacer(Modifier.width(3.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(palette.bg2)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(
                text = "${seconds}s",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = palette.t1,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
