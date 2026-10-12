package com.mengzhen.app.ui.components.main

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.target
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.mengzhen.app.R
import com.mengzhen.app.audio.PlaybackStateStore
import com.mengzhen.app.audio.PlaybackTransportState
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.scheduler.QuickPlaybackSessionFactory
import com.mengzhen.app.ui.navigation.Screen
import com.mengzhen.app.ui.navigation.MEDIA_IMPORT_REFRESH_KEY
import com.mengzhen.app.ui.navigation.MEDIA_IMPORT_TAB_PRESSED_KEY
import com.mengzhen.app.ui.navigation.MEDIA_IMPORT_TAB_REFRESHING_KEY
import com.mengzhen.app.ui.navigation.TAB_REFRESH_STATE_DURATION_MS
import com.mengzhen.app.ui.navigation.TOP_LEVEL_REFRESH_KEY
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import com.mengzhen.app.ui.theme.LocalThemeMode
import com.mengzhen.app.ui.theme.ThemeMode
import kotlin.math.abs
import com.mengzhen.app.ui.theme.ThemeModeStore
// 页面模式弹窗用**源码版**（thememode/ih），不用 qqmusic-runtime 里那份编译版
// （playersong.ai）。两者是同一个 PlayModePopupWindow，但编译版读的是 QQ 自己的资源 id，
// 文案和图标改不了 —— 那就是「右上角弹窗文案图标都没变」的根因。
// 源码版 com.mengzhen.app.R，文案与图标都能换成梦枕自己的。
import com.tencent.qqmusic.business.playernew.view.playersong.thememode.ih as QqThemeModePopupWindow
import com.ximalaya.ting.android.host.view.bar.RoundProgressBar
import kotlinx.coroutines.launch

@Composable
fun XimalayaSourceHomeTopBar(
    navController: NavController,
    onSearch: () -> Unit,
    onVoiceSearch: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { TaskStore.get(context) }
    val user by store.sessionUser.collectAsState()
    val appliedMode = LocalThemeMode.current
    var selectedMode by remember { mutableStateOf(appliedMode) }
    val darkTheme = LocalIsDarkTheme.current
    val scope = rememberCoroutineScope()
    // 主题在别处（设置页等）被改了，顶栏图标要跟着变 —— selectedMode 只在本弹窗里改，
    // 不跟随 appliedMode 的话会出现「主题已经是深色、图标还显示太阳」。
    LaunchedEffect(appliedMode) { selectedMode = appliedMode }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.xm_main_background))
            .statusBarsPadding()
            .height(42.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BiliSourceHomeAvatar(
            user = user,
            onClick = {
                val route = if (user == null) Screen.Login.route else Screen.Profile.route
                navController.navigate(route) { launchSingleTop = true }
            },
        )
        Spacer(Modifier.width(8.dp))

        AndroidView(
            factory = { viewContext ->
                LayoutInflater.from(viewContext).inflate(
                    R.layout.ximalaya_main_search_block,
                    null,
                    false,
                )
            },
            modifier = Modifier.weight(1f).height(34.dp),
            update = { searchBar ->
                searchBar.setOnClickListener { onSearch() }
                searchBar.findViewById<View>(R.id.xm_main_voice_search)
                    .setOnClickListener { onVoiceSearch() }
            },
        )

        Spacer(Modifier.width(4.dp))

        AndroidView(
            factory = { viewContext ->
                ImageView(viewContext).apply {
                    scaleType = ImageView.ScaleType.CENTER
                    contentDescription = "页面模式"
                }
            },
            modifier = Modifier.size(40.dp),
            update = { trigger ->
                trigger.setImageResource(selectedMode.themeModeIcon)
                trigger.setColorFilter(
                    if (darkTheme) Color.WHITE else Color.BLACK,
                    PorterDuff.Mode.SRC_IN,
                )
                trigger.setOnClickListener {
                    val showing = trigger.tag as? QqThemeModePopupWindow
                    if (showing?.isShowing == true) return@setOnClickListener
                    val popup = QqThemeModePopupWindow(
                        context,
                        object : QqThemeModePopupWindow.b {
                            // 真本这个接口有五个回调，梦枕只有三种页面模式：
                            // a()/d() 属于「单曲重复次数 / 设置次数」两行，b()/c() 属于底部点击长按区，
                            // 都已连同它们的行与布局一起删掉，接口契约因此只剩 e(int)：
                            // 105 浅色 / 103 深色 / 101 自动。
                            override fun e(sourceMode: Int) {
                                val mode = sourceMode.toThemeMode()
                                if (mode == selectedMode) return
                                selectedMode = mode
                                scope.launch {
                                    ThemeModeStore.setMode(context, mode)
                                    // 立刻重建 Activity，不再弹「需要重启应用」的提示 ——
                                    // 用户点完就该看到结果，而不是被要求自己去重启。
                                    //
                                    // 必须重建的原因：顶栏那批喜马拉雅皮肤色是 values-night
                                    // 资源，跟着 Configuration.uiMode 走，只靠 Compose 重组
                                    // 换不掉（会出现「Compose 变深色、顶栏还是浅色」的夹生状态）。
                                    // attachBaseContext 重建时会按新 mode 重设 uiMode，一次到位。
                                    context.findActivity()?.recreate()
                                }
                            }
                        }
                    )
                    trigger.tag = popup
                    popup.t(darkTheme)
                    popup.r(selectedMode.qqSourceValue)
                    try {
                        popup.u(trigger, false)
                    } catch (error: NullPointerException) {
                        // QQ's source popup is already visible when its optional exposure tracker
                        // asks for a QQ account-process Context. Dream Pillow does not host that
                        // process, so only ignore this post-display analytics failure.
                        if (!popup.isShowing) throw error
                    }
                    val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                        as? AccessibilityManager
                    if (accessibility?.isTouchExplorationEnabled != true) {
                        trigger.postDelayed({ if (popup.isShowing) popup.dismiss() }, 1_500L)
                    }
                }
            },
        )
    }

}

