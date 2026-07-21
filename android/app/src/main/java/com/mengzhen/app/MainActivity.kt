package com.mengzhen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mengzhen.app.audio.PlayProgressStore
import com.mengzhen.app.scheduler.AlarmScheduler
import com.mengzhen.app.scheduler.TaskStorage
import com.mengzhen.app.ui.theme.MengZhenTheme
import com.mengzhen.app.ui.navigation.MengZhenApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动时清理过期数据 + 同步云端进度 + 恢复闹钟
        TaskStorage.get(this).cleanupCompletedOnceTasks()
        TaskStorage.get(this).cleanupCancelledTasks()
        PlayProgressStore.get(this).cleanupExpired()
        AlarmScheduler.get(this).restoreAllAlarms()

        // 异步同步云端播放进度
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            PlayProgressStore.get(this@MainActivity).syncFromCloud()
        }

        setContent {
            MengZhenTheme {
                MengZhenApp()
            }
        }
    }
}
