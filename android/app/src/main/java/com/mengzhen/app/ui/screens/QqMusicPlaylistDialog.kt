package com.mengzhen.app.ui.screens

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.audio.QqMusicPlaybackMode
import com.mengzhen.app.data.model.TaskAudio
import com.tencent.qqmusic.ui.ViewPagerCircleIndicator
import com.tencent.qqmusic.activity.QqMusicShufflePlayHistoryActivity
import org.libpag.PAGView

internal data class QqMusicPlayedList(
    val id: String,
    val title: String,
    val audios: List<TaskAudio>,
)

/**
 * QQ Music 20.6.5.8 PlayerPopupPlayListNormal source adaptation.
 *
 * Its source height, three pages, tab indicator, current/recent/history queue
 * pages, row actions, handle-only ItemTouchHelper drag and clear confirmation
 * lifecycle are retained. Only QQ's cloud SongInfo/MusicPlayList objects are
 * mapped to this project's local TaskAudio/task history models.
 */
internal class QqMusicPlaylistDialog(context: Context) : BottomSheetDialog(context) {

    var onSelect: (Int) -> Unit = {}
    var onRemove: (Int) -> Unit = {}
    var onMove: (Int, Int) -> Unit = { _, _ -> }
    var onCycleMode: () -> QqMusicPlaybackMode = {
        QqMusicPlaybackMode.LIST_REPEAT
    }
    var onDownloadAll: (List<TaskAudio>) -> Unit = {}
    var onAddAll: (List<TaskAudio>) -> Unit = {}
    var onClear: () -> Unit = {}
    var onSelectPlayedAudio: (String, Int) -> Unit = { _, _ -> }
    var onOpenPlayedList: (String) -> Unit = {}
    var onClearPlayedList: (String) -> Unit = {}

    private val inflater = LayoutInflater.from(context)
    private val content = inflater.inflate(R.layout.qq_player_popup_playlist_outer, null)
    private val tabLayout = content.findViewById<ConstraintLayout>(R.id.l1g)
    private val currentTab = content.findViewById<View>(R.id.bej)
    private val recentTab = content.findViewById<View>(R.id.iso)
    private val playedListsTab = content.findViewById<View>(R.id.icg)
    private val currentTitle = content.findViewById<TextView>(R.id.bel)
    private val recentTitle = content.findViewById<TextView>(R.id.isz)
    private val playedListsTitle = content.findViewById<TextView>(R.id.ich)
    private val currentCount = content.findViewById<TextView>(R.id.bei)
    private val recentCount = content.findViewById<TextView>(R.id.isb)
    private val playedListsCount = content.findViewById<TextView>(R.id.icf)
    private val currentSource = content.findViewById<ImageView>(R.id.bem)
    private val indicator = content.findViewById<ImageView>(R.id.hla)
    private val pager = content.findViewById<ViewPager>(R.id.hzo)

    private val currentPage = CurrentPage(
        inflater.inflate(R.layout.qq_player_popup_playlist_current, pager, false),
    )
    private val recentPage = SongsPage(
        inflater.inflate(R.layout.qq_player_popup_playlist_recent, pager, false),
    ) { audio ->
        currentAudios.indexOfFirst { it.queueKey() == audio.queueKey() }
            .takeIf { it >= 0 }
            ?.let(onSelect)
    }
    private val playedListsPage = PlayedListsPage(
        inflater.inflate(R.layout.qq_player_popup_playlist_played_lists, pager, false),
    )
    private val pages = listOf(currentPage.root, recentPage.root, playedListsPage.root)
    private val shuffleAdjustDialog = QqMusicShufflePlayAdjustDialog(context)

    private var selectedPage = 0
    private var currentAudios = emptyList<TaskAudio>()
    private var currentQueueCleared = false

