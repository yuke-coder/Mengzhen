package com.mengzhen.app.scheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.mengzhen.app.receiver.AlarmReceiver;
import com.mengzhen.app.scheduler.TaskInfo;
import com.mengzhen.app.scheduler.TaskStorage;

import java.util.List;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    private static AlarmScheduler instance;
    private final Context context;
    private final AlarmManager alarmManager;
    private final TaskStorage taskStorage;

    private AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.taskStorage = TaskStorage.getInstance(this.context);
    }

    public static synchronized AlarmScheduler getInstance(Context context) {
        if (instance == null) {
            instance = new AlarmScheduler(context);
        }
        return instance;
    }

    public void scheduleAlarm(String taskId, String taskName, long triggerAt,
                              int playDurationMinutes, int volume,
                              boolean enableFade, int fadeInDuration, int fadeOutDuration,
                              String audioUrl, String audioName) {

        TaskInfo task = new TaskInfo();
        task.taskId = taskId;
        task.taskName = taskName;
        task.triggerAt = triggerAt;
        task.playDurationMinutes = playDurationMinutes;
        task.volume = volume;
        task.enableFade = enableFade;
        task.fadeInDuration = fadeInDuration;
        task.fadeOutDuration = fadeOutDuration;
        task.audioUrl = audioUrl;
        task.audioName = audioName;

        taskStorage.saveTask(task);

        setAlarm(task);
    }

    private void setAlarm(TaskInfo task) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.mengzhen.app.ALARM_TRIGGER");
        intent.putExtra("taskId", task.taskId);

        int requestCode = task.taskId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 使用 setExactAndAllowWhileIdle 保证息屏也能触发
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查是否能使用精确闹钟
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, task.triggerAt, pendingIntent);
                    Log.i(TAG, "Set exact alarm for " + task.taskId + " at " + task.triggerAt);
                } else {
                    // 降级：用 inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, task.triggerAt, pendingIntent);
                    Log.w(TAG, "Exact alarm not permitted, using inexact for " + task.taskId);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, task.triggerAt, pendingIntent);
                Log.i(TAG, "Set exact alarm for " + task.taskId + " at " + task.triggerAt);
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.triggerAt, pendingIntent);
        }
    }

    public void cancelAlarm(String taskId) {
        // 取消 AlarmManager 定时器
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.mengzhen.app.ALARM_TRIGGER");
        int requestCode = taskId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);

        // 从存储中删除
        taskStorage.removeTask(taskId);

        Log.i(TAG, "Cancelled alarm for " + taskId);
    }

    public void cancelAll() {
        List<TaskInfo> tasks = taskStorage.getAllTasks();
        for (TaskInfo task : tasks) {
            cancelAlarm(task.taskId);
        }
    }

    /**
     * 设备重启后恢复所有闹钟
     */
    public void restoreAllAlarms() {
        List<TaskInfo> tasks = taskStorage.getAllTasks();
        long now = System.currentTimeMillis();
        for (TaskInfo task : tasks) {
            if (task.triggerAt > now) {
                setAlarm(task);
                Log.i(TAG, "Restored alarm for " + task.taskId);
            } else if (task.triggerAt > now - 60000) {
                // 刚过期 1 分钟内，立即触发
                triggerTaskNow(task);
            } else {
                // 过期太久，删除
                taskStorage.removeTask(task.taskId);
            }
        }
    }

    private void triggerTaskNow(TaskInfo task) {
        Intent serviceIntent = new Intent(context, com.mengzhen.app.audio.AudioPlaybackService.class);
        serviceIntent.setAction("com.mengzhen.app.START_PLAYBACK");
        serviceIntent.putExtra("taskId", task.taskId);
        serviceIntent.putExtra("taskName", task.taskName);
        serviceIntent.putExtra("playDurationMinutes", task.playDurationMinutes);
        serviceIntent.putExtra("volume", task.volume);
        serviceIntent.putExtra("enableFade", task.enableFade);
        serviceIntent.putExtra("fadeInDuration", task.fadeInDuration);
        serviceIntent.putExtra("fadeOutDuration", task.fadeOutDuration);
        serviceIntent.putExtra("audioUrl", task.audioUrl);
        serviceIntent.putExtra("audioName", task.audioName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
