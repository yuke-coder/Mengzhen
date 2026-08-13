package com.mengzhen.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.audio.AudioPlaybackService
import com.mengzhen.app.audio.PlaybackStateStore
import com.mengzhen.app.audio.PlaybackTransportState
import com.mengzhen.app.data.model.ScheduledTask
import com.mengzhen.app.data.model.TaskAudio
import com.mengzhen.app.data.store.AudioLibraryState
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.scheduler.AlarmScheduler
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * NewShowNotesDetailFragment + gatekeeper/play-page-introduce/v2 的本地音频适配。
 *
 * 原版 9.4.95.3 的「声音详情」不是原生表单，而是 fullscreen NativeHybridFragment。
 * 本页继续使用 WebView 宿主；层级、选择器尺寸、滚动导航和底部播放栏均直接取自
 * 官方 play-page-introduce 源码，只把远端 track 数据替换为真实 TaskAudio。
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun XimalayaSoundDetailsScreen(
    navController: NavController,
    taskId: String,
    audioIndex: Int,
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val store = remember(context) { TaskStore.get(context) }
    val task = remember(taskId) { store.getTaskById(taskId) }
    val scope = rememberCoroutineScope()
    val resolvedIndex = audioIndex.coerceIn(0, (task?.audios?.lastIndex ?: 0).coerceAtLeast(0))
    val audio = task?.audios?.getOrNull(resolvedIndex)
    val playbackSnapshot by remember(context) {
        PlaybackStateStore.get(context).snapshot
    }.collectAsState()
    val artwork by rememberLocalAudioArtwork(audio?.localUri ?: audio?.serverUrl)
    val preferences = remember(context) {
        context.getSharedPreferences("ximalaya_player_actions", Context.MODE_PRIVATE)
    }
    var liked by remember(taskId, audio?.id, audio?.savedToLibrary) {
        mutableStateOf(audio?.savedToLibrary == true)
    }
    var favoriteUpdating by remember(taskId, audio?.id) { mutableStateOf(false) }
    var waited by remember(taskId, audio?.id) {
        mutableStateOf(
            preferences.getBoolean(
                "sound_details_waited_${taskId}_${audio?.id.orEmpty()}",
                false,
            ),
        )
    }
    var webView by remember { mutableStateOf<WebView?>(null) }

    val snapshotIsCurrent = playbackSnapshot.taskId == taskId &&
        playbackSnapshot.trackIndex == resolvedIndex &&
        !playbackSnapshot.isTerminal &&
        AudioPlaybackService.getCurrentTaskId() == taskId
    val isPlaying = snapshotIsCurrent &&
        playbackSnapshot.transportState == PlaybackTransportState.PLAYING

    val closePage = rememberUpdatedState {
        navController.popBackStack()
    }
    val togglePlayback = rememberUpdatedState {
        when {
            snapshotIsCurrent && isPlaying -> AudioPlaybackService.pause(context)
            snapshotIsCurrent -> AudioPlaybackService.resume(context)
            !AlarmScheduler.get(context).startManualPlayback(taskId, resolvedIndex) ->
                AppNotice.error(context, "音频暂时无法播放，请重新选择文件")
        }
    }
    val share = rememberUpdatedState {
        scope.launch {
            shareCurrentAudio(context, audio, audio?.name ?: task?.name ?: "音频")
        }
    }
    val toggleLike = rememberUpdatedState {
        if (!favoriteUpdating && audio != null) {
            val favorite = !liked
            favoriteUpdating = true
            scope.launch {
                AudioLibraryState.setFavorite(context, taskId, audio, favorite)
                    .onSuccess { liked = favorite }
                    .onFailure { error ->
                        AppNotice.error(context, error.message ?: "更新收藏状态失败")
                    }
                favoriteUpdating = false
            }
        }
    }
    val toggleWaited = rememberUpdatedState {
        waited = !waited
        preferences.edit()
            .putBoolean("sound_details_waited_${taskId}_${audio?.id.orEmpty()}", waited)
            .apply()
        AppNotice.success(context, if (waited) "已加入待播" else "已移出待播")
    }
    val showComments = rememberUpdatedState {
        AppNotice.info(context, "本地音频暂无评论")
    }
    val openAlbum = rememberUpdatedState {
        navController.popBackStack()
    }
    val bridge = remember {
        XimalayaShowNotesBridge(
            onClose = { closePage.value.invoke() },
            onTogglePlayback = { togglePlayback.value.invoke() },
            onShare = { share.value.invoke() },
            onToggleLike = { toggleLike.value.invoke() },
            onToggleWaited = { toggleWaited.value.invoke() },
            onComments = { showComments.value.invoke() },
            onOpenAlbum = { openAlbum.value.invoke() },
        )
    }

    BackHandler { closePage.value.invoke() }
    XimalayaHybridWindowEffect(isDark = isDark)

    val density = LocalDensity.current
    val statusBarCssPx = with(density) {
        WindowInsets.statusBars.getTop(this).toDp().value.roundToInt()
    }.coerceAtLeast(0)
    val sourceIcons = remember(context, isDark) {
        XimalayaShowNotesIcons(
            back = bitmapResourceDataUri(context, R.drawable.arg_res_0x7f080ab2),
            share = bitmapResourceDataUri(context, R.drawable.arg_res_0x7f0826b2),
            play = SHOW_NOTES_PLAY_ICON,
            pause = SHOW_NOTES_PAUSE_ICON,
            add = if (isDark) SHOW_NOTES_WAIT_ICON_DARK else SHOW_NOTES_WAIT_ICON_LIGHT,
            comment = if (isDark) SHOW_NOTES_COMMENT_ICON_DARK else SHOW_NOTES_COMMENT_ICON_LIGHT,
            like = if (isDark) SHOW_NOTES_LIKE_ICON_DARK else SHOW_NOTES_LIKE_ICON_LIGHT,
            liked = SHOW_NOTES_LIKED_ICON,
            defaultCover = bitmapResourceDataUri(context, R.drawable.xm_ad_default_album),
        )
    }
    val coverData = remember(artwork, sourceIcons.defaultCover) {
        artwork?.let(::bitmapDataUri) ?: sourceIcons.defaultCover
    }
    val html = remember(task, audio, coverData, sourceIcons, isDark, statusBarCssPx) {
        ximalayaShowNotesHtml(
            task = task,
            audio = audio,
            coverData = coverData,
            icons = sourceIcons,
            isDark = isDark,
            statusBarCssPx = statusBarCssPx,
        )
    }
    val currentWebPlaybackState = rememberUpdatedState(
        XimalayaShowNotesPlaybackUiState(
            isPlaying = isPlaying,
            isCurrent = snapshotIsCurrent,
            isLiked = liked,
            isWaited = waited,
        ),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    setBackgroundColor(if (isDark) Color.rgb(19, 19, 19) else Color.rgb(248, 248, 248))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.textZoom = 100
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            view.applyShowNotesPlaybackState(currentWebPlaybackState.value)
                        }
                    }
                    addJavascriptInterface(bridge, SHOW_NOTES_BRIDGE_NAME)
                    loadDataWithBaseURL(
                        SHOW_NOTES_SOURCE_URL,
                        html,
                        "text/html",
                        Charsets.UTF_8.name(),
                        null,
                    )
                    tag = html.hashCode()
                    webView = this
                }
            },
            update = { view ->
                webView = view
                if (view.tag != html.hashCode()) {
                    view.setBackgroundColor(
                        if (isDark) Color.rgb(19, 19, 19) else Color.rgb(248, 248, 248),
                    )
                    view.loadDataWithBaseURL(
                        SHOW_NOTES_SOURCE_URL,
                        html,
                        "text/html",
                        Charsets.UTF_8.name(),
                        null,
                    )
                    view.tag = html.hashCode()
                }
            },
            onRelease = { view ->
                view.removeJavascriptInterface(SHOW_NOTES_BRIDGE_NAME)
                view.stopLoading()
                view.loadUrl("about:blank")
                view.destroy()
                if (webView === view) webView = null
            },
        )
    }

    LaunchedEffect(webView, isPlaying, snapshotIsCurrent, liked, waited) {
        webView?.applyShowNotesPlaybackState(
            XimalayaShowNotesPlaybackUiState(
                isPlaying = isPlaying,
                isCurrent = snapshotIsCurrent,
                isLiked = liked,
                isWaited = waited,
            ),
        )
    }

}

