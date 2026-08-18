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
enum class TaskPhase { WAITING, FADING_IN, PLAYING, FADING_OUT, IDLE }
enum class PlayMode { DEFAULT, CUSTOM }
enum class ScheduledStopMode { NONE, MINUTES, TRACKS }

/**
 * 音频文件 - 对标 Web 端 audios 表
 */
data class TaskAudio(
    val id: String = "",
    val name: String = "",
    val duration: Long = 0,
    val size: Long = 0,
    /**
     * Durable device-side source used for immediate/offline playback.
     *
     * This is intentionally kept even after [serverUrl] becomes available: an upload
     * completing in the background must never make the selected local file unplayable.
     */
    val localUri: String? = null,
    val fileKey: String? = null,
    val serverUrl: String? = null,
    val dbKey: String? = null,
    val savedToLibrary: Boolean = false,
    val mimeType: String? = null,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    val createdAt: String = "",
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
    val enableFadeOut: Boolean = false,
    val volume: Int = 70,
    val repeatType: TaskRepeatType = TaskRepeatType.ONCE,
    /**
     * 喜马拉雅 AlarmRecord.reapeatDays 的原始位掩码：
     * 周一至周日依次为 1、2、4、8、16、32、64；0 表示仅执行一次。
     *
     * null 表示旧任务继续使用 [repeatType]，从而保持已有数据兼容；
     * 非 null 时调度器以此字段为准，完整支持自定义星期组合。
     */
    val repeatDays: Int? = null,
    val audios: List<TaskAudio> = emptyList(),
    /** 喜马拉雅“跳过片头片尾”，对当前听单内的全部音频生效。 */
    val skipHeadSeconds: Int = 0,
    val skipTailSeconds: Int = 0,
    /**
     * Alarm.buildPlayingAlarm() is represented by null. A concrete index selects
     * one of the task's already chosen audios, matching AlarmRingSettingFragmentNew
     * without introducing a second file-picker entrance.
     */
    val alarmAudioIndex: Int? = null,
    /** 定时启播选择的音频键，列表顺序就是播放顺序；空列表保持旧版续播行为。 */
    val alarmAudioOrder: List<String> = emptyList(),
    /** 定时启播开始后是否、以及如何自动停止；NONE 表示按所选顺序自然播完。 */
    val scheduledStopMode: ScheduledStopMode = ScheduledStopMode.NONE,
    /** MINUTES 对应分钟数，TRACKS 对应播完的音频数。 */
    val scheduledStopValue: Int = 0,
    /**
     * 时间关闭的精确播放时长。旧任务未保存该字段时继续使用
     * [scheduledStopValue] 分钟；由结束时刻反算时可保留秒级精度。
     */
    val scheduledStopDurationSeconds: Int = 0,
    /** 按时间停止到点后，是否等待当前音频自然播完。 */
    val scheduledFinishCurrentTrack: Boolean = false,
    /**
     * False for a newly opened, manually playable session. Legacy persisted tasks did
     * not contain this field and therefore deserialize as armed for compatibility.
     */
    val scheduleArmed: Boolean = true,
    val status: TaskStatus = TaskStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val lastExecutedAt: Long? = null,
    val nextExecuteAt: Long? = null,
    val completedAt: Long? = null,
    val skipUntil: Long? = null,
    val executionStartedAt: Long? = null,
    val executionEndsAt: Long? = null,
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
    val enableFadeOut: Boolean = false,
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
    /** 喜马拉雅个人页的背景图；本地文件和服务端 URL 均可。 */
    val backgroundUrl: String? = null,
    val createdAt: String = "",
    /** API /api/profile 返回的绑定手机号。 */
    val mobile: String? = null,
    /** API /api/profile 返回的用户名修改次数。 */
    val usernameChangeCount: Int? = null,
    /** API /api/profile 返回的用户名修改次数重置时间。 */
    val usernameChangeResetAt: String? = null,
)

// === JSON 序列化 ===

