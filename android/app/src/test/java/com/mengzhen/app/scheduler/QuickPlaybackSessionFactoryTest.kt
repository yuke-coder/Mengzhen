package com.mengzhen.app.scheduler

import com.mengzhen.app.data.model.PlaybackDraft
import com.mengzhen.app.data.model.TaskAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class QuickPlaybackSessionFactoryTest {

    @Test
    fun `creates one-off session from absolute play window`() {
        val start = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 29, 23, 10, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = start + 45 * 60_000L
        val draft = PlaybackDraft(
            audios = listOf(
                TaskAudio(id = "audio-1", name = "海浪", serverUrl = "https://example.test/a.mp3")
            ),
            volume = 64,
            fadeInDuration = 20,
            fadeOutDuration = 30,
            enableFade = true,
            enableFadeOut = true,
        )

        val task = QuickPlaybackSessionFactory.create(
            id = "${QUICK_PLAYBACK_TASK_PREFIX}test",
            startAt = start,
            endAt = end,
            draft = draft,
            createdAt = 123L,
        )

        assertTrue(task.isQuickPlaybackSession())
        assertEquals(45, task.playDurationMinutes)
        assertEquals(64, task.volume)
        assertEquals(20, task.fadeInDuration)
        assertEquals(30, task.fadeOutDuration)
        assertTrue(task.enableFadeOut)
        assertEquals(23, task.startTime.hour)
        assertEquals(10, task.startTime.minute)
        assertEquals(123L, task.createdAt)
        assertTrue(task.scheduleArmed)
    }

    @Test
    fun `disabled fade switches preserve editable durations`() {
        val start = 1_800_000_000_000L
        val draft = PlaybackDraft(
            fadeInDuration = 20,
            fadeOutDuration = 30,
            enableFade = false,
        )

        val task = QuickPlaybackSessionFactory.create(
            id = "${QUICK_PLAYBACK_TASK_PREFIX}test",
            startAt = start,
            endAt = start + 61_000L,
            draft = draft,
        )

        assertFalse(task.enableFade)
        assertFalse(task.enableFadeOut)
        assertEquals(20, task.fadeInDuration)
        assertEquals(30, task.fadeOutDuration)
        assertEquals(2, task.playDurationMinutes)
    }

    @Test
    fun `idle session is unarmed and preserves playback settings`() {
        val createdAt = 1_800_000_000_000L
        val audio = TaskAudio(
            id = "local-1",
            name = "夜雨",
            localUri = "content://audio/night-rain",
        )
        val draft = PlaybackDraft(
            audios = listOf(audio),
            volume = 38,
            fadeInDuration = 12,
            fadeOutDuration = 18,
            enableFade = true,
            enableFadeOut = true,
        )

        val task = QuickPlaybackSessionFactory.createIdle(
            id = "${QUICK_PLAYBACK_TASK_PREFIX}idle",
            draft = draft,
            createdAt = createdAt,
        )

        assertFalse(task.scheduleArmed)
        assertNull(task.nextExecuteAt)
        assertEquals(30, task.playDurationMinutes)
        assertEquals(38, task.volume)
        assertEquals(12, task.fadeInDuration)
        assertEquals(18, task.fadeOutDuration)
        assertTrue(task.enableFadeOut)
        assertEquals(listOf(audio), task.audios)
        assertEquals(createdAt + 5 * 60_000L, task.startTime.toEpochMillis())
    }
}
