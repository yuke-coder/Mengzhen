package com.mengzhen.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.ConnectivityManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.compat.VendorCompat
import com.mengzhen.app.data.tutorial.PermissionKey
import com.mengzhen.app.ui.navigation.Screen

/**
 * 喜马拉雅 9.4.95.3 ListenPermissionFragment 的宿主移植。
 *
 * 页面、列表项、文案、状态色、按钮状态和厂商顺序均来自原客户端源码；
 * 系统目标包名替换为梦枕自身，保证每个入口实际可执行。
 */
@Composable
fun PermissionSettingsScreen(
    navController: NavController,
    fromAlarm: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val brand = remember { VendorCompat.detectBrand() }
    val permissionItems = remember(brand, fromAlarm) {
        buildSourcePermissionItems(brand, fromAlarm)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTrigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openSourceTutorial(item: SourcePermissionItem) {
        item.permissionKey?.let {
            navController.navigate(Screen.PermissionTutorial.createRoute(it))
        }
    }

    fun openPermission(item: SourcePermissionItem) {
        if (!item.hasAction || activity == null) return

        if (item.id == SourcePermissionId.CLOSE_POWER_SAVE &&
            VendorCompat.isPowerSaveModeDisabled(context)
        ) {
            Toast.makeText(context, "省电模式已关闭", Toast.LENGTH_SHORT).show()
            return
        }
        if (item.id == SourcePermissionId.BATTERY_OPTIMIZATION &&
            VendorCompat.isIgnoringBatteryOptimizations(context)
        ) {
            Toast.makeText(context, "电池优化已关闭", Toast.LENGTH_SHORT).show()
            return
        }

        // 源码中除 VIVO 自启动外，带 H5 教程的厂商项不尝试直达系统页。
        if (item.showsTutorial &&
            !(brand == VendorCompat.Brand.VIVO &&
                item.id == SourcePermissionId.AUTO_START)
        ) {
            openSourceTutorial(item)
            return
        }

        val opened = when (item.id) {
            SourcePermissionId.CLOSE_POWER_SAVE ->
                VendorCompat.openPowerSaveModeSettings(activity)
            SourcePermissionId.BATTERY_OPTIMIZATION ->
                VendorCompat.openBatteryOptimizationSettings(activity)
            SourcePermissionId.AUTO_START ->
                VendorCompat.openAutoStartSettings(activity)
            SourcePermissionId.BACKGROUND_RUNNING ->
                VendorCompat.openBackgroundRunningSettings(activity)
            SourcePermissionId.BACKGROUND_DATA,
            SourcePermissionId.NETWORK_CONTROL ->
                VendorCompat.openNetworkControlSettings(activity)
            SourcePermissionId.SHOW_OTHER_APP_TOP ->
                openOverlayPermission(activity)
            SourcePermissionId.KEEP_NET_CONNECTION,
            SourcePermissionId.SMART_DATA_SAVER,
            SourcePermissionId.KEEP_APP_FOREGROUND -> false
        }
        if (!opened) {
            openSourceTutorial(item)
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_listen_permission, null, false)
                .also { root ->
                    installXimalayaTitleBar(
                        host = root.findViewById(R.id.main_title_bar),
                        title = "后台播放优化",
                        left = XimalayaTitleAction.Back { navController.popBackStack() },
                    )
                }
        },
        update = { root ->
            resumeTrigger
            val container = root.findViewById<LinearLayout>(R.id.main_v_container)
            container.removeAllViews()
            permissionItems.forEach { item ->
                container.addView(
                    createSourcePermissionRow(
                        context = context,
                        parent = container,
                        item = item,
                        done = sourcePermissionDone(context, item.id),
                        onClick = { openPermission(item) },
                    ),
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
        },
    )
}

private fun createSourcePermissionRow(
    context: Context,
    parent: ViewGroup,
    item: SourcePermissionItem,
    done: Boolean,
    onClick: () -> Unit,
): View {
    val row = LayoutInflater.from(context)
        .inflate(R.layout.main_item_listen_permission, parent, false)
    row.findViewById<TextView>(R.id.main_tv_title).text = item.copy.title
    row.findViewById<TextView>(R.id.main_tv_sub_title).text = item.copy.subtitle
    row.findViewById<TextView>(R.id.main_tv_quick_setup).apply {
        when {
            item.canAutoCheck && done -> {
                visibility = View.VISIBLE
                text = item.copy.openedTitle
                background = null
                setTextColor(ContextCompat.getColor(context, R.color.arg_res_0x7f060af1))
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.arg_res_0x7f082a49,
                    0,
                    0,
                    0,
                )
            }
            item.canAutoCheck -> {
                visibility = View.VISIBLE
                text = if (item.showsTutorial) "查看教程" else "快速设置"
                setTextColor(ContextCompat.getColor(context, R.color.arg_res_0x7f060dfb))
                setBackgroundResource(R.drawable.arg_res_0x7f082b74)
                setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }
            item.hasAction -> {
                visibility = View.VISIBLE
                text = if (item.showsTutorial) "查看教程" else "快速设置"
                setTextColor(ContextCompat.getColor(context, R.color.arg_res_0x7f060ac3))
                setBackgroundResource(R.drawable.arg_res_0x7f082b75)
                setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }
            else -> visibility = View.INVISIBLE
        }
        setOnClickListener { onClick() }
    }
    return row
}

private enum class SourcePermissionId {
    KEEP_APP_FOREGROUND,
    SHOW_OTHER_APP_TOP,
    KEEP_NET_CONNECTION,
    CLOSE_POWER_SAVE,
    BATTERY_OPTIMIZATION,
    AUTO_START,
    BACKGROUND_RUNNING,
    BACKGROUND_DATA,
    NETWORK_CONTROL,
    SMART_DATA_SAVER,
}

private data class SourcePermissionItem(
    val id: SourcePermissionId,
    val copy: PermissionCopy,
    val canAutoCheck: Boolean,
    val hasAction: Boolean,
    val showsTutorial: Boolean,
    val permissionKey: PermissionKey?,
)

private data class PermissionCopy(
    val title: String,
    val subtitle: String,
    val openedTitle: String,
)

private fun buildSourcePermissionItems(
    brand: VendorCompat.Brand,
    fromAlarm: Boolean,
): List<SourcePermissionItem> {
    val orderedIds = when (brand) {
        VendorCompat.Brand.HUAWEI,
        VendorCompat.Brand.HONOR -> listOf(
            SourcePermissionId.KEEP_NET_CONNECTION,
            SourcePermissionId.CLOSE_POWER_SAVE,
            SourcePermissionId.BATTERY_OPTIMIZATION,
            SourcePermissionId.AUTO_START,
            SourcePermissionId.BACKGROUND_RUNNING,
            SourcePermissionId.NETWORK_CONTROL,
            SourcePermissionId.SMART_DATA_SAVER,
        )
        VendorCompat.Brand.XIAOMI -> listOf(
            SourcePermissionId.CLOSE_POWER_SAVE,
            SourcePermissionId.BATTERY_OPTIMIZATION,
            SourcePermissionId.AUTO_START,
            SourcePermissionId.BACKGROUND_RUNNING,
            SourcePermissionId.BACKGROUND_DATA,
            SourcePermissionId.NETWORK_CONTROL,
        )
        VendorCompat.Brand.OPPO -> listOf(
            SourcePermissionId.CLOSE_POWER_SAVE,
            SourcePermissionId.BATTERY_OPTIMIZATION,
            SourcePermissionId.AUTO_START,
            SourcePermissionId.BACKGROUND_RUNNING,
            SourcePermissionId.BACKGROUND_DATA,
            SourcePermissionId.NETWORK_CONTROL,
            SourcePermissionId.SMART_DATA_SAVER,
        )
        VendorCompat.Brand.VIVO -> listOf(
            SourcePermissionId.CLOSE_POWER_SAVE,
            SourcePermissionId.BATTERY_OPTIMIZATION,
            SourcePermissionId.AUTO_START,
            SourcePermissionId.BACKGROUND_RUNNING,
            SourcePermissionId.NETWORK_CONTROL,
            SourcePermissionId.SMART_DATA_SAVER,
        )
        VendorCompat.Brand.SAMSUNG -> listOf(
            SourcePermissionId.CLOSE_POWER_SAVE,
            SourcePermissionId.BATTERY_OPTIMIZATION,
            SourcePermissionId.AUTO_START,
            SourcePermissionId.BACKGROUND_RUNNING,
            SourcePermissionId.NETWORK_CONTROL,
        )
        VendorCompat.Brand.OTHER -> listOf(
            SourcePermissionId.CLOSE_POWER_SAVE,
            SourcePermissionId.BATTERY_OPTIMIZATION,
            SourcePermissionId.AUTO_START,
            SourcePermissionId.BACKGROUND_RUNNING,
        )
    }
    return buildList {
        if (fromAlarm) {
            add(sourcePermissionItem(brand, SourcePermissionId.KEEP_APP_FOREGROUND))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(sourcePermissionItem(brand, SourcePermissionId.SHOW_OTHER_APP_TOP))
            }
        }
        orderedIds.forEach { add(sourcePermissionItem(brand, it)) }
    }
}

