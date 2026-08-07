package com.tencent.qqmusic.business.playernew.fxeffect.custom.shader;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.ColorInt;

import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import android.util.Log;
import kotlin.Pair;
import kotlin.collections.ArraysKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes19.dex */
public abstract class b {
    private static final int u = 0;

    @NotNull
    private final String b;

    @NotNull
    private String d;

    @NotNull
    private float[] e;
    private int f;
    private float g;

    @NotNull
    private Pair<Integer, Integer> h;

    @Nullable
    private Pair<Integer, Integer> i;
    private boolean j;
    private float l;
    private int m;
    private int n;

    @Nullable
    private Integer o;
    private boolean p;
    private float q;
    private boolean r;

    @NotNull
    private final HandlerC0031b s;

    @NotNull
    public static final a t = new a(null);

    @NotNull
    private static final Pair<Integer, Integer> v = new Pair<>(-16777216, -1);
    private static final float w = 33.333332f;
    private static final float x = 1 / 33.333332f;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        public final int a() {
            byte[] bArr = SwordSwitches.switches6;
            if (bArr != null && ((bArr[1331] >> 4) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg((Object) null, this, 66653);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return ((Integer) swordProxyResultProxyOneArg.result).intValue();
                }
            }
            return com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.u;
        }

        public final float b() {
            byte[] bArr = SwordSwitches.switches6;
            if (bArr != null && ((bArr[1333] >> 7) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg((Object) null, this, 66672);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return ((Float) swordProxyResultProxyOneArg.result).floatValue();
                }
            }
            return com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.w;
        }

