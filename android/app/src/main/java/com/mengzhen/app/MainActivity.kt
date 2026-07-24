package com.mengzhen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mengzhen.app.audio.PlayProgressStore
import com.mengzhen.app.scheduler.AlarmScheduler
import com.mengzhen.app.scheduler.TaskStorage
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import com.mengzhen.app.ui.theme.LocalThemeMode
import com.mengzhen.app.ui.theme.MengZhenTheme
import com.mengzhen.app.ui.theme.ThemeMode
import com.mengzhen.app.ui.theme.ThemeModeStore
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
            // 三模式主题（明/暗/跟随系统），对齐 Web 端 theme-context
            val themeMode by ThemeModeStore.modeFlow(this@MainActivity)
                .collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val resolvedDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }
            CompositionLocalProvider(
                LocalThemeMode provides themeMode,
                LocalIsDarkTheme provides resolvedDark,
            ) {
                MengZhenTheme(darkTheme = resolvedDark) {
                    MengZhenApp()
                }
            }
        }
    }
}
