package com.mengzhen.app.bilibili

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiliCacheMetadataParserTest {
    @Test
    fun `pc cache metadata and playurl identify the audio stream`() {
        val metadata = BiliCacheMetadataParser.parse(
            entryJson = null,
            videoInfoJson = """
                {
                  "bvid":"BV1FhdaYMEPh",
                  "cid":29374350340,
                  "p":1,
                  "title":"九十分钟纯享版",
                  "tabName":"正片",
                  "uname":"测试UP主",
                  "duration":5432,
                  "status":"completed",
                  "progress":100
                }
            """.trimIndent(),
            fallbackId = "fallback",
            fallbackTitle = "fallback",
        )
        val streams = BiliCacheMetadataParser.parsePlayUrl(
            """
                {
                  "data":{"dash":{"audio":[{
                    "id":30280,
                    "baseUrl":"https://example.test/29374350340-1-30280.m4s?token=1",
                    "mimeType":"audio/mp4",
                    "codecs":"mp4a.40.2"
                  }]}}
                }
            """.trimIndent(),
        )

        assertEquals("BV1FhdaYMEPh:29374350340:1", metadata.sourceId)
        assertEquals("九十分钟纯享版", metadata.title)
        assertEquals("正片", metadata.subtitle)
        assertEquals("测试UP主", metadata.owner)
        assertEquals(5432L, metadata.durationSeconds)
        assertTrue(metadata.completed)
        assertEquals("29374350340-1-30280.m4s", streams.single().fileName)
        assertEquals("mp4a.40.2", streams.single().codec)
    }

    @Test
    fun `android entry marks incomplete cache unavailable`() {
        val metadata = BiliCacheMetadataParser.parse(
            entryJson = """
                {
                  "title":"测试视频",
                  "avid":123,
                  "is_completed":false,
                  "page_data":{"cid":456,"page":2,"part":"第二集","duration":88}
                }
            """.trimIndent(),
            videoInfoJson = null,
            fallbackId = "fallback",
            fallbackTitle = "fallback",
        )

        assertEquals("123:456:2", metadata.sourceId)
        assertEquals("第二集", metadata.subtitle)
        assertFalse(metadata.completed)
    }

    @Test
    fun `m4s header accepts normal and nine zero prefixed iso bmff`() {
        val normal = byteArrayOf(0, 0, 0, 24) + "ftyp".toByteArray() + ByteArray(16)
        val prefixed = "000000000".toByteArray() + normal

        assertEquals(0, BiliM4sHeader.bytesToSkip(normal))
        assertEquals(9, BiliM4sHeader.bytesToSkip(prefixed))
        assertEquals(-1, BiliM4sHeader.bytesToSkip("not an mp4".toByteArray()))
    }
}
