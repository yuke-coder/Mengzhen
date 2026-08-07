package TekEngineLib.Render;

import TekEngineLib.State.TekErrorCode;
import TekEngineLib.State.TekLog;
import TekEngineLib.State.TekProxyLog;
import android.opengl.GLES20;
import com.tencent.qqmusicplayerprocess.userdata.RecentPlayFolderSongInfoTable;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class c {
    static float[] u = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
    static float[] v = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    static String w = "TEK TekViewRender";
    private FloatBuffer c;
    private FloatBuffer d;
    private int e;
    private int f;
    private int g;
    private int h;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private final String f19a = "attribute vec4 position;attribute vec4 inputTextureCoordinate;varying mediump vec2 textureCoordinate;void main() {  gl_Position = position;textureCoordinate = inputTextureCoordinate.xy;}";
    private final String b = "varying mediump vec2 textureCoordinate;uniform sampler2D inputImageTexture;void main() {  gl_FragColor = texture2D(inputImageTexture, textureCoordinate);}";
    private final int i = u.length / 2;
    private final int j = v.length / 2;
    private final int k = 8;
    private int l = 0;
    private int m = 0;
    private int n = 0;
    private int o = 0;
    private int p = 0;
    private int q = 0;
    private int r = 0;
    private int s = 0;
    private boolean t = false;

    private void c(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        float f9;
        float f10;
        if (f <= 1.0E-4d || f2 <= 1.0E-4d || f4 <= 1.0E-4d || f3 <= 1.0E-4d || f5 <= 1.0E-4d || f6 <= 1.0E-4d) {
            l();
            return;
        }
        l();
        float f11 = 0.0f;
        if (f4 / f3 > f2 / f) {
            f9 = f / f3;
            f10 = ((f4 * f9) - f2) / 2.0f;
        } else {
            float f12 = f2 / f4;
            f11 = ((f3 * f12) - f) / 2.0f;
            f9 = f12;
            f10 = 0.0f;
        }
        float f13 = f5 * f9;
        float f14 = f6 * f9;
        float f15 = (f7 * f9) - f11;
        float f16 = (f2 - ((f8 * f9) - f10)) - f14;
        float f17 = f14 + f16;
        float f18 = (float) ((((double) (f15 / f)) * 2.0d) - 1.0d);
        float f19 = (float) ((((double) ((f13 + f15) / f)) * 2.0d) - 1.0d);
        float f20 = (float) ((((double) (f16 / f2)) * 2.0d) - 1.0d);
        float f21 = (float) ((((double) (f17 / f2)) * 2.0d) - 1.0d);
        float[] fArr = u;
        fArr[0] = f18;
        fArr[1] = f21;
        fArr[2] = f19;
        fArr[3] = f21;
        fArr[4] = f18;
        fArr[5] = f20;
        fArr[6] = f19;
        fArr[7] = f20;
        FloatBuffer floatBuffer = this.c;
        if (floatBuffer != null) {
            floatBuffer.put(fArr);
            this.c.position(0);
        }
    }

    private void l() {
        float[] fArr = u;
        fArr[0] = -1.0f;
        fArr[1] = 1.0f;
        fArr[2] = 1.0f;
        fArr[3] = 1.0f;
        fArr[4] = -1.0f;
        fArr[5] = -1.0f;
        fArr[6] = 1.0f;
        fArr[7] = -1.0f;
    }

    public int a(int i, String str) {
        int iGlCreateShader = GLES20.glCreateShader(i);
        if (iGlCreateShader == 0) {
            return 0;
        }
        GLES20.glShaderSource(iGlCreateShader, str);
        GLES20.glCompileShader(iGlCreateShader);
        return iGlCreateShader;
    }

    public void b() {
        TekProxyLog.i(w, "destory" + Thread.currentThread().getName());
        int i = this.l;
        if (i > 0) {
            GLES20.glDeleteShader(i);
            this.l = 0;
        }
        int i2 = this.m;
        if (i2 > 0) {
            GLES20.glDeleteShader(i2);
            this.m = 0;
        }
        int i3 = this.h;
        if (i3 > 0) {
            GLES20.glDeleteProgram(i3);
            this.h = 0;
        }
    }

    public void d(int i) {
        GLES20.glUniform1i(this.g, i);
    }

    public void e(int i, int i2) {
        GLES20.glBindFramebuffer(36160, 0);
        GLES20.glViewport(0, 0, i, i2);
    }

    public void f(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        if (i != this.n || i2 != this.o || i3 != this.p || i4 != this.q || i5 != this.r || i6 != this.s) {
            c(i, i2, i3, i4, i5, i6, i7, i8);
        }
        if (i() != 0) {
            return;
        }
        this.n = i;
        this.o = i2;
        this.p = i3;
        this.q = i4;
        this.r = i5;
        this.s = i6;
        GLES20.glBindFramebuffer(36160, 0);
        GLES20.glViewport(0, 0, i, i2);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(16384);
        GLES20.glUseProgram(this.h);
        GLES20.glEnableVertexAttribArray(this.e);
        GLES20.glVertexAttribPointer(this.e, 2, 5126, false, 8, (Buffer) this.c);
        GLES20.glEnableVertexAttribArray(this.f);
        GLES20.glVertexAttribPointer(this.f, 2, 5126, false, 8, (Buffer) this.d);
    }

    public void g() {
        GLES20.glDisableVertexAttribArray(this.e);
        GLES20.glDisableVertexAttribArray(this.f);
    }

    public void h(int i, int i2) {
        TekLog.write(w, "clearView");
        GLES20.glBindFramebuffer(36160, 0);
        GLES20.glViewport(0, 0, i, i2);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(16384);
    }

    public int i() {
        if (this.t) {
            return 0;
        }
        ByteBuffer byteBufferAllocateDirect = ByteBuffer.allocateDirect(u.length * 4);
        byteBufferAllocateDirect.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferAsFloatBuffer = byteBufferAllocateDirect.asFloatBuffer();
        this.c = floatBufferAsFloatBuffer;
        floatBufferAsFloatBuffer.put(u);
        this.c.position(0);
        ByteBuffer byteBufferAllocateDirect2 = ByteBuffer.allocateDirect(v.length * 4);
        byteBufferAllocateDirect2.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferAsFloatBuffer2 = byteBufferAllocateDirect2.asFloatBuffer();
        this.d = floatBufferAsFloatBuffer2;
        floatBufferAsFloatBuffer2.put(v);
        this.d.position(0);
        this.l = a(35633, "attribute vec4 position;attribute vec4 inputTextureCoordinate;varying mediump vec2 textureCoordinate;void main() {  gl_Position = position;textureCoordinate = inputTextureCoordinate.xy;}");
        int iA = a(35632, "varying mediump vec2 textureCoordinate;uniform sampler2D inputImageTexture;void main() {  gl_FragColor = texture2D(inputImageTexture, textureCoordinate);}");
        this.m = iA;
        if (this.l == 0 || iA == 0) {
            return TekErrorCode.VIEW_INIT_LOADSHADER_ERROR;
        }
        int iGlCreateProgram = GLES20.glCreateProgram();
        this.h = iGlCreateProgram;
        GLES20.glAttachShader(iGlCreateProgram, this.l);
        GLES20.glAttachShader(this.h, this.m);
        GLES20.glLinkProgram(this.h);
        GLES20.glUseProgram(this.h);
        this.g = GLES20.glGetUniformLocation(this.h, "inputImageTexture");
        this.e = GLES20.glGetAttribLocation(this.h, RecentPlayFolderSongInfoTable.KEY_USER_FOLDER_POSITION);
        this.f = GLES20.glGetAttribLocation(this.h, "inputTextureCoordinate");
        if (GLES20.glGetError() != 0) {
            return TekErrorCode.VIEW_INIT_GLERROR;
        }
        this.t = true;
        return 0;
    }

    public void j(int i, int i2) {
        GLES20.glViewport(0, 0, i, i2);
    }

    public void k() {
        GLES20.glDrawArrays(5, 0, 4);
    }
}
