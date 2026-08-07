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

    @NotNull
    private final TextView f;

    @NotNull
    private final TextView g;
    private boolean h;
    private int i;

    public enum ItemType {
        TYPE_LIST_SHUFFLE,
        TYPE_LIST_REPEAT,
        TYPE_ONESHOT_REPEAT,
        TYPE_ONESHOT_MULTI_REPEAT,
        TYPE_ONESHOT_COUNT_SET
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
            try {
                iArr[ItemType.TYPE_ONESHOT_MULTI_REPEAT.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                iArr[ItemType.TYPE_ONESHOT_COUNT_SET.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
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
        View viewFindViewById2 = viewInflate.findViewById(R.id.hsz);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById2, "findViewById(...)");
        TextView textView = (TextView) viewFindViewById2;
        this.f = textView;
        View viewFindViewById3 = viewInflate.findViewById(R.id.ht2);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById3, "findViewById(...)");
        TextView textView2 = (TextView) viewFindViewById3;
        this.g = textView2;
        this.i = 2;
        int i = ItemTypeSwitchMap.a[itemType.ordinal()];
        if (i == 1) {
            viewInflate.setContentDescription(context.getString(R.string.qq_theme_mode_light));
            imageView.setImageResource(R.drawable.popup_window_play_mode_shuffle);
            textView2.setText(R.string.qq_theme_mode_light);
        } else if (i == 2) {
            viewInflate.setContentDescription(context.getString(R.string.qq_theme_mode_dark));
            imageView.setImageResource(R.drawable.popup_window_play_mode_list_repeat);
            textView2.setText(R.string.qq_theme_mode_dark);
        } else if (i == 3) {
            viewInflate.setContentDescription(context.getString(R.string.qq_theme_mode_system));
            imageView.setImageResource(R.drawable.popup_window_play_mode_onshot_repeat);
            textView2.setText(R.string.qq_theme_mode_system);
        } else if (i == 4) {
            imageView.setImageResource(R.drawable.popup_window_play_mode_oneshot_multi_repeat);
            textView.setVisibility(0);
            i();
        } else {
            if (i != 5) {
                throw new NoWhenBranchMatchedException();
            }
            viewInflate.setContentDescription(context.getString(R.string.qq_mode_set_count));
            imageView.setImageResource(R.drawable.popup_window_play_mode_set_count);
            textView2.setText(R.string.qq_mode_set_count);
        }
        imageView.setContentDescription(null);
        textView2.setContentDescription(null);
        viewInflate.setOnClickListener(new a());
        k();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final int d() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr != null && ((bArr[772] >> 5) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg((Object) null, this, 84582);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Integer) swordProxyResultProxyOneArg.result).intValue();
            }
        }
        int i = ItemTypeSwitchMap.a[this.a.ordinal()];
        if (i == 1) {
            return 1;
        }
        if (i == 2) {
            return 2;
        }
        if (i == 3) {
            return 3;
        }
        if (i != 4) {
            return i != 5 ? 0 : 5;
        }
        return 4;
    }

    private final void i() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[774] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 84600).isSupported) {
            this.f.setText(String.valueOf(this.i));
            String strL = this.d.getContext().getString(R.string.qq_mode_multi_repeat, Integer.valueOf(this.i));
            this.d.setContentDescription(strL);
            this.g.setText(strL);
        }
    }

    private final void j() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[775] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 84607).isSupported) {
            TimeInterpolator cubicBezierInterpolator = new CubicBezierInterpolator(0.66f, 0.0f, 0.34f, 1.0f);
            float f = this.h ? 1.04f : 1.0f;
            this.e.animate().scaleX(f).scaleY(f).setDuration(150L).setInterpolator(cubicBezierInterpolator).start();
            this.g.animate().scaleX(f).scaleY(f).setDuration(150L).setInterpolator(cubicBezierInterpolator).start();
            if (this.a == ItemType.TYPE_ONESHOT_MULTI_REPEAT) {
                this.f.animate().scaleX(f).scaleY(f).setDuration(200L).setInterpolator(cubicBezierInterpolator).start();
            }
        }
    }

    private final void k() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[776] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 84614).isSupported) {
            int i = this.c ? R.color.white : R.color.black;
            this.e.setColorFilter(this.d.getContext().getColor(i), PorterDuff.Mode.SRC_IN);
            this.g.setTextColor(this.d.getContext().getColor(i));
            this.f.setTextColor(this.d.getContext().getColor(i));
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

    public final void g(int i) {
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[774] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 84597).isSupported) && this.a == ItemType.TYPE_ONESHOT_MULTI_REPEAT && this.i != i) {
            this.i = i;
            i();
        }
    }

    public final void h(boolean z) {
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[773] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 84590).isSupported) && this.h != z) {
            this.h = z;
            j();
        }
    }
}

