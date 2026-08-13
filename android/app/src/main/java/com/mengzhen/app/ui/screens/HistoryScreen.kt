package com.mengzhen.app.ui.screens

import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.model.parseAudioList
import com.mengzhen.app.ui.components.ChatGptLoadingSpinner
import com.mengzhen.app.ui.theme.BrandEndThemed
import com.mengzhen.app.ui.theme.BrandGlowThemed
import com.mengzhen.app.ui.theme.BrandStartThemed
import com.mengzhen.app.ui.theme.Destructive
import com.mengzhen.app.ui.theme.MutedForeground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==================== UI 间距常量 ====================

private val CARD_PADDING = 12.dp
private val CARD_RADIUS = 12.dp
private val LIST_HORIZONTAL_PADDING = 16.dp
private val LIST_VERTICAL_PADDING = 8.dp
private val LIST_SPACING = 8.dp
private val PLAY_BUTTON_SIZE = 44.dp
private val ACTION_BUTTON_SIZE = 36.dp
private val ACTION_ICON_SIZE = 18.dp
private val INFO_ICON_SIZE = 12.dp
private val EMPTY_ICON_BOX_SIZE = 56.dp
private val EMPTY_ICON_SIZE = 28.dp

// ==================== 主界面 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, topLevel: Boolean = false) {
    val state = rememberHistoryScreenState()

    LaunchedEffect(Unit) { state.fetchAudios() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的音频", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!topLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { state.fetchAudios() }, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { state.fetchAudios() }
                )
                state.audios.isEmpty() -> EmptyState(
                    onBack = { navController.popBackStack() }
                )
                else -> AudioList(
                    audios = state.audios,
                    playingId = state.playingId,
                    downloadingId = state.downloadingId,
                    onPlayClick = state::togglePlay,
                    onDownloadClick = state::download,
                    onImportClick = { navController.popBackStack() }
                )
            }
        }
    }
}

// ==================== 状态管理 ====================

private class HistoryScreenState(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val api: ApiClient,
    private val context: android.content.Context,
) {
    var audios by mutableStateOf<List<TaskAudio>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var playingId by mutableStateOf<String?>(null)
        private set
    var downloadingId by mutableStateOf<String?>(null)
        private set

    fun fetchAudios() {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            try {
                val res = api.getMyAudios()
                if (res.optBoolean("success", false)) {
                    val list = parseAudioList(res)
                    withContext(Dispatchers.Main) { audios = list; loading = false }
                } else {
                    withContext(Dispatchers.Main) {
                        error = res.optString("error", "加载失败")
                        loading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.message ?: "网络错误"
                    loading = false
                }
            }
        }
    }

    fun togglePlay(audioId: String) {
        playingId = if (playingId == audioId) null else audioId
    }

    fun download(audio: TaskAudio) {
        downloadingId = audio.id
        scope.launch(Dispatchers.IO) {
            try {
                val downloadUrl = resolveDownloadUrl(audio)
                if (downloadUrl.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { downloadingId = null }
                    return@launch
                }
                val finalFile = downloadToCache(downloadUrl, audio)
                shareFile(context, finalFile, audio.mimeType)
                withContext(Dispatchers.Main) { downloadingId = null }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { downloadingId = null }
            }
        }
    }

    private fun resolveDownloadUrl(audio: TaskAudio): String? {
        val signedRes = audio.fileKey?.let { api.getSignedUrl(it) }
        return signedRes?.optString("url", "")?.ifEmpty { audio.serverUrl } ?: audio.serverUrl
    }

    private fun downloadToCache(url: String, audio: TaskAudio): File {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connect()
        val safeName = audio.name.ifBlank { "audio_${audio.id}" }
            .replace(Regex("[^\\w.-]"), "_")
        val ext = audio.mimeType?.substringAfter("/")?.let { ".$it" } ?: ""
        val file = File(context.cacheDir, "$safeName$ext")
        FileOutputStream(file).use { out -> conn.inputStream.use { it.copyTo(out) } }
        conn.disconnect()
        return file
    }

    private fun shareFile(context: android.content.Context, file: File, mimeType: String?) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType ?: "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "保存音频"))
    }
}

@Composable
private fun rememberHistoryScreenState(): HistoryScreenState {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val api = remember(context) { ApiClient.get(context) }
    return remember { HistoryScreenState(scope, api, context) }
}

// ==================== 子状态组件 ====================

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        ChatGptLoadingSpinner(
            color = MaterialTheme.colorScheme.onSurface,
            loadingDescription = "正在加载音频",
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = Destructive, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) { Text("重新加载") }
    }
}

@Composable
private fun EmptyState(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(EMPTY_ICON_BOX_SIZE)
                .clip(RoundedCornerShape(16.dp))
                .background(BrandGlowThemed.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = BrandGlowThemed, modifier = Modifier.size(EMPTY_ICON_SIZE))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "音频库还是空的",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "选择音频只会为任务准备播放资源，不会自动保存到这里。请在设置页手动点击\"存入音频库\"。",
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MutedForeground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBack) {
            Text("返回设置", color = BrandGlowThemed, fontWeight = FontWeight.Medium)
        }
    }
}

