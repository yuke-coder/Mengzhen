package com.mengzhen.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.scheduler.QuickPlaybackSessionFactory
import com.mengzhen.app.speech.VoskChineseModel
import com.mengzhen.app.speech.VoskSpeechInput
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen

@Composable
fun XimalayaAudioSearchScreen(
    navController: NavController,
    startVoiceInput: Boolean = false,
) {
    val context = LocalContext.current
    val store = remember(context) { TaskStore.get(context) }
    val audios = remember { store.getDraft().audios }
    var query by remember { mutableStateOf("") }
    var voiceState by remember { mutableStateOf(VoiceInputState.IDLE) }
    val inputHolder = remember { arrayOfNulls<EditText>(1) }
    val voiceInput = remember(context) {
        VoskSpeechInput(
            onListeningChanged = { listening ->
                voiceState = if (listening) {
                    VoiceInputState.LISTENING
                } else if (voiceState == VoiceInputState.PREPARING) {
                    VoiceInputState.PREPARING
                } else {
                    VoiceInputState.IDLE
                }
            },
            onRecognized = { text ->
                voiceState = VoiceInputState.IDLE
                query = text
                inputHolder[0]?.let { input ->
                    input.setText(text)
                    input.setSelection(input.text.length)
                }
            },
            onError = {
                voiceState = VoiceInputState.IDLE
                AppNotice.error(context, "语音识别失败，请重试")
            },
        )
    }
    val filtered = remember(query, audios) {
        if (query.isBlank()) audios else audios.filter { audio ->
            audio.name.contains(query, ignoreCase = true) ||
                audio.artist.orEmpty().contains(query, ignoreCase = true)
        }
    }

    fun beginVoiceRecognition() {
        if (voiceState != VoiceInputState.IDLE) return
        inputHolder[0]?.let { input ->
            context.getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(input.windowToken, 0)
            input.clearFocus()
        }
        voiceState = VoiceInputState.PREPARING
        VoskChineseModel.load(
            context = context,
            onReady = { model ->
                if (!voiceInput.start(model) && voiceState == VoiceInputState.PREPARING) {
                    voiceState = VoiceInputState.IDLE
                }
            },
            onError = {
                voiceState = VoiceInputState.IDLE
                AppNotice.error(context, "语音模型加载失败，请重试")
            },
        )
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            beginVoiceRecognition()
        } else {
            voiceState = VoiceInputState.IDLE
            AppNotice.warning(context, "需要麦克风权限才能使用语音搜索")
        }
    }

    fun requestVoiceInput() {
        when (voiceState) {
            VoiceInputState.PREPARING -> Unit
            VoiceInputState.LISTENING -> voiceInput.stop()
            VoiceInputState.IDLE -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    beginVoiceRecognition()
                } else {
                    microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    DisposableEffect(voiceInput) {
        onDispose { voiceInput.release() }
    }

    LaunchedEffect(Unit) {
        if (startVoiceInput) requestVoiceInput()
    }

    fun openAudio(audio: TaskAudio) {
        val session = QuickPlaybackSessionFactory.createIdle(
            id = QuickPlaybackSessionFactory.newId(),
            draft = store.getDraft().copy(audios = listOf(audio)),
        )
        QuickPlaybackSessionFactory.save(store, session)
        navController.navigate(Screen.Templates.createRoute(session.id))
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        AndroidView(
            factory = { viewContext ->
                LayoutInflater.from(viewContext).inflate(
                    R.layout.ximalaya_audio_search_title_bar,
                    null,
                    false,
                ).apply {
                    val input = findViewById<EditText>(R.id.xm_audio_search_input)
                    inputHolder[0] = input
                    fun close() = navController.popBackStack()
                    findViewById<View>(R.id.xm_audio_search_back).setOnClickListener { close() }
                    findViewById<TextView>(R.id.xm_audio_search_cancel).setOnClickListener { close() }
                    input.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            query = s?.toString().orEmpty()
                        }
                        override fun afterTextChanged(s: Editable?) = Unit
                    })
                    if (!startVoiceInput) {
                        input.requestFocus()
                        input.post {
                            (viewContext.getSystemService(InputMethodManager::class.java))
                                ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }
                }
            },
            update = { titleBar ->
                val input = titleBar.findViewById<EditText>(R.id.xm_audio_search_input)
                val action = titleBar.findViewById<ImageView>(R.id.xm_audio_search_clear)
                if (input.text.toString() != query) {
                    input.setText(query)
                    input.setSelection(input.text.length)
                }
                input.hint = when (voiceState) {
                    VoiceInputState.PREPARING -> "正在准备语音识别…"
                    VoiceInputState.LISTENING -> "请说出音频名称"
                    VoiceInputState.IDLE -> "搜索已选择音频"
                }
                val hasQuery = input.text.isNotEmpty()
                action.setImageResource(
                    if (hasQuery) {
                        R.drawable.xm_main_v9514_0x7f080a68
                    } else {
                        R.drawable.xm_main_v9514_0x7f080ac1
                    },
                )
                action.contentDescription = if (hasQuery) "清除搜索内容" else "语音搜索"
                action.setOnClickListener {
                    if (input.text.isNotEmpty()) input.text.clear() else requestVoiceInput()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        )

        if (filtered.isEmpty()) {
            Text(
                text = if (audios.isEmpty()) "暂无已选择音频" else "未找到相关音频",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.id.ifBlank { it.name } }) { audio ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable { openAudio(audio) }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AudioSearchArtwork(audio)
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                text = audio.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            audio.artist?.takeIf(String::isNotBlank)?.let { artist ->
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = artist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class VoiceInputState {
    IDLE,
    PREPARING,
    LISTENING,
}

@Composable
private fun AudioSearchArtwork(audio: TaskAudio) {
    val artwork by rememberLocalAudioArtwork(audio.localUri, audio.artworkUri)
    val modifier = Modifier.size(44.dp)
    if (artwork != null) {
        Image(
            bitmap = artwork!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        Image(
            painter = painterResource(R.drawable.xm_main_v9514_default_cover),
            contentDescription = null,
            modifier = modifier,
        )
    }
}
