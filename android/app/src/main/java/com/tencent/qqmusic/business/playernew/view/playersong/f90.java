package com.tencent.qqmusic.business.playernew.view.playersong;

import android.animation.TypeEvaluator;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import java.util.concurrent.ThreadLocalRandom;
import kotlin.jvm.internal.DefaultConstructorMarker;
import org.jetbrains.annotations.NotNull;

/* loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes18.dex */
public final class f90 implements TypeEvaluator<Float> {

    @NotNull
    public static final a g = new a(null);
    private float a;
    private float b = Float.MAX_VALUE;
    private float c = Float.MAX_VALUE;
    private boolean d = true;
    private float e = Float.MAX_VALUE;
    private boolean f = true;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    public f90(float f) {
        this.a = f;
    }

    @NotNull
    public Float a(float f, float f2, float f3) {
        float fNextDouble;
        byte[] bArr = SwordSwitches.switches8;
        if (bArr != null && ((bArr[264] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3)}, this, 80519);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Float) swordProxyResultProxyMoreArgs.result;
            }
        }
        boolean z = f < this.e;
        this.d = z;
        this.e = f;
        if (z) {
            if (!this.f) {
                this.a = this.b;
            }
            this.f = false;
            try {
                fNextDouble = ((float) ThreadLocalRandom.current().nextDouble(Math.abs(f2 - f3) + 0.001d)) + Math.min(f2, f3);
            } catch (Throwable unused) {
                fNextDouble = this.a;
            }
            this.c = fNextDouble;
        }
        float f4 = this.c;
        float f5 = this.a;
        float f6 = ((f4 - f5) * f) + f5;
        this.b = f6;
        return Float.valueOf(f6);
    }

    @Override // android.animation.TypeEvaluator
    public /* bridge */ /* synthetic */ Float evaluate(float f, Float f2, Float f3) {
        return a(f, f2.floatValue(), f3.floatValue());
    }
}


