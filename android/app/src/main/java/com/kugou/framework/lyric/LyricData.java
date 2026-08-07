package com.kugou.framework.lyric;

/**
 * Compile-time contract used by QQ Music's recovered TEK lyric branch.
 * The foreground player effect does not submit lyric data.
 */
public abstract class LyricData {
    public abstract int getLyricType();

    public abstract String[][] getWords();

    public abstract long[] getRowBeginTime();

    public abstract long[] getRowDelayTime();

    public abstract long[][] getWordBeginTime();

    public abstract long[][] getWordDelayTime();
}
