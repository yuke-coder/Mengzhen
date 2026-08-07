package com.tencent.qqmusic;

import com.tencent.qqmusic.ui.MusicUIConfigure;

public class InstanceManager {
    private static final MusicUIConfigure MUSIC_UI = new MusicUIConfigure();

    public static InstanceManager getInstance(int type) {
        return MUSIC_UI;
    }
}
