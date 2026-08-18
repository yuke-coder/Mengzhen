package com.mengzhen.app.ui.screens

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.ContextThemeWrapper
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.ximalaya.ting.android.main.view.NumberPickerView
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.audio.PlaybackSnapshot
import com.mengzhen.app.audio.PlaybackStateStore
import com.mengzhen.app.audio.PlaybackTransportState
import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.ScheduledStopMode
import com.mengzhen.app.data.model.TaskPhase
import com.mengzhen.app.data.model.TaskRepeatType
import com.mengzhen.app.data.model.TaskStartTime
import com.mengzhen.app.data.model.TaskStatus
import com.mengzhen.app.data.model.effectiveScheduledStopDurationSeconds
import com.mengzhen.app.data.model.hasActiveSchedule
import com.mengzhen.app.data.model.hasConfiguredStop
import com.mengzhen.app.data.model.isOneShotSchedule
import com.mengzhen.app.data.model.selectionKey
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.data.tutorial.PermissionKey
import com.mengzhen.app.scheduler.AlarmScheduler
import com.mengzhen.app.scheduler.isQuickPlaybackSession
import com.mengzhen.app.scheduler.TaskScheduleCalculator
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import java.util.Calendar
import java.util.Locale

private const val LIGHT_WAKEUP_DESCRIPTION = "所选音频开始播放时音量渐强"

private enum class AlarmCreationMode {
    START_AND_STOP,
    START_ONLY,
    STOP_ONLY,
}

private enum class LightWakeupDurationUnit(
    val label: String,
    val secondsPerUnit: Int,
    val defaultMaximum: Int,
) {
    SECONDS("秒", 1, 120),
    MINUTES("分", 60, 30),
    ;

    fun toSeconds(value: Int): Int? {
        if (value < 0 || value > Int.MAX_VALUE / secondsPerUnit) return null
        return value * secondsPerUnit
    }

    fun fromSeconds(seconds: Int): Int = seconds.coerceAtLeast(0) / secondsPerUnit
}

/**
 * Direct UI/behavior port of Ximalaya 9.5.1.4:
 * AlarmManagerFragment + main_fra_alarm_manager.xml + AlarmNewAdapter.
 *
 * Only persistence, playback and permission destinations are adapted to Mengzhen
 * services. Layout IDs, static copy and interaction order remain source-compatible.
 */
