package com.tencent.qqmusic.business.customskin.player.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import com.mengzhen.app.R;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusiccommon.util.j0;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes3.dex */
@SourceDebugExtension({"SMAP\nBtnTypeSeekbarView.kt\nKotlin\n*S Kotlin\n*F\n+ 1 BtnTypeSeekbarView.kt\ncom/tencent/qqmusic/business/customskin/player/view/BtnTypeSeekbarView\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,648:1\n1872#2,3:649\n1872#2,3:652\n1872#2,3:655\n1863#2,2:658\n1872#2,3:660\n*S KotlinDebug\n*F\n+ 1 BtnTypeSeekbarView.kt\ncom/tencent/qqmusic/business/customskin/player/view/BtnTypeSeekbarView\n*L\n199#1:649,3\n383#1:652,3\n415#1:655,3\n456#1:658,2\n478#1:660,3\n*E\n"})
public final class BtnTypeSeekbarView extends View {

    @NotNull
    private d A;

    @NotNull
    private final ArrayList<d> B;

    @NotNull
    private final Paint C;

    @NotNull
    private c D;

    @NotNull
    private a E;

    @NotNull
    private a F;

    @Nullable
    private GestureDetector G;

    @NotNull
    private final ArrayList<String> H;
    private int I;
    private int J;
    private float K;
    private int L;
    private float M;
    private float N;
    private float O;
    private float P;
    private int Q;
    private float R;
    private float S;
    private float T;
    private float U;
    private int V;

    @Nullable
    private b W;

    @NotNull
    private final ArrayList<String> a0;
    private final int b;
    private float b0;
    private float c0;
    private final int d;
    private int d0;
    private final int e;

    @NotNull
    private GestureDetector.SimpleOnGestureListener e0;
    private final int f;
    private final int g;
    private final int h;
    private final float i;
    private final float j;
    private float l;
    private final float m;
    private final float n;
    private final float o;
    private final float p;
    private final float q;
    private final float r;
    private final float s;
    private boolean t;
    private int u;
    private int v;
    private float w;
    private float x;
    private int y;
    private int z;

    public static final class a {
        private float a;
        private float b;

        @NotNull
        private RectF c = new RectF();

        @NotNull
        public final RectF a() {
            return this.c;
        }

