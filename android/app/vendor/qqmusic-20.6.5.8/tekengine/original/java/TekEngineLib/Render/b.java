package TekEngineLib.Render;

import TekEngineLib.Engine.TekEngineWrapper;
import TekEngineLib.Interface.UsualParamInfo;
import TekEngineLib.Lyric.TekLyricParam;
import TekEngineLib.State.TekAdditionImageRef;
import TekEngineLib.State.TekAdditionInfo;
import TekEngineLib.State.TekAdditionLayerInfo;
import TekEngineLib.State.TekCustomLayer;
import TekEngineLib.State.TekEffectConfig;
import TekEngineLib.State.TekEffectLayerType;
import TekEngineLib.State.TekErrorCode;
import TekEngineLib.State.TekLog;
import TekEngineLib.State.TekProxyLog;
import android.opengl.GLES20;
import com.tencent.ttpic.baseutils.io.FileUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import okhttp3.HttpUrl;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class b {
    private static String s = "TEK TekRender";
    private String h;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private volatile TekEngineWrapper f18a = null;
    private TekBaseShader b = null;
    private c c = null;
    private int d = 0;
    private int e = 0;
    private volatile int f = 0;
    private long g = 0;
    private a i = null;
    private volatile boolean j = false;
    private long k = 0;
    private volatile boolean l = false;
    private volatile long m = 0;
    private volatile long n = 0;
    private volatile boolean o = true;
    private volatile float p = 1.0f;
    private boolean q = true;
    public int r = 0;

    /* JADX WARN: Type inference fix 'apply assigned field type' failed
    java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
    	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:596)
    	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
    	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
     */
    private void G() {
        TekLog.write(s, "create");
        if (this.c != null && this.f18a != null) {
            TekLog.write(s, "create had create");
            return;
        }
        this.c = new c();
        this.f18a = new TekEngineWrapper();
        if (this.b != null) {
            this.f18a.updateUpdaterImpl(this.b);
        }
        this.f18a.setIsDirectRenderView(this.q ? 1 : 0);
    }

    private int J() {
        this.r = 0;
        V();
        if (this.f18a == null) {
            TekLog.write(s, "doUpdateResource _engine == null.");
            return TekErrorCode.RENDER_ENGINE_NULL;
        }
        TekLog.write(s, "doUpdateResource:" + this.h);
        int iInitWithJSON = this.f18a.initWithJSON(this.h);
        if (iInitWithJSON != 0) {
            this.f = 0;
            TekLog.write(s, "initWihtJSON failed");
            return iInitWithJSON;
        }
        this.f = this.f18a.getFramerate();
        HashMap map = new HashMap();
        map.put("uiViewWidth", new Float(this.d));
        map.put("uiViewHeight", new Float(this.e));
        this.f18a.updateParam(UsualParamInfo.kTargetLayerAll, map);
        this.q = this.f18a.getIsDirectRenderView();
        TekLog.write(s, "initWihtJSON success");
        return 0;
    }

    public void A() {
        TekLog.write(s, "cleanTextureCache");
        if (this.f18a != null) {
            this.f18a.cleanTextureCache();
        }
    }

    public void B(ArrayList<String> arrayList) {
        String str;
        String str2;
        TekLog.write(s, "updateInputImages");
        if (this.f18a == null) {
            str = s;
            str2 = "updateInputImages _engine == null";
        } else {
            this.f18a.cleanInputImage();
            if (arrayList != null) {
                for (int i = 0; i < arrayList.size(); i++) {
                    this.f18a.addInputImage(arrayList.get(i));
                }
                return;
            }
            str = s;
            str2 = "updateInputImages inputPaths == null";
        }
        TekLog.write(str, str2);
    }

    public void C(boolean z) {
        this.j = z;
        TekLog.write(s, "setRuning:" + String.valueOf(z));
    }

    public TekEffectLayerType D(int i) {
        if (this.f18a != null) {
            return this.f18a.getLayerType(i);
        }
        TekLog.write(s, "getLayerType _engine == null");
        return TekEffectLayerType.TEK_INFO_LAYER_TYPE_COVER;
    }

    public void E() {
        int i;
        int i2;
        TekLog.write(s, "clearView");
        c cVar = this.c;
        if (cVar == null || (i = this.d) <= 0 || (i2 = this.e) <= 0) {
            return;
        }
        cVar.h(i, i2);
    }

    public void F(boolean z) {
        TekLog.write(s, "updateAudioPlaying:" + z);
        this.o = z;
    }

    public void H(int i) {
        TekLog.write(s, "updateEffectFillMode");
        if (this.f18a != null) {
            this.f18a.updateEffectFillMode(i);
        }
    }

    public void I() {
        TekProxyLog.i(s, "destory" + Thread.currentThread().getName());
        this.j = false;
        c cVar = this.c;
        if (cVar != null) {
            cVar.b();
            this.c = null;
        }
        if (this.f18a != null) {
            this.f18a.destory();
            this.f18a = null;
        }
        this.f = 0;
        this.d = 0;
        this.e = 0;
    }

    public long K() {
        TekLog.write(s, "getAudioTimestamp:" + String.valueOf(this.k));
        return this.k;
    }

    public int L() {
        if (this.f18a == null) {
            return 0;
        }
        return this.f18a.getEndFrame();
    }

    public int M() {
        if (this.f18a == null) {
            return 0;
        }
        return this.f18a.getContentHeight();
    }

    public int N() {
        if (this.f18a == null) {
            return 0;
        }
        return this.f18a.getContentWidth();
    }

    public int O() {
        return this.f;
    }

    public int P() {
        if (this.f18a != null) {
            return this.f18a.getLayerCount();
        }
        TekLog.write(s, "getLayerCount _engine == null");
        return 0;
    }

    public int Q() {
        if (this.f18a == null) {
            return -1;
        }
        return this.f18a.getOutputTexture();
    }

    public int R() {
        if (this.f18a != null) {
            return this.f18a.getProgress();
        }
        return 0;
    }

    public boolean S() {
        return this.j;
    }

    public int T() {
        if (this.f18a == null || this.f <= 0 || this.d <= 0 || this.e <= 0 || this.c == null) {
            TekLog.write(s, "onDrawFrame null");
            return 1;
        }
        if (this.r < 0) {
            TekLog.write(s, "_engineStatus < 0:" + this.r);
            return this.r;
        }
        if (!this.j) {
            this.g = 0L;
            TekLog.write(s, "onDrawFrame !_running");
            return 2;
        }
        W();
        if (this.q) {
            this.c.e(this.d, this.e);
            a aVar = this.i;
            if (aVar != null) {
                aVar.a(this.k);
            }
            this.r = this.f18a.updateWithTimestampWithRet(this.k);
            a aVar2 = this.i;
            if (aVar2 != null) {
                aVar2.a(this.k, this.f18a.getOutputTexture(), this.d, this.e, this.q);
            }
            return this.r;
        }
        int viewWidth = this.f18a.getViewWidth();
        int viewHeight = this.f18a.getViewHeight();
        int contentWidth = this.f18a.getContentWidth();
        int contentHeight = this.f18a.getContentHeight();
        int x = this.f18a.getX();
        int y = this.f18a.getY();
        if (viewWidth <= 0 || viewHeight <= 0) {
            TekLog.write(s, "onDrawFrame frameWidth <= 0");
            return 3;
        }
        TekLog.write(s, "onDrawFrame:" + String.valueOf(this.k));
        a aVar3 = this.i;
        if (aVar3 != null) {
            aVar3.a(this.k);
        }
        int iUpdateWithTimestampWithRet = this.f18a.updateWithTimestampWithRet(this.k);
        this.r = iUpdateWithTimestampWithRet;
        if (iUpdateWithTimestampWithRet != 0) {
            return iUpdateWithTimestampWithRet;
        }
        int outputTexture = this.f18a.getOutputTexture();
        if (outputTexture <= 0) {
            return 4;
        }
        this.c.f(this.d, this.e, viewWidth, viewHeight, contentWidth, contentHeight, x, y);
        GLES20.glActiveTexture(33986);
        GLES20.glBindTexture(3553, outputTexture);
        this.c.d(2);
        this.c.k();
        this.c.g();
        GLES20.glBindTexture(3553, 0);
        a aVar4 = this.i;
        if (aVar4 != null) {
            aVar4.a(this.k, outputTexture, viewWidth, viewHeight, this.q);
        }
        return 0;
    }

    public void U() {
        TekLog.write(s, "onSurfaceCreated:" + Thread.currentThread().getName());
        G();
    }

    public void V() {
        TekLog.write(s, "resetTimestamp");
        this.g = 0L;
        this.k = 0L;
        this.n = -1L;
        this.m = System.currentTimeMillis();
    }

    public void W() {
        TekBaseShader tekBaseShader;
        if (this.f18a == null || (tekBaseShader = this.b) == null) {
            return;
        }
        this.f18a.updateParmaMap(tekBaseShader.getParamMap());
    }

    public long X() {
        String str;
        String str2;
        if (!this.l) {
            if (this.f <= 0 || this.g < 0) {
                this.k = 0L;
            } else if (this.o) {
                this.k = (long) ((1000.0f / this.f) * this.g * this.p);
                TekLog.write(s, "updateTimestamp _timestamp:" + String.valueOf(this.k));
                long j = this.g + 1;
                this.g = j;
                if (j > 100000000) {
                    this.g = 0L;
                }
            } else {
                str = s;
                str2 = "updateTimestamp _externalAudioPlaying = false";
            }
            return this.k;
        }
        if (this.n < 0) {
            TekLog.write(s, "_isEnableExternAudioTimestamp updateTimestamp _externalAudioTime < 0:");
            return 0L;
        }
        if (this.o) {
            long jCurrentTimeMillis = System.currentTimeMillis() - this.m;
            this.k = this.n + ((long) (jCurrentTimeMillis * this.p));
            TekLog.write(s, "_isEnableExternAudioTimestamp updateTimestamp _externalAudioTime:" + String.valueOf(this.k) + ":" + String.valueOf(jCurrentTimeMillis));
            if (this.k > 18000000) {
                this.k = 0L;
            }
            return this.k;
        }
        str = s;
        str2 = "_isEnableExternAudioTimestamp updateTimestamp _externalAudioPlaying = false";
        TekLog.write(str, str2);
        return this.k;
    }

    public int a(int i) {
        if (this.f18a != null) {
            return this.f18a.getLayerDuration(i);
        }
        TekLog.write(s, "getLayerDuration _engine == null");
        return 0;
    }

    public int b(TekLyricParam tekLyricParam) {
        if (this.f18a != null) {
            return this.f18a.updateLyricParam(tekLyricParam);
        }
        TekLog.write(s, "updateLyricParam _engine == null");
        return 0;
    }

    public int c(TekCustomLayer tekCustomLayer) {
        this.r = 0;
        V();
        if (this.f18a == null) {
            TekLog.write(s, "updateCustomLayer _engine == null.");
            return TekErrorCode.RENDER_ENGINE_NULL;
        }
        TekLog.write(s, "updateCustomLayer:");
        int iInitWithCustomLayer = this.f18a.initWithCustomLayer(tekCustomLayer);
        if (iInitWithCustomLayer == 0) {
            this.f = this.f18a.getFramerate();
            TekLog.write(s, "initWihtJSON success");
            return 0;
        }
        this.f = 0;
        TekLog.write(s, "initWihtJSON failed");
        return iInitWithCustomLayer;
    }

    public void d() {
        TekLog.write(s, "cleanCache");
        if (this.f18a != null) {
            this.f18a.cleanAllCache();
        }
    }

    public void e(float f) {
        TekLog.write(s, "setSpeed:" + String.valueOf(f));
        this.p = f;
    }

    public void f(int i, int i2) {
        TekLog.write(s, "onSurfaceChanged:" + i + "," + i2);
        this.d = i;
        this.e = i2;
        c cVar = this.c;
        if (cVar != null) {
            cVar.j(i, i2);
        }
        if (this.f18a != null) {
            HashMap map = new HashMap();
            map.put("uiViewWidth", new Float(this.d));
            map.put("uiViewHeight", new Float(this.e));
            this.f18a.updateParam(UsualParamInfo.kTargetLayerAll, map);
        }
    }

    public void g(long j) {
        TekLog.write(s, "updateAudioTimestamp:" + String.valueOf(j));
        this.l = true;
        this.m = System.currentTimeMillis();
        this.n = j;
        this.k = j;
    }

    public void h(TekBaseShader tekBaseShader) {
        this.b = tekBaseShader;
        if (this.f18a != null) {
            this.f18a.updateUpdaterImpl(this.b);
        }
    }

    public void i(a aVar) {
        TekLog.write(s, "setFrameUpdateListener");
        this.i = aVar;
    }

    public void j(TekAdditionImageRef tekAdditionImageRef) {
        TekLog.write(s, "addRefImage");
        if (this.f18a == null || tekAdditionImageRef == null) {
            TekLog.write(s, "addRefImage _engine == null");
        } else {
            this.f18a.addUserDefineLayerImageRef2(tekAdditionImageRef._refImageName, tekAdditionImageRef._imagePath, tekAdditionImageRef._fillMode.ordinal());
        }
    }

    public void k(TekAdditionInfo tekAdditionInfo) {
        TekLog.write(s, "setAdditionInfo");
        if (this.f18a == null || tekAdditionInfo == null) {
            TekLog.write(s, "setAdditionInfo _engine == null");
            return;
        }
        if (tekAdditionInfo.audioID != null) {
            this.f18a.setUUID(tekAdditionInfo.audioID);
        }
        ArrayList<TekAdditionLayerInfo> arrayList = tekAdditionInfo._userDefinelayers;
        if (arrayList == null || arrayList.size() <= 0) {
            return;
        }
        this.f18a.cleanUserDefineOrderLayers();
        for (int i = 0; i < tekAdditionInfo._userDefinelayers.size(); i++) {
            TekAdditionLayerInfo tekAdditionLayerInfo = tekAdditionInfo._userDefinelayers.get(i);
            TekLog.write(s, "addUserDefineOrderLayer," + tekAdditionLayerInfo._layerIndex);
            this.f18a.addUserDefineOrderLayer(tekAdditionLayerInfo._layerIndex);
            ArrayList<TekAdditionImageRef> arrayList2 = tekAdditionLayerInfo._imageRefs;
            if (arrayList2 != null && arrayList2.size() > 0) {
                for (int i2 = 0; i2 < tekAdditionLayerInfo._imageRefs.size(); i2++) {
                    TekAdditionImageRef tekAdditionImageRef = tekAdditionLayerInfo._imageRefs.get(i2);
                    this.f18a.addUserDefineLayerImageRef(tekAdditionImageRef._imagePath, tekAdditionImageRef._fillMode.ordinal());
                }
            }
        }
        this.f18a.updateRunningLayerInfo();
    }

    public void l(TekEffectConfig tekEffectConfig) {
        TekLog.write(s, "updateConfig");
        if (this.f18a == null) {
            TekLog.write(s, "updateConfig _engine == null");
        } else {
            this.f18a.updateConfig(tekEffectConfig);
        }
    }

    public void m(Object obj) {
        if (this.f18a == null) {
            TekLog.write(s, "setDelegate _engine == null");
        } else {
            this.f18a.setDelegate(obj);
        }
    }

    public void n(String str) {
        TekLog.write(s, "addLayer");
        if (this.f18a == null) {
            TekLog.write(s, "addLayer _engine == null");
        } else {
            this.f18a.addLayer(str);
        }
    }

    public void o(String str, String str2) {
        if (this.f18a == null) {
            TekLog.write(s, "cleanParam _engine == null.");
        } else {
            this.f18a.cleanParam(str, str2);
        }
    }

    public void p(String str, Map<String, Object> map) {
        if (this.f18a == null) {
            TekLog.write(s, "updateParam _engine == null.");
        } else {
            this.f18a.updateParam(str, map);
        }
    }

    public void q(ArrayList<TekAdditionImageRef> arrayList) {
        TekLog.write(s, "addInputImageRefs");
        if (this.f18a == null) {
            TekLog.write(s, "addInputImageRefs _engine == null");
        } else {
            this.f18a.addInputImageRefs(arrayList);
        }
    }

    public void r(boolean z) {
        TekLog.write(s, "setEnableExternAudioTimestamp:" + String.valueOf(z));
        this.l = z;
    }

    public void s(byte[] bArr, int i) {
        if (this.f18a == null) {
            TekLog.write(s, "updateFFTData _engine == null");
        } else {
            this.f18a.updateFFTData(bArr, i);
        }
    }

    public int t(String str, String str2) {
        this.h = str + FileUtils.RES_PREFIX_STORAGE + str2;
        TekLog.write(s, "updateResource:" + this.h);
        return J();
    }

    public void u() {
        if (this.f18a == null) {
            TekLog.write(s, "cleanLyric _engine == null");
        } else {
            this.f18a.cleanLyric();
        }
    }

    public void v(String str) {
        if (this.f18a != null) {
            this.f18a.setCryptKey(str);
        }
    }

    public void w(ArrayList<String> arrayList) {
        String str;
        String str2;
        TekLog.write(s, "appendInputImages");
        if (this.f18a == null) {
            str = s;
            str2 = "appendInputImages _engine == null";
        } else {
            if (arrayList != null) {
                for (int i = 0; i < arrayList.size(); i++) {
                    this.f18a.appendInputImage(arrayList.get(i));
                }
                return;
            }
            str = s;
            str2 = "appendInputImages inputPaths == null";
        }
        TekLog.write(str, str2);
    }

    public void x(boolean z) {
        this.q = z;
        if (this.f18a == null) {
            TekLog.write(s, "setIsDirectRenderView _engine == null");
        } else {
            this.f18a.setIsDirectRenderView(z ? 1 : 0);
        }
    }

    public boolean y(int i) {
        if (this.f18a != null) {
            return this.f18a.getLayerHasKeyPoints(i);
        }
        TekLog.write(s, "getLayerHasKeyPoints _engine == null");
        return false;
    }

    public String z(int i) {
        if (this.f18a != null) {
            return this.f18a.getLayerName(i);
        }
        TekLog.write(s, "getLayerName _engine == null");
        return HttpUrl.FRAGMENT_ENCODE_SET;
    }
}
