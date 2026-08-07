package com.mengzhen.app.ui.screens

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.widget.ConstraintLayout
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.audio.healing.QqMusicHealingScene
import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.hasActiveSchedule
import com.mengzhen.app.scheduler.TaskScheduleCalculator
import com.mengzhen.app.ui.feedback.AppNotice
import com.ximalaya.ting.android.main.view.NumberPickerView
import java.util.Calendar
import java.util.Locale

/**
 * Ximalaya 9.5.1.4 PlanTerminalNewDialog and
 * CustomTimeOffDialogNewFragment, using their original XML resources.
 *
 * Playback state is adapted only at the service boundary.
 */
@Composable
internal fun XimalayaSourcePlanTerminalSheet(
    task: ScheduledTask,
    initialSleepDurationSeconds: Int,
    initialSleepTracks: Int,
    initialFinishCurrentTrack: Boolean,
    initialFadeOutEnabled: Boolean,
    initialFadeOutSeconds: Int,
    canEnableSleepTimer: () -> Boolean,
    isSleepTimerRunning: () -> Boolean,
    onDismiss: () -> Unit,
    onOpenAlarmManager: () -> Unit,
    onScheduleEnabledChanged: (Boolean) -> Boolean,
    onSleepDuration: (Int) -> Unit,
    onSleepTracks: (Int) -> Unit,
    onFinishCurrentTrackChanged: (Boolean) -> Unit,
    onFadeOutChanged: (Boolean, Int) -> Unit,
    onHealingSceneSelected: (QqMusicHealingScene) -> Unit,
) {
    val context = LocalContext.current
    var page by remember(task.id) { mutableStateOf(PlanTerminalPage.MAIN) }
    val currentTask by rememberUpdatedState(task)
    val currentCanEnableSleepTimer by rememberUpdatedState(canEnableSleepTimer)
    val currentIsSleepTimerRunning by rememberUpdatedState(isSleepTimerRunning)
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentOpenAlarmManager by rememberUpdatedState(onOpenAlarmManager)
    val currentScheduleEnabledChanged by rememberUpdatedState(onScheduleEnabledChanged)
    val currentSleepDuration by rememberUpdatedState(onSleepDuration)
    val currentSleepTracks by rememberUpdatedState(onSleepTracks)
    val currentFinishCurrentTrackChanged by rememberUpdatedState(onFinishCurrentTrackChanged)
    val currentFadeOutChanged by rememberUpdatedState(onFadeOutChanged)
    val currentHealingSceneSelected by rememberUpdatedState(onHealingSceneSelected)

    DisposableEffect(
        context,
        task.id,
        page,
    ) {
        val dialog = when (page) {
            PlanTerminalPage.MAIN -> createPlanTerminalDialog(
                context = context,
                task = currentTask,
                initialSleepDurationSeconds = initialSleepDurationSeconds,
                initialSleepTracks = initialSleepTracks,
                initialFinishCurrentTrack = initialFinishCurrentTrack,
                initialFadeOutEnabled = initialFadeOutEnabled,
                initialFadeOutSeconds = initialFadeOutSeconds,
                canEnableSleepTimer = { currentCanEnableSleepTimer() },
                isSleepTimerRunning = { currentIsSleepTimerRunning() },
                onDismiss = currentDismiss,
                onCustomDuration = { page = PlanTerminalPage.CUSTOM_DURATION },
                onEndTime = { page = PlanTerminalPage.END_TIME },
                onOpenAlarmManager = currentOpenAlarmManager,
                onScheduleEnabledChanged = currentScheduleEnabledChanged,
                onSleepDuration = currentSleepDuration,
                onSleepTracks = currentSleepTracks,
                onFinishCurrentTrackChanged = currentFinishCurrentTrackChanged,
                onFadeOutChanged = currentFadeOutChanged,
                onHealing = { page = PlanTerminalPage.HEALING },
            )

            PlanTerminalPage.CUSTOM_DURATION -> createTimePickerDialog(
                context = context,
                title = "自定义关闭",
                initialHour = initialSleepDurationSeconds.coerceAtLeast(0) / 3_600,
                initialMinute = initialSleepDurationSeconds.coerceAtLeast(0) % 3_600 / 60,
                hourUnit = "小时",
                minuteUnit = "分钟",
                onDismiss = currentDismiss,
                onReturnToPlan = { page = PlanTerminalPage.MAIN },
                onConfirm = { hour, minute ->
                    val durationSeconds = (hour * 3_600) + (minute * 60)
                    when {
                        durationSeconds <= 0 -> page = PlanTerminalPage.MAIN
                        currentCanEnableSleepTimer() -> {
                            saveLastDuration(context, durationSeconds)
                            currentSleepDuration(durationSeconds)
                            page = PlanTerminalPage.MAIN
                        }
                        else -> AppNotice.warning(
                            context,
                            "当前没有正在播放的音频，无法设置定时关闭",
                        )
                    }
                },
            )

            PlanTerminalPage.END_TIME -> {
                val now = System.currentTimeMillis()
                val playbackStartsAt = sourcePlaybackStartAt(
                    currentTask,
                    currentIsSleepTimerRunning(),
                    now,
                ) ?: now
                val initialDeadline = sourceStopDeadline(
                    currentTask,
                    initialSleepDurationSeconds,
                    currentIsSleepTimerRunning(),
                    now,
                ) ?: (maxOf(now, playbackStartsAt) + 15 * 60_000L)
                val calendar = Calendar.getInstance().apply { timeInMillis = initialDeadline }
                createTimePickerDialog(
                    context = context,
                    title = "结束时间",
                    initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                    initialMinute = calendar.get(Calendar.MINUTE),
                    hourUnit = "时",
                    minuteUnit = "分",
                    onDismiss = currentDismiss,
                    onReturnToPlan = { page = PlanTerminalPage.MAIN },
                    onConfirm = { hour, minute ->
                        if (!currentCanEnableSleepTimer()) {
                            AppNotice.warning(
                                context,
                                "当前没有正在播放的音频，无法设置定时关闭",
                            )
                        } else {
                            val durationSeconds = TaskScheduleCalculator.durationSecondsUntilClock(
                                playbackStartedAt = playbackStartsAt,
                                afterMillis = System.currentTimeMillis(),
                                hour = hour,
                                minute = minute,
                            )
                            saveLastDuration(context, durationSeconds)
                            currentSleepDuration(durationSeconds)
                            page = PlanTerminalPage.MAIN
                        }
                    },
                )
            }

            PlanTerminalPage.HEALING -> createQqMusicHealingPickerDialog(
                context = context,
                onReturn = { page = PlanTerminalPage.MAIN },
                onSceneSelected = { scene ->
                    currentHealingSceneSelected(scene)
                    currentDismiss()
                },
            )
        }
        dialog.show()
        if (page == PlanTerminalPage.HEALING) {
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
        } else {
            sizePlanTerminalDialog(dialog)
        }
        onDispose {
            dialog.setOnCancelListener(null)
            dialog.setOnDismissListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}

private enum class PlanTerminalPage {
    MAIN,
    CUSTOM_DURATION,
    END_TIME,
    HEALING,
}

private fun createPlanTerminalDialog(
    context: Context,
    task: ScheduledTask,
    initialSleepDurationSeconds: Int,
    initialSleepTracks: Int,
    initialFinishCurrentTrack: Boolean,
    initialFadeOutEnabled: Boolean,
    initialFadeOutSeconds: Int,
    canEnableSleepTimer: () -> Boolean,
    isSleepTimerRunning: () -> Boolean,
    onDismiss: () -> Unit,
    onCustomDuration: () -> Unit,
    onEndTime: () -> Unit,
    onOpenAlarmManager: () -> Unit,
    onScheduleEnabledChanged: (Boolean) -> Boolean,
    onSleepDuration: (Int) -> Unit,
    onSleepTracks: (Int) -> Unit,
    onFinishCurrentTrackChanged: (Boolean) -> Unit,
    onFadeOutChanged: (Boolean, Int) -> Unit,
    onHealing: () -> Unit,
): Dialog {
    val dialog = sourcePlanDialog(context, R.layout.main_dialog_plan_terminal_new)
    val root = dialog.findViewById<ConstraintLayout>(R.id.main_plan_terminal_root)
    installHealingEntry(context, root, onHealing)
    val terminalText = dialog.findViewById<TextView>(R.id.main_tv_terminal_time)
    val terminalBox = dialog.findViewById<CheckBox>(R.id.main_terminal_box)
    val finishTrack = dialog.findViewById<TextView>(R.id.main_tv_select_time_subtitle)
    val openText = dialog.findViewById<TextView>(R.id.main_tv_open)
    val alarmText = dialog.findViewById<TextView>(R.id.main_tv_alarm)
    val openLayout = dialog.findViewById<ConstraintLayout>(R.id.main_open_layout)
    val scheduleBox = dialog.findViewById<CheckBox>(R.id.main_schedule_box).apply {
        contentDescription = "定时启播"
    }
    var fadeOutEnabled = initialFadeOutEnabled
    var fadeOutSeconds = initialFadeOutSeconds.coerceAtLeast(0)
    val fadeOutSwitchRow = dialog.findViewById<ConstraintLayout>(R.id.main_fade_out_switch_row)
    val fadeOutBox = dialog.findViewById<CheckBox>(R.id.main_fade_out_box).apply {
        isChecked = fadeOutEnabled
        contentDescription = "音量渐弱"
    }
    val fadeOutRow = createSourceFadeDurationRow(
        context = context,
        label = "音量渐弱",
        showLabel = false,
        value = fadeOutSeconds,
        seekId = R.id.main_alarm_fade_out_seek,
        valueId = R.id.main_alarm_fade_out_value,
        unitId = R.id.main_alarm_fade_out_unit,
        onChanged = { seconds ->
            fadeOutSeconds = seconds
            onFadeOutChanged(fadeOutEnabled, seconds)
        },
        sourceRow = dialog.findViewById<RelativeLayout>(R.id.main_fade_out_duration_row),
    )
    val preferences = context.getSharedPreferences(
        PLAN_TERMINAL_PREFERENCES,
        Context.MODE_PRIVATE,
    )

    var selectedDurationSeconds = initialSleepDurationSeconds.coerceAtLeast(0)
    var selectedTracks = initialSleepTracks.coerceAtLeast(0)
    var finishCurrentTrack = initialFinishCurrentTrack
    var enabled = selectedDurationSeconds > 0 || selectedTracks > 0
    var scheduleEnabled = task.hasActiveSchedule()

    val minuteViews = linkedMapOf(
        15 to dialog.findViewById<TextView>(R.id.main_tv_select_time15),
        30 to dialog.findViewById<TextView>(R.id.main_tv_select_time30),
        60 to dialog.findViewById<TextView>(R.id.main_tv_select_time60),
        90 to dialog.findViewById<TextView>(R.id.main_tv_select_time90),
    )
    val customView = dialog.findViewById<TextView>(R.id.main_tv_select_time_custom)
    val trackViews = linkedMapOf(
        1 to dialog.findViewById<TextView>(R.id.main_tv_select_series1),
        2 to dialog.findViewById<TextView>(R.id.main_tv_select_series2),
        3 to dialog.findViewById<TextView>(R.id.main_tv_select_series3),
        5 to dialog.findViewById<TextView>(R.id.main_tv_select_series5),
    )

    fun refreshSelection() {
        terminalBox.isChecked = enabled
        fadeOutSwitchRow.visibility = if (enabled) View.VISIBLE else View.GONE
        fadeOutRow.visibility = if (enabled && fadeOutEnabled) View.VISIBLE else View.GONE
        if (fadeOutBox.isChecked != fadeOutEnabled) fadeOutBox.isChecked = fadeOutEnabled
        minuteViews.forEach { (minutes, view) ->
            val selected = enabled && selectedTracks == 0 &&
                selectedDurationSeconds == minutes * 60
            view.isSelected = selected
        }
        customView.isSelected = enabled &&
            selectedTracks == 0 &&
            selectedDurationSeconds > 0 &&
            minuteViews.keys.none { it * 60 == selectedDurationSeconds }
        trackViews.forEach { (tracks, view) ->
            val selected = enabled && selectedTracks == tracks
            view.isSelected = selected
        }
        finishTrack.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (finishCurrentTrack) {
                R.drawable.xm_alarm_v9514_0x7f082d66
            } else {
                R.drawable.xm_alarm_v9514_0x7f082d67
            },
            0,
            0,
            0,
        )
        val sleepTimerRunning = isSleepTimerRunning()
        val waitsForScheduledStart = scheduleEnabled && !sleepTimerRunning
        val primarySummary = when {
            !enabled -> "定时关闭"
            waitsForScheduledStart && selectedTracks == 1 -> "启播后播完本集停止"
            waitsForScheduledStart && selectedTracks > 1 ->
                "启播后播完${selectedTracks}集停止"
            waitsForScheduledStart ->
                "启播后${sourceDurationText(selectedDurationSeconds)}停止"
            selectedTracks == 1 -> "播完本集后停止"
            selectedTracks > 1 -> "播完${selectedTracks}集后停止"
            else -> "${sourceDurationText(selectedDurationSeconds)}后停止"
        }
        val stopSummary = when {
            enabled && selectedTracks == 0 -> sourceScheduledStopSummary(
                task,
                selectedDurationSeconds,
                sleepTimerRunning,
            )
            !enabled && canEnableSleepTimer() -> "点击设置具体结束时间"
            else -> null
        }
        terminalText.maxLines = if (stopSummary == null) 1 else 2
        terminalText.text = if (stopSummary == null) {
            primarySummary
        } else {
            SpannableString("$primarySummary\n$stopSummary").apply {
                val start = primarySummary.length + 1
                setSpan(
                    RelativeSizeSpan(0.72f),
                    start,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                setSpan(
                    StyleSpan(Typeface.NORMAL),
                    start,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }

    fadeOutBox.setOnCheckedChangeListener { _, checked ->
        fadeOutEnabled = checked
        onFadeOutChanged(checked, fadeOutSeconds)
        refreshSelection()
    }
    fadeOutSwitchRow.setOnClickListener {
        fadeOutBox.isChecked = !fadeOutBox.isChecked
    }

    fun rejectUnavailableSleepTimer(): Boolean {
        if (canEnableSleepTimer()) return false
        AppNotice.warning(context, "当前没有正在播放的音频，无法设置定时关闭")
        refreshSelection()
        return true
    }

    minuteViews.forEach { (minutes, view) ->
        view.setOnClickListener {
            if (rejectUnavailableSleepTimer()) return@setOnClickListener
            selectedDurationSeconds = minutes * 60
            selectedTracks = 0
            enabled = true
            saveLastDuration(context, selectedDurationSeconds)
            onSleepDuration(selectedDurationSeconds)
            refreshSelection()
        }
    }
    trackViews.forEach { (tracks, view) ->
        view.setOnClickListener {
            if (rejectUnavailableSleepTimer()) return@setOnClickListener
            selectedDurationSeconds = 0
            selectedTracks = tracks
            enabled = true
            preferences.edit()
                .putString(KEY_LAST_TERMINAL_MODE, "tracks")
                .putInt(KEY_LAST_TERMINAL_VALUE, tracks)
                .apply()
            onSleepTracks(tracks)
            refreshSelection()
        }
    }
    customView.setOnClickListener {
        if (!rejectUnavailableSleepTimer()) onCustomDuration()
    }
    terminalText.setOnClickListener {
        if (!rejectUnavailableSleepTimer()) onEndTime()
    }
    finishTrack.setOnClickListener {
        finishCurrentTrack = !finishCurrentTrack
        onFinishCurrentTrackChanged(finishCurrentTrack)
        refreshSelection()
    }
    terminalBox.setOnClickListener {
        if (!terminalBox.isChecked) {
            enabled = false
            selectedDurationSeconds = 0
            selectedTracks = 0
            onSleepDuration(0)
        } else {
            if (rejectUnavailableSleepTimer()) return@setOnClickListener
            val lastMode = preferences.getString(KEY_LAST_TERMINAL_MODE, "minutes")
            val lastValue = preferences.getInt(KEY_LAST_TERMINAL_VALUE, 15)
            enabled = true
            if (lastMode == "tracks") {
                selectedDurationSeconds = 0
                selectedTracks = lastValue.takeIf { it in trackViews.keys } ?: 1
                onSleepTracks(selectedTracks)
            } else {
                selectedTracks = 0
                selectedDurationSeconds = preferences.getInt(
                    KEY_LAST_TERMINAL_DURATION_SECONDS,
                    lastValue.coerceAtLeast(1) * 60,
                ).coerceAtLeast(60)
                onSleepDuration(selectedDurationSeconds)
            }
        }
        refreshSelection()
    }

    fun refreshSchedule() {
        scheduleBox.isChecked = scheduleEnabled
        openText.text = "定时启播"
        alarmText.text = if (scheduleEnabled) sourceAlarmSummary(task) else ""
    }
    scheduleBox.setOnClickListener {
        val requested = scheduleBox.isChecked
        scheduleEnabled = onScheduleEnabledChanged(requested)
        if (!scheduleEnabled && enabled && !canEnableSleepTimer()) {
            enabled = false
            selectedDurationSeconds = 0
            selectedTracks = 0
            onSleepDuration(0)
            refreshSelection()
        }
        refreshSchedule()
        refreshSelection()
    }
    openLayout.setOnClickListener {
        onOpenAlarmManager()
    }
    dialog.findViewById<View>(R.id.main_iv_close).setOnClickListener {
        onDismiss()
    }
    dialog.setOnCancelListener { onDismiss() }

    refreshSelection()
    refreshSchedule()
    val countdownRefresh = object : Runnable {
        override fun run() {
            if (!dialog.isShowing) return
            val running = isSleepTimerRunning()
            if (enabled && !running && !canEnableSleepTimer()) {
                enabled = false
                selectedDurationSeconds = 0
                selectedTracks = 0
                scheduleEnabled = false
                refreshSchedule()
                refreshSelection()
            } else if (enabled && selectedTracks == 0 && running) {
                val remaining = AudioPlaybackService.getSleepTimerRemainingSeconds(context)
                if (remaining <= 0) {
                    enabled = false
                    selectedDurationSeconds = 0
                } else {
                    selectedDurationSeconds = AudioPlaybackService
                        .getConfiguredSleepDurationSeconds()
                        .takeIf { it > 0 }
                        ?: remaining
                }
                refreshSelection()
            } else if (enabled) {
                refreshSelection()
            }
            root.postDelayed(this, 1_000L)
        }
    }
    root.postDelayed(countdownRefresh, 1_000L)
    return dialog
}

private fun createTimePickerDialog(
    context: Context,
    title: String,
    initialHour: Int,
    initialMinute: Int,
    hourUnit: String,
    minuteUnit: String,
    onDismiss: () -> Unit,
    onReturnToPlan: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
): Dialog {
    val dialog = sourcePlanDialog(context, R.layout.main_fra_custom_time_off_dialog_new)
    val hourPicker = dialog.findViewById<NumberPickerView>(R.id.main_picker_hour)
    val minutePicker = dialog.findViewById<NumberPickerView>(R.id.main_picker_minute)
    val pickerTypeface = Typeface.create("sans-serif-light", Typeface.BOLD)

    dialog.findViewById<TextView>(R.id.main_tv_title).text = title
    dialog.findViewById<TextView>(R.id.main_picker_hour_unit).text = hourUnit
    dialog.findViewById<TextView>(R.id.main_picker_minute_unit).text = minuteUnit
    hourPicker.setHintTextTypeface(pickerTypeface)
    hourPicker.setContentTextTypeface(pickerTypeface)
    minutePicker.setHintTextTypeface(pickerTypeface)
    minutePicker.setContentTextTypeface(pickerTypeface)
    configureSourcePicker(hourPicker, 0, 23, initialHour)
    configureSourcePicker(minutePicker, 0, 59, initialMinute)

    dialog.findViewById<View>(R.id.main_tv_cancel).setOnClickListener {
        onReturnToPlan()
    }
    dialog.findViewById<View>(R.id.main_iv_close).setOnClickListener {
        onDismiss()
    }
    dialog.findViewById<View>(R.id.main_tv_ok).setOnClickListener {
        onConfirm(hourPicker.value, minutePicker.value)
    }
    dialog.setOnCancelListener { onReturnToPlan() }
    return dialog
}

private fun configureSourcePicker(
    picker: NumberPickerView,
    min: Int,
    max: Int,
    initial: Int,
) {
    picker.minValue = min
    picker.maxValue = max
    picker.value = initial.coerceIn(min, max)
    picker.setOnValueChangedListener { view, oldValue, newValue ->
        if (oldValue != newValue) view.performHapticFeedback(1)
    }
}

private fun installHealingEntry(
    context: Context,
    root: ConstraintLayout,
    onHealing: () -> Unit,
) {
    val entry = LayoutInflater.from(context)
        .inflate(R.layout.qq_auto_close_healing_entry, root, false)
        .apply {
            id = View.generateViewId()
            setOnClickListener { onHealing() }
        }
    root.addView(
        entry,
        ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            planDp(context, 54),
        ).apply {
            topToBottom = R.id.main_open_layout
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = planDp(context, 10)
            marginStart = planDp(context, 16)
            marginEnd = planDp(context, 16)
        },
    )
}

private fun planDp(context: Context, value: Int): Int =
    (value * context.resources.displayMetrics.density + .5f).toInt()

private fun sourcePlanDialog(context: Context, layoutId: Int): Dialog =
    Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(true)
        setContentView(layoutId)
        window?.apply {
            decorView.setPadding(0, 0, 0, 0)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM
                dimAmount = 0.7f
            }
            setWindowAnimations(R.style.arg_res_0x7f1303c3)
        }
    }

private fun sizePlanTerminalDialog(dialog: Dialog) {
    dialog.window?.apply {
        setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        setGravity(Gravity.BOTTOM)
    }
}

private fun sourceAlarmSummary(task: ScheduledTask): String {
    val time = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        task.startTime.hour,
        task.startTime.minute,
    )
    val days = task.repeatDays ?: when (task.repeatType.name) {
        "WORKDAY" -> 31
        "HOLIDAY" -> 96
        "DAILY" -> 127
        else -> 0
    }
    val repeat = when (days) {
        0 -> {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(task.startTime.year, task.startTime.month - 1, task.startTime.day)
            }
            if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
            ) {
                "今天"
            } else {
                "明天"
            }
        }
        31 -> "工作日"
        96 -> "周末"
        127 -> "每天"
        else -> buildString {
            append("每周")
            arrayOf("一", "二", "三", "四", "五", "六", "日")
                .forEachIndexed { index, label ->
                    if (days and (1 shl index) != 0) append(label)
                }
        }
    }
    return "$repeat $time"
}

