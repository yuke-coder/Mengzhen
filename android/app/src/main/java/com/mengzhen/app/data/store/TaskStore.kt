package com.mengzhen.app.data.store

import android.content.Context
import android.content.SharedPreferences
import com.mengzhen.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class TaskStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dream_pillow", Context.MODE_PRIVATE)

    private val tasksKey = "dream_pillow_tasks"
    private val modeKey = "dream_pillow_mode"
    private val draftKey = "dream_pillow_draft"
    private val homeAvatarDestinationKey = "pref_key_side_center"

    private var cachedTasksRaw: String? = null
    private var cachedTasks: List<ScheduledTask> = emptyList()
    private var cachedDraftRaw: String? = null
    private var cachedDraft = PlaybackDraft()
    private val _sessionUser = MutableStateFlow(readSession()?.second)
    val sessionUser: StateFlow<UserInfo?> = _sessionUser.asStateFlow()

    // === Play Mode ===
    fun getPlayMode(): PlayMode {
        val raw = prefs.getString(modeKey, "default") ?: "default"
        return if (raw == "custom") PlayMode.CUSTOM else PlayMode.DEFAULT
    }

    fun setPlayMode(mode: PlayMode) {
        prefs.edit().putString(modeKey, if (mode == PlayMode.CUSTOM) "custom" else "default").apply()
    }

    fun getHomeAvatarDestination(): String =
        prefs.getString(homeAvatarDestinationKey, "profile") ?: "profile"

    fun setHomeAvatarDestination(destination: String) {
        prefs.edit().putString(homeAvatarDestinationKey, destination).apply()
    }

    // === Tasks ===
    @Synchronized
    fun getAllTasks(): List<ScheduledTask> {
        val raw = prefs.getString(tasksKey, null) ?: return emptyList()
        if (raw == cachedTasksRaw) return cachedTasks
        cachedTasks = try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                arr.getJSONObject(i).toScheduledTask()
            }
        } catch (e: Exception) {
            emptyList()
        }
        cachedTasksRaw = raw
        return cachedTasks
    }

    @Synchronized
    fun saveAllTasks(tasks: List<ScheduledTask>) {
        val arr = JSONArray()
        tasks.forEach { arr.put(it.toJson()) }
        val raw = arr.toString()
        cachedTasksRaw = raw
        cachedTasks = tasks.toList()
        prefs.edit().putString(tasksKey, raw).apply()
    }

    fun registerTasksChangedListener(
        onChanged: (List<ScheduledTask>) -> Unit,
    ): SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == tasksKey) onChanged(getAllTasks())
        }.also(prefs::registerOnSharedPreferenceChangeListener)

    fun unregisterTasksChangedListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    @Synchronized
    fun getTaskById(id: String): ScheduledTask? =
        getAllTasks().find { it.id == id }

    @Synchronized
    fun createTask(data: ScheduledTask): ScheduledTask {
        val tasks = getAllTasks().toMutableList()
        val existing = tasks.indexOfFirst { it.id == data.id }
        if (existing >= 0) tasks[existing] = data else tasks.add(data)
        saveAllTasks(tasks)
        return data
    }

    @Synchronized
    fun saveTask(task: ScheduledTask): ScheduledTask {
        val tasks = getAllTasks().toMutableList()
        val idx = tasks.indexOfFirst { it.id == task.id }
        if (idx >= 0) tasks[idx] = task else tasks.add(task)
        saveAllTasks(tasks)
        return task
    }

    @Synchronized
    fun updateTask(id: String, transform: (ScheduledTask) -> ScheduledTask): ScheduledTask? {
        val tasks = getAllTasks().toMutableList()
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx == -1) return null
        val updated = transform(tasks[idx]).copy(updatedAt = System.currentTimeMillis())
        tasks[idx] = updated
        saveAllTasks(tasks)
        return updated
    }

    @Synchronized
    fun updateTask(id: String, updates: Map<String, Any?>): ScheduledTask? {
        val tasks = getAllTasks().toMutableList()
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx == -1) return null

        val current = tasks[idx]
        val updated = current.copy(
            name = updates["name"] as? String ?: current.name,
            startTime = updates["startTime"] as? TaskStartTime ?: current.startTime,
            playDurationMinutes = (updates["playDurationMinutes"] as? Int) ?: current.playDurationMinutes,
            volume = (updates["volume"] as? Int) ?: current.volume,
            enableFade = (updates["enableFade"] as? Boolean) ?: current.enableFade,
            enableFadeOut = (updates["enableFadeOut"] as? Boolean) ?: current.enableFadeOut,
            fadeInDuration = (updates["fadeInDuration"] as? Int) ?: current.fadeInDuration,
            fadeOutDuration = (updates["fadeOutDuration"] as? Int) ?: current.fadeOutDuration,
            repeatType = (updates["repeatType"] as? TaskRepeatType) ?: current.repeatType,
            repeatDays = if (updates.containsKey("repeatDays")) {
                updates["repeatDays"] as? Int
            } else {
                current.repeatDays
            },
            audios = (updates["audios"] as? List<*>)?.filterIsInstance<TaskAudio>() ?: current.audios,
            skipHeadSeconds = (updates["skipHeadSeconds"] as? Int)
                ?.coerceIn(0, 120)
                ?: current.skipHeadSeconds,
            skipTailSeconds = (updates["skipTailSeconds"] as? Int)
                ?.coerceIn(0, 120)
                ?: current.skipTailSeconds,
            scheduledStopMode = (updates["scheduledStopMode"] as? ScheduledStopMode)
                ?: current.scheduledStopMode,
            scheduledStopValue = (updates["scheduledStopValue"] as? Int)
                ?.coerceAtLeast(0)
                ?: current.scheduledStopValue,
            scheduledStopDurationSeconds =
                (updates["scheduledStopDurationSeconds"] as? Int)
                    ?.coerceAtLeast(0)
                    ?: current.scheduledStopDurationSeconds,
            scheduledFinishCurrentTrack =
                (updates["scheduledFinishCurrentTrack"] as? Boolean)
                    ?: current.scheduledFinishCurrentTrack,
            scheduleArmed = (updates["scheduleArmed"] as? Boolean) ?: current.scheduleArmed,
            status = (updates["status"] as? TaskStatus) ?: current.status,
            lastExecutedAt = if (updates.containsKey("lastExecutedAt")) updates["lastExecutedAt"] as? Long else current.lastExecutedAt,
            nextExecuteAt = if (updates.containsKey("nextExecuteAt")) updates["nextExecuteAt"] as? Long else current.nextExecuteAt,
            completedAt = if (updates.containsKey("completedAt")) updates["completedAt"] as? Long else current.completedAt,
            skipUntil = if (updates.containsKey("skipUntil")) updates["skipUntil"] as? Long else current.skipUntil,
            executionStartedAt = if (updates.containsKey("executionStartedAt")) updates["executionStartedAt"] as? Long else current.executionStartedAt,
            executionEndsAt = if (updates.containsKey("executionEndsAt")) updates["executionEndsAt"] as? Long else current.executionEndsAt,
            updatedAt = System.currentTimeMillis(),
        )
        tasks[idx] = updated
        saveAllTasks(tasks)
        return updated
    }

    @Synchronized
    fun deleteTask(id: String): Boolean {
        val existing = getAllTasks()
        val tasks = existing.filter { it.id != id }
        val before = existing.size
        if (tasks.size < before) {
            saveAllTasks(tasks)
            return true
        }
        return false
    }

    @Synchronized
    fun cleanupTransientPlaybackSessions(activeTaskId: String? = null): Int {
        val tasks = getAllTasks()
        val remaining = tasks.filter { task ->
            !task.id.startsWith(TRANSIENT_PLAYBACK_PREFIX) ||
                task.id == activeTaskId ||
                task.hasActiveSchedule() ||
                task.status == TaskStatus.EXECUTING
        }
        val removed = tasks.size - remaining.size
        if (removed > 0) saveAllTasks(remaining)
        return removed
    }

    fun cancelTask(id: String): ScheduledTask? =
        updateTask(id, mapOf("status" to TaskStatus.CANCELLED))

    fun cleanupCompletedOnce(): Int {
        val tasks = getAllTasks()
        val remaining = tasks.filter { !(it.isOneShotSchedule() && it.status == TaskStatus.COMPLETED) }
        val removed = tasks.size - remaining.size
        if (removed > 0) saveAllTasks(remaining)
        return removed
    }

    fun cleanupCancelled(): Int {
        val tasks = getAllTasks()
        val remaining = tasks.filter { it.status != TaskStatus.CANCELLED }
        val removed = tasks.size - remaining.size
        if (removed > 0) saveAllTasks(remaining)
        return removed
    }

    @Synchronized
    fun clearExpiredSkips(now: Long = System.currentTimeMillis()): Int {
        val tasks = getAllTasks()
        var changed = 0
        val updated = tasks.map { task ->
            if (task.skipUntil != null && task.skipUntil <= now) {
                changed++
                task.copy(skipUntil = null, updatedAt = now)
            } else {
                task
            }
        }
        if (changed > 0) saveAllTasks(updated)
        return changed
    }

    // === Playback Draft ===
    @Synchronized
    fun getDraft(): PlaybackDraft {
        val raw = prefs.getString(draftKey, null) ?: return PlaybackDraft()
        if (raw == cachedDraftRaw) return cachedDraft
        cachedDraft = try {
            val json = JSONObject(raw)
            val arr = json.optJSONArray("audios") ?: JSONArray()
            val audios = (0 until arr.length()).map { i ->
                arr.getJSONObject(i).toTaskAudio()
            }
            PlaybackDraft(
                audios = audios,
                volume = json.optInt("volume", 70),
                fadeInDuration = json.optInt("fadeInDuration", 0),
                fadeOutDuration = json.optInt("fadeOutDuration", 0),
                enableFade = json.optBoolean("enableFade", false),
                enableFadeOut = if (json.has("enableFadeOut")) {
                    json.optBoolean("enableFadeOut", false)
                } else {
                    json.optInt("fadeOutDuration", 0) > 0
                },
            )
        } catch (e: Exception) {
            PlaybackDraft()
        }
        cachedDraftRaw = raw
        return cachedDraft
    }

    @Synchronized
    fun saveDraft(draft: PlaybackDraft) {
        val raw = draftToJson(draft).toString()
        cachedDraftRaw = raw
        cachedDraft = draft.copy(audios = draft.audios.toList())
        prefs.edit().putString(draftKey, raw).apply()
    }

    /**
     * Atomically reads, transforms and writes the playback draft.
     *
     * Upload completion can race with selection/deletion UI; keeping the whole
     * read-modify-write operation under the store monitor prevents a stale snapshot
     * from replacing newer audio entries or their durable local URIs.
     */
    @Synchronized
    fun updateDraft(transform: (PlaybackDraft) -> PlaybackDraft): PlaybackDraft {
        val updated = transform(getDraft())
        saveDraft(updated)
        return updated
    }

    /**
     * Merges cloud metadata into every persisted reference to an audio item.
     *
     * Both the draft and any already-created player/task sessions are committed by
     * one SharedPreferences editor while holding the store lock. Device-only fields
     * such as localUri are taken from the latest persisted object, never from the
     * older snapshot captured when the upload began.
     */
    @Synchronized
    fun mergeUploadedAudio(
        audioId: String,
        serverUrl: String,
        fileKey: String,
        fileName: String,
        fileSize: Long,
    ): TaskAudio? {
        var latest: TaskAudio? = null

        fun merge(audio: TaskAudio): TaskAudio {
            val merged = audio.copy(
                name = fileName.ifBlank { audio.name },
                size = fileSize.takeIf { it > 0 } ?: audio.size,
                fileKey = fileKey.ifBlank { audio.fileKey },
                serverUrl = serverUrl.ifBlank { audio.serverUrl },
            )
            latest = merged
            return merged
        }

        var tasksChanged = false
        val tasks = getAllTasks().map { task ->
            var taskChanged = false
            val audios = task.audios.map { audio ->
                if (audio.id == audioId) {
                    taskChanged = true
                    merge(audio)
                } else {
                    audio
                }
            }
            if (taskChanged) {
                tasksChanged = true
                task.copy(audios = audios, updatedAt = System.currentTimeMillis())
            } else {
                task
            }
        }

        val draft = getDraft()
        var draftChanged = false
        val draftAudios = draft.audios.map { audio ->
            if (audio.id == audioId) {
                draftChanged = true
                merge(audio)
            } else {
                audio
            }
        }
        val updatedDraft = if (draftChanged) draft.copy(audios = draftAudios) else draft

        if (tasksChanged || draftChanged) {
            val editor = prefs.edit()
            if (tasksChanged) {
                val arr = JSONArray()
                tasks.forEach { arr.put(it.toJson()) }
                editor.putString(tasksKey, arr.toString())
            }
            if (draftChanged) {
                editor.putString(draftKey, draftToJson(updatedDraft).toString())
            }
            editor.apply()
        }
        return latest
    }

    private fun draftToJson(draft: PlaybackDraft): JSONObject {
        val json = JSONObject()
        val arr = JSONArray()
        draft.audios.forEach { audio -> arr.put(audio.toJson()) }
        json.put("audios", arr)
        json.put("volume", draft.volume)
        json.put("fadeInDuration", draft.fadeInDuration)
        json.put("fadeOutDuration", draft.fadeOutDuration)
        json.put("enableFade", draft.enableFade)
        json.put("enableFadeOut", draft.enableFadeOut)
        return json
    }

    // === Auth ===
    @Synchronized
    fun saveUserSession(token: String, user: UserInfo) {
        val previous = _sessionUser.value
        val persistedUser = if (
            user.backgroundUrl == null && previous?.id == user.id
        ) {
            user.copy(backgroundUrl = previous.backgroundUrl)
        } else {
            user
        }
        val json = JSONObject()
        json.put("token", token)
        json.put("userId", persistedUser.id)
        json.put("username", persistedUser.username)
        json.put("email", persistedUser.email)
        persistedUser.avatarUrl?.let { json.put("avatarUrl", it) }
        persistedUser.nickname?.let { json.put("nickname", it) }
        persistedUser.gender?.let { json.put("gender", it) }
        persistedUser.birthday?.let { json.put("birthday", it) }
        persistedUser.location?.let { json.put("location", it) }
        persistedUser.bio?.let { json.put("bio", it) }
        persistedUser.signature?.let { json.put("signature", it) }
        persistedUser.backgroundUrl?.let { json.put("backgroundUrl", it) }
        if (persistedUser.createdAt.isNotEmpty()) json.put("createdAt", persistedUser.createdAt)
        prefs.edit().putString("session", json.toString()).apply()
        _sessionUser.value = persistedUser
    }

    @Synchronized
    fun getSession(): Pair<String, UserInfo>? = readSession()

    private fun readSession(): Pair<String, UserInfo>? {
        val raw = prefs.getString("session", null) ?: return null
        return try {
            val json = JSONObject(raw)
            val user = UserInfo(
                id = json.optString("userId"),
                username = json.optString("username"),
                email = json.optString("email"),
                avatarUrl = json.optString("avatarUrl", "").ifEmpty { null },
                nickname = json.optString("nickname", "").ifEmpty { null },
                gender = json.optString("gender", "").ifEmpty { null },
                birthday = json.optString("birthday", "").ifEmpty { null },
                location = json.optString("location", "").ifEmpty { null },
                bio = json.optString("bio", "").ifEmpty { null },
                signature = json.optString("signature", "").ifEmpty { null },
                backgroundUrl = json.optString("backgroundUrl", "").ifEmpty { null },
                createdAt = json.optString("createdAt", ""),
            )
            json.optString("token") to user
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun clearSession() {
        prefs.edit().remove("session").apply()
        _sessionUser.value = null
    }

    fun generateTaskId(): String =
        "task_" + System.currentTimeMillis().toString(36) + "_" + (0..9999).random().toString(36)

    companion object {
        private const val TRANSIENT_PLAYBACK_PREFIX = "quick_playback_"
        @Volatile private var instance: TaskStore? = null
        fun get(context: Context): TaskStore =
            instance ?: synchronized(this) {
                instance ?: TaskStore(context.applicationContext).also { instance = it }
            }
    }
}
