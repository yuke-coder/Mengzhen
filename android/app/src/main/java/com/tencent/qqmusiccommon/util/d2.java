package com.tencent.qqmusiccommon.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import kotlin.Pair;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-dex\classes20.dex */
@SourceDebugExtension({"SMAP\nMagicColorAlgorithm.kt\nKotlin\n*S Kotlin\n*F\n+ 1 MagicColorAlgorithm.kt\ncom/tencent/qqmusiccommon/util/MagicColorAlgorithm\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,347:1\n2341#2,14:348\n2341#2,14:362\n2341#2,14:376\n2341#2,14:390\n1010#2,2:405\n1010#2,2:407\n1#3:404\n*S KotlinDebug\n*F\n+ 1 MagicColorAlgorithm.kt\ncom/tencent/qqmusiccommon/util/MagicColorAlgorithm\n*L\n47#1:348,14\n78#1:362,14\n81#1:376,14\n90#1:390,14\n146#1:405,2\n327#1:407,2\n*E\n"})
public final class d2 {

    @NotNull
    public static final d2 a = new d2();
    private static final int b;

    @NotNull
    private static final Function1<float[], Float> c;

    @NotNull
    private static final Function1<Float, Float> d;

    @SourceDebugExtension({"SMAP\nComparisons.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Comparisons.kt\nkotlin/comparisons/ComparisonsKt__ComparisonsKt$compareBy$2\n+ 2 MagicColorAlgorithm.kt\ncom/tencent/qqmusiccommon/util/MagicColorAlgorithm\n*L\n1#1,102:1\n146#2:103\n*E\n"})
    public static final class a<T> implements Comparator<T> {
        @Override // java.util.Comparator
        public final int compare(T t, T t2) {
            byte[] bArr = SwordSwitches.switches38;
            if (bArr != null && ((bArr[678] >> 0) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{t, t2}, this, 419825);
                if (swordProxyResultProxyMoreArgs.isSupported) {
                    return ((Integer) swordProxyResultProxyMoreArgs.result).intValue();
                }
            }
            return ComparisonsKt.compareValues(Integer.valueOf(((List) t).size()), Integer.valueOf(((List) t2).size()));
        }
    }

    static {
        Float fValueOf = Float.valueOf(0.0f);
        b = Color.HSVToColor(ArraysKt.toFloatArray(new Float[]{fValueOf, fValueOf, Float.valueOf(0.25f)}));
        c = new Function1() { // from class: com.tencent.qqmusiccommon.util.y1
            public final Object invoke(Object obj) {
                return Float.valueOf(d2.h((float[]) obj));
            }
        };
        d = new Function1() { // from class: com.tencent.qqmusiccommon.util.z1
            public final Object invoke(Object obj) {
                return Float.valueOf(d2.f(((Float) obj).floatValue()));
            }
        };
    }

    private d2() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final float f(float f) {
        return (float) ((((double) f) * 0.2d) + 0.5d);
    }