@Composable
fun XimalayaAlarmManagerScreen(
    navController: NavController,
    taskId: String = "",
    topLevel: Boolean = false,
) {
    val context = LocalContext.current
    val store = remember { TaskStore.get(context) }
    val scheduler = remember { AlarmScheduler.get(context) }
    val playbackState = remember(context) { PlaybackStateStore.get(context).snapshot }
        .collectAsState()
    val playback by remember { derivedStateOf { playbackState.value.forAlarmList() } }
    var storedTasks by remember { mutableStateOf(store.getAllTasks()) }

    DisposableEffect(store) {
        val listener = store.registerTasksChangedListener { tasks -> storedTasks = tasks }
        onDispose { store.unregisterTasksChangedListener(listener) }
    }

    val alarms by remember(storedTasks, playback) {
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
                        .thenByDescending(ScheduledTask::updatedAt),
                )
        }
    }

    val onAdd: () -> Unit = {
        showAlarmCreationModeDialog(context) { mode ->
            if (mode == AlarmCreationMode.STOP_ONLY) {
                val activeTaskId = AudioPlaybackService.getCurrentTaskId()
                val activeTask = activeTaskId?.let(store::getTaskById)
                if (activeTask == null || !AudioPlaybackService.isCurrentlyPlaying()) {
                    AppNotice.warning(
                        context,
                        "当前没有实际播放的音频，请先选择音频并开始播放，再创建仅定时关闭任务",
                    )
                } else {
                    navController.navigate(
                        Screen.Templates.createRoute(activeTask.id, openTimer = true),
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
                    openTimerAfterSave = mode == AlarmCreationMode.START_AND_STOP,
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { viewContext ->
                val root = LayoutInflater.from(viewContext)
                    .inflate(R.layout.main_fra_alarm_manager, null, false)
                val titleBar = root.findViewById<View>(R.id.main_title_bar)
                titleBar.findViewById<TextView>(R.id.ximalaya_title_text).text =
                    if (topLevel) "任务" else "定时启播"
                titleBar.findViewById<View>(R.id.ximalaya_title_back).apply {
                    visibility = if (topLevel) View.GONE else View.VISIBLE
                    setOnClickListener { navController.popBackStack() }
                }
                titleBar.findViewById<TextView>(R.id.ximalaya_title_right).apply {
                    text = "帮助"
                    visibility = View.VISIBLE
                    setOnClickListener { navController.navigate(Screen.AlarmHelp.route) }
                }
                root.findViewById<View>(R.id.main_alarm_rl_add_alarm)
                    .setOnClickListener { onAdd() }
                root
            },
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (alarms.isEmpty()) {
                Text(
                    text = "当前没有定时任务",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(alarms, key = { it.id }) { task ->
                        TaskRowCard(
                            data = task.toRowData(playback),
                            isDark = isSystemInDarkTheme(),
                            onClick = {
                                navController.navigate(
                                    Screen.Templates.createRoute(task.id, openTimer = true),
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
                                            "将会在${
                                                alarmRelativeText(
                                                    resumed.toXimalayaRepeatDays(),
                                                    resumed.startTime.hour,
                                                    resumed.startTime.minute,
                                                )
                                            }",
                                        )
                                    }
                                } else {
                                    scheduler.disableSchedule(task.id)
                                }
                            },
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    navController.navigate(
                                        Screen.PermissionSettings.createRoute(fromAlarm = true),
                                    )
                                }
                                .padding(12.dp),
                        ) {
                            Text(
                                "优化提示",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "为保证正常使用 请不要退出梦枕",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "为保证正常使用，请注意：",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "1.退出梦枕APP时，无法响铃\n2.关机、打开手机静音开关、音量为0，处于勿扰模式、省电模式时，都将无法响铃\n3.插入耳机时，闹钟仅能在耳机中播放\n4.电话通话时，也将正常响铃\n5.Android10 以上设备需要开启悬浮窗权限",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

        }
    }
}

private fun createAlarmDraft(store: TaskStore, sourceTaskId: String): ScheduledTask {
    val source = store.getTaskById(sourceTaskId)
        ?: store.getAllTasks()
            .asSequence()
            .filterNot(ScheduledTask::isQuickPlaybackSession)
            .filter { it.audios.isNotEmpty() }
            .maxByOrNull(ScheduledTask::updatedAt)
    val now = System.currentTimeMillis()
    val nextMinute = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.MINUTE, 1)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val initialStartTime = nextAlarmStartTime(
        hour = nextMinute.get(Calendar.HOUR_OF_DAY),
        minute = nextMinute.get(Calendar.MINUTE),
        days = 0,
        now = now,
    )
    if (source != null) {
        return source.copy(
            id = store.generateTaskId(),
            startTime = initialStartTime,
            scheduleArmed = false,
            status = TaskStatus.PENDING,
            lastExecutedAt = null,
            nextExecuteAt = null,
            completedAt = null,
            skipUntil = null,
            executionStartedAt = null,
            executionEndsAt = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    val playbackDraft = store.getDraft()
    return ScheduledTask(
        id = store.generateTaskId(),
        name = "本次助眠",
        startTime = initialStartTime,
        fadeInDuration = playbackDraft.fadeInDuration,
        fadeOutDuration = playbackDraft.fadeOutDuration,
        enableFade = playbackDraft.enableFade,
        enableFadeOut = playbackDraft.enableFadeOut,
        volume = playbackDraft.volume,
        audios = playbackDraft.audios,
        scheduleArmed = false,
        status = TaskStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )
}

/**
 * Direct UI/behavior port of AddOrEditAlarmFragment.
 */
@Composable
fun XimalayaAlarmEditorScreen(
    navController: NavController,
    taskId: String,
    discardOnCancel: Boolean = false,
    openTimerAfterSave: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { TaskStore.get(context) }
    val scheduler = remember { AlarmScheduler.get(context) }
    val original = remember(taskId) { store.getTaskById(taskId) }
    var repeatDays by remember(taskId) {
        mutableIntStateOf(original?.toXimalayaRepeatDays() ?: 0)
    }
    var fadeEnabled by remember(taskId) {
        mutableStateOf(original?.enableFade == true)
    }
    var fadeInSeconds by remember(taskId) {
        mutableIntStateOf(
            original?.fadeInDuration?.takeIf { it > 0 } ?: 0
        )
    }
    val cancelEditor = {
        if (discardOnCancel) store.deleteTask(taskId)
        navController.popBackStack()
        Unit
    }
    BackHandler(onBack = cancelEditor)
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        factory = { viewContext ->
            val sourceMaterialContext = ContextThemeWrapper(
                viewContext,
                com.google.android.material.R.style.Theme_MaterialComponents_DayNight,
            )
            LayoutInflater.from(sourceMaterialContext)
                .inflate(R.layout.main_fra_alarm_edit_or_add, null, false)
                .also { root ->
                    root.setBackgroundResource(R.color.xm_alarm_v9514_0x7f060e75)
                    val sourceTask = original
                    if (sourceTask == null) {
                        installXimalayaTitleBar(
                            root.findViewById(R.id.main_title_bar),
                            "添加定时任务",
                            XimalayaTitleAction.Cancel(cancelEditor),
                            "完成",
                            onRight = { navController.popBackStack() },
                        )
                        return@also
                    }

                    val editing = !discardOnCancel
                    val hourPicker = root.findViewById<NumberPickerView>(R.id.hour_number_picker)
                    val minutePicker = root.findViewById<NumberPickerView>(R.id.minute_number_picker)
                    val wakeup = root.findViewById<TextView>(R.id.main_alarm_time_to_wakeup)
                    val repeatText =
                        root.findViewById<TextView>(R.id.main_alarm_tv_repeat_content)
                    val ringText =
                        root.findViewById<TextView>(R.id.main_alarm_tv_ring_name_content)

                    var hour = sourceTask.startTime.hour.coerceIn(0, 23)
                    var minute = sourceTask.startTime.minute.coerceIn(0, 59)

                    fun updateWakeupText() {
                        wakeup.text = alarmRelativeText(repeatDays, hour, minute)
                        repeatText.text = alarmRepeatLabel(repeatDays)
                    }

                    installXimalayaTitleBar(
                        host = root.findViewById(R.id.main_title_bar),
                        title = if (editing) "编辑定时任务" else "添加定时任务",
                        left = XimalayaTitleAction.Cancel(cancelEditor),
                        rightText = "完成",
                        onRight = {
                            val now = System.currentTimeMillis()
                            hour = hourPicker.value.coerceIn(0, 23)
                            minute = minutePicker.value.coerceIn(0, 59)
                            if (!isBatteryOptimizationAllowed(viewContext)) {
                                showBackgroundOptimizationPrompt(
                                    context = viewContext,
                                    onSettings = {
                                        navController.navigate(
                                            Screen.PermissionSettings.createRoute(
                                                fromAlarm = true
                                            )
                                        )
                                    },
                                )
                                return@installXimalayaTitleBar
                            }

                            val startTime = nextAlarmStartTime(hour, minute, repeatDays, now)
                            val mappedType = repeatDays.toTaskRepeatType()
                            val latestTask = store.getTaskById(taskId) ?: sourceTask
                            val saved = scheduler.saveAndSchedule(
                                latestTask.copy(
                                    startTime = startTime,
                                    repeatType = mappedType,
                                    repeatDays = repeatDays.coerceIn(0, 127),
                                    enableFade = fadeEnabled,
                                    fadeInDuration = fadeInSeconds.coerceAtLeast(0),
                                    enableFadeOut = if (openTimerAfterSave && !fadeEnabled) {
                                        false
                                    } else {
                                        latestTask.enableFadeOut
                                    },
                                    fadeOutDuration = latestTask.fadeOutDuration,
                                    scheduleArmed = true,
                                    status = TaskStatus.PENDING,
                                    nextExecuteAt = null,
                                    completedAt = null,
                                    executionStartedAt = null,
                                    executionEndsAt = null,
                                )
                            )
                            AppNotice.success(
                                viewContext,
                                "将会在${alarmRelativeText(saved.toXimalayaRepeatDays(), hour, minute)}",
                            )
                            navController.popBackStack()
                            if (openTimerAfterSave) {
                                navController.navigate(
                                    Screen.Templates.createRoute(
                                        saved.id,
                                        openTimer = true,
                                        timerMode = Screen.Templates.TIMER_MODE_COMBINED,
                                    )
                                )
                            }
                        },
                    )

                    configurePicker(hourPicker, 0, 23, hour) { value ->
                        hour = value
                        updateWakeupText()
                    }
                    configurePicker(minutePicker, 0, 59, minute) { value ->
                        minute = value
                        updateWakeupText()
                    }
                    ringText.text = sourceAlarmRingName(
                        store.getTaskById(taskId) ?: sourceTask,
                    )
                    root.findViewById<View>(R.id.main_alarm_repeat_ll).setOnClickListener {
                        showAlarmRepeatTypeDialog(viewContext, repeatDays) { selected ->
                            repeatDays = selected
                            updateWakeupText()
                        }
                    }
                    root.findViewById<View>(R.id.main_alarm_ring_name_ll).setOnClickListener {
                        navController.navigate(Screen.AlarmRing.createRoute(taskId))
                    }
                    val fadeRow = root.findViewById<View>(R.id.main_alarm_music_ll)
                    val fadeSwitch = root.findViewById<CheckBox>(R.id.main_music_switch)
                    val fadeDurationPanel = createLightWakeupDurationPanel(
                        context = viewContext,
                        fadeInSeconds = fadeInSeconds,
                        onFadeInChanged = { fadeInSeconds = it },
                    )
                    (fadeRow.parent as? ViewGroup)?.let { parent ->
                        parent.addView(fadeDurationPanel, parent.indexOfChild(fadeRow) + 1)
                    }
                    root.findViewById<TextView>(R.id.main_alarm_tv_music_title).text =
                        "轻唤醒"
                    root.findViewById<TextView>(R.id.main_alarm_tv_music_desc).text =
                        if (openTimerAfterSave) {
                            "关闭后同时关闭音量渐强与渐弱"
                        } else {
                            LIGHT_WAKEUP_DESCRIPTION
                        }
                    fadeRow.visibility = View.VISIBLE
                    fadeSwitch.isChecked = fadeEnabled
                    fadeSwitch.contentDescription = "轻唤醒"
                    fadeSwitch.setOnCheckedChangeListener { _, checked ->
                        fadeEnabled = checked
                        fadeDurationPanel.visibility =
                            if (checked) View.VISIBLE else View.GONE
                    }
                    fadeRow.setOnClickListener {
                        fadeSwitch.isChecked = !fadeSwitch.isChecked
                    }
                    root.findViewById<View>(R.id.main_tips_bg).setOnClickListener {
                        navController.navigate(
                            Screen.PermissionSettings.createRoute(fromAlarm = true)
                        )
                    }
                    updateWakeupText()
                }
        },
        update = {
            it.findViewById<TextView?>(R.id.main_alarm_tv_repeat_content)?.text =
                alarmRepeatLabel(repeatDays)
            store.getTaskById(taskId)?.let { current ->
                it.findViewById<TextView?>(R.id.main_alarm_tv_ring_name_content)?.text =
                    sourceAlarmRingName(current)
            }
            it.findViewById<View?>(R.id.main_alarm_music_ll)?.visibility = View.VISIBLE
            it.findViewById<CheckBox?>(R.id.main_music_switch)?.let { fadeSwitch ->
                if (fadeSwitch.isChecked != fadeEnabled) fadeSwitch.isChecked = fadeEnabled
            }
            it.findViewById<TextView?>(R.id.main_alarm_tv_music_desc)?.text =
                if (openTimerAfterSave) {
                    "关闭后同时关闭音量渐强与渐弱"
                } else {
                    LIGHT_WAKEUP_DESCRIPTION
                }
            bindLightWakeupDurationPanel(
                root = it,
                enabled = fadeEnabled,
                fadeInSeconds = fadeInSeconds,
            )
        },
    )
}

private fun createLightWakeupDurationPanel(
    context: Context,
    fadeInSeconds: Int,
    onFadeInChanged: (Int) -> Unit,
): LinearLayout = (LayoutInflater.from(context).inflate(
    R.layout.main_alarm_light_wakeup_duration_panel,
    null,
    false,
) as LinearLayout).apply {
    addView(
        createSourceFadeDurationRow(
            context = context,
            label = "音量渐强",
            value = fadeInSeconds,
            seekId = R.id.main_alarm_fade_in_seek,
            valueId = R.id.main_alarm_fade_in_value,
            unitId = R.id.main_alarm_fade_in_unit,
            onChanged = onFadeInChanged,
        ),
    )
}

internal fun createSourceFadeDurationRow(
    context: Context,
    label: String,
    showLabel: Boolean = true,
    value: Int,
    seekId: Int,
    valueId: Int,
    unitId: Int,
    onChanged: (Int) -> Unit,
    sourceRow: RelativeLayout? = null,
): RelativeLayout {
    val row = sourceRow ?: LayoutInflater.from(context).inflate(
        R.layout.main_alarm_fade_duration_row,
        null,
        false,
    ) as RelativeLayout
    val labelText = row.findViewById<TextView>(R.id.main_alarm_fade_label)
    val displayValue = row.findViewById<TextView>(R.id.main_alarm_fade_value)
    val unitText = row.findViewById<TextView>(R.id.main_alarm_fade_unit)
    val seek = row.findViewById<SeekBar>(R.id.main_alarm_fade_seek)
    if (sourceRow == null) row.id = View.generateViewId()

    labelText.apply {
        id = View.generateViewId()
        text = label
        visibility = if (showLabel) View.VISIBLE else View.GONE
        layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
            marginStart = context.dp(16)
            topMargin = context.dp(8)
        }
    }

    fun valueLayoutParams() = RelativeLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        if (showLabel) {
            addRule(RelativeLayout.END_OF, labelText.id)
            addRule(RelativeLayout.ALIGN_TOP, labelText.id)
            marginStart = context.dp(8)
        } else {
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
            marginStart = context.dp(16)
            topMargin = context.dp(8)
        }
    }

    displayValue.apply {
        id = valueId
        text = LightWakeupDurationUnit.SECONDS.fromSeconds(value).toString()
        isClickable = true
        isFocusable = true
        contentDescription = "编辑${label}时长"
        layoutParams = valueLayoutParams()
    }
    unitText.apply {
        id = unitId
        tag = LightWakeupDurationUnit.SECONDS
        text = LightWakeupDurationUnit.SECONDS.label
        isClickable = true
        isFocusable = true
        contentDescription = "切换${label}计量单位"
        layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            addRule(RelativeLayout.END_OF, valueId)
            addRule(RelativeLayout.ALIGN_TOP, valueId)
            marginStart = context.dp(2)
        }
    }
    seek.apply {
        id = seekId
        max = maxOf(
            LightWakeupDurationUnit.SECONDS.defaultMaximum,
            LightWakeupDurationUnit.SECONDS.fromSeconds(value),
        )
        progress = LightWakeupDurationUnit.SECONDS.fromSeconds(value)
        contentDescription = label
        layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            addRule(RelativeLayout.BELOW, valueId)
            marginStart = context.dp(8)
            marginEnd = context.dp(8)
            topMargin = context.dp(4)
        }
    }

    fun replaceValueView(view: TextView) {
        val current = row.findViewById<View?>(valueId)
        val index = current?.let(row::indexOfChild) ?: row.childCount
        current?.let(row::removeView)
        view.id = valueId
        view.layoutParams = valueLayoutParams()
        row.addView(view, index)
    }

    fun showDisplayValue(displayedValue: Int) {
        displayValue.text = displayedValue.coerceAtLeast(0).toString()
        replaceValueView(displayValue)
    }

    val beginInlineEdit = View.OnClickListener {
        if (row.findViewById<View?>(valueId) is EditText) return@OnClickListener
        val unit = unitText.durationUnit()
        var finishing = false
        val editor = EditText(context).apply {
            setText(seek.progress.toString())
            selectAll()
            gravity = displayValue.gravity
            setTextColor(displayValue.textColors)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, displayValue.textSize)
            typeface = displayValue.typeface
            setSingleLine(true)
            background = null
            setPadding(
                displayValue.paddingLeft,
                displayValue.paddingTop,
                displayValue.paddingRight,
                displayValue.paddingBottom,
            )
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
            contentDescription = "编辑${label}时长"
        }

        fun finishEditing(showError: Boolean): Boolean {
            val selectedValue = editor.text?.toString()?.trim()?.toIntOrNull()
            val selectedSeconds = selectedValue?.let(unit::toSeconds)
            if (selectedValue == null || selectedSeconds == null) {
                if (showError) editor.error = "请输入有效时长"
                return false
            }
            finishing = true
            if (selectedValue > seek.max) seek.max = selectedValue
            if (seek.progress != selectedValue) seek.progress = selectedValue
            onChanged(selectedSeconds)
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(editor.windowToken, 0)
            showDisplayValue(selectedValue)
            return true
        }

        editor.setOnEditorActionListener { _, actionId, _ ->
            actionId == EditorInfo.IME_ACTION_DONE && finishEditing(showError = true)
        }
        editor.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !finishing && !finishEditing(showError = false)) {
                finishing = true
                showDisplayValue(seek.progress)
            }
        }
        replaceValueView(editor)
        editor.requestFocus()
        editor.post {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    displayValue.setOnClickListener(beginInlineEdit)

    seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean,
        ) {
            if (!fromUser) return
            (row.findViewById<View?>(valueId) as? TextView)?.let { displayed ->
                if (!displayed.hasFocus()) displayed.text = progress.toString()
            }
            unitText.durationUnit().toSeconds(progress)?.let(onChanged)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })
    unitText.setOnClickListener {
        (row.findViewById<View?>(valueId) as? EditText)?.clearFocus()
        val displayedValue = seek.progress
        val nextUnit = when (unitText.durationUnit()) {
            LightWakeupDurationUnit.SECONDS -> LightWakeupDurationUnit.MINUTES
            LightWakeupDurationUnit.MINUTES -> LightWakeupDurationUnit.SECONDS
        }
        unitText.tag = nextUnit
        unitText.text = nextUnit.label
        seek.max = maxOf(nextUnit.defaultMaximum, displayedValue)
        seek.progress = displayedValue
        nextUnit.toSeconds(displayedValue)?.let(onChanged)
        showDisplayValue(displayedValue)
    }
    return row
}

