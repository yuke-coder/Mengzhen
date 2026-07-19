package com.mengzhen.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;
import com.mengzhen.app.scheduler.AlarmSchedulerPlugin;

public class MainActivity extends BridgeActivity {

    private static final int REQ_NOTIFICATION = 1001;
    private static final int REQ_EXACT_ALARM = 1002;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AlarmSchedulerPlugin.class);
        WebView.setWebContentsDebuggingEnabled(true);
        super.onCreate(savedInstanceState);

        // 请求所有必要权限
        requestAllPermissions();
    }

    private void requestAllPermissions() {
        // 1. Android 13+ 通知权限（前台 Service 必需）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
            }
        }

        // 2. Android 12+ 精确闹钟权限（引导用户到设置页）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!Settings.canDrawOverlays(this)) {
                // 不需要 overlay，但检查 exact alarm
            }
        }

        // 3. 电池优化白名单（小米必备）
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String packageName = getPackageName();
                if (!isIgnoringBatteryOptimizations()) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            // 忽略，不阻塞启动
        }
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATION) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            android.util.Log.i("MainActivity", "通知权限: " + (granted ? "已授权" : "被拒绝"));
        }
    }
}
