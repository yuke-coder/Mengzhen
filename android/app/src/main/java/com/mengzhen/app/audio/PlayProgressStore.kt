package com.mengzhen.app.audio

import android.content.Context
import android.util.Log
import com.mengzhen.app.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 播放进度存储 - 本地 SharedPreferences + 云端同步
 *
 * 本地：每秒保存播放进度，播放时 seekTo 恢复，7天过期清除
 * 云端：通过 Web API /api/playback/progress 同步，支持跨设备恢复
 *
 * 对标喜马拉雅断点续播：本地缓存 + 云端同步
 */
class PlayProgressStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("dream_pillow_progress", Context.MODE_PRIVATE)
    private val api = ApiClient.get()

    /**
     * 保存播放进度到本地
     * @param audioId 音频标识（fileKey 或 URL）
     * @param positionSeconds 当前播放位置（秒）
     * @param durationSeconds 总时长（秒）
     */
    fun saveLocal(audioId: String, positionSeconds: Long, durationSeconds: Long) {
        val json = JSONObject()
        json.put("position", positionSeconds)
        json.put("duration", durationSeconds)
        json.put("timestamp", System.currentTimeMillis())
        prefs.edit().putString(audioId, json.toString()).apply()
    }

    /**
     * 读取本地播放进度
     * @return Pair(positionSeconds, durationSeconds) 或 null
     */
    fun getLocal(audioId: String): Pair<Long, Long>? {
        val raw = prefs.getString(audioId, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val timestamp = json.optLong("timestamp", 0)
            // 7天过期
            if (System.currentTimeMillis() - timestamp > 7 * 24 * 60 * 60 * 1000L) {
                prefs.edit().remove(audioId).apply()
                return null
            }
            Pair(json.optLong("position", 0), json.optLong("duration", 0))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 上报播放进度到云端
     * 在 IO 协程中调用
     */
    suspend fun saveToCloud(audioId: String, positionSeconds: Long, durationSeconds: Long) {
        withContext(Dispatchers.IO) {
            try {
                val result = api.savePlaybackProgress(audioId, positionSeconds, durationSeconds)
                if (!result.optBoolean("success", false)) {
                    Log.w(TAG, "Cloud sync failed: ${result.optString("error")}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync failed", e)
            }
        }
    }

    /**
     * 从云端拉取所有播放进度，合并到本地
     * 应用启动时调用
     */
    suspend fun syncFromCloud() {
        withContext(Dispatchers.IO) {
            try {
                val result = api.getPlaybackProgress()
                if (!result.optBoolean("success", false)) {
                    Log.w(TAG, "Cloud sync fetch failed: ${result.optString("error")}")
                    return@withContext
                }
                val arr = result.optJSONArray("progress") ?: return@withContext
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val audioId = item.optString("audio_id")
                    val position = item.optLong("position_seconds", 0)
                    val duration = item.optLong("duration_seconds", 0)
                    val updatedAt = item.optString("updated_at", "")
                    if (audioId.isNotEmpty() && position > 0) {
                        // 只在本地没有或云端更新时覆盖
                        val local = getLocal(audioId)
                        if (local == null || local.first < position) {
                            saveLocal(audioId, position, duration)
                        }
                    }
                }
                Log.i(TAG, "Cloud sync complete: ${arr.length()} records")
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync failed", e)
            }
        }
    }

    /**
     * 删除播放进度（本地 + 云端）
     */
    suspend fun delete(audioId: String) {
        prefs.edit().remove(audioId).apply()
        withContext(Dispatchers.IO) {
            try {
                api.deletePlaybackProgress(audioId)
            } catch (e: Exception) {
                Log.w(TAG, "Cloud delete failed", e)
            }
        }
    }

    /**
     * 清理过期的本地进度
     */
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val keys = prefs.all.keys
        for (key in keys) {
            val raw = prefs.getString(key, null) ?: continue
            try {
                val json = JSONObject(raw)
                val timestamp = json.optLong("timestamp", 0)
                if (now - timestamp > 7 * 24 * 60 * 60 * 1000L) {
                    prefs.edit().remove(key).apply()
                }
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val TAG = "PlayProgressStore"

        @Volatile private var instance: PlayProgressStore? = null
        fun get(context: Context): PlayProgressStore =
            instance ?: synchronized(this) {
                instance ?: PlayProgressStore(context.applicationContext).also { instance = it }
            }
    }
}
