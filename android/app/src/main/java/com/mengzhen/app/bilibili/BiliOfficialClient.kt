package com.mengzhen.app.bilibili

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri

data class BiliAccountStatus(
    val installed: Boolean,
    val loggedIn: Boolean,
    val uid: Long? = null,
)

class BiliOfficialClient(context: Context) {
    private val appContext = context.applicationContext

    fun accountStatus(): BiliAccountStatus {
        val installed = runCatching {
            appContext.packageManager.getApplicationInfo(BILI_PACKAGE, 0)
        }.isSuccess
        if (!installed) return BiliAccountStatus(installed = false, loggedIn = false)

        val providerStatus = runCatching {
            appContext.contentResolver.query(
                Uri.parse(STATUS_URI),
                arrayOf("uid", "logged"),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use BiliAccountStatus(true, false)
                val uidIndex = cursor.getColumnIndex("uid")
                val loggedIndex = cursor.getColumnIndex("logged")
                val logged = loggedIndex >= 0 && cursor.getInt(loggedIndex) == 1
                val uid = uidIndex.takeIf { it >= 0 }?.let(cursor::getLong)?.takeIf { it > 0 }
                BiliAccountStatus(true, logged, uid)
            } ?: BiliAccountStatus(true, false)
        }.getOrDefault(BiliAccountStatus(true, false))
        return providerStatus.copy(
            loggedIn = providerStatus.loggedIn || BiliAuthorizationSession.isAuthorized,
        )
    }

    fun authorizationIntent(): Intent = Intent(ACTION_AUTHORIZE).apply {
        component = ComponentName(BILI_PACKAGE, AUTH_ACTIVITY)
        putExtra("package_name", appContext.packageName)
    }

    fun acceptAuthorizationResult(resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK) return false
        val accessKey = data?.getStringExtra("access_key").orEmpty()
        if (accessKey.isBlank()) return false
        BiliAuthorizationSession.accessKey = accessKey
        return true
    }

    fun offlineCacheIntent(): Intent = Intent().apply {
        component = ComponentName(BILI_PACKAGE, OFFLINE_ACTIVITY)
    }

    fun officialDownloadIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://app.bilibili.com/"))

    companion object {
        const val BILI_PACKAGE = "tv.danmaku.bili"
        private const val STATUS_URI =
            "content://tv.danmaku.bili.providers.BiliDataProvider/status/logged"
        private const val ACTION_AUTHORIZE = "tv.danmaku.bili.action.AUTHORIZE"
        private const val AUTH_ACTIVITY = "tv.danmaku.bili.activities.login.SSOActivity"
        private const val OFFLINE_ACTIVITY =
            "tv.danmaku.bili.ui.videodownload.VideoDownloadListActivity"
    }
}

object BiliAuthorizationSession {
    @Volatile
    var accessKey: String? = null
        internal set

    val isAuthorized: Boolean
        get() = !accessKey.isNullOrBlank()
}
