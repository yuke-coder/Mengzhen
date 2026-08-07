package com.tencent.qqmusic.business.playernew.fxeffect.custom.shader;

import com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes19.dex */
public class e extends b {

    @NotNull
    public static final a E = new a(null);
    private float A;
    private float B;
    private float C;
    private float D;
    private float y;
    private float z;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public e(@NotNull String str) {
        super(str);
        Intrinsics.checkNotNullParameter(str, "fragmentShader");
        this.z = 14.0f;
        this.B = 1.0f;
        this.D = 1.0f;
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b
    public void D() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1335] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66683).isSupported) {
            if (this.y >= this.z) {
                this.y = 0.0f;
            }
            this.y += b.t.b() / 1000;
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b
    public void M() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1333] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66667).isSupported) {
            I(true);
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b
    public void N(boolean z) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1334] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 66674).isSupported) {
            I(false);
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b
    public void O(@Nullable com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.b bVar) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1337] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(bVar, this, 66697).isSupported) {
            com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a aVar = bVar instanceof com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a ? (com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a) bVar : null;
            if (aVar != null) {
                aVar.o(B());
                aVar.n(z());
                b.a aVar2 = b.t;
                aVar.e(aVar2.c(y()));
                aVar.l(aVar2.d(((Number) A().getFirst()).intValue()));
                aVar.p(aVar2.d(((Number) A().getSecond()).intValue()));
                aVar.j(this.z);
                aVar.s(this.y);
                aVar.d(x());
                aVar.h(this.A);
                aVar.f(this.B);
                aVar.i(this.C);
                aVar.g(this.D);
            }
        }
    }

    public final void P(float f) {
        this.B = f;
    }

    public final void Q(float f) {
        this.D = f;
    }

    public final void R(float f) {
        this.A = f;
    }

    public final void S(float f) {
        this.C = f;
    }

    public final void T(float f) {
        this.y = f;
    }

    public final void U(float f) {
        this.z = f;
    }
}
