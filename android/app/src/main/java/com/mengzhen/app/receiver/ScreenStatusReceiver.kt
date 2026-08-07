package com.mengzhen.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.util.concurrent.CopyOnWriteArrayList

/** 喜马拉雅 9.5.1.4 ScreenStatusReceiver 的事件分发结构。 */
class ScreenStatusReceiver : BroadcastReceiver() {

    fun interface Listener {
        fun onScreenStatusChange(status: Int)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val status = when (intent.action) {
            Intent.ACTION_SCREEN_ON -> SCREEN_ON
            Intent.ACTION_SCREEN_OFF -> SCREEN_OFF
            else -> return
        }
        listeners.forEach { it.onScreenStatusChange(status) }
    }

    private fun register(context: Context) {
        context.registerReceiver(
            this,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
        )
    }

    companion object {
        const val SCREEN_ON = 1
        const val SCREEN_OFF = 2

        private val listeners = CopyOnWriteArrayList<Listener>()

        @Volatile
        private var registered = false

        @Synchronized
        fun register(context: Context) {
            if (registered) return
            ScreenStatusReceiver().register(context.applicationContext)
            registered = true
        }

        fun addListener(listener: Listener) {
            if (!listeners.contains(listener)) listeners.add(listener)
        }

        fun removeListener(listener: Listener) {
            listeners.remove(listener)
        }
    }
}
