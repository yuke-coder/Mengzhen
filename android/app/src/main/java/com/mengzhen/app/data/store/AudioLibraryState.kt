package com.mengzhen.app.data.store

import android.content.Context
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.TaskAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioLibraryState {
    suspend fun setFavorite(
        context: Context,
        taskId: String,
        audio: TaskAudio,
        favorite: Boolean,
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val fileKey = audio.fileKey?.takeIf(String::isNotBlank)
            if (fileKey != null && TaskStore.get(context).getSession() != null) {
                val response = if (favorite) {
                    ApiClient.get(context).saveToLibrary(fileKey)
                } else {
                    ApiClient.get(context).removeFromLibrary(fileKey)
                }
                check(response.optBoolean("success", false)) {
                    response.optString("error").ifBlank { "更新收藏状态失败" }
                }
            }

            val identity = identityOf(audio)
            checkNotNull(TaskStore.get(context).updateTask(taskId) { task ->
                task.copy(
                    audios = task.audios.map { candidate ->
                        if (identityOf(candidate) == identity) {
                            candidate.copy(savedToLibrary = favorite)
                        } else {
                            candidate
                        }
                    },
                )
            }) { "当前任务不存在" }
        }
    }

    private fun identityOf(audio: TaskAudio): String =
        audio.id.takeIf(String::isNotBlank)
            ?: audio.fileKey?.takeIf(String::isNotBlank)
            ?: audio.localUri?.takeIf(String::isNotBlank)
            ?: audio.serverUrl?.takeIf(String::isNotBlank)
            ?: audio.dbKey.orEmpty()
}
