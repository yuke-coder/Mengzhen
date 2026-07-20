package com.mengzhen.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.mengzhen.app.MainActivity
import com.mengzhen.app.R
import com.mengzhen.app.receiver.ScreenStatusReceiver
import com.mengzhen.app.audio.PlayProgressStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@UnstableApi
class AudioPlaybackService : Service() {

    private val tag = "AudioPlaybackService"
    private val channelId = "dream_pillow_playback"
    private val notificationId = 1001

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var cache: SimpleCache? = null

    private val playlist = mutableListOf<Track>()
    private var currentTrackIndex = 0
    private var userStopped = false // 区分用户主动停止 vs 系统杀死

    private var currentTaskId: String? = null
    private var taskName = "梦枕"
    private var targetVolume = 70
    private var enableFade = false
    private var fadeInDuration = 0
    private var fadeOutDuration = 0
    private var playDurationMinutes = 30
    private var loopSingle = true
    private var startTimeMs = 0L
    private var endTimeMs = 0L
    private var wasPlayingBeforeCall = false
    private var coverUrl: String? = null

    private var fadeInRunnable: Runnable? = null
    private var fadeOutRunnable: Runnable? = null
    private var stopRunnable: Runnable? = null
    private var progressRunnable: Runnable? = null

    private var telephonyManager: TelephonyManager? = null
    private var phoneListenerRegistered = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    private var noisyReceiverRegistered = false
    private var headsetUnplugDebounced = false // 耳机拔出 3 秒去抖 - 对标喜马拉雅 al.java f45972b