@Composable
private fun XimalayaHybridWindowEffect(isDark: Boolean) {
    val context = LocalContext.current
    DisposableEffect(context, isDark) {
        val activity = context.findActivity()
        val window = activity?.window
        if (window == null) {
            onDispose { }
        } else {
            @Suppress("DEPRECATION")
            val oldStatusBarColor = window.statusBarColor
            @Suppress("DEPRECATION")
            val oldNavigationBarColor = window.navigationBarColor
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val oldLightStatusBars = controller.isAppearanceLightStatusBars
            val oldLightNavigationBars = controller.isAppearanceLightNavigationBars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            run {
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor =
                    if (isDark) Color.rgb(40, 40, 40) else Color.WHITE
            }
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
            onDispose {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                @Suppress("DEPRECATION")
                run {
                    window.statusBarColor = oldStatusBarColor
                    window.navigationBarColor = oldNavigationBarColor
                }
                controller.isAppearanceLightStatusBars = oldLightStatusBars
                controller.isAppearanceLightNavigationBars = oldLightNavigationBars
            }
        }
    }
}

private class XimalayaShowNotesBridge(
    private val onClose: () -> Unit,
    private val onTogglePlayback: () -> Unit,
    private val onShare: () -> Unit,
    private val onToggleLike: () -> Unit,
    private val onToggleWaited: () -> Unit,
    private val onComments: () -> Unit,
    private val onOpenAlbum: () -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun close() = mainHandler.post(onClose)

    @JavascriptInterface
    fun togglePlayback() = mainHandler.post(onTogglePlayback)

    @JavascriptInterface
    fun share() = mainHandler.post(onShare)

    @JavascriptInterface
    fun toggleLike() = mainHandler.post(onToggleLike)

    @JavascriptInterface
    fun toggleWaited() = mainHandler.post(onToggleWaited)

    @JavascriptInterface
    fun comments() = mainHandler.post(onComments)

    @JavascriptInterface
    fun openAlbum() = mainHandler.post(onOpenAlbum)
}

