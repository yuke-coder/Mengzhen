package com.mengzhen.app.ui.screens

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.litao.slider.NiftySlider
import com.litao.slider.effect.ColorPickEffect
import com.mengzhen.app.R
import com.tencent.qqmusic.business.customskin.player.adapter.custom.k0
import com.tencent.qqmusic.business.customskin.player.view.BtnTypeSeekbarView
import com.tencent.qqmusiccommon.util.j0
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** QQ 音乐 20.6.5.8 的 3000026「简约雅黑」播放器配置。 */
internal data class QqPlayerStyleSettings(
    val enabled: Boolean = false,
    val backgroundId: String = "default",
    val backgroundAlpha: Float = 1f,
    val backgroundBlur: Float = 0f,
    val customBackgroundPath: String = "",
    val stickerId: String = "",
    val recordBaseId: String = "def",
    val customRecordBasePath: String = "",
    val highlightColorId: String = "blur",
    val highlightAlpha: Float = 1f,
    val textAndButtonColorId: String = "color0",
    val textAndButtonAlpha: Float = 1f,
    val recordStyleId: String = "recordStyle4",
    val recordStyleAlpha: Float = 1f,
    val recordDurationMs: Long = 7_000L,
    val backgroundLightEffectId: String = "gradient_light_effect",
    val foregroundLightEffectId: String = "none",
)

internal enum class QqCustomImageTarget {
    BACKGROUND,
    RECORD_BASE,
}

internal object QqPlayerStyleStore {
    private const val PREFS = "qq_music_player_style_3000026"

    fun load(context: Context): QqPlayerStyleSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return QqPlayerStyleSettings(
            enabled = prefs.getBoolean("enabled", false),
            backgroundId = prefs.getString("background", "default") ?: "default",
            backgroundAlpha = prefs.getFloat("background_alpha", 1f),
            backgroundBlur = prefs.getFloat("background_blur", 0f),
            customBackgroundPath = prefs.getString("custom_background", "").orEmpty(),
            stickerId = prefs.getString("sticker", "").orEmpty(),
            recordBaseId = prefs.getString("record_base", "def") ?: "def",
            customRecordBasePath = prefs.getString("custom_record_base", "").orEmpty(),
            highlightColorId = prefs.getString("highlight_color", "blur") ?: "blur",
            highlightAlpha = prefs.getFloat("highlight_alpha", 1f),
            textAndButtonColorId = prefs.getString("text_button_color", "color0") ?: "color0",
            textAndButtonAlpha = prefs.getFloat("text_button_alpha", 1f),
            recordStyleId = prefs.getString("record_style", "recordStyle4")
                ?: "recordStyle4",
            recordStyleAlpha = prefs.getFloat("record_style_alpha", 1f),
            recordDurationMs = prefs.getLong("record_duration", 7_000L),
            backgroundLightEffectId = prefs.getString(
                "background_light_effect",
                "gradient_light_effect",
            ) ?: "gradient_light_effect",
            foregroundLightEffectId = prefs.getString("foreground_light_effect", "none")
                ?: "none",
        )
    }

    fun save(context: Context, value: QqPlayerStyleSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", value.enabled)
            .putString("background", value.backgroundId)
            .putFloat("background_alpha", value.backgroundAlpha)
            .putFloat("background_blur", value.backgroundBlur)
            .putString("custom_background", value.customBackgroundPath)
            .putString("sticker", value.stickerId)
            .putString("record_base", value.recordBaseId)
            .putString("custom_record_base", value.customRecordBasePath)
            .putString("highlight_color", value.highlightColorId)
            .putFloat("highlight_alpha", value.highlightAlpha)
            .putString("text_button_color", value.textAndButtonColorId)
            .putFloat("text_button_alpha", value.textAndButtonAlpha)
            .putString("record_style", value.recordStyleId)
            .putFloat("record_style_alpha", value.recordStyleAlpha)
            .putLong("record_duration", value.recordDurationMs)
            .putString("background_light_effect", value.backgroundLightEffectId)
            .putString("foreground_light_effect", value.foregroundLightEffectId)
            .apply()
    }

    fun importImage(context: Context, target: QqCustomImageTarget, uri: Uri): String? {
        val directory = File(context.filesDir, "qq-player-style").apply {
            if (!exists() && !mkdirs()) return null
        }
        val output = File(
            directory,
            if (target == QqCustomImageTarget.BACKGROUND) {
                "custom-background"
            } else {
                "custom-record-base"
            },
        )
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(output, false).use(input::copyTo)
            } ?: return null
            output.absolutePath
        }.getOrNull()
    }
}

