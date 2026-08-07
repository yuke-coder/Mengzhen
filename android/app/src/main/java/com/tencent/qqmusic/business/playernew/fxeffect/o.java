package com.tencent.qqmusic.business.playernew.fxeffect;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusiccommon.util.d2;
import kotlin.Pair;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes19.dex */
@SourceDebugExtension({"SMAP\nMagicColorUtil.kt\nKotlin\n*S Kotlin\n*F\n+ 1 MagicColorUtil.kt\ncom/tencent/qqmusic/business/playernew/fxeffect/MagicColorUtil\n+ 2 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n*L\n1#1,431:1\n13441#2,3:432\n*S KotlinDebug\n*F\n+ 1 MagicColorUtil.kt\ncom/tencent/qqmusic/business/playernew/fxeffect/MagicColorUtil\n*L\n398#1:432,3\n*E\n"})
public final class o {

    @NotNull
    public static final o a = new o();

    @NotNull
    private static final Pair<Integer, Integer> b;

    @NotNull
    private static final Pair<Integer, Integer> c;

    @NotNull
    private static final Pair<Integer, Integer> d;

    @NotNull
    private static final Function2<float[], float[], Pair<Integer, Integer>> e;

    @NotNull
    private static final Pair<Integer, Integer> f;

    static {
        Float fValueOf = Float.valueOf(0.0f);
        b = new Pair<>(Integer.valueOf(Color.HSVToColor(ArraysKt.toFloatArray(new Float[]{fValueOf, fValueOf, Float.valueOf(0.25f)}))), Integer.valueOf(Color.HSVToColor(ArraysKt.toFloatArray(new Float[]{fValueOf, fValueOf, Float.valueOf(0.45f)}))));
        Float fValueOf2 = Float.valueOf(153.0f);
        Float fValueOf3 = Float.valueOf(0.4f);
        c = new Pair<>(Integer.valueOf(Color.HSVToColor(ArraysKt.toFloatArray(new Float[]{fValueOf2, fValueOf3, Float.valueOf(0.94f)}))), Integer.valueOf(Color.HSVToColor(ArraysKt.toFloatArray(new Float[]{fValueOf2, Float.valueOf(0.16f), Float.valueOf(1.0f)}))));
        d = new Pair<>(Integer.valueOf(Color.HSVToColor(ArraysKt.toFloatArray(new Float[]{fValueOf, fValueOf, fValueOf3}))), Integer.valueOf(Color.HSVToColor(ArraysKt.toFloatArray(new Float[]{fValueOf, fValueOf, Float.valueOf(0.56f)}))));
        e = new Function2() { // from class: com.tencent.qqmusic.business.playernew.fxeffect.n
            public final Object invoke(Object obj, Object obj2) {
                return o.b((float[]) obj, (float[]) obj2);
            }
        };
        d2 d2Var = d2.a;
        f = new Pair<>(Integer.valueOf(d2Var.v()), Integer.valueOf(d2Var.v()));
    }

