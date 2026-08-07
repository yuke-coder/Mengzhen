package com.ximalaya.ting.android.framework.view.image

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import com.mengzhen.app.R

/** Runtime-compatible port of Ximalaya's rounded cover image host. */
class RoundImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private var radius = 0f

    init {
        if (attrs != null) {
            val values = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.corner_radius))
            radius = values.getDimension(0, 0f)
            values.recycle()
        }
        clipToOutline = radius > 0f
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
    }
}
