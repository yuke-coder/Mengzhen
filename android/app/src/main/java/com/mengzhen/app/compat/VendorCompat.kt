package com.mengzhen.app.compat

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri

/**
 * 厂商电池优化/后台保活适配 - 对标喜马拉雅 permissionSetting/device/ 目录
 *
 * 喜马拉雅为每个厂商写了独立的 Device 类，包含自启动/后台运行/电池优化/省电模式/网络控制等权限引导。
 * 这里提取核心逻辑：检测品牌 -> 跳转对应厂商系统设置页。
 *
 * 参考来源：
 * - HuaWeiDevice.java: 自启动/后台运行/电池优化/省电模式/网络保持/流量控制
 * - XiaoMiDevice.java: 自启动/后台运行/电池优化/省电模式/后台数据/网络控制
 * - OPPODevice.java: 自启动/后台运行/电池优化/省电模式/后台数据/网络控制/自动省流量
 * - VIVODevice.java: 自启动/后台运行/电池优化/省电模式/网络控制/自动省流量
 * - SamsungDevice.java: 自启动/后台运行/电池优化/省电模式
 * - HonorDevice.java: 同华为
 */
object VendorCompat {

    private const val TAG = "VendorCompat"

    enum class Brand {
        HUAWEI, HONOR, XIAOMI, OPPO, VIVO, SAMSUNG, OTHER
    }

    fun detectBrand(): Brand {
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase()
        val brand = (Build.BRAND ?: "").lowercase()
        return when {
            manufacturer.contains("huawei") || brand.contains("honor") && !manufacturer.contains("honor") -> Brand.HUAWEI
            manufacturer.contains("honor") || brand.contains("honor") -> Brand.HONOR
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi") -> Brand.XIAOMI
            manufacturer.contains("oppo") || brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus") -> Brand.OPPO
            manufacturer.contains("vivo") || brand.contains("vivo") || brand.contains("iqoo") -> Brand.VIVO
            manufacturer.contains("samsung") || brand.contains("samsung") -> Brand.SAMSUNG
            else -> Brand.OTHER
        }
    }

