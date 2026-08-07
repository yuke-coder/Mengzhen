package com.mengzhen.app.ui.screens

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mengzhen.app.BuildConfig
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.audio.PlayProgressStore
import com.mengzhen.app.audio.PlaybackSoundEffect
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.store.AppSettingsStore
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import com.mengzhen.app.ui.theme.LocalThemeMode
import com.mengzhen.app.ui.theme.ThemeMode
import com.mengzhen.app.ui.theme.ThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun XimalayaAppSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember(context) { AppSettingsStore.get(context) }
    val taskStore = remember(context) { TaskStore.get(context) }
    val api = remember(context) { ApiClient.get(context) }
    val user by taskStore.sessionUser.collectAsState()
    val themeMode = LocalThemeMode.current
    var effect by remember(context) {
        mutableStateOf(AudioPlaybackService.getSoundEffect(context))
    }
    var showSoundSettings by remember { mutableStateOf(false) }
    var breakpointResume by remember {
        mutableStateOf(settings.getBoolean(AppSettingsStore.KEY_BREAKPOINT_RESUME, true))
    }
    var continueAfterInterruption by remember {
        mutableStateOf(
            settings.getBoolean(AppSettingsStore.KEY_CONTINUE_AFTER_INTERRUPTION, false),
        )
    }
    var bluetoothAutoPlay by remember {
        mutableStateOf(settings.getBoolean(AppSettingsStore.KEY_BLUETOOTH_AUTO_PLAY, false))
    }
    var allowMeteredDownload by remember {
        mutableStateOf(settings.getBoolean(AppSettingsStore.KEY_ALLOW_METERED_DOWNLOAD, false))
    }
    var autoPlayNext by remember {
        mutableStateOf(
            settings.getBoolean(
                AppSettingsStore.KEY_AUTO_PLAY_RECOMMENDATION,
                AudioPlaybackService.getAutoContinue(context),
            ),
        )
    }
    var cacheBytes by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        cacheBytes = withContext(Dispatchers.IO) {
            AudioPlaybackService.getTransientCacheSize(context)
        }
    }

    fun chooseTheme() {
        AlertDialog.Builder(context)
            .setTitle("页面模式")
            .setSingleChoiceItems(
                ThemeMode.entries.map(ThemeMode::label).toTypedArray(),
                ThemeMode.entries.indexOf(themeMode),
            ) { dialog, which ->
                val selected = ThemeMode.entries[which]
                dialog.dismiss()
                if (selected == themeMode) return@setSingleChoiceItems
                scope.launch {
                    ThemeModeStore.setMode(context, selected)
                    context.findActivity()?.recreate()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    fun clearCache() {
        AlertDialog.Builder(context)
            .setTitle("存储空间清理")
            .setMessage("清除播放和导入过程中产生的临时缓存？已下载音频不会被删除。")
            .setNegativeButton("取消", null)
            .setPositiveButton("清理") { _, _ ->
                scope.launch {
                    val cleared = withContext(Dispatchers.IO) {
                        AudioPlaybackService.clearTransientCache(context)
                    }
                    cacheBytes = withContext(Dispatchers.IO) {
                        AudioPlaybackService.getTransientCacheSize(context)
                    }
                    AppNotice.success(context, "已清理${formatBytes(context, cleared)}缓存")
                }
            }
            .show()
    }

    fun leaveAccount() {
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { api.logout() } }
            taskStore.clearSession()
            navController.navigate(Screen.Login.route) { launchSingleTop = true }
        }
    }

    AndroidView(
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_app_setting_source, null, false)
        },
        update = { root ->
            root.bindSourceTitle("设置") { navController.popBackStack() }

            root.findViewById<View>(R.id.main_tv_account_safety).setOnClickListener {
                navController.navigate(if (user == null) Screen.Login.route else Screen.Profile.route)
            }
            root.findViewById<View>(R.id.main_privacySetting).setOnClickListener {
                navController.navigate(Screen.PrivacySettings.route)
            }
            root.findViewById<View>(R.id.main_preferenceSetting).setOnClickListener {
                navController.navigate(Screen.Settings.route) { launchSingleTop = true }
            }
            root.findViewById<View>(R.id.main_tv_mode_change).setOnClickListener {
                chooseTheme()
            }
            root.findViewById<View>(R.id.main_ll_track_quality_level).setOnClickListener {
                showSoundSettings = true
            }
            root.findViewById<View>(R.id.main_ll_track_download_quality_level).setOnClickListener {
                showSoundSettings = true
            }
            root.findViewById<TextView>(R.id.main_tv_track_quality).text = effect.displayName
            root.findViewById<TextView>(R.id.main_tv_track_download_quality).text = "原始音质"

            root.findViewById<CheckBox>(R.id.main_volume_setting).bindChecked(
                checked = effect == PlaybackSoundEffect.LOUDNESS,
            ) { enabled ->
                effect = if (enabled) PlaybackSoundEffect.LOUDNESS else PlaybackSoundEffect.ORIGINAL
                settings.setBoolean(AppSettingsStore.KEY_VOLUME_BALANCE, enabled)
                AudioPlaybackService.setSoundEffect(context, effect)
            }
            root.findViewById<CheckBox>(R.id.main_continueListenSwitch).bindChecked(
                breakpointResume,
            ) { enabled ->
                breakpointResume = enabled
                settings.setBoolean(AppSettingsStore.KEY_BREAKPOINT_RESUME, enabled)
                if (!enabled) scope.launch(Dispatchers.IO) {
                    PlayProgressStore.get(context).clearAll()
                }
            }
            root.findViewById<CheckBox>(R.id.main_sb_play_continue_when_interrupted)
                .bindChecked(continueAfterInterruption) { enabled ->
                    continueAfterInterruption = enabled
                    settings.setBoolean(
                        AppSettingsStore.KEY_CONTINUE_AFTER_INTERRUPTION,
                        enabled,
                    )
                }
            root.findViewById<CheckBox>(R.id.main_sb_forbid_wire_control_auto_play)
                .bindChecked(bluetoothAutoPlay) { enabled ->
                    bluetoothAutoPlay = enabled
                    settings.setBoolean(AppSettingsStore.KEY_BLUETOOTH_AUTO_PLAY, enabled)
                }
            root.findViewById<CheckBox>(R.id.main_cb_allow_download_without_wifi)
                .bindChecked(allowMeteredDownload) { enabled ->
                    if (enabled) {
                        AlertDialog.Builder(context)
                            .setTitle("允许移动网络下载")
                            .setMessage("使用移动网络下载音频可能产生流量费用。")
                            .setNegativeButton("取消") { _, _ -> allowMeteredDownload = false }
                            .setPositiveButton("允许") { _, _ ->
                                allowMeteredDownload = true
                                settings.setBoolean(
                                    AppSettingsStore.KEY_ALLOW_METERED_DOWNLOAD,
                                    true,
                                )
                            }
                            .show()
                    } else {
                        allowMeteredDownload = false
                        settings.setBoolean(AppSettingsStore.KEY_ALLOW_METERED_DOWNLOAD, false)
                    }
                }
            root.findViewById<CheckBox>(R.id.main_cb_auto_play).bindChecked(autoPlayNext) { enabled ->
                autoPlayNext = enabled
                settings.setBoolean(AppSettingsStore.KEY_AUTO_PLAY_RECOMMENDATION, enabled)
                AudioPlaybackService.setAutoContinue(context, enabled)
            }
            root.findViewById<View>(R.id.main_setting_listen_permission).setOnClickListener {
                navController.navigate(Screen.PermissionSettings.createRoute())
            }
            root.findViewById<View>(R.id.main_ll_push_set).setOnClickListener {
                navController.navigate(Screen.NotificationSettings.route)
            }
            root.findViewById<TextView>(R.id.main_pushSetting_sub).text =
                if (settings.getBoolean(AppSettingsStore.KEY_PUSH_ALL, true)) "已开启" else "已关闭"
            root.findViewById<View>(R.id.main_tv_down_cache).setOnClickListener { clearCache() }
            root.findViewById<TextView>(R.id.main_tv_cache_size).text =
                cacheBytes?.let { formatBytes(context, it) } ?: "计算中"
            root.findViewById<View>(R.id.main_tv_chat_xmly_setting).setOnClickListener {
                navController.navigate(Screen.AudioSearch.createRoute(voice = true))
            }
            root.findViewById<View>(R.id.main_setting_dark_mode).setOnClickListener { chooseTheme() }
            root.findViewById<TextView>(R.id.main_tv_dark_mode).text = themeMode.label
            root.findViewById<View>(R.id.main_setting_lock_screen).setOnClickListener {
                navController.navigate(Screen.PermissionSettings.createRoute())
            }
            root.findViewById<TextView>(R.id.main_setting_lock_screen_open_tv).text = "播放时开启"
            root.findViewById<View>(R.id.main_tv_about).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("关于梦枕")
                    .setMessage("版本 ${BuildConfig.VERSION_NAME}")
                    .setPositiveButton("知道了", null)
                    .show()
            }

            val changeAccount = root.findViewById<TextView>(R.id.main_tv_change_account)
            val logout = root.findViewById<TextView>(R.id.main_tv_login)
            if (user == null) {
                changeAccount.text = "登录/注册"
                changeAccount.setOnClickListener { navController.navigate(Screen.Login.route) }
                logout.visibility = View.GONE
            } else {
                changeAccount.text = "切换账号"
                changeAccount.setOnClickListener { leaveAccount() }
                logout.visibility = View.VISIBLE
                logout.text = "退出登录"
                logout.setOnClickListener { leaveAccount() }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )

    if (showSoundSettings) {
        XimalayaSourceSoundEffectQualitySheet(
            initialEffect = effect,
            onEffectSelected = { selected ->
                effect = selected
                settings.setBoolean(
                    AppSettingsStore.KEY_VOLUME_BALANCE,
                    selected == PlaybackSoundEffect.LOUDNESS,
                )
                AudioPlaybackService.setSoundEffect(context, selected)
            },
            onDismiss = { showSoundSettings = false },
        )
    }
}

