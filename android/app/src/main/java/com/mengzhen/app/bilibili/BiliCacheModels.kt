package com.mengzhen.app.bilibili

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class BiliCacheAccessMode {
    DOCUMENT,
    ROOT,
    SHIZUKU,
    NETWORK,
}

data class BiliCacheItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val owner: String = "",
    val durationSeconds: Long = 0,
    val audioSize: Long = 0,
    val mimeType: String = "audio/mp4",
    val codec: String = "",
    val audioLocation: String,
    val coverLocation: String? = null,
    val accessMode: BiliCacheAccessMode,
    val completed: Boolean = true,
) {
    fun displayTitle(): String = when {
        subtitle.isBlank() || subtitle == title -> title
        else -> "$title · $subtitle"
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("subtitle", subtitle)
        put("owner", owner)
        put("durationSeconds", durationSeconds)
        put("audioSize", audioSize)
        put("mimeType", mimeType)
        put("codec", codec)
        put("audioLocation", audioLocation)
        coverLocation?.let { put("coverLocation", it) }
        put("accessMode", accessMode.name)
        put("completed", completed)
    }

    companion object {
        fun fromJson(json: JSONObject): BiliCacheItem = BiliCacheItem(
            id = json.optString("id"),
            title = json.optString("title", "B站缓存"),
            subtitle = json.optString("subtitle"),
            owner = json.optString("owner"),
            durationSeconds = json.optLong("durationSeconds"),
            audioSize = json.optLong("audioSize"),
            mimeType = json.optString("mimeType", "audio/mp4"),
            codec = json.optString("codec"),
            audioLocation = json.optString("audioLocation"),
            coverLocation = json.optString("coverLocation").ifBlank { null },
            accessMode = runCatching {
                BiliCacheAccessMode.valueOf(json.optString("accessMode"))
            }.getOrDefault(BiliCacheAccessMode.DOCUMENT),
            completed = json.optBoolean("completed", true),
        )

        fun listFromJson(raw: String): List<BiliCacheItem> {
            val array = JSONArray(raw)
            return (0 until array.length()).mapNotNull { index ->
                runCatching { fromJson(array.getJSONObject(index)) }.getOrNull()
            }
        }

        fun listToJson(items: List<BiliCacheItem>): String =
            JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()
    }
}

internal data class BiliCacheMetadata(
    val sourceId: String,
    val title: String,
    val subtitle: String,
    val owner: String,
    val durationSeconds: Long,
    val completed: Boolean,
)

internal data class BiliPlayUrlAudio(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val codec: String,
)

internal object BiliCacheMetadataParser {
    fun parse(
        entryJson: String?,
        videoInfoJson: String?,
        fallbackId: String,
        fallbackTitle: String,
    ): BiliCacheMetadata {
        val pc = parseObject(videoInfoJson)
        val android = parseObject(entryJson)
        val page = android?.optJSONObject("page_data")

        val title = firstNotBlank(
            pc?.optString("title"),
            android?.optString("title"),
            page?.optString("download_title"),
            fallbackTitle,
        )
        val subtitle = firstNotBlank(
            pc?.optString("tabName"),
            page?.optString("part"),
            page?.optString("download_subtitle"),
        ).takeUnless { it == title }.orEmpty()
        val owner = firstNotBlank(
            pc?.optString("uname"),
            android?.optString("owner_name"),
            android?.optJSONObject("owner")?.optString("name"),
        )
        val duration = firstPositive(
            pc?.optLong("duration") ?: 0,
            page?.optLong("duration") ?: 0,
            android?.optLong("duration") ?: 0,
            (android?.optLong("total_time_milli") ?: 0) / 1_000L,
        )
        val bvid = firstNotBlank(
            pc?.optString("bvid"),
            android?.optString("bvid"),
        )
        val aid = firstNotBlank(
            pc?.opt("aid")?.toString(),
            android?.opt("avid")?.toString(),
            android?.opt("aid")?.toString(),
        )
        val cid = firstNotBlank(
            pc?.opt("cid")?.toString(),
            page?.opt("cid")?.toString(),
            android?.opt("cid")?.toString(),
        )
        val pageNumber = firstNotBlank(
            pc?.opt("p")?.toString(),
            page?.opt("page")?.toString(),
        )
        val sourceId = listOf(bvid.ifBlank { aid }, cid, pageNumber)
            .filter(String::isNotBlank)
            .joinToString(":")
            .ifBlank { fallbackId }

        return BiliCacheMetadata(
            sourceId = sourceId,
            title = title,
            subtitle = subtitle,
            owner = owner,
            durationSeconds = duration,
            completed = completed(pc, android),
        )
    }

