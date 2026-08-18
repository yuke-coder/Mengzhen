package com.mengzhen.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val FeedbackAssetHost = "m.ximalaya.com"
private const val FeedbackAssetRoot = "ximalaya_feedback_source"
private const val FeedbackBridgeName = "MengzhenFeedback"
private const val FeedbackTemplatePath =
    "/cs-flow-app/api/flow/template/out/common-feedback"
private const val FeedbackPagePath = "/cs-flow-app/page/common-feedback"

/**
 * Ximalaya 9.5.1.4 common-feedback source page.
 *
 * The original flowy-c renderer, styles and questionnaire schema are packaged as assets. This
 * native adapter only supplies local assets, applies Mengzhen branding and bridges the source
 * submit action to Mengzhen's feedback API. No Ximalaya analytics or form endpoint is reachable.
 */
@Composable
fun XimalayaFeedbackEvaluationScreen(
    navController: NavController,
    initialScore: Int,
    initialReasons: String,
    initialRemark: String,
) {
    val context = LocalContext.current
    val api = remember(context) { ApiClient.get(context) }
    val store = remember(context) { TaskStore.get(context) }
    val scope = rememberCoroutineScope()
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(Unit) {
        if (store.getSession() == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.FeedbackEvaluation.route) { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.run {
                removeJavascriptInterface(FeedbackBridgeName)
                stopLoading()
                destroy()
            }
            webView = null
        }
    }

    AndroidView(
        factory = { viewContext ->
            LayoutInflater.from(viewContext)
                .inflate(R.layout.ximalaya_feedback_web_source, null, false)
                .also { root ->
                    root.findViewById<View>(R.id.feedback_web_back).setOnClickListener {
                        navController.popBackStack()
                    }
                    root.findViewById<View>(R.id.feedback_web_share).setOnClickListener {
                        shareFeedbackPage(viewContext)
                    }
                    root.findViewById<WebView>(R.id.feedback_web_view).apply {
                        webView = this
                        configureFeedbackWebView(
                            context = viewContext,
                            api = api,
                            scope = scope,
                            initialRemark = initialRemark,
                            onSessionExpired = {
                                AppNotice.warning(viewContext, "登录状态已失效，请重新登录")
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.FeedbackEvaluation.route) { inclusive = true }
                                }
                            },
                        )
                        loadUrl(
                            Uri.Builder()
                                .scheme("https")
                                .authority(FeedbackAssetHost)
                                .path(FeedbackPagePath)
                                .appendQueryParameter("appKey", "mengzhen-feedback")
                                .appendQueryParameter(
                                    "recommendValue",
                                    initialScore.coerceIn(0, 10).toString(),
                                )
                                .appendQueryParameter("rateReasons", initialReasons)
                                .appendQueryParameter("rateRemark", initialRemark)
                                .build()
                                .toString(),
                        )
                    }
                }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
private fun WebView.configureFeedbackWebView(
    context: Context,
    api: ApiClient,
    scope: CoroutineScope,
    initialRemark: String,
    onSessionExpired: () -> Unit,
) {
    setBackgroundColor(Color.WHITE)
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
    }
    addJavascriptInterface(
        MengzhenFeedbackBridge(
            context = context.applicationContext,
            webView = this,
            api = api,
            scope = scope,
            initialRemark = initialRemark,
            onSessionExpired = onSessionExpired,
        ),
        FeedbackBridgeName,
    )
    webViewClient = FeedbackSourceWebViewClient(context.applicationContext)
}

private class FeedbackSourceWebViewClient(
    private val context: Context,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean = request?.url?.host != FeedbackAssetHost

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse {
        val uri = request?.url ?: return blockedResponse()
        if (uri.host != FeedbackAssetHost) return blockedResponse()

        return when {
            uri.path == FeedbackPagePath -> assetResponse("index.html", "text/html")
            uri.path == FeedbackTemplatePath -> templateResponse()
            uri.path?.startsWith("/assets/$FeedbackAssetRoot/") == true -> {
                val fileName = uri.lastPathSegment.orEmpty()
                assetResponse(fileName, mimeType(fileName))
            }
            else -> notFoundResponse()
        }
    }

    private fun assetResponse(fileName: String, mimeType: String): WebResourceResponse =
        WebResourceResponse(
            mimeType,
            "utf-8",
            context.assets.open("$FeedbackAssetRoot/$fileName"),
        )

    private fun templateResponse(): WebResourceResponse {
        val source = context.assets.open("$FeedbackAssetRoot/template.json")
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }
        val adapted = source
            .replace(
                "您当前是平台会员用户吗？（包括喜马拉雅VIP/儿童VIP/ximi团/大师课等）",
                "您当前是平台会员用户吗？",
            )
            .replace("喜马拉雅", "梦枕")
            .replace("喜马", "梦枕")
        return WebResourceResponse(
            "application/json",
            "utf-8",
            ByteArrayInputStream(adapted.toByteArray(StandardCharsets.UTF_8)),
        )
    }

    private fun blockedResponse() = response(403, "Blocked")

    private fun notFoundResponse() = response(404, "Not Found")

    private fun response(status: Int, reason: String) = WebResourceResponse(
        "text/plain",
        "utf-8",
        status,
        reason,
        emptyMap(),
        ByteArrayInputStream(ByteArray(0)),
    )
}