private fun sourcePermissionItem(
    brand: VendorCompat.Brand,
    id: SourcePermissionId,
): SourcePermissionItem = SourcePermissionItem(
    id = id,
    copy = permissionCopy(id),
    canAutoCheck = id in setOf(
        SourcePermissionId.SHOW_OTHER_APP_TOP,
        SourcePermissionId.KEEP_NET_CONNECTION,
        SourcePermissionId.CLOSE_POWER_SAVE,
        SourcePermissionId.BATTERY_OPTIMIZATION,
        SourcePermissionId.BACKGROUND_DATA,
    ),
    hasAction = id != SourcePermissionId.KEEP_APP_FOREGROUND,
    showsTutorial = sourceUsesTutorial(brand, id),
    permissionKey = sourcePermissionKey(id),
)

private fun permissionCopy(id: SourcePermissionId): PermissionCopy = when (id) {
    SourcePermissionId.KEEP_APP_FOREGROUND -> PermissionCopy(
        "保持应用在前台",
        "保持应用打开在页面上，可以提高定时开启成功概率",
        "已开启",
    )
    SourcePermissionId.SHOW_OTHER_APP_TOP -> PermissionCopy(
        "开启悬浮窗权限",
        "开启悬浮窗权限，保证闹钟页面正常展示",
        "已开启",
    )
    SourcePermissionId.CLOSE_POWER_SAVE -> PermissionCopy(
        "关闭省电模式",
        "省电模式可能导致app在后台被冻结无法获取数据甚至停止运行。相关设置通常在'设置-电池-省电模式'",
        "已关闭",
    )
    SourcePermissionId.BATTERY_OPTIMIZATION -> PermissionCopy(
        "忽略电池优化",
        "在省电模式下，应用的运行仍然可能受到限制。为了更好的保证收听体验，请将梦枕加入电池优化白名单",
        "已忽略",
    )
    SourcePermissionId.AUTO_START -> PermissionCopy(
        "自启动设置",
        "将梦枕加入后台保护名单，可以在一定程度上帮助梦枕在后台持续运行",
        "",
    )
    SourcePermissionId.BACKGROUND_RUNNING -> PermissionCopy(
        "后台运行策略",
        "由于系统会对后台运行的应用自动采取一些限制措施，为保证后台收听不受影响，请按照指引配置后台运行策略",
        "已允许",
    )
    SourcePermissionId.BACKGROUND_DATA -> PermissionCopy(
        "后台获取数据",
        "若应用无法在后台拉取新内容，请检查此项并开启",
        "已允许",
    )
    SourcePermissionId.NETWORK_CONTROL -> PermissionCopy(
        "联网控制（是否允许4G/wifi联网）",
        "若梦枕不能访问网络，请检查是否已经打开4G/Wifi联网开关",
        "已开启",
    )
    SourcePermissionId.KEEP_NET_CONNECTION -> PermissionCopy(
        "休眠状态保持网络连接",
        "为避免系统休眠导致应用无法获取数据，请开启此项",
        "已允许",
    )
    SourcePermissionId.SMART_DATA_SAVER -> PermissionCopy(
        "智能省流量",
        "开启智能省流量后，系统将阻止梦枕在后台使用网络数据，并降低其在前台使用网络的频率",
        "",
    )
}

