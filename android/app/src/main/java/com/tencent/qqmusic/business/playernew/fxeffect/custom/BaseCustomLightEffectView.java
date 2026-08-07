package com.tencent.qqmusic.business.playernew.fxeffect.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.ColorInt;
import com.tencent.qqmusic.ui.minibar.v;
import com.tencent.qqmusicplayerprocess.songinfo.SongInfo;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.JvmOverloads;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes4.dex */
public abstract class BaseCustomLightEffectView extends FrameLayout {
    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    @JvmOverloads
    public BaseCustomLightEffectView(@NotNull Context context) {
        this(context, null, 0, 0, 14, null);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    @JvmOverloads
    public BaseCustomLightEffectView(@NotNull Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0, 12, null);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    @JvmOverloads
    public BaseCustomLightEffectView(@NotNull Context context, @Nullable AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0, 8, null);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    public /* synthetic */ BaseCustomLightEffectView(Context context, AttributeSet attributeSet, int i, int i2, int i3, DefaultConstructorMarker defaultConstructorMarker) {
        this(context, (i3 & 2) != 0 ? null : attributeSet, (i3 & 4) != 0 ? 0 : i, (i3 & 8) != 0 ? 0 : i2);
    }

    public static /* synthetic */ void e(BaseCustomLightEffectView baseCustomLightEffectView, boolean z, int i, Object obj) {
        if (obj != null) {
            throw new UnsupportedOperationException("Super calls with default arguments not supported in this target, function: stop");
        }
        if ((i & 1) != 0) {
            z = true;
        }
        baseCustomLightEffectView.d(z);
    }

    public static /* synthetic */ void setFillBgColor$default(BaseCustomLightEffectView baseCustomLightEffectView, int i, boolean z, int i2, Object obj) {
        if (obj != null) {
            throw new UnsupportedOperationException("Super calls with default arguments not supported in this target, function: setFillBgColor");
        }
        if ((i2 & 2) != 0) {
            z = true;
        }
        baseCustomLightEffectView.setFillBgColor(i, z);
    }

    public abstract void a(@Nullable SongInfo songInfo, boolean z, @Nullable Function1<? super Pair<Integer, Integer>, Unit> function1);

    public abstract void b();

    public abstract void c();

    public abstract void d(boolean z);

    public abstract void setEffectShader(@NotNull com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar);

    public abstract void setFillBgColor(@ColorInt int i, boolean z);

    public abstract void setMagicColorGenerator(@Nullable Function1<? super Bitmap, Pair<Integer, Integer>> function1);

    public abstract void setPicLoadListener(@Nullable v vVar);

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    @JvmOverloads
    public BaseCustomLightEffectView(@NotNull Context context, @Nullable AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        Intrinsics.checkNotNullParameter(context, "context");
    }
}
