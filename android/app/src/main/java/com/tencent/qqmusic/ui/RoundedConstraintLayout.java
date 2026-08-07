package com.tencent.qqmusic.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.view.ViewOutlineProvider;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes20.dex */
public class RoundedConstraintLayout extends ConstraintLayout {
    public static final float C;
    private static final List<Pair<Integer, Float>> D;
    private int A;
    private int B;
    private d p;
    private boolean q;
    private float r;
    private HashMap<Integer, Float> s;
    private List<Pair<Integer, Float>> t;
    private float u;
    private final Paint v;
    private final Path w;
    private final RectF x;
    private float y;
    private boolean z;

    private final class c implements d {
        private final Rect a;

        class a extends ViewOutlineProvider {
            final /* synthetic */ int a;
            final /* synthetic */ int b;

            a(int i, int i2) {
                this.a = i;
                this.b = i2;
            }

            /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view, Outline outline) {
                byte[] bArr = SwordSwitches.switches15;
                if (bArr == null || ((bArr[1078] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{view, outline}, this, 165429).isSupported) {
                    if (RoundedConstraintLayout.this.s.size() <= 0) {
                        outline.setRoundRect(c.this.a, RoundedConstraintLayout.this.r);
                        return;
                    }
                    Path path = new Path();
                    float[] fArr = new float[8];
                    Float f = (Float) RoundedConstraintLayout.this.s.get(1);
                    fArr[0] = f != null ? f.floatValue() : 0.0f;
                    fArr[1] = f != null ? f.floatValue() : 0.0f;
                    Float f2 = (Float) RoundedConstraintLayout.this.s.get(2);
                    fArr[2] = f2 != null ? f2.floatValue() : 0.0f;
                    fArr[3] = f2 != null ? f2.floatValue() : 0.0f;
                    Float f3 = (Float) RoundedConstraintLayout.this.s.get(8);
                    fArr[4] = f3 != null ? f3.floatValue() : 0.0f;
                    fArr[5] = f3 != null ? f3.floatValue() : 0.0f;
                    Float f4 = (Float) RoundedConstraintLayout.this.s.get(4);
                    fArr[6] = f4 != null ? f4.floatValue() : 0.0f;
                    fArr[7] = f4 != null ? f4.floatValue() : 0.0f;
                    path.addRoundRect(0.0f, 0.0f, this.a, this.b, fArr, Path.Direction.CW);
                    if (Build.VERSION.SDK_INT >= 30) {
                        outline.setPath(path);
                    } else {
                        outline.setConvexPath(path);
                    }
                }
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
        private c() {
            this.a = new Rect();
        }

        @Override // com.tencent.qqmusic.ui.RoundedConstraintLayout.d
        public void a(Canvas canvas, boolean z) {
        }

        @Override // com.tencent.qqmusic.ui.RoundedConstraintLayout.d
        public void b(Canvas canvas, boolean z) {
        }

        /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
        @Override // com.tencent.qqmusic.ui.RoundedConstraintLayout.d
        public void c(int i, int i2) {
            byte[] bArr = SwordSwitches.switches15;
            if (bArr == null || ((bArr[852] >> 1) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}, this, 163618).isSupported) {
                this.a.set(0, 0, i, i2);
                RoundedConstraintLayout.this.setClipToOutline(true);
                RoundedConstraintLayout.this.setOutlineProvider(new a(i, i2));
            }
        }

        @Override // com.tencent.qqmusic.ui.RoundedConstraintLayout.d
        public void d(float f, float f2, float f3, float f4) {
            byte[] bArr = SwordSwitches.switches15;
            if (bArr == null || ((bArr[850] >> 6) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3), Float.valueOf(f4)}, this, 163607).isSupported) {
                RoundedConstraintLayout.this.invalidateOutline();
            }
        }

        @Override // com.tencent.qqmusic.ui.RoundedConstraintLayout.d
        public void setCornerRadius(float f) {
            byte[] bArr = SwordSwitches.switches15;
            if (bArr == null || ((bArr[849] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 163599).isSupported) {
                RoundedConstraintLayout.this.invalidateOutline();
            }
        }
    }

