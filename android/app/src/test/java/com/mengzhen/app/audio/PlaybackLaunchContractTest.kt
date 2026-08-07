package com.mengzhen.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackLaunchContractTest {

    @Test
    fun `content and file sources bypass network download`() {
        assertTrue(PlaybackLaunchContract.isDirectSource("content://media/external/audio/1"))
        assertTrue(PlaybackLaunchContract.isDirectSource("file:///data/user/0/audio.mp3"))
        assertTrue(PlaybackLaunchContract.isDirectSource("/data/user/0/audio.mp3"))
        assertTrue(PlaybackLaunchContract.isDirectSource("android.resource://com.mengzhen.app/1"))
    }

    @Test
    fun `http sources use network path`() {
        assertFalse(PlaybackLaunchContract.isDirectSource("https://example.com/audio.mp3"))
        assertFalse(PlaybackLaunchContract.isDirectSource("HTTP://example.com/audio.mp3"))
    }

    @Test
    fun `earliest positive deadline preserves scheduled hard stop`() {
        assertEquals(1_000L, PlaybackLaunchContract.earliestPositiveDeadline(1_000L, 2_000L))
        assertEquals(2_000L, PlaybackLaunchContract.earliestPositiveDeadline(0L, 2_000L))
        assertEquals(0L, PlaybackLaunchContract.earliestPositiveDeadline(0L, 0L))
    }

    @Test
    fun `finish-current-track applies only when sleep timer is effective deadline`() {
        assertTrue(
            PlaybackLaunchContract.shouldFinishCurrentTrack(
                executionEnd = 0L,
                sleepEnd = 2_000L,
                finishCurrentTrack = true,
            )
        )
        assertTrue(
            PlaybackLaunchContract.shouldFinishCurrentTrack(
                executionEnd = 3_000L,
                sleepEnd = 2_000L,
                finishCurrentTrack = true,
            )
        )
        assertFalse(
            PlaybackLaunchContract.shouldFinishCurrentTrack(
                executionEnd = 1_000L,
                sleepEnd = 2_000L,
                finishCurrentTrack = true,
            )
        )
        assertFalse(
            PlaybackLaunchContract.shouldFinishCurrentTrack(
                executionEnd = 2_000L,
                sleepEnd = 2_000L,
                finishCurrentTrack = true,
            )
        )
        assertFalse(
            PlaybackLaunchContract.shouldFinishCurrentTrack(
                executionEnd = 0L,
                sleepEnd = 2_000L,
                finishCurrentTrack = false,
            )
        )
    }

    @Test
    fun `scheduled playlist stops after its final selected audio`() {
        assertFalse(PlaybackLaunchContract.shouldStopAtPlaylistEnd(true, 0, 1))
        assertTrue(PlaybackLaunchContract.shouldStopAtPlaylistEnd(true, 1, 1))
        assertFalse(PlaybackLaunchContract.shouldStopAtPlaylistEnd(false, 1, 1))
    }
}
