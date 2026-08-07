package com.ximalaya.ting.android.main.playpage.playy.component.functionv2.view

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.mengzhen.app.R

/**
 * Runtime-compatible port of Ximalaya 9.4.95.3 SpeedIconView.
 *
 * The original layout, dimensions, alpha and Ximalaya number font are retained.
 */
class SpeedIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    val tvSpeed: TextView

    init {
        LayoutInflater.from(context).inflate(
            R.layout.main_layout_function_speed_icon_linear,
            this,
            true,
        )
        tvSpeed = findViewById(R.id.main_tv_speed)
        val unit = findViewById<TextView>(R.id.main_tv_speed_unit)
        runCatching {
            Typeface.createFromAsset(
                context.assets,
                "fonts/XmlyNumberV1.0-SemiBold.otf",
            )
        }.getOrNull()?.let { typeface ->
            tvSpeed.typeface = typeface
            unit.typeface = typeface
        }
    }
}
