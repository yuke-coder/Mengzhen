package com.mengzhen.app.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import com.mengzhen.app.R as AppR
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.components.main.absoluteAvatarUrl
import com.mengzhen.app.ui.components.rememberQqMusicImagePicker
import com.mengzhen.app.ui.navigation.Screen
import android.graphics.drawable.GradientDrawable
import coil3.load
import com.tencent.qqmusic.homepage.header.viewdelegate.i as SourceHeaderBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun QqMusicMineHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val store = remember(context) { TaskStore.get(context) }
    val scope = rememberCoroutineScope()
    val user by store.sessionUser.collectAsState()
    val activity = context as? Activity
    val backgroundPicker = rememberQqMusicImagePicker(maxSelection = 1) { selected ->
        val uri = selected.firstOrNull() ?: return@rememberQqMusicImagePicker
        scope.launch {
            uploadSelectedProfileBackground(context, uri, user)
        }
    }

    DisposableEffect(activity) {
        val controller = activity?.window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        val wasLight = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (wasLight != null) controller.isAppearanceLightStatusBars = wasLight
        }
    }

    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate(Screen.Login.route) { launchSingleTop = true }
        }
    }

    val profile = user ?: return Box(
        Modifier.fillMaxSize().background(ComposeColor.Black),
    )
    var showNicknameCompletion by remember(profile.id) { mutableStateOf(false) }
    val sourceView = remember(context) { QqMusicSourceProfileView(context) }
    sourceView.onEdit = { navController.navigate(Screen.ProfileEdit.route) }
    sourceView.onNickname = {
        if (XimalayaNicknameCompletionTrigger.shouldShow(context, profile)) {
            XimalayaNicknameCompletionTrigger.markShown(context)
            showNicknameCompletion = true
        } else {
            navController.navigate(Screen.ProfileEdit.route)
        }
    }
    sourceView.onSettings = { navController.navigate(Screen.AppSettings.route) }
    sourceView.onBackground = backgroundPicker

    DisposableEffect(sourceView) {
        onDispose(sourceView::release)
    }
    AndroidView(
        factory = { sourceView },
        update = { it.bind(profile) },
        modifier = Modifier.fillMaxSize(),
    )
    if (showNicknameCompletion) {
        XimalayaNicknameCompletionSheet(
            user = profile,
            onDismiss = { showNicknameCompletion = false },
        )
    }
}

/**
 * Minimal host for QQ Music 20.7.5.3's original personal-home header resources.
 * The bridge only supplies Mengzhen identity data, background selection and host navigation.
 */
private class QqMusicSourceProfileView(context: Context) : FrameLayout(context) {
    var onEdit: () -> Unit = {}
    var onNickname: () -> Unit = {}
    var onSettings: () -> Unit = {}
    var onBackground: () -> Unit = {}

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val backgroundComposer = SourceHeaderBackground()
    private val header = LayoutInflater.from(context).inflate(SOURCE_LAYOUT_HEADER, this, false)
    private val rawBackground = header.findViewById<ImageView>(SOURCE_ID_RAW_BACKGROUND)
    private val combinedBackground = header.findViewById<ImageView>(SOURCE_ID_COMBINED_BACKGROUND)
    private val defaultBackground = header.findViewById<ImageView>(SOURCE_ID_DEFAULT_BACKGROUND)
    private val avatar = header.findViewById<ImageView>(SOURCE_ID_AVATAR)
    private var lastBoundUser: UserInfo? = null

    private val backgroundObserver = Observer<Bitmap> { bitmap ->
        combinedBackground.setImageBitmap(bitmap)
        combinedBackground.visibility = View.VISIBLE
        rawBackground.visibility = View.GONE
        defaultBackground.visibility = View.GONE
    }

    init {
        setBackgroundColor(Color.BLACK)
        clipChildren = false
        clipToPadding = false
        addView(header)
        backgroundComposer.M6().observeForever(backgroundObserver)
        installSourceActions()
        applySourceDarkPalette()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutSource(w)
    }

