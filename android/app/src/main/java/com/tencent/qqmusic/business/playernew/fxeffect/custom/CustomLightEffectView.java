package com.tencent.qqmusic.business.playernew.fxeffect.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import androidx.annotation.ColorInt;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusic.ui.minibar.v;
import android.util.Log;
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
public final class CustomLightEffectView extends BaseCustomLightEffectView {

    @NotNull
    public static final a h = new a(null);

    @Nullable
    private CustomLightEffectSurfaceView b;

    @Nullable
    private SongInfo d;
    private int e;
    private boolean f;
    private boolean g;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    @JvmOverloads
    public CustomLightEffectView(@NotNull Context context) {
        this(context, null, 0, 0, 14, null);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    @JvmOverloads
    public CustomLightEffectView(@NotNull Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0, 12, null);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    /* JADX WARN: 'this' call moved to the top of the method (can break code semantics) */
    @JvmOverloads
    public CustomLightEffectView(@NotNull Context context, @Nullable AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0, 8, null);
        Intrinsics.checkNotNullParameter(context, "context");
    }

    public /* synthetic */ CustomLightEffectView(Context context, AttributeSet attributeSet, int i, int i2, int i3, DefaultConstructorMarker defaultConstructorMarker) {
        this(context, (i3 & 2) != 0 ? null : attributeSet, (i3 & 4) != 0 ? 0 : i, (i3 & 8) != 0 ? 0 : i2);
    }

