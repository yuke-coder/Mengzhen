package com.ximalaya.ting.android.framework.view.image

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mengzhen.app.R

/** Runtime-compatible port of Ximalaya's selectively rounded image host. */
class FlexibleRoundImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val clipPath = Path()
    private val clipBounds = RectF()
    private var cornerRadius = 0f
    private var corners = ALL_CORNERS

    init {
        if (attrs != null) {
            val values = context.obtainStyledAttributes(
                attrs,
                R.styleable.FlexibleRoundImageView,
                defStyleAttr,
                0,
            )
            cornerRadius = values.getDimension(
                R.styleable.FlexibleRoundImageView_flexible_corner_radius,
                0f,
            )
            corners = values.getInt(
                R.styleable.FlexibleRoundImageView_flexible_corners,
                ALL_CORNERS,
            )
            values.recycle()
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        rebuildClipPath(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (cornerRadius <= 0f) {
            super.onDraw(canvas)
            return
        }
        val checkpoint = canvas.save()
        canvas.clipPath(clipPath)
        super.onDraw(canvas)
        canvas.restoreToCount(checkpoint)
    }

    private fun rebuildClipPath(width: Int, height: Int) {
        clipPath.reset()
        if (width <= 0 || height <= 0 || cornerRadius <= 0f) return
        clipBounds.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.addRoundRect(
            clipBounds,
            cornerRadii(cornerRadius, corners),
            Path.Direction.CW,
        )
    }

    private fun cornerRadii(radius: Float, mask: Int): FloatArray = floatArrayOf(
        if (mask and LEFT_TOP != 0) radius else 0f,
        if (mask and LEFT_TOP != 0) radius else 0f,
        if (mask and RIGHT_TOP != 0) radius else 0f,
        if (mask and RIGHT_TOP != 0) radius else 0f,
        if (mask and RIGHT_BOTTOM != 0) radius else 0f,
        if (mask and RIGHT_BOTTOM != 0) radius else 0f,
        if (mask and LEFT_BOTTOM != 0) radius else 0f,
        if (mask and LEFT_BOTTOM != 0) radius else 0f,
    )

    private companion object {
        const val LEFT_TOP = 1
        const val RIGHT_TOP = 2
        const val LEFT_BOTTOM = 4
        const val RIGHT_BOTTOM = 8
        const val ALL_CORNERS = LEFT_TOP or RIGHT_TOP or LEFT_BOTTOM or RIGHT_BOTTOM
    }
}
