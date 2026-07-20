package com.mengzhen.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.audio.SustainedListenService

/**
 * 息屏监听 - 确保保活 Service 运行
 * 对标喜马拉雅 ScreenOnOffReceiver
 *
 * 单例注册，避免 Service 重建时重复注册多个 receiver
 */
class ScreenStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.i(TAG, "Screen OFF - ensuring keepalive service")
                if (AudioPlaybackService.isCurrentlyPlaying()) {
                    SustainedListenService.start(context, "梦枕")
                }
            }
            Intent.ACTION_SCREEN_ON -> Log.i(TAG, "Screen ON")
            Intent.ACTION_USER_PRESENT -> Log.i(TAG, "User present (unlocked)")
        }
    }

    companion object {
        private const val TAG = "ScreenStatusReceiver"

        @Volatile private var instance: ScreenStatusReceiver? = null

        /**
         * 注册息屏监听 - 单例，重复调用不会创建多个 receiver
         */
        fun register(context: Context) {
            if (instance != null) {
                Log.d(TAG, "Already registered, skipping")
                return
            }
            synchronized(this) {
                if (instance != null) return
                val receiver = ScreenStatusReceiver()
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_USER_PRESENT)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(receiver, filter)
                }
                instance = receiver
                Log.i(TAG, "ScreenStatusReceiver registered")
            }
        }

        /**
         * 注销息屏监听
         */
        fun unregister(context: Context) {
            synchronized(this) {
                instance?.let {
                    try {
                        context.unregisterReceiver(it)
                    } catch (e: Exception) {
                        Log.w(TAG, "Unregister failed", e)
                    }
                    instance = null
                    Log.i(TAG, "ScreenStatusReceiver unregistered")
                }
            }
        }
    }
}
