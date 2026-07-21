package com.mengzhen.app.scheduler

import org.json.JSONObject

/**
 * 任务数据模型
 * repeatType: 重复类型 (0=一次性, 1=每天, 2=法定工作日, 3=法定节假日)
 *   对标 Web 端 task-types.ts 的 TaskRepeatType
 *   对标喜马拉雅 e.java AlarmRecord.reapeatDays（喜马拉雅用位掩码，Web 端用枚举类型，Android 端跟随 Web 端）
 * repeatDays: 旧位掩码字段（保留兼容，优先用 repeatType）
 * status: 任务状态 - 对标 Web 端 task-types.ts 的 TaskStatus
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
    var repeatDays: Int = 0, // 旧字段，保留兼容
    var repeatType: Int = 0, // 0=一次性, 1=每天, 2=法定工作日, 3=法定节假日
    var coverUrl: String = "", // 封面图 URL
    var status: String = "pending", // pending|executing|completed|cancelled
    var lastExecutedAt: Long = 0, // 上次执行时间
    var skipUntil: Long = 0, // 跳过直到此时间
    var createdAt: Long = 0, // 创建时间
    var updatedAt: Long = 0, // 更新时间
    var completedAt: Long = 0, // 完成时间
    var nextExecuteAt: Long = 0, // 下次执行时间
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
    put("repeatType", repeatType)
    put("coverUrl", coverUrl)
    put("status", status)
    put("lastExecutedAt", lastExecutedAt)
    put("skipUntil", skipUntil)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("completedAt", completedAt)
    put("nextExecuteAt", nextExecuteAt)
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
    repeatType = obj.optInt("repeatType", 0),
    coverUrl = obj.optString("coverUrl", ""),
    status = obj.optString("status", "pending"),
    lastExecutedAt = obj.optLong("lastExecutedAt", 0),
    skipUntil = obj.optLong("skipUntil", 0),
    createdAt = obj.optLong("createdAt", 0),
    updatedAt = obj.optLong("updatedAt", 0),
    completedAt = obj.optLong("completedAt", 0),
    nextExecuteAt = obj.optLong("nextExecuteAt", 0),
)
