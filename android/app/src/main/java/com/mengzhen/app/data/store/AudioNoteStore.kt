package com.mengzhen.app.data.store

import android.content.Context
import com.mengzhen.app.data.model.TaskAudio
import org.json.JSONObject

/**
 * Device-local notes keyed by the durable identity of an audio item.
 *
 * Notes deliberately live beside, rather than inside, scheduled-task JSON. The same audio can
 * appear in the draft, several scheduled tasks and the active playback queue; one identity-keyed
 * record keeps those references consistent without rewriting every task when a note changes.
 */
class AudioNoteStore private constructor(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun get(audio: TaskAudio): String {
        val key = audio.audioNoteIdentity() ?: return ""
        return readAll().optString(key, "")
    }

    /** Saves a note, or removes it when the editor contains only whitespace. */
    @Synchronized
    fun save(audio: TaskAudio, content: String): String {
        val key = audio.audioNoteIdentity() ?: return ""
        val notes = readAll()
        if (content.isBlank()) {
            notes.remove(key)
        } else {
            notes.put(key, content)
        }
        preferences.edit().putString(NOTES_KEY, notes.toString()).apply()
        return if (content.isBlank()) "" else content
    }

    private fun readAll(): JSONObject {
        val raw = preferences.getString(NOTES_KEY, null) ?: return JSONObject()
        return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }

    companion object {
        private const val PREFERENCES_NAME = "dream_pillow_audio_notes"
        private const val NOTES_KEY = "notes"

        @Volatile
        private var instance: AudioNoteStore? = null

        fun get(context: Context): AudioNoteStore =
            instance ?: synchronized(this) {
                instance ?: AudioNoteStore(context).also { instance = it }
            }
    }
}

internal fun TaskAudio.audioNoteIdentity(): String? = when {
    id.isNotBlank() -> "id:$id"
    !sourceType.isNullOrBlank() && !sourceId.isNullOrBlank() ->
        "source:$sourceType:$sourceId"
    !fileKey.isNullOrBlank() -> "file:$fileKey"
    !localUri.isNullOrBlank() -> "local:$localUri"
    !serverUrl.isNullOrBlank() -> "url:$serverUrl"
    !dbKey.isNullOrBlank() -> "db:$dbKey"
    name.isNotBlank() -> "metadata:$name:$duration:$size"
    else -> null
}
