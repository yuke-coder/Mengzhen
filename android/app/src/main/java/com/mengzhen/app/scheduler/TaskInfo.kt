package com.mengzhen.app.scheduler

import org.json.JSONObject

/**
 * 任务数据模型
 * repeatDays: 重复天数（位掩码），bit 0 = 周日, bit 1 = 周一, ..., bit 6 = 周六
 * 0 = 不重复（一次性任务）
 * 对标喜马拉雅 AlarmRecord.reapeatDays + DaysOfWeek
 */
data class TaskInfo(
    var taskId: String = "",
    var taskName: String = "梦枕",
    var triggerAt: Long = 0,
    var playDurationMinutes: Int = 30,
    var volume: Int = 70,
    var enableFade: Boolean = false,
    var fadeInDuration: Int = 0,
    var fadeOutDuration: Int = 0,
    var audioUrl: String = "",
    var audioName: String = "",
    var tracksJson: String = "",
    var loopSingle: Boolean = true,
    var endTime: Long = 0,
    var repeatDays: Int = 0, // 位掩码：bit0=周日, bit1=周一, ..., bit6=周六
)

fun TaskInfo.toJson(): JSONObject = JSONObject().apply {
    put("taskId", taskId)
    put("taskName", taskName)
    put("triggerAt", triggerAt)
    put("playDurationMinutes", playDurationMinutes)
    put("volume", volume)
    put("enableFade", enableFade)
    put("fadeInDuration", fadeInDuration)
    put("fadeOutDuration", fadeOutDuration)
    put("audioUrl", audioUrl)
    put("audioName", audioName)
    put("tracksJson", tracksJson)
    put("loopSingle", loopSingle)
    put("endTime", endTime)
    put("repeatDays", repeatDays)
}

fun taskInfoFromJson(obj: JSONObject): TaskInfo = TaskInfo(
    taskId = obj.optString("taskId", ""),
    taskName = obj.optString("taskName", "梦枕"),
    triggerAt = obj.optLong("triggerAt", 0),
    playDurationMinutes = obj.optInt("playDurationMinutes", 30),
    volume = obj.optInt("volume", 70),
    enableFade = obj.optBoolean("enableFade", false),
    fadeInDuration = obj.optInt("fadeInDuration", 0),
    fadeOutDuration = obj.optInt("fadeOutDuration", 0),
    audioUrl = obj.optString("audioUrl", ""),
    audioName = obj.optString("audioName", ""),
    tracksJson = obj.optString("tracksJson", ""),
    loopSingle = obj.optBoolean("loopSingle", true),
    endTime = obj.optLong("endTime", 0),
    repeatDays = obj.optInt("repeatDays", 0),
)
