package com.ximalaya.ting.android.main.playpage.playy.component.speed

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.mengzhen.app.R
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Runtime port of Ximalaya 9.4.95.3 SpeedScaleView.
 *
 * It retains the original 0.5x–3.0x / 0.1x scale, source item layouts,
 * centered LinearSnapHelper, five source shortcuts and haptic transitions.
 */
class SpeedScaleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener {

    var onSpeedSelected: ((Float) -> Unit)? = null
    var currentSpeed: Float = 1f
        private set

    private val speeds = (5..30).map { it / 10f }
    private val recycler: RecyclerView
    private val currentValue: TextView
    private val snapHelper = LinearSnapHelper()
    private var initialized = false
    private var userScrolling = false
    private var lastPosition = RecyclerView.NO_POSITION

    init {
        val root = LayoutInflater.from(context).inflate(
            R.layout.main_layout_speed_scale_view,
            this,
            true,
        )
        recycler = root.findViewById(R.id.main_rv_speed_scale)
        currentValue = root.findViewById(R.id.main_tv_speed_cur_value)

        listOf(
            R.id.main_tv_speed_icon_08,
            R.id.main_tv_speed_icon_10,
            R.id.main_tv_speed_icon_12,
            R.id.main_tv_speed_icon_15,
            R.id.main_tv_speed_icon_18,
        ).forEach { root.findViewById<View>(it).setOnClickListener(this) }

        recycler.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        recycler.adapter = SourceSpeedAdapter(speeds)
        snapHelper.attachToRecyclerView(recycler)
        recycler.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) userScrolling = true
                return false
            }
        })
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val position = snappedPosition() ?: return
                if (position != lastPosition) {
                    if (initialized) rv.performHapticFeedback(1)
                    lastPosition = position
                }
                currentValue.text = sourceSpeedText(speeds[position])
            }

            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                val position = snappedPosition() ?: return
                setSpeedInternal(speeds[position], notify = initialized && userScrolling)
                initialized = true
                userScrolling = false
            }
        })
        post { setSpeed(1f, notify = false) }
    }

    fun setSpeed(speed: Float, notify: Boolean = false) {
        val safe = ((speed.coerceIn(0.5f, 3f) * 10).roundToInt() / 10f)
        val position = speeds.indexOf(safe).coerceAtLeast(0)
        recycler.scrollToPosition(position + 1)
        recycler.post {
            val manager = recycler.layoutManager as? LinearLayoutManager
            val centerOffset = (recycler.width / 2) - context.dp(11)
            manager?.scrollToPositionWithOffset(position + 1, centerOffset)
            setSpeedInternal(safe, notify)
            initialized = true
        }
    }

    override fun onClick(view: View) {
        userScrolling = true
        val speed = when (view.id) {
            R.id.main_tv_speed_icon_08 -> 0.8f
            R.id.main_tv_speed_icon_10 -> 1.0f
            R.id.main_tv_speed_icon_12 -> 1.2f
            R.id.main_tv_speed_icon_15 -> 1.5f
            R.id.main_tv_speed_icon_18 -> 1.8f
            else -> return
        }
        view.performHapticFeedback(1)
        setSpeed(speed, notify = true)
    }

    private fun setSpeedInternal(speed: Float, notify: Boolean) {
        currentSpeed = speed
        currentValue.text = sourceSpeedText(speed)
        if (notify) onSpeedSelected?.invoke(speed)
    }

    private fun snappedPosition(): Int? {
        val manager = recycler.layoutManager ?: return null
        val view = snapHelper.findSnapView(manager) ?: return null
        return (recycler.getChildAdapterPosition(view) - 1)
            .takeIf { it in speeds.indices }
    }

    private fun sourceSpeedText(speed: Float): String =
        String.format(Locale.US, "%.1fx", speed)

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}

private class SourceSpeedAdapter(
    private val speeds: List<Float>,
) : RecyclerView.Adapter<SourceSpeedHolder>() {

    override fun getItemCount(): Int = speeds.size + 2

    override fun getItemViewType(position: Int): Int = when {
        position == 0 -> TYPE_EMPTY
        position == itemCount - 1 -> TYPE_EMPTY
        ((speeds[position - 1] * 10).roundToInt() % 5) == 0 -> TYPE_BIG
        else -> TYPE_SMALL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceSpeedHolder {
        val layout = when (viewType) {
            TYPE_BIG -> R.layout.main_item_speed_big_scale
            TYPE_SMALL -> R.layout.main_item_speed_small_scale
            else -> R.layout.main_item_speed_empty
        }
        return SourceSpeedHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: SourceSpeedHolder, position: Int) {
        if (getItemViewType(position) == TYPE_EMPTY) {
            holder.itemView.layoutParams = holder.itemView.layoutParams.apply {
                width = (holder.itemView.resources.displayMetrics.widthPixels -
                    holder.itemView.context.dpAdapter(32)) / 2
            }
            return
        }
        holder.itemView.findViewById<TextView>(R.id.main_tv_item_speed)?.apply {
            text = String.format(Locale.US, "%.1fx", speeds[position - 1])
            visibility = if (getItemViewType(position) == TYPE_BIG) View.VISIBLE else View.INVISIBLE
        }
    }

    private companion object {
        const val TYPE_BIG = 1
        const val TYPE_SMALL = 2
        const val TYPE_EMPTY = 3
    }
}

private class SourceSpeedHolder(view: View) : RecyclerView.ViewHolder(view)

private fun Context.dpAdapter(value: Int): Int =
    (value * resources.displayMetrics.density + 0.5f).toInt()
