package com.mengzhen.app.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mengzhen.app.R
import com.mengzhen.app.audio.healing.QqMusicHealingResources
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.api.AudioUploadQueue
import com.mengzhen.app.data.api.AudioUploadState
import com.mengzhen.app.data.model.PlaybackDraft
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.screens.createQqMusicHealingPickerDialog
import com.mengzhen.app.ui.screens.rememberLocalAudioArtwork
import com.mengzhen.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

// === Helpers ===

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "%.1f KB".format(bytes / 1024.0)
    return "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0:00"
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

private fun getFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
    var name = "audio_${System.currentTimeMillis()}"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIdx >= 0) cursor.getString(nameIdx)?.let { name = it }
            if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
        }
    }
    return name to size
}

private fun getAudioDuration(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val ms = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?: 0
        ms / 1000
    } catch (_: Exception) {
        0
    } finally {
        retriever.release()
    }
}

private fun isSupportedAudio(fileName: String, mimeType: String?): Boolean {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val supported = listOf("mp3", "wav", "ogg", "m4a", "flac", "aac")
    return ext in supported || (mimeType?.startsWith("audio/") == true)
}

private fun duplicateKey(fileName: String, fileSize: Long): String =
    "${fileName.trim().lowercase(Locale.ROOT)}::$fileSize"

/**
 * 喜马拉雅 main_ting_list_detail_track_item_layout 的封面结构：
 * 48dp 方形封面、4dp 圆角，默认态右下角叠放播放标识。
 */
@Composable
private fun AudioListArtwork(
    audio: TaskAudio,
    selectionMode: Boolean,
    selected: Boolean,
) {
    val artwork by rememberLocalAudioArtwork(
        audio.localUri ?: audio.serverUrl,
        audio.artworkUri,
    )
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.xm_ad_default_album),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        if (selectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f)),
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (selected) {
                            Color(0xFFFF4444)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        } else {
            Image(
                painter = painterResource(R.drawable.arg_res_0x7f082580),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp)
                    .size(17.dp),
            )
        }
    }
}

/**
 * Keep the selected audio in app-private storage.
 *
 * A persisted document grant can still disappear when provider data is restored, a picker
 * implementation does not really persist its grant, or a task is recreated from stored JSON.
 * Playback, scheduled alarms and guest mode must not depend on that external grant, so the
 * private copy is the authoritative local source. The grant is retained only as a fallback when
 * an unusual provider lets us keep the URI but fails while streaming the copy.
 */