fun TaskAudio.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("duration", duration)
    put("size", size)
    localUri?.let { put("localUri", it) }
    fileKey?.let { put("fileKey", it) }
    serverUrl?.let { put("serverUrl", it) }
    dbKey?.let { put("dbKey", it) }
    put("savedToLibrary", savedToLibrary)
    mimeType?.let { put("mimeType", it) }
    sourceType?.let { put("sourceType", it) }
    sourceId?.let { put("sourceId", it) }
    artist?.let { put("artist", it) }
    artworkUri?.let { put("artworkUri", it) }
    put("createdAt", createdAt)
}

fun JSONObject.toTaskAudio(): TaskAudio = TaskAudio(
    id = optString("id"),
    name = optString("name"),
    duration = optLong("duration"),
    size = optLong("size"),
    localUri = optString("localUri", "").ifEmpty { null },
    fileKey = optString("fileKey", "").ifEmpty { null },
    serverUrl = optString("serverUrl", "").ifEmpty { null },
    dbKey = optString("dbKey", "").ifEmpty { null },
    savedToLibrary = optBoolean("savedToLibrary", false),
    mimeType = optString("mimeType", "").ifEmpty { null },
    sourceType = optString("sourceType", "").ifEmpty { null },
    sourceId = optString("sourceId", "").ifEmpty { null },
    artist = optString("artist", "").ifEmpty { null },
    artworkUri = optString("artworkUri", "").ifEmpty { null },
    createdAt = optString("createdAt", ""),
)

fun TaskAudio.selectionKey(): String =
    id.takeIf(String::isNotBlank)
        ?: fileKey?.takeIf(String::isNotBlank)
        ?: localUri?.takeIf(String::isNotBlank)
        ?: serverUrl?.takeIf(String::isNotBlank)
        ?: dbKey?.takeIf(String::isNotBlank)
        ?: name

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
    json.put("enableFadeOut", enableFadeOut)
    json.put("volume", volume)
    json.put("repeatType", repeatType.name.lowercase())
    repeatDays?.let { json.put("repeatDays", it) }
    json.put("skipHeadSeconds", skipHeadSeconds.coerceIn(0, 120))
    json.put("skipTailSeconds", skipTailSeconds.coerceIn(0, 120))
    alarmAudioIndex?.let { json.put("alarmAudioIndex", it) }
    json.put(
        "alarmAudioOrder",
        org.json.JSONArray().apply { alarmAudioOrder.forEach { key -> put(key) } },
    )
    json.put("scheduledStopMode", scheduledStopMode.name.lowercase())
    json.put("scheduledStopValue", scheduledStopValue.coerceAtLeast(0))
    json.put("scheduledStopDurationSeconds", scheduledStopDurationSeconds.coerceAtLeast(0))
    json.put("scheduledFinishCurrentTrack", scheduledFinishCurrentTrack)
    json.put("scheduleArmed", scheduleArmed)
    json.put("status", status.name.lowercase())
    json.put("createdAt", createdAt)
    json.put("updatedAt", updatedAt)
    lastExecutedAt?.let { json.put("lastExecutedAt", it) }
    nextExecuteAt?.let { json.put("nextExecuteAt", it) }
    completedAt?.let { json.put("completedAt", it) }
    skipUntil?.let { json.put("skipUntil", it) }
    executionStartedAt?.let { json.put("executionStartedAt", it) }
    executionEndsAt?.let { json.put("executionEndsAt", it) }

    val arr = org.json.JSONArray()
    audios.forEach { audio -> arr.put(audio.toJson()) }
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
        enableFadeOut = if (has("enableFadeOut")) {
            optBoolean("enableFadeOut", false)
        } else {
            optInt("fadeOutDuration", 0) > 0
        },
        volume = optInt("volume", 70),
        repeatType = when (optString("repeatType", "once")) {
            "workday" -> TaskRepeatType.WORKDAY
            "holiday" -> TaskRepeatType.HOLIDAY
            "daily" -> TaskRepeatType.DAILY
            else -> TaskRepeatType.ONCE
        },
        repeatDays = if (has("repeatDays")) optInt("repeatDays").coerceIn(0, 127) else null,
        skipHeadSeconds = optInt("skipHeadSeconds", 0).coerceIn(0, 120),
        skipTailSeconds = optInt("skipTailSeconds", 0).coerceIn(0, 120),
        alarmAudioIndex = if (has("alarmAudioIndex")) {
            optInt("alarmAudioIndex").coerceAtLeast(0)
        } else {
            null
        },
        alarmAudioOrder = run {
            val arr = optJSONArray("alarmAudioOrder") ?: return@run emptyList()
            (0 until arr.length()).mapNotNull { index ->
                arr.optString(index).takeIf(String::isNotBlank)
            }
        },
        scheduledStopMode = when (optString("scheduledStopMode", "none")) {
            "minutes" -> ScheduledStopMode.MINUTES
            "tracks" -> ScheduledStopMode.TRACKS
            else -> ScheduledStopMode.NONE
        },
        scheduledStopValue = optInt("scheduledStopValue", 0).coerceAtLeast(0),
        scheduledStopDurationSeconds = optInt(
            "scheduledStopDurationSeconds",
            0,
        ).coerceAtLeast(0),
        scheduledFinishCurrentTrack = optBoolean("scheduledFinishCurrentTrack", false),
        audios = run {
            val arr = optJSONArray("audios") ?: return@run emptyList()
            (0 until arr.length()).map { i -> arr.getJSONObject(i).toTaskAudio() }
        },
        scheduleArmed = optBoolean("scheduleArmed", true),
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
        executionStartedAt = if (has("executionStartedAt")) optLong("executionStartedAt") else null,
        executionEndsAt = if (has("executionEndsAt")) optLong("executionEndsAt") else null,
        updatedAt = optLong("updatedAt"),
    )
}

