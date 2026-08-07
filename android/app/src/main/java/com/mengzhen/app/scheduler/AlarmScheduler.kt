package com.mengzhen.app.scheduler

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.ScheduledStopMode
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.model.selectionKey
import com.mengzhen.app.data.model.TaskRepeatType
import com.mengzhen.app.data.model.TaskStatus
import com.mengzhen.app.data.model.effectiveScheduledStopDurationSeconds
import com.mengzhen.app.data.model.isOneShotSchedule
import com.mengzhen.app.data.store.AppSettingsStore
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.receiver.AlarmReceiver
import org.json.JSONArray
import org.json.JSONObject

/**
 * Android 原生任务调度器。
 *
 * 可靠性策略参考喜马拉雅 AlarmRecordManager：
 * 1. 任务先持久化，再交给 AlarmManager；
 * 2. 使用 RTC_WAKEUP 精确闹钟；
 * 3. 每次触发、完成、编辑或开机恢复后重新计算下一次执行时间；
 * 4. 进程重建时从持久化的执行窗口恢复未完成播放。
 *
 * 与参考实现不同的是，梦枕为每个任务使用独立 PendingIntent，避免多个任务
 * 同时到点时只唤醒其中一个；任务状态统一存放在 TaskStore。
 */
class AlarmScheduler private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager =
        appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val store = TaskStore.get(appContext)

    @Synchronized
    fun saveAndSchedule(task: ScheduledTask): ScheduledTask {
        cancelPendingIntent(task.id)
        if (
            AudioPlaybackService.getCurrentTaskId() == task.id &&
            AudioPlaybackService.isCurrentSessionScheduled()
        ) {
            AudioPlaybackService.stopPlayback(appContext)
        }

        val normalized = task.copy(
            scheduleArmed = true,
            status = TaskStatus.PENDING,
            lastExecutedAt = null,
            completedAt = null,
            skipUntil = null,
            executionStartedAt = null,
            executionEndsAt = null,
            updatedAt = System.currentTimeMillis(),
        )
        val nextAt = TaskScheduleCalculator.nextExecuteAt(normalized)
        val saved = store.saveTask(normalized.copy(nextExecuteAt = nextAt))
        if (nextAt != null) scheduleOccurrence(saved, nextAt)
        return saved
    }

    @Synchronized
    fun deleteTask(taskId: String): Boolean {
        cancelPendingIntent(taskId)
        if (AudioPlaybackService.getCurrentTaskId() == taskId) {
            AudioPlaybackService.stopPlayback(appContext)
        }
        return store.deleteTask(taskId)
    }

    @Synchronized
    fun cancelTask(taskId: String): ScheduledTask? {
        val task = store.getTaskById(taskId) ?: return null
        cancelPendingIntent(taskId)
        if (AudioPlaybackService.getCurrentTaskId() == taskId) {
            AudioPlaybackService.stopPlayback(appContext)
        }
        return persistCancellation(task)
    }

    /**
     * Turns future scheduled starts off without treating the audio that has
     * already started as part of the switch operation.
     */
    @Synchronized
    fun disableSchedule(taskId: String): ScheduledTask? {
        val task = store.getTaskById(taskId) ?: return null
        cancelPendingIntent(taskId)
        return store.saveTask(
            task.copy(
                scheduleArmed = false,
                status = TaskStatus.CANCELLED,
                nextExecuteAt = null,
                skipUntil = null,
                executionStartedAt = null,
                executionEndsAt = null,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    @Synchronized
    fun onPlaybackStoppedByUser(taskId: String) {
        val task = store.getTaskById(taskId) ?: return
        cancelPendingIntent(taskId)
        persistCancellation(task)
    }

    private fun persistCancellation(task: ScheduledTask): ScheduledTask {
        val now = System.currentTimeMillis()
        val updated = if (task.isOneShotSchedule()) {
            task.copy(
                status = TaskStatus.CANCELLED,
                nextExecuteAt = null,
                executionStartedAt = null,
                executionEndsAt = null,
                updatedAt = now,
            )
        } else {
            val skipUntil = now + task.playDurationMinutes.coerceAtLeast(1) * 60_000L
            val nextAt = TaskScheduleCalculator.nextExecuteAt(task, skipUntil)
            task.copy(
                status = TaskStatus.PENDING,
                skipUntil = skipUntil,
                nextExecuteAt = nextAt,
                executionStartedAt = null,
                executionEndsAt = null,
                updatedAt = now,
            )
        }
        store.saveTask(updated)
        updated.nextExecuteAt?.let { scheduleOccurrence(updated, it) }
        return updated
    }

    @Synchronized
    fun resumeTask(taskId: String): ScheduledTask? {
        val task = store.getTaskById(taskId) ?: return null
        cancelPendingIntent(taskId)
        val now = System.currentTimeMillis()
        val candidate = task.copy(
            scheduleArmed = true,
            status = TaskStatus.PENDING,
            skipUntil = null,
            completedAt = null,
            executionStartedAt = null,
            executionEndsAt = null,
            updatedAt = now,
        )
        val nextAt = TaskScheduleCalculator.nextExecuteAt(candidate, now)
        val updated = candidate.copy(
            status = if (nextAt == null && candidate.isOneShotSchedule()) {
                TaskStatus.COMPLETED
            } else {
                TaskStatus.PENDING
            },
            nextExecuteAt = nextAt,
            completedAt = if (nextAt == null) now else null,
        )
        store.saveTask(updated)
        nextAt?.let { scheduleOccurrence(updated, it) }
        return updated
    }

    @Synchronized
    fun executeNow(taskId: String): Boolean {
        val task = store.getTaskById(taskId) ?: return false
        cancelPendingIntent(taskId)
        val now = System.currentTimeMillis()
        return startOccurrence(
            task = task.copy(scheduleArmed = true),
            scheduledExecuteAt = now,
        )
    }

    /**
     * Starts an ordinary player session without mutating or cancelling this task's
     * future alarm. The session has no implicit stop deadline; the player sheet can
     * add one with AudioPlaybackService.setSleepTimer / setSleepAfterTracks.
     */
    @Synchronized
    fun startManualPlayback(taskId: String, startIndex: Int = 0): Boolean {
        val task = store.getTaskById(taskId) ?: return false
        if (startIndex !in task.audios.indices) return false
        if (playableUrl(task.audios[startIndex]) == null) return false

        val currentTaskId = AudioPlaybackService.getCurrentTaskId()
        if (currentTaskId == task.id) {
            if (AudioPlaybackService.getCurrentTrackIndex() == startIndex) {
                AudioPlaybackService.resume(appContext)
            } else {
                AudioPlaybackService.playIndex(appContext, startIndex)
            }
            return true
        }

        currentTaskId?.let { playingTaskId ->
            if (AudioPlaybackService.isCurrentSessionScheduled()) {
                cancelTask(playingTaskId)
            } else {
                AudioPlaybackService.stopPlayback(appContext)
            }
        }
        return startPlaybackService(
            task = task,
            endsAt = 0L,
            scheduledExecution = false,
            startIndex = startIndex,
        )
    }

    @Synchronized
    fun handleAlarmTrigger(taskId: String) {
        val task = store.getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        if (task.status == TaskStatus.CANCELLED || task.status == TaskStatus.COMPLETED) return
        if (!task.scheduleArmed && task.status != TaskStatus.EXECUTING) return
        if (task.status == TaskStatus.EXECUTING) {
            val recoveryEnd = TaskScheduleCalculator.executionRecoveryEndAt(task)
            if (recoveryEnd == null || now < recoveryEnd) {
                if (AudioPlaybackService.getCurrentTaskId() != task.id) {
                    startPlaybackService(
                        task,
                        task.executionEndsAt ?: 0L,
                        scheduledExecution = true,
                    )
                }
            } else {
                onPlaybackCompleted(task.id)
            }
            return
        }
        if (task.skipUntil?.let { it > now } == true) {
            scheduleNext(task, task.skipUntil)
            return
        }

        val executeAt = task.nextExecuteAt ?: TaskScheduleCalculator.nextExecuteAt(task, now)
        if (executeAt == null) {
            completeExpiredOnce(task, now)
            return
        }

        val recoveryEnd = TaskScheduleCalculator.occurrenceRecoveryEndAt(task, executeAt)
        if (now >= recoveryEnd) {
            if (task.isOneShotSchedule()) {
                completeExpiredOnce(task, now)
            } else {
                scheduleNext(task, now)
            }
            return
        }
        startOccurrence(
            task = task,
            scheduledExecuteAt = executeAt,
        )
    }

    @Synchronized
    fun onPlaybackCompleted(taskId: String) {
        val task = store.getTaskById(taskId) ?: return
        if (task.status != TaskStatus.EXECUTING) return
        val now = System.currentTimeMillis()
        if (task.isOneShotSchedule()) {
            store.saveTask(
                task.copy(
                    status = TaskStatus.COMPLETED,
                    completedAt = now,
                    nextExecuteAt = null,
                    executionStartedAt = null,
                    executionEndsAt = null,
                    updatedAt = now,
                )
            )
        } else {
            val pending = task.copy(
                status = TaskStatus.PENDING,
                executionStartedAt = null,
                executionEndsAt = null,
                updatedAt = now,
            )
            scheduleNext(pending, now + 1_000L)
        }
    }

    /**
     * 由播放器在第一帧音频真正进入播放态时确认执行起点。下载、解码和缓冲
     * 都不会占用用户设置的播放时长，渐强也从这个时刻开始。
     */
    @Synchronized
    fun onPlaybackActuallyStarted(taskId: String, startedAt: Long): Long? {
        val task = store.getTaskById(taskId) ?: return null
        if (task.status != TaskStatus.EXECUTING) return task.executionEndsAt
        val safeStartedAt = startedAt.coerceAtMost(System.currentTimeMillis())
        val endsAt = TaskScheduleCalculator.scheduledStopDeadlineAt(task, safeStartedAt)
        store.saveTask(
            task.copy(
                lastExecutedAt = safeStartedAt,
                executionStartedAt = safeStartedAt,
                executionEndsAt = endsAt,
                updatedAt = System.currentTimeMillis(),
            )
        )
        return endsAt
    }

    fun runtimeState(
        task: ScheduledTask,
        now: Long = System.currentTimeMillis(),
    ): TaskRuntimeState = TaskScheduleCalculator.runtimeState(task, now)

    /**
     * App 启动、开机广播后恢复全部调度。对于被系统杀死但仍在执行窗口内的任务，
     * 继续使用原结束时间恢复播放；已错过整个窗口的任务直接收口状态。
     */
    @Synchronized
    fun restoreAllAlarms(deferPlaybackToAlarm: Boolean = false) {
        LegacyTaskMigrator(appContext, store).migrate()
        store.clearExpiredSkips()
        val now = System.currentTimeMillis()

        // The player can own only one scheduled execution. A pair of alarms delivered
        // in the same process-start window may both have reached persistent EXECUTING
        // before the foreground service publishes its in-memory task id. Keep the
        // service-owned execution when available, otherwise the newest persisted one.
        val executingTasks = store.getAllTasks().filter { it.status == TaskStatus.EXECUTING }
        val survivingExecutionId = AudioPlaybackService.getCurrentTaskId()
            ?.takeIf { currentId ->
                AudioPlaybackService.isCurrentSessionScheduled() &&
                    executingTasks.any { it.id == currentId }
            }
            ?: executingTasks.maxByOrNull {
                it.executionStartedAt ?: it.lastExecutedAt ?: Long.MIN_VALUE
            }?.id
        executingTasks
            .asSequence()
            .filter { it.id != survivingExecutionId }
            .forEach { displaced ->
                cancelPendingIntent(displaced.id)
                onPlaybackCompleted(displaced.id)
            }

        store.getAllTasks().forEach { original ->
            val task = store.getTaskById(original.id) ?: return@forEach
            cancelPendingIntent(task.id)

            when {
                task.status == TaskStatus.CANCELLED || task.status == TaskStatus.COMPLETED -> Unit
                task.status == TaskStatus.EXECUTING ->
                    restoreExecutingTask(task, now, deferPlaybackToAlarm)
                !AlarmRestoreContract.shouldRestore(task.status, task.scheduleArmed) -> Unit
                task.skipUntil?.let { it > now } == true -> scheduleNext(task, task.skipUntil)
                else -> restorePendingTask(task, now, deferPlaybackToAlarm)
            }
        }
    }

    private fun restoreExecutingTask(
        task: ScheduledTask,
        now: Long,
        deferPlaybackToAlarm: Boolean,
    ) {
        val recoveryEnd = TaskScheduleCalculator.executionRecoveryEndAt(task)
        if (recoveryEnd == null || now < recoveryEnd) {
            if (AudioPlaybackService.getCurrentTaskId() != task.id) {
                if (deferPlaybackToAlarm) {
                    scheduleReceiverWake(task.id, now + RESTORE_ALARM_DELAY_MS)
                } else {
                    startPlaybackService(
                        task,
                        task.executionEndsAt ?: 0L,
                        scheduledExecution = true,
                    )
                }
            }
        } else {
            onPlaybackCompleted(task.id)
        }
    }

    private fun restorePendingTask(
        task: ScheduledTask,
        now: Long,
        deferPlaybackToAlarm: Boolean,
    ) {
        val storedNext = task.nextExecuteAt
        if (storedNext != null) {
            val alarmAt = TaskScheduleCalculator.alarmAt(task, storedNext)
            val recoveryEnd = TaskScheduleCalculator.occurrenceRecoveryEndAt(task, storedNext)
            when {
                alarmAt > now -> {
                    scheduleOccurrence(task, storedNext)
                    return
                }
                now < recoveryEnd -> {
                    if (deferPlaybackToAlarm) {
                        scheduleReceiverWake(task.id, now + RESTORE_ALARM_DELAY_MS)
                    } else {
                        startOccurrence(
                            task = task,
                            scheduledExecuteAt = storedNext,
                        )
                    }
                    return
                }
                task.isOneShotSchedule() -> {
                    completeExpiredOnce(task, now)
                    return
                }
            }
        }
        scheduleNext(task, now)
    }

    private fun startOccurrence(
        task: ScheduledTask,
        scheduledExecuteAt: Long,
    ): Boolean {
        if (!hasPlayableAudio(task)) {
            Log.e(TAG, "Task ${task.id} has no playable audio")
            store.saveTask(
                task.copy(
                status = TaskStatus.CANCELLED,
                nextExecuteAt = null,
                executionStartedAt = null,
                executionEndsAt = null,
                updatedAt = System.currentTimeMillis(),
                )
            )
            return false
        }

        val displacedScheduledTaskIds = buildSet {
            AudioPlaybackService.getCurrentTaskId()
                ?.takeIf { it != task.id && AudioPlaybackService.isCurrentSessionScheduled() }
                ?.let(::add)
            store.getAllTasks()
                .asSequence()
                .filter { it.id != task.id && it.status == TaskStatus.EXECUTING }
                .mapTo(this) { it.id }
        }

        val now = System.currentTimeMillis()
        val executing = task.copy(
            scheduleArmed = true,
            status = TaskStatus.EXECUTING,
            lastExecutedAt = null,
            nextExecuteAt = scheduledExecuteAt,
            completedAt = null,
            skipUntil = null,
            executionStartedAt = null,
            executionEndsAt = null,
            updatedAt = now,
        )
        store.saveTask(executing)
        val started = startPlaybackService(
            task = executing,
            endsAt = 0L,
            scheduledExecution = true,
            startIndex = task.alarmAudioIndex
                ?.takeIf { it in task.audios.indices }
                ?: 0,
        )
        if (started) {
            displacedScheduledTaskIds.forEach(::onPlaybackCompleted)
        }
        return started
    }

    private fun startPlaybackService(
        task: ScheduledTask,
        endsAt: Long,
        scheduledExecution: Boolean,
        startIndex: Int = 0,
    ): Boolean {
        val selectedAlarmAudios = if (scheduledExecution) selectedAlarmAudios(task) else null
        val playbackAudios = selectedAlarmAudios ?: task.audios
        val playbackStartIndex = if (selectedAlarmAudios == null) startIndex else 0
        val tracks = JSONArray()
        var resolvedStartIndex = 0
        playbackAudios.forEachIndexed { originalIndex, audio ->
            playableUrl(audio)?.let { url ->
                if (originalIndex == playbackStartIndex) resolvedStartIndex = tracks.length()
                tracks.put(
                    JSONObject()
                        .put(
                            "id",
                            audio.id.ifBlank {
                                audio.fileKey?.takeIf(String::isNotBlank) ?: url
                            }
                        )
                        .put("url", url)
                        .put("name", audio.name.ifEmpty { "音频" })
                )
            }
        }
        if (tracks.length() == 0) return false

        val first = tracks.getJSONObject(0)
        val intent = Intent(appContext, AudioPlaybackService::class.java)
            .setAction(AudioPlaybackService.ACTION_START)
            .putExtra("taskId", task.id)
            .putExtra("taskName", task.name)
            .putExtra("playDurationMinutes", task.playDurationMinutes)
            .putExtra("volume", task.volume.coerceIn(0, 100))
            .putExtra("enableFade", task.enableFade)
            .putExtra("enableFadeOut", task.enableFadeOut)
            .putExtra("fadeInDuration", task.fadeInDuration.coerceAtLeast(0))
            .putExtra("fadeOutDuration", task.fadeOutDuration.coerceAtLeast(0))
            .putExtra("audioUrl", first.optString("url"))
            .putExtra("audioId", first.optString("id"))
            .putExtra("audioName", first.optString("name"))
            .putExtra(
                "coverUrl",
                playbackAudios.getOrNull(playbackStartIndex)?.artworkUri
                    ?: playbackAudios.firstOrNull()?.artworkUri,
            )
            .putExtra("tracksJson", tracks.toString())
            .putExtra("loopSingle", tracks.length() == 1)
            .putExtra(AudioPlaybackService.EXTRA_SKIP_HEAD_SECONDS, task.skipHeadSeconds)
            .putExtra(AudioPlaybackService.EXTRA_SKIP_TAIL_SECONDS, task.skipTailSeconds)
            .putExtra("endTime", 0L)
            .apply {
                task.executionStartedAt?.let {
                    putExtra(AudioPlaybackService.EXTRA_STARTED_AT, it)
                }
                if (scheduledExecution) {
                    putExtra(
                        AudioPlaybackService.EXTRA_EXECUTION_DURATION_SECONDS,
                        task.effectiveScheduledStopDurationSeconds(),
                    )
                    if (
                        task.scheduledStopMode == ScheduledStopMode.MINUTES &&
                        endsAt > 0L
                    ) {
                        putExtra(AudioPlaybackService.EXTRA_SLEEP_TIMER_END_TIME, endsAt)
                    }
                    if (
                        task.scheduledStopMode == ScheduledStopMode.TRACKS &&
                        task.scheduledStopValue > 0
                    ) {
                        putExtra(
                            AudioPlaybackService.EXTRA_REMAINING_TRACKS,
                            task.scheduledStopValue,
                        )
                    }
                    putExtra(
                        AudioPlaybackService.EXTRA_FINISH_CURRENT_TRACK,
                        task.scheduledFinishCurrentTrack,
                    )
                    putExtra(AudioPlaybackService.EXTRA_STOP_AT_PLAYLIST_END, true)
                }
            }
            .putExtra(AudioPlaybackService.EXTRA_IS_SCHEDULED_EXECUTION, scheduledExecution)
            .putExtra(AudioPlaybackService.EXTRA_TRACK_INDEX, resolvedStartIndex)

        appContext.startForegroundService(intent)
        Log.i(
            TAG,
            "Started ${if (scheduledExecution) "scheduled" else "manual"} task ${task.id}, ends at $endsAt"
        )
        return true
    }

    private fun scheduleNext(task: ScheduledTask, fromMillis: Long): ScheduledTask {
        val nextAt = TaskScheduleCalculator.nextExecuteAt(task, fromMillis)
        val status = if (nextAt == null && task.isOneShotSchedule()) {
            TaskStatus.COMPLETED
        } else {
            TaskStatus.PENDING
        }
        val updated = task.copy(
            status = status,
            nextExecuteAt = nextAt,
            completedAt = if (status == TaskStatus.COMPLETED) System.currentTimeMillis() else null,
            executionStartedAt = null,
            executionEndsAt = null,
            updatedAt = System.currentTimeMillis(),
        )
        store.saveTask(updated)
        nextAt?.let { scheduleOccurrence(updated, it) }
        return updated
    }

    private fun scheduleOccurrence(task: ScheduledTask, executeAt: Long) {
        if (!task.scheduleArmed) return
        val alarmAt = TaskScheduleCalculator.alarmAt(task, executeAt)
        if (alarmAt <= System.currentTimeMillis()) {
            handleAlarmTrigger(task.id)
            return
        }
        val pendingIntent = alarmPendingIntent(task.id)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmAt,
                    pendingIntent,
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmAt,
                    pendingIntent,
                )
                notifyExactAlarmPermissionMissing()
            }
            else -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmAt,
                    pendingIntent,
                )
            }
        }
        Log.i(TAG, "Scheduled ${task.id} at $alarmAt (task time $executeAt)")
    }

    private fun scheduleReceiverWake(taskId: String, triggerAt: Long) {
        val pendingIntent = alarmPendingIntent(taskId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
            notifyExactAlarmPermissionMissing()
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        }
        Log.i(TAG, "Deferred playback restore for $taskId to alarm at $triggerAt")
    }

    private fun completeExpiredOnce(task: ScheduledTask, now: Long) {
        store.saveTask(
            task.copy(
                status = TaskStatus.COMPLETED,
                completedAt = now,
                nextExecuteAt = null,
                executionStartedAt = null,
                executionEndsAt = null,
                updatedAt = now,
            )
        )
    }

    private fun hasPlayableAudio(task: ScheduledTask): Boolean =
        (selectedAlarmAudios(task) ?: task.audios).any { playableUrl(it) != null }

    private fun selectedAlarmAudios(task: ScheduledTask): List<TaskAudio>? {
        if (task.alarmAudioOrder.isEmpty()) return null
        val byKey = task.audios.associateBy { it.selectionKey() }
        return task.alarmAudioOrder.mapNotNull(byKey::get).takeIf { it.isNotEmpty() }
    }

    private fun playableUrl(audio: TaskAudio): String? =
        audio.localUri?.takeIf { it.isNotBlank() }
            ?: audio.serverUrl?.takeIf { it.isNotBlank() }

    private fun alarmPendingIntent(taskId: String): PendingIntent {
        val intent = alarmIntent(taskId, withIdentity = true)
        return PendingIntent.getBroadcast(
            appContext,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelPendingIntent(taskId: String) {
        listOf(
            alarmIntent(taskId, withIdentity = true),
            alarmIntent(taskId, withIdentity = false),
        ).forEach { intent ->
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun alarmIntent(taskId: String, withIdentity: Boolean): Intent =
        Intent(appContext, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ALARM_TRIGGER)
            .apply {
                if (withIdentity) {
                    data = Uri.Builder()
                        .scheme(ALARM_URI_SCHEME)
                        .authority(ALARM_URI_AUTHORITY)
                        .appendPath(taskId)
                        .build()
                }
            }
            .putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)

    private fun notifyExactAlarmPermissionMissing() {
        val settings = AppSettingsStore.get(appContext)
        if (
            !settings.getBoolean(AppSettingsStore.KEY_PUSH_ALL, true) ||
            !settings.getBoolean(AppSettingsStore.KEY_PUSH_RESERVATION, true)
        ) {
            return
        }
        val channel = android.app.NotificationChannel(
            WARNING_CHANNEL_ID,
            "定时播放提醒",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
        val settingsIntent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(android.net.Uri.parse("package:${appContext.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, WARNING_CHANNEL_ID)
            .setContentTitle("梦枕")
            .setContentText("定时播放需要精确闹钟权限，点击前往设置开启")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(WARNING_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val WARNING_CHANNEL_ID = "dream_pillow_alarm_warning"
        private const val WARNING_NOTIFICATION_ID = 2001
        private const val RESTORE_ALARM_DELAY_MS = 1_500L
        private const val ALARM_URI_SCHEME = "mengzhen"
        private const val ALARM_URI_AUTHORITY = "alarm"

        @Volatile
        private var instance: AlarmScheduler? = null

        fun get(context: Context): AlarmScheduler =
            instance ?: synchronized(this) {
                instance ?: AlarmScheduler(context.applicationContext).also { instance = it }
            }
    }
}

internal object AlarmRestoreContract {
    fun shouldRestore(status: TaskStatus, scheduleArmed: Boolean): Boolean =
        status == TaskStatus.EXECUTING || scheduleArmed
}
