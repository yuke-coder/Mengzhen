package com.mengzhen.app.audio

/**
 * QQ 音乐 20.6.5.8 SwitchPlayModeUseCase 的三种普通播放模式。
 * 数值与点击轮换顺序保持源实现：103 -> 101 -> 105 -> 103。
 */
enum class QqMusicPlaybackMode(val sourceValue: Int) {
    LIST_REPEAT(103),
    SINGLE_REPEAT(101),
    SHUFFLE(105),
    ;

    fun next(): QqMusicPlaybackMode = when (this) {
        LIST_REPEAT -> SINGLE_REPEAT
        SINGLE_REPEAT -> SHUFFLE
        SHUFFLE -> LIST_REPEAT
    }

    companion object {
        fun fromSourceValue(value: Int): QqMusicPlaybackMode =
            entries.firstOrNull { it.sourceValue == value } ?: LIST_REPEAT
    }
}

/** QQ 音乐 20.6.5.8 ShufflePlayAdjustDialog 的三种随机偏好。 */
enum class QqMusicShufflePreference(val sourceValue: Int) {
    RECENT_FREQUENT(1),
    DEFAULT(2),
    FRESH_EXPLORE(3),
    ;

    companion object {
        fun fromSourceValue(value: Int): QqMusicShufflePreference =
            entries.firstOrNull { it.sourceValue == value } ?: DEFAULT
    }
}
