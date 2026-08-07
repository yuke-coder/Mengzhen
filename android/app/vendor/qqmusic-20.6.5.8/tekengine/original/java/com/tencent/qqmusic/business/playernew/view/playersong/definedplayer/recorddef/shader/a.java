package com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.shader;

import TekEngineLib.Render.TekBaseShader;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import java.util.LinkedHashMap;
import java.util.Map;
import kotlin.jvm.internal.DefaultConstructorMarker;
import org.jetbrains.annotations.NotNull;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes18.dex */
public final class a extends TekBaseShader {

    @NotNull
    public static final C0049a h = new C0049a(null);
    private float d;
    private final float b = System.currentTimeMillis();

    @NotNull
    private float[] e = new float[4];
    private int f = -16776961;
    private float g = 1.0f;

    /* JADX INFO: renamed from: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.shader.a$a, reason: collision with other inner class name */
    public static final class C0049a {
        private C0049a() {
        }

        public /* synthetic */ C0049a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    @NotNull
    public Map<String, Object> getParamMap() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr != null && ((bArr[542] >> 5) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg((Object) null, this, 82742);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Map) swordProxyResultProxyOneArg.result;
            }
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        if (this.d >= 14400.0f) {
            this.d = 0.0f;
        }
        this.d += 0.03333333f;
        linkedHashMap.put("IsForSurfaceView", Integer.valueOf(((TekBaseShader) this).mIsForSurfaceView));
        linkedHashMap.put("BgColor", TekBaseShader.toBgColorFloats(((TekBaseShader) this).mBgColor));
        linkedHashMap.put("AspectRatio", Float.valueOf(((TekBaseShader) this).aspectRatio));
        linkedHashMap.put("Time", Float.valueOf(this.d));
        linkedHashMap.put("Color", TekBaseShader.toColorFloats(this.f));
        linkedHashMap.put("CanvasSize", this.e);
        linkedHashMap.put("Alpha", Float.valueOf(this.g));
        linkedHashMap.put("EffectSeed", Float.valueOf(this.b));
        return linkedHashMap;
    }

    public final void setCurrentTime(float f) {
        this.d = f;
    }

    public final void setViewSize(float f, float f2) {
        float[] fArr = this.e;
        fArr[0] = f;
        fArr[1] = f2;
    }
}
