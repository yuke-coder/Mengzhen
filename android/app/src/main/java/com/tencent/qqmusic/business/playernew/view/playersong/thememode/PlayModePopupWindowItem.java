package com.tencent.qqmusic.business.playernew.view.playersong.thememode;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusic.ui.CubicBezierInterpolator;
import com.mengzhen.app.R;

import kotlin.NoWhenBranchMatchedException;


import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes18.dex */
public final class PlayModePopupWindowItem {

    @NotNull
    public static final b j = new b(null);

    @NotNull
    private final ItemType a;

    @NotNull
    private final c b;
    private boolean c;

    @NotNull
    private final View d;

    @NotNull
    private final ImageView e;

    // 真本这里还有个 f（R.id.hsz，单曲重复次数的计数角标）和 i（重复次数），
    // 只有 TYPE_ONESHOT_MULTI_REPEAT 那一行用得到 —— 梦枕已不装配该行，一并删掉。
    @NotNull
    private final TextView g;
    private boolean h;

    public enum ItemType {
        TYPE_LIST_SHUFFLE,
        TYPE_LIST_REPEAT,
        TYPE_ONESHOT_REPEAT
    }



















































    public final class a extends com.tencent.qqmusiccommon.util.l0 {
        a() {
            super(500L);
        }

        public void fastOnClick(View view) {
            byte[] bArr = SwordSwitches.switches8;
            if (bArr == null || ((bArr[778] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(view, this, 84627).isSupported) {

                PlayModePopupWindowItem.this.b.a(PlayModePopupWindowItem.this.a);
            }
        }
    }

    public static final class b {
        private b() {
        }

        public /* synthetic */ b(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    public interface c {
        void a(@NotNull ItemType itemType);
    }

    public static final class ItemTypeSwitchMap {
        public static final /* synthetic */ int[] a;

        static {
            int[] iArr = new int[ItemType.values().length];
            try {
                iArr[ItemType.TYPE_LIST_SHUFFLE.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                iArr[ItemType.TYPE_LIST_REPEAT.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                iArr[ItemType.TYPE_ONESHOT_REPEAT.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            a = iArr;
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: kotlin.NoWhenBranchMatchedException */
    public PlayModePopupWindowItem(@NotNull Context context, @NotNull ItemType itemType, @NotNull c cVar) throws NoWhenBranchMatchedException {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(itemType, "type");
        Intrinsics.checkNotNullParameter(cVar, "clickListener");
        this.a = itemType;
        this.b = cVar;
        View viewInflate = LayoutInflater.from(context).inflate(R.layout.qq_play_mode_popup_window_item, (ViewGroup) null);
        Intrinsics.checkNotNullExpressionValue(viewInflate, "inflate(...)");
        this.d = viewInflate;
        View viewFindViewById = viewInflate.findViewById(R.id.ht0);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
        ImageView imageView = (ImageView) viewFindViewById;
        this.e = imageView;
        View viewFindViewById3 = viewInflate.findViewById(R.id.ht2);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById3, "findViewById(...)");
        TextView textView2 = (TextView) viewFindViewById3;
        this.g = textView2;
        int i = ItemTypeSwitchMap.a[itemType.ordinal()];
        // 前三项在梦枕是「页面模式」（浅色 / 深色 / 自动），图标用 lucide 三态图标
        // （与 Web 端 theme-toggle.tsx 同源），不再借用真本的播歌顺序图标
        // —— 随机/顺序/单曲循环跟主题切换没有任何关系。
        if (i == 1) {
            viewInflate.setContentDescription(context.getString(R.string.qq_theme_mode_light));
            imageView.setImageResource(R.drawable.theme_mode_light);
            textView2.setText(R.string.qq_theme_mode_light);
        } else if (i == 2) {
            viewInflate.setContentDescription(context.getString(R.string.qq_theme_mode_dark));
            imageView.setImageResource(R.drawable.theme_mode_dark);
            textView2.setText(R.string.qq_theme_mode_dark);
        } else {
            if (i != 3) {
                throw new NoWhenBranchMatchedException();
            }
            viewInflate.setContentDescription(context.getString(R.string.qq_theme_mode_system));
            imageView.setImageResource(R.drawable.theme_mode_system);
            textView2.setText(R.string.qq_theme_mode_system);
        }
        imageView.setContentDescription(null);
        textView2.setContentDescription(null);
        viewInflate.setOnClickListener(new a());
        k();
    }

    // 真本的 d()（把 ItemType 映射成 1..5 的序号）和 i()（写「循环 N 次」角标）
    // 只服务于已删除的两行播歌顺序，全项目零调用，一并删掉。

    private final void j() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[775] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 84607).isSupported) {
            TimeInterpolator cubicBezierInterpolator = new CubicBezierInterpolator(0.66f, 0.0f, 0.34f, 1.0f);
            float f = this.h ? 1.04f : 1.0f;
            this.e.animate().scaleX(f).scaleY(f).setDuration(150L).setInterpolator(cubicBezierInterpolator).start();
            this.g.animate().scaleX(f).scaleY(f).setDuration(150L).setInterpolator(cubicBezierInterpolator).start();
        }
    }

    private final void k() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[776] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 84614).isSupported) {
            int i = this.c ? R.color.white : R.color.black;
            this.e.setColorFilter(this.d.getContext().getColor(i), PorterDuff.Mode.SRC_IN);
            this.g.setTextColor(this.d.getContext().getColor(i));
        }
    }

    @NotNull
    public final View e() {
        return this.d;
    }

    public final void f(boolean z) {
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[774] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 84595).isSupported) && this.c != z) {
            this.c = z;
            k();
            j();
        }
    }

    // 真本的 g(count) 是给「单曲重复 N 次」那行喂次数的，该行已删，随之删除。

    public final void h(boolean z) {
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[773] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 84590).isSupported) && this.h != z) {
            this.h = z;
            j();
        }
    }
}

