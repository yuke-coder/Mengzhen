package com.sankuai.waimai.store.shimmer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mengzhen.app.R;
import com.sankuai.waimai.store.shimmer.SGShimmer;

/* JADX INFO: loaded from: classes2.dex */
public class SGShimmerFrameLayout extends FrameLayout {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public final Paint f131922a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public final a f131923b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    public boolean f131924c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    public boolean f131925d;

    public final void c() {

            this.f131923b.d();

    }

    @Nullable
    public SGShimmer getShimmer() {
        return this.f131923b.f;
    }

    @Override // android.view.View
    public final boolean verifyDrawable(@NonNull Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == this.f131923b;
    }

    public final void d() {

            this.f131925d = false;
            this.f131923b.e();

    }

    @Override // android.view.ViewGroup, android.view.View
    public final void onAttachedToWindow() {

            super.onAttachedToWindow();
            this.f131923b.b();

    }

    @Override // android.view.ViewGroup, android.view.View
    public final void onDetachedFromWindow() {

            super.onDetachedFromWindow();
            d();

    }

    public SGShimmerFrameLayout(Context context) {
        super(context);

        this.f131922a = new Paint();
        this.f131923b = new a();
        this.f131924c = true;
        this.f131925d = false;
        a(context, null);
    }

    public final SGShimmerFrameLayout b(@Nullable SGShimmer sGShimmer) {

        this.f131923b.c(sGShimmer);
        if (sGShimmer != null && sGShimmer.n) {
            setLayerType(2, this.f131922a);
        } else {
            setLayerType(0, null);
        }
        return this;
    }

    @Override // android.view.ViewGroup, android.view.View
    public final void dispatchDraw(Canvas canvas) {

        super.dispatchDraw(canvas);
        if (this.f131924c) {
            this.f131923b.draw(canvas);
        }
    }

    public SGShimmerFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        this.f131922a = new Paint();
        this.f131923b = new a();
        this.f131924c = true;
        this.f131925d = false;
        a(context, attributeSet);
    }

    public final void a(@Nullable Context context, AttributeSet attributeSet) {
        SGShimmer.b aVar;

        setWillNotDraw(false);
        this.f131923b.setCallback(this);
        if (attributeSet == null) {
            b(new SGShimmer.a().a());
            return;
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, new int[]{R.attr.shimmer_auto_start, R.attr.shimmer_base_alpha, R.attr.shimmer_base_color, R.attr.shimmer_clip_to_children, R.attr.shimmer_colored, R.attr.shimmer_direction, R.attr.shimmer_dropoff, R.attr.shimmer_duration, R.attr.shimmer_fixed_height, R.attr.shimmer_fixed_width, R.attr.shimmer_height_ratio, R.attr.shimmer_highlight_alpha, R.attr.shimmer_highlight_color, R.attr.shimmer_intensity, R.attr.shimmer_repeat_count, R.attr.shimmer_repeat_delay, R.attr.shimmer_repeat_mode, R.attr.shimmer_shape, R.attr.shimmer_start_delay, R.attr.shimmer_tilt, R.attr.shimmer_width_ratio}, 0, 0);
        try {
            if (typedArrayObtainStyledAttributes.hasValue(4) && typedArrayObtainStyledAttributes.getBoolean(4, false)) {
                aVar = new SGShimmer.c();
            } else {
                aVar = new SGShimmer.a();
            }
            b(aVar.b(typedArrayObtainStyledAttributes).a());
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    @Override // android.view.View
    public final void onVisibilityChanged(View view, int i) {

        super.onVisibilityChanged(view, i);
        a aVar = this.f131923b;
        if (aVar == null) {
            return;
        }
        if (i != 0) {
            if (aVar.a()) {
                d();
                this.f131925d = true;
                return;
            }
            return;
        }
        if (this.f131925d) {
            aVar.b();
            this.f131925d = false;
        }
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    public final void onLayout(boolean z, int i, int i2, int i3, int i4) {

            super.onLayout(z, i, i2, i3, i4);
            this.f131923b.setBounds(0, 0, getWidth(), getHeight());

    }
}