internal data class QqPlayerStyleChoice(
    val id: String,
    val text: String,
    val color: String,
    val previewFile: String,
    val backgroundFile: String,
    val foregroundFile: String,
    val autoRecordBase: String,
    val autoHighlightColor: String,
    val autoRecordStyle: String,
    val autoRecordStyleAlpha: Float?,
    val autoHighlightAlpha: Float?,
    val useSlider: Boolean?,
)

internal data class QqPlayerStyleOption(
    val key: String,
    val title: String,
    val groups: List<List<QqPlayerStyleChoice>>,
    val entryTitle: String,
    val entryShowsArrow: Boolean,
    val templateIds: List<String>,
)

/** 选项、分组、顺序与默认值直接读取设备中当前 QQ 音乐样式包。 */
internal class QqPlayerStyleCatalog private constructor(
    val options: List<QqPlayerStyleOption>,
) {
    fun option(key: String): QqPlayerStyleOption? = options.firstOrNull { it.key == key }

    fun choice(key: String, id: String): QqPlayerStyleChoice? =
        option(key)?.groups?.asSequence()?.flatten()?.firstOrNull { it.id == id }

    fun color(key: String, id: String, fallback: Int): Int {
        val source = choice(key, id)?.color.orEmpty()
        return if (source.startsWith('#')) {
            runCatching { Color.parseColor(source) }.getOrDefault(fallback)
        } else {
            fallback
        }
    }

    companion object {
        @Volatile
        private var cached: QqPlayerStyleCatalog? = null

        fun load(context: Context): QqPlayerStyleCatalog {
            cached?.let { return it }
            val parsed = runCatching {
                context.assets.open("qq-player-style/qq-player-style-3000026.json")
                    .bufferedReader()
                    .use { JSONObject(it.readText()) }
            }.getOrNull() ?: return QqPlayerStyleCatalog(emptyList()).also { cached = it }
            val result = ArrayList<QqPlayerStyleOption>()
            val options = parsed.optJSONArray("option") ?: JSONArray()
            for (index in 0 until options.length()) {
                val option = options.optJSONObject(index) ?: continue
                val key = option.optString("selectionKey")
                if (key.isBlank()) continue
                val groups = ArrayList<List<QqPlayerStyleChoice>>()
                val groupArray = option.optJSONArray("selectionArray")
                if (groupArray != null) {
                    for (groupIndex in 0 until groupArray.length()) {
                        val choices = groupArray.optJSONArray(groupIndex) ?: continue
                        groups += parseChoices(key, choices)
                    }
                } else {
                    val choices = option.optJSONArray("selection")
                    if (choices != null) groups += parseChoices(key, choices)
                }
                result += QqPlayerStyleOption(
                    key = key,
                    title = option.optString("title"),
                    groups = groups,
                    entryTitle = option.optJSONObject("entry")?.optString("title").orEmpty(),
                    entryShowsArrow = option.optJSONObject("entry")?.optBoolean("showArrow") == true,
                    templateIds = option.optJSONArray("templateIdStrs").toStringList(),
                )
            }
            return QqPlayerStyleCatalog(result).also { cached = it }
        }

        private fun parseChoices(key: String, values: JSONArray): List<QqPlayerStyleChoice> {
            val result = ArrayList<QqPlayerStyleChoice>(values.length())
            for (index in 0 until values.length()) {
                val value = values.optJSONObject(index) ?: continue
                val id = value.optString("id")
                if (id.isBlank()) continue
                val automatic = value.optJSONObject("switchAutoSetting")
                result += QqPlayerStyleChoice(
                    id = id,
                    text = value.optString("text", value.optString("name", "")),
                    color = value.optString("color"),
                    previewFile = if (key == "stickers") {
                        "sticker_$id.webp"
                    } else {
                        "${key}_$id.png"
                    },
                    backgroundFile = value.optString("backgroundPath"),
                    foregroundFile = value.optString("foregroundPath"),
                    autoRecordBase = automatic?.optString("recordBase").orEmpty(),
                    autoHighlightColor = automatic?.optString("highlightColor").orEmpty(),
                    autoRecordStyle = automatic?.optString("recordStyle").orEmpty(),
                    autoRecordStyleAlpha = automatic?.optDouble("recordStyleAlpha")
                        ?.takeUnless(Double::isNaN)?.toFloat(),
                    autoHighlightAlpha = automatic?.optDouble("highlightColorAlpha")
                        ?.takeUnless(Double::isNaN)?.toFloat(),
                    useSlider = if (value.has("useSlider")) {
                        value.optBoolean("useSlider")
                    } else {
                        null
                    },
                )
            }
            return result
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }.filter(String::isNotBlank)
}

internal fun QqPlayerStyleSettings.selectedId(key: String): String = when (key) {
    "background" -> backgroundId
    "stickers" -> stickerId
    "recordBase" -> recordBaseId
    "highlightColor" -> highlightColorId
    "textAndButtonColor" -> textAndButtonColorId
    "recordStyle" -> recordStyleId
    "recordSpeed" -> recordDurationMs.toString()
    "backgroundLightEffectStyle" -> backgroundLightEffectId
    "foregroundLightEffectStyle" -> foregroundLightEffectId
    else -> ""
}

internal fun QqPlayerStyleSettings.select(
    key: String,
    choice: QqPlayerStyleChoice,
): QqPlayerStyleSettings {
    var next = when (key) {
        "background" -> copy(backgroundId = choice.id)
        "stickers" -> copy(stickerId = choice.id)
        "recordBase" -> copy(recordBaseId = choice.id)
        "highlightColor" -> copy(highlightColorId = choice.id)
        "textAndButtonColor" -> copy(textAndButtonColorId = choice.id)
        "recordStyle" -> copy(recordStyleId = choice.id)
        "recordSpeed" -> copy(recordDurationMs = choice.id.toLongOrNull() ?: 7_000L)
        "backgroundLightEffectStyle" -> copy(backgroundLightEffectId = choice.id)
        "foregroundLightEffectStyle" -> copy(foregroundLightEffectId = choice.id)
        else -> this
    }
    if (key == "background") {
        if (choice.autoRecordBase.isNotBlank()) {
            next = next.copy(recordBaseId = choice.autoRecordBase)
        }
        if (choice.autoHighlightColor.isNotBlank()) {
            next = next.copy(highlightColorId = choice.autoHighlightColor)
        }
        if (choice.autoRecordStyle.isNotBlank()) {
            next = next.copy(recordStyleId = choice.autoRecordStyle)
        }
        choice.autoRecordStyleAlpha?.let { next = next.copy(recordStyleAlpha = it) }
        choice.autoHighlightAlpha?.let { next = next.copy(highlightAlpha = it) }
    }
    return next
}

internal fun qqStyleColor(
    context: Context,
    key: String,
    id: String,
    fallback: Int,
): Int = QqPlayerStyleCatalog.load(context).color(key, id, fallback)

internal fun qqStyleDrawable(context: Context, fileName: String): Int {
    if (fileName.isBlank()) return 0
    val base = fileName.substringBeforeLast('.').lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9_]"), "_")
    return context.resources.getIdentifier(
        "qq_style_$base",
        "drawable",
        context.packageName,
    )
}

