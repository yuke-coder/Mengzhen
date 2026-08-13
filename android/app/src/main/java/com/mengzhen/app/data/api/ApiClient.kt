package com.mengzhen.app.data.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.mengzhen.app.data.store.TaskStore
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Web API 客户端 - 对接 https://driftcue.com/api/
 *
 * 认证方式：cookie session（Web 端 set-cookie，OkHttp CookieJar 自动管理）
 * 不直连 Supabase，不暴露 service_role key
 *
 * 接口列表：
 * - POST /api/auth/login { username, password, turnstileToken }
 * - POST /api/auth/register { username, password, turnstileToken }
 * - GET  /api/auth/me
 * - POST /api/auth/logout
 * - GET  /api/profile
 * - PUT  /api/profile
 * - GET  /api/audio/my-list
 * - GET  /api/audio/signed-url?key=xxx
 * - POST /api/audio/upload-ticket { fileName, fileSize, mimeType }
 * - POST /api/audio/upload-complete { fileKey, fileName, fileSize, mimeType }
     * - POST/DELETE /api/audio/save-to-library { fileKey }
 * - POST /api/feedback
 */
class ApiClient private constructor(
    context: Context,
    private val baseUrl: String,
) : CookieJar {

    private val applicationContext = context.applicationContext
    private val apiUrl = baseUrl.toHttpUrl()
    private val cookiePrefs: SharedPreferences = applicationContext.getSharedPreferences(
        "mengzhen_api_cookies",
        Context.MODE_PRIVATE,
    )
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    init {
        restoreCookies()
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(this)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    // === CookieJar ===

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val store = cookieStore.getOrPut(host) { mutableListOf() }
        val now = System.currentTimeMillis()
        cookies.forEach { newCookie ->
            store.removeAll {
                it.name == newCookie.name &&
                    it.domain == newCookie.domain &&
                    it.path == newCookie.path
            }
            if (newCookie.expiresAt > now) store.add(newCookie)
        }
        if (host == apiUrl.host) persistApiCookies()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val now = System.currentTimeMillis()
        val store = cookieStore[host] ?: return emptyList()
        val expired = store.removeAll { it.expiresAt <= now }
        if (expired && host == apiUrl.host) persistApiCookies()
        return store.filter { it.matches(url) }
    }

    @Synchronized
    fun clearCookies() {
        cookieStore.clear()
        cookiePrefs.edit().remove(COOKIES_KEY).apply()
    }

    private fun restoreCookies() {
        val raw = cookiePrefs.getString(COOKIES_KEY, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            val restored = mutableListOf<Cookie>()
            for (index in 0 until array.length()) {
                Cookie.parse(apiUrl, array.getString(index))
                    ?.takeIf { it.expiresAt > System.currentTimeMillis() }
                    ?.let(restored::add)
            }
            if (restored.isNotEmpty()) cookieStore[apiUrl.host] = restored
        }.onFailure {
            cookiePrefs.edit().remove(COOKIES_KEY).apply()
        }
    }

    private fun persistApiCookies() {
        val array = JSONArray()
        cookieStore[apiUrl.host]
            .orEmpty()
            .filter { it.expiresAt > System.currentTimeMillis() }
            .forEach { array.put(it.toString()) }
        cookiePrefs.edit().putString(COOKIES_KEY, array.toString()).apply()
    }

    // === 请求辅助 ===

    private fun post(path: String, body: JSONObject): JSONObject {
        val req = Request.Builder()
            .url("$baseUrl$path")
            .post(body.toString().toRequestBody(json))
            .build()
        return execute(req)
    }

    private fun put(path: String, body: JSONObject): JSONObject {
        val req = Request.Builder()
            .url("$baseUrl$path")
            .put(body.toString().toRequestBody(json))
            .build()
        return execute(req)
    }

    private fun get(path: String): JSONObject {
        val req = Request.Builder()
            .url("$baseUrl$path")
            .get()
            .build()
        return execute(req)
    }

    private fun delete(path: String): JSONObject {
        val req = Request.Builder()
            .url("$baseUrl$path")
            .delete()
            .build()
        return execute(req)
    }

    private fun execute(req: Request): JSONObject {
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: "{}"
            val json = try { JSONObject(body) } catch (e: Exception) { JSONObject().put("success", false).put("error", "解析响应失败") }
            json.put("httpStatus", resp.code)
            if (!resp.isSuccessful && !json.has("success")) {
                json.put("success", false)
                json.put("error", json.optString("error", "请求失败 (${resp.code})"))
            }
            // 登录凭据错误同样返回 401，不能误清除已有会话。
            if (resp.code == 401 && req.url.encodedPath !in CREDENTIAL_ENDPOINTS) {
                Log.w(TAG, "Session expired (401), clearing cookies")
                clearCookies()
                TaskStore.get(applicationContext).clearSession()
                json.put("sessionExpired", true)
            }
            return json
        }
    }

    // === 认证 ===

    fun login(username: String, password: String, turnstileToken: String): JSONObject {
        return post("/api/auth/login", JSONObject()
            .put("username", username)
            .put("password", password)
            .put("turnstileToken", turnstileToken))
    }

    fun register(username: String, password: String, turnstileToken: String): JSONObject {
        return post("/api/auth/register", JSONObject()
            .put("username", username)
            .put("password", password)
            .put("turnstileToken", turnstileToken))
    }

    fun me(): JSONObject = get("/api/auth/me")

    fun logout(): JSONObject = try {
        post("/api/auth/logout", JSONObject())
    } finally {
        clearCookies()
        TaskStore.get(applicationContext).clearSession()
    }

    // === 用户资料 ===

    // getProfile/updateProfile 定义在下方（含完整参数）

    // === 音频 ===

    fun getMyAudios(): JSONObject = get("/api/audio/my-list")

    fun getSignedUrl(fileKey: String): JSONObject =
        get("/api/audio/signed-url?key=${java.net.URLEncoder.encode(fileKey, "UTF-8")}")

    fun uploadTicket(fileName: String, fileSize: Long, mimeType: String): JSONObject {
        return post("/api/audio/upload-ticket", JSONObject()
            .put("fileName", fileName)
            .put("fileSize", fileSize)
            .put("mimeType", mimeType))
    }

    fun uploadComplete(fileKey: String, fileName: String, fileSize: Long, mimeType: String): JSONObject {
        return post("/api/audio/upload-complete", JSONObject()
            .put("fileKey", fileKey)
            .put("fileName", fileName)
            .put("fileSize", fileSize)
            .put("mimeType", mimeType))
    }

    fun saveToLibrary(fileKey: String): JSONObject {
        return post("/api/audio/save-to-library", JSONObject()
            .put("fileKey", fileKey))
    }

    fun removeFromLibrary(fileKey: String): JSONObject {
        return delete(
            "/api/audio/save-to-library?fileKey=${java.net.URLEncoder.encode(fileKey, "UTF-8")}",
        )
    }

    /**
     * 直传文件到 Supabase Storage（签名 URL）
     * 对标 Web 端 audio-upload.ts 的 uploadToSignedUrl 逻辑
     */
    fun uploadFileToSignedUrl(
        signedUrl: String,
        file: File,
        mimeType: String,
        onProgress: (uploadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ) {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("cacheControl", "3600")
            .addFormDataPart(
                "",
                file.name,
                ProgressRequestBody(file, mimeType.toMediaType(), onProgress),
            )
            .build()
        val req = Request.Builder()
            .url(signedUrl)
            .header("x-upsert", "false")
            .put(multipart)
            .build()

        try {
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) return

                val responseBody = response.body.string().trim()
                val storageMessage = runCatching {
                    JSONObject(responseBody).optString("message").trim()
                }.getOrNull().orEmpty()
                val message = buildString {
                    append("存储服务上传失败（HTTP ")
                    append(response.code)
                    append('）')
                    if (storageMessage.isNotEmpty()) {
                        append("：")
                        append(storageMessage.take(200))
                    }
                }
                Log.e(TAG, "$message; response=${responseBody.take(800)}")
                throw IllegalStateException(message)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Signed upload network failure", e)
            throw IOException("文件上传网络连接失败", e)
        }
    }

    // === 播放进度同步 ===

    fun getPlaybackProgress(): JSONObject = get("/api/playback/progress")

    fun savePlaybackProgress(audioId: String, positionSeconds: Long, durationSeconds: Long): JSONObject {
        return put("/api/playback/progress", JSONObject()
            .put("audioId", audioId)
            .put("positionSeconds", positionSeconds)
            .put("durationSeconds", durationSeconds))
    }

    fun deletePlaybackProgress(audioId: String): JSONObject {
        return delete("/api/playback/progress?audioId=${java.net.URLEncoder.encode(audioId, "UTF-8")}")
    }

    // === 反馈 ===

    fun getProfile(): JSONObject = get("/api/profile")

    fun updateProfile(
        username: String? = null,
        nickname: String? = null,
        gender: String? = null,
        birthday: String? = null,
        location: String? = null,
        bio: String? = null,
        signature: String? = null,
        avatarUrl: String? = null,
        backgroundUrl: String? = null,
    ): JSONObject {
        val body = JSONObject()
        username?.let { body.put("username", it) }
        nickname?.let { body.put("nickname", it) }
        gender?.let { body.put("gender", it) }
        birthday?.let { body.put("birthday", it) }
        location?.let { body.put("location", it) }
        bio?.let { body.put("bio", it) }
        signature?.let { body.put("signature", it) }
        avatarUrl?.let { body.put("avatar_url", it) }
        backgroundUrl?.let { body.put("background_url", it) }
        return put("/api/profile", body)
    }

    fun submitFeedback(
        type: String,
        content: String,
        category: String? = null,
        contact: String? = null,
        images: List<String> = emptyList(),
    ): JSONObject {
        val body = JSONObject()
            .put("type", type)
            .put("content", content)
        category?.let { body.put("category", it) }
        contact?.let { body.put("contact", it) }
        if (images.isNotEmpty()) body.put("images", JSONArray(images))
        return post("/api/feedback", body)
    }

    fun getFeedbacks(): JSONObject = get("/api/feedback")

    fun getFeedback(id: String): JSONObject =
        get("/api/feedback?id=${java.net.URLEncoder.encode(id, "UTF-8")}")

    fun replyFeedback(
        feedbackId: String,
        content: String,
        images: List<String> = emptyList(),
    ): JSONObject {
        val body = JSONObject()
            .put("feedbackId", feedbackId)
            .put("content", content)
        if (images.isNotEmpty()) body.put("images", JSONArray(images))
        return post("/api/feedback", body)
    }

    // === 头像 ===

    fun uploadAvatar(file: File, mimeType: String): JSONObject {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("avatar", file.name, file.asRequestBody(mimeType.toMediaType()))
            .build()
        val req = Request.Builder()
            .url("$baseUrl/api/avatar")
            .post(multipart)
            .build()
        return execute(req)
    }

    fun uploadProfileBackground(file: File, mimeType: String): JSONObject {
        val ticket = post(
            "/api/profile/background",
            JSONObject()
                .put("fileSize", file.length())
                .put("mimeType", mimeType),
        )
        if (!ticket.optBoolean("success")) return ticket

        val fileKey = ticket.optString("file_key")
        val signedUploadUrl = ticket.optString("signed_upload_url")
        if (fileKey.isBlank() || signedUploadUrl.isBlank()) {
            return JSONObject()
                .put("success", false)
                .put("error", "背景图上传凭证无效")
        }

        uploadFileToSignedUrl(signedUploadUrl, file, mimeType)
        return put(
            "/api/profile/background",
            JSONObject()
                .put("fileKey", fileKey)
                .put("fileSize", file.length())
                .put("mimeType", mimeType),
        )
    }

    companion object {
        const val BASE_URL = "https://driftcue.com"
        private const val TAG = "ApiClient"
        private const val COOKIES_KEY = "api_cookies"
        private val CREDENTIAL_ENDPOINTS = setOf(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/entry",
        )

        @Volatile private var instance: ApiClient? = null
        fun get(context: Context): ApiClient =
            instance ?: synchronized(this) {
                instance ?: ApiClient(context.applicationContext, BASE_URL).also { instance = it }
            }
    }
}

/**
 * Streams the signed-URL request body and reports the bytes accepted by OkHttp.
 *
 * The callback stays inside the request body instead of using a network interceptor,
 * so retries and unrelated API calls cannot leak progress into this upload.
 */
private class ProgressRequestBody(
    private val file: File,
    private val mediaType: MediaType,
    private val onProgress: (uploadedBytes: Long, totalBytes: Long) -> Unit,
) : RequestBody() {

    override fun contentType(): MediaType = mediaType

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var uploaded = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        file.inputStream().buffered().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded, total)
            }
        }
    }
}
