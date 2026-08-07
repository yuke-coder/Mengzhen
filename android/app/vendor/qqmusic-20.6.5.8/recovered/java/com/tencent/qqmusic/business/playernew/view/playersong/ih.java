package com.tencent.qqmusic.business.playernew.view.playersong;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.tencent.qqmusic.business.playernew.view.playersong.PlayModePopupWindowItem;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusic.ui.CubicBezierInterpolator;
import com.tencent.qqmusic.ui.PopupWindow;
import com.tencent.qqmusiccommon.appconfig.Resource;
import com.tencent.qqmusiccommon.statistics.trackpoint.ExposureStatistics;
import com.tencent.qqmusiccommon.util.MLog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.NoWhenBranchMatchedException;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes18.dex */
@SourceDebugExtension({"SMAP\nPlayModePopupWindow.kt\nKotlin\n*S Kotlin\n*F\n+ 1 PlayModePopupWindow.kt\ncom/tencent/qqmusic/business/playernew/view/playersong/PlayModePopupWindow\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,345:1\n1#2:346\n*E\n"})
public final class ih extends PopupWindow {

    @NotNull
    private static final LinearLayout.LayoutParams A;

    @NotNull
    private static final LinearLayout.LayoutParams B;

    @NotNull
    private static final LinearLayout.LayoutParams C;

    @NotNull
    private static final LinearLayout.LayoutParams D;

    @NotNull
    public static final a x = new a(null);
    private static final int y;
    private static final int z;

    @NotNull
    private final b b;
    private final View d;
    private final RelativeLayout e;
    private final LinearLayout f;
    private final FrameLayout g;

    @Nullable
    private PointF h;

    @NotNull
    private final AtomicBoolean i;

    @NotNull
    private final c j;

    @NotNull
    private final PlayModePopupWindowItem l;

    @NotNull
    private final PlayModePopupWindowItem m;

    @NotNull
    private final PlayModePopupWindowItem n;

    @NotNull
    private final PlayModePopupWindowItem o;

    @NotNull
    private final PlayModePopupWindowItem p;

    @NotNull
    private final View q;

    @NotNull
    private final ArrayList<PlayModePopupWindowItem> r;

    @NotNull
    private final View s;

    @Nullable
    private PlayModePopupWindowItem t;

    @Nullable
    private View u;

    @NotNull
    private com.tencent.qqmusicplayerprocess.audio.playlist.y v;
    private int w;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    public interface b {
        void a(int i);

        void b();

        void c();

        void d();

        void e(int i);
    }

    public static final class c implements PlayModePopupWindowItem.c {

        public /* synthetic */ class a {
            public static final /* synthetic */ int[] a;

            static {
                int[] iArr = new int[PlayModePopupWindowItem.ItemType.values().length];
                try {
                    iArr[PlayModePopupWindowItem.ItemType.TYPE_LIST_SHUFFLE.ordinal()] = 1;
                } catch (NoSuchFieldError unused) {
                }
                try {
                    iArr[PlayModePopupWindowItem.ItemType.TYPE_LIST_REPEAT.ordinal()] = 2;
                } catch (NoSuchFieldError unused2) {
                }
                try {
                    iArr[PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_REPEAT.ordinal()] = 3;
                } catch (NoSuchFieldError unused3) {
                }
                try {
                    iArr[PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_COUNT_SET.ordinal()] = 4;
                } catch (NoSuchFieldError unused4) {
                }
                try {
                    iArr[PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_MULTI_REPEAT.ordinal()] = 5;
                } catch (NoSuchFieldError unused5) {
                }
                a = iArr;
            }
        }

        c() {
        }

