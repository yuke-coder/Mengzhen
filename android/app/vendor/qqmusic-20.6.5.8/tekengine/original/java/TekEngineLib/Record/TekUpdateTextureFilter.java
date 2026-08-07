package TekEngineLib.Record;

import TekEngineLib.State.TekProxyLog;
import android.opengl.GLES20;
import android.util.Log;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekUpdateTextureFilter {
    private static final String TAG = "TekUpdateTextureFilter";
    private Buffer texBuffer;
    private Buffer vertexBuffer;
    private int program = -1;
    private int vertexShaderId = -1;
    private int fragmentShaderId = -1;

    public static void checkGLError(String str, String str2) {
        int iGlGetError = GLES20.glGetError();
        if (iGlGetError == 0) {
            return;
        }
        Log.e(str, str2 + ": glError " + iGlGetError);
        throw new RuntimeException(str2 + ": glError " + iGlGetError);
    }

    private int loadShader(int i, String str) {
        int iGlCreateShader = GLES20.glCreateShader(i);
        GLES20.glShaderSource(iGlCreateShader, str);
        GLES20.glCompileShader(iGlCreateShader);
        return iGlCreateShader;
    }

    public void flush(int i, int i2, int i3, long j) {
        if (i <= 0) {
            TekProxyLog.e(TAG, "Invalid texture ID: " + i);
            return;
        }
        GLES20.glUseProgram(this.program);
        int iGlGetAttribLocation = GLES20.glGetAttribLocation(this.program, "aPosition");
        GLES20.glEnableVertexAttribArray(iGlGetAttribLocation);
        GLES20.glVertexAttribPointer(iGlGetAttribLocation, 2, 5126, false, 0, this.vertexBuffer);
        int iGlGetAttribLocation2 = GLES20.glGetAttribLocation(this.program, "aTexCoord");
        GLES20.glEnableVertexAttribArray(iGlGetAttribLocation2);
        GLES20.glVertexAttribPointer(iGlGetAttribLocation2, 2, 5126, false, 0, this.texBuffer);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, i);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(this.program, "uTexture"), 0);
        GLES20.glViewport(0, 0, i2, i3);
        GLES20.glDrawArrays(5, 0, 4);
        try {
            checkGLError(TAG, "flush");
        } catch (Exception e) {
            TekProxyLog.e(TAG, "flush error: " + e.getMessage());
        }
    }

    public void initGLContext() {
        this.program = GLES20.glCreateProgram();
        this.vertexShaderId = loadShader(35633, "attribute vec4 aPosition;\nattribute vec2 aTexCoord;\nvarying vec2 vTexCoord;\nvoid main() {\n    gl_Position = aPosition;\n    vTexCoord = aTexCoord;\n}");
        this.fragmentShaderId = loadShader(35632, "precision mediump float;\nvarying vec2 vTexCoord;\nuniform sampler2D uTexture;\nvoid main() {\n    gl_FragColor = texture2D(uTexture, vTexCoord);\n}");
        GLES20.glAttachShader(this.program, this.vertexShaderId);
        GLES20.glAttachShader(this.program, this.fragmentShaderId);
        GLES20.glLinkProgram(this.program);
        this.vertexBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer().put(new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f}).position(0);
        this.texBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer().put(new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f}).position(0);
    }

    public void release() {
        int i = this.program;
        if (i != -1) {
            GLES20.glDeleteProgram(i);
        }
        int i2 = this.vertexShaderId;
        if (i2 != -1) {
            GLES20.glDeleteShader(i2);
        }
        int i3 = this.fragmentShaderId;
        if (i3 != -1) {
            GLES20.glDeleteShader(i3);
        }
    }
}