    private final void g(float[] fArr) {
        byte[] bArr = SwordSwitches.switches38;
        if ((bArr == null || ((bArr[692] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(fArr, this, 419941).isSupported) && fArr != null) {
            Function1<Float, Float> function1 = d;
            float fMin = Math.min(((Number) function1.invoke(Float.valueOf(fArr[1]))).floatValue(), 0.7f);
            fArr[1] = fMin;
            fArr[1] = Math.max(fMin, 0.5f);
            float fMin2 = Math.min(((Number) function1.invoke(Float.valueOf(fArr[2]))).floatValue(), 0.7f);
            fArr[2] = fMin2;
            fArr[2] = Math.max(fMin2, 0.5f);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final float h(float[] fArr) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[709] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(fArr, null, 420074);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Float) swordProxyResultProxyOneArg.result).floatValue();
            }
        }
        Intrinsics.checkNotNullParameter(fArr, "it");
        float f = fArr[1];
        float f2 = fArr[2];
        return (float) Math.sqrt(((f - 0.6f) * (f - 0.6f)) + ((f2 - 0.6f) * (f2 - 0.6f)));
    }

    private final float[] i(List<float[]> list) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[698] >> 5) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(list, this, 419990);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (float[]) swordProxyResultProxyOneArg.result;
            }
        }
        if (list.size() == 0) {
            return null;
        }
        float[] fArr = new float[3];
        char c2 = 0;
        float[] fArr2 = list.get(0);
        if (E(fArr2) || z(fArr2) || B(fArr2) || A(fArr2) || C(fArr2)) {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            Float fValueOf = null;
            int i = 0;
            float f = 0.0f;
            float f2 = 0.0f;
            for (float[] fArr3 : list) {
                f += fArr3[1];
                f2 += fArr3[2];
                float f3 = fArr3[0];
                Integer num = (Integer) linkedHashMap.get(Float.valueOf(f3));
                int iIntValue = num == null ? 1 : num.intValue() + 1;
                linkedHashMap.put(Float.valueOf(f3), Integer.valueOf(iIntValue));
                if (iIntValue > i) {
                    fValueOf = Float.valueOf(f3);
                    i = iIntValue;
                }
            }
            if (fValueOf == null) {
                return null;
            }
            fArr[0] = fValueOf.floatValue();
            fArr[1] = f / list.size();
            fArr[2] = f2 / list.size();
            return fArr;
        }
        if (!D(fArr2)) {
            float f4 = 0.0f;
            float f5 = 0.0f;
            float f6 = 0.0f;
            for (float[] fArr4 : list) {
                f5 += fArr4[0];
                f6 += fArr4[1];
                f4 += fArr4[2];
            }
            fArr[0] = f5 / list.size();
            fArr[1] = f6 / list.size();
            fArr[2] = f4 / list.size();
            return fArr;
        }
        Iterator<float[]> it = list.iterator();
        boolean z = false;
        boolean z2 = false;
        while (it.hasNext()) {
            double d2 = it.next()[0];
            if (!(330.0d <= d2 && d2 <= 360.0d)) {
                z2 = true;
                if (z) {
                    break;
                }
            } else {
                z = true;
                if (z2) {
                    break;
                }
            }
        }
        boolean z3 = z2 && z;
        Iterator<float[]> it2 = list.iterator();
        float f7 = 0.0f;
        float f8 = 0.0f;
        float f9 = 0.0f;
        while (it2.hasNext()) {
            float[] next = it2.next();
            float f10 = next[c2];
            Iterator<float[]> it3 = it2;
            double d3 = f10;
            if (!(330.0d <= d3 && d3 <= 360.0d) && z3) {
                f10 += 360;
            }
            f8 += f10;
            f9 += next[1];
            f7 += next[2];
            it2 = it3;
            c2 = 0;
        }
        fArr[0] = f8 / list.size();
        fArr[1] = f9 / list.size();
        fArr[2] = f7 / list.size();
        return fArr;
    }

    public static /* synthetic */ Integer k(d2 d2Var, Bitmap bitmap, int i, Integer num, int i2, Object obj) {
        if ((i2 & 2) != 0) {
            i = 255;
        }
        return d2Var.j(bitmap, i, num);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final int l(int i, float[] fArr) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[709] >> 4) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), fArr}, null, 420077);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Integer) swordProxyResultProxyMoreArgs.result).intValue();
            }
        }
        return Color.HSVToColor(i, fArr);
    }

    public static /* synthetic */ int n(d2 d2Var, Bitmap bitmap, int i, int i2, Object obj) {
        if ((i2 & 2) != 0) {
            i = 255;
        }
        return d2Var.m(bitmap, i);
    }

    public static /* synthetic */ Pair p(d2 d2Var, Bitmap bitmap, int i, int i2, Object obj) {
        if ((i2 & 2) != 0) {
            i = 255;
        }
        return d2Var.o(bitmap, i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Pair q(int i, float[] fArr, float[] fArr2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[709] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), fArr, fArr2}, null, 420079);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Pair) swordProxyResultProxyMoreArgs.result;
            }
        }
        return new Pair(Integer.valueOf(Color.HSVToColor(i, fArr)), Integer.valueOf(Color.HSVToColor(i, fArr2)));
    }

    public static /* synthetic */ Pair s(d2 d2Var, Bitmap bitmap, int i, int i2, Object obj) {
        if ((i2 & 2) != 0) {
            i = 255;
        }
        return d2Var.r(bitmap, i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Pair t(int i, float[] fArr, float[] fArr2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[710] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), fArr, fArr2}, null, 420081);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Pair) swordProxyResultProxyMoreArgs.result;
            }
        }
        return new Pair(Integer.valueOf(Color.HSVToColor(i, fArr)), Integer.valueOf(Color.HSVToColor(i, fArr2)));
    }

    public final boolean A(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[706] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(fArr, this, 420055);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(fArr, "hsv");
        return fArr[1] < 0.06f && fArr[2] < 0.25f;
    }

    public final boolean B(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[707] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(fArr, this, 420057);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(fArr, "hsv");
        if (fArr[1] >= 0.06f) {
            return false;
        }
        float f = fArr[2];
        return f < 0.5f && f >= 0.25f;
    }

    public final boolean C(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[707] >> 3) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(fArr, this, 420060);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(fArr, "hsv");
        if (fArr[1] < 0.06f) {
            float f = fArr[2];
            if (f < 0.75f && f >= 0.5f) {
                return true;
            }
        }
        return false;
    }

    public final boolean D(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[708] >> 3) & 1) > 0) {
            SwordProxyResult result = SwordProxy.proxyOneArg(fArr, this, 420068);
            if (result.isSupported) {
                return ((Boolean) result.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(fArr, "hsv");
        float hue = fArr[0];
        boolean redHue = (hue >= 0.0f && hue <= 20.0f) ||
            (hue >= 330.0f && hue <= 360.0f);
        return redHue && fArr[1] >= 0.06f && fArr[2] >= 0.1f && fArr[2] <= 1.0f;
    }

    public final boolean E(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[708] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(fArr, this, 420065);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(fArr, "hsv");
        return fArr[1] < 0.06f && fArr[2] >= 0.75f;
    }

    @Nullable
    public final Integer j(@Nullable Bitmap bitmap, final int i, @Nullable Integer num) {
        Object obj;
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[686] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{bitmap, Integer.valueOf(i), num}, this, 419895);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Integer) swordProxyResultProxyMoreArgs.result;
            }
        }
        if (bitmap == null) {
            return num;
        }
        Function1 function1 = new Function1() { // from class: com.tencent.qqmusiccommon.util.c2
            public final Object invoke(Object obj2) {
                return Integer.valueOf(d2.l(i, (float[]) obj2));
            }
        };
        List<List<float[]>> listX = x(bitmap);
        if (!listX.get(0).isEmpty()) {
            Iterator<float[]> it = listX.get(0).iterator();
            if (it.hasNext()) {
                Object next = it.next();
                if (it.hasNext()) {
                    float fFloatValue = ((Number) c.invoke((float[]) next)).floatValue();
                    do {
                        Object next2 = it.next();
                        float fFloatValue2 = ((Number) c.invoke((float[]) next2)).floatValue();
                        if (Float.compare(fFloatValue, fFloatValue2) > 0) {
                            next = next2;
                            fFloatValue = fFloatValue2;
                        }
                    } while (it.hasNext());
                }
                obj = next;
            } else {
                obj = null;
            }
            float[] fArr = (float[]) obj;
            if (fArr != null) {
                g(fArr);
                return (Integer) function1.invoke(fArr);
            }
        }
        return num;
    }

    public final int m(@Nullable Bitmap bitmap, int i) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[685] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{bitmap, Integer.valueOf(i)}, this, 419887);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Integer) swordProxyResultProxyMoreArgs.result).intValue();
            }
        }
        Integer numJ = j(bitmap, i, Integer.valueOf(b));
        Intrinsics.checkNotNull(numJ);
        return numJ.intValue();
    }

    @NotNull
    public final Pair<Integer, Integer> o(@Nullable Bitmap bitmap, final int i) {
        Object next;
        Object next2;
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[689] >> 7) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{bitmap, Integer.valueOf(i)}, this, 419920);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Pair) swordProxyResultProxyMoreArgs.result;
            }
        }
        Object next3 = null;
        if (bitmap == null) {
            return new Pair<>((Integer) null, (Integer) null);
        }
        Function2 function2 = new Function2() { // from class: com.tencent.qqmusiccommon.util.b2
            public final Object invoke(Object obj, Object obj2) {
                return d2.q(i, (float[]) obj, (float[]) obj2);
            }
        };
        List<List<float[]>> listX = x(bitmap);
        if (!(!listX.get(0).isEmpty()) || !(!listX.get(1).isEmpty())) {
            if (true ^ listX.get(0).isEmpty()) {
                Iterator<float[]> it = listX.get(0).iterator();
                if (it.hasNext()) {
                    next = it.next();
                    if (it.hasNext()) {
                        float fFloatValue = ((Number) c.invoke((float[]) next)).floatValue();
                        do {
                            Object next4 = it.next();
                            float fFloatValue2 = ((Number) c.invoke((float[]) next4)).floatValue();
                            if (Float.compare(fFloatValue, fFloatValue2) > 0) {
                                next = next4;
                                fFloatValue = fFloatValue2;
                            }
                        } while (it.hasNext());
                    }
                } else {
                    next = null;
                }
                float[] fArr = (float[]) next;
                if (fArr != null) {
                    g(fArr);
                    return new Pair<>(Integer.valueOf(Color.HSVToColor(i, fArr)), (Integer) null);
                }
            }
            return new Pair<>(-16777216, -16777216);
        }
        Iterator<float[]> it2 = listX.get(0).iterator();
        if (it2.hasNext()) {
            next2 = it2.next();
            if (it2.hasNext()) {
                float fFloatValue3 = ((Number) c.invoke((float[]) next2)).floatValue();
                do {
                    Object next5 = it2.next();
                    float fFloatValue4 = ((Number) c.invoke((float[]) next5)).floatValue();
                    if (Float.compare(fFloatValue3, fFloatValue4) > 0) {
                        next2 = next5;
                        fFloatValue3 = fFloatValue4;
                    }
                } while (it2.hasNext());
            }
        } else {
            next2 = null;
        }
        float[] fArr2 = (float[]) next2;
        Iterator<float[]> it3 = listX.get(1).iterator();
        if (it3.hasNext()) {
            next3 = it3.next();
            if (it3.hasNext()) {
                float fFloatValue5 = ((Number) c.invoke((float[]) next3)).floatValue();
                do {
                    Object next6 = it3.next();
                    float fFloatValue6 = ((Number) c.invoke((float[]) next6)).floatValue();
                    if (Float.compare(fFloatValue5, fFloatValue6) > 0) {
                        next3 = next6;
                        fFloatValue5 = fFloatValue6;
                    }
                } while (it3.hasNext());
            }
        }
        float[] fArr3 = (float[]) next3;
        g(fArr2);
        g(fArr3);
        return (Pair) function2.invoke(fArr2, fArr3);
    }

    @NotNull
    public final Pair<Integer, Integer> r(@Nullable Bitmap bitmap, final int i) {
        float[] fArrI;
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[695] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{bitmap, Integer.valueOf(i)}, this, 419962);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Pair) swordProxyResultProxyMoreArgs.result;
            }
        }
        if (bitmap == null) {
            return new Pair<>((Integer) null, (Integer) null);
        }
        Function2 function2 = new Function2() { // from class: com.tencent.qqmusiccommon.util.a2
            public final Object invoke(Object obj, Object obj2) {
                return d2.t(i, (float[]) obj, (float[]) obj2);
            }
        };
        List<List<float[]>> listY = y(bitmap);
        return ((listY.get(0).isEmpty() ^ true) && (listY.get(1).isEmpty() ^ true)) ? (Pair) function2.invoke(i(listY.get(0)), i(listY.get(1))) : (!(true ^ listY.get(0).isEmpty()) || (fArrI = i(listY.get(0))) == null) ? new Pair<>((Integer) null, (Integer) null) : new Pair<>(Integer.valueOf(Color.HSVToColor(i, fArrI)), (Integer) null);
    }

    @NotNull
    public final Function1<Float, Float> u() {
        return d;
    }

    public final int v() {
        return b;
    }

    @NotNull
    public final Function1<float[], Float> w() {
        return c;
    }

    @NotNull
    public final List<List<float[]>> x(@NotNull Bitmap bitmap) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[693] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(bitmap, this, 419946);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (List) swordProxyResultProxyOneArg.result;
            }
        }
        Intrinsics.checkNotNullParameter(bitmap, "source");
        ArrayList arrayList = new ArrayList(8);
        for (int i = 0; i < 8; i++) {
            arrayList.add(new ArrayList());
        }
        int width = bitmap.getWidth();
        for (int i2 = 0; i2 < width; i2++) {
            int height = bitmap.getHeight();
            for (int i3 = 0; i3 < height; i3++) {
                float[] fArr = new float[3];
                Color.colorToHSV(bitmap.getPixel(i2, i3), fArr);
                if (fArr[1] >= 0.12f && fArr[2] >= 0.18f) {
                    float f = fArr[0] / 360.0f;
                    if (f < 0.06f) {
                        ((List) arrayList.get(0)).add(fArr);
                    } else if (f < 0.11f) {
                        ((List) arrayList.get(1)).add(fArr);
                    } else if (f < 0.21f) {
                        ((List) arrayList.get(2)).add(fArr);
                    } else if (f < 0.43f) {
                        ((List) arrayList.get(3)).add(fArr);
                    } else if (f < 0.53f) {
                        ((List) arrayList.get(4)).add(fArr);
                    } else if (f < 0.71f) {
                        ((List) arrayList.get(5)).add(fArr);
                    } else if (f < 0.82f) {
                        ((List) arrayList.get(6)).add(fArr);
                    } else if (f < 0.91f) {
                        ((List) arrayList.get(7)).add(fArr);
                    } else if (f <= 1.0f) {
                        ((List) arrayList.get(0)).add(fArr);
                    }
                }
            }
        }
        if (arrayList.size() > 1) {
            CollectionsKt.sortWith(arrayList, new a());
        }
        CollectionsKt.reverse(arrayList);
        return arrayList;
    }

    @NotNull
    public final List<List<float[]>> y(@NotNull Bitmap bitmap) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[703] >> 6) & 1) > 0) {
            SwordProxyResult result = SwordProxy.proxyOneArg(bitmap, this, 420031);
            if (result.isSupported) {
                return (List) result.result;
            }
        }
        Intrinsics.checkNotNullParameter(bitmap, "source");
        ArrayList<List<float[]>> groups = new ArrayList<>(13);
        for (int index = 0; index < 13; index++) {
            groups.add(new ArrayList<float[]>());
        }
        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                float[] hsv = new float[3];
                Color.colorToHSV(bitmap.getPixel(x, y), hsv);
                if (E(hsv)) {
                    groups.get(0).add(hsv);
                } else if (C(hsv)) {
                    groups.get(1).add(hsv);
                } else if (B(hsv)) {
                    groups.get(2).add(hsv);
                } else if (A(hsv)) {
                    groups.get(3).add(hsv);
                } else if (z(hsv)) {
                    groups.get(4).add(hsv);
                } else if (D(hsv)) {
                    groups.get(5).add(hsv);
                } else if (hsv[1] >= 0.06f && hsv[2] >= 0.1f && hsv[2] <= 1.0f) {
                    float hue = hsv[0];
                    if (hue > 20.0f && hue <= 40.0f) {
                        groups.get(6).add(hsv);
                    } else if (hue > 40.0f && hue <= 75.0f) {
                        groups.get(7).add(hsv);
                    } else if (hue > 75.0f && hue <= 155.0f) {
                        groups.get(8).add(hsv);
                    } else if (hue > 155.0f && hue <= 190.0f) {
                        groups.get(9).add(hsv);
                    } else if (hue > 190.0f && hue <= 256.0f) {
                        groups.get(10).add(hsv);
                    } else if (hue > 256.0f && hue <= 295.0f) {
                        groups.get(11).add(hsv);
                    } else if (hue > 295.0f && hue < 330.0f) {
                        groups.get(12).add(hsv);
                    }
                }
            }
        }
        CollectionsKt.sortWith(groups, new a<List<float[]>>());
        CollectionsKt.reverse(groups);
        return groups;
    }

    public final boolean z(@NotNull float[] fArr) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[706] >> 3) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(fArr, this, 420052);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(fArr, "hsv");
        float f = fArr[2];
        return f >= 0.0f && f < 0.1f && fArr[1] >= 0.06f;
    }
}