private val ThemeMode.qqSourceValue: Int
    get() = when (this) {
        ThemeMode.LIGHT -> 105
        ThemeMode.DARK -> 103
        ThemeMode.SYSTEM -> 101
    }

/**
 * 顶栏触发图标 —— 用 lucide 三态图标（与 Web 端 theme-toggle.tsx 同源）。
 *
 * 这里原先是 `popup_window_play_mode_shuffle / list_repeat / onshot_repeat`，
 * 三个都是真本的**播歌顺序**图标（随机 / 顺序 / 单曲循环），跟主题切换没有半点关系，
 * 用户看到的「右上角图标没换」就是它。
 */
private val ThemeMode.themeModeIcon: Int
    get() = when (this) {
        ThemeMode.LIGHT -> R.drawable.theme_mode_light
        ThemeMode.DARK -> R.drawable.theme_mode_dark
        ThemeMode.SYSTEM -> R.drawable.theme_mode_system
    }

private fun Int.toThemeMode(): ThemeMode = when (this) {
    105 -> ThemeMode.LIGHT
    103 -> ThemeMode.DARK
    else -> ThemeMode.SYSTEM
}

@Composable
fun XimalayaSourceBottomNavigation(
    navController: NavController,
    currentRoute: String?,
) {
    val context = LocalContext.current
    val store = remember(context) { TaskStore.get(context) }
    val user by store.sessionUser.collectAsState()
    val playback by remember(context) { PlaybackStateStore.get(context).snapshot }
        .collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val tabRefreshStateFlow = remember(currentBackStackEntry) {
        currentBackStackEntry?.savedStateHandle?.getStateFlow(
            MEDIA_IMPORT_TAB_REFRESHING_KEY,
            false,
        )
    }
    val tabRefreshState = tabRefreshStateFlow?.collectAsState()
    val cacheTabRefreshing = tabRefreshState?.value ?: false
    val darkTheme = LocalIsDarkTheme.current

    fun openPlayer() {
        val activeId = playback.taskId?.takeIf { store.getTaskById(it) != null }
        if (activeId != null) {
            navController.navigate(Screen.Templates.createRoute(activeId)) {
                launchSingleTop = true
            }
            return
        }
        val draft = store.getDraft()
        if (draft.audios.isEmpty()) {
            navigateTopLevel(navController, Screen.Settings.route)
            return
        }
        val session = QuickPlaybackSessionFactory.createIdle(
            id = QuickPlaybackSessionFactory.newId(),
            draft = draft,
        )
        QuickPlaybackSessionFactory.save(store, session)
        navController.navigate(Screen.Templates.createRoute(session.id))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Color(
                    ContextCompat.getColor(context, R.color.xm_main_bottom_fill),
                ),
            )
            .navigationBarsPadding(),
    ) {
        AndroidView(
            factory = { viewContext ->
                LayoutInflater.from(viewContext).inflate(
                    R.layout.ximalaya_main_bottom_navigation,
                    null,
                    false,
                ).also { root ->
                    installTabLotties(root, darkTheme)
                    val group = root.findViewById<RadioGroup>(R.id.xm_main_tabs)
                    val destinationFor: (Int) -> String = { checkedId ->
                        when (checkedId) {
                            R.id.xm_main_tab_cache -> Screen.MediaImport.route
                            R.id.xm_main_tab_audio -> Screen.Tasks.route
                            R.id.xm_main_tab_mine -> Screen.Profile.route
                            else -> Screen.Settings.route
                        }
                    }
                    fun setCacheTabPressActive(active: Boolean) {
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            MEDIA_IMPORT_TAB_PRESSED_KEY,
                            active,
                        )
                    }
                    fun activateTab(id: Int, refreshSelectedTab: Boolean = false) {
                        val route = destinationFor(id)
                        if (refreshSelectedTab || navController.currentDestination?.route == route) {
                            if (id == R.id.xm_main_tab_cache) {
                                updateTabRefreshState(root, id, refreshing = true)
                            } else {
                                triggerTabRefreshFeedback(root, id)
                            }
                        }
                        navigateTopLevel(navController, route)
                    }
                    group.setOnCheckedChangeListener { _, checkedId ->
                        updateTabSelection(root, checkedId)
                    }
                    listOf(
                        R.id.xm_main_tab_home,
                        R.id.xm_main_tab_cache,
                        R.id.xm_main_tab_audio,
                        R.id.xm_main_tab_mine,
                    ).forEach { id ->
                        root.findViewById<RadioButton>(id).apply {
                            setOnClickListener { activateTab(id) }
                            var consumingRefreshPress = false
                            setOnTouchListener { buttonView, event ->
                                when (event.actionMasked) {
                                    android.view.MotionEvent.ACTION_DOWN -> {
                                        if (!(buttonView as RadioButton).isChecked) {
                                            consumingRefreshPress = false
                                            false
                                        } else {
                                            consumingRefreshPress = true
                                            updateTabPressedRefreshState(root, id, pressed = true)
                                            if (id == R.id.xm_main_tab_cache) {
                                                setCacheTabPressActive(true)
                                            }
                                            activateTab(id, refreshSelectedTab = true)
                                            true
                                        }
                                    }
                                    android.view.MotionEvent.ACTION_UP,
                                    android.view.MotionEvent.ACTION_CANCEL -> {
                                        if (!consumingRefreshPress) return@setOnTouchListener false
                                        consumingRefreshPress = false
                                        updateTabPressedRefreshState(root, id, pressed = false)
                                        if (id == R.id.xm_main_tab_cache) {
                                            setCacheTabPressActive(false)
                                        }
                                        true
                                    }
                                    else -> consumingRefreshPress
                                }
                            }
                        }
                    }
                    val player = root.findViewById<View>(R.id.xm_main_player)
                    val touchSlop = ViewConfiguration.get(viewContext).scaledTouchSlop
                    var pressedTabId: Int? = null
                    var downX = 0f
                    var downY = 0f
                    var tabGestureMoved = false
                    var pressedCurrentTab = false

                    fun tabForTouch(x: Float): Int? {
                        val slot = (x / (root.width / 5f)).toInt().coerceIn(0, 4)
                        return when (slot) {
                            0 -> R.id.xm_main_tab_home
                            1 -> R.id.xm_main_tab_cache
                            3 -> R.id.xm_main_tab_audio
                            4 -> R.id.xm_main_tab_mine
                            else -> null
                        }
                    }

                    root.setOnTouchListener { _, event ->
                        when (event.actionMasked) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                val touchesPlayer = event.x >= player.left &&
                                    event.x < player.right &&
                                    event.y >= player.top &&
                                    event.y < player.bottom
                                if (touchesPlayer) {
                                    // Let the full player artwork use its own click listener.
                                    return@setOnTouchListener false
                                }
                                val id = tabForTouch(event.x) ?: return@setOnTouchListener false
                                pressedTabId = id
                                downX = event.x
                                downY = event.y
                                tabGestureMoved = false
                                pressedCurrentTab = root.findViewById<RadioButton>(id).isChecked
                                if (pressedCurrentTab) {
                                    updateTabPressedRefreshState(root, id, pressed = true)
                                    if (id == R.id.xm_main_tab_cache) {
                                        setCacheTabPressActive(true)
                                    }
                                    activateTab(id, refreshSelectedTab = true)
                                }
                                true
                            }
                            android.view.MotionEvent.ACTION_MOVE -> {
                                if (pressedTabId == null) return@setOnTouchListener false
                                if (
                                    abs(event.x - downX) > touchSlop ||
                                    abs(event.y - downY) > touchSlop
                                ) {
                                    tabGestureMoved = true
                                    if (pressedCurrentTab) {
                                        pressedTabId?.let { updateTabPressedRefreshState(root, it, pressed = false) }
                                        if (pressedTabId == R.id.xm_main_tab_cache) {
                                            setCacheTabPressActive(false)
                                        }
                                        pressedCurrentTab = false
                                    }
                                }
                                true
                            }
                            android.view.MotionEvent.ACTION_UP -> {
                                val id = pressedTabId ?: return@setOnTouchListener false
                                if (pressedCurrentTab) {
                                    updateTabPressedRefreshState(root, id, pressed = false)
                                    if (id == R.id.xm_main_tab_cache) {
                                        setCacheTabPressActive(false)
                                    }
                                } else if (!tabGestureMoved) {
                                    root.findViewById<RadioButton>(id).performClick()
                                }
                                pressedTabId = null
                                tabGestureMoved = false
                                pressedCurrentTab = false
                                true
                            }
                            android.view.MotionEvent.ACTION_CANCEL,
                            android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                                val hadPressedTab = pressedTabId != null
                                if (pressedCurrentTab) {
                                    pressedTabId?.let { updateTabPressedRefreshState(root, it, pressed = false) }
                                    if (pressedTabId == R.id.xm_main_tab_cache) {
                                        setCacheTabPressActive(false)
                                    }
                                }
                                pressedTabId = null
                                tabGestureMoved = false
                                pressedCurrentTab = false
                                hadPressedTab
                            }
                            else -> pressedTabId != null
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(63.dp),
            update = { root ->
                val group = root.findViewById<RadioGroup>(R.id.xm_main_tabs)
                val selectedId = when (currentRoute) {
                    Screen.MediaImport.route -> R.id.xm_main_tab_cache
                    Screen.Tasks.route -> R.id.xm_main_tab_audio
                    Screen.Profile.route -> R.id.xm_main_tab_mine
                    else -> R.id.xm_main_tab_home
                }
                // 未登录时，源页面右下角入口直接标识为“未登录”；
                // 登录状态恢复后再回到“我的”，避免把空个人页误认为已登录。
                val mineTab = root.findViewById<RadioButton>(R.id.xm_main_tab_mine)
                val mineLabel = if (user == null) "未登录" else "我的"
                mineTab.text = mineLabel
                (root.getTag(R.id.xm_main_tabs) as? TabLottieState)
                    ?.tabLabels
                    ?.set(R.id.xm_main_tab_mine, mineLabel)
                if (group.checkedRadioButtonId != selectedId) {
                    group.check(selectedId)
                } else {
                    updateTabSelection(root, selectedId)
                }
                updateTabRefreshState(
                    root = root,
                    tabId = R.id.xm_main_tab_cache,
                    refreshing = currentRoute == Screen.MediaImport.route && cacheTabRefreshing,
                )

                val player = root.findViewById<View>(R.id.xm_main_player)
                player.setOnClickListener { openPlayer() }

                val progress = root.findViewById<RoundProgressBar>(R.id.xm_main_player_progress)
                progress.setMax(100)
                progress.setRoundColor(Color.TRANSPARENT)
                progress.setCricleProgressColor(
                    ContextCompat.getColor(context, R.color.xm_main_tab_checked),
                )
                progress.setRoundWidth(context.resources.displayMetrics.density * 2f)
                progress.setTextIsDisplayable(false)
                progress.setStrokeCap(Paint.Cap.ROUND)
                val percent = if (playback.durationMs > 0) {
                    (playback.positionMs * 100L / playback.durationMs).toInt().coerceIn(0, 100)
                } else 0
                progress.setProgress(percent)

                val state = root.findViewById<ImageView>(R.id.xm_main_player_state)
                state.alpha = if (playback.transportState == PlaybackTransportState.PLAYING) 0f else 1f

                val cover = root.findViewById<ImageView>(R.id.xm_main_player_cover)
                val audio = activeAudio(store, playback.taskId, playback.trackIndex)
                val coverModel = audio?.artworkUri
                val coverKey = coverModel.orEmpty()
                if (cover.tag != coverKey) {
                    cover.tag = coverKey
                    if (coverModel.isNullOrBlank()) {
                        cover.setImageResource(R.drawable.xm_main_v9514_default_cover)
                    } else {
                        context.imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .data(coverModel)
                                .target(cover)
                                .build(),
                        )
                    }
                }

                updatePlayerRotation(
                    player = player,
                    cover = cover,
                    playing = playback.transportState == PlaybackTransportState.PLAYING,
                    visible = currentRoute?.startsWith("templates") != true,
                )
            },
        )
    }
}

