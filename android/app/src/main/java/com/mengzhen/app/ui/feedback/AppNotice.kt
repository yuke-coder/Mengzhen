package com.mengzhen.app.ui.feedback

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mengzhen.app.ui.feedback.toast.DefaultToast
import com.mengzhen.app.ui.feedback.toast.LocalToastStackExpanded
import com.mengzhen.app.ui.feedback.toast.ToastAlignment
import com.mengzhen.app.ui.feedback.toast.ToastCategory
import com.mengzhen.app.ui.feedback.toast.ToastData
import com.mengzhen.app.ui.feedback.toast.ToastHost
import com.mengzhen.app.ui.feedback.toast.ToastHostState
import com.mengzhen.app.ui.feedback.toast.ToastStyle
import com.mengzhen.app.ui.feedback.toast.ToastTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class NoticeTone {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

object AppNotice {
    private const val DEFAULT_DURATION_MS = 3_500L
    private const val ERROR_DURATION_MS = 5_000L

    internal val hostState = ToastHostState()
    private val presentations = mutableStateMapOf<String, NoticePresentation>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun info(context: Context, message: String, title: String = "提示") {
        show(context, title, message, NoticeTone.INFO)
    }

    fun success(context: Context, message: String, title: String = "已完成") {
        show(context, title, message, NoticeTone.SUCCESS)
    }

    fun warning(context: Context, message: String, title: String = "请注意") {
        show(context, title, message, NoticeTone.WARNING)
    }

    fun error(context: Context, message: String, title: String = "操作失败") {
        show(context, title, message, NoticeTone.ERROR)
    }

    @Suppress("UNUSED_PARAMETER")
    fun show(
        context: Context,
        title: String,
        message: String,
        tone: NoticeTone = NoticeTone.INFO,
        durationMs: Long = if (tone == NoticeTone.ERROR) {
            ERROR_DURATION_MS
        } else {
            DEFAULT_DURATION_MS
        },
    ) {
        val normalizedTitle = title.trim().ifEmpty { null }
        val normalizedMessage = message.trim()
        if (normalizedMessage.isEmpty() && normalizedTitle == null) return

        scope.launch {
            val toastId = hostState.showToast(
                title = normalizedTitle,
                message = normalizedMessage,
                icon = tone.icon,
                category = tone.category,
                height = 88.dp,
            )
            presentations[toastId] = NoticePresentation(
                tone = tone,
                durationMs = durationMs.coerceAtLeast(1_000L),
            )
        }
    }

    internal fun presentationFor(toastId: String): NoticePresentation =
        presentations[toastId] ?: NoticePresentation(
            tone = NoticeTone.INFO,
            durationMs = DEFAULT_DURATION_MS,
        )

    internal fun release(toastId: String) {
        presentations.remove(toastId)
    }

    private val NoticeTone.icon
        get() = when (this) {
            NoticeTone.INFO -> Icons.Rounded.Info
            NoticeTone.SUCCESS -> Icons.Rounded.CheckCircle
            NoticeTone.WARNING -> Icons.Rounded.WarningAmber
            NoticeTone.ERROR -> Icons.Rounded.ErrorOutline
        }

    private val NoticeTone.category
        get() = when (this) {
            NoticeTone.INFO -> ToastCategory.General
            NoticeTone.SUCCESS -> ToastCategory.Success
            NoticeTone.WARNING -> ToastCategory.Warning
            NoticeTone.ERROR -> ToastCategory.Error
    }
}

internal data class NoticePresentation(
    val tone: NoticeTone,
    val durationMs: Long,
)

@Composable
fun AppNoticeHost(modifier: Modifier = Modifier) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface
    val toastTheme = remember(surfaceColor, contentColor) {
        ToastTheme(
            default = ToastStyle(
                backgroundColor = surfaceColor,
                textColor = contentColor,
                shape = RoundedCornerShape(16.dp),
                elevation = 10.dp,
                tonalElevation = 0.dp,
            ),
        )
    }

    ToastHost(
        hostState = AppNotice.hostState,
        modifier = modifier.fillMaxSize(),
        alignment = ToastAlignment.Top,
        visibleCount = 3,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        showCloseButton = true,
        theme = toastTheme,
        toast = { toastData ->
            val presentation = AppNotice.presentationFor(toastData.id)
            CountdownToast(
                toastData = toastData,
                theme = toastTheme,
                tone = presentation.tone,
                durationMs = presentation.durationMs,
            )
        },
    )
}

@Composable
private fun CountdownToast(
    toastData: ToastData,
    theme: ToastTheme,
    tone: NoticeTone,
    durationMs: Long,
) {
    val progress = remember(toastData.id) { Animatable(1f) }
    val stackExpanded = LocalToastStackExpanded.current

    DisposableEffect(toastData.id) {
        onDispose {
            AppNotice.release(toastData.id)
        }
    }

    LaunchedEffect(toastData.id, durationMs, stackExpanded) {
        if (stackExpanded) {
            progress.stop()
            return@LaunchedEffect
        }

        val remainingDurationMs = (durationMs * progress.value)
            .roundToInt()
            .coerceAtLeast(1)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(remainingDurationMs, easing = LinearEasing),
        )
        toastData.dismiss()
    }

    Box {
        DefaultToast(
            toastData = toastData,
            theme = theme,
            showCloseButton = true,
        )
        LinearProgressIndicator(
            progress = { progress.value },
            color = tone.progressColor,
            trackColor = tone.progressColor.copy(alpha = 0.12f),
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(
                    RoundedCornerShape(
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                    ),
                ),
        )
    }
}

private val NoticeTone.progressColor: Color
    get() = when (this) {
        NoticeTone.INFO -> Color(0xFF2196F3)
        NoticeTone.SUCCESS -> Color(0xFF4CAF50)
        NoticeTone.WARNING -> Color(0xFFFF9800)
        NoticeTone.ERROR -> Color(0xFFF44336)
    }
