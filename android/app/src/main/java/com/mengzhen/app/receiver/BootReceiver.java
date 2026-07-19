package com.mengzhen.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mengzhen.app.scheduler.AlarmScheduler;

/**
 * 设备重启后恢复所有闹钟
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AlarmScheduler scheduler = AlarmScheduler.getInstance(context);
            scheduler.restoreAllAlarms();
        }
    }
}
