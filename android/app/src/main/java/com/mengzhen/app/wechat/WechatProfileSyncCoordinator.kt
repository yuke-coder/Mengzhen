package com.mengzhen.app.wechat

import android.content.Context
import com.mengzhen.app.BuildConfig
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import java.util.UUID

internal data class WechatProfileDraft(
    val nickname: String,
    val avatarUrl: String,
)

internal sealed interface WechatProfileSyncEvent {
    data class Success(val profile: WechatProfileDraft) : WechatProfileSyncEvent
    data class Error(val message: String) : WechatProfileSyncEvent
}

/**
 * Host-side equivalent of Ximalaya's WeChat bind callback registry.
 *
 * The expected OAuth state and the last result are persisted so a callback delivered after
 * Android reclaims the foreground activity is not silently discarded.
 */
internal object WechatProfileSyncCoordinator {
    private const val PREFS_NAME = "mengzhen_wechat_profile_sync"
    private const val KEY_EXPECTED_STATE = "expected_state"
    private const val KEY_PENDING_RESULT = "pending_result"
    private const val TRANSACTION_PROFILE_SYNC = "mengzhen_profile_sync"
    private const val MIN_SUPPORTED_WECHAT_API = 620_823_552

    private val mutableEvents = MutableSharedFlow<WechatProfileSyncEvent>(
        extraBufferCapacity = 1,
    )
    val events = mutableEvents.asSharedFlow()

    /** Returns null after dispatch, otherwise a user-facing failure copied from source behavior. */
    fun request(context: Context): String? {
        val appId = BuildConfig.WECHAT_APP_ID.trim()
        if (appId.isEmpty()) return "微信同步尚未配置"

        val api = WXAPIFactory.createWXAPI(context.applicationContext, appId, true)
        val installed = runCatching(api::isWXAppInstalled).getOrDefault(true)
        if (!installed) return "请安装微信"
        if (runCatching(api::getWXAppSupportAPI).getOrDefault(Int.MAX_VALUE) < MIN_SUPPORTED_WECHAT_API) {
            return "微信版本低，请升级"
        }

        api.registerApp(appId)
        val state = UUID.randomUUID().toString()
        preferences(context).edit()
            .putString(KEY_EXPECTED_STATE, state)
            .remove(KEY_PENDING_RESULT)
            .apply()
        val request = SendAuth.Req().apply {
            scope = "snsapi_userinfo"
            this.state = state
            transaction = TRANSACTION_PROFILE_SYNC
        }
        if (api.sendReq(request)) return null

        preferences(context).edit().remove(KEY_EXPECTED_STATE).apply()
        return "登录微信失败！"
    }

    fun consumeExpectedState(context: Context, returnedState: String?): Boolean {
        val prefs = preferences(context)
        val expected = prefs.getString(KEY_EXPECTED_STATE, null)
        prefs.edit().remove(KEY_EXPECTED_STATE).apply()
        return !expected.isNullOrBlank() && expected == returnedState
    }

    fun publish(context: Context, event: WechatProfileSyncEvent) {
        val json = when (event) {
            is WechatProfileSyncEvent.Success -> JSONObject()
                .put("type", "success")
                .put("nickname", event.profile.nickname)
                .put("avatarUrl", event.profile.avatarUrl)
            is WechatProfileSyncEvent.Error -> JSONObject()
                .put("type", "error")
                .put("message", event.message)
        }
        preferences(context).edit().putString(KEY_PENDING_RESULT, json.toString()).apply()
        mutableEvents.tryEmit(event)
    }

    fun takePending(context: Context): WechatProfileSyncEvent? {
        val prefs = preferences(context)
        val raw = prefs.getString(KEY_PENDING_RESULT, null) ?: return null
        prefs.edit().remove(KEY_PENDING_RESULT).apply()
        return runCatching {
            val json = JSONObject(raw)
            when (json.optString("type")) {
                "success" -> WechatProfileSyncEvent.Success(
                    WechatProfileDraft(
                        nickname = json.getString("nickname"),
                        avatarUrl = json.getString("avatarUrl"),
                    ),
                )
                "error" -> WechatProfileSyncEvent.Error(json.getString("message"))
                else -> null
            }
        }.getOrNull()
    }

    fun acknowledge(context: Context) {
        preferences(context).edit().remove(KEY_PENDING_RESULT).apply()
    }

    private fun preferences(context: Context) = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )
}
