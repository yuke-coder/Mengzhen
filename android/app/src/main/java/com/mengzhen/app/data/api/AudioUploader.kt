package com.mengzhen.app.data.api

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 音频上传器 - 完整三步流程
 *
 * 1. POST /api/audio/upload-ticket -> 拿签名 URL + fileKey
 * 2. PUT 签名 URL -> 直传文件到 Supabase Storage（不走 Vercel，无大小限制）
 * 3. POST /api/audio/upload-complete -> 入库
 *
 * 对标 Web 端 src/lib/audio-upload.ts
 * 重复文件拦截：文件名 + 文件大小都匹配，直接不允许上传
 */
class AudioUploader(private val api: ApiClient = ApiClient.get()) {

    /**
     * 上传音频文件
     * @param context 用于读取 Uri
     * @param fileUri 音频文件 Uri
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param mimeType MIME 类型
     * @return UploadResult 成功/失败/重复
     */
    suspend fun upload(
        context: Context,
        fileUri: Uri,
        fileName: String,
        fileSize: Long,
        mimeType: String,
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            // Step 1: 获取上传凭证
            Log.i(TAG, "Step 1: Requesting upload ticket for $fileName ($fileSize bytes)")
            val ticket = api.uploadTicket(fileName, fileSize, mimeType)
            if (!ticket.optBoolean("success", false)) {
                return@withContext UploadResult.Failed(ticket.optString("error", "获取上传凭证失败"))
            }

            val fileKey = ticket.optString("fileKey")
            val signedUploadUrl = ticket.optString("signedUploadUrl")
            if (fileKey.isEmpty() || signedUploadUrl.isEmpty()) {
                return@withContext UploadResult.Failed("上传凭证信息不完整")
            }

            // Step 2: 直传文件到 Supabase Storage
            Log.i(TAG, "Step 2: Uploading file to Supabase Storage")
            val tempFile = uriToTempFile(context, fileUri, fileName)
            val uploaded = api.uploadFileToSignedUrl(signedUploadUrl, tempFile, mimeType)
            tempFile.delete()

            if (!uploaded) {
                return@withContext UploadResult.Failed("文件上传失败，请检查网络后重试")
            }
            Log.i(TAG, "Step 2 complete: File uploaded")

            // Step 3: 入库
            Log.i(TAG, "Step 3: Completing upload")
            val complete = api.uploadComplete(fileKey, fileName, fileSize, mimeType)
            if (!complete.optBoolean("success", false)) {
                return@withContext UploadResult.Failed(complete.optString("error", "音频入库失败"))
            }

            val audioUrl = complete.optString("audio_url")
            val finalFileKey = complete.optString("file_key")
            val finalFileName = complete.optString("file_name")
            val finalFileSize = complete.optLong("file_size", fileSize)

            Log.i(TAG, "Upload complete: $finalFileName -> $audioUrl")
            UploadResult.Success(
                audioUrl = audioUrl,
                fileKey = finalFileKey,
                fileName = finalFileName,
                fileSize = finalFileSize,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            UploadResult.Failed(e.message ?: "上传失败")
        }
    }

    /**
     * 检查是否重复文件
     * @param existingAudios 已有音频列表
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @return true 如果重复
     */
    fun isDuplicate(
        existingAudios: List<com.mengzhen.app.data.model.TaskAudio>,
        fileName: String,
        fileSize: Long,
    ): Boolean {
        return existingAudios.any { it.name == fileName && it.size == fileSize }
    }

    private fun uriToTempFile(context: Context, uri: Uri, fileName: String): File {
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
        context.contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(tempFile).use { output ->
                input?.copyTo(output)
            }
        }
        return tempFile
    }

    companion object {
        private const val TAG = "AudioUploader"
    }
}

sealed class UploadResult {
    data class Success(
        val audioUrl: String,
        val fileKey: String,
        val fileName: String,
        val fileSize: Long,
    ) : UploadResult()

    data class Failed(val message: String) : UploadResult()
}
