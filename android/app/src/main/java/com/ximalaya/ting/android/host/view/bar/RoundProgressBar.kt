package com.ximalaya.ting.android.host.view.bar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Runtime port of Ximalaya 9.5.1.4's host RoundProgressBar.
 *
 * The original class reads host-only styleable attributes. The main play bar uses
 * the same public setters after inflation, so the drawing and state contract stay
 * intact without importing the rest of the host resource table.
 */
class RoundProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val arcBounds = RectF()
    private val paint = Paint()
    private var circleColor = Color.TRANSPARENT
    private var progressColor = Color.RED
    private var textColor = Color.GREEN
    private var progressTextSize = 15f
    private var roundWidth = 5f
    private var max = 100
    private var progress = 0
    private var textDisplayable = false
    private var style = 0
    private var strokeCap = Paint.Cap.ROUND
    private var specialStyle = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val center = width / 2
        val radius = if (specialStyle == 1) {
            (center - roundWidth / 2f).toInt()
        } else {
            (center - roundWidth / 2f).toInt() - 2
        }

        paint.color = circleColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (specialStyle == 1) roundWidth else roundWidth - 2f
        paint.isAntiAlias = true
        canvas.drawCircle(center.toFloat(), center.toFloat(), radius.toFloat(), paint)

        paint.strokeWidth = 0f
        paint.color = textColor
        paint.textSize = progressTextSize
        paint.typeface = Typeface.DEFAULT_BOLD
        val percent = (progress.toFloat() / max.coerceAtLeast(1) * 100f).toInt()
        val percentText = "$percent%"
        if (textDisplayable && style == 0) {
            val textWidth = paint.measureText(percentText)
            canvas.drawText(
                percentText,
                center - textWidth / 2f,
                center + progressTextSize / 2f,
                paint,
            )
        }

        paint.strokeWidth = roundWidth
        paint.color = progressColor
        val edge = if (specialStyle == 1) center - radius else center - radius - 1
        val farEdge = if (specialStyle == 1) center + radius else center + radius + 1
        arcBounds.set(edge.toFloat(), edge.toFloat(), farEdge.toFloat(), farEdge.toFloat())
        val sweep = (progress.toFloat() / max.coerceAtLeast(1) * 360f).toInt()
        if (style == 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = strokeCap
            canvas.drawArc(arcBounds, 270f, sweep.toFloat(), false, paint)
        } else if (style == 1 && progress != 0) {
            paint.style = Paint.Style.FILL_AND_STROKE
            canvas.drawArc(arcBounds, 270f, sweep.toFloat(), true, paint)
        }
    }

    @Synchronized
    fun getMax(): Int = max

    @Synchronized
    fun setMax(value: Int) {
        require(value >= 0) { "max not less than 0" }
        max = value
    }

    @Synchronized
    fun getProgress(): Int = progress

    @Synchronized
    fun setProgress(value: Int) {
        require(value >= 0) { "progress not less than 0, progress: $value" }
        progress = value.coerceAtMost(max)
        postInvalidate()
    }

    fun getCricleColor(): Int = circleColor
    fun setCricleColor(value: Int) { circleColor = value }
    fun setStyleSpecial(value: Int) { specialStyle = value }
    fun getCricleProgressColor(): Int = progressColor
    fun setCricleProgressColor(value: Int) { progressColor = value }
    fun getTextColor(): Int = textColor
    fun setTextColor(value: Int) { textColor = value }
    fun getTextSize(): Float = progressTextSize
    fun setTextSize(value: Float) { progressTextSize = value }
    fun getRoundWidth(): Float = roundWidth
    fun setRoundWidth(value: Float) { roundWidth = value }
    fun setRoundColor(value: Int) { circleColor = value; invalidate() }
    fun setTextIsDisplayable(value: Boolean) { textDisplayable = value }
    fun setStrokeCap(value: Paint.Cap) { strokeCap = value }
}
