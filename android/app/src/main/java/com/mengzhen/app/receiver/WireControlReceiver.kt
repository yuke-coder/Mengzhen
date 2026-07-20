package com.mengzhen.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.IntentCompat
import com.mengzhen.app.audio.AudioPlaybackService

class WireControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return

        val event = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return
        if (event.action != KeyEvent.ACTION_UP) return

        when (event.keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> handlePlayPauseClick(context)
            KeyEvent.KEYCODE_MEDIA_NEXT -> sendAction(context, AudioPlaybackService.ACTION_NEXT)
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> sendAction(context, AudioPlaybackService.ACTION_PREV)
            KeyEvent.KEYCODE_MEDIA_STOP -> sendAction(context, AudioPlaybackService.ACTION_STOP)
            KeyEvent.KEYCODE_MEDIA_PLAY -> sendAction(context, AudioPlaybackService.ACTION_RESUME)
            KeyEvent.KEYCODE_MEDIA_PAUSE -> sendAction(context, AudioPlaybackService.ACTION_PAUSE)
            else -> Log.d(TAG, "Unhandled keyCode: ${event.keyCode}")
        }
    }

    private fun handlePlayPauseClick(context: Context) {
        val now = SystemClock.uptimeMillis()
        if (now - lastClickTime < MULTI_CLICK_TIMEOUT) {
            clickCount++
        } else {
            clickCount = 1
        }
        lastClickTime = now

        Handler(Looper.getMainLooper()).postDelayed({
            val count = clickCount
            clickCount = 0
            when {
                count == 1 -> {
                    val action = if (AudioPlaybackService.isCurrentlyPlaying()) AudioPlaybackService.ACTION_PAUSE else AudioPlaybackService.ACTION_RESUME
                    sendAction(context, action)
                }
                count == 2 -> sendAction(context, AudioPlaybackService.ACTION_NEXT)
                count >= 3 -> sendAction(context, AudioPlaybackService.ACTION_PREV)
            }
        }, MULTI_CLICK_TIMEOUT)
    }

    private fun sendAction(context: Context, action: String) {
        val intent = Intent(context, AudioPlaybackService::class.java).setAction(action)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send action: $action", e)
        }
    }

    companion object {
        private const val TAG = "WireControlReceiver"
        private const val MULTI_CLICK_TIMEOUT = 500L

        private var lastClickTime = 0L
        private var clickCount = 0
    }
}
