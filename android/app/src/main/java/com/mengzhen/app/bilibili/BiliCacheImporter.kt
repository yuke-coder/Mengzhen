package com.mengzhen.app.bilibili

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.mengzhen.app.data.model.TaskAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.coroutineContext

data class BiliImportProgress(
    val current: Int,
    val total: Int,
    val title: String,
    val percent: Int?,
)

class BiliCacheImporter(
    context: Context,
    private val rootBridge: BiliRootBridge,
    private val shizukuBridge: BiliShizukuBridge,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val appContext = context.applicationContext

    suspend fun prepareArtwork(items: List<BiliCacheItem>): Map<String, String> =
        withContext(Dispatchers.IO) {
            val coverRoot = File(appContext.cacheDir, "bili_offline")
            items.mapNotNull { item ->
                if (item.coverLocation.isNullOrBlank()) return@mapNotNull null
                val suffix = item.id.hashCode().toUInt().toString(16)
                val cached = File(coverRoot, "covers/$suffix.jpg")
                    .takeIf { it.isFile && it.length() > 0L }
                    ?: copyArtwork(item, coverRoot, suffix)
                cached?.let { item.id to Uri.fromFile(it).toString() }
            }.toMap()
        }

    suspend fun import(
        items: List<BiliCacheItem>,
        onProgress: (BiliImportProgress) -> Unit = {},
    ): List<TaskAudio> = withContext(Dispatchers.IO) {
        items.mapIndexed { index, item ->
            importOne(item) { copied, total ->
                onProgress(
                    BiliImportProgress(
                        current = index + 1,
                        total = items.size,
                        title = item.displayTitle(),
                        percent = total.takeIf { it > 0 }?.let {
                            ((copied * 100L) / it).toInt().coerceIn(0, 100)
                        },
                    )
                )
            }
        }
    }

    private suspend fun importOne(
        item: BiliCacheItem,
        onBytes: (Long, Long) -> Unit,
    ): TaskAudio {
        if (!item.completed) throw IOException("「${item.displayTitle()}」尚未缓存完成")
        val audioDir = File(appContext.filesDir, "audio_imports/bili").apply {
            if (!exists() && !mkdirs()) throw IOException("无法创建 B 站音频目录")
        }
        val stableSuffix = item.id.hashCode().toUInt().toString(16)
        val destination = File(
            audioDir,
            "${safeBiliFileStem(item.displayTitle())}_$stableSuffix.m4a",
        )
        val partial = File(destination.parentFile, "${destination.name}.partial")
        if (partial.exists() && !partial.delete()) {
            throw IOException("无法清理上次未完成的转换")
        }
        if (item.audioSize > 0 && audioDir.usableSpace <= item.audioSize + FREE_SPACE_MARGIN) {
            throw IOException("存储空间不足，请至少保留 ${formatMegabytes(item.audioSize)}")
        }

        try {
            openSource(item, cover = false).use { source ->
                val input = BufferedInputStream(source.input, COPY_BUFFER_SIZE)
                input.mark(64)
                val header = ByteArray(32)
                val headerSize = input.read(header)
                input.reset()
                val skip = BiliM4sHeader.bytesToSkip(
                    if (headerSize > 0) header.copyOf(headerSize) else byteArrayOf(),
                )
                if (skip < 0) {
                    throw IOException("缓存音轨不是可识别的 M4S/M4A 文件")
                }
                var skipped = 0L
                while (skipped < skip) {
                    val count = input.skip(skip - skipped)
                    if (count <= 0) throw IOException("缓存音轨头部不完整")
                    skipped += count
                }
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    var copied = 0L
                    val expected = (source.length.takeIf { it > 0 } ?: item.audioSize)
                        .let { (it - skip).coerceAtLeast(0) }
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        onBytes(copied, expected)
                    }
                    output.fd.sync()
                }
            }
            validateAudioFile(partial)
            if (destination.exists() && !destination.delete()) {
                throw IOException("无法替换旧的转换结果")
            }
            if (!partial.renameTo(destination)) {
                throw IOException("无法提交转换结果")
            }
        } catch (error: Throwable) {
            partial.delete()
            throw error
        }

        val artwork = copyArtwork(item, audioDir, stableSuffix)
        val durationSeconds = audioDurationSeconds(destination)
            .takeIf { it > 0 }
            ?: item.durationSeconds
        return TaskAudio(
            id = "bili_${UUID.randomUUID()}",
            name = "${safeBiliFileStem(item.displayTitle())}.m4a",
            duration = durationSeconds,
            size = destination.length(),
            localUri = Uri.fromFile(destination).toString(),
            mimeType = "audio/mp4",
            sourceType = "bilibili",
            sourceId = item.id,
            artist = item.owner.ifBlank { null },
            artworkUri = artwork?.let(Uri::fromFile)?.toString(),
        )
    }

    private suspend fun copyArtwork(
        item: BiliCacheItem,
        audioDir: File,
        stableSuffix: String,
    ): File? {
        if (item.coverLocation.isNullOrBlank()) return null
        val coverDir = File(audioDir, "covers").apply {
            if (!exists() && !mkdirs()) return null
        }
        val partial = File(coverDir, "$stableSuffix.partial")
        val destination = File(coverDir, "$stableSuffix.jpg")
        return runCatching {
            openSource(item, cover = true).use { source ->
                FileOutputStream(partial).use { output ->
                    source.input.copyTo(output, COPY_BUFFER_SIZE)
                    output.fd.sync()
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(partial.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("封面文件无效")
            }
            if (destination.exists()) destination.delete()
            if (!partial.renameTo(destination)) throw IOException("无法保存封面")
            destination
        }.onFailure {
            partial.delete()
        }.getOrNull()
    }

    private suspend fun openSource(item: BiliCacheItem, cover: Boolean): OpenedSource {
        val location = if (cover) item.coverLocation else item.audioLocation
        location ?: throw IOException("缓存文件位置为空")
        return when (item.accessMode) {
            BiliCacheAccessMode.DOCUMENT -> {
                val uri = Uri.parse(location)
                val input = appContext.contentResolver.openInputStream(uri)
                    ?: throw IOException("无法读取所选缓存")
                val length = if (cover) 0L else item.audioSize
                OpenedSource(input, length)
            }
            BiliCacheAccessMode.SHIZUKU -> {
                val descriptor = shizukuBridge.openFile(location)
                    ?: throw IOException("Shizuku/Sui 无法读取缓存文件")
                OpenedSource(
                    ParcelFileDescriptor.AutoCloseInputStream(descriptor),
                    if (cover) 0L else item.audioSize,
                )
            }
            BiliCacheAccessMode.ROOT -> {
                val descriptor = rootBridge.openFile(location)
                    ?: throw IOException("无法读取 B 站缓存文件")
                OpenedSource(
                    ParcelFileDescriptor.AutoCloseInputStream(descriptor),
                    if (cover) 0L else item.audioSize,
                )
            }
            BiliCacheAccessMode.NETWORK -> {
                val bvid = item.id.substringBefore(':').takeIf { it.startsWith("BV") }
                val request = Request.Builder()
                    .url(location)
                    .header("User-Agent", USER_AGENT)
                    .header(
                        "Referer",
                        bvid?.let { "https://www.bilibili.com/video/$it" }
                            ?: "https://www.bilibili.com/",
                    )
                    .build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    throw IOException("音轨下载失败：HTTP ${response.code}")
                }
                val body = response.body ?: run {
                    response.close()
                    throw IOException("B 站返回了空音轨")
                }
                OpenedSource(
                    input = body.byteStream(),
                    length = body.contentLength().takeIf { it > 0 } ?: item.audioSize,
                    onClose = response::close,
                )
            }
        }
    }

    private fun validateAudioFile(file: File) {
        if (file.length() <= 8L) throw IOException("转换后的音频为空")
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val hasAudio = (0 until extractor.trackCount).any { index ->
                extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME)
                    .orEmpty()
                    .startsWith("audio/")
            }
            if (!hasAudio) throw IOException("转换结果中没有可播放的音轨")
        } finally {
            extractor.release()
        }
    }

    private fun audioDurationSeconds(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.div(1_000L)
                ?: 0L
        } catch (_: RuntimeException) {
            0L
        } finally {
            runCatching(retriever::release)
        }
    }

    private data class OpenedSource(
        val input: InputStream,
        val length: Long,
        val onClose: () -> Unit = {},
    ) : Closeable {
        override fun close() {
            runCatching(input::close)
            runCatching(onClose)
        }
    }

    companion object {
        private const val COPY_BUFFER_SIZE = 256 * 1_024
        private const val FREE_SPACE_MARGIN = 16L * 1_024L * 1_024L
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/136 Mobile Safari/537.36"

        private fun formatMegabytes(bytes: Long): String =
            "${((bytes + FREE_SPACE_MARGIN) / (1_024L * 1_024L)).coerceAtLeast(1)} MB"
    }
}
