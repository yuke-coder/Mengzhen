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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.mengzhen.app.R;

import java.io.IOException;

/**
 * Foreground Service 负责音频播放
 * - 使用 MediaPlayer 播放音频（绕过 WebView autoplay 限制）
 * - PARTIAL_WAKE_LOCK 保持 CPU 运转
 * - 前台通知保持 Service 不被杀
 * - 支持音量渐入渐出
 */
public class AudioPlaybackService extends Service {

    private static final String TAG = "AudioPlaybackService";
    private static final String CHANNEL_ID = "dream_pillow_playback";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "com.mengzhen.app.START_PLAYBACK";
    public static final String ACTION_STOP = "com.mengzhen.app.STOP_PLAYBACK";

    private static AudioPlaybackService instance;

    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    private String currentTaskId;
    private boolean isPlaying = false;
    private int targetVolume = 70;
    private boolean enableFade = false;
    private int fadeInDuration = 0;
    private int fadeOutDuration = 0;
    private int playDurationMinutes = 30;
    private String taskName = "梦枕";
    private Runnable fadeOutRunnable;
    private Runnable stopRunnable;

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

        if (ACTION_START.equals(action)) {
            startForegroundWithNotification();

            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(12 * 60 * 60 * 1000L); // 最长 12 小时
            }

            String taskId = intent.getStringExtra("taskId");
            taskName = intent.getStringExtra("taskName");
            playDurationMinutes = intent.getIntExtra("playDurationMinutes", 30);
            targetVolume = intent.getIntExtra("volume", 70);
            enableFade = intent.getBooleanExtra("enableFade", false);
            fadeInDuration = intent.getIntExtra("fadeInDuration", 0);
            fadeOutDuration = intent.getIntExtra("fadeOutDuration", 0);
            String audioUrl = intent.getStringExtra("audioUrl");

            if (taskId != null) {
                currentTaskId = taskId;
                playAudio(audioUrl);
            }
        }

        return START_STICKY;
    }

    private void playAudio(String audioUrl) {
        // 先释放旧的 MediaPlayer
        releaseMediaPlayer();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            mediaPlayer.setLooping(true);

            if (audioUrl != null && !audioUrl.isEmpty()) {
                Log.i(TAG, "Playing audio: " + audioUrl);
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.prepareAsync();
            } else {
                Log.e(TAG, "No audio URL provided");
                stopSelf();
                return;
            }

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.i(TAG, "MediaPlayer prepared, starting playback");

                if (enableFade && fadeInDuration > 0) {
                    // 渐入
                    startFadeIn();
                } else {
                    // 直接设置音量
                    setMediaPlayerVolume(targetVolume);
                }

                mp.start();
                isPlaying = true;
                setupMediaSession();
                updateNotification("正在播放: " + taskName);

                // 设置播放结束定时器
                schedulePlaybackEnd();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                stopPlaybackInternal();
                stopForeground(true);
                stopSelf();
                return true;
            });

        } catch (IOException e) {
            Log.e(TAG, "Failed to set data source", e);
            stopSelf();
        }
    }

    private void startFadeIn() {
        final int steps = Math.max(1, fadeInDuration * 20); // 50ms per step
        final float volumePerStep = (float) targetVolume / 100f / steps;
        final int[] currentStep = {0};

        setMediaPlayerVolume(0);

        Runnable fadeInRunnable = new Runnable() {
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

    private void startFadeOut() {
        if (!enableFade || fadeOutDuration <= 0 || mediaPlayer == null) {
            stopPlaybackInternal();
            return;
        }

        final int currentVol = getCurrentVolumePercent();
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

    private void schedulePlaybackEnd() {
        if (stopRunnable != null) handler.removeCallbacks(stopRunnable);

        long durationMs = playDurationMinutes * 60L * 1000L;
        long fadeOutMs = (enableFade && fadeOutDuration > 0) ? fadeOutDuration * 1000L : 0;
        long stopAt = System.currentTimeMillis() + durationMs - fadeOutMs;

        stopRunnable = () -> {
            Log.i(TAG, "Playback duration ended, starting fade out");
            startFadeOut();
        };
        handler.postAtTime(stopRunnable, stopAt);

        Log.i(TAG, "Scheduled stop in " + playDurationMinutes + " minutes (fade out " + fadeOutMs + "ms before end)");
    }

    private void setMediaPlayerVolume(int volumePercent) {
        if (mediaPlayer == null) return;
        float vol = Math.max(0f, Math.min(1f, volumePercent / 100f));
        mediaPlayer.setVolume(vol, vol);
    }

    private int getCurrentVolumePercent() {
        if (mediaPlayer == null) return 0;
        // MediaPlayer 没有 getVolume，用 targetVolume 近似
        return targetVolume;
    }

    private void setupMediaSession() {
        if (mediaSession != null) {
            mediaSession.release();
        }
        mediaSession = new MediaSession(this, "DreamPillow");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_STOP)
                .build());
        mediaSession.setActive(true);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception e) {
                // ignore
            }
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
        currentTaskId = null;
        releaseMediaPlayer();
        if (stopRunnable != null) {
            handler.removeCallbacks(stopRunnable);
            stopRunnable = null;
        }
        if (fadeOutRunnable != null) {
            handler.removeCallbacks(fadeOutRunnable);
            fadeOutRunnable = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        Log.i(TAG, "Playback stopped");
    }

    private void startForegroundWithNotification() {
        Notification notification = buildNotification("准备播放...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String contentText) {
        Intent stopIntent = new Intent(this, AudioPlaybackService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("梦枕")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "停止", stopPending)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(text));
        }
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
        Intent intent = new Intent(context, AudioPlaybackService.class);
        intent.setAction(ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static boolean isCurrentlyPlaying() {
        return instance != null && instance.isPlaying;
    }

    public static String getCurrentTaskId() {
        return instance != null ? instance.currentTaskId : null;
    }
}
