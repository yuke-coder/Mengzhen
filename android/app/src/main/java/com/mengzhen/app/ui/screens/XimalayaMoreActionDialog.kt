package com.mengzhen.app.ui.screens

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.ximalaya.ting.android.main.view.LinearItemDecoration

/**
 * Direct layout and action-model port of Ximalaya 9.5.1.4:
 * YPlayMoreActionDialogFragment, YPlayMoreActionHeaderAdapter and
 * YPlayMoreActionAdapter.
 */
@Composable
internal fun XimalayaSourceMoreActionSheet(
    liked: Boolean,
    downloaded: Boolean,
    notePreview: String?,
    onAction: (XimalayaMoreAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentAction by rememberUpdatedState(onAction)
    val currentDismiss by rememberUpdatedState(onDismiss)

    DisposableEffect(context, liked, downloaded, notePreview) {
        val dialog = createXimalayaMoreActionDialog(
            context = context,
            liked = liked,
            downloaded = downloaded,
            notePreview = notePreview,
            onAction = { action ->
                currentAction(action)
                if (!action.staysOpen) currentDismiss()
            },
            onDismiss = currentDismiss,
        )
        dialog.show()
        sizeXimalayaMoreActionDialog(dialog)
        onDispose {
            dialog.setOnCancelListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}

internal enum class XimalayaMoreAction(
    val staysOpen: Boolean = false,
) {
    SHARE,
    SKIP_HEAD_TAIL,
    ADD_TO_PLAYLIST,
    FAVORITE,
    FREE_AD,
    NOTE,
    PLAY_SETTINGS,
    SOUND_EFFECT_QUALITY,
    SOUND_DETAILS,
    RINGTONE,
    DOWNLOAD,
    DLNA,
    SURVEY_CONTENT,
    FEEDBACK,
    COMPLAIN,
    COPYRIGHT,
}

private data class MoreHeaderItem(
    val title: String,
    val icon: Int,
    val action: XimalayaMoreAction,
)

private data class MoreActionItem(
    val title: String,
    val icon: Int,
    val action: XimalayaMoreAction,
    val subtitle: String? = null,
)

private fun createXimalayaMoreActionDialog(
    context: Context,
    liked: Boolean,
    downloaded: Boolean,
    notePreview: String?,
    onAction: (XimalayaMoreAction) -> Unit,
    onDismiss: () -> Unit,
): Dialog {
    val dialog = Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(true)
    }
    val root = LayoutInflater.from(context)
        .inflate(R.layout.main_dialog_frag_xplay_more_action, null, false)
    dialog.setContentView(root)

    val headers = listOf(
        MoreHeaderItem("分享", R.drawable.arg_res_0x7f082a24, XimalayaMoreAction.SHARE),
        MoreHeaderItem("跳过头尾", R.drawable.arg_res_0x7f082534, XimalayaMoreAction.SKIP_HEAD_TAIL),
        MoreHeaderItem("加入听单", R.drawable.arg_res_0x7f082532, XimalayaMoreAction.ADD_TO_PLAYLIST),
        MoreHeaderItem(
            if (liked) "声音已收藏" else "声音收藏",
            if (liked) R.drawable.arg_res_0x7f082376 else R.drawable.arg_res_0x7f082375,
            XimalayaMoreAction.FAVORITE,
        ),
    )
    root.findViewById<RecyclerView>(R.id.main_play_more_action_header_rv).apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = MoreHeaderAdapter(headers, onAction)
        val intervalCount = if (headers.size > 1) headers.size - 1 else 3
        val availableGap = resources.displayMetrics.widthPixels -
            context.dpMore((((intervalCount + 1) * 54) + 32).toFloat())
        addItemDecoration(
            LinearItemDecoration().apply {
                b(context.dpMore(16f))
                a(availableGap / intervalCount / 2)
            },
        )
    }

    val actions = listOf(
        MoreActionItem("免广告", R.drawable.arg_res_0x7f082825, XimalayaMoreAction.FREE_AD),
        MoreActionItem(
            "备注",
            R.drawable.arg_res_0x7f082b13,
            XimalayaMoreAction.NOTE,
            subtitle = notePreview
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
                ?.takeIf(String::isNotBlank),
        ),
        MoreActionItem("播放设置", R.drawable.arg_res_0x7f082539, XimalayaMoreAction.PLAY_SETTINGS),
        MoreActionItem(
            "音质音效",
            R.drawable.arg_res_0x7f08253c,
            XimalayaMoreAction.SOUND_EFFECT_QUALITY,
            subtitle = AudioPlaybackService.getSoundEffect(context).displayName,
        ),
        MoreActionItem("声音详情", R.drawable.arg_res_0x7f0826fa, XimalayaMoreAction.SOUND_DETAILS),
        MoreActionItem("添加到听单", R.drawable.arg_res_0x7f082257, XimalayaMoreAction.ADD_TO_PLAYLIST),
        MoreActionItem("设为铃声", R.drawable.arg_res_0x7f082827, XimalayaMoreAction.RINGTONE),
        MoreActionItem(
            "下载节目",
            R.drawable.arg_res_0x7f0823e3,
            XimalayaMoreAction.DOWNLOAD,
            subtitle = if (downloaded) "已下载" else null,
        ),
        MoreActionItem(
            "连接外设",
            R.drawable.arg_res_0x7f082826,
            XimalayaMoreAction.DLNA,
        ),
        MoreActionItem("内容评价", R.drawable.arg_res_0x7f082810, XimalayaMoreAction.SURVEY_CONTENT),
        MoreActionItem("意见反馈", R.drawable.arg_res_0x7f08241b, XimalayaMoreAction.FEEDBACK),
        MoreActionItem("举报", R.drawable.arg_res_0x7f082e24, XimalayaMoreAction.COMPLAIN),
        MoreActionItem("版权申诉", R.drawable.arg_res_0x7f082e25, XimalayaMoreAction.COPYRIGHT),
    )
    root.findViewById<ListView>(R.id.main_play_more_action_lv).apply {
        divider = null
        dividerHeight = 0
        adapter = MoreActionAdapter(context, actions, onAction)
    }

    dialog.setOnCancelListener { onDismiss() }
    return dialog
}

private fun sizeXimalayaMoreActionDialog(dialog: Dialog) {
    dialog.window?.apply {
        decorView.setPadding(0, 0, 0, 0)
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        attributes = attributes.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = (decorView.resources.displayMetrics.heightPixels * 0.8f).toInt()
            gravity = Gravity.BOTTOM
            dimAmount = 0.8f
        }
        setWindowAnimations(R.style.arg_res_0x7f1303c3)
    }
}

