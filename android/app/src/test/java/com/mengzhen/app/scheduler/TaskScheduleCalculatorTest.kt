package com.mengzhen.app.scheduler

import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.ScheduledStopMode
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.model.TaskPhase
import com.mengzhen.app.data.model.TaskRepeatType
import com.mengzhen.app.data.model.TaskStartTime
import com.mengzhen.app.data.model.TaskStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TaskScheduleCalculatorTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun onceTaskReturnsExactFutureTimeAndRejectsPastTime() {
        val executeAt = time(2026, 8, 1, 22, 30)
        val task = task(
            repeatType = TaskRepeatType.ONCE,
            startTime = TaskStartTime(2026, 8, 1, 22, 30),
        )

        assertEquals(executeAt, TaskScheduleCalculator.nextExecuteAt(task, executeAt - 1_000))
        assertNull(TaskScheduleCalculator.nextExecuteAt(task, executeAt + 1_000))
    }

    @Test
    fun dailyTaskUsesTodayBeforeTheTimeAndTomorrowAfterIt() {
        val task = task(
            repeatType = TaskRepeatType.DAILY,
            startTime = TaskStartTime(hour = 8, minute = 0),
        )

        assertEquals(
            time(2026, 7, 29, 8, 0),
            TaskScheduleCalculator.nextExecuteAt(task, time(2026, 7, 29, 7, 0)),
        )
        assertEquals(
            time(2026, 7, 30, 8, 0),
            TaskScheduleCalculator.nextExecuteAt(task, time(2026, 7, 29, 8, 1)),
        )
    }

    @Test
    fun workdayTaskIncludesOfficialWeekendMakeupDay() {
        val task = task(
            repeatType = TaskRepeatType.WORKDAY,
            startTime = TaskStartTime(hour = 8, minute = 0),
        )

        assertEquals(
            time(2026, 2, 15, 8, 0),
            TaskScheduleCalculator.nextExecuteAt(task, time(2026, 2, 14, 9, 0)),
        )
    }

    @Test
    fun holidayTaskIncludesWeekend() {
        val task = task(
            repeatType = TaskRepeatType.HOLIDAY,
            startTime = TaskStartTime(hour = 8, minute = 0),
        )

        assertEquals(
            time(2026, 2, 21, 8, 0),
            TaskScheduleCalculator.nextExecuteAt(task, time(2026, 2, 20, 9, 0)),
        )
    }

    @Test
    fun audioAndFadeInStartAtTheConfiguredTime() {
        val task = task(
            enableFade = true,
            fadeInDuration = 90,
        )
        val executeAt = time(2026, 7, 29, 22, 0)

        assertEquals(executeAt, TaskScheduleCalculator.alarmAt(task, executeAt))
    }

    @Test
    fun alarmDoesNotStartEarlyWhenLightWakeupIsOff() {
        val task = task(
            enableFade = false,
            fadeInDuration = 90,
        )
        val executeAt = time(2026, 7, 29, 22, 0)

        assertEquals(executeAt, TaskScheduleCalculator.alarmAt(task, executeAt))
    }

    @Test
    fun runtimeStateMovesThroughFadeInPlayingAndFadeOut() {
        val task = task(
            status = TaskStatus.EXECUTING,
            enableFade = true,
            enableFadeOut = true,
            fadeInDuration = 10,
            fadeOutDuration = 20,
            executionStartedAt = 10_000L,
            executionEndsAt = 100_000L,
        )

        val fadeIn = TaskScheduleCalculator.runtimeState(task, 15_000L)
        assertEquals(TaskPhase.FADING_IN, fadeIn.phase)
        assertEquals(5_000L, fadeIn.remainingMs)

        val playing = TaskScheduleCalculator.runtimeState(task, 30_000L)
        assertEquals(TaskPhase.PLAYING, playing.phase)
        assertEquals(50_000L, playing.remainingMs)

        val fadeOut = TaskScheduleCalculator.runtimeState(task, 90_000L)
        assertEquals(TaskPhase.FADING_OUT, fadeOut.phase)
        assertEquals(10_000L, fadeOut.remainingMs)
    }

    @Test
    fun fadeInAndFadeOutSwitchesAreIndependent() {
        val fadeInOnly = task(
            status = TaskStatus.EXECUTING,
            enableFade = true,
            enableFadeOut = false,
            fadeInDuration = 10,
            fadeOutDuration = 20,
            executionStartedAt = 10_000L,
            executionEndsAt = 100_000L,
        )
        val fadeOutOnly = fadeInOnly.copy(
            enableFade = false,
            enableFadeOut = true,
        )

        assertEquals(
            TaskPhase.PLAYING,
            TaskScheduleCalculator.runtimeState(fadeInOnly, 90_000L).phase,
        )
        assertEquals(
            TaskPhase.FADING_OUT,
            TaskScheduleCalculator.runtimeState(fadeOutOnly, 90_000L).phase,
        )
    }

    @Test
    fun scheduledPlaybackWithoutStopHasNoForcedDeadline() {
        val task = task()

        assertNull(TaskScheduleCalculator.scheduledStopDeadlineAt(task, 10_000L))
    }

    @Test
    fun scheduledMinuteStopStartsCountingFromActualPlaybackStart() {
        val task = ScheduledTask(
            scheduledStopMode = ScheduledStopMode.MINUTES,
            scheduledStopValue = 15,
        )

        assertEquals(
            910_000L,
            TaskScheduleCalculator.scheduledStopDeadlineAt(task, 10_000L),
        )
    }

    @Test
    fun exactPlaybackDurationEndsAtTheSameExactTime() {
        val task = ScheduledTask(
            scheduledStopMode = ScheduledStopMode.MINUTES,
            scheduledStopValue = 16,
            scheduledStopDurationSeconds = 901,
        )

        assertEquals(
            911_000L,
            TaskScheduleCalculator.scheduledStopDeadlineAt(task, 10_000L),
        )
    }

    @Test
    fun endClockConvertsDirectlyToPlaybackDuration() {
        val startedAt = time(2026, 8, 3, 22, 0)

        assertEquals(
            30 * 60,
            TaskScheduleCalculator.durationSecondsUntilClock(
                playbackStartedAt = startedAt,
                afterMillis = startedAt,
                hour = 22,
                minute = 30,
            ),
        )
        assertEquals(
            23 * 60 * 60 + 30 * 60,
            TaskScheduleCalculator.durationSecondsUntilClock(
                playbackStartedAt = startedAt,
                afterMillis = startedAt,
                hour = 21,
                minute = 30,
            ),
        )
    }

    @Test
    fun naturalRecoveryWindowUsesSelectedAudioOrderInsteadOfLegacyDuration() {
        val task = ScheduledTask(
            enableFade = false,
            audios = listOf(
                TaskAudio(id = "one", duration = 60),
                TaskAudio(id = "two", duration = 120),
            ),
            alarmAudioOrder = listOf("two", "one"),
            scheduledStopMode = ScheduledStopMode.NONE,
            playDurationMinutes = 30,
        )

        assertEquals(
            190_000L,
            TaskScheduleCalculator.occurrenceRecoveryEndAt(task, 10_000L),
        )
    }

    @Test
    fun executingNaturalPlaybackReportsPlayingWithoutArtificialThirtyMinuteEnd() {
        val task = task(
            status = TaskStatus.EXECUTING,
            executionStartedAt = 10_000L,
            executionEndsAt = null,
        )

        val state = TaskScheduleCalculator.runtimeState(task, 20_000L)

        assertEquals(TaskPhase.PLAYING, state.phase)
        assertEquals(0L, state.remainingMs)
    }

    private fun task(
        repeatType: TaskRepeatType = TaskRepeatType.DAILY,
        startTime: TaskStartTime = TaskStartTime(hour = 8, minute = 0),
        status: TaskStatus = TaskStatus.PENDING,
        enableFade: Boolean = false,
        enableFadeOut: Boolean = false,
        fadeInDuration: Int = 0,
        fadeOutDuration: Int = 0,
        executionStartedAt: Long? = null,
        executionEndsAt: Long? = null,
    ) = ScheduledTask(
        id = "test",
        name = "测试任务",
        repeatType = repeatType,
        startTime = startTime,
        status = status,
        enableFade = enableFade,
        enableFadeOut = enableFadeOut,
        fadeInDuration = fadeInDuration,
        fadeOutDuration = fadeOutDuration,
        executionStartedAt = executionStartedAt,
        executionEndsAt = executionEndsAt,
    )

    private fun time(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = Calendar.getInstance().apply {
        set(year, month - 1, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
