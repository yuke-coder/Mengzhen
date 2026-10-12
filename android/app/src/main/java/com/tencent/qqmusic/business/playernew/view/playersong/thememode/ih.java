package com.tencent.qqmusic.business.playernew.view.playersong.thememode;

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
import com.tencent.qqmusic.business.playernew.view.playersong.thememode.PlayModePopupWindowItem;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusic.ui.CubicBezierInterpolator;
import com.tencent.qqmusic.ui.PopupWindow;
import com.mengzhen.app.R;


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

    // 真本的 D 是第四行（「设置次数」）的 LayoutParams，该行已删，随之删除。

    @NotNull
    public static final a x = new a(null);
    private static final int y;
    private static final int z;

    @NotNull
    private final b b;
    private final View d;
    private final RelativeLayout e;
    private final LinearLayout f;

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

    // 真本还有 o（TYPE_ONESHOT_MULTI_REPEAT）和 p（TYPE_ONESHOT_COUNT_SET）两行，
    // 服务的是「单曲重复 N 次 / 设置次数」。梦枕只装配三种页面模式，这两行从来不 addView，
    // 白白 inflate 两遍布局，且会拉进 QQ 的播歌顺序图标 —— 字段与构造一起删。

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

    private int w;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    // 真本这个接口有五个回调。梦枕只有三种页面模式，真正会触发的只有 e(int)：
    // a(count) / d() 属于已删除的「单曲重复次数 / 设置次数」两行，
    // b() / c() 属于已随布局删掉的底部点击长按区。留着就是没人实现的空契约。
    public interface b {
        void e(int i);
    }

    public final class c implements PlayModePopupWindowItem.c {

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

                int i = a.a[itemType.ordinal()];
                if (i == 1) {
                    ih.this.b.e(105);
                } else if (i == 2) {
                    ih.this.b.e(103);
                } else {
                    if (i != 3) {
                        throw new NoWhenBranchMatchedException();
                    }
                    ih.this.b.e(101);
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
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    /* JADX WARN: Multi-variable type inference failed */
    public ih(@NotNull Context context, @NotNull b bVar) {
        super(context);
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(bVar, "itemClickListener");
        this.b = bVar;
        View viewInflate = LayoutInflater.from(context).inflate(R.layout.qq_play_mode_popup_window, (ViewGroup) null);
        this.d = viewInflate;
        this.e = (RelativeLayout) viewInflate.findViewById(R.id.jb3);
        this.f = (LinearLayout) viewInflate.findViewById(R.id.ht1);
        // 真本这里还有个 nh9 的 FrameLayout（40x40dp），是「播放次数设置」的点击/长按区，
        // 触发 b.c() / b.b() 两个回调 —— 梦枕只有三种页面模式，这两个回调没有任何实现，
        // 留着就是一块点了没反应的死区域，所以连布局一起去掉了（见 layout 注释）。
        this.i = new AtomicBoolean(false);
        c cVar = new c();
        this.j = cVar;
        PlayModePopupWindowItem playModePopupWindowItem = new PlayModePopupWindowItem(context, PlayModePopupWindowItem.ItemType.TYPE_LIST_REPEAT, cVar);
        playModePopupWindowItem.h(true);
        this.l = playModePopupWindowItem;
        this.m = new PlayModePopupWindowItem(context, PlayModePopupWindowItem.ItemType.TYPE_LIST_SHUFFLE, cVar);
        this.n = new PlayModePopupWindowItem(context, PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_REPEAT, cVar);
        this.r = new ArrayList<>();
        View view = new View(context);
        view.setLayoutParams(new FrameLayout.LayoutParams(y, z));
        view.setBackground(context.getDrawable(R.drawable.bg_play_mode_popup_window_item));
        this.s = view;
        // 真本这里还会 new 一个 playlist.y(-1,-1) 存进 v，作为「单曲重复次数」的来源
        // （真本写 y(0,0,3,null) 是 Kotlin 合成构造器，展开后等价于 y(-1,-1)）。
        // 那一行已不装配，v 连同喂它的 s(y) 一并删除。
        this.w = 103;
        setContentView(viewInflate);
        setOutsideTouchable(true);
        setFocusable(true);
        setWidth(-2);
        setHeight(-2);
        View view2 = new View(context);
        view2.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
        view2.setBackgroundColor(context.getColor(R.color.black_10_opacity));
        this.q = view2;
    }

    private final void o(PlayModePopupWindowItem playModePopupWindowItem) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[11] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(playModePopupWindowItem, this, 89689).isSupported) {
            View viewE = playModePopupWindowItem.e();
            if (this.s.getParent() == null) {
                this.e.addView(this.s);
                // 指示块刚挂上去时停在 (0,0)。先无动画直接落到目标行 ——
                // 否则每次开弹窗都能看到高亮块从左上角「飞」到选中行，看着就是错位。
                this.s.setX(viewE.getX());
                this.s.setY(viewE.getY());
                this.h = new PointF(viewE.getX(), viewE.getY());
                return;
            }
            PointF pointF = this.h;
            if (pointF != null) {
                this.s.setX(pointF.x);
                this.s.setY(pointF.y);
            }
            TimeInterpolator cubicBezierInterpolator = new CubicBezierInterpolator(0.66f, 0.0f, 0.34f, 1.0f);
            this.s.animate().x(viewE.getX()).y(viewE.getY()).setDuration(150L).setInterpolator(cubicBezierInterpolator).withEndAction(new Runnable() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.gh
                @Override // java.lang.Runnable
                public final void run() {
                    ih.p(ih.this);
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
    private final void dismissImmediately() {
        super.dismiss();
    }

    public static final void q(ih ihVar) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[22] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(ihVar, (Object) null, 89784).isSupported) {
            Intrinsics.checkNotNullParameter(ihVar, "this$0");
            ihVar.dismissImmediately();
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
            // 真本这行前面还有一截 `v.b() != -1 → TYPE_ONESHOT_MULTI_REPEAT`：
            // v 恒为 y(-1,-1)、b() 恒返回 -1，且喂它的 s() 已删，那一支永远走不到，去掉。
            PlayModePopupWindowItem.ItemType itemType2 = this.w == 105 ? PlayModePopupWindowItem.ItemType.TYPE_LIST_SHUFFLE : this.w == 101 ? PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_REPEAT : itemType;
            boolean z2 = itemType2 == PlayModePopupWindowItem.ItemType.TYPE_LIST_SHUFFLE;
            boolean z3 = itemType2 == itemType;
            boolean z4 = itemType2 == PlayModePopupWindowItem.ItemType.TYPE_ONESHOT_REPEAT;
            this.m.h(z2);
            this.l.h(z3);
            this.n.h(z4);
            if (z2) {
                playModePopupWindowItem = this.m;
            } else if (z3) {
                playModePopupWindowItem = this.l;
            } else if (z4) {
                playModePopupWindowItem = this.n;
            }
            if (playModePopupWindowItem == null) {
                return;
            }
            // 行要等 u() 里才 addView。调用侧是「先 r(mode) 后 u(anchor)」，
            // r() 触发的这次 w() 拿到的 getX()/getY() 全是 0 —— 一旦在这里定位，
            // 指示块就被扔到左上角，还会把 t 记成「已定位」，
            // 于是 u() 里 post 的那次真正刷新会被下面的 areEqual 短路掉。
            // 表现就是「选中的那行没有高亮 / 高亮乱跳」。行没挂上去就先不定位。
            if (playModePopupWindowItem.e().getParent() == null) {
                return;
            }
            if (Intrinsics.areEqual(playModePopupWindowItem, this.t)) {
                return;
            }

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
                    ih.q(ih.this);
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

    // 真本的 s(y) 是给「单曲重复次数」那行喂数据的（顺带触发 w() 重排），
    // 该行已不装配，且调用侧本来就没人调它 —— 连 v 字段一起删。

    public final void t(boolean z2) {
        byte[] bArr = SwordSwitches.switches9;
        if (bArr == null || ((bArr[3] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z2), this, 89632).isSupported) {

            Drawable drawableG = this.d.getContext().getDrawable(R.drawable.bg_play_mode_popup_window);
            Intrinsics.checkNotNull(drawableG, "null cannot be cast to non-null type android.graphics.drawable.GradientDrawable");
            GradientDrawable gradientDrawable = (GradientDrawable) drawableG;
            gradientDrawable.setColor(this.d.getContext().getColor(z2 ? R.color.as_background_dark : R.color.ds_background_light));
            this.f.setBackground(gradientDrawable);
            this.l.f(z2);
            this.m.f(z2);
            this.n.f(z2);
            Drawable drawableG2 = this.d.getContext().getDrawable(R.drawable.bg_play_mode_popup_window_item);
            Intrinsics.checkNotNull(drawableG2, "null cannot be cast to non-null type android.graphics.drawable.GradientDrawable");
            GradientDrawable gradientDrawable2 = (GradientDrawable) drawableG2;
            gradientDrawable2.setColor(this.d.getContext().getColor(z2 ? R.color.white_5_transparent : R.color.black_5_transparent));
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
            // 梦枕是「页面模式」三选一，固定三行：浅色 / 深色 / 自动。
            //
            // 真本这里按 f.f()（随机播放开关）和 v.a()（单曲重复次数）动态决定加哪几行：
            // f.f() 在梦枕恒 false → 少一行；v.a() 又挑中 TYPE_ONESHOT_MULTI_REPEAT
            // （回调是 a(count)，梦枕空实现）→ 于是只剩两行且第二行点了没反应。
            // 那套分支服务的是播歌顺序，跟主题切换无关，梦枕直接钉死三行。
            this.r.add(this.m);
            this.f.addView(this.m.e(), A);
            this.r.add(this.l);
            this.f.addView(this.l.e(), B);
            this.r.add(this.n);
            this.f.addView(this.n.e(), C);
            // z2（真本的「设置次数」入口）与 p 在梦枕不存在，不装配 —— 见构造函数注释。
            this.d.measure(0, 0);
            Drawable background = this.f.getBackground();
            Intrinsics.checkNotNull(background, "null cannot be cast to non-null type android.graphics.drawable.GradientDrawable");
            final GradientDrawable gradientDrawable = (GradientDrawable) background;
            float measuredHeight = this.d.getMeasuredHeight() / 2;
            gradientDrawable.setCornerRadius(measuredHeight);
            this.d.setScaleX(0.0f);
            this.d.setScaleY(0.0f);
            this.d.setAlpha(0.0f);
            // 缩放动画的锚点：真本固定 pivotX=27dp（偏左），因为 QQ 那个按钮在屏幕**左下角**，
            // 弹窗要贴着按钮的左侧长出来。梦枕的触发图标在**右上角**，锚点必须跟着翻到右侧，
            // 否则动画是从左边「甩」出来的，看着像别处的弹窗飘过来。
            this.d.setPivotX(this.d.getMeasuredWidth() - com.tencent.qqmusiccommon.util.j0.b(27.0f));
            int[] anchorLocation = new int[2];
            view.getLocationOnScreen(anchorLocation);
            boolean opensAbove = anchorLocation[1] >= this.d.getMeasuredHeight();
            this.d.setPivotY(opensAbove
                    ? this.d.getMeasuredHeight() - com.tencent.qqmusiccommon.util.j0.b(20.0f)
                    : com.tencent.qqmusiccommon.util.j0.b(20.0f));
            // 水平方向右对齐：showAsDropDown 的 xoff 是相对锚点左边缘的偏移，
            // 要让弹窗右边缘对齐锚点右边缘，偏移就是「锚点宽 - 弹窗宽」。
            // 真本那个 -7dp 是给左下角按钮做微调的，跟右上角对不上。
            int xoff = view.getWidth() - this.d.getMeasuredWidth();
            showAsDropDown(view, xoff, opensAbove ? -this.d.getMeasuredHeight() : 0);
            TimeInterpolator cubicBezierInterpolator = new CubicBezierInterpolator(0.84f, 0.0f, 0.16f, 1.0f);
            this.d.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(300L).setInterpolator(cubicBezierInterpolator).start();
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(measuredHeight, com.tencent.qqmusiccommon.util.j0.b(6.0f));
            valueAnimatorOfFloat.setDuration(300L);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.fh
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    ih.v(gradientDrawable, ih.this, valueAnimator);
                }
            });
            valueAnimatorOfFloat.setInterpolator(cubicBezierInterpolator);
            valueAnimatorOfFloat.start();
            this.i.set(true);
            // 强制放一次选中指示块。真本靠 r() / s() 触发 w()，但那两处都有
            // 「值变了才执行」的短路（r() 是 w != n，s() 是 v 不等）。梦枕每次打开都是新建弹窗，
            // 若当前模式恰好等于内部默认值 103（深色），r() 就被短路掉，w() 永不执行，
            // 指示块根本不会 addView —— 表现就是「选中的那行没有高亮块」。
            // 放在 show 之后 post，是因为 w() 要读行 View 的 x/y，得等 layout 完成。
            this.d.post(new Runnable() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.thememode.PageModeIndicatorRefresh
                @Override // java.lang.Runnable
                public final void run() {
                    w();
                }
            });

        }
    }
}