private fun TextView.durationUnit(): LightWakeupDurationUnit =
    tag as? LightWakeupDurationUnit ?: LightWakeupDurationUnit.SECONDS

private fun bindLightWakeupDurationPanel(
    root: View,
    enabled: Boolean,
    fadeInSeconds: Int,
) {
    root.findViewById<View?>(R.id.main_alarm_light_wakeup_duration_panel)?.visibility =
        if (enabled) View.VISIBLE else View.GONE
    bindLightWakeupDurationValue(
        root,
        R.id.main_alarm_fade_in_seek,
        R.id.main_alarm_fade_in_value,
        R.id.main_alarm_fade_in_unit,
        fadeInSeconds,
    )
}

private fun bindLightWakeupDurationValue(
    root: View,
    seekId: Int,
    valueId: Int,
    unitId: Int,
    seconds: Int,
) {
    val value = root.findViewById<TextView?>(valueId) ?: return
    if (value.hasFocus()) return
    val unit = root.findViewById<TextView?>(unitId)?.durationUnit()
        ?: LightWakeupDurationUnit.SECONDS
    val displayedValue = unit.fromSeconds(seconds)
    root.findViewById<SeekBar?>(seekId)?.let { seek ->
        seek.max = maxOf(unit.defaultMaximum, displayedValue)
        if (seek.progress != displayedValue) seek.progress = displayedValue
    }
    value.text = displayedValue.toString()
}