private fun durableAudioUri(
    context: Context,
    source: Uri,
    audioId: String,
    fileName: String,
): String {
    if (source.scheme != "content") return source.toString()

    val hasPersistedReadGrant = try {
        context.contentResolver.takePersistableUriPermission(
            source,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        context.contentResolver.persistedUriPermissions.any {
            it.uri == source && it.isReadPermission
        }
    } catch (_: RuntimeException) {
        false
    }

    val extension = fileName
        .substringAfterLast('.', "")
        .lowercase(Locale.ROOT)
        .takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
    val importDir = File(context.filesDir, "audio_imports").apply {
        if (!exists() && !mkdirs()) throw IOException("无法创建本地音频目录")
    }
    val destination = File(importDir, buildString {
        append(audioId)
        extension?.let { append('.').append(it) }
    })
    try {
        val input = context.contentResolver.openInputStream(source)
            ?: throw IOException("无法读取所选音频")
        input.use {
            FileOutputStream(destination).use { output -> it.copyTo(output) }
        }
    } catch (error: Exception) {
        destination.delete()
        if (hasPersistedReadGrant) return source.toString()
        throw error
    }
    return Uri.fromFile(destination).toString()
}

// === Main Composable ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioUploadSection(
    draft: PlaybackDraft,
    onDraftChange: (PlaybackDraft) -> Unit,
    onSelectionReady: (updatedDraft: PlaybackDraft, selectedAudios: List<TaskAudio>) -> Unit,
    onOpenAudio: (TaskAudio) -> Unit,
    onOpenBiliCache: () -> Unit,
    api: ApiClient,
    isLoggedIn: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { TaskStore.get(context) }
    val uploadQueue = remember(context) { AudioUploadQueue.get(context) }
    val uploadStates by uploadQueue.states.collectAsState()
    var librarySavingIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(uploadStates) {
        if (uploadStates.values.any { it is AudioUploadState.Success }) {
            val persisted = store.getDraft()
            if (persisted != draft) onDraftChange(persisted)
        }
    }

    // 游客文件始终先持久化在本机；登录后再补传尚未同步的文件。
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            store.getDraft().audios
                .filter {
                    !it.localUri.isNullOrBlank() &&
                        it.fileKey.isNullOrBlank() &&
                        it.serverUrl.isNullOrBlank()
                }
                .forEach(uploadQueue::enqueue)
        }
    }

    var selectedAudioIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteIds by remember { mutableStateOf<Set<String>?>(null) }
    var draggingAudioId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var showAudioSourceSheet by remember { mutableStateOf(false) }
    var showHealingPicker by remember { mutableStateOf(false) }
    val audioRowHeightPx = with(LocalDensity.current) { 60.dp.toPx() }
    val selectionMode = selectedAudioIds.isNotEmpty()

    LaunchedEffect(draft.audios.map { it.id }) {
        val availableIds = draft.audios.mapTo(mutableSetOf()) { it.id }
        selectedAudioIds = selectedAudioIds.intersect(availableIds)
    }

    fun selectAudio(audioId: String) {
        selectedAudioIds = selectedAudioIds + audioId
    }

    fun toggleAudioSelection(audioId: String) {
        selectedAudioIds = if (audioId in selectedAudioIds) {
            selectedAudioIds - audioId
        } else {
            selectedAudioIds + audioId
        }
    }

    fun moveAudio(audioId: String, direction: Int): Boolean {
        var moved = false
        val updated = store.updateDraft { current ->
            val from = current.audios.indexOfFirst { it.id == audioId }
            val to = from + direction
            if (from < 0 || to !in current.audios.indices) {
                current
            } else {
                moved = true
                val reordered = current.audios.toMutableList()
                reordered.add(to, reordered.removeAt(from))
                current.copy(audios = reordered)
            }
        }
        if (moved) onDraftChange(updated)
        return moved
    }

    // File picker
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        scope.launch(Dispatchers.IO) {
            val newAudios = mutableListOf<TaskAudio>()
            val messages = mutableListOf<String>()
            val seen = draft.audios
                .mapTo(mutableSetOf()) { duplicateKey(it.name, it.size) }

            for (uri in uris) {
                val (fileName, fileSize) = getFileNameAndSize(context, uri)
                val mimeType = context.contentResolver.getType(uri) ?: "audio/mpeg"

                if (!isSupportedAudio(fileName, mimeType)) {
                    messages += "「$fileName」不是支持的音频格式"
                    continue
                }

                if (!seen.add(duplicateKey(fileName, fileSize))) {
                    messages += "「$fileName」已存在，已跳过"
                    continue
                }

                val duration = getAudioDuration(context, uri)
                val audioId = "local_${UUID.randomUUID()}"
                try {
                    newAudios += TaskAudio(
                        id = audioId,
                        name = fileName,
                        duration = duration,
                        size = fileSize,
                        localUri = durableAudioUri(context, uri, audioId, fileName),
                        mimeType = mimeType,
                    )
                } catch (error: Exception) {
                    messages += "「$fileName」读取失败：${error.message ?: "未知错误"}"
                }
            }

            if (newAudios.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    // Persist first. Navigation happens only after this synchronous store update,
                    // so the player never depends on the picker Activity's temporary grant.
                    var persistedSelection = emptyList<TaskAudio>()
                    val updatedDraft = store.updateDraft { current ->
                        val existingKeys = current.audios
                            .mapTo(mutableSetOf()) { duplicateKey(it.name, it.size) }
                        persistedSelection = newAudios.filter {
                            existingKeys.add(duplicateKey(it.name, it.size))
                        }
                        current.copy(audios = current.audios + persistedSelection)
                    }
                    onDraftChange(updatedDraft)
                    if (persistedSelection.isEmpty()) {
                        AppNotice.info(context, "所选音频已经在列表中")
                        return@withContext
                    }
                    onSelectionReady(updatedDraft, persistedSelection)
                    // The session is now durable. Queue work uses an application scope and keeps
                    // running after this composable is removed by navigation.
                    if (isLoggedIn) persistedSelection.forEach(uploadQueue::enqueue)
                }
            }

            if (newAudios.isEmpty() && messages.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    AppNotice.warning(context, messages.joinToString("\n"))
                }
            }
        }
    }

    // === UI ===

    Box {
        Column {
            // Upload area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAudioSourceSheet = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(BrandGlow.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = BrandGlow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击选择音频",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (draft.audios.isNotEmpty()) {
                        Text(
                            "已添加 ${draft.audios.size} 个音频",
                            fontSize = 12.sp,
                            color = BrandStart
                        )
                    }
                }
            }

            // Audio list
            if (draft.audios.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectionMode) {
                        IconButton(
                            onClick = { selectedAudioIds = emptySet() },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "退出选择",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "已选 ${selectedAudioIds.size} 项",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.weight(1f))

                        val allSelected = selectedAudioIds.size == draft.audios.size
                        TextButton(
                            onClick = {
                                selectedAudioIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    draft.audios.mapTo(mutableSetOf()) { it.id }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                if (allSelected) "取消全选" else "全选",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        TextButton(
                            onClick = { pendingDeleteIds = selectedAudioIds },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "删除",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "已选择 ${draft.audios.size} 个音频",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.weight(1f))

                        val unsavedAudios =
                            draft.audios.filter { !it.savedToLibrary && it.fileKey != null }
                        if (unsavedAudios.isNotEmpty() && isLoggedIn) {
                            val anySaving = unsavedAudios.any { it.id in librarySavingIds }
                            TextButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        for (audio in unsavedAudios) {
                                            withContext(Dispatchers.Main) {
                                                librarySavingIds = librarySavingIds + audio.id
                                            }
                                            try {
                                                val res = api.saveToLibrary(audio.fileKey!!)
                                                if (res.optBoolean("success", false)) {
                                                    val updated = store.updateDraft { current ->
                                                        current.copy(audios = current.audios.map {
                                                            if (it.id == audio.id) {
                                                                it.copy(savedToLibrary = true)
                                                            } else {
                                                                it
                                                            }
                                                        })
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        onDraftChange(updated)
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                // Keep the cloud action available for a later retry.
                                            } finally {
                                                withContext(Dispatchers.Main) {
                                                    librarySavingIds = librarySavingIds - audio.id
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !anySaving,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                if (anySaving) {
                                    ChatGptLoadingSpinner(
                                        size = 16.dp,
                                        loadingDescription = "正在保存全部音频",
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("保存中...", fontSize = 12.sp)
                                } else {
                                    Text(
                                        "全部存入音频库",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFF4444),
                                    )
                                }
                            }
                        } else if (draft.audios.size > 1) {
                            Text(
                                "长按选择或拖动排序",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                draft.audios.forEach { audio ->
                    key(audio.id) {
                        val uploadState = uploadStates[audio.id]
                        val isUploading = uploadState is AudioUploadState.Queued ||
                            uploadState is AudioUploadState.Uploading
                        val uploadProgress =
                            (uploadState as? AudioUploadState.Uploading)?.progress
                        val uploadError = (uploadState as? AudioUploadState.Failed)?.message
                        val isDragging = draggingAudioId == audio.id
                        val isSelected = audio.id in selectedAudioIds

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        Color(0xFFFF4444).copy(alpha = 0.08f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .semantics(mergeDescendants = true) {
                                    onClick(
                                        label =
                                            if (selectionMode) {
                                                "切换选择"
                                            } else {
                                                "打开播放页"
                                            },
                                    ) {
                                        if (selectionMode) {
                                            toggleAudioSelection(audio.id)
                                        } else {
                                            onOpenAudio(audio)
                                        }
                                        true
                                    }
                                    onLongClick(label = "选择并拖动排序") {
                                        selectAudio(audio.id)
                                        true
                                    }
                                }
                                .pointerInput(audio.id) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val longPress = awaitLongPressOrCancellation(down.id)

                                        if (longPress == null) {
                                            val up = currentEvent.changes
                                                .firstOrNull { it.id == down.id }
                                            if (up != null && !up.pressed && !up.isConsumed) {
                                                if (selectedAudioIds.isNotEmpty()) {
                                                    toggleAudioSelection(audio.id)
                                                } else {
                                                    onOpenAudio(audio)
                                                }
                                            }
                                            return@awaitEachGesture
                                        }

                                        try {
                                            selectAudio(audio.id)
                                            draggingAudioId = audio.id
                                            dragOffsetY = 0f
                                            drag(longPress.id) { change ->
                                                val dragY = change.positionChange().y
                                                if (dragY == 0f) return@drag

                                                change.consume()
                                                dragOffsetY += dragY
                                                if (abs(dragOffsetY) >= audioRowHeightPx / 2f) {
                                                    val direction =
                                                        if (dragOffsetY > 0f) 1 else -1
                                                    if (moveAudio(audio.id, direction)) {
                                                        dragOffsetY -=
                                                            direction * audioRowHeightPx
                                                    } else {
                                                        dragOffsetY = 0f
                                                    }
                                                }
                                            }
                                        } finally {
                                            draggingAudioId = null
                                            dragOffsetY = 0f
                                        }
                                    }
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AudioListArtwork(
                                    audio = audio,
                                    selectionMode = selectionMode,
                                    selected = isSelected,
                                )
                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        audio.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        if (audio.duration > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(
                                                        R.drawable.arg_res_0x7f080b23
                                                    ),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(12.dp),
                                                )
                                                Spacer(Modifier.width(2.dp))
                                                Text(
                                                    formatDuration(audio.duration),
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                        Text(
                                            formatFileSize(audio.size),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (audio.savedToLibrary) {
                                            Text(
                                                "已存音频库",
                                                fontSize = 12.sp,
                                                color = Color(0xFF48CD7D),
                                            )
                                        } else if (!isLoggedIn) {
                                            Text(
                                                "本机",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }

                                if (
                                    !selectionMode &&
                                    !audio.savedToLibrary &&
                                    audio.fileKey != null &&
                                    isLoggedIn
                                ) {
                                    val saving = audio.id in librarySavingIds
                                    IconButton(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                withContext(Dispatchers.Main) {
                                                    librarySavingIds = librarySavingIds + audio.id
                                                }
                                                try {
                                                    val res = api.saveToLibrary(audio.fileKey)
                                                    if (res.optBoolean("success", false)) {
                                                        val updated = store.updateDraft { current ->
                                                            current.copy(audios = current.audios.map {
                                                                if (it.id == audio.id) {
                                                                    it.copy(savedToLibrary = true)
                                                                } else {
                                                                    it
                                                                }
                                                            })
                                                        }
                                                        withContext(Dispatchers.Main) {
                                                            onDraftChange(updated)
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                    // Keep the cloud action available for a later retry.
                                                } finally {
                                                    withContext(Dispatchers.Main) {
                                                        librarySavingIds = librarySavingIds - audio.id
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !saving,
                                        modifier = Modifier.size(42.dp),
                                    ) {
                                        if (saving) {
                                            ChatGptLoadingSpinner(
                                                size = 18.dp,
                                                loadingDescription = "正在保存音频",
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.CloudUpload,
                                                contentDescription = "存入音频库",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            if (isUploading) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 76.dp, end = 42.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ChatGptLoadingSpinner(
                                        size = 14.dp,
                                        color = Color(0xFFFF4444),
                                        loadingDescription = "正在上传音频",
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        uploadProgress?.let { "正在上传 $it%" } ?: "等待上传",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF4444),
                                    )
                                }
                            }

                            if (uploadError != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 76.dp, end = 8.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        uploadError,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    if (isLoggedIn) {
                                        TextButton(
                                            onClick = { uploadQueue.enqueue(audio) },
                                            enabled = !audio.localUri.isNullOrBlank(),
                                        ) {
                                            Text("重试", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }

    }

    pendingDeleteIds?.let { deleteIds ->
        AlertDialog(
            onDismissRequest = { pendingDeleteIds = null },
            title = {
                Text(
                    if (deleteIds.size == draft.audios.size) {
                        "确认清空当前音频列表？"
                    } else {
                        "确认删除已选的 ${deleteIds.size} 个音频？"
                    }
                )
            },
            text = { Text("只会移除当前配置中的引用，已经保存的任务不会受到影响。") },
            confirmButton = {
                TextButton(onClick = {
                    val updated = store.updateDraft { current ->
                        current.copy(audios = current.audios.filterNot { it.id in deleteIds })
                    }
                    onDraftChange(updated)
                    selectedAudioIds = emptySet()
                    pendingDeleteIds = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIds = null }) { Text("取消") }
            }
        )
    }

    if (showAudioSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioSourceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
            ) {
                Text(
                    "选择音频来源",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                ListItem(
                    headlineContent = { Text("手机音频文件") },
                    supportingContent = { Text("选择 MP3、M4A、FLAC 等本地音频") },
                    leadingContent = {
                        Icon(Icons.Default.AudioFile, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showAudioSourceSheet = false
                        audioPicker.launch(arrayOf("audio/*"))
                    },
                )
                ListItem(
                    headlineContent = { Text("B站离线缓存") },
                    supportingContent = { Text("无损提取缓存音轨并加入当前列表") },
                    leadingContent = {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showAudioSourceSheet = false
                        onOpenBiliCache()
                    },
                )
                ListItem(
                    headlineContent = { Text("更多助眠音乐") },
                    supportingContent = { Text("星空、海洋、雨林，实时合成后加入音频列表") },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.qq_healing_sky),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showAudioSourceSheet = false
                        showHealingPicker = true
                    },
                )
            }
        }
    }

    if (showHealingPicker) {
        DisposableEffect(context) {
            val dialog = createQqMusicHealingPickerDialog(
                context = context,
                onReturn = { showHealingPicker = false },
                onSceneSelected = { scene ->
                    showHealingPicker = false
                    val audio = scene.asTaskAudio(context)
                    val updated = store.updateDraft { current ->
                        current.copy(
                            audios = current.audios
                                .filterNot { it.id == audio.id }
                                .plus(audio),
                        )
                    }
                    onDraftChange(updated)
                    QqMusicHealingResources.prepareAsync(context)
                    onSelectionReady(updated, listOf(audio))
                    AppNotice.info(context, "${scene.title}已加入列表，正在准备播放资源")
                },
            )
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }
}
