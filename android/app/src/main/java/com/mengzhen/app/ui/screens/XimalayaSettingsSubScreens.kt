package com.mengzhen.app.ui.screens

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.store.AppSettingsStore
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 账号与安全页面。
 *
 * 喜马拉雅原版在 SettingFragment 中通过 RN bundle "account" 打开，
 * 通过设备 UI dump 抓取到真实页面结构：账号绑定、设置密码、最近登录设备、
 * 允许快捷登录（开关）、注销账户、常见问题。
 * 梦枕无 RN 环境，改为原生 AndroidView 实现上述功能项。
 */
@Composable
fun XimalayaAccountSafetyScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taskStore = remember(context) { TaskStore.get(context) }
    val api = remember(context) { ApiClient.get(context) }
    val settings = remember(context) { AppSettingsStore.get(context) }
    val user by taskStore.sessionUser.collectAsState()
    var quickLogin by remember {
        mutableStateOf(settings.getBoolean(AppSettingsStore.KEY_QUICK_LOGIN, true))
    }

    AndroidView(
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_account_safety_source, null, false)
        },
        update = { root ->
            root.bindSourceTitle("账号与安全") { navController.popBackStack() }

            // 账号绑定 — 展示绑定手机号（中间四位掩码）
            root.findViewById<View>(R.id.account_safety_bind).setOnClickListener {
                val phone = user?.mobile?.takeIf { it.isNotBlank() }
                val display = phone?.replaceRange(3, 7, "****") ?: "未绑定"
                AlertDialog.Builder(context)
                    .setTitle("账号绑定")
                    .setMessage("当前绑定手机号：$display")
                    .setPositiveButton("知道了", null)
                    .show()
            }

            // 设置密码
            root.findViewById<View>(R.id.account_safety_change_password).setOnClickListener {
                if (user == null) {
                    AlertDialog.Builder(context)
                        .setTitle("设置密码")
                        .setMessage("请先登录后设置密码。")
                        .setPositiveButton("去登录") { _, _ ->
                            navController.navigate(Screen.Login.route)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                } else {
                    AlertDialog.Builder(context)
                        .setTitle("设置密码")
                        .setMessage("密码修改功能开发中，请通过短信验证码登录。")
                        .setPositiveButton("知道了", null)
                        .show()
                }
            }

            // 最近登录设备
            root.findViewById<View>(R.id.account_safety_device_manage).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("最近登录设备")
                    .setMessage("当前设备为唯一登录设备。")
                    .setPositiveButton("知道了", null)
                    .show()
            }

            // 允许快捷登录 — 开关
            root.findViewById<CheckBox>(R.id.account_safety_quick_login).bindChecked(
                quickLogin,
            ) { enabled ->
                quickLogin = enabled
                settings.setBoolean(AppSettingsStore.KEY_QUICK_LOGIN, enabled)
            }

            // 注销账户
            root.findViewById<View>(R.id.account_safety_cancel_account).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("注销账户")
                    .setMessage("注销账户后将永久删除所有数据，且不可恢复。确定要注销吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定注销") { _, _ ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                runCatching { api.logout() }
                            }
                            taskStore.clearSession()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Settings.route) { inclusive = false }
                            }
                        }
                    }
                    .show()
            }

            // 常见问题
            root.findViewById<View>(R.id.account_safety_faq).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("常见问题")
                    .setMessage("1. 如何修改手机号？\n  在「账号绑定」中更换绑定的手机号。\n\n" +
                        "2. 忘记密码怎么办？\n  可通过短信验证码登录后重新设置密码。\n\n" +
                        "3. 如何注销账户？\n  点击「注销账户」按提示操作。\n\n" +
                        "4. 快捷登录是什么？\n  开启后可在已登录设备上免密快捷登录。")
                    .setPositiveButton("知道了", null)
                    .show()
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * 收听偏好设置页面。
 *
 * 喜马拉雅原版通过 InterestUtil.a("设置") 跳转到兴趣标签编辑页，
 * 梦枕改为原生 AndroidView 实现，保留：
 * 年龄/性别偏好展示、兴趣分类入口、自动播放推荐、个性化推荐开关、重置偏好。
 */
