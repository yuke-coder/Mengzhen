package com.mengzhen.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mengzhen.app.scheduler.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            AlarmScheduler.get(context).restoreAllAlarms()
        }
    }
}