        /* JADX INFO: Thrown type has an unknown type hierarchy: kotlin.NoWhenBranchMatchedException */
        @Override // com.tencent.qqmusic.business.playernew.view.playersong.PlayModePopupWindowItem.c
        public void a(PlayModePopupWindowItem.ItemType itemType) throws NoWhenBranchMatchedException {
            byte[] bArr = SwordSwitches.switches8;
            if (bArr == null || ((bArr[1119] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(itemType, this, 87358).isSupported) {
                Intrinsics.checkNotNullParameter(itemType, "itemType");
                MLog.i("PlayModePopupWindow", "onItemClick itemType = " + itemType);
                int i = a.a[itemType.ordinal()];
                if (i == 1) {
                    ih.this.b.e(105);
                } else if (i == 2) {
                    ih.this.b.e(103);
                } else if (i == 3) {
                    ih.this.b.e(101);
                } else if (i == 4) {
                    ih.this.b.d();
                } else {
                    if (i != 5) {
                        throw new NoWhenBranchMatchedException();
                    }
                    ih.this.b.a(ih.this.v.a());
                }
                ih.this.dismiss();
            }
        }
    }

    static {
        int iB = com.tencent.qqmusiccommon.util.j0.b(107.0f);
        y = iB;
        int iB2 = com.tencent.qqmusiccommon.util.j0.b(40.0f);
        z = iB2;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(iB, iB2);
        layoutParams.leftMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        layoutParams.topMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        layoutParams.rightMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        A = layoutParams;
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(iB, iB2);
        layoutParams2.leftMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        layoutParams2.topMargin = com.tencent.qqmusiccommon.util.j0.b(3.0f);
        layoutParams2.rightMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        B = layoutParams2;
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(iB, iB2);
        layoutParams3.leftMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        layoutParams3.topMargin = com.tencent.qqmusiccommon.util.j0.b(3.0f);
        layoutParams3.rightMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        layoutParams3.bottomMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        C = layoutParams3;
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(iB, iB2);
        layoutParams4.leftMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        layoutParams4.rightMargin = com.tencent.qqmusiccommon.util.j0.b(4.0f);
        D = layoutParams4;
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    /* JADX WARN: Multi-variable type inference failed */
    public ih(@NotNull Context context, @NotNull b bVar) {
        super(context);
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(bVar, "itemClickListener");
        this.b = bVar;
        View viewInflate = LayoutInflater.from(context).inflate(2131495177, (ViewGroup) null);
        this.d = viewInflate;
        this.e = (RelativeLayout) viewInflate.findViewById(2131312873);
        this.f = (LinearLayout) viewInflate.findViewById(2131310796);
        FrameLayout frameLayout = (FrameLayout) viewInflate.findViewById(2131318586);
        this.g = frameLayout;
        this.i = new AtomicBoolean(false);
        c cVar = new c();
        this.j = cVar;
        PlayModePopupWindowItem playModePopupWindowItem = new PlayModePopupWindowItem(context, PlayModePopupWindowItem.ItemType.TYPE_LIST_REPEAT, cVar);
        playModePopupWindowItem.h(true);
        this.l = playModePopupWindowItem;
        this.m = new PlayModePopupWindowItem(context, PlayModePopupWindowItem.ItemType.TYPE_LIST_SHUFFLE, cVar);
        this.n = new PlayModePopupWindowItem(context, PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_REPEAT, cVar);
        this.o = new PlayModePopupWindowItem(context, PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_MULTI_REPEAT, cVar);
        this.p = new PlayModePopupWindowItem(context, PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_COUNT_SET, cVar);
        this.r = new ArrayList<>();
        View view = new View(context);
        view.setLayoutParams(new FrameLayout.LayoutParams(y, z));
        view.setBackground(Resource.g(2131232040));
        this.s = view;
        this.v = new com.tencent.qqmusicplayerprocess.audio.playlist.y(0, 0, 3, (DefaultConstructorMarker) null);
        this.w = 103;
        setContentView(viewInflate);
        setOutsideTouchable(true);
        setFocusable(true);
        setWidth(-2);
        setHeight(-2);
        View view2 = new View(context);
        view2.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
        view2.setBackgroundColor(Resource.getColor(2131099873));
        this.q = view2;
        frameLayout.setOnClickListener(new View.OnClickListener() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.dh
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                ih.k(this.b, view3);
            }
        });
        frameLayout.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.eh
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(View view3) {
                return ih.l(this.b, view3);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void k(ih ihVar, View view) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[17] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{ihVar, view}, (Object) null, 89741).isSupported) {
            Intrinsics.checkNotNullParameter(ihVar, "this$0");
            ihVar.b.c();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean l(ih ihVar, View view) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr != null && ((bArr[19] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{ihVar, view}, (Object) null, 89754);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Boolean) swordProxyResultProxyMoreArgs.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(ihVar, "this$0");
        ihVar.b.b();
        return true;
    }