private fun sourceScheduledStopSummary(
    task: ScheduledTask,
    durationSeconds: Int,
    running: Boolean,
): String? {
    val now = System.currentTimeMillis()
    val deadline = sourceStopDeadline(task, durationSeconds, running, now) ?: return null
    return "停止时间 ${sourceStopDateTime(deadline, now)}"
}

private fun sourceStopDeadline(
    task: ScheduledTask,
    durationSeconds: Int,
    running: Boolean,
    now: Long,
): Long? {
    if (durationSeconds <= 0) return null
    AudioPlaybackService.getSleepTimerEndTimeMs()
        .takeIf { running && it > now }
        ?.let { return it }
    return sourcePlaybackStartAt(task, running, now)?.let {
        it + durationSeconds * 1_000L
    }
}

private fun sourcePlaybackStartAt(
    task: ScheduledTask,
    running: Boolean,
    now: Long,
): Long? = AudioPlaybackService.getPlaybackStartedAtMs()
    .takeIf { running && it > 0L }
    ?: task.executionStartedAt?.takeIf { it > 0L }
    ?: task.nextExecuteAt?.takeIf { it >= now }
    ?: TaskScheduleCalculator.nextExecuteAt(task, now)

internal fun sourceDurationText(durationSeconds: Int): String {
    val safeSeconds = durationSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3_600
    val minutes = safeSeconds % 3_600 / 60
    val seconds = safeSeconds % 60
    return buildString {
        if (hours > 0) append("${hours}小时")
        if (minutes > 0) append("${minutes}分钟")
        if (seconds > 0 || isEmpty()) append("${seconds}秒")
    }
}