private fun updatePlayerRotation(
    player: View,
    cover: View,
    playing: Boolean,
    visible: Boolean,
) {
    val running = player.getTag(R.id.xm_main_player) as? ObjectAnimator
    if (playing && visible) {
        val animator = running ?: ObjectAnimator.ofFloat(cover, View.ROTATION, 0f, 360f).apply {
            duration = 7_000L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }.also { player.setTag(R.id.xm_main_player, it) }
        if (!animator.isStarted) animator.start()
    } else {
        running?.cancel()
        player.setTag(R.id.xm_main_player, null)
        cover.rotation = 0f
    }
}

private class TabLottieState {
    var selectedTabId: Int? = null
    val tabDrawables = mutableMapOf<Int, LottieDrawable>()
    val tabLabels = mutableMapOf<Int, CharSequence>()
    var refreshDrawable: LottieDrawable? = null
    var externalRefreshingTabId: Int? = null
    var feedbackRefreshingTabId: Int? = null
    var pressedRefreshingTabId: Int? = null
    var refreshingTabId: Int? = null
    var refreshEndRunnable: Runnable? = null
}

internal fun installTabLotties(root: View, darkTheme: Boolean) {
    val folder = if (darkTheme) "lottie-night-v9514" else "lottie-v9514"
    val state = TabLottieState()
    root.setTag(R.id.xm_main_tabs, state)
    val tabs = listOf(
        R.id.xm_main_tab_home to "bottom_tab_brush_song_tab_btn.json",
        R.id.xm_main_tab_cache to "bottom_tab_quick_listen_page_btn.json",
        R.id.xm_main_tab_audio to "bottom_tab_finding_btn.json",
        R.id.xm_main_tab_mine to "bottom_tab_mine_v9_btn.json",
    )
    tabs.forEach { (id, file) ->
        val button = root.findViewById<RadioButton>(id)
        state.tabLabels[id] = button.text
        LottieCompositionFactory.fromAsset(root.context, "$folder/$file")
            .addListener { composition ->
                val drawable = createTabDrawable(root, button, composition).apply {
                    progress = if (button.isChecked) 1f else 0f
                }
                state.tabDrawables[id] = drawable
                if (state.refreshingTabId != id || state.refreshDrawable == null) {
                    showTabDrawable(button, drawable)
                }
            }
    }
    val cacheButton = root.findViewById<RadioButton>(R.id.xm_main_tab_cache)
    LottieCompositionFactory.fromAsset(
        root.context,
        "$folder/bottom_tab_home_page_refresh_btn.json",
    ).addListener { composition ->
        state.refreshDrawable = createTabDrawable(root, cacheButton, composition).apply {
            repeatCount = 0
        }
        state.refreshingTabId?.let { tabId ->
            showTabRefreshDrawable(root, state, tabId)
        }
    }
}