private fun sourcePermissionKey(id: SourcePermissionId): PermissionKey? = when (id) {
    SourcePermissionId.KEEP_NET_CONNECTION -> PermissionKey.KEEP_NET_CONNECTION
    SourcePermissionId.CLOSE_POWER_SAVE -> PermissionKey.CLOSE_POWER_SAVE
    SourcePermissionId.BATTERY_OPTIMIZATION -> PermissionKey.BATTERY_OPTIMIZATION
    SourcePermissionId.AUTO_START -> PermissionKey.AUTO_START
    SourcePermissionId.BACKGROUND_RUNNING -> PermissionKey.BACKGROUND_RUNNING
    SourcePermissionId.BACKGROUND_DATA -> PermissionKey.BACKGROUND_DATA
    SourcePermissionId.NETWORK_CONTROL -> PermissionKey.NETWORK_CONTROL
    SourcePermissionId.SMART_DATA_SAVER -> PermissionKey.SMART_DATA_SAVER
    SourcePermissionId.KEEP_APP_FOREGROUND,
    SourcePermissionId.SHOW_OTHER_APP_TOP -> null
}

private fun sourceUsesTutorial(
    brand: VendorCompat.Brand,
    id: SourcePermissionId,
): Boolean = when (brand) {
    VendorCompat.Brand.HUAWEI,
    VendorCompat.Brand.HONOR -> id in setOf(
        SourcePermissionId.KEEP_NET_CONNECTION,
        SourcePermissionId.AUTO_START,
        SourcePermissionId.BACKGROUND_RUNNING,
        SourcePermissionId.NETWORK_CONTROL,
        SourcePermissionId.SMART_DATA_SAVER,
    )
    VendorCompat.Brand.OPPO -> id in setOf(
        SourcePermissionId.AUTO_START,
        SourcePermissionId.BACKGROUND_RUNNING,
        SourcePermissionId.BACKGROUND_DATA,
        SourcePermissionId.NETWORK_CONTROL,
        SourcePermissionId.SMART_DATA_SAVER,
    )
    VendorCompat.Brand.VIVO -> id in setOf(
        SourcePermissionId.AUTO_START,
        SourcePermissionId.BACKGROUND_RUNNING,
        SourcePermissionId.NETWORK_CONTROL,
        SourcePermissionId.SMART_DATA_SAVER,
    )
    VendorCompat.Brand.SAMSUNG -> id in setOf(
        SourcePermissionId.AUTO_START,
        SourcePermissionId.BACKGROUND_RUNNING,
        SourcePermissionId.NETWORK_CONTROL,
    )
    VendorCompat.Brand.XIAOMI,
    VendorCompat.Brand.OTHER -> false
}

