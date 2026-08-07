package com.mengzhen.app.audio

import android.content.Context
import com.mengzhen.app.data.model.TaskPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class PlaybackTransportState {
    IDLE,
    PREPARING,
    PLAYING,
    PAUSED,
    COMPLETED,
    STOPPED,
    ERROR,
}

data class PlaybackSnapshot(
    val taskId: String? = null,
    val taskName: String = "",
    val transportState: PlaybackTransportState = PlaybackTransportState.IDLE,
    val phase: TaskPhase = TaskPhase.IDLE,
    val trackIndex: Int = 0,
    val trackCount: Int = 0,
    val trackName: String = "",
    val nextTrackName: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val startedAt: Long = 0,
    val endsAt: Long = 0,
    val targetVolume: Int = 0,
    val message: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isTerminal: Boolean
        get() = transportState == PlaybackTransportState.COMPLETED ||
            transportState == PlaybackTransportState.STOPPED ||
            transportState == PlaybackTransportState.ERROR
}

/**
 * 播放器与 Compose 页面之间的轻量状态桥。
 *
 * 运行中的进度只保存在进程内；完成、停止和失败回执会持久化 24 小时，
 * 让 Activity 重建后仍能展示本次会话的终态。
 */
class PlaybackStateStore private constructor(context: Context) {

    private val prefs =
        context.getSharedPreferences("dream_pillow_monitor", Context.MODE_PRIVATE)
    private val _snapshot = MutableStateFlow(loadReceipt())

    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    fun begin(
        taskId: String?,
        taskName: String,
        trackNames: List<String>,
        startedAt: Long,
        endsAt: Long,
        targetVolume: Int,
        phase: TaskPhase,
    ) {
        prefs.edit().remove(RECEIPT_KEY).apply()
        _snapshot.value = PlaybackSnapshot(
            taskId = taskId,
            taskName = taskName,
            transportState = PlaybackTransportState.PREPARING,
            phase = phase,
            trackCount = trackNames.size,
            trackName = trackNames.firstOrNull().orEmpty(),
            nextTrackName = trackNames.getOrNull(1),
            startedAt = startedAt,
            endsAt = endsAt,
            targetVolume = targetVolume,
        )
    }

    @Synchronized
    fun update(
        transportState: PlaybackTransportState? = null,
        phase: TaskPhase? = null,
        trackIndex: Int? = null,
        trackCount: Int? = null,
        trackName: String? = null,
        nextTrackName: String? = null,
        replaceNextTrackName: Boolean = false,
        positionMs: Long? = null,
        durationMs: Long? = null,
        startedAt: Long? = null,
        endsAt: Long? = null,
        message: String? = null,
    ) {
        val current = _snapshot.value
        if (current.isTerminal) return
        _snapshot.value = current.copy(
            transportState = transportState ?: current.transportState,
            phase = phase ?: current.phase,
            trackIndex = trackIndex ?: current.trackIndex,
            trackCount = trackCount ?: current.trackCount,
            trackName = trackName ?: current.trackName,
            nextTrackName = if (replaceNextTrackName) nextTrackName else current.nextTrackName,
            positionMs = positionMs?.coerceAtLeast(0) ?: current.positionMs,
            durationMs = durationMs?.coerceAtLeast(0) ?: current.durationMs,
            startedAt = startedAt?.coerceAtLeast(0) ?: current.startedAt,
            endsAt = endsAt?.coerceAtLeast(0) ?: current.endsAt,
            message = message,
            updatedAt = System.currentTimeMillis(),
        )
    }

    @Synchronized
    fun finish(
        taskId: String?,
        transportState: PlaybackTransportState,
        message: String? = null,
    ) {
        require(
            transportState == PlaybackTransportState.COMPLETED ||
                transportState == PlaybackTransportState.STOPPED ||
                transportState == PlaybackTransportState.ERROR
        )
        val current = _snapshot.value
        val terminal = current.copy(
            taskId = taskId ?: current.taskId,
            transportState = transportState,
            phase = TaskPhase.IDLE,
            message = message,
            updatedAt = System.currentTimeMillis(),
        )
        _snapshot.value = terminal
        prefs.edit().putString(RECEIPT_KEY, terminal.toJson().toString()).apply()
    }

    private fun loadReceipt(): PlaybackSnapshot {
        val raw = prefs.getString(RECEIPT_KEY, null) ?: return PlaybackSnapshot()
        return try {
            val json = JSONObject(raw)
            val updatedAt = json.optLong("updatedAt", 0)
            if (System.currentTimeMillis() - updatedAt > RECEIPT_RETENTION_MS) {
                prefs.edit().remove(RECEIPT_KEY).apply()
                PlaybackSnapshot()
            } else {
                PlaybackSnapshot(
                    taskId = json.optString("taskId", "").ifEmpty { null },
                    taskName = json.optString("taskName"),
                    transportState = runCatching {
                        PlaybackTransportState.valueOf(json.optString("transportState"))
                    }.getOrDefault(PlaybackTransportState.IDLE),
                    phase = TaskPhase.IDLE,
                    trackIndex = json.optInt("trackIndex"),
                    trackCount = json.optInt("trackCount"),
                    trackName = json.optString("trackName"),
                    nextTrackName = json.optString("nextTrackName", "").ifEmpty { null },
                    positionMs = json.optLong("positionMs"),
                    durationMs = json.optLong("durationMs"),
                    startedAt = json.optLong("startedAt"),
                    endsAt = json.optLong("endsAt"),
                    targetVolume = json.optInt("targetVolume"),
                    message = json.optString("message", "").ifEmpty { null },
                    updatedAt = updatedAt,
                )
            }
        } catch (_: Exception) {
            prefs.edit().remove(RECEIPT_KEY).apply()
            PlaybackSnapshot()
        }
    }

    private fun PlaybackSnapshot.toJson(): JSONObject = JSONObject()
        .put("taskId", taskId)
        .put("taskName", taskName)
        .put("transportState", transportState.name)
        .put("trackIndex", trackIndex)
        .put("trackCount", trackCount)
        .put("trackName", trackName)
        .put("nextTrackName", nextTrackName)
        .put("positionMs", positionMs)
        .put("durationMs", durationMs)
        .put("startedAt", startedAt)
        .put("endsAt", endsAt)
        .put("targetVolume", targetVolume)
        .put("message", message)
        .put("updatedAt", updatedAt)

    companion object {
        private const val RECEIPT_KEY = "last_receipt"
        private const val RECEIPT_RETENTION_MS = 24L * 60L * 60L * 1_000L

        @Volatile
        private var instance: PlaybackStateStore? = null

        fun get(context: Context): PlaybackStateStore =
            instance ?: synchronized(this) {
                instance ?: PlaybackStateStore(context.applicationContext).also { instance = it }
            }
    }
}