@Composable
fun XimalayaListeningPreferenceScreen(navController: NavController) {
    val context = LocalContext.current
    val settings = remember(context) { AppSettingsStore.get(context) }
    var autoPlayRecommend by remember {
        mutableStateOf(
            settings.getBoolean(AppSettingsStore.KEY_AUTO_PLAY_RECOMMENDATION, true),
        )
    }
    var personalizedContent by remember {
        mutableStateOf(
            settings.getBoolean(AppSettingsStore.KEY_PERSONALIZED_CONTENT, true),
        )
    }

    AndroidView(
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_listening_preference_source, null, false)
        },
        update = { root ->
            root.bindSourceTitle("收听偏好设置") { navController.popBackStack() }

            root.findViewById<TextView>(R.id.preference_age_value).text = "未设置"
            root.findViewById<TextView>(R.id.preference_gender_value).text = "未设置"

            root.findViewById<View>(R.id.preference_age_layout).setOnClickListener {
                val ages = arrayOf("0-12岁", "13-18岁", "19-25岁", "26-35岁", "36-45岁", "45岁以上")
                AlertDialog.Builder(context)
                    .setTitle("选择年龄段")
                    .setItems(ages) { _, which ->
                        root.findViewById<TextView>(R.id.preference_age_value).text = ages[which]
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }

            root.findViewById<View>(R.id.preference_gender_layout).setOnClickListener {
                val genders = arrayOf("男", "女", "不指定")
                AlertDialog.Builder(context)
                    .setTitle("选择性别偏好")
                    .setItems(genders) { _, which ->
                        root.findViewById<TextView>(R.id.preference_gender_value).text = genders[which]
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }

            root.findViewById<View>(R.id.preference_category_layout).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("兴趣分类")
                    .setMessage("兴趣分类编辑功能开发中。")
                    .setPositiveButton("知道了", null)
                    .show()
            }

            root.findViewById<CheckBox>(R.id.preference_cb_auto_play).bindChecked(
                autoPlayRecommend,
            ) { enabled ->
                autoPlayRecommend = enabled
                settings.setBoolean(AppSettingsStore.KEY_AUTO_PLAY_RECOMMENDATION, enabled)
                AudioPlaybackService.setAutoContinue(context, enabled)
            }

            root.findViewById<CheckBox>(R.id.preference_cb_personalized).bindChecked(
                personalizedContent,
            ) { enabled ->
                personalizedContent = enabled
                settings.setBoolean(AppSettingsStore.KEY_PERSONALIZED_CONTENT, enabled)
            }

            root.findViewById<View>(R.id.preference_clear_history).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("重置收听偏好")
                    .setMessage("确定要重置所有收听偏好吗？重置后推荐内容将重新学习。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("重置") { _, _ ->
                        autoPlayRecommend = true
                        personalizedContent = true
                        settings.setBoolean(AppSettingsStore.KEY_AUTO_PLAY_RECOMMENDATION, true)
                        settings.setBoolean(AppSettingsStore.KEY_PERSONALIZED_CONTENT, true)
                        AudioPlaybackService.setAutoContinue(context, true)
                        root.findViewById<TextView>(R.id.preference_age_value).text = "未设置"
                        root.findViewById<TextView>(R.id.preference_gender_value).text = "未设置"
                        AppNotice.success(context, "收听偏好已重置")
                    }
                    .show()
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * 锁屏显示设置页面。
 *
 * 基于喜马拉雅 LockScreenSettingFragment.java 原版逻辑：
 * - KEY_LOCK_SCREEN_OPEN: 锁屏总开关
 * - KEY_LOCK_SCREEN_CHECKBOX_CHECKED: 锁屏复选框状态
 * - Android 10+ 需检查 bo.b(context)（锁屏可见性权限）
 * - 低版本直接使用 checkbox 状态
 */
@Composable
fun XimalayaLockScreenSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val settings = remember(context) { AppSettingsStore.get(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var lockScreenEnabled by remember {
        mutableStateOf(settings.getBoolean(AppSettingsStore.KEY_LOCK_SCREEN, true))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lockScreenEnabled = settings.getBoolean(AppSettingsStore.KEY_LOCK_SCREEN, true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_lock_screen_setting_source, null, false)
        },
        update = { root ->
            root.bindSourceTitle("锁屏显示") { navController.popBackStack() }

            root.findViewById<CheckBox>(R.id.lock_screen_cb_enable).bindChecked(
                lockScreenEnabled,
            ) { enabled ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!enabled) {
                        lockScreenEnabled = false
                        settings.setBoolean(AppSettingsStore.KEY_LOCK_SCREEN, false)
                    } else {
                        val canShow = Settings.canDrawOverlays(context)
                        if (!canShow) {
                            AlertDialog.Builder(context)
                                .setTitle("需要权限")
                                .setMessage("锁屏显示需要「显示在其他应用上层」权限，是否前往设置？")
                                .setNegativeButton("取消") { _, _ ->
                                    lockScreenEnabled = false
                                }
                                .setPositiveButton("去设置") { _, _ ->
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}"),
                                    )
                                    context.startActivity(intent)
                                }
                                .show()
                        } else {
                            lockScreenEnabled = true
                            settings.setBoolean(AppSettingsStore.KEY_LOCK_SCREEN, true)
                        }
                    }
                } else {
                    lockScreenEnabled = enabled
                    settings.setBoolean(AppSettingsStore.KEY_LOCK_SCREEN, enabled)
                    AudioPlaybackService.setLockScreenControl(context, enabled)
                }
            }

            root.findViewById<View>(R.id.lock_screen_notification_setting).setOnClickListener {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }

            root.findViewById<View>(R.id.lock_screen_help).setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("锁屏不显示？")
                    .setMessage("如果已开启锁屏显示但仍不显示，请检查以下设置：\n\n" +
                        "1. 系统设置 → 应用 → 梦枕 → 显示在其他应用上层（已开启）\n" +
                        "2. 系统设置 → 应用 → 梦枕 → 通知权限（已开启）\n" +
                        "3. 系统设置 → 锁屏 → 隐私 → 显示完整通知内容\n\n" +
                        "如仍有问题，请重启应用或联系客服。")
                    .setPositiveButton("知道了", null)
                    .show()
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
