package com.mengzhen.app.data.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Web API 客户端 - 对接 https://mengzhen-chi.vercel.app/api/
 *
 * 认证方式：cookie session（Web 端 set-cookie，OkHttp CookieJar 自动管理）
 * 不直连 Supabase，不暴露 service_role key
 *
 * 接口列表：
 * - POST /api/auth/login { username, password }
 * - POST /api/auth/register { username, password }
 * - GET  /api/auth/me
 * - POST /api/auth/logout
 * - GET  /api/profile
 * - PUT  /api/profile
 * - GET  /api/audio/my-list
 * - GET  /api/audio/signed-url?key=xxx
 * - POST /api/audio/upload-ticket { fileName, fileSize, mimeType }
 * - POST /api/audio/upload-complete { fileKey, fileName, fileSize, mimeType }
 * - POST /api/audio/save-to-library { fileKey }
 * - POST /api/feedback
 */
class ApiClient private constructor(
    private val baseUrl: String,
) : CookieJar {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(this)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    // === CookieJar ===

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val store = cookieStore[host] ?: mutableListOf()
        // 移除同名旧 cookie，加新的
        cookies.forEach { newCookie ->
            store.removeAll { it.name == newCookie.name }
            store.add(newCookie)
        }
        cookieStore[host] = store
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val now = System.currentTimeMillis()
        return (cookieStore[host] ?: emptyList()).filter { it.expiresAt > now }
    }

    fun clearCookies() {
        cookieStore.clear()
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
            if (!resp.isSuccessful && !json.has("success")) {
                json.put("success", false)
                json.put("error", json.optString("error", "请求失败 (${resp.code})"))
            }
            return json
        }
    }

    // === 认证 ===

    fun login(username: String, password: String): JSONObject {
        return post("/api/auth/login", JSONObject()
            .put("username", username)
            .put("password", password))
    }

    fun register(username: String, password: String): JSONObject {
        return post("/api/auth/register", JSONObject()
            .put("username", username)
            .put("password", password))
    }

    fun me(): JSONObject = get("/api/auth/me")

    fun logout(): JSONObject = post("/api/auth/logout", JSONObject())

    // === 用户资料 ===

    fun getProfile(): JSONObject = get("/api/profile")

    fun updateProfile(updates: JSONObject): JSONObject = put("/api/profile", updates)

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

    /**
     * 直传文件到 Supabase Storage（签名 URL）
     * 对标 Web 端 audio-upload.ts 的 uploadToSignedUrl 逻辑
     */
    fun uploadFileToSignedUrl(signedUrl: String, file: File, mimeType: String): Boolean {
        val req = Request.Builder()
            .url(signedUrl)
            .header("Content-Type", mimeType)
            .header("x-upsert", "true")
            .put(file.asRequestBody(mimeType.toMediaType()))
            .build()
        return try {
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: IOException) {
            false
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

    fun submitFeedback(content: String, contact: String? = null): JSONObject {
        val body = JSONObject().put("content", content)
        contact?.let { body.put("contact", it) }
        return post("/api/feedback", body)
    }

    companion object {
        const val BASE_URL = "https://mengzhen-chi.vercel.app"

        @Volatile private var instance: ApiClient? = null
        fun get(): ApiClient =
            instance ?: synchronized(this) {
                instance ?: ApiClient(BASE_URL).also { instance = it }
            }
    }
}
