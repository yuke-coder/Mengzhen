package com.mengzhen.app.data.model

import org.json.JSONObject

/**
 * 重复类型 - 对标 Web 端 task-types.ts
 * ONCE: 一次性
 * DAILY: 每天
 * WORKDAY: 工作日
 * HOLIDAY: 节假日
 */
enum class TaskRepeatType { ONCE, WORKDAY, HOLIDAY, DAILY }
enum class TaskStatus { PENDING, EXECUTING, COMPLETED, CANCELLED }
enum class PlayMode { DEFAULT, CUSTOM }

/**
 * 音频文件 - 对标 Web 端 audios 表
 */
data class TaskAudio(
    val id: String = "",
    val name: String = "",
    val duration: Long = 0,
    val size: Long = 0,
    val fileKey: String? = null,
    val serverUrl: String? = null,
    val dbKey: String? = null,
    val savedToLibrary: Boolean = false,
    val mimeType: String? = null,
)

/**
 * 定时播放任务 - 对标 Web 端 ScheduledTask
 */
data class ScheduledTask(
    val id: String = "",
    val name: String = "",
    val startTime: TaskStartTime = TaskStartTime(),
    val playDurationMinutes: Int = 30,
    val fadeInDuration: Int = 0,
    val fadeOutDuration: Int = 0,
    val enableFade: Boolean = false,
    val volume: Int = 70,
    val repeatType: TaskRepeatType = TaskRepeatType.ONCE,
    val audios: List<TaskAudio> = emptyList(),
    val status: TaskStatus = TaskStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val lastExecutedAt: Long? = null,
    val nextExecuteAt: Long? = null,
    val completedAt: Long? = null,
    val skipUntil: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

data class TaskStartTime(
    val year: Int = 2026,
    val month: Int = 1,
    val day: Int = 1,
    val hour: Int = 8,
    val minute: Int = 0,
    val second: Int = 0,
) {
    fun toEpochMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, day, hour, minute, second)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

/**
 * 播放草稿 - 编辑中的播放配置
 */
data class PlaybackDraft(
    val audios: List<TaskAudio> = emptyList(),
    val volume: Int = 70,
    val fadeInDuration: Int = 0,
    val fadeOutDuration: Int = 0,
    val enableFade: Boolean = false,
)

/**
 * 用户信息 - 对标 Web API /api/auth/me + /api/profile 响应
 */
data class UserInfo(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val nickname: String? = null,
    val gender: String? = null,
    val birthday: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val signature: String? = null,
    val createdAt: String = "",
)

// === JSON 序列化 ===

fun ScheduledTask.toJson(): JSONObject {
    val json = JSONObject()
    json.put("id", id)
    json.put("name", name)
    val st = JSONObject()
    st.put("year", startTime.year)
    st.put("month", startTime.month)
    st.put("day", startTime.day)
    st.put("hour", startTime.hour)
    st.put("minute", startTime.minute)
    st.put("second", startTime.second)
    json.put("startTime", st)
    json.put("playDurationMinutes", playDurationMinutes)
    json.put("fadeInDuration", fadeInDuration)
    json.put("fadeOutDuration", fadeOutDuration)
    json.put("enableFade", enableFade)
    json.put("volume", volume)
    json.put("repeatType", repeatType.name.lowercase())
    json.put("status", status.name.lowercase())
    json.put("createdAt", createdAt)
    json.put("updatedAt", updatedAt)
    lastExecutedAt?.let { json.put("lastExecutedAt", it) }
    nextExecuteAt?.let { json.put("nextExecuteAt", it) }
    completedAt?.let { json.put("completedAt", it) }
    skipUntil?.let { json.put("skipUntil", it) }

    val arr = org.json.JSONArray()
    audios.forEach { audio ->
        val a = JSONObject()
        a.put("id", audio.id)
        a.put("name", audio.name)
        a.put("duration", audio.duration)
        a.put("size", audio.size)
        audio.fileKey?.let { a.put("fileKey", it) }
        audio.serverUrl?.let { a.put("serverUrl", it) }
        audio.dbKey?.let { a.put("dbKey", it) }
        a.put("savedToLibrary", audio.savedToLibrary)
        audio.mimeType?.let { a.put("mimeType", it) }
        arr.put(a)
    }
    json.put("audios", arr)
    return json
}

fun JSONObject.toScheduledTask(): ScheduledTask {
    val st = optJSONObject("startTime")
    return ScheduledTask(
        id = optString("id"),
        name = optString("name"),
        startTime = st?.let {
            TaskStartTime(
                year = it.optInt("year"),
                month = it.optInt("month"),
                day = it.optInt("day"),
                hour = it.optInt("hour"),
                minute = it.optInt("minute"),
                second = it.optInt("second"),
            )
        } ?: TaskStartTime(),
        playDurationMinutes = optInt("playDurationMinutes", 30),
        fadeInDuration = optInt("fadeInDuration", 0),
        fadeOutDuration = optInt("fadeOutDuration", 0),
        enableFade = optBoolean("enableFade", false),
        volume = optInt("volume", 70),
        repeatType = when (optString("repeatType", "once")) {
            "workday" -> TaskRepeatType.WORKDAY
            "holiday" -> TaskRepeatType.HOLIDAY
            "daily" -> TaskRepeatType.DAILY
            else -> TaskRepeatType.ONCE
        },
        audios = run {
            val arr = optJSONArray("audios") ?: return@run emptyList()
            (0 until arr.length()).map { i ->
                val a = arr.getJSONObject(i)
                TaskAudio(
                    id = a.optString("id"),
                    name = a.optString("name"),
                    duration = a.optLong("duration"),
                    size = a.optLong("size"),
                    fileKey = a.optString("fileKey", "").ifEmpty { null },
                    serverUrl = a.optString("serverUrl", "").ifEmpty { null },
                    dbKey = a.optString("dbKey", "").ifEmpty { null },
                    savedToLibrary = a.optBoolean("savedToLibrary", false),
                    mimeType = a.optString("mimeType", "").ifEmpty { null },
                )
            }
        },
        status = when (optString("status", "pending")) {
            "executing" -> TaskStatus.EXECUTING
            "completed" -> TaskStatus.COMPLETED
            "cancelled" -> TaskStatus.CANCELLED
            else -> TaskStatus.PENDING
        },
        createdAt = optLong("createdAt"),
        lastExecutedAt = if (has("lastExecutedAt")) optLong("lastExecutedAt") else null,
        nextExecuteAt = if (has("nextExecuteAt")) optLong("nextExecuteAt") else null,
        completedAt = if (has("completedAt")) optLong("completedAt") else null,
        skipUntil = if (has("skipUntil")) optLong("skipUntil") else null,
        updatedAt = optLong("updatedAt"),
    )
}

/**
 * 从 Web API /api/audio/my-list 响应解析音频列表
 */
fun parseAudioList(json: JSONObject): List<TaskAudio> {
    val arr = json.optJSONArray("audios") ?: return emptyList()
    return (0 until arr.length()).map { i ->
        val a = arr.getJSONObject(i)
        TaskAudio(
            id = a.optString("id"),
            name = a.optString("title", a.optString("file_name", "")),
            duration = a.optLong("duration", 0),
            size = a.optLong("file_size", 0),
            fileKey = a.optString("file_key", "").ifEmpty { null },
            serverUrl = a.optString("file_url", "").ifEmpty { null },
            savedToLibrary = true,
            mimeType = a.optString("mime_type", "").ifEmpty { null },
        )
    }
}

/**
 * 从 Web API /api/auth/me 响应解析用户信息
 */
fun parseUser(json: JSONObject): UserInfo? {
    if (!json.optBoolean("authenticated", false)) return null
    val user = json.optJSONObject("user") ?: return null
    return UserInfo(
        id = user.optString("id"),
        username = user.optString("username"),
        email = "",
        avatarUrl = user.optString("avatar_url", "").ifEmpty { null },
        nickname = user.optString("nickname", "").ifEmpty { null },
        gender = user.optString("gender", "").ifEmpty { null },
        birthday = user.optString("birthday", "").ifEmpty { null },
        location = user.optString("location", "").ifEmpty { null },
        bio = user.optString("bio", "").ifEmpty { null },
        signature = user.optString("signature", "").ifEmpty { null },
        createdAt = user.optString("created_at", ""),
    )
}

/**
 * 从 Web API /api/profile 响应解析用户资料
 */
fun parseProfile(json: JSONObject): UserInfo? {
    if (!json.optBoolean("success", false)) return null
    val profile = json.optJSONObject("profile") ?: return null
    return UserInfo(
        id = profile.optString("id"),
        username = profile.optString("username"),
        email = "",
        avatarUrl = profile.optString("avatar_url", "").ifEmpty { null },
        nickname = profile.optString("nickname", "").ifEmpty { null },
        gender = profile.optString("gender", "").ifEmpty { null },
        birthday = profile.optString("birthday", "").ifEmpty { null },
        location = profile.optString("location", "").ifEmpty { null },
        bio = profile.optString("bio", "").ifEmpty { null },
        signature = profile.optString("signature", "").ifEmpty { null },
        createdAt = profile.optString("createdAt", ""),
    )
}
