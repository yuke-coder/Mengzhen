package com.tencent.qqmusic.business.playernew.fxeffect.custom.programs;

import android.opengl.GLES20;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes19.dex */
public class a extends b {
    private final int b;
    private final int c;
    private final int d;
    private final int e;
    private final int f;
    private final int g;
    private final int h;
    private final int i;
    private final int j;
    private final int k;
    private final int l;
    private final int m;
    private final int n;
    private final int o;
    private final int p;
    private final int q;
    private final int r;
    private final int s;
    private final int t;
    private final int u;
    private final int v;
    private final int w;

    public a(String str, String str2) {
        super(str, str2);
        this.b = GLES20.glGetUniformLocation(this.a, "matrix");
        this.c = GLES20.glGetUniformLocation(this.a, "isForSurfaceView");
        this.d = GLES20.glGetUniformLocation(this.a, "bgColor");
        this.e = GLES20.glGetUniformLocation(this.a, "firstColor");
        this.f = GLES20.glGetUniformLocation(this.a, "secondColor");
        this.g = GLES20.glGetUniformLocation(this.a, "spectrumArray");
        this.h = GLES20.glGetUniformLocation(this.a, "showSpectrum");
        this.i = GLES20.glGetUniformLocation(this.a, "spectrum2Array");
        this.j = GLES20.glGetUniformLocation(this.a, "aspectRatio");
        this.k = GLES20.glGetUniformLocation(this.a, "cropMinX");
        this.l = GLES20.glGetUniformLocation(this.a, "cropMaxX");
        this.m = GLES20.glGetUniformLocation(this.a, "cropMinY");
        this.n = GLES20.glGetUniformLocation(this.a, "cropMaxY");
        this.o = GLES20.glGetUniformLocation(this.a, "duration");
        this.p = GLES20.glGetUniformLocation(this.a, "time");
        this.q = GLES20.glGetUniformLocation(this.a, "expand");
        this.r = GLES20.glGetUniformLocation(this.a, "volume");
        this.s = GLES20.glGetUniformLocation(this.a, "volumeIncrease");
        this.t = GLES20.glGetUniformLocation(this.a, "entry");
        this.u = GLES20.glGetUniformLocation(this.a, "bottomColor");
        this.v = GLES20.glGetAttribLocation(this.a, "position");
        this.w = GLES20.glGetAttribLocation(this.a, "inputTextureCoordinate");
    }

    public int b() {
        return this.v;
    }

    public int c() {
        return this.w;
    }

    public void d(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1302] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178420).isSupported) {
            GLES20.glUniform1f(this.j, f);
        }
    }

    public void e(float[] fArr) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1296] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 178371).isSupported) {
            GLES20.glUniform4fv(this.d, 1, fArr, 0);
        }
    }

    public void f(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1305] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178443).isSupported) {
            GLES20.glUniform1f(this.l, f);
        }
    }

    public void g(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1307] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178459).isSupported) {
            GLES20.glUniform1f(this.n, f);
        }
    }

    public void h(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1303] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178432).isSupported) {
            GLES20.glUniform1f(this.k, f);
        }
    }

    public void i(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1306] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178450).isSupported) {
            GLES20.glUniform1f(this.m, f);
        }
    }

    public void j(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1308] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178469).isSupported) {
            GLES20.glUniform1f(this.o, f);
        }
    }

    public void k(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1310] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178482).isSupported) {
            GLES20.glUniform1f(this.q, f);
        }
    }

    public void l(float[] fArr) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1297] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 178379).isSupported) {
            GLES20.glUniform3fv(this.e, 1, fArr, 0);
        }
    }

    public void m(float[] fArr) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1301] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 178413).isSupported) {
            GLES20.glUniform1fv(this.i, fArr.length, fArr, 0);
        }
    }

    public void n(int i) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1294] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 178356).isSupported) {
            GLES20.glUniform1i(this.c, i);
        }
    }

    public void o(float[] fArr) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1291] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 178335).isSupported) {
            GLES20.glUniformMatrix4fv(this.b, 1, false, fArr, 0);
        }
    }

    public void p(float[] fArr) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1297] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 178383).isSupported) {
            GLES20.glUniform3fv(this.f, 1, fArr, 0);
        }
    }

    public void q(int i) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1299] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 178394).isSupported) {
            GLES20.glUniform1i(this.h, i);
        }
    }

    public void r(float[] fArr) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1300] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 178402).isSupported) {
            GLES20.glUniform1fv(this.g, fArr.length, fArr, 0);
        }
    }

    public void s(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1309] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178475).isSupported) {
            GLES20.glUniform1f(this.p, f);
        }
    }

    public void t(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1311] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178492).isSupported) {
            GLES20.glUniform1f(this.r, f);
        }
    }

    public void u(float f) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1312] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 178504).isSupported) {
            GLES20.glUniform1f(this.s, f);
        }
    }
}
