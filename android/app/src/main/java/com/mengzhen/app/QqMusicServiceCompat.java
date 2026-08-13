package com.mengzhen.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;

import com.tencent.qqmusicplayerprocess.servicenew.QQPlayerServiceNew;

/**
 * Keeps the imported QQ Music service intact while supplying the receiver flags
 * required by Android 13+ for the service's legacy broadcast registrations.
 */
public final class QqMusicServiceCompat extends QQPlayerServiceNew {
    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
        return super.registerReceiver(receiver, filter);
    }

    @Override
    public Intent registerReceiver(
            BroadcastReceiver receiver,
            IntentFilter filter,
            String permission,
            Handler scheduler
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return super.registerReceiver(
                    receiver,
                    filter,
                    permission,
                    scheduler,
                    Context.RECEIVER_NOT_EXPORTED
            );
        }
        return super.registerReceiver(receiver, filter, permission, scheduler);
    }
}
