package com.mengzhen.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.mengzhen.app.scheduler.AlarmScheduler;
import com.mengzhen.app.scheduler.TaskInfo;
import com.mengzhen.app.scheduler.TaskStorage;
import com.mengzhen.app.audio.AudioPlaybackService;

/**
 * AlarmManager 触发时的接收器
 * 息屏状态下也能被唤醒
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received broadcast: " + action);

        if ("com.mengzhen.app.ALARM_TRIGGER".equals(action)) {
            String taskId = intent.getStringExtra("taskId");
            if (taskId == null) return;

            TaskStorage storage = TaskStorage.getInstance(context);
            TaskInfo task = storage.getTask(taskId);
            if (task == null) {
                Log.w(TAG, "Task not found: " + taskId);
                return;
            }

            // 启动 Foreground Service 播放音频
            Intent serviceIntent = new Intent(context, AudioPlaybackService.class);
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

            Log.i(TAG, "Started playback service for task: " + taskId);
        }
    }
}