    private fun layoutSource(screenWidth: Int) {
        if (screenWidth <= 0) return
        val adjustedWidth = (screenWidth * SOURCE_WIDTH_PERCENT).roundToInt()
        val backgroundHeight = adjustedWidth * 2
        header.layoutParams = LayoutParams(screenWidth, backgroundHeight)

        header.findViewById<View>(SOURCE_ID_PROFILE_CARD).let { card ->
            card.layoutParams = card.layoutParams.apply {
                height = (backgroundHeight * SOURCE_CARD_HEIGHT_PERCENT).roundToInt()
            }
            card.setBackgroundResource(0x7f080b64)
        }

    }

    fun bind(user: UserInfo) {
        if (lastBoundUser == user) return
        lastBoundUser = user
        bindHeader(user)
        bindBackground(user)
    }

    private fun bindHeader(user: UserInfo) {
        header.findViewById<TextView>(SOURCE_ID_NAME).apply {
            text = user.nickname?.takeIf(String::isNotBlank) ?: user.username
            setOnClickListener { onNickname() }
        }
        header.findViewById<TextView>(SOURCE_ID_PROFILE_SUMMARY).text = user.profileSummary()

        avatar.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f * resources.displayMetrics.density
                setColor(SOURCE_HEADER_FALLBACK_COLOR)
            }
            setImageDrawable(null)
            user.avatarUrl?.takeIf(String::isNotBlank)?.let { url ->
                load(absoluteAvatarUrl(url))
            }
            setOnClickListener { onEdit() }
        }

        header.findViewById<Button>(SOURCE_ID_EDIT).apply {
            visibility = View.VISIBLE
            text = "编辑资料"
            setOnClickListener { onEdit() }
        }
        header.findViewById<Button>(SOURCE_ID_SETTINGS).apply {
            visibility = View.VISIBLE
            text = null
            contentDescription = "设置"
            background = null
            gravity = Gravity.CENTER
            isAllCaps = false
            minimumWidth = 0
            minimumHeight = 0
            setMinWidth(0)
            setMinHeight(0)
            val density = resources.displayMetrics.density
            val buttonSize = (36f * density).roundToInt()
            val iconPadding = (6f * density).roundToInt()
            val iconSize = (24f * density).roundToInt()
            layoutParams = layoutParams.apply {
                width = buttonSize
                height = buttonSize
            }
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            val settingsIcon = AppCompatResources
                .getDrawable(context, AppR.drawable.ximalaya_mine_setting)
                ?.mutate()
                ?.apply {
                    setTint(Color.WHITE)
                    setBounds(0, 0, iconSize, iconSize)
                }
            setCompoundDrawables(settingsIcon, null, null, null)
            setOnClickListener { onSettings() }
        }

        // === 喜马拉雅真实简介组件（迁移自 main_anchor_space_top_view_v5.xml 的 main_csl_honor_and_personal_info） ===
        val introBio = user.bio?.takeIf(String::isNotBlank)
        val introSignature = user.signature?.takeIf { it.isNotBlank() && it != introBio }
        header.findViewById<TextView>(SOURCE_ID_PERSONAL_INTRO).apply {
            text = introBio ?: "点击填写介绍，让大家认识你吧～"
            setTextColor(if (introBio != null) Color.WHITE else SOURCE_SECONDARY_TEXT_COLOR)
            setOnClickListener { onEdit() }
        }
        header.findViewById<TextView>(SOURCE_ID_PERSONAL_TITLE).apply {
            text = introSignature ?: ""
            setTextColor(SOURCE_SECONDARY_TEXT_COLOR)
            setOnClickListener { onEdit() }
        }
        header.findViewById<ImageView>(SOURCE_ID_MORE_INFO).apply {
            setOnClickListener { onEdit() }
        }
        applySourceDarkPalette()
    }

    private fun bindBackground(user: UserInfo) {
        val url = user.backgroundUrl?.takeIf(String::isNotBlank)
        if (url == null) {
            rawBackground.visibility = View.GONE
            combinedBackground.visibility = View.GONE
            defaultBackground.visibility = View.GONE
            header.setBackgroundColor(SOURCE_HEADER_FALLBACK_COLOR)
            return
        }

        header.setBackgroundColor(SOURCE_HEADER_FALLBACK_COLOR)
        rawBackground.visibility = View.VISIBLE
        combinedBackground.visibility = View.VISIBLE
        defaultBackground.visibility = View.GONE
        rawBackground.load(url) {
            target(
                onSuccess = {
                    val drawable = rawBackground.drawable ?: return@target
                    val bitmap = com.tencent.component.widget.e.a(drawable) ?: return@target
                    scope.launch(Dispatchers.Default) {
                        backgroundComposer.L6(bitmap)
                    }
                },
            )
        }
    }

    private fun installSourceActions() {
        header.findViewById<View>(SOURCE_ID_BACKGROUND_ACTION).setOnClickListener { onBackground() }
    }

    private fun applySourceDarkPalette() {
        header.findViewById<TextView>(SOURCE_ID_NAME).setTextColor(SOURCE_NAME_COLOR)
        header.findViewById<TextView>(SOURCE_ID_PROFILE_SUMMARY).setTextColor(Color.WHITE)
        header.findViewById<Button>(SOURCE_ID_EDIT).setTextColor(Color.WHITE)
        header.findViewById<Button>(SOURCE_ID_SETTINGS).setTextColor(Color.WHITE)
    }

    fun release() {
        backgroundComposer.M6().removeObserver(backgroundObserver)
        scope.cancel()
    }

    private companion object {
        const val SOURCE_WIDTH_PERCENT = 0.975f
        const val SOURCE_CARD_HEIGHT_PERCENT = 0.23f
        const val SOURCE_HEADER_FALLBACK_COLOR = 0xFF1A1A1A.toInt()
        const val SOURCE_NAME_COLOR = 0xFFD7B45D.toInt()
        const val SOURCE_SECONDARY_TEXT_COLOR = 0xB3FFFFFF.toInt()
        const val SOURCE_LAYOUT_HEADER = 0x7f0c04c9
        const val SOURCE_ID_DEFAULT_BACKGROUND = 0x7f090c8c
        const val SOURCE_ID_BACKGROUND_ACTION = 0x7f0915b4
        const val SOURCE_ID_COMBINED_BACKGROUND = 0x7f0915c9
        const val SOURCE_ID_RAW_BACKGROUND = 0x7f0915cb
        const val SOURCE_ID_NAME = 0x7f093651
        const val SOURCE_ID_AVATAR = 0x7f09369a
        const val SOURCE_ID_EDIT = 0x7f094061
        const val SOURCE_ID_SETTINGS = 0x7f09406e
        const val SOURCE_ID_PROFILE_SUMMARY = 0x7f0949c6
        const val SOURCE_ID_PROFILE_CARD = 0x7f095446
        const val SOURCE_ID_MORE_INFO = 0x7f095b85
        const val SOURCE_ID_PERSONAL_INTRO = 0x7f095c16
        const val SOURCE_ID_PERSONAL_TITLE = 0x7f095c18
    }
}

private fun UserInfo.profileSummary(): String = listOfNotNull(
    when (gender?.lowercase()) {
        "male", "男", "1" -> "男"
        "female", "女", "2" -> "女"
        else -> null
    },
    birthday?.let { value ->
        runCatching {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrNull()
    }
        ?.let { date ->
            when (date.monthValue * 100 + date.dayOfMonth) {
                in 120..218 -> "水瓶座"
                in 219..320 -> "双鱼座"
                in 321..419 -> "白羊座"
                in 420..520 -> "金牛座"
                in 521..621 -> "双子座"
                in 622..722 -> "巨蟹座"
                in 723..822 -> "狮子座"
                in 823..922 -> "处女座"
                in 923..1023 -> "天秤座"
                in 1024..1122 -> "天蝎座"
                in 1123..1221 -> "射手座"
                else -> "摩羯座"
            }
        },
    location?.takeIf(String::isNotBlank),
).joinToString(" ")
