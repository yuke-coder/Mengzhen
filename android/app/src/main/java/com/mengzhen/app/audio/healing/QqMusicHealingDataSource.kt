package com.mengzhen.app.audio.healing

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.tencent.qqmusic.supersound.SuperSoundJni
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar

class QqMusicHealingDispatchDataSourceFactory(
    private val context: Context,
    private val delegate: DataSource.Factory,
) : DataSource.Factory {
    override fun createDataSource(): DataSource = DispatchDataSource(context, delegate)
}

private class DispatchDataSource(
    private val context: Context,
    private val delegate: DataSource.Factory,
) : DataSource {
    private var active: DataSource? = null
    private val listeners = mutableListOf<androidx.media3.datasource.TransferListener>()

    override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
        listeners += transferListener
        active?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        active = if (dataSpec.uri.scheme == QqMusicHealingScene.SCHEME) {
            QqMusicHealingDataSource(context)
        } else {
            delegate.createDataSource()
        }
        listeners.forEach(active!!::addTransferListener)
        return active!!.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        active?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        active?.responseHeaders ?: emptyMap()

    override fun close() {
        active?.close()
        active = null
    }
}

private class QqMusicHealingDataSource(
    private val context: Context,
) : BaseDataSource(false) {
    private var dataSpec: DataSpec? = null
    private var sourceUri: Uri? = null
    private var instance = 0L
    private var position = 0L
    private var streamLength = 0L
    private var pcmOffset = 0
    private var pcmLength = 0

    private val sampleBuffer = FloatArray(
        AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT,
        ),
    )
    private val pcmBuffer = ByteArray(sampleBuffer.size * Float.SIZE_BYTES)
    private val pcmWriter = ByteBuffer.wrap(pcmBuffer).order(ByteOrder.LITTLE_ENDIAN)
    private val generated = IntArray(1)

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        this.dataSpec = dataSpec
        sourceUri = dataSpec.uri
        val scene = dataSpec.uri.lastPathSegment?.toIntOrNull()
            ?.takeIf { it in 0..2 }
            ?: throw IOException("无效的助眠场景")
        val durationMs = dataSpec.uri.getQueryParameter("duration")
            ?.toLongOrNull()
            ?.coerceAtLeast(1_000L)
            ?: QqMusicHealingScene.DEFAULT_DURATION_MS
        val root = QqMusicHealingResources.readyRoot(context)
            ?: QqMusicHealingResources.prepareBlocking(context)
        val calendar = Calendar.getInstance()
        val dayTime = (
            calendar.get(Calendar.HOUR_OF_DAY) * 60f + calendar.get(Calendar.MINUTE)
        ) / (24f * 60f)
        instance = SuperSoundJni.ss_bs_create_inst(
            root.absolutePath.trimEnd('/') + '/',
            0,
            scene,
            dayTime,
        )
        if (instance == 0L) throw IOException("助眠实时合成器启动失败")
        val config = arrayOf("")
        if (SuperSoundJni.ss_bs_update_params(instance, "", config) != 0) {
            close()
            throw IOException("助眠实时合成器配置失败")
        }

        streamLength = WAV_HEADER_SIZE + durationMs * SAMPLE_RATE * CHANNEL_COUNT *
            Float.SIZE_BYTES / 1_000L
        position = dataSpec.position.coerceIn(0L, streamLength)
        pcmOffset = 0
        pcmLength = 0
        transferStarted(dataSpec)
        return streamLength - position
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (position >= streamLength) return C.RESULT_END_OF_INPUT
        var writeOffset = offset
        var remaining = minOf(length.toLong(), streamLength - position).toInt()
        val requested = remaining

        if (position < WAV_HEADER_SIZE) {
            val header = waveHeader(streamLength - WAV_HEADER_SIZE)
            val count = minOf(remaining, WAV_HEADER_SIZE.toInt() - position.toInt())
            header.copyInto(buffer, writeOffset, position.toInt(), position.toInt() + count)
            position += count
            writeOffset += count
            remaining -= count
        }

        while (remaining > 0) {
            if (pcmOffset >= pcmLength) generatePcm()
            val count = minOf(remaining, pcmLength - pcmOffset)
            pcmBuffer.copyInto(buffer, writeOffset, pcmOffset, pcmOffset + count)
            pcmOffset += count
            writeOffset += count
            remaining -= count
            position += count
        }

        bytesTransferred(requested)
        return requested
    }

    private fun generatePcm() {
        generated[0] = 0
        SuperSoundJni.ss_bs_process_out(
            instance,
            sampleBuffer,
            sampleBuffer.size,
            generated,
        )
        if (generated[0] <= 0) {
            throw IOException("助眠实时音频生成失败：${generated[0]}")
        }
        pcmWriter.clear()
        pcmWriter.asFloatBuffer().put(sampleBuffer, 0, generated[0])
        pcmOffset = 0
        pcmLength = generated[0] * Float.SIZE_BYTES
    }

    override fun getUri(): Uri? = sourceUri

    override fun close() {
        if (instance != 0L) {
            SuperSoundJni.ss_bs_destroy_inst(instance)
            instance = 0L
        }
        if (dataSpec != null) transferEnded()
        dataSpec = null
        sourceUri = null
        pcmOffset = 0
        pcmLength = 0
    }

    private fun waveHeader(dataSize: Long): ByteArray = ByteBuffer
        .allocate(WAV_HEADER_SIZE.toInt())
        .order(ByteOrder.LITTLE_ENDIAN)
        .apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt((dataSize + 36L).toInt())
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(3.toShort())
            putShort(CHANNEL_COUNT.toShort())
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * CHANNEL_COUNT * Float.SIZE_BYTES)
            putShort((CHANNEL_COUNT * Float.SIZE_BYTES).toShort())
            putShort(32.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize.toInt())
        }
        .array()

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val CHANNEL_COUNT = 2
        const val WAV_HEADER_SIZE = 44L
    }
}
