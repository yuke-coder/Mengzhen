package com.mengzhen.app.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.audio.QqMusicShufflePreference

/** QQ 音乐 20.6.5.8 ShufflePlayAdjustDialog source port. */
internal class QqMusicShufflePlayAdjustDialog(
    context: Context,
) : BottomSheetDialog(context) {

    private val root = LayoutInflater.from(context)
        .inflate(R.layout.qq_shuffle_play_adjust_dialog, null, false)
    private val defaultRow = root.findViewById<View>(R.id.bl6)
    private val exploreRow = root.findViewById<View>(R.id.jk9)
    private val frequentRow = root.findViewById<View>(R.id.hi2)
    private val defaultMark = root.findViewById<ImageView>(R.id.bl3)
    private val exploreMark = root.findViewById<ImageView>(R.id.jho)
    private val frequentMark = root.findViewById<ImageView>(R.id.hi1)
    private var selected = AudioPlaybackService.getShufflePreference(context)

    init {
        setContentView(root)
        setCanceledOnTouchOutside(true)
        defaultRow.setOnClickListener { select(QqMusicShufflePreference.DEFAULT) }
        exploreRow.setOnClickListener { select(QqMusicShufflePreference.FRESH_EXPLORE) }
        frequentRow.setOnClickListener { select(QqMusicShufflePreference.RECENT_FREQUENT) }
        setOnShowListener {
            findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(Color.TRANSPARENT)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun show() {
        selected = AudioPlaybackService.getShufflePreference(context)
        updateColor()
        updateCheckedStatus()
        super.show()
    }

    private fun select(value: QqMusicShufflePreference) {
        if (selected != value) {
            selected = AudioPlaybackService.setShufflePreference(context, value)
        }
        dismiss()
    }

    private fun updateCheckedStatus() {
        defaultMark.visibility = if (selected == QqMusicShufflePreference.DEFAULT) {
            View.VISIBLE
        } else {
            View.GONE
        }
        exploreMark.visibility = if (selected == QqMusicShufflePreference.FRESH_EXPLORE) {
            View.VISIBLE
        } else {
            View.GONE
        }
        frequentMark.visibility = if (selected == QqMusicShufflePreference.RECENT_FREQUENT) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateColor() {
        val dark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        root.setBackgroundResource(
            if (dark) {
                R.drawable.shuffle_play_adjust_dialog_top_round_dark_bg
            } else {
                R.drawable.shuffle_play_adjust_dialog_top_round_light_bg
            },
        )
        val titleColor = ContextCompat.getColor(context, if (dark) R.color.white else R.color.black)
        val summaryColor = ContextCompat.getColor(
            context,
            if (dark) R.color.white_70_transparent else R.color.black_70_opacity,
        )
        listOf(R.id.jwn, R.id.ble, R.id.jlm, R.id.hia).forEach { id ->
            root.findViewById<TextView>(id).setTextColor(titleColor)
        }
        listOf(R.id.bld, R.id.jlk, R.id.hi_).forEach { id ->
            root.findViewById<TextView>(id).setTextColor(summaryColor)
        }
    }
}