private data class XimalayaShowNotesIcons(
    val back: String,
    val share: String,
    val play: String,
    val pause: String,
    val add: String,
    val comment: String,
    val like: String,
    val liked: String,
    val defaultCover: String,
)

private data class XimalayaShowNotesPlaybackUiState(
    val isPlaying: Boolean,
    val isCurrent: Boolean,
    val isLiked: Boolean,
    val isWaited: Boolean,
)

private fun WebView.applyShowNotesPlaybackState(state: XimalayaShowNotesPlaybackUiState) {
    evaluateJavascript(
        "window.setPlaybackState(" +
            "${state.isPlaying},${state.isCurrent},${state.isLiked},${state.isWaited}" +
            ");",
        null,
    )
}

private fun ximalayaShowNotesHtml(
    task: ScheduledTask?,
    audio: TaskAudio?,
    coverData: String,
    icons: XimalayaShowNotesIcons,
    isDark: Boolean,
    statusBarCssPx: Int,
): String {
    val rawTitle = audio?.name?.ifBlank { null } ?: task?.name ?: "音频播放器"
    val title = rawTitle
        .substringAfterLast('.', "")
        .lowercase(Locale.ROOT)
        .takeIf { it in setOf("mp3", "wav", "ogg", "m4a", "flac", "aac") }
        ?.let { rawTitle.substringBeforeLast('.') }
        ?: rawTitle
    val albumTitle = task?.name?.ifBlank { null } ?: "本地音频"
    val duration = formatShowNotesDuration(audio?.duration ?: 0L)
    val createdAt = task?.createdAt?.takeIf { it > 0L } ?: System.currentTimeMillis()
    val createdText = SimpleDateFormat("yyyy/M/d", Locale.CHINA).format(Date(createdAt))
    val mimeType = audio?.mimeType?.substringAfter("audio/", "")?.uppercase(Locale.CHINA)
        ?.takeIf(String::isNotBlank)
        ?: audio?.name?.substringAfterLast('.', "")?.uppercase(Locale.CHINA)
            ?.takeIf(String::isNotBlank)
        ?: "音频"
    val fileSize = formatPlayerFileSize(audio?.size ?: 0L)
    val sourceText = when {
        !audio?.localUri.isNullOrBlank() -> "本地文件"
        !audio?.serverUrl.isNullOrBlank() -> "云端音频"
        else -> "音频文件"
    }
    val titleHtml = title.htmlEncoded()
    val albumHtml = albumTitle.htmlEncoded()
    val detailsHtml = """
        <section class="ContentCard_card__1OD7F">
          <h2 class="ContentCard_title__3bU3d">声音信息</h2>
          <div id="podcastRichInfoContainer">
            <p>${title.htmlEncoded()}</p>
            <p>所属听单　${albumHtml}</p>
            <p>文件格式　${mimeType.htmlEncoded()}</p>
            <p>文件大小　${fileSize.htmlEncoded()}</p>
            <p>声音来源　${sourceText.htmlEncoded()}</p>
          </div>
        </section>
    """.trimIndent()

    return """
        <!doctype html>
        <html lang="zh-CN"${if (isDark) " theme=\"dark\"" else ""}>
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=0,viewport-fit=cover"/>
          <title>声音详情</title>
          <style>
            :root {
              --base-bg-color:#f8f8f8;
              --dark-bg-color:#131313;
              --status-bar-height:${statusBarCssPx}px;
              --title-color:#131313;
              --nav-bar-bg:rgba(248,248,248,0);
              --header-bg:url(https://s1.xmcdn.com/yx/play-page-introduce/last/dist/static/media/podcast-home-bg.93f09f7a.png);
              --info-text-color:#bbb;
              --linear-bg:linear-gradient(180deg,rgba(248,248,248,0),#f8f8f8);
              --bottom-bar-bg-color:#fff;
              --bottom-bar-text-color:#333949;
              --bottom-bar-waited-bg-color:#f0f0f0;
              --bottom-bar-border:.5px solid rgba(0,0,0,.08);
            }
            :root[theme=dark] {
              --base-bg-color:#131313;
              --title-color:#fff;
              --nav-bar-bg:rgba(19,19,19,0);
              --header-bg:url(https://s1.xmcdn.com/yx/play-page-introduce/last/dist/static/media/podcast-home-bg-dark.3e10f68d.png);
              --info-text-color:#66666b;
              --linear-bg:linear-gradient(180deg,rgba(19,19,19,0),#131313);
              --bottom-bar-bg-color:#282828;
              --bottom-bar-text-color:#fff;
              --bottom-bar-waited-bg-color:#66666b;
              --bottom-bar-border:none;
            }
            * { box-sizing:border-box; }
            html,body { margin:0; min-height:100%; background:var(--base-bg-color); }
            body {
              font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","Roboto","Helvetica Neue",sans-serif;
              font-size:15px;
              line-height:26px;
              -webkit-font-smoothing:antialiased;
              -webkit-text-size-adjust:none;
              -webkit-tap-highlight-color:rgba(0,0,0,0);
              color:var(--title-color);
            }
            img { border:0; }
            button { font:inherit; border:0; padding:0; background:none; color:inherit; }
            .NavBar_container__1euYQ {
              position:fixed; left:0; right:0; top:0; display:flex; align-items:center;
              justify-content:space-between; padding:var(--status-bar-height) 18px 0;
              background-color:var(--nav-bar-bg); z-index:99;
            }
            .NavBar_left__250x9 { flex:1; min-width:0; display:flex; align-items:center; overflow:hidden; }
            .NavBar_navBack__28AeL { width:42px; margin-left:-18px; padding-left:18px; display:flex; align-items:center; }
            .NavBar_navItem__1fy3j { width:24px; height:24px; object-fit:contain; }
            .NavBar_trackTitle__3T8_r {
              flex:1; color:rgba(19,19,19,0); font-size:17px; font-weight:700;
              margin-left:5px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;
            }
            :root[theme=dark] .NavBar_trackTitle__3T8_r { color:rgba(255,255,255,0); }
            .NavBar_icon__GleBV { width:24px; height:24px; margin-left:16px; object-fit:contain; }
            .NavBar_playIcon__1-Jl2 { opacity:0; pointer-events:none; transition:opacity .3s ease-in; }
            .NavBar_show__1K4SV { opacity:1; pointer-events:auto; }
            .podcast-container {
              padding:0 18px calc(112px + env(safe-area-inset-bottom));
              min-height:100vh; background:var(--base-bg-color) var(--header-bg) no-repeat 100% 0;
              background-size:375px 372px;
            }
            .PodcastHeader_headerContainer__2yC0Y { position:relative; margin-bottom:27px; padding-top:158px; }
            .PodcastHeader_coverContainer__2LIUv {
              position:absolute; top:58px; right:22px; perspective:2000px;
            }
            .PodcastHeader_cover__1EPTL,.PodcastHeader_shadow__3qLGr {
              width:115px; height:115px; border-radius:16px;
              transform:rotateX(53deg) rotateY(-11deg) rotate(31deg); transform-style:preserve-3d;
            }
            .PodcastHeader_cover__1EPTL {
              margin:0; object-fit:cover; pointer-events:none; position:relative; z-index:10; display:block;
            }
            .PodcastHeader_shadow__3qLGr {
              background:#bac8d4; position:absolute; right:0; top:3px;
            }
            .PodcastHeader_infoContainer__3UxcN { width:100%; position:relative; z-index:11; }
            .PodcastHeader_titleText__2WPYc {
              max-width:95%; font-size:23px; font-weight:700; line-height:36px;
              color:var(--title-color); word-break:break-all;
            }
            .PodcastHeader_otherText__3C3Es {
              display:-webkit-box; overflow:hidden; -webkit-line-clamp:2; -webkit-box-orient:vertical;
            }
            .PodcastHeader_albumContainer__272Ot {
              display:flex; align-items:center; margin-top:10px; width:max-content; max-width:100%;
            }
            .PodcastHeader_albumCoverContainer__2nAL_ {
              width:24px; height:24px; border-radius:2px; overflow:hidden; margin-right:8px; flex:none;
            }
            .PodcastHeader_albumCover__2NIAc { width:24px; height:24px; display:block; object-fit:cover; }
            .PodcastHeader_albumName__l6BWY {
              min-width:20px; max-width:220px; font-size:14px; color:var(--title-color);
              overflow:hidden; text-overflow:ellipsis; white-space:nowrap;
            }
            .PodcastHeader_infoDataContainer__1bD5B {
              display:flex; align-items:center; justify-content:space-between; margin-top:35px; width:100%;
            }
            .PodcastHeader_infoText__3PmTQ {
              font-size:14px; font-weight:500; line-height:normal; text-transform:uppercase;
              color:var(--info-text-color);
            }
            .PodcastHeader_dot__e-BTr { margin:0 4px; }
            .PodcastHeader_icons__1nTd9 { display:flex; align-items:center; }
            .LikeItem_container__31S5z { position:relative; width:26px; height:26px; display:inline-block; }
            .LikeItem_container__31S5z + .LikeItem_container__31S5z { margin-left:22px; }
            .LikeItem_img__4VlXA { width:26px; height:26px; display:block; object-fit:contain; }
            .LikeItem_text__2udY6 {
              position:absolute; top:0; left:20px; font-size:8.26px; line-height:8.26px; color:var(--title-color);
            }
            .ContentCard_card__1OD7F + .ContentCard_card__1OD7F { margin-top:42px; }
            .ContentCard_title__3bU3d {
              color:var(--title-color); font-size:18px; font-weight:500; margin:0 0 8px; line-height:25px;
            }
            #podcastRichInfoContainer { position:relative; text-align:justify; min-height:156px; }
            #podcastRichInfoContainer p { margin:0 0 10px; color:var(--title-color); word-break:break-all; }
            #podcastRichInfoContainer p:not(:first-child) { color:${if (isDark) "rgba(255,255,255,.72)" else "#66666b"}; }
            .BottomBar_bottomBarContainer__knP31 {
              position:fixed; left:0; right:0; bottom:0; z-index:99;
              height:calc(86px + env(safe-area-inset-bottom));
              background:var(--bottom-bar-bg-color); box-shadow:0 0 15px rgba(0,0,0,.07);
              padding-bottom:env(safe-area-inset-bottom);
            }
            .BottomBar_container__bh5HG { padding:15px 18px; display:flex; }
            .BottomBar_button__3nb7X {
              flex:1; border-radius:8px; display:flex; align-items:center; justify-content:center;
              height:44px; font-size:16px; font-weight:500;
            }
            .BottomBar_button__3nb7X + .BottomBar_button__3nb7X { margin-left:12px; }
            .BottomBar_wait__O47KR {
              color:var(--title-color); background:var(--bottom-bar-waited-bg-color);
              border:var(--bottom-bar-border);
            }
            .BottomBar_waited__2eRn3 > * { opacity:.5; }
            .BottomBar_play__1Rsml { background:#f44; color:#fff; }
            .BottomBar_btnIcon__38Z2X { width:20px; height:20px; margin-right:4px; object-fit:contain; }
          </style>
        </head>
        <body>
          <nav id="navbar" class="NavBar_container__1euYQ">
            <span class="NavBar_left__250x9">
              <button class="NavBar_navBack__28AeL" aria-label="返回" onclick="invoke('close')">
                <img class="NavBar_navItem__1fy3j" src="${icons.back}"/>
              </button>
              <span id="navTitle" class="NavBar_trackTitle__3T8_r">${titleHtml}</span>
            </span>
            <img id="navPlay" class="NavBar_icon__GleBV NavBar_playIcon__1-Jl2" src="${icons.play}" aria-label="播放" onclick="invoke('togglePlayback')"/>
            <img class="NavBar_icon__GleBV" src="${icons.share}" aria-label="分享" onclick="invoke('share')"/>
          </nav>

          <main class="podcast-container">
            <header id="headerContainer" class="PodcastHeader_headerContainer__2yC0Y">
              <div class="PodcastHeader_coverContainer__2LIUv">
                <div class="PodcastHeader_shadow__3qLGr"></div>
                <img class="PodcastHeader_cover__1EPTL" src="$coverData"/>
              </div>
              <div class="PodcastHeader_infoContainer__3UxcN">
                <div class="PodcastHeader_titleText__2WPYc">
                  <div class="PodcastHeader_otherText__3C3Es">${titleHtml}</div>
                </div>
                <button class="PodcastHeader_albumContainer__272Ot" onclick="invoke('openAlbum')">
                  <span class="PodcastHeader_albumCoverContainer__2nAL_">
                    <img class="PodcastHeader_albumCover__2NIAc" src="$coverData"/>
                  </span>
                  <span class="PodcastHeader_albumName__l6BWY">${albumHtml}</span>
                </button>
                <div class="PodcastHeader_infoDataContainer__1bD5B">
                  <span class="PodcastHeader_infoText__3PmTQ">
                    ${createdText.htmlEncoded()}<span class="PodcastHeader_dot__e-BTr">·</span>${duration.htmlEncoded()}
                  </span>
                  <span class="PodcastHeader_icons__1nTd9">
                    <button class="LikeItem_container__31S5z" aria-label="评论" onclick="invoke('comments')">
                      <img class="LikeItem_img__4VlXA" src="${icons.comment}"/>
                    </button>
                    <button class="LikeItem_container__31S5z" aria-label="点赞" onclick="invoke('toggleLike')">
                      <img id="likeIcon" class="LikeItem_img__4VlXA" src="${icons.like}"/>
                    </button>
                  </span>
                </div>
              </div>
            </header>
            $detailsHtml
          </main>

          <div class="BottomBar_bottomBarContainer__knP31">
            <div class="BottomBar_container__bh5HG">
              <button id="waitButton" class="BottomBar_button__3nb7X BottomBar_wait__O47KR" onclick="invoke('toggleWaited')">
                <img class="BottomBar_btnIcon__38Z2X" src="${icons.add}"/>
                <span id="waitLabel">加入待播</span>
              </button>
              <button class="BottomBar_button__3nb7X BottomBar_play__1Rsml" onclick="invoke('togglePlayback')">
                <img id="bottomPlayIcon" class="BottomBar_btnIcon__38Z2X" src="${icons.play}"/>
                <span id="playLabel">立即播放</span>
              </button>
            </div>
          </div>

          <script>
            const iconPlay = ${JSONObject.quote(icons.play)};
            const iconPause = ${JSONObject.quote(icons.pause)};
            const iconLike = ${JSONObject.quote(icons.like)};
            const iconLiked = ${JSONObject.quote(icons.liked)};
            const dark = $isDark;
            function invoke(name) {
              const bridge = window.$SHOW_NOTES_BRIDGE_NAME;
              if (bridge && typeof bridge[name] === 'function') bridge[name]();
            }
            function updateNav() {
              const p = Math.max(0, Math.min(1, window.scrollY / 100));
              const bg = dark ? 'rgba(19,19,19,' + p + ')' : 'rgba(248,248,248,' + p + ')';
              const fg = dark ? 'rgba(255,255,255,' + p + ')' : 'rgba(19,19,19,' + p + ')';
              document.getElementById('navbar').style.backgroundColor = bg;
              document.getElementById('navTitle').style.color = fg;
              const headerBottom = document.getElementById('headerContainer').getBoundingClientRect().bottom;
              document.getElementById('navPlay').classList.toggle('NavBar_show__1K4SV', headerBottom <= 88);
            }
            window.setPlaybackState = function(playing, active, liked, waited) {
              document.getElementById('navPlay').src = playing ? iconPause : iconPlay;
              document.getElementById('bottomPlayIcon').src = playing ? iconPause : iconPlay;
              document.getElementById('playLabel').textContent =
                playing ? '暂停播放' : (active ? '继续播放' : '立即播放');
              document.getElementById('likeIcon').src = liked ? iconLiked : iconLike;
              document.getElementById('waitLabel').textContent = waited ? '已加入' : '加入待播';
              document.getElementById('waitButton').classList.toggle('BottomBar_waited__2eRn3', waited);
            };
            window.addEventListener('scroll', updateNav, {passive:true});
            updateNav();
          </script>
        </body>
        </html>
    """.trimIndent()
}

