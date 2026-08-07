package com.mengzhen.app.audio

/**
 * 梦枕对喜马拉雅 SoundEffectItem 的本地音频数据适配。
 *
 * 面板及状态切换沿用喜马拉雅 9.4.95.3；DSP 由 Android 平台 AudioEffect
 * 在当前 ExoPlayer audioSessionId 上执行，避免向本地音频伪造云端音效数据。
 */
enum class PlaybackSoundEffect(
    val storageValue: String,
    val displayName: String,
    val description: String,
) {
    ORIGINAL(
        storageValue = "original",
        displayName = "原声",
        description = "不使用音效，按音频原始效果播放",
    ),
    VOICE(
        storageValue = "voice",
        displayName = "人声增强",
        description = "突出人声频段，对白更清晰",
    ),
    BASS(
        storageValue = "bass",
        displayName = "低音增强",
        description = "增强低频表现，声音更有力度",
    ),
    SURROUND(
        storageValue = "surround",
        displayName = "空间环绕",
        description = "扩展声场，建议佩戴耳机使用",
    ),
    LOUDNESS(
        storageValue = "loudness",
        displayName = "响度增强",
        description = "提升整体响度，弱音内容更易听清",
    );

    companion object {
        fun fromStorage(value: String?): PlaybackSoundEffect =
            entries.firstOrNull { it.storageValue == value } ?: ORIGINAL
    }
}
