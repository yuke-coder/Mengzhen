package com.tencent.qqmusic.ui;

import android.graphics.PointF;
import android.view.animation.Interpolator;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;

/* JADX INFO: loaded from: classes7.dex */
public class CubicBezierInterpolator implements Interpolator {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    protected PointF f34517a;
    protected PointF b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    protected PointF f34518c;
    protected PointF d;
    protected PointF e;

    public CubicBezierInterpolator(PointF pointF, PointF pointF2) throws IllegalArgumentException {
        this.f34518c = new PointF();
        this.d = new PointF();
        this.e = new PointF();
        float f = pointF.x;
        if (f < 0.0f || f > 1.0f) {
            throw new IllegalArgumentException("startX value must be in the range [0, 1]");
        }
        float f2 = pointF2.x;
        if (f2 < 0.0f || f2 > 1.0f) {
            throw new IllegalArgumentException("endX value must be in the range [0, 1]");
        }
        this.f34517a = pointF;
        this.b = pointF2;
    }

    private float a(float f) {
        PointF pointF = this.e;
        PointF pointF2 = this.f34517a;
        float f2 = pointF2.x * 3.0f;
        pointF.x = f2;
        PointF pointF3 = this.d;
        float f3 = ((this.b.x - pointF2.x) * 3.0f) - f2;
        pointF3.x = f3;
        PointF pointF4 = this.f34518c;
        float f4 = (1.0f - pointF.x) - f3;
        pointF4.x = f4;
        return f * (pointF.x + ((pointF3.x + (f4 * f)) * f));
    }

    private float c(float f) {
        return this.e.x + (f * ((this.d.x * 2.0f) + (this.f34518c.x * 3.0f * f)));
    }

    public float b(float f) {
        PointF pointF = this.e;
        PointF pointF2 = this.f34517a;
        float f2 = pointF2.y * 3.0f;
        pointF.y = f2;
        PointF pointF3 = this.d;
        float f3 = ((this.b.y - pointF2.y) * 3.0f) - f2;
        pointF3.y = f3;
        PointF pointF4 = this.f34518c;
        float f4 = (1.0f - pointF.y) - f3;
        pointF4.y = f4;
        return f * (pointF.y + ((pointF3.y + (f4 * f)) * f));
    }

    public float d(float f) {
        byte[] bArr = SwordSwitches.switches37;
        if (bArr != null && ((bArr[1081] >> 7) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Float.valueOf(f), this, 411856);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Float) swordProxyResultProxyOneArg.result).floatValue();
            }
        }
        float fC = f;
        for (int i = 1; i < 14; i++) {
            float fA = a(fC) - f;
            if (Math.abs(fA) < 0.001d) {
                break;
            }
            fC -= fA / c(fC);
        }
        return fC;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float f) {
        byte[] bArr = SwordSwitches.switches37;
        if (bArr != null && ((bArr[1079] >> 7) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Float.valueOf(f), this, 411840);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Float) swordProxyResultProxyOneArg.result).floatValue();
            }
        }
        return b(d(f));
    }

    public CubicBezierInterpolator(float f, float f2, float f3, float f4) {
        this(new PointF(f, f2), new PointF(f3, f4));
    }

    public CubicBezierInterpolator(double d, double d2, double d3, double d4) {
        this((float) d, (float) d2, (float) d3, (float) d4);
    }
}

