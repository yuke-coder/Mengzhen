package com.tencent.qqmusic.business.playernew.fxeffect.custom.programs;

import android.opengl.GLES20;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes19.dex */
public abstract class b {
    protected final int a;

    protected b(String str, String str2) {
        this.a = com.tencent.qqmusic.business.playernew.fxeffect.custom.util.b.a(str, str2);
    }

    public void a() {
        GLES20.glUseProgram(this.a);
    }
}