// ==================== 列表 ====================

@Composable
private fun AudioList(
    audios: List<TaskAudio>,
    playingId: String?,
    downloadingId: String?,
    onPlayClick: (String) -> Unit,
    onDownloadClick: (TaskAudio) -> Unit,
    onImportClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = LIST_HORIZONTAL_PADDING, vertical = LIST_VERTICAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(LIST_SPACING)
    ) {
        item {
            Text("共 ${audios.size} 个已保存音频", fontSize = 13.sp, color = MutedForeground)
            Spacer(Modifier.height(4.dp))
        }
        items(audios) { audio ->
            AudioItem(
                audio = audio,
                isPlaying = playingId == audio.id,
                isDownloading = downloadingId == audio.id,
                onPlayClick = { onPlayClick(audio.id) },
                onDownloadClick = { onDownloadClick(audio) },
                onImportClick = onImportClick,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ==================== 音频卡片 ====================

@Composable
private fun AudioItem(
    audio: TaskAudio,
    isPlaying: Boolean,
    isDownloading: Boolean,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_RADIUS),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayButton(isPlaying = isPlaying, onClick = onPlayClick)
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    audio.name.ifBlank { "未知音频" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                AudioInfoChips(audio)
            }

            ActionButtons(
                isDownloading = isDownloading,
                onDownloadClick = onDownloadClick,
                onImportClick = onImportClick
            )
        }
    }
}

@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(PLAY_BUTTON_SIZE)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(BrandStartThemed, BrandEndThemed)))
            .scale(if (isPlaying) 0.95f else 1f)
            .let { it },
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            PlayingIndicator()
        } else {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "播放",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AudioInfoChips(audio: TaskAudio) {
    val chips = buildList {
        if (audio.size > 0) add(InfoChipData(Icons.Default.Straighten, formatFileSize(audio.size)))
        if (audio.duration > 0) add(InfoChipData(Icons.Default.Schedule, formatDuration(audio.duration)))
        if (audio.createdAt.isNotEmpty()) add(InfoChipData(Icons.Default.CalendarMonth, formatDate(audio.createdAt)))
    }
    if (chips.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { (icon, text) -> InfoChip(icon, text) }
    }
}

private data class InfoChipData(val icon: ImageVector, val text: String)

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(INFO_ICON_SIZE))
        Text(text, fontSize = 11.sp, color = MutedForeground)
    }
}

@Composable
private fun ActionButtons(
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onDownloadClick, enabled = !isDownloading, modifier = Modifier.size(ACTION_BUTTON_SIZE)) {
            if (isDownloading) {
                ChatGptLoadingSpinner(
                    size = 18.dp,
                    color = MutedForeground,
                    loadingDescription = "正在下载",
                )
            } else {
                Icon(Icons.Default.Download, contentDescription = "下载", tint = MutedForeground, modifier = Modifier.size(ACTION_ICON_SIZE))
            }
        }
        IconButton(onClick = onImportClick, modifier = Modifier.size(ACTION_BUTTON_SIZE)) {
            Icon(Icons.Default.Refresh, contentDescription = "导入设置", tint = BrandGlowThemed, modifier = Modifier.size(ACTION_ICON_SIZE))
        }
    }
}

// ==================== 播放指示器 ====================

@Composable
private fun PlayingIndicator() {
    val transition = rememberInfiniteTransition(label = "playing")
    val bars = listOf(
        rememberBarAnim(transition, 0.3f, 1f, 400, "bar1"),
        rememberBarAnim(transition, 1f, 0.3f, 600, "bar2"),
        rememberBarAnim(transition, 0.5f, 0.9f, 500, "bar3"),
    )
    Row(
        modifier = Modifier.size(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bars.forEach { scale -> Bar(scale) }
    }
}

@Composable
private fun rememberBarAnim(
    transition: androidx.compose.animation.core.InfiniteTransition,
    initial: Float,
    target: Float,
    durationMs: Int,
    label: String,
): Float {
    return transition.animateFloat(
        initialValue = initial,
        targetValue = target,
        animationSpec = infiniteRepeatable(animation = tween(durationMs), repeatMode = RepeatMode.Reverse),
        label = label
    ).value
}

@Composable
private fun Bar(scale: Float) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(16.dp * scale)
            .clip(RoundedCornerShape(1.5.dp))
            .background(Color.White)
    )
}

// ==================== 格式化工具 ====================

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    if (bytes < 1024 * 1024) return "${bytes / 1024}KB"
    return String.format(Locale.US, "%.1fMB", bytes / (1024.0 * 1024.0))
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}

private fun formatDate(dateStr: String): String {
    return try {
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val outputFmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        val date = inputFmt.parse(dateStr)
        outputFmt.format(date ?: Date())
    } catch (e: Exception) {
        dateStr.substringBefore('T').ifEmpty { dateStr }
    }
}
