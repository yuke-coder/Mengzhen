package com.ximalaya.ting.android.host.view.other

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * XML-compatible content host used by the source sound-quality panel.
 *
 * The original class subclasses ViewPager; the local panel preserves the same
 * hierarchy while replacing its server-backed Fragment adapter with local audio data.
 */
class MyViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr)
