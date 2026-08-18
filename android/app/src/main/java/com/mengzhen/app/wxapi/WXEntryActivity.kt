package com.mengzhen.app.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.mengzhen.app.BuildConfig
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.wechat.WechatProfileDraft
import com.mengzhen.app.wechat.WechatProfileSyncCoordinator
import com.mengzhen.app.wechat.WechatProfileSyncEvent
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Exact OpenSDK callback path required for applicationId `com.mengzhen.app`. */
class WXEntryActivity : Activity(), IWXAPIEventHandler {
    private lateinit var wechatApi: IWXAPI
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        val appId = BuildConfig.WECHAT_APP_ID
        if (appId.isBlank()) {
            fail("微信同步尚未配置")
            return
        }
        wechatApi = WXAPIFactory.createWXAPI(this, appId, false)
        if (!wechatApi.handleIntent(intent, this)) finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::wechatApi.isInitialized && !wechatApi.handleIntent(intent, this)) finish()
    }

    override fun onReq(request: BaseReq?) {
        finish()
    }

    override fun onResp(response: BaseResp?) {
        val authResponse = response as? SendAuth.Resp
        if (authResponse == null) {
            fail("登录微信失败！")
            return
        }
        val stateMatches = WechatProfileSyncCoordinator.consumeExpectedState(
            this,
            authResponse.state,
        )
        when (authResponse.errCode) {
            0 -> {
                if (!stateMatches) {
                    fail("微信授权状态无效，请重试")
                    return
                }
                val code = authResponse.code.orEmpty()
                if (code.isBlank()) {
                    fail("微信授权凭证无效")
                    return
                }
                exchangeProfile(code)
            }
            -2 -> fail("登录取消！")
            else -> fail("登录微信失败！")
        }
    }

    private fun exchangeProfile(code: String) {
        scope.launch {
            val event = runCatching {
                withContext(Dispatchers.IO) {
                    val response = ApiClient.get(this@WXEntryActivity)
                        .getWechatProfileForSync(code)
                    if (!response.optBoolean("success")) {
                        error(response.optString("error", "微信资料同步失败，请重试"))
                    }
                    val nickname = response.optString("nickname", "").trim()
                    val avatarUrl = response.optString("avatar_url", "").trim()
                    if (nickname.isEmpty() || avatarUrl.isEmpty()) {
                        error("未能读取微信头像和昵称")
                    }
                    WechatProfileSyncEvent.Success(
                        WechatProfileDraft(nickname, avatarUrl),
                    )
                }
            }.getOrElse {
                WechatProfileSyncEvent.Error(
                    it.message?.takeIf(String::isNotBlank)
                        ?: "微信资料同步失败，请重试",
                )
            }
            WechatProfileSyncCoordinator.publish(this@WXEntryActivity, event)
            finish()
        }
    }

    private fun fail(message: String) {
        WechatProfileSyncCoordinator.publish(
            this,
            WechatProfileSyncEvent.Error(message),
        )
        finish()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
