package com.mengzhen.app.data.store

import com.mengzhen.app.data.model.TaskAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioNoteIdentityTest {
    @Test
    fun `durable id keeps note identity stable while upload metadata changes`() {
        val selected = TaskAudio(
            id = "audio-42",
            name = "示例.mp3",
            localUri = "content://selected/42",
        )
        val uploaded = selected.copy(
            fileKey = "uploads/42.mp3",
            serverUrl = "https://example.invalid/42.mp3",
            savedToLibrary = true,
        )

        assertEquals(selected.audioNoteIdentity(), uploaded.audioNoteIdentity())
    }

    @Test
    fun `same display name from different local files does not share notes`() {
        val first = TaskAudio(name = "同名音频.mp3", localUri = "content://audio/first")
        val second = TaskAudio(name = "同名音频.mp3", localUri = "content://audio/second")

        assertNotEquals(first.audioNoteIdentity(), second.audioNoteIdentity())
    }

    @Test
    fun `source identity covers extracted cache entries without a local id`() {
        val audio = TaskAudio(
            name = "缓存音频",
            sourceType = "bilibili",
            sourceId = "BV1-test-page-1",
        )

        assertEquals("source:bilibili:BV1-test-page-1", audio.audioNoteIdentity())
    }

    @Test
    fun `empty placeholder cannot receive a note`() {
        assertNull(TaskAudio().audioNoteIdentity())
    }
}