    private o() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Pair b(float[] fArr, float[] fArr2) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1389] >> 2) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{fArr, fArr2}, (Object) null, 67115);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Pair) swordProxyResultProxyMoreArgs.result;
            }
        }
        return new Pair(Integer.valueOf(Color.HSVToColor(fArr)), Integer.valueOf(Color.HSVToColor(fArr2)));
    }

    public static /* synthetic */ Bitmap f(o oVar, Bitmap bitmap, Pair pair, Integer num, int i, Object obj) {
        if ((i & 4) != 0) {
            num = null;
        }
        return oVar.e(bitmap, pair, num);
    }

    public static /* synthetic */ Pair w(o oVar, Bitmap bitmap, int i, int i2, int i3, Object obj) {
        if ((i3 & 2) != 0) {
            i = 102;
        }
        if ((i3 & 4) != 0) {
            i2 = 153;
        }
        return oVar.v(bitmap, i, i2);
    }

    private final boolean x(float f2, float f3) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1377] >> 2) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f2), Float.valueOf(f3)}, this, 67019);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Boolean) swordProxyResultProxyMoreArgs.result).booleanValue();
            }
        }
        float fAbs = Math.abs(f2 - f3);
        return Math.min(fAbs, 360.0f - fAbs) > 45.0f;
    }

    @NotNull
    public final Pair<Integer, Integer> c(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1378] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(pair, this, 67031);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        Intrinsics.checkNotNullParameter(pair, "pair");
        int iIntValue = ((Number) pair.getFirst()).intValue();
        int iIntValue2 = ((Number) pair.getSecond()).intValue();
        float[] fArr = new float[3];
        Color.colorToHSV(iIntValue, fArr);
        float[] fArr2 = new float[3];
        Color.colorToHSV(iIntValue2, fArr2);
        float f2 = fArr[0];
        float f3 = fArr2[0];
        if (f3 < f2) {
            f3 += 360;
        }
        float fMin = f3 < 180.0f + f2 ? Math.min(f2 + 36.0f, f3) : Math.max(f3, (f2 + 360) - 36.0f);
        if (fMin >= 360.0f) {
            fMin -= 360;
        }
        fArr2[0] = fMin;
        fArr[1] = fArr[1] + 0.2f;
        fArr[2] = fArr[2] - 0.04f;
        fArr2[1] = fArr2[1] + 0.2f;
        fArr2[2] = fArr2[2] - 0.04f;
        return (Pair) e.invoke(fArr, fArr2);
    }

    @NotNull
    public final Pair<Integer, Integer> d(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1381] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(pair, this, 67050);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        Intrinsics.checkNotNullParameter(pair, "pair");
        int iIntValue = ((Number) pair.getFirst()).intValue();
        int iIntValue2 = ((Number) pair.getSecond()).intValue();
        float[] fArr = {0.0f, 0.3f, 0.8f};
        Color.colorToHSV(iIntValue, fArr);
        float[] fArr2 = {0.0f, 0.3f, 0.8f};
        Color.colorToHSV(iIntValue2, fArr2);
        return (Pair) e.invoke(fArr, fArr2);
    }

    @Nullable
    public final Bitmap e(@Nullable Bitmap bitmap, @Nullable Pair<Integer, Integer> pair, @Nullable Integer num) {
        byte[] bArr = SwordSwitches.switches6;
        char c2 = 0;
        int i = 1;
        if (bArr != null && ((bArr[1382] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{bitmap, pair, num}, this, 67063);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Bitmap) swordProxyResultProxyMoreArgs.result;
            }
        }
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        if (pair == null) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == 0 || height == 0) {
            return null;
        }
        int i2 = width * height;
        try {
            int[] iArr = new int[i2];
            bitmap.getPixels(iArr, 0, width, 0, 0, width, height);
            int[] iArr2 = new int[3];
            int iIntValue = ((Number) pair.getFirst()).intValue();
            int iIntValue2 = ((Number) pair.getSecond()).intValue();
            int i3 = 0;
            int i4 = 0;
            while (i3 < i2) {
                int i5 = iArr[i3];
                int i6 = i4 + 1;
                int iAlpha = Color.alpha(iArr[i4]);
                iArr2[c2] = (int) (Color.red(iIntValue) + (((Color.red(iIntValue2) - Color.red(iIntValue)) * Color.red(iArr[i4])) / 255.0f));
                iArr2[i] = (int) (Color.green(iIntValue) + (((Color.green(iIntValue2) - Color.green(iIntValue)) * Color.green(iArr[i4])) / 255.0f));
                int iBlue = (int) (Color.blue(iIntValue) + (((Color.blue(iIntValue2) - Color.blue(iIntValue)) * Color.blue(iArr[i4])) / 255.0f));
                iArr2[2] = iBlue;
                if (num == null) {
                    iArr[i4] = Color.argb(iAlpha, iArr2[c2], iArr2[i], iBlue);
                } else {
                    float f2 = iAlpha / 255.0f;
                    float f3 = i - f2;
                    iArr2[0] = (int) ((Color.red(num.intValue()) * f3) + (iArr2[0] * f2));
                    iArr2[1] = (int) ((Color.green(num.intValue()) * f3) + (iArr2[1] * f2));
                    iArr2[2] = (int) ((Color.blue(num.intValue()) * f3) + (iArr2[2] * f2));
                    iArr[i4] = Color.argb(Color.alpha(num.intValue()), iArr2[0], iArr2[1], iArr2[2]);
                }
                i3++;
                i4 = i6;
                c2 = 0;
                i = 1;
            }
            return Bitmap.createBitmap(iArr, width, height, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError unused) {
            return null;
        }
    }

    @NotNull
    public final Pair<Integer, Integer> g(@Nullable Bitmap bitmap) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1356] >> 3) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(bitmap, this, 66852);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        return h(u(bitmap));
    }

    @NotNull
    public final Pair<Integer, Integer> h(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1370] >> 2) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(pair, this, 66963);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        Intrinsics.checkNotNullParameter(pair, "pair");
        Integer num = (Integer) pair.getFirst();
        Integer num2 = (Integer) pair.getSecond();
        if (num != null && num2 != null) {
            return new Pair<>(num, num2);
        }
        if (num == null) {
            return d;
        }
        float[] fArr = new float[3];
        Color.colorToHSV(num.intValue(), fArr);
        float[] fArr2 = new float[3];
        fArr2[0] = fArr[0];
        float f2 = fArr[1];
        if (f2 > 0.6f) {
            f2 -= 0.2f;
        } else if (f2 < 0.6f) {
            f2 += 0.2f;
        }
        fArr2[1] = f2;
        float f3 = fArr[2];
        if (f3 > 0.6f) {
            f3 -= 0.2f;
        } else if (f3 < 0.6f) {
            f3 += 0.2f;
        }
        fArr2[2] = f3;
        return (Pair) e.invoke(fArr, fArr2);
    }

    @NotNull
    public final Pair<Integer, Integer> i() {
        return b;
    }

    @NotNull
    public final Pair<Integer, Integer> j() {
        return c;
    }

    @NotNull
    public final Pair<Integer, Integer> k() {
        return f;
    }

    @NotNull
    public final Pair<Integer, Integer> l(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1359] >> 3) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(pair, this, 66876);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        Intrinsics.checkNotNullParameter(pair, "pair");
        Integer num = (Integer) pair.getFirst();
        Integer num2 = (Integer) pair.getSecond();
        if (num == null || num2 == null) {
            if (num == null) {
                return c;
            }
            float[] fArr = new float[3];
            Color.colorToHSV(num.intValue(), fArr);
            float f2 = fArr[0];
            float f3 = 360 * f2;
            if (f3 > 21.0f && f3 < 190.0f) {
                fArr[1] = 0.7f;
                fArr[2] = 0.8f;
            }
            return (Pair) e.invoke(fArr, new float[]{f2, 0.4f, 0.95f});
        }
        float[] fArr2 = new float[3];
        Color.colorToHSV(num.intValue(), fArr2);
        float[] fArr3 = new float[3];
        Color.colorToHSV(num2.intValue(), fArr3);
        float f4 = 360;
        float f5 = fArr2[0] * f4;
        if (f5 > 21.0f && f5 < 190.0f) {
            fArr2[1] = 0.7f;
            fArr2[2] = 0.8f;
        }
        float f6 = fArr3[0] * f4;
        if (f6 > 21.0f && f6 < 190.0f) {
            fArr3[1] = 0.4f;
            fArr3[2] = 0.95f;
        }
        return (Pair) e.invoke(fArr2, fArr3);
    }

    @NotNull
    public final Pair<Integer, Integer> m(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1364] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(pair, this, 66914);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        Intrinsics.checkNotNullParameter(pair, "pair");
        Integer num = (Integer) pair.getFirst();
        Integer num2 = (Integer) pair.getSecond();
        if (num == null || num2 == null) {
            return d;
        }
        float[] fArr = new float[3];
        Color.colorToHSV(num.intValue(), fArr);
        float f2 = fArr[1];
        if (f2 > 0.1d) {
            fArr[1] = (f2 * 0.1f) + 0.3f;
        }
        fArr[2] = (fArr[2] * 0.1f) + 0.9f;
        float[] fArr2 = new float[3];
        Color.colorToHSV(num2.intValue(), fArr2);
        if (fArr[1] > 0.1d) {
            fArr2[1] = (fArr2[1] * 0.1f) + 0.3f;
        }
        fArr2[2] = (fArr2[2] * 0.1f) + 0.9f;
        return (Pair) e.invoke(fArr, fArr2);
    }

    @NotNull
    public final String n(int i) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1388] >> 3) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Integer.valueOf(i), this, 67108);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (String) swordProxyResultProxyOneArg.result;
            }
        }
        float[] fArr = new float[3];
        Color.colorToHSV(i, fArr);
        return "H = " + fArr[0] + " S = " + fArr[1] + " B = " + fArr[2];
    }

    @NotNull
    public final String o(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1387] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(fArr, this, 67098);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (String) swordProxyResultProxyOneArg.result;
            }
        }
        Intrinsics.checkNotNullParameter(fArr, "array");
        return "H = " + fArr[0] + " S = " + fArr[1] + " B = " + fArr[2];
    }

    @NotNull
    public final Pair<Integer, Integer> p(@Nullable Bitmap bitmap) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1353] >> 4) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(bitmap, this, 66829);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        return g(bitmap);
    }

    @NotNull
    public final Pair<Integer, Integer> q(@Nullable Bitmap bitmap) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1355] >> 5) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(bitmap, this, 66846);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        return r(u(bitmap));
    }

    @NotNull
    public final Pair<Integer, Integer> r(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1367] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(pair, this, 66937);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        Intrinsics.checkNotNullParameter(pair, "pair");
        Integer num = (Integer) pair.getFirst();
        Integer num2 = (Integer) pair.getSecond();
        if (num == null || num2 == null) {
            if (num == null) {
                return c;
            }
            float[] fArr = {0.0f, 0.4f, 0.94f};
            Color.colorToHSV(num.intValue(), fArr);
            return (Pair) e.invoke(fArr, new float[]{fArr[0], 0.16f, 1.0f});
        }
        float[] fArr2 = {0.0f, 0.4f, 0.94f};
        Color.colorToHSV(num.intValue(), fArr2);
        float[] fArr3 = {0.0f, 0.4f, 0.94f};
        Color.colorToHSV(num2.intValue(), fArr3);
        return (Pair) e.invoke(fArr2, fArr3);
    }

    @NotNull
    public final Pair<Integer, Integer> s(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1374] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(pair, this, 66994);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        Intrinsics.checkNotNullParameter(pair, "pair");
        Integer num = (Integer) pair.getFirst();
        Integer num2 = (Integer) pair.getSecond();
        if (num == null || num2 == null) {
            if (num == null) {
                return c;
            }
            float[] fArr = {0.0f, 0.4f, 0.94f};
            Color.colorToHSV(num.intValue(), fArr);
            return (Pair) e.invoke(fArr, new float[]{((fArr[0] + 45.0f) + 360.0f) % 360.0f, 0.16f, 0.8f});
        }
        float[] fArr2 = {0.0f, 0.4f, 0.94f};
        Color.colorToHSV(num.intValue(), fArr2);
        float[] fArr3 = new float[3];
        Color.colorToHSV(num2.intValue(), fArr3);
        if (!x(fArr2[0], fArr3[0])) {
            fArr3[0] = ((fArr3[0] + 45.0f) + 360.0f) % 360.0f;
        }
        fArr3[1] = 0.4f;
        fArr3[2] = 0.8f;
        return (Pair) e.invoke(fArr2, fArr3);
    }

    @NotNull
    public final GradientDrawable t(@Nullable Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1357] >> 2) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(pair, this, 66859);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (GradientDrawable) swordProxyResultProxyOneArg.result;
            }
        }
        if (pair != null) {
            return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{((Number) pair.getFirst()).intValue(), ((Number) pair.getSecond()).intValue()});
        }
        GradientDrawable.Orientation orientation = GradientDrawable.Orientation.TOP_BOTTOM;
        d2 d2Var = d2.a;
        return new GradientDrawable(orientation, new int[]{d2Var.v(), d2Var.v()});
    }

    @NotNull
    public final Pair<Integer, Integer> u(@Nullable Bitmap bitmap) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1354] >> 7) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(bitmap, this, 66840);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (Pair) swordProxyResultProxyOneArg.result;
            }
        }
        return d2.p(d2.a, com.tencent.image.algorithms.a.m(bitmap, 10, 10), 0, 2, (Object) null);
    }

    @NotNull
    public final Pair<Integer, Integer> v(@Nullable Bitmap bitmap, int i, int i2) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1351] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{bitmap, Integer.valueOf(i), Integer.valueOf(i2)}, this, 66809);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Pair) swordProxyResultProxyMoreArgs.result;
            }
        }
        Bitmap bitmapM = com.tencent.image.algorithms.a.m(bitmap, 10, 10);
        if (bitmapM == null) {
            return new Pair<>(f.getFirst(), f.getSecond());
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmapM, 0, 0, 10, 5);
        Intrinsics.checkNotNullExpressionValue(bitmapCreateBitmap, "createBitmap(...)");
        Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap(bitmapM, 0, bitmapM.getHeight() / 2, 10, 5);
        Intrinsics.checkNotNullExpressionValue(bitmapCreateBitmap2, "createBitmap(...)");
        d2 d2Var = d2.a;
        return new Pair<>(Integer.valueOf(d2Var.m(bitmapCreateBitmap, i)), Integer.valueOf(d2Var.m(bitmapCreateBitmap2, i2)));
    }

    public final boolean y(int i, boolean z) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr != null && ((bArr[1348] >> 7) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Boolean.valueOf(z)}, this, 66792);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Boolean) swordProxyResultProxyMoreArgs.result).booleanValue();
            }
        }
        return ((((double) Color.red(i)) * 0.299d) + (((double) Color.green(i)) * 0.587d)) + (((double) Color.blue(i)) * 0.144d) >= 170.0d ? z : !z;
    }
}
