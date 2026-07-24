package com.mengzhen.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeModeDataStore by preferencesDataStore(name = "theme_mode")

/**
 * 主题模式 —— 对齐 Web 端 theme-context.tsx 三模式
 * - DataStore key `theme-mode`（与 Web 端 localStorage key 同名）
 * - 值：light / dark / auto（Web 端 system 存储为 auto）
 */
enum class ThemeMode(val prefValue: String, val label: String, val description: String) {
    LIGHT("light", "浅色模式", "始终使用浅色主题"),
    DARK("dark", "深色模式", "始终使用深色主题"),
    SYSTEM("auto", "自动模式", "跟随系统主题设置");

    companion object {
        fun fromPref(value: String?): ThemeMode = when (value) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM // "auto" / "system" / null 均归 SYSTEM（与 Web 端 getStoredTheme 一致）
        }
    }
}

/** 主题模式持久化（DataStore） */
object ThemeModeStore {
    private val KEY = stringPreferencesKey("theme-mode")

    fun modeFlow(context: Context): Flow<ThemeMode> =
        context.themeModeDataStore.data.map { ThemeMode.fromPref(it[KEY]) }

    suspend fun setMode(context: Context, mode: ThemeMode) {
        context.themeModeDataStore.edit { it[KEY] = mode.prefValue }
    }
}

/** 当前主题模式（MainActivity 注入） */
val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }

/** 解析后的明/暗结果（MainActivity 注入：SYSTEM 已按系统解析） */
val LocalIsDarkTheme = compositionLocalOf { false }

// ==================== 品牌色 token（随主题变化，对齐 globals.css） ====================

/** brand-start：亮 #9ED2BE / 暗 #7EEDC4 */
val BrandStartThemed: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF7EEDC4) else Color(0xFF9ED2BE)

/** brand-mid：亮 #7BC4A8 / 暗 #50DDB0 */
val BrandMidThemed: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF50DDB0) else Color(0xFF7BC4A8)

/** brand-end：亮 #5BB892 / 暗 #2BC496 */
val BrandEndThemed: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2BC496) else Color(0xFF5BB892)

/** brand-glow：亮 oklch(0.62 0.16 165)≈Primary / 暗 oklch(0.75 0.14 170)≈PrimaryDark */
val BrandGlowThemed: Color
    @Composable get() = if (LocalIsDarkTheme.current) PrimaryDark else Primary

/** brand-dim：亮 oklch(0.55 0.17 170) / 暗 oklch(0.68 0.15 172) */
val BrandDimThemed: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3FC4A0) else Color(0xFF35977A)
