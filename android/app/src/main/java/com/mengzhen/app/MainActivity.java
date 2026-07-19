package com.mengzhen.app;

import android.os.Bundle;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;
import com.mengzhen.app.scheduler.AlarmSchedulerPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AlarmSchedulerPlugin.class);
        // Debug 模式开启 WebView 调试
        WebView.setWebContentsDebuggingEnabled(true);
        super.onCreate(savedInstanceState);
    }
}