private fun saveLastDuration(context: Context, durationSeconds: Int) {
    context.getSharedPreferences(PLAN_TERMINAL_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LAST_TERMINAL_MODE, "minutes")
        .putInt(KEY_LAST_TERMINAL_VALUE, (durationSeconds + 59) / 60)
        .putInt(KEY_LAST_TERMINAL_DURATION_SECONDS, durationSeconds)
        .apply()
}

private fun sourceStopDateTime(deadline: Long, nowMillis: Long): String {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val target = Calendar.getInstance().apply { timeInMillis = deadline }
    val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val day = when {
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> "今天"
        tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> "明天"
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) ->
            "${target.get(Calendar.MONTH) + 1}月${target.get(Calendar.DAY_OF_MONTH)}日"
        else ->
            "${target.get(Calendar.YEAR)}年${target.get(Calendar.MONTH) + 1}月" +
                "${target.get(Calendar.DAY_OF_MONTH)}日"
    }
    val time = if (target.get(Calendar.SECOND) == 0) {
        String.format(
            Locale.getDefault(),
            "%02d:%02d",
            target.get(Calendar.HOUR_OF_DAY),
            target.get(Calendar.MINUTE),
        )
    } else {
        String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            target.get(Calendar.HOUR_OF_DAY),
            target.get(Calendar.MINUTE),
            target.get(Calendar.SECOND),
        )
    }
    return "$day $time"
}

private const val PLAN_TERMINAL_PREFERENCES = "ximalaya_plan_terminal"
private const val KEY_LAST_TERMINAL_MODE = "last_plan_terminal_mode"
private const val KEY_LAST_TERMINAL_VALUE = "last_plan_terminal_value"
private const val KEY_LAST_TERMINAL_DURATION_SECONDS = "last_plan_terminal_duration_seconds"
