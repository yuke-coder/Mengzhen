package com.mengzhen.app.ui.screens

import android.content.Context
import android.net.Uri
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.feedback.AppNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal suspend fun uploadSelectedProfileBackground(
    context: Context,
    uri: Uri,
    currentUser: UserInfo?,
): UserInfo? {
    val file = withContext(Dispatchers.IO) {
        persistProfileFile(context, uri, "background")
    }
    if (file == null) {
        AppNotice.error(context, "背景图读取失败，请重试")
        return null
    }

    val store = TaskStore.get(context)
    fun persist(user: UserInfo) {
        store.getSession()?.first?.let { token -> store.saveUserSession(token, user) }
    }
    fun keepLocal(message: String): UserInfo {
        val user = (currentUser ?: UserInfo()).copy(backgroundUrl = Uri.fromFile(file).toString())
        persist(user)
        AppNotice.warning(context, message)
        return user
    }

    return runCatching {
        withContext(Dispatchers.IO) {
            ApiClient.get(context).uploadProfileBackground(
                file,
                context.contentResolver.getType(uri) ?: "image/jpeg",
            )
        }
    }.fold(
        onSuccess = { response ->
            val remoteUrl = response.optString("background_url", "").ifBlank { null }
            if (response.optBoolean("success") && remoteUrl != null) {
                val user = (currentUser ?: UserInfo()).copy(backgroundUrl = remoteUrl)
                persist(user)
                AppNotice.success(context, "背景图已更新")
                user
            } else {
                val error = response.optString("error").trim()
                keepLocal(
                    if (error.isNotEmpty()) "$error；已保留本地背景" else
                        "云端保存失败，已保留本地背景；请稍后重试",
                )
            }
        },
        onFailure = {
            keepLocal("背景图上传失败，已保留本地背景；请稍后重试")
        },
    )
}

internal fun persistProfileFile(context: Context, uri: Uri, name: String): File? = runCatching {
    val directory = File(context.filesDir, "profile").apply { mkdirs() }
    val type = context.contentResolver.getType(uri).orEmpty()
    val extension = when {
        type.contains("png") -> "png"
        type.contains("webp") -> "webp"
        type.contains("gif") -> "gif"
        else -> "jpg"
    }
    val file = File(directory, "$name.$extension")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    } ?: return null
    file
}.getOrNull()
