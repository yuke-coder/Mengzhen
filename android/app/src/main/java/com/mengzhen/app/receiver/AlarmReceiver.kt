package com.mengzhen.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mengzhen.app.scheduler.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Received broadcast: $action")

        if (action != ACTION_ALARM_TRIGGER) return

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        AlarmScheduler.get(context).handleAlarmTrigger(taskId)
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_ALARM_TRIGGER = "com.mengzhen.app.ALARM_TRIGGER"
        const val EXTRA_TASK_ID = "taskId"
    }
}
