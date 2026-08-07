package TekEngineLib.Manager;

import TekEngineLib.Engine.TekNativeInterface;
import TekEngineLib.Interface.TekEventListener;
import TekEngineLib.Interface.UsualParamInfo;
import TekEngineLib.Lyric.TekLyricParam;
import TekEngineLib.Render.TekBaseShader;
import TekEngineLib.State.TekAdditionImageRef;
import TekEngineLib.State.TekAdditionInfo;
import TekEngineLib.State.TekBeforeRenderCallback;
import TekEngineLib.State.TekCustomLayer;
import TekEngineLib.State.TekEffectConfig;
import TekEngineLib.State.TekEffectInfo;
import TekEngineLib.State.TekEffectLayerInfo;
import TekEngineLib.State.TekErrorCode;
import TekEngineLib.State.TekGyroscopeData;
import TekEngineLib.State.TekLog;
import TekEngineLib.State.TekProxyLog;
import TekEngineLib.State.TekRenderProgressListener;
import TekEngineLib.State.TekState;
import TekEngineLib.State.TekStateListener;
import TekEngineLib.State.TekTimestampGetter;
import android.graphics.SurfaceTexture;
import android.os.Build;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekEffectThread extends TekEngineLib.Manager.c {
    private static String LOGTAG = "TEK TekEffectThread";
    private byte[] _fftData;
    private int _fftSamplerate;
    private boolean _isDirectRenderView;
    private boolean _isEnableDepth;
    private SurfaceTexture _surfaceTexture = null;
    private TekEngineLib.Manager.d _context = null;
    private volatile TekEngineLib.Render.b _render = null;
    private AtomicBoolean hasDestroySurface = new AtomicBoolean(false);
    private volatile TekStateListener _stateListener = null;
    private volatile TekEventListener _eventListener = null;
    private volatile TekBaseShader _shader = null;
    private volatile TekRenderProgressListener _renderProgressListener = null;
    private TekBeforeRenderCallback _beforeRenderCallback = null;
    private TekTimestampGetter _timestampGetter = null;
    private ArrayList<TekAdditionImageRef> _inputImageRefs = null;
    private boolean _inputImageRefUpdated = false;
    private boolean _hasSendPlayOver = false;
    public volatile boolean _fastPauseFlag = false;
    private int _width = 0;
    private int _height = 0;
    private volatile boolean _firstFrameFlag = false;
    private Lock _ffDataLock = new ReentrantLock();
    public volatile boolean _isOpeningEncoder = false;
    public long mRenderConsumTime = 0;
    public long mRenderFrame = 0;
    public long _lastRenderTime = 0;
    private float _playSpeed = 1.0f;
    private ArrayList<k> _gyroscopeListeners = new ArrayList<>();
    private volatile boolean isStopRenderMainThread = false;

    class a implements Runnable {
        final /* synthetic */ String b;
        final /* synthetic */ String d;

        a(String str, String str2) {
            this.b = str;
            this.d = str2;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (TekEffectThread.this._context == null && TekEffectThread.this._surfaceTexture != null && TekEffectThread.this._width > 0 && TekEffectThread.this._height > 0) {
                TekEffectThread tekEffectThread = TekEffectThread.this;
                tekEffectThread.innerCreateRenderSurface(tekEffectThread._surfaceTexture, TekEffectThread.this._width, TekEffectThread.this._height);
            }
            if (TekEffectThread.this.isRenderAvailable()) {
                TekEffectThread.this.doUpdateEffectResource(this.b, this.d);
            } else {
                TekLog.write(TekEffectThread.LOGTAG, "updateEffectResource _render == null.");
                TekEffectThread.this.sendStateListener(TekState.OPEN_EFFECT_RESOURCE_FAILED, null, -1004);
            }
        }
    }

    class a0 implements Runnable {
        final /* synthetic */ TekGyroscopeData b;

        a0(TekGyroscopeData tekGyroscopeData) {
            this.b = tekGyroscopeData;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "onGyroscopeDataUpdated");
            if (TekEffectThread.this._render != null) {
                for (int i = 0; i < TekEffectThread.this._gyroscopeListeners.size(); i++) {
                    try {
                        ((k) TekEffectThread.this._gyroscopeListeners.get(i)).a(this.b);
                    } catch (Exception e) {
                        TekProxyLog.w(TekEffectThread.LOGTAG, "updateGyroscopeData error:" + e);
                        return;
                    }
                }
            }
        }
    }

    class b implements Runnable {
        final /* synthetic */ ArrayList b;

        b(ArrayList arrayList) {
            this.b = arrayList;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "addInputImages 11");
            if (TekEffectThread.this._render == null) {
                TekLog.write(TekEffectThread.LOGTAG, "addInputImages _render == null");
            } else {
                TekEffectThread.this._render.B(this.b);
            }
        }
    }

    class b0 implements Runnable {
        b0() {
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "clearView 00.");
            if (TekEffectThread.this._render == null || TekEffectThread.this._context == null || TekEffectThread.this._surfaceTexture == null || TekEffectThread.this._render.O() <= 0) {
                return;
            }
            TekLog.write(TekEffectThread.LOGTAG, "clearView 11.");
            TekEffectThread.this._render.E();
            TekEffectThread.this._context.h();
        }
    }

    class c implements Runnable {
        final /* synthetic */ TekCustomLayer b;

        c(TekCustomLayer tekCustomLayer) {
            this.b = tekCustomLayer;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (TekEffectThread.this.isRenderAvailable()) {
                TekEffectThread.this.doUpdateEffectWithCustom(this.b);
            } else {
                TekLog.write(TekEffectThread.LOGTAG, "updateEffectWithCustom _render == null.");
                TekEffectThread.this.sendStateListener(TekState.OPEN_EFFECT_RESOURCE_FAILED, null, -1004);
            }
        }
    }

    class c0 implements Runnable {
        final /* synthetic */ boolean b;

        c0(boolean z) {
            this.b = z;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "setIsDirectRenderView 00." + this.b);
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.x(this.b);
            }
        }
    }

    class d implements Runnable {
        final /* synthetic */ ArrayList b;

        d(ArrayList arrayList) {
            this.b = arrayList;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "appendInputImages 11");
            if (TekEffectThread.this._render == null) {
                TekLog.write(TekEffectThread.LOGTAG, "appendInputImages _render == null");
            } else {
                TekEffectThread.this._render.w(this.b);
            }
        }
    }

    class d0 implements Runnable {
        final /* synthetic */ TekEngineLib.Render.a b;

        d0(TekEngineLib.Render.a aVar) {
            this.b = aVar;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "setFrameUpdateListener 00." + this.b);
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.i(this.b);
            }
        }
    }

    class e implements Runnable {
        final /* synthetic */ String b;
        final /* synthetic */ Map d;

        e(String str, Map map) {
            this.b = str;
            this.d = map;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (TekEffectThread.this.isRenderAvailable()) {
                TekEffectThread.this._render.p(this.b, this.d);
            } else {
                TekLog.write(TekEffectThread.LOGTAG, "updateParam _render == null.");
            }
        }
    }

    class e0 implements Runnable {
        e0() {
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "cleanCache 00.");
            if (TekEffectThread.this._surfaceTexture == null) {
                TekLog.write(TekEffectThread.LOGTAG, "cleanCache _surfaceTexture == null.");
                return;
            }
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.d();
            }
            TekEffectThread.this._runningState = TekEngineLib.Manager.c.b.IDLEING;
        }
    }

    class f implements Runnable {
        final /* synthetic */ ArrayList b;

        f(ArrayList arrayList) {
            this.b = arrayList;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "addInputImageRefs 11");
            if (TekEffectThread.this._render == null) {
                TekLog.write(TekEffectThread.LOGTAG, "addInputImageRefs _render == null");
            } else {
                if (TekEffectThread.this._surfaceTexture != null) {
                    TekEffectThread.this._render.q(this.b);
                    return;
                }
                TekEffectThread.this._inputImageRefUpdated = true;
                TekEffectThread.this._inputImageRefs = this.b;
            }
        }
    }

    class f0 implements Runnable {
        final /* synthetic */ String b;

        f0(String str) {
            this.b = str;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.v(this.b);
            }
        }
    }

    class g implements Runnable {
        final /* synthetic */ String b;
        final /* synthetic */ String d;

        g(String str, String str2) {
            this.b = str;
            this.d = str2;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (TekEffectThread.this.isRenderAvailable()) {
                TekEffectThread.this._render.o(this.b, this.d);
            } else {
                TekLog.write(TekEffectThread.LOGTAG, "cleanParam _render == null.");
            }
        }
    }

    class h implements Runnable {
        final /* synthetic */ TekEffectConfig b;

        h(TekEffectConfig tekEffectConfig) {
            this.b = tekEffectConfig;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "updateConfig 11");
            if (TekEffectThread.this._render == null) {
                TekLog.write(TekEffectThread.LOGTAG, "updateConfig _render == null");
            } else {
                TekEffectThread.this._render.l(this.b);
            }
        }
    }

    class i implements Runnable {
        final /* synthetic */ boolean b;

        i(boolean z) {
            this.b = z;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "enableAudioTimestamp 00:" + String.valueOf(this.b));
            if (TekEffectThread.this._render != null) {
                TekLog.write(TekEffectThread.LOGTAG, "enableAudioTimestamp 11:" + String.valueOf(this.b));
                TekEffectThread.this._render.r(this.b);
            }
        }
    }

    class j implements Runnable {
        j() {
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "resetTimestamp 11");
            if (TekEffectThread.this._render == null) {
                TekLog.write(TekEffectThread.LOGTAG, "resetTimestamp _render == null");
            } else {
                TekEffectThread.this._render.V();
            }
        }
    }

    class k {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        public long f8a;

        public k(long j) {
            this.f8a = j;
        }

        public void a(TekGyroscopeData tekGyroscopeData) {
            if (TekNativeInterface.LoadLibrarySuccess) {
                long j = this.f8a;
                if (j == 0) {
                    TekLog.write(TekEffectThread.LOGTAG, "updateGyroscopeData _helperPoint == 0");
                } else {
                    TekNativeInterface.updateGyroscopeData(j, tekGyroscopeData.timestamp.doubleValue(), tekGyroscopeData.x.doubleValue(), tekGyroscopeData.y.doubleValue(), tekGyroscopeData.z.doubleValue());
                }
            }
        }
    }

    class l implements Runnable {
        final /* synthetic */ String b;

        l(String str) {
            this.b = str;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "addLayer 11");
            if (TekEffectThread.this._render == null) {
                TekLog.write(TekEffectThread.LOGTAG, "addLayer _render == null");
            } else {
                TekEffectThread.this._render.n(this.b);
            }
        }
    }

    class m implements Runnable {
        m() {
        }

        @Override // java.lang.Runnable
        public void run() {
            TekEffectThread.this.destroyRenderSurfaceInner();
        }
    }

    class n implements Runnable {
        n() {
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "startRender 11.");
            if (TekEffectThread.this._surfaceTexture == null) {
                TekLog.write(TekEffectThread.LOGTAG, "startRender _surfaceTexture == null.");
                if (TekEffectThread.this._render != null) {
                    TekEffectThread.this._render.C(false);
                    return;
                }
                return;
            }
            if (TekEffectThread.this._render != null) {
                HashMap map = new HashMap();
                map.put("isPlaying", 1);
                TekEffectThread.this._render.p(UsualParamInfo.kTargetLayerAll, map);
                TekEffectThread.this._render.C(true);
            }
            TekEffectThread.this._runningState = TekEngineLib.Manager.c.b.RENDERING;
        }
    }

    class o implements Runnable {
        o() {
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "stopRender 11.");
            if (TekEffectThread.this._render != null) {
                HashMap map = new HashMap();
                map.put("isPlaying", 0);
                TekEffectThread.this._render.p(UsualParamInfo.kTargetLayerAll, map);
                TekEffectThread.this._render.C(false);
            }
            TekEffectThread.this._runningState = TekEngineLib.Manager.c.b.IDLEING;
        }
    }

    class p implements Runnable {
        p() {
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "forceRender 11.");
            TekEffectThread.this.doForceRender();
        }
    }

    class q implements Runnable {
        final /* synthetic */ SurfaceTexture b;
        final /* synthetic */ int d;
        final /* synthetic */ int e;

        q(SurfaceTexture surfaceTexture, int i, int i2) {
            this.b = surfaceTexture;
            this.d = i;
            this.e = i2;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekEffectThread.this.innerCreateRenderSurface(this.b, this.d, this.e);
        }
    }

    class r implements Runnable {
        final /* synthetic */ int b;
        final /* synthetic */ int d;

        r(int i, int i2) {
            this.b = i;
            this.d = i2;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekEffectThread.this._width = this.b;
            TekEffectThread.this._height = this.d;
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.f(TekEffectThread.this._width, TekEffectThread.this._height);
            }
        }
    }

    class s implements Runnable {
        final /* synthetic */ TekAdditionInfo b;

        s(TekAdditionInfo tekAdditionInfo) {
            this.b = tekAdditionInfo;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "setAdditionInfo 00.");
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.k(this.b);
            }
        }
    }

    class t implements Runnable {
        final /* synthetic */ TekAdditionImageRef b;

        t(TekAdditionImageRef tekAdditionImageRef) {
            this.b = tekAdditionImageRef;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "setAdditionInfo 00.");
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.j(this.b);
            }
        }
    }

    class u implements Runnable {
        final /* synthetic */ TekLyricParam b;

        u(TekLyricParam tekLyricParam) {
            this.b = tekLyricParam;
        }

        @Override // java.lang.Runnable
        public void run() {
            int iB;
            TekLog.write(TekEffectThread.LOGTAG, "updateLyricParam 00.");
            if (TekEffectThread.this._render == null || (iB = TekEffectThread.this._render.b(this.b)) >= 0) {
                return;
            }
            TekEffectThread.this.sendStateListener(TekState.UPDATE_LYRIC_ERROR, null, iB);
        }
    }

    class v implements Runnable {
        v() {
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "cleanLyric.");
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.u();
            }
        }
    }

    class w implements Runnable {
        w() {
        }

        @Override // java.lang.Runnable
        public void run() {
            TekProxyLog.i(TekEffectThread.LOGTAG, "closeEffect 11");
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.I();
                TekEffectThread.this._render = null;
            }
            if (TekEffectThread.this._context != null) {
                TekEffectThread.this._context.c();
                TekEffectThread.this._context = null;
            }
            TekEffectThread.this._runningState = TekEngineLib.Manager.c.b.IDLEING;
        }
    }

    class x implements Runnable {
        final /* synthetic */ TekRenderProgressListener b;

        x(TekRenderProgressListener tekRenderProgressListener) {
            this.b = tekRenderProgressListener;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "setRenderProgressListener 00");
            TekEffectThread.this._renderProgressListener = this.b;
        }
    }

    class y implements Runnable {
        final /* synthetic */ TekBeforeRenderCallback b;

        y(TekBeforeRenderCallback tekBeforeRenderCallback) {
            this.b = tekBeforeRenderCallback;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "setBeforeRenderCallback 00");
            TekEffectThread.this._beforeRenderCallback = this.b;
        }
    }

    class z implements Runnable {
        final /* synthetic */ int b;

        z(int i) {
            this.b = i;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekLog.write(TekEffectThread.LOGTAG, "updateEffectFillMode 00.");
            if (TekEffectThread.this._render != null) {
                TekEffectThread.this._render.H(this.b);
            }
        }
    }

    public TekEffectThread() {
        TekProxyLog.i(LOGTAG, "construct.");
    }

    private int createContext() {
        TekLog.write(LOGTAG, "createContext");
        TekEngineLib.Manager.d dVar = this._context;
        if (dVar != null && dVar.f()) {
            return 0;
        }
        this._render = null;
        TekEngineLib.Manager.d dVar2 = new TekEngineLib.Manager.d();
        this._context = dVar2;
        return dVar2.b(this._isEnableDepth);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void destroyRenderSurfaceInner() {
        TekProxyLog.i(LOGTAG, "destorySurface 11. " + this.hasDestroySurface.get());
        if (this.hasDestroySurface.get()) {
            return;
        }
        this.hasDestroySurface.set(true);
        if (this._render != null) {
            this._render.A();
        }
        TekEngineLib.Manager.d dVar = this._context;
        if (dVar != null) {
            dVar.d();
        }
        SurfaceTexture surfaceTexture = this._surfaceTexture;
        if (surfaceTexture != null) {
            if (Build.VERSION.SDK_INT < 26) {
                surfaceTexture.release();
            } else if (!surfaceTexture.isReleased()) {
                surfaceTexture = this._surfaceTexture;
                surfaceTexture.release();
            }
            this._surfaceTexture = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doForceRender() {
        if (this._render == null || this._context == null || this._surfaceTexture == null) {
            TekLog.write(LOGTAG, "forceRender _render == null.");
            return;
        }
        boolean zS = this._render.S();
        this._render.C(true);
        int iT = this._render.T();
        if (iT == 0) {
            this._context.h();
            if (this._firstFrameFlag) {
                sendStateListener(TekState.ON_FIRST_FRAME, null, 0);
                this._firstFrameFlag = false;
            }
        } else if (iT < 0) {
            this._runningState = TekEngineLib.Manager.c.b.IDLEING;
            sendStateListener(TekState.RENDER_ERROR, null, iT);
        }
        this._render.C(zS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int doUpdateEffectResource(String str, String str2) {
        TekLog.write(LOGTAG, "doUpdateEffectResource:" + str + " inputFileName:" + str2);
        int iT = this._render.t(str, str2);
        if (iT == 0) {
            this._render.m(this);
            TekEffectInfo tekEffectInfo = new TekEffectInfo();
            tekEffectInfo._width = this._render.N();
            tekEffectInfo._height = this._render.M();
            tekEffectInfo._framerate = this._render.O();
            tekEffectInfo._duration = (this._render.L() * 1000) / tekEffectInfo._framerate;
            TekLog.write(LOGTAG, "_width:" + tekEffectInfo._width + " _height:" + tekEffectInfo._height + " _framerate:" + tekEffectInfo._framerate + " _duration:" + tekEffectInfo._duration);
            int iP = this._render.P();
            if (iP > 0) {
                tekEffectInfo._layers = new ArrayList<>();
                for (int i2 = 0; i2 < iP; i2++) {
                    TekEffectLayerInfo tekEffectLayerInfo = new TekEffectLayerInfo();
                    tekEffectLayerInfo._layerIndex = i2;
                    tekEffectLayerInfo._type = this._render.D(i2);
                    tekEffectLayerInfo._frameDruation = this._render.a(i2);
                    tekEffectLayerInfo._hasKeyPoints = this._render.y(i2);
                    tekEffectLayerInfo._name = this._render.z(i2);
                    tekEffectInfo._layers.add(tekEffectLayerInfo);
                }
            }
            sendStateListener(TekState.OPEN_EFFECT_RESOURCE_SUCCESS, tekEffectInfo, 0);
        } else {
            sendStateListener(TekState.OPEN_EFFECT_RESOURCE_FAILED, null, iT);
        }
        return iT;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int doUpdateEffectWithCustom(TekCustomLayer tekCustomLayer) {
        TekLog.write(LOGTAG, "doUpdateEffectWithCustom:");
        int iC = this._render.c(tekCustomLayer);
        if (iC == 0) {
            TekEffectInfo tekEffectInfo = new TekEffectInfo();
            tekEffectInfo._width = this._render.N();
            tekEffectInfo._height = this._render.M();
            tekEffectInfo._framerate = this._render.O();
            tekEffectInfo._duration = (this._render.L() * 1000) / tekEffectInfo._framerate;
            TekLog.write(LOGTAG, "_width:" + tekEffectInfo._width + " _height:" + tekEffectInfo._height + " _framerate:" + tekEffectInfo._framerate + " _duration:" + tekEffectInfo._duration);
            sendStateListener(TekState.OPEN_EFFECT_RESOURCE_SUCCESS, tekEffectInfo, 0);
        } else {
            sendStateListener(TekState.OPEN_EFFECT_RESOURCE_FAILED, null, iC);
        }
        return iC;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isRenderAvailable() {
        TekEngineLib.Manager.d dVar;
        return (this._render == null || (dVar = this._context) == null || !dVar.g() || this._surfaceTexture == null) ? false : true;
    }

    private void renderStatistic(long j2) {
        this.mRenderConsumTime += j2;
        long j3 = this.mRenderFrame + 1;
        this.mRenderFrame = j3;
        if (j3 != 90 || this._render == null) {
            return;
        }
        TekEffectInfo tekEffectInfo = new TekEffectInfo();
        tekEffectInfo._width = this._width;
        tekEffectInfo._height = this._height;
        tekEffectInfo._framerate = this._render.O();
        tekEffectInfo._consumePerFrame = (int) (this.mRenderConsumTime / this.mRenderFrame);
        sendStateListener(TekState.RENDER_SATTISTIC, tekEffectInfo, 0);
    }

    private void sendBeforeRenderCallback(long j2) {
        TekBeforeRenderCallback tekBeforeRenderCallback = this._beforeRenderCallback;
        if (tekBeforeRenderCallback != null) {
            tekBeforeRenderCallback.onBeforRender(j2);
        }
    }

    private void sendRenderProgressListener(long j2, int i2, int i3) {
        if (this._renderProgressListener != null) {
            this._renderProgressListener.onProgressListener(j2, i2, i3);
        }
        if (i2 >= 100 && !this._hasSendPlayOver) {
            this._hasSendPlayOver = true;
            sendStateListener(TekState.PLAY_OVER, null, 0);
        }
        if (i2 < 100) {
            this._hasSendPlayOver = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendStateListener(TekState tekState, TekEffectInfo tekEffectInfo, int i2) {
        if (this._stateListener != null) {
            this._stateListener.onStateChange(tekState, tekEffectInfo, i2);
        }
    }

    private long waitTimeForDraw(float f2) {
        if (f2 < 16.0f) {
            f2 = 16.0f;
        }
        if (f2 > 60.0f) {
            f2 = 60.0f;
        }
        long j2 = (long) (1000.0f / (this._playSpeed * f2));
        long jCurrentTimeMillis = System.currentTimeMillis() - this._lastRenderTime;
        long j3 = 1;
        long j4 = jCurrentTimeMillis < j2 ? j2 - jCurrentTimeMillis : 1L;
        if (j4 >= 1) {
            j3 = j4 > 200 ? 200L : j4;
        }
        waitfor(j3);
        TekLog.write(LOGTAG, "doDraw 00:" + String.valueOf(jCurrentTimeMillis) + ":" + String.valueOf(j3));
        return j3;
    }

    public void addInputImageRefs(ArrayList<TekAdditionImageRef> arrayList) {
        TekLog.write(LOGTAG, "addInputImageRefs:");
        addRunable(new f(arrayList));
    }

    public void addLayer(String str) {
        TekLog.write(LOGTAG, "addLayer:");
        addRunable(new l(str));
    }

    public void addRefImage(TekAdditionImageRef tekAdditionImageRef) {
        TekLog.write(LOGTAG, "addRefImage.");
        addRunable(new t(tekAdditionImageRef));
    }

    public void appendInputImages(ArrayList<String> arrayList) {
        TekLog.write(LOGTAG, "appendInputImages:");
        addRunable(new d(arrayList));
    }

    public void cleanCache() {
        TekLog.write(LOGTAG, "cleanCache.");
        addRunable(new e0());
    }

    public void cleanLyric() {
        TekLog.write(LOGTAG, "cleanLyric");
        addRunable(new v());
    }

    public void cleanParam(String str, String str2) {
        addRunable(new g(str, str2));
    }

    public void clearView() {
        TekLog.write(LOGTAG, "clearView.");
        addRunable(new b0());
    }

    public void closeEffect() {
        TekProxyLog.i(LOGTAG, "closeEffect");
        addRunable(new w());
    }

    public void createRenderSurface(SurfaceTexture surfaceTexture, int i2, int i3) {
        TekLog.write(LOGTAG, "createRenderSurface.");
        addRunable(new q(surfaceTexture, i2, i3));
    }

    public void destory() {
        TekProxyLog.i(LOGTAG, "destory.");
        stopRun();
    }

    public void destoryRenderSurface() {
        TekLog.write(LOGTAG, "destorySurface.");
        forceRun(new m());
    }

    @Override // TekEngineLib.Manager.c
    public void doDraw() {
        if (this._fastPauseFlag) {
            TekProxyLog.i(LOGTAG, "doDraw _fastPauseFlag.");
            waitfor(5L);
            return;
        }
        if (!isRenderAvailable()) {
            TekProxyLog.i(LOGTAG, "doDraw _render == null.");
            waitfor(30L);
            return;
        }
        int iO = this._render.O();
        if (iO <= 0) {
            TekLog.write(LOGTAG, "doDraw 11.");
            waitfor(30L);
            return;
        }
        byte[] bArr = this._fftData;
        if (bArr != null && bArr.length > 1 && this._fftSamplerate > 1) {
            this._ffDataLock.lock();
            byte[] bArr2 = this._fftData;
            if (bArr2 != null && bArr2.length > 1 && this._fftSamplerate > 1) {
                this._render.s(this._fftData, this._fftSamplerate);
            }
            this._fftData = null;
            this._ffDataLock.unlock();
        }
        TekTimestampGetter tekTimestampGetter = this._timestampGetter;
        if (tekTimestampGetter != null) {
            this._render.g(tekTimestampGetter.timestamp());
        }
        long jX = this._render.X();
        sendBeforeRenderCallback(jX);
        if (this.isStopRenderMainThread) {
            this._runningState = TekEngineLib.Manager.c.b.IDLEING;
            return;
        }
        if (this._fastPauseFlag) {
            TekLog.write(LOGTAG, "doDraw _fastPauseFlag 0000.");
            return;
        }
        int iT = this._render.T();
        if (iT != 0) {
            if (iT < 0) {
                this._runningState = TekEngineLib.Manager.c.b.IDLEING;
                sendStateListener(TekState.RENDER_ERROR, null, iT);
                return;
            }
            return;
        }
        long jWaitTimeForDraw = waitTimeForDraw(iO);
        renderStatistic(jWaitTimeForDraw);
        sendRenderProgressListener(jX, this._render.R(), (int) jWaitTimeForDraw);
        long jCurrentTimeMillis = System.currentTimeMillis();
        int iH = this._context.h();
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        this._lastRenderTime = jCurrentTimeMillis2;
        long j2 = jCurrentTimeMillis2 - jCurrentTimeMillis;
        TekProxyLog.f(LOGTAG, "swapBuffers diff:" + j2);
        if (iH != 0) {
            this._runningState = TekEngineLib.Manager.c.b.IDLEING;
            sendStateListener(TekState.RENDER_ERROR, null, iH);
        } else if (this._firstFrameFlag) {
            sendStateListener(TekState.ON_FIRST_FRAME, null, 0);
            this._firstFrameFlag = false;
        }
    }

    @Override // TekEngineLib.Manager.c
    public void doRelease() {
        super.doRelease();
        destroyRenderSurfaceInner();
        TekProxyLog.i(LOGTAG, "doRelease.");
        this._stateListener = null;
        this._renderProgressListener = null;
        this._beforeRenderCallback = null;
        this._timestampGetter = null;
        this._gyroscopeListeners.clear();
        if (this._render != null) {
            this._render.I();
            this._render = null;
        }
        TekEngineLib.Manager.d dVar = this._context;
        if (dVar != null) {
            dVar.c();
            this._context = null;
        }
    }

    public void enableAudioTimestamp(boolean z2) {
        TekLog.write(LOGTAG, "enableAudioTimestamp:" + String.valueOf(z2));
        addRunable(new i(z2));
    }

    public void forceRender() {
        TekLog.write(LOGTAG, "forceRender.");
        addRunable(new p());
    }

    public long getAudioTimestamp() {
        TekLog.write(LOGTAG, "getAudioTimestamp");
        if (this._render != null) {
            return this._render.K();
        }
        TekLog.write(LOGTAG, "getAudioTimestamp _render == null.");
        return 0L;
    }

    public int getFrameHeight() {
        TekLog.write(LOGTAG, "getFrameHeight");
        if (this._render != null) {
            return this._render.M();
        }
        TekLog.write(LOGTAG, "getFrameHeight _render == null");
        return 0;
    }

    public int getFrameWidth() {
        TekLog.write(LOGTAG, "getFrameWidth");
        if (this._render != null) {
            return this._render.N();
        }
        TekLog.write(LOGTAG, "getFrameWidth _render == null");
        return 0;
    }

    public int getOutputTexture() {
        TekLog.write(LOGTAG, "getOutputTexture");
        if (this._render != null) {
            return this._render.Q();
        }
        TekLog.write(LOGTAG, "getOutputTexture _render == null");
        return 0;
    }

    public void innerCreateRenderSurface(SurfaceTexture surfaceTexture, int i2, int i3) {
        TekProxyLog.i(LOGTAG, "innerCreateRenderSurface.");
        this._firstFrameFlag = true;
        this._width = i2;
        this._height = i3;
        if (Build.VERSION.SDK_INT >= 26 && surfaceTexture.isReleased()) {
            this._surfaceTexture = null;
            sendStateListener(TekState.INIT_GL_FAILED, null, TekErrorCode.SURFACETEXTURE_RELEASED);
            TekProxyLog.e(LOGTAG, "createRenderSurface s.isReleased.");
            return;
        }
        this._surfaceTexture = surfaceTexture;
        int iCreateContext = createContext();
        if (iCreateContext < 0) {
            TekProxyLog.e(LOGTAG, "createRenderSurface error 1.");
            sendStateListener(TekState.INIT_GL_FAILED, null, iCreateContext);
            this._context.c();
            this._context = null;
            this._runningState = TekEngineLib.Manager.c.b.IDLEING;
            return;
        }
        int iA = this._context.a(surfaceTexture);
        if (iA < 0) {
            TekProxyLog.e(LOGTAG, "createRenderSurface error 2.");
            sendStateListener(TekState.INIT_GL_FAILED, null, iA);
            this._runningState = TekEngineLib.Manager.c.b.IDLEING;
            return;
        }
        if (this._render == null) {
            this._render = new TekEngineLib.Render.b();
            this._render.h(this._shader);
            this._render.x(this._isDirectRenderView);
            this._render.U();
            this._render.f(this._width, this._height);
        } else {
            this._render.f(this._width, this._height);
            if (this._inputImageRefUpdated) {
                this._render.q(this._inputImageRefs);
                this._inputImageRefUpdated = false;
            }
            doForceRender();
        }
        this._runningState = TekEngineLib.Manager.c.b.IDLEING;
        sendStateListener(TekState.INIT_GL_SUCCESS, null, 0);
    }

    public void onReceiveEvent(String str) {
        if (this._eventListener != null) {
            this._eventListener.onEvent(str);
        }
    }

    public void onSurfaceChanged(int i2, int i3) {
        TekLog.write(LOGTAG, "onSurfaceChanged:" + String.valueOf(i2) + ":" + String.valueOf(i3));
        addRunable(new r(i2, i3));
    }

    public void registerGyroscopeDataListener(long j2) {
        String str;
        StringBuilder sb;
        Iterator<k> it = this._gyroscopeListeners.iterator();
        while (it.hasNext()) {
            if (it.next().f8a == j2) {
                str = LOGTAG;
                sb = new StringBuilder();
                sb.append("registerGyroscopeDataListener: listener with helperPoint ");
                sb.append(j2);
                sb.append(" already exists");
                TekProxyLog.i(str, sb.toString());
            }
        }
        this._gyroscopeListeners.add(new k(j2));
        str = LOGTAG;
        sb = new StringBuilder();
        sb.append("registerGyroscopeDataListener: added listener with helperPoint ");
        sb.append(j2);
        TekProxyLog.i(str, sb.toString());
    }

    public void resetTimestamp() {
        TekLog.write(LOGTAG, "resetTimestamp:");
        addRunable(new j());
    }

    public void sendCallback(int i2, int i3, String str, Object obj) {
        if (this._stateListener != null) {
            TekEffectInfo tekEffectInfo = new TekEffectInfo();
            tekEffectInfo._message = str;
            tekEffectInfo._data = obj;
            if (i2 < 0) {
                this._stateListener.onStateChange(TekState.RENDER_ERROR, tekEffectInfo, i2);
            } else {
                this._stateListener.onStateChange(TekState.MESSAGE, tekEffectInfo, 0);
            }
        }
    }

    public void sendCallback(String str) {
        if (this._stateListener != null) {
            TekEffectInfo tekEffectInfo = new TekEffectInfo();
            tekEffectInfo._message = str;
            this._stateListener.onStateChange(TekState.MESSAGE, tekEffectInfo, 0);
        }
    }

    public void setAdditionInfo(TekAdditionInfo tekAdditionInfo) {
        TekLog.write(LOGTAG, "setAdditionInfo.");
        addRunable(new s(tekAdditionInfo));
    }

    public void setBeforeRenderCallback(TekBeforeRenderCallback tekBeforeRenderCallback) {
        TekLog.write(LOGTAG, "setBeforeRenderCallback");
        addRunable(new y(tekBeforeRenderCallback));
    }

    public void setCryptKey(String str) {
        TekLog.write(LOGTAG, "setCryptKey:" + str);
        addRunable(new f0(str));
    }

    public void setEventListener(TekEventListener tekEventListener) {
        this._eventListener = tekEventListener;
    }

    public void setFrameUpdateListener(TekEngineLib.Render.a aVar) {
        TekLog.write(LOGTAG, "setFrameUpdateListener：" + aVar);
        addRunable(new d0(aVar));
    }

    public void setIsDirectRenderView(boolean z2) {
        TekLog.write(LOGTAG, "setIsDirectRenderView：" + z2);
        this._isDirectRenderView = z2;
        addRunable(new c0(z2));
    }

    public void setIsEnableDepth(boolean z2) {
        TekLog.write(LOGTAG, "setIsEnableDepth：" + z2);
        this._isEnableDepth = z2;
    }

    public void setRenderProgressListener(TekRenderProgressListener tekRenderProgressListener) {
        TekLog.write(LOGTAG, "setRenderProgressListener");
        addRunable(new x(tekRenderProgressListener));
    }

    public void setShader(TekBaseShader tekBaseShader) {
        TekLog.write(LOGTAG, "setShader");
        this._shader = tekBaseShader;
    }

    public void setSpeed(float f2) {
        TekLog.write(LOGTAG, "setSpeed:" + String.valueOf(f2));
        if (this._render == null) {
            TekLog.write(LOGTAG, "setSpeed _render == null.");
        } else {
            this._render.e(f2);
        }
    }

    public void setStateListener(TekStateListener tekStateListener) {
        TekLog.write(LOGTAG, "setStateListener");
        this._stateListener = tekStateListener;
    }

    public void setTimestampGetter(TekTimestampGetter tekTimestampGetter) {
        TekLog.write(LOGTAG, "setTimestampGetter");
        this._timestampGetter = tekTimestampGetter;
    }

    public void startRender() {
        TekLog.write(LOGTAG, "startRender.");
        this._fastPauseFlag = false;
        this.isStopRenderMainThread = false;
        addRunable(new n());
    }

    public void stopRender() {
        TekLog.write(LOGTAG, "stopRender.");
        this._fastPauseFlag = true;
        this.isStopRenderMainThread = true;
        this._ffDataLock.lock();
        this._fftData = null;
        this._fftSamplerate = 0;
        this._ffDataLock.unlock();
        addRunable(new o());
    }

    public void updateAudioPlaying(boolean z2) {
        TekLog.write(LOGTAG, "updateAudioPlaying");
        if (this._render == null) {
            TekLog.write(LOGTAG, "updateAudioPlaying _render == null.");
        } else {
            this._render.F(z2);
        }
    }

    public void updateAudioTimestamp(long j2) {
        TekLog.write(LOGTAG, "updateAudioTimestamp");
        if (j2 < 0) {
            TekLog.write(LOGTAG, "updateAudioTimestamp t < 0.");
        }
        if (this._render == null) {
            TekLog.write(LOGTAG, "updateAudioTimestamp _render == null.");
        } else {
            this._render.g(j2);
        }
    }

    public void updateConfig(TekEffectConfig tekEffectConfig) {
        TekLog.write(LOGTAG, "updateConfig:");
        addRunable(new h(tekEffectConfig));
    }

    public void updateEffectFillMode(int i2) {
        TekLog.write(LOGTAG, "updateEffectFillMode");
        addRunable(new z(i2));
    }

    public int updateEffectResource(String str, String str2) {
        TekLog.write(LOGTAG, "updateResource:" + str + " inputFileName:" + str2);
        this._fastPauseFlag = false;
        addRunable(new a(str, str2));
        return 0;
    }

    public int updateEffectWithCustom(TekCustomLayer tekCustomLayer) {
        TekLog.write(LOGTAG, "updateEffectWithCustom:");
        addRunable(new c(tekCustomLayer));
        return 0;
    }

    public void updateFFTData(byte[] bArr, int i2) {
        if (bArr == null || bArr.length < 1 || i2 < 1) {
            return;
        }
        this._ffDataLock.lock();
        this._fftData = bArr;
        this._fftSamplerate = i2;
        this._ffDataLock.unlock();
    }

    public void updateGyroscopeData(TekGyroscopeData tekGyroscopeData) {
        TekLog.write(LOGTAG, "onGyroscopeDataUpdated");
        addRunable(new a0(tekGyroscopeData));
    }

    public void updateInputImages(ArrayList<String> arrayList) {
        TekLog.write(LOGTAG, "addInputImages:");
        addRunable(new b(arrayList));
    }

    public void updateLyricParam(TekLyricParam tekLyricParam) {
        TekLog.write(LOGTAG, "updateLyricParam");
        addRunable(new u(tekLyricParam));
    }

    public void updateParam(String str, Map<String, Object> map) {
        e eVar = new e(str, map);
        if (map.get("WithOutNotify") == null) {
            addRunable(eVar);
        } else {
            addRunableWithOutNotify(eVar);
        }
    }

    public void updatePlaySpeed(float f2) {
        this._playSpeed = f2;
        if (f2 < 0.1f) {
            this._playSpeed = 0.1f;
        }
    }
}