    /**
     * 引导用户到对应厂商的自启动管理页面
     * 对标喜马拉雅 BaseAutoStartPermission -> toSystemSettingPage()
     */
    fun openAutoStartSettings(activity: Activity): Boolean {
        val brand = detectBrand()
        val components = when (brand) {
            Brand.HUAWEI, Brand.HONOR -> listOf(
                ComponentName("com.huawei.systemmanager", ".startupmgr.ui.StartupNormalAppListActivity"),
                ComponentName("com.huawei.systemmanager", ".optimize.bootstart.BootStartActivity"),
                ComponentName("com.huawei.systemmanager", ".startupmgr.ui.StartupAwakedAppListActivity"),
                ComponentName("com.huawei.systemmanager", ".appcontrol.activity.StartupAppControlActivity"),
                ComponentName("com.hihonor.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                ComponentName("com.hihonor.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"),
            )
            Brand.XIAOMI -> listOf(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            )
            Brand.OPPO -> listOf(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            )
            Brand.VIVO -> listOf(
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity"),
            )
            Brand.SAMSUNG -> listOf(
                ComponentName("com.samsung.android.lool", "com.samsung.android.lool.activity.applist.AppListActivity"),
            )
            Brand.OTHER -> return openAppDetailSettings(activity)
        }
        return tryStartActivity(activity, components) || openAppDetailSettings(activity)
    }

    /**
     * 引导用户到对应厂商的后台运行管理页面
     * 对标喜马拉雅 BaseBackgroundRunningPermission -> toSystemSettingPage()
     */
    fun openBackgroundRunningSettings(activity: Activity): Boolean {
        val brand = detectBrand()
        val components = when (brand) {
            Brand.HUAWEI, Brand.HONOR -> listOf(
                ComponentName("com.huawei.systemmanager", ".power.ui.HwPowerManagerActivity"),
                ComponentName("com.huawei.systemmanager", ".optimize.process.ProtectActivity"),
                ComponentName("com.hihonor.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"),
                ComponentName("com.hihonor.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            )
            Brand.XIAOMI -> listOf(
                ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity")
            )
            Brand.OPPO -> listOf(
                ComponentName("com.coloros.oppoguardelf", "com.coloros.oppoguardelf.guide.OppoGuideActivity"),
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            )
            Brand.VIVO -> listOf(
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            )
            Brand.SAMSUNG -> listOf(
                ComponentName("com.samsung.android.lool", "com.samsung.android.lool.activity.applist.AppListActivity"),
            )
            Brand.OTHER -> return openAppDetailSettings(activity)
        }
        return tryStartActivity(activity, components) || openAppDetailSettings(activity)
    }

    /**
     * 引导用户到电池优化白名单页面
     * 对标喜马拉雅 BaseBatteryOptimizationPermission
     */
    fun openBatteryOptimizationSettings(activity: Activity): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData("package:${activity.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            true
        } catch (e: Exception) {
            // 降级到电池优化列表页
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
                true
            } catch (e2: Exception) {
                Log.w(TAG, "Failed to open battery optimization settings", e2)
                false
            }
        }
    }

    /**
     * 引导用户到省电模式设置页面
     * 对标喜马拉雅 BaseClosePowerSaveModePermission
     */
    fun openPowerSaveModeSettings(activity: Activity): Boolean {
        val brand = detectBrand()
        // 小米特殊处理 - 检查 POWER_SAVE_MODE_OPEN
        if (brand == Brand.XIAOMI) {
            return try {
                val intent = Intent("miui.powercenter.POWER_SAVE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (activity.packageManager.resolveActivity(intent, 0) != null) {
                    activity.startActivity(intent)
                    true
                } else false
            } catch (e: Exception) { false }
        }
        // 华为特殊处理 - 检查 SmartModeStatus
        if (brand == Brand.HUAWEI || brand == Brand.HONOR) {
            return tryStartActivity(activity, listOf(
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"),
                ComponentName("com.hihonor.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"),
            )) || openAppDetailSettings(activity)
        }
        // VIVO - 跳 iqoo.secure
        if (brand == Brand.VIVO) {
            return tryStartActivity(activity, listOf(
                ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgListActivity"),
            )) || openAppDetailSettings(activity)
        }
        // OPPO / 三星 / 其他 - 通用省电设置
        return try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            true
        } catch (e: Exception) {
            openAppDetailSettings(activity)
        }
    }

    /**
     * 引导用户到网络控制/后台数据管理页面
     * 对标喜马拉雅 BaseNetworkControlPermission / BaseBackgroundDataPermission
     */
    fun openNetworkControlSettings(activity: Activity): Boolean {
        val brand = detectBrand()
        val components = when (brand) {
            Brand.XIAOMI -> listOf(
                ComponentName("com.miui.securitycenter", "com.miui.networkaccess.ui.NetworkAccessActivity"),
            )
            Brand.HUAWEI, Brand.HONOR -> listOf(
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.netnotice.ui.NetNoticeActivity"),
                ComponentName("com.hihonor.systemmanager", "com.huawei.systemmanager.netnotice.ui.NetNoticeActivity"),
            )
            Brand.OPPO -> listOf(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.net.auto.AppNetworkControllerActivity"),
            )
            Brand.VIVO -> listOf(
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.NetworkAutoStartManagerActivity"),
            )
            else -> return openAppDetailSettings(activity)
        }
        return tryStartActivity(activity, components) || openAppDetailSettings(activity)
    }

