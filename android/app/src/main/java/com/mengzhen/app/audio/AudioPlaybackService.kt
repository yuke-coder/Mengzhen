package com.mengzhen.app.audio

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.mengzhen.app.MainActivity
import com.mengzhen.app.R
import com.mengzhen.app.audio.healing.QqMusicHealingDispatchDataSourceFactory
import com.mengzhen.app.audio.healing.QqMusicHealingScene
import com.mengzhen.app.data.model.TaskPhase
import com.mengzhen.app.data.store.AppSettingsStore
import com.mengzhen.app.receiver.ScreenStatusReceiver
import com.mengzhen.app.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

@androidx.annotation.OptIn(UnstableApi::class)
class AudioPlaybackService : Service() {

    private val tag = "AudioPlaybackService"
    private val channelId = "dream_pillow_playback"
    private val notificationId = 1001

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val playbackStateStore by lazy { PlaybackStateStore.get(this) }

    private var cache: SimpleCache? = null

    private val playlist = mutableListOf<Track>()
    private var currentTrackIndex = 0
    private var playbackMode = QqMusicPlaybackMode.LIST_REPEAT
    private var shufflePreference = QqMusicShufflePreference.DEFAULT
    private val shuffleRemaining = mutableListOf<Int>()
    private val shuffleHistory = mutableListOf<Int>()
    private var recordedShuffleTrackId: String? = null
    private var desiredPlaying = true
    private var pausedForTransientFocusLoss = false
    private var scheduledExecution = true

    private var currentTaskId: String? = null
    private var taskName = "梦枕"
    private var targetVolume = 70
    private var enableFade = false
    private var enableFadeOut = false
    private var fadeInDuration = 0
    private var fadeOutDuration = 0
    private var playDurationMinutes = 30
    private var scheduledStopDurationSeconds = 0
    private var loopSingle = true
    private var autoContinue = true
    private var skipHeadMs = 0L
    private var skipTailMs = 0L
    private var tailSkipTriggeredForTrack = false
    private var startTimeMs = 0L
    private var executionClockStarted = false
    private var endTimeMs = 0L
    private var executionEndTimeMs = 0L
    private var sleepTimerEndTimeMs = 0L
    private var remainingTracksUntilStop = 0
    private var finishCurrentTrackAfterSleep = false
    private var stopAfterCurrentTrack = false
    private var stopAtPlaylistEnd = false
    private var finishingPlayback = false
    private var wasPlayingBeforeCall = false
    private var coverUrl: String? = null

    private val gainEnvelope = PlaybackGainEnvelope()
    private val gainProcessor = GainProcessor(gainEnvelope)
    private var fadeInCompletionRunnable: Runnable? = null
    private var fadeInRemainingMs = 0L
    private var fadeOutCompletionRunnable: Runnable? = null
    private var stopRunnable: Runnable? = null
    private var progressRunnable: Runnable? = null

    private var telephonyManager: TelephonyManager? = null
    private var phoneListenerRegistered = false
    private var telephonyCallback: TelephonyCallback? = null

    private var noisyReceiverRegistered = false
    private var headsetUnplugDebounced = false // 耳机拔出 3 秒去抖 - 对标喜马拉雅 al.java f45972b

