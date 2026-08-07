package com.mengzhen.app.bilibili

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class BiliOnlineAudioClient(
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun resolveSharedVideo(sharedText: String): BiliCacheItem =
        withContext(Dispatchers.IO) {
            val resolvedText = resolveShortLink(sharedText)
            val bvid = BVID_REGEX.find(resolvedText)?.value
                ?: throw IllegalArgumentException("分享内容中没有找到 B 站视频编号")
            val requestedPage = PAGE_REGEX.find(resolvedText)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?.coerceAtLeast(1)
                ?: 1

            val viewUrl = "https://api.bilibili.com/x/web-interface/view"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("bvid", bvid)
                .build()
            val view = getJson(viewUrl.toString(), "https://www.bilibili.com/")
            if (view.optInt("code") != 0) {
                throw IOException(view.optString("message", "无法读取 B 站视频信息"))
            }
            val data = view.optJSONObject("data")
                ?: throw IOException("B 站返回的视频信息不完整")
            val pages = data.optJSONArray("pages")
            val page = pages?.optJSONObject((requestedPage - 1).coerceAtMost(pages.length() - 1))
            val cid = page?.optLong("cid")?.takeIf { it > 0 }
                ?: data.optLong("cid").takeIf { it > 0 }
                ?: throw IOException("无法确定视频分集")
            val pageNumber = page?.optInt("page")?.takeIf { it > 0 } ?: requestedPage

            val playUrl = "https://api.bilibili.com/x/player/playurl"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("bvid", bvid)
                .addQueryParameter("cid", cid.toString())
                .addQueryParameter("fnval", "16")
                .addQueryParameter("qn", "80")
                .build()
            val play = getJson(
                playUrl.toString(),
                "https://www.bilibili.com/video/$bvid",
            )
            if (play.optInt("code") != 0) {
                throw IOException(play.optString("message", "无法获取 B 站音轨"))
            }
            val audioArray = play
                .optJSONObject("data")
                ?.optJSONObject("dash")
                ?.optJSONArray("audio")
                ?: throw IOException("该视频没有可提取的公开音轨")
            val audio = (0 until audioArray.length())
                .mapNotNull(audioArray::optJSONObject)
                .maxByOrNull { it.optLong("bandwidth") }
                ?: throw IOException("该视频没有可提取的公开音轨")
            val audioUrl = listOf("baseUrl", "base_url", "url")
                .firstNotNullOfOrNull { key -> audio.optString(key).takeIf(String::isNotBlank) }
                ?: throw IOException("B 站音轨地址为空")
            val title = data.optString("title", bvid)
            val part = page?.optString("part").orEmpty().takeUnless { it == title }.orEmpty()
            val owner = data.optJSONObject("owner")?.optString("name").orEmpty()
            val cover = data.optString("pic").ifBlank { null }?.replace("http://", "https://")

            BiliCacheItem(
                id = "$bvid:$cid:$pageNumber",
                title = title,
                subtitle = part,
                owner = owner,
                durationSeconds = page?.optLong("duration")
                    ?.takeIf { it > 0 }
                    ?: data.optLong("duration"),
                audioSize = audio.optLong("size"),
                mimeType = listOf("mimeType", "mime_type")
                    .firstNotNullOfOrNull { key ->
                        audio.optString(key).takeIf(String::isNotBlank)
                    }
                    ?: "audio/mp4",
                codec = audio.optString("codecs"),
                audioLocation = audioUrl,
                coverLocation = cover,
                accessMode = BiliCacheAccessMode.NETWORK,
            )
        }

    fun audioRequest(url: String, bvid: String?): Request = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header(
            "Referer",
            bvid?.let { "https://www.bilibili.com/video/$it" }
                ?: "https://www.bilibili.com/",
        )
        .build()

    fun coverRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Referer", "https://www.bilibili.com/")
        .build()

    private fun getJson(url: String, referer: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", referer)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("B 站请求失败：HTTP ${response.code}")
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun resolveShortLink(sharedText: String): String {
        val shortUrl = SHORT_LINK_REGEX.find(sharedText)?.value ?: return sharedText
        val request = Request.Builder()
            .url(shortUrl)
            .header("User-Agent", USER_AGENT)
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                response.request.url.toString()
            }
        }.getOrDefault(sharedText)
    }

    companion object {
        private val BVID_REGEX = Regex("""BV[0-9A-Za-z]{10}""", RegexOption.IGNORE_CASE)
        private val PAGE_REGEX = Regex("""[?&]p=(\d+)""", RegexOption.IGNORE_CASE)
        private val SHORT_LINK_REGEX =
            Regex("""https?://b23\.tv/[0-9A-Za-z]+""", RegexOption.IGNORE_CASE)
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/136 Mobile Safari/537.36"
    }
}