    /**
     * 检查电池优化白名单状态
     * 对标喜马拉雅 BaseBatteryOptimizationPermission.getPermissionStatus()
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 检查"休眠状态保持网络连接"状态（仅华为/荣耀）
     * 对标喜马拉雅 HuaWeiDevice 休眠保持网络检查（wifi_sleep_policy）
     *
     * 三态返回（仅参考、失败不拦截、不误报——内容文档 §一脚注）：
     * - true  = 已开启（WIFI_SLEEP_POLICY_ALWAYS）
     * - false = 未开启
     * - null  = 读取失败（HarmonyOS 新版本可能缺该 key）→ 列表页按"未确认"中性展示
     */
    fun isWifiSleepPolicyAlways(context: Context): Boolean? {
        return try {
            // 值 2 = 旧常量 WIFI_SLEEP_POLICY_NEVER（API 29 起 deprecated，无替代 API；
            // 但华为/荣耀存量设备的「休眠时始终保持网络连接」仍写这个 key，检测目标本身就是历史 key）
            Settings.Global.getInt(context.contentResolver, "wifi_sleep_policy") == 2
        } catch (e: Settings.SettingNotFoundException) {
            Log.d(TAG, "wifi_sleep_policy key not found on this device")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read wifi_sleep_policy", e)
            null
        }
    }

    /**
     * 检查省电模式状态
     * 对标喜马拉雅 BaseClosePowerSaveModePermission.getPermissionStatus()
     * - 小米: Settings.System POWER_SAVE_MODE_OPEN != 1
     * - 华为: Settings.System SmartModeStatus != 4
     */
    fun isPowerSaveModeDisabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (!pm.isPowerSaveMode) return true

        // 厂商特定检查
        when (detectBrand()) {
            Brand.XIAOMI -> {
                try {
                    return Settings.System.getInt(context.contentResolver, "POWER_SAVE_MODE_OPEN") != 1
                } catch (e: Settings.SettingNotFoundException) { }
            }
            Brand.HUAWEI, Brand.HONOR -> {
                try {
                    return Settings.System.getInt(context.contentResolver, "SmartModeStatus") != 4
                } catch (e: Settings.SettingNotFoundException) { }
            }
            else -> {}
        }
        return !pm.isPowerSaveMode
    }

    /**
     * 尝试启动 ComponentName 列表中的任意一个
     * 对标喜马拉雅 XiaoMiDevice.toSettingPage() / VIVODevice.toSettingPage()
     */
    private fun tryStartActivity(activity: Activity, components: List<ComponentName>): Boolean {
        for (component in components) {
            try {
                val intent = Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (activity.packageManager.resolveActivity(intent, 0) != null) {
                    activity.startActivity(intent)
                    Log.i(TAG, "Started: ${component.className}")
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to start ${component.className}: ${e.message}")
            }
        }
        return false
    }

    /**
     * 降级方案 - 打开应用详情页
     * 对标喜马拉雅 DefaultDevice.toAppDetailPage()
     */
    fun openAppDetailSettings(activity: Activity): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData("package:${activity.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open app detail settings", e)
            false
        }
    }

    /**
     * 一次性检查所有保活相关权限，返回需要引导的权限列表
     */
    fun checkAllPermissions(context: Context): List<PermissionItem> {
        val items = mutableListOf<PermissionItem>()

        if (!isIgnoringBatteryOptimizations(context)) {
            items.add(PermissionItem("电池优化", "允许后台运行，防止系统杀掉播放服务", ::openBatteryOptimizationSettings))
        }

        if (!isPowerSaveModeDisabled(context)) {
            items.add(PermissionItem("省电模式", "关闭省电模式，确保息屏播放不被限制", ::openPowerSaveModeSettings))
        }

        // 自启动和后台运行无法通过 API 检测状态，只能引导用户去设置
        val brand = detectBrand()
        if (brand != Brand.OTHER) {
            items.add(PermissionItem("自启动", "允许梦枕开机自启动，确保定时播放生效") { activity ->
                openAutoStartSettings(activity)
            })
            items.add(PermissionItem("后台运行", "允许梦枕后台运行，确保息屏播放持续") { activity ->
                openBackgroundRunningSettings(activity)
            })
        }

        return items
    }

    data class PermissionItem(
        val name: String,
        val description: String,
        val openSettings: (Activity) -> Boolean,
    )
}
