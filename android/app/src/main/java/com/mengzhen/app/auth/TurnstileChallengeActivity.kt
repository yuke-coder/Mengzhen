package com.mengzhen.app.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import com.mengzhen.app.data.api.ApiClient

class TurnstileChallengeActivity : Activity() {
    private lateinit var webView: WebView
    private var completed = false
    private val timeoutRunnable = Runnable {
        finishWithError("安全验证超时，请检查网络后重试")
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply challengeView@ {
            alpha = 0f
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@challengeView, true)
            }
            addJavascriptInterface(TurnstileBridge(), BRIDGE_NAME)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = request.isForMainFrame && !request.url.isTrustedChallengeUrl()

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) {
                        finishWithError("安全验证页面加载失败，请检查网络后重试")
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: android.webkit.WebResourceResponse,
                ) {
                    if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                        finishWithError("安全验证页面加载失败，请重试")
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail,
                ): Boolean {
                    finishWithError("安全验证进程异常，请重试")
                    return true
                }
            }
        }

        setContentView(webView)
        webView.postDelayed(timeoutRunnable, TIMEOUT_MS)
        webView.loadUrl(CHALLENGE_URL)
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeCallbacks(timeoutRunnable)
            webView.removeJavascriptInterface(BRIDGE_NAME)
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun finishWithToken(token: String) {
        if (completed) return
        completed = true
        webView.removeCallbacks(timeoutRunnable)
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(EXTRA_TOKEN, token),
        )
        finish()
    }

    private fun finishWithError(message: String) {
        if (completed) return
        completed = true
        if (::webView.isInitialized) webView.removeCallbacks(timeoutRunnable)
        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(EXTRA_ERROR, message),
        )
        finish()
    }

    private fun showInteractiveChallenge() {
        webView.alpha = 1f
    }

    private inner class TurnstileBridge {
        @JavascriptInterface
        fun onToken(token: String) {
            runOnUiThread {
                if (token.length in 1..MAX_TOKEN_LENGTH) {
                    finishWithToken(token)
                } else {
                    finishWithError("安全验证结果无效，请重试")
                }
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            runOnUiThread {
                finishWithError(message.ifBlank { "安全验证失败，请重试" })
            }
        }

        @JavascriptInterface
        fun onInteractive() {
            runOnUiThread(::showInteractiveChallenge)
        }
    }

    companion object {
        const val EXTRA_TOKEN = "turnstile_token"
        const val EXTRA_ERROR = "turnstile_error"

        private const val BRIDGE_NAME = "MengzhenTurnstile"
        private const val MAX_TOKEN_LENGTH = 2_048
        private const val TIMEOUT_MS = 30_000L
        private const val CHALLENGE_PATH = "/auth/native-turnstile"
        private const val CHALLENGE_URL = "${ApiClient.BASE_URL}$CHALLENGE_PATH"

        fun createIntent(context: Context): Intent =
            Intent(context, TurnstileChallengeActivity::class.java)

        private fun Uri.isTrustedChallengeUrl(): Boolean =
            scheme == "https" &&
                host == Uri.parse(ApiClient.BASE_URL).host &&
                path == CHALLENGE_PATH
    }
}