internal fun updateTabSelection(root: View, checkedId: Int) {
    val state = root.getTag(R.id.xm_main_tabs) as? TabLottieState
        ?: TabLottieState().also { root.setTag(R.id.xm_main_tabs, it) }
    val selectionChanged = state.selectedTabId != checkedId
    if (selectionChanged) {
        state.refreshEndRunnable?.let(root::removeCallbacks)
        state.refreshEndRunnable = null
        state.feedbackRefreshingTabId = null
        state.pressedRefreshingTabId = state.pressedRefreshingTabId?.takeIf { it == checkedId }
        state.externalRefreshingTabId = state.externalRefreshingTabId?.takeIf { it == checkedId }
    }
    state.selectedTabId = checkedId

    listOf(
        R.id.xm_main_tab_home,
        R.id.xm_main_tab_cache,
        R.id.xm_main_tab_audio,
        R.id.xm_main_tab_mine,
    ).forEach { id ->
        val button = root.findViewById<RadioButton>(id)
        val selected = id == checkedId
        button.typeface = if (selected) {
            Typeface.create("sans-serif-light", Typeface.BOLD)
        } else {
            Typeface.DEFAULT
        }
        if (selectionChanged) {
            val drawable = state.tabDrawables[id]
                ?: (button.compoundDrawables[1] as? LottieDrawable)
            if (drawable != null) {
                (button.compoundDrawables[1] as? LottieDrawable)
                    ?.takeIf { it !== drawable }
                    ?.cancelAnimation()
                if (button.compoundDrawables[1] !== drawable) {
                    showTabDrawable(button, drawable)
                }
                drawable.cancelAnimation()
                if (selected) {
                    drawable.progress = 0f
                    drawable.playAnimation()
                } else {
                    drawable.progress = 0f
                }
            }
        }
    }
    syncTabRefreshDrawable(root, state)
}

