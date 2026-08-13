package com.mengzhen.app.ui.screens

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.bilibili.BiliAccountStatus
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
import com.mengzhen.app.ui.theme.BrandGlow
import com.mengzhen.app.ui.theme.MutedForeground
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
    topLevel: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { TaskStore.get(context) }
    val scanner = remember(context) { BiliCacheScanner(context) }
    val official = remember(context) { BiliOfficialClient(context) }
    val root = remember(context) { BiliRootBridge(context) }
    val shizuku = remember(context) { BiliShizukuBridge(context) }
    val importer = remember(context) { BiliCacheImporter(context, root, shizuku) }
    val online = remember { BiliOnlineAudioClient() }
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

    var account by remember { mutableStateOf(official.accountStatus()) }
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
    var scannedFiles by remember { mutableIntStateOf(0) }
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
            scannedFiles = 0
        }
        if (showPull) refreshing = true
        try {
            scanMutex.withLock {
                val items = when (source.mode) {
                    BiliCacheAccessMode.DOCUMENT -> {
                        scanner.scanTree(Uri.parse(source.value)) { count ->
                            if (showLoading || showPull) {
                                scope.launch { scannedFiles = count }
                            }
                        }
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
            if (showLoading || showPull || scanMessage == null) {
                scanMessage = error.message ?: "缓存刷新失败"
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

    val authorizeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val authorized = official.acceptAuthorizationResult(result.resultCode, result.data)
        account = official.accountStatus()
        if (authorized || account.loggedIn) {
            AppNotice.success(context, "B站账号连接成功")
            scope.launch {
                if (!connectAvailableLocalCache(requestPrivilegedPermission = true)) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        requestDefaultCachePermission(force = true)
                    } else {
                        scanMessage = if (shizuku.isManagerInstalled()) {
                            "请完成一次本地缓存读取授权，返回后将自动显示缓存列表"
                        } else {
                            "当前系统需通过 Root 或 Shizuku 完成一次缓存读取授权"
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!sharedVideo.isNullOrBlank()) return@LaunchedEffect
        account = official.accountStatus()
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
            val source = activeSource?.takeUnless { it.mode == BiliCacheAccessMode.NETWORK }
            if (source != null) {
                refreshSource(source)
            } else {
                account = official.accountStatus()
                if (root.hasPermission() || shizuku.isRunning()) {
                    connectAvailableLocalCache(requestPrivilegedPermission = true)
                }
            }
        }
    }

    fun scanDefaultCache() {
        scope.launch {
            if (connectAvailableLocalCache(requestPrivilegedPermission = true)) return@launch
            if (root.hasPermission() || shizuku.hasPermission()) return@launch
            if (!shizuku.isManagerInstalled()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    requestDefaultCachePermission(force = true)
                } else {
                    scanMessage = "无 Root 设备需通过 Shizuku 完成一次缓存读取授权"
                    AppNotice.info(context, "请先安装并启动 Shizuku，返回后会自动继续")
                }
                return@launch
            }
            if (!shizuku.isRunning()) {
                shizuku.managerLaunchIntent()?.let(context::startActivity)
                scanMessage = "请在 Shizuku 中启动服务，返回后会自动授权并显示缓存列表"
                AppNotice.info(context, "启动 Shizuku 后直接返回即可")
                return@launch
            }
            scanMessage = "本地缓存读取授权未完成"
            AppNotice.warning(context, "未获得缓存读取授权")
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
                        .statusBarsPadding(),
                )
            } else {
                BiliOfflineToolbar(
                    topLevel = topLevel,
                    canEdit = hasCompletedItems,
                    editMode = editMode,
                    onBack = navController::popBackStack,
                    onSearch = {
                        editMode = false
                        selectedIds = emptySet()
                        searchMode = true
                    },
                    onSettings = ::scanDefaultCache,
                    onEdit = {
                        editMode = !editMode
                        if (!editMode) selectedIds = emptySet()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
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
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            state = pullRefreshState,
            onRefresh = {
                if (activeSource == null) {
                    scope.launch {
                        account = official.accountStatus()
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
                .background(colorResource(R.color.Ga0))
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.Ga0)),
            ) {
                if (activeSource == null && cacheItems.isEmpty()) {
                    item {
                        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                        BiliAccountCard(
                        status = account,
                        onAuthorize = {
                            if (account.installed) {
                                runCatching {
                                    authorizeLauncher.launch(official.authorizationIntent())
                                }.onFailure {
                                    AppNotice.error(context, "无法打开 B 站授权页面")
                                }
                            } else {
                                context.startActivity(official.officialDownloadIntent())
                            }
                        },
                        onOpenOffline = {
                            runCatching { context.startActivity(official.offlineCacheIntent()) }
                                .onFailure {
                                    AppNotice.warning(context, "请先安装最新版 B 站客户端")
                                }
                        },
                        )
                        }
                    }
                    item {
                        Box(Modifier.padding(16.dp)) {
                        CacheAccessCard(
                        scanning = scanning,
                        scannedFiles = scannedFiles,
                        scanMessage = scanMessage,
                        automaticReady = root.hasPermission() || shizuku.hasPermission(),
                        onAutomatic = ::scanDefaultCache,
                        )
                        }
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
                        item(key = "downloaded_section") {
                            BiliOfflineSectionTitle(Modifier.fillMaxWidth())
                        }
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
private fun BiliAccountCard(
    status: BiliAccountStatus,
    onAuthorize: () -> Unit,
    onOpenOffline: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFB7299).copy(alpha = 0.12f),
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFFFB7299),
                        modifier = Modifier.padding(10.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            !status.installed -> "未安装 B 站客户端"
                            status.loggedIn -> "B站账号已连接"
                            else -> "尚未登录 B 站"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        status.uid?.let { "UID $it · 由官方客户端授权" }
                            ?: "登录和授权均在官方 B 站中完成",
                        color = MutedForeground,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAuthorize,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB7299)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (status.installed) "连接账号" else "获取客户端")
                }
                TextButton(onClick = onOpenOffline, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("离线缓存")
                }
            }
        }
    }
}

@Composable
private fun CacheAccessCard(
    scanning: Boolean,
    scannedFiles: Int,
    scanMessage: String?,
    automaticReady: Boolean,
    onAutomatic: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            ListItem(
                headlineContent = { Text("授权读取缓存") },
                supportingContent = {
                    Text(
                        if (automaticReady) {
                            "已完成一次授权，后续自动读取并实时更新"
                        } else {
                            "只需确认一次，完成后自动返回并显示缓存列表"
                        }
                    )
                },
                leadingContent = { Icon(Icons.Default.Memory, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(enabled = !scanning, onClick = onAutomatic),
            )
            if (scanning || scanMessage != null) {
                Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp)) {
                    if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        scanMessage ?: "正在扫描，已检查 $scannedFiles 个文件",
                        color = MutedForeground,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

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