/**
 * Direct UI/behavior port of AlarmHelpFragment.
 */
@Composable
fun XimalayaAlarmHelpScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_alarm_help, null, false)
                .also { root ->
                    installXimalayaTitleBar(
                        host = root.findViewById(R.id.main_top),
                        title = "定时启播帮助",
                        left = XimalayaTitleAction.Back {
                            navController.popBackStack()
                        },
                    )
                    val container = root.findViewById<LinearLayout>(R.id.main_container)
                    sourceHelpRows(viewContext).forEach { row ->
                        container.addView(
                            TextView(viewContext).apply {
                                text = row.first
                                textSize = 15f
                                setTextColor(
                                    ContextCompat.getColor(
                                        viewContext,
                                        R.color.xm_alarm_v9514_0x7f060c68,
                                    )
                                )
                                gravity = Gravity.CENTER_VERTICAL
                                setPadding(dp(10), 0, dp(10), 0)
                                setBackgroundResource(R.drawable.xm_alarm_v9514_0x7f082da5)
                                setOnClickListener {
                                    navController.navigate(
                                        Screen.PermissionTutorial.createRoute(row.second)
                                    )
                                }
                            },
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                viewContext.dp(52),
                            ),
                        )
                    }
                    root.findViewById<View>(R.id.tv_more_help).setOnClickListener {
                        navController.navigate(
                            Screen.PermissionSettings.createRoute(fromAlarm = true)
                        )
                    }
                }
        },
    )
}

