package com.mengzhen.app.ui.components.permission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mengzhen.app.data.tutorial.PermissionGroup
import com.mengzhen.app.data.tutorial.PermissionKey

/** 权限项展示状态 */
data class PermissionItemState(
    val key: PermissionKey,
    val done: Boolean,
    /** 同入口源项标题（如"后台运行策略"/"联网控制"）：非 null 表示已随源项自动完成，整行置灰 + 副标题说明 */
    val autoCompletedByTitle: String? = null,
    /** 系统检测不可用（如新版 HarmonyOS 缺 wifi_sleep_policy key）：未完成时显示"未确认"，不标红不拦截 */
    val undetectable: Boolean = false,
)

/**
 * PermissionGroupSection 分组折叠组件（设计 §4.1）
 * - 必须完成组：常驻展开，无折叠交互
 * - 遇到问题再设置组：默认折叠，chevron 旋转 180° / 200ms
 */
@Composable
fun PermissionGroupSection(
    group: PermissionGroup,
    items: List<PermissionItemState>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onItemClick: (PermissionKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collapsible = group == PermissionGroup.ON_DEMAND
    val effectiveExpanded = if (collapsible) expanded else true

    Column(modifier = modifier.fillMaxWidth()) {
        // 组头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (collapsible) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .semantics {
                                contentDescription = "${group.title}，${if (expanded) "已展开" else "已折叠"}，双击切换"
                            }
                            .clickable(onClick = onToggleExpand)
                            .padding(vertical = 8.dp)
                    } else {
                        Modifier.padding(vertical = 8.dp)
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                group.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${items.size} 项",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (collapsible) {
                Spacer(Modifier.weight(1f))
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = tween(200),
                    label = "chevron",
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation),
                )
            }
        }

        // 组首副标题（仅遇到问题再设置组）
        if (collapsible) {
            Text(
                "播放正常就不用管下面这些",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
        }

        // 权限项列表
        if (collapsible) {
            AnimatedVisibility(
                visible = effectiveExpanded,
                enter = expandVertically(tween(200)),
                exit = shrinkVertically(tween(200)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { PermissionRow(it, onItemClick) }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { PermissionRow(it, onItemClick) }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    item: PermissionItemState,
    onItemClick: (PermissionKey) -> Unit,
) {
    val autoCompleted = item.autoCompletedByTitle != null
    val alpha = if (autoCompleted) 0.5f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (autoCompleted) Modifier
                else Modifier.clickable { onItemClick(item.key) }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.key.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
                if (item.key.group == PermissionGroup.REQUIRED) {
                    Spacer(Modifier.width(6.dp))
                    RequiredBadge()
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                when {
                    autoCompleted -> "已随「${item.autoCompletedByTitle}」完成"
                    item.undetectable && !item.done ->
                        item.key.subtitle + "（无法自动检测，设置后勾选）"
                    else -> item.key.subtitle
                },
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        Spacer(Modifier.width(12.dp))
        // 右侧状态
        when {
            autoCompleted || item.done -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (autoCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (autoCompleted) "已完成" else "已开启",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item.undetectable -> {
                // 检测不可用：中性"未确认"，不标红不拦截，点按仍进教程页
                Text(
                    "未确认",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Text(
                    if (item.key == PermissionKey.BATTERY_OPTIMIZATION) "一键设置" else "查看教程",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
