package com.mengzhen.app.data.api

import android.content.Context
import android.net.Uri
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.store.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface AudioUploadState {
    data object Queued : AudioUploadState
    data class Uploading(val progress: Int) : AudioUploadState
    data class Success(val audio: TaskAudio) : AudioUploadState
    data class Failed(val message: String) : AudioUploadState
}

/**
 * Process-level upload queue independent of any Compose destination lifecycle.
 *
 * Selection is persisted before enqueueing, so navigating straight to the player
 * cannot cancel an upload. A successful upload is merged into both the current
 * draft and already-created task sessions without replacing their localUri.
 */
class AudioUploadQueue private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private val uploader = AudioUploader(ApiClient.get(applicationContext))
    private val store = TaskStore.get(applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _states =
        MutableStateFlow<Map<String, AudioUploadState>>(emptyMap())
    val states: StateFlow<Map<String, AudioUploadState>> = _states.asStateFlow()

    /**
     * @return true when a new upload was queued, false for invalid or already-active input.
     */
    fun enqueue(audio: TaskAudio): Boolean {
        val source = audio.localUri
        if (source.isNullOrBlank()) {
            setState(audio.id, AudioUploadState.Failed("本地音频地址不可用"))
            return false
        }

        synchronized(this) {
            when (_states.value[audio.id]) {
                AudioUploadState.Queued,
                is AudioUploadState.Uploading -> return false
                else -> setState(audio.id, AudioUploadState.Queued)
            }
        }

        scope.launch {
            setState(audio.id, AudioUploadState.Uploading(0))
            val result = uploader.upload(
                context = applicationContext,
                fileUri = source.toPlayableUri(),
                fileName = audio.name,
                fileSize = audio.size,
                mimeType = audio.mimeType ?: "audio/mpeg",
                onProgress = { progress ->
                    setState(audio.id, AudioUploadState.Uploading(progress))
                },
            )
            when (result) {
                is UploadResult.Success -> {
                    val persisted = store.mergeUploadedAudio(
                        audioId = audio.id,
                        serverUrl = result.audioUrl,
                        fileKey = result.fileKey,
                        fileName = result.fileName,
                        fileSize = result.fileSize,
                    )
                    val merged = persisted ?: audio.copy(
                        name = result.fileName.ifBlank { audio.name },
                        size = result.fileSize.takeIf { it > 0 } ?: audio.size,
                        fileKey = result.fileKey.ifBlank { audio.fileKey },
                        serverUrl = result.audioUrl.ifBlank { audio.serverUrl },
                    )
                    setState(audio.id, AudioUploadState.Success(merged))
                }

                is UploadResult.Failed -> {
                    setState(audio.id, AudioUploadState.Failed(result.message))
                }
            }
        }
        return true
    }

    fun clear(audioId: String) {
        _states.update { it - audioId }
    }

    private fun setState(audioId: String, state: AudioUploadState) {
        _states.update { it + (audioId to state) }
    }

    private fun String.toPlayableUri(): Uri =
        if (startsWith("/")) Uri.fromFile(File(this)) else Uri.parse(this)

    companion object {
        @Volatile
        private var instance: AudioUploadQueue? = null

        fun get(context: Context): AudioUploadQueue =
            instance ?: synchronized(this) {
                instance ?: AudioUploadQueue(context).also { instance = it }
            }
    }
}
