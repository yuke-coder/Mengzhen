# -*- coding: utf-8 -*-
import sys

filepath = r"D:\Mengzhen\android\app\src\main\java\com\mengzhen\app\ui\screens\XimalyaAlarmScreens.kt"

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# === 1. Replace import section ===
old_imports = """import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView"""

new_imports = """import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView"""

if old_imports not in content:
    print("ERROR: Could not find old import section")
    sys.exit(1)
content = content.replace(old_imports, new_imports)
print("OK: imports replaced")

# === 2. Replace XimalyaAlarmManagerScreen function ===
func_start_marker = '/**\n * Direct UI/behavior port of Ximalaya 9.5.1.4:\n * AlarmManagerFragment + main_fra_alarm_manager.xml + AlarmNewAdapter.'
func_end_marker = 'private fun createAlarmDraft('

if func_start_marker not in content:
    print("ERROR: Could not find function start marker")
    sys.exit(1)
if func_end_marker not in content:
    print("ERROR: Could not find function end marker")
    sys.exit(1)

start_idx = content.index(func_start_marker)
end_idx = content.index(func_end_marker)

new_func = '''/**
 * Task list screen — Compose-native LazyColumn + TaskRowCard.
 * Replaces the legacy ListView + XimalyaAlarmListAdapter + main_alarm_item.xml.
 */
@Composable
fun XimalyaAlarmManagerScreen(
    navController: NavController,
    taskId: String = "",
    topLevel: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { TaskStore.get(context) }
    val scheduler = remember { AlarmScheduler.get(context) }
    val playbackState = remember(context) { PlaybackStateStore.get(context).snapshot }
        .collectAsState()
    val playback by remember {
        derivedStateOf { playbackState.value.forAlarmList() }
    }
    var storedTasks by remember { mutableStateOf(store.getAllTasks()) }
    val isDark = isSystemInDarkTheme()

    DisposableEffect(store) {
        val listener = store.registerTasksChangedListener { tasks ->
            storedTasks = tasks
        }
        onDispose { store.unregisterTasksChangedListener(listener) }
    }

    val filteredTasks by remember(storedTasks, playback) {
        derivedStateOf {
            storedTasks
                .filter { task ->
                    if (task.isQuickPlaybackSession()) {
                        task.hasActiveSchedule() ||
                            task.hasConfiguredStop() && taskRuntimeIsActive(task, playback)
                    } else {
                        true
                    }
                }
                .sortedWith(
                    compareByDescending<ScheduledTask>(ScheduledTask::hasActiveSchedule)
                        .thenBy { it.nextExecuteAt ?: Long.MAX_VALUE }
                        .thenByDescending(ScheduledTask::updatedAt)
                )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Title bar
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { viewContext ->
                val titleBar = LayoutInflater.from(viewContext)
                    .inflate(R.layout.ximalaya_alarm_title_bar, null, false)
                titleBar.setBackgroundResource(R.color.xm_alarm_v9514_0x7f060e75)
                installXimalyaTitleBar(
                    host = titleBar,
                    title = if (topLevel) "任务" else "定时启播",
                    left = if (topLevel) {
                        XimalyaTitleAction.None
                    } else {
                        XimalyaTitleAction.Back { navController.popBackStack() }
                    },
                    rightText = "帮助",
                    onRight = {
                        navController.navigate(Screen.AlarmHelp.route)
                    },
                )
                titleBar
            },
        )

        // Add button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showAlarmCreationModeDialog(context) { mode ->
                        if (mode == AlarmCreationMode.STOP_ONLY) {
                            val activeTaskId = AudioPlaybackService.getCurrentTaskId()
                            val activeTask = activeTaskId?.let(store::getTaskById)
                            if (
                                activeTask == null ||
                                !AudioPlaybackService.isCurrentlyPlaying()
                            ) {
                                AppNotice.warning(
                                    context,
                                    "当前没有实际播放的音频，请先选择音频并开始播放，再创建仅定时关闭任务",
                                )
                            } else {
                                navController.navigate(
                                    Screen.Templates.createRoute(
                                        activeTask.id,
                                        openTimer = true,
                                    )
                                )
                            }
                            return@showAlarmCreationModeDialog
                        }
                        if (requiresOverlayPermission(context)) {
                            showOverlayPermissionPrompt(context)
                            return@showAlarmCreationModeDialog
                        }
                        val sourceDraft = createAlarmDraft(store, taskId)
                        val combined = mode == AlarmCreationMode.START_AND_STOP
                        val draft = sourceDraft.copy(
                            scheduledStopMode = ScheduledStopMode.NONE,
                            scheduledStopValue = 0,
                            scheduledStopDurationSeconds = 0,
                            scheduledFinishCurrentTrack = false,
                            enableFadeOut = combined && sourceDraft.enableFadeOut,
                            fadeOutDuration = sourceDraft.fadeOutDuration,
                        )
                        store.saveTask(draft)
                        navController.navigate(
                            Screen.AlarmEditor.createRoute(
                                taskId = draft.id,
                                newAlarm = true,
                                openTimerAfterSave =
                                    mode == AlarmCreationMode.START_AND_STOP,
                            )
                        )
                    }
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "+",
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "添加定时任务",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Task list or empty state
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "当前没有定时任务",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = 16.dp,
                ),
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    val rowData = remember(task, playback) {
                        task.toRowData(playback)
                    }
                    TaskRowCard(
                        data = rowData,
                        isDark = isDark,
                        onClick = {
                            navController.navigate(
                                Screen.Templates.createRoute(task.id, openTimer = true)
                            )
                        },
                        onLongClick = {
                            showDeleteAlarmDialog(context) {
                                scheduler.deleteTask(task.id)
                            }
                        },
                        onToggle = { enabled ->
                            if (enabled && requiresOverlayPermission(context)) {
                                showOverlayPermissionPrompt(context)
                            } else if (enabled) {
                                val resumed = scheduler.resumeTask(task.id)
                                if (resumed != null) {
                                    AppNotice.success(
                                        context,
                                        "将会在" + alarmRelativeText(
                                            resumed.toXimalyaRepeatDays(),
                                            resumed.startTime.hour,
                                            resumed.startTime.minute,
                                        ),
                                    )
                                }
                            } else {
                                scheduler.disableSchedule(task.id)
                            }
                        },
                    )
                }

                // Footer tips
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable {
                                navController.navigate(
                                    Screen.PermissionSettings.createRoute(fromAlarm = true)
                                )
                            }
                            .padding(10.dp),
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "优化提示",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "为保证正常使用 请不要退出梦枕",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    ),
                                    maxLines = 1,
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = "为保证正常使用，请注意：",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "1.退出梦枕APP时，无法响铃\\n2.关机、打开手机静音开关、音量为0，处于勿扰模式、省电模式时，都将无法响铃\\n3.插入耳机时，闹钟仅能在耳机中播放\\n4.电话通话时，也将正常响铃\\n5.Android10 以上设备需要开启悬浮窗权限",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.4f
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

'''

content = content[:start_idx] + new_func + content[end_idx:]
print("OK: function replaced")

# === 3. Remove XimalyaAlarmListAdapter class ===
adapter_start = 'private class XimalyaAlarmListAdapter('
adapter_end = 'private fun taskRuntimeIsActive('

if adapter_start not in content:
    print("WARNING: Could not find adapter class start - already removed?")
elif adapter_end not in content:
    print("ERROR: Could not find adapter class end marker")
    sys.exit(1)
else:
    a_start = content.index(adapter_start)
    a_end = content.index(adapter_end)
    content = content[:a_start] + content[a_end:]
    print("OK: adapter class removed")

# === Write back ===
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("DONE: file written successfully")
