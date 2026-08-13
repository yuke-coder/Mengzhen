package com.mengzhen.app.ui.screens

import android.text.format.Formatter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import coil3.load
import com.mengzhen.app.R
import com.mengzhen.app.bilibili.BiliCacheItem
import com.mengzhen.app.bilibili.BiliImportProgress
import java.util.Locale

@Composable
internal fun BiliOfflineToolbar(
    topLevel: Boolean,
    canEdit: Boolean,
    editMode: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LayoutInflater.from(context).inflate(
                R.layout.bili_app_offline_toolbar,
                null,
                false,
            ) as Toolbar
        },
        update = { toolbar ->
            toolbar.title = "离线缓存"
            toolbar.navigationIcon = if (topLevel) {
                null
            } else {
                ContextCompat.getDrawable(
                    toolbar.context,
                    androidx.appcompat.R.drawable.abc_ic_ab_back_material,
                )
            }
            toolbar.navigationIcon?.let { icon ->
                DrawableCompat.setTint(
                    icon.mutate(),
                    ContextCompat.getColor(toolbar.context, R.color.theme_color_primary_tr_icon),
                )
            }
            toolbar.setNavigationOnClickListener { onBack() }

            val search = toolbar.menu.findItem(R.id.offline_video_search)
            val settings = toolbar.menu.findItem(R.id.offline_video_setting)
            val edit = toolbar.menu.findItem(R.id.offline_video_edit)
            search?.isVisible = !editMode
            settings?.isVisible = !editMode
            edit?.let {
                it.isVisible = canEdit
                it.title = if (editMode) "取消" else "编辑"
                it.icon = if (editMode) {
                    null
                } else {
                    ContextCompat.getDrawable(toolbar.context, R.drawable.ic_download_edit)
                }
            }
            listOf(search, settings, edit).filterNotNull().forEach { item ->
                item.icon?.let { icon ->
                    DrawableCompat.setTint(
                        icon.mutate(),
                        ContextCompat.getColor(
                            toolbar.context,
                            R.color.theme_color_primary_tr_icon,
                        ),
                    )
                }
            }
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.offline_video_search -> onSearch()
                    R.id.offline_video_setting -> onSettings()
                    R.id.offline_video_edit -> onEdit()
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
        },
    )
}

@Composable
internal fun BiliOfflineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LayoutInflater.from(context).inflate(
                R.layout.bili_app_offline_search_bar,
                null,
                false,
            ).also { root ->
                root.findViewById<SearchView>(R.id.search_bar).apply {
                    findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
                        ?.apply {
                            setTextColor(ContextCompat.getColor(context, R.color.Ga10))
                            setHintTextColor(ContextCompat.getColor(context, R.color.Ga4))
                            textSize = 14f
                            background = null
                        }
                    post {
                        requestFocus()
                        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }
        },
        update = { root ->
            val search = root.findViewById<SearchView>(R.id.search_bar)
            if (search.query.toString() != query) search.setQuery(query, false)
            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(value: String?): Boolean {
                    onQueryChange(value.orEmpty())
                    search.clearFocus()
                    return true
                }

                override fun onQueryTextChange(value: String?): Boolean {
                    onQueryChange(value.orEmpty())
                    return true
                }
            })
            root.findViewById<View>(R.id.cancel).setOnClickListener { onCancel() }
        },
    )
}

@Composable
internal fun BiliOfflineDownloadingItem(
    items: List<BiliCacheItem>,
    artworkLocation: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = items.first()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LayoutInflater.from(context).inflate(
                R.layout.bili_app_list_item_offline_home_downloading,
                null,
                false,
            )
        },
        update = { view ->
            view.setOnClickListener { onClick() }
            view.findViewById<ImageView>(R.id.cover).loadIfChanged(artworkLocation)
            view.findViewById<TextView>(R.id.count).text = "${items.size} 个内容"
            view.findViewById<TextView>(R.id.title).text = first.title
            view.findViewById<TextView>(R.id.subtitle).apply {
                text = first.subtitle
                visibility = if (first.subtitle.isBlank() || first.subtitle == first.title) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
            view.findViewById<TextView>(R.id.tip).text = "正在缓存"
            view.findViewById<TextView>(R.id.total_size).text =
                items.sumOf(BiliCacheItem::audioSize)
                    .takeIf { it > 0L }
                    ?.let { Formatter.formatFileSize(view.context, it) }
                    .orEmpty()
        },
    )
}

@Composable
internal fun BiliOfflineSectionTitle(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LayoutInflater.from(context).inflate(
                R.layout.bili_app_list_item_offline_home_sectitle,
                null,
                false,
            )
        },
    )
}