internal sealed interface XimalayaTitleAction {
    val click: () -> Unit

    data object None : XimalayaTitleAction {
        override val click: () -> Unit = {}
    }
    data class Back(override val click: () -> Unit) : XimalayaTitleAction
    data class Cancel(override val click: () -> Unit) : XimalayaTitleAction
}

/**
 * Visible subset of host_titlebar_top.xml plus BaseFragment2 title injection.
 */
internal fun installXimalayaTitleBar(
    host: RelativeLayout,
    title: String,
    left: XimalayaTitleAction,
    rightText: String? = null,
    onRight: (() -> Unit)? = null,
) {
    val back = host.findViewById<ImageView>(R.id.ximalaya_title_back)
    val cancel = host.findViewById<TextView>(R.id.ximalaya_title_cancel)
    when (left) {
        XimalayaTitleAction.None -> {
            back.visibility = View.GONE
            cancel.visibility = View.GONE
        }
        is XimalayaTitleAction.Back -> {
            back.visibility = View.VISIBLE
            back.setOnClickListener { left.click() }
            cancel.visibility = View.GONE
        }
        is XimalayaTitleAction.Cancel -> {
            back.visibility = View.GONE
            cancel.visibility = View.VISIBLE
            cancel.setOnClickListener { left.click() }
        }
    }
    host.findViewById<TextView>(R.id.ximalaya_title_text).text = title
    host.findViewById<TextView>(R.id.ximalaya_title_right).apply {
        visibility = if (rightText != null && onRight != null) View.VISIBLE else View.GONE
        text = rightText.orEmpty()
        textSize = if (rightText == "帮助") 14f else 15f
        setOnClickListener(if (onRight == null) null else View.OnClickListener { onRight() })
    }
}

private fun taskRuntimeIsActive(task: ScheduledTask, playback: PlaybackSnapshot): Boolean =
    playback.taskId == task.id &&
        playback.transportState != PlaybackTransportState.IDLE &&
        !playback.isTerminal

private fun PlaybackSnapshot.forAlarmList(): PlaybackSnapshot = copy(
    taskName = "",
    trackIndex = 0,
    trackCount = 0,
    trackName = "",
    nextTrackName = null,
    positionMs = 0,
    durationMs = 0,
    startedAt = 0,
    targetVolume = 0,
    message = null,
    updatedAt = 0,
)

