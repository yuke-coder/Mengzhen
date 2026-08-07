package com.mengzhen.app.ui.screens

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.mengzhen.app.R
import com.ximalaya.ting.android.main.playpage.playy.component.speed.SpeedScaleView

/** Direct dialog/window/layout port of Ximalaya 9.4.95.3 SpeedDialog. */
@Composable
internal fun XimalayaSourceSpeedSheet(
    currentSpeed: Float,
    applyToCurrentTask: Boolean,
    onSpeedSelected: (Float) -> Unit,
    onApplyScopeChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentSelect by rememberUpdatedState(onSpeedSelected)
    val currentScope by rememberUpdatedState(onApplyScopeChanged)
    val currentDismiss by rememberUpdatedState(onDismiss)

    DisposableEffect(context, currentSpeed, applyToCurrentTask) {
        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(true)
        }
        val root = LayoutInflater.from(context)
            .inflate(R.layout.main_layout_dialog_speed_new, null, false)
        dialog.setContentView(root)

        val scale = root.findViewById<SpeedScaleView>(R.id.main_speed_scale_view)
        scale.onSpeedSelected = currentSelect
        scale.setSpeed(currentSpeed, notify = false)

        root.findViewById<View>(R.id.main_close).setOnClickListener { currentDismiss() }
        root.findViewById<CheckBox>(R.id.main_cb_switch_speed).apply {
            isChecked = applyToCurrentTask
            setOnCheckedChangeListener { _, checked -> currentScope(checked) }
        }
        dialog.setOnCancelListener { currentDismiss() }
        dialog.show()
        dialog.window?.apply {
            decorView.setPadding(0, 0, 0, 0)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM
                dimAmount = 0.8f
            }
            setWindowAnimations(R.style.arg_res_0x7f1303c3)
        }
        onDispose {
            dialog.setOnCancelListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}
