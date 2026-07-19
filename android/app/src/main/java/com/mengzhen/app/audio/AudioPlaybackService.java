package com.mengzhen.app.audio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;

import com.mengzhen.app.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Foreground Service 负责音频播放（对标喜马拉雅播放内核）
 *
 * 核心能力：
 * - 播放列表支持（多首音频顺序播放 + 单曲循环）
 * - MediaPlayer 播放（绕过 WebView autoplay 限制）
 * - PARTIAL_WAKE_LOCK 保持 CPU 运转（息屏不断）
 * - 前台通知保持 Service 不被杀（含上一首/下一首/停止按钮）
 * - 音量渐入渐出（Handler 实现，不依赖 JS 定时器）
 * - 定时停止（Handler.postAtTime，不依赖 JS setTimeout）
 * - 音频焦点管理（来电时自动降音/暂停，通话结束恢复）
 * - MediaSession 集成（锁屏控制）
 * - 音频下载缓存（避免重复下载）
 */
public class AudioPlaybackService extends Service {

    private static final String TAG = "AudioPlaybackService";
    private static final String CHANNEL_ID = "dream_pillow_playback";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "com.mengzhen.app.START_PLAYBACK";
    public static final String ACTION_STOP = "com.mengzhen.app.STOP_PLAYBACK";
    public static final String ACTION_PAUSE = "com.mengzhen.app.PAUSE_PLAYBACK";
    public static final String ACTION_RESUME = "com.mengzhen.app.RESUME_PLAYBACK";
    public static final String ACTION_NEXT = "com.mengzhen.app.NEXT_TRACK";
    public static final String ACTION_PREV = "com.mengzhen.app.PREV_TRACK";

    private static AudioPlaybackService instance;

    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    // 播放列表
    private List<TrackInfo> playlist = new ArrayList<>();
    private int currentTrackIndex = 0;

    // 播放配置
    private String currentTaskId;
    private String taskName = "梦枕";
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private int targetVolume = 70;
    private boolean enableFade = false;
    private int fadeInDuration = 0;
    private int fadeOutDuration = 0;
    private int playDurationMinutes = 30;
    private long startTimeMs = 0;
    private long endTimeMs = 0;
    private boolean loopSingle = true; // 单曲循环（对标喜马拉雅睡眠模式）

    // 渐入渐出 Runnable
    private Runnable fadeOutRunnable;
    private Runnable stopRunnable;
    private Runnable fadeInRunnable;

