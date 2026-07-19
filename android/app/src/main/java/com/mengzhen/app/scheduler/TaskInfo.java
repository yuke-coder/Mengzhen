package com.mengzhen.app.scheduler;

import org.json.JSONObject;

public class TaskInfo {
    public String taskId;
    public String taskName;
    public long triggerAt;
    public int playDurationMinutes;
    public int volume;
    public boolean enableFade;
    public int fadeInDuration;
    public int fadeOutDuration;
    public String audioUrl;   // 兼容旧接口
    public String audioName;  // 兼容旧接口
    // 新增：播放列表 JSON 和循环模式
    public String tracksJson;
    public boolean loopSingle;
    public long endTime; // 0 = 使用 playDurationMinutes 计算

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("taskId", taskId);
            obj.put("taskName", taskName);
            obj.put("triggerAt", triggerAt);
            obj.put("playDurationMinutes", playDurationMinutes);
            obj.put("volume", volume);
            obj.put("enableFade", enableFade);
            obj.put("fadeInDuration", fadeInDuration);
            obj.put("fadeOutDuration", fadeOutDuration);
            obj.put("audioUrl", audioUrl);
            obj.put("audioName", audioName);
            obj.put("tracksJson", tracksJson != null ? tracksJson : "");
            obj.put("loopSingle", loopSingle);
            obj.put("endTime", endTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static TaskInfo fromJson(JSONObject obj) {
        TaskInfo task = new TaskInfo();
        task.taskId = obj.optString("taskId", "");
        task.taskName = obj.optString("taskName", "梦枕");
        task.triggerAt = obj.optLong("triggerAt", 0);
        task.playDurationMinutes = obj.optInt("playDurationMinutes", 30);
        task.volume = obj.optInt("volume", 70);
        task.enableFade = obj.optBoolean("enableFade", false);
        task.fadeInDuration = obj.optInt("fadeInDuration", 0);
        task.fadeOutDuration = obj.optInt("fadeOutDuration", 0);
        task.audioUrl = obj.optString("audioUrl", "");
        task.audioName = obj.optString("audioName", "");
        task.tracksJson = obj.optString("tracksJson", "");
        task.loopSingle = obj.optBoolean("loopSingle", true);
        task.endTime = obj.optLong("endTime", 0L);
        return task;
    }
}
