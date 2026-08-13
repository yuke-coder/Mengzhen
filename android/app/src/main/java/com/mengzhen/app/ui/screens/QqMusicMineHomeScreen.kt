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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.mengzhen.app.R as AppR
import com.mengzhen.app.data.model.UserInfo
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.components.main.absoluteAvatarUrl
import com.mengzhen.app.ui.components.rememberQqMusicImagePicker
import com.mengzhen.app.ui.navigation.Screen
import com.tencent.component.widget.AsyncEffectImageView
import com.tencent.component.widget.AsyncImageView
import com.tencent.component.widget.b
import com.tencent.qqmusic.R as QqR
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
    val sourceView = remember(context) { QqMusicSourceProfileView(context) }
    sourceView.onEdit = { navController.navigate(Screen.ProfileEdit.route) }
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
}

/**
 * Minimal host for QQ Music 20.7.5.3's original personal-home header resources.
 * The bridge only supplies Mengzhen identity data, background selection and host navigation.
 */
private class QqMusicSourceProfileView(context: Context) : FrameLayout(context) {
    var onEdit: () -> Unit = {}
    var onSettings: () -> Unit = {}
    var onBackground: () -> Unit = {}

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val backgroundComposer = SourceHeaderBackground()
    private val header = LayoutInflater.from(context).inflate(QqR.layout.a6c, this, false)
    private val rawBackground = header.findViewById<AsyncEffectImageView>(QqR.id.dbt)
    private val combinedBackground = header.findViewById<AsyncEffectImageView>(QqR.id.dbr)
    private val defaultBackground = header.findViewById<AsyncEffectImageView>(QqR.id.blu)
    private val avatar = header.findViewById<AsyncEffectImageView>(QqR.id.hdt)
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

        header.findViewById<View>(QqR.id.my0).let { card ->
            card.layoutParams = card.layoutParams.apply {
                height = (backgroundHeight * SOURCE_CARD_HEIGHT_PERCENT).roundToInt()
            }
            card.setBackgroundResource(QqR.drawable.homepage_card_bg_dark)
        }

    }

    fun bind(user: UserInfo) {
        if (lastBoundUser == user) return
        lastBoundUser = user
        bindHeader(user)
        bindBackground(user)
    }

    private fun bindHeader(user: UserInfo) {
        header.findViewById<TextView>(QqR.id.hbu).text = user.username
        header.findViewById<TextView>(QqR.id.kzb).text = user.profileSummary()

        avatar.apply {
            setRadius(32f * resources.displayMetrics.density)
            Glide.with(this).clear(this)
            setImageDrawable(null)
            user.avatarUrl?.takeIf(String::isNotBlank)?.let { url ->
                Glide.with(this)
                    .load(absoluteAvatarUrl(url))
                    .circleCrop()
                    .into(this)
            }
            setOnClickListener { onEdit() }
        }
        header.findViewById<ImageView>(QqR.id.mzd).visibility = View.GONE
        header.findViewById<View>(QqR.id.cq_).visibility = View.GONE

        header.findViewById<Button>(QqR.id.j8e).apply {
            visibility = View.VISIBLE
            text = "编辑资料"
            setOnClickListener { onEdit() }
        }
        header.findViewById<Button>(QqR.id.j8r).apply {
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

        header.findViewById<View>(QqR.id.mxz).visibility = View.INVISIBLE
        header.findViewById<TextView>(QqR.id.cr3).text = "0"
        header.findViewById<TextView>(QqR.id.cct).text = "0"
        header.findViewById<TextView>(QqR.id.nkv).apply {
            visibility = View.VISIBLE
            text = "0"
        }
        header.findViewById<View>(QqR.id.nkt).visibility = View.GONE
        header.findViewById<View>(QqR.id.nku).visibility = View.GONE
        header.findViewById<View>(QqR.id.hol).visibility = View.VISIBLE
        header.findViewById<TextView>(QqR.id.hom).text = "0"
        applySourceDarkPalette()
    }

    private fun bindBackground(user: UserInfo) {
        val url = user.backgroundUrl?.takeIf(String::isNotBlank)
        if (url == null) {
            rawBackground.setAsyncImageListener(null)
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
        rawBackground.setAsyncImageListener(object : b.a {
            override fun onImageStarted(view: b) = Unit
            override fun onImageProgress(view: b, progress: Float) = Unit
            override fun onImageFailed(view: b) = Unit

            override fun onImageLoaded(view: b) {
                val drawable = (view as? AsyncImageView)?.drawable ?: return
                val bitmap = com.tencent.component.widget.e.a(drawable) ?: return
                scope.launch(Dispatchers.Default) {
                    backgroundComposer.L6(bitmap)
                }
            }
        })
        rawBackground.m(url)
    }

    private fun installSourceActions() {
        header.findViewById<View>(QqR.id.db7).setOnClickListener { onBackground() }
    }

    private fun applySourceDarkPalette() {
        header.findViewById<TextView>(QqR.id.hbu).setTextColor(SOURCE_NAME_COLOR)
        header.findViewById<TextView>(QqR.id.kzb).setTextColor(Color.WHITE)
        intArrayOf(QqR.id.cq7, QqR.id.cc0, QqR.id.nks, QqR.id.hoj).forEach { id ->
            header.findViewById<TextView>(id).setTextColor(SOURCE_SECONDARY_TEXT_COLOR)
        }
        intArrayOf(QqR.id.cr3, QqR.id.cct, QqR.id.nkv, QqR.id.hom).forEach { id ->
            header.findViewById<TextView>(id).setTextColor(Color.WHITE)
        }
        header.findViewById<Button>(QqR.id.j8e).setTextColor(Color.WHITE)
        header.findViewById<Button>(QqR.id.j8r).setTextColor(Color.WHITE)
    }

    fun release() {
        Glide.with(avatar).clear(avatar)
        rawBackground.setAsyncImageListener(null)
        backgroundComposer.M6().removeObserver(backgroundObserver)
        scope.cancel()
    }

    private companion object {
        const val SOURCE_WIDTH_PERCENT = 0.975f
        const val SOURCE_CARD_HEIGHT_PERCENT = 0.23f
        const val SOURCE_HEADER_FALLBACK_COLOR = 0xFF1A1A1A.toInt()
        const val SOURCE_NAME_COLOR = 0xFFD7B45D.toInt()
        const val SOURCE_SECONDARY_TEXT_COLOR = 0xB3FFFFFF.toInt()
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