        /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
        public final void b(float f, float f2, float f3, float f4) {
            byte[] bArr = SwordSwitches.switches10;
            if (bArr == null || ((bArr[230] >> 6) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3), Float.valueOf(f4)}, this, 102647).isSupported) {
                this.a = f;
                this.b = f2;
                this.c = new RectF(f, f2, f3, f4);
            }
        }
    }

    public interface b {
        void a(int i);

        void b(int i);
    }

    public static final class c {
        private float a;
        private float b;
        private float c;
        private float d;

        public final float a() {
            return this.a;
        }

        public final float b() {
            return this.b;
        }

        public final float c() {
            return this.c;
        }

        public final float d() {
            return this.d;
        }

        /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
        public final void e(float f, float f2, float f3, float f4) {
            this.a = f;
            this.b = f2;
            this.c = f3;
            this.d = f4;
        }
    }

    public static final class d {
        private float a;
        private float b;
        private float c;
        private int d;

        public d(float f) {
            this.a = f;
        }

        /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
        public final boolean a(float f, float f2) {
            byte[] bArr = SwordSwitches.switches10;
            if (bArr != null && ((bArr[229] >> 3) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f), Float.valueOf(f2)}, this, 102636);
                if (swordProxyResultProxyMoreArgs.isSupported) {
                    return ((Boolean) swordProxyResultProxyMoreArgs.result).booleanValue();
                }
            }
            float f3 = this.b;
            float f4 = (f3 - f) * (f3 - f);
            float f5 = this.c;
            return Math.sqrt((double) (f4 + ((f5 - f2) * (f5 - f2)))) < ((double) ((this.a / 2.0f) + ((float) j0.b(20.0f))));
        }

        public final int b() {
            return this.d;
        }

        public final float c() {
            return this.a;
        }

        public final float d() {
            return this.b;
        }

        public final float e() {
            return this.c;
        }

        public final void f(int i) {
            this.d = i;
        }

        public final void g(float f) {
            this.a = f;
        }

        public final void h(float f) {
            this.b = f;
        }

        public final void i(float f) {
            this.c = f;
        }
    }

    public final class e extends GestureDetector.SimpleOnGestureListener {
        e() {
        }

        /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onDown(MotionEvent motionEvent) {
            byte[] bArr = SwordSwitches.switches10;
            if (bArr != null && ((bArr[231] >> 3) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(motionEvent, this, 102652);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
                }
            }
            Intrinsics.checkNotNullParameter(motionEvent, "e");
            BtnTypeSeekbarView btnTypeSeekbarView = BtnTypeSeekbarView.this;
            btnTypeSeekbarView.t = btnTypeSeekbarView.A.a(motionEvent.getX(), motionEvent.getY());
            return super.onDown(motionEvent);
        }

        /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
        /* JADX WARN: Removed duplicated region for block: B:28:0x00be  */
        /* JADX WARN: Removed duplicated region for block: B:43:0x011f  */
        /* JADX WARN: Removed duplicated region for block: B:47:0x012c  */
        /* JADX WARN: Removed duplicated region for block: B:50:0x013a  */
        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            byte[] bArr = SwordSwitches.switches10;
            if (bArr != null && ((bArr[233] >> 0) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{motionEvent, motionEvent2, Float.valueOf(f), Float.valueOf(f2)}, this, 102665);
                if (swordProxyResultProxyMoreArgs.isSupported) {
                    return ((Boolean) swordProxyResultProxyMoreArgs.result).booleanValue();
                }
            }
            Intrinsics.checkNotNullParameter(motionEvent2, "e2");
            if (Math.abs(f2) > Math.abs(f)) {
                return super.onScroll(motionEvent, motionEvent2, f, f2);
            }
            if (!BtnTypeSeekbarView.this.t) {
                return super.onScroll(motionEvent, motionEvent2, f, f2);
            }
            BtnTypeSeekbarView.this.getParent().requestDisallowInterceptTouchEvent(true);
            float fD = BtnTypeSeekbarView.this.A.d();
            int blockedIndex = -1;
            boolean blocked = false;
            if (BtnTypeSeekbarView.this.b0 >= 0.0f && fD > BtnTypeSeekbarView.this.b0) {
                if (BtnTypeSeekbarView.this.B.size() > BtnTypeSeekbarView.this.V &&
                        ((d) BtnTypeSeekbarView.this.B.get(BtnTypeSeekbarView.this.V)).d() == BtnTypeSeekbarView.this.b0) {
                    blockedIndex = BtnTypeSeekbarView.this.V + 1;
                    blocked = true;
                }
                fD = BtnTypeSeekbarView.this.b0;
            } else if (BtnTypeSeekbarView.this.c0 >= 0.0f && fD < BtnTypeSeekbarView.this.c0) {
                if (BtnTypeSeekbarView.this.B.size() > BtnTypeSeekbarView.this.V &&
                        ((d) BtnTypeSeekbarView.this.B.get(BtnTypeSeekbarView.this.V)).d() == BtnTypeSeekbarView.this.c0) {
                    blockedIndex = BtnTypeSeekbarView.this.V - 1;
                    blocked = true;
                }
                fD = BtnTypeSeekbarView.this.c0;
            }

            if (blocked && BtnTypeSeekbarView.this.d0 == -1) {
                BtnTypeSeekbarView.this.d0 = blockedIndex;
            } else {
                BtnTypeSeekbarView.this.v(fD - f, false, false, false);
                BtnTypeSeekbarView.this.postInvalidate();
            }
            return true;
        }

        /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            byte[] bArr = SwordSwitches.switches10;
            if (bArr != null && ((bArr[231] >> 6) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(motionEvent, this, 102655);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
                }
            }
            Intrinsics.checkNotNullParameter(motionEvent, "e");
            c cVar = BtnTypeSeekbarView.this.D;
            float x = motionEvent.getX();
            if (x > cVar.c()) {
                x = cVar.c();
            } else if (x < cVar.a()) {
                x = cVar.a();
            }
            int i = -1;
            boolean z = false;
            if (BtnTypeSeekbarView.this.b0 >= 0.0f && x > BtnTypeSeekbarView.this.b0) {
                if (BtnTypeSeekbarView.this.B.size() > BtnTypeSeekbarView.this.V) {
                    if (((d) BtnTypeSeekbarView.this.B.get(BtnTypeSeekbarView.this.V)).d() == BtnTypeSeekbarView.this.b0) {
                        i = BtnTypeSeekbarView.this.V + 1;
                        z = true;
                    }
                }
                x = BtnTypeSeekbarView.this.b0;
            } else if (BtnTypeSeekbarView.this.c0 >= 0.0f && x < BtnTypeSeekbarView.this.c0) {
                if (BtnTypeSeekbarView.this.B.size() > BtnTypeSeekbarView.this.V) {
                    if (((d) BtnTypeSeekbarView.this.B.get(BtnTypeSeekbarView.this.V)).d() == BtnTypeSeekbarView.this.c0) {
                        i = BtnTypeSeekbarView.this.V - 1;
                        z = true;
                    }
                }
                x = BtnTypeSeekbarView.this.c0;
            }
            if (z) {
                b bVar = BtnTypeSeekbarView.this.W;
                if (bVar != null) {
                    bVar.b(i);
                }
            } else {
                BtnTypeSeekbarView.this.p(x - cVar.a(), true, true);
            }
            return true;
        }
    }

    public final class f extends AnimatorListenerAdapter {
        final /* synthetic */ int d;
        final /* synthetic */ boolean e;

        f(int i, boolean z) {
            this.d = i;
            this.e = z;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            byte[] bArr = SwordSwitches.switches10;
            if (bArr == null || ((bArr[230] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(animator, this, 102645).isSupported) {
                Intrinsics.checkNotNullParameter(animator, "animation");
                BtnTypeSeekbarView btnTypeSeekbarView = BtnTypeSeekbarView.this;
                btnTypeSeekbarView.v(((d) btnTypeSeekbarView.B.get(this.d)).d(), false, this.e, true);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    public BtnTypeSeekbarView(@NotNull Context context) {
        this(context, null);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    private final float getDefaultHeight() {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr != null && ((bArr[227] >> 3) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg((Object) null, this, 102620);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Float) swordProxyResultProxyOneArg.result).floatValue();
            }
        }
        this.C.setTextSize(this.K);
        Paint.FontMetrics fontMetrics = this.C.getFontMetrics();
        return getPaddingTop() + this.w + getPaddingBottom() + this.U + (fontMetrics.bottom - fontMetrics.top);
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    private final boolean o(int i, int i2) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr != null && ((bArr[235] >> 4) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}, this, 102685);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Boolean) swordProxyResultProxyMoreArgs.result).booleanValue();
            }
        }
        if (i == i2) {
            return true;
        }
        String str = this.H.get(i2);
        Intrinsics.checkNotNullExpressionValue(str, "get(...)");
        if (!this.a0.contains(str)) {
            return true;
        }
        setSelectedIndex(i);
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX INFO: Access modifiers changed from: private */
    public final void p(float f2, final boolean z, boolean z2) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[232] >> 7) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f2), Boolean.valueOf(z), Boolean.valueOf(z2)}, this, 102664).isSupported) {
            float f3 = this.x;
            int i = (int) (f2 / f3);
            if (f2 % f3 > f3 / 2.0f) {
                i++;
            }
            int iAbs = Math.abs(this.A.b() - i);
            if (iAbs == 0) {
                if (z) {
                    return;
                } else {
                    iAbs = 1;
                }
            }
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.A.d(), this.B.get(i).d());
            valueAnimatorOfFloat.setDuration(((long) 100) + (((long) iAbs) * 30));
            valueAnimatorOfFloat.setInterpolator(new AccelerateDecelerateInterpolator());
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.tencent.qqmusic.business.customskin.player.view.a
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    BtnTypeSeekbarView.q(BtnTypeSeekbarView.this, z, valueAnimator);
                }
            });
            valueAnimatorOfFloat.addListener(new f(i, z2));
            valueAnimatorOfFloat.start();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX INFO: Access modifiers changed from: private */
    public static final void q(BtnTypeSeekbarView btnTypeSeekbarView, boolean z, ValueAnimator valueAnimator) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[236] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{btnTypeSeekbarView, Boolean.valueOf(z), valueAnimator}, (Object) null, 102693).isSupported) {
            Intrinsics.checkNotNullParameter(btnTypeSeekbarView, "this$0");
            Intrinsics.checkNotNullParameter(valueAnimator, "animation");
            Object animatedValue = valueAnimator.getAnimatedValue();
            Intrinsics.checkNotNull(animatedValue, "null cannot be cast to non-null type kotlin.Float");
            btnTypeSeekbarView.v(((Float) animatedValue).floatValue(), z, false, false);
            btnTypeSeekbarView.postInvalidate();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    private final void r() {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[225] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 102604).isSupported) {
            int size = this.H.size() - 1;
            int i = 0;
            int i2 = 0;
            for (Object obj : this.H) {
                int i3 = i + 1;
                if (i < 0) {
                    CollectionsKt.throwIndexOverflow();
                }
                if (this.a0.contains((String) obj)) {
                    if (i <= size) {
                    }
                    i = i3;
                } else if (i < i2) {
                    i2 = i;
                }
                size = i;
                i = i3;
            }
            if (this.H.size() == this.B.size()) {
                this.c0 = this.B.get(i2).d();
                this.b0 = this.B.get(size).d();
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    private final void s() {
        byte[] bArr = SwordSwitches.switches10;
        if ((bArr == null || ((bArr[229] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 102634).isSupported) && this.v > 0 && this.x > 0.0f) {
            this.B.clear();
            int i = 0;
            for (Object obj : this.H) {
                int i2 = i + 1;
                if (i < 0) {
                    CollectionsKt.throwIndexOverflow();
                }
                d dVar = new d(this.T);
                dVar.f(i);
                if (i == 0) {
                    dVar.h(this.D.a() + (this.T / 2.0f));
                } else if (i == this.H.size() - 1) {
                    dVar.h((this.D.a() + (i * this.x)) - (this.T / 2.0f));
                } else {
                    dVar.h(this.D.a() + (i * this.x));
                }
                dVar.i(this.D.b());
                this.B.add(dVar);
                i = i2;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    private final void t() {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[228] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 102629).isSupported) {
            int i = 0;
            for (Object obj : this.B) {
                int i2 = i + 1;
                if (i < 0) {
                    CollectionsKt.throwIndexOverflow();
                }
                if (i == this.V) {
                    this.A.i(this.D.b());
                    if (i == 0) {
                        this.A.h(this.D.a() + (this.S / 2.0f));
                    } else if (i == this.H.size() - 1) {
                        this.A.h((this.D.a() + (i * this.x)) - (this.S / 2.0f));
                    } else {
                        this.A.h(this.D.a() + (i * this.x));
                    }
                }
                i = i2;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    /* JADX INFO: Access modifiers changed from: private */
    public final void v(float f2, boolean z, boolean z2, boolean z3) {
        b bVar;
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[234] >> 2) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f2), Boolean.valueOf(z), Boolean.valueOf(z2), Boolean.valueOf(z3)}, this, 102675).isSupported) {
            float fA = this.D.a();
            float fC = this.D.c();
            float f3 = this.S;
            if (f2 < (f3 / 2.0f) + fA) {
                f2 = fA + (f3 / 2.0f);
            } else if (f2 > fC - (f3 / 2.0f)) {
                f2 = fC - (f3 / 2.0f);
            }
            this.A.h(f2);
            if (z) {
                return;
            }
            int iB = this.A.b();
            float fC2 = this.D.c() - this.D.a();
            int size = this.B.size();
            int i = iB;
            for (int i2 = 0; i2 < size; i2++) {
                float fAbs = Math.abs(f2 - this.B.get(i2).d());
                if (fC2 > fAbs) {
                    i = i2;
                    fC2 = fAbs;
                }
            }
            if (iB != i || z2) {
                int i3 = this.V;
                this.A.f(i);
                this.V = i;
                if (o(i3, i)) {
                    if ((z3 || z2) && (bVar = this.W) != null) {
                        bVar.a(this.V);
                    }
                }
            }
        }
    }

    @Nullable
    public final b getIndexChangeListener() {
        return this.W;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    @Override // android.view.View
    public void onDraw(@NotNull Canvas canvas) {
        float fD;
        byte[] bArr = SwordSwitches.switches10;
        int i = 0;
        if (bArr == null || ((bArr[230] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(canvas, this, 102641).isSupported) {
            Intrinsics.checkNotNullParameter(canvas, "canvas");
            c cVar = this.D;
            this.C.setColor(this.u);
            this.C.setStrokeWidth(this.w);
            float f2 = 255;
            this.C.setAlpha((int) ((this.M / 1.0f) * f2));
            canvas.drawLine(cVar.a() + (this.T / 2.0f), cVar.b(), cVar.c() - (this.T / 2.0f), cVar.d(), this.C);
            canvas.drawArc(this.E.a(), 90.0f, 180.0f, true, this.C);
            canvas.drawArc(this.F.a(), 270.0f, 180.0f, true, this.C);
            for (d dVar : this.B) {
                if (dVar.d() > this.b0) {
                    this.C.setColor(this.Q);
                    this.C.setAlpha((int) ((this.P / 1.0f) * f2));
                } else {
                    this.C.setColor(this.z);
                    this.C.setAlpha((int) ((this.R / 1.0f) * f2));
                }
                canvas.drawCircle(dVar.d(), dVar.e(), dVar.c() / 2.0f, this.C);
            }
            d dVar2 = this.A;
            this.C.setColor(this.y);
            this.C.setAlpha(255);
            canvas.drawCircle(dVar2.d(), dVar2.e(), dVar2.c() / 2.0f, this.C);
            if ((!this.H.isEmpty()) && (!this.B.isEmpty())) {
                this.C.setTextSize(this.K);
                for (Object obj : this.H) {
                    int i2 = i + 1;
                    if (i < 0) {
                        CollectionsKt.throwIndexOverflow();
                    }
                    String str = (String) obj;
                    if (this.a0.contains(str)) {
                        this.C.setColor(this.J);
                        this.C.setAlpha((int) ((this.O / 1.0f) * f2));
                    } else {
                        this.C.setColor(this.I);
                        this.C.setAlpha((int) ((this.N / 1.0f) * f2));
                    }
                    d dVar3 = this.B.get(i);
                    Intrinsics.checkNotNullExpressionValue(dVar3, "get(...)");
                    d dVar4 = dVar3;
                    float fMeasureText = this.C.measureText(str);
                    if (i == 0) {
                        fD = dVar4.d();
                        fMeasureText = dVar4.c();
                    } else if (i != this.H.size() - 1) {
                        fD = dVar4.d();
                    } else {
                        fD = dVar4.d() + (dVar4.c() / 2.0f);
                        canvas.drawText(str, fD - fMeasureText, dVar4.e() + dVar4.c() + this.U, this.C);
                        i = i2;
                    }
                    fMeasureText /= 2.0f;
                    canvas.drawText(str, fD - fMeasureText, dVar4.e() + dVar4.c() + this.U, this.C);
                    i = i2;
                }
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    @Override // android.view.View
    public void onMeasure(int i, int i2) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[226] >> 6) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}, this, 102615).isSupported) {
            super.onMeasure(i, i2);
            int size = View.MeasureSpec.getSize(i);
            int size2 = View.MeasureSpec.getSize(i2);
            if (getLayoutParams().height == -2) {
                setMeasuredDimension(size, (int) getDefaultHeight());
            } else {
                setMeasuredDimension(size, size2);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    @Override // android.view.View
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[227] >> 7) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4)}, this, 102624).isSupported) {
            super.onSizeChanged(i, i2, i3, i4);
            int paddingLeft = (i - getPaddingLeft()) - getPaddingRight();
            this.v = paddingLeft;
            this.x = (paddingLeft * 1.0f) / (this.H.size() - 1);
            float f2 = (i - this.v) / 2.0f;
            float paddingTop = ((this.S / 2.0f) - (this.w / 2.0f)) + getPaddingTop();
            this.D.e(f2, paddingTop, this.v + f2, paddingTop);
            s();
            t();
            r();
            this.A.g(this.S);
            a aVar = this.E;
            float f3 = this.w;
            aVar.b(f2, paddingTop - (f3 / 2.0f), this.T + f2, (f3 / 2.0f) + paddingTop);
            a aVar2 = this.F;
            int i5 = this.v;
            float f4 = (i5 + f2) - this.T;
            float f5 = this.w;
            aVar2.b(f4, paddingTop - (f5 / 2.0f), f2 + i5, paddingTop + (f5 / 2.0f));
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    @Override // android.view.View
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouchEvent(@NotNull MotionEvent motionEvent) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr != null && ((bArr[232] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(motionEvent, this, 102658);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(motionEvent, "event");
        GestureDetector gestureDetector = this.G;
        if (!(gestureDetector != null && gestureDetector.onTouchEvent(motionEvent)) && motionEvent.getAction() == 1 && this.t) {
            p(this.A.d() - this.D.a(), false, true);
            int i = this.d0;
            if (i >= 0) {
                this.d0 = -1;
                b bVar = this.W;
                if (bVar != null) {
                    bVar.b(i);
                }
            }
        }
        return true;
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public final void setDisableTextList(@NotNull List<String> list) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[225] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(list, this, 102602).isSupported) {
            Intrinsics.checkNotNullParameter(list, "disableTexts");
            this.a0.clear();
            this.a0.addAll(list);
            r();
            postInvalidate();
        }
    }

    public final void setIndexChangeListener(@NotNull b bVar) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[224] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(bVar, this, 102600).isSupported) {
            Intrinsics.checkNotNullParameter(bVar, "listener");
            this.W = bVar;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public final void setSelectedIndex(int i) {
        byte[] bArr = SwordSwitches.switches10;
        if ((bArr == null || ((bArr[226] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 102613).isSupported) && i != this.V && i < this.H.size()) {
            this.V = i;
            this.A.f(i);
            t();
            postInvalidate();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public final void u(@NotNull List<String> list) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[226] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(list, this, 102611).isSupported) {
            Intrinsics.checkNotNullParameter(list, "sectionTexts");
            this.H.clear();
            this.H.addAll(list);
            s();
            t();
            postInvalidate();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 2 */
    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    public BtnTypeSeekbarView(@NotNull Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, 0);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    @SuppressLint({"CustomViewStyleable"})
    public BtnTypeSeekbarView(@NotNull Context context, @Nullable AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        Intrinsics.checkNotNullParameter(context, "context");
        int color = Color.parseColor("#00F168");
        this.b = color;
        int color2 = Color.parseColor("#00F168");
        this.d = color2;
        int color3 = Color.parseColor("#00F168");
        this.e = color3;
        int color4 = Color.parseColor("#FFFFFF");
        this.f = color4;
        int color5 = Color.parseColor("#FFFFFF");
        this.g = color5;
        this.h = Color.parseColor("#2F2F2F");
        float fC = j0.c(18.0f);
        this.i = fC;
        float fC2 = j0.c(6.0f);
        this.j = fC2;
        this.l = j0.c(6.0f);
        this.m = 0.12f;
        this.n = 0.12f;
        float fC3 = j0.c(14.0f);
        this.o = fC3;
        this.p = 0.7f;
        this.q = 0.2f;
        this.r = 1.0f;
        float fC4 = j0.c(24.0f);
        this.s = fC4;
        this.u = color;
        this.w = this.l;
        this.A = new d(fC);
        this.B = new ArrayList<>();
        this.D = new c();
        this.E = new a();
        this.F = new a();
        ArrayList<String> arrayList = new ArrayList<>();
        this.H = arrayList;
        this.M = 0.12f;
        this.N = 0.7f;
        this.O = 0.2f;
        this.P = 0.12f;
        this.Q = color2;
        this.R = 0.12f;
        this.S = fC;
        this.T = fC2;
        this.U = fC4;
        this.a0 = new ArrayList<>();
        this.b0 = -1.0f;
        this.c0 = -1.0f;
        this.d0 = -1;
        this.e0 = new e();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, com.mengzhen.app.R.styleable.btnTypeSeekbar);
        Intrinsics.checkNotNullExpressionValue(typedArrayObtainStyledAttributes, "obtainStyledAttributes(...)");
        this.K = typedArrayObtainStyledAttributes.getDimension(13, fC3);
        this.L = typedArrayObtainStyledAttributes.getColor(11, color5);
        this.w = typedArrayObtainStyledAttributes.getDimension(4, this.l);
        this.M = typedArrayObtainStyledAttributes.getFloat(2, 0.12f);
        this.u = typedArrayObtainStyledAttributes.getColor(3, color);
        this.S = typedArrayObtainStyledAttributes.getDimension(9, fC);
        this.T = typedArrayObtainStyledAttributes.getDimension(7, fC2);
        this.R = typedArrayObtainStyledAttributes.getFloat(5, 0.12f);
        this.z = typedArrayObtainStyledAttributes.getColor(6, color2);
        this.y = typedArrayObtainStyledAttributes.getColor(8, color3);
        this.I = typedArrayObtainStyledAttributes.getColor(11, color5);
        this.J = typedArrayObtainStyledAttributes.getColor(1, color4);
        boolean z = false;
        this.O = typedArrayObtainStyledAttributes.getFloat(0, 0.2f);
        String string = typedArrayObtainStyledAttributes.getString(12);
        if (string != null && (!StringsKt.isBlank(string))) {
            z = true;
        }
        if (z) {
            arrayList.addAll(Arrays.asList(string.split(",", -1)));
        }
        typedArrayObtainStyledAttributes.recycle();
        Paint paint = new Paint(1);
        this.C = paint;
        paint.setStyle(Paint.Style.FILL);
        this.G = new GestureDetector(context, this.e0);
    }
}
