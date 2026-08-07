package com.sankuai.waimai.store.shimmer;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.meituan.android.paladin.Paladin;
import com.meituan.robust.ChangeQuickRedirect;
import com.meituan.robust.PatchProxy;

/* JADX INFO: loaded from: classes2.dex */
public final class a extends Drawable {
    public static ChangeQuickRedirect changeQuickRedirect;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public final C4013a f131926a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public final Paint f131927b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    public final Rect f131928c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    public final Matrix f131929d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    @Nullable
    public ValueAnimator f131930e;

    @Nullable
    public SGShimmer f;

    /* JADX INFO: renamed from: com.sankuai.waimai.store.shimmer.a$a, reason: collision with other inner class name */
    public class C4013a implements ValueAnimator.AnimatorUpdateListener {
        public C4013a() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public final void onAnimationUpdate(ValueAnimator valueAnimator) {
            a.this.invalidateSelf();
        }
    }

    static {
        Paladin.record(4052931059195851428L);
    }

    public final boolean a() {
        Object[] objArr = new Object[0];
        ChangeQuickRedirect changeQuickRedirect2 = changeQuickRedirect;
        if (PatchProxy.isSupport(objArr, this, changeQuickRedirect2, 2252069)) {
            return ((Boolean) PatchProxy.accessDispatch(objArr, this, changeQuickRedirect2, 2252069)).booleanValue();
        }
        ValueAnimator valueAnimator = this.f131930e;
        return valueAnimator != null && valueAnimator.isStarted();
    }

    @Override // android.graphics.drawable.Drawable
    public final int getOpacity() {
        SGShimmer sGShimmer = this.f;
        return (sGShimmer == null || !(sGShimmer.n || sGShimmer.p)) ? -1 : -3;
    }

    @Override // android.graphics.drawable.Drawable
    public final void setAlpha(int i) {
    }

    @Override // android.graphics.drawable.Drawable
    public final void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    public a() {
        Object[] objArr = new Object[0];
        ChangeQuickRedirect changeQuickRedirect2 = changeQuickRedirect;
        if (PatchProxy.isSupport(objArr, this, changeQuickRedirect2, 15475113)) {
            PatchProxy.accessDispatch(objArr, this, changeQuickRedirect2, 15475113);
            return;
        }
        this.f131926a = new C4013a();
        Paint paint = new Paint();
        this.f131927b = paint;
        this.f131928c = new Rect();
        this.f131929d = new Matrix();
        paint.setAntiAlias(true);
    }

    public final void b() {
        SGShimmer sGShimmer;
        Object[] objArr = new Object[0];
        ChangeQuickRedirect changeQuickRedirect2 = changeQuickRedirect;
        if (PatchProxy.isSupport(objArr, this, changeQuickRedirect2, 704726)) {
            PatchProxy.accessDispatch(objArr, this, changeQuickRedirect2, 704726);
            return;
        }
        ValueAnimator valueAnimator = this.f131930e;
        if (valueAnimator != null && !valueAnimator.isStarted() && (sGShimmer = this.f) != null && sGShimmer.o && getCallback() != null) {
            this.f131930e.start();
        }
    }

    public final void d() {
        Object[] objArr = new Object[0];
        ChangeQuickRedirect changeQuickRedirect2 = changeQuickRedirect;
        if (PatchProxy.isSupport(objArr, this, changeQuickRedirect2, 11113171)) {
            PatchProxy.accessDispatch(objArr, this, changeQuickRedirect2, 11113171);
        } else {
            if (this.f131930e == null || a() || getCallback() == null) {
                return;
            }
            this.f131930e.start();
        }
    }

    public final void e() {
        Object[] objArr = new Object[0];
        ChangeQuickRedirect changeQuickRedirect2 = changeQuickRedirect;
        if (PatchProxy.isSupport(objArr, this, changeQuickRedirect2, 395194)) {
            PatchProxy.accessDispatch(objArr, this, changeQuickRedirect2, 395194);
        } else {
            if (this.f131930e == null || !a()) {
                return;
            }
            this.f131930e.cancel();
        }
    }

    public final void f() {
        SGShimmer sGShimmer;
        Shader radialGradient;
        int i = 0;
        Object[] objArr = new Object[0];
        ChangeQuickRedirect changeQuickRedirect2 = changeQuickRedirect;
        if (PatchProxy.isSupport(objArr, this, changeQuickRedirect2, 6300766)) {
            PatchProxy.accessDispatch(objArr, this, changeQuickRedirect2, 6300766);
            return;
        }
        Rect bounds = getBounds();
        int iWidth = bounds.width();
        int iHeight = bounds.height();
        if (iWidth != 0 && iHeight != 0 && (sGShimmer = this.f) != null) {
            int iB = sGShimmer.b(iWidth);
            int iA = this.f.a(iHeight);
            SGShimmer sGShimmer2 = this.f;
            boolean z = true;
            if (sGShimmer2.f != 1) {
                int i2 = sGShimmer2.f131918c;
                if (i2 != 1 && i2 != 3) {
                    z = false;
                }
                if (z) {
                    iB = 0;
                }
                if (z) {
                    i = iA;
                }
                float f = i;
                SGShimmer sGShimmer3 = this.f;
                radialGradient = new LinearGradient(0.0f, 0.0f, iB, f, sGShimmer3.f131917b, sGShimmer3.f131916a, Shader.TileMode.CLAMP);
            } else {
                float f2 = iA / 2.0f;
                float fMax = (float) (((double) Math.max(iB, iA)) / Math.sqrt(2.0d));
                SGShimmer sGShimmer4 = this.f;
                radialGradient = new RadialGradient(iB / 2.0f, f2, fMax, sGShimmer4.f131917b, sGShimmer4.f131916a, Shader.TileMode.CLAMP);
            }
            this.f131927b.setShader(radialGradient);
        }
    }

