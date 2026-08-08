package com.mengzhen.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 本地音频到喜马拉雅播放页封面的数据适配层。
 *
 * 喜马拉雅播放页本身接收已经解码好的封面 Bitmap；这里保持同一契约，
 * 仅负责从用户选择的音频元数据中取出嵌入封面。
 */
@Composable
internal fun rememberLocalAudioArtwork(
    uriValue: String?,
    artworkUri: String? = null,
): State<Bitmap?> {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, key1 = uriValue, key2 = artworkUri) {
        value = withContext(Dispatchers.IO) {
            decodeArtworkUri(context, artworkUri)?.let { return@withContext it }
            if (uriValue.isNullOrBlank()) return@withContext null
            val retriever = MediaMetadataRetriever()
            try {
                val uri = Uri.parse(uriValue)
                when (uri.scheme?.lowercase()) {
                    "content", "file", "android.resource" ->
                        retriever.setDataSource(context, uri)
                    "http", "https" ->
                        retriever.setDataSource(uriValue, emptyMap())
                    else ->
                        retriever.setDataSource(uriValue)
                }
                retriever.embeddedPicture?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }
    }
}

private fun decodeArtworkUri(context: android.content.Context, value: String?): Bitmap? {
    if (value.isNullOrBlank()) return null
    return runCatching {
        val uri = Uri.parse(value)
        when (uri.scheme?.lowercase()) {
            "content", "file", "android.resource" ->
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            "http", "https" -> {
                val connection = URL(value).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 5_000
                    connection.connect()
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.use(BitmapFactory::decodeStream)
                    } else {
                        null
                    }
                } finally {
                    connection.disconnect()
                }
            }
            else -> BitmapFactory.decodeFile(value)
        }
    }.getOrNull()
}
