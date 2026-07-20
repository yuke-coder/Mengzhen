package com.mengzhen.app.data.store

import android.content.Context
import android.content.SharedPreferences
import com.mengzhen.app.data.model.*
import org.json.JSONArray
import org.json.JSONObject

class TaskStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dream_pillow", Context.MODE_PRIVATE)

    private val tasksKey = "dream_pillow_tasks"
    private val modeKey = "dream_pillow_mode"
    private val draftKey = "dream_pillow_draft"

    // === Play Mode ===
    fun getPlayMode(): PlayMode {
        val raw = prefs.getString(modeKey, "default") ?: "default"
        return if (raw == "custom") PlayMode.CUSTOM else PlayMode.DEFAULT
    }

    fun setPlayMode(mode: PlayMode) {
        prefs.edit().putString(modeKey, if (mode == PlayMode.CUSTOM) "custom" else "default").apply()
    }

    // === Tasks ===
    fun getAllTasks(): List<ScheduledTask> {
        val raw = prefs.getString(tasksKey, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                arr.getJSONObject(i).toScheduledTask()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAllTasks(tasks: List<ScheduledTask>) {
        val arr = JSONArray()
        tasks.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(tasksKey, arr.toString()).apply()
    }

    fun getTaskById(id: String): ScheduledTask? =
        getAllTasks().find { it.id == id }

    fun createTask(data: ScheduledTask): ScheduledTask {
        val tasks = getAllTasks().toMutableList()
        tasks.add(data)
        saveAllTasks(tasks)
        return data
    }

    fun updateTask(id: String, updates: Map<String, Any?>): ScheduledTask? {
        val tasks = getAllTasks().toMutableList()
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx == -1) return null

        val current = tasks[idx]
        val updated = current.copy(
            name = updates["name"] as? String ?: current.name,
            playDurationMinutes = (updates["playDurationMinutes"] as? Int) ?: current.playDurationMinutes,
            volume = (updates["volume"] as? Int) ?: current.volume,
            enableFade = (updates["enableFade"] as? Boolean) ?: current.enableFade,
            fadeInDuration = (updates["fadeInDuration"] as? Int) ?: current.fadeInDuration,
            fadeOutDuration = (updates["fadeOutDuration"] as? Int) ?: current.fadeOutDuration,
            status = (updates["status"] as? TaskStatus) ?: current.status,
            skipUntil = updates["skipUntil"] as? Long ?: current.skipUntil,
            updatedAt = System.currentTimeMillis(),
        )
        tasks[idx] = updated
        saveAllTasks(tasks)
        return updated
    }

    fun deleteTask(id: String): Boolean {
        val tasks = getAllTasks().filter { it.id != id }
        val before = getAllTasks().size
        if (tasks.size < before) {
            saveAllTasks(tasks)
            return true
        }
        return false
    }

    fun cancelTask(id: String): ScheduledTask? =
        updateTask(id, mapOf("status" to TaskStatus.CANCELLED))

    fun cleanupCompletedOnce(): Int {
        val tasks = getAllTasks()
        val remaining = tasks.filter { !(it.repeatType == TaskRepeatType.ONCE && it.status == TaskStatus.COMPLETED) }
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

    // === Playback Draft ===
    fun getDraft(): PlaybackDraft {
        val raw = prefs.getString(draftKey, null) ?: return PlaybackDraft()
        return try {
            val json = JSONObject(raw)
            val arr = json.optJSONArray("audios") ?: JSONArray()
            val audios = (0 until arr.length()).map { i ->
                val a = arr.getJSONObject(i)
                TaskAudio(
                    id = a.optString("id"),
                    name = a.optString("name"),
                    duration = a.optLong("duration"),
                    size = a.optLong("size"),
                    fileKey = a.optString("fileKey", "").ifEmpty { null },
                    serverUrl = a.optString("serverUrl", "").ifEmpty { null },
                )
            }
            PlaybackDraft(
                audios = audios,
                volume = json.optInt("volume", 70),
                fadeInDuration = json.optInt("fadeInDuration", 0),
                fadeOutDuration = json.optInt("fadeOutDuration", 0),
                enableFade = json.optBoolean("enableFade", false),
            )
        } catch (e: Exception) {
            PlaybackDraft()
        }
    }

    fun saveDraft(draft: PlaybackDraft) {
        val json = JSONObject()
        val arr = JSONArray()
        draft.audios.forEach { audio ->
            val a = JSONObject()
            a.put("id", audio.id)
            a.put("name", audio.name)
            a.put("duration", audio.duration)
            a.put("size", audio.size)
            audio.fileKey?.let { a.put("fileKey", it) }
            audio.serverUrl?.let { a.put("serverUrl", it) }
            arr.put(a)
        }
        json.put("audios", arr)
        json.put("volume", draft.volume)
        json.put("fadeInDuration", draft.fadeInDuration)
        json.put("fadeOutDuration", draft.fadeOutDuration)
        json.put("enableFade", draft.enableFade)
        prefs.edit().putString(draftKey, json.toString()).apply()
    }

    // === Auth ===
    fun saveUserSession(token: String, user: UserInfo) {
        val json = JSONObject()
        json.put("token", token)
        json.put("userId", user.id)
        json.put("username", user.username)
        json.put("email", user.email)
        user.avatarUrl?.let { json.put("avatarUrl", it) }
        user.nickname?.let { json.put("nickname", it) }
        prefs.edit().putString("session", json.toString()).apply()
    }

    fun getSession(): Pair<String, UserInfo>? {
        val raw = prefs.getString("session", null) ?: return null
        return try {
            val json = JSONObject(raw)
            val user = UserInfo(
                id = json.optString("userId"),
                username = json.optString("username"),
                email = json.optString("email"),
                avatarUrl = json.optString("avatarUrl", "").ifEmpty { null },
                nickname = json.optString("nickname", "").ifEmpty { null },
            )
            json.optString("token") to user
        } catch (e: Exception) {
            null
        }
    }

    fun clearSession() {
        prefs.edit().remove("session").apply()
    }

    fun generateTaskId(): String =
        "task_" + System.currentTimeMillis().toString(36) + "_" + (0..9999).random().toString(36)

    companion object {
        @Volatile private var instance: TaskStore? = null
        fun get(context: Context): TaskStore =
            instance ?: synchronized(this) {
                instance ?: TaskStore(context.applicationContext).also { instance = it }
            }
    }
}
