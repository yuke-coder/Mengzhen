package com.mengzhen.app.ui.screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.model.selectionKey
import com.mengzhen.app.data.store.TaskStore

/**
 * AlarmRingSettingFragmentNew 的页面结构，候选内容适配为用户当前已选择的音频。
 * 音频可多选；点击选中的先后顺序就是定时启播顺序。
 */
@Composable
fun XimalayaAlarmRingScreen(
    navController: NavController,
    taskId: String,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { TaskStore.get(context) }
    val task = remember(taskId) { store.getTaskById(taskId) }
    val candidates = remember(taskId) { alarmAudioCandidates(store, task) }
    val initialOrder = remember(taskId) { initialAlarmAudioOrder(task) }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.main_fra_alarm_ring_setting_v2, null, false)
                .also { root ->
                    root.setBackgroundResource(R.color.xm_alarm_v9514_0x7f060e75)
                    val adapter = SourceAlarmRingAdapter(candidates, initialOrder)
                    installXimalayaTitleBar(
                        host = root.findViewById(R.id.main_title_bar),
                        title = "播放内容",
                        left = XimalayaTitleAction.Back {
                            navController.popBackStack()
                        },
                        rightText = "完成",
                        onRight = {
                            val selectedOrder = adapter.selectedAudioKeysInOrder()
                            store.updateTask(taskId) {
                                it.copy(
                                    audios = adapter.allAudiosInOrder(),
                                    alarmAudioIndex = if (selectedOrder.isEmpty()) null else 0,
                                    alarmAudioOrder = selectedOrder,
                                    updatedAt = System.currentTimeMillis(),
                                )
                            }
                            navController.popBackStack()
                        },
                    )
                    root.findViewById<RecyclerView>(R.id.main_rv_alarm_ring_setting).apply {
                        layoutManager = LinearLayoutManager(viewContext)
                        itemAnimator = null
                        this.adapter = adapter
                    }
                }
        },
    )
}

private fun alarmAudioCandidates(
    store: TaskStore,
    task: ScheduledTask?,
): List<TaskAudio> {
    val seen = mutableSetOf<String>()
    return buildList {
        (store.getDraft().audios + task.orEmptyAudios()).forEach { audio ->
            if (seen.add(audio.selectionKey())) add(audio)
        }
    }
}

private fun ScheduledTask?.orEmptyAudios(): List<TaskAudio> = this?.audios.orEmpty()

private fun initialAlarmAudioOrder(task: ScheduledTask?): List<String> {
    if (task == null) return emptyList()
    if (task.alarmAudioOrder.isNotEmpty()) return task.alarmAudioOrder
    return task.alarmAudioIndex
        ?.let(task.audios::getOrNull)
        ?.selectionKey()
        ?.let(::listOf)
        .orEmpty()
}

private data class SourceAlarmRingItem(
    val title: String?,
    val name: String,
    val audio: TaskAudio?,
)

private class SourceAlarmRingAdapter(
    candidates: List<TaskAudio>,
    initialOrder: List<String>,
) : RecyclerView.Adapter<SourceAlarmRingAdapter.Holder>() {

    private val audios = candidates.toList()
    private val selectedKeys = initialOrder
        .distinct()
        .filter { selected -> audios.any { it.selectionKey() == selected } }
        .toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.main_item_alarm_ring_setting, parent, false),
        )

    override fun getItemCount(): Int = audios.size + 1

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = itemAt(position)
        holder.title.apply {
            text = item.title.orEmpty()
            visibility = if (item.title == null) View.GONE else View.VISIBLE
        }
        holder.name.text = item.name
        holder.cover.visibility = View.VISIBLE
        holder.cover.setImageResource(R.drawable.xm_alarm_v9514_0x7f0807ce)

        val key = item.audio?.selectionKey()
        val selectedPosition = key?.let(selectedKeys::indexOf)
            ?.takeIf { it >= 0 }
        val selected = if (key == null) selectedKeys.isEmpty() else key in selectedKeys
        holder.check.visibility = if (key == null && selected) View.VISIBLE else View.INVISIBLE
        holder.right.apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            text = selectedPosition?.let { (it + 1).toString() }.orEmpty()
            visibility = if (selectedPosition == null) View.INVISIBLE else View.VISIBLE
        }
        holder.itemView.setOnClickListener {
            if (key == null) {
                selectedKeys.clear()
            } else {
                val existing = selectedKeys.indexOf(key)
                if (existing >= 0) {
                    selectedKeys.removeAt(existing)
                } else {
                    selectedKeys.add(key)
                }
            }
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun selectedAudioKeysInOrder(): List<String> =
        selectedKeys.toList()

    fun allAudiosInOrder(): List<TaskAudio> = audios.toList()

    private fun itemAt(position: Int): SourceAlarmRingItem = if (position == 0) {
        SourceAlarmRingItem(
            title = "继续播放",
            name = "续播上一次收听",
            audio = null,
        )
    } else {
        val audio = audios[position - 1]
        SourceAlarmRingItem(
            title = if (position == 1) "推荐内容（点击顺序即播放顺序）" else null,
            name = audio.name.ifBlank { "音频" },
            audio = audio,
        )
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.main_tv_title)
        val cover: ImageView = itemView.findViewById(R.id.main_iv_cover)
        val name: TextView = itemView.findViewById(R.id.main_tv_name)
        val check: ImageView = itemView.findViewById(R.id.main_iv_check)
        val right: TextView = itemView.findViewById(R.id.main_tv_right)
    }
}
