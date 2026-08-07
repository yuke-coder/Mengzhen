package com.mengzhen.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.mengzhen.app.R

/**
 * 直接移植喜马拉雅 9.4.95.3 SkipHeadTailDialog（g.java）：
 * 原始 XML、PopupWindow、0.5 背景透明度、120 秒 SeekBar、取消/保存语义。
 *
 * 喜马拉雅按专辑保存；梦枕没有云端专辑概念，因此等价映射为当前听单/任务。
 */
@Composable
internal fun XimalayaSourceSkipHeadTailSheet(
    initialHeadSeconds: Int,
    initialTailSeconds: Int,
    onSave: (headSeconds: Int, tailSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findSkipDialogActivity() ?: return
    val currentSave by rememberUpdatedState(onSave)
    val currentDismiss by rememberUpdatedState(onDismiss)

    DisposableEffect(
        activity,
        initialHeadSeconds,
        initialTailSeconds,
    ) {
        val popup = createSkipHeadTailPopup(
            activity = activity,
            initialHeadSeconds = initialHeadSeconds,
            initialTailSeconds = initialTailSeconds,
            onSave = currentSave,
            onDismiss = currentDismiss,
        )
        popup.showAtLocation(activity.window.decorView, Gravity.BOTTOM, 0, 0)
        activity.setSkipDialogDim(0.5f)

        onDispose {
            popup.setOnDismissListener(null)
            if (popup.isShowing) popup.dismiss()
            activity.setSkipDialogDim(1f)
        }
    }
}

private fun createSkipHeadTailPopup(
    activity: Activity,
    initialHeadSeconds: Int,
    initialTailSeconds: Int,
    onSave: (headSeconds: Int, tailSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
): PopupWindow {
    val root = LayoutInflater.from(activity).inflate(
        R.layout.main_bottom_dialog_skip_head_tail,
        null,
        false,
    ) as RelativeLayout
    val popup = PopupWindow(
        root,
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        true,
    ).apply {
        isTouchable = true
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        animationStyle = R.style.arg_res_0x7f1303c3
    }

    var headSeconds = initialHeadSeconds.coerceIn(0, MAX_SKIP_SECONDS)
    var tailSeconds = initialTailSeconds.coerceIn(0, MAX_SKIP_SECONDS)
    val headSeek = root.findViewById<SeekBar>(R.id.main_seek_bar_head)
    val tailSeek = root.findViewById<SeekBar>(R.id.main_seek_bar_tail)
    val headValue = root.findViewById<TextView>(R.id.main_tv_skip_head_time)
    val tailValue = root.findViewById<TextView>(R.id.main_tv_skip_tail_time)

    root.findViewById<TextView>(R.id.main_tv_hint).text =
        "设置后对本听单内所有声音生效"
    fun bindValue(seekBar: SeekBar, valueView: TextView, value: Int, prefix: String) {
        seekBar.max = MAX_SKIP_SECONDS
        seekBar.progress = value
        valueView.text = "${value}s"
        val description = "$prefix${value}秒"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            seekBar.stateDescription = description
        } else {
            seekBar.contentDescription = description
        }
    }
    bindValue(headSeek, headValue, headSeconds, "跳过片头")
    bindValue(tailSeek, tailValue, tailSeconds, "跳过片尾")

    headSeek.setOnSeekBarChangeListener(
        sourceSkipListener(
            label = "跳过片头",
            valueView = headValue,
            onStopped = { headSeconds = it },
        ),
    )
    tailSeek.setOnSeekBarChangeListener(
        sourceSkipListener(
            label = "跳过片尾",
            valueView = tailValue,
            onStopped = { tailSeconds = it },
        ),
    )
    root.findViewById<View>(R.id.main_tv_cancel).setOnClickListener {
        popup.dismiss()
    }
    root.findViewById<View>(R.id.main_tv_save).setOnClickListener {
        headSeconds = headSeek.progress.coerceIn(0, MAX_SKIP_SECONDS)
        tailSeconds = tailSeek.progress.coerceIn(0, MAX_SKIP_SECONDS)
        onSave(headSeconds, tailSeconds)
        popup.dismiss()
    }
    popup.setOnDismissListener {
        activity.setSkipDialogDim(1f)
        onDismiss()
    }
    return popup
}

private fun sourceSkipListener(
    label: String,
    valueView: TextView,
    onStopped: (Int) -> Unit,
): SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        valueView.text = "${progress}s"
        val description = "$label${progress}秒"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            seekBar.stateDescription = description
        } else {
            seekBar.contentDescription = description
        }
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        onStopped(seekBar.progress.coerceIn(0, MAX_SKIP_SECONDS))
    }
}

private tailrec fun Context.findSkipDialogActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findSkipDialogActivity()
    else -> null
}

private fun Activity.setSkipDialogDim(alpha: Float) {
    window.attributes = window.attributes.apply {
        this.alpha = alpha
    }
}

private const val MAX_SKIP_SECONDS = 120