@Composable
fun XimalayaPrivacySettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_privacy_setting_source, null, false)
        },
        update = { root ->
            root.bindSourceTitle("隐私设置") { navController.popBackStack() }
            root.findViewById<View>(R.id.main_cl_system_setting).setOnClickListener {
                navController.navigate(Screen.SystemPermissions.route)
            }
            root.findViewById<View>(R.id.main_cl_clear_history).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("清除历史个性记录")
                    .setMessage("清除全部音频播放进度？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("清除") { _, _ ->
                        scope.launch(Dispatchers.IO) {
                            PlayProgressStore.get(context).clearAll()
                            withContext(Dispatchers.Main) {
                                AppNotice.success(context, "历史播放进度已清除")
                            }
                        }
                    }
                    .show()
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun XimalayaNotificationSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsStore = remember(context) { AppSettingsStore.get(context) }
    var notificationPermission by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var taskNotices by remember {
        mutableStateOf(settingsStore.getBoolean(AppSettingsStore.KEY_PUSH_ALL, true))
    }
    var permissionWarnings by remember {
        mutableStateOf(settingsStore.getBoolean(AppSettingsStore.KEY_PUSH_RESERVATION, true))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermission = granted
        taskNotices = granted
        settingsStore.setBoolean(AppSettingsStore.KEY_PUSH_ALL, granted)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationPermission = NotificationManagerCompat.from(context)
                    .areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_push_setting_source, null, false)
        },
        update = { root ->
            root.bindSourceTitle("通知设置") { navController.popBackStack() }
            root.findViewById<TextView>(R.id.main_tv_open_push_prompt).visibility =
                if (taskNotices && notificationPermission) View.GONE else View.VISIBLE
            root.findViewById<CheckBox>(R.id.main_sb_accept_push_switch).bindChecked(
                taskNotices && notificationPermission,
            ) { enabled ->
                if (!enabled) {
                    taskNotices = false
                    settingsStore.setBoolean(AppSettingsStore.KEY_PUSH_ALL, false)
                } else if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !notificationPermission
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (!notificationPermission) {
                    context.openNotificationSettings()
                } else {
                    taskNotices = true
                    settingsStore.setBoolean(AppSettingsStore.KEY_PUSH_ALL, true)
                }
            }
            root.findViewById<CheckBox>(R.id.main_sb_reservation).apply {
                isEnabled = taskNotices && notificationPermission
                bindChecked(permissionWarnings && isEnabled) { enabled ->
                    permissionWarnings = enabled
                    settingsStore.setBoolean(AppSettingsStore.KEY_PUSH_RESERVATION, enabled)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun View.bindSourceTitle(title: String, onBack: () -> Unit) {
    findViewById<TextView>(R.id.ximalaya_title_text).text = title
    findViewById<View>(R.id.ximalaya_title_back).apply {
        visibility = View.VISIBLE
        setOnClickListener { onBack() }
    }
}

private fun CheckBox.bindChecked(checked: Boolean, onChanged: (Boolean) -> Unit) {
    setOnCheckedChangeListener(null)
    isChecked = checked
    setOnCheckedChangeListener { _, value -> onChanged(value) }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
    )
}

private fun formatBytes(context: Context, bytes: Long): String =
    android.text.format.Formatter.formatFileSize(context, bytes.coerceAtLeast(0L))
