package com.tencent.qqmusic.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** QQ 音乐 20.6.5.8 ShufflePlayHistoryActivity source port. */
class QqMusicShufflePlayHistoryActivity : ComponentActivity() {

    private val adapter = HistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qq_shuffle_play_history)
        findViewById<TextView>(R.id.lbd).setText(R.string.qq_shuffle_history_title)
        findViewById<View>(R.id.a54).setOnClickListener { finish() }
        findViewById<RecyclerView>(R.id.jd6).apply {
            layoutManager = LinearLayoutManager(this@QqMusicShufflePlayHistoryActivity)
            adapter = this@QqMusicShufflePlayHistoryActivity.adapter
            itemAnimator = null
        }
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val currentId = AudioPlaybackService.getCurrentQueueIds()
            .getOrNull(AudioPlaybackService.getCurrentTrackIndex())
        adapter.update(
            AudioPlaybackService.getShufflePlayHistory(this).asReversed(),
            currentId,
        )
        findViewById<View>(R.id.kwa).visibility = View.GONE
    }

    private inner class HistoryAdapter :
        RecyclerView.Adapter<HistoryAdapter.Holder>() {

        private val entries = mutableListOf<AudioPlaybackService.ShuffleHistoryEntry>()
        private var currentId: String? = null

        fun update(
            values: List<AudioPlaybackService.ShuffleHistoryEntry>,
            selectedId: String?,
        ) {
            entries.clear()
            entries.addAll(values)
            currentId = selectedId
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.qq_shuffle_play_history_item,
                    parent,
                    false,
                ),
            )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = entries[position]
            holder.title.text = entry.name
            holder.subtitle.text = ""
            holder.time.text = formatHistoryTime(entry.playedAt)
            val selected = position == 0 && entry.id == currentId
            holder.title.setTextColor(
                ContextCompat.getColor(
                    this@QqMusicShufflePlayHistoryActivity,
                    if (selected) {
                        R.color.common_song_list_tag_text_green
                    } else {
                        R.color.skin_text_main_color
                    },
                ),
            )
            holder.subtitle.setTextColor(
                ContextCompat.getColor(
                    this@QqMusicShufflePlayHistoryActivity,
                    if (selected) {
                        R.color.common_song_list_tag_text_green
                    } else {
                        R.color.skin_text_sub_color
                    },
                ),
            )
        }

        override fun getItemCount(): Int = entries.size

        inner class Holder(root: View) : RecyclerView.ViewHolder(root) {
            val title: TextView = root.findViewById(R.id.mif)
            val subtitle: TextView = root.findViewById(R.id.mie)
            val time: TextView = root.findViewById(R.id.mid)
        }
    }

    private fun formatHistoryTime(value: Long): String {
        if (value <= 0L) return ""
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = value }
        val pattern = when {
            now.get(Calendar.ERA) == target.get(Calendar.ERA) &&
                now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> "HH:mm"
            now.get(Calendar.ERA) == target.get(Calendar.ERA) &&
                now.get(Calendar.YEAR) == target.get(Calendar.YEAR) -> "MM月dd日 HH:mm"
            else -> "yyyy年MM月dd日 HH:mm"
        }
        return SimpleDateFormat(pattern, Locale.CHINA).format(Date(value))
    }
}
