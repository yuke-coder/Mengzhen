package com.mengzhen.app.ui.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.airbnb.lottie.LottieAnimationView
import com.lyricengine.ui.SingleLyricView
import com.mengzhen.app.R
import com.mengzhen.app.audio.healing.QqMusicHealingScene
import com.tencent.image.algorithms.a as QqBitmapAlgorithms
import com.tencent.qqmusic.business.playernew.fxeffect.o as QqMagicColor
import com.tencent.qqmusic.business.playernew.fxeffect.custom.BaseCustomLightEffectView
import com.tencent.qqmusic.business.playernew.fxeffect.custom.CustomLightEffectView
import com.tencent.qqmusic.business.playernew.fxeffect.custom.CustomLightEffectView4Texture
import com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b as QqLightEffectShader
import com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.e as QqGradientLightEffectShader
import com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.f as QqSpectrumLightEffectShader
import com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.QqForegroundTekEffectView
import com.tencent.qqmusic.business.playernew.view.playersong.ih
import com.tencent.qqmusic.ui.PlayerDiscAlbumPoleView
import com.tencent.qqmusic.ui.ViewPagerCircleIndicator
import com.tencent.qqmusicplayerprocess.audio.playlist.y
import com.mengzhen.app.audio.QqMusicPlaybackMode
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * QQ 音乐 20.6.5.8 当前普通播放器的业务适配入口。
 *
 * 根背景、simple vinyl 唱机、进度区与控制区分别对应 PlayerRootViewDelegate、
 * PlayerSimpleDiscAlbumView、c91 与 c6c。这里只连接本应用已有的播放能力。
 */
@Composable
internal fun QqMusicSourcePlayerChrome(
    modifier: Modifier,
    artwork: Bitmap?,
    themeColor: Int,
    title: String,
    artist: String,
    state: String,
    positionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    playbackMode: QqMusicPlaybackMode,
    liked: Boolean,
    healingScene: QqMusicHealingScene?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onShowPlaylist: () -> Unit,
    onShowTimer: () -> Unit,
    onDownload: () -> Unit,
    onShowSpeed: () -> Unit,
    onToggleLike: () -> Unit,
    onCyclePlaybackMode: () -> QqMusicPlaybackMode,
    onSetPlaybackMode: (QqMusicPlaybackMode) -> Unit,
    onMore: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val activeRoot = remember { arrayOfNulls<QqPlayerSourceRoot>(1) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { activeRoot[0]?.applyPickedStyleImage(it) }
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            QqPlayerSourceBinding.create(context).also { binding ->
                binding.installListeners()
                binding.root.requestCustomStyleImage = {
                    imagePicker.launch(arrayOf("image/*"))
                }
                activeRoot[0] = binding.root
            }.root
        },
        update = { root ->
            activeRoot[0] = root
            val binding = root.tag as QqPlayerSourceBinding
            binding.onBack = onBack
            binding.onShare = onShare
            binding.onShowPlaylist = onShowPlaylist
            binding.onShowTimer = onShowTimer
            binding.onDownload = onDownload
            binding.onShowSpeed = onShowSpeed
            binding.onToggleLike = onToggleLike
            binding.onCyclePlaybackMode = onCyclePlaybackMode
            binding.onSetPlaybackMode = onSetPlaybackMode
            binding.onMore = onMore
            binding.onPlayPause = onPlayPause
            binding.onPrevious = onPrevious
            binding.onNext = onNext
            binding.onSeek = onSeek
            binding.bind(
                artwork = artwork,
                themeColor = themeColor,
                title = title,
                artist = artist,
                state = state,
                positionMs = positionMs,
                durationMs = durationMs,
                playbackSpeed = playbackSpeed,
                playbackMode = playbackMode,
                liked = liked,
                healingScene = healingScene,
            )
        },
    )
}

private class QqPlayerSourceBinding private constructor(
    val root: QqPlayerSourceRoot,
) {
    var onBack: () -> Unit = {}
    var onShare: () -> Unit = {}
    var onShowPlaylist: () -> Unit = {}
    var onShowTimer: () -> Unit = {}
    var onDownload: () -> Unit = {}
    var onShowSpeed: () -> Unit = {}
    var onToggleLike: () -> Unit = {}
    var onCyclePlaybackMode: () -> QqMusicPlaybackMode = {
        QqMusicPlaybackMode.LIST_REPEAT
    }
    var onSetPlaybackMode: (QqMusicPlaybackMode) -> Unit = {}
    var onMore: () -> Unit = {}
    var onPlayPause: () -> Unit = {}
    var onPrevious: () -> Unit = {}
    var onNext: () -> Unit = {}
    var onSeek: (Long) -> Unit = {}

    fun installListeners() {
        root.backButton.setOnClickListener { onBack() }
        root.shareButton.setOnClickListener { onShare() }
        root.styleButton.setOnClickListener { root.showStyleEditor() }
        root.songPage.apply {
            onShowLyrics = { root.showLyrics() }
            onDownload = { this@QqPlayerSourceBinding.onDownload() }
            onShowSpeed = { this@QqPlayerSourceBinding.onShowSpeed() }
            onShowTimer = { this@QqPlayerSourceBinding.onShowTimer() }
            onToggleLike = { this@QqPlayerSourceBinding.onToggleLike() }
            onMore = { this@QqPlayerSourceBinding.onMore() }
            onShowPlaylist = { this@QqPlayerSourceBinding.onShowPlaylist() }
            onCyclePlaybackMode = {
                this@QqPlayerSourceBinding.onCyclePlaybackMode()
            }
            onSetPlaybackMode = {
                this@QqPlayerSourceBinding.onSetPlaybackMode(it)
            }
            onPlayPause = { this@QqPlayerSourceBinding.onPlayPause() }
            onPrevious = { this@QqPlayerSourceBinding.onPrevious() }
            onNext = { this@QqPlayerSourceBinding.onNext() }
            onSeek = { this@QqPlayerSourceBinding.onSeek(it) }
        }
        root.lyricPage.onPlayPause = { onPlayPause() }
    }

    fun bind(
        artwork: Bitmap?,
        themeColor: Int,
        title: String,
        artist: String,
        state: String,
        positionMs: Long,
        durationMs: Long,
        playbackSpeed: Float,
        playbackMode: QqMusicPlaybackMode,
        liked: Boolean,
        healingScene: QqMusicHealingScene?,
    ) {
        val isPlaying = state == "PLAYING"
        root.bindSourceAppearance(
            artwork = artwork,
            color = themeColor,
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            healingScene = healingScene,
        )
        root.songPage.bind(
            artwork = artwork,
            themeColor = themeColor,
            title = title,
            artist = artist,
            state = state,
            positionMs = positionMs,
            durationMs = durationMs,
            playbackSpeed = playbackSpeed,
            playbackMode = playbackMode,
            liked = liked,
            style = root.playerStyle,
        )
        root.lyricPage.bind(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            positionMs = positionMs,
            style = root.playerStyle,
        )
    }

    companion object {
        fun create(context: Context): QqPlayerSourceBinding {
            val root = QqPlayerSourceRoot(context)
            return QqPlayerSourceBinding(root).also { root.tag = it }
        }
    }
}

