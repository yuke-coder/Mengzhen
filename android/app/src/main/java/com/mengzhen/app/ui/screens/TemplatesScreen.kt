package com.mengzhen.app.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.SeekBar
import com.airbnb.lottie.LottieAnimationView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.audio.PlayProgressStore
import com.mengzhen.app.audio.PlaybackSnapshot
import com.mengzhen.app.audio.PlaybackStateStore
import com.mengzhen.app.audio.PlaybackTransportState
import com.mengzhen.app.audio.QqMusicPlaybackMode
import com.mengzhen.app.audio.healing.QqMusicHealingResources
import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.ScheduledStopMode
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.model.TaskStartTime
import com.mengzhen.app.data.model.TaskStatus
import com.mengzhen.app.data.model.effectiveScheduledStopDurationSeconds
import com.mengzhen.app.data.model.hasActiveSchedule
import com.mengzhen.app.data.model.hasConfiguredStop
import com.mengzhen.app.data.store.AudioNoteStore
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.scheduler.AlarmScheduler
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class PlayerUiState {
    READY,
    SCHEDULED,
    PREPARING,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    navController: NavController,
    taskId: String,
    openTimer: Boolean = false,
    timerMode: String = Screen.Templates.TIMER_MODE_FULL,
) {
    val context = LocalContext.current
    val store = remember { TaskStore.get(context) }
    val noteStore = remember { AudioNoteStore.get(context) }
    val scheduler = remember { AlarmScheduler.get(context) }
    val playbackStore = remember { PlaybackStateStore.get(context) }
    val progressStore = remember { PlayProgressStore.get(context) }
    val scope = rememberCoroutineScope()
    val playbackSnapshot by playbackStore.snapshot.collectAsState()

    var task by remember(taskId) { mutableStateOf(store.getTaskById(taskId)) }
    var selectedTrackIndex by remember(taskId) { mutableIntStateOf(0) }
    var showTimerSheet by remember(taskId, openTimer) { mutableStateOf(openTimer) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showSkipHeadTailSheet by remember { mutableStateOf(false) }
    var showSoundEffectQualitySheet by remember { mutableStateOf(false) }
    var noteEditingAudio by remember(taskId) { mutableStateOf<TaskAudio?>(null) }
    val playerPreferences = remember {
        context.getSharedPreferences("ximalaya_player_actions", Context.MODE_PRIVATE)
    }
    var playbackSpeed by remember {
        mutableFloatStateOf(AudioPlaybackService.getPlaybackSpeed(context))
    }
    var speedOnlyForCurrentTask by remember(taskId) {
        mutableStateOf(playerPreferences.getBoolean("speed_only_$taskId", false))
    }
    var liked by remember(taskId) {
        mutableStateOf(playerPreferences.getBoolean("liked_$taskId", false))
    }

    DisposableEffect(store, taskId) {
        val listener = store.registerTasksChangedListener { tasks ->
            task = tasks.firstOrNull { it.id == taskId }
        }
        onDispose { store.unregisterTasksChangedListener(listener) }
    }

    val snapshot = playbackSnapshot.takeIf { it.taskId == taskId }
    val hasLivePlayback = snapshot != null &&
        !snapshot.isTerminal &&
        AudioPlaybackService.getCurrentTaskId() == taskId
    LaunchedEffect(snapshot?.trackIndex) {
        snapshot?.trackIndex?.let { selectedTrackIndex = it }
    }

    val state = playerState(task, snapshot)
    val liveQueueAudioId = if (hasLivePlayback) {
        AudioPlaybackService.getCurrentQueueIds().getOrNull(snapshot?.trackIndex ?: -1)
    } else {
        null
    }
    val currentAudio = liveQueueAudioId?.let { queueId ->
        task?.audios?.firstOrNull { qqMusicQueueAudioId(it) == queueId }
    } ?: task?.audios?.getOrNull(selectedTrackIndex)
        ?: task?.audios?.firstOrNull()
    var currentNote by remember(currentAudio) {
        mutableStateOf(currentAudio?.let(noteStore::get).orEmpty())
    }
    val progressAudioId = currentAudio?.let { audio ->
        audio.id.ifBlank {
            audio.fileKey?.takeIf(String::isNotBlank)
                ?: audio.localUri?.takeIf(String::isNotBlank)
                ?: audio.serverUrl?.takeIf(String::isNotBlank)
                ?: audio.dbKey.orEmpty()
        }
    }.orEmpty()
    val savedProgress = remember(progressAudioId) {
        progressAudioId.takeIf(String::isNotBlank)?.let(progressStore::getLocal)
    }
    var idlePositionMs by remember(progressAudioId) {
        mutableLongStateOf((savedProgress?.first ?: 0L) * 1_000L)
    }
    var idleDurationMs by remember(progressAudioId) {
        mutableLongStateOf((savedProgress?.second ?: 0L) * 1_000L)
    }
    LaunchedEffect(hasLivePlayback, snapshot?.positionMs, snapshot?.durationMs) {
        if (hasLivePlayback) {
            idlePositionMs = snapshot?.positionMs ?: idlePositionMs
            idleDurationMs = snapshot?.durationMs?.takeIf { it > 0L } ?: idleDurationMs
        }
    }
    val title = snapshot?.trackName?.takeIf(String::isNotBlank)
        ?: currentAudio?.name
        ?: task?.name
        ?: "音频播放器"
    val returnToSettings = {
        if (!navController.popBackStack()) {
            navController.navigate(Screen.Settings.route) { launchSingleTop = true }
        }
        Unit
    }


    val sheetTask = task

    if (showTimerSheet && sheetTask != null) {
        val timerTask = sheetTask
        val timerTaskId = timerTask.id
        val configuresScheduledPlayback = timerTask.hasActiveSchedule()
        val editsCombinedTimer =
            timerMode == Screen.Templates.TIMER_MODE_COMBINED ||
                timerTask.hasConfiguredStop()
        val currentTaskHasSession = {
            AudioPlaybackService.getCurrentTaskId() == taskId
        }
        val scheduledSessionIsActive = {
            AudioPlaybackService.getCurrentTaskId() == timerTaskId &&
                AudioPlaybackService.isCurrentSessionScheduled()
        }
        val configuresScheduledStop = {
            store.getTaskById(timerTaskId)?.hasActiveSchedule() == true ||
                scheduledSessionIsActive()
        }
        fun updateTimerTask(transform: (ScheduledTask) -> ScheduledTask): ScheduledTask? {
            val updated = store.updateTask(timerTaskId, transform)
            if (timerTaskId == taskId) task = updated
            return updated
        }
        val scheduledPlaybackIsRunningNow = scheduledSessionIsActive()
        val initialSleepDurationSeconds = if (
            scheduledPlaybackIsRunningNow
        ) {
            AudioPlaybackService.getConfiguredSleepDurationSeconds()
                .takeIf { it > 0 }
                ?: timerTask.effectiveScheduledStopDurationSeconds().takeIf { it > 0 }
                ?: AudioPlaybackService.getSleepTimerRemainingSeconds(context)
        } else if (
            configuresScheduledPlayback &&
            timerTask.scheduledStopMode == ScheduledStopMode.MINUTES
        ) {
            timerTask.effectiveScheduledStopDurationSeconds()
        } else if (!configuresScheduledPlayback && currentTaskHasSession()) {
            AudioPlaybackService.getConfiguredSleepDurationSeconds()
                .takeIf { it > 0 }
                ?: AudioPlaybackService.getSleepTimerRemainingSeconds(context)
        } else {
            0
        }
        val initialSleepTracks = if (
            scheduledPlaybackIsRunningNow
        ) {
            AudioPlaybackService.getSleepTrackCount(context)
        } else if (
            configuresScheduledPlayback &&
            timerTask.scheduledStopMode == ScheduledStopMode.TRACKS
        ) {
            timerTask.scheduledStopValue
        } else if (!configuresScheduledPlayback && currentTaskHasSession()) {
            AudioPlaybackService.getSleepTrackCount(context)
        } else {
            0
        }
        XimalayaSourcePlanTerminalSheet(
            task = timerTask,
            initialSleepDurationSeconds = initialSleepDurationSeconds,
            initialSleepTracks = initialSleepTracks,
            initialFinishCurrentTrack = if (configuresScheduledPlayback) {
                if (scheduledPlaybackIsRunningNow) {
                    AudioPlaybackService.getSleepFinishCurrentTrack(context)
                } else {
                    timerTask.scheduledFinishCurrentTrack
                }
            } else if (currentTaskHasSession()) {
                AudioPlaybackService.getSleepFinishCurrentTrack(context)
            } else {
                false
            },
            initialFadeOutEnabled = when {
                scheduledPlaybackIsRunningNow -> AudioPlaybackService.isSleepFadeOutEnabled()
                configuresScheduledPlayback -> timerTask.enableFadeOut
                currentTaskHasSession() -> AudioPlaybackService.isSleepFadeOutEnabled()
                else -> false
            },
            initialFadeOutSeconds = when {
                scheduledPlaybackIsRunningNow ->
                    AudioPlaybackService.getSleepFadeOutSeconds()
                configuresScheduledPlayback -> timerTask.fadeOutDuration
                currentTaskHasSession() -> AudioPlaybackService.getSleepFadeOutSeconds()
                else -> 0
            },
            canEnableSleepTimer = {
                store.getTaskById(timerTaskId)?.hasActiveSchedule() == true ||
                    scheduledSessionIsActive() ||
                    currentTaskHasSession()
            },
            isSleepTimerRunning = {
                if (configuresScheduledStop()) {
                    scheduledSessionIsActive()
                } else {
                    currentTaskHasSession()
                }
            },
            onDismiss = { showTimerSheet = false },
            onOpenAlarmManager = {
                showTimerSheet = false
                if (openTimer || timerTask.hasActiveSchedule()) {
                    navController.navigate(
                        Screen.AlarmEditor.createRoute(
                            taskId = timerTaskId,
                            openTimerAfterSave = editsCombinedTimer,
                        )
                    )
                } else {
                    navController.navigate(Screen.AlarmManager.createRoute(taskId))
                }
            },
            onScheduleEnabledChanged = { requested ->
                if (requested) {
                    val synchronizedTask = if (currentTaskHasSession()) {
                        val durationSeconds = AudioPlaybackService
                            .getConfiguredSleepDurationSeconds()
                            .takeIf { it > 0 }
                            ?: AudioPlaybackService.getSleepTimerRemainingSeconds(context)
                        val tracks = AudioPlaybackService.getSleepTrackCount(context)
                        updateTimerTask { current ->
                            current.copy(
                                scheduledStopMode = when {
                                    tracks > 0 -> ScheduledStopMode.TRACKS
                                    durationSeconds > 0 -> ScheduledStopMode.MINUTES
                                    else -> ScheduledStopMode.NONE
                                },
                                scheduledStopValue = if (tracks > 0) {
                                    tracks
                                } else {
                                    (durationSeconds + 59) / 60
                                },
                                scheduledStopDurationSeconds = if (tracks > 0) {
                                    0
                                } else {
                                    durationSeconds
                                },
                                scheduledFinishCurrentTrack =
                                    AudioPlaybackService.getSleepFinishCurrentTrack(context),
                                enableFadeOut =
                                    AudioPlaybackService.isSleepFadeOutEnabled(),
                                fadeOutDuration =
                                    AudioPlaybackService.getSleepFadeOutSeconds(),
                            )
                        }
                    } else {
                        store.getTaskById(timerTaskId)
                    }
                    showTimerSheet = false
                    navController.navigate(
                        Screen.AlarmEditor.createRoute(
                            taskId = timerTaskId,
                            openTimerAfterSave = editsCombinedTimer ||
                                synchronizedTask?.let {
                                    it.hasConfiguredStop()
                                } == true,
                        )
                    )
                    false
                } else {
                    val beforeDisable = store.getTaskById(timerTaskId)
                    val transfersToManualPlayback =
                        timerTaskId == taskId && currentTaskHasSession() &&
                            !AudioPlaybackService.isCurrentSessionScheduled()
                    val updated = scheduler.disableSchedule(timerTaskId)
                    if (updated == null) {
                        AppNotice.error(context, "定时启播关闭失败")
                        true
                    } else {
                        if (transfersToManualPlayback && beforeDisable != null) {
                            AudioPlaybackService.setSleepFadeOut(
                                context,
                                beforeDisable.enableFadeOut,
                                beforeDisable.fadeOutDuration,
                            )
                            AudioPlaybackService.setSleepFinishCurrentTrack(
                                context,
                                beforeDisable.scheduledFinishCurrentTrack,
                            )
                            when (beforeDisable.scheduledStopMode) {
                                ScheduledStopMode.MINUTES -> {
                                    val durationSeconds = beforeDisable
                                        .effectiveScheduledStopDurationSeconds()
                                    val startedAt = AudioPlaybackService.getPlaybackStartedAtMs()
                                        .takeIf { it > 0L }
                                        ?: System.currentTimeMillis()
                                    AudioPlaybackService.setSleepTimerDuration(
                                        context,
                                        durationSeconds,
                                        startedAt + durationSeconds * 1_000L,
                                    )
                                }
                                ScheduledStopMode.TRACKS ->
                                    AudioPlaybackService.setSleepAfterTracks(
                                        context,
                                        beforeDisable.scheduledStopValue,
                                    )
                                ScheduledStopMode.NONE -> Unit
                            }
                        }
                        if (timerTaskId == taskId) task = updated
                        AppNotice.success(context, "定时启播已关闭")
                        updated.hasActiveSchedule()
                    }
                }
            },
            onSleepDuration = { durationSeconds ->
                val safeDuration = durationSeconds.coerceAtLeast(0)
                val scheduledStop = configuresScheduledStop()
                val updated = updateTimerTask { current ->
                    val playbackStartedAt = current.executionStartedAt
                        ?: AudioPlaybackService.getPlaybackStartedAtMs().takeIf { it > 0L }
                    current.copy(
                        scheduledStopMode = if (safeDuration > 0) {
                            ScheduledStopMode.MINUTES
                        } else {
                            ScheduledStopMode.NONE
                        },
                        scheduledStopValue = (safeDuration + 59) / 60,
                        scheduledStopDurationSeconds = safeDuration,
                        executionEndsAt = if (
                            current.status == TaskStatus.EXECUTING &&
                            safeDuration > 0 &&
                            playbackStartedAt != null
                        ) {
                            playbackStartedAt + safeDuration * 1_000L
                        } else if (current.status == TaskStatus.EXECUTING) {
                            null
                        } else {
                            current.executionEndsAt
                        },
                    )
                }
                if (scheduledSessionIsActive() || !scheduledStop && currentTaskHasSession()) {
                    val playbackStartedAt = AudioPlaybackService.getPlaybackStartedAtMs()
                        .takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                    AudioPlaybackService.setSleepTimerDuration(
                        context,
                        safeDuration,
                        updated?.executionEndsAt
                            ?.takeIf { it > 0L }
                            ?: if (safeDuration > 0) {
                                playbackStartedAt + safeDuration * 1_000L
                            } else {
                                0L
                            },
                    )
                }
                AppNotice.success(
                    context,
                    when {
                        safeDuration == 0 -> "定时关闭已取消"
                        scheduledStop -> "定时启播后的播放时长已设为" +
                            sourceDurationText(safeDuration)
                        else -> "播放时长已设为${sourceDurationText(safeDuration)}"
                    },
                )
            },
            onSleepTracks = { count ->
                val scheduledStop = configuresScheduledStop()
                updateTimerTask { current ->
                    current.copy(
                        scheduledStopMode = if (count > 0) {
                            ScheduledStopMode.TRACKS
                        } else {
                            ScheduledStopMode.NONE
                        },
                        scheduledStopValue = count.coerceAtLeast(0),
                        scheduledStopDurationSeconds = 0,
                        executionEndsAt = if (current.status == TaskStatus.EXECUTING) {
                            null
                        } else {
                            current.executionEndsAt
                        },
                    )
                }
                if (scheduledSessionIsActive() || !scheduledStop && currentTaskHasSession()) {
                    AudioPlaybackService.setSleepAfterTracks(context, count)
                }
                AppNotice.success(
                    context,
                    when {
                        scheduledStop && count == 1 ->
                            "定时启播后将在本集播放完毕时停止"
                        scheduledStop ->
                            "定时启播后将在播完 $count 集时停止"
                        count == 1 -> "将在本集播放后停止"
                        else -> "将在播完 $count 集后停止"
                    },
                )
            },
            onFinishCurrentTrackChanged = { enabled ->
                val scheduledStop = configuresScheduledStop()
                updateTimerTask {
                    it.copy(scheduledFinishCurrentTrack = enabled)
                }
                if (scheduledSessionIsActive() || !scheduledStop && currentTaskHasSession()) {
                    AudioPlaybackService.setSleepFinishCurrentTrack(context, enabled)
                }
            },
            onFadeOutChanged = { enabled, seconds ->
                val scheduledStop = configuresScheduledStop()
                updateTimerTask {
                    it.copy(
                        enableFadeOut = enabled,
                        fadeOutDuration = seconds.coerceAtLeast(0),
                    )
                }
                if (scheduledSessionIsActive() || !scheduledStop && currentTaskHasSession()) {
                    AudioPlaybackService.setSleepFadeOut(context, enabled, seconds)
                }
            },
            onHealingSceneSelected = { scene ->
                showTimerSheet = false
                AppNotice.info(context, "正在准备${scene.title}")
                scope.launch {
                    runCatching {
                        QqMusicHealingResources.prepare(context)
                    }.onSuccess {
                        val healingAudio = scene.asTaskAudio(context)
                        val updated = store.updateTask(timerTaskId) { current ->
                            val audios = current.audios.toMutableList()
                            val existing = audios.indexOfFirst { it.id == healingAudio.id }
                            if (existing >= 0) {
                                audios[existing] = healingAudio
                            } else {
                                audios += healingAudio
                            }
                            current.copy(audios = audios)
                        }
                        if (timerTaskId == taskId) task = updated
                        AudioPlaybackService.playHealing(
                            context = context,
                            taskId = timerTaskId,
                            taskName = updated?.name ?: timerTask.name,
                            scene = scene,
                        )
                        AppNotice.success(context, "${scene.title}已开始播放")
                    }.onFailure { error ->
                        AppNotice.error(
                            context,
                            error.message ?: "助眠资源准备失败",
                        )
                    }
                }
            },
        )
    }

    if (showSpeedSheet) {
        XimalayaSourceSpeedSheet(
            currentSpeed = playbackSpeed,
            applyToCurrentTask = speedOnlyForCurrentTask,
            onSpeedSelected = { speed ->
                playbackSpeed = speed
                AudioPlaybackService.setPlaybackSpeed(context, speed)
            },
            onApplyScopeChanged = { onlyCurrentTask ->
                speedOnlyForCurrentTask = onlyCurrentTask
                playerPreferences.edit()
                    .putBoolean("speed_only_$taskId", onlyCurrentTask)
                    .apply()
            },
            onDismiss = { showSpeedSheet = false },
        )
    }

    if (showSoundEffectQualitySheet) {
        XimalayaSourceSoundEffectQualitySheet(
            initialEffect = AudioPlaybackService.getSoundEffect(context),
            onEffectSelected = { effect ->
                AudioPlaybackService.setSoundEffect(context, effect)
            },
            onDismiss = { showSoundEffectQualitySheet = false },
        )
    }

    if (showMoreSheet && sheetTask != null) {
        XimalayaSourceMoreActionSheet(
            liked = liked,
            notePreview = currentNote,
            onAction = { action ->
                when (action) {
                    XimalayaMoreAction.SHARE ->
                        shareCurrentAudio(context, currentAudio, title)

                    XimalayaMoreAction.PLAY_SETTINGS -> showSpeedSheet = true
                    XimalayaMoreAction.FAVORITE -> {
                        liked = !liked
                        playerPreferences.edit()
                            .putBoolean("liked_$taskId", liked)
                            .apply()
                        AppNotice.success(context, if (liked) "收藏成功" else "已取消收藏")
                    }

                    XimalayaMoreAction.DOWNLOAD ->
                        AppNotice.info(context, "该音频已保存在本地")

                    XimalayaMoreAction.ADD_TO_PLAYLIST -> {
                        val added = currentAudio?.let { addToAudioList(store, listOf(it)) } ?: 0
                        if (added > 0) {
                            AppNotice.success(context, "已加入清单")
                        } else {
                            AppNotice.info(context, "音频已在清单中")
                        }
                    }

                    XimalayaMoreAction.DLNA -> openXimalayaSystemPanel(
                        context,
                        android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,
                        "未找到外设设置页面",
                    )

                    XimalayaMoreAction.RINGTONE -> openXimalayaSystemPanel(
                        context,
                        android.provider.Settings.ACTION_SOUND_SETTINGS,
                        "未找到声音设置页面",
                    )

                    XimalayaMoreAction.FEEDBACK,
                    XimalayaMoreAction.SURVEY_CONTENT,
                    XimalayaMoreAction.COMPLAIN,
                    XimalayaMoreAction.COPYRIGHT -> {
                        navController.navigate(Screen.Feedback.route)
                    }

                    XimalayaMoreAction.SKIP_HEAD_TAIL ->
                        run { showSkipHeadTailSheet = true }

                    XimalayaMoreAction.FREE_AD ->
                        AppNotice.info(context, "本地音频全程无广告")

                    XimalayaMoreAction.NOTE -> {
                        if (currentAudio == null) {
                            AppNotice.info(context, "当前没有可备注的音频")
                        } else {
                            noteEditingAudio = currentAudio
                        }
                    }

                    XimalayaMoreAction.SOUND_DETAILS ->
                        navController.navigate(
                            Screen.SoundDetails.createRoute(taskId, selectedTrackIndex),
                        )

                    XimalayaMoreAction.SOUND_EFFECT_QUALITY ->
                        run { showSoundEffectQualitySheet = true }
                }
            },
            onDismiss = { showMoreSheet = false },
        )
    }

    if (showSkipHeadTailSheet && sheetTask != null) {
        XimalayaSourceSkipHeadTailSheet(
            initialHeadSeconds = sheetTask.skipHeadSeconds,
            initialTailSeconds = sheetTask.skipTailSeconds,
            onSave = { headSeconds, tailSeconds ->
                task = store.updateTask(taskId) {
                    it.copy(
                        skipHeadSeconds = headSeconds,
                        skipTailSeconds = tailSeconds,
                    )
                }
                AudioPlaybackService.setSkipHeadTail(
                    context = context,
                    taskId = taskId,
                    headSeconds = headSeconds,
                    tailSeconds = tailSeconds,
                )
                AppNotice.success(context, "设置跳过片头片尾成功")
            },
            onDismiss = { showSkipHeadTailSheet = false },
        )
    }

    noteEditingAudio?.let { editorAudio ->
        val editorAudioKey = qqMusicQueueAudioId(editorAudio)
        val editorIndex = sheetTask?.audios?.indexOfFirst { audio ->
            qqMusicQueueAudioId(audio) == editorAudioKey
        } ?: -1
        val editorIsCurrent = hasLivePlayback && editorIndex >= 0 &&
            snapshot?.trackIndex == editorIndex
        val editorDurationMs = if (editorIsCurrent) {
            snapshot?.durationMs?.takeIf { it > 0L } ?: 0L
        } else {
            editorAudio.duration.coerceAtLeast(0L) * 1_000L
        }

        XimalayaSourceAudioNoteEditor(
            audio = editorAudio,
            initialNote = noteStore.get(editorAudio),
            durationMs = editorDurationMs,
            isPlaying = editorIsCurrent && state == PlayerUiState.PLAYING,
            onTogglePlayback = {
                if (editorIsCurrent) {
                    if (AudioPlaybackService.isCurrentlyPlaying()) {
                        AudioPlaybackService.pause(context)
                    } else {
                        AudioPlaybackService.resume(context)
                    }
                } else if (editorIndex < 0 ||
                    !scheduler.startManualPlayback(taskId, editorIndex)
                ) {
                    AppNotice.error(context, "音频暂时无法播放，请重新选择文件")
                }
            },
            onSave = { content ->
                val saved = noteStore.save(editorAudio, content)
                if (currentAudio?.let(::qqMusicQueueAudioId) == editorAudioKey) {
                    currentNote = saved
                }
                noteEditingAudio = null
                AppNotice.success(context, if (saved.isBlank()) "备注已清除" else "保存成功")
            },
            onDismiss = { noteEditingAudio = null },
        )
    }
}


private fun playerState(
    task: ScheduledTask?,
    snapshot: PlaybackSnapshot?,
): PlayerUiState = when {
    snapshot?.transportState == PlaybackTransportState.ERROR -> PlayerUiState.ERROR
    snapshot?.transportState == PlaybackTransportState.PREPARING -> PlayerUiState.PREPARING
    snapshot?.transportState == PlaybackTransportState.PLAYING -> PlayerUiState.PLAYING
    snapshot?.transportState == PlaybackTransportState.PAUSED -> PlayerUiState.PAUSED
    snapshot?.transportState == PlaybackTransportState.COMPLETED ||
        snapshot?.transportState == PlaybackTransportState.STOPPED -> PlayerUiState.COMPLETED
    task?.status == TaskStatus.EXECUTING -> PlayerUiState.PREPARING
    task?.scheduleArmed == true && task.status == TaskStatus.PENDING -> PlayerUiState.SCHEDULED
    else -> PlayerUiState.READY
}

private fun showDatePicker(
    context: Context,
    initial: Long,
    onSelected: (Long) -> Unit,
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initial }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onSelected(
                Calendar.getInstance().apply {
                    timeInMillis = initial
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }.timeInMillis
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
}

private fun Long.withHourMinute(hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = this@withHourMinute
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun Long.toTaskStartTime(): TaskStartTime =
    Calendar.getInstance().apply { timeInMillis = this@toTaskStartTime }.let {
        TaskStartTime(
            year = it.get(Calendar.YEAR),
            month = it.get(Calendar.MONTH) + 1,
            day = it.get(Calendar.DAY_OF_MONTH),
            hour = it.get(Calendar.HOUR_OF_DAY),
            minute = it.get(Calendar.MINUTE),
            second = 0,
        )
    }

private fun qqMusicQueueAudioId(audio: TaskAudio): String =
    audio.id.ifBlank {
        audio.fileKey?.takeIf(String::isNotBlank)
            ?: audio.localUri?.takeIf(String::isNotBlank)
            ?: audio.serverUrl?.takeIf(String::isNotBlank)
            ?: audio.dbKey.orEmpty()
    }

private fun addToAudioList(store: TaskStore, audios: List<TaskAudio>): Int {
    var added = 0
    store.updateDraft { draft ->
        val keys = draft.audios.mapTo(mutableSetOf(), ::qqMusicQueueAudioId)
        val additions = audios.filter { keys.add(qqMusicQueueAudioId(it)) }
        added = additions.size
        if (additions.isEmpty()) draft else draft.copy(audios = draft.audios + additions)
    }
    return added
}

private fun formatCountdown(milliseconds: Long): String {
    val totalMinutes = (milliseconds.coerceAtLeast(0L) + 59_999L) / 60_000L
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    return buildList {
        if (days > 0) add("${days}天")
        if (hours > 0) add("${hours}小时")
        if (minutes > 0 || isEmpty()) add("${minutes}分钟")
    }.joinToString("")
}

internal fun shareCurrentAudio(
    context: Context,
    audio: TaskAudio?,
    title: String,
) {
    val localUri = audio?.localUri
        ?.takeIf { it.startsWith("content://", ignoreCase = true) }
    val serverUrl = audio?.serverUrl
        ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    val intent = if (localUri != null) {
        Intent(Intent.ACTION_SEND).apply {
            type = audio.mimeType ?: "audio/*"
            putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(localUri))
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, serverUrl ?: title)
        }
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}

private fun openXimalayaSystemPanel(
    context: Context,
    action: String,
    unavailableMessage: String,
) {
    runCatching {
        context.startActivity(Intent(action))
    }.onFailure {
        AppNotice.error(context, unavailableMessage)
    }
}

internal fun formatPlayerFileSize(bytes: Long): String = when {
    bytes <= 0L -> "未知大小"
    bytes < 1_024L -> "${bytes} B"
    bytes < 1_048_576L -> String.format(Locale.CHINA, "%.1f KB", bytes / 1_024f)
    else -> String.format(Locale.CHINA, "%.1f MB", bytes / 1_048_576f)
}
