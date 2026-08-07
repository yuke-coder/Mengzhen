package com.mengzhen.app.audio.healing

import android.content.Context
import android.net.Uri
import com.mengzhen.app.R
import com.mengzhen.app.data.model.TaskAudio

enum class QqMusicHealingScene(
    val sceneName: String,
    val sceneIndex: Int,
    val title: String,
    val subtitle: String,
    val color: Long,
    val coverRes: Int,
    val videoRes: Int,
) {
    SKY(
        sceneName = "sky",
        sceneIndex = 0,
        title = "星空",
        subtitle = "sky",
        color = 0xFF07255B,
        coverRes = R.drawable.qq_healing_sky,
        videoRes = R.raw.qq_healing_sky,
    ),
    OCEAN(
        sceneName = "ocean",
        sceneIndex = 1,
        title = "海洋",
        subtitle = "ocean",
        color = 0xFF228473,
        coverRes = R.drawable.qq_healing_ocean,
        videoRes = R.raw.qq_healing_ocean,
    ),
    FOREST(
        sceneName = "forest",
        sceneIndex = 2,
        title = "雨林",
        subtitle = "forest",
        color = 0xFF154B31,
        coverRes = R.drawable.qq_healing_forest,
        videoRes = R.raw.qq_healing_forest,
    );

    fun sourceUri(durationMs: Long = DEFAULT_DURATION_MS): String =
        Uri.Builder()
            .scheme(SCHEME)
            .authority("scene")
            .appendPath(sceneIndex.toString())
            .appendQueryParameter("duration", durationMs.toString())
            .build()
            .toString()

    fun asTaskAudio(context: Context): TaskAudio = TaskAudio(
        id = "qq_healing_$sceneName",
        name = title,
        duration = DEFAULT_DURATION_MS / 1_000L,
        localUri = sourceUri(),
        mimeType = "audio/wav",
        sourceType = "qqmusic_healing",
        sourceId = sceneName,
        artist = "助眠音乐",
        artworkUri = "android.resource://${context.packageName}/$coverRes",
    )

    companion object {
        const val SCHEME = "qqhealing"
        const val DEFAULT_DURATION_MS = 30L * 60_000L

        fun fromSourceId(value: String?): QqMusicHealingScene? =
            entries.firstOrNull { it.sceneName == value }
    }
}