/** PlayerRootViewDelegate: dynamic effect, foreground texture, pager and top controls. */
private class QqPlayerSourceRoot(context: Context) : FrameLayout(context) {
    private val healingBackground = QqMusicHealingVideoBackgroundView(context)
    private val dynamicBackground = QqDynamicDiffuseBackgroundView(context, useTexture = true)
    private val foregroundTexture = ImageView(context).apply {
        setImageResource(R.drawable.player_realism_fore_background)
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = .2f
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    val songPage = QqPlayerSongPage(context)
    val lyricPage = QqPlayerLyricPage(context)
    private val pager = ViewPager(context).apply {
        id = View.generateViewId()
        clipChildren = false
        clipToPadding = false
        overScrollMode = OVER_SCROLL_NEVER
        adapter = QqSourcePagerAdapter(songPage, lyricPage)
    }
    private val topBar = LayoutInflater.from(context)
        .inflate(R.layout.qq_source_player_top_bar, this, false) as FrameLayout
    val backButton = topBar.findViewById<ImageView>(R.id.qq_source_top_back)
    val styleButton = topBar.findViewById<ImageView>(R.id.qq_source_top_style)
    val shareButton = topBar.findViewById<ImageView>(R.id.qq_source_top_share)
    private val pageIndicator = topBar
        .findViewById<ViewPagerCircleIndicator>(R.id.qq_source_top_indicator)
        .apply {
        setCount(2)
        setViewPager(pager)
    }
    var requestCustomStyleImage: (QqCustomImageTarget) -> Unit = {}
    var playerStyle: QqPlayerStyleSettings = QqPlayerStyleStore.load(context)
        private set
    private var currentArtwork: Bitmap? = null
    private var currentThemeColor = DEFAULT_SOURCE_COLOR
    private var currentTitle = ""
    private var currentArtist = ""
    private var currentPlaying = false
    private var currentHealingScene: QqMusicHealingScene? = null
    private var pendingImageTarget: QqCustomImageTarget? = null
    private var editorVisible = false
    private val editor by lazy(LazyThreadSafetyMode.NONE) {
        QqPlayerStyleEditorView(
            context = context,
            onDismiss = ::hideStyleEditor,
            onUse = ::usePlayerStyle,
            onRequestImage = { target ->
                pendingImageTarget = target
                requestCustomStyleImage(target)
            },
        ).also { styleEditor ->
            styleEditor.visibility = GONE
            addView(styleEditor, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
    }
    private val editorBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            hideStyleEditor()
        }
    }

    init {
        clipChildren = false
        clipToPadding = false
        addView(healingBackground, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(dynamicBackground, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(foregroundTexture, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(pager, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(topBar, LayoutParams(MATCH_PARENT, 0))
        backButton.setColorFilter(TOP_ICON_COLOR, PorterDuff.Mode.SRC_IN)
        styleButton.setColorFilter(TOP_ICON_COLOR, PorterDuff.Mode.SRC_IN)
        shareButton.setColorFilter(TOP_ICON_COLOR, PorterDuff.Mode.SRC_IN)
        (context as? ComponentActivity)?.onBackPressedDispatcher?.addCallback(
            editorBackCallback,
        )
    }

    fun bindSourceAppearance(
        artwork: Bitmap?,
        color: Int,
        title: String,
        artist: String,
        isPlaying: Boolean,
        healingScene: QqMusicHealingScene?,
    ) {
        currentArtwork = artwork
        currentThemeColor = color
        currentTitle = title
        currentArtist = artist
        currentPlaying = isPlaying
        currentHealingScene = healingScene
        healingBackground.bind(healingScene)
        dynamicBackground.visibility = if (healingScene == null) VISIBLE else GONE
        foregroundTexture.visibility = if (healingScene == null) VISIBLE else GONE
        styleButton.visibility = if (healingScene == null) VISIBLE else GONE
        if (healingScene == null) {
            dynamicBackground.bind(artwork, color, playerStyle)
        }
        applyChromeStyle(playerStyle)
        if (editorVisible) {
            editor.bind(playerStyle, artwork, color, title, artist, isPlaying)
        }
    }

    fun showStyleEditor() {
        editor.bind(
            current = playerStyle,
            artwork = currentArtwork,
            themeColor = currentThemeColor,
            title = currentTitle,
            artist = currentArtist,
            isPlaying = currentPlaying,
        )
        editor.visibility = VISIBLE
        editor.bringToFront()
        editorVisible = true
        editorBackCallback.isEnabled = true
    }

    fun applyPickedStyleImage(uri: Uri) {
        val target = pendingImageTarget ?: return
        pendingImageTarget = null
        QqPlayerStyleStore.importImage(context, target, uri)?.let {
            editor.applyImportedImage(target, it)
        }
    }

    private fun hideStyleEditor() {
        if (!editorVisible) return
        editor.visibility = GONE
        editorVisible = false
        pendingImageTarget = null
        editorBackCallback.isEnabled = false
    }

    private fun usePlayerStyle(value: QqPlayerStyleSettings) {
        playerStyle = value.copy(enabled = true)
        QqPlayerStyleStore.save(context, playerStyle)
        if (currentHealingScene == null) {
            dynamicBackground.bind(currentArtwork, currentThemeColor, playerStyle)
        }
        songPage.applyPlayerStyle(playerStyle)
        lyricPage.applyPlayerStyle(playerStyle)
        applyChromeStyle(playerStyle)
        hideStyleEditor()
    }

    private fun applyChromeStyle(style: QqPlayerStyleSettings) {
        val chosen = qqStyleColor(
            context,
            "textAndButtonColor",
            style.textAndButtonColorId,
            TOP_ICON_COLOR,
        )
        val amount = if (style.enabled) style.textAndButtonAlpha.coerceIn(0f, 1f) * .55f else 0f
        val tint = ColorUtils.blendARGB(TOP_ICON_COLOR, chosen, amount)
        listOf(backButton, styleButton, shareButton).forEach {
            it.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        }
    }

    fun showLyrics() {
        pager.setCurrentItem(1, true)
    }

    override fun onDetachedFromWindow() {
        editorBackCallback.remove()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return
        val sourceTopMargin = dp(6)
        val buttonSize = (w * .1f).roundToInt()
        val topHeight = sourceTopMargin + buttonSize + dp(4)
        topBar.layoutParams = (topBar.layoutParams as LayoutParams).apply {
            width = MATCH_PARENT
            height = topHeight
        }
        pager.layoutParams = (pager.layoutParams as LayoutParams).apply {
            width = MATCH_PARENT
            height = MATCH_PARENT
            topMargin = topHeight
        }
        val side = (w * .064f).roundToInt()
        backButton.layoutParams = LayoutParams(buttonSize, buttonSize).apply {
            leftMargin = side
            topMargin = sourceTopMargin
        }
        shareButton.layoutParams = LayoutParams(buttonSize, buttonSize).apply {
            gravity = Gravity.END
            rightMargin = side
            topMargin = sourceTopMargin
        }
        styleButton.layoutParams = LayoutParams(buttonSize, buttonSize).apply {
            gravity = Gravity.END
            rightMargin = side + buttonSize + dp(5)
            topMargin = sourceTopMargin
        }
        pageIndicator.layoutParams = LayoutParams(dp(66), buttonSize).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = sourceTopMargin
        }
        topBar.requestLayout()
        pager.requestLayout()
    }
}

/** QQ 音乐 20.6.5.8 CustomLightEffectView 宿主。 */
internal class QqDynamicDiffuseBackgroundView(
    context: Context,
    private val useTexture: Boolean = false,
) : FrameLayout(context) {
    private val styleBackground = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val styleForeground = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val foregroundEffect = QqForegroundTekEffectView(context)
    private var artworkToken: Bitmap? = null
    private var loadedBackground: Bitmap? = null
    private var styleToken = ""
    private var magicColors = QqMagicColor.a.i()
    private var appliedMagicColors: Pair<Int, Int>? = null
    private var effectType = ""
    private var effectShader: QqLightEffectShader? = null
    private var effectView: BaseCustomLightEffectView? = null

    init {
        addView(styleBackground, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(styleForeground, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(foregroundEffect, LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    fun bind(
        artwork: Bitmap?,
        color: Int,
        style: QqPlayerStyleSettings = QqPlayerStyleSettings(),
    ) {
        if (artworkToken !== artwork) {
            artworkToken = artwork
            magicColors = QqMagicColor.a.p(artwork)
        }
        val nextStyleToken = listOf(
            style.enabled,
            style.backgroundId,
            style.customBackgroundPath,
            style.backgroundBlur,
        ).joinToString("|")
        if (styleToken != nextStyleToken) {
            styleToken = nextStyleToken
            loadStyleImages(style)
        }
        styleBackground.alpha = if (style.enabled) {
            style.backgroundAlpha.coerceIn(0f, 1f)
        } else {
            0f
        }
        val baseColor = if (style.enabled) {
            qqStyleColor(context, "background", style.backgroundId, color)
        } else {
            ROOT_DARK_COLOR
        }
        if (style.enabled) {
            setBackgroundColor(baseColor)
        } else {
            background = QqMagicColor.a.t(magicColors)
        }
        val nextEffect = if (style.enabled) {
            style.backgroundLightEffectId
        } else {
            "gradient_light_effect"
        }
        if (effectType != nextEffect) replaceEffect(nextEffect)
        effectView?.apply {
            if (appliedMagicColors != magicColors) {
                applyMagicColors(this, magicColors)
                appliedMagicColors = magicColors
            }
            setFillBgColor(baseColor, true)
        }
        foregroundEffect.bind(if (style.enabled) style.foregroundLightEffectId else "none")
    }

    private fun replaceEffect(type: String) {
        effectView?.let { current ->
            current.d(true)
            current.b()
        }
        effectView = null
        effectShader = null
        effectType = type
        val raw = when (type) {
            "gradient_light_effect" -> R.raw.qq_music_gradient_light_effect
            "spectrum_bar" -> R.raw.qq_music_spectrum_bar
            "spectrum_peak" -> R.raw.qq_music_spectrum_peak
            else -> return
        }
        val source = resources.openRawResource(raw).bufferedReader().use { it.readText() }
        val shader = when (type) {
            "gradient_light_effect" -> QqGradientLightEffectShader(source).apply {
                U(4f)
                T(0f)
                E(if (height > 0) width.toFloat() / height else 1f)
                R(0f)
                P(1f)
                S(0f)
                Q(1f)
            }
            else -> QqSpectrumLightEffectShader(source, true).apply { E(3f) }
        }
        val view: BaseCustomLightEffectView = if (useTexture) {
            CustomLightEffectView4Texture(context)
        } else {
            CustomLightEffectView(context)
        }.apply {
            setEffectShader(shader)
        }
        applyMagicColors(view, magicColors)
        appliedMagicColors = magicColors
        effectShader = shader
        effectView = view
        addView(view, 0)
        updateEffectLayout()
        if (isAttachedToWindow) view.post { if (view.isAttachedToWindow) view.c() }
    }

    private fun applyMagicColors(
        view: BaseCustomLightEffectView,
        colors: Pair<Int, Int>,
    ) {
        when (view) {
            is CustomLightEffectView -> view.g(colors)
            is CustomLightEffectView4Texture -> view.g(colors)
        }
    }

    private fun updateEffectLayout() {
        val view = effectView ?: return
        val spectrum = effectType == "spectrum_bar" || effectType == "spectrum_peak"
        view.layoutParams = LayoutParams(
            MATCH_PARENT,
            if (spectrum && width > 0) (width * 130f / 390f).roundToInt() else MATCH_PARENT,
        ).apply { gravity = Gravity.BOTTOM }
        (effectShader as? QqGradientLightEffectShader)?.E(
            if (height > 0) width.toFloat() / height else 1f,
        )
    }

    private fun loadStyleImages(style: QqPlayerStyleSettings) {
        styleBackground.setImageDrawable(null)
        styleForeground.setImageDrawable(null)
        loadedBackground?.takeUnless(Bitmap::isRecycled)?.recycle()
        loadedBackground = null
        if (!style.enabled) {
            styleBackground.visibility = GONE
            styleForeground.visibility = GONE
            return
        }
        if (
            style.backgroundId == "customBackground" &&
            style.customBackgroundPath.isNotBlank()
        ) {
            val source = BitmapFactory.decodeFile(style.customBackgroundPath)
            val blur = style.backgroundBlur.coerceIn(0f, 1f)
            loadedBackground = source?.let { bitmap ->
                if (blur > 0.06f) {
                    val blurred: Bitmap = QqBitmapAlgorithms.d(
                        context,
                        bitmap,
                        (blur * 25f).coerceIn(0.01f, 25f),
                        3f,
                    )
                    if (blurred !== bitmap && !bitmap.isRecycled) bitmap.recycle()
                    blurred
                } else {
                    bitmap
                }
            }
            styleBackground.setImageBitmap(loadedBackground)
        } else if (style.backgroundId.startsWith("customPreset")) {
            val background = qqStyleDrawable(
                context,
                "background_${style.backgroundId}_background.png",
            )
            if (background != 0) styleBackground.setImageResource(background)
        }
        styleBackground.visibility = if (styleBackground.drawable != null) VISIBLE else GONE
        if (style.backgroundId.startsWith("customPreset")) {
            val foreground = qqStyleDrawable(
                context,
                "background_${style.backgroundId}_foreground.png",
            )
            if (foreground != 0) styleForeground.setImageResource(foreground)
        }
        styleForeground.visibility = if (styleForeground.drawable != null) VISIBLE else GONE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateEffectLayout()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        effectView?.c()
    }

    override fun onDetachedFromWindow() {
        effectView?.d(true)
        loadedBackground?.takeUnless(Bitmap::isRecycled)?.recycle()
        loadedBackground = null
        artworkToken = null
        super.onDetachedFromWindow()
    }
}

private class QqSourcePagerAdapter(
    private val songPage: View,
    private val lyricPage: View,
) : PagerAdapter() {
    override fun getCount(): Int = 2

    override fun isViewFromObject(view: View, item: Any): Boolean = view === item

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val page = if (position == 0) songPage else lyricPage
        (page.parent as? ViewGroup)?.removeView(page)
        container.addView(page, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return page
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        container.removeView(item as View)
    }
}

/** Normal-song page using the source middle2 proportions. */
private class QqPlayerSongPage(context: Context) : ViewGroup(context) {
    private val cover = QqVinylHostView(context)
    private val titleView = sourceText(context, 24f, true, Color.WHITE).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL
    }
    private val dislikeButton = sourceImageButton(
        context,
        R.drawable.player_btn_radio_not_like_disable,
        "不喜欢",
    ).apply {
        isClickable = false
        isFocusable = false
        setColorFilter(ACTION_ICON_COLOR, PorterDuff.Mode.SRC_IN)
    }
    private val favoriteButton = sourceImageButton(
        context,
        R.drawable.player_btn_favorite_normal,
        "收藏",
    )
    private val metaRow = QqSourceMetaRow(context)
    private val lyricPreview = sourceText(context, 16f, false, 0xFFABABAE.toInt()).apply {
        text = "纯音乐，请欣赏"
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL
    }
    private val actionRow = QqSourceActionRow(context)
    private val seek = QqSourceSeek(context)
    private val controls = QqSourceControls(context)
    private var currentArtwork: Bitmap? = null
    private var currentThemeColor = DEFAULT_SOURCE_COLOR
    private var currentPlaying = false
    private var currentStyle = QqPlayerStyleSettings()

    var onShowLyrics: () -> Unit = {}
    var onDownload: () -> Unit = {}
    var onShowSpeed: () -> Unit = {}
    var onShowTimer: () -> Unit = {}
    var onToggleLike: () -> Unit = {}
    var onMore: () -> Unit = {}
    var onShowPlaylist: () -> Unit = {}
    var onCyclePlaybackMode: () -> QqMusicPlaybackMode = {
        QqMusicPlaybackMode.LIST_REPEAT
    }
    var onSetPlaybackMode: (QqMusicPlaybackMode) -> Unit = {}
    var onPlayPause: () -> Unit = {}
    var onPrevious: () -> Unit = {}
    var onNext: () -> Unit = {}
    var onSeek: (Long) -> Unit = {}

    init {
        clipChildren = false
        clipToPadding = false
        addView(cover)
        addView(titleView)
        addView(dislikeButton)
        addView(favoriteButton)
        addView(metaRow)
        addView(lyricPreview)
        addView(actionRow)
        addView(seek)
        addView(controls)

        favoriteButton.setOnClickListener { onToggleLike() }
        actionRow.onShowLyrics = { onShowLyrics() }
        actionRow.onDownload = { onDownload() }
        actionRow.onShowSpeed = { onShowSpeed() }
        actionRow.onShowTimer = { onShowTimer() }
        actionRow.onMore = { onMore() }
        seek.onSeek = { onSeek(it) }
        controls.onShowPlaylist = { onShowPlaylist() }
        controls.onCyclePlaybackMode = { onCyclePlaybackMode() }
        controls.onSetPlaybackMode = { onSetPlaybackMode(it) }
        controls.onPlayPause = { onPlayPause() }
        controls.onPrevious = { onPrevious() }
        controls.onNext = { onNext() }
    }

    fun bind(
        artwork: Bitmap?,
        themeColor: Int,
        title: String,
        artist: String,
        state: String,
        positionMs: Long,
        durationMs: Long,
        playbackSpeed: Float,
        playbackMode: QqMusicPlaybackMode,
        liked: Boolean,
        style: QqPlayerStyleSettings,
    ) {
        currentArtwork = artwork
        currentThemeColor = themeColor
        currentPlaying = state == "PLAYING"
        currentStyle = style
        cover.bind(artwork, themeColor, currentPlaying, style)
        titleView.text = title
        metaRow.bind(artist.ifBlank { "本地音频" })
        favoriteButton.setImageResource(
            if (liked) R.drawable.player_btn_favorited_normal
            else R.drawable.player_btn_favorite_normal,
        )
        favoriteButton.setColorFilter(
            if (liked) SOURCE_ACCENT_COLOR else ACTION_ICON_COLOR,
            PorterDuff.Mode.SRC_IN,
        )
        favoriteButton.contentDescription = if (liked) "取消收藏" else "收藏"
        actionRow.setSpeed(playbackSpeed)
        seek.bind(positionMs, durationMs)
        controls.bind(state, playbackMode)
        applyPlayerStyle(style)
    }

    fun applyPlayerStyle(style: QqPlayerStyleSettings) {
        currentStyle = style
        cover.bind(currentArtwork, currentThemeColor, currentPlaying, style)
        val chosen = qqStyleColor(
            context,
            "textAndButtonColor",
            style.textAndButtonColorId,
            Color.WHITE,
        )
        val amount = if (style.enabled) {
            (.25f + style.textAndButtonAlpha.coerceIn(0f, 1f) * .35f)
        } else {
            0f
        }
        val textColor = ColorUtils.blendARGB(Color.WHITE, chosen, amount)
        val secondary = ColorUtils.setAlphaComponent(textColor, 178)
        titleView.setTextColor(textColor)
        lyricPreview.setTextColor(secondary)
        dislikeButton.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
        favoriteButton.setColorFilter(
            if (favoriteButton.contentDescription == "取消收藏") SOURCE_ACCENT_COLOR
            else textColor,
            PorterDuff.Mode.SRC_IN,
        )
        metaRow.applyStyle(textColor)
        actionRow.applyStyle(textColor)
        seek.applyStyle(textColor)
        controls.applyStyle(textColor)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val scale = sourceScale(width)
        val side = (width * .064f).roundToInt()
        val buttonSize = sdp(40, scale)
        val controlsHeight = sdp(60, scale)
        val seekHeight = sdp(30, scale)
        val actionHeight = sdp(44, scale)

        val coverWidth = (width * .846f).roundToInt()
        val sourceCoverHeight = (coverWidth * 1.0788f).roundToInt()
        val bottomArea = controlsHeight + seekHeight + actionHeight + sdp(61, scale)
        val infoArea = sdp(105, scale)
        val coverHeight = min(
            sourceCoverHeight,
            (height - bottomArea - infoArea).coerceAtLeast(coverWidth * 3 / 4),
        )
        cover.measure(exact(coverWidth), exact(coverHeight))

        dislikeButton.measure(exact(buttonSize), exact(buttonSize))
        favoriteButton.measure(exact(buttonSize), exact(buttonSize))
        val titleWidth = width - side * 2 - buttonSize * 2 - dp(16)
        titleView.measure(atMost(titleWidth.coerceAtLeast(0)), unspecified())
        metaRow.measure(exact(width - side * 2), exact(sdp(29, scale)))
        lyricPreview.measure(exact(width - side * 2), exact(sdp(28, scale)))
        actionRow.measure(exact(width), exact(actionHeight))
        seek.measure(exact(width), exact(seekHeight))
        controls.measure(exact(width), exact(controlsHeight))
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        val scale = sourceScale(width)
        val side = (width * .064f).roundToInt()
        var y = sdp(4, scale)

        val coverLeft = (width - cover.measuredWidth) / 2
        cover.layout(coverLeft, y, coverLeft + cover.measuredWidth, y + cover.measuredHeight)
        y += cover.measuredHeight + sdp(5, scale)

        val favoriteLeft = width - side - favoriteButton.measuredWidth
        val dislikeLeft = favoriteLeft - dislikeButton.measuredWidth - dp(2)
        val titleTop = y
        val titleBottom = y + titleView.measuredHeight.coerceAtLeast(dislikeButton.measuredHeight)
        titleView.layout(side, titleTop, dislikeLeft - dp(8), titleTop + titleView.measuredHeight)
        dislikeButton.layout(
            dislikeLeft,
            titleTop,
            dislikeLeft + dislikeButton.measuredWidth,
            titleTop + dislikeButton.measuredHeight,
        )
        favoriteButton.layout(
            favoriteLeft,
            titleTop,
            favoriteLeft + favoriteButton.measuredWidth,
            titleTop + favoriteButton.measuredHeight,
        )
        y = titleBottom
        metaRow.layout(side, y, width - side, y + metaRow.measuredHeight)
        y += metaRow.measuredHeight + sdp(5, scale)
        lyricPreview.layout(side, y, width - side, y + lyricPreview.measuredHeight)

        val controlsBottom = height - sdp(8, scale)
        val controlsTop = controlsBottom - controls.measuredHeight
        controls.layout(0, controlsTop, width, controlsBottom)
        val seekBottom = controlsTop - sdp(13, scale)
        val seekTop = seekBottom - seek.measuredHeight
        seek.layout(0, seekTop, width, seekBottom)
        val actionBottom = seekTop - sdp(20, scale)
        val actionTop = actionBottom - actionRow.measuredHeight
        actionRow.layout(0, actionTop, width, actionBottom)
    }

    private fun sourceScale(width: Int): Float =
        width / (390f * resources.displayMetrics.density)
}

/** c9o + c93: current simple-vinyl source layout and source animation values. */
internal class QqSimpleVinylView(context: Context) : FrameLayout(context) {
    private val outer = LayoutInflater.from(context)
        .inflate(R.layout.qq_source_simple_vinyl_middle, this, false) as ViewGroup
    private val machineShadow = outer.findViewById<ImageView>(R.id.i62)
    private val machine = outer.findViewById<ImageView>(R.id.i5x)
    private val pole = outer.findViewById<PlayerDiscAlbumPoleView>(R.id.i63)
    private val discHost = outer.findViewById<FrameLayout>(R.id.hvu)
    private val discRoot = LayoutInflater.from(context)
        .inflate(R.layout.qq_source_simple_vinyl_disc, discHost, false) as ViewGroup
    private val discShadow = discRoot.findViewById<ImageView>(R.id.i5u)
    private val discBackground = discRoot.findViewById<ImageView>(R.id.i5q)
    private val albumBlur = discRoot.findViewById<ImageView>(R.id.i5r)
    private val discBase = discRoot.findViewById<ImageView>(R.id.i5p)
    private val discLighting = discRoot.findViewById<ImageView>(R.id.i5t)
    private val discTop = discRoot.findViewById<ImageView>(R.id.i5w)
    private val artwork = discRoot.findViewById<ImageView>(R.id.v2)
    private val discAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 7_000L
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            val angle = it.animatedValue as Float
            artwork.rotation = angle
            albumBlur.rotation = angle
            discBase.rotation = angle
        }
    }
    private val lightingAnimator = ObjectAnimator.ofFloat(
        discLighting,
        View.ROTATION,
        -1.2f,
        2f,
    ).apply {
        duration = 600L
        interpolator = LinearInterpolator()
        repeatMode = ObjectAnimator.REVERSE
        repeatCount = ObjectAnimator.INFINITE
    }
    private var artworkToken: Bitmap? = null
    private var blurSample: Bitmap? = null
    private var playing = false
    private var attached = false

    init {
        clipChildren = false
        clipToPadding = false
        outer.clipChildren = false
        outer.clipToPadding = false
        discHost.clipChildren = false
        discHost.clipToPadding = false
        discRoot.clipChildren = false
        discRoot.clipToPadding = false
        addView(outer, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        discHost.addView(discRoot, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        machineShadow.setImageResource(R.drawable.player_vinyl_machine_simple_shadow_dark)
        machineShadow.scaleType = ImageView.ScaleType.FIT_XY
        machine.setImageResource(R.drawable.player_vinyl_machine_simple_dark)
        machine.scaleType = ImageView.ScaleType.FIT_XY
        pole.setImageResource(R.drawable.player_vinyl_pole_simple_dark)
        pole.getPoleImageView().scaleType = ImageView.ScaleType.FIT_XY
        pole.m(.607f, .1855f)
        discShadow.setImageResource(R.drawable.player_vinyl_disc_shawdow_simple_dark)
        discBackground.setImageResource(R.drawable.player_vinyl_disc_background_simple_dark)
        discBase.setImageResource(R.drawable.player_vinyl_disc_base)
        discLighting.setImageResource(R.drawable.player_vinyl_disc_lighting_simple_dark)
        discTop.setImageResource(R.drawable.player_vinyl_disc_top_simple_dark)
        listOf(
            discShadow,
            discBackground,
            albumBlur,
            discBase,
            discLighting,
            discTop,
            artwork,
        ).forEach { image -> image.scaleType = ImageView.ScaleType.FIT_XY }

        artwork.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        artwork.clipToOutline = true
        albumBlur.outlineProvider = artwork.outlineProvider
        albumBlur.clipToOutline = true
    }

    fun bind(bitmap: Bitmap?, themeColor: Int, isPlaying: Boolean) {
        if (artworkToken !== bitmap) {
            val hadArtwork = artworkToken != null
            artworkToken = bitmap
            bindArtwork(bitmap)
            if (hadArtwork && isLaidOut) {
                outer.animate().cancel()
                outer.translationX = width * .12f
                outer.alpha = .72f
                outer.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(1_000L)
                    .setInterpolator(PathInterpolator(.32f, .94f, .6f, 1f))
                    .start()
            }
        }
        discTop.setColorFilter(simpleDiscTint(themeColor), PorterDuff.Mode.SRC_ATOP)
        discBackground.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP)
        setPlaying(isPlaying)
    }

    private fun bindArtwork(bitmap: Bitmap?) {
        val valid = bitmap?.takeUnless(Bitmap::isRecycled)
        if (valid != null) {
            artwork.setImageBitmap(valid)
            discBase.setImageResource(R.drawable.player_vinyl_disc_base)
            discLighting.setImageResource(R.drawable.player_vinyl_disc_lighting_simple_dark)
        } else {
            artwork.setImageResource(R.drawable.player_album_cover_default_dark)
            discBase.setImageResource(R.drawable.player_vinyl_disc_base_simple_dark_default)
            discLighting.setImageResource(
                R.drawable.player_vinyl_disc_lighting_simple_dark_default,
            )
        }
        val oldSample = blurSample
        blurSample = valid?.let { source ->
            runCatching { Bitmap.createScaledBitmap(source, 20, 20, true) }.getOrNull()
        }
        if (blurSample != null) {
            albumBlur.setImageBitmap(blurSample)
        } else {
            albumBlur.setImageResource(R.drawable.player_album_cover_default_dark)
        }
        if (oldSample != null && oldSample !== blurSample && !oldSample.isRecycled) {
            oldSample.recycle()
        }
    }

    private fun setPlaying(value: Boolean) {
        if (playing == value) return
        playing = value
        if (!attached) return
        if (value) {
            if (!discAnimator.isStarted) discAnimator.start() else discAnimator.resume()
            if (!lightingAnimator.isStarted) lightingAnimator.start() else lightingAnimator.resume()
        } else {
            if (discAnimator.isStarted && !discAnimator.isPaused) discAnimator.pause()
            if (lightingAnimator.isStarted && !lightingAnimator.isPaused) lightingAnimator.pause()
        }
        pole.k(value)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        machineShadow.layoutParams = machineShadow.layoutParams.apply {
            width = w
            height = (h * 1.011236f).roundToInt()
        }
        machineShadow.translationY = h * .07f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        if (!discAnimator.isStarted) {
            discAnimator.start()
            discAnimator.pause()
        }
        if (!lightingAnimator.isStarted) {
            lightingAnimator.start()
            lightingAnimator.pause()
        }
        if (playing) {
            discAnimator.resume()
            lightingAnimator.resume()
            pole.k(true)
        }
    }

    override fun onDetachedFromWindow() {
        attached = false
        outer.animate().cancel()
        pole.h()
        discAnimator.cancel()
        lightingAnimator.cancel()
        blurSample?.takeUnless(Bitmap::isRecycled)?.recycle()
        blurSample = null
        artworkToken = null
        super.onDetachedFromWindow()
    }
}

private class QqSourceMetaRow(context: Context) : LinearLayout(context) {
    private val artist = sourceText(context, 16f, false, 0xFFAAAAAD.toInt()).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL
    }
    private val sourceTag = sourceText(context, 11f, false, 0xFFBEBEC1.toInt()).apply {
        text = "本地音频"
        gravity = Gravity.CENTER
        setPadding(dp(7), 0, dp(7), 0)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(4).toFloat()
            setColor(0x78000000)
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(artist, LayoutParams(0, MATCH_PARENT, 1f))
        addView(sourceTag, LayoutParams(WRAP_CONTENT, dp(21)).apply {
            marginStart = dp(8)
        })
    }

    fun bind(value: String) {
        artist.text = value
    }

    fun applyStyle(color: Int) {
        artist.setTextColor(ColorUtils.setAlphaComponent(color, 180))
        sourceTag.setTextColor(ColorUtils.setAlphaComponent(color, 194))
    }
}

/** Source c6u composition with c6s/c75/c6q/c6n/c8m button cells. */
private class QqSourceActionRow(context: Context) : FrameLayout(context) {
    private val source = LayoutInflater.from(context)
        .inflate(R.layout.qq_source_player_action_row, this, false)
    private val lyrics = source.findViewById<ImageView>(R.id.qq_source_action_ksong_icon)
    private val speed = source.findViewById<ImageView>(R.id.qq_source_action_sound_icon)
    private val download = source.findViewById<ImageView>(R.id.qq_source_action_download_icon)
    private val timer = source.findViewById<ImageView>(R.id.qq_source_action_timer_icon)
    private val more = source.findViewById<ImageView>(R.id.qq_source_action_more_icon)
    private val speedState = source.findViewById<TextView>(R.id.qq_source_action_sound_state)

    var onShowLyrics: () -> Unit = {}
    var onDownload: () -> Unit = {}
    var onShowSpeed: () -> Unit = {}
    var onShowTimer: () -> Unit = {}
    var onMore: () -> Unit = {}

    init {
        clipChildren = false
        addView(source, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        lyrics.setOnClickListener { onShowLyrics() }
        speed.setOnClickListener { onShowSpeed() }
        download.setOnClickListener { onDownload() }
        timer.setOnClickListener { onShowTimer() }
        more.setOnClickListener { onMore() }
    }

    fun setSpeed(value: Float) {
        val description = "播放速度，${formatSpeed(value)}倍"
        speed.contentDescription = description
        speedState.text = if (value == 1f) "Off" else "${formatSpeed(value)}x"
    }

    fun applyStyle(color: Int) {
        listOf(lyrics, speed, download, timer, more).forEach {
            it.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        speedState.setTextColor(ColorUtils.setAlphaComponent(color, 205))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val side = (width * .064f).roundToInt()
        setPadding(side, 0, side, 0)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

/** Direct adapter around source c91. */
private class QqSourceSeek(context: Context) : FrameLayout(context) {
    private val source = LayoutInflater.from(context)
        .inflate(R.layout.qq_source_player_seek, this, false)
    private val seekBar = source.findViewById<SeekBar>(R.id.hr7)
    private val current = source.findViewById<TextView>(R.id.ifo)
    private val duration = source.findViewById<TextView>(R.id.ly3)
    private var durationMs = 0L
    private var binding = false

    var onSeek: (Long) -> Unit = {}

    init {
        clipChildren = false
        clipToPadding = false
        (source as? ViewGroup)?.apply {
            clipChildren = false
            clipToPadding = false
        }
        addView(source, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        seekBar.max = SEEK_MAX
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !binding) {
                    current.text = formatTime(progress.toLong() * durationMs / SEEK_MAX)
                }
            }

            override fun onStartTrackingTouch(bar: SeekBar?) = Unit

            override fun onStopTrackingTouch(bar: SeekBar?) {
                val progress = bar?.progress ?: 0
                onSeek(progress.toLong() * durationMs / SEEK_MAX)
            }
        })
    }

    fun bind(positionMs: Long, durationMs: Long) {
        this.durationMs = durationMs.coerceAtLeast(0L)
        binding = true
        seekBar.progress = if (this.durationMs > 0L) {
            (positionMs.coerceIn(0L, this.durationMs) * SEEK_MAX / this.durationMs).toInt()
        } else {
            0
        }
        binding = false
        current.text = formatTime(positionMs.coerceAtLeast(0L))
        duration.text = formatTime(this.durationMs)
    }

    fun applyStyle(color: Int) {
        current.setTextColor(ColorUtils.setAlphaComponent(color, 170))
        duration.setTextColor(ColorUtils.setAlphaComponent(color, 170))
        seekBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
        seekBar.thumbTintList = android.content.res.ColorStateList.valueOf(color)
    }
}

/** Direct adapter around source c6c. */
private class QqSourceControls(context: Context) : FrameLayout(context) {
    private val source = LayoutInflater.from(context)
        .inflate(R.layout.qq_source_player_controls, this, false)
    private val modeContainer = source.findViewById<FrameLayout>(R.id.abt)
    private val playlistContainer = source.findViewById<FrameLayout>(R.id.ac7)
    private val leftPrevious = source.findViewById<ImageView>(R.id.ae2)
    private val rightNext = source.findViewById<ImageView>(R.id.adw)
    private val playContainer = source.findViewById<View>(R.id.adx)
    private val play = source.findViewById<ImageView>(R.id.adu)
    private val loading = source.findViewById<LottieAnimationView>(R.id.adv)
    private val mode = sourceImageButton(
        context,
        R.drawable.player_btn_repeat_normal,
        "播放顺序",
    )
    private val playlist = sourceImageButton(
        context,
        R.drawable.player_btn_playlist_normal,
        "播放列表",
    )
    private var playbackMode = QqMusicPlaybackMode.LIST_REPEAT
    private var boundPlaybackMode: QqMusicPlaybackMode? = null
    private var boundState: String? = null
    private var controlTint = CONTROL_ICON_COLOR
    private var popup: ih? = null
    private val dismissPopup = Runnable { popup?.dismiss() }

    var onShowPlaylist: () -> Unit = {}
    var onCyclePlaybackMode: () -> QqMusicPlaybackMode = { playbackMode.next() }
    var onSetPlaybackMode: (QqMusicPlaybackMode) -> Unit = {}
    var onPlayPause: () -> Unit = {}
    var onPrevious: () -> Unit = {}
    var onNext: () -> Unit = {}

    init {
        clipChildren = false
        clipToPadding = false
        (source as? ViewGroup)?.apply {
            clipChildren = false
            clipToPadding = false
        }
        addView(source, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        modeContainer.addView(mode, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        playlistContainer.addView(playlist, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        mode.isClickable = false
        mode.isFocusable = false
        playlist.isClickable = false
        playlist.isFocusable = false
        leftPrevious.setImageResource(R.drawable.player_btn_pre_normal)
        leftPrevious.setColorFilter(CONTROL_ICON_COLOR, PorterDuff.Mode.SRC_IN)
        leftPrevious.contentDescription = "上一首"
        rightNext.setImageResource(R.drawable.player_btn_next_normal)
        rightNext.setColorFilter(CONTROL_ICON_COLOR, PorterDuff.Mode.SRC_IN)
        rightNext.contentDescription = "下一首"
        leftPrevious.setOnClickListener { onPrevious() }
        rightNext.setOnClickListener { onNext() }
        mode.setColorFilter(CONTROL_ICON_COLOR, PorterDuff.Mode.SRC_IN)
        playlist.setColorFilter(CONTROL_ICON_COLOR, PorterDuff.Mode.SRC_IN)
        playContainer.setOnClickListener { onPlayPause() }
        playlistContainer.setOnClickListener { onShowPlaylist() }
        modeContainer.setOnClickListener {
            bindMode(onCyclePlaybackMode())
            showModePopup()
        }
        modeContainer.setOnLongClickListener {
            showModePopup()
            true
        }
        loading.setAnimation("qq-lottie/player_button_loading.json")
        loading.repeatCount = ValueAnimator.INFINITE
    }

    fun bind(state: String, playbackMode: QqMusicPlaybackMode) {
        bindMode(playbackMode)
        if (boundState == state) return
        boundState = state
        val loadingState = state == "PREPARING"
        val playing = state == "PLAYING"
        play.setImageResource(
            if (playing) R.drawable.player_btn_pause_normal
            else R.drawable.player_btn_play_normal,
        )
        play.setColorFilter(controlTint, PorterDuff.Mode.SRC_IN)
        play.visibility = if (loadingState) View.INVISIBLE else View.VISIBLE
        loading.visibility = if (loadingState) View.VISIBLE else View.INVISIBLE
        if (loadingState) {
            if (!loading.isAnimating) loading.playAnimation()
        } else {
            loading.cancelAnimation()
        }
        playContainer.contentDescription = if (playing) "暂停" else "播放"
    }

    private fun bindMode(value: QqMusicPlaybackMode) {
        playbackMode = value
        if (boundPlaybackMode == value) return
        boundPlaybackMode = value
        mode.setImageResource(
            when (value) {
                QqMusicPlaybackMode.LIST_REPEAT -> R.drawable.player_btn_repeat_normal
                QqMusicPlaybackMode.SINGLE_REPEAT -> R.drawable.player_btn_repeatone_normal
                QqMusicPlaybackMode.SHUFFLE -> R.drawable.player_btn_random_normal
            },
        )
        mode.setColorFilter(controlTint, PorterDuff.Mode.SRC_IN)
        mode.contentDescription = when (value) {
            QqMusicPlaybackMode.LIST_REPEAT -> "顺序播放"
            QqMusicPlaybackMode.SINGLE_REPEAT -> "单曲循环"
            QqMusicPlaybackMode.SHUFFLE -> "随机播放"
        }
        popup?.r(value.sourceValue)
    }

    fun applyStyle(color: Int) {
        controlTint = color
        listOf(leftPrevious, rightNext, play, mode, playlist).forEach {
            it.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun showModePopup() {
        val sourcePopup = popup ?: ih(context, object : ih.b {
            override fun a(count: Int) = Unit

            override fun b() = Unit

            override fun c() {
                bindMode(onCyclePlaybackMode())
            }

            override fun d() = Unit

            override fun e(sourceMode: Int) {
                val selected = QqMusicPlaybackMode.fromSourceValue(sourceMode)
                bindMode(selected)
                onSetPlaybackMode(selected)
            }
        }).also { popup = it }
        if (sourcePopup.isShowing) return
        sourcePopup.t(true)
        sourcePopup.r(playbackMode.sourceValue)
        sourcePopup.s(y())
        sourcePopup.u(modeContainer, false)
        modeContainer.removeCallbacks(dismissPopup)
        val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager
        if (accessibility?.isTouchExplorationEnabled != true) {
            modeContainer.postDelayed(dismissPopup, 1_500L)
        }
    }

    override fun onDetachedFromWindow() {
        modeContainer.removeCallbacks(dismissPopup)
        popup?.dismiss()
        popup = null
        loading.cancelAnimation()
        super.onDetachedFromWindow()
    }
}

/** QQ 音乐 20.6.5.8 c4v：原歌词引擎仅由宿主连接播放状态。 */
private class QqPlayerLyricPage(context: Context) : FrameLayout(context) {
    private val title: TextView
    private val artist: TextView
    private val lyricLine: SingleLyricView
    private val play: ImageView

    var onPlayPause: () -> Unit = {}

    init {
        LayoutInflater.from(context).inflate(R.layout.qq_source_player_lyric, this, true)
        title = findViewById(R.id.qq_source_lyric_title)
        artist = findViewById(R.id.qq_source_lyric_artist)
        lyricLine = findViewById(R.id.qq_source_lyric_line)
        play = findViewById(R.id.qq_source_lyric_play)
        lyricLine.setPriorityText("纯音乐，请欣赏")
        play.setOnClickListener { onPlayPause() }
    }

    fun bind(
        title: String,
        artist: String,
        isPlaying: Boolean,
        positionMs: Long,
        style: QqPlayerStyleSettings,
    ) {
        this.title.text = title
        this.artist.text = artist.ifBlank { "本地音频" }
        lyricLine.seek(positionMs)
        play.setImageResource(
            if (isPlaying) R.drawable.player_btn_lyric_pause_normal
            else R.drawable.player_btn_lyric_play_normal,
        )
        play.contentDescription = if (isPlaying) "暂停" else "播放"
        applyPlayerStyle(style)
    }

    fun applyPlayerStyle(style: QqPlayerStyleSettings) {
        val chosen = qqStyleColor(
            context,
            "textAndButtonColor",
            style.textAndButtonColorId,
            Color.WHITE,
        )
        val amount = if (style.enabled) {
            (.25f + style.textAndButtonAlpha.coerceIn(0f, 1f) * .35f)
        } else {
            0f
        }
        val color = ColorUtils.blendARGB(Color.WHITE, chosen, amount)
        title.setTextColor(color)
        artist.setTextColor(ColorUtils.setAlphaComponent(color, 178))
        lyricLine.setColor(ColorUtils.setAlphaComponent(color, 158))
        lyricLine.setHColor(color)
        play.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}

private fun sourceImageButton(
    context: Context,
    resource: Int,
    description: String,
): ImageView = ImageView(context).apply {
    setImageResource(resource)
    scaleType = ImageView.ScaleType.FIT_CENTER
    background = null
    isClickable = true
    isFocusable = true
    contentDescription = description
}

private fun sourceText(
    context: Context,
    sizeSp: Float,
    bold: Boolean,
    color: Int,
): TextView = TextView(context).apply {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    setTextColor(color)
    includeFontPadding = false
    if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
}

private fun simpleDiscTint(color: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)
    hsv[1] = if (hsv[1] < .05f) 0f else .3f
    hsv[2] = .6f
    return Color.HSVToColor(hsv)
}

private fun ViewGroup.sdp(value: Int, scale: Float): Int = (dp(value) * scale).roundToInt()

private fun View.dp(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()

private fun ViewGroup.dp(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()

private fun exact(value: Int): Int = View.MeasureSpec.makeMeasureSpec(value, View.MeasureSpec.EXACTLY)
private fun atMost(value: Int): Int = View.MeasureSpec.makeMeasureSpec(value, View.MeasureSpec.AT_MOST)
private fun unspecified(): Int = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun formatSpeed(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString()
    else String.format(Locale.US, "%.1f", value)

private const val SEEK_MAX = 10_000
private const val DEFAULT_SOURCE_COLOR = 0xFF666666.toInt()
private const val ROOT_DARK_COLOR = 0xFF1C1C1E.toInt()
private const val TOP_ICON_COLOR = 0xFFC8C8CB.toInt()
private const val ACTION_ICON_COLOR = 0xFFB5B5B8.toInt()
private const val CONTROL_ICON_COLOR = 0xFFF4F4F6.toInt()
private const val SOURCE_ACCENT_COLOR = 0xFF18D784.toInt()
private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
