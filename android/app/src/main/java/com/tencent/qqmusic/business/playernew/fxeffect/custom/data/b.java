package com.tencent.qqmusic.business.playernew.fxeffect.custom.data;

import android.opengl.GLES20;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes19.dex */
public class b {
    private final int a;

    public b(float[] fArr) {
        int[] iArr = new int[1];
        GLES20.glGenBuffers(1, iArr, 0);
        int i = iArr[0];
        if (i == 0) {
            throw new RuntimeException("Could not create a new vertex buffer object.");
        }
        this.a = i;
        GLES20.glBindBuffer(34962, i);
        FloatBuffer floatBufferPut = ByteBuffer.allocateDirect(fArr.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(fArr);
        floatBufferPut.position(0);
        GLES20.glBufferData(34962, floatBufferPut.capacity() * 4, floatBufferPut, 35044);
        GLES20.glBindBuffer(34962, 0);
    }

    public void a(int i, int i2, int i3, int i4) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1287] >> 3) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4)}, this, 178300).isSupported) {
            GLES20.glBindBuffer(34962, this.a);
            GLES20.glVertexAttribPointer(i2, i3, 5126, false, i4, i);
            GLES20.glEnableVertexAttribArray(i2);
            GLES20.glBindBuffer(34962, 0);
        }
    }
}
