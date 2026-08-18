package com.mengzhen.app.data.store

import android.content.Context

/**
 * 设置页与播放服务共用的持久化入口。
 *
 * 键名沿用喜马拉雅 9.5.1.4 SettingFragment / PushSettingFragment，
 * 使迁入界面的状态与实际业务只经过这一处存取。
 */
class AppSettingsStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(key, defaultValue)

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int =
        prefs.getInt(key, defaultValue)

    fun setInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    /** Matches Bilibili's reset-preferences scope: settings only, never account or content. */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val KEY_BREAKPOINT_RESUME = "KEY_BREAKPOINT_RESUME"
        const val KEY_CONTINUE_AFTER_INTERRUPTION = "play_continue_when_interrupted"
        const val KEY_BLUETOOTH_AUTO_PLAY = "KEY_BLUETOOTH_CONTROL_AUTO_PLAY"
        const val KEY_ALLOW_METERED_DOWNLOAD = "is_download_enabled_without_wifi"
        const val KEY_CHILD_SLEEP_MODE = "key_open_child_sleep_mode"
        const val KEY_AUTO_PLAY_RECOMMENDATION = "key_auto_play_recommend"
        const val KEY_VOLUME_BALANCE = "play_volumn_balance"
        const val KEY_LOCK_SCREEN = "KEY_LOCK_SCREEN_OPEN"
        const val KEY_NOTIFICATION_STYLE = "notification_style"
        const val KEY_PUSH_ALL = "is_push_all_v2"
        const val KEY_PUSH_RESERVATION = "pushReservation"
        const val KEY_PUSH_SUBSCRIBE = "pushSubscribe"
        const val KEY_PERSONALIZED_CONTENT = "personalized_content_recommend"
        const val KEY_QUICK_LOGIN = "key_quick_login"

        private const val PREFERENCES_NAME = "setting"

        @Volatile
        private var instance: AppSettingsStore? = null

        fun get(context: Context): AppSettingsStore =
            instance ?: synchronized(this) {
                instance ?: AppSettingsStore(context).also { instance = it }
            }
    }
}