private fun sourcePermissionDone(
    context: Context,
    id: SourcePermissionId,
): Boolean = when (id) {
    SourcePermissionId.SHOW_OTHER_APP_TOP ->
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    SourcePermissionId.CLOSE_POWER_SAVE ->
        VendorCompat.isPowerSaveModeDisabled(context)
    SourcePermissionId.BATTERY_OPTIMIZATION ->
        VendorCompat.isIgnoringBatteryOptimizations(context)
    SourcePermissionId.BACKGROUND_DATA -> {
        val connectivity =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivity?.let {
            ConnectivityManagerCompat.getRestrictBackgroundStatus(it) ==
                ConnectivityManagerCompat.RESTRICT_BACKGROUND_STATUS_DISABLED
        } == true
    }
    SourcePermissionId.KEEP_NET_CONNECTION ->
        VendorCompat.isWifiSleepPolicyAlways(context) == true
    SourcePermissionId.KEEP_APP_FOREGROUND,
    SourcePermissionId.AUTO_START,
    SourcePermissionId.BACKGROUND_RUNNING,
    SourcePermissionId.NETWORK_CONTROL,
    SourcePermissionId.SMART_DATA_SAVER -> false
}

private fun openOverlayPermission(activity: Activity): Boolean = try {
    activity.startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}"),
        ),
    )
    true
} catch (_: Exception) {
    false
}
