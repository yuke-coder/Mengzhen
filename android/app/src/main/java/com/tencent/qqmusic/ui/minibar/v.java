package com.tencent.qqmusic.ui.minibar;

import android.graphics.Bitmap;
import com.tencent.qqmusicplayerprocess.songinfo.SongInfo;

public interface v {
    void a(SongInfo song, Bitmap bitmap, String url, boolean fromCache);
    void b(Bitmap bitmap);
}