/** 3000026 样式包的 2D 黑胶唱机组合与唱片旋转。 */
internal class QqDefinedVinylView(context: Context) : ViewGroup(context) {
    private val customPlate = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    private val base = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
    }
    private val disc = FrameLayout(context).apply {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
    }
    private val discBackground = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
    }
    private val album = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    private val highlight = View(context)
    private val texture = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
    }
    private val sticker = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private var playing = false
    private var attached = false
    private var currentDuration = 7_000L
    private val spin = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = currentDuration
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { disc.rotation = it.animatedValue as Float }
    }

    init {
        clipChildren = false
        clipToPadding = false
        addView(customPlate)
        addView(base)
        addView(disc)
        disc.addView(discBackground, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        disc.addView(album, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        disc.addView(highlight, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        disc.addView(texture, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(sticker)
    }

    fun bind(
        artwork: Bitmap?,
        themeColor: Int,
        isPlaying: Boolean,
        settings: QqPlayerStyleSettings,
    ) {
        val validArtwork = artwork?.takeUnless(Bitmap::isRecycled)
        if (validArtwork != null) album.setImageBitmap(validArtwork)
        else album.setImageResource(R.drawable.player_album_cover_default_dark)

        val backgroundResource = when (settings.recordBaseId) {
            "def" -> qqStyleDrawable(context, "recordBase_def_background.png")
            "onlyPole" -> qqStyleDrawable(context, "recordBase_onlyPole_background.png")
            "customBackground" -> qqStyleDrawable(
                context,
                "recordBase_customBackground_background.png",
            )
            else -> 0
        }
        base.setImageResource(backgroundResource)
        base.visibility = if (backgroundResource != 0) VISIBLE else INVISIBLE
        customPlate.visibility = if (
            settings.recordBaseId == "customBackground" &&
            settings.customRecordBasePath.isNotBlank()
        ) VISIBLE else INVISIBLE
        if (customPlate.visibility == VISIBLE) {
            customPlate.setImageBitmap(BitmapFactory.decodeFile(settings.customRecordBasePath))
        }

        val hasDisc = settings.recordBaseId != "none"
        disc.visibility = if (hasDisc) VISIBLE else INVISIBLE
        discBackground.setImageResource(qqStyleDrawable(context, "recordStyle_background.png"))
        texture.setImageResource(
            qqStyleDrawable(context, "${settings.recordStyleId}_recordStyle.png"),
        )
        texture.alpha = settings.recordStyleAlpha.coerceIn(0f, 1f)
        val highlightColor = when (settings.highlightColorId) {
            "blur" -> ColorUtils.blendARGB(themeColor, Color.WHITE, .12f)
            "magic" -> themeColor
            else -> qqStyleColor(
                context,
                "highlightColor",
                settings.highlightColorId,
                themeColor,
            )
        }
        highlight.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(highlightColor)
        }
        highlight.alpha = (.18f + settings.highlightAlpha.coerceIn(0f, 1f) * .34f)
        val stickerResource = if (settings.stickerId.isBlank()) 0 else {
            qqStyleDrawable(context, "sticker_${settings.stickerId}.webp")
        }
        sticker.setImageResource(stickerResource)
        sticker.visibility = if (stickerResource == 0) GONE else VISIBLE

        if (currentDuration != settings.recordDurationMs) {
            currentDuration = settings.recordDurationMs.coerceAtLeast(1_000L)
            val shouldResume = playing && attached
            spin.cancel()
            spin.duration = currentDuration
            if (attached) {
                spin.start()
                if (!shouldResume) spin.pause()
            }
        }
        setPlaying(isPlaying)
    }

    private fun setPlaying(value: Boolean) {
        if (playing == value) return
        playing = value
        if (!attached) return
        if (value) {
            if (!spin.isStarted) spin.start() else if (spin.isPaused) spin.resume()
        } else if (spin.isStarted && !spin.isPaused) {
            spin.pause()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val plateWidth = min(width, (height * 660f / 620f).roundToInt())
        val plateHeight = (plateWidth * 620f / 660f).roundToInt()
        val discSize = (plateWidth * .8f).roundToInt()
        customPlate.measure(exactStyle(plateWidth), exactStyle(plateHeight))
        base.measure(exactStyle(plateWidth), exactStyle(plateHeight))
        disc.measure(exactStyle(discSize), exactStyle(discSize))
        val stickerSize = (plateWidth * .28f).roundToInt()
        sticker.measure(exactStyle(stickerSize), exactStyle(stickerSize))
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        val plateWidth = min(width, (height * 660f / 620f).roundToInt())
        val plateHeight = (plateWidth * 620f / 660f).roundToInt()
        val plateLeft = (width - plateWidth) / 2
        val plateTop = (height - plateHeight) / 2
        customPlate.layout(plateLeft, plateTop, plateLeft + plateWidth, plateTop + plateHeight)
        base.layout(plateLeft, plateTop, plateLeft + plateWidth, plateTop + plateHeight)
        val discSize = (plateWidth * .8f).roundToInt()
        val discLeft = plateLeft + (plateWidth * .1f).roundToInt()
        val discTop = plateTop + (plateHeight * .0758f).roundToInt()
        disc.layout(discLeft, discTop, discLeft + discSize, discTop + discSize)
        val stickerSize = (plateWidth * .28f).roundToInt()
        sticker.layout(
            plateLeft + (plateWidth * .055f).roundToInt(),
            plateTop + (plateHeight * .04f).roundToInt(),
            plateLeft + (plateWidth * .055f).roundToInt() + stickerSize,
            plateTop + (plateHeight * .04f).roundToInt() + stickerSize,
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        if (!spin.isStarted) {
            spin.start()
            if (!playing) spin.pause()
        } else if (playing && spin.isPaused) {
            spin.resume()
        }
    }

    override fun onDetachedFromWindow() {
        attached = false
        spin.cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    }
}

/** 默认源码唱机与 3000026 自定义唱机之间按“使用”状态切换。 */
internal class QqVinylHostView(context: Context) : FrameLayout(context) {
    private val source = QqSimpleVinylView(context)
    private val defined = QqDefinedVinylView(context)

    init {
        addView(source, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(defined, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun bind(
        artwork: Bitmap?,
        themeColor: Int,
        isPlaying: Boolean,
        settings: QqPlayerStyleSettings,
    ) {
        source.visibility = if (settings.enabled) GONE else VISIBLE
        defined.visibility = if (settings.enabled) VISIBLE else GONE
        if (settings.enabled) defined.bind(artwork, themeColor, isPlaying, settings)
        else source.bind(artwork, themeColor, isPlaying)
    }
}

/** PDEditActivity 的 o3 源布局。 */
internal class QqPlayerStyleEditorView(
    context: Context,
    private val onDismiss: () -> Unit,
    private val onUse: (QqPlayerStyleSettings) -> Unit,
    private val onRequestImage: (QqCustomImageTarget) -> Unit,
) : FrameLayout(context) {
    private val sourceRoot = LayoutInflater.from(context)
        .inflate(R.layout.qq_source_player_style_editor, this, false) as ViewGroup
    private val previewContainer = sourceRoot.findViewById<ViewGroup>(R.id.n7g)
    private val preview = QqPlayerStylePreviewView(context)
    private val list = sourceRoot.findViewById<RecyclerView>(R.id.iqz)
    private val catalog = QqPlayerStyleCatalog.load(context)
    private val adapter = QqPlayerStyleAdapter(
        catalog = catalog,
        current = { draft },
        onSelect = ::select,
        onSlider = ::updateSlider,
    )
    private var draft = QqPlayerStyleSettings(enabled = true)
    private var artwork: Bitmap? = null
    private var themeColor = 0xFF666666.toInt()
    private var title = ""
    private var artist = ""
    private var isPlaying = false

    init {
        addView(sourceRoot, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        sourceRoot.findViewById<ViewGroup>(R.id.n7f).apply {
            removeAllViews()
            addView(preview, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        previewContainer.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dp(12.5f))
            }
        }
        previewContainer.clipToOutline = true
        sourceRoot.findViewById<View>(R.id.dt_).setOnClickListener { onDismiss() }
        sourceRoot.findViewById<View>(R.id.akk).setOnClickListener {
            onUse(draft.copy(enabled = true))
        }
        sourceRoot.findViewById<View>(R.id.ak0).setOnClickListener {
            draft = QqPlayerStyleSettings(enabled = true)
            refresh()
        }
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        list.itemAnimator = null
        elevation = dp(18f)
    }

    fun bind(
        current: QqPlayerStyleSettings,
        artwork: Bitmap?,
        themeColor: Int,
        title: String,
        artist: String,
        isPlaying: Boolean,
    ) {
        draft = current.copy(enabled = true)
        this.artwork = artwork
        this.themeColor = themeColor
        this.title = title
        this.artist = artist
        this.isPlaying = isPlaying
        refresh()
    }

    fun applyImportedImage(target: QqCustomImageTarget, path: String) {
        draft = if (target == QqCustomImageTarget.BACKGROUND) {
            draft.copy(backgroundId = "customBackground", customBackgroundPath = path)
        } else {
            draft.copy(recordBaseId = "customBackground", customRecordBasePath = path)
        }
        refresh()
    }

    private fun select(option: QqPlayerStyleOption, choice: QqPlayerStyleChoice) {
        if (choice.id == "customBackground") {
            val target = if (option.key == "background") {
                QqCustomImageTarget.BACKGROUND
            } else {
                QqCustomImageTarget.RECORD_BASE
            }
            val existing = if (target == QqCustomImageTarget.BACKGROUND) {
                draft.customBackgroundPath
            } else {
                draft.customRecordBasePath
            }
            if (existing.isBlank()) {
                onRequestImage(target)
                return
            }
        }
        draft = draft.select(option.key, choice)
        refresh()
    }

    private fun updateSlider(key: String, value: Float, settled: Boolean) {
        draft = when (key) {
            "backgroundAlpha" -> draft.copy(backgroundAlpha = value)
            "backgroundBlur" -> draft.copy(backgroundBlur = value)
            "highlightAlpha" -> draft.copy(highlightAlpha = value)
            "textAndButtonAlpha" -> draft.copy(textAndButtonAlpha = value)
            "recordStyleAlpha" -> draft.copy(recordStyleAlpha = value)
            else -> draft
        }
        preview.bind(draft, artwork, themeColor, title, artist, isPlaying)
        if (settled) adapter.notifyDataSetChanged()
    }

    private fun refresh() {
        preview.bind(draft, artwork, themeColor, title, artist, isPlaying)
        adapter.notifyDataSetChanged()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    }
}

private class QqPlayerStylePreviewView(context: Context) : ViewGroup(context) {
    private val background = QqDynamicDiffuseBackgroundView(context, useTexture = true)
    private val vinyl = QqDefinedVinylView(context)
    private val title = styleText(18f, true, Color.WHITE)
    private val artist = styleText(13f, false, 0xB3FFFFFF.toInt())

    init {
        clipChildren = true
        addView(background)
        addView(vinyl)
        addView(title)
        addView(artist)
    }

    fun bind(
        settings: QqPlayerStyleSettings,
        artwork: Bitmap?,
        themeColor: Int,
        title: String,
        artist: String,
        isPlaying: Boolean,
    ) {
        val enabled = settings.copy(enabled = true)
        background.bind(artwork, themeColor, enabled)
        vinyl.bind(artwork, themeColor, isPlaying, enabled)
        this.title.text = title
        this.artist.text = artist.ifBlank { "本地音频" }
        val tint = qqStyleColor(
            context,
            "textAndButtonColor",
            settings.textAndButtonColorId,
            Color.WHITE,
        )
        this.title.setTextColor(ColorUtils.blendARGB(Color.WHITE, tint, .42f))
        this.artist.setTextColor(ColorUtils.setAlphaComponent(tint, 190))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        background.measure(exactStyle(width), exactStyle(height))
        val vinylWidth = (width * .76f).roundToInt()
        val vinylHeight = (height * .69f).roundToInt()
        vinyl.measure(exactStyle(vinylWidth), exactStyle(vinylHeight))
        title.measure(atMostStyle((width * .86f).roundToInt()), unspecifiedStyle())
        artist.measure(atMostStyle((width * .86f).roundToInt()), unspecifiedStyle())
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        background.layout(0, 0, width, height)
        val vinylLeft = (width - vinyl.measuredWidth) / 2
        vinyl.layout(vinylLeft, dp(4), vinylLeft + vinyl.measuredWidth, dp(4) + vinyl.measuredHeight)
        val textLeft = (width * .07f).roundToInt()
        val titleTop = (height * .77f).roundToInt()
        title.layout(textLeft, titleTop, width - textLeft, titleTop + title.measuredHeight)
        val artistTop = titleTop + title.measuredHeight + dp(5)
        artist.layout(textLeft, artistTop, width - textLeft, artistTop + artist.measuredHeight)
    }

    private fun styleText(size: Float, bold: Boolean, color: Int) = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(color)
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
}

private class QqPlayerStyleAdapter(
    private val catalog: QqPlayerStyleCatalog,
    private val current: () -> QqPlayerStyleSettings,
    private val onSelect: (QqPlayerStyleOption, QqPlayerStyleChoice) -> Unit,
    private val onSlider: (String, Float, Boolean) -> Unit,
) : RecyclerView.Adapter<QqPlayerStyleAdapter.Holder>() {
    class Holder(val optionView: QqStyleOptionView) : RecyclerView.ViewHolder(optionView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(QqStyleOptionView(parent.context))

    override fun getItemCount(): Int = catalog.options.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val option = catalog.options[position]
        holder.optionView.bind(
            option = option,
            settings = current(),
            onChoice = { onSelect(option, it) },
            onSlider = onSlider,
        )
    }
}

/** 原 Holder 根布局；这里只把本应用的配置对象接到源 RecyclerView 与回调。 */
private class QqStyleOptionView(context: Context) : FrameLayout(context) {
    fun bind(
        option: QqPlayerStyleOption,
        settings: QqPlayerStyleSettings,
        onChoice: (QqPlayerStyleChoice) -> Unit,
        onSlider: (String, Float, Boolean) -> Unit,
    ) {
        removeAllViews()
        val layout = when (option.key) {
            "background" -> R.layout.qq_source_player_style_background
            "highlightColor", "textAndButtonColor" -> R.layout.qq_source_player_style_color
            "recordSpeed" -> R.layout.qq_source_player_style_segment
            else -> R.layout.qq_source_player_style_picture
        }
        val source = LayoutInflater.from(context).inflate(layout, this, false) as ViewGroup
        bindHeader(source, option, settings)
        when (option.key) {
            "background" -> bindBackground(source, option, settings, onChoice, onSlider)
            "highlightColor" -> bindColor(
                source, option, settings, onChoice, "鲜艳度",
                settings.highlightAlpha, "highlightAlpha", onSlider,
            )
            "textAndButtonColor" -> bindColor(
                source, option, settings, onChoice, "鲜艳度",
                settings.textAndButtonAlpha, "textAndButtonAlpha", onSlider,
            )
            "recordSpeed" -> bindRecordSpeed(source, option, settings, onChoice)
            else -> bindPicture(source, option, settings, onChoice, onSlider)
        }
        addView(source, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun bindHeader(
        source: View,
        option: QqPlayerStyleOption,
        settings: QqPlayerStyleSettings,
    ) {
        source.findViewById<TextView>(R.id.m9c).text = option.title
        val selected = option.groups.asSequence().flatten()
            .firstOrNull { it.id == settings.selectedId(option.key) }
        source.findViewById<TextView>(R.id.m9b).text = when {
            option.entryTitle.isNotBlank() -> option.entryTitle
            option.key.endsWith("LightEffectStyle") -> ""
            else -> selected?.text.orEmpty()
        }
        source.findViewById<View>(R.id.dqt).visibility =
            if (option.entryShowsArrow) VISIBLE else GONE
    }

    private fun bindBackground(
        source: View,
        option: QqPlayerStyleOption,
        settings: QqPlayerStyleSettings,
        onChoice: (QqPlayerStyleChoice) -> Unit,
        onSlider: (String, Float, Boolean) -> Unit,
    ) {
        bindChoices(source.findViewById(R.id.a80), option, option.groups.getOrElse(0) { emptyList() }, settings, onChoice)
        source.findViewById<RecyclerView>(R.id.a81).apply {
            val second = option.groups.getOrElse(1) { emptyList() }
            visibility = if (second.isEmpty()) GONE else VISIBLE
            if (second.isNotEmpty()) bindChoices(this, option, second, settings, onChoice)
        }
        val selected = option.groups.flatten().firstOrNull {
            it.id == settings.selectedId(option.key)
        }
        bindSlider(
            source.findViewById(R.id.n70), "透明度", settings.backgroundAlpha,
            "backgroundAlpha", selected?.useSlider != false, onSlider,
        )
        bindSlider(
            source.findViewById(R.id.n74), "模糊值", settings.backgroundBlur,
            "backgroundBlur",
            selected?.useSlider != false && selected?.id?.startsWith("custom") == true,
            onSlider,
        )
    }

    private fun bindColor(
        source: View,
        option: QqPlayerStyleOption,
        settings: QqPlayerStyleSettings,
        onChoice: (QqPlayerStyleChoice) -> Unit,
        label: String,
        value: Float,
        key: String,
        onSlider: (String, Float, Boolean) -> Unit,
    ) {
        bindChoices(source.findViewById(R.id.iqp), option, option.groups.flatten(), settings, onChoice)
        val selected = option.groups.flatten().firstOrNull {
            it.id == settings.selectedId(option.key)
        }
        bindSlider(source.findViewById(R.id.n70), label, value, key, selected?.useSlider != false, onSlider)
    }

    private fun bindPicture(
        source: View,
        option: QqPlayerStyleOption,
        settings: QqPlayerStyleSettings,
        onChoice: (QqPlayerStyleChoice) -> Unit,
        onSlider: (String, Float, Boolean) -> Unit,
    ) {
        bindChoices(source.findViewById(R.id.iqu), option, option.groups.flatten(), settings, onChoice)
        bindSlider(
            source.findViewById(R.id.n70), "透明度", settings.recordStyleAlpha,
            "recordStyleAlpha", option.key == "recordStyle", onSlider,
        )
    }

    private fun bindRecordSpeed(
        source: View,
        option: QqPlayerStyleOption,
        settings: QqPlayerStyleSettings,
        onChoice: (QqPlayerStyleChoice) -> Unit,
    ) {
        val choices = option.groups.flatten()
        source.findViewById<BtnTypeSeekbarView>(R.id.n91).apply {
            u(choices.map(QqPlayerStyleChoice::text))
            setSelectedIndex(choices.indexOfFirst {
                it.id == settings.recordDurationMs.toString()
            }.coerceAtLeast(0))
            setDisableTextList(emptyList())
            setIndexChangeListener(object : BtnTypeSeekbarView.b {
                override fun a(index: Int) {
                    choices.getOrNull(index)?.let(onChoice)
                }

                override fun b(index: Int) = Unit
            })
            contentDescription = option.title
        }
    }

    private fun bindChoices(
        list: RecyclerView,
        option: QqPlayerStyleOption,
        choices: List<QqPlayerStyleChoice>,
        settings: QqPlayerStyleSettings,
        onChoice: (QqPlayerStyleChoice) -> Unit,
    ) {
        list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        list.addItemDecoration(if (option.key == "background") QqBackgroundItemDecoration() else k0())
        list.adapter = QqSourceStyleChoiceAdapter(option, choices, settings, onChoice)
    }

    private fun bindSlider(
        row: ViewGroup,
        label: String,
        value: Float,
        key: String,
        visible: Boolean,
        onSlider: (String, Float, Boolean) -> Unit,
    ) {
        row.visibility = if (visible) VISIBLE else GONE
        if (!visible) return
        row.findViewById<TextView>(R.id.epl).text = label
        val inverted = key != "backgroundBlur"
        row.findViewById<NiftySlider>(R.id.jnb).apply {
            valueFrom = 0f
            valueTo = 1f
            effect = ColorPickEffect(this).also {
                it.updateColors(intArrayOf(Color.WHITE, context.getColor(R.color.defined_record_seekbar_end_color)))
            }
            setThumbTintList(ColorStateList.valueOf(Color.WHITE))
            setThumbStrokeColor(ColorStateList.valueOf(Color.WHITE))
            setValue(if (inverted) 1f - value.coerceIn(0f, 1f) else value.coerceIn(0f, 1f), false)
            addOnValueChangeListener { _, changed, fromUser ->
                if (fromUser) onSlider(key, if (inverted) 1f - changed else changed, false)
            }
            addOnSliderTouchStopListener {
                onSlider(key, if (inverted) 1f - it.value else it.value, true)
            }
            contentDescription = label
        }
    }

    private companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}

/** BasePDEditPicHolder / PDEditColorHolder / PDEditBgHolder 的宿主数据适配。 */
private class QqSourceStyleChoiceAdapter(
    private val option: QqPlayerStyleOption,
    private val choices: List<QqPlayerStyleChoice>,
    private val settings: QqPlayerStyleSettings,
    private val onChoice: (QqPlayerStyleChoice) -> Unit,
) : RecyclerView.Adapter<QqSourceStyleChoiceAdapter.Holder>() {
    private val isBackground = option.key == "background"
    private val isColor = option.key == "highlightColor" || option.key == "textAndButtonColor"
    private val layout = when {
        isBackground -> R.layout.qq_source_player_style_background_item
        isColor -> R.layout.qq_source_player_style_color_item
        else -> R.layout.qq_source_player_style_picture_item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(layout, parent, false))

    override fun getItemCount(): Int = choices.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val choice = choices[position]
        holder.selected.visibility =
            if (settings.selectedId(option.key) == choice.id) View.VISIBLE else View.GONE
        holder.itemView.contentDescription = choice.text.ifBlank { choice.id }
        holder.itemView.setOnClickListener { onChoice(choice) }
        holder.image.alpha = 1f
        holder.image.scaleType = ImageView.ScaleType.FIT_CENTER
        holder.image.setImageDrawable(null)

        val customPath = when {
            option.key == "background" && choice.id == "customBackground" &&
                settings.backgroundId == choice.id -> settings.customBackgroundPath
            option.key == "recordBase" && choice.id == "customBackground" &&
                settings.recordBaseId == choice.id -> settings.customRecordBasePath
            else -> ""
        }
        val color = choice.color.takeIf { it.startsWith('#') }
            ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
        when {
            customPath.isNotBlank() -> {
                holder.image.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.image.setImageBitmap(BitmapFactory.decodeFile(customPath))
            }
            color != null -> holder.image.setImageDrawable(ColorDrawable(color))
            qqStyleDrawable(holder.itemView.context, choice.previewFile) != 0 ->
                holder.image.setImageResource(
                    qqStyleDrawable(holder.itemView.context, choice.previewFile),
                )
            !isBackground && !isColor -> holder.image.setImageResource(R.drawable.player_edit_default_pic)
        }
    }

    inner class Holder(root: View) : RecyclerView.ViewHolder(root) {
        val selected: ImageView = root.findViewById(R.id.ds0)
        val image: ImageView = root.findViewById(if (isBackground || isColor) R.id.dow else R.id.dr4)
        init {
            if (option.key == "stickers") image.setBackgroundResource(R.drawable.player_edit_sticker_background)
            if (!isBackground && !isColor) {
                val (widthRatio, aspectRatio) = when (option.key) {
                    "recordBase", "stickers" -> .25641027f to 1f
                    "recordStyle" -> .23076923f to 1f
                    "backgroundLightEffectStyle", "foregroundLightEffectStyle" ->
                        .37435898f to 2.4333334f
                    else -> .37435898f to 1.825f
                }
                val width = (root.resources.displayMetrics.widthPixels * widthRatio).roundToInt()
                image.layoutParams.width = width
                image.layoutParams.height = (width / aspectRatio).roundToInt()
            }
        }
    }
}

/** PDEditBgHolder.C26494c 原间距算法，仅把 dp 工具接到宿主。 */
private class QqBackgroundItemDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = j0.a(16f)
            outRect.right = j0.a(3f)
        } else {
            outRect.left = j0.a(5f)
            outRect.right = j0.a(3f)
        }
    }
}

private fun exactStyle(value: Int): Int =
    View.MeasureSpec.makeMeasureSpec(value, View.MeasureSpec.EXACTLY)

private fun atMostStyle(value: Int): Int =
    View.MeasureSpec.makeMeasureSpec(value, View.MeasureSpec.AT_MOST)

private fun unspecifiedStyle(): Int =
    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

private fun View.dp(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()
