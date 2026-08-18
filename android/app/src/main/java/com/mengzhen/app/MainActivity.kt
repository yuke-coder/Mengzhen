package com.mengzhen.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.audio.PlayProgressStore
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.parseUser
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.scheduler.AlarmScheduler
import com.mengzhen.app.ui.navigation.MengZhenApp
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import com.mengzhen.app.ui.theme.LocalThemeMode
import com.mengzhen.app.ui.theme.MengZhenTheme
import com.mengzhen.app.ui.theme.ThemeMode
import com.mengzhen.app.ui.theme.ThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var initialThemeMode = ThemeMode.SYSTEM
    private var openPlaybackTaskId by mutableStateOf<String?>(null)
    private var sharedBiliVideo by mutableStateOf<String?>(null)
    private var startInBiliAuthorization by mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        initialThemeMode = ThemeModeStore.bootstrapMode(newBase)
        if (initialThemeMode == ThemeMode.SYSTEM) {
            super.attachBaseContext(newBase)
            return
        }
        val configuration = Configuration(newBase.resources.configuration).apply {
            val nightMode = if (initialThemeMode == ThemeMode.DARK) {
                Configuration.UI_MODE_NIGHT_YES
            } else {
                Configuration.UI_MODE_NIGHT_NO
            }
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        val biliPreferences = getSharedPreferences(BILI_CACHE_PREFS_NAME, Context.MODE_PRIVATE)
        val shouldStartBiliAuthorization =
            savedInstanceState == null &&
                intent.action == Intent.ACTION_MAIN &&
                intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
                !biliPreferences.getBoolean(KEY_INITIAL_BILI_AUTH_PAGE_SHOWN, false)
        if (shouldStartBiliAuthorization) {
            startInBiliAuthorization = true
            biliPreferences.edit()
                .putBoolean(KEY_INITIAL_BILI_AUTH_PAGE_SHOWN, true)
                .apply()
        }

        openPlaybackTaskId = intent.getStringExtra(EXTRA_OPEN_PLAYBACK_TASK_ID)
        sharedBiliVideo = extractSharedBiliVideo(intent)

        val taskStore = TaskStore.get(this)
        val api = ApiClient.get(this)
        taskStore.cleanupTransientPlaybackSessions(AudioPlaybackService.getCurrentTaskId())
        PlayProgressStore.get(this).cleanupExpired()
        AlarmScheduler.get(this).restoreAllAlarms()

        lifecycleScope.launch(Dispatchers.IO) {
            val response = runCatching { api.me() }.getOrNull()
            if (response?.optBoolean("authenticated", false) == true) {
                parseUser(response)?.let { taskStore.saveUserSession("cookie_session", it) }
            } else if (response?.optBoolean("authenticated", true) == false) {
                taskStore.clearSession()
            }
            if (taskStore.getSession() != null) {
                PlayProgressStore.get(this@MainActivity).syncFromCloud()
            }
        }

        var themeReady = false
        splash.setKeepOnScreenCondition { !themeReady }
        lifecycleScope.launch {
            val themeMode = ThemeModeStore.modeFlow(this@MainActivity).first()
            if (themeMode != initialThemeMode) {
                ThemeModeStore.syncBootstrapMode(this@MainActivity, themeMode)
                themeReady = true
                recreate()
                return@launch
            }
            val resolvedDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            setContent {
                CompositionLocalProvider(
                    LocalThemeMode provides themeMode,
                    LocalIsDarkTheme provides resolvedDark,
                ) {
                    MengZhenTheme(darkTheme = resolvedDark) {
                        MengZhenApp(
                            startInBiliAuthorization = startInBiliAuthorization,
                            openPlaybackTaskId = openPlaybackTaskId,
                            onPlaybackNavigationConsumed = { openPlaybackTaskId = null },
                            sharedBiliVideo = sharedBiliVideo,
                            onSharedBiliVideoConsumed = { sharedBiliVideo = null },
                        )
                    }
                }
            }
            applySystemBarAppearance(resolvedDark)
            themeReady = true
        }
    }

    private fun applySystemBarAppearance(darkTheme: Boolean) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = if (darkTheme) Color.rgb(40, 40, 40) else Color.WHITE
        window.isNavigationBarContrastEnforced = false
        window.decorView.post {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openPlaybackTaskId = intent.getStringExtra(EXTRA_OPEN_PLAYBACK_TASK_ID)
        sharedBiliVideo = extractSharedBiliVideo(intent)
    }

    private fun extractSharedBiliVideo(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        Intent.ACTION_VIEW -> intent.dataString
        else -> null
    }?.takeIf(String::isNotBlank)

    companion object {
        const val EXTRA_OPEN_PLAYBACK_TASK_ID =
            "com.mengzhen.app.extra.OPEN_PLAYBACK_TASK_ID"

        private const val BILI_CACHE_PREFS_NAME = "bili_cache_preferences"
        private const val KEY_INITIAL_BILI_AUTH_PAGE_SHOWN =
            "initial_bili_auth_page_shown_v2"
    }
}
