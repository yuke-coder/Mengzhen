package com.tencent.qqmusic.business.playernew.fxeffect.custom;

import android.opengl.GLES20;
import com.tencent.qqmusic.business.playernew.fxeffect.custom.data.a;
import com.tencent.qqmusic.business.playernew.fxeffect.custom.data.b;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes19.dex */
public class e {
    private final int a;
    private final int b;
    private final int c = b();
    private final b d = new b(e());
    private final a e = new a(c());

    public e(int i, int i2) {
        this.a = i;
        this.b = i2;
    }

    private int b() {
        return (this.a - 1) * (this.b - 1) * 2 * 3;
    }

    private int[] c() {
        int[] iArr = new int[this.c];
        int i = 0;
        for (int i2 = 0; i2 < this.b - 1; i2++) {
            int i3 = 0;
            while (true) {
                int i4 = this.a;
                if (i3 < i4 - 1) {
                    int i5 = (i2 * i4) + i3;
                    int i6 = (i2 * i4) + i3 + 1;
                    int i7 = i2 + 1;
                    int i8 = (i7 * i4) + i3;
                    int i9 = (i7 * i4) + i3 + 1;
                    int i10 = i + 1;
                    iArr[i] = i5;
                    int i11 = i10 + 1;
                    iArr[i10] = i8;
                    int i12 = i11 + 1;
                    iArr[i11] = i6;
                    int i13 = i12 + 1;
                    iArr[i12] = i6;
                    int i14 = i13 + 1;
                    iArr[i13] = i8;
                    i = i14 + 1;
                    iArr[i14] = i9;
                    i3++;
                }
            }
        }
        return iArr;
    }

    private float[] e() {
        float[] fArr = new float[this.a * this.b * 4];
        int i = 0;
        for (int i2 = 0; i2 < this.b; i2++) {
            int i3 = 0;
            while (true) {
                if (i3 < this.a) {
                    float f = i3 / (this.a - 1);
                    float f2 = i2 / (this.b - 1);
                    int i4 = i + 1;
                    fArr[i] = (f * 2.0f) - 1.0f;
                    int i5 = i4 + 1;
                    fArr[i4] = 1.0f - (2.0f * f2);
                    int i6 = i5 + 1;
                    fArr[i5] = f;
                    i = i6 + 1;
                    fArr[i6] = f2;
                    i3++;
                }
            }
        }
        return fArr;
    }

    public void a(com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a aVar) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1297] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(aVar, this, 178378).isSupported) {
            this.d.a(0, aVar.b(), 2, 16);
            this.d.a(8, aVar.c(), 2, 16);
        }
    }

    public void d() {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1298] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 178389).isSupported) {
            GLES20.glBindBuffer(34963, this.e.a());
            GLES20.glDrawElements(4, this.c, 5125, 0);
            GLES20.glBindBuffer(34963, 0);
        }
    }
}
