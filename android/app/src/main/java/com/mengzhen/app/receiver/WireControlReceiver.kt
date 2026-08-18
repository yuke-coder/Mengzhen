package com.mengzhen.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import com.mengzhen.app.audio.AudioPlaybackService

/**
 * 耳机/线控 MediaButton 事件接收器。
 *
 * 对标喜马拉雅 WireControlReceiver 的按键映射：
 * - 单击播放/暂停
 * - 双击下一首
 * - 三击上一首
 * - 长按忽略（不触发语音助手）
 *
 * 该 Receiver 作为 MediaSession 未激活时的兜底入口；播放期间系统优先把
 * MediaButton 事件分发给已激活的 MediaSession，其行为与这里保持一致。
 */
class WireControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
        appContext = context.applicationContext
        handleKeyEvent(event)
    }

    private fun handleKeyEvent(event: KeyEvent) {
        when (event.keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            -> {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        // 重复事件来自长按，按喜马拉雅策略不响应。
                        if (event.repeatCount > 0) {
                            pendingHookClicks = 0
                            handler.removeCallbacks(clickDispatcher)
                            return
                        }
                        lastHookDownTime = event.eventTime
                    }

                    KeyEvent.ACTION_UP -> {
                        if (lastHookDownTime == 0L) return
                        lastHookDownTime = 0L
                        pendingHookClicks++
                        handler.removeCallbacks(clickDispatcher)
                        handler.postDelayed(clickDispatcher, CLICK_WINDOW_MS)
                    }
                }
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> appContext?.let { AudioPlaybackService.next(it) }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> appContext?.let { AudioPlaybackService.previous(it) }
        }
    }

    companion object {
        private const val TAG = "WireControlReceiver"
        private const val CLICK_WINDOW_MS = 500L

        private val handler = Handler(Looper.getMainLooper())

        @Volatile
        private var appContext: Context? = null

        @Volatile
        private var lastHookDownTime: Long = 0L

        @Volatile
        private var pendingHookClicks: Int = 0

        private val clickDispatcher = Runnable {
            val context = appContext ?: return@Runnable
            val count = pendingHookClicks
            pendingHookClicks = 0
            Log.i(TAG, "Dispatch $count hook click(s)")
            when (count) {
                1 -> {
                    if (AudioPlaybackService.isCurrentlyPlaying()) {
                        AudioPlaybackService.pause(context)
                    } else {
                        AudioPlaybackService.resume(context)
                    }
                }

                2 -> AudioPlaybackService.next(context)
                3 -> AudioPlaybackService.previous(context)
                else -> Unit
            }
        }
    }
}