private fun taskActivationSummary(startEnabled: Boolean, stopEnabled: Boolean): String = when {
    startEnabled && stopEnabled -> "定时启播、定时关闭均开启"
    startEnabled -> "仅定时启播开启"
    stopEnabled -> "仅定时关闭开启"
    else -> "定时启播、定时关闭均关闭"
}

private fun taskRuntimeStatus(task: ScheduledTask, playback: PlaybackSnapshot): String {
    if (playback.taskId == task.id) {
        return when (playback.transportState) {
            PlaybackTransportState.PREPARING -> "正在准备播放"
            PlaybackTransportState.PLAYING -> when (playback.phase) {
                TaskPhase.FADING_IN -> "正在音量渐强"
                TaskPhase.FADING_OUT -> "正在音量渐弱"
                else -> "正在播放"
            }
            PlaybackTransportState.PAUSED -> "播放已暂停"
            PlaybackTransportState.COMPLETED -> "本次任务已完成"
            PlaybackTransportState.STOPPED -> "本次任务已停止"
            PlaybackTransportState.ERROR -> "本次任务播放异常"
            PlaybackTransportState.IDLE -> "等待执行"
        }
    }
    return when {
        task.status == TaskStatus.EXECUTING -> "正在执行"
        task.hasActiveSchedule() -> "等待起播"
        task.status == TaskStatus.COMPLETED -> "已完成"
        task.status == TaskStatus.CANCELLED -> "已关闭"
        else -> "未开启"
    }
}

private fun taskDetailText(task: ScheduledTask, playback: PlaybackSnapshot): String {
    val startEnabled = task.hasActiveSchedule()
    val runtimeActive = taskRuntimeIsActive(task, playback)
    val stopEnabled = task.hasConfiguredStop() && (startEnabled || runtimeActive)
    val start = if (startEnabled) {
        val executeAt = task.nextExecuteAt?.let(::formatTaskDateTime)
            ?: String.format(
                Locale.getDefault(),
                "%02d:%02d",
                task.startTime.hour,
                task.startTime.minute,
            )
        "$executeAt · ${alarmRepeatLabel(task.toXimalayaRepeatDays())}"
    } else {
        "关闭"
    }
    val configuredStop = when {
        !task.hasConfiguredStop() -> "关闭，所选音频按顺序自然播完"
        task.scheduledStopMode == ScheduledStopMode.MINUTES -> {
            val durationSeconds = task.effectiveScheduledStopDurationSeconds()
            val exactStopAt = playback.takeIf { it.taskId == task.id && it.endsAt > 0 }
                ?.endsAt
                ?: task.executionEndsAt
                ?: task.nextExecuteAt?.let { executeAt ->
                    TaskScheduleCalculator.alarmAt(task, executeAt) +
                        durationSeconds * 1_000L
                }
            buildString {
                append("启播后播放${sourceDurationText(durationSeconds)}")
                exactStopAt?.let { append("（${formatTaskDateTime(it)}）") }
                if (task.scheduledFinishCurrentTrack) append("，到点后播完本集")
            }
        }
        else -> "播完${task.scheduledStopValue}个音频"
    }
    val stop = if (stopEnabled || !task.hasConfiguredStop()) {
        configuredStop
    } else {
        "关闭（已保留：$configuredStop）"
    }
    val selectedNames = task.alarmAudioOrder.mapNotNull { selectedKey ->
        task.audios.firstOrNull { it.selectionKey() == selectedKey }
            ?.name
            ?.takeIf(String::isNotBlank)
    }
    val content = if (task.alarmAudioOrder.isEmpty()) {
        "续播上一次收听"
    } else if (selectedNames.isNotEmpty()) {
        selectedNames.joinToString(" → ")
    } else {
        "已选择${task.alarmAudioOrder.size}个音频"
    }
    val fadeIn = when {
        startEnabled && task.enableFade && task.fadeInDuration > 0 ->
            "开启，${task.fadeInDuration}秒渐强"
        task.enableFade && task.fadeInDuration > 0 ->
            "关闭（已保留${task.fadeInDuration}秒）"
        else -> "关闭"
    }
    val fadeOut = when {
        stopEnabled && task.enableFadeOut && task.fadeOutDuration > 0 ->
            "开启，${task.fadeOutDuration}秒渐弱"
        task.enableFadeOut && task.fadeOutDuration > 0 ->
            "关闭（已保留${task.fadeOutDuration}秒）"
        else -> "关闭"
    }
    return listOf(
        "定时启播：$start",
        "定时关闭：$stop",
        "播放内容：$content",
        "轻唤醒：$fadeIn · 音量渐弱：$fadeOut",
    ).joinToString("\n")
}

private fun formatTaskDateTime(millis: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val day = when {
        target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "今天"
        target.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "明天"
        else -> String.format(
            Locale.getDefault(),
            "%02d月%02d日",
            target.get(Calendar.MONTH) + 1,
            target.get(Calendar.DAY_OF_MONTH),
        )
    }
    return if (target.get(Calendar.SECOND) == 0) {
        String.format(
            Locale.getDefault(),
            "%s %02d:%02d",
            day,
            target.get(Calendar.HOUR_OF_DAY),
            target.get(Calendar.MINUTE),
        )
    } else {
        String.format(
            Locale.getDefault(),
            "%s %02d:%02d:%02d",
            day,
            target.get(Calendar.HOUR_OF_DAY),
            target.get(Calendar.MINUTE),
            target.get(Calendar.SECOND),
        )
    }
}