        @NotNull
        public final float[] c(@ColorInt int i) {
            byte[] bArr = SwordSwitches.switches6;
            if (bArr != null && ((bArr[1337] >> 4) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Integer.valueOf(i), this, 66701);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return (float[]) swordProxyResultProxyOneArg.result;
                }
            }
            return ArraysKt.toFloatArray(new Float[]{Float.valueOf(Color.red(i) / 255.0f), Float.valueOf(Color.green(i) / 255.0f), Float.valueOf(Color.blue(i) / 255.0f), Float.valueOf(Color.alpha(i) / 255.0f)});
        }

        @NotNull
        public final float[] d(@ColorInt int i) {
            byte[] bArr = SwordSwitches.switches6;
            if (bArr != null && ((bArr[1335] >> 0) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Integer.valueOf(i), this, 66681);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return (float[]) swordProxyResultProxyOneArg.result;
                }
            }
            return ArraysKt.toFloatArray(new Float[]{Float.valueOf(Color.red(i) / 255.0f), Float.valueOf(Color.green(i) / 255.0f), Float.valueOf(Color.blue(i) / 255.0f)});
        }
    }

    /* JADX INFO: renamed from: com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b$b, reason: collision with other inner class name */
    public final class HandlerC0031b extends Handler {
        HandlerC0031b(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            Integer num;
            byte[] bArr = SwordSwitches.switches6;
            if (bArr == null || ((bArr[1329] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(message, this, 66639).isSupported) {
                Intrinsics.checkNotNullParameter(message, "msg");
                int i = message.what;
                if (i == 1) {
                    Pair<Integer, Integer> pair = b.this.i;
                    if (pair != null) {
                        b bVar = b.this;
                        bVar.l = Math.min(bVar.l + com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.x, 1.0f);
                        if (bVar.l < 1.0f) {
                            bVar.J(bVar.v(bVar.A(), pair, bVar.l));
                        } else {
                            bVar.J(pair);
                            bVar.j = false;
                            bVar.i = null;
                        }
                        if (bVar.j) {
                            sendEmptyMessageDelayed(1, (long) com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.t.b());
                            return;
                        }
                        return;
                    }
                    return;
                }
                if (i != 2 || (num = b.this.o) == null) {
                    return;
                }
                b bVar2 = b.this;
                int iIntValue = num.intValue();
                bVar2.q = Math.min(bVar2.q + com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.x, 1.0f);
                if (bVar2.q < 1.0f) {
                    bVar2.H(bVar2.u(bVar2.m, iIntValue, bVar2.q));
                } else {
                    bVar2.H(iIntValue);
                    bVar2.p = false;
                    bVar2.o = null;
                    bVar2.m = bVar2.y();
                }
                if (bVar2.p) {
                    sendEmptyMessageDelayed(2, (long) com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.t.b());
                }
            }
        }
    }

    public b(@NotNull String str) {
        Intrinsics.checkNotNullParameter(str, "fragmentShader");
        this.b = str;
        this.d = "precision highp float;\nuniform mat4 matrix;\nattribute vec4 position;\nattribute vec2 inputTextureCoordinate;\nvarying vec2 textureCoordinate;\nvoid main(void)\n{\n    textureCoordinate = inputTextureCoordinate;\n    gl_Position = matrix * position;\n}";
        this.e = new float[16];
        this.g = 1.0f;
        this.h = v;
        int i = u;
        this.m = i;
        this.n = i;
        this.s = new HandlerC0031b(Looper.getMainLooper());
    }

    @NotNull
    public final Pair<Integer, Integer> A() {
        return this.h;
    }

    @NotNull
    public final float[] B() {
        return this.e;
    }

    @NotNull
    public final String C() {
        return this.d;
    }

    public abstract void D();

    public final void E(float f) {
        this.g = f;
    }

    public void F(@ColorInt int i, boolean z) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1341] >> 1) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Boolean.valueOf(z)}, this, 66730).isSupported) {
            Log.i("BaseShader", "[setBgColor] mBgColor=" + Integer.toHexString(this.n) + ", bgColor=" + Integer.toHexString(i));
            this.f = 1;
            if (this.p) {
                Integer num = this.o;
                if (num != null) {
                    this.n = num.intValue();
                    this.q = 1.0f;
                    this.s.removeMessages(2);
                    this.p = false;
                }
                this.o = null;
                this.m = this.n;
            }
            int i2 = this.n;
            if (i2 != i) {
                if (i2 == u || !z) {
                    this.n = i;
                    this.o = null;
                    this.m = i;
                    this.q = 1.0f;
                    this.s.removeMessages(2);
                    this.s.sendEmptyMessage(2);
                    return;
                }
                this.m = i2;
                this.o = Integer.valueOf(i);
                this.p = true;
                this.q = 0.0f;
                this.s.removeMessages(2);
                this.s.sendEmptyMessage(2);
            }
        }
    }

    public final void G(float f) {
        this.g = f;
    }

    public final void H(int i) {
        this.n = i;
    }

    public final void I(boolean z) {
        this.r = z;
    }

    public final void J(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1338] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(pair, this, 66711).isSupported) {
            Intrinsics.checkNotNullParameter(pair, "<set-?>");
            this.h = pair;
        }
    }

    public final void K(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1336] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 66694).isSupported) {
            Intrinsics.checkNotNullParameter(fArr, "<set-?>");
            this.e = fArr;
        }
    }

    public void L(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1345] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(pair, this, 66766).isSupported) {
            Intrinsics.checkNotNullParameter(pair, "magicColorPair");
            if (Intrinsics.areEqual(this.h, v)) {
                this.h = pair;
                return;
            }
            this.i = pair;
            this.j = true;
            this.l = 0.0f;
            this.s.removeMessages(1);
            this.s.sendEmptyMessage(1);
        }
    }

    public abstract void M();

    public abstract void N(boolean z);

    public abstract void O(@Nullable com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.b bVar);

    public final int u(int i, int i2, float f) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1347] >> 7) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Float.valueOf(f)}, this, 66784);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Integer) swordProxyResultProxyMoreArgs.result).intValue();
            }
        }
        return Color.argb((int) (Color.alpha(i) + ((Color.alpha(i2) - Color.alpha(i)) * f)), (int) (Color.red(i) + ((Color.red(i2) - Color.red(i)) * f)), (int) (Color.green(i) + ((Color.green(i2) - Color.green(i)) * f)), (int) (Color.blue(i) + ((Color.blue(i2) - Color.blue(i)) * f)));
    }

    @NotNull
    public final Pair<Integer, Integer> v(@NotNull Pair<Integer, Integer> pair, @NotNull Pair<Integer, Integer> pair2, float f) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1350] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{pair, pair2, Float.valueOf(f)}, this, 66802);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Pair) swordProxyResultProxyMoreArgs.result;
            }
        }
        Intrinsics.checkNotNullParameter(pair, "originMagicColor");
        Intrinsics.checkNotNullParameter(pair2, "targetMagicColor");
        return new Pair<>(Integer.valueOf(u(((Number) pair.getFirst()).intValue(), ((Number) pair2.getFirst()).intValue(), f)), Integer.valueOf(u(((Number) pair.getSecond()).intValue(), ((Number) pair2.getSecond()).intValue(), f)));
    }

    @NotNull
    public final String w() {
        return this.b;
    }

    public final float x() {
        return this.g;
    }

    public final int y() {
        return this.n;
    }

    public final int z() {
        return this.f;
    }
}
