package com.mengzhen.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.receiver.AlarmReceiver
import java.util.Calendar

/**
 * AlarmManager 定时调度 - 对标喜马拉雅 host/manager/alarm/e.java
 *
 * 喜马拉雅原版 AlarmRecordManager：
 * - AlarmRecord 有 reapeatDays（DaysOfWeek 位掩码）
 * - c() 方法遍历所有闹钟记录找最近的下一个触发时间
 * - a(j, pendingIntent) 根据 API level 选 setExactAndAllowWhileIdle / setExact / set
 * - 闹钟触发后如果有重复天数，自动计算下次触发时间并重新设置
 */
class AlarmScheduler private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val storage = TaskStorage.get(appContext)
    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

    fun scheduleAlarm(
        taskId: String,
        taskName: String,
        triggerAt: Long,
        playDurationMinutes: Int,
        volume: Int,
        enableFade: Boolean,
        fadeInDuration: Int,
        fadeOutDuration: Int,
        audioUrl: String,
        audioName: String,
        tracksJson: String,
        loopSingle: Boolean,
        endTime: Long,
        repeatDays: Int = 0,
    ) {
        val task = TaskInfo(
            taskId = taskId,
            taskName = taskName,
            triggerAt = triggerAt,
            playDurationMinutes = playDurationMinutes,
            volume = volume,
            enableFade = enableFade,
            fadeInDuration = fadeInDuration,
            fadeOutDuration = fadeOutDuration,
            audioUrl = audioUrl,
            audioName = audioName,
            tracksJson = tracksJson,
            loopSingle = loopSingle,
            endTime = endTime,
            repeatDays = repeatDays,
        )
        storage.saveTask(task)
        setAlarm(task)
    }

    /**
     * 设置闹钟 - 对标喜马拉雅 e.java a(j, pendingIntent)
     * 根据 API level 选择最可靠的闹钟类型
     */
    private fun setAlarm(task: TaskInfo) {
        val intent = Intent(appContext, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ALARM_TRIGGER)
            .putExtra("taskId", task.taskId)

        val requestCode = task.taskId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            appContext, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.triggerAt, pendingIntent)
                Log.i(TAG, "Set exact alarm for ${task.taskId} at ${task.triggerAt}")
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // 不允许精确闹钟 - 降级为非精确闹钟 + 通知用户
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.triggerAt, pendingIntent)
                Log.w(TAG, "Exact alarm not permitted, using inexact for ${task.taskId}")
                notifyExactAlarmPermissionMissing()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.triggerAt, pendingIntent)
                Log.i(TAG, "Set exact alarm for ${task.taskId} at ${task.triggerAt}")
            }
            else -> {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.triggerAt, pendingIntent)
            }
        }
    }

    /**
     * 闹钟触发后，如果有重复天数，计算并设置下次闹钟
     * 对标喜马拉雅 e.java c() 方法：遍历所有闹钟记录找最近的下一个触发时间
     */
    fun rescheduleIfRepeating(taskId: String) {
        val task = storage.getTask(taskId) ?: return
        if (task.repeatDays == 0) {
            // 不重复 - 删除一次性任务
            storage.removeTask(taskId)
            Log.i(TAG, "One-shot task $taskId completed, removed")
            return
        }

        // 计算下次触发时间 - 对标喜马拉雅 c.a(hour, minute, DaysOfWeek)
        val nextTrigger = calculateNextTrigger(task.repeatDays, task.triggerAt)
        if (nextTrigger > 0) {
            task.triggerAt = nextTrigger
            storage.saveTask(task)
            setAlarm(task)
            Log.i(TAG, "Rescheduled repeating task $taskId for $nextTrigger")
        } else {
            Log.w(TAG, "Could not calculate next trigger for $taskId")
        }
    }

    /**
     * 根据重复天数位掩码计算下次触发时间
     * 位掩码：bit 0 = 周日, bit 1 = 周一, ..., bit 6 = 周六
     * 对标喜马拉雅 DaysOfWeek.getNextAlarmAfter()
     */
    private fun calculateNextTrigger(repeatDays: Int, originalTriggerAt: Long): Long {
        if (repeatDays == 0) return 0

        // 从原始触发时间提取时分
        val cal = Calendar.getInstance().apply { timeInMillis = originalTriggerAt }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        // 从明天开始找下一个匹配的星期几
        val now = Calendar.getInstance()
        for (i in 1..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY=1 -> bit 0
            if ((repeatDays and (1 shl dayOfWeek)) != 0 && candidate.timeInMillis > System.currentTimeMillis()) {
                return candidate.timeInMillis
            }
        }
        return 0
    }

    fun cancelAlarm(taskId: String) {
        val intent = Intent(appContext, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ALARM_TRIGGER)
            .putExtra("taskId", taskId)

        val requestCode = taskId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            appContext, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        storage.removeTask(taskId)
        Log.i(TAG, "Cancelled alarm for $taskId")
    }

    fun cancelAll() {
        storage.getAllTasks().forEach { cancelAlarm(it.taskId) }
    }

    /**
     * 恢复所有闹钟 - 对标喜马拉雅 e.java e() 方法从本地存储恢复
     * 开机后调用
     */
    fun restoreAllAlarms() {
        val now = System.currentTimeMillis()
        storage.getAllTasks().forEach { task ->
            if (task.repeatDays != 0) {
                // 重复任务 - 重新计算下次触发时间
                val nextTrigger = calculateNextTrigger(task.repeatDays, task.triggerAt)
                if (nextTrigger > 0) {
                    task.triggerAt = nextTrigger
                    storage.saveTask(task)
                    setAlarm(task)
                    Log.i(TAG, "Restored repeating alarm for ${task.taskId}")
                }
            } else {
                // 一次性任务
                when {
                    task.triggerAt > now -> {
                        setAlarm(task)
                        Log.i(TAG, "Restored alarm for ${task.taskId}")
                    }
                    task.triggerAt > now - 60000 -> triggerTaskNow(task)
                    else -> storage.removeTask(task.taskId)
                }
            }
        }
    }

    private fun triggerTaskNow(task: TaskInfo) {
        val intent = Intent(appContext, AudioPlaybackService::class.java)
            .setAction(AudioPlaybackService.ACTION_START)
            .putExtra("taskId", task.taskId)
            .putExtra("taskName", task.taskName)
            .putExtra("playDurationMinutes", task.playDurationMinutes)
            .putExtra("volume", task.volume)
            .putExtra("enableFade", task.enableFade)
            .putExtra("fadeInDuration", task.fadeInDuration)
            .putExtra("fadeOutDuration", task.fadeOutDuration)
            .putExtra("audioUrl", task.audioUrl)
            .putExtra("audioName", task.audioName)
            .putExtra("tracksJson", task.tracksJson)
            .putExtra("loopSingle", task.loopSingle)
            .putExtra("endTime", task.endTime)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    /** 精确闹钟权限缺失通知 - 引导用户去设置开启 */
    private fun notifyExactAlarmPermissionMissing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val channel = android.app.NotificationChannel(
            "dream_pillow_alarm_warning",
            "定时播放提醒",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(android.net.Uri.parse("package:${appContext.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(appContext, "dream_pillow_alarm_warning")
            .setContentTitle("梦枕")
            .setContentText("定时播放需要精确闹钟权限，点击前往设置开启")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(2001, notification)
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        @Volatile private var instance: AlarmScheduler? = null
        fun get(context: Context): AlarmScheduler =
            instance ?: synchronized(this) {
                instance ?: AlarmScheduler(context.applicationContext).also { instance = it }
            }
    }
}
