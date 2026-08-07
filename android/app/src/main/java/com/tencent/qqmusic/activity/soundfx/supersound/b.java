package com.tencent.qqmusic.activity.soundfx.supersound;

import com.tencent.qqmusic.activity.soundfx.supersound.spectrumstrategy.j;
import java.lang.ref.WeakReference;

/** Compile bridge for the dormant QQ private audio-feature branch. */
public final class b {
    private static final b INSTANCE = new b();

    public static b m() { return INSTANCE; }
    public void t(WeakReference<j> strategy) {}
    public void v(j strategy) {}
}
