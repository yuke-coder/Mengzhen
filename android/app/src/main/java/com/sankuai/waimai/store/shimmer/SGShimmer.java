package com.sankuai.waimai.store.shimmer;

import android.content.res.TypedArray;
import android.graphics.RectF;
import androidx.annotation.ColorInt;
import androidx.core.view.ViewCompat;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* JADX INFO: loaded from: classes2.dex */
public final class SGShimmer {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public final float[] f131916a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public final int[] f131917b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    public int f131918c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    @ColorInt
    public int f131919d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    @ColorInt
    public int f131920e;
    public int f;
    public int g;
    public int h;
    public float i;
    public float j;
    public float k;
    public float l;
    public float m;
    public boolean n;
    public boolean o;
    public boolean p;
    public int q;
    public int r;
    public long s;
    public long t;
    public long u;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {
        public static final int BOTTOM_TO_TOP = 3;
        public static final int LEFT_TO_RIGHT = 0;
        public static final int RIGHT_TO_LEFT = 2;
        public static final int TOP_TO_BOTTOM = 1;
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Shape {
        public static final int LINEAR = 0;
        public static final int RADIAL = 1;
    }

    public final int a(int i) {

        int i2 = this.h;
        return i2 > 0 ? i2 : Math.round(this.j * i);
    }

    public final int b(int i) {

        int i2 = this.g;
        return i2 > 0 ? i2 : Math.round(this.i * i);
    }

    public static class a extends b<a> {

        @Override // com.sankuai.waimai.store.shimmer.SGShimmer.b
        public final a c() {
            return this;
        }

        public a() {

                this.f131921a.p = true;

        }
    }

    public static abstract class b<T extends b<T>> {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        public SGShimmer f131921a;

        public abstract T c();

        public b() {

                this.f131921a = new SGShimmer();

        }

        public final SGShimmer a() {

            SGShimmer sGShimmer = this.f131921a;
            int i = sGShimmer.f;
            if (i != 1) {
                int[] iArr = sGShimmer.f131917b;
                int i2 = sGShimmer.f131920e;
                iArr[0] = i2;
                int i3 = sGShimmer.f131919d;
                iArr[1] = i3;
                iArr[2] = i3;
                iArr[3] = i2;
            } else {
                int[] iArr2 = sGShimmer.f131917b;
                int i4 = sGShimmer.f131919d;
                iArr2[0] = i4;
                iArr2[1] = i4;
                int i5 = sGShimmer.f131920e;
                iArr2[2] = i5;
                iArr2[3] = i5;
            }
            if (i != 1) {
                sGShimmer.f131916a[0] = Math.max(((1.0f - sGShimmer.k) - sGShimmer.l) / 2.0f, 0.0f);
                sGShimmer.f131916a[1] = Math.max(((1.0f - sGShimmer.k) - 0.001f) / 2.0f, 0.0f);
                sGShimmer.f131916a[2] = Math.min(((sGShimmer.k + 1.0f) + 0.001f) / 2.0f, 1.0f);
                sGShimmer.f131916a[3] = Math.min(((sGShimmer.k + 1.0f) + sGShimmer.l) / 2.0f, 1.0f);
            } else {
                float[] fArr = sGShimmer.f131916a;
                fArr[0] = 0.0f;
                fArr[1] = Math.min(sGShimmer.k, 1.0f);
                sGShimmer.f131916a[2] = Math.min(sGShimmer.k + sGShimmer.l, 1.0f);
                sGShimmer.f131916a[3] = 1.0f;
            }
            return this.f131921a;
        }

        public T b(TypedArray typedArray) {

            if (typedArray.hasValue(3)) {
                boolean z = typedArray.getBoolean(3, this.f131921a.n);

                    this.f131921a.n = z;
                    c();

            }
            if (typedArray.hasValue(0)) {
                boolean z2 = typedArray.getBoolean(0, this.f131921a.o);

                    this.f131921a.o = z2;
                    c();

            }
            if (typedArray.hasValue(1)) {
                float f = typedArray.getFloat(1, 0.3f);

                    int iMin = (int) (Math.min(1.0f, Math.max(0.0f, f)) * 255.0f);
                    SGShimmer sGShimmer = this.f131921a;
                    sGShimmer.f131920e = (iMin << 24) | (sGShimmer.f131920e & ViewCompat.MEASURED_SIZE_MASK);
                    c();

            }
            if (typedArray.hasValue(11)) {
                float f2 = typedArray.getFloat(11, 1.0f);

                    int iMin2 = (int) (Math.min(1.0f, Math.max(0.0f, f2)) * 255.0f);
                    SGShimmer sGShimmer2 = this.f131921a;
                    sGShimmer2.f131919d = (iMin2 << 24) | (16777215 & sGShimmer2.f131919d);
                    c();

            }
            if (typedArray.hasValue(7)) {
                long j = typedArray.getInt(7, (int) this.f131921a.s);
                if (j >= 0) {
                    this.f131921a.s = j;
                    c();
                } else {
                    throw new IllegalArgumentException("Given a negative duration: " + j);
                }
            }
            if (typedArray.hasValue(14)) {
                int i = typedArray.getInt(14, this.f131921a.q);

                    this.f131921a.q = i;
                    c();

            }
            if (typedArray.hasValue(15)) {
                long j2 = typedArray.getInt(15, (int) this.f131921a.t);
                if (j2 >= 0) {
                    this.f131921a.t = j2;
                    c();
                } else {
                    throw new IllegalArgumentException("Given a negative repeat delay: " + j2);
                }
            }
            if (typedArray.hasValue(16)) {
                int i2 = typedArray.getInt(16, this.f131921a.r);

                    this.f131921a.r = i2;
                    c();

            }
            if (typedArray.hasValue(18)) {
                long j3 = typedArray.getInt(18, (int) this.f131921a.u);
                if (j3 >= 0) {
                    this.f131921a.u = j3;
                    c();
                } else {
                    throw new IllegalArgumentException("Given a negative start delay: " + j3);
                }
            }
            if (typedArray.hasValue(5)) {
                int i3 = typedArray.getInt(5, this.f131921a.f131918c);
                if (i3 != 1) {
                    if (i3 != 2) {
                        if (i3 != 3) {
                            d(0);
                        } else {
                            d(3);
                        }
                    } else {
                        d(2);
                    }
                } else {
                    d(1);
                }
            }
            if (typedArray.hasValue(17)) {
                if (typedArray.getInt(17, this.f131921a.f) != 1) {
                    this.f131921a.f = 0;
                    c();
                } else {
                    this.f131921a.f = 1;
                    c();
                }
            }
            if (typedArray.hasValue(6)) {
                float f3 = typedArray.getFloat(6, this.f131921a.l);
                if (f3 >= 0.0f) {
                    this.f131921a.l = f3;
                    c();
                } else {
                    throw new IllegalArgumentException("Given invalid dropoff value: " + f3);
                }
            }
            if (typedArray.hasValue(9)) {
                int dimensionPixelSize = typedArray.getDimensionPixelSize(9, this.f131921a.g);
                if (dimensionPixelSize >= 0) {
                    this.f131921a.g = dimensionPixelSize;
                    c();
                } else {
                    throw new IllegalArgumentException("Given invalid width: " + dimensionPixelSize);
                }
            }
            if (typedArray.hasValue(8)) {
                int dimensionPixelSize2 = typedArray.getDimensionPixelSize(8, this.f131921a.h);
                if (dimensionPixelSize2 >= 0) {
                    this.f131921a.h = dimensionPixelSize2;
                    c();
                } else {
                    throw new IllegalArgumentException("Given invalid height: " + dimensionPixelSize2);
                }
            }
            if (typedArray.hasValue(13)) {
                float f4 = typedArray.getFloat(13, this.f131921a.k);
                if (f4 >= 0.0f) {
                    this.f131921a.k = f4;
                    c();
                } else {
                    throw new IllegalArgumentException("Given invalid intensity value: " + f4);
                }
            }
            if (typedArray.hasValue(20)) {
                float f5 = typedArray.getFloat(20, this.f131921a.i);
                if (f5 >= 0.0f) {
                    this.f131921a.i = f5;
                    c();
                } else {
                    throw new IllegalArgumentException("Given invalid width ratio: " + f5);
                }
            }
            if (typedArray.hasValue(10)) {
                float f6 = typedArray.getFloat(10, this.f131921a.j);
                if (f6 >= 0.0f) {
                    this.f131921a.j = f6;
                    c();
                } else {
                    throw new IllegalArgumentException("Given invalid height ratio: " + f6);
                }
            }
            if (typedArray.hasValue(19)) {
                float f7 = typedArray.getFloat(19, this.f131921a.m);

                    this.f131921a.m = f7;
                    c();

            }
            return (T) c();
        }

        public final T d(int i) {

            this.f131921a.f131918c = i;
            return (T) c();
        }
    }

    public static class c extends b<c> {

        @Override // com.sankuai.waimai.store.shimmer.SGShimmer.b
        public final c c() {
            return this;
        }

        public c() {

                this.f131921a.p = false;

        }

        @Override // com.sankuai.waimai.store.shimmer.SGShimmer.b
        public final c b(TypedArray typedArray) {

            super.b(typedArray);
            if (typedArray.hasValue(2)) {
                int color = typedArray.getColor(2, this.f131921a.f131920e);
                SGShimmer sGShimmer = this.f131921a;
                sGShimmer.f131920e = (color & ViewCompat.MEASURED_SIZE_MASK) | (sGShimmer.f131920e & (-16777216));
            }
            if (typedArray.hasValue(12)) {
                this.f131921a.f131919d = typedArray.getColor(12, this.f131921a.f131919d);
            }
            return this;
        }
    }

    public SGShimmer() {

        this.f131916a = new float[4];
        this.f131917b = new int[4];
        new RectF();
        this.f131918c = 0;
        this.f131919d = -1;
        this.f131920e = 1627389951;
        this.f = 0;
        this.g = 0;
        this.h = 0;
        this.i = 1.0f;
        this.j = 1.0f;
        this.k = 0.0f;
        this.l = 0.4f;
        this.m = 20.0f;
        this.n = true;
        this.o = true;
        this.p = true;
        this.q = -1;
        this.r = 1;
        this.s = 1000L;
    }
}
