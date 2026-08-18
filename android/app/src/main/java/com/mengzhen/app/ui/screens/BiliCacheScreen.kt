package com.mengzhen.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.bilibili.BiliCacheAccessMode
import com.mengzhen.app.bilibili.BiliCacheImporter
import com.mengzhen.app.bilibili.BiliCacheItem
import com.mengzhen.app.bilibili.BiliCacheScanner
import com.mengzhen.app.bilibili.BiliImportProgress
import com.mengzhen.app.bilibili.BiliOfficialClient
import com.mengzhen.app.bilibili.BiliOnlineAudioClient
import com.mengzhen.app.bilibili.BiliRootBridge
import com.mengzhen.app.bilibili.BiliShizukuBridge
import com.mengzhen.app.data.api.AudioUploadQueue
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.scheduler.QuickPlaybackSessionFactory
import com.mengzhen.app.ui.components.ChatGptLoadingSpinner
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun BiliCacheScreen(
    navController: NavController,
    sharedVideo: String? = null,
    onSharedVideoConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    BiliOfflineSystemBarEffect(context)
    val scope = rememberCoroutineScope()
    val store = remember(context) { TaskStore.get(context) }
    val scanner = remember(context) { BiliCacheScanner(context) }
    val official = remember(context) { BiliOfficialClient(context) }
    val root = remember(context) { BiliRootBridge(context) }
    val shizuku = remember(context) { BiliShizukuBridge(context) }
    val importer = remember(context) { BiliCacheImporter(context, root, shizuku) }
    val online = remember(context) { BiliOnlineAudioClient(context) }
    val scanMutex = remember { Mutex() }
    val cacheChangeEvents = remember {
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }
    val pullRefreshState = rememberPullToRefreshState()
    val preferences = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun storedExtractedSourceIds() =
        store.getDraft().audios.mapNotNullTo(mutableSetOf(), TaskAudio::sourceId)

    var account by remember { mutableStateOf(official.officialAccountStatus()) }
    var cacheItems by remember { mutableStateOf<List<BiliCacheItem>>(emptyList()) }
    var artworkLocations by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editMode by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var extractedSourceIds by remember { mutableStateOf(storedExtractedSourceIds()) }
    var activeSource by remember { mutableStateOf<BiliRefreshSource?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    var importProgress by remember { mutableStateOf<BiliImportProgress?>(null) }
    var importing by remember { mutableStateOf(false) }
    var lifecycleStarted by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var resumeGeneration by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        var wasStopped = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    lifecycleStarted = true
                    if (wasStopped) {
                        wasStopped = false
                        resumeGeneration++
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    wasStopped = true
                    lifecycleStarted = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun replaceItems(items: List<BiliCacheItem>, sourceLabel: String) {
        extractedSourceIds = storedExtractedSourceIds()
        if (cacheItems != items) cacheItems = items
        selectedIds = selectedIds.intersect(
            items.asSequence()
                .filter(BiliCacheItem::completed)
                .map(BiliCacheItem::id)
                .filterNot(extractedSourceIds::contains)
                .toSet()
        )
        if (items.none(BiliCacheItem::completed)) editMode = false
        scanMessage = if (items.isEmpty()) {
            "${sourceLabel}中没有发现已完成的 B 站音轨"
        } else if (sourceLabel == "分享视频") {
            "已找到分享视频的公开音轨"
        } else {
            "已从${sourceLabel}发现 ${items.size} 个缓存视频"
        }
    }

    suspend fun refreshSource(
        source: BiliRefreshSource,
        showLoading: Boolean = false,
        showPull: Boolean = false,
    ): Boolean {
        var succeeded = false
        if (showLoading) {
            scanning = true
            scanMessage = null
        }
        if (showPull) refreshing = true
        try {
            scanMutex.withLock {
                val items = when (source.mode) {
                    BiliCacheAccessMode.DOCUMENT -> {
                        scanner.scanTree(Uri.parse(source.value))
                    }
                    BiliCacheAccessMode.ROOT -> root.scanDefaultCaches()
                    BiliCacheAccessMode.SHIZUKU -> shizuku.scanDefaultCaches()
                    BiliCacheAccessMode.NETWORK -> listOf(
                        online.resolveSharedVideo(source.value)
                    )
                }
                activeSource = source
                replaceItems(items, source.label)
                artworkLocations = importer.prepareArtwork(items)
                succeeded = true
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val message = error.message ?: "缓存刷新失败"
            if (showLoading || showPull || scanMessage == null) {
                scanMessage = message
            }
            if (source.mode == BiliCacheAccessMode.NETWORK) {
                Log.w(
                    BILI_CACHE_LOG_TAG,
                    "分享视频解析失败：${error.javaClass.simpleName}: $message",
                )
                AppNotice.error(context, message)
            }
        } finally {
            if (showLoading) scanning = false
            if (showPull) refreshing = false
        }
        return succeeded
    }

    fun scanDocumentTree(uri: Uri) {
        val source = BiliRefreshSource(BiliCacheAccessMode.DOCUMENT, uri.toString())
        preferences.edit()
            .putString(KEY_TREE_URI, source.value)
            .apply()
        scope.launch { refreshSource(source, showLoading = true) }
    }

    fun refreshActiveSource(showLoading: Boolean = false, showPull: Boolean = false) {
        val source = activeSource ?: return
        scope.launch {
            refreshSource(
                source = source,
                showLoading = showLoading,
                showPull = showPull,
            )
        }
    }

    fun availableLocalSource(): BiliRefreshSource? {
        activeSource?.takeUnless { it.mode == BiliCacheAccessMode.NETWORK }?.let { return it }
        if (root.hasPermission()) {
            return BiliRefreshSource(BiliCacheAccessMode.ROOT)
        }
        if (shizuku.hasPermission()) {
            return BiliRefreshSource(BiliCacheAccessMode.SHIZUKU)
        }
        return preferences.getString(KEY_TREE_URI, null)
            ?.takeIf(String::isNotBlank)
            ?.let { BiliRefreshSource(BiliCacheAccessMode.DOCUMENT, it) }
    }

    suspend fun connectAvailableLocalCache(requestPrivilegedPermission: Boolean): Boolean {
        availableLocalSource()?.let { source ->
            if (refreshSource(source, showLoading = true)) return true
        }
        if (
            requestPrivilegedPermission &&
            root.requestPermission()
        ) {
            val source = BiliRefreshSource(BiliCacheAccessMode.ROOT)
            if (refreshSource(source, showLoading = true)) return true
        }
        if (
            requestPrivilegedPermission &&
            shizuku.isManagerInstalled() &&
            shizuku.isRunning() &&
            shizuku.requestPermission()
        ) {
            val source = BiliRefreshSource(BiliCacheAccessMode.SHIZUKU)
            if (refreshSource(source, showLoading = true)) return true
        }
        activeSource = null
        return false
    }

    val directoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) {
            scanMessage = "未取得 B 站缓存读取权限"
            return@rememberLauncherForActivityResult
        }
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        scanDocumentTree(uri)
    }

    fun requestDefaultCachePermission(force: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scanMessage = "当前系统需通过 Root 或 Shizuku 完成一次缓存读取授权"
            return
        }
        val accountKey = account.uid ?: 0L
        if (
            !force &&
            preferences.getLong(KEY_CACHE_PERMISSION_PROMPT_ACCOUNT, Long.MIN_VALUE) == accountKey
        ) {
            return
        }
        preferences.edit()
            .putLong(KEY_CACHE_PERMISSION_PROMPT_ACCOUNT, accountKey)
            .apply()
        scanMessage = "请确认允许读取 B 站缓存，确认后将自动返回并显示列表"
        directoryLauncher.launch(DEFAULT_BILI_CACHE_TREE_URI)
    }

    LaunchedEffect(Unit) {
        if (!sharedVideo.isNullOrBlank()) return@LaunchedEffect
        account = official.officialAccountStatus()
        if (
            !connectAvailableLocalCache(requestPrivilegedPermission = account.loggedIn) &&
            account.loggedIn
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                requestDefaultCachePermission()
            } else {
                scanMessage = "点击授权读取缓存，完成后将自动显示缓存列表"
            }
        }
    }

    LaunchedEffect(sharedVideo) {
        val shared = sharedVideo?.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        try {
            refreshSource(
                source = BiliRefreshSource(BiliCacheAccessMode.NETWORK, shared),
                showLoading = true,
            )
            selectedIds = cacheItems
                .firstOrNull { it.completed && it.id !in extractedSourceIds }
                ?.let { setOf(it.id) }
                .orEmpty()
        } finally {
            onSharedVideoConsumed()
        }
    }

    LaunchedEffect(activeSource, lifecycleStarted) {
        val source = activeSource
        if (
            !lifecycleStarted ||
            source == null ||
            source.mode == BiliCacheAccessMode.NETWORK
        ) {
            return@LaunchedEffect
        }
        val changes = when (source.mode) {
            BiliCacheAccessMode.ROOT -> root.observeDefaultCacheChanges()
            BiliCacheAccessMode.SHIZUKU -> shizuku.observeDefaultCacheChanges()
            else -> null
        }
        changes
            ?.debounce(CACHE_EVENT_DEBOUNCE_MS)
            ?.collect { cacheChangeEvents.tryEmit(Unit) }
    }

    DisposableEffect(
        activeSource?.takeIf { it.mode == BiliCacheAccessMode.DOCUMENT }?.value,
        lifecycleStarted,
    ) {
        val source = activeSource?.takeIf { it.mode == BiliCacheAccessMode.DOCUMENT }
        if (!lifecycleStarted || source == null) {
            onDispose { }
        } else {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    cacheChangeEvents.tryEmit(Unit)
                }

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    cacheChangeEvents.tryEmit(Unit)
                }
            }
            val registered = runCatching {
                context.contentResolver.registerContentObserver(
                    Uri.parse(source.value),
                    true,
                    observer,
                )
            }.isSuccess
            onDispose {
                if (registered) {
                    runCatching { context.contentResolver.unregisterContentObserver(observer) }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        cacheChangeEvents
            .debounce(CACHE_EVENT_DEBOUNCE_MS)
            .collect {
                activeSource
                    ?.takeUnless { it.mode == BiliCacheAccessMode.NETWORK }
                    ?.let { refreshSource(it) }
            }
    }

    LaunchedEffect(resumeGeneration) {
        if (resumeGeneration > 0) {
            account = official.officialAccountStatus()
            val source = activeSource?.takeUnless { it.mode == BiliCacheAccessMode.NETWORK }
            if (source != null) {
                refreshSource(source)
            } else {
                if (root.hasPermission() || shizuku.isRunning()) {
                    connectAvailableLocalCache(requestPrivilegedPermission = true)
                }
            }
        }
    }

    fun importSelection() {
        val selected = cacheItems.filter {
            it.id in selectedIds && it.completed && it.id !in extractedSourceIds
        }
        if (selected.isEmpty() || importing) return
        scope.launch {
            importing = true
            importProgress = BiliImportProgress(1, selected.size, selected.first().displayTitle(), 0)
            val existingIds = store.getDraft().audios
                .mapNotNull(TaskAudio::sourceId)
                .toSet()
            val newItems = selected.filterNot { it.id in existingIds }
            if (newItems.isEmpty()) {
                importing = false
                importProgress = null
                AppNotice.info(context, "所选 B 站音频已经在列表中")
                return@launch
            }
            runCatching {
                importer.import(newItems) { progress ->
                    scope.launch { importProgress = progress }
                }
            }.onSuccess { imported ->
                val newSourceIds = newItems.mapTo(mutableSetOf(), BiliCacheItem::id)
                val persisted = store.updateDraft { current ->
                    val currentSourceIds = current.audios.mapNotNullTo(mutableSetOf(), TaskAudio::sourceId)
                    current.copy(
                        audios = current.audios + imported.filter {
                            currentSourceIds.add(it.sourceId.orEmpty())
                        }
                    )
                }
                val persistedImported = persisted.audios.filter {
                    it.sourceType == "bilibili" && it.sourceId in newSourceIds
                }
                if (store.getSession() != null) {
                    val queue = AudioUploadQueue.get(context)
                    persistedImported.forEach(queue::enqueue)
                }
                if (persistedImported.isNotEmpty()) {
                    val session = QuickPlaybackSessionFactory.createIdle(
                        id = QuickPlaybackSessionFactory.newId(),
                        draft = persisted.copy(audios = persistedImported),
                    )
                    QuickPlaybackSessionFactory.save(store, session)
                    AppNotice.success(context, "已加入音频列表")
                    navController.navigate(Screen.Templates.createRoute(session.id)) {
                        popUpTo(Screen.BiliCache.route) { inclusive = true }
                    }
                }
            }.onFailure {
                AppNotice.error(context, it.message ?: "B 站音频转换失败")
            }
            importing = false
            importProgress = null
        }
    }

    val hasCompletedItems = remember(cacheItems) { cacheItems.any(BiliCacheItem::completed) }
    val selectableIds = remember(cacheItems, extractedSourceIds) {
        cacheItems.asSequence()
            .filter(BiliCacheItem::completed)
            .map(BiliCacheItem::id)
            .filterNot(extractedSourceIds::contains)
            .toSet()
    }
    val visibleItems = remember(cacheItems, searchMode, searchQuery) {
        if (!searchMode) {
            cacheItems
        } else {
            val query = searchQuery.trim()
            if (query.isEmpty()) {
                emptyList()
            } else {
                cacheItems.filter { item ->
                    item.title.contains(query, ignoreCase = true) ||
                        item.subtitle.contains(query, ignoreCase = true) ||
                        item.owner.contains(query, ignoreCase = true)
                }
            }
        }
    }
    val downloading = remember(visibleItems) { visibleItems.filterNot(BiliCacheItem::completed) }
    val downloaded = remember(visibleItems) { visibleItems.filter(BiliCacheItem::completed) }
    val usedStorageBytes = remember(cacheItems) {
        cacheItems.sumOf { item -> item.audioSize.coerceAtLeast(0L) }
    }
    val availableStorageBytes = remember(cacheItems, activeSource) {
        runCatching {
            StatFs(Environment.getExternalStorageDirectory().path).availableBytes
        }.getOrDefault(0L)
    }

    BackHandler(enabled = searchMode || editMode) {
        if (editMode) {
            editMode = false
            selectedIds = emptySet()
        } else {
            searchMode = false
            searchQuery = ""
        }
    }

    Scaffold(
        containerColor = colorResource(R.color.theme_color_primary_tr_background),
        topBar = {
            if (searchMode) {
                BiliOfflineSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onCancel = {
                        searchMode = false
                        searchQuery = ""
                        editMode = false
                        selectedIds = emptySet()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.theme_color_primary_tr_background))
                        .statusBarsPadding(),
                )
            } else {
                BiliOfflineToolbar(
                    canEdit = hasCompletedItems,
                    editMode = editMode,
                    onSearch = {
                        editMode = false
                        selectedIds = emptySet()
                        searchMode = true
                    },
                    onSettings = {
                        navController.navigate(Screen.BiliAuthorization.route) {
                            launchSingleTop = true
                        }
                    },
                    onEdit = {
                        editMode = !editMode
                        if (!editMode) selectedIds = emptySet()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.theme_color_primary_tr_background))
                        .statusBarsPadding(),
                )
            }
        },
        bottomBar = {
            if (editMode && hasCompletedItems) {
                BiliOfflineBottomBar(
                    allSelected = selectableIds.isNotEmpty() && selectedIds == selectableIds,
                    selectionEnabled = selectableIds.isNotEmpty() && !scanning,
                    selectedCount = selectedIds.size,
                    importing = importing,
                    progress = importProgress,
                    onToggleAll = {
                        selectedIds = if (
                            selectableIds.isNotEmpty() && selectedIds == selectableIds
                        ) {
                            emptySet()
                        } else {
                            selectableIds
                        }
                    },
                    onExtract = ::importSelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
            } else if (!searchMode && activeSource != null) {
                BiliOfflineStorageBar(
                    usedBytes = usedStorageBytes,
                    availableBytes = availableStorageBytes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            state = pullRefreshState,
            onRefresh = {
                if (activeSource == null) {
                    scope.launch {
                        account = official.officialAccountStatus()
                        refreshing = true
                        try {
                            if (!connectAvailableLocalCache(requestPrivilegedPermission = account.loggedIn)) {
                                scanMessage = "请先完成一次缓存读取授权"
                                AppNotice.info(context, "还没有可刷新的缓存来源")
                            }
                        } finally {
                            refreshing = false
                        }
                    }
                } else {
                    refreshActiveSource(showPull = true)
                }
            },
            indicator = {
                PullToRefreshDefaults.IndicatorBox(
                    state = pullRefreshState,
                    isRefreshing = refreshing,
                    containerColor = Color.Transparent,
                    elevation = 0.dp,
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    ChatGptLoadingSpinner(
                        size = 28.dp,
                        color = Color(0xFFFB7299),
                        loadingDescription = "正在刷新缓存",
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.theme_color_primary_tr_background))
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.theme_color_primary_tr_background)),
            ) {
                if (activeSource == null && cacheItems.isEmpty()) {
                    item(key = "cache_loading") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    if (searchMode && searchQuery.isNotBlank()) {
                        item(key = "search_summary") {
                            BiliOfflineSearchSummary(
                                query = searchQuery.trim(),
                                count = visibleItems.size,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (downloading.isNotEmpty()) {
                        item(key = "downloading") {
                            BiliOfflineDownloadingItem(
                                items = downloading,
                                artworkLocation = artworkLocations[downloading.first().id]
                                    ?: downloading.first().coverLocation,
                                onClick = {
                                    runCatching {
                                        context.startActivity(official.offlineCacheIntent())
                                    }.onFailure {
                                        AppNotice.warning(context, "请先安装最新版 B 站客户端")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (downloaded.isNotEmpty()) {
                        items(downloaded, key = BiliCacheItem::id) { item ->
                            BiliOfflineDownloadedItem(
                                item = item,
                                artworkLocation = artworkLocations[item.id] ?: item.coverLocation,
                                selected = item.id in selectedIds,
                                extracted = item.id in extractedSourceIds,
                                editMode = editMode,
                                onClick = {
                                    if (editMode) {
                                        selectedIds = if (item.id in selectedIds) {
                                            selectedIds - item.id
                                        } else {
                                            selectedIds + item.id
                                        }
                                    } else {
                                        editMode = true
                                        selectedIds = setOf(item.id)
                                    }
                                },
                                onLongClick = {
                                    if (!editMode) editMode = true
                                    selectedIds = selectedIds + item.id
                                },
                                onToggle = {
                                    selectedIds = if (item.id in selectedIds) {
                                        selectedIds - item.id
                                    } else {
                                        selectedIds + item.id
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else if (
                        !scanning &&
                        downloading.isEmpty() &&
                        (!searchMode || searchQuery.isNotBlank())
                    ) {
                        item(key = "empty") {
                            BiliOfflineEmpty(
                                modifier = Modifier.fillMaxWidth(),
                                message = if (searchMode) {
                                    "没有搜到相关内容，请尝试别的搜索词"
                                } else {
                                    "这里还什么都没有呢～"
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BiliOfflineSystemBarEffect(context: Context) {
    val isDark = LocalIsDarkTheme.current
    val statusBarColor = colorResource(R.color.theme_color_primary_tr_background).toArgb()
    DisposableEffect(context, isDark, statusBarColor) {
        val window = context.findBiliHostActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            @Suppress("DEPRECATION")
            val previousColor = window.statusBarColor
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val previousLightStatusBars = controller.isAppearanceLightStatusBars
            @Suppress("DEPRECATION")
            run { window.statusBarColor = statusBarColor }
            controller.isAppearanceLightStatusBars = !isDark
            onDispose {
                @Suppress("DEPRECATION")
                run { window.statusBarColor = previousColor }
                controller.isAppearanceLightStatusBars = previousLightStatusBars
            }
        }
    }
}

private tailrec fun Context.findBiliHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findBiliHostActivity()
    else -> null
}

private const val BILI_CACHE_LOG_TAG = "BiliCacheScreen"

private const val PREFS_NAME = "bili_cache_preferences"
private const val KEY_TREE_URI = "tree_uri"
private const val KEY_CACHE_PERMISSION_PROMPT_ACCOUNT = "cache_permission_prompt_account"
private const val CACHE_EVENT_DEBOUNCE_MS = 450L
private val DEFAULT_BILI_CACHE_TREE_URI = Uri.parse(
    "content://com.android.externalstorage.documents/document/" +
        "primary%3AAndroid%2Fdata%2Ftv.danmaku.bili",
)

private data class BiliRefreshSource(
    val mode: BiliCacheAccessMode,
    val value: String = "",
) {
    val label: String
        get() = when (mode) {
            BiliCacheAccessMode.DOCUMENT -> "所选目录"
            BiliCacheAccessMode.ROOT -> "B站默认目录"
            BiliCacheAccessMode.SHIZUKU -> "B站默认目录"
            BiliCacheAccessMode.NETWORK -> "分享视频"
        }
}
