package com.mengzhen.app.scheduler;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.mengzhen.app.audio.AudioPlaybackService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

@CapacitorPlugin(name = "AlarmScheduler")
public class AlarmSchedulerPlugin extends Plugin {

    private static final String TAG = "AlarmScheduler";

    @PluginMethod
    public void scheduleTask(PluginCall call) {
        String taskId = call.getString("taskId");
        String taskName = call.getString("taskName", "梦枕");
        Long triggerAt = call.getLong("triggerAt");
        Integer playDurationMinutes = call.getInt("playDurationMinutes", 30);
        Integer volume = call.getInt("volume", 70);
        Boolean enableFade = call.getBoolean("enableFade", false);
        Integer fadeInDuration = call.getInt("fadeInDuration", 0);
        Integer fadeOutDuration = call.getInt("fadeOutDuration", 0);
        String audioUrl = call.getString("audioUrl", "");
        String audioName = call.getString("audioName", "");

        if (taskId == null || triggerAt == null) {
            call.reject("taskId and triggerAt are required");
            return;
        }

        try {
            AlarmScheduler scheduler = AlarmScheduler.getInstance(getContext());
            scheduler.scheduleAlarm(taskId, taskName, triggerAt, playDurationMinutes,
                    volume, enableFade, fadeInDuration, fadeOutDuration,
                    audioUrl, audioName);
            Log.i(TAG, "Scheduled task: " + taskId + " at " + triggerAt);
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule task", e);
            call.reject("Failed to schedule: " + e.getMessage());
        }
    }

    @PluginMethod
    public void cancelTask(PluginCall call) {
        String taskId = call.getString("taskId");
        if (taskId == null) {
            call.reject("taskId is required");
            return;
        }
        try {
            AlarmScheduler scheduler = AlarmScheduler.getInstance(getContext());
            scheduler.cancelAlarm(taskId);
            Log.i(TAG, "Cancelled task: " + taskId);
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel task", e);
            call.reject("Failed to cancel: " + e.getMessage());
        }
    }

    @PluginMethod
    public void cancelAllTasks(PluginCall call) {
        try {
            AlarmScheduler scheduler = AlarmScheduler.getInstance(getContext());
            scheduler.cancelAll();
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to cancel all: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopPlayback(PluginCall call) {
        try {
            AudioPlaybackService.stopPlayback(getContext());
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to stop playback: " + e.getMessage());
        }
    }

    @PluginMethod
    public void isPlaying(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("playing", AudioPlaybackService.isCurrentlyPlaying());
        call.resolve(ret);
    }
}
