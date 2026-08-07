package com.tencent.qqmusic.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import androidx.annotation.FloatRange;
import com.tencent.component.widget.AsyncImageView;
import com.tencent.qqmusic.business.playernew.view.playersong.f90;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusiccommon.util.GlobalLifeCycleManager;
import com.tencent.qqmusiccommon.util.MLog;
import com.tme.karaoke.base.report.AbstractClickReport;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.json.internal.AbstractJsonLexerKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: classes20.dex */
public final class PlayerDiscAlbumPoleView extends FrameLayout {

    @NotNull
    public static final Companion n = new Companion(null);

    @NotNull
    private final AsyncImageView b;

    @Nullable
    private ObjectAnimator d;

    @Nullable
    private ValueAnimator e;
    private float f;
    private float g;
    private float h;
    private float i;
    private float j;
    private boolean l;
    private boolean m;

    public static final class Companion {
        private Companion() {
        }

        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    /* JADX INFO: loaded from: classes7.dex */
    public static final class ReverseDetectableAnimator {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        @NotNull
        private final ValueAnimator f34593a;
        private boolean b;

        public boolean equals(@Nullable Object obj) {
            byte[] bArr = SwordSwitches.switches2;
            if (bArr != null && ((bArr[1179] >> 7) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(obj, this, 20640);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
                }
            }
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ReverseDetectableAnimator)) {
                return false;
            }
            ReverseDetectableAnimator reverseDetectableAnimator = (ReverseDetectableAnimator) obj;
            return Intrinsics.areEqual(this.f34593a, reverseDetectableAnimator.f34593a) && this.b == reverseDetectableAnimator.b;
        }

        public int hashCode() {
            byte[] bArr = SwordSwitches.switches2;
            if (bArr != null && ((bArr[1179] >> 0) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(null, this, 20633);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return ((Integer) swordProxyResultProxyOneArg.result).intValue();
                }
            }
            return (this.f34593a.hashCode() * 31) + defpackage.o.a(this.b);
        }

        @NotNull
        public String toString() {
            byte[] bArr = SwordSwitches.switches2;
            if (bArr != null && ((bArr[1178] >> 0) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(null, this, 20625);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return (String) swordProxyResultProxyOneArg.result;
                }
            }
            return "ReverseDetectableAnimator(animator=" + this.f34593a + ", isReverse=" + this.b + ')';
        }
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public PlayerDiscAlbumPoleView(@NotNull Context context) {
        super(context);
        Intrinsics.checkNotNullParameter(context, "context");
        this.f = 11.0f;
        this.g = 10.0f;
        this.h = 0.57575756f;
        this.i = 0.22610015f;
        setClipChildren(false);
        AsyncImageView asyncImageView = new AsyncImageView(getContext());
        addView(asyncImageView, new FrameLayout.LayoutParams(-1, -1));
        this.b = asyncImageView;
    }

    private final float getCurrentPlayAngle() {
        return this.f + (this.j * this.g);
    }