internal fun updateTabRefreshState(root: View, tabId: Int, refreshing: Boolean) {
    val state = root.getTag(R.id.xm_main_tabs) as? TabLottieState
        ?: TabLottieState().also { root.setTag(R.id.xm_main_tabs, it) }
    if (refreshing) {
        state.externalRefreshingTabId = tabId
    } else if (state.externalRefreshingTabId == tabId) {
        state.externalRefreshingTabId = null
    }
    syncTabRefreshDrawable(root, state)
}

private fun updateTabPressedRefreshState(root: View, tabId: Int, pressed: Boolean) {
    val state = root.getTag(R.id.xm_main_tabs) as? TabLottieState ?: return
    if (pressed) {
        state.pressedRefreshingTabId = tabId
    } else if (state.pressedRefreshingTabId == tabId) {
        state.pressedRefreshingTabId = null
    }
    syncTabRefreshDrawable(root, state)
}

private fun syncTabRefreshDrawable(root: View, state: TabLottieState) {
    val activeTabId = state.pressedRefreshingTabId
        ?: state.externalRefreshingTabId
        ?: state.feedbackRefreshingTabId
    if (state.refreshingTabId == activeTabId) {
        if (activeTabId != null) {
            val refreshDrawable = state.refreshDrawable
            val repeatCount = if (state.pressedRefreshingTabId == activeTabId) {
                LottieDrawable.INFINITE
            } else {
                0
            }
            if (refreshDrawable != null && refreshDrawable.repeatCount != repeatCount) {
                refreshDrawable.cancelAnimation()
                refreshDrawable.repeatCount = repeatCount
                refreshDrawable.progress = 0f
                refreshDrawable.playAnimation()
            }
        }
        return
    }

    state.refreshingTabId?.let { previousTabId ->
        restoreTabDrawable(root, state, previousTabId)
    }
    state.refreshingTabId = activeTabId
    activeTabId?.let { showTabRefreshDrawable(root, state, it) }
}

