package com.mengzhen.app.audio

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

/**
 * 喜马拉雅 9.5.1.4 播放器同名 Service 的本地迁移。
 *
 * 它只在播放服务存活期间建立普通绑定，不创建第二条前台通知，也不额外持有
 * WakeLock/WifiLock；播放所需锁由播放器自己的 wake mode 按播放状态管理。
 */
class SustainedListenService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SustainedListenService"

        private val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.i(TAG, "onServiceConnected")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.i(TAG, "onServiceDisconnected")
            }
        }

        fun bind(context: Context) {
            context.bindService(
                Intent(context, SustainedListenService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
        }

        fun unbind(context: Context) {
            runCatching { context.unbindService(connection) }
                .onFailure { Log.w(TAG, "unbind failed", it) }
        }
    }
}
