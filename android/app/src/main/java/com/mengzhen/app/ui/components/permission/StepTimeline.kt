package com.mengzhen.app.ui.components.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mengzhen.app.data.tutorial.TutorialCard
import com.mengzhen.app.data.tutorial.TutorialStep
import com.mengzhen.app.ui.theme.BrandEnd
import com.mengzhen.app.ui.theme.BrandStart

/** 步骤勾选态 key 工具 */
fun stepStateKey(cardId: String, stepIndex: Int) = "$cardId:$stepIndex"

private val CARD_LABELS = listOf("A", "B", "C", "D", "E", "F")

/**
 * StepTimeline 步骤时间线（设计 §4.4：小卡模式 + 序号圆兼作勾选框）
 *
 * @param checkedStates 勾选状态表，key = "$cardId:$stepIndex"
 * @param nextUncheckedKey 首个未勾步 key（高亮显示，Primary 16%/20% 底）
 * @param onStepToggle 点击序号圆切换勾选（外部负责持久化）
 */
@Composable
fun StepTimeline(
    cards: List<TutorialCard>,
    checkedStates: Map<String, Boolean>,
    nextUncheckedKey: String?,
    onStepToggle: (cardId: String, stepIndex: Int, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onHighlightPositioned: ((Float) -> Unit)? = null,
) {
    val multiCard = cards.size > 1
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        cards.forEachIndexed { cardIndex, card ->
            val cardDone = card.steps.all { checkedStates[stepStateKey(card.id, it.index)] == true }
            if (multiCard) {
                // 多卡教程：卡容器 + 卡标题行
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CardIndexChip("小卡 ${CARD_LABELS.getOrElse(cardIndex) { "${cardIndex + 1}" }}")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            card.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (cardDone) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "本卡已完成",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    StepList(card, checkedStates, nextUncheckedKey, onStepToggle, onHighlightPositioned)
                }
            } else {
                // 单卡教程：省略卡容器直接平铺
                StepList(card, checkedStates, nextUncheckedKey, onStepToggle, onHighlightPositioned)
            }
        }
    }
}

/** 卡序号 Chip（Primary 12% 底） */
@Composable
private fun CardIndexChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun StepList(
    card: TutorialCard,
    checkedStates: Map<String, Boolean>,
    nextUncheckedKey: String?,
    onStepToggle: (cardId: String, stepIndex: Int, checked: Boolean) -> Unit,
    onHighlightPositioned: ((Float) -> Unit)?,
) {
    Column {
        card.steps.forEachIndexed { i, step ->
            val key = stepStateKey(card.id, step.index)
            StepRow(
                step = step,
                checked = checkedStates[key] == true,
                highlighted = key == nextUncheckedKey,
                isLast = i == card.steps.lastIndex,
                onToggle = { onStepToggle(card.id, step.index, checkedStates[key] != true) },
                onHighlightPositioned = onHighlightPositioned,
            )
        }
    }
}

@Composable
private fun StepRow(
    step: TutorialStep,
    checked: Boolean,
    highlighted: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    onHighlightPositioned: ((Float) -> Unit)?,
) {
    val dark = isSystemInDarkTheme()
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // 序号列：圆 + 连接线
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight().width(48.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "步骤 ${step.index}，${if (checked) "已完成" else "未完成"}，双击切换"
                    }
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (checked) Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
                            else Brush.linearGradient(listOf(BrandStart, BrandEnd))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text(
                            "${step.index}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(BrandStart.copy(alpha = 0.3f))
                )
            }
        }

        // 内容列
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, bottom = if (isLast) 0.dp else 20.dp),
        ) {
            Box(
                modifier = (if (highlighted) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.20f else 0.16f))
                        .padding(6.dp)
                } else {
                    Modifier.padding(6.dp)
                }).then(
                    if (highlighted && onHighlightPositioned != null) {
                        Modifier.onGloballyPositioned { onHighlightPositioned(it.boundsInRoot().top) }
                    } else Modifier
                )
            ) {
                Text(
                    step.text,
                    fontSize = 16.sp,
                    lineHeight = 27.2.sp, // 1.7 行高
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = if (checked) 0.5f else 0.9f
                    ),
                )
            }

            // 并列子项 bullet（允许自启动 / 允许关联启动 / 允许后台活动）
            step.subItems?.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item,
                        fontSize = 16.sp,
                        lineHeight = 27.2.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (checked) 0.5f else 0.9f
                        ),
                    )
                }
            }

            // StepTip 步骤级提示（正文下方、PathChip 上方）
            step.tip?.let { tip ->
                Spacer(Modifier.height(6.dp))
                StepTip(tip)
            }

            // PathChip 路径行
            step.pathSegments?.let { segments ->
                Spacer(Modifier.height(8.dp))
                PathChipRow(segments, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

/** StepTip 步骤级提示（§4.5：Warning 14dp 琥珀 + 13sp 琥珀文字，无前缀） */
@Composable
fun StepTip(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(start = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = AmberWarning,
            modifier = Modifier.size(14.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            fontSize = 13.sp,
            lineHeight = 19.5.sp, // 1.5 行高
            color = AmberWarning,
        )
    }
}

/** 多卡进度指示 Chip（"操作步骤 [卡A✓][卡B][卡C]"，教程页标题区用） */
@Composable
fun CardProgressRow(
    cards: List<TutorialCard>,
    checkedStates: Map<String, Boolean>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        cards.forEachIndexed { index, card ->
            val done = card.steps.all { checkedStates[stepStateKey(card.id, it.index)] == true }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "卡${CARD_LABELS.getOrElse(index) { "${index + 1}" }}${if (done) " ✓" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (done) Color.White else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