private fun sourceAlarmRingName(task: ScheduledTask): String {
    if (task.alarmAudioOrder.size > 1) return "已选择${task.alarmAudioOrder.size}个音频"
    task.alarmAudioOrder.firstOrNull()?.let { selectedKey ->
        return task.audios
            .firstOrNull { it.selectionKey() == selectedKey }
            ?.name
            ?.takeIf(String::isNotBlank)
            ?: "音频"
    }
    return task.alarmAudioIndex
        ?.let(task.audios::getOrNull)
        ?.name
        ?.takeIf(String::isNotBlank)
        ?: "续播上一次收听"
}

private fun configurePicker(
    picker: NumberPickerView,
    min: Int,
    max: Int,
    value: Int,
    onChanged: (Int) -> Unit,
) {
    picker.minValue = min
    picker.maxValue = max
    picker.value = value.coerceIn(min, max)
    picker.setOnValueChangedListener { _, _, newValue -> onChanged(newValue) }
}

private fun showAlarmCreationModeDialog(
    context: Context,
    onSelected: (AlarmCreationMode) -> Unit,
) {
    val options = listOf(
        RepeatOption(1, "定时启播 + 定时关闭"),
        RepeatOption(2, "仅定时启播"),
        RepeatOption(3, "仅定时关闭"),
        RepeatOption(4, "取消"),
    )
    val dialog = sourceBottomDialog(context, R.layout.main_dialog_alarm_repeat_type)
    val list = dialog.findViewById<ListView>(R.id.main_repeat_setting_list)
    list.adapter = RepeatOptionAdapter(context, options, selectedDays = -1)
    list.setOnItemClickListener { _, _, position, _ ->
        dialog.dismiss()
        when (position) {
            0 -> onSelected(AlarmCreationMode.START_AND_STOP)
            1 -> onSelected(AlarmCreationMode.START_ONLY)
            2 -> onSelected(AlarmCreationMode.STOP_ONLY)
        }
    }
    dialog.show()
    sizeSourceBottomDialog(dialog)
}

private fun showAlarmRepeatTypeDialog(
    context: Context,
    selectedDays: Int,
    onSelected: (Int) -> Unit,
) {
    val options = listOf(
        RepeatOption(0, "仅一次"),
        RepeatOption(127, "每天"),
        RepeatOption(96, "周末"),
        RepeatOption(31, "周一至周五"),
        RepeatOption(if (selectedDays in 1..126 && selectedDays != 31 && selectedDays != 96) {
            selectedDays
        } else {
            255
        }, "自定义"),
    )
    lateinit var dialog: Dialog
    dialog = sourceBottomDialog(context, R.layout.main_dialog_alarm_repeat_type)
    val list = dialog.findViewById<ListView>(R.id.main_repeat_setting_list)
    list.adapter = RepeatOptionAdapter(context, options, selectedDays)
    list.setOnItemClickListener { _, _, position, _ ->
        val option = options[position]
        if (position == 4) {
            showAlarmCustomRepeatDialog(context, option.days) { custom ->
                dialog.dismiss()
                onSelected(custom)
            }
        } else {
            dialog.dismiss()
            onSelected(option.days)
        }
    }
    dialog.show()
    sizeSourceBottomDialog(dialog)
}

private fun showAlarmCustomRepeatDialog(
    context: Context,
    currentDays: Int,
    onSelected: (Int) -> Unit,
) {
    val calendar = Calendar.getInstance()
    val todayBit = 1 shl when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> 6
    }
    val initial = if (currentDays == 255) todayBit else currentDays.coerceIn(0, 127)
    val labels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val options = labels.mapIndexed { index, label ->
        RepeatOption(1 shl index, label)
    }
    var checkedDays = initial
    val dialog = sourceBottomDialog(context, R.layout.main_dialog_alarm_repeat_type)
    val complete = dialog.findViewById<TextView>(R.id.main_repeat_setting_complete)
    complete.visibility = View.VISIBLE
    val list = dialog.findViewById<ListView>(R.id.main_repeat_setting_list)
    val adapter = RepeatOptionAdapter(context, options, checkedDays, multiSelect = true)
    list.adapter = adapter
    list.setOnItemClickListener { _, _, position, _ ->
        checkedDays = checkedDays xor options[position].days
        adapter.selectedDays = checkedDays
        adapter.notifyDataSetChanged()
    }
    complete.setOnClickListener {
        onSelected(checkedDays)
        dialog.dismiss()
    }
    dialog.show()
    sizeSourceBottomDialog(dialog)
}

private data class RepeatOption(val days: Int, val label: String)

private class RepeatOptionAdapter(
    private val context: Context,
    private val values: List<RepeatOption>,
    var selectedDays: Int,
    private val multiSelect: Boolean = false,
) : BaseAdapter() {
    override fun getCount(): Int = values.size
    override fun getItem(position: Int): RepeatOption = values[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
        val view = recycled ?: LayoutInflater.from(context)
            .inflate(R.layout.main_dialog_alarm_repeat_item, parent, false)
        val value = getItem(position)
        val selected = if (multiSelect) {
            selectedDays and value.days != 0
        } else if (position == 4) {
            selectedDays in 1..126 && selectedDays != 31 && selectedDays != 96
        } else {
            selectedDays == value.days
        }
        view.findViewById<TextView>(R.id.main_alarm_dialog_repeat_name).apply {
            text = value.label
            setTextColor(
                Color.parseColor(
                    if (selected) "#EA6347"
                    else if (isNightMode(context)) "#CFCFCF" else "#333333",
                )
            )
        }
        view.findViewById<ImageView>(R.id.main_alarm_dialog_repeat_iv_choose).visibility =
            if (selected) View.VISIBLE else View.INVISIBLE
        return view
    }
}

private fun sourceBottomDialog(context: Context, layoutId: Int): Dialog =
    Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(true)
        setContentView(layoutId)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
                dimAmount = 0.5f
            }
            decorView.setPadding(
                context.dp(10),
                0,
                context.dp(10),
                context.dp(10),
            )
        }
    }

