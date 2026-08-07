package TekEngineLib.Manager;

import TekEngineLib.Interface.TekEventListener;
import TekEngineLib.Lyric.TekLyricParam;
import TekEngineLib.Render.TekBaseShader;
import TekEngineLib.State.TekAdditionImageRef;
import TekEngineLib.State.TekBeforeRenderCallback;
import TekEngineLib.State.TekCustomLayer;
import TekEngineLib.State.TekEffectInfo;
import TekEngineLib.State.TekGyroscopeData;
import TekEngineLib.State.TekLog;
import TekEngineLib.State.TekProxyLog;
import TekEngineLib.State.TekRenderProgressListener;
import TekEngineLib.State.TekState;
import TekEngineLib.State.TekStateListener;
import TekEngineLib.State.TekTimestampGetter;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import com.tencent.ttpic.baseutils.io.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class a implements TextureView.SurfaceTextureListener {
    private static String s = "TEK TekEffectManager";
    private TekStateListener c;
    private c.a r;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private TextureView f9a = null;
    private TekBaseShader b = null;
    private TekRenderProgressListener d = null;
    private TekBeforeRenderCallback e = null;
    private SurfaceTexture f = null;
    private int g = 0;
    private int h = 0;
    private TekEffectThread i = null;
    private String j = null;
    private String k = null;
    private String l = null;
    private TekCustomLayer m = null;
    private boolean n = false;
    volatile LinkedList<Runnable> o = new LinkedList<>();
    private boolean p = true;
    private boolean q = false;

    /* JADX INFO: renamed from: TekEngineLib.Manager.a$a, reason: collision with other inner class name */
    class C0000a implements TekStateListener {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        final /* synthetic */ TekStateListener f10a;

        /* JADX INFO: renamed from: TekEngineLib.Manager.a$a$a, reason: collision with other inner class name */
        class RunnableC0001a implements Runnable {
            RunnableC0001a() {
            }

            @Override // java.lang.Runnable
            public void run() {
                if (a.this.f9a != null) {
                    a.this.f9a.setAlpha(0.99f);
                }
            }
        }

        C0000a(TekStateListener tekStateListener) {
            this.f10a = tekStateListener;
        }

        @Override // TekEngineLib.State.TekStateListener
        public void onStateChange(TekState tekState, TekEffectInfo tekEffectInfo, int i) {
            TekLog.write(a.s, "onStateChange:" + String.valueOf(tekState));
            if (tekState == TekState.INIT_GL_FAILED) {
                a.this.j = null;
                a.this.k = null;
                a.this.m = null;
            }
            if (tekState == TekState.ON_FIRST_FRAME && a.this.f9a != null) {
                a.this.f9a.post(new RunnableC0001a());
            }
            TekStateListener tekStateListener = this.f10a;
            if (tekStateListener != null) {
                tekStateListener.onStateChange(tekState, tekEffectInfo, i);
            }
        }
    }

    public a(TekStateListener tekStateListener) {
        this.c = null;
        TekLog.write(s, "construct");
        this.c = new C0000a(tekStateListener);
    }

    public static void a(String str) {
        TekLog.addFilter(str);
    }

    private int b(String str, String str2) {
        TekLog.write(s, "forceUpdateEffectResource:" + str + " inputFileName:" + str2);
        if (str == null || str2 == null) {
            TekLog.write(s, "forceUpdateEffectResource resourcePath == null or inputFileName == null");
            return -1;
        }
        this.i.updateEffectResource(str, str2);
        this.k = str;
        this.l = str2;
        return 0;
    }

    private boolean b(String str) {
        try {
            return new File(str).exists();
        } catch (Exception unused) {
            return false;
        }
    }

    private void f() {
        TekProxyLog.i(s, "createEffectThread");
        if (this.i != null) {
            TekProxyLog.i(s, "createEffectThread _effectThread != null");
            return;
        }
        if (this.f == null) {
            TekProxyLog.e(s, "createEffectThread _surface == null");
            return;
        }
        if (this.g <= 0 || this.h <= 0) {
            TekProxyLog.e(s, "createEffectThread _width <= 0");
            return;
        }
        TekEffectThread tekEffectThread = new TekEffectThread();
        this.i = tekEffectThread;
        tekEffectThread.setIsEnableDepth(this.q);
        this.i.setStateListener(this.c);
        this.i.setShader(this.b);
        this.i.setIsDirectRenderView(this.p);
        this.i.setRenderProgressListener(this.d);
        this.i.setBeforeRenderCallback(this.e);
        this.i.setOnReleaseListener(this.r);
        TekProxyLog.i(s, "createEffectThread success");
        this.i.start();
        while (!this.o.isEmpty()) {
            this.i.addRunable(this.o.removeFirst());
        }
    }

    public static void h() {
        TekLog.enablePrint();
    }

    public View a(Context context) {
        TekLog.write(s, "createView");
        if (this.f9a != null) {
            TekLog.write(s, "createView _textureView != null");
        } else {
            TextureView textureView = new TextureView(context);
            this.f9a = textureView;
            textureView.setSurfaceTextureListener(this);
            this.f9a.setAlpha(0.99f);
        }
        return this.f9a;
    }

    public void a(float f) {
        TekLog.write(s, "setSpeed");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || this.f == null) {
            TekLog.write(s, "setSpeed _effectThread == null");
        } else {
            tekEffectThread.setSpeed(f);
        }
    }

    public void a(int i) {
        if (this.i == null) {
            TekLog.write(s, "updatePlaySpeed _effectThread == null");
            return;
        }
        TekLog.write(s, "updateEffectFillMode");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread != null) {
            tekEffectThread.updateEffectFillMode(i);
        }
    }

    public void a(int i, int i2) {
        SurfaceTexture surfaceTexture;
        TekProxyLog.i(s, "createOffscreenSurface:" + i + "," + i2);
        if (this.f != null) {
            TekProxyLog.i(s, "createOffscreenSurface: _surface already exists");
            return;
        }
        SurfaceTexture surfaceTexture2 = new SurfaceTexture(0);
        this.f = surfaceTexture2;
        this.g = i;
        this.h = i2;
        surfaceTexture2.setDefaultBufferSize(i, i2);
        f();
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread != null && (surfaceTexture = this.f) != null && i > 0 && i2 > 0) {
            tekEffectThread.createRenderSurface(surfaceTexture, i, i2);
            return;
        }
        TekProxyLog.i(s, "createOffscreenSurface: _effectThread =" + this.i + " surface =" + this.f + " width =" + i + " height =" + i2);
    }

    public void a(long j) {
        TekLog.write(s, "updateAudioTimastamp:" + String.valueOf(j));
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "updateAudioTimestamp _effectThread == null");
        } else {
            tekEffectThread.updateAudioTimestamp(j);
        }
    }

    public void a(TekEventListener tekEventListener) {
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "setEventListener _effectThread == null");
        } else {
            tekEffectThread.setEventListener(tekEventListener);
        }
    }

    public void a(TekLyricParam tekLyricParam) {
        String str;
        String str2;
        TekLog.write(s, "updateLyricParam");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || this.f == null) {
            str = s;
            str2 = "updateLyricParam _effectThread == null";
        } else if (tekLyricParam != null) {
            tekEffectThread.updateLyricParam(tekLyricParam);
            return;
        } else {
            str = s;
            str2 = "updateLyricParam param == null";
        }
        TekLog.write(str, str2);
    }

    public void a(c.a aVar) {
        this.r = aVar;
    }

    public void a(TekBaseShader tekBaseShader) {
        this.b = tekBaseShader;
    }

    public void a(TekEngineLib.Render.a aVar) {
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "setFrameUpdateListener _effectThread == null");
        } else {
            tekEffectThread.setFrameUpdateListener(aVar);
        }
    }

    public void a(TekGyroscopeData tekGyroscopeData) {
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "onGyroscopeDataUpdated _effectThread == null");
        } else {
            tekEffectThread.updateGyroscopeData(tekGyroscopeData);
        }
    }

    public void a(TekTimestampGetter tekTimestampGetter) {
        TekLog.write(s, "setTimestampGetter");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "setTimestampGetter _effectThread == null");
        } else {
            tekEffectThread.setTimestampGetter(tekTimestampGetter);
        }
    }

    public void a(Runnable runnable) {
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || !tekEffectThread.isAlive()) {
            this.o.add(runnable);
        } else {
            this.i.addRunable(runnable);
        }
    }

    public void a(String str, String str2) {
        TekLog.write(s, "cleanParam:");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || this.f == null) {
            TekLog.write(s, "cleanParam _effectThread == null");
        } else {
            tekEffectThread.cleanParam(str, str2);
        }
    }

    public void a(String str, Map<String, Object> map) {
        TekLog.write(s, "updateParam:");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || this.f == null) {
            TekLog.write(s, "updateParam _effectThread == null");
        } else {
            tekEffectThread.updateParam(str, map);
        }
    }

    public void a(ArrayList<TekAdditionImageRef> arrayList) {
        TekLog.write(s, "addInputImageRefs");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "addInputImageRefs _effectThread == null");
        } else {
            tekEffectThread.addInputImageRefs(arrayList);
        }
    }

    public void a(boolean z) {
        this.p = z;
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "onGyroscopeDataUpdated _effectThread == null");
        } else {
            tekEffectThread.setIsDirectRenderView(z);
        }
    }

    public void a(byte[] bArr, int i) {
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "updateFFTData _effectThread == null");
        } else {
            tekEffectThread.updateFFTData(bArr, i);
        }
    }

    public void b() {
        TekLog.write(s, "releaseGLResource");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || this.f == null) {
            TekLog.write(s, "releaseGLResource _effectThread == null");
        } else {
            tekEffectThread.cleanCache();
            this.k = null;
        }
    }

    public void b(float f) {
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "updatePlaySpeed _effectThread == null");
        } else {
            tekEffectThread.updatePlaySpeed(f);
        }
    }

    public void b(ArrayList<String> arrayList) {
        TekLog.write(s, "updateInputImages");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || arrayList == null || this.f == null) {
            TekLog.write(s, "updateInputImages _effectThread == null");
        } else {
            tekEffectThread.updateInputImages(arrayList);
        }
    }

    public void b(boolean z) {
        this.q = z;
    }

    public int c(String str, String str2) {
        TekLog.write(s, "updateEffectResource:" + str);
        this.m = null;
        if (str == null || this.i == null || this.f == null) {
            TekProxyLog.i(s, "updateEffectResource:resourcePath == null");
            return -1;
        }
        if (!b(str)) {
            if (!b(str + FileUtils.RES_PREFIX_STORAGE + str2)) {
                TekProxyLog.i(s, "updateEffectResource:resourcePath no fileIsExists");
                return -2;
            }
        }
        String str3 = this.k;
        if (str3 == null || this.l == null || str3.compareTo(str) != 0 || this.l.compareTo(str2) != 0) {
            return b(str, str2);
        }
        TekProxyLog.i(s, "updateEffectResource:compareTo(resourcePath) == 0");
        return 0;
    }

    public void c() {
        TekLog.write(s, "cleanLyric");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "cleanLyric _effectThread == null");
        } else {
            tekEffectThread.cleanLyric();
        }
    }

    public void c(String str) {
        if (this.i == null || TextUtils.isEmpty(str)) {
            return;
        }
        this.i.setCryptKey(str);
    }

    public void c(boolean z) {
        TekLog.write(s, "updateAudioPlaying:" + z);
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "updateAudioTimestamp _effectThread == null");
        } else {
            tekEffectThread.updateAudioPlaying(z);
        }
    }

    public void d() {
        TekLog.write(s, "clearView");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || this.f == null) {
            TekLog.write(s, "clearView _effectThread == null");
        } else {
            tekEffectThread.clearView();
        }
    }

    public void e() {
        TekLog.write(s, "closeEffect");
        this.k = null;
        this.m = null;
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "closeEffect _effectThread == null");
        } else {
            tekEffectThread.closeEffect();
        }
    }

    public void g() {
        TekProxyLog.i(s, "destory");
        TextureView textureView = this.f9a;
        if (textureView != null) {
            textureView.setSurfaceTextureListener(null);
            this.f9a = null;
        }
        if (this.f != null) {
            this.f = null;
        }
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread != null) {
            tekEffectThread.destory();
            this.i = null;
        }
        this.e = null;
        this.d = null;
        this.c = null;
    }

    public int i() {
        TekLog.write(s, "forceRender");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || this.f == null) {
            TekLog.write(s, "forceRender _effectThread == null");
            return -1;
        }
        tekEffectThread.forceRender();
        return 0;
    }

    public long j() {
        TekLog.write(s, "getAudioTimestamp:");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread != null) {
            return tekEffectThread.getAudioTimestamp();
        }
        TekLog.write(s, "getAudioTimestamp _effectThread == null");
        return 0L;
    }

    public int k() {
        TekLog.write(s, "getOutputTexture");
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread != null) {
            return tekEffectThread.getOutputTexture();
        }
        TekLog.write(s, "getOutputTexture _effectThread == null");
        return 0;
    }

    public void l() {
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread != null) {
            tekEffectThread._fastPauseFlag = true;
        }
    }

    public int m() {
        TekLog.write(s, "startRender");
        this.n = true;
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null || ((this.k == null && this.m == null) || this.f == null)) {
            TekLog.write(s, "startRender _render == null");
            return -1;
        }
        tekEffectThread.startRender();
        return 0;
    }

    public int n() {
        TekLog.write(s, "stopRender");
        this.n = false;
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread == null) {
            TekLog.write(s, "stopRender _effectThread == null");
            return -1;
        }
        tekEffectThread.stopRender();
        return 0;
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        TekProxyLog.i(s, "onSurfaceTextureAvailable:" + String.valueOf(i) + "," + String.valueOf(i2));
        this.g = i;
        this.h = i2;
        this.f = surfaceTexture;
        f();
        if (this.i != null && this.f != null && i > 0 && i2 > 0) {
            TextureView textureView = this.f9a;
            if (textureView != null) {
                textureView.setAlpha(0.0f);
            }
            this.i.createRenderSurface(surfaceTexture, this.g, this.h);
            return;
        }
        TekProxyLog.e(s, "onSurfaceTextureAvailable:" + String.valueOf(i) + "," + String.valueOf(i2));
        if (this.f == null) {
            TekProxyLog.e(s, "onSurfaceTextureAvailable:_surface == null");
        }
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        TekProxyLog.i(s, "onSurfaceTextureDestroyed");
        this.f = null;
        this.g = 0;
        this.h = 0;
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread != null) {
            tekEffectThread.stopRender();
            long jCurrentTimeMillis = System.currentTimeMillis();
            this.i.destoryRenderSurface();
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            TekProxyLog.i(s, "onSurfaceTextureDestroyed diff:" + String.valueOf(jCurrentTimeMillis2 - jCurrentTimeMillis));
        }
        return false;
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
        TekProxyLog.i(s, "onSurfaceTextureSizeChanged:" + String.valueOf(i) + "," + String.valueOf(i2));
        this.g = i;
        this.h = i2;
        TekEffectThread tekEffectThread = this.i;
        if (tekEffectThread != null) {
            tekEffectThread.onSurfaceChanged(i, i2);
            return;
        }
        TekProxyLog.i(s, "onSurfaceTextureSizeChanged null:" + String.valueOf(i) + "," + String.valueOf(i2));
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
}
