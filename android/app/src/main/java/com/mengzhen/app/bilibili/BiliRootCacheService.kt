package com.mengzhen.app.bilibili

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

class BiliRootCacheService : RootService() {
    private val delegate = BiliCacheUserService()

    override fun onBind(intent: Intent): IBinder = delegate

    override fun onDestroy() {
        delegate.stopWatchingDefaultCaches()
        super.onDestroy()
    }
}