private class MengzhenFeedbackBridge(
    private val context: Context,
    private val webView: WebView,
    private val api: ApiClient,
    private val scope: CoroutineScope,
    private val initialRemark: String,
    private val onSessionExpired: () -> Unit,
) {
    @JavascriptInterface
    fun submit(payload: String) {
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                val content = buildSourceFeedbackContent(context, payload, initialRemark)
                require(content.length <= 2_000) {
                    "反馈内容超过服务器限制，请精简填写"
                }
                api.submitFeedback(
                    type = "suggestion",
                    category = "评价反馈",
                    content = content,
                )
            }

            withContext(Dispatchers.Main) {
                result.onSuccess { response ->
                    when {
                        response.optBoolean("success", false) -> complete(true, "")
                        response.optBoolean("sessionExpired", false) -> {
                            complete(false, "登录状态已失效")
                            onSessionExpired()
                        }
                        else -> complete(
                            false,
                            response.optString("message").ifBlank {
                                response.optString("error", "提交失败")
                            },
                        )
                    }
                }.onFailure {
                    complete(false, it.message ?: "提交失败")
                }
            }
        }
    }

    private fun complete(success: Boolean, message: String) {
        val script = "window.completeMengzhenFeedback($success,${JSONObject.quote(message)});"
        webView.post { webView.evaluateJavascript(script, null) }
    }
}

private fun buildSourceFeedbackContent(
    context: Context,
    payload: String,
    initialRemark: String,
): String {
    val templateResponse = context.assets.open("$FeedbackAssetRoot/template.json")
        .bufferedReader(StandardCharsets.UTF_8)
        .use { it.readText() }
    val schema = JSONObject(
        JSONObject(templateResponse)
            .getJSONObject("data")
            .getString("content"),
    ).getJSONObject("schema")
        .getJSONObject("properties")
    val fields = JSONObject(payload).optJSONArray("extentFieldDtoList") ?: JSONArray()
    val lines = mutableListOf<String>()
    var includesRemark = false

    for (index in 0 until fields.length()) {
        val field = fields.optJSONObject(index) ?: continue
        val code = field.optString("dictionaryCode")
        if (code in setOf("level", "SatFootII", "ximaUserId")) continue
        val title = schema.optJSONObject(code)?.optString("title").orEmpty()
            .replace("喜马拉雅", "梦枕")
            .replace("喜马", "梦枕")
            .trim()
        val content = readableFeedbackValue(field.opt("content"))
        if (title.isBlank() || content.isBlank()) continue
        if (code == "SatOpenI") includesRemark = true
        lines += "$title：$content"
    }

    if (!includesRemark && initialRemark.isNotBlank()) {
        lines += "对我们有什么其他的建议/吐槽吗？（选填）：${initialRemark.trim()}"
    }
    return lines.joinToString("\n")
}

private fun readableFeedbackValue(value: Any?): String {
    val text = when (value) {
        null, JSONObject.NULL -> ""
        else -> value.toString()
    }.trim()
    if (!text.startsWith("[") || !text.endsWith("]")) return text
    return runCatching {
        val array = JSONArray(text)
        (0 until array.length()).joinToString("、") { array.optString(it) }
    }.getOrDefault(text)
}

private fun mimeType(fileName: String): String = when {
    fileName.endsWith(".html") -> "text/html"
    fileName.endsWith(".css") -> "text/css"
    fileName.endsWith(".js") -> "application/javascript"
    fileName.endsWith(".json") -> "application/json"
    fileName.endsWith(".png") -> "image/png"
    else -> "application/octet-stream"
}

private fun shareFeedbackPage(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "梦枕评价反馈")
        putExtra(Intent.EXTRA_TEXT, "梦枕评价反馈\nhttps://driftcue.com")
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}