private fun formatShowNotesDuration(seconds: Long): String {
    if (seconds <= 0L) return "未知时长"
    val minutes = seconds / 60
    val remainder = seconds % 60
    return if (minutes < 1L) {
        "${remainder}秒"
    } else if (remainder >= 30L) {
        "${minutes + 1}分钟"
    } else {
        "${minutes}分钟"
    }
}

private fun String.htmlEncoded(): String =
    android.text.TextUtils.htmlEncode(this)

private fun bitmapResourceDataUri(context: Context, resourceId: Int): String =
    BitmapFactory.decodeResource(context.resources, resourceId)
        ?.let(::bitmapDataUri)
        .orEmpty()

private fun bitmapDataUri(bitmap: Bitmap): String {
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    return "data:image/png;base64," +
        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val SHOW_NOTES_BRIDGE_NAME = "MengZhenShowNotes"
private const val SHOW_NOTES_SOURCE_URL =
    "https://mobile.ximalaya.com/gatekeeper/play-page-introduce/v2"

/*
 * play-page-introduce 9.4.95.3 对应线上构建中的原始 H5 data-uri。
 * 这些图标不经过 Compose/Vector 重绘，避免与 Hybrid 页面发生像素偏差。
 */
private const val SHOW_NOTES_PLAY_ICON =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACgAAAAoBAMAAAB+0KVeAAAAMFBMVEUAAAD///////////////////////////////////////////////////////////87TQQwAAAAD3RSTlMA378gYO8Qz4+fQICvb1CojK7YAAAA+ElEQVQoz2MgGZgcj/8qeRlVzOk/GKggizn+hwIphFjGfzhog4kxyyMEvxtABQ3/IwFhiBhbPbLgxwSwIMd/FNAAFlyPKvgFLAi2Jmo/XD9IjAXMVGDWh4k6AAWZIIIMXDCHKQAF7WEs7niI4Geg4H24dCbCpvlwQYZFYOYfkOUIQQZXsE+BjHgkQTaQw74CGVAroWEDYmMVRNPODNNejyToCPPneYggImB/Aln+cEFLiPF/UbwpD/MmIkBYEAGCCLr5iKCD+VMOOZBhNiHAL9wRxyaPLPY9AXdiYGCuR0oLBlgTGBwshImJ4Eq0CGA7Mf5rjTPJuQIA9p+ZbKJ4r+IAAAAASUVORK5CYII="
private const val SHOW_NOTES_PAUSE_ICON =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACgAAAAoBAMAAAB+0KVeAAAALVBMVEUAAAD///////////////////////////////////////////////////////+hSKubAAAADnRSTlMAgL9AEO/fYK+fz49fIDUMo/kAAADFSURBVCjPYyAZuE9aGqZ5BFWs6B0YKCOLGQEF0EUZ38HBBpgYbx9C8JEDVNDtHRLIhQrqIQu+gIhxv0MBAmBBO1TBxwjdSFaBxJghehghBBAUAAWZ0AUVgIJ+6IJPgILn0AWfAwXnoQu+hFiOEIRZn4cu+AooGIcu+BSX4Dp0wWe4LOrD5qQ6dME3WL2JK0DY0QUNsAUyzuiA6MFIDn2ouiHgGrJgClSQBcmqFxeAAujJQQBvUkSIqqMmZZ9JqVEaJSTnCgCT1XaHHaiuXwAAAABJRU5ErkJggg=="
private const val SHOW_NOTES_WAIT_ICON_DARK =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADwAAAA8BAMAAADI0sRBAAAAFVBMVEUAAAD///////////////////////9Iz20EAAAABnRSTlMAgL9g70B9vIxcAAAAVklEQVQ4y2MYBQMCGNOwAgF6SLMKYgUBDKMAGRgKGuCTVktTwBqh9JdGRCiKNCagsbSREhC4pTmBKCxBJ4acFCmTJmA3HfyNCPPBlxwIegwznY8CCgAATW1fFu0EItMAAAAASUVORK5CYII="
private const val SHOW_NOTES_WAIT_ICON_LIGHT =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADwAAAA8BAMAAADI0sRBAAAAFVBMVEUAAAASEhITExMTExMTExMUFBQTExNFtMaNAAAABnRSTlMAgL9g70B9vIxcAAAAVklEQVQ4y2MYBQMCGNOwAgF6SLMKYgUBDKMAGRgKGuCTVktTwBqh9JdGRCiKNCagsbSREhC4pTmBKCxBJ4acFCmTJmA3HfyNCPPBlxwIegwznY8CCgAATW1fFu0EItMAAAAASUVORK5CYII="
private const val SHOW_NOTES_LIKED_ICON =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAE4AAABOCAMAAAC5dNAvAAAANlBMVEUAAAD/RET/RET/QED/QED/RET/QkL/Q0P/Q0P/RET/RUX/RET/RET/Q0P/RUX/Q0P/RET/RERZBQ+4AAAAEXRSTlMAv0AgEI9g35/vb8+vUDBfgKABH1AAAAGTSURBVFjD7ZfZbkMhDEQxYLa7+v9/tpUadYlpr415aKR7XiNNMgZmHHdz8+8IreDZDh+nqO2ZHmT0drmFvgFWwUA/QZucpydgrhzhXDlKc+WySW7qz9uYmOk0DuoQhuVaT24blkOmZborK00dXu7JVdOT5RhOYqYc9OWC5U1wBpO5UJ+pB0HVNDkOMFb0189fxXnZOjr81aXTscpPgqOJ6egH3C6/aKX14+KjTg77Wp+FsOvkjr7WV6ZlhRh/ei0/fVgso/Os/rxldIXfomy4xcDfeTGMDln9adzCRVgW9041JEC1uPVX4es1IVV54CeDW3CM2EmIVSjXHAe422TIzsbdRuHo+oHJ3cJoEvNHtYvdNsk6h+KID5ItYmFulUVe2ZeK3BbZVtKEbg/XJ3XcLqOj49csiOo7S9ecJHKL0k0CRG4P4SYhrO8oWf3F9b1c/8/R1DcK12te3/rFrpK2vqNwXef1rf+LlkhXaHX/Uy6Spr4r7Lr9/3SjcLeQgjMSHoPPZYtuAgGIFvRuGjG6m9fnDaudnzpi9ehRAAAAAElFTkSuQmCC"
private const val SHOW_NOTES_LIKE_ICON_DARK =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAE4AAABOCAMAAAC5dNAvAAAANlBMVEUAAAD////////////////////////////////////////////////////////////////////xY8b8AAAAEXRSTlMAQL+AIN8Q72Cfj2+vMM9QX7kOBZkAAAHrSURBVFjD7ZjbcoMwDETj+w0D+v+f7Zh2ooCgMlYeOp3sU5t2DvJaKzF5fPTRn5MtOZuyKv8WWnXwI5eVHOfgRVoKtLBXluFUY6Skn0UmOe77JxOwPjGuqWzAKMXhNTeeE+IC/hobfRnHLc9ysG+mcdyKOCzPDuMKAMyvfSg7bTq2mhb1ygQA5vWD3LI2jGudsR7vJgzPE3SeNOKACqmlSnCahN42nCRiiuL8eJuEEzcFxZmL1I05FzxNHWiiqVSOFrE4lIErFWZNBCyOxbGTwc+nj7TuEpdY2nTyh9WcqGV7vobFrQjX0WDogb5iTbAp2FtNkH9j3eovj5OHsFD1RsPT6C0OSdrhvfLKJzehkBXtllctsS4jq5V6Y2d5nDz7veDik3DjBWIh1lEDJmZDc/tI7Y8X+5eMw1lxWFtxP3fVsHWP42XOvW+HK44eMuDwY8MFg5ZBi172a6F2Wlf4PcgEgzW54PHIIZjdzj2nPxjpskEdZq8/GAE9kQSDvhaJgkGtEwaDtoM8GGSuC4JB/BEEgzxREgw61yXBoCtRFAy6EoXBoI0vDwa1jg+GuhZ60xkMXr5zVNs4BQaF/8wEoxYNPQqVX3MmOeiD6cqOiJ1m4ZdFEVA62nd97+TS4h9vkNXkiDL5x0f/QF9t3T3CxVSP8QAAAABJRU5ErkJggg=="
private const val SHOW_NOTES_LIKE_ICON_LIGHT =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAE4AAABOCAMAAAC5dNAvAAAAOVBMVEUAAAATExMQEBATExMSEhIQEBASEhIQEBATExMTExMTExMTExMSEhIUFBQQEBASEhITExMSEhITExOQf5ZxAAAAEnRSTlMAv0BggCDfEO+fj69vQDDPUHDFFhhNAAAB7ElEQVRYw+2Y2XKDMAxFMd5XCP//sY2ZTlRHUBkrD50O9y2LT2RZ15fJdOvWn5NOIagks/0IrZjtWyZkPu5JAwkuUG+tAg83V4b34lWk5+Omqqwc1MfGVT12oOTi4JgrzzBxDl7KSl/HceurHJibOI6TgIPy9DAuPVcvE0jzduvfR02wZiU+V6vph0L12jDOwaDB2bhRWoHOo0EcUEK1FA5OINNrBi7XtRnj7PiYuINuMopTJ64b65yz2HWbQIqpkH6F4kBqO9ODiAkHxZE48mawy+FPanOK8yQtHnwg1YFic/PgNXsRhhiwtgfijBW3XU5fGoLwG+vSfFm4eRALVC4MPLbeaoC0537qxYWDk8jAknr3q+C0LgCrlnohsyzcPG0uGPkiXHiAWFHrcAMikdBUHs3t9mR/yNRjU0Rs6Uqfh1s3vR/mQti63YfFb7cdVZQxcBm46LWNhdLZukTnIG0MaHI+SWlDboLOdnyYF4zhTwfUgPf6jeGgJxxj4MciljFw61jGwOPAMwaugG+MDP1hGQP/IsMYRCSSxqAjkWUMHIlMY+DB5xsDt442xnwudDlRxqBlO69qLaMjUPBlwhglia1HrtAxp7zZ+mCikFdEoyWQYUvvFp6lPvW/k/GrnT4gLdAWebLTrX+gL6ETQU6kxT/wAAAAAElFTkSuQmCC"
private const val SHOW_NOTES_COMMENT_ICON_DARK =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAE4AAABOBAMAAAB8hD0uAAAAMFBMVEUAAAD///////////////////////////////////////////////////////////87TQQwAAAAD3RSTlMAIEC/31+fEM9g74+AcK9JZ1t6AAAA2UlEQVRIx2MYBSMCFAqiA3Fsym78xwDfsCjj/k+cuvlEqusnUt3//xOJ8Qfj//8CmKKj6qigjj0trQC/OoTU0FPH4oIADnjU8SOl0w90VMekhAAKgzD8qK2ONRQBAgZhfDAbI4DBIAy/UXWDQh07tN4qwKyLJf//x6wHE8AcMdT68AeSuvtI6pTR6s2fmPV0AhZl/zcgO3cGVB2msl40fwkKvgeqmwKU+XUQfy2c/z/9FEjZcTRxTHVexChj2A8OhQkMhEA8UNlnBQaCoII4ZQzsKW4KDKMAAgA0osuL68q+aQAAAABJRU5ErkJggg=="
private const val SHOW_NOTES_COMMENT_ICON_LIGHT =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAE4AAABOBAMAAAB8hD0uAAAAMFBMVEUAAAAQEBAQEBATExMSEhITExMTExMQEBASEhITExMTExMUFBQSEhISEhITExMTExMPbKlxAAAAD3RSTlMAIEC/31+fEM9g74+AcK9JZ1t6AAAA2UlEQVRIx2MYBSMCFAqiA3Fsym78xwDfsCjj/k+cuvlEqusnUt3//xOJ8Qfj//8CmKKj6qigjj0trQC/OoTU0FPH4oIADnjU8SOl0w90VMekhAAKgzD8qK2ONRQBAgZhfDAbI4DBIAy/UXWDQh07tN4qwKyLJf//x6wHE8AcMdT68AeSuvtI6pTR6s2fmPV0AhZl/zcgO3cGVB2msl40fwkKvgeqmwKU+XUQfy2c/z/9FEjZcTRxTHVexChj2A8OhQkMhEA8UNlnBQaCoII4ZQzsKW4KDKMAAgA0osuL68q+aQAAAABJRU5ErkJggg=="