    private final ObjectAnimator getPoleSwingAnimator() {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr != null && ((bArr[1370] >> 4) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(null, this, 22165);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (ObjectAnimator) swordProxyResultProxyOneArg.result;
            }
        }
        ObjectAnimator objectAnimatorOfObject = ObjectAnimator.ofObject(this.b, "rotation", new f90(0.0f), Float.valueOf(-0.3f), Float.valueOf(0.3f));
        objectAnimatorOfObject.setRepeatCount(-1);
        objectAnimatorOfObject.setInterpolator(new LinearInterpolator());
        objectAnimatorOfObject.setDuration(800L);
        Intrinsics.checkNotNullExpressionValue(objectAnimatorOfObject, "apply(...)");
        return objectAnimatorOfObject;
    }

    private final boolean i() {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr != null && ((bArr[1363] >> 5) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(null, this, 22110);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
            }
        }
        ValueAnimator valueAnimator = this.e;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            ValueAnimator valueAnimator2 = this.e;
            if (!(valueAnimator2 != null && valueAnimator2.isPaused())) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:35:0x006f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final void j(final boolean z) {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1366] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 22133).isSupported) {
            if (getContext() instanceof Activity) {
                Context context = getContext();
                Intrinsics.checkNotNull(context, "null cannot be cast to non-null type android.app.Activity");
                Activity activity = (Activity) context;
                if (activity.isFinishing() || activity.isDestroyed()) {
                    MLog.i("PlayerDiscAlbumPoleView", "startNewPoleSwitchAnimator return activity is finish");
                    return;
                }
            }
            float rotation = getRotation();
            float currentPlayAngle = z ? getCurrentPlayAngle() : 0.0f;
            ValueAnimator valueAnimator = this.e;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                ValueAnimator valueAnimator2 = this.e;
                if (valueAnimator2 != null) {
                    valueAnimator2.cancel();
                }
            } else {
                ValueAnimator valueAnimator3 = this.e;
                if (valueAnimator3 != null && valueAnimator3.isRunning()) {
                }
            }
            if (this.d == null) {
                this.d = getPoleSwingAnimator();
            }
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, "rotation", rotation, currentPlayAngle);
            objectAnimatorOfFloat.setDuration((long) (1000 * (Math.abs(rotation - currentPlayAngle) / 11.0f)));
            objectAnimatorOfFloat.setInterpolator(new CubicBezierInterpolator(0.4d, AbstractClickReport.DOUBLE_NULL, 0.45d, 1.0d));
            objectAnimatorOfFloat.addListener(new Animator.AnimatorListener() { // from class: com.tencent.qqmusic.ui.PlayerDiscAlbumPoleView$startNewPoleSwitchAnimator$1$1
                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animation) {
                    byte[] bArr2 = SwordSwitches.switches2;
                    if (bArr2 == null || ((bArr2[1160] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(animation, this, 20483).isSupported) {
                        Intrinsics.checkNotNullParameter(animation, "animation");
                    }
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    byte[] bArr2 = SwordSwitches.switches2;
                    if (bArr2 == null || ((bArr2[1159] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(animation, this, 20473).isSupported) {
                        Intrinsics.checkNotNullParameter(animation, "animation");
                    }
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation, boolean z2) {
                    byte[] bArr2 = SwordSwitches.switches2;
                    boolean z3 = false;
                    if (bArr2 == null || ((bArr2[1155] >> 1) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{animation, Boolean.valueOf(z2)}, this, 20442).isSupported) {
                        Intrinsics.checkNotNullParameter(animation, "animation");
                        super.onAnimationEnd(animation, z2);
                        PlayerDiscAlbumPoleView playerDiscAlbumPoleView = this.b;
                        boolean z4 = z;
                        playerDiscAlbumPoleView.l = (z4 && !z2) || (!z4 && z2);
                        if (this.b.l != this.b.m) {
                            PlayerDiscAlbumPoleView playerDiscAlbumPoleView2 = this.b;
                            playerDiscAlbumPoleView2.j(playerDiscAlbumPoleView2.m);
                            return;
                        }
                        if (this.b.l) {
                            this.b.o();
                            if (!GlobalLifeCycleManager.INSTANCE.isAppInForeground()) {
                                ObjectAnimator objectAnimator = this.b.d;
                                if (objectAnimator != null) {
                                    objectAnimator.end();
                                    return;
                                }
                                return;
                            }
                            ObjectAnimator objectAnimator2 = this.b.d;
                            if (objectAnimator2 != null && objectAnimator2.isPaused()) {
                                z3 = true;
                            }
                            if (z3) {
                                ObjectAnimator objectAnimator3 = this.b.d;
                                if (objectAnimator3 != null) {
                                    objectAnimator3.resume();
                                    return;
                                }
                                return;
                            }
                            ObjectAnimator objectAnimator4 = this.b.d;
                            if (objectAnimator4 != null) {
                                objectAnimator4.start();
                            }
                        }
                    }
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationRepeat(Animator animation) {
                    byte[] bArr2 = SwordSwitches.switches2;
                    if (bArr2 == null || ((bArr2[1160] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(animation, this, 20487).isSupported) {
                        Intrinsics.checkNotNullParameter(animation, "animation");
                    }
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator animation) {
                    byte[] bArr2 = SwordSwitches.switches2;
                    if (bArr2 == null || ((bArr2[1153] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(animation, this, 20427).isSupported) {
                        Intrinsics.checkNotNullParameter(animation, "animation");
                        this.b.l = false;
                        ObjectAnimator objectAnimator = this.b.d;
                        if (objectAnimator != null) {
                            objectAnimator.pause();
                        }
                    }
                }
            });
            if (com.tencent.qqmusic.i.j()) {
                objectAnimatorOfFloat.end();
            } else {
                objectAnimatorOfFloat.start();
            }
            this.e = objectAnimatorOfFloat;
        }
    }

    private final void l(int i, int i2) {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1354] >> 0) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}, this, 22033).isSupported) {
            float f = i * this.h;
            float f2 = i2 * this.i;
            if (getPivotX() == f) {
                if (getPivotY() == f2) {
                    return;
                }
            }
            this.b.setPivotX(f);
            this.b.setPivotY(f2);
            setPivotX(f);
            setPivotY(f2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void o() {
        byte[] bArr = SwordSwitches.switches2;
        if ((bArr == null || ((bArr[1360] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(null, this, 22086).isSupported) && this.l && !i()) {
            float rotation = getRotation();
            float currentPlayAngle = getCurrentPlayAngle();
            float fAbs = Math.abs(currentPlayAngle - rotation);
            if (fAbs > 1.0f) {
                j(true);
            } else if (fAbs > 0.0f) {
                setRotation(currentPlayAngle);
            }
        }
    }

    public final void g() {
        ValueAnimator valueAnimator;
        byte[] bArr = SwordSwitches.switches2;
        if ((bArr == null || ((bArr[1365] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(null, this, 22123).isSupported) && (valueAnimator = this.e) != null) {
            valueAnimator.pause();
        }
    }

    public final float getCurrentProgress() {
        return this.j;
    }

    @NotNull
    public final AsyncImageView getPoleImageView() {
        return this.b;
    }

    public final void h() {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1373] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(null, this, 22189).isSupported) {
            ObjectAnimator objectAnimator = this.d;
            if (objectAnimator != null) {
                objectAnimator.removeAllListeners();
            }
            ObjectAnimator objectAnimator2 = this.d;
            if (objectAnimator2 != null) {
                objectAnimator2.cancel();
            }
            this.d = null;
            ValueAnimator valueAnimator = this.e;
            if (valueAnimator != null) {
                valueAnimator.removeAllListeners();
            }
            ValueAnimator valueAnimator2 = this.e;
            if (valueAnimator2 != null) {
                valueAnimator2.cancel();
            }
            this.e = null;
            this.b.clearAnimation();
        }
    }

    public final void k(boolean z) {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1362] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 22101).isSupported) {
            this.m = z;
            if (i()) {
                return;
            }
            j(this.m);
        }
    }

    public final void m(float f, float f2) {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1357] >> 1) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f), Float.valueOf(f2)}, this, 22058).isSupported) {
            this.h = f;
            this.i = f2;
            requestLayout();
        }
    }

    public final void n() {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1360] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(null, this, 22083).isSupported) {
            o();
        }
    }

    @Override // android.widget.FrameLayout, android.view.View
    public void onMeasure(int i, int i2) {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1351] >> 7) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}, this, 22016).isSupported) {
            setMeasuredDimension(View.getDefaultSize(0, i), View.getDefaultSize(0, i2));
            int measuredWidth = getMeasuredWidth();
            int measuredHeight = getMeasuredHeight();
            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(measuredHeight, 1073741824);
            int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824);
            l(measuredWidth, measuredHeight);
            super.onMeasure(iMakeMeasureSpec2, iMakeMeasureSpec);
        }
    }

    public final void setAnglePole(float f, float f2) {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1358] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f), Float.valueOf(f2)}, this, 22069).isSupported) {
            if (f >= 0.0f && f2 >= 0.0f && f2 >= f) {
                this.f = f;
                this.g = f2 - f;
                return;
            }
            MLog.w("PlayerDiscAlbumPoleView", "setAnglePole error " + f + AbstractJsonLexerKt.COMMA + f2);
        }
    }

    public final void setImageResource(int i) {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1355] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 22047).isSupported) {
            this.b.setImageResource(i);
        }
    }

    public final void setProgress(@FloatRange(from = AbstractClickReport.DOUBLE_NULL, to = 1.0d) float f) {
        this.j = f;
    }

    @Override // android.view.View
    public void setRotation(float f) {
        byte[] bArr = SwordSwitches.switches2;
        if (bArr == null || ((bArr[1365] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 22128).isSupported) {
            super.setRotation(f);
        }
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public PlayerDiscAlbumPoleView(@NotNull Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        Intrinsics.checkNotNullParameter(context, "context");
        this.f = 11.0f;
        this.g = 10.0f;
        this.h = 0.57575756f;
        this.i = 0.22610015f;
        setClipChildren(false);
        AsyncImageView asyncImageView = new AsyncImageView(getContext());
        addView(asyncImageView, new FrameLayout.LayoutParams(-1, -1));
        this.b = asyncImageView;
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public PlayerDiscAlbumPoleView(@NotNull Context context, @Nullable AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        Intrinsics.checkNotNullParameter(context, "context");
        this.f = 11.0f;
        this.g = 10.0f;
        this.h = 0.57575756f;
        this.i = 0.22610015f;
        setClipChildren(false);
        AsyncImageView asyncImageView = new AsyncImageView(getContext());
        addView(asyncImageView, new FrameLayout.LayoutParams(-1, -1));
        this.b = asyncImageView;
    }
}


