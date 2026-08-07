package com.mengzhen.app.scheduler

import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.ScheduledStopMode
import com.mengzhen.app.data.model.TaskPhase
import com.mengzhen.app.data.model.TaskRepeatType
import com.mengzhen.app.data.model.TaskStatus
import com.mengzhen.app.data.model.effectiveScheduledStopDurationSeconds
import com.mengzhen.app.data.model.isOneShotSchedule
import com.mengzhen.app.data.model.selectionKey
import java.util.Calendar

data class TaskRuntimeState(
    val status: TaskStatus,
    val phase: TaskPhase,
    val remainingMs: Long,
)

/**
 * 纯时间计算层。AlarmManager、设置页倒计时和启动恢复都通过这里计算，
 * 避免各自维护一套略有差异的时间规则。
 */
object TaskScheduleCalculator {

    fun nextExecuteAt(
        task: ScheduledTask,
        fromMillis: Long = System.currentTimeMillis(),
    ): Long? {
        if (task.isOneShotSchedule()) {
            return task.startTime.toEpochMillis().takeIf { it >= fromMillis }
        }

        val candidate = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, task.startTime.hour.coerceIn(0, 23))
            set(Calendar.MINUTE, task.startTime.minute.coerceIn(0, 59))
            set(Calendar.SECOND, task.startTime.second.coerceIn(0, 59))
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < fromMillis) add(Calendar.DAY_OF_YEAR, 1)
        }

        repeat(367) {
            if (matches(task, candidate)) return candidate.timeInMillis
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    /** 用户设置的启播时刻就是音频与渐强同时开始的时刻。 */
    fun alarmAt(task: ScheduledTask, executeAt: Long): Long = executeAt

    fun scheduledStopDeadlineAt(task: ScheduledTask, playbackStartedAt: Long): Long? =
        task.effectiveScheduledStopDurationSeconds()
            .takeIf { it > 0 }
            ?.let { playbackStartedAt + it * 1_000L }

    fun durationSecondsUntilClock(
        playbackStartedAt: Long,
        afterMillis: Long,
        hour: Int,
        minute: Int,
    ): Int {
        val notBefore = maxOf(playbackStartedAt, afterMillis)
        val target = Calendar.getInstance().apply {
            timeInMillis = notBefore
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= notBefore) add(Calendar.DAY_OF_YEAR, 1)
        }
        return ((target.timeInMillis - playbackStartedAt) / 1_000L)
            .coerceIn(1L, Int.MAX_VALUE.toLong())
            .toInt()
    }

    /**
     * Recovery window only; this value is never used as a forced playback deadline.
     * With no explicit timer, the selected playlist is allowed to finish naturally.
     */
    fun occurrenceRecoveryEndAt(task: ScheduledTask, executeAt: Long): Long {
        val playbackStartsAt = alarmAt(task, executeAt)
        val naturalEnd = naturalPlaybackDurationMs(task)?.let(playbackStartsAt::plus)
        val timedEnd = scheduledStopDeadlineAt(task, playbackStartsAt)
        return when {
            timedEnd != null && !task.scheduledFinishCurrentTrack -> timedEnd
            timedEnd != null && naturalEnd != null -> maxOf(timedEnd, naturalEnd)
            timedEnd != null -> timedEnd + UNKNOWN_DURATION_RECOVERY_MS
            naturalEnd != null -> naturalEnd
            else -> playbackStartsAt + UNKNOWN_DURATION_RECOVERY_MS
        }
    }

    fun executionRecoveryEndAt(task: ScheduledTask): Long? {
        val startedAt = task.executionStartedAt ?: task.lastExecutedAt ?: return null
        val naturalEnd = naturalPlaybackDurationMs(task)?.let(startedAt::plus)
        val timedEnd = task.executionEndsAt
        return when {
            timedEnd != null && !task.scheduledFinishCurrentTrack -> timedEnd
            timedEnd != null && naturalEnd != null -> maxOf(timedEnd, naturalEnd)
            timedEnd != null -> timedEnd + UNKNOWN_DURATION_RECOVERY_MS
            naturalEnd != null -> naturalEnd
            else -> startedAt + UNKNOWN_DURATION_RECOVERY_MS
        }
    }

    fun runtimeState(
        task: ScheduledTask,
        now: Long = System.currentTimeMillis(),
    ): TaskRuntimeState {
        if (task.status == TaskStatus.CANCELLED) {
            return TaskRuntimeState(TaskStatus.CANCELLED, TaskPhase.IDLE, 0)
        }

        task.skipUntil?.takeIf { it > now }?.let {
            return TaskRuntimeState(TaskStatus.CANCELLED, TaskPhase.IDLE, it - now)
        }

        if (task.status == TaskStatus.EXECUTING) {
            val startedAt = task.executionStartedAt ?: task.lastExecutedAt
            val endsAt = task.executionEndsAt
            if (startedAt != null && endsAt != null && now < endsAt) {
                val fadeInEnd = if (task.enableFade) {
                    (startedAt + task.fadeInDuration.coerceAtLeast(0) * 1_000L)
                        .coerceAtMost(endsAt)
                } else {
                    startedAt
                }
                val fadeOutStart = if (task.enableFadeOut) {
                    (endsAt - task.fadeOutDuration.coerceAtLeast(0) * 1_000L)
                        .coerceAtLeast(fadeInEnd)
                } else {
                    endsAt
                }
                return when {
                    now < fadeInEnd -> TaskRuntimeState(
                        TaskStatus.EXECUTING,
                        TaskPhase.FADING_IN,
                        fadeInEnd - now,
                    )
                    now >= fadeOutStart -> TaskRuntimeState(
                        TaskStatus.EXECUTING,
                        TaskPhase.FADING_OUT,
                        endsAt - now,
                    )
                    else -> TaskRuntimeState(
                        TaskStatus.EXECUTING,
                        TaskPhase.PLAYING,
                        fadeOutStart - now,
                    )
                }
            }
            if (startedAt != null && endsAt == null) {
                val fadeInEnd = if (task.enableFade) {
                    startedAt + task.fadeInDuration.coerceAtLeast(0) * 1_000L
                } else {
                    startedAt
                }
                return if (now < fadeInEnd) {
                    TaskRuntimeState(
                        TaskStatus.EXECUTING,
                        TaskPhase.FADING_IN,
                        fadeInEnd - now,
                    )
                } else {
                    TaskRuntimeState(TaskStatus.EXECUTING, TaskPhase.PLAYING, 0)
                }
            }
        }

        if (task.status == TaskStatus.COMPLETED) {
            return TaskRuntimeState(TaskStatus.COMPLETED, TaskPhase.IDLE, 0)
        }

        val nextAt = task.nextExecuteAt ?: nextExecuteAt(task, now)
        val remaining = nextAt?.let { (alarmAt(task, it) - now).coerceAtLeast(0) } ?: 0
        return TaskRuntimeState(TaskStatus.PENDING, TaskPhase.WAITING, remaining)
    }

    private fun matches(task: ScheduledTask, calendar: Calendar): Boolean {
        task.repeatDays?.let { repeatDays ->
            if (repeatDays !in 1..127) return false
            val mondayFirstIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> return false
            }
            return repeatDays and (1 shl mondayFirstIndex) != 0
        }
        return when (task.repeatType) {
            TaskRepeatType.DAILY -> true
            TaskRepeatType.WORKDAY -> ChineseHolidayCalendar.isWorkday(calendar)
            TaskRepeatType.HOLIDAY -> ChineseHolidayCalendar.isHolidayOrWeekend(calendar)
            TaskRepeatType.ONCE -> false
        }
    }

    private fun naturalPlaybackDurationMs(task: ScheduledTask): Long? {
        val ordered = if (task.alarmAudioOrder.isNotEmpty()) {
            val byKey = task.audios.associateBy { it.selectionKey() }
            task.alarmAudioOrder.mapNotNull(byKey::get)
        } else {
            task.audios
        }
        val limited = if (
            task.scheduledStopMode == ScheduledStopMode.TRACKS &&
            task.scheduledStopValue > 0
        ) {
            ordered.take(task.scheduledStopValue)
        } else {
            ordered
        }
        if (limited.isEmpty() || limited.any { it.duration <= 0L }) return null
        val skippedPerTrack = task.skipHeadSeconds.coerceAtLeast(0) +
            task.skipTailSeconds.coerceAtLeast(0)
        val seconds = limited.sumOf { audio ->
            (audio.duration - skippedPerTrack).coerceAtLeast(1L)
        }
        return seconds * 1_000L
    }

    private const val UNKNOWN_DURATION_RECOVERY_MS = 24 * 60 * 60 * 1_000L
}
