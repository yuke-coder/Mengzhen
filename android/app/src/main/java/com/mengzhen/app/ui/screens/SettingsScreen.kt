package com.mengzhen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.data.model.*
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val store = remember { TaskStore.get(context) }
    var tasks by remember { mutableStateOf(store.getAllTasks()) }
    var showTaskForm by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<ScheduledTask?>(null) }
    var draft by remember { mutableStateOf(store.getDraft()) }
    var playMode by remember { mutableStateOf(store.getPlayMode()) }

    // 刷新任务列表
    fun refreshTasks() {
        tasks = store.getAllTasks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("梦枕", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text("自定义定时 · 自动助眠播放",
                            fontSize = 12.sp, color = MutedForeground)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "个人")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 模式切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = playMode == PlayMode.DEFAULT,
                    onClick = {
                        playMode = PlayMode.DEFAULT
                        store.setPlayMode(playMode)
                    },
                    label = { Text("默认模式") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary
                    )
                )
                Spacer(Modifier.width(12.dp))
                FilterChip(
                    selected = playMode == PlayMode.CUSTOM,
                    onClick = {
                        playMode = PlayMode.CUSTOM
                        store.setPlayMode(playMode)
                    },
                    label = { Text("自定义模式") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary
                    )
                )
            }

            // 播放设置面板
            PlaybackSettingsCard(draft = draft, onDraftChange = {
                draft = it
                store.saveDraft(it)
            })

            // 后台播放优化入口
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { navController.navigate("bg_optimization") },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null,
                        tint = Primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("后台播放优化",
                            fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("确保息屏播放不被系统杀死",
                            fontSize = 12.sp, color = MutedForeground)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MutedForeground)
                }
            }

            // 任务列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("定时任务", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        FilledIconButton(
                            onClick = {
                                editingTask = null
                                showTaskForm = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加任务")
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Alarm, contentDescription = null,
                                    modifier = Modifier.size(48.dp), tint = MutedForeground)
                                Spacer(Modifier.height(12.dp))
                                Text("还没有定时任务", color = MutedForeground)
                                Text("点击 + 创建第一个任务",
                                    fontSize = 12.sp, color = MutedForeground)
                            }
                        }
                    }
                } else {
                    items(tasks) { task ->
                        TaskCard(task = task,
                            onEdit = {
                                editingTask = task
                                showTaskForm = true
                            },
                            onDelete = {
                                store.deleteTask(task.id)
                                refreshTasks()
                            },
                            onToggle = {
                                val newStatus = if (task.status == TaskStatus.CANCELLED)
                                    TaskStatus.PENDING else TaskStatus.CANCELLED
                                store.updateTask(task.id, mapOf("status" to newStatus))
                                refreshTasks()
                            }
                        )
                    }
                }
            }
        }
    }

    // 任务编辑弹窗
    if (showTaskForm) {
        TaskFormDialog(
            task = editingTask,
            draft = draft,
            onDismiss = { showTaskForm = false },
            onSave = { task ->
                if (editingTask == null) {
                    val newTask = task.copy(
                        id = store.generateTaskId(),
                        createdAt = System.currentTimeMillis()
                    )
                    store.createTask(newTask)
                } else {
                    store.updateTask(editingTask!!.id, mapOf(
                        "name" to task.name,
                        "playDurationMinutes" to task.playDurationMinutes,
                        "volume" to task.volume,
                        "enableFade" to task.enableFade,
                        "fadeInDuration" to task.fadeInDuration,
                        "fadeOutDuration" to task.fadeOutDuration,
                    ))
                }
                refreshTasks()
                showTaskForm = false
            }
        )
    }
}

