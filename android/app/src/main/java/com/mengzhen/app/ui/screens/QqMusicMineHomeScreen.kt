package com.mengzhen.app.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mengzhen.app.R as AppR
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.model.parseUser
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.components.main.absoluteAvatarUrl
import com.mengzhen.app.ui.components.rememberQqMusicImagePicker
import com.mengzhen.app.ui.navigation.Screen
import com.mengzhen.app.ui.navigation.TOP_LEVEL_REFRESH_KEY
import com.mengzhen.app.ui.membership.MengzhenMembershipDialog
import com.mengzhen.app.util.ConstellationUtils
import com.mengzhen.app.util.genderLabel
import com.mengzhen.app.util.isGenderSecret
import android.graphics.drawable.GradientDrawable
import coil3.load
import coil3.size.Size
import coil3.toBitmap
import com.tencent.qqmusic.homepage.header.viewdelegate.i as SourceHeaderBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun QqMusicMineHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val store = remember(context) { TaskStore.get(context) }
    val api = remember(context) { ApiClient.get(context) }
    val scope = rememberCoroutineScope()
    val user by store.sessionUser.collectAsState()
    // 资料卡的性别 / 生日(星座) / 地区跟随编辑资料页的「不展示」开关：
    // 开关写的是服务端 hide_birthday / hide_region，保存后 sessionUser 会带新值重新发射，
    // 卡片随之重画 —— 用户勾了不展示、回到我的页不会仍然看到真值。
    val activity = context as? Activity
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    val backgroundPicker = rememberQqMusicImagePicker(maxSelection = 1) { selected ->
        val uri = selected.firstOrNull() ?: return@rememberQqMusicImagePicker
        android.widget.Toast.makeText(context, "正在上传背景图...", android.widget.Toast.LENGTH_SHORT).show()
        scope.launch {
            val result = uploadSelectedProfileBackground(context, uri, user)
            if (result != null) {
                android.widget.Toast.makeText(context, "背景图已更新", android.widget.Toast.LENGTH_SHORT).show()
            }
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
            // 未登录的个人页只是登录入口，不应留在返回栈中；否则返回登录页后
            // 个人页会再次触发这段跳转，形成“返回键只闪键盘”的循环。
            navController.navigate(Screen.Login.createRoute(Screen.Profile.route)) {
                popUpTo(Screen.Profile.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val profile = user ?: return Box(
        Modifier.fillMaxSize().background(ComposeColor.Black),
    )
    // Do not render a previous account's snapshot during the one composition
    // frame before bindAccount() completes.
    val lifecycleOwner = LocalLifecycleOwner.current
    val refreshProfile = {
        scope.launch {
            val current = store.sessionUser.value ?: return@launch
            val remote = withContext(Dispatchers.IO) {
                runCatching { api.me() }
                    .getOrNull()
                    ?.let(::parseUser)
            } ?: return@launch
            if (remote.id != current.id) return@launch

            val merged = current.copy(
                username = remote.username.ifBlank { current.username },
                avatarUrl = remote.avatarUrl ?: current.avatarUrl,
                nickname = remote.nickname ?: current.nickname,
                gender = remote.gender ?: current.gender,
                birthday = remote.birthday ?: current.birthday,
                location = remote.location ?: current.location,
                bio = remote.bio ?: current.bio,
                signature = remote.signature ?: current.signature,
                backgroundUrl = remote.backgroundUrl ?: current.backgroundUrl,
                createdAt = remote.createdAt.ifBlank { current.createdAt },
                mobile = remote.mobile ?: current.mobile,
                usernameChangeCount = remote.usernameChangeCount ?: current.usernameChangeCount,
                usernameChangeResetAt = remote.usernameChangeResetAt
                    ?: current.usernameChangeResetAt,
                // 保密开关也要合并：Web 端改过之后回到本页要能反映出来
                hideBirthday = remote.hideBirthday ?: current.hideBirthday,
                hideRegion = remote.hideRegion ?: current.hideRegion,
                // 「保密」前的真实性别（取消保密时回退用）同理：可能是刚在 Web 端选的
                lastGender = remote.lastGender ?: current.lastGender,
            )
            if (merged != current) {
                store.getSession()?.first?.let { token ->
                    store.saveUserSession(token, merged)
                }
            }
        }
    }
    DisposableEffect(lifecycleOwner, profile) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshProfile()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(currentBackStackEntry) {
        val savedStateHandle = currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        savedStateHandle.getStateFlow(TOP_LEVEL_REFRESH_KEY, 0L).collect { token ->
            if (token > 0L) {
                savedStateHandle[TOP_LEVEL_REFRESH_KEY] = 0L
                refreshProfile()
            }
        }
    }
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
    sourceView.onMembership = { MengzhenMembershipDialog.show(context) }

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
    var onMembership: () -> Unit = {}

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val backgroundComposer = SourceHeaderBackground()
    private val header = LayoutInflater.from(context).inflate(SOURCE_LAYOUT_HEADER, this, false)
    private val rawBackground = header.findViewById<ImageView>(SOURCE_ID_RAW_BACKGROUND)
    private val combinedBackground = header.findViewById<ImageView>(SOURCE_ID_COMBINED_BACKGROUND)
    private val defaultBackground = header.findViewById<ImageView>(SOURCE_ID_DEFAULT_BACKGROUND)
    private val avatar = header.findViewById<ImageView>(SOURCE_ID_AVATAR)
    private val membershipEntry = TextView(context).apply {
        text = "梦枕会员  ¥6起"
        textSize = 12f
        setTextColor(0xFFFFE5D8.toInt())
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f * resources.displayMetrics.density
            setColor(0x553A211B)
        }
        contentDescription = "梦枕会员"
        setOnClickListener { onMembership() }
    }
    private var lastBoundUser: UserInfo? = null
    private var released = false

    private val backgroundObserver = Observer<Bitmap?> { bitmap ->
        if (bitmap == null) return@Observer
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
        addView(
            membershipEntry,
            LayoutParams(dp(116f), dp(36f), Gravity.TOP or Gravity.END),
        )
        attachBackgroundObserver()
        installSourceActions()
        applySourceDarkPalette()
    }

    private fun attachBackgroundObserver() {
        backgroundComposer.M6().observeForever(backgroundObserver)
        released = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == oldw && h == oldh) return
        post { layoutSource(w) }
    }

    private fun layoutSource(screenWidth: Int) {
        if (screenWidth <= 0) return
        val displayWidth = resources.displayMetrics.widthPixels
        val adjustedWidth = (displayWidth * SOURCE_WIDTH_PERCENT).roundToInt()
        val backgroundHeight = adjustedWidth * 2
        header.layoutParams = LayoutParams(displayWidth, backgroundHeight)
        val membershipParams = membershipEntry.layoutParams as LayoutParams
        membershipEntry.layoutParams = membershipParams.apply {
            width = dp(116f)
            height = dp(36f)
            gravity = Gravity.TOP or Gravity.END
            topMargin = (backgroundHeight * 0.401f).roundToInt() + dp(142f)
            rightMargin = dp(18f)
        }

        val cardView = header.findViewById<View>(SOURCE_ID_PROFILE_CARD)
        cardView.setBackgroundResource(0x7f080b64)
        cardView.layoutParams = cardView.layoutParams.apply {
            height = (backgroundHeight * SOURCE_CARD_HEIGHT_PERCENT).roundToInt()
        }
    }

    fun bind(user: UserInfo) {
        if (released) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            attachBackgroundObserver()
        }
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
        val signature = user.signature?.takeIf(String::isNotBlank)
        val summary = user.profileSummary().takeIf(String::isNotBlank)
        header.findViewById<TextView>(SOURCE_ID_PROFILE_SUMMARY).apply {
            setSingleLine(false)
            setMaxLines(2)
            text = listOfNotNull(summary, signature).joinToString("\n")
            setTextColor(Color.WHITE)
        }

        avatar.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f * resources.displayMetrics.density
                setColor(SOURCE_HEADER_FALLBACK_COLOR)
            }
            setImageResource(0x7f0808db)
            user.avatarUrl?.takeIf(String::isNotBlank)?.let { url ->
                load(absoluteAvatarUrl(url)) {
                    listener(
                        onError = { _, _ ->
                            setImageResource(0x7f0808db)
                        },
                    )
                }
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
            val iconPadding = (6f * density).roundToInt()
            val iconSize = (24f * density).roundToInt()
            layoutParams = layoutParams.apply {
                width = (36f * density).roundToInt()
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

        val introBio = user.bio?.takeIf(String::isNotBlank)
        header.findViewById<TextView>(SOURCE_ID_PERSONAL_INTRO).apply {
            text = introBio ?: "点击填写介绍，让大家认识你吧～"
            setTextColor(if (introBio != null) Color.WHITE else SOURCE_SECONDARY_TEXT_COLOR)
            setOnClickListener { onEdit() }
        }

        header.findViewById<ImageView>(SOURCE_ID_MORE_INFO).apply {
            setColorFilter(Color.WHITE)
            setOnClickListener { onEdit() }
        }
        applySourceDarkPalette()
    }

    private fun bindBackground(user: UserInfo) {
        val url = user.backgroundUrl?.takeIf(String::isNotBlank)
        if (url == null) {
            rawBackground.visibility = View.GONE
            combinedBackground.setImageResource(0x7f080b6b)
            combinedBackground.visibility = View.VISIBLE
            defaultBackground.visibility = View.GONE
            header.setBackgroundColor(SOURCE_HEADER_FALLBACK_COLOR)
            return
        }

        header.setBackgroundColor(SOURCE_HEADER_FALLBACK_COLOR)
        rawBackground.visibility = View.VISIBLE
        combinedBackground.setImageResource(0x7f080b6b)
        combinedBackground.visibility = View.VISIBLE
        defaultBackground.visibility = View.GONE
        rawBackground.load(absoluteAvatarUrl(url)) {
            size(Size.ORIGINAL)
            listener(
                onSuccess = { _, result ->
                    val bitmap = result.image.toBitmap()
                    combinedBackground.setImageBitmap(bitmap)
                    combinedBackground.visibility = View.VISIBLE
                    rawBackground.visibility = View.GONE
                    defaultBackground.visibility = View.GONE
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
        if (released) return
        backgroundComposer.M6().removeObserver(backgroundObserver)
        scope.cancel()
        lastBoundUser = null
        released = true
    }

    private companion object {
        const val SOURCE_WIDTH_PERCENT = 0.975f
        const val SOURCE_CARD_HEIGHT_PERCENT = 0.35f
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
        const val SOURCE_ID_PERSONAL_INFO = 0x7f095b60
        const val SOURCE_ID_PERSONAL_INTRO = 0x7f095c16
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}

/**
 * 资料卡摘要行：性别 + 星座 + 地区。
 *
 * 三个字段都受「保密」（Android 旧文案叫「不展示」）控制，取值一律读服务端下发的字段：
 * - 性别：`gender == "secret"`，与 Web 端同一个编码
 * - 生日：`hide_birthday` / 地区：`hide_region`，与 Web 端共用同一后端字段
 *   （null 表示服务端没下发该字段，按可见处理；只有明确 true 才隐藏）
 *
 * 保密中的字段**整段不出现在摘要行里**，不显示「保密」字样：
 * 摘要行是卡片上给人看的一行字，没有字段标签，写「保密」既不美观也说不清是哪一项。
 * 编辑资料页保留文字是因为那里是带标签的列表行。
 * 星座由生日推导而来，所以生日保密时星座一并隐藏。
 */
private fun UserInfo.profileSummary(): String = listOfNotNull(
    if (isGenderSecret()) null else genderLabel(),
    if (hideBirthday == true) null else constellationLabel(),
    if (hideRegion == true) null else location?.takeIf(String::isNotBlank),
).joinToString(" ")

/** 性别文案走共享实现（`util/GenderLabels.kt`），不在这里再维护一份映射。 */
/** 星座沿用真本迁移来的 ConstellationUtils（0 基月份），不在这里再维护一份边界日表。 */
private fun UserInfo.constellationLabel(): String? = birthday?.let { value ->
    runCatching {
        LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrNull()
}
    ?.let { date -> ConstellationUtils.calculate(date.monthValue - 1, date.dayOfMonth) }
    ?.takeIf(String::isNotBlank)
