package com.tencent.qqmusic.business.playernew.fxeffect.custom;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusic.ui.OnSurfaceChangeListener;

import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes4.dex */
@TargetApi(11)
public abstract class CustomRenderSurfaceView extends GLSurfaceView {
    private int b;
    private int d;
    private boolean e;
    private boolean f;
    private final List<OnSurfaceChangeListener> g;
    private final List<Integer> h;
    private final GLSurfaceView.Renderer i;

    class a implements GLSurfaceView.Renderer {
        a() {
        }

        @Override // android.opengl.GLSurfaceView.Renderer
        public void onDrawFrame(GL10 gl10) {
            byte[] bArr = SwordSwitches.switches16;
            if (bArr == null || ((bArr[1285] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(gl10, this, 178286).isSupported) {
                GLES20.glClear(16640);
                CustomRenderSurfaceView.this.g();
                Iterator it = CustomRenderSurfaceView.this.g.iterator();
                while (it.hasNext()) {
                    ((OnSurfaceChangeListener) it.next()).onDrawFrame();
                }
            }
        }

        @Override // android.opengl.GLSurfaceView.Renderer
        public void onSurfaceChanged(GL10 gl10, int i, int i2) {
            byte[] bArr = SwordSwitches.switches16;
            if (bArr == null || ((bArr[1282] >> 2) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{gl10, Integer.valueOf(i), Integer.valueOf(i2)}, this, 178259).isSupported) {
                Log.i("CustomRenderSurfaceView", "[onSurfaceChanged] hashCode:" + hashCode() + ", width:" + i + " height:" + i2);
                CustomRenderSurfaceView.this.b = i;
                CustomRenderSurfaceView.this.d = i2;
                GLES20.glViewport(0, 0, i, i2);
                CustomRenderSurfaceView.this.h(i, i2);
                CustomRenderSurfaceView.this.e = true;
                Iterator it = CustomRenderSurfaceView.this.g.iterator();
                while (it.hasNext()) {
                    ((OnSurfaceChangeListener) it.next()).onSurfaceChanged(i, i2);
                }
            }
        }

        @Override // android.opengl.GLSurfaceView.Renderer
        public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
            byte[] bArr = SwordSwitches.switches16;
            if (bArr == null || ((bArr[1278] >> 7) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{gl10, eGLConfig}, this, 178232).isSupported) {
                Log.i("CustomRenderSurfaceView", "[onSurfaceCreated] hashCode:" + hashCode());
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GLES20.glEnable(2884);
                GLES20.glFrontFace(2305);
                CustomRenderSurfaceView.this.i();
                CustomRenderSurfaceView.this.f = true;
                Iterator it = CustomRenderSurfaceView.this.g.iterator();
                while (it.hasNext()) {
                    ((OnSurfaceChangeListener) it.next()).onSurfaceCreated();
                }
            }
        }
    }

    class b implements Runnable {
        b() {
        }

        @Override // java.lang.Runnable
        public void run() {
            CustomRenderSurfaceView.this.k();
        }
    }

    public CustomRenderSurfaceView(Context context) {
        super(context);
        this.b = -1;
        this.d = -1;
        this.e = false;
        this.f = false;
        this.g = new CopyOnWriteArrayList();
        this.h = new ArrayList();
        this.i = new a();
        f();
    }

    private void f() {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1287] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 178298).isSupported) {
            setEGLContextClientVersion(2);
            setEGLConfigChooser(8, 8, 8, 8, 0, 0);
            setRenderer(this.i);
            setRenderMode(0);
        }
    }

    private void l() {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1290] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 178326).isSupported) {
            Iterator<Integer> it = this.h.iterator();
            while (it.hasNext()) {
                GLES20.glDeleteTextures(1, new int[]{it.next().intValue()}, 0);
            }
        }
    }

    public abstract void g();

    public int getViewHeight() {
        return this.d;
    }

    public int getViewWidth() {
        return this.b;
    }

    public abstract void h(int i, int i2);

    public abstract void i();

    public void j() {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1291] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 178336).isSupported) {
            queueEvent(new b());
        }
    }

    public void k() {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1293] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 178348).isSupported) {
            l();
        }
    }

    @Override // android.opengl.GLSurfaceView, android.view.SurfaceView, android.view.View
    public void onAttachedToWindow() {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1298] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 178391).isSupported) {
            super.onAttachedToWindow();
            Log.i("CustomRenderSurfaceView", "[onAttachedToWindow] hashCode:" + hashCode());
            if (this.f) {
                Log.i("CustomRenderSurfaceView", "[onAttachedToWindow] calling onResume()");
                super.onResume();
            }
        }
    }

    @Override // android.opengl.GLSurfaceView, android.view.SurfaceView, android.view.View
    public void onDetachedFromWindow() {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr == null || ((bArr[1300] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 178403).isSupported) {
            Log.i("CustomRenderSurfaceView", "[onDetachedFromWindow] hashCode:" + hashCode());
            super.onPause();
            super.onDetachedFromWindow();
        }
    }

    public CustomRenderSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.b = -1;
        this.d = -1;
        this.e = false;
        this.f = false;
        this.g = new CopyOnWriteArrayList();
        this.h = new ArrayList();
        this.i = new a();
        f();
    }
}
