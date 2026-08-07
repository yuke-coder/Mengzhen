package com.mengzhen.app.ui.screens

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mengzhen.app.R
import com.mengzhen.app.audio.PlaybackSoundEffect
import com.ximalaya.ting.android.host.view.other.MyViewPager
import com.ximalaya.ting.android.main.playpage.view.PlayListPagerSlidingTabStrip

/**
 * Direct outer-layout and interaction port of Ximalaya 9.4.95.3
 * ChooseTrackSoundEffectAiDialogXNew.
 *
 * Ximalaya obtains TrackQualityAndEffectInfo from its track API. Local files have no
 * trackId/albumId contract, so the same view hierarchy is bound to the file's original
 * quality and Android AudioEffect-backed SoundEffectItem equivalents.
 */
@Composable
internal fun XimalayaSourceSoundEffectQualitySheet(
    initialEffect: PlaybackSoundEffect,
    onEffectSelected: (PlaybackSoundEffect) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentSelected by rememberUpdatedState(onEffectSelected)
    val currentDismiss by rememberUpdatedState(onDismiss)

    DisposableEffect(context) {
        val dialog = createSoundEffectQualityDialog(
            context = context,
            initialEffect = initialEffect,
            onEffectSelected = currentSelected,
            onDismiss = currentDismiss,
        )
        dialog.show()
        sizeSoundEffectQualityDialog(dialog)
        onDispose {
            dialog.setOnCancelListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}

private fun createSoundEffectQualityDialog(
    context: Context,
    initialEffect: PlaybackSoundEffect,
    onEffectSelected: (PlaybackSoundEffect) -> Unit,
    onDismiss: () -> Unit,
): Dialog {
    val dialog = Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(true)
    }
    val root = LayoutInflater.from(context)
        .inflate(R.layout.main_layout_sound_effect_quality, null, false)
    dialog.setContentView(root)

    val tabs = root.findViewById<PlayListPagerSlidingTabStrip>(R.id.main_play_tabs)
    val content = root.findViewById<MyViewPager>(R.id.main_play_viewpager)
    val tabNames = listOf("音质", "音效")
    val tabViews = mutableListOf<SourceSoundTab>()
    var selectedTab = 1
    var selectedEffect = initialEffect

    fun showTab(position: Int) {
        selectedTab = position.coerceIn(tabNames.indices)
        tabViews.forEachIndexed { index, tab -> tab.bindSelected(index == selectedTab) }
        content.removeAllViews()
        val list = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(ContextCompat.getColor(context, R.color.arg_res_0x7f060521))
            adapter = if (selectedTab == 0) {
                SourceQualityAdapter()
            } else {
                SourceSoundEffectAdapter(
                    initialEffect = selectedEffect,
                    onSelected = { effect ->
                        selectedEffect = effect
                        onEffectSelected(effect)
                    },
                )
            }
        }
        content.addView(
            list,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    tabNames.forEachIndexed { index, title ->
        val tab = SourceSoundTab(context, title) { showTab(index) }
        tabViews += tab
        tabs.addView(
            tab.root,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
        )
    }
    root.findViewById<ImageView>(R.id.main_close).setOnClickListener {
        dialog.dismiss()
    }
    dialog.setOnCancelListener { onDismiss() }
    dialog.setOnDismissListener { onDismiss() }
    showTab(selectedTab)
    return dialog
}

private fun sizeSoundEffectQualityDialog(dialog: Dialog) {
    dialog.window?.apply {
        dialog.findViewById<View>(R.id.main_cl_main_content)?.let { content ->
            content.layoutParams = content.layoutParams.apply {
                height = (decorView.resources.displayMetrics.heightPixels * 0.6f).toInt()
            }
        }
        decorView.setPadding(0, 0, 0, 0)
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        attributes = attributes.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.BOTTOM
            dimAmount = 0.5f
        }
        setWindowAnimations(R.style.arg_res_0x7f1303c3)
    }
}

private class SourceSoundTab(
    context: Context,
    title: String,
    onClick: () -> Unit,
) {
    val root = LinearLayout(context).apply {
        gravity = Gravity.CENTER
        orientation = LinearLayout.VERTICAL
        isClickable = true
        isFocusable = true
        contentDescription = title
        setOnClickListener { onClick() }
    }
    private val label = TextView(context).apply {
        text = title
        gravity = Gravity.CENTER
        includeFontPadding = false
    }
    private val indicator = View(context).apply {
        background = ColorDrawable(ContextCompat.getColor(context, R.color.arg_res_0x7f060646))
    }

    init {
        root.addView(
            label,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                1f,
            ).apply { gravity = Gravity.CENTER },
        )
        root.addView(
            indicator,
            LinearLayout.LayoutParams(context.dpSound(24f), context.dpSound(3f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
    }

    fun bindSelected(selected: Boolean) {
        label.setTextColor(
            ContextCompat.getColor(
                root.context,
                if (selected) R.color.arg_res_0x7f06062e else R.color.arg_res_0x7f060ae9,
            ),
        )
        label.textSize = if (selected) 20f else 15f
        label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        indicator.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        root.isSelected = selected
    }
}

private class SourceQualityAdapter : RecyclerView.Adapter<SourceQualityAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.main_item_track_play_quality, parent, false),
        )

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.title.text = "原始音质"
        holder.subtitle.text = "按本地文件原始品质播放"
        holder.selected.visibility = View.VISIBLE
        holder.itemView.contentDescription = "原始音质，已选择"
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.main_tv_title)
        val subtitle: TextView = itemView.findViewById(R.id.main_tv_subtitle)
        val selected: ImageView = itemView.findViewById(R.id.main_iv_select)
    }
}

private class SourceSoundEffectAdapter(
    initialEffect: PlaybackSoundEffect,
    private val onSelected: (PlaybackSoundEffect) -> Unit,
) : RecyclerView.Adapter<SourceSoundEffectAdapter.Holder>() {
    private val items = PlaybackSoundEffect.entries
    private var selectedEffect = initialEffect

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.main_item_track_sound_effect_y, parent, false),
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val effect = items[position]
        val selected = effect == selectedEffect
        holder.title.text = effect.displayName
        holder.subtitle.text = effect.description
        holder.select.text = if (selected) "使用中" else "使用"
        holder.select.isSelected = selected
        holder.select.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (selected) R.color.arg_res_0x7f0603a1 else R.color.arg_res_0x7f060de3,
            ),
        )
        holder.itemView.contentDescription =
            "${effect.displayName}，${if (selected) "使用中" else "使用"}"
        val select = {
            if (selectedEffect != effect) {
                selectedEffect = effect
                notifyDataSetChanged()
                onSelected(effect)
            }
        }
        holder.itemView.setOnClickListener { select() }
        holder.select.setOnClickListener { select() }
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.main_tv_title)
        val subtitle: TextView = itemView.findViewById(R.id.main_tv_subtitle)
        val select: TextView = itemView.findViewById(R.id.main_tv_select)
    }
}

private fun Context.dpSound(value: Float): Int =
    (value * resources.displayMetrics.density + 0.5f).toInt()