private fun sizeSourceBottomDialog(dialog: Dialog) {
    dialog.window?.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
    )
    dialog.window?.setGravity(Gravity.BOTTOM)
}

private fun showDeleteAlarmDialog(context: Context, onDelete: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("请选择需要的操作")
        .setItems(arrayOf("删除定时任务")) { dialog, _ ->
            dialog.dismiss()
            onDelete()
        }
        .show()
}

private fun showBackgroundOptimizationPrompt(
    context: Context,
    onSettings: () -> Unit,
) {
    AlertDialog.Builder(context)
        .setTitle("还差一步")
        .setMessage("请在梦枕APP\"设置-后台播放优化\"中完成设置～")
        .setPositiveButton("去设置") { _, _ -> onSettings() }
        .setNegativeButton("知道了", null)
        .show()
}

private fun showOverlayPermissionPrompt(context: Context) {
    AlertDialog.Builder(context)
        .setTitle("开启悬浮窗权限")
        .setMessage("Android10 以上设备需要开启悬浮窗权限")
        .setPositiveButton("打开权限") { _, _ ->
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        .setNegativeButton("取消", null)
        .show()
}

private fun requiresOverlayPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(context)

private fun isBatteryOptimizationAllowed(context: Context): Boolean {
    val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return power.isIgnoringBatteryOptimizations(context.packageName)
}

private fun ScheduledTask.toXimalayaRepeatDays(): Int =
    repeatDays ?: when (repeatType) {
        TaskRepeatType.ONCE -> 0
        TaskRepeatType.DAILY -> 127
        TaskRepeatType.WORKDAY -> 31
        TaskRepeatType.HOLIDAY -> 96
    }

private fun Int.toTaskRepeatType(): TaskRepeatType = when (this) {
    0 -> TaskRepeatType.ONCE
    31 -> TaskRepeatType.WORKDAY
    96 -> TaskRepeatType.HOLIDAY
    else -> TaskRepeatType.DAILY
}

private fun alarmRepeatLabel(days: Int): String = when (days) {
    0 -> "仅一次"
    31 -> "周一至周五"
    96 -> "周末"
    127 -> "每天"
    else -> buildString {
        val labels = arrayOf("一", "二", "三", "四", "五", "六", "日")
        labels.forEachIndexed { index, label ->
            if (days and (1 shl index) != 0) append(label)
        }
    }
}

private fun alarmRelativeText(days: Int, hour: Int, minute: Int): String {
    val now = System.currentTimeMillis()
    val next = nextAlarmCalendar(hour, minute, days).timeInMillis
    val delta = next - now
    if (delta <= 0) return "未知的时间"
    val day = (delta / 86_400_000L).toInt()
    val hourPart = ((delta % 86_400_000L) / 3_600_000L).toInt()
    val minutePart = ((delta % 3_600_000L) / 60_000L).toInt()
    val seconds = ((delta % 60_000L) / 1_000L).toInt()
    return buildString {
        if (day > 0) append(day).append("天")
        if (hourPart > 0) append(hourPart).append("小时")
        if (minutePart > 0) append(minutePart).append("分钟")
        if (day == 0 && hourPart == 0 && minutePart == 0 && seconds > 0) {
            append("不到一分钟")
        }
        append("后启播")
    }
}

private fun nextAlarmCalendar(
    hour: Int,
    minute: Int,
    days: Int,
    now: Long = System.currentTimeMillis(),
): Calendar {
    val candidate = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (days == 0) {
        if (candidate.timeInMillis <= now) candidate.add(Calendar.DAY_OF_YEAR, 1)
        return candidate
    }
    repeat(8) {
        val bit = when (candidate.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 8
            Calendar.FRIDAY -> 16
            Calendar.SATURDAY -> 32
            else -> 64
        }
        if (candidate.timeInMillis > now && days and bit != 0) return candidate
        candidate.add(Calendar.DAY_OF_YEAR, 1)
    }
    return candidate
}

private fun nextAlarmStartTime(
    hour: Int,
    minute: Int,
    days: Int,
    now: Long = System.currentTimeMillis(),
): TaskStartTime {
    val value = nextAlarmCalendar(hour, minute, days, now)
    return TaskStartTime(
        year = value.get(Calendar.YEAR),
        month = value.get(Calendar.MONTH) + 1,
        day = value.get(Calendar.DAY_OF_MONTH),
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
        second = 0,
    )
}

private fun sourceHelpRows(context: Context): List<Pair<String, PermissionKey>> {
    val rows = mutableListOf<Pair<String, PermissionKey>>()
    if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
        rows += "正在使用MIUI系统 >" to PermissionKey.BACKGROUND_RUNNING
    }
    val packages = listOf(
        "com.qihoo360.mobilesafe" to "已安装360卫士 >",
        "com.tencent.qqpimsecure" to "已安装腾讯手机管家 >",
        "com.lbe.security" to "已安装LBE安全大师 >",
        "cn.opda.a.phonoalbumshoushou" to "已安装百度手机卫士 >",
        "com.cleanmaster.mguard_cn" to "已安装猎豹清理大师 >",
    )
    packages.forEach { (packageName, label) ->
        if (isPackageInstalled(context, packageName)) {
            rows += label to PermissionKey.BACKGROUND_RUNNING
        }
    }
    return rows
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean =
    runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
    }.isSuccess

private fun isNightMode(context: Context): Boolean =
    context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES

private fun View.dp(value: Int): Int =
    (value * resources.displayMetrics.density + 0.5f).toInt()

private fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density + 0.5f).toInt()