private fun showTabRefreshDrawable(root: View, state: TabLottieState, tabId: Int) {
    val button = root.findViewById<RadioButton>(tabId)
    button.text = "刷新"
    val refreshDrawable = state.refreshDrawable ?: return
    state.tabDrawables[tabId]?.cancelAnimation()
    showTabDrawable(button, refreshDrawable)
    refreshDrawable.repeatCount = if (state.pressedRefreshingTabId == tabId) {
        LottieDrawable.INFINITE
    } else {
        0
    }
    refreshDrawable.progress = 0f
    refreshDrawable.playAnimation()
}

private fun restoreTabDrawable(root: View, state: TabLottieState, tabId: Int) {
    val button = root.findViewById<RadioButton>(tabId)
    state.refreshDrawable?.let { drawable ->
        drawable.cancelAnimation()
        drawable.progress = 0f
    }
    button.text = state.tabLabels[tabId] ?: button.text
    val tabDrawable = state.tabDrawables[tabId]
    if (tabDrawable != null) {
        showTabDrawable(button, tabDrawable)
        tabDrawable.progress = if (button.isChecked) 1f else 0f
    } else {
        button.setCompoundDrawables(null, null, null, null)
    }
}

private fun triggerTabRefreshFeedback(root: View, tabId: Int) {
    val state = root.getTag(R.id.xm_main_tabs) as? TabLottieState ?: return
    state.refreshEndRunnable?.let(root::removeCallbacks)
    state.feedbackRefreshingTabId = tabId
    syncTabRefreshDrawable(root, state)
    val endRefresh = Runnable {
        if (state.feedbackRefreshingTabId == tabId) {
            state.feedbackRefreshingTabId = null
            syncTabRefreshDrawable(root, state)
        }
        state.refreshEndRunnable = null
    }
    state.refreshEndRunnable = endRefresh
    root.postDelayed(endRefresh, TAB_REFRESH_STATE_DURATION_MS)
}

