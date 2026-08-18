package com.mengzhen.app.ui.components.main

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityManager
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.target
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
import com.mengzhen.app.ui.screens.BiliHomeAvatarDestination
import com.mengzhen.app.ui.screens.resolveBiliHomeAvatarDestination
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import com.mengzhen.app.ui.theme.LocalThemeMode
import com.mengzhen.app.ui.theme.ThemeMode
import com.mengzhen.app.ui.theme.ThemeModeStore
import com.tencent.qqmusic.business.playernew.view.playersong.ai as QqThemeModePopupWindow
import com.tencent.qqmusicplayerprocess.audio.playlist.y
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
    val systemDark = (context.applicationContext.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val scope = rememberCoroutineScope()
    var showRestartPrompt by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .height(42.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BiliSourceHomeAvatar(
            user = user,
            onClick = {
                val route = if (user == null) {
                    Screen.Login.route
                } else {
                    resolveBiliHomeAvatarDestination(
                        context,
                        BiliHomeAvatarDestination.fromStoredValue(
                            store.getHomeAvatarDestination(),
                        ),
                    )
                }
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
                trigger.setImageResource(selectedMode.qqSourceIcon)
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
                            override fun a(count: Int) = Unit
                            override fun b() = Unit
                            override fun c() = Unit
                            override fun d() = Unit

                            override fun e(sourceMode: Int) {
                                val mode = sourceMode.toThemeMode()
                                if (mode == selectedMode) return
                                selectedMode = mode
                                scope.launch {
                                    ThemeModeStore.setMode(context, mode)
                                    val nextDark = when (mode) {
                                        ThemeMode.LIGHT -> false
                                        ThemeMode.DARK -> true
                                        ThemeMode.SYSTEM -> systemDark
                                    }
                                    if (nextDark != darkTheme) showRestartPrompt = true
                                }
                            }
                        }
                    )
                    trigger.tag = popup
                    popup.t(darkTheme)
                    popup.r(selectedMode.qqSourceValue)
                    popup.s(y())
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

    if (showRestartPrompt) {
        AlertDialog(
            onDismissRequest = { showRestartPrompt = false },
            title = { Text("新的设置需要重启应用才能生效") },
            confirmButton = {
                TextButton(onClick = {
                    showRestartPrompt = false
                    context.findActivity()?.recreate()
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRestartPrompt = false }) { Text("取消") }
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

private val ThemeMode.qqSourceIcon: Int
    get() = when (this) {
        ThemeMode.LIGHT -> R.drawable.popup_window_play_mode_shuffle
        ThemeMode.DARK -> R.drawable.popup_window_play_mode_list_repeat
        ThemeMode.SYSTEM -> R.drawable.popup_window_play_mode_onshot_repeat
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
    val playback by remember(context) { PlaybackStateStore.get(context).snapshot }
        .collectAsState()
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
                            R.id.xm_main_tab_cache -> Screen.BiliCache.route
                            R.id.xm_main_tab_audio -> Screen.Tasks.route
                            R.id.xm_main_tab_mine -> Screen.Profile.route
                            else -> Screen.Settings.route
                        }
                    }
                    group.setOnCheckedChangeListener { changedGroup, checkedId ->
                        updateTabSelection(root, checkedId)
                    }
                    listOf(
                        R.id.xm_main_tab_home,
                        R.id.xm_main_tab_cache,
                        R.id.xm_main_tab_audio,
                        R.id.xm_main_tab_mine,
                    ).forEach { id ->
                        root.findViewById<RadioButton>(id).setOnClickListener {
                            if (group.checkedRadioButtonId != id) {
                                group.check(id)
                            } else {
                                updateTabSelection(root, id)
                            }
                            navigateTopLevel(navController, destinationFor(id))
                        }
                    }
                    root.setOnTouchListener { view, event ->
                        val slotWidth = view.width / 5f
                        val slot = (event.x / slotWidth).toInt().coerceIn(0, 4)
                        val id = when (slot) {
                            0 -> R.id.xm_main_tab_home
                            1 -> R.id.xm_main_tab_cache
                            3 -> R.id.xm_main_tab_audio
                            4 -> R.id.xm_main_tab_mine
                            else -> null
                        }
                        if (id == null) {
                            false
                        } else {
                            if (event.actionMasked == android.view.MotionEvent.ACTION_UP) {
                                root.findViewById<RadioButton>(id).performClick()
                            }
                            true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(63.dp),
            update = { root ->
                val group = root.findViewById<RadioGroup>(R.id.xm_main_tabs)
                val selectedId = when (currentRoute) {
                    Screen.BiliCache.route -> R.id.xm_main_tab_cache
                    Screen.Tasks.route -> R.id.xm_main_tab_audio
                    Screen.Profile.route -> R.id.xm_main_tab_mine
                    else -> R.id.xm_main_tab_home
                }
                if (group.checkedRadioButtonId != selectedId) {
                    group.check(selectedId)
                } else {
                    updateTabSelection(root, selectedId)
                }

                root.findViewById<View>(R.id.xm_main_player).setOnClickListener { openPlayer() }
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
            },
        )
    }
}

internal fun installTabLotties(root: View, darkTheme: Boolean) {
    val folder = if (darkTheme) "lottie-night-v9514" else "lottie-v9514"
    val tabs = listOf(
        R.id.xm_main_tab_home to "bottom_tab_home_page_btn.json",
        R.id.xm_main_tab_cache to "bottom_tab_quick_listen_page_btn.json",
        R.id.xm_main_tab_audio to "bottom_tab_finding_btn.json",
        R.id.xm_main_tab_mine to "bottom_tab_mine_v9_btn.json",
    )
    tabs.forEach { (id, file) ->
        val button = root.findViewById<RadioButton>(id)
        LottieCompositionFactory.fromAsset(root.context, "$folder/$file")
            .addListener { composition ->
                val drawable = LottieDrawable().apply {
                    setComposition(composition)
                    setBounds(0, 0, root.dp(24), root.dp(24))
                    progress = if (button.isChecked) 1f else 0f
                    callback = button
                }
                button.setCompoundDrawables(null, drawable, null, null)
                button.invalidate()
            }
    }
}

internal fun updateTabSelection(root: View, checkedId: Int) {
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
        (button.compoundDrawables[1] as? LottieDrawable)?.let { drawable ->
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

private fun navigateTopLevel(navController: NavController, route: String) {
    if (navController.currentDestination?.route == route) return
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
