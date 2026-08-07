package com.mengzhen.app.ui.screens

import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import com.ximalaya.ting.android.host.model.CustomButtonModel
import com.ximalaya.ting.android.host.model.QuestionnaireFormTag
import com.ximalaya.ting.android.host.model.QuestionnaireFormText
import com.ximalaya.ting.android.host.model.XNpsQueryModel
import com.ximalaya.ting.android.host.model.XNpsQuestionnaireFormScore
import com.ximalaya.ting.android.host.xnps.XNpsCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FeedbackBackground = Color(0xFFFFFFFF)

private fun createXnpsQueryModel() =
    XNpsQueryModel(
        "您有多大意愿向他人推荐使用梦枕",
        1,
        0,
        "",
        false,
        0L,
        listOf(
            CustomButtonModel("提交", 1, "", 1),
            CustomButtonModel("提交并反馈更多", 2, "", 2),
        ),
        QuestionnaireFormText("", "", 0),
        (0..10).map { score ->
            XNpsQuestionnaireFormScore(
                QuestionnaireFormTag(
                    listOf("Bug 缺陷", "产品建议"),
                    "请选择原因",
                    "",
                    0,
                ),
                score.toString(),
                score,
            )
        },
    )

/** 喜马拉雅 9.5.1.4 XNPS 推荐意愿入口。 */
@Composable
fun FeedbackScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(context) { ApiClient.get(context) }
    val store = remember(context) { TaskStore.get(context) }
    val xnpsModel = remember { createXnpsQueryModel() }
    var submitting by remember { mutableStateOf(false) }

    fun submit(action: Int, score: Int, reasons: List<String>, remark: String) {
        if (store.getSession() == null) {
            AppNotice.info(context, "请先登录后再提交反馈")
            navController.navigate(Screen.Login.route)
            return
        }
        submitting = true
        scope.launch(Dispatchers.IO) {
            val response = runCatching {
                api.submitFeedback(
                    type = if ("Bug 缺陷" in reasons) "bug" else "suggestion",
                    content = buildRecommendationFeedbackContent(score, reasons, remark),
                )
            }
            withContext(Dispatchers.Main) {
                submitting = false
                response.onSuccess { result ->
                    when {
                        result.optBoolean("success", false) -> {
                            if (action == 2) {
                                navController.navigate(Screen.FeedbackChooseType.route) {
                                    popUpTo(Screen.Feedback.route) { inclusive = true }
                                }
                            } else {
                                AppNotice.success(context, "感谢你的反馈")
                                navController.popBackStack()
                            }
                        }
                        result.optBoolean("sessionExpired", false) -> {
                            AppNotice.warning(context, "登录状态已失效，请重新登录")
                            navController.navigate(Screen.Login.route)
                        }
                        else -> AppNotice.error(
                            context,
                            result.optString("message").ifBlank {
                                result.optString("error", "提交失败")
                            },
                        )
                    }
                }.onFailure {
                    AppNotice.error(context, it.message ?: "提交失败")
                }
            }
        }
    }

    ConfigureXnpsDialogWindow()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FeedbackBackground)
            .windowInsetsPadding(WindowInsets.ime)
            .verticalScroll(rememberScrollState()),
    ) {
        AndroidView(
            factory = { viewContext ->
                LayoutInflater.from(viewContext)
                    .inflate(R.layout.host_dialog_fra_xnps, null, false)
                    .also { dialogView ->
                        dialogView.findViewById<XNpsCardView>(R.id.host_xnps_card_view).apply {
                            setInitData(
                                false,
                                true,
                                "feedback",
                                xnpsModel,
                                navController::popBackStack,
                            )
                            setSubmitListener { score ->
                                if (submitting) return@setSubmitListener
                                val form = xnpsModel.questionnaireFormScores
                                    .first { it.score == score }
                                submit(
                                    selectedAction,
                                    score,
                                    form.questionnaireFormTag.tags,
                                    xnpsModel.questionnaireFormText.content,
                                )
                            }
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ConfigureXnpsDialogWindow() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setGravity(Gravity.BOTTOM)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        window.setWindowAnimations(R.style.XmFeedbackXnpsDialogAnimation)
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }
}

private fun buildRecommendationFeedbackContent(
    score: Int,
    reasons: List<String>,
    remark: String,
): String = buildString {
    append("推荐意愿评分：")
    append(score)
    append("/10")
    if (reasons.isNotEmpty()) {
        append("\n原因：")
        append(reasons.joinToString("、"))
    }
    remark.trim().takeIf(String::isNotEmpty)?.let {
        append("\n其他原因：")
        append(it)
    }
}
