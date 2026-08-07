@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.mengzhen.app.ui.screens

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mengzhen.app.audio.healing.QqMusicHealingScene

/**
 * QQ 音乐 20.6.5.8 BWVideoBackgroundView 的 Android 宿主适配。
 *
 * 保留源实现的 cover 裁切、静音循环、封面占位和首帧 1 秒切换；只把
 * Kuikly 的 TPVideoView 宿主换成本项目已经使用的 Media3。
 */
internal class QqMusicHealingVideoBackgroundView(context: Context) : FrameLayout(context) {
    private val video = PlayerView(context).apply {
        useController = false
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        setShutterBackgroundColor(Color.TRANSPARENT)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val placeholder = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private var scene: QqMusicHealingScene? = null
    private var player: ExoPlayer? = null

    init {
        addView(
            video,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        addView(
            placeholder,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        visibility = GONE
    }

    fun bind(value: QqMusicHealingScene?) {
        if (scene == value) {
            updatePlayState()
            return
        }
        scene = value
        placeholder.animate().cancel()
        placeholder.alpha = 1f
        if (value == null) {
            visibility = GONE
            releasePlayer()
            return
        }
        visibility = VISIBLE
        placeholder.setImageResource(value.coverRes)
        prepare(value)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scene?.let(::prepare)
    }

    override fun onDetachedFromWindow() {
        releasePlayer()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updatePlayState()
    }

    private fun prepare(value: QqMusicHealingScene) {
        if (!isAttachedToWindow) return
        releasePlayer()
        val next = ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    placeholder.animate()
                        .alpha(0f)
                        .setDuration(1_000L)
                        .start()
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    placeholder.animate().cancel()
                    placeholder.alpha = 1f
                }
            })
            setMediaItem(
                MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(value.videoRes)),
            )
            prepare()
        }
        player = next
        video.player = next
        updatePlayState()
    }

    private fun updatePlayState() {
        player?.playWhenReady = isAttachedToWindow && windowVisibility == View.VISIBLE
    }

    private fun releasePlayer() {
        video.player = null
        player?.release()
        player = null
    }
}