@Composable
internal fun BiliOfflineDownloadedItem(
    item: BiliCacheItem,
    artworkLocation: String?,
    selected: Boolean,
    extracted: Boolean,
    editMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LayoutInflater.from(context).inflate(
                R.layout.bili_app_list_item_offline_home_downloaded,
                null,
                false,
            )
        },
        update = { view ->
            val checkbox = view.findViewById<CheckBox>(R.id.checkbox)
            checkbox.setOnCheckedChangeListener(null)
            checkbox.visibility = if (editMode && !extracted) View.VISIBLE else View.GONE
            checkbox.isChecked = selected
            checkbox.isEnabled = !extracted
            checkbox.setOnCheckedChangeListener { _, checked ->
                if (!extracted && checked != selected) onToggle()
            }
            view.isClickable = !extracted
            view.setOnClickListener(if (extracted) null else View.OnClickListener { onClick() })
            view.setOnLongClickListener(if (extracted) null else View.OnLongClickListener {
                onLongClick()
                true
            })

            view.findViewById<ImageView>(R.id.cover).loadIfChanged(artworkLocation)
            view.findViewById<TextView>(R.id.cover_desc).apply {
                val duration = item.durationSeconds
                text = if (duration > 0L) formatOfflineDuration(duration) else ""
                visibility = if (duration > 0L) View.VISIBLE else View.GONE
            }
            view.findViewById<TextView>(R.id.label).apply {
                text = if (extracted) "已提取" else ""
                visibility = if (extracted) View.VISIBLE else View.GONE
            }
            view.findViewById<TextView>(R.id.title).text = item.displayTitle()
            view.findViewById<TextView>(R.id.up_name).apply {
                text = item.owner
                visibility = if (item.owner.isBlank()) View.GONE else View.VISIBLE
            }
            view.findViewById<TextView>(R.id.video_size).text =
                item.audioSize.takeIf { it > 0L }
                    ?.let { Formatter.formatFileSize(view.context, it) }
                    .orEmpty()
        },
    )
}

private fun ImageView.loadIfChanged(location: String?) {
    val key = location.orEmpty()
    if (tag == key) return
    tag = key
    load(location)
}

@Composable
internal fun BiliOfflineBottomBar(
    allSelected: Boolean,
    selectionEnabled: Boolean,
    selectedCount: Int,
    importing: Boolean,
    progress: BiliImportProgress?,
    onToggleAll: () -> Unit,
    onExtract: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LayoutInflater.from(context).inflate(
                R.layout.bili_app_view_offline_bottom_bar,
                null,
                false,
            )
        },
        update = { view ->
            val checkbox = view.findViewById<CheckBox>(R.id.checkbox)
            val checkArea = view.findViewById<View>(R.id.check_area)
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isEnabled = selectionEnabled && !importing
            checkbox.isChecked = allSelected
            checkbox.setOnCheckedChangeListener { _, checked ->
                if (checked != allSelected && selectionEnabled && !importing) onToggleAll()
            }
            checkArea.isEnabled = selectionEnabled && !importing
            checkArea.setOnClickListener {
                if (selectionEnabled && !importing) onToggleAll()
            }

            view.findViewById<TextView>(R.id.extract).apply {
                val enabled = selectedCount > 0 && !importing
                isEnabled = enabled
                text = when {
                    importing && progress?.percent != null ->
                        "${progress.current}/${progress.total}  ${progress.percent}%"
                    importing -> "正在提取"
                    selectedCount > 0 -> "提取音频（$selectedCount）"
                    else -> "提取音频"
                }
                contentDescription = progress?.title
                setTextColor(
                    context.getColor(if (enabled || importing) R.color.Pi5 else R.color.Ga4)
                )
                setOnClickListener { if (enabled) onExtract() }
            }
        },
    )
}

@Composable
internal fun BiliOfflineSearchSummary(
    query: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LayoutInflater.from(context).inflate(
                R.layout.bili_app_offline_search_summary,
                null,
                false,
            )
        },
        update = { root ->
            root.findViewById<TextView>(R.id.search_summary).text =
                "共找到关于${query}的${count}个内容"
        },
    )
}

@Composable
internal fun BiliOfflineEmpty(
    modifier: Modifier = Modifier,
    message: String = "这里还什么都没有呢～",
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LayoutInflater.from(context).inflate(
                R.layout.bili_app_view_offline_empty,
                null,
                false,
            )
        },
        update = { root ->
            root.findViewById<TextView>(R.id.empty_text).text = message
        },
    )
}

private fun formatOfflineDuration(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remain = seconds % 60
    return if (hours > 0L) {
        String.format(Locale.CHINA, "%d:%02d:%02d", hours, minutes, remain)
    } else {
        String.format(Locale.CHINA, "%d:%02d", minutes, remain)
    }
}