    // 音频焦点回调
    private AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    // 永久失去焦点（如其他音乐播放器），停止播放
                    Log.i(TAG, "Audio focus lost permanently");
                    stopPlaybackInternal();
                    stopForeground(true);
                    stopSelf();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // 短暂失去焦点（如来电），暂停播放
                    Log.i(TAG, "Audio focus lost transient (phone call)");
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        isPaused = true;
                        updateNotification("暂停中: " + taskName);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // 可以降低音量（如通知声）
                    Log.i(TAG, "Audio focus: ducking");
                    if (mediaPlayer != null) {
                        float duckVolume = 0.2f;
                        mediaPlayer.setVolume(duckVolume, duckVolume);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    // 恢复音量和播放
                    Log.i(TAG, "Audio focus regained");
                    if (mediaPlayer != null) {
                        setMediaPlayerVolume(targetVolume);
                        if (isPaused) {
                            mediaPlayer.start();
                            isPaused = false;
                            updateNotification("正在播放: " + taskName);
                        }
                    }
                    break;
            }
        }
    };

    /** 音轨信息 */
    private static class TrackInfo {
        String url;
        String name;
        File cachedFile;

        TrackInfo(String url, String name) {
            this.url = url;
            this.name = name;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DreamPillow::Playback");
        wakeLock.setReferenceCounted(false);

        createNotificationChannel();
        Log.i(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_STOP.equals(action)) {
            stopPlaybackInternal();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_PAUSE.equals(action)) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPaused = true;
                updateNotification("暂停中: " + taskName);
                updateMediaSessionPlaybackState(false);
            }
            return START_STICKY;
        }

        if (ACTION_RESUME.equals(action)) {
            if (mediaPlayer != null && isPaused) {
                mediaPlayer.start();
                isPaused = false;
                updateNotification("正在播放: " + taskName);
                updateMediaSessionPlaybackState(true);
            }
            return START_STICKY;
        }

        if (ACTION_NEXT.equals(action)) {
            playNextTrack();
            return START_STICKY;
        }

        if (ACTION_PREV.equals(action)) {
            playPrevTrack();
            return START_STICKY;
        }

        if (ACTION_START.equals(action)) {
            startForegroundWithNotification();

            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(12 * 60 * 60 * 1000L); // 最长 12 小时
            }

            // 解析播放列表
            String tracksJson = intent.getStringExtra("tracksJson");
            currentTaskId = intent.getStringExtra("taskId");
            taskName = intent.getStringExtra("taskName");
            playDurationMinutes = intent.getIntExtra("playDurationMinutes", 30);
            targetVolume = intent.getIntExtra("volume", 70);
            enableFade = intent.getBooleanExtra("enableFade", false);
            fadeInDuration = intent.getIntExtra("fadeInDuration", 0);
            fadeOutDuration = intent.getIntExtra("fadeOutDuration", 0);
            loopSingle = intent.getBooleanExtra("loopSingle", true);
            long endTime = intent.getLongExtra("endTime", 0);

            playlist.clear();
            if (tracksJson != null && !tracksJson.isEmpty()) {
                try {
                    JSONArray arr = new JSONArray(tracksJson);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        playlist.add(new TrackInfo(
                            obj.optString("url", ""),
                            obj.optString("name", "音频" + (i + 1))
                        ));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse tracksJson", e);
                }
            }

            // 兼容旧接口：单个 audioUrl
            if (playlist.isEmpty()) {
                String audioUrl = intent.getStringExtra("audioUrl");
                String audioName = intent.getStringExtra("audioName");
                if (audioUrl != null && !audioUrl.isEmpty()) {
                    playlist.add(new TrackInfo(audioUrl, audioName != null ? audioName : "音频"));
                }
            }

            // 计算结束时间
            startTimeMs = System.currentTimeMillis();
            if (endTime > 0) {
                endTimeMs = endTime;
            } else {
                endTimeMs = startTimeMs + playDurationMinutes * 60L * 1000L;
            }

            // 请求音频焦点
            requestAudioFocus();

            // 开始播放第一首
            currentTrackIndex = 0;
            if (!playlist.isEmpty()) {
                playTrack(currentTrackIndex);
            } else {
                Log.e(TAG, "No tracks to play");
                stopSelf();
            }
        }

        return START_STICKY;
    }

    /** 播放指定轨道 */
    private void playTrack(int index) {
        if (index < 0 || index >= playlist.size()) return;

        TrackInfo track = playlist.get(index);
        currentTrackIndex = index;
        Log.i(TAG, "Playing track " + index + ": " + track.name);

        // 释放旧的 MediaPlayer
        releaseMediaPlayer();

        if (track.url == null || track.url.isEmpty()) {
            Log.e(TAG, "Track URL is empty");
            playNextTrack(); // 跳到下一首
            return;
        }

        // 本地文件
        if (track.url.startsWith("file://") || track.url.startsWith("/")) {
            String path = track.url.startsWith("file://") ? track.url.substring(7) : track.url;
            playFromLocalPath(path);
        } else {
            // 网络URL：下载到缓存后播放
            final String url = track.url;
            new Thread(() -> {
                File localFile = downloadToCache(url);
                if (localFile == null) {
                    Log.e(TAG, "Download failed, falling back to streaming");
                    handler.post(() -> streamPlay(url));
                    return;
                }
                track.cachedFile = localFile;
                handler.post(() -> playFromLocalPath(localFile.getAbsolutePath()));
            }).start();
        }
    }

    /** 从本地路径播放 */
    private void playFromLocalPath(String path) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            mediaPlayer.setLooping(loopSingle);

            Log.i(TAG, "Playing from local path: " + path);
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                playNextTrack();
                return true;
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                if (!loopSingle) {
                    playNextTrack();
                }
            });

            onMediaPlayerReady();
        } catch (Exception e) {
            Log.e(TAG, "Local path playback failed", e);
            playNextTrack();
        }
    }

    /** 流式播放（fallback） */
    private void streamPlay(String audioUrl) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            mediaPlayer.setLooping(loopSingle);

            Log.i(TAG, "Streaming audio: " + audioUrl);
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer stream error: what=" + what + " extra=" + extra);
                playNextTrack();
                return true;
            });

            mediaPlayer.setOnPreparedListener(mp -> onMediaPlayerReady());
        } catch (IOException e) {
            Log.e(TAG, "Failed to set data source", e);
            playNextTrack();
        }
    }

    /** MediaPlayer 准备好后的通用逻辑 */
    private void onMediaPlayerReady() {
        Log.i(TAG, "MediaPlayer ready, starting playback");

        if (enableFade && fadeInDuration > 0) {
            startFadeIn();
        } else {
            setMediaPlayerVolume(targetVolume);
        }

        mediaPlayer.start();
        isPlaying = true;
        setupMediaSession();

        // 更新通知显示当前轨道名
        TrackInfo track = playlist.get(currentTrackIndex);
        updateNotification("正在播放: " + (track != null ? track.name : taskName));

        schedulePlaybackEnd();
    }

    /** 开始渐入 */
    private void startFadeIn() {
        final int steps = Math.max(1, fadeInDuration * 20); // 50ms per step
        final float volumePerStep = (float) targetVolume / 100f / steps;
        final int[] currentStep = {0};

        setMediaPlayerVolume(0);

        fadeInRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentStep[0] >= steps || mediaPlayer == null) {
                    setMediaPlayerVolume(targetVolume);
                    Log.i(TAG, "Fade in complete");
                    return;
                }
                currentStep[0]++;
                float vol = volumePerStep * currentStep[0];
                setMediaPlayerVolume((int)(vol * 100));
                handler.postDelayed(this, 50);
            }
        };
        handler.post(fadeInRunnable);
    }

    /** 开始渐出 */
    private void startFadeOut() {
        if (!enableFade || fadeOutDuration <= 0 || mediaPlayer == null) {
            stopPlaybackInternal();
            stopForeground(true);
            stopSelf();
            return;
        }

        // 获取实际当前音量（优先用 targetVolume，因为 MediaPlayer 没有 getVolume）
        final int currentVol = targetVolume;
        final int steps = Math.max(1, fadeOutDuration * 20);
        final float volumePerStep = (float) currentVol / steps;
        final int[] currentStep = {0};

        fadeOutRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentStep[0] >= steps || mediaPlayer == null) {
                    stopPlaybackInternal();
                    stopForeground(true);
                    stopSelf();
                    Log.i(TAG, "Fade out complete, stopping");
                    return;
                }
                currentStep[0]++;
                int vol = Math.max(0, currentVol - (int)(volumePerStep * currentStep[0]));
                setMediaPlayerVolume(vol);
                handler.postDelayed(this, 50);
            }
        };
        handler.post(fadeOutRunnable);
    }

    /** 调度播放结束（使用 Handler.postAtTime，不依赖 JS setTimeout） */
    private void schedulePlaybackEnd() {
        if (stopRunnable != null) handler.removeCallbacks(stopRunnable);

        long durationMs = endTimeMs - System.currentTimeMillis();
        long fadeOutMs = (enableFade && fadeOutDuration > 0) ? fadeOutDuration * 1000L : 0;
        long stopAt = System.currentTimeMillis() + Math.max(0, durationMs - fadeOutMs);

        stopRunnable = () -> {
            Log.i(TAG, "Playback duration ended, starting fade out");
            startFadeOut();
        };

        // 使用 postAtTime 确保精确触发（息屏也能触发，因为有 PARTIAL_WAKE_LOCK）
        handler.postAtTime(stopRunnable, stopAt);

        Log.i(TAG, "Scheduled stop in " + (durationMs / 1000) + " seconds (fade out " + fadeOutMs + "ms before end)");
    }

    /** 播放下一首 */
    private void playNextTrack() {
        if (playlist.isEmpty()) return;
        int nextIndex = currentTrackIndex + 1;
        if (nextIndex >= playlist.size()) {
            nextIndex = 0; // 循环播放列表
        }
        stopFadeRunnables();
        playTrack(nextIndex);
    }

    /** 播放上一首 */
    private void playPrevTrack() {
        if (playlist.isEmpty()) return;
        int prevIndex = currentTrackIndex - 1;
        if (prevIndex < 0) {
            prevIndex = playlist.size() - 1;
        }
        stopFadeRunnables();
        playTrack(prevIndex);
    }

    /** 停止渐入渐出 Runnable */
    private void stopFadeRunnables() {
        if (fadeInRunnable != null) {
            handler.removeCallbacks(fadeInRunnable);
            fadeInRunnable = null;
        }
        if (fadeOutRunnable != null) {
            handler.removeCallbacks(fadeOutRunnable);
            fadeOutRunnable = null;
        }
    }

    /** 设置 MediaPlayer 音量 */
    private void setMediaPlayerVolume(int volumePercent) {
        if (mediaPlayer == null) return;
        float vol = Math.max(0f, Math.min(1f, volumePercent / 100f));
        mediaPlayer.setVolume(vol, vol);
    }

    /** 请求音频焦点 */
    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(audioFocusListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    /** 释放音频焦点 */
    private void releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
            audioFocusRequest = null;
        } else {
            audioManager.abandonAudioFocus(audioFocusListener);
        }
    }

    /** 设置 MediaSession */
    private void setupMediaSession() {
        if (mediaSession != null) {
            mediaSession.release();
        }
        mediaSession = new MediaSession(this, "DreamPillow");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                if (isPaused && mediaPlayer != null) {
                    mediaPlayer.start();
                    isPaused = false;
                    updateNotification("正在播放: " + taskName);
                    updateMediaSessionPlaybackState(true);
                }
            }

            @Override
            public void onPause() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPaused = true;
                    updateNotification("暂停中: " + taskName);
                    updateMediaSessionPlaybackState(false);
                }
            }

            @Override
            public void onStop() {
                stopPlaybackInternal();
                stopForeground(true);
                stopSelf();
            }

            @Override
            public void onSkipToNext() {
                playNextTrack();
            }

            @Override
            public void onSkipToPrevious() {
                playPrevTrack();
            }
        });

        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT |
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .build());
        mediaSession.setActive(true);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception e) { /* ignore */ }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
    }

    private void stopPlaybackInternal() {
        isPlaying = false;
        isPaused = false;
        currentTaskId = null;
        releaseMediaPlayer();
        stopFadeRunnables();
        if (stopRunnable != null) {
            handler.removeCallbacks(stopRunnable);
            stopRunnable = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        releaseAudioFocus();
        Log.i(TAG, "Playback stopped");
    }

    private void startForegroundWithNotification() {
        Notification notification = buildNotification("准备播放...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String contentText) {
        // 停止按钮
        Intent stopIntent = new Intent(this, AudioPlaybackService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 暂停按钮
        Intent pauseIntent = new Intent(this, AudioPlaybackService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pausePending = PendingIntent.getService(
                this, 1, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 下一首按钮
        Intent nextIntent = new Intent(this, AudioPlaybackService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getService(
                this, 2, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 上一首按钮
        Intent prevIntent = new Intent(this, AudioPlaybackService.class);
        prevIntent.setAction(ACTION_PREV);
        PendingIntent prevPending = PendingIntent.getService(
                this, 3, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("梦枕")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_previous, "上一首", prevPending)
                .addAction(android.R.drawable.ic_media_pause, "暂停", pausePending)
                .addAction(android.R.drawable.ic_media_next, "下一首", nextPending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopPending);

        // 关联 MediaSession（Android 5.0+）
        if (mediaSession != null) {
            builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2));
        }

        return builder.build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    private void updateMediaSessionPlaybackState(boolean playing) {
        if (mediaSession == null) return;
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, 0, 1.0f)
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT |
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "梦枕播放", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("音频播放服务");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    /** 下载音频到缓存 */
    private File downloadToCache(String audioUrl) {
        HttpURLConnection conn = null;
        InputStream input = null;
        FileOutputStream output = null;
        try {
            URL url = new URL(audioUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                Log.e(TAG, "Download failed: HTTP " + responseCode + " for " + audioUrl);
                return null;
            }

            int totalSize = conn.getContentLength();
            Log.i(TAG, "Downloading audio: " + totalSize + " bytes from " + audioUrl);

            File cacheDir = new File(getCacheDir(), "audio_cache");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            String fileName = "audio_" + Math.abs(audioUrl.hashCode()) + ".dat";
            File cacheFile = new File(cacheDir, fileName);

            input = conn.getInputStream();
            output = new FileOutputStream(cacheFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            output.flush();

            Log.i(TAG, "Download complete: " + totalRead + " bytes");
            return cacheFile;
        } catch (Exception e) {
            Log.e(TAG, "Download error", e);
            return null;
        } finally {
            try { if (input != null) input.close(); } catch (Exception e) {}
            try { if (output != null) output.close(); } catch (Exception e) {}
            if (conn != null) conn.disconnect();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopPlaybackInternal();
        Log.i(TAG, "Service destroyed");
    }

    // === 静态方法供 JS 桥调用 ===

    public static void stopPlayback(Context context) {
        if (instance != null) {
            instance.stopPlaybackInternal();
            instance.stopForeground(true);
            instance.stopSelf();
            return;
        }
        Intent intent = new Intent(context, AudioPlaybackService.class);
        intent.setAction(ACTION_STOP);
        try {
            context.startService(intent);
        } catch (Exception e) {
            Log.w(TAG, "stopPlayback: service not running or cannot start");
        }
    }

    public static boolean isCurrentlyPlaying() {
        return instance != null && instance.isPlaying;
    }

    public static String getCurrentTaskId() {
        return instance != null ? instance.currentTaskId : null;
    }

    public static int getCurrentTrackIndex() {
        return instance != null ? instance.currentTrackIndex : 0;
    }
}