    /** 网络变化监听 - 对标喜马拉雅 al.java ACTION_BROADCAST_NETWORK_CHANGE
     *  喜马拉雅原版将网络变化和耳机拔出放在同一个 BroadcastReceiver 里处理
     *  网络断开时暂停播放，网络恢复时不自动恢复（等用户手动操作）
     */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1) {
                        resumeWhenAudioOutputConnects()
                    } else if (state == 0) { // 拔出
                        if (headsetUnplugDebounced) return // 去抖：3 秒内已处理过一次
                        headsetUnplugDebounced = true
                        handler.postDelayed({ headsetUnplugDebounced = false }, 3000)
                        desiredPlaying = false
                        pausedForTransientFocusLoss = false
                        wasPlayingBeforeCall = false
                        wasPlayingBeforeNetworkLoss = false
                        player?.let { if (it.isPlaying) it.pause() }
                        persistLastPlaybackState()
                        updateNotification("暂停中(耳机拔出): $taskName")
                    }
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    if (
                        intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) ==
                        BluetoothProfile.STATE_CONNECTED
                    ) {
                        resumeWhenAudioOutputConnects()
                    }
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    if (headsetUnplugDebounced) return // 去抖
                    headsetUnplugDebounced = true
                    handler.postDelayed({ headsetUnplugDebounced = false }, 3000)
                    desiredPlaying = false
                    pausedForTransientFocusLoss = false
                    wasPlayingBeforeCall = false
                    wasPlayingBeforeNetworkLoss = false
                    player?.let { if (it.isPlaying) it.pause() }
                    persistLastPlaybackState()
                    updateNotification("暂停中(音频输出变更): $taskName")
                }
            }
        }
    }

    private fun resumeWhenAudioOutputConnects() {
        if (
            !AppSettingsStore.get(this).getBoolean(
                AppSettingsStore.KEY_BLUETOOTH_AUTO_PLAY,
                false,
            ) || playlist.isEmpty()
        ) {
            return
        }
        val currentPlayer = player ?: return
        if (currentPlayer.isPlaying) return
        desiredPlaying = true
        if (requestAudioFocus()) {
            currentPlayer.play()
            persistLastPlaybackState()
            updateNotification("正在播放: $taskName")
        }
    }

    /** 网络变化监听 - 对标喜马拉雅 al.java ACTION_BROADCAST_NETWORK_CHANGE
     *  用 ConnectivityManager.NetworkCallback（最新 API，替代 deprecated 的 CONNECTIVITY_ACTION broadcast）
     *  网络断开时暂停播放，网络恢复时自动恢复（梦枕优于喜马拉雅）
     */
    private val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
        override fun onLost(network: android.net.Network) {
            Log.w(tag, "Network lost, pausing playback")
            val p = player
            if (p != null && p.isPlaying) {
                wasPlayingBeforeNetworkLoss = true
                handler.post { p.pause() }
            }
            updateNotification("暂停中(网络断开): $taskName")
        }

        override fun onAvailable(network: android.net.Network) {
            if (desiredPlaying && wasPlayingBeforeNetworkLoss) {
                Log.i(tag, "Network restored, resuming playback")
                wasPlayingBeforeNetworkLoss = false
                val p = player
                if (p != null && !p.isPlaying) {
                    handler.post {
                        p.play()
                        updateNotification("正在播放: $taskName")
                    }
                }
            } else {
                wasPlayingBeforeNetworkLoss = false
            }
        }
    }
    private var networkCallbackRegistered = false

    @UnstableApi
    private inner class DreamPillowTelephonyCallback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            onCallStateChangedInternal(state)
        }
    }

    private fun onCallStateChangedInternal(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                val p = player ?: return
                wasPlayingBeforeCall = p.isPlaying
                if (p.isPlaying) p.pause()
                updateNotification("暂停中(来电): $taskName")
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                val p = player ?: return
                if (desiredPlaying && wasPlayingBeforeCall && !p.isPlaying) {
                    p.play()
                    updateNotification("正在播放: $taskName")
                }
                wasPlayingBeforeCall = false
            }
        }
    }

    /** 音频焦点 - 对标喜马拉雅 al.java OnAudioFocusChangeListener
     *  喜马拉雅原版：LOSS/LOSS_TRANSIENT -> 暂停+abandonFocus，CAN_DUCK -> 不处理，GAIN -> 不处理
     *  梦枕调整：LOSS -> 暂停+abandon（不杀 Service，对标喜马拉雅），
     *           LOSS_TRANSIENT -> 暂停但保持焦点（等 GAIN 恢复），
     *           CAN_DUCK -> 降音量，GAIN -> 恢复
     */
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        val p = player ?: return@OnAudioFocusChangeListener
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 对标喜马拉雅 al.java: LOSS 只暂停+abandon，不杀 Service
                desiredPlaying = false
                pausedForTransientFocusLoss = false
                if (p.isPlaying) p.pause()
                releaseAudioFocus()
                persistLastPlaybackState()
                updateNotification("暂停中(焦点丢失): $taskName")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (p.isPlaying) {
                    pausedForTransientFocusLoss = true
                    p.pause()
                    updateNotification("暂停中: $taskName")
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                p.volume = 0.2f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                p.volume = targetVolume / 100f
                val continueAfterInterruption = AppSettingsStore.get(this)
                    .getBoolean(AppSettingsStore.KEY_CONTINUE_AFTER_INTERRUPTION, false)
                val shouldResume = continueAfterInterruption && desiredPlaying &&
                    pausedForTransientFocusLoss && !p.isPlaying
                pausedForTransientFocusLoss = false
                if (shouldResume) {
                    p.play()
                    updateNotification("正在播放: $taskName")
                }
            }
        }
    }

    /** 错误重试 - 对标喜马拉雅 i.java LoadErrorHandlingPolicy + onPlayerError */
    private var errorRetryCount = 0
    private val maxErrorRetries = 3
    private var wasPlayingBeforeNetworkLoss = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    onTrackPlaybackEnded()
                }
                Player.STATE_BUFFERING -> {
                    playbackStateStore.update(
                        transportState = PlaybackTransportState.PREPARING,
                        message = "正在缓冲音频",
                    )
                }
                Player.STATE_IDLE -> {
                    // STATE_IDLE 通常意味着播放错误后被 stop()，不在这里处理重试
                }
                Player.STATE_READY -> {
                    // 准备就绪后检查断点续播位置是否越界
                    // 对标喜马拉雅 i.java isCausedByPositionOutOfRange
                    val p = player ?: return
                    val duration = p.duration
                    if (
                        duration > 0 &&
                        p.currentPosition >= (duration - 1_000L).coerceAtLeast(0L)
                    ) {
                        Log.w(tag, "Resume position ${p.currentPosition} is at the end of $duration, resetting to 0")
                        p.seekTo(0)
                    }
                    playbackStateStore.update(
                        transportState = if (p.isPlaying) {
                            PlaybackTransportState.PLAYING
                        } else {
                            PlaybackTransportState.PAUSED
                        },
                        positionMs = p.currentPosition,
                        durationMs = duration.takeIf { it != C.TIME_UNSET } ?: 0,
                    )
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                beginPlaybackClockIfNeeded()
                scheduleFadeInPhaseCompletion()
            }
            updateMediaSessionState(isPlaying)
            updateNotification(if (isPlaying) "正在播放: $taskName" else "暂停中: $taskName")
            if (isPlaying) {
                recordShuffleTrackIfNeeded()
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
            val p = player
            playbackStateStore.update(
                transportState = when {
                    isPlaying -> PlaybackTransportState.PLAYING
                    p?.playbackState == Player.STATE_BUFFERING -> PlaybackTransportState.PREPARING
                    else -> PlaybackTransportState.PAUSED
                },
                positionMs = p?.currentPosition ?: 0,
                durationMs = p?.duration?.takeIf { it != C.TIME_UNSET } ?: 0,
                message = null,
            )
            persistLastPlaybackState()
        }

        /** 对标喜马拉雅 i.java onPlayerError - 按错误类型分别处理
         *  喜马拉雅原版按错误类型分支：
         *  - FileNotFoundException: 重新 setDataSource 重试
         *  - UnrecognizedInputFormatException: 删本地文件后用备用 URL 重试
         *  - FfmpegDecoderException: 走 ffmpeg 降级
         *  - HlsPlaylistTracker.PlaylistStuckException: 有独立重试计数器
         *  - 其他: 直接上报
         *  梦枕简化为 3 类：本地文件损坏删除重下 / 网络错误重试 / 其他直接停
         */
        override fun onPlayerErrorChanged(error: androidx.media3.common.PlaybackException?) {
            if (error == null) {
                errorRetryCount = 0
                return
            }
            Log.e(tag, "Player error #${errorRetryCount + 1}: ${error.errorCodeName}", error)

            if (errorRetryCount >= maxErrorRetries) {
                Log.e(tag, "Max retries ($maxErrorRetries) reached, stopping")
                errorRetryCount = 0
                val failedTaskId = currentTaskId
                playbackStateStore.finish(
                    taskId = failedTaskId,
                    transportState = PlaybackTransportState.ERROR,
                    message = "音频播放失败，请返回设置后重试",
                )
                stopPlaybackInternal()
                completeScheduledExecutionIfNeeded(failedTaskId)
                ServiceCompat.stopForeground(this@AudioPlaybackService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            errorRetryCount++
            val delay = 2000L * errorRetryCount // 递增延迟：2s, 4s, 6s
            val track = playlist.getOrNull(currentTrackIndex)
            if (track == null) {
                val failedTaskId = currentTaskId
                playbackStateStore.finish(
                    taskId = failedTaskId,
                    transportState = PlaybackTransportState.ERROR,
                    message = "没有找到可播放的音频",
                )
                stopPlaybackInternal()
                completeScheduledExecutionIfNeeded(failedTaskId)
                stopSelf()
                return
            }

            // 对标喜马拉雅 i.java: 按错误类型分别处理
            // 1. 本地缓存文件损坏（EOF/decode error）-> 删除后重新下载
            // 2. 网络错误 -> 直接重试
            // 3. 其他 -> 直接停
            val cause = error.cause
            val isFileError = cause is java.io.EOFException
                || cause is java.io.IOException && cause.message?.contains("unexpected end") == true
                || error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED

            if (isFileError) {
                Log.i(tag, "File corruption detected (EOF/malformed), deleting and retrying")
                val localFile = downloadToCacheFilePath(track.url)
                localFile?.let { if (it.exists()) it.delete() }
            } else {
                // 对标喜马拉雅 i.java: 其他错误也尝试删本地缓存
                val localFile = downloadToCacheFilePath(track.url)
                if (localFile != null && localFile.exists()) {
                    Log.i(tag, "Deleting cached file: ${localFile.absolutePath}")
                    localFile.delete()
                }
            }

            handler.postDelayed({
                val p = player ?: return@postDelayed
                // 确保没有被切换到其他 track
                if (playlist.getOrNull(currentTrackIndex) != track) return@postDelayed
                // 重新走 playTrack 逻辑（含 downloadToCache + 断点续播）
                // 对标喜马拉雅 i.java reset() + setDataSource() + prepare()
                playTrack(currentTrackIndex, 0L)
                Log.i(tag, "Retrying playback (attempt $errorRetryCount/$maxErrorRetries)")
            }, delay)
        }
    }

    data class Track(
        val id: String,
        val url: String,
        val name: String,
    )

    data class ShuffleHistoryEntry(
        val id: String,
        val name: String,
        val playedAt: Long,
    )

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val callback = DreamPillowTelephonyCallback()
            telephonyCallback = callback
            try {
                telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
                phoneListenerRegistered = true
            } catch (error: SecurityException) {
                telephonyCallback = null
                Log.w(tag, "Call-state listener unavailable; audio focus still handles calls", error)
            }
        } else {
            Log.i(tag, "READ_PHONE_STATE not granted; relying on audio focus for call interruption")
        }

        // 耳机插拔 + 音频输出变更监听 - 对标喜马拉雅 al.java IntentFilter
        val noisyFilter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            noisyReceiver,
            noisyFilter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        noisyReceiverRegistered = true

        // 网络变化监听 - ConnectivityManager.NetworkCallback（替代 deprecated CONNECTIVITY_ACTION）
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        networkCallbackRegistered = true

        ScreenStatusReceiver.register(this)
        createNotificationChannel()
        initCache()
        initPlayer()
        Log.i(tag, "Service created")
    }

    private fun initCache() {
        try {
            val cacheDir = File(cacheDir, "audio_cache").apply { mkdirs() }
            cache = SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(500L * 1024 * 1024),
                androidx.media3.database.StandaloneDatabaseProvider(this)
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to init cache", e)
        }
        // 清理下载缓存目录中超过 7 天的文件
        cleanDownloadCache()
    }

    /** 清理下载缓存 - 删除超过 7 天的文件，总大小不超过 500MB */
    private fun cleanDownloadCache() {
        val dir = File(cacheDir, "audio_download")
        if (!dir.exists()) return
        val now = System.currentTimeMillis()
        val maxAge = 7L * 24 * 60 * 60 * 1000
        var totalSize = 0L
        val files = dir.listFiles()
            ?.filter { it.extension == AUDIO_STREAM_CACHE_EXTENSION }
            ?.toMutableList()
            ?: return
        // 先删过期文件
        files.removeAll { f ->
            val expired = now - f.lastModified() > maxAge
            if (expired) f.delete()
            expired
        }
        // 计算剩余总大小
        files.forEach { totalSize += it.length() }
        // 如果超过 500MB，按最旧优先删除
        val maxSize = 500L * 1024 * 1024
        if (totalSize > maxSize) {
            files.sortBy { it.lastModified() }
            for (f in files) {
                if (totalSize <= maxSize) break
                totalSize -= f.length()
                f.delete()
                Log.i(tag, "Cleaned download cache: ${f.name}")
            }
        }
    }

    private fun initPlayer() {
        val cache = this.cache
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)
        // DefaultDataSource performs the protocol dispatch used by Ximalaya's player:
        // content/file/android.resource stay local, while http/https use the tuned network source.
        // Supplying DefaultHttpDataSource directly to CacheDataSource makes content:// fall
        // through java.net.URL and fail with "unknown protocol: content".
        val upstream = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val standardDataSourceFactory = if (cache != null) {
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } else {
            upstream
        }
        // QQ Music's healing source is generated live and must never be materialized in
        // the normal download/cache layer. Everything else keeps the existing dispatch.
        val dataSourceFactory = QqMusicHealingDispatchDataSourceFactory(
            this,
            standardDataSourceFactory,
        )

        // 对标喜马拉雅 i.java 的缓冲配置；四个阈值同时满足 Media3 的顺序约束。
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs= */ 1000,
                /* maxBufferMs= */ 20000,
                /* bufferForPlaybackMs= */ 500,
                /* bufferForPlaybackAfterRebufferMs= */ 1000,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 自定义 LoadError - 对标喜马拉雅 i.java LoadErrorHandlingPolicy
        // HttpDataSource.InvalidResponseCodeException (4xx/5xx) 不重试，直接报错
        // 其他错误走默认重试策略
        val loadErrorHandlingPolicy = object : androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy() {
            override fun getRetryDelayMsFor(loadErrorInfo: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val exception = loadErrorInfo.exception
                // 4xx 错误不重试（文件不存在、权限错误等）
                if (exception is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                    val code = exception.responseCode
                    if (code in 400..499 && code != 416) {
                        return -Long.MAX_VALUE
                    }
                }
                // 其他错误走默认重试
                return super.getRetryDelayMsFor(loadErrorInfo)
            }
        }

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf<AudioProcessor>(gainProcessor))
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
        }

        val activePlayer = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
            )
            .setLoadControl(loadControl)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                // Audio focus is owned by audioFocusListener below. Letting ExoPlayer create a
                // second focus request makes the two listeners interrupt each other on resume.
                false
            )
            .setHandleAudioBecomingNoisy(false) // 自己处理，注册了 noisyReceiver
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(SEEK_INTERVAL_MS)
            .setSeekForwardIncrementMs(SEEK_INTERVAL_MS)
            .setTrackSelector(DefaultTrackSelector(this))
            .build()
            .apply {
                addListener(playerListener)
                setPlaybackSpeed(getPlaybackSpeed(this@AudioPlaybackService))
            }
        player = activePlayer

        applySoundEffect(getSoundEffect(this))

        mediaSession = MediaSession.Builder(this, activePlayer)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    private fun applySoundEffect(effect: PlaybackSoundEffect) {
        releaseSoundEffects()
        val sessionId = player?.audioSessionId ?: return
        if (sessionId <= 0 || effect == PlaybackSoundEffect.ORIGINAL) {
            Log.i(tag, "Sound effect applied: ${effect.storageValue}")
            return
        }
        runCatching {
            when (effect) {
                PlaybackSoundEffect.ORIGINAL -> Unit
                PlaybackSoundEffect.VOICE -> {
                    equalizer = Equalizer(0, sessionId).apply {
                        val minLevel = bandLevelRange[0].toInt()
                        val maxLevel = bandLevelRange[1].toInt()
                        for (bandIndex in 0 until numberOfBands.toInt()) {
                            val band = bandIndex.toShort()
                            val centerHz = getCenterFreq(band) / 1_000
                            val targetLevel = when {
                                centerHz < 250 -> -900
                                centerHz < 700 -> 200
                                centerHz <= 4_500 -> 1_800
                                centerHz <= 8_000 -> 700
                                else -> -300
                            }.coerceIn(minLevel, maxLevel)
                            setBandLevel(band, targetLevel.toShort())
                        }
                        enabled = true
                    }
                }
                PlaybackSoundEffect.BASS -> {
                    bassBoost = BassBoost(0, sessionId).apply {
                        if (strengthSupported) setStrength(700)
                        enabled = true
                    }
                }
                PlaybackSoundEffect.SURROUND -> {
                    @Suppress("DEPRECATION")
                    virtualizer = Virtualizer(0, sessionId).apply {
                        if (strengthSupported) setStrength(650)
                        enabled = true
                    }
                }
                PlaybackSoundEffect.LOUDNESS -> {
                    loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                        setTargetGain(600)
                        enabled = true
                    }
                }
            }
            Log.i(tag, "Sound effect applied: ${effect.storageValue}")
        }.onFailure { error ->
            releaseSoundEffects()
            Log.w(tag, "Sound effect ${effect.storageValue} unavailable", error)
        }
    }

    private fun releaseSoundEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        @Suppress("DEPRECATION")
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP -> {
                val stoppedTaskId = currentTaskId
                val stoppedScheduledExecution = scheduledExecution
                if (stoppedTaskId != null) {
                    playbackStateStore.finish(
                        taskId = stoppedTaskId,
                        transportState = PlaybackTransportState.STOPPED,
                        message = "本次播放已停止",
                    )
                }
                stopPlaybackInternal()
                if (stoppedScheduledExecution) stoppedTaskId?.let {
                    AlarmScheduler.get(this).onPlaybackStoppedByUser(it)
                }
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                desiredPlaying = false
                pausedForTransientFocusLoss = false
                wasPlayingBeforeCall = false
                wasPlayingBeforeNetworkLoss = false
                fadeInCompletionRunnable?.let(handler::removeCallbacks)
                fadeInCompletionRunnable = null
                fadeInRemainingMs = 0L
                gainEnvelope.finishFadeIn()
                player?.pause()
                player?.volume = targetVolume / 100f
                persistLastPlaybackState()
                updateNotification("暂停中: $taskName")
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                // 恢复播放时重新请求音频焦点（对标喜马拉雅 al.java a()）
                // 因为 LOSS 时 abandon 了焦点，恢复需要重新请求
                desiredPlaying = true
                pausedForTransientFocusLoss = false
                requestAudioFocus()
                player?.play()
                persistLastPlaybackState()
                updateNotification("正在播放: $taskName")
                return START_NOT_STICKY
            }
            ACTION_NEXT -> {
                desiredPlaying = true
                playNextTrack()
                return START_NOT_STICKY
            }
            ACTION_PREV -> {
                desiredPlaying = true
                playPrevTrack()
                return START_NOT_STICKY
            }
            ACTION_SET_PLAYBACK_MODE -> {
                playbackMode = QqMusicPlaybackMode.fromSourceValue(
                    intent.getIntExtra(
                        EXTRA_PLAYBACK_MODE,
                        QqMusicPlaybackMode.LIST_REPEAT.sourceValue,
                    ),
                )
                getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(PENDING_PLAYBACK_MODE, playbackMode.sourceValue)
                    .apply()
                resetShuffleTraversal()
                persistLastPlaybackState()
                return START_NOT_STICKY
            }
            ACTION_SEEK_BACK -> {
                player?.seekBack()
                return START_NOT_STICKY
            }
            ACTION_SEEK_FORWARD -> {
                player?.seekForward()
                return START_NOT_STICKY
            }
            ACTION_PLAY_INDEX -> {
                val index = intent.getIntExtra(EXTRA_TRACK_INDEX, -1)
                if (index in playlist.indices) {
                    desiredPlaying = true
                    val savedProgressMs = savedProgressMs(playlist[index].id)
                    playTrack(index, savedProgressMs)
                } else {
                    Log.w(tag, "Ignoring invalid track index $index for ${playlist.size} tracks")
                }
                return START_NOT_STICKY
            }
            ACTION_REMOVE_TRACK -> {
                removeTrackAt(intent.getIntExtra(EXTRA_TRACK_INDEX, -1))
                return START_NOT_STICKY
            }
            ACTION_MOVE_TRACK -> {
                moveTrack(
                    intent.getIntExtra(EXTRA_FROM_TRACK_INDEX, -1),
                    intent.getIntExtra(EXTRA_TO_TRACK_INDEX, -1),
                )
                return START_NOT_STICKY
            }
            ACTION_SEEK -> {
                val pos = intent.getLongExtra("position", -1L)
                if (pos >= 0) player?.seekTo(pos)
                return START_NOT_STICKY
            }
            ACTION_SET_PLAYBACK_SPEED -> {
                val speed = intent.getFloatExtra(EXTRA_PLAYBACK_SPEED, 1f)
                    .coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
                getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putFloat(PENDING_PLAYBACK_SPEED, speed)
                    .apply()
                player?.setPlaybackSpeed(speed)
                return START_NOT_STICKY
            }
            ACTION_SET_SOUND_EFFECT -> {
                val effect = PlaybackSoundEffect.fromStorage(
                    intent.getStringExtra(EXTRA_SOUND_EFFECT),
                )
                getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PENDING_SOUND_EFFECT, effect.storageValue)
                    .apply()
                applySoundEffect(effect)
                return START_NOT_STICKY
            }
            ACTION_SET_SLEEP_TIMER -> {
                cancelActiveFadeOut()
                val durationSeconds = intent.getIntExtra(
                    EXTRA_SLEEP_DURATION_SECONDS,
                    intent.getIntExtra(EXTRA_MINUTES, 0).coerceAtLeast(0) * 60,
                ).coerceAtLeast(0)
                scheduledStopDurationSeconds = durationSeconds
                stopAfterCurrentTrack = false
                if (durationSeconds > 0) {
                    sleepTimerEndTimeMs = intent.getLongExtra(
                        EXTRA_SLEEP_TIMER_END_TIME,
                        System.currentTimeMillis() + durationSeconds * 1_000L,
                    ).coerceAtLeast(0L)
                    remainingTracksUntilStop = 0
                    updateNotification("已设定${formatTimerDuration(durationSeconds)}后停止: $taskName")
                } else {
                    sleepTimerEndTimeMs = 0L
                    remainingTracksUntilStop = 0
                    updateNotification("已关闭定时停止: $taskName")
                }
                refreshEffectiveEndTime()
                schedulePlaybackEnd()
                persistLastPlaybackState()
                return START_NOT_STICKY
            }
            ACTION_SET_SLEEP_FADE_OUT -> {
                cancelActiveFadeOut()
                enableFadeOut = intent.getBooleanExtra(EXTRA_FADE_OUT_ENABLED, false)
                fadeOutDuration = intent.getIntExtra(EXTRA_FADE_OUT_SECONDS, 0)
                    .coerceAtLeast(0)
                schedulePlaybackEnd()
                persistLastPlaybackState()
                return START_NOT_STICKY
            }
            ACTION_SET_SLEEP_AFTER_TRACKS -> {
                cancelActiveFadeOut()
                val count = intent.getIntExtra(EXTRA_TRACK_COUNT, 0).coerceAtLeast(0)
                stopAfterCurrentTrack = false
                remainingTracksUntilStop = count
                scheduledStopDurationSeconds = 0
                if (count > 0) {
                    sleepTimerEndTimeMs = 0L
                    updateNotification("将在播完${count}集后停止: $taskName")
                } else {
                    updateNotification("已关闭按集数停止: $taskName")
                }
                refreshEffectiveEndTime()
                schedulePlaybackEnd()
                persistLastPlaybackState()
                return START_NOT_STICKY
            }
            ACTION_SET_SLEEP_FINISH_CURRENT_TRACK -> {
                finishCurrentTrackAfterSleep =
                    intent.getBooleanExtra(EXTRA_FINISH_CURRENT_TRACK, false)
                persistLastPlaybackState()
                return START_NOT_STICKY
            }
            ACTION_SET_AUTO_CONTINUE -> {
                autoContinue = intent.getBooleanExtra(EXTRA_AUTO_CONTINUE, true)
                getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(PENDING_AUTO_CONTINUE, autoContinue)
                    .apply()
                persistLastPlaybackState()
                return START_NOT_STICKY
            }
            ACTION_SET_SKIP_HEAD_TAIL -> {
                val targetTaskId = intent.getStringExtra(EXTRA_SKIP_TASK_ID)
                if (targetTaskId != null && targetTaskId != currentTaskId) {
                    return START_NOT_STICKY
                }
                skipHeadMs = intent.getIntExtra(EXTRA_SKIP_HEAD_SECONDS, 0)
                    .coerceIn(0, MAX_SKIP_SECONDS) * 1_000L
                skipTailMs = intent.getIntExtra(EXTRA_SKIP_TAIL_SECONDS, 0)
                    .coerceIn(0, MAX_SKIP_SECONDS) * 1_000L
                tailSkipTriggeredForTrack = false
                val currentPosition = player?.currentPosition ?: 0L
                if (currentPosition in 0 until skipHeadMs) {
                    player?.seekTo(skipHeadMs)
                }
                persistLastPlaybackState()
                return START_NOT_STICKY
            }
            ACTION_RESTART -> {
                // 显式恢复入口：仅在持久化会话数据完整时恢复播放。
                val state = getLastPlaybackState()
                if (state != null) {
                    startForegroundWithNotification()
                    val restart = Intent(this, AudioPlaybackService::class.java)
                    restart.putExtras(state)
                    startPlayback(restart)
                    Log.i(tag, "Restarted from saved state")
                } else {
                    Log.w(tag, "No saved state to restart")
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForegroundWithNotification()
                startPlayback(intent)
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("准备播放...")
        startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    private fun startPlayback(intent: Intent) {
        val incomingTaskId = intent.getStringExtra("taskId")
        if (currentTaskId != null && currentTaskId != incomingTaskId) {
            saveProgress()
            player?.pause()
        }
        finishingPlayback = false
        stopAfterCurrentTrack = false
        pausedForTransientFocusLoss = false
        wasPlayingBeforeCall = false
        wasPlayingBeforeNetworkLoss = false
        currentDownloadJob?.cancel()
        fadeInCompletionRunnable?.let { handler.removeCallbacks(it) }
        fadeOutCompletionRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable?.let { handler.removeCallbacks(it) }
        fadeInCompletionRunnable = null
        fadeInRemainingMs = 0L
        fadeOutCompletionRunnable = null
        stopRunnable = null
        gainEnvelope.reset()
        currentTaskId = incomingTaskId
        taskName = intent.getStringExtra("taskName") ?: "梦枕"
        playDurationMinutes = intent.getIntExtra("playDurationMinutes", 30)
        scheduledStopDurationSeconds = intent.getIntExtra(
            EXTRA_EXECUTION_DURATION_SECONDS,
            0,
        ).coerceAtLeast(0)
        targetVolume = intent.getIntExtra("volume", 70)
        enableFade = intent.getBooleanExtra("enableFade", false)
        enableFadeOut = intent.getBooleanExtra("enableFadeOut", false)
        fadeInDuration = intent.getIntExtra("fadeInDuration", 0)
        fadeOutDuration = intent.getIntExtra("fadeOutDuration", 0)
        loopSingle = intent.getBooleanExtra("loopSingle", true)
        scheduledExecution = intent.getBooleanExtra(EXTRA_IS_SCHEDULED_EXECUTION, true)
        playbackMode = getPlaybackMode(this)
        shufflePreference = getShufflePreference(this)
        resetShuffleTraversal()
        autoContinue = if (intent.hasExtra(EXTRA_AUTO_CONTINUE)) {
            intent.getBooleanExtra(EXTRA_AUTO_CONTINUE, true)
        } else if (scheduledExecution) {
            true
        } else {
            AppSettingsStore.get(this).getBoolean(
                AppSettingsStore.KEY_AUTO_PLAY_RECOMMENDATION,
                getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                    .getBoolean(PENDING_AUTO_CONTINUE, true),
            )
        }
        skipHeadMs = intent.getIntExtra(EXTRA_SKIP_HEAD_SECONDS, 0)
            .coerceIn(0, MAX_SKIP_SECONDS) * 1_000L
        skipTailMs = intent.getIntExtra(EXTRA_SKIP_TAIL_SECONDS, 0)
            .coerceIn(0, MAX_SKIP_SECONDS) * 1_000L
        coverUrl = intent.getStringExtra("coverUrl")
        stopAtPlaylistEnd = intent.getBooleanExtra(
            EXTRA_STOP_AT_PLAYLIST_END,
            scheduledExecution,
        )
        desiredPlaying = intent.getBooleanExtra(EXTRA_DESIRED_PLAYING, true)
        remainingTracksUntilStop = if (intent.hasExtra(EXTRA_REMAINING_TRACKS)) {
            intent.getIntExtra(EXTRA_REMAINING_TRACKS, 0).coerceAtLeast(0)
        } else {
            0
        }
        finishCurrentTrackAfterSleep = if (intent.hasExtra(EXTRA_FINISH_CURRENT_TRACK)) {
            intent.getBooleanExtra(EXTRA_FINISH_CURRENT_TRACK, false)
        } else {
            false
        }
        stopAfterCurrentTrack =
            intent.getBooleanExtra(EXTRA_STOP_AFTER_CURRENT_TRACK, false)
        val suppliedEndTime = intent.getLongExtra("endTime", 0L)
        executionEndTimeMs = intent.getLongExtra(
            EXTRA_EXECUTION_END_TIME,
            if (scheduledExecution) suppliedEndTime else 0L,
        )
        sleepTimerEndTimeMs = if (intent.hasExtra(EXTRA_SLEEP_TIMER_END_TIME)) {
            intent.getLongExtra(EXTRA_SLEEP_TIMER_END_TIME, 0L)
        } else {
            0L
        }
        refreshEffectiveEndTime()

        // 异步加载封面 - 避免主线程 ANR
        coverBitmap = null
        coverArtworkData = null
        loadCoverAsync(coverUrl)

        playlist.clear()
        val tracksJson = intent.getStringExtra("tracksJson")
        if (!tracksJson.isNullOrEmpty()) {
            parseTracksJson(tracksJson)
        } else {
            val audioUrl = intent.getStringExtra("audioUrl")
            val audioName = intent.getStringExtra("audioName")
            if (!audioUrl.isNullOrEmpty()) {
                playlist.add(
                    Track(
                        id = intent.getStringExtra("audioId").orEmpty().ifEmpty { audioUrl },
                        url = audioUrl,
                        name = audioName ?: "音频",
                    )
                )
            }
        }

        startTimeMs = intent.getLongExtra(EXTRA_STARTED_AT, 0L).coerceAtLeast(0L)
        executionClockStarted = startTimeMs > 0L
        playbackStateStore.begin(
            taskId = currentTaskId,
            taskName = taskName,
            trackNames = playlist.map(Track::name),
            startedAt = startTimeMs,
            endsAt = endTimeMs,
            targetVolume = targetVolume,
            phase = if (executionClockStarted && enableFade && fadeInDuration > 0) {
                TaskPhase.FADING_IN
            } else {
                TaskPhase.IDLE
            },
        )

        if (playlist.isEmpty()) {
            Log.e(tag, "No tracks to play")
            val failedTaskId = currentTaskId
            playbackStateStore.finish(
                taskId = failedTaskId,
                transportState = PlaybackTransportState.ERROR,
                message = "没有找到可播放的音频",
            )
            stopPlaybackInternal()
            completeScheduledExecutionIfNeeded(failedTaskId)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        if (desiredPlaying) requestAudioFocus()
        SustainedListenService.bind(this)

        currentTrackIndex = intent.getIntExtra(EXTRA_TRACK_INDEX, 0)
            .coerceIn(0, playlist.lastIndex)
        // 检查断点续播 - 对标喜马拉雅播放进度恢复
        val savedProgressMs = if (!breakpointResumeEnabled()) {
            0L
        } else if (intent.hasExtra(EXTRA_RESUME_POSITION_MS)) {
            intent.getLongExtra(EXTRA_RESUME_POSITION_MS, 0L).coerceAtLeast(0L)
        } else {
            savedProgressMs(playlist[currentTrackIndex].id)
        }
        persistLastPlaybackState()
        playTrack(currentTrackIndex, savedProgressMs)
    }

    private fun parseTracksJson(json: String) {
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val url = obj.optString("url", "")
                if (url.isNotBlank()) playlist.add(
                    Track(
                        id = obj.optString("id", "").ifEmpty { url },
                        url = url,
                        name = obj.optString("name", "音频${i + 1}"),
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse tracksJson", e)
        }
    }

    private var currentDownloadJob: kotlinx.coroutines.Job? = null

    private fun playTrack(index: Int, resumePositionMs: Long = 0L) {
        if (index < 0 || index >= playlist.size) return
        currentTrackIndex = index
        recordedShuffleTrackId = null
        tailSkipTriggeredForTrack = false
        val track = playlist[index]
        val effectiveResumePositionMs = maxOf(resumePositionMs, skipHeadMs)
        errorRetryCount = 0
        playbackStateStore.update(
            transportState = PlaybackTransportState.PREPARING,
            trackIndex = index,
            trackCount = playlist.size,
            trackName = track.name,
            nextTrackName = if (playlist.size > 1) {
                playlist[(index + 1) % playlist.size].name
            } else {
                null
            },
            replaceNextTrackName = true,
            positionMs = effectiveResumePositionMs,
            durationMs = 0,
            message = "正在准备音频",
        )
        persistLastPlaybackState()

        // 取消上一个下载任务 - 避免并发下载竞态
        currentDownloadJob?.cancel()

        if (PlaybackLaunchContract.isDirectSource(track.url)) {
            val uri = if (track.url.startsWith("/")) {
                Uri.fromFile(File(track.url))
            } else {
                Uri.parse(track.url)
            }
            prepareAndPlay(uri, effectiveResumePositionMs)
        } else {
            currentDownloadJob = serviceScope.launch {
                val localFile = downloadToCache(track.url)
                withContext(Dispatchers.Main) {
                    if (currentTrackIndex == index) { // 确保没有被切换到其他 track
                        if (localFile != null) {
                            prepareAndPlay(Uri.fromFile(localFile), effectiveResumePositionMs)
                        } else {
                            prepareAndPlay(Uri.parse(track.url), effectiveResumePositionMs)
                        }
                    }
                }
            }
        }
    }

    /**
     * 对齐喜马拉雅 MediaControlManagerCompat.addMetaData：锁屏同时提供封面、
     * 标题、展示标题、专辑、作者和展示副标题。时长与播放位置由 Media3
     * 从当前 Player timeline 持续同步给系统媒体会话。
     */
    private fun buildMediaMetadata(): MediaMetadata {
        val title = playlist.getOrNull(currentTrackIndex)?.name ?: taskName
        return MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .setAlbumTitle(taskName)
            .setArtist("梦枕")
            .setSubtitle("梦枕")
            .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .apply {
                coverUrl?.takeIf(String::isNotBlank)?.let { setArtworkUri(Uri.parse(it)) }
                coverArtworkData?.let {
                    setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            }
            .build()
    }

    private fun updateCurrentMediaMetadata() {
        val p = player ?: return
        val mediaItem = p.currentMediaItem ?: return
        val index = p.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return
        p.replaceMediaItem(
            index,
            mediaItem.buildUpon().setMediaMetadata(buildMediaMetadata()).build(),
        )
    }

    private fun prepareAndPlay(uri: Uri, resumePositionMs: Long = 0L) {
        val p = player ?: return
        p.stop()
        p.clearMediaItems()
        playbackStateStore.update(
            transportState = PlaybackTransportState.PREPARING,
            positionMs = resumePositionMs,
            durationMs = 0,
            message = "正在缓冲音频",
        )

        val mediaItem = MediaItem.Builder()
            .setMediaId(playlist.getOrNull(currentTrackIndex)?.id.orEmpty())
            .setUri(uri)
            .setMediaMetadata(buildMediaMetadata())
            .build()

        val fadeDurationMs = fadeInDuration.coerceAtLeast(0) * 1_000L
        val fadeElapsedMs = if (executionClockStarted) {
            (System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L)
        } else {
            0L
        }
        val fadeInActive = desiredPlaying && enableFade &&
            fadeDurationMs > 0L && fadeElapsedMs < fadeDurationMs
        if (fadeInActive) {
            gainEnvelope.startFadeIn(fadeDurationMs, fadeElapsedMs)
        } else {
            gainEnvelope.reset()
        }

        p.setMediaItem(mediaItem)
        // 由服务统一处理曲目自然结束，才能准确支持“播完 N 集后停止”。
        p.repeatMode = Player.REPEAT_MODE_OFF
        p.prepare()
        p.volume = targetVolume / 100f
        if (desiredPlaying) {
            p.play()
        } else {
            p.pause()
        }

        // 断点续播 - 恢复播放进度
        if (resumePositionMs > 0) {
            // 对标喜马拉雅 i.java isCausedByPositionOutOfRange
            // 等待 STATE_READY 后在 listener 里检查 position 是否越界
            // 这里先 seekTo，如果越界会在 STATE_READY 时重置为 0
            p.seekTo(resumePositionMs)
            Log.i(tag, "Resumed from ${resumePositionMs / 1000}s")
        }

        fadeInCompletionRunnable?.let(handler::removeCallbacks)
        fadeInCompletionRunnable = null
        if (fadeInActive) {
            fadeInRemainingMs = fadeDurationMs - fadeElapsedMs
            playbackStateStore.update(phase = TaskPhase.FADING_IN)
        } else {
            fadeInRemainingMs = 0L
            playbackStateStore.update(
                transportState = if (desiredPlaying) {
                    PlaybackTransportState.PREPARING
                } else {
                    PlaybackTransportState.PAUSED
                },
                phase = if (desiredPlaying) TaskPhase.PLAYING else TaskPhase.IDLE,
            )
        }

        updateNotification(
            if (desiredPlaying) {
                "正在播放: ${playlist.getOrNull(currentTrackIndex)?.name ?: taskName}"
            } else {
                "暂停中: ${playlist.getOrNull(currentTrackIndex)?.name ?: taskName}"
            }
        )
        schedulePlaybackEnd()
        persistLastPlaybackState()
    }

    private fun beginPlaybackClockIfNeeded() {
        if (executionClockStarted) return
        val startedAt = System.currentTimeMillis()
        startTimeMs = startedAt
        executionClockStarted = true
        if (scheduledExecution) {
            val taskId = currentTaskId
            val scheduledEnd = taskId?.let {
                AlarmScheduler.get(this).onPlaybackActuallyStarted(it, startedAt)
            }
            sleepTimerEndTimeMs = scheduledEnd
                ?: scheduledStopDurationSeconds.takeIf { it > 0 }?.let {
                    startedAt + it * 1_000L
                }
                ?: 0L
        }
        refreshEffectiveEndTime()
        playbackStateStore.update(
            startedAt = startedAt,
            endsAt = endTimeMs,
            phase = if (enableFade && fadeInRemainingMs > 0L) {
                TaskPhase.FADING_IN
            } else {
                TaskPhase.PLAYING
            },
        )
        schedulePlaybackEnd()
        persistLastPlaybackState()
    }

    private fun scheduleFadeInPhaseCompletion() {
        if (fadeInRemainingMs <= 0L || fadeInCompletionRunnable != null) return
        val activePlayer = player ?: return
        val completion = Runnable {
            fadeInRemainingMs = 0L
            fadeInCompletionRunnable = null
            if (activePlayer == player && activePlayer.isPlaying && !finishingPlayback) {
                playbackStateStore.update(phase = TaskPhase.PLAYING)
            }
        }
        fadeInCompletionRunnable = completion
        handler.postDelayed(completion, fadeInRemainingMs)
    }

    private fun startFadeOut() {
        val p = player ?: return
        if (finishingPlayback) return
        finishingPlayback = true
        if (!enableFadeOut || fadeOutDuration <= 0) {
            finishCurrentPlayback("本次播放已完成")
            return
        }

        val configuredDurationMs = fadeOutDuration * 1_000L
        val remainingDeadlineMs = if (endTimeMs > 0L) {
            (endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
        } else {
            configuredDurationMs
        }
        val effectiveDurationMs = minOf(configuredDurationMs, remainingDeadlineMs)
        if (effectiveDurationMs <= 0L) {
            finishCurrentPlayback("本次播放已完成")
            return
        }
        playbackStateStore.update(phase = TaskPhase.FADING_OUT)
        gainEnvelope.startFadeOut(effectiveDurationMs)
        fadeOutCompletionRunnable?.let(handler::removeCallbacks)
        val completion = Runnable {
            if (p == player) finishCurrentPlayback("本次播放已完成")
        }
        fadeOutCompletionRunnable = completion
        handler.postDelayed(completion, effectiveDurationMs)
    }

    private fun cancelActiveFadeOut() {
        if (!finishingPlayback) return
        finishingPlayback = false
        fadeOutCompletionRunnable?.let(handler::removeCallbacks)
        fadeOutCompletionRunnable = null
        gainEnvelope.reset()
        playbackStateStore.update(
            phase = if (desiredPlaying) TaskPhase.PLAYING else TaskPhase.IDLE,
        )
    }

    private fun schedulePlaybackEnd() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        refreshEffectiveEndTime()
        if (endTimeMs <= 0L) {
            Log.i(tag, "Playback end timer disabled")
            return
        }
        val durationMs = endTimeMs - System.currentTimeMillis()
        if (durationMs <= 0) {
            startFadeOut()
            return
        }
        val fadeOutMs = if (enableFadeOut && fadeOutDuration > 0) {
            fadeOutDuration * 1000L
        } else {
            0L
        }
        val stopAt = System.currentTimeMillis() + maxOf(0, durationMs - fadeOutMs)

        val reachedSleepDeadline = PlaybackLaunchContract.isSleepDeadline(
            executionEndTimeMs,
            sleepTimerEndTimeMs,
        )
        val finishTrackAtSleepDeadline =
            PlaybackLaunchContract.shouldFinishCurrentTrack(
                executionEndTimeMs,
                sleepTimerEndTimeMs,
                finishCurrentTrackAfterSleep,
            )
        val runnable = Runnable {
            if (finishTrackAtSleepDeadline) {
                Log.i(tag, "Sleep timer elapsed; waiting for current track to finish")
                stopAfterCurrentTrack = true
                sleepTimerEndTimeMs = 0L
                refreshEffectiveEndTime()
                updateNotification("将在本集播完后停止: $taskName")
                persistLastPlaybackState()
            } else {
                Log.i(tag, "Playback duration ended, starting fade out")
                startFadeOut()
            }
        }
        stopRunnable = runnable
        // Handler.postAtTime 使用 uptimeMillis；这里持有的是 epoch millis，必须换算成 delay。
        handler.postDelayed(runnable, (stopAt - System.currentTimeMillis()).coerceAtLeast(0L))
        Log.i(tag, "Scheduled stop in ${durationMs / 1000}s")
    }

    private fun refreshEffectiveEndTime() {
        endTimeMs = PlaybackLaunchContract.earliestPositiveDeadline(
            executionEndTimeMs,
            sleepTimerEndTimeMs,
        )
    }

    private fun formatTimerDuration(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val hours = safeSeconds / 3_600
        val minutes = safeSeconds % 3_600 / 60
        val remainder = safeSeconds % 60
        return buildString {
            if (hours > 0) append("${hours}小时")
            if (minutes > 0) append("${minutes}分钟")
            if (remainder > 0 || isEmpty()) append("${remainder}秒")
        }
    }

    private fun onTrackPlaybackEnded() {
        if (playlist.isEmpty() || finishingPlayback) return
        if (stopAfterCurrentTrack) {
            stopAfterCurrentTrack = false
            startFadeOut()
            return
        }
        if (remainingTracksUntilStop > 0) {
            remainingTracksUntilStop--
            persistLastPlaybackState()
            if (remainingTracksUntilStop == 0) {
                startFadeOut()
                return
            }
        }
        if (!autoContinue) {
            finishCurrentPlayback("本次播放已完成")
            return
        }
        if (
            PlaybackLaunchContract.shouldStopAtPlaylistEnd(
                stopAtPlaylistEnd = stopAtPlaylistEnd,
                currentTrackIndex = currentTrackIndex,
                lastTrackIndex = playlist.lastIndex,
            )
        ) {
            finishCurrentPlayback("所选音频已播放完毕")
            return
        }
        playNextTrack(fromCompletion = true)
    }

    private fun finishCurrentPlayback(message: String) {
        val finishedTaskId = currentTaskId
        val finishedScheduledExecution = scheduledExecution
        playbackStateStore.finish(
            taskId = finishedTaskId,
            transportState = PlaybackTransportState.COMPLETED,
            message = message,
        )
        stopPlaybackInternal()
        if (finishedScheduledExecution) {
            finishedTaskId?.let { AlarmScheduler.get(this).onPlaybackCompleted(it) }
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun completeScheduledExecutionIfNeeded(taskId: String?) {
        if (scheduledExecution) {
            taskId?.let { AlarmScheduler.get(this).onPlaybackCompleted(it) }
        }
    }

    private fun playNextTrack(fromCompletion: Boolean = false) {
        if (playlist.isEmpty()) return
        if (!scheduledExecution && fromCompletion && playbackMode == QqMusicPlaybackMode.SINGLE_REPEAT) {
            playTrack(currentTrackIndex, 0L)
            return
        }
        val next = if (!scheduledExecution && playbackMode == QqMusicPlaybackMode.SHUFFLE) {
            nextShuffleTrackIndex()
        } else if (currentTrackIndex + 1 < playlist.size) {
            currentTrackIndex + 1
        } else {
            0
        }
        playTrack(next)
    }

    private fun playPrevTrack() {
        if (playlist.isEmpty()) return
        val prev = if (!scheduledExecution && playbackMode == QqMusicPlaybackMode.SHUFFLE) {
            previousShuffleTrackIndex()
        } else if (currentTrackIndex - 1 >= 0) {
            currentTrackIndex - 1
        } else {
            playlist.size - 1
        }
        playTrack(prev)
    }

    private fun resetShuffleTraversal() {
        shuffleRemaining.clear()
        shuffleHistory.clear()
    }

    private fun nextShuffleTrackIndex(): Int {
        if (playlist.size <= 1) return currentTrackIndex
        if (shuffleRemaining.isEmpty()) {
            val history = getShufflePlayHistory(this)
            val frequency = history.groupingBy(ShuffleHistoryEntry::id).eachCount()
            val lastPlayedAt = history.associate { it.id to it.playedAt }
            val candidates = playlist.indices
                .filter { it != currentTrackIndex }
                .shuffled()
            shuffleRemaining += when (shufflePreference) {
                QqMusicShufflePreference.DEFAULT -> candidates
                QqMusicShufflePreference.FRESH_EXPLORE -> candidates.sortedWith(
                    compareBy<Int>(
                        { frequency[playlist[it].id] ?: 0 },
                        { lastPlayedAt[playlist[it].id] ?: 0L },
                    ),
                )
                QqMusicShufflePreference.RECENT_FREQUENT -> candidates.sortedWith(
                    compareByDescending<Int> { frequency[playlist[it].id] ?: 0 }
                        .thenByDescending { lastPlayedAt[playlist[it].id] ?: 0L },
                )
            }
        }
        shuffleHistory += currentTrackIndex
        return shuffleRemaining.removeAt(0)
    }

    private fun recordShuffleTrackIfNeeded() {
        if (scheduledExecution || playbackMode != QqMusicPlaybackMode.SHUFFLE) return
        val track = playlist.getOrNull(currentTrackIndex) ?: return
        if (recordedShuffleTrackId == track.id) return
        recordedShuffleTrackId = track.id
        val history = getShufflePlayHistory(this).toMutableList()
        history += ShuffleHistoryEntry(track.id, track.name, System.currentTimeMillis())
        while (history.size > MAX_SHUFFLE_HISTORY) history.removeAt(0)
        saveShufflePlayHistory(this, history)
    }

    private fun previousShuffleTrackIndex(): Int {
        if (playlist.size <= 1 || shuffleHistory.isEmpty()) return currentTrackIndex
        shuffleRemaining.add(0, currentTrackIndex)
        return shuffleHistory.removeAt(shuffleHistory.lastIndex)
    }

    private fun removeTrackAt(index: Int) {
        if (index !in playlist.indices) return
        val removingCurrent = index == currentTrackIndex
        playlist.removeAt(index)
        resetShuffleTraversal()
        if (playlist.isEmpty()) {
            finishCurrentPlayback("播放列表已清空")
            return
        }
        when {
            index < currentTrackIndex -> currentTrackIndex--
            removingCurrent -> {
                currentTrackIndex = index.coerceAtMost(playlist.lastIndex)
                desiredPlaying = true
                playTrack(currentTrackIndex, 0L)
                return
            }
        }
        persistLastPlaybackState()
        publishQueueState()
    }

    /**
     * Queue reorder committed by QQ Music's handle-only ItemTouchHelper flow.
     * The currently playing media item is not restarted; only its queue index
     * and the persisted traversal order change.
     */
    private fun moveTrack(from: Int, to: Int) {
        if (from !in playlist.indices || to !in playlist.indices || from == to) return
        val moved = playlist.removeAt(from)
        playlist.add(to, moved)
        currentTrackIndex = when {
            currentTrackIndex == from -> to
            from < currentTrackIndex && to >= currentTrackIndex -> currentTrackIndex - 1
            from > currentTrackIndex && to <= currentTrackIndex -> currentTrackIndex + 1
            else -> currentTrackIndex
        }
        resetShuffleTraversal()
        persistLastPlaybackState()
        publishQueueState()
    }

    private fun publishQueueState() {
        val current = playlist.getOrNull(currentTrackIndex) ?: return
        playbackStateStore.update(
            trackIndex = currentTrackIndex,
            trackCount = playlist.size,
            trackName = current.name,
            nextTrackName = if (playlist.size > 1) {
                playlist[(currentTrackIndex + 1) % playlist.size].name
            } else {
                null
            },
            replaceNextTrackName = true,
        )
        updateCurrentMediaMetadata()
    }

    private fun requestAudioFocus(): Boolean {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()
        audioFocusRequest = request
        return audioManager?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun releaseAudioFocus() {
        val request = audioFocusRequest ?: return
        audioManager?.abandonAudioFocusRequest(request)
        audioFocusRequest = null
    }

    /** 断点续播 - 保存进度 - 对标喜马拉雅播放进度存储
     *  本地每秒保存 + 云端异步同步
     */
    private fun saveProgress() {
        if (!breakpointResumeEnabled()) return
        val p = player ?: return
        val track = playlist.getOrNull(currentTrackIndex) ?: return
        if (!p.isPlaying && p.currentPosition <= 0) return
        val store = PlayProgressStore.get(this)
        val positionSec = p.currentPosition / 1000
        val durationSec = p.duration / 1000
        store.saveLocal(track.id, positionSec, durationSec)
        // 云端同步 - 异步，不阻塞播放
        serviceScope.launch { store.saveToCloud(track.id, positionSec, durationSec) }
        Log.d(tag, "Saved progress: ${track.id} at ${positionSec}s")
    }

    private fun breakpointResumeEnabled(): Boolean =
        AppSettingsStore.get(this)
            .getBoolean(AppSettingsStore.KEY_BREAKPOINT_RESUME, true)

    private fun savedProgressMs(audioId: String): Long =
        if (breakpointResumeEnabled()) {
            (PlayProgressStore.get(this).getLocal(audioId)?.first ?: 0L) * 1_000L
        } else {
            0L
        }

    /** 通知栏进度条更新 */
    private fun startProgressUpdates() {
        stopProgressUpdates()
        val runnable = object : Runnable {
            override fun run() {
                val p = player ?: return
                if (p.isPlaying) {
                    if (shouldSkipCurrentTail(p)) {
                        onTrackPlaybackEnded()
                        return
                    }
                    saveProgress()
                    playbackStateStore.update(
                        transportState = PlaybackTransportState.PLAYING,
                        positionMs = p.currentPosition,
                        durationMs = p.duration.takeIf { it != C.TIME_UNSET } ?: 0,
                    )
                    persistLastPlaybackState()
                    handler.postDelayed(this, 1000)
                }
            }
        }
        progressRunnable = runnable
        handler.post(runnable)
    }

    private fun shouldSkipCurrentTail(p: Player): Boolean {
        if (tailSkipTriggeredForTrack || skipTailMs <= 0L) return false
        val duration = p.duration
        if (duration <= 1_000L || duration == C.TIME_UNSET) return false
        // 至少保留 1 秒可播放区间，避免极短本地音频被头尾设置直接吞掉。
        val effectiveTail = skipTailMs.coerceAtMost((duration - 1_000L).coerceAtLeast(0L))
        if (effectiveTail <= 0L || p.currentPosition < duration - effectiveTail) return false
        tailSkipTriggeredForTrack = true
        return true
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun stopPlaybackInternal(preventRestart: Boolean = true) {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        saveProgress()
        stopProgressUpdates()
        player?.stop()
        player?.clearMediaItems()
        if (preventRestart) {
            currentTaskId = null
            getSharedPreferences("dream_pillow_playback", MODE_PRIVATE)
                .edit()
                .remove("last_state")
                .apply()
        }
        errorRetryCount = 0
        fadeInCompletionRunnable?.let { handler.removeCallbacks(it) }
        fadeOutCompletionRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null) // 清理所有待执行回调，包括耳机去抖
        fadeInCompletionRunnable = null
        fadeOutCompletionRunnable = null
        stopRunnable = null
        fadeInRemainingMs = 0L
        scheduledStopDurationSeconds = 0
        executionClockStarted = false
        gainEnvelope.reset()
        coverBitmap = null
        coverArtworkData = null
        releaseAudioFocus()
        SustainedListenService.unbind(this)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(channelId, "梦枕播放", NotificationManager.IMPORTANCE_LOW).apply {
            description = "音频播放服务"
            setShowBadge(false)
        }
        (getSystemService(NotificationManager::class.java))?.createNotificationChannel(channel)
    }

    /** 喜马拉雅 9.5.1.4 XmNotificationCreater 的 MediaStyle 通知结构。 */
    private fun buildNotification(contentText: String): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            currentTaskId?.let {
                putExtra(MainActivity.EXTRA_OPEN_PLAYBACK_TASK_ID, it)
            }
        }
        val contentPending = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPlaying = player?.isPlaying == true
        val title = playlist.getOrNull(currentTrackIndex)?.name ?: taskName
        val subtitle = if (playlist.isEmpty()) {
            contentText
        } else {
            taskName.takeUnless { it == title }.orEmpty().ifBlank { "梦枕" }
        }
        val playPauseAction = if (isPlaying) ACTION_PAUSE else ACTION_RESUME
        val playPauseIcon = if (isPlaying) {
            R.drawable.xm_notify_v9514_0x7f080079
        } else {
            R.drawable.xm_notify_v9514_0x7f08007a
        }
        val playPauseLabel = if (isPlaying) "暂停" else "播放"

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setLargeIcon(coverBitmap)
            .setSmallIcon(R.drawable.xm_notify_v9514_small_icon)
            .setGroup("player")
            .setGroupSummary(false)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setContentIntent(contentPending)
            .setDeleteIntent(actionPending(ACTION_STOP, 0))
            .addAction(
                R.drawable.xm_notify_v9514_0x7f080073,
                "减15秒",
                actionPending(ACTION_SEEK_BACK, 1),
            )
            .addAction(
                R.drawable.xm_notify_v9514_0x7f08007c,
                "上一首",
                actionPending(ACTION_PREV, 2),
            )
            .addAction(playPauseIcon, playPauseLabel, actionPending(playPauseAction, 3))
            .addAction(
                R.drawable.xm_notify_v9514_0x7f080078,
                "下一首",
                actionPending(ACTION_NEXT, 4),
            )
            .addAction(
                R.drawable.xm_notify_v9514_0x7f080074,
                "加15秒",
                actionPending(ACTION_SEEK_FORWARD, 5),
            )

        // 关联 MediaSession - Android 13+ 锁屏控件必需
        // 对标喜马拉雅通知栏 MediaStyle
        mediaSession?.let { session ->
            builder.setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(1, 2, 3)
            )
        }

        return builder.build()
    }

    private var coverBitmap: Bitmap? = null // 封面缓存，避免每秒重复加载
    private var coverArtworkData: ByteArray? = null

    /** 异步加载封面 - 避免主线程 ANR */
    private fun loadCoverAsync(url: String?) {
        if (coverBitmap != null) return // 已加载
        val requestedUrl = url
        serviceScope.launch {
            val decoded = if (url.isNullOrBlank()) {
                BitmapFactory.decodeResource(resources, R.drawable.xm_main_v9514_default_cover)
            } else {
                try {
                    val uri = Uri.parse(url)
                    when (uri.scheme?.lowercase()) {
                        "content", "file", "android.resource" ->
                            contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                        "http", "https" -> {
                            val conn = URL(url).openConnection() as HttpURLConnection
                            try {
                                conn.connectTimeout = 5000
                                conn.readTimeout = 5000
                                conn.connect()
                                if (conn.responseCode == 200) {
                                    conn.inputStream.use(BitmapFactory::decodeStream)
                                } else {
                                    null
                                }
                            } finally {
                                conn.disconnect()
                            }
                        }
                        else -> BitmapFactory.decodeFile(url)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            val bitmap = decoded?.let(::fitLockScreenArtwork)
            if (bitmap != null) {
                if (coverUrl != requestedUrl) return@launch
                coverBitmap = bitmap
                coverArtworkData = encodeLockScreenArtwork(bitmap)
                withContext(Dispatchers.Main) {
                    updateCurrentMediaMetadata()
                    updateNotification("正在播放: ${playlist.getOrNull(currentTrackIndex)?.name ?: taskName}")
                }
            }
        }
    }

    private fun fitLockScreenArtwork(bitmap: Bitmap): Bitmap {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        if (longestSide <= LOCK_SCREEN_ARTWORK_SIZE_PX) return bitmap
        val scale = LOCK_SCREEN_ARTWORK_SIZE_PX.toFloat() / longestSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun encodeLockScreenArtwork(bitmap: Bitmap): ByteArray =
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            output.toByteArray()
        }

    private fun actionPending(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AudioPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun updateNotification(text: String) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notify(notificationId, buildNotification(text))
    }

    private fun updateMediaSessionState(isPlaying: Boolean) {
        // Media3 会自动同步播放状态到 MediaSession，无需手动设置
    }

    private fun downloadToCache(audioUrl: String): File? {
        return downloadAudioToCache(this, audioUrl)
    }

    /** 返回 URL 对应的下载缓存文件（如果存在） */
    private fun downloadToCacheFilePath(audioUrl: String): File? {
        if (audioUrl.isEmpty()) return null
        return existingAudioDownloadFile(this, audioUrl)
            ?: audioDownloadCacheFile(this, audioUrl, persistent = false)
    }

    /** 持久化当前会话，供显式进程恢复使用。 */
    private fun persistLastPlaybackState() {
        if (currentTaskId == null || playlist.isEmpty()) return
        val tracks = JSONArray()
        playlist.forEach { track ->
            tracks.put(
                JSONObject()
                    .put("id", track.id)
                    .put("url", track.url)
                    .put("name", track.name)
            )
        }
        val json = JSONObject()
        json.put("taskId", currentTaskId ?: "")
        json.put("taskName", taskName)
        json.put("playDurationMinutes", playDurationMinutes)
        json.put("executionDurationSeconds", scheduledStopDurationSeconds)
        json.put("volume", targetVolume)
        json.put("enableFade", enableFade)
        json.put("enableFadeOut", enableFadeOut)
        json.put("fadeInDuration", fadeInDuration)
        json.put("fadeOutDuration", fadeOutDuration)
        json.put("loopSingle", loopSingle)
        json.put("autoContinue", autoContinue)
        json.put("skipHeadSeconds", (skipHeadMs / 1_000L).toInt())
        json.put("skipTailSeconds", (skipTailMs / 1_000L).toInt())
        json.put("coverUrl", coverUrl ?: "")
        json.put("endTime", endTimeMs)
        json.put("executionEndTime", executionEndTimeMs)
        json.put("sleepTimerEndTime", sleepTimerEndTimeMs)
        json.put("tracksJson", tracks.toString())
        json.put("desiredPlaying", desiredPlaying)
        json.put("scheduledExecution", scheduledExecution)
        json.put("trackIndex", currentTrackIndex)
        json.put("resumePositionMs", player?.currentPosition?.coerceAtLeast(0L) ?: 0L)
        json.put("remainingTracks", remainingTracksUntilStop)
        json.put("finishCurrentTrackAfterSleep", finishCurrentTrackAfterSleep)
        json.put("stopAfterCurrentTrack", stopAfterCurrentTrack)
        json.put("stopAtPlaylistEnd", stopAtPlaylistEnd)
        json.put("startedAt", startTimeMs)
        json.put("timestamp", System.currentTimeMillis())
        getSharedPreferences("dream_pillow_playback", MODE_PRIVATE)
            .edit().putString("last_state", json.toString()).apply()
    }

    /** 读取上次播放状态 */
    private fun getLastPlaybackState(): Bundle? {
        val raw = getSharedPreferences("dream_pillow_playback", MODE_PRIVATE)
            .getString("last_state", null) ?: return null
        return try {
            val json = JSONObject(raw)
            val timestamp = json.optLong("timestamp", 0)
            // 超过 1 小时不恢复
            if (System.currentTimeMillis() - timestamp > 60 * 60 * 1000L) return null
            val bundle = Bundle()
            json.optString("taskId").takeIf { it.isNotEmpty() }?.let { bundle.putString("taskId", it) }
            bundle.putString("taskName", json.optString("taskName", "梦枕"))
            bundle.putInt("playDurationMinutes", json.optInt("playDurationMinutes", 30))
            bundle.putInt(
                EXTRA_EXECUTION_DURATION_SECONDS,
                json.optInt("executionDurationSeconds", 0).coerceAtLeast(0),
            )
            bundle.putInt("volume", json.optInt("volume", 70))
            bundle.putBoolean("enableFade", json.optBoolean("enableFade", false))
            bundle.putBoolean(
                "enableFadeOut",
                if (json.has("enableFadeOut")) {
                    json.optBoolean("enableFadeOut", false)
                } else {
                    json.optInt("fadeOutDuration", 0) > 0
                },
            )
            bundle.putInt("fadeInDuration", json.optInt("fadeInDuration", 0))
            bundle.putInt("fadeOutDuration", json.optInt("fadeOutDuration", 0))
            bundle.putBoolean("loopSingle", json.optBoolean("loopSingle", true))
            bundle.putBoolean(
                EXTRA_AUTO_CONTINUE,
                json.optBoolean("autoContinue", true),
            )
            bundle.putInt(
                EXTRA_SKIP_HEAD_SECONDS,
                json.optInt("skipHeadSeconds", 0).coerceIn(0, MAX_SKIP_SECONDS),
            )
            bundle.putInt(
                EXTRA_SKIP_TAIL_SECONDS,
                json.optInt("skipTailSeconds", 0).coerceIn(0, MAX_SKIP_SECONDS),
            )
            json.optString("coverUrl").takeIf { it.isNotEmpty() }?.let { bundle.putString("coverUrl", it) }
            bundle.putLong("endTime", json.optLong("endTime", 0))
            bundle.putLong(
                EXTRA_EXECUTION_END_TIME,
                json.optLong("executionEndTime", json.optLong("endTime", 0)),
            )
            bundle.putLong(EXTRA_SLEEP_TIMER_END_TIME, json.optLong("sleepTimerEndTime", 0))
            json.optString("tracksJson").takeIf { it.isNotEmpty() }?.let { bundle.putString("tracksJson", it) }
            bundle.putBoolean(EXTRA_DESIRED_PLAYING, json.optBoolean("desiredPlaying", true))
            bundle.putBoolean(EXTRA_IS_SCHEDULED_EXECUTION, json.optBoolean("scheduledExecution", true))
            bundle.putInt(EXTRA_TRACK_INDEX, json.optInt("trackIndex", 0))
            bundle.putLong(EXTRA_RESUME_POSITION_MS, json.optLong("resumePositionMs", 0))
            bundle.putInt(EXTRA_REMAINING_TRACKS, json.optInt("remainingTracks", 0))
            bundle.putBoolean(
                EXTRA_FINISH_CURRENT_TRACK,
                json.optBoolean("finishCurrentTrackAfterSleep", false),
            )
            bundle.putBoolean(
                EXTRA_STOP_AFTER_CURRENT_TRACK,
                json.optBoolean("stopAfterCurrentTrack", false),
            )
            bundle.putBoolean(
                EXTRA_STOP_AT_PLAYLIST_END,
                json.optBoolean("stopAtPlaylistEnd", false),
            )
            bundle.putLong(EXTRA_STARTED_AT, json.optLong("startedAt", timestamp))
            bundle
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse last state", e)
            null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (currentTaskId != null) persistLastPlaybackState()
        stopPlaybackInternal(preventRestart = false)
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        currentTaskId = null
        if (phoneListenerRegistered) {
            telephonyCallback?.let { callback ->
                try {
                    telephonyManager?.unregisterTelephonyCallback(callback)
                } catch (error: SecurityException) {
                    Log.w(tag, "Unable to unregister call-state listener", error)
                }
            }
            phoneListenerRegistered = false
        }
        telephonyCallback = null
        if (noisyReceiverRegistered) {
            unregisterReceiver(noisyReceiver)
            noisyReceiverRegistered = false
        }
        if (networkCallbackRegistered) {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            connectivityManager?.unregisterNetworkCallback(networkCallback)
            networkCallbackRegistered = false
        }
        player?.removeListener(playerListener)
        releaseSoundEffects()
        player?.release()
        player = null
        mediaSession?.release()
        mediaSession = null
        cache?.release()
        cache = null
        serviceScope.cancel()
        instance = null
        super.onDestroy()
        Log.i(tag, "Service destroyed")
    }

    companion object {
        const val ACTION_START = "com.mengzhen.app.START_PLAYBACK"
        const val ACTION_STOP = "com.mengzhen.app.STOP_PLAYBACK"
        const val ACTION_PAUSE = "com.mengzhen.app.PAUSE_PLAYBACK"
        const val ACTION_RESUME = "com.mengzhen.app.RESUME_PLAYBACK"
        const val ACTION_NEXT = "com.mengzhen.app.NEXT_TRACK"
        const val ACTION_PREV = "com.mengzhen.app.PREV_TRACK"
        const val ACTION_SEEK_BACK = "com.mengzhen.app.SEEK_BACK_15"
        const val ACTION_SEEK_FORWARD = "com.mengzhen.app.SEEK_FORWARD_15"
        const val ACTION_PLAY_INDEX = "com.mengzhen.app.PLAY_TRACK_INDEX"
        const val ACTION_REMOVE_TRACK = "com.mengzhen.app.REMOVE_TRACK"
        const val ACTION_MOVE_TRACK = "com.mengzhen.app.MOVE_TRACK"
        const val ACTION_SEEK = "com.mengzhen.app.SEEK_TO"
        const val ACTION_SET_PLAYBACK_SPEED = "com.mengzhen.app.SET_PLAYBACK_SPEED"
        const val ACTION_SET_PLAYBACK_MODE = "com.mengzhen.app.SET_PLAYBACK_MODE"
        const val ACTION_SET_SOUND_EFFECT = "com.mengzhen.app.SET_SOUND_EFFECT"
        const val ACTION_SET_SLEEP_TIMER = "com.mengzhen.app.SET_SLEEP_TIMER"
        const val ACTION_SET_SLEEP_FADE_OUT = "com.mengzhen.app.SET_SLEEP_FADE_OUT"
        const val ACTION_SET_SLEEP_AFTER_TRACKS = "com.mengzhen.app.SET_SLEEP_AFTER_TRACKS"
        const val ACTION_SET_SLEEP_FINISH_CURRENT_TRACK =
            "com.mengzhen.app.SET_SLEEP_FINISH_CURRENT_TRACK"
        const val ACTION_SET_AUTO_CONTINUE = "com.mengzhen.app.SET_AUTO_CONTINUE"
        const val ACTION_SET_SKIP_HEAD_TAIL = "com.mengzhen.app.SET_SKIP_HEAD_TAIL"
        const val ACTION_RESTART = "com.mengzhen.app.RESTART_PLAYBACK"

        const val EXTRA_IS_SCHEDULED_EXECUTION = "isScheduledExecution"
        const val EXTRA_EXECUTION_DURATION_SECONDS = "executionDurationSeconds"
        const val EXTRA_TRACK_INDEX = "trackIndex"
        private const val EXTRA_FROM_TRACK_INDEX = "fromTrackIndex"
        private const val EXTRA_TO_TRACK_INDEX = "toTrackIndex"
        const val EXTRA_SKIP_HEAD_SECONDS = "skipHeadSeconds"
        const val EXTRA_SKIP_TAIL_SECONDS = "skipTailSeconds"
        private const val EXTRA_MINUTES = "minutes"
        private const val EXTRA_TRACK_COUNT = "trackCount"
        private const val EXTRA_FADE_OUT_ENABLED = "fadeOutEnabled"
        private const val EXTRA_FADE_OUT_SECONDS = "fadeOutSeconds"
        private const val EXTRA_DESIRED_PLAYING = "desiredPlaying"
        private const val EXTRA_RESUME_POSITION_MS = "resumePositionMs"
        private const val EXTRA_EXECUTION_END_TIME = "executionEndTime"
        const val EXTRA_SLEEP_TIMER_END_TIME = "sleepTimerEndTime"
        private const val EXTRA_SLEEP_DURATION_SECONDS = "sleepDurationSeconds"
        const val EXTRA_REMAINING_TRACKS = "remainingTracks"
        const val EXTRA_FINISH_CURRENT_TRACK = "finishCurrentTrack"
        private const val EXTRA_STOP_AFTER_CURRENT_TRACK = "stopAfterCurrentTrack"
        const val EXTRA_STOP_AT_PLAYLIST_END = "stopAtPlaylistEnd"
        private const val EXTRA_AUTO_CONTINUE = "autoContinue"
        private const val EXTRA_SKIP_TASK_ID = "skipTaskId"
        const val EXTRA_STARTED_AT = "startedAt"
        private const val PLAYBACK_PREFS = "dream_pillow_playback"
        private const val PENDING_PLAYBACK_SPEED = "pending_playback_speed"
        private const val PENDING_PLAYBACK_MODE = "pending_playback_mode"
        private const val SHUFFLE_PREFERENCE = "KEY_SHUFFLE_PLAY_ADJUST_LISTEN_COUNT_TYPE"
        private const val SHUFFLE_HISTORY = "qq_shuffle_play_history"
        private const val PENDING_SOUND_EFFECT = "pending_sound_effect"
        private const val PENDING_AUTO_CONTINUE = "pending_auto_continue"
        private const val PENDING_LOCK_SCREEN_CONTROL = "pending_lock_screen_control"
        private const val EXTRA_PLAYBACK_SPEED = "playback_speed"
        private const val EXTRA_PLAYBACK_MODE = "playback_mode"
        private const val EXTRA_SOUND_EFFECT = "sound_effect"
        private const val MIN_PLAYBACK_SPEED = 0.5f
        private const val MAX_PLAYBACK_SPEED = 3.0f
        private const val MAX_SKIP_SECONDS = 120
        private const val SEEK_INTERVAL_MS = 15_000L
        private const val LOCK_SCREEN_ARTWORK_SIZE_PX = 512
        private const val MAX_SHUFFLE_HISTORY = 100
        @Volatile private var instance: AudioPlaybackService? = null

        fun isCurrentlyPlaying(): Boolean = instance?.player?.isPlaying == true
        fun getCurrentTaskId(): String? = instance?.currentTaskId
        fun isCurrentSessionScheduled(): Boolean = instance?.scheduledExecution == true
        fun getCurrentTrackIndex(): Int = instance?.currentTrackIndex ?: 0
        fun getCurrentQueueIds(): List<String> =
            instance?.playlist?.map(Track::id).orEmpty()

        fun cacheAudioUri(context: Context, audioUrl: String): String? =
            downloadAudioToCache(
                context = context.applicationContext,
                audioUrl = audioUrl,
                persistent = true,
            )?.let(Uri::fromFile)?.toString()

        fun getCachedAudioUri(context: Context, audioUrl: String): String? =
            audioDownloadCacheFile(
                context = context.applicationContext,
                audioUrl = audioUrl,
                persistent = true,
            ).takeIf { it.isFile && it.length() > 0L }
                ?.let(Uri::fromFile)
                ?.toString()

        fun getTransientCacheSize(context: Context): Long {
            val appContext = context.applicationContext
            val simpleCacheBytes = instance?.cache?.cacheSpace
                ?: directorySize(File(appContext.cacheDir, "audio_cache"))
            return simpleCacheBytes +
                directorySize(File(appContext.cacheDir, "audio_download")) +
                directorySize(File(appContext.cacheDir, "bili_offline"))
        }

        fun clearTransientCache(context: Context): Long {
            val appContext = context.applicationContext
            val before = getTransientCacheSize(appContext)
            instance?.cache?.let { activeCache ->
                activeCache.keys.toList().forEach { key ->
                    runCatching { activeCache.removeResource(key) }
                }
            } ?: deleteDirectoryContents(File(appContext.cacheDir, "audio_cache"))
            deleteDirectoryContents(File(appContext.cacheDir, "audio_download"))
            deleteDirectoryContents(File(appContext.cacheDir, "bili_offline"))
            return (before - getTransientCacheSize(appContext)).coerceAtLeast(0L)
        }

        fun getSleepTimerMinutes(context: Context): Int {
            val seconds = getSleepTimerRemainingSeconds(context)
            return if (seconds > 0) (seconds + 59) / 60 else 0
        }

        fun getSleepTimerRemainingSeconds(context: Context): Int {
            val activeEnd = instance?.sleepTimerEndTimeMs ?: 0L
            return if (activeEnd > System.currentTimeMillis()) {
                ((activeEnd - System.currentTimeMillis() + 999L) / 1_000L).toInt()
            } else {
                0
            }
        }

        fun getSleepTimerEndTimeMs(): Long = instance?.sleepTimerEndTimeMs ?: 0L

        fun getPlaybackStartedAtMs(): Long = instance?.startTimeMs
            ?.takeIf { instance?.executionClockStarted == true }
            ?: 0L

        fun getConfiguredSleepDurationSeconds(): Int =
            instance?.scheduledStopDurationSeconds?.coerceAtLeast(0) ?: 0

        fun getSleepTrackCount(context: Context): Int =
            instance?.remainingTracksUntilStop ?: 0

        fun getSleepFinishCurrentTrack(context: Context): Boolean =
            instance?.finishCurrentTrackAfterSleep ?: false

        fun isSleepFadeOutEnabled(): Boolean = instance?.enableFadeOut == true

        fun getSleepFadeOutSeconds(): Int =
            instance?.fadeOutDuration?.coerceAtLeast(0) ?: 0

        fun getAutoContinue(context: Context): Boolean =
            instance?.autoContinue
                ?: context.applicationContext
                    .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                    .getBoolean(PENDING_AUTO_CONTINUE, true)

        fun playHealing(
            context: Context,
            taskId: String,
            taskName: String,
            scene: QqMusicHealingScene,
        ) {
            val source = scene.sourceUri()
            val tracks = JSONArray().put(
                JSONObject()
                    .put("id", "qq_healing_${scene.sceneName}")
                    .put("url", source)
                    .put("name", scene.title),
            )
            val artwork = "android.resource://${context.packageName}/${scene.coverRes}"
            val intent = Intent(context, AudioPlaybackService::class.java)
                .setAction(ACTION_START)
                .putExtra("taskId", taskId.ifBlank { "qq_healing_${scene.sceneName}" })
                .putExtra("taskName", taskName.ifBlank { scene.title })
                .putExtra("tracksJson", tracks.toString())
                .putExtra("coverUrl", artwork)
                .putExtra("playDurationMinutes", 30)
                .putExtra("volume", 70)
                .putExtra(EXTRA_TRACK_INDEX, 0)
                .putExtra(EXTRA_RESUME_POSITION_MS, 0L)
                .putExtra(EXTRA_DESIRED_PLAYING, true)
                .putExtra(EXTRA_IS_SCHEDULED_EXECUTION, false)
                .putExtra(EXTRA_STOP_AT_PLAYLIST_END, true)
                .putExtra(EXTRA_AUTO_CONTINUE, false)
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) = sendAction(context, ACTION_PAUSE)

        fun resume(context: Context) = sendAction(context, ACTION_RESUME)

        fun next(context: Context) = sendAction(context, ACTION_NEXT)

        fun previous(context: Context) = sendAction(context, ACTION_PREV)

        fun playIndex(context: Context, index: Int) {
            if (index < 0) return
            val intent = Intent(context, AudioPlaybackService::class.java)
                .setAction(ACTION_PLAY_INDEX)
                .putExtra(EXTRA_TRACK_INDEX, index)
            sendIntent(context, intent)
        }

        fun removeTrack(context: Context, index: Int) {
            if (index < 0 || instance == null) return
            sendIntent(
                context,
                Intent(context, AudioPlaybackService::class.java)
                    .setAction(ACTION_REMOVE_TRACK)
                    .putExtra(EXTRA_TRACK_INDEX, index),
            )
        }

        fun moveTrack(context: Context, from: Int, to: Int) {
            if (from < 0 || to < 0 || from == to || instance == null) return
            sendIntent(
                context,
                Intent(context, AudioPlaybackService::class.java)
                    .setAction(ACTION_MOVE_TRACK)
                    .putExtra(EXTRA_FROM_TRACK_INDEX, from)
                    .putExtra(EXTRA_TO_TRACK_INDEX, to),
            )
        }

        fun getPlaybackMode(context: Context): QqMusicPlaybackMode =
            instance?.playbackMode
                ?: QqMusicPlaybackMode.fromSourceValue(
                    context.applicationContext
                        .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                        .getInt(
                            PENDING_PLAYBACK_MODE,
                            QqMusicPlaybackMode.LIST_REPEAT.sourceValue,
                        ),
                )

        fun getShufflePreference(context: Context): QqMusicShufflePreference =
            instance?.shufflePreference
                ?: QqMusicShufflePreference.fromSourceValue(
                    context.applicationContext
                        .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                        .getInt(
                            SHUFFLE_PREFERENCE,
                            QqMusicShufflePreference.DEFAULT.sourceValue,
                        ),
                )

        fun setShufflePreference(
            context: Context,
            preference: QqMusicShufflePreference,
        ): QqMusicShufflePreference {
            context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(SHUFFLE_PREFERENCE, preference.sourceValue)
                .apply()
            instance?.let {
                it.shufflePreference = preference
                it.resetShuffleTraversal()
            }
            return preference
        }

        fun getShufflePlayHistory(context: Context): List<ShuffleHistoryEntry> {
            val raw = context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .getString(SHUFFLE_HISTORY, null)
                ?: return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList(array.length()) {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val id = item.optString("id")
                        if (id.isBlank()) continue
                        add(
                            ShuffleHistoryEntry(
                                id = id,
                                name = item.optString("name", id),
                                playedAt = item.optLong("playedAt"),
                            ),
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        private fun saveShufflePlayHistory(
            context: Context,
            history: List<ShuffleHistoryEntry>,
        ) {
            val array = JSONArray()
            history.forEach { entry ->
                array.put(
                    JSONObject()
                        .put("id", entry.id)
                        .put("name", entry.name)
                        .put("playedAt", entry.playedAt),
                )
            }
            context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(SHUFFLE_HISTORY, array.toString())
                .apply()
        }

        fun setPlaybackMode(
            context: Context,
            mode: QqMusicPlaybackMode,
        ): QqMusicPlaybackMode {
            context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(PENDING_PLAYBACK_MODE, mode.sourceValue)
                .apply()
            if (instance != null) {
                sendIntent(
                    context,
                    Intent(context, AudioPlaybackService::class.java)
                        .setAction(ACTION_SET_PLAYBACK_MODE)
                        .putExtra(EXTRA_PLAYBACK_MODE, mode.sourceValue),
                )
            }
            return mode
        }

        fun cyclePlaybackMode(context: Context): QqMusicPlaybackMode =
            setPlaybackMode(context, getPlaybackMode(context).next())

        fun seekTo(context: Context, positionMs: Long) {
            val intent = Intent(context, AudioPlaybackService::class.java)
                .setAction(ACTION_SEEK)
                .putExtra("position", positionMs.coerceAtLeast(0))
            sendIntent(context, intent)
        }

        fun getPlaybackSpeed(context: Context): Float =
            context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .getFloat(PENDING_PLAYBACK_SPEED, 1f)
                .coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)

        fun setPlaybackSpeed(context: Context, speed: Float) {
            val safeSpeed = speed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
            context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putFloat(PENDING_PLAYBACK_SPEED, safeSpeed)
                .apply()
            if (instance != null) {
                val intent = Intent(context, AudioPlaybackService::class.java)
                    .setAction(ACTION_SET_PLAYBACK_SPEED)
                    .putExtra(EXTRA_PLAYBACK_SPEED, safeSpeed)
                sendIntent(context, intent)
            }
        }

        fun getSoundEffect(context: Context): PlaybackSoundEffect =
            context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .getString(PENDING_SOUND_EFFECT, null)
                .let(PlaybackSoundEffect::fromStorage)

        fun setSoundEffect(context: Context, effect: PlaybackSoundEffect) {
            context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(PENDING_SOUND_EFFECT, effect.storageValue)
                .apply()
            if (instance != null) {
                val intent = Intent(context, AudioPlaybackService::class.java)
                    .setAction(ACTION_SET_SOUND_EFFECT)
                    .putExtra(EXTRA_SOUND_EFFECT, effect.storageValue)
                sendIntent(context, intent)
            }
        }

        /**
         * 设置当前或下一次手动播放的按时停止偏好。0 表示关闭。
         * 未启动播放服务时只持久化偏好，不会创建一个空的后台 Service。
         */
        fun setSleepTimer(context: Context, minutes: Int) {
            setSleepTimerDuration(context, minutes.coerceAtLeast(0) * 60)
        }

        fun setSleepTimerDuration(
            context: Context,
            durationSeconds: Int,
            endTimeMs: Long = 0L,
        ) {
            val safeDuration = durationSeconds.coerceAtLeast(0)
            if (instance != null) {
                val intent = Intent(context, AudioPlaybackService::class.java)
                    .setAction(ACTION_SET_SLEEP_TIMER)
                    .putExtra(EXTRA_SLEEP_DURATION_SECONDS, safeDuration)
                    .apply {
                        if (safeDuration > 0 && endTimeMs > 0L) {
                            putExtra(EXTRA_SLEEP_TIMER_END_TIME, endTimeMs)
                        }
                    }
                sendIntent(context, intent)
            }
        }

        /**
         * 设置当前或下一次手动播放在自然播完 N 集后停止。0 表示关闭。
         */
        fun setSleepAfterTracks(context: Context, count: Int) {
            val safeCount = count.coerceAtLeast(0)
            if (instance != null) {
                val intent = Intent(context, AudioPlaybackService::class.java)
                    .setAction(ACTION_SET_SLEEP_AFTER_TRACKS)
                    .putExtra(EXTRA_TRACK_COUNT, safeCount)
                sendIntent(context, intent)
            }
        }

        /**
         * 与喜马拉雅“播完整集声音再停止”一致：倒计时到点后等待当前音频自然结束。
         */
        fun setSleepFinishCurrentTrack(context: Context, enabled: Boolean) {
            if (instance != null) {
                val intent = Intent(context, AudioPlaybackService::class.java)
                    .setAction(ACTION_SET_SLEEP_FINISH_CURRENT_TRACK)
                    .putExtra(EXTRA_FINISH_CURRENT_TRACK, enabled)
                sendIntent(context, intent)
            }
        }

        fun setSleepFadeOut(context: Context, enabled: Boolean, seconds: Int) {
            if (instance == null) return
            sendIntent(
                context,
                Intent(context, AudioPlaybackService::class.java)
                    .setAction(ACTION_SET_SLEEP_FADE_OUT)
                    .putExtra(EXTRA_FADE_OUT_ENABLED, enabled)
                    .putExtra(EXTRA_FADE_OUT_SECONDS, seconds.coerceAtLeast(0)),
            )
        }

        fun setAutoContinue(context: Context, enabled: Boolean) {
            context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PENDING_AUTO_CONTINUE, enabled)
                .apply()
            if (instance != null) {
                val intent = Intent(context, AudioPlaybackService::class.java)
                    .setAction(ACTION_SET_AUTO_CONTINUE)
                    .putExtra(EXTRA_AUTO_CONTINUE, enabled)
                sendIntent(context, intent)
            }
        }

        fun setLockScreenControl(context: Context, enabled: Boolean) {
            context.applicationContext
                .getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PENDING_LOCK_SCREEN_CONTROL, enabled)
                .apply()
        }

        fun setSkipHeadTail(
            context: Context,
            taskId: String,
            headSeconds: Int,
            tailSeconds: Int,
        ) {
            if (instance == null) return
            val intent = Intent(context, AudioPlaybackService::class.java)
                .setAction(ACTION_SET_SKIP_HEAD_TAIL)
                .putExtra(EXTRA_SKIP_TASK_ID, taskId)
                .putExtra(EXTRA_SKIP_HEAD_SECONDS, headSeconds.coerceIn(0, MAX_SKIP_SECONDS))
                .putExtra(EXTRA_SKIP_TAIL_SECONDS, tailSeconds.coerceIn(0, MAX_SKIP_SECONDS))
            sendIntent(context, intent)
        }

        fun requestStop(context: Context) = sendAction(context, ACTION_STOP)

        private fun sendAction(context: Context, action: String) {
            val intent = Intent(context, AudioPlaybackService::class.java).setAction(action)
            sendIntent(context, intent)
        }

        private fun sendIntent(context: Context, intent: Intent) {
            runCatching { context.startService(intent) }
                .onFailure { Log.w("AudioPlaybackService", "${intent.action} failed", it) }
        }

        fun stopPlayback(context: Context) {
            instance?.let {
                it.stopPlaybackInternal()
                ServiceCompat.stopForeground(it, ServiceCompat.STOP_FOREGROUND_REMOVE)
                it.stopSelf()
                return
            }
            val intent = Intent(context, AudioPlaybackService::class.java).setAction(ACTION_STOP)
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w("AudioPlaybackService", "stopPlayback failed", e)
            }
        }
    }
}

private const val AUDIO_STREAM_CACHE_EXTENSION = "dat"
private const val AUDIO_DOWNLOAD_EXTENSION = "downloaded"
private val audioDownloadLock = Any()

private fun audioDownloadCacheFile(
    context: Context,
    audioUrl: String,
    persistent: Boolean,
): File {
    val root = if (persistent) context.filesDir else context.cacheDir
    val directory = File(root, "audio_download").apply { mkdirs() }
    val extension = if (persistent) AUDIO_DOWNLOAD_EXTENSION else AUDIO_STREAM_CACHE_EXTENSION
    return File(directory, "audio_${Math.abs(audioUrl.hashCode())}.$extension")
}

private fun existingAudioDownloadFile(context: Context, audioUrl: String): File? =
    sequenceOf(true, false)
        .map { persistent -> audioDownloadCacheFile(context, audioUrl, persistent) }
        .firstOrNull { it.isFile && it.length() > 0L }

private fun downloadAudioToCache(
    context: Context,
    audioUrl: String,
    persistent: Boolean = false,
): File? = synchronized(audioDownloadLock) {
    if (audioUrl.isBlank()) return@synchronized null

    if (persistent) {
        val settings = AppSettingsStore.get(context)
        val allowMetered = settings.getBoolean(
            AppSettingsStore.KEY_ALLOW_METERED_DOWNLOAD,
            false,
        )
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as?
            android.net.ConnectivityManager
        if (!allowMetered && connectivity?.isActiveNetworkMetered == true) {
            Log.i("AudioPlaybackService", "Persistent download skipped on metered network")
            return@synchronized null
        }
    }

    existingAudioDownloadFile(context, audioUrl)?.let { existing ->
        if (!persistent || existing.extension == AUDIO_DOWNLOAD_EXTENSION) {
            existing.setLastModified(System.currentTimeMillis())
            return@synchronized existing
        }
    }

    val destination = audioDownloadCacheFile(context, audioUrl, persistent)
    val partial = File(destination.parentFile, "${destination.name}.part")
    var connection: HttpURLConnection? = null
    try {
        connection = URL(audioUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.connect()
        if (connection.responseCode !in 200..299) {
            Log.e("AudioPlaybackService", "Download failed: HTTP ${connection.responseCode}")
            return@synchronized null
        }

        connection.inputStream.use { input ->
            FileOutputStream(partial).use { output -> input.copyTo(output) }
        }
        if (!partial.renameTo(destination)) {
            partial.copyTo(destination, overwrite = true)
        }
        destination.takeIf { it.isFile && it.length() > 0L }
    } catch (error: Exception) {
        Log.e("AudioPlaybackService", "Download error", error)
        null
    } finally {
        partial.delete()
        connection?.disconnect()
    }
}

private fun directorySize(directory: File): Long =
    directory.walkTopDown()
        .filter(File::isFile)
        .sumOf(File::length)

private fun deleteDirectoryContents(directory: File) {
    if (!directory.isDirectory) return
    directory.listFiles().orEmpty().forEach { child ->
        if (child.isDirectory) {
            deleteDirectoryContents(child)
            child.delete()
        } else {
            child.delete()
        }
    }
}

internal object PlaybackLaunchContract {
    fun isDirectSource(value: String): Boolean {
        if (value.startsWith("/")) return true
        val scheme = value.substringBefore(':', missingDelimiterValue = "").lowercase()
        return when (scheme) {
            "http", "https" -> false
            else -> true
        }
    }

    fun earliestPositiveDeadline(first: Long, second: Long): Long =
        listOf(first, second).filter { it > 0L }.minOrNull() ?: 0L

    fun isSleepDeadline(executionEnd: Long, sleepEnd: Long): Boolean =
        sleepEnd > 0L && (executionEnd <= 0L || sleepEnd < executionEnd)

    fun shouldFinishCurrentTrack(
        executionEnd: Long,
        sleepEnd: Long,
        finishCurrentTrack: Boolean,
    ): Boolean =
        finishCurrentTrack && isSleepDeadline(executionEnd, sleepEnd)

    fun shouldStopAtPlaylistEnd(
        stopAtPlaylistEnd: Boolean,
        currentTrackIndex: Int,
        lastTrackIndex: Int,
    ): Boolean =
        stopAtPlaylistEnd && lastTrackIndex >= 0 && currentTrackIndex >= lastTrackIndex
}