private fun createTabDrawable(
    root: View,
    button: RadioButton,
    composition: LottieComposition,
) = LottieDrawable().apply {
    setComposition(composition)
    // Ximalaya's 90 px source animations are displayed at one third scale.
    setBounds(0, 0, root.dp(30), root.dp(30))
    callback = button
}

private fun showTabDrawable(button: RadioButton, drawable: LottieDrawable) {
    button.setCompoundDrawables(null, drawable, null, null)
    button.invalidate()
}

private fun navigateTopLevel(navController: NavController, route: String) {
    if (navController.currentDestination?.route == route) {
        val refreshKey = if (route == Screen.MediaImport.route) {
            MEDIA_IMPORT_REFRESH_KEY
        } else {
            TOP_LEVEL_REFRESH_KEY
        }
        navController.currentBackStackEntry?.savedStateHandle?.set(
            refreshKey,
            System.currentTimeMillis(),
        )
        return
    }
    if (route == Screen.Settings.route) {
        val returnedHome = navController.popBackStack(
            route = Screen.Settings.route,
            inclusive = false,
            saveState = true,
        )
        if (!returnedHome) {
            navController.navigate(Screen.Settings.route) { launchSingleTop = true }
        }
        return
    }
    navController.navigate(route) {
        popUpTo(Screen.Settings.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun activeAudio(store: TaskStore, taskId: String?, trackIndex: Int): TaskAudio? =
    taskId?.let(store::getTaskById)?.audios?.getOrNull(trackIndex)
        ?: store.getDraft().audios.getOrNull(trackIndex)

internal fun absoluteAvatarUrl(url: String): String {
    val normalized = url.trim()
    return when {
        normalized.isEmpty() -> normalized
        normalized.startsWith("http://") ||
            normalized.startsWith("https://") ||
            normalized.startsWith("content://") ||
            normalized.startsWith("file://") -> normalized
        normalized.startsWith("//") -> "https:$normalized"
        normalized.startsWith("/") -> "${ApiClient.BASE_URL}$normalized"
        else -> "${ApiClient.BASE_URL}/$normalized"
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun View.dp(value: Int): Int =
    (value * resources.displayMetrics.density + 0.5f).toInt()
