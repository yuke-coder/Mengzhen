package com.tencent.qqmusiccommon.util;

import android.content.Context;
import android.content.res.Resources;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes20.dex */
public class j0 {
    private static float a;
    private static float b;
    public static int c = a(1.0f);
    public static int d = a(2.0f);
    public static int e = a(3.0f);
    public static int f = a(4.0f);
    public static int g = a(5.0f);
    public static int h = a(7.0f);
    public static int i = a(8.0f);
    public static int j = a(9.0f);
    public static int k = a(10.0f);
    public static int l = a(12.0f);
    public static int m = a(15.0f);
    public static int n = a(16.0f);
    public static int o = a(18.0f);
    public static int p = a(20.0f);
    public static int q = a(21.0f);
    public static int r = a(30.0f);
    public static int s = a(92.0f);
    public static int t = a(150.0f);

    public static int a(float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[55] >> 7) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Float.valueOf(f2), null, 414848);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Integer) swordProxyResultProxyOneArg.result).intValue();
            }
        }
        return b(f2);
    }

    public static int b(float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[47] >> 3) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Float.valueOf(f2), null, 414780);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Integer) swordProxyResultProxyOneArg.result).intValue();
            }
        }
        return (int) c(f2);
    }

    public static float c(float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[48] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Float.valueOf(f2), null, 414791);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Float) swordProxyResultProxyOneArg.result).floatValue();
            }
        }
        return (f2 * d()) + 0.5f;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public static float d() {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[44] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(null, null, 414753);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Float) swordProxyResultProxyOneArg.result).floatValue();
            }
        }
        if (a == 0.0f) {
            a = Resources.getSystem().getDisplayMetrics().density;
        }
        return a;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    private static float e() {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[46] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(null, null, 414770);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Float) swordProxyResultProxyOneArg.result).floatValue();
            }
        }
        if (b == 0.0f) {
            b = Resources.getSystem().getDisplayMetrics().scaledDensity;
        }
        return b;
    }

    public static int f(float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[58] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Float.valueOf(f2), null, 414865);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Integer) swordProxyResultProxyOneArg.result).intValue();
            }
        }
        return (int) ((f2 / d()) + 0.5f);
    }

    public static int g(Context context, float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[57] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{context, Float.valueOf(f2)}, null, 414858);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Integer) swordProxyResultProxyMoreArgs.result).intValue();
            }
        }
        return (int) ((f2 / d()) + 0.5f);
    }

    public static float h(Context context, float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[58] >> 7) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{context, Float.valueOf(f2)}, null, 414872);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Float) swordProxyResultProxyMoreArgs.result).floatValue();
            }
        }
        return f2 / e();
    }

    public static float i(float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[49] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Float.valueOf(f2), null, 414799);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Float) swordProxyResultProxyOneArg.result).floatValue();
            }
        }
        return f2 * d();
    }

    public static int j(float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[54] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Float.valueOf(f2), null, 414833);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Integer) swordProxyResultProxyOneArg.result).intValue();
            }
        }
        return (int) k(f2);
    }

    public static float k(float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr != null && ((bArr[55] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Float.valueOf(f2), null, 414841);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Float) swordProxyResultProxyOneArg.result).floatValue();
            }
        }
        return (f2 * e()) + 0.5f;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public static void l(Float f2) {
        byte[] bArr = SwordSwitches.switches38;
        if ((bArr != null && ((bArr[50] >> 7) & 1) > 0 && SwordProxy.proxyOneArg(f2, null, 414808).isSupported) || f2 == null || f2.floatValue() == 0.0f) {
            return;
        }
        c = (int) (f2.floatValue() + 0.5f);
        d = (int) ((f2.floatValue() * 2.0f) + 0.5f);
        e = (int) ((f2.floatValue() * 3.0f) + 0.5f);
        f = (int) ((f2.floatValue() * 4.0f) + 0.5f);
        g = (int) ((f2.floatValue() * 5.0f) + 0.5f);
        h = (int) ((f2.floatValue() * 7.0f) + 0.5f);
        i = (int) ((f2.floatValue() * 8.0f) + 0.5f);
        j = (int) ((f2.floatValue() * 9.0f) + 0.5f);
        k = (int) ((f2.floatValue() * 10.0f) + 0.5f);
        l = (int) ((f2.floatValue() * 12.0f) + 0.5f);
        m = (int) ((f2.floatValue() * 15.0f) + 0.5f);
        n = (int) ((f2.floatValue() * 16.0f) + 0.5f);
        o = (int) ((f2.floatValue() * 18.0f) + 0.5f);
        p = (int) ((f2.floatValue() * 20.0f) + 0.5f);
        q = (int) ((f2.floatValue() * 21.0f) + 0.5f);
        r = (int) ((f2.floatValue() * 30.0f) + 0.5f);
        s = (int) ((f2.floatValue() * 92.0f) + 0.5f);
        t = (int) ((f2.floatValue() * 150.0f) + 0.5f);
    }
}