    private final void o(PlayModePopupWindowItem playModePopupWindowItem) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[11] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(playModePopupWindowItem, this, 89689).isSupported) {
            if (this.s.getParent() == null) {
                this.e.addView(this.s);
            }
            PointF pointF = this.h;
            if (pointF != null) {
                this.s.setX(pointF.x);
                this.s.setY(pointF.y);
            }
            TimeInterpolator cubicBezierInterpolator = new CubicBezierInterpolator(0.66f, 0.0f, 0.34f, 1.0f);
            View viewE = playModePopupWindowItem.e();
            this.s.animate().x(viewE.getX()).y(viewE.getY()).setDuration(150L).setInterpolator(cubicBezierInterpolator).withEndAction(new Runnable() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.gh
                @Override // java.lang.Runnable
                public final void run() {
                    ih.p(this.b);
                }
            }).start();
            this.h = new PointF(viewE.getX(), viewE.getY());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void p(ih ihVar) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[20] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(ihVar, (Object) null, 89763).isSupported) {
            Intrinsics.checkNotNullParameter(ihVar, "this$0");
            ihVar.h = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    public static final void q(ih ihVar) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[22] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(ihVar, (Object) null, 89784).isSupported) {
            Intrinsics.checkNotNullParameter(ihVar, "this$0");
            super/*android.widget.PopupWindow*/.dismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void v(GradientDrawable gradientDrawable, ih ihVar, ValueAnimator valueAnimator) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[21] >> 6) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{gradientDrawable, ihVar, valueAnimator}, (Object) null, 89775).isSupported) {
            Intrinsics.checkNotNullParameter(gradientDrawable, "$background");
            Intrinsics.checkNotNullParameter(ihVar, "this$0");
            Intrinsics.checkNotNullParameter(valueAnimator, "animation");
            Object animatedValue = valueAnimator.getAnimatedValue();
            Intrinsics.checkNotNull(animatedValue, "null cannot be cast to non-null type kotlin.Float");
            gradientDrawable.setCornerRadius(((Float) animatedValue).floatValue());
            ihVar.f.setBackground(gradientDrawable);
        }
    }

    private final void w() {
        byte[] bArr = SwordSwitches.switches9;
        PlayModePopupWindowItem playModePopupWindowItem = null;
        if (bArr == null || ((bArr[9] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 89674).isSupported) {
            PlayModePopupWindowItem.ItemType itemType = PlayModePopupWindowItem.ItemType.TYPE_LIST_REPEAT;
            PlayModePopupWindowItem.ItemType itemType2 = this.v.b() != -1 ? PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_MULTI_REPEAT : com.tencent.qqmusiccommon.util.music.i.a.d(this.w) ? PlayModePopupWindowItem.ItemType.TYPE_LIST_SHUFFLE : this.w == 101 ? PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_REPEAT : itemType;
            boolean z2 = itemType2 == PlayModePopupWindowItem.ItemType.TYPE_LIST_SHUFFLE;
            boolean z3 = itemType2 == itemType;
            boolean z4 = itemType2 == PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_REPEAT;
            boolean z5 = itemType2 == PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_MULTI_REPEAT;
            this.m.h(z2);
            this.l.h(z3);
            this.n.h(z4);
            this.o.h(z5);
            if (z2) {
                playModePopupWindowItem = this.m;
            } else if (z3) {
                playModePopupWindowItem = this.l;
            } else if (z4) {
                playModePopupWindowItem = this.n;
            } else if (z5) {
                playModePopupWindowItem = this.o;
            }
            if (playModePopupWindowItem == null || Intrinsics.areEqual(playModePopupWindowItem, this.t)) {
                return;
            }
            MLog.i("PlayModePopupWindow", "animateSelectionIndicator: " + playModePopupWindowItem);
            o(playModePopupWindowItem);
            this.t = playModePopupWindowItem;
        }
    }

    public void dismiss() {
        byte[] bArr = SwordSwitches.switches9;
        if ((bArr == null || ((bArr[15] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 89724).isSupported) && this.i.compareAndSet(true, false)) {
            this.d.animate().scaleX(0.0f).scaleY(0.0f).alpha(0.0f).setDuration(300L).setInterpolator(new CubicBezierInterpolator(0.52f, 0.0f, 0.74f, 0.0f)).withEndAction(new Runnable() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.hh
                @Override // java.lang.Runnable
                public final void run() {
                    ih.q(this.b);
                }
            }).start();
        }
    }

    public final void r(int i) {
        byte[] bArr = SwordSwitches.switches9;
        if ((bArr == null || ((bArr[7] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 89658).isSupported) && this.w != i) {
            this.w = i;
            w();
        }
    }

    public final void s(@NotNull com.tencent.qqmusicplayerprocess.audio.playlist.y yVar) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[8] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(yVar, this, 89665).isSupported) {
            Intrinsics.checkNotNullParameter(yVar, "info");
            if (Intrinsics.areEqual(this.v, yVar)) {
                return;
            }
            this.v = yVar;
            w();
            this.o.g(yVar.a());
        }
    }

    public final void t(boolean z2) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[3] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z2), this, 89632).isSupported) {
            MLog.d("PlayModePopupWindow", "setUseBlackTheme: " + z2);
            Drawable drawableG = Resource.g(2131232039);
            Intrinsics.checkNotNull(drawableG, "null cannot be cast to non-null type android.graphics.drawable.GradientDrawable");
            GradientDrawable gradientDrawable = (GradientDrawable) drawableG;
            gradientDrawable.setColor(Resource.getColor(z2 ? 2131099806 : 2131100270));
            this.f.setBackground(gradientDrawable);
            this.l.f(z2);
            this.m.f(z2);
            this.n.f(z2);
            this.o.f(z2);
            this.p.f(z2);
            Drawable drawableG2 = Resource.g(2131232040);
            Intrinsics.checkNotNull(drawableG2, "null cannot be cast to non-null type android.graphics.drawable.GradientDrawable");
            GradientDrawable gradientDrawable2 = (GradientDrawable) drawableG2;
            gradientDrawable2.setColor(Resource.getColor(z2 ? 2131102028 : 2131099885));
            this.s.setBackground(gradientDrawable2);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final void u(@NotNull View view, boolean z2) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[12] >> 7) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{view, Boolean.valueOf(z2)}, this, 89704).isSupported) {
            Intrinsics.checkNotNullParameter(view, "anchor");
            this.u = view;
            Iterator<PlayModePopupWindowItem> it = this.r.iterator();
            Intrinsics.checkNotNullExpressionValue(it, "iterator(...)");
            while (it.hasNext()) {
                PlayModePopupWindowItem next = it.next();
                Intrinsics.checkNotNullExpressionValue(next, "next(...)");
                this.f.removeView(next.e());
            }
            this.r.clear();
            this.f.removeView(this.q);
            this.f.removeView(this.p.e());
            if (com.tencent.qqmusiccommon.util.music.f.f()) {
                this.r.add(this.m);
                this.r.add(this.l);
                this.f.addView(this.m.e(), A);
                this.f.addView(this.l.e(), B);
            } else {
                this.r.add(this.l);
                this.f.addView(this.l.e(), A);
            }
            if (this.v.a() != -1) {
                this.r.add(this.o);
                this.f.addView(this.o.e(), C);
            } else {
                this.r.add(this.n);
                this.f.addView(this.n.e(), C);
            }
            if (z2) {
                this.f.addView(this.q);
                this.f.addView(this.p.e(), D);
            }
            this.d.measure(0, 0);
            Drawable background = this.f.getBackground();
            Intrinsics.checkNotNull(background, "null cannot be cast to non-null type android.graphics.drawable.GradientDrawable");
            final GradientDrawable gradientDrawable = (GradientDrawable) background;
            float measuredHeight = this.d.getMeasuredHeight() / 2;
            gradientDrawable.setCornerRadius(measuredHeight);
            this.d.setScaleX(0.0f);
            this.d.setScaleY(0.0f);
            this.d.setAlpha(0.0f);
            this.d.setPivotX(com.tencent.qqmusiccommon.util.j0.b(27.0f));
            this.d.setPivotY(r4.getMeasuredHeight() - com.tencent.qqmusiccommon.util.j0.b(20.0f));
            showAsDropDown(view, -com.tencent.qqmusiccommon.util.j0.b(7.0f), -this.d.getMeasuredHeight());
            TimeInterpolator cubicBezierInterpolator = new CubicBezierInterpolator(0.84f, 0.0f, 0.16f, 1.0f);
            this.d.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(300L).setInterpolator(cubicBezierInterpolator).start();
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(measuredHeight, com.tencent.qqmusiccommon.util.j0.b(6.0f));
            valueAnimatorOfFloat.setDuration(300L);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.fh
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    ih.v(gradientDrawable, this, valueAnimator);
                }
            });
            valueAnimatorOfFloat.setInterpolator(cubicBezierInterpolator);
            valueAnimatorOfFloat.start();
            this.i.set(true);
            new ExposureStatistics(5022014);
        }
    }
}

