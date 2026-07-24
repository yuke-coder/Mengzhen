package com.mengzhen.app.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.mengzhen.app.compat.VendorCompat
import com.mengzhen.app.data.tutorial.PermissionGroup
import com.mengzhen.app.data.tutorial.PermissionKey
import com.mengzhen.app.data.tutorial.TutorialRepository
import com.mengzhen.app.ui.components.permission.PermissionGroupSection
import com.mengzhen.app.ui.components.permission.PermissionItemState
import com.mengzhen.app.ui.components.permission.StatusBanner
import com.mengzhen.app.ui.navigation.Screen

/**
 * 权限设置列表页（推倒重写，完全对齐喜马拉雅「后台播放优化」+ 设计 §3.1）
 *
 * - 必须完成组：忽略电池优化（系统弹窗直达）+ 后台运行策略（教程页），常驻展开
 * - 遇到问题再设置组：按品牌矩阵过滤，默认折叠
 * - StatusBanner：onResume 重检，存在未完成必要项时显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val brand = remember { TutorialRepository.mapBrand(VendorCompat.detectBrand()) }
    val visiblePermissions = remember(brand) { TutorialRepository.getVisiblePermissions(brand) }

    // ---- onResume 触发系统状态重检 ----
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTrigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 系统可检测项（电池优化 / 省电模式 / 休眠保持网络），随 onResume 刷新
    // 值为 null 表示"检测不可用"（如 HarmonyOS 新版缺 wifi_sleep_policy key）——仅参考、不拦截、不误报
    val systemDetected: Map<PermissionKey, Boolean?> = remember(resumeTrigger) {
        mapOf(
            PermissionKey.BATTERY_OPTIMIZATION to VendorCompat.isIgnoringBatteryOptimizations(context),
            PermissionKey.CLOSE_POWER_SAVE to VendorCompat.isPowerSaveModeDisabled(context),
            PermissionKey.KEEP_NET_CONNECTION to VendorCompat.isWifiSleepPolicyAlways(context),
        )
    }

    // 教程勾选完成标记（DataStore），无系统检测的项以此为准
    val tutorialDoneStates = visiblePermissions.associateWith { key ->
        TutorialRepository.tutorialDoneFlow(context, brand, key)
            .collectAsState(initial = false)
    }

    fun isDone(key: PermissionKey): Boolean {
        systemDetected[key]?.let { return it }
        val autoSource = TutorialRepository.autoCompletedBy(brand, key)
        if (autoSource != null) {
            return tutorialDoneStates[autoSource]?.value == true
        }
        return tutorialDoneStates[key]?.value == true
    }

    fun handleItemClick(key: PermissionKey) {
        when (key) {
            // 一键跳转系统授权弹窗（ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS）
            PermissionKey.BATTERY_OPTIMIZATION -> {
                (context as? Activity)?.let { VendorCompat.openBatteryOptimizationSettings(it) }
            }
            // 省电模式有系统直达页
            PermissionKey.CLOSE_POWER_SAVE -> {
                (context as? Activity)?.let { VendorCompat.openPowerSaveModeSettings(it) }
            }
            else -> navController.navigate(Screen.PermissionTutorial.createRoute(key))
        }
    }

    val requiredItems = visiblePermissions
        .filter { it.group == PermissionGroup.REQUIRED }
        .map { PermissionItemState(it, isDone(it)) }
    val onDemandItems = visiblePermissions
        .filter { it.group == PermissionGroup.ON_DEMAND }
        .map {
            val autoSource = TutorialRepository.autoCompletedBy(brand, it)
            PermissionItemState(
                key = it,
                done = isDone(it),
                // 同入口项：源项完成后本项自动打勾置灰（华为自启动←后台策略；三星后台数据←联网控制）
                autoCompletedByTitle = if (autoSource != null && isDone(it)) autoSource.title else null,
                // 有系统检测项但读不到（如新版 HarmonyOS 缺 wifi_sleep_policy key）→ "未确认"中性展示
                undetectable = systemDetected.containsKey(it) && systemDetected[it] == null,
            )
        }

    val unfinishedRequired = requiredItems.count { !it.done }
    var onDemandExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("后台播放优化") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // StatusBanner：有未完成必要项时显示，全部完成 200ms 淡出
            AnimatedVisibility(
                visible = unfinishedRequired > 0,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                Column {
                    StatusBanner(
                        unfinishedCount = unfinishedRequired,
                        onClick = {
                            // 点击直达首个未完成必要项的操作
                            requiredItems.firstOrNull { !it.done }?.let { handleItemClick(it.key) }
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 必须完成组（常驻展开）
            PermissionGroupSection(
                group = PermissionGroup.REQUIRED,
                items = requiredItems,
                expanded = true,
                onToggleExpand = {},
                onItemClick = ::handleItemClick,
            )

            Spacer(Modifier.height(24.dp))

            // 遇到问题再设置组（默认折叠）
            if (onDemandItems.isNotEmpty()) {
                PermissionGroupSection(
                    group = PermissionGroup.ON_DEMAND,
                    items = onDemandItems,
                    expanded = onDemandExpanded,
                    onToggleExpand = { onDemandExpanded = !onDemandExpanded },
                    onItemClick = ::handleItemClick,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