fun ScheduledTask.isOneShotSchedule(): Boolean =
    repeatDays?.let { it == 0 } ?: (repeatType == TaskRepeatType.ONCE)

fun ScheduledTask.hasActiveSchedule(): Boolean =
    scheduleArmed && status != TaskStatus.CANCELLED && status != TaskStatus.COMPLETED

fun ScheduledTask.effectiveScheduledStopDurationSeconds(): Int =
    if (scheduledStopMode == ScheduledStopMode.MINUTES) {
        scheduledStopDurationSeconds.takeIf { it > 0 }
            ?: scheduledStopValue.coerceAtLeast(0) * 60
    } else {
        0
    }

fun ScheduledTask.hasConfiguredStop(): Boolean = when (scheduledStopMode) {
    ScheduledStopMode.MINUTES -> effectiveScheduledStopDurationSeconds() > 0
    ScheduledStopMode.TRACKS -> scheduledStopValue > 0
    ScheduledStopMode.NONE -> false
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
            createdAt = a.optString("created_at", ""),
        )
    }
}

/**
 * 从 Web API /api/auth/me 响应解析用户信息
 */
fun parseUser(json: JSONObject): UserInfo? {
    val authenticated = json.optBoolean("authenticated", false) ||
        (json.optBoolean("success", false) && json.has("user"))
    if (!authenticated) return null
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
        bio = user.optString("bio", "").ifEmpty { null }?.takeIf { it != "null" },
        signature = user.optString("signature", "").ifEmpty { null },
        backgroundUrl = user.optString("background_url", "").ifEmpty { null },
        createdAt = user.optString("createdAt", user.optString("created_at", "")),
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
        bio = profile.optString("bio", "").ifEmpty { null }?.takeIf { it != "null" },
        signature = profile.optString("signature", "").ifEmpty { null },
        backgroundUrl = profile.optString("background_url", "").ifEmpty { null },
        createdAt = profile.optString("createdAt", ""),
        usernameChangeCount = if (profile.has("username_change_count")) profile.optInt("username_change_count") else null,
        usernameChangeResetAt = profile.optString("username_change_reset_at", "").ifEmpty { null },
    )
}
