package com.mengzhen.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.data.tutorial.PermissionKey
import com.mengzhen.app.ui.navigation.Screen
import com.mengzhen.app.ui.theme.LocalIsDarkTheme

internal enum class BiliHomeAvatarDestination(
    val storedValue: String,
    val title: String,
) {
    LANDING("landing", "营销页"),
    AUDIO("settings", "音频"),
    AUDIO_SEARCH("audio_search", "搜索"),
    BILI_CACHE("bili_cache", "B站缓存"),
    TASKS("tasks", "定时任务"),
    LOGIN("login", "登录"),
    HISTORY("history", "播放历史"),
    PLAYER("templates", "播放页"),
    SOUND_DETAILS("sound_details", "音频详情"),
    ALARM_MANAGER("alarm_manager", "定时任务详情"),
    ALARM_EDITOR("alarm_editor", "编辑定时任务"),
    ALARM_RING("alarm_ring", "播放内容选择"),
    ALARM_HELP("alarm_help", "定时帮助"),
    PROFILE("profile", "我的"),
    APP_SETTINGS("app_settings", "应用设置"),
    FEEDBACK("feedback", "意见反馈"),
    PERMISSION_SETTINGS("permission_settings", "后台播放优化"),
    PERMISSION_TUTORIAL("permission_tutorial", "权限教程"),
    SYSTEM_PERMISSIONS("system_permissions", "系统权限"),
    AVATAR_DESTINATION("home_avatar_destination", "首页头像入口设置"),
    ;

    companion object {
        fun fromStoredValue(value: String?): BiliHomeAvatarDestination =
            entries.firstOrNull { it.storedValue == value } ?: PROFILE
    }
}

internal fun resolveBiliHomeAvatarDestination(
    context: Context,
    destination: BiliHomeAvatarDestination,
): String {
    val store = TaskStore.get(context)
    val activeTask = AudioPlaybackService.getCurrentTaskId()?.let(store::getTaskById)
    val recentTask = activeTask ?: store.getAllTasks().maxByOrNull { it.updatedAt }
    val playableTask = activeTask?.takeIf { it.audios.isNotEmpty() }
        ?: store.getAllTasks()
            .asSequence()
            .filter { it.audios.isNotEmpty() }
            .maxByOrNull { it.updatedAt }

    return when (destination) {
        BiliHomeAvatarDestination.LANDING -> Screen.Landing.route
        BiliHomeAvatarDestination.AUDIO -> Screen.Settings.route
        BiliHomeAvatarDestination.AUDIO_SEARCH -> Screen.AudioSearch.createRoute()
        BiliHomeAvatarDestination.BILI_CACHE -> Screen.BiliCache.route
        BiliHomeAvatarDestination.TASKS -> Screen.Tasks.route
        BiliHomeAvatarDestination.LOGIN -> Screen.Login.route
        BiliHomeAvatarDestination.HISTORY -> Screen.History.route
        BiliHomeAvatarDestination.PLAYER -> playableTask
            ?.let { Screen.Templates.createRoute(it.id) }
            ?: Screen.Settings.route
        BiliHomeAvatarDestination.SOUND_DETAILS -> playableTask
            ?.let {
                Screen.SoundDetails.createRoute(
                    it.id,
                    AudioPlaybackService.getCurrentTrackIndex().coerceIn(it.audios.indices),
                )
            }
            ?: Screen.Settings.route
        BiliHomeAvatarDestination.ALARM_MANAGER -> recentTask
            ?.let { Screen.AlarmManager.createRoute(it.id) }
            ?: Screen.Tasks.route
        BiliHomeAvatarDestination.ALARM_EDITOR -> recentTask
            ?.let { Screen.AlarmEditor.createRoute(it.id) }
            ?: Screen.Tasks.route
        BiliHomeAvatarDestination.ALARM_RING -> playableTask
            ?.let { Screen.AlarmRing.createRoute(it.id) }
            ?: Screen.Tasks.route
        BiliHomeAvatarDestination.ALARM_HELP -> Screen.AlarmHelp.route
        BiliHomeAvatarDestination.PROFILE -> Screen.Profile.route
        BiliHomeAvatarDestination.APP_SETTINGS -> Screen.AppSettings.route
        BiliHomeAvatarDestination.FEEDBACK -> Screen.Feedback.route
        BiliHomeAvatarDestination.PERMISSION_SETTINGS -> Screen.PermissionSettings.createRoute()
        BiliHomeAvatarDestination.PERMISSION_TUTORIAL ->
            Screen.PermissionTutorial.createRoute(PermissionKey.BATTERY_OPTIMIZATION)
        BiliHomeAvatarDestination.SYSTEM_PERMISSIONS -> Screen.SystemPermissions.route
        BiliHomeAvatarDestination.AVATAR_DESTINATION -> Screen.HomeAvatarDestination.route
    }
}

