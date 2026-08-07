package com.mengzhen.app.scheduler

import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.data.model.PlaybackDraft
import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.TaskRepeatType
import com.mengzhen.app.data.model.TaskStartTime
import com.mengzhen.app.data.model.TaskStatus
import com.mengzhen.app.data.store.TaskStore
import java.util.Calendar

const val QUICK_PLAYBACK_TASK_PREFIX = "quick_playback_"

fun ScheduledTask.isQuickPlaybackSession(): Boolean =
    id.startsWith(QUICK_PLAYBACK_TASK_PREFIX)

object QuickPlaybackSessionFactory {

    private const val IDLE_START_DELAY_MILLIS = 5 * 60_000L
    private const val IDLE_PLAY_DURATION_MILLIS = 30 * 60_000L

    fun create(
        id: String,
        startAt: Long,
        endAt: Long,
        draft: PlaybackDraft,
        createdAt: Long = System.currentTimeMillis(),
    ): ScheduledTask {
        require(id.startsWith(QUICK_PLAYBACK_TASK_PREFIX))
        require(endAt > startAt)

        val start = Calendar.getInstance().apply { timeInMillis = startAt }
        val durationMinutes = ((endAt - startAt + 59_999L) / 60_000L)
            .coerceIn(1L, 1_440L)
            .toInt()

        return ScheduledTask(
            id = id,
            name = "本次助眠",
            startTime = TaskStartTime(
                year = start.get(Calendar.YEAR),
                month = start.get(Calendar.MONTH) + 1,
                day = start.get(Calendar.DAY_OF_MONTH),
                hour = start.get(Calendar.HOUR_OF_DAY),
                minute = start.get(Calendar.MINUTE),
                second = start.get(Calendar.SECOND),
            ),
            playDurationMinutes = durationMinutes,
            fadeInDuration = draft.fadeInDuration,
            fadeOutDuration = draft.fadeOutDuration,
            enableFade = draft.enableFade,
            enableFadeOut = draft.enableFadeOut,
            volume = draft.volume.coerceIn(0, 100),
            repeatType = TaskRepeatType.ONCE,
            audios = draft.audios,
            scheduleArmed = true,
            status = TaskStatus.PENDING,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    /**
     * Creates the player session shown immediately after selecting audio.
     *
     * The future window gives the timer sheet useful defaults, while scheduleArmed=false
     * guarantees that opening the player never starts or schedules playback by itself.
     */
    fun createIdle(
        id: String,
        draft: PlaybackDraft,
        createdAt: Long = System.currentTimeMillis(),
    ): ScheduledTask {
        val startAt = createdAt + IDLE_START_DELAY_MILLIS
        return create(
            id = id,
            startAt = startAt,
            endAt = startAt + IDLE_PLAY_DURATION_MILLIS,
            draft = draft,
            createdAt = createdAt,
        ).copy(
            scheduleArmed = false,
            nextExecuteAt = null,
        )
    }

    fun save(store: TaskStore, session: ScheduledTask): ScheduledTask {
        require(session.isQuickPlaybackSession())
        store.cleanupTransientPlaybackSessions(AudioPlaybackService.getCurrentTaskId())
        return store.saveTask(session)
    }

    fun newId(now: Long = System.currentTimeMillis()): String =
        QUICK_PLAYBACK_TASK_PREFIX + now.toString(36) + "_" +
            (0..9999).random().toString(36)
}
