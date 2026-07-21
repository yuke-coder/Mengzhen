package com.mengzhen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.model.parseAudioList
import com.mengzhen.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient.get() }
    var audios by remember { mutableStateOf<List<TaskAudio>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun fetchAudios() {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            try {
                val res = api.getMyAudios()
                if (res.optBoolean("success", false)) {
                    val list = parseAudioList(res)
                    withContext(Dispatchers.Main) {
                        audios = list
                        loading = false
                    }
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

    LaunchedEffect(Unit) { fetchAudios() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的音频") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { fetchAudios() }, enabled = !loading) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(error!!, color = Destructive, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { fetchAudios() }) { Text("重新加载") }
                }
            } else if (audios.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("音频库还是空的", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Foreground)
                    Spacer(Modifier.height(8.dp))
                    Text("在设置页点击\"存入音频库\"来保存音频", fontSize = 14.sp, color = MutedForeground)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("共 ${audios.size} 个已保存音频", fontSize = 13.sp, color = MutedForeground)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(audios) { audio ->
                        AudioItem(audio)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioItem(audio: TaskAudio) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 播放按钮
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(BrandStart, BrandEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "播放", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    audio.name.ifBlank { "未知音频" },
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Foreground,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                val info = buildString {
                    if (audio.size > 0) append(formatFileSize(audio.size))
                    if (audio.duration > 0) { if (isNotEmpty()) append(" · "); append(formatDuration(audio.duration)) }
                }
                Text(info, fontSize = 12.sp, color = MutedForeground)
            }
            // 下载按钮
            IconButton(onClick = { /* TODO: 下载 */ }) {
                Icon(Icons.Default.Download, contentDescription = "下载", tint = MutedForeground, modifier = Modifier.size(18.dp))
            }
        }
    }
}

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
