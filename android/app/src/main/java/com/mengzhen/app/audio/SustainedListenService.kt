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
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mengzhen.app.MainActivity

/**
 * 息屏保活 Service - 对标喜马拉雅 XiMaLaYaService + AssistService 双 Service 保活
 *
 * 功能：
 * 1. 前台 Service + WakeLock + WifiLock 保持 CPU 和网络
 * 2. Doze 模式监听 - 检测到 Doze 时引导用户加白名单
 * 3. 电池优化白名单引导
 * 4. 双 Service 互相拉起（AudioPlaybackService 挂了 SustainedListenService 保活，反过来同理）
 */
class SustainedListenService : Service() {

    private val tag = "SustainedListenSvc"
    private val channelId = "dream_pillow_keepalive"
    private val notificationId = 1000

    private var wakeLock: PowerManager.WakeLock? = null
    private var dozeReceiverRegistered = false
    private var userStopped = false // 区分用户主动停止 vs 系统杀死

    /** Doze 模式监听 - 对标喜马拉雅 DozeReceiver */
    private val dozeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isDeviceIdleMode) {
                    Log.w(tag, "Device entered Doze mode")
                    // Doze 模式下检查是否在白名单
                    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                        Log.w(tag, "App not in battery optimization whitelist, playback may be interrupted")
                    }
                } else {
                    Log.i(tag, "Device exited Doze mode")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DreamPillow::KeepAlive")
        wakeLock?.setReferenceCounted(false)

        // Doze 模式监听 - 对标喜马拉雅 DozeReceiver
        // Android 14+ 必须指定 RECEIVER_NOT_EXPORTED
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dozeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dozeReceiver, filter)
        }
        dozeReceiverRegistered = true

        Log.i(tag, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopKeepAlive()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val taskName = intent?.getStringExtra("taskName") ?: "梦枕"
        startForegroundWithNotification(taskName)
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(12 * 60 * 60 * 1000L)
        }

        // 检查电池优化白名单 - 对标喜马拉雅 BaseBatteryOptimizationPermission
        checkBatteryOptimization()

        Log.i(tag, "KeepAlive started for: $taskName")
        return START_STICKY
    }

    /** 电池优化白名单检查 - 对标喜马拉雅 BaseBatteryOptimizationPermission */
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                // 不在白名单中，发送通知引导用户加入
                showBatteryOptimizationNotification()
            }
        }
    }

    private fun showBatteryOptimizationNotification() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("梦枕")
            .setContentText("点击允许后台运行，确保息屏播放不被中断")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        (getSystemService(NotificationManager::class.java))?.notify(notificationId + 1, notification)
    }

    private fun startForegroundWithNotification(taskName: String) {
        val notification = buildNotification(taskName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun buildNotification(taskName: String): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("梦枕")
            .setContentText("后台播放保持中: $taskName")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPending)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "梦枕后台保活", NotificationManager.IMPORTANCE_LOW).apply {
                description = "保持音频播放服务在后台持续运行"
                setShowBadge(false)
            }
            (getSystemService(NotificationManager::class.java))?.createNotificationChannel(channel)
        }
    }

    private fun stopKeepAlive() {
        userStopped = true
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 用户从最近任务列表划掉时，尝试拉起保活
        // 对标喜马拉雅 XiMaLaYaService + AssistService 互相拉起
        if (AudioPlaybackService.isCurrentlyPlaying()) {
            Log.i(tag, "Task removed but playback active, restarting keepalive")
            val restartIntent = Intent(this, SustainedListenService::class.java)
                .setAction(ACTION_START)
                .putExtra("taskName", "梦枕")
            try {
                startService(restartIntent)
            } catch (e: Exception) {
                Log.w(tag, "Restart failed", e)
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (dozeReceiverRegistered) {
            unregisterReceiver(dozeReceiver)
            dozeReceiverRegistered = false
        }
        stopKeepAlive()
        // 双 Service 保活 - 如果不是用户主动停止，拉起 AudioPlaybackService
        // 对标喜马拉雅 AssistService.onDestroy -> startService(XiMaLaYaService)
        if (!userStopped) {
            Log.i(tag, "System killed service, restarting AudioPlaybackService")
            val restartIntent = Intent(this, AudioPlaybackService::class.java)
                .setAction(AudioPlaybackService.ACTION_RESTART)
            try {
                // Android 8+ 必须用 startForegroundService 启动前台 Service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(restartIntent)
                } else {
                    startService(restartIntent)
                }
            } catch (e: Exception) {
                Log.w(tag, "Restart AudioPlaybackService failed", e)
            }
        }
        instance = null
        super.onDestroy()
        Log.i(tag, "Service destroyed")
    }

    companion object {
        private const val ACTION_START = "com.mengzhen.app.START_KEEPALIVE"
        private const val ACTION_STOP = "com.mengzhen.app.STOP_KEEPALIVE"

        @Volatile private var instance: SustainedListenService? = null
        fun isRunning(): Boolean = instance != null

        fun start(context: Context, taskName: String) {
            val intent = Intent(context, SustainedListenService::class.java)
                .setAction(ACTION_START)
                .putExtra("taskName", taskName)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("SustainedListenService", "Start failed", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SustainedListenService::class.java).setAction(ACTION_STOP)
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w("SustainedListenService", "Stop failed", e)
            }
        }
    }
}