@Composable
fun BiliHomeAvatarDestinationScreen(navController: NavController) {
    val context = LocalContext.current
    val store = remember(context) { TaskStore.get(context) }
    var selected by remember {
        mutableStateOf(
            BiliHomeAvatarDestination.fromStoredValue(store.getHomeAvatarDestination()),
        )
    }
    val colors = biliHomeAvatarColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg2),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            BiliSourceTopAppBar(
                title = "首页头像入口设置",
                colors = colors,
                onBack = { navController.popBackStack() },
            )
            Text(
                text = "首页头像入口跳转设置",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 10.dp),
                color = colors.text3,
                style = TextStyle(fontSize = 12.sp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg1),
            ) {
                BiliHomeAvatarDestination.entries.forEach { destination ->
                    BiliSourceDestinationRow(
                        destination = destination,
                        checked = destination == selected,
                        colors = colors,
                        onClick = {
                            selected = destination
                            store.setHomeAvatarDestination(destination.storedValue)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun BiliHomeAvatarSettingsSection(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = biliHomeAvatarColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg2),
    ) {
        Text(
            text = "工具设置",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 10.dp),
            color = colors.text3,
            style = TextStyle(fontSize = 12.sp),
        )
        BiliHomeAvatarSettingEntry(onClick = onClick, colors = colors)
    }
}

@Composable
private fun BiliHomeAvatarSettingEntry(
    onClick: () -> Unit,
    colors: BiliHomeAvatarColors,
) {
    val iconFont = remember { FontFamily(Font(R.font.bili_source_iconfont)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg1)
            .sourceClickable(onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "首页头像入口设置",
            color = colors.text1,
            style = TextStyle(fontSize = 16.sp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "\uEA24",
            color = colors.text3,
            fontFamily = iconFont,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun BiliSourceTopAppBar(
    title: String,
    colors: BiliHomeAvatarColors,
    onBack: () -> Unit,
) {
    val iconFont = remember { FontFamily(Font(R.font.bili_source_iconfont)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg1)
            .statusBarsPadding()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .sourceClickable(onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\uEA03",
                color = colors.text1,
                fontFamily = iconFont,
                fontSize = 24.sp,
                lineHeight = 24.sp,
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = colors.text1,
            style = TextStyle(fontSize = 17.sp),
        )
    }
}

@Composable
private fun BiliSourceDestinationRow(
    destination: BiliHomeAvatarDestination,
    checked: Boolean,
    colors: BiliHomeAvatarColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sourceClickable(onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = destination.title,
                color = colors.text1,
                style = TextStyle(fontSize = 16.sp),
            )
            Text(
                text = if (destination == BiliHomeAvatarDestination.PROFILE) {
                    "点击首页左上角头像入口跳转至我的页"
                } else {
                    "点击首页左上角头像入口跳转至${destination.title}"
                },
                color = colors.text3,
                style = TextStyle(fontSize = 12.sp),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { if (!checked) onClick() },
            modifier = Modifier.padding(start = 20.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.pi5,
                checkedTrackColor = colors.pi5,
                checkedTrackAlpha = 0.6f,
                uncheckedThumbColor = Color(0xFFE3E5E7),
                uncheckedTrackColor = Color(0xFF9499A0),
                uncheckedTrackAlpha = 1f,
                disabledCheckedThumbColor = Color(0xFF999999),
                disabledCheckedTrackColor = Color(0xFFAEB3B9),
                disabledUncheckedThumbColor = Color(0xFF999999),
                disabledUncheckedTrackColor = Color(0xFFAEB3B9),
            ),
        )
    }
}

private fun Modifier.sourceClickable(onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )

private data class BiliHomeAvatarColors(
    val bg1: Color,
    val bg2: Color,
    val text1: Color,
    val text3: Color,
    val pi5: Color,
)

@Composable
private fun biliHomeAvatarColors(): BiliHomeAvatarColors =
    if (LocalIsDarkTheme.current) {
        BiliHomeAvatarColors(
            bg1 = Color(0xFF17181A),
            bg2 = Color(0xFF101011),
            text1 = Color(0xFFE7E9EB),
            text3 = Color(0xFF757A81),
            pi5 = Color(0xFFD44E7D),
        )
    } else {
        BiliHomeAvatarColors(
            bg1 = Color(0xFFFFFFFF),
            bg2 = Color(0xFFF6F7F8),
            text1 = Color(0xFF18191C),
            text3 = Color(0xFF9499A0),
            pi5 = Color(0xFFFF6699),
        )
    }
