package com.mengzhen.app.ui.screens

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mengzhen.app.R
import com.mengzhen.app.audio.healing.QqMusicHealingScene

internal fun createQqMusicHealingPickerDialog(
    context: Context,
    onReturn: () -> Unit,
    onSceneSelected: (QqMusicHealingScene) -> Unit,
): Dialog = Dialog(context).apply {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.qq_auto_close_healing_dialog)
    findViewById<View>(R.id.qq_healing_back).setOnClickListener { onReturn() }
    findViewById<RecyclerView>(R.id.qq_healing_list).apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = QqMusicHealingAdapter(onSceneSelected)
    }
    setOnCancelListener { onReturn() }
    window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }
}

private class QqMusicHealingAdapter(
    private val onSceneSelected: (QqMusicHealingScene) -> Unit,
) : RecyclerView.Adapter<QqMusicHealingAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.qq_auto_close_healing_item, parent, false)
        view.layoutParams = RecyclerView.LayoutParams(dp(parent.context, 148), dp(parent.context, 136))
        return Holder(view)
    }

    override fun getItemCount(): Int = QqMusicHealingScene.entries.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(QqMusicHealingScene.entries[position], onSceneSelected)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cover = itemView.findViewById<ImageView>(R.id.qq_healing_cover).apply {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(view.context, 6).toFloat())
                }
            }
            clipToOutline = true
        }
        private val title = itemView.findViewById<TextView>(R.id.qq_healing_title)
        private val subtitle = itemView.findViewById<TextView>(R.id.qq_healing_subtitle)
        private val duration = itemView.findViewById<TextView>(R.id.qq_healing_duration)
        private val bottom = itemView.findViewById<View>(R.id.qq_healing_bottom)

        fun bind(
            scene: QqMusicHealingScene,
            onSceneSelected: (QqMusicHealingScene) -> Unit,
        ) {
            cover.setImageResource(scene.coverRes)
            title.text = scene.title
            subtitle.text = scene.subtitle
            duration.text = "30分钟"
            bottom.backgroundTintList = android.content.res.ColorStateList.valueOf(scene.color.toInt())
            itemView.setOnClickListener { onSceneSelected(scene) }
        }
    }
}

private fun dp(context: Context, value: Int): Int =
    (value * context.resources.displayMetrics.density + .5f).toInt()
