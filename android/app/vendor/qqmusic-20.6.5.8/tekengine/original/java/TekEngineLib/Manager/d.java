package TekEngineLib.Manager;

import TekEngineLib.State.TekErrorCode;
import TekEngineLib.State.TekLog;
import TekEngineLib.State.TekProxyLog;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class d {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private EGL10 f12a;
    private EGLDisplay b = EGL10.EGL_NO_DISPLAY;
    private EGLSurface c;
    private EGLSurface d;
    private EGLContext e;
    private int f;
    private int g;
    private boolean h;
    private int i;
    EGLConfig[] j;

    public static class a {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        private final boolean f13a;
        private final int b;

        public a(boolean z, int i) {
            this.f13a = z;
            this.b = i;
        }

        public String a() {
            if (!this.f13a || this.b == 0) {
                return "深度缓冲: 未启用";
            }
            return "深度缓冲: 已启用 (" + this.b + "位)";
        }

        public String toString() {
            return a();
        }
    }

    public d() {
        EGLSurface eGLSurface = EGL10.EGL_NO_SURFACE;
        this.c = eGLSurface;
        this.d = eGLSurface;
        this.e = EGL10.EGL_NO_CONTEXT;
        this.f = 0;
        this.g = 0;
        this.h = false;
        this.i = 0;
        TekLog.write("TEK TekGLContext", "construct.");
    }

    public int a(SurfaceTexture surfaceTexture) {
        EGLContext eGLContext;
        TekLog.write("TEK TekGLContext", "createRenderSurface.");
        if (this.f12a == null || this.b == null) {
            TekLog.write("TEK TekGLContext", "createRenderSurface mEgl == null.");
            return TekErrorCode.CONTEXT_CREATESURFACE_EGL_NONE;
        }
        if (this.c != EGL10.EGL_NO_SURFACE) {
            TekLog.write("TEK TekGLContext", "createRenderSurface != EGL10.EGL_NO_SURFACE.");
            this.f12a.eglDestroySurface(this.b, this.c);
            this.c = EGL10.EGL_NO_SURFACE;
        }
        EGLSurface eGLSurface = this.d;
        if (eGLSurface != EGL10.EGL_NO_SURFACE) {
            this.f12a.eglDestroySurface(this.b, eGLSurface);
            this.d = EGL10.EGL_NO_SURFACE;
        }
        EGLSurface eGLSurfaceEglCreateWindowSurface = this.f12a.eglCreateWindowSurface(this.b, this.j[0], surfaceTexture, null);
        this.c = eGLSurfaceEglCreateWindowSurface;
        if (eGLSurfaceEglCreateWindowSurface != EGL10.EGL_NO_SURFACE && (eGLContext = this.e) != EGL10.EGL_NO_CONTEXT) {
            if (this.f12a.eglMakeCurrent(this.b, eGLSurfaceEglCreateWindowSurface, eGLSurfaceEglCreateWindowSurface, eGLContext)) {
                return 0;
            }
            TekLog.write("TEK TekGLContext", "createRenderSurface eglMakeCurrent error.");
            return TekErrorCode.CONTEXT_CREATESURFACE_MAKECURRENT_ERROR;
        }
        if (this.f12a.eglGetError() == 12299) {
            Log.i("TEK TekGLContext", "createRenderSurface EGL_BAD_NATIVE_WINDOW.");
        }
        TekLog.write("TEK TekGLContext", "createRenderSurface other error.");
        if (this.c == EGL10.EGL_NO_SURFACE) {
            TekLog.write("TEK TekGLContext", "createRenderSurface other error 11.");
        }
        if (this.e == EGL10.EGL_NO_CONTEXT) {
            TekLog.write("TEK TekGLContext", "createRenderSurface other error 22.");
        }
        if (this.c != EGL10.EGL_NO_SURFACE) {
            TekLog.write("TEK TekGLContext", "createRenderSurface != EGL10.EGL_NO_SURFACE.");
            this.f12a.eglDestroySurface(this.b, this.c);
            this.c = EGL10.EGL_NO_SURFACE;
        }
        EGLSurface eGLSurface2 = this.d;
        if (eGLSurface2 == EGL10.EGL_NO_SURFACE) {
            return TekErrorCode.CONTEXT_CREATESURFACE_CREATEWINDOWS_ERROR;
        }
        this.f12a.eglDestroySurface(this.b, eGLSurface2);
        this.d = EGL10.EGL_NO_SURFACE;
        return TekErrorCode.CONTEXT_CREATESURFACE_CREATEWINDOWS_ERROR;
    }

    /* JADX WARN: Code duplicated, block: B:29:0x00df  */
    /* JADX WARN: Code duplicated, block: B:32:0x00e5  */
    /* JADX WARN: Code duplicated, block: B:34:0x00e9  */
    /* JADX WARN: Code duplicated, block: B:35:0x00ec  */
    public int b(boolean z) {
        int[] iArr;
        int[] iArr2;
        EGLConfig[] eGLConfigArr;
        EGLContext eGLContextEglCreateContext;
        String str;
        this.h = z;
        TekProxyLog.d("TEK TekGLContext", "initGLESContext");
        EGL10 egl10 = (EGL10) EGLContext.getEGL();
        this.f12a = egl10;
        EGLDisplay eGLDisplayEglGetDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        this.b = eGLDisplayEglGetDisplay;
        if (eGLDisplayEglGetDisplay == EGL10.EGL_NO_DISPLAY) {
            TekLog.write("TEK TekGLContext", "initGLESContext EGL_NO_DISPLAY");
            return TekErrorCode.CONTEXT_EGL_NO_DISPLAY;
        }
        if (!this.f12a.eglInitialize(eGLDisplayEglGetDisplay, new int[2])) {
            TekLog.write("TEK TekGLContext", "initGLESContext eglInitialize error.");
            return TekErrorCode.CONTEXT_EGL_INIT_ERROR;
        }
        int[] iArr3 = new int[1];
        int[] iArr4 = new int[0];
        int i = 3;
        if (this.h) {
            int[][] iArr5 = {new int[]{12325, 32, 12320, 32, 12321, 8, 12322, 8, 12323, 8, 12324, 8, 12352, 4, 12339, 4, 12344}, new int[]{12325, 24, 12320, 32, 12321, 8, 12322, 8, 12323, 8, 12324, 8, 12352, 4, 12339, 4, 12344}, new int[]{12325, 16, 12320, 32, 12321, 8, 12322, 8, 12323, 8, 12324, 8, 12352, 4, 12339, 4, 12344}};
            String[] strArr = {"32-bit", "24-bit", "16-bit"};
            int[] iArr6 = iArr4;
            int i2 = 0;
            while (true) {
                if (i2 >= i) {
                    iArr2 = iArr6;
                    break;
                }
                int i3 = i2;
                if (!this.f12a.eglChooseConfig(this.b, iArr5[i2], this.j, 1, iArr3) || iArr3[0] <= 0) {
                    if (i3 == 2 && iArr3[0] <= 0) {
                        TekLog.write("TEK TekGLContext", "No depth buffer configuration available, falling back to no depth.");
                        this.h = false;
                        this.i = 0;
                        iArr6 = new int[]{12320, 32, 12321, 8, 12322, 8, 12323, 8, 12324, 8, 12352, 4, 12339, 4, 12344};
                    }
                    i2 = i3 + 1;
                    i = 3;
                } else {
                    TekLog.write("TEK TekGLContext", "Using " + strArr[i3] + " depth buffer configuration.");
                    iArr = iArr5[i3];
                    this.i = iArr[1];
                }
            }
            eGLConfigArr = new EGLConfig[1];
            this.j = eGLConfigArr;
            if (!this.f12a.eglChooseConfig(this.b, iArr2, eGLConfigArr, 1, iArr3)) {
                str = "initGLESContext eglChooseConfig error.";
            } else {
                if (iArr3[0] <= 0) {
                    eGLContextEglCreateContext = this.f12a.eglCreateContext(this.b, this.j[0], EGL10.EGL_NO_CONTEXT, new int[]{12440, 2, 12344});
                    this.e = eGLContextEglCreateContext;
                    if (eGLContextEglCreateContext != null || eGLContextEglCreateContext == EGL10.EGL_NO_CONTEXT) {
                        this.e = null;
                        return TekErrorCode.CONTEXT_EGL_CREATECONTEXT_ERROR;
                    }
                    TekProxyLog.i("TEK TekGLContext", "initGLESContext DepthBufferInfo=" + e());
                    return 0;
                }
                str = "initGLESContext no matching config found for depth settings, trying without depth.";
            }
            TekLog.write("TEK TekGLContext", str);
            return TekErrorCode.CONTEXT_EGL_CONFIG_ERROR;
        }
        iArr = new int[]{12320, 32, 12321, 8, 12322, 8, 12323, 8, 12324, 8, 12352, 4, 12339, 4, 12344};
        this.i = 0;
        iArr2 = iArr;
        eGLConfigArr = new EGLConfig[1];
        this.j = eGLConfigArr;
        if (!this.f12a.eglChooseConfig(this.b, iArr2, eGLConfigArr, 1, iArr3)) {
            str = "initGLESContext eglChooseConfig error.";
        } else {
            if (iArr3[0] <= 0) {
                eGLContextEglCreateContext = this.f12a.eglCreateContext(this.b, this.j[0], EGL10.EGL_NO_CONTEXT, new int[]{12440, 2, 12344});
                this.e = eGLContextEglCreateContext;
                if (eGLContextEglCreateContext != null) {
                }
                this.e = null;
                return TekErrorCode.CONTEXT_EGL_CREATECONTEXT_ERROR;
            }
            str = "initGLESContext no matching config found for depth settings, trying without depth.";
        }
        TekLog.write("TEK TekGLContext", str);
        return TekErrorCode.CONTEXT_EGL_CONFIG_ERROR;
    }

    public void c() {
        EGLDisplay eGLDisplay;
        EGLContext eGLContext;
        EGLContext eGLContext2;
        TekProxyLog.i("TEK TekGLContext", "destory.");
        EGL10 egl10 = this.f12a;
        if (egl10 != null && (eGLDisplay = this.b) != EGL10.EGL_NO_DISPLAY && (eGLContext = this.e) != (eGLContext2 = EGL10.EGL_NO_CONTEXT) && eGLContext != null) {
            EGLSurface eGLSurface = EGL10.EGL_NO_SURFACE;
            if (!egl10.eglMakeCurrent(eGLDisplay, eGLSurface, eGLSurface, eGLContext2)) {
                TekProxyLog.e("TEK TekGLContext", "destory eglMakeCurrent error:" + this.f12a.eglGetError());
            }
            EGLSurface eGLSurface2 = this.c;
            if (eGLSurface2 != EGL10.EGL_NO_SURFACE && !this.f12a.eglDestroySurface(this.b, eGLSurface2)) {
                TekProxyLog.e("TEK TekGLContext", "destory eglDestroySurface error:" + this.f12a.eglGetError());
            }
            EGLSurface eGLSurface3 = this.d;
            if (eGLSurface3 != EGL10.EGL_NO_SURFACE && !this.f12a.eglDestroySurface(this.b, eGLSurface3)) {
                TekProxyLog.e("TEK TekGLContext", "destory eglDestroySurface _encodeSurface error:" + this.f12a.eglGetError());
            }
            if (!this.f12a.eglDestroyContext(this.b, this.e)) {
                TekProxyLog.e("TEK TekGLContext", "destory eglDestroyContext error:" + this.f12a.eglGetError());
            }
            if (!this.f12a.eglTerminate(this.b)) {
                TekProxyLog.e("TEK TekGLContext", "destory eglTerminate error:" + this.f12a.eglGetError());
            }
            this.f12a = null;
            this.b = null;
        }
        this.e = EGL10.EGL_NO_CONTEXT;
        EGLSurface eGLSurface4 = EGL10.EGL_NO_SURFACE;
        this.c = eGLSurface4;
        this.d = eGLSurface4;
    }

    public void d() {
        TekLog.write("TEK TekGLContext", "destoryRenderSurface.");
        GLES20.glFinish();
        EGLSurface eGLSurface = this.c;
        if (eGLSurface != EGL10.EGL_NO_SURFACE) {
            this.f12a.eglDestroySurface(this.b, eGLSurface);
            this.c = EGL10.EGL_NO_SURFACE;
        }
    }

    public a e() {
        return new a(this.h, this.i);
    }

    public boolean f() {
        EGL10 egl10 = this.f12a;
        if (egl10 != null && egl10.eglGetCurrentContext() != EGL10.EGL_NO_CONTEXT) {
            return true;
        }
        TekLog.write("TEK TekGLContext", "isCurrentContentAvaialbe false.");
        return false;
    }

    public boolean g() {
        EGL10 egl10;
        if (this.c != EGL10.EGL_NO_SURFACE && (egl10 = this.f12a) != null && egl10.eglGetCurrentContext() != EGL10.EGL_NO_CONTEXT) {
            return true;
        }
        TekLog.write("TEK TekGLContext", "isRenderSurfaceAvaiable false.");
        return false;
    }

    public int h() {
        try {
            if (this.f12a.eglSwapBuffers(this.b, this.c)) {
                return 0;
            }
            int iEglGetError = this.f12a.eglGetError();
            TekProxyLog.w("TEK TekGLContext", "swapBuffers error:" + iEglGetError);
            if (iEglGetError != 0) {
                return iEglGetError;
            }
            return -1;
        } catch (Throwable th) {
            TekProxyLog.w("TEK TekGLContext", "swapBuffers error" + th);
            return -1;
        }
    }
}
