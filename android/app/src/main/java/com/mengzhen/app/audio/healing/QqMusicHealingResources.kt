package com.mengzhen.app.audio.healing

import android.content.Context
import com.tencent.qqmusic.mediaplayer.codec.OggToWaveDecoder
import com.tencent.qqmusic.supersound.SuperSoundJni
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object QqMusicHealingResources {
    private const val RESOURCE_VERSION = "6642789"
    private const val RESOURCE_URL =
        "https://dlied5sdk.myapp.com/music/release/upload/t_mm_file_publish/6642789.zip"
    private val prepareMutex = Mutex()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow<PreparationState>(PreparationState.Idle)

    val state: StateFlow<PreparationState> = mutableState.asStateFlow()

    fun prepareAsync(context: Context) {
        val applicationContext = context.applicationContext
        applicationScope.launch { runCatching { prepare(applicationContext) } }
    }

    suspend fun prepare(context: Context): File = withContext(Dispatchers.IO) {
        prepareMutex.withLock {
            val root = resourceRoot(context)
            if (isReady(root)) {
                mutableState.value = PreparationState.Ready(root)
                return@withLock root
            }

            runCatching {
                root.mkdirs()
                val archive = File(context.cacheDir, "qqmusic_healing_$RESOURCE_VERSION.zip")
                download(archive)
                unzip(archive, root)
                decodeResources(root)
                if (!isReady(root)) error("助眠资源校验失败")
                archive.delete()
                mutableState.value = PreparationState.Ready(root)
                root
            }.getOrElse { error ->
                mutableState.value = PreparationState.Failed(
                    error.message ?: "助眠资源准备失败",
                )
                throw error
            }
        }
    }

    fun prepareBlocking(context: Context): File = runBlocking { prepare(context) }

    fun readyRoot(context: Context): File? = resourceRoot(context).takeIf(::isReady)

    private fun resourceRoot(context: Context): File =
        File(context.filesDir, "qqmusic/healing/$RESOURCE_VERSION")

    private fun isReady(root: File): Boolean =
        root.isDirectory && SuperSoundJni.ss_bs_check_resource(root.pathWithSeparator())

    private fun download(destination: File) {
        if (destination.isFile && destination.length() > 0L) return
        val partial = File(destination.parentFile, destination.name + ".part")
        partial.delete()
        val connection = URL(RESOURCE_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("助眠资源下载失败：HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong.coerceAtLeast(1L)
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        mutableState.value = PreparationState.Preparing(
                            progress = (copied.toFloat() / total * .25f).coerceIn(0f, .25f),
                            message = "正在下载助眠资源",
                        )
                    }
                }
            }
            if (!partial.renameTo(destination)) error("助眠资源保存失败")
        } finally {
            connection.disconnect()
            if (!destination.exists()) partial.delete()
        }
    }

    private fun unzip(archive: File, root: File) {
        val canonicalRoot = root.canonicalFile
        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = File(root, entry.name).canonicalFile
                if (!target.path.startsWith(canonicalRoot.path + File.separator)) {
                    error("助眠资源路径无效")
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output -> zip.copyTo(output, 64 * 1024) }
                }
                zip.closeEntry()
            }
        }
        mutableState.value = PreparationState.Preparing(.3f, "正在展开助眠资源")
    }

    private fun decodeResources(root: File) {
        val sources = root.walkTopDown()
            .filter { it.isFile && it.extension.equals("ogg", ignoreCase = true) }
            .toList()
        if (sources.isEmpty()) error("助眠资源包不完整")
        sources.forEachIndexed { index, source ->
            val target = File(source.parentFile, source.nameWithoutExtension + ".wav")
            if (!target.isFile || target.length() <= 44L) {
                if (!OggToWaveDecoder.decode(source, target)) {
                    error("助眠音色解码失败：${source.name}")
                }
            }
            mutableState.value = PreparationState.Preparing(
                progress = .3f + ((index + 1f) / sources.size * .69f),
                message = "正在准备助眠音色 ${index + 1}/${sources.size}",
            )
        }
    }

    private fun File.pathWithSeparator(): String =
        absolutePath.trimEnd(File.separatorChar) + File.separator
}

sealed interface PreparationState {
    data object Idle : PreparationState
    data class Preparing(val progress: Float, val message: String) : PreparationState
    data class Ready(val root: File) : PreparationState
    data class Failed(val message: String) : PreparationState
}
