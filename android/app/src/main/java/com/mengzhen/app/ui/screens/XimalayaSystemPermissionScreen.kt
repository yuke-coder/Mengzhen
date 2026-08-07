package com.mengzhen.app.ui.screens

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mengzhen.app.R
import com.mengzhen.app.compat.VendorCompat

/**
 * SystemPermissionSettingFragment + SystemPermissionSettingAdapter 的源码移植。
 *
 * 原页面列出相机、录音、存储和定位；梦枕按同一交互契约映射为实际使用的通知、
 * 电话状态、精确闹钟和悬浮窗权限，避免出现无功能对应的空权限入口。
 */
@Composable
fun XimalayaSystemPermissionScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var pendingRuntimePermission by remember { mutableStateOf<String?>(null) }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val permission = pendingRuntimePermission
        pendingRuntimePermission = null
        refreshTrigger++
        if (!granted && permission != null && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        ) {
            VendorCompat.openAppDetailSettings(activity)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    SourceSystemPermission(
                        kind = SystemPermissionKind.NOTIFICATIONS,
                        title = "允许发送通知权限",
                        subtitle = "为了展示播放控制和定时启播提醒，需要您授权",
                    ),
                )
            }
            add(
                SourceSystemPermission(
                    kind = SystemPermissionKind.PHONE_STATE,
                    title = "允许访问电话状态权限",
                    subtitle = "为了在来电时暂停播放，需要您授权",
                ),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    SourceSystemPermission(
                        kind = SystemPermissionKind.EXACT_ALARM,
                        title = "允许设置闹钟和提醒",
                        subtitle = "为了让定时启播按时触发，需要您授权",
                    ),
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(
                    SourceSystemPermission(
                        kind = SystemPermissionKind.OVERLAY,
                        title = "允许显示在其他应用上层",
                        subtitle = "为了在定时启播时正常显示闹钟页面，需要您授权",
                    ),
                )
            }
        }
    }

    fun openPermission(item: SourceSystemPermission) {
        if (activity == null) return
        if (systemPermissionAllowed(context, item.kind)) {
            VendorCompat.openAppDetailSettings(activity)
            return
        }
        when (item.kind) {
            SystemPermissionKind.NOTIFICATIONS -> {
                pendingRuntimePermission = Manifest.permission.POST_NOTIFICATIONS
                runtimePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            SystemPermissionKind.PHONE_STATE -> {
                pendingRuntimePermission = Manifest.permission.READ_PHONE_STATE
                runtimePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }
            SystemPermissionKind.EXACT_ALARM -> {
                try {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${activity.packageName}"),
                        ),
                    )
                } catch (_: Exception) {
                    VendorCompat.openAppDetailSettings(activity)
                }
            }
            SystemPermissionKind.OVERLAY -> {
                try {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${activity.packageName}"),
                        ),
                    )
                } catch (_: Exception) {
                    VendorCompat.openAppDetailSettings(activity)
                }
            }
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_system_permission_setting, null, false)
                .also { root ->
                    installXimalayaTitleBar(
                        host = root.findViewById(R.id.main_title_bar),
                        title = "系统权限设置",
                        left = XimalayaTitleAction.Back { navController.popBackStack() },
                    )
                    root.findViewById<RecyclerView>(R.id.main_rv_system_permission).apply {
                        layoutManager = LinearLayoutManager(viewContext)
                        adapter = SourceSystemPermissionAdapter(
                            onClick = ::openPermission,
                        )
                    }
                }
        },
        update = { root ->
            refreshTrigger
            val rows = permissions.map {
                it.copy(allowed = systemPermissionAllowed(context, it.kind))
            }
            (root.findViewById<RecyclerView>(R.id.main_rv_system_permission).adapter
                as? SourceSystemPermissionAdapter)?.submit(rows)
        },
    )
}

private enum class SystemPermissionKind {
    NOTIFICATIONS,
    PHONE_STATE,
    EXACT_ALARM,
    OVERLAY,
}

private data class SourceSystemPermission(
    val kind: SystemPermissionKind,
    val title: String,
    val subtitle: String,
    val allowed: Boolean = false,
)

private fun systemPermissionAllowed(
    context: Context,
    kind: SystemPermissionKind,
): Boolean = when (kind) {
    SystemPermissionKind.NOTIFICATIONS ->
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    SystemPermissionKind.PHONE_STATE ->
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    SystemPermissionKind.EXACT_ALARM ->
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                ?.canScheduleExactAlarms() == true
    SystemPermissionKind.OVERLAY ->
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(context)
}

private class SourceSystemPermissionAdapter(
    private val onClick: (SourceSystemPermission) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val rows = mutableListOf<SourceSystemPermission>()

    fun submit(newRows: List<SourceSystemPermission>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size + 1

    override fun getItemViewType(position: Int): Int =
        if (position == 0) TYPE_HEADER else TYPE_PERMISSION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(
                inflater.inflate(
                    R.layout.main_layout_system_permission_setting_header,
                    parent,
                    false,
                ),
            )
        } else {
            PermissionHolder(
                inflater.inflate(
                    R.layout.main_layout_system_permission_item,
                    parent,
                    false,
                ),
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is PermissionHolder || position == 0) return
        val item = rows[position - 1]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.setting.text = if (item.allowed) "已允许" else "权限设置"
        holder.container.setOnClickListener { onClick(item) }
    }

    private class HeaderHolder(view: View) : RecyclerView.ViewHolder(view)

    private class PermissionHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.main_cl_permission_item)
        val title: TextView = view.findViewById(R.id.main_tv_title)
        val subtitle: TextView = view.findViewById(R.id.main_tv_subtitle)
        val setting: TextView = view.findViewById(R.id.main_tv_setting)
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_PERMISSION = 1
    }
}