private class MoreHeaderAdapter(
    private val items: List<MoreHeaderItem>,
    private val onAction: (XimalayaMoreAction) -> Unit,
) : RecyclerView.Adapter<MoreHeaderAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_xplay_more_action_header_view, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.icon)
        holder.title.text = item.title
        holder.itemView.contentDescription = item.title
        holder.itemView.setOnClickListener { onAction(item.action) }
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.main_pay_more_action_head_iv)
        val title: TextView = itemView.findViewById(R.id.main_pay_more_action_head_tv)
    }
}

private class MoreActionAdapter(
    private val context: Context,
    private val items: List<MoreActionItem>,
    private val onAction: (XimalayaMoreAction) -> Unit,
) : BaseAdapter() {
    override fun getCount(): Int = items.size
    override fun getItem(position: Int): MoreActionItem = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.main_item_yplay_more_action, parent, false)
        val item = getItem(position)
        val icon = view.findViewById<ImageView>(R.id.main_play_more_action_item_iv)
        val title = view.findViewById<TextView>(R.id.main_play_more_action_item_tv)
        val subtitle = view.findViewById<TextView>(R.id.main_play_more_action_item_subtitle)
        val toggle = view.findViewById<View>(R.id.main_cb_switch)

        icon.setImageResource(item.icon)
        icon.imageTintList = null
        title.text = item.title
        subtitle.visibility = if (item.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
        subtitle.text = item.subtitle.orEmpty()

        toggle.visibility = View.GONE
        view.contentDescription = item.title
        view.setOnClickListener { onAction(item.action) }
        return view
    }
}

private fun Context.dpMore(value: Float): Int =
    (value * resources.displayMetrics.density + 0.5f).toInt()