    /** 耳机拔出/音频变嘈杂 - 对标喜马拉雅 al.java
     *  喜马拉雅用 f45972b 标志 + 3 秒 Timer 去抖，避免 HEADSET_PLUG 和 AUDIO_BECOMING_NOISY
     *  两个广播短时间内都触发暂停。
     */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 0) { // 拔出
                        if (headsetUnplugDebounced) return // 去抖：3 秒内已处理过一次
                        headsetUnplugDebounced = true
                        handler.postDelayed({ headsetUnplugDebounced = false }, 3000)
                        player?.let { if (it.isPlaying) it.pause() }
                        updateNotification("暂停中(耳机拔出): $taskName")
                    }
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    if (headsetUnplugDebounced) return // 去抖
                    headsetUnplugDebounced = true
                    handler.postDelayed({ headsetUnplugDebounced = false }, 3000)
                    player?.let { if (it.isPlaying) it.pause() }
                    updateNotification("暂停中(音频输出变更): $taskName")
                }
            }
        }
    }

    private inner class DreamPillowPhoneStateListener : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            onCallStateChangedInternal(state)
        }
    }

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
                if (wasPlayingBeforeCall && !p.isPlaying) {
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
                if (p.isPlaying) p.pause()
                releaseAudioFocus()
                updateNotification("暂停中(焦点丢失): $taskName")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (p.isPlaying) {
                    p.pause()
                    updateNotification("暂停中: $taskName")
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                p.volume = 0.2f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                p.volume = targetVolume / 100f
                if (!p.isPlaying) {
                    requestAudioFocus()
                    p.play()
                    updateNotification("正在播放: $taskName")
                }
            }
        }
    }

    /** 错误重试 - 对标喜马拉雅 i.java LoadErrorHandlingPolicy + onPlayerError */
    private var errorRetryCount = 0
    private val maxErrorRetries = 3

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    if (!loopSingle && currentTrackIndex < playlist.size - 1) {
                        playNextTrack()
                    } else if (loopSingle) {
                        // 循环模式由 ExoPlayer repeatMode 处理，这里不需要额外操作
                    }
                }
                Player.STATE_IDLE -> {
                    // STATE_IDLE 通常意味着播放错误后被 stop()，不在这里处理重试
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateMediaSessionState(isPlaying)
            updateNotification(if (isPlaying) "正在播放: $taskName" else "暂停中: $taskName")
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
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
                stopPlaybackInternal()
                ServiceCompat.stopForeground(this@AudioPlaybackService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            errorRetryCount++
            val delay = 2000L * errorRetryCount // 递增延迟：2s, 4s, 6s
            val track = playlist.getOrNull(currentTrackIndex)
            if (track == null) {
                stopPlaybackInternal()
                stopSelf()
                return
            }

            // 对标喜马拉雅 i.java: 如果是本地缓存文件损坏，删除后重新下载
            val localFile = downloadToCacheFilePath(track.url)
            if (localFile != null && localFile.exists()) {
                Log.i(tag, "Deleting corrupted local file: ${localFile.absolutePath}")
                localFile.delete()
            }

            handler.postDelayed({
                val p = player ?: return@postDelayed
                // 重新走 playTrack 逻辑（含 downloadToCache + 断点续播）
                // 对标喜马拉雅 i.java reset() + setDataSource() + prepare()
                playTrack(currentTrackIndex, 0L)
                Log.i(tag, "Retrying playback (attempt $errorRetryCount/$maxErrorRetries)")
            }, delay)
        }
    }

    data class Track(val url: String, val name: String)

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DreamPillow::Playback")
        wakeLock?.setReferenceCounted(false)

        // WifiLock - 对标喜马拉雅 ExoPlayer ar.java
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DreamPillow::WifiLock")
        wifiLock?.setReferenceCounted(false)

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = DreamPillowTelephonyCallback()
            telephonyManager?.registerTelephonyCallback(mainExecutor, telephonyCallback!!)
        } else {
            phoneStateListener = DreamPillowPhoneStateListener()
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
        phoneListenerRegistered = true

        // 耳机插拔监听 - 对标喜马拉雅 al.java IntentFilter
        // Android 14+ 必须指定 RECEIVER_NOT_EXPORTED
        val noisyFilter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(noisyReceiver, noisyFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(noisyReceiver, noisyFilter)
        }
        noisyReceiverRegistered = true

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
    }

    private fun initPlayer() {
        val cache = this.cache
        val dataSourceFactory = if (cache != null) {
            val upstream = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(30000)
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } else {
            DefaultHttpDataSource.Factory().setConnectTimeoutMs(15000)
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(false) // 自己处理，注册了 noisyReceiver
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setTrackSelector(DefaultTrackSelector(this))
            .build()
            .apply {
                addListener(playerListener)
            }

        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP -> {
                stopPlaybackInternal()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                player?.pause()
                updateNotification("暂停中: $taskName")
                return START_STICKY
            }
            ACTION_RESUME -> {
                // 恢复播放时重新请求音频焦点（对标喜马拉雅 al.java a()）
                // 因为 LOSS 时 abandon 了焦点，恢复需要重新请求
                requestAudioFocus()
                player?.play()
                updateNotification("正在播放: $taskName")
                return START_STICKY
            }
            ACTION_NEXT -> {
                playNextTrack()
                return START_STICKY
            }
            ACTION_PREV -> {
                playPrevTrack()
                return START_STICKY
            }
            ACTION_SEEK -> {
                val pos = intent.getLongExtra("position", -1L)
                if (pos >= 0) player?.seekTo(pos)
                return START_STICKY
            }
            ACTION_SET_SLEEP_TIMER -> {
                val minutes = intent.getIntExtra("minutes", 0)
                if (minutes > 0) {
                    endTimeMs = System.currentTimeMillis() + minutes * 60_000L
                    schedulePlaybackEnd()
                    updateNotification("已设定${minutes}分钟后停止: $taskName")
                }
                return START_STICKY
            }
            ACTION_RESTART -> {
                // 双 Service 保活 - 被 SustainedListenService 拉起后从断点恢复
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
                return START_STICKY
            }
            ACTION_START -> {
                startForegroundWithNotification()
                startPlayback(intent)
                return START_STICKY
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("准备播放...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun startPlayback(intent: Intent) {
        // 持有 WakeLock + WifiLock - 对标喜马拉雅 ar.java
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(12 * 60 * 60 * 1000L)
        }
        if (wifiLock?.isHeld != true) {
            wifiLock?.acquire()
        }

        currentTaskId = intent.getStringExtra("taskId")
        taskName = intent.getStringExtra("taskName") ?: "梦枕"
        playDurationMinutes = intent.getIntExtra("playDurationMinutes", 30)
        targetVolume = intent.getIntExtra("volume", 70)
        enableFade = intent.getBooleanExtra("enableFade", false)
        fadeInDuration = intent.getIntExtra("fadeInDuration", 0)
        fadeOutDuration = intent.getIntExtra("fadeOutDuration", 0)
        loopSingle = intent.getBooleanExtra("loopSingle", true)
        coverUrl = intent.getStringExtra("coverUrl")
        val endTime = intent.getLongExtra("endTime", 0)

        // 保存播放状态用于双 Service 保活恢复 - 对标喜马拉雅 XiMaLaYaService+AssistService
        saveLastPlaybackState(intent)

        // 异步加载封面 - 避免主线程 ANR
        coverBitmap = null
        loadCoverAsync(coverUrl)

        playlist.clear()
        val tracksJson = intent.getStringExtra("tracksJson")
        if (!tracksJson.isNullOrEmpty()) {
            parseTracksJson(tracksJson)
        } else {
            val audioUrl = intent.getStringExtra("audioUrl")
            val audioName = intent.getStringExtra("audioName")
            if (!audioUrl.isNullOrEmpty()) {
                playlist.add(Track(audioUrl, audioName ?: "音频"))
            }
        }

        if (playlist.isEmpty()) {
            Log.e(tag, "No tracks to play")
            stopSelf()
            return
        }

        startTimeMs = System.currentTimeMillis()
        endTimeMs = if (endTime > 0) endTime else startTimeMs + playDurationMinutes * 60_000L

        requestAudioFocus()
        SustainedListenService.start(this, taskName)

        currentTrackIndex = 0
        // 检查断点续播 - 对标喜马拉雅播放进度恢复
        val savedProgress = PlayProgressStore.get(this).getLocal(playlist[0].url)?.first ?: 0L
        playTrack(currentTrackIndex, savedProgress)
    }

    private fun parseTracksJson(json: String) {
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                playlist.add(Track(obj.optString("url", ""), obj.optString("name", "音频${i + 1}")))
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse tracksJson", e)
        }
    }

    private fun playTrack(index: Int, resumePositionMs: Long = 0L) {
        if (index < 0 || index >= playlist.size) return
        currentTrackIndex = index
        val track = playlist[index]
        errorRetryCount = 0

        if (track.url.startsWith("file://") || track.url.startsWith("/")) {
            val path = if (track.url.startsWith("file://")) track.url.substring(7) else track.url
            prepareAndPlay(Uri.fromFile(File(path)), resumePositionMs)
        } else {
            serviceScope.launch {
                val localFile = downloadToCache(track.url)
                withContext(Dispatchers.Main) {
                    if (localFile != null) {
                        prepareAndPlay(Uri.fromFile(localFile), resumePositionMs)
                    } else {
                        prepareAndPlay(Uri.parse(track.url), resumePositionMs)
                    }
                }
            }
        }
    }

    private fun prepareAndPlay(uri: Uri, resumePositionMs: Long = 0L) {
        val p = player ?: return
        p.stop()
        p.clearMediaItems()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playlist.getOrNull(currentTrackIndex)?.name ?: taskName)
                    .setArtist("梦枕")
                    .build()
            )
            .build()

        p.setMediaItem(mediaItem)
        p.repeatMode = if (loopSingle) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        p.prepare()
        p.volume = 0f
        p.play()

        // 断点续播 - 恢复播放进度
        if (resumePositionMs > 0) {
            p.seekTo(resumePositionMs)
            Log.i(tag, "Resumed from ${resumePositionMs / 1000}s")
        }

        if (enableFade && fadeInDuration > 0) {
            startFadeIn()
        } else {
            p.volume = targetVolume / 100f
        }

        updateNotification("正在播放: ${playlist.getOrNull(currentTrackIndex)?.name ?: taskName}")
        schedulePlaybackEnd()
    }

    private fun startFadeIn() {
        val p = player ?: return
        val steps = maxOf(1, fadeInDuration * 20)
        val volumeStep = (targetVolume / 100f) / steps
        var step = 0

        fadeInRunnable?.let { handler.removeCallbacks(it) }
        fadeInRunnable = object : Runnable {
            override fun run() {
                if (step >= steps || p != player) {
                    p.volume = targetVolume / 100f
                    return
                }
                step++
                p.volume = volumeStep * step
                handler.postDelayed(this, 50)
            }
        }
        handler.post(fadeInRunnable!!)
    }

    private fun startFadeOut() {
        val p = player ?: return
        if (!enableFade || fadeOutDuration <= 0) {
            stopPlaybackInternal()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val steps = maxOf(1, fadeOutDuration * 20)
        val volumeStep = (targetVolume / 100f) / steps
        var step = 0

        fadeOutRunnable?.let { handler.removeCallbacks(it) }
        fadeOutRunnable = object : Runnable {
            override fun run() {
                if (step >= steps || p != player) {
                    stopPlaybackInternal()
                    ServiceCompat.stopForeground(this@AudioPlaybackService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return
                }
                step++
                p.volume = maxOf(0f, (targetVolume / 100f) - volumeStep * step)
                handler.postDelayed(this, 50)
            }
        }
        handler.post(fadeOutRunnable!!)
    }

    private fun schedulePlaybackEnd() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        val durationMs = endTimeMs - System.currentTimeMillis()
        if (durationMs <= 0) {
            startFadeOut()
            return
        }
        val fadeOutMs = if (enableFade && fadeOutDuration > 0) fadeOutDuration * 1000L else 0L
        val stopAt = System.currentTimeMillis() + maxOf(0, durationMs - fadeOutMs)

        stopRunnable = Runnable {
            Log.i(tag, "Playback duration ended, starting fade out")
            startFadeOut()
        }
        handler.postAtTime(stopRunnable!!, stopAt)
        Log.i(tag, "Scheduled stop in ${durationMs / 1000}s")
    }

    private fun playNextTrack() {
        if (playlist.isEmpty()) return
        val next = if (currentTrackIndex + 1 < playlist.size) currentTrackIndex + 1 else 0
        playTrack(next)
    }

    private fun playPrevTrack() {
        if (playlist.isEmpty()) return
        val prev = if (currentTrackIndex - 1 >= 0) currentTrackIndex - 1 else playlist.size - 1
        playTrack(prev)
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager?.abandonAudioFocusRequest(audioFocusRequest!!)
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(audioFocusListener)
        }
    }

    /** 断点续播 - 保存进度 - 对标喜马拉雅播放进度存储
     *  本地每秒保存 + 云端异步同步
     */
    private fun saveProgress() {
        val p = player ?: return
        val track = playlist.getOrNull(currentTrackIndex) ?: return
        if (!p.isPlaying && p.currentPosition <= 0) return
        val store = PlayProgressStore.get(this)
        val positionSec = p.currentPosition / 1000
        val durationSec = p.duration / 1000
        store.saveLocal(track.url, positionSec, durationSec)
        // 云端同步 - 异步，不阻塞播放
        serviceScope.launch { store.saveToCloud(track.url, positionSec, durationSec) }
        Log.d(tag, "Saved progress: ${track.url} at ${positionSec}s")
    }

    /** 通知栏进度条更新 */
    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressRunnable = object : Runnable {
            override fun run() {
                val p = player ?: return
                if (p.isPlaying) {
                    saveProgress()
                    updateNotificationProgress(p.currentPosition, p.duration)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun stopPlaybackInternal() {
        userStopped = true
        saveProgress()
        stopProgressUpdates()
        player?.stop()
        player?.clearMediaItems()
        currentTaskId = null
        errorRetryCount = 0
        fadeInRunnable?.let { handler.removeCallbacks(it) }
        fadeOutRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable?.let { handler.removeCallbacks(it) }
        fadeInRunnable = null
        fadeOutRunnable = null
        stopRunnable = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        if (wifiLock?.isHeld == true) wifiLock?.release()
        coverBitmap = null
        releaseAudioFocus()
        SustainedListenService.stop(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "梦枕播放", NotificationManager.IMPORTANCE_LOW).apply {
                description = "音频播放服务"
                setShowBadge(false)
            }
            (getSystemService(NotificationManager::class.java))?.createNotificationChannel(channel)
        }
    }

    /** 通知栏 - 通知 + 进度条 + 封面 + 定时停止按钮 - 对标喜马拉雅通知栏 */
    private fun buildNotification(contentText: String): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = actionPending(ACTION_STOP, 0)
        val pausePending = actionPending(ACTION_PAUSE, 1)
        val nextPending = actionPending(ACTION_NEXT, 2)
        val prevPending = actionPending(ACTION_PREV, 3)
        val sleepPending = actionPending(ACTION_SET_SLEEP_TIMER + "_15", 4) // 默认15分钟

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("梦枕")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(contentPending)
            .addAction(android.R.drawable.ic_media_previous, "上一首", prevPending)
            .addAction(android.R.drawable.ic_media_pause, "暂停", pausePending)
            .addAction(android.R.drawable.ic_media_next, "下一首", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopPending)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "定时停止", sleepPending)

        // 封面 - 对标喜马拉雅通知栏封面
        if (coverBitmap != null) {
            builder.setLargeIcon(coverBitmap)
        }

        return builder.build()
    }

    /** 带进度条的通知更新 */
    private fun updateNotificationProgress(position: Long, duration: Long) {
        if (duration <= 0) return
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("梦枕")
            .setContentText("正在播放: ${playlist.getOrNull(currentTrackIndex)?.name ?: taskName}")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(contentPending)
            .setProgress(duration.toInt(), position.toInt(), false)
            .setShowWhen(false)
            .addAction(android.R.drawable.ic_media_previous, "上一首", actionPending(ACTION_PREV, 3))
            .addAction(android.R.drawable.ic_media_pause, "暂停", actionPending(ACTION_PAUSE, 1))
            .addAction(android.R.drawable.ic_media_next, "下一首", actionPending(ACTION_NEXT, 2))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", actionPending(ACTION_STOP, 0))

        if (coverBitmap != null) {
            builder.setLargeIcon(coverBitmap)
        }

        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.notify(notificationId, builder.build())
    }

    private var coverBitmap: Bitmap? = null // 封面缓存，避免每秒重复加载

    /** 异步加载封面 - 避免主线程 ANR */
    private fun loadCoverAsync(url: String?) {
        if (url == null || url.isEmpty()) return
        if (coverBitmap != null) return // 已加载
        serviceScope.launch {
            val bitmap = try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                if (conn.responseCode == 200) BitmapFactory.decodeStream(conn.inputStream) else null
            } catch (e: Exception) { null }
            if (bitmap != null) {
                coverBitmap = bitmap
                withContext(Dispatchers.Main) {
                    updateNotification("正在播放: ${playlist.getOrNull(currentTrackIndex)?.name ?: taskName}")
                }
            }
        }
    }

    private fun actionPending(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AudioPlaybackService::class.java).setAction(action)
        if (action.startsWith(ACTION_SET_SLEEP_TIMER)) {
            val parts = action.split("_")
            val minutes = parts.lastOrNull()?.toIntOrNull() ?: 15
            intent.putExtra("minutes", minutes)
            intent.action = ACTION_SET_SLEEP_TIMER
        }
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun updateNotification(text: String) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notify(notificationId, buildNotification(text))
    }

    private fun updateMediaSessionState(isPlaying: Boolean) {
        // Media3 会自动同步播放状态到 MediaSession，无需手动设置
    }

    private fun downloadToCache(audioUrl: String): File? {
        if (audioUrl.isEmpty()) return null
        var conn: HttpURLConnection? = null
        var input: java.io.InputStream? = null
        var output: FileOutputStream? = null
        return try {
            val url = URL(audioUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.connect()

            if (conn.responseCode != 200) {
                Log.e(tag, "Download failed: HTTP ${conn.responseCode}")
                return null
            }

            val cacheDir = File(cacheDir, "audio_download").apply { mkdirs() }
            val fileName = "audio_" + Math.abs(audioUrl.hashCode()) + ".dat"
            val cacheFile = File(cacheDir, fileName)

            input = conn.inputStream
            output = FileOutputStream(cacheFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            output.flush()
            cacheFile
        } catch (e: Exception) {
            Log.e(tag, "Download error", e)
            null
        } finally {
            try { input?.close() } catch (_: Exception) {}
            try { output?.close() } catch (_: Exception) {}
            conn?.disconnect()
        }
    }

    /** 返回 URL 对应的下载缓存文件（如果存在） */
    private fun downloadToCacheFilePath(audioUrl: String): File? {
        if (audioUrl.isEmpty()) return null
        val cacheDir = File(cacheDir, "audio_download")
        val fileName = "audio_" + Math.abs(audioUrl.hashCode()) + ".dat"
        return File(cacheDir, fileName)
    }

    /** 保存播放状态用于恢复 - 双 Service 保活 */
    private fun saveLastPlaybackState(intent: Intent) {
        val json = JSONObject()
        json.put("taskId", intent.getStringExtra("taskId") ?: "")
        json.put("taskName", intent.getStringExtra("taskName") ?: "梦枕")
        json.put("playDurationMinutes", intent.getIntExtra("playDurationMinutes", 30))
        json.put("volume", intent.getIntExtra("volume", 70))
        json.put("enableFade", intent.getBooleanExtra("enableFade", false))
        json.put("fadeInDuration", intent.getIntExtra("fadeInDuration", 0))
        json.put("fadeOutDuration", intent.getIntExtra("fadeOutDuration", 0))
        json.put("loopSingle", intent.getBooleanExtra("loopSingle", true))
        json.put("coverUrl", intent.getStringExtra("coverUrl") ?: "")
        json.put("endTime", intent.getLongExtra("endTime", 0))
        json.put("tracksJson", intent.getStringExtra("tracksJson") ?: "")
        json.put("audioUrl", intent.getStringExtra("audioUrl") ?: "")
        json.put("audioName", intent.getStringExtra("audioName") ?: "")
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
            bundle.putInt("volume", json.optInt("volume", 70))
            bundle.putBoolean("enableFade", json.optBoolean("enableFade", false))
            bundle.putInt("fadeInDuration", json.optInt("fadeInDuration", 0))
            bundle.putInt("fadeOutDuration", json.optInt("fadeOutDuration", 0))
            bundle.putBoolean("loopSingle", json.optBoolean("loopSingle", true))
            json.optString("coverUrl").takeIf { it.isNotEmpty() }?.let { bundle.putString("coverUrl", it) }
            bundle.putLong("endTime", json.optLong("endTime", 0))
            json.optString("tracksJson").takeIf { it.isNotEmpty() }?.let { bundle.putString("tracksJson", it) }
            json.optString("audioUrl").takeIf { it.isNotEmpty() }?.let { bundle.putString("audioUrl", it) }
            json.optString("audioName").takeIf { it.isNotEmpty() }?.let { bundle.putString("audioName", it) }
            bundle
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse last state", e)
            null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPlaybackInternal()
        // 双 Service 保活 - 如果不是用户主动停止，拉起 SustainedListenService
        // 对标喜马拉雅 XiMaLaYaService.onDestroy -> startService(AssistService)
        if (!userStopped && currentTaskId != null) {
            Log.i(tag, "System killed service, restarting via SustainedListenService")
            SustainedListenService.start(this, taskName)
        }
        if (phoneListenerRegistered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback != null) {
                telephonyManager?.unregisterTelephonyCallback(telephonyCallback!!)
            } else {
                telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
            phoneListenerRegistered = false
        }
        if (noisyReceiverRegistered) {
            unregisterReceiver(noisyReceiver)
            noisyReceiverRegistered = false
        }
        player?.removeListener(playerListener)
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
        const val ACTION_SEEK = "com.mengzhen.app.SEEK_TO"
        const val ACTION_SET_SLEEP_TIMER = "com.mengzhen.app.SET_SLEEP_TIMER"
        const val ACTION_RESTART = "com.mengzhen.app.RESTART_PLAYBACK"

        @Volatile private var instance: AudioPlaybackService? = null

        fun isCurrentlyPlaying(): Boolean = instance?.player?.isPlaying == true
        fun getCurrentTaskId(): String? = instance?.currentTaskId

        fun stopPlayback(context: Context) {
            instance?.let {
                it.userStopped = true
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