    fun parsePlayUrl(raw: String?): List<BiliPlayUrlAudio> {
        val root = parseObject(raw) ?: return emptyList()
        val data = root.optJSONObject("data") ?: root
        val dash = data.optJSONObject("dash") ?: return emptyList()
        val audio = dash.optJSONArray("audio") ?: return emptyList()
        return (0 until audio.length()).mapNotNull { index ->
            val item = audio.optJSONObject(index) ?: return@mapNotNull null
            val id = item.opt("id")?.toString().orEmpty()
            val url = firstNotBlank(
                item.optString("baseUrl"),
                item.optString("base_url"),
                item.optString("url"),
            )
            val fileName = url.substringBefore('?').substringAfterLast('/')
            if (id.isBlank() && fileName.isBlank()) return@mapNotNull null
            BiliPlayUrlAudio(
                id = id,
                fileName = fileName,
                mimeType = firstNotBlank(
                    item.optString("mimeType"),
                    item.optString("mime_type"),
                    "audio/mp4",
                ),
                codec = item.optString("codecs"),
            )
        }
    }

    private fun completed(pc: JSONObject?, android: JSONObject?): Boolean {
        if (pc?.has("status") == true) {
            val status = pc.optString("status").lowercase(Locale.ROOT)
            if (status.isNotBlank() && status !in setOf("completed", "complete", "finished")) {
                return false
            }
        }
        if (pc?.has("progress") == true && pc.optInt("progress") in 0..99) return false
        if (android?.has("is_completed") == true && !android.optBoolean("is_completed")) {
            return false
        }
        return true
    }

    private fun parseObject(raw: String?): JSONObject? =
        raw?.takeIf(String::isNotBlank)?.let { runCatching { JSONObject(it) }.getOrNull() }

    private fun firstPositive(vararg values: Long): Long =
        values.firstOrNull { it > 0L } ?: 0L

    private fun firstNotBlank(vararg values: String?): String =
        values.firstOrNull { !it.isNullOrBlank() }.orEmpty()
}

internal object BiliM4sHeader {
    const val PREFIX_BYTES = 9
    private val supportedBoxes = setOf("ftyp", "styp", "moov")

    /**
     * Returns 9 for Bilibili's nine ASCII-zero prefix, 0 for a normal ISO-BMFF file,
     * and -1 when the header is not a supported MP4/M4A container.
     */
    fun bytesToSkip(header: ByteArray): Int {
        if (isIsoBmffAt(header, 0)) return 0
        val hasBiliPrefix = header.size >= PREFIX_BYTES &&
            (0 until PREFIX_BYTES).all { header[it] == '0'.code.toByte() }
        return if (hasBiliPrefix && isIsoBmffAt(header, PREFIX_BYTES)) {
            PREFIX_BYTES
        } else {
            -1
        }
    }

    private fun isIsoBmffAt(header: ByteArray, offset: Int): Boolean {
        if (header.size < offset + 8) return false
        val box = String(header, offset + 4, 4, Charsets.US_ASCII)
        return box in supportedBoxes
    }
}

internal fun safeBiliFileStem(value: String): String {
    val cleaned = value
        .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trim('.')
    return cleaned.take(96).ifBlank { "B站音频" }
}