    private final void f() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1340] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66725).isSupported) {
            this.b = new CustomLightEffectSurfaceView(getContext());
            addView(this.b, new FrameLayout.LayoutParams(-1, -1));
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.BaseCustomLightEffectView
    public void a(@Nullable SongInfo songInfo, boolean z, @Nullable Function1<? super Pair<Integer, Integer>, Unit> function1) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1348] >> 3) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{songInfo, Boolean.valueOf(z), function1}, this, 66788).isSupported) {
            Log.i("CustomLightEffectView", "[loadMagicColor] songInfo: " + songInfo);
            SongInfo songInfo2 = this.d;
            if (songInfo2 != null && Intrinsics.areEqual(songInfo2, songInfo) && !z) {
                Log.i("CustomLightEffectView", "[loadMagicColor] same songInfo， return");
                return;
            }
            this.d = songInfo;
            CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
            if (customLightEffectSurfaceView != null) {
                customLightEffectSurfaceView.q(songInfo, function1);
            }
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.BaseCustomLightEffectView
    public void b() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1356] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66850).isSupported) {
            this.f = false;
            this.g = false;
            CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
            if (customLightEffectSurfaceView != null) {
                customLightEffectSurfaceView.j();
            }
            ViewParent parent = getParent();
            ViewGroup viewGroup = parent instanceof ViewGroup ? (ViewGroup) parent : null;
            if (viewGroup != null) {
                viewGroup.removeView(this);
            }
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.BaseCustomLightEffectView
    public void c() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1353] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66825).isSupported) {
            Log.i("CustomLightEffectView", "[start] isAttachedToWindow=" + isAttachedToWindow() + ", mLightEffectSurfaceView=" + this.b);
            if (this.b == null) {
                Log.w("CustomLightEffectView", "[start] mLightEffectSurfaceView is null, return");
                return;
            }
            if (!isAttachedToWindow()) {
                Log.i("CustomLightEffectView", "[start] View not attached yet, pending start");
                this.f = true;
                this.g = false;
            } else {
                this.f = false;
                Log.i("CustomLightEffectView", "[start] calling mLightEffectSurfaceView?.start()");
                CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
                if (customLightEffectSurfaceView != null) {
                    customLightEffectSurfaceView.t();
                }
            }
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.BaseCustomLightEffectView
    public void d(boolean z) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1355] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 66841).isSupported) {
            Log.i("CustomLightEffectView", "[stop] stopImmediately=" + z + ", isAttachedToWindow=" + isAttachedToWindow());
            if (!isAttachedToWindow()) {
                Log.i("CustomLightEffectView", "[stop] View not attached yet, pending stop");
                this.g = true;
                this.f = false;
            } else {
                this.g = false;
                CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
                if (customLightEffectSurfaceView != null) {
                    customLightEffectSurfaceView.u(z);
                }
            }
        }
    }

    public void g(@NotNull Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1351] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(pair, this, 66813).isSupported) {
            Intrinsics.checkNotNullParameter(pair, "magicColorPair");
            Log.i("CustomLightEffectView", "[loadMagicColor] color pair: (" + Integer.toHexString(((Number) pair.getFirst()).intValue()) + ", " + Integer.toHexString(((Number) pair.getSecond()).intValue()) + ')');
            CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
            if (customLightEffectSurfaceView != null) {
                customLightEffectSurfaceView.r(pair);
            }
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onAttachedToWindow() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1357] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66860).isSupported) {
            super.onAttachedToWindow();
            Log.i("CustomLightEffectView", "[onAttachedToWindow] pendingStart=" + this.f + ", pendingStop=" + this.g);
            if (this.f) {
                this.f = false;
                Log.i("CustomLightEffectView", "[onAttachedToWindow] executing pending start");
                CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
                if (customLightEffectSurfaceView != null) {
                    customLightEffectSurfaceView.t();
                }
            }
            if (this.g) {
                this.g = false;
                Log.i("CustomLightEffectView", "[onAttachedToWindow] executing pending stop");
                CustomLightEffectSurfaceView customLightEffectSurfaceView2 = this.b;
                if (customLightEffectSurfaceView2 != null) {
                    customLightEffectSurfaceView2.u(true);
                }
            }
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onDetachedFromWindow() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1360] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66881).isSupported) {
            Log.i("CustomLightEffectView", "[onDetachedFromWindow]");
            this.f = false;
            this.g = false;
            CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
            if (customLightEffectSurfaceView != null) {
                customLightEffectSurfaceView.u(true);
            }
            super.onDetachedFromWindow();
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.BaseCustomLightEffectView
    public void setEffectShader(@NotNull com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1341] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(bVar, this, 66736).isSupported) {
            Intrinsics.checkNotNullParameter(bVar, "shaderFilter");
            CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
            if (customLightEffectSurfaceView != null) {
                customLightEffectSurfaceView.setEffectShader(bVar);
            }
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.BaseCustomLightEffectView
    public void setFillBgColor(@ColorInt int i, boolean z) {
        byte[] bArr = SwordSwitches.switches6;
        if ((bArr == null || ((bArr[1343] >> 3) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Boolean.valueOf(z)}, this, 66748).isSupported) && i != this.e) {
            Log.d("CustomLightEffectView", "[setFillBgColor] bgColor: " + i);
            this.e = i;
            setBackgroundColor(i);
            CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
            if (customLightEffectSurfaceView != null) {
                customLightEffectSurfaceView.setFillBgColor(i, z);
            }
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.BaseCustomLightEffectView
    public void setMagicColorGenerator(@Nullable Function1<? super Bitmap, Pair<Integer, Integer>> function1) {
        CustomLightEffectSurfaceView customLightEffectSurfaceView;
        byte[] bArr = SwordSwitches.switches6;
        if ((bArr == null || ((bArr[1347] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(function1, this, 66781).isSupported) && (customLightEffectSurfaceView = this.b) != null) {
            customLightEffectSurfaceView.setMagicColorGenerator(function1);
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.BaseCustomLightEffectView
    public void setPicLoadListener(@Nullable v vVar) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1346] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(vVar, this, 66769).isSupported) {
            Log.i("CustomLightEffectView", "[setFillBgColor] FillBgColor: " + vVar);
            CustomLightEffectSurfaceView customLightEffectSurfaceView = this.b;
            if (customLightEffectSurfaceView != null) {
                customLightEffectSurfaceView.setPicLoadListener(vVar);
            }
        }
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    @JvmOverloads
    public CustomLightEffectView(@NotNull Context context, @Nullable AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        Intrinsics.checkNotNullParameter(context, "context");
        this.e = com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.t.a();
        f();
    }
}
