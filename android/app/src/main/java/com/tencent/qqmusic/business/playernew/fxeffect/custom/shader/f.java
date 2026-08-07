package com.tencent.qqmusic.business.playernew.fxeffect.custom.shader;


import com.tencent.qqmusic.activity.soundfx.supersound.spectrumstrategy.i;
import com.tencent.qqmusic.activity.soundfx.supersound.spectrumstrategy.j;
import com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.a;

import com.tencent.qqmusic.supersound.SSAudioFeature;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes4.dex */
@SourceDebugExtension({"SMAP\nSpectrumShader.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SpectrumShader.kt\ncom/tencent/qqmusic/business/playernew/fxeffect/custom/shader/SpectrumShader\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,151:1\n1#2:152\n*E\n"})
public class f extends b implements com.tencent.qqmusic.activity.soundfx.supersound.c, a.b {

    @NotNull
    public static final a D = new a(null);

    @NotNull
    private float[] A;

    @Nullable
    private j B;

    @Nullable
    private com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.a C;
    private final boolean y;

    @NotNull
    private AtomicBoolean z;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        @NotNull
        public final float[] a() {
            float[] fArr = new float[16];
            for (int i = 0; i < 16; i++) {
                fArr[i] = 0.005f;
            }
            return fArr;
        }
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public f(@NotNull String str, boolean z) {
        super(str);
        Intrinsics.checkNotNullParameter(str, "fragmentShader");
        this.y = z;
        this.z = new AtomicBoolean(false);
        this.A = D.a();
    }

    private final void S() {
        byte[] bArr = SwordSwitches.switches6;
        if ((bArr == null || ((bArr[1332] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66661).isSupported) && this.z.compareAndSet(false, true)) {
            if (!this.y) {
                if (this.B == null) {
                    this.B = P();
                }
                com.tencent.qqmusic.activity.soundfx.supersound.b.m().t(new WeakReference(this.B));
            } else {
                if (this.C == null) {
                    this.C = new com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.a(this);
                }
                com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.a aVar = this.C;
                if (aVar != null) {
                    aVar.f();
                }
            }
        }
    }

    private final void U() {
        byte[] bArr = SwordSwitches.switches6;
        if ((bArr == null || ((bArr[1334] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66679).isSupported) && this.z.compareAndSet(true, false)) {
            if (this.y) {
                com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.a aVar = this.C;
                if (aVar != null) {
                    aVar.g();
                    return;
                }
                return;
            }
            j jVar = this.B;
            if (jVar != null) {
                com.tencent.qqmusic.activity.soundfx.supersound.b.m().v(jVar);
            }
        }
    }

    public void D() {
    }

    public void M() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1336] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66695).isSupported) {
            I(true);
            S();
        }
    }

    public void N(boolean z) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1338] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 66706).isSupported) {
            if (z) {
                I(false);
            }
            j jVar = this.B;
            if (jVar != null) {
                jVar.L(z);
            }
            U();
        }
    }

    public void O(@Nullable com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.b bVar) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1339] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(bVar, this, 66714).isSupported) {
            com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a aVar = bVar instanceof com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a ? (com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a) bVar : null;
            if (aVar != null) {
                aVar.o(B());
                aVar.n(z());
                b.a aVar2 = b.t;
                aVar.e(aVar2.c(y()));
                aVar.l(aVar2.d(((Number) A().getFirst()).intValue()));
                aVar.p(aVar2.d(((Number) A().getSecond()).intValue()));
                aVar.r(R());
                aVar.d(x());
            }
        }
    }

    @NotNull
    public j P() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1331] >> 3) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg((Object) null, this, 66652);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (j) swordProxyResultProxyOneArg.result;
            }
        }
        i iVar = new i(this);
        iVar.K(true);
        return iVar;
    }

    public float Q(float f) {
        if (f < 0.5688889f) {
            f = 0.5688889f;
        } else if (f > 0.8888889f) {
            f = 0.8888889f;
        }
        float f2 = (f - 0.5688889f) / 0.32f;
        return (0.995f * f2 * f2) + 0.005f;
    }

    @NotNull
    public float[] R() {
        return this.A;
    }

    public void T(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1330] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 66646).isSupported) {
            Intrinsics.checkNotNullParameter(fArr, "<set-?>");
            this.A = fArr;
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.a.b
    public void c(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1344] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 66753).isSupported) {
            Intrinsics.checkNotNullParameter(fArr, "spectrumData");
            T(fArr);
        }
    }

    public void onUpdate(@Nullable SSAudioFeature sSAudioFeature) {
        float[] fArr;
        byte[] bArr = SwordSwitches.switches6;
        if ((bArr != null && ((bArr[1341] >> 0) & 1) > 0 && SwordProxy.proxyOneArg(sSAudioFeature, this, 66729).isSupported) || sSAudioFeature == null || (fArr = sSAudioFeature.leftSpectrumValues) == null) {
            return;
        }
        if (true ^ (fArr.length == 0)) {
            int i = sSAudioFeature.spectrumBands;
            for (int i2 = 0; i2 < i; i2++) {
                fArr[i2] = Q(fArr[i2]);
            }
            T(fArr);
        }
    }

    public /* synthetic */ f(String str, boolean z, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, (i & 2) != 0 ? false : z);
    }
}