    init {
        val dialogHeight = (context.resources.displayMetrics.heightPixels * FULL_WINDOW_SCALE).toInt()
        setContentView(
            content,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dialogHeight),
        )
        setCanceledOnTouchOutside(true)
        behavior.skipCollapsed = true
        behavior.peekHeight = dialogHeight
        behavior.isHideable = true
        pager.adapter = SourcePagerAdapter()
        pager.offscreenPageLimit = 1
        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                selectedPage = position
                updateTabSelection()
            }
        })
        currentTab.setOnClickListener { pager.currentItem = 0 }
        recentTab.setOnClickListener { pager.currentItem = 1 }
        playedListsTab.setOnClickListener { pager.currentItem = 2 }

        currentPage.modeButton.setOnClickListener {
            currentPage.updateMode(onCycleMode())
        }
        currentPage.modeTitle.setOnClickListener {
            currentPage.updateMode(onCycleMode())
        }
        currentPage.shuffleAdjust.setOnClickListener { shuffleAdjustDialog.show() }
        currentPage.shuffleHistory.setOnClickListener {
            context.startActivity(Intent(context, QqMusicShufflePlayHistoryActivity::class.java))
        }
        currentPage.downloadAll.setOnClickListener { onDownloadAll(currentAudios) }
        currentPage.addAll.setOnClickListener { onAddAll(currentAudios) }
        currentPage.clear.setOnClickListener {
            showClearConfirmation {
                currentQueueCleared = true
                currentAudios = emptyList()
                onClear()
                currentPage.update(emptyList(), -1, false, false)
                currentCount.text = "0"
                updateTabContentDescriptions()
            }
        }

        val accessibility = context.getSystemService(AccessibilityManager::class.java)
        if (accessibility?.isEnabled == true) {
            listOf(currentCount, recentCount, playedListsCount).forEach {
                it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            listOf(currentTitle, recentTitle, playedListsTitle).forEach {
                it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            listOf(currentTab, recentTab, playedListsTab).forEach {
                it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
        }

        setOnShowListener {
            findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(Color.TRANSPARENT)
            window?.navigationBarColor = SHEET_COLOR
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        updateTabSelection()
    }

    fun update(
        audios: List<TaskAudio>,
        recentAudios: List<TaskAudio>,
        playedLists: List<QqMusicPlayedList>,
        selectedIndex: Int,
        isPlaying: Boolean,
        playbackMode: QqMusicPlaybackMode,
        canEditQueue: Boolean,
    ) {
        val visibleCurrentAudios = if (currentQueueCleared) emptyList() else audios
        currentAudios = visibleCurrentAudios
        currentCount.text = countText(visibleCurrentAudios.size)
        recentCount.text = countText(recentAudios.size)
        playedListsCount.text = countText(playedLists.size)
        currentPage.updateMode(playbackMode)
        currentPage.update(
            visibleCurrentAudios,
            if (currentQueueCleared) -1 else selectedIndex,
            isPlaying && !currentQueueCleared,
            canEditQueue && !currentQueueCleared,
        )
        recentPage.update(
            recentAudios,
            emptyText = context.getString(R.string.qq_playlist_empty_songs),
        )
        playedListsPage.update(playedLists)
        updateTabContentDescriptions()
    }

    private fun updateTabSelection() {
        val selectedViews = listOf(currentTitle, recentTitle, playedListsTitle)
        val selectedCounts = listOf(currentCount, recentCount, playedListsCount)
        selectedViews.forEachIndexed { index, view ->
            view.setTextColor(if (index == selectedPage) Color.WHITE else TAB_UNSELECTED)
        }
        selectedCounts.forEachIndexed { index, view ->
            view.setTextColor(if (index == selectedPage) Color.WHITE else TAB_UNSELECTED)
        }
        currentSource.setColorFilter(
            if (selectedPage == 0) Color.WHITE else TAB_UNSELECTED,
            PorterDuff.Mode.SRC_IN,
        )
        indicator.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        val target = when (selectedPage) {
            1 -> R.id.iso
            2 -> R.id.icg
            else -> R.id.bej
        }
        ConstraintSet().apply {
            clone(tabLayout)
            clear(indicator.id, ConstraintSet.START)
            connect(indicator.id, ConstraintSet.START, target, ConstraintSet.START)
            applyTo(tabLayout)
        }
        currentTab.isSelected = selectedPage == 0
        recentTab.isSelected = selectedPage == 1
        playedListsTab.isSelected = selectedPage == 2
        updateTabContentDescriptions()
    }

    private fun updateTabContentDescriptions() {
        currentTab.contentDescription = tabDescription(currentTitle.text, currentCount.text)
        recentTab.contentDescription = tabDescription(recentTitle.text, recentCount.text)
        playedListsTab.contentDescription =
            tabDescription(playedListsTitle.text, playedListsCount.text)
    }

    private fun tabDescription(title: CharSequence?, count: CharSequence?): String =
        buildString {
            if (!title.isNullOrEmpty()) append(title)
            if (!count.isNullOrEmpty()) append(count).append('首')
        }

    private fun countText(count: Int): String = when {
        count <= 0 -> "0"
        count <= 999 -> count.toString()
        else -> "999+"
    }

    private fun showClearConfirmation(onConfirmed: () -> Unit) {
        val confirmation = Dialog(context, R.style.QqMusicDialogStyle)
        confirmation.setContentView(R.layout.qq_music_clear_playlist_dialog)
        confirmation.setCancelable(false)
        confirmation.setCanceledOnTouchOutside(false)
        confirmation.findViewById<TextView>(R.id.content).text =
            context.getString(R.string.qq_playlist_clear_message)
        confirmation.findViewById<View>(R.id.close_btn).setOnClickListener {
            confirmation.dismiss()
        }
        confirmation.findViewById<TextView>(R.id.ha9).apply {
            text = context.getString(R.string.qq_playlist_cancel)
            setOnClickListener {
                confirmation.dismiss()
            }
        }
        confirmation.findViewById<TextView>(R.id.iav).apply {
            text = context.getString(R.string.qq_playlist_confirm_clear)
            setOnClickListener {
                onConfirmed()
                confirmation.dismiss()
            }
        }
        confirmation.setOnShowListener {
            confirmation.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        confirmation.show()
    }

    private inner class CurrentPage(val root: View) {
        val modeButton = root.findViewById<ImageButton>(R.id.e_)
        val modeTitle = root.findViewById<TextView>(R.id.hzf)
        val downloadAll = root.findViewById<ImageView>(R.id.hyv)
        val addAll = root.findViewById<ImageView>(R.id.hyt)
        val clear = root.findViewById<ImageView>(R.id.hyu)
        val shuffleAdjust = root.findViewById<View>(R.id.hzh)
        val shuffleHistory = root.findViewById<View>(R.id.hzk)
        private val shuffleAdjustTitle = root.findViewById<TextView>(R.id.hzi)
        private val list = root.findViewById<RecyclerView>(R.id.hz6)
        private lateinit var touchHelper: ItemTouchHelper
        private val adapter = PlaylistAdapter(
            onSelect = { _, index -> onSelect(index) },
            onRemove = { index, remainingCount ->
                onRemove(index)
                currentCount.text = countText(remainingCount)
            },
            onStartDrag = { holder -> touchHelper.startDrag(holder) },
        )

        init {
            shuffleAdjustTitle.setText(R.string.qq_playlist_shuffle_adjust_short)
            list.layoutManager = LinearLayoutManager(context)
            list.adapter = adapter
            list.itemAnimator = null
            touchHelper = ItemTouchHelper(SourceDragCallback(adapter))
            touchHelper.attachToRecyclerView(list)
        }

        fun updateMode(mode: QqMusicPlaybackMode) {
            val (icon, title) = when (mode) {
                QqMusicPlaybackMode.LIST_REPEAT ->
                    R.drawable.play_mode_normal_for_black to R.string.qq_mode_list_repeat
                QqMusicPlaybackMode.SINGLE_REPEAT ->
                    R.drawable.play_mode_single_new_for_black to R.string.qq_mode_single_repeat
                QqMusicPlaybackMode.SHUFFLE ->
                    R.drawable.play_mode_shuffle_new_for_black to R.string.qq_mode_shuffle
            }
            modeButton.setImageResource(icon)
            modeButton.contentDescription = context.getString(title)
            modeTitle.setText(title)
            val shuffle = mode == QqMusicPlaybackMode.SHUFFLE
            shuffleAdjust.visibility = if (shuffle) View.VISIBLE else View.GONE
            shuffleHistory.visibility = if (
                shuffle && AudioPlaybackService.getShufflePlayHistory(context).isNotEmpty()
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        fun update(
            audios: List<TaskAudio>,
            selectedIndex: Int,
            isPlaying: Boolean,
            canEditQueue: Boolean,
        ) {
            adapter.update(audios, selectedIndex, isPlaying, canEditQueue)
            clear.visibility = if (canEditQueue && audios.isNotEmpty()) View.VISIBLE else View.GONE
            downloadAll.visibility = if (audios.isNotEmpty()) View.VISIBLE else View.GONE
            addAll.visibility = if (audios.isNotEmpty()) View.VISIBLE else View.GONE
            if (selectedIndex in audios.indices) list.scrollToPosition(selectedIndex)
        }
    }

    private inner class SourceDragCallback(
        private val adapter: PlaylistAdapter,
    ) : ItemTouchHelper.Callback() {
        private var dragging = false
        private var dragStart = RecyclerView.NO_POSITION
        private var dragStop = RecyclerView.NO_POSITION

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
        ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

        override fun isItemViewSwipeEnabled(): Boolean = false

        override fun isLongPressDragEnabled(): Boolean = false

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            val moved = adapter.moveItem(from, to)
            if (moved) {
                recyclerView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                recyclerView.announceForAccessibility("${adapter.titleAt(to)}，${to + 1}")
            }
            return moved
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                dragging = true
                dragStart = viewHolder?.bindingAdapterPosition ?: RecyclerView.NO_POSITION
            }
        }

        override fun clearView(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
        ) {
            super.clearView(recyclerView, viewHolder)
            if (!dragging) return
            dragging = false
            dragStop = viewHolder.bindingAdapterPosition
            if (
                dragStart != RecyclerView.NO_POSITION &&
                dragStop != RecyclerView.NO_POSITION &&
                dragStart != dragStop
            ) {
                currentAudios = adapter.snapshot()
                onMove(dragStart, dragStop)
            }
            dragStart = RecyclerView.NO_POSITION
            dragStop = RecyclerView.NO_POSITION
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
    }

    private inner class SongsPage(
        val root: View,
        onSelectAudio: (TaskAudio) -> Unit,
    ) {
        private val list = root.findViewById<RecyclerView>(R.id.hz6)
        private val empty = root.findViewById<ViewStub>(R.id.hcp).inflate()
        private val emptyText = empty.findViewById<TextView>(R.id.hcq)
        private val adapter = PlaylistAdapter(
            onSelect = { audio, _ -> onSelectAudio(audio) },
            onRemove = { _, _ -> },
            onStartDrag = {},
        )

        init {
            list.layoutManager = LinearLayoutManager(context)
            list.adapter = adapter
            list.itemAnimator = null
        }

        fun update(audios: List<TaskAudio>, emptyText: String) {
            adapter.update(audios, -1, false, false)
            this.emptyText.text = emptyText
            empty.visibility = if (audios.isEmpty()) View.VISIBLE else View.GONE
            list.visibility = if (audios.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private inner class PlayedListsPage(val root: View) {
        private val listPager = root.findViewById<ViewPager>(R.id.hzm)
        private val pageIndicator = root.findViewById<ViewPagerCircleIndicator>(R.id.hzn)
        private val empty = root.findViewById<ViewStub>(R.id.hcp).inflate()
        private val emptyText = empty.findViewById<TextView>(R.id.hcq)
        private val queues = mutableListOf<QqMusicPlayedList>()
        private val queuePages = mutableListOf<PlayedListPage>()
        private val adapter = PlayedListsPagerAdapter()
        private var signature = ""

        init {
            emptyText.setText(R.string.qq_playlist_empty_lists)
            listPager.adapter = adapter
            listPager.offscreenPageLimit = 1
            pageIndicator.setImgsResId(
                R.drawable.pager_selected_for_black,
                R.drawable.pager_not_selected_for_black,
            )
            pageIndicator.setViewPager(listPager)
        }

        fun update(newQueues: List<QqMusicPlayedList>) {
            val nextSignature = newQueues.joinToString("|") { queue ->
                queue.id + ":" + queue.title + ":" +
                    queue.audios.joinToString(",") { it.queueKey() }
            }
            if (signature != nextSignature) {
                signature = nextSignature
                queues.clear()
                queues.addAll(newQueues)
                queuePages.clear()
                queuePages.addAll(newQueues.map(::PlayedListPage))
                adapter.notifyDataSetChanged()
            }
            val hasQueues = queues.isNotEmpty()
            listPager.visibility = if (hasQueues) View.VISIBLE else View.GONE
            empty.visibility = if (hasQueues) View.GONE else View.VISIBLE
            pageIndicator.setCount(queues.size.coerceAtLeast(1))
            pageIndicator.visibility = if (queues.size > 1) View.VISIBLE else View.GONE
            if (hasQueues && listPager.currentItem > queues.lastIndex) {
                listPager.currentItem = 0
            }
        }

        private fun removeQueue(id: String) {
            update(queues.filterNot { it.id == id })
            playedListsCount.text = countText(queues.size)
            updateTabContentDescriptions()
        }

        private inner class PlayedListPage(
            private val queue: QqMusicPlayedList,
        ) {
            val root: View = inflater.inflate(
                R.layout.qq_player_popup_playlist_played_list,
                listPager,
                false,
            )
            private val title = root.findViewById<TextView>(R.id.ice)
            private val count = root.findViewById<TextView>(R.id.ica)
            private val source = root.findViewById<ImageView>(R.id.icd)
            private val download = root.findViewById<ImageView>(R.id.icb)
            private val add = root.findViewById<ImageView>(R.id.ic9)
            private val clear = root.findViewById<ImageView>(R.id.ic_)
            private val list = root.findViewById<RecyclerView>(R.id.icc)
            private val songAdapter = PlaylistAdapter(
                onSelect = { _, index -> onSelectPlayedAudio(queue.id, index) },
                onRemove = { _, _ -> },
                onStartDrag = {},
            )

            init {
                title.text = queue.title
                title.isSelected = true
                count.text = context.getString(
                    R.string.qq_playlist_song_count,
                    queue.audios.size,
                )
                source.setOnClickListener { onOpenPlayedList(queue.id) }
                download.setOnClickListener { onDownloadAll(queue.audios) }
                add.setOnClickListener { onAddAll(queue.audios) }
                clear.setOnClickListener {
                    showClearConfirmation {
                        onClearPlayedList(queue.id)
                        removeQueue(queue.id)
                    }
                }
                list.layoutManager = LinearLayoutManager(context)
                list.adapter = songAdapter
                list.itemAnimator = null
                songAdapter.update(queue.audios, -1, false, false)
            }
        }

        private inner class PlayedListsPagerAdapter : PagerAdapter() {
            override fun getCount(): Int = queuePages.size

            override fun getItemPosition(objectValue: Any): Int = POSITION_NONE

            override fun isViewFromObject(view: View, objectValue: Any): Boolean =
                view === objectValue

            override fun instantiateItem(container: ViewGroup, position: Int): Any =
                queuePages[position].root.also { view ->
                    (view.parent as? ViewGroup)?.removeView(view)
                    container.addView(view)
                }

            override fun destroyItem(
                container: ViewGroup,
                position: Int,
                objectValue: Any,
            ) {
                container.removeView(objectValue as View)
            }
        }
    }

    private inner class SourcePagerAdapter : PagerAdapter() {
        override fun getCount(): Int = pages.size

        override fun isViewFromObject(view: View, objectValue: Any): Boolean =
            view === objectValue

        override fun instantiateItem(container: ViewGroup, position: Int): Any =
            pages[position].also(container::addView)

        override fun destroyItem(container: ViewGroup, position: Int, objectValue: Any) {
            container.removeView(objectValue as View)
        }
    }

    private inner class PlaylistAdapter(
        private val onSelect: (TaskAudio, Int) -> Unit,
        private val onRemove: (Int, Int) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    ) : RecyclerView.Adapter<PlaylistAdapter.Holder>() {
        private val entries = mutableListOf<TaskAudio>()
        private var selectedIndex = -1
        private var isPlaying = false
        private var canEdit = false

        fun update(
            audios: List<TaskAudio>,
            selectedIndex: Int,
            isPlaying: Boolean,
            canEdit: Boolean,
        ) {
            val listChanged =
                entries.map { it.queueKey() } != audios.map { it.queueKey() }
            val visualStateChanged =
                this.selectedIndex != selectedIndex ||
                    this.isPlaying != isPlaying ||
                    this.canEdit != canEdit
            if (listChanged) {
                entries.clear()
                entries.addAll(audios)
            }
            this.selectedIndex = selectedIndex
            this.isPlaying = isPlaying
            this.canEdit = canEdit
            if (listChanged || visualStateChanged) notifyDataSetChanged()
        }

        fun setSelected(index: Int) {
            val previous = selectedIndex
            selectedIndex = index
            if (previous in entries.indices) notifyItemChanged(previous)
            if (index in entries.indices) notifyItemChanged(index)
        }

        fun removeAt(index: Int) {
            if (index !in entries.indices) return
            entries.removeAt(index)
            when {
                index < selectedIndex -> selectedIndex--
                index == selectedIndex -> selectedIndex = index.coerceAtMost(entries.lastIndex)
            }
            notifyItemRemoved(index)
            notifyItemRangeChanged(index, entries.size - index)
        }

        fun moveItem(from: Int, to: Int): Boolean {
            if (!canEdit || from !in entries.indices || to !in entries.indices || from == to) {
                return false
            }
            val moved = entries.removeAt(from)
            entries.add(to, moved)
            selectedIndex = when {
                selectedIndex == from -> to
                from < selectedIndex && to >= selectedIndex -> selectedIndex - 1
                from > selectedIndex && to <= selectedIndex -> selectedIndex + 1
                else -> selectedIndex
            }
            notifyItemMoved(from, to)
            return true
        }

        fun snapshot(): List<TaskAudio> = entries.toList()

        fun titleAt(index: Int): String =
            entries.getOrNull(index)?.name?.substringBeforeLast('.').orEmpty()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(
                inflater.inflate(
                    R.layout.qq_player_popup_playlist_item,
                    parent,
                    false,
                ),
            )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(entries[position], position)
        }

        override fun onViewRecycled(holder: Holder) {
            holder.playing.stop()
        }

        override fun getItemCount(): Int = entries.size

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val title = itemView.findViewById<TextView>(R.id.kd_)
            private val artist = itemView.findViewById<TextView>(R.id.k31)
            private val move = itemView.findViewById<ImageView>(R.id.grv)
            private val favorite = itemView.findViewById<ImageView>(R.id.ccm)
            private val delete = itemView.findViewById<ImageView>(R.id.bm7)
            val playing = itemView.findViewById<PAGView>(R.id.dzl)

            init {
                playing.setPath("assets://pag/song_playing_icon.pag")
                playing.setRepeatCount(0)
                move.setOnTouchListener { view, event ->
                    if (
                        event.actionMasked == MotionEvent.ACTION_DOWN &&
                        canEdit &&
                        itemCount > 1
                    ) {
                        onStartDrag(this)
                        true
                    } else {
                        if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                        false
                    }
                }
            }

            fun bind(audio: TaskAudio, position: Int) {
                title.text = audio.name.substringBeforeLast('.')
                title.setTextColor(Color.WHITE)
                val artistName = audio.artist?.takeIf(String::isNotBlank)
                artist.text = artistName?.let { " - $it" }.orEmpty()
                artist.visibility = if (artistName == null) View.GONE else View.VISIBLE
                artist.setTextColor(TAB_UNSELECTED)
                val selected = position == selectedIndex
                playing.visibility = if (selected) View.VISIBLE else View.GONE
                if (selected && isPlaying) playing.play() else playing.stop()
                delete.visibility = if (canEdit) View.VISIBLE else View.GONE
                move.visibility = if (canEdit && itemCount > 1) View.VISIBLE else View.GONE
                favorite.visibility = View.VISIBLE
                itemView.contentDescription = audio.name
                itemView.setOnClickListener {
                    val index = bindingAdapterPosition
                    if (index != RecyclerView.NO_POSITION) {
                        if (selectedIndex >= 0) setSelected(index)
                        onSelect(entries[index], index)
                    }
                }
                delete.setOnClickListener {
                    val index = bindingAdapterPosition
                    if (index != RecyclerView.NO_POSITION) {
                        removeAt(index)
                        onRemove(index, itemCount)
                    }
                }
                favorite.setOnClickListener {
                    val key = audio.id.ifBlank { audio.name }
                    val prefs = context.getSharedPreferences(
                        "qq_playlist_favorites",
                        Context.MODE_PRIVATE,
                    )
                    val liked = !prefs.getBoolean(key, false)
                    prefs.edit().putBoolean(key, liked).apply()
                    favorite.setImageResource(
                        if (liked) R.drawable.player_popup_play_list_liked
                        else R.drawable.player_popup_play_list_like_black,
                    )
                }
                val key = audio.id.ifBlank { audio.name }
                val liked = context.getSharedPreferences(
                    "qq_playlist_favorites",
                    Context.MODE_PRIVATE,
                ).getBoolean(key, false)
                favorite.setImageResource(
                    if (liked) R.drawable.player_popup_play_list_liked
                    else R.drawable.player_popup_play_list_like_black,
                )
            }
        }
    }

    companion object {
        private const val FULL_WINDOW_SCALE = 0.64f
        private const val SHEET_COLOR = 0xFF1E1E1F.toInt()
        private const val TAB_UNSELECTED = 0x77FFFFFF
    }

    private fun TaskAudio.queueKey(): String = id.ifBlank {
        fileKey?.takeIf(String::isNotBlank)
            ?: localUri?.takeIf(String::isNotBlank)
            ?: serverUrl?.takeIf(String::isNotBlank)
            ?: dbKey.orEmpty()
    }
}

@Composable
internal fun QqMusicPlaylistSheet(
    audios: List<TaskAudio>,
    recentAudios: List<TaskAudio>,
    playedLists: List<QqMusicPlayedList>,
    selectedIndex: Int,
    isPlaying: Boolean,
    playbackMode: QqMusicPlaybackMode,
    canEditQueue: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onCycleMode: () -> QqMusicPlaybackMode,
    onDownloadAll: (List<TaskAudio>) -> Unit,
    onAddAll: (List<TaskAudio>) -> Unit,
    onClear: () -> Unit,
    onSelectPlayedAudio: (String, Int) -> Unit,
    onOpenPlayedList: (String) -> Unit,
    onClearPlayedList: (String) -> Unit,
) {
    val context = LocalContext.current
    val latestDismiss = rememberUpdatedState(onDismiss)
    val dialog = remember(context) { QqMusicPlaylistDialog(context) }

    SideEffect {
        dialog.onSelect = onSelect
        dialog.onRemove = onRemove
        dialog.onMove = onMove
        dialog.onCycleMode = onCycleMode
        dialog.onDownloadAll = onDownloadAll
        dialog.onAddAll = onAddAll
        dialog.onClear = onClear
        dialog.onSelectPlayedAudio = onSelectPlayedAudio
        dialog.onOpenPlayedList = onOpenPlayedList
        dialog.onClearPlayedList = onClearPlayedList
        dialog.update(
            audios = audios,
            recentAudios = recentAudios,
            playedLists = playedLists,
            selectedIndex = selectedIndex,
            isPlaying = isPlaying,
            playbackMode = playbackMode,
            canEditQueue = canEditQueue,
        )
    }

    DisposableEffect(dialog) {
        dialog.setOnDismissListener { latestDismiss.value() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}