    public final void c(@Nullable SGShimmer sGShimmer) {
        boolean zIsStarted;
        PorterDuff.Mode mode;
        Object[] objArr = {sGShimmer};
        ChangeQuickRedirect changeQuickRedirect2 = changeQuickRedirect;
        if (PatchProxy.isSupport(objArr, this, changeQuickRedirect2, 10727083)) {
            PatchProxy.accessDispatch(objArr, this, changeQuickRedirect2, 10727083);
            return;
        }
        this.f = sGShimmer;
        if (sGShimmer != null) {
            Paint paint = this.f131927b;
            if (this.f.p) {
                mode = PorterDuff.Mode.DST_IN;
            } else {
                mode = PorterDuff.Mode.SRC_IN;
            }
            paint.setXfermode(new PorterDuffXfermode(mode));
        }
        f();
        if (this.f != null) {
            ValueAnimator valueAnimator = this.f131930e;
            if (valueAnimator != null) {
                zIsStarted = valueAnimator.isStarted();
                this.f131930e.cancel();
                this.f131930e.removeAllUpdateListeners();
            } else {
                zIsStarted = false;
            }
            SGShimmer sGShimmer2 = this.f;
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, (sGShimmer2.t / sGShimmer2.s) + 1.0f);
            this.f131930e = valueAnimatorOfFloat;
            valueAnimatorOfFloat.setInterpolator(new LinearInterpolator());
            this.f131930e.setRepeatMode(this.f.r);
            this.f131930e.setStartDelay(this.f.u);
            this.f131930e.setRepeatCount(this.f.q);
            ValueAnimator valueAnimator2 = this.f131930e;
            SGShimmer sGShimmer3 = this.f;
            valueAnimator2.setDuration(sGShimmer3.s + sGShimmer3.t);
            this.f131930e.addUpdateListener(this.f131926a);
            if (zIsStarted) {
                this.f131930e.start();
            }
        }
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public final void draw(@NonNull Canvas canvas) {
        float fFloatValue;
        float f;
        Object[] objArr = {canvas};
        ChangeQuickRedirect changeQuickRedirect2 = changeQuickRedirect;
        if (PatchProxy.isSupport(objArr, this, changeQuickRedirect2, 15000188)) {
            PatchProxy.accessDispatch(objArr, this, changeQuickRedirect2, 15000188);
            return;
        }
        if (this.f != null && this.f131927b.getShader() != null) {
            float fTan = (float) Math.tan(Math.toRadians(this.f.m));
            float fHeight = this.f131928c.height() + (this.f131928c.width() * fTan);
            float fWidth = this.f131928c.width() + (fTan * this.f131928c.height());
            ValueAnimator valueAnimator = this.f131930e;
            float f2 = 0.0f;
            if (valueAnimator != null) {
                fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            } else {
                fFloatValue = 0.0f;
            }
            int i = this.f.f131918c;
            if (i != 1) {
                if (i != 2) {
                    if (i != 3) {
                        float f3 = -fWidth;
                        f2 = f3 + ((fWidth - f3) * fFloatValue);
                    } else {
                        f = fHeight + (((-fHeight) - fHeight) * fFloatValue);
                    }
                } else {
                    f2 = fWidth + (((-fWidth) - fWidth) * fFloatValue);
                }
                f = 0.0f;
            } else {
                float f4 = -fHeight;
                f = f4 + ((fHeight - f4) * fFloatValue);
            }
            this.f131929d.reset();
            this.f131929d.setRotate(this.f.m, this.f131928c.width() / 2.0f, this.f131928c.height() / 2.0f);
            this.f131929d.postTranslate(f2, f);
            this.f131927b.getShader().setLocalMatrix(this.f131929d);
            canvas.drawRect(this.f131928c, this.f131927b);
        }
    }

    @Override // android.graphics.drawable.Drawable
    public final void onBoundsChange(Rect rect) {
        Object[] objArr = {rect};
        ChangeQuickRedirect changeQuickRedirect2 = changeQuickRedirect;
        if (PatchProxy.isSupport(objArr, this, changeQuickRedirect2, 3348585)) {
            PatchProxy.accessDispatch(objArr, this, changeQuickRedirect2, 3348585);
            return;
        }
        super.onBoundsChange(rect);
        this.f131928c.set(rect);
        f();
        b();
    }
}