@Composable
private fun PlaybackSettingsCard(
    draft: PlaybackDraft,
    onDraftChange: (PlaybackDraft) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("播放设置", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            // 音量
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null,
                    tint = MutedForeground, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("音量", fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("${draft.volume}%", fontSize = 14.sp, color = Primary)
            }
            Slider(
                value = draft.volume.toFloat(),
                onValueChange = { onDraftChange(draft.copy(volume = it.toInt())) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Primary,
                    activeTrackColor = Primary
                )
            )

            // 渐入渐出
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    onDraftChange(draft.copy(enableFade = !draft.enableFade))
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null,
                    tint = MutedForeground, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("音量渐入渐出", fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = draft.enableFade,
                    onCheckedChange = { onDraftChange(draft.copy(enableFade = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }

            if (draft.enableFade) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 渐入
                    Column(modifier = Modifier.weight(1f)) {
                        Text("渐入(秒)", fontSize = 12.sp, color = MutedForeground)
                        OutlinedTextField(
                            value = draft.fadeInDuration.toString(),
                            onValueChange = { v ->
                                v.toIntOrNull()?.let {
                                    onDraftChange(draft.copy(fadeInDuration = it))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    // 渐出
                    Column(modifier = Modifier.weight(1f)) {
                        Text("渐出(秒)", fontSize = 12.sp, color = MutedForeground)
                        OutlinedTextField(
                            value = draft.fadeOutDuration.toString(),
                            onValueChange = { v ->
                                v.toIntOrNull()?.let {
                                    onDraftChange(draft.copy(fadeOutDuration = it))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: ScheduledTask,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val statusColor = when (task.status) {
        TaskStatus.PENDING -> Primary
        TaskStatus.EXECUTING -> Color(0xFFFF9800)
        TaskStatus.COMPLETED -> MutedForeground
        TaskStatus.CANCELLED -> Color(0xFFEF4444)
    }

    val statusText = when (task.status) {
        TaskStatus.PENDING -> "待执行"
        TaskStatus.EXECUTING -> "执行中"
        TaskStatus.COMPLETED -> "已完成"
        TaskStatus.CANCELLED -> "已取消"
    }

    val repeatText = when (task.repeatType) {
        TaskRepeatType.ONCE -> "一次性"
        TaskRepeatType.WORKDAY -> "工作日"
        TaskRepeatType.HOLIDAY -> "节假日"
        TaskRepeatType.DAILY -> "每天"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(task.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(statusText, fontSize = 11.sp, color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null,
                    tint = MutedForeground, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${task.startTime.hour}:${String.format("%02d", task.startTime.minute)}",
                    fontSize = 13.sp, color = MutedForeground)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Repeat, contentDescription = null,
                    tint = MutedForeground, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(repeatText, fontSize = 13.sp, color = MutedForeground)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Timer, contentDescription = null,
                    tint = MutedForeground, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${task.playDurationMinutes}分钟", fontSize = 13.sp, color = MutedForeground)
            }
            if (task.audios.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("音频: ${task.audios.joinToString("、") { it.name }}",
                    fontSize = 12.sp, color = MutedForeground, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onToggle) {
                    Text(if (task.status == TaskStatus.CANCELLED) "启用" else "停用",
                        color = Primary)
                }
                TextButton(onClick = onDelete) {
                    Text("删除", color = Destructive)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFormDialog(
    task: ScheduledTask?,
    draft: PlaybackDraft,
    onDismiss: () -> Unit,
    onSave: (ScheduledTask) -> Unit
) {
    var name by remember { mutableStateOf(task?.name ?: "") }
    var hour by remember { mutableStateOf(task?.startTime?.hour?.toString() ?: "8") }
    var minute by remember { mutableStateOf(task?.startTime?.minute?.toString() ?: "0") }
    var duration by remember { mutableStateOf(task?.playDurationMinutes?.toString() ?: "30") }
    var volume by remember { mutableStateOf(task?.volume ?: draft.volume) }
    var enableFade by remember { mutableStateOf(task?.enableFade ?: draft.enableFade) }
    var fadeIn by remember { mutableStateOf(task?.fadeInDuration?.toString() ?: draft.fadeInDuration.toString()) }
    var fadeOut by remember { mutableStateOf(task?.fadeOutDuration?.toString() ?: draft.fadeOutDuration.toString()) }
    var repeatType by remember {
        mutableStateOf(task?.repeatType ?: TaskRepeatType.ONCE)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "新建任务" else "编辑任务") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("任务名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                // 时间选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("时") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it.filter { c -> c.isDigit() } },
                        label = { Text("播放(分)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))

                // 重复类型
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TaskRepeatType.entries.forEach { type ->
                        FilterChip(
                            selected = repeatType == type,
                            onClick = { repeatType = type },
                            label = {
                                Text(when (type) {
                                    TaskRepeatType.ONCE -> "一次"
                                    TaskRepeatType.WORKDAY -> "工作日"
                                    TaskRepeatType.HOLIDAY -> "节假日"
                                    TaskRepeatType.DAILY -> "每天"
                                }, fontSize = 11.sp)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // 音量
                Text("音量: $volume%", fontSize = 13.sp)
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { volume = it.toInt() },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                )

                // 渐入渐出
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        enableFade = !enableFade
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("音量渐入渐出", fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = enableFade,
                        onCheckedChange = { enableFade = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary))
                }

                if (enableFade) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fadeIn,
                            onValueChange = { fadeIn = it.filter { c -> c.isDigit() } },
                            label = { Text("渐入(秒)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fadeOut,
                            onValueChange = { fadeOut = it.filter { c -> c.isDigit() } },
                            label = { Text("渐出(秒)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val now = java.util.Calendar.getInstance()
                val taskData = ScheduledTask(
                    id = task?.id ?: "",
                    name = name.ifEmpty { "未命名任务" },
                    startTime = TaskStartTime(
                        year = now.get(java.util.Calendar.YEAR),
                        month = now.get(java.util.Calendar.MONTH) + 1,
                        day = now.get(java.util.Calendar.DAY_OF_MONTH),
                        hour = hour.toIntOrNull() ?: 8,
                        minute = minute.toIntOrNull() ?: 0,
                    ),
                    playDurationMinutes = duration.toIntOrNull() ?: 30,
                    volume = volume,
                    enableFade = enableFade,
                    fadeInDuration = fadeIn.toIntOrNull() ?: 0,
                    fadeOutDuration = fadeOut.toIntOrNull() ?: 0,
                    repeatType = repeatType,
                    audios = task?.audios ?: draft.audios,
                    status = task?.status ?: TaskStatus.PENDING,
                    createdAt = task?.createdAt ?: System.currentTimeMillis(),
                )
                onSave(taskData)
            }) { Text("保存", color = Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
