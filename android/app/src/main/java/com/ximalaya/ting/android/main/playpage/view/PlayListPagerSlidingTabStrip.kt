package com.ximalaya.ting.android.main.playpage.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * XML-compatible host for the original 9.4.95.3 tab strip layout.
 * Tab content and source state are bound by XimalayaSoundEffectQualityDialog.
 */
class PlayListPagerSlidingTabStrip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        orientation = HORIZONTAL
    }
}
