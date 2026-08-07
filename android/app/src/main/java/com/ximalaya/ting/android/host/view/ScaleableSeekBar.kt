package com.ximalaya.ting.android.host.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar

/**
 * Runtime-compatible host for Ximalaya's ScaleableSeekBar.
 * The local player uses the same XML dimensions and connects seeking directly
 * to the playback service.
 */
class ScaleableSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.seekBarStyle,
) : AppCompatSeekBar(context, attrs, defStyleAttr)
