package com.tencent.qqmusic.business.playernew.fxeffect.custom.data;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes19.dex */
public class a {
    private final int a;

    public a(int[] iArr) {
        int[] iArr2 = new int[1];
        GLES20.glGenBuffers(1, iArr2, 0);
        int i = iArr2[0];
        if (i == 0) {
            throw new RuntimeException("Could not create a new index buffer object.");
        }
        this.a = i;
        GLES20.glBindBuffer(34963, i);
        IntBuffer intBufferPut = ByteBuffer.allocateDirect(iArr.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer().put(iArr);
        intBufferPut.position(0);
        GLES20.glBufferData(34963, intBufferPut.capacity() * 4, intBufferPut, 35044);
        GLES20.glBindBuffer(34963, 0);
    }

    public int a() {
        return this.a;
    }
}
