package com.mengzhen.app;

import android.os.Bundle;
import android.util.Log;

import com.getcapacitor.BridgeActivity;
import com.mengzhen.app.scheduler.AlarmSchedulerPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AlarmSchedulerPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
