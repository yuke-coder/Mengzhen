package com.mengzhen.app.data.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsJsonTest {

    @Test
    fun `audio JSON round trip preserves local and cloud metadata`() {
        val audio = TaskAudio(
            id = "audio-1",
            name = "海浪",
            duration = 123_000L,
            size = 456L,
            localUri = "content://audio/1",
            fileKey = "user/audio.mp3",
            serverUrl = "https://example.test/audio.mp3",
            dbKey = "db-1",
            savedToLibrary = true,
            mimeType = "audio/mpeg",
            sourceType = "bilibili",
            sourceId = "BV1test:123",
            artist = "测试UP主",
            artworkUri = "file:///data/user/0/app/files/cover.jpg",
            createdAt = "2026-07-29T12:00:00Z",
        )

        assertEquals(audio, audio.toJson().toTaskAudio())
    }

    @Test
    fun `scheduled task JSON round trip preserves armed state and local uri`() {
        val task = ScheduledTask(
            id = "quick_playback_test",
            audios = listOf(
                TaskAudio(
                    id = "audio-1",
                    name = "雨声",
                    localUri = "file:///data/user/0/app/files/rain.mp3",
                )
            ),
            alarmAudioOrder = listOf("audio-1"),
            scheduledStopMode = ScheduledStopMode.TRACKS,
            scheduledStopValue = 2,
            scheduledFinishCurrentTrack = true,
            fadeOutDuration = 18,
            enableFadeOut = true,
            scheduleArmed = false,
        )

        val restored = task.toJson().toScheduledTask()

        assertFalse(restored.scheduleArmed)
        assertEquals(task.audios, restored.audios)
        assertEquals(task.alarmAudioOrder, restored.alarmAudioOrder)
        assertEquals(ScheduledStopMode.TRACKS, restored.scheduledStopMode)
        assertEquals(2, restored.scheduledStopValue)
        assertTrue(restored.scheduledFinishCurrentTrack)
        assertTrue(restored.enableFadeOut)
        assertEquals(18, restored.fadeOutDuration)
    }

    @Test
    fun `legacy task without armed field remains armed`() {
        val legacy = JSONObject()
            .put("id", "legacy-task")
            .put("status", "pending")

        assertTrue(legacy.toScheduledTask().scheduleArmed)
    }

    @Test
    fun `exact stop duration round trips and old minute tasks still work`() {
        val exact = ScheduledTask(
            scheduledStopMode = ScheduledStopMode.MINUTES,
            scheduledStopValue = 16,
            scheduledStopDurationSeconds = 901,
        ).toJson().toScheduledTask()
        val legacy = JSONObject()
            .put("scheduledStopMode", "minutes")
            .put("scheduledStopValue", 15)
            .toScheduledTask()

        assertEquals(901, exact.effectiveScheduledStopDurationSeconds())
        assertEquals(900, legacy.effectiveScheduledStopDurationSeconds())
    }

    @Test
    fun `auth user parser keeps the complete me response`() {
        val user = parseUser(
            JSONObject()
                .put("success", true)
                .put("authenticated", true)
                .put(
                    "user",
                    JSONObject()
                        .put("id", "42")
                        .put("username", "余客")
                        .put("nickname", "梦枕")
                        .put("avatar_url", "https://example.test/avatar.jpg")
                        .put("gender", "secret")
                        .put("birthday", "2000-01-02")
                        .put("location", "上海")
                        .put("bio", "简介")
                        .put("signature", "签名")
                        .put("createdAt", "2026-08-05T00:00:00Z"),
                ),
        )

        assertEquals("42", user?.id)
        assertEquals("余客", user?.username)
        assertEquals("梦枕", user?.nickname)
        assertEquals("https://example.test/avatar.jpg", user?.avatarUrl)
        assertEquals("2026-08-05T00:00:00Z", user?.createdAt)
    }

    @Test
    fun `auth user parser rejects an unauthenticated response`() {
        val response = JSONObject()
            .put("success", true)
            .put("authenticated", false)
            .put("user", JSONObject.NULL)

        assertEquals(null, parseUser(response))
    }
}
