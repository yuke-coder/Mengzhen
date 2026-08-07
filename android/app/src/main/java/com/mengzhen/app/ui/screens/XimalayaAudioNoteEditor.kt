package com.mengzhen.app.ui.screens

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.mengzhen.app.R
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Source-layout host for Ximalaya 9.5.1.4 KachaNoteEditFragment.
 *
 * The cloud note-book request layer is replaced by a caller-provided local save callback; the
 * original editor hierarchy, audio strip, preview control, focus behavior and title actions stay
 * intact.
 */
@Composable
internal fun XimalayaSourceAudioNoteEditor(
    audio: TaskAudio,
    initialNote: String,
    durationMs: Long,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val artwork by rememberLocalAudioArtwork(
        uriValue = audio.localUri ?: audio.serverUrl,
        artworkUri = audio.artworkUri,
    )
    val cardColor by produceState(
        initialValue = if (isDark) DARK_CARD_COLOR else LIGHT_CARD_FALLBACK,
        key1 = artwork,
        key2 = isDark,
    ) {
        value = if (isDark) {
            DARK_CARD_COLOR
        } else {
            withContext(Dispatchers.Default) {
                artwork
                    ?.takeUnless { it.isRecycled }
                    ?.let { bitmap ->
                        Palette.from(bitmap)
                            .maximumColorCount(16)
                            .generate()
                            .getDominantColor(LIGHT_CARD_FALLBACK)
                    }
                    ?: LIGHT_CARD_FALLBACK
            }
        }
    }

    val currentToggle by rememberUpdatedState(onTogglePlayback)
    val currentSave by rememberUpdatedState(onSave)
    val currentDismiss by rememberUpdatedState(onDismiss)

    Dialog(
        onDismissRequest = currentDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF121212) else Color.White),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    LayoutInflater.from(context)
                        .inflate(R.layout.main_fra_kacha_note_edit, null, false)
                        .also { root ->
                            val editor = root.findViewById<EditText>(R.id.main_kacha_note_edit_et)
                            editor.setText(initialNote)
                            editor.setSelection(editor.text.length)

                            installXimalayaTitleBar(
                                host = root.findViewById(R.id.main_title_bar),
                                title = "编辑备注",
                                left = XimalayaTitleAction.Cancel {
                                    editor.hideSourceKeyboard()
                                    currentDismiss()
                                },
                                rightText = "保存",
                                onRight = {
                                    editor.hideSourceKeyboard()
                                    currentSave(editor.text.toString())
                                },
                            )

                            root.findViewById<ImageView>(R.id.main_kacha_note_edit_play_iv)
                                .setOnClickListener { currentToggle() }

                            editor.postDelayed(
                                {
                                    editor.requestFocus()
                                    val manager = context.getSystemService(InputMethodManager::class.java)
                                    manager?.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
                                },
                                KEYBOARD_DELAY_MS,
                            )
                        }
                },
                update = { root ->
                    val cover = root.findViewById<ImageView>(R.id.main_kacha_note_edit_album_cover)
                    if (artwork == null || artwork?.isRecycled == true) {
                        cover.setImageResource(R.drawable.xm_main_v9514_default_cover)
                    } else {
                        cover.setImageBitmap(artwork)
                    }

                    root.findViewById<View>(R.id.main_kacha_note_edit_track_bg).background =
                        GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = root.dpNote(64f)
                            setColor(cardColor)
                        }
                    root.findViewById<View>(R.id.main_view_mask).visibility =
                        if (isDark) View.INVISIBLE else View.VISIBLE

                    root.findViewById<TextView>(R.id.main_kacha_note_edit_track_name).text = audio.name
                    root.findViewById<TextView>(R.id.main_kacha_note_edit_time_info).text =
                        sourceAudioTimeInfo(
                            root = root,
                            durationMs = durationMs.takeIf { it > 0L }
                                ?: audio.duration.coerceAtLeast(0L) * 1_000L,
                        )

                    root.findViewById<ImageView>(R.id.main_kacha_note_edit_play_iv).apply {
                        setImageResource(
                            if (isPlaying) {
                                R.drawable.xm_main_v9514_0x7f0824be
                            } else {
                                R.drawable.xm_main_v9514_0x7f0824bf
                            },
                        )
                        contentDescription = if (isPlaying) "暂停试听" else "播放试听"
                    }
                },
            )
        }
    }
}

private fun sourceAudioTimeInfo(root: View, durationMs: Long): CharSequence {
    val startText = "00:00"
    val durationText = formatSourceDuration(durationMs)
    val primaryColor = ContextCompat.getColor(root.context, R.color.xm_audio_note_primary_text)
    val result = SpannableStringBuilder("从")

    val start = result.length
    result.append(startText)
    result.setSpan(
        StyleSpan(Typeface.BOLD),
        start,
        result.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    result.setSpan(
        ForegroundColorSpan(primaryColor),
        start,
        result.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    result.append("开始   |   时长")
    val durationStart = result.length
    result.append(durationText)
    result.setSpan(
        StyleSpan(Typeface.BOLD),
        durationStart,
        result.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    result.setSpan(
        ForegroundColorSpan(primaryColor),
        durationStart,
        result.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return result
}

private fun formatSourceDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.CHINA, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.CHINA, "%02d:%02d", minutes, seconds)
    }
}

private fun EditText.hideSourceKeyboard() {
    clearFocus()
    context.getSystemService(InputMethodManager::class.java)
        ?.hideSoftInputFromWindow(windowToken, 0)
}

private fun View.dpNote(value: Float): Float = value * resources.displayMetrics.density

private const val KEYBOARD_DELAY_MS = 180L
private const val DARK_CARD_COLOR = 0xFF2A2A2A.toInt()
private const val LIGHT_CARD_FALLBACK = 0xFF999999.toInt()
