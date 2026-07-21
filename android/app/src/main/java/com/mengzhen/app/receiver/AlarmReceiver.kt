package com.mengzhen.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.scheduler.AlarmScheduler
import com.mengzhen.app.scheduler.TaskStorage

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Received broadcast: $action")

        if (action != ACTION_ALARM_TRIGGER) return

        val taskId = intent.getStringExtra("taskId") ?: return
        val task = TaskStorage.get(context).getTask(taskId)
        if (task == null) {
            Log.w(TAG, "Task not found: $taskId")
            return
        }

        val serviceIntent = Intent(context, AudioPlaybackService::class.java)
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
            .putExtra("coverUrl", task.coverUrl)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        Log.i(TAG, "Started playback service for task: $taskId")

        // 标记任务为执行中
        task.status = "executing"
        task.lastExecutedAt = System.currentTimeMillis()
        TaskStorage.get(context).saveTask(task)

        // 如果是重复任务，触发后立即设置下次闹钟
        // 对标喜马拉雅 e.java c() 方法：闹钟触发后重新计算下次时间
        // repeatType 优先，旧 repeatDays 兼容
        if (task.repeatType != 0 || task.repeatDays != 0) {
            AlarmScheduler.get(context).rescheduleIfRepeating(taskId)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_ALARM_TRIGGER = "com.mengzhen.app.ALARM_TRIGGER"
    }
}
