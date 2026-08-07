package com.ximalaya.ting.android.main.playpage.playy.component.functionv2.view

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.mengzhen.app.R

/**
 * Source-compatible port of Ximalaya 9.4.95.3 YFunctionLayoutV2.
 *
 * Child order, measurements, margins, alpha, labels, equal-spacing layout and
 * expanded 15dp touch targets follow the decompiled client implementation.
 * Business icons are injected by the player binding, as in BusinessIconManagerV2.
 */
class YFunctionLayoutV2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {

    val firstBizLayout = FrameLayout(context)
    val speedIconPanel = SpeedIconView(context)
    val likeFrameLayout = FrameLayout(context)
    val biz3Layout = FrameLayout(context)
    val moreIcon = ImageView(context)
    val moreText = sourceText("更多")
    val biz3Text = sourceText("评论")
    val likeText = sourceText("点赞")
    val speedText = sourceText("倍速")
    val biz1Text = sourceText("音色切换")

    private val topPaddingValue = context.dp(5)
    private val bottomPaddingValue = context.dp(28)
    private val iconSize = context.dp(26)
    private val labelTopMargin = context.dp(4)
    private val touchRects = mutableListOf<Pair<View, Rect>>()
    private var activeTouchView: View? = null

    init {
        clipChildren = false
        clipToPadding = false

        firstBizLayout.layoutParams = MarginLayoutParams(iconSize, iconSize).apply {
            leftMargin = context.dp(16)
        }
        addView(firstBizLayout)

        speedIconPanel.layoutParams = MarginLayoutParams(
            LayoutParams.WRAP_CONTENT,
            iconSize,
        ).apply {
            topMargin = labelTopMargin
        }
        addView(speedIconPanel)

        likeFrameLayout.layoutParams = MarginLayoutParams(iconSize, iconSize)
        addView(likeFrameLayout)

        biz3Layout.layoutParams = MarginLayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        )
        addView(biz3Layout)

        moreIcon.layoutParams = MarginLayoutParams(iconSize, iconSize).apply {
            rightMargin = context.dp(16)
        }
        moreIcon.imageAlpha = (255 * SOURCE_ICON_ALPHA).toInt()
        moreIcon.setColorFilter(Color.WHITE)
        moreIcon.setImageResource(R.drawable.arg_res_0x7f08259e)
        moreIcon.contentDescription = "更多"
        addView(moreIcon)

        moreText.layoutParams = MarginLayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        ).apply {
            rightMargin = context.dp(16)
            topMargin = labelTopMargin
        }
        addView(moreText)

        listOf(biz3Text, likeText, speedText).forEach { label ->
            label.layoutParams = MarginLayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = labelTopMargin
            }
            addView(label)
        }
        biz1Text.layoutParams = MarginLayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        )
        addView(biz1Text)
    }

    fun setFirstLabel(value: String) {
        biz1Text.text = value
    }

    fun setThirdLabel(value: String) {
        biz3Text.text = value
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
        MarginLayoutParams(context, attrs)

    override fun generateLayoutParams(params: LayoutParams?): LayoutParams =
        MarginLayoutParams(params)

    override fun checkLayoutParams(params: LayoutParams?): Boolean =
        params is MarginLayoutParams

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxHeight = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == GONE) continue
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            maxHeight = maxOf(maxHeight, child.measuredHeight)
        }
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            maxHeight + topPaddingValue + bottomPaddingValue,
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val firstParams = firstBizLayout.layoutParams as MarginLayoutParams
        layoutAt(firstBizLayout, firstParams.leftMargin, topPaddingValue)
        layoutCenteredBelow(biz1Text, firstBizLayout)

        val moreParams = moreIcon.layoutParams as MarginLayoutParams
        layoutAt(
            moreIcon,
            width - moreIcon.measuredWidth - moreParams.rightMargin,
            topPaddingValue,
        )
        layoutCenteredBelow(moreText, moreIcon)

        val availableStart = firstBizLayout.right
        val availableEnd = moreIcon.left
        val firstGapCenter = availableStart +
            ((availableEnd - availableStart - likeFrameLayout.measuredWidth) / 2)
        layoutAt(
            likeFrameLayout,
            firstGapCenter,
            topPaddingValue,
        )
        layoutCenteredBelow(likeText, likeFrameLayout)

        layoutAt(
            speedIconPanel,
            availableStart +
                ((likeFrameLayout.left - availableStart - speedIconPanel.measuredWidth) / 2),
            topPaddingValue,
        )
        layoutCenteredBelow(speedText, speedIconPanel)

        layoutAt(
            biz3Layout,
            likeFrameLayout.right +
                ((availableEnd - likeFrameLayout.right - biz3Layout.measuredWidth) / 2),
            likeFrameLayout.bottom - biz3Layout.measuredHeight,
        )
        layoutCenteredBelow(biz3Text, biz3Layout)
        updateTouchRects()
    }

    private fun layoutCenteredBelow(label: TextView, icon: View) {
        val params = label.layoutParams as MarginLayoutParams
        layoutAt(
            label,
            icon.left + ((icon.measuredWidth - label.measuredWidth) / 2),
            icon.bottom + params.topMargin,
        )
    }

    private fun layoutAt(view: View, left: Int, top: Int) {
        view.layout(left, top, left + view.measuredWidth, top + view.measuredHeight)
    }

    private fun updateTouchRects() {
        val expand = context.dp(15)
        touchRects.clear()
        listOf(
            firstBizLayout,
            speedIconPanel,
            likeFrameLayout,
            biz3Layout,
            moreIcon,
        ).filter { it.visibility != GONE }.forEach { child ->
            val rect = Rect()
            child.getHitRect(rect)
            rect.inset(-expand, -expand)
            touchRects += child to rect
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeTouchView = touchRects.firstOrNull {
                    it.second.contains(event.x.toInt(), event.y.toInt())
                }?.first
            }

            MotionEvent.ACTION_CANCEL -> activeTouchView = null
        }
        val target = activeTouchView ?: return false
        val forwarded = MotionEvent.obtain(event).apply {
            setLocation(target.width / 2f, target.height / 2f)
        }
        val handled = target.dispatchTouchEvent(forwarded)
        forwarded.recycle()
        if (event.actionMasked == MotionEvent.ACTION_UP) activeTouchView = null
        return handled
    }

    private fun sourceText(value: String) = TextView(context).apply {
        text = value
        setTextColor(Color.WHITE)
        alpha = SOURCE_TEXT_ALPHA
        textSize = SOURCE_TEXT_SIZE_SP
        isSingleLine = true
        includeFontPadding = true
    }

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        const val SOURCE_ICON_ALPHA = 0.55f
        const val SOURCE_TEXT_ALPHA = 0.4f
        const val SOURCE_TEXT_SIZE_SP = 10f
    }
}
