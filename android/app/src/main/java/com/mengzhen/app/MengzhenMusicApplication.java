package com.mengzhen.app;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import com.tencent.qqmusic.MusicApplication;
import com.tencent.qqmusiccommon.util.Util4Common;
import com.tencent.qqmusiccommon.util.Util4Process;

public final class MengzhenMusicApplication extends MusicApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        // QQ Music image pipeline (e.g. PictureSelectorActivity) needs
        // com.tencent.qqmusic.module.common.a to hold the Application context.
        // MusicApplication only stores it in its own mContext field — the
        // module-level holder is never primed, so the first call to
        // CgiUtil.init() → sp.a.c() → a.c() → getSharedPreferences()
        // throws a NullPointerException.
        com.tencent.qqmusic.module.common.a.d(getApplicationContext());

        // The QQ player page (NewPlayerActivity) uses a translucent theme and relies
        // on the skin engine to draw its background/colors. In the original app the
        // skin engine is initialised by the SkinEngineInitTask boot task, which is
        // only scheduled by QQ's own AppStarterActivity launch chain. Since Mengzhen
        // uses its own MainActivity as the entry point, that boot task never runs,
        // so the player shows a blank/white screen. Trigger the same initialisation
        // directly. SkinManager.c0() has internal guards (K flag) and try/catch, so
        // it is safe to call even if some dependency is missing.
        try {
            com.tencent.qqmusic.ui.skin.SkinManager.c0();
        } catch (Throwable ignored) {
            // Skin initialisation is best-effort; the player still opens.
        }
    }

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

    @Override
    protected void attachBaseContext(Context base) {
        String process = Application.getProcessName();
        String qqProcessName = process.endsWith(":QQPlayerService")
                ? Util4Common.QQ_PLAYER_PROCESS_NAME
                : Util4Common.QQ_MAIN_PROCESS_NAME;
        Util4Process.setBackupProcessName(qqProcessName);
        primeQqProcessName(qqProcessName);
        super.attachBaseContext(base);
    }

    private static void primeQqProcessName(String processName) {
        try {
            Class<?> type = Class.forName("com.tencent.qqmusiccommon.util.Util4Process");
            java.lang.reflect.Field name = type.getDeclaredField("mProgressName");
            java.lang.reflect.Field hash = type.getDeclaredField("mProcessNameHashCode");
            java.lang.reflect.Field ready = type.getDeclaredField("mHasGetProcessNameHashCode");
            name.setAccessible(true);
            hash.setAccessible(true);
            ready.setAccessible(true);
            name.set(null, processName);
            hash.setInt(null, processName.hashCode());
            ready.setBoolean(null, true);
        } catch (Throwable ignored) {
            // The bundled runtime may change its private fields between versions.
        }
    }
}