    interface d {
        void a(Canvas canvas, boolean z);

        void b(Canvas canvas, boolean z);

        void c(int i, int i2);

        void d(float f, float f2, float f3, float f4);

        void setCornerRadius(float f);
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    static {
        float fE;
        try {
            fE = com.tencent.qqmusiccommon.util.j0.b(7.5f);
        } catch (Throwable unused) {
            fE = 0.0f;
        }
        C = fE;
        D = Arrays.asList(new Pair(-16716927, Float.valueOf(0.0314f)), new Pair(-16716927, Float.valueOf(0.4935f)), new Pair(-16738837, Float.valueOf(0.9382f)));
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public RoundedConstraintLayout(@NonNull Context context) {
        this(context, null);
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    static boolean M(RoundedConstraintLayout view) {
        return view.q;
    }

    static float[] N(RoundedConstraintLayout view) {
        return view.getRadiusArray();
    }

    static float O(RoundedConstraintLayout view) {
        return view.r;
    }

    private void Q(Canvas canvas) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1211] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(canvas, this, 166489).isSupported) {
            this.v.setStrokeWidth(this.y);
            float f = this.y / 2.0f;
            this.x.set(f, f, getWidth() - f, getHeight() - f);
            float fMax = Math.max(this.r - f, 0.0f);
            this.w.reset();
            this.w.addRoundRect(this.x, fMax, fMax, Path.Direction.CW);
            canvas.drawPath(this.w, this.v);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    private void R() {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1195] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(null, this, 166368).isSupported) {
            if (this.B == 0) {
                this.p = new c();
            } else {
                this.p = new RoundedConstraintLayout$b(this);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r15v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    private void S() {
        byte[] bArr = SwordSwitches.switches15;
        if ((bArr == null || ((bArr[1207] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg(null, this, 166460).isSupported) && getWidth() > 0 && getHeight() > 0) {
            if (this.t == null) {
                this.v.setShader(null);
                this.v.setColor(this.A);
                return;
            }
            double radians = Math.toRadians(((double) this.u) - 90.0d);
            float width = getWidth() / 2.0f;
            float height = getHeight() / 2.0f;
            double dSqrt = Math.sqrt((getWidth() * getWidth()) + (getHeight() * getHeight())) / 2.0d;
            float fCos = width - ((float) (Math.cos(radians) * dSqrt));
            float fSin = height - ((float) (Math.sin(radians) * dSqrt));
            float fCos2 = width + ((float) (Math.cos(radians) * dSqrt));
            float fSin2 = height + ((float) (dSqrt * Math.sin(radians)));
            int size = this.t.size();
            int[] iArr = new int[size];
            float[] fArr = new float[size];
            for (int i = 0; i < size; i++) {
                iArr[i] = ((Integer) this.t.get(i).first).intValue();
                fArr[i] = ((Float) this.t.get(i).second).floatValue();
            }
            this.v.setShader(new LinearGradient(fCos, fSin, fCos2, fSin2, iArr, fArr, Shader.TileMode.CLAMP));
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX INFO: Access modifiers changed from: private */
    public float[] getRadiusArray() {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr != null && ((bArr[1213] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(null, this, 166506);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (float[]) swordProxyResultProxyOneArg.result;
            }
        }
        if (this.B == 0) {
            return null;
        }
        float[] fArr = new float[8];
        int[] iArr = {1, 2, 8, 4};
        boolean z = false;
        for (int i = 0; i < 4; i++) {
            Float f = this.s.get(Integer.valueOf(iArr[i]));
            int i2 = iArr[i];
            if ((this.B & i2) != i2 || f == null) {
                int i3 = i * 2;
                fArr[i3] = 0.0f;
                fArr[i3 + 1] = 0.0f;
            } else {
                int i4 = i * 2;
                fArr[i4] = f.floatValue();
                fArr[i4 + 1] = f.floatValue();
                z = true;
            }
        }
        if (z) {
            return fArr;
        }
        return null;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    public void dispatchDraw(Canvas canvas) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1200] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(canvas, this, 166406).isSupported) {
            d dVar = this.p;
            if (dVar != null) {
                dVar.b(canvas, true);
            }
            super.dispatchDraw(canvas);
            d dVar2 = this.p;
            if (dVar2 != null) {
                dVar2.a(canvas, true);
            }
            if (!this.z || this.y <= 0.0f || getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            Q(canvas);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    public void draw(Canvas canvas) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1199] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(canvas, this, 166397).isSupported) {
            d dVar = this.p;
            if (dVar != null) {
                dVar.b(canvas, false);
            }
            super.draw(canvas);
            d dVar2 = this.p;
            if (dVar2 != null) {
                dVar2.a(canvas, false);
            }
        }
    }

    public float getRadius() {
        return this.r;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1203] >> 2) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4)}, this, 166427).isSupported) {
            super.onSizeChanged(i, i2, i3, i4);
            d dVar = this.p;
            if (dVar != null) {
                dVar.c(i, i2);
            }
            S();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    @MainThread
    public void setCornerRadius(float f) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1197] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 166377).isSupported) {
            this.r = f;
            this.p.setCornerRadius(f);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    public void setGradientStrokeAngle(float f) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1206] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 166455).isSupported) {
            this.u = f;
            S();
            invalidate();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    public void setGradientStrokeColor(int i) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1205] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 166447).isSupported) {
            this.A = i;
            this.t = null;
            S();
            invalidate();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    public void setGradientStrokeColorStops(List<Pair<Integer, Float>> list) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1206] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(list, this, 166453).isSupported) {
            if (list != null) {
                this.t = new ArrayList(list);
            }
            S();
            invalidate();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    public void setGradientStrokeVisible(boolean z) {
        byte[] bArr = SwordSwitches.switches15;
        if ((bArr == null || ((bArr[1204] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 166438).isSupported) && this.z != z) {
            this.z = z;
            invalidate();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    public void setGradientStrokeWidthDp(float f) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1205] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 166444).isSupported) {
            this.y = com.tencent.qqmusiccommon.util.j0.b(f);
            invalidate();
        }
    }

    public void setIsClipBackground(boolean z) {
        this.q = z;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public void setRadiusMode(int i) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1196] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 166374).isSupported) {
            this.B = i;
            R();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    public RoundedConstraintLayout(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.tencent.qqmusic.ui.RoundedConstraintLayout */
    /* JADX WARN: Multi-variable type inference failed */
    public RoundedConstraintLayout(@NonNull Context context, @Nullable AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.s = new HashMap<>();
        this.t = new ArrayList(D);
        this.u = 328.3f;
        Paint paint = new Paint(1);
        this.v = paint;
        this.w = new Path();
        this.x = new RectF();
        this.y = com.tencent.qqmusiccommon.util.j0.b(1.0f);
        this.z = false;
        this.A = 0;
        if (isInEditMode()) {
            return;
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, com.mengzhen.app.R.styleable.RoundedConstraintLayout);
        this.r = typedArrayObtainStyledAttributes.getDimension(2, C);
        this.B = typedArrayObtainStyledAttributes.getInt(1, 0);
        this.q = typedArrayObtainStyledAttributes.getBoolean(0, false);
        typedArrayObtainStyledAttributes.recycle();
        R();
        setCornerRadius(this.r);
        paint.setStyle(Paint.Style.STROKE);
    }

    @MainThread
    public void setCornerRadius(float f, float f2, float f3, float f4) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1197] >> 5) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3), Float.valueOf(f4)}, this, 166382).isSupported) {
            this.s.put(1, Float.valueOf(f));
            this.s.put(2, Float.valueOf(f2));
            this.s.put(4, Float.valueOf(f3));
            this.s.put(8, Float.valueOf(f4));
            this.p.d(f, f2, f3, f4);
        }
    }
}


