package com.mengzhen.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mengzhen.app.scheduler.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                AlarmScheduler.get(context).restoreAllAlarms()
            }
        }
    }
}
