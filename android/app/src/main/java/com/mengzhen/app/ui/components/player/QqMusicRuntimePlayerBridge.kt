package com.mengzhen.app.ui.components.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.TaskAudio
import com.tencent.qqmusic.business.playernew.view.NewPlayerActivity
import com.tencent.qqmusic.business.playernew.interactor.playerstyle.i3
import com.tencent.qqmusic.common.ipc.MusicProcess
import com.tencent.qqmusic.framework.ipc.toolbox.IPC
import com.tencent.qqmusiccommon.util.music.ExtraInfo
import com.tencent.qqmusicplayerprocess.servicenew.FromInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/** The irreducible host-to-QQ-Music queue bridge. Player UI and behavior stay in QQ's runtime. */
internal object QqMusicRuntimePlayerBridge {
    suspend fun newIntent(
        context: Context,
        audios: List<TaskAudio>,
        selectedIndex: Int,
    ): Intent = withContext(Dispatchers.IO) {
        val paths = audios.map { audio -> resolvePath(context, audio) }
        check(paths.isNotEmpty()) { "当前没有可播放的音频" }

        awaitPlayerProcess()
        // This is the same local-path entry point used by QQ Music's own
        // ApiMethodsImpl.playSongLocalPath: register the paths in the local
        // song table before handing the queue to the player process.
        val songs = MusicProcess.w().createSongInfoOfPaths(paths, true)
        check(songs.isNotEmpty()) { "QQ 音乐无法识别所选音频" }
        val index = selectedIndex.coerceIn(0, songs.lastIndex)
        com.tencent.qqmusiccommon.util.music.a.g0(songs)
            .e(ExtraInfo().from(FromInfo.FROM_API_AIDL.value))
            .k(index)
            .b()

        NewPlayerActivity.newIntent(
            context,
            NewPlayerActivity.SONG,
            false,
            i3.g(),
            NewPlayerActivity.FROM_MINIBAR,
            true,
            null,
        )
    }

    private suspend fun awaitPlayerProcess() {
        MusicProcess.p()
        if (MusicProcess.r()) return

        withTimeout(15_000L) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : IPC.IPCConnectListener {
                    override fun onConnected() {
                        MusicProcess.v(this)
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onDisconnected() = Unit
                }
                continuation.invokeOnCancellation { MusicProcess.v(listener) }
                MusicProcess.k(listener)
            }
        }
    }

    private fun resolvePath(context: Context, audio: TaskAudio): String {
        audio.localUri?.takeIf(String::isNotBlank)?.let { value ->
            val uri = Uri.parse(value)
            when (uri.scheme?.lowercase(Locale.ROOT)) {
                "file" -> uri.path?.let(::File)?.takeIf(File::isFile)?.let(File::getAbsolutePath)
                "content" -> copyContentUri(context, uri, audio)
                null -> File(value).takeIf(File::isFile)?.absolutePath
                else -> null
            }?.let { return it }
        }

        val url = audio.serverUrl
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: audio.fileKey?.takeIf(String::isNotBlank)?.let { fileKey ->
                val response = ApiClient.get(context).getSignedUrl(fileKey)
                response.optString("signedUrl").takeIf(String::isNotBlank)
                    ?: error(response.optString("message").ifBlank { "无法获取音频地址" })
            }
            ?: error("${audio.name.ifBlank { "所选音频" }}没有可用文件")

        val cached = AudioPlaybackService.cacheAudioUri(context, url)
            ?: error("${audio.name.ifBlank { "所选音频" }}下载失败")
        return Uri.parse(cached).path?.let(::File)?.takeIf(File::isFile)?.absolutePath
            ?: error("下载后的音频文件不可用")
    }

    private fun copyContentUri(context: Context, uri: Uri, audio: TaskAudio): String {
        val extension = audio.name.substringAfterLast('.', "")
            .takeIf { it.length in 2..5 }
            ?.let { ".$it" }
            .orEmpty()
        val targetDirectory = File(context.filesDir, "qqmusic_runtime_audio").apply { mkdirs() }
        val target = File(targetDirectory, "${uri.toString().hashCode().toUInt()}$extension")
        if (!target.isFile || target.length() == 0L) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use(input::copyTo)
            } ?: error("无法读取 ${audio.name.ifBlank { "所选音频" }}")
        }
        return target.absolutePath
    }
}
