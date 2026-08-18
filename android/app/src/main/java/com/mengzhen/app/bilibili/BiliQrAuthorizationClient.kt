package com.mengzhen.app.bilibili

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

internal data class BiliQrCode(
    val content: String,
    val key: String,
)

internal sealed interface BiliQrPollResult {
    data object WaitingForScan : BiliQrPollResult
    data object WaitingForConfirmation : BiliQrPollResult
    data object Retrying : BiliQrPollResult
    data object Expired : BiliQrPollResult
    data class Authorized(
        val cookieHeader: String,
        val refreshToken: String?,
    ) : BiliQrPollResult
    data class Failed(val message: String) : BiliQrPollResult
}

internal class BiliQrAuthorizationClient(
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun createQrCode(): BiliQrCode = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(GENERATE_URL)
            .biliWebHeaders()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("二维码获取失败")
            }
            val payload = JSONObject(response.body.string())
            if (payload.optInt("code") != 0) {
                throw IOException(payload.optString("message", "二维码获取失败"))
            }
            val data = payload.optJSONObject("data")
                ?: throw IOException("二维码获取失败")
            val content = data.optString("url")
            val key = data.optString("qrcode_key")
            if (content.isBlank() || key.isBlank()) {
                throw IOException("二维码获取失败")
            }
            BiliQrCode(content = content, key = key)
        }
    }

    suspend fun poll(key: String): BiliQrPollResult = withContext(Dispatchers.IO) {
        val url = POLL_URL.toHttpUrl()
            .newBuilder()
            .addQueryParameter("qrcode_key", key)
            .addQueryParameter("source", "main-fe-header")
            .build()
        val request = Request.Builder()
            .url(url)
            .biliWebHeaders()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext BiliQrPollResult.Failed("轮询失败")
                val payload = runCatching { JSONObject(response.body.string()) }.getOrElse {
                    return@withContext BiliQrPollResult.Failed("轮询失败")
                }
                if (payload.optInt("code") != 0) {
                    return@withContext BiliQrPollResult.Failed(
                        payload.optString("message", "轮询失败"),
                    )
                }
                val data = payload.optJSONObject("data")
                    ?: return@withContext BiliQrPollResult.Failed("轮询失败")
                when (data.optInt("code")) {
                    CODE_AUTHORIZED -> {
                        val cookieHeader = collectSessionCookies(
                            redirectUrl = data.optString("url"),
                            setCookieHeaders = response.headers.values("Set-Cookie"),
                        )
                        if (cookieHeader.isBlank()) {
                            BiliQrPollResult.Failed("登录凭据不完整，请刷新二维码重试")
                        } else {
                            BiliQrPollResult.Authorized(
                                cookieHeader = cookieHeader,
                                refreshToken = data.optString("refresh_token")
                                    .takeIf(String::isNotBlank),
                            )
                        }
                    }
                    CODE_NOT_SCANNED -> BiliQrPollResult.WaitingForScan
                    CODE_SCANNED -> BiliQrPollResult.WaitingForConfirmation
                    CODE_EXPIRED -> BiliQrPollResult.Expired
                    else -> BiliQrPollResult.Failed(
                        data.optString("message", "轮询失败"),
                    )
                }
            }
        } catch (_: IOException) {
            BiliQrPollResult.Retrying
        }
    }

    private fun Request.Builder.biliWebHeaders(): Request.Builder =
        header("Accept", "application/json")
            .header("Referer", "https://www.bilibili.com/")
            .header("User-Agent", USER_AGENT)

    private fun collectSessionCookies(
        redirectUrl: String,
        setCookieHeaders: List<String>,
    ): String {
        val cookies = linkedMapOf<String, String>()
        setCookieHeaders.forEach { header ->
            val pair = header.substringBefore(';')
            val separator = pair.indexOf('=')
            if (separator > 0) {
                val name = pair.substring(0, separator).trim()
                val value = pair.substring(separator + 1).trim()
                if (name in SESSION_COOKIE_NAMES && value.isNotBlank()) cookies[name] = value
            }
        }
        runCatching { Uri.parse(redirectUrl) }.getOrNull()?.let { uri ->
            SESSION_COOKIE_NAMES.forEach { name ->
                uri.getQueryParameter(name)
                    ?.takeIf(String::isNotBlank)
                    ?.let { cookies[name] = it }
            }
        }
        return cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" }
    }

    private companion object {
        const val GENERATE_URL =
            "https://passport.bilibili.com/x/passport-login/web/qrcode/generate"
        const val POLL_URL =
            "https://passport.bilibili.com/x/passport-login/web/qrcode/poll"
        const val CODE_AUTHORIZED = 0
        const val CODE_EXPIRED = 86038
        const val CODE_SCANNED = 86090
        const val CODE_NOT_SCANNED = 86101
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/136 Mobile Safari/537.36"
        val SESSION_COOKIE_NAMES = setOf(
            "DedeUserID",
            "DedeUserID__ckMd5",
            "SESSDATA",
            "bili_jct",
            "sid",
        )
    }
}
