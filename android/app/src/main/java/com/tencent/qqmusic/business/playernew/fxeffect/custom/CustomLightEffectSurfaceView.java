package com.tencent.qqmusic.business.playernew.fxeffect.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import androidx.annotation.ColorInt;
import com.tencent.qqmusic.business.playernew.fxeffect.custom.CustomLightEffectSurfaceView;
import com.tencent.qqmusic.business.playernew.fxeffect.o;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusic.ui.minibar.n1;
import com.tencent.qqmusic.ui.minibar.v;
import android.util.Log;
import com.tencent.qqmusicplayerprocess.songinfo.SongInfo;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes4.dex */
public final class CustomLightEffectSurfaceView extends CustomRenderSurfaceView {

    @NotNull
    public static final a u = new a(null);

    @NotNull
    private final float[] j;

    @NotNull
    private final b l;

    @Nullable
    private com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a m;

    @Nullable
    private e n;

    @Nullable
    private com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b o;
    private int p;

    @Nullable
    private Pair<Integer, Integer> q;

    @Nullable
    private v r;

    @Nullable
    private Function1<? super Bitmap, Pair<Integer, Integer>> s;

    @Nullable
    private v t;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    public final class b extends Handler {
        public b() {
            super(Looper.getMainLooper());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void b(CustomLightEffectSurfaceView customLightEffectSurfaceView, b bVar) {
            byte[] bArr = SwordSwitches.switches6;
            if (bArr == null || ((bArr[1342] >> 6) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{customLightEffectSurfaceView, bVar}, (Object) null, 66743).isSupported) {
                Intrinsics.checkNotNullParameter(customLightEffectSurfaceView, "this$0");
                Intrinsics.checkNotNullParameter(bVar, "this$1");
                com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar2 = customLightEffectSurfaceView.o;
                if (bVar2 != null) {
                    bVar2.D();
                }
                customLightEffectSurfaceView.requestRender();
                bVar.sendEmptyMessageDelayed(0, (long) com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.t.b());
            }
        }

        @Override // android.os.Handler
        public void handleMessage(@NotNull Message message) {
            byte[] bArr = SwordSwitches.switches6;
            if (bArr == null || ((bArr[1340] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(message, this, 66727).isSupported) {
                Intrinsics.checkNotNullParameter(message, "msg");
                int i = message.what;
                if (i == 0) {
                    final CustomLightEffectSurfaceView customLightEffectSurfaceView = CustomLightEffectSurfaceView.this;
                    customLightEffectSurfaceView.queueEvent(new Runnable() { // from class: com.tencent.qqmusic.business.playernew.fxeffect.custom.b
                        @Override // java.lang.Runnable
                        public final void run() {
                            CustomLightEffectSurfaceView.b.b(customLightEffectSurfaceView, CustomLightEffectSurfaceView.b.this);
                        }
                    });
                } else if (i == 1) {
                    CustomLightEffectSurfaceView.this.u(true);
                }
            }
        }
    }

    public final class c implements v {
        final /* synthetic */ Function1<? super Pair<Integer, Integer>, Unit> b;

        c(Function1<? super Pair<Integer, Integer>, Unit> function1) {
            this.b = function1;
        }

        public void a(SongInfo songInfo, Bitmap bitmap, String str, boolean z) {
            Pair<Integer, Integer> pairP;
            byte[] bArr = SwordSwitches.switches6;
            if (bArr == null || ((bArr[1335] >> 7) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{songInfo, bitmap, str, Boolean.valueOf(z)}, this, 66688).isSupported) {
                Intrinsics.checkNotNullParameter(bitmap, "bitmap");
                Intrinsics.checkNotNullParameter(str, "url");
                v vVar = CustomLightEffectSurfaceView.this.r;
                if (vVar != null) {
                    vVar.a(songInfo, bitmap, str, z);
                }
                n1.a.E(this);
                if (CustomLightEffectSurfaceView.this.s != null) {
                    Function1 function1 = CustomLightEffectSurfaceView.this.s;
                    if (function1 == null || (pairP = (Pair) function1.invoke(bitmap)) == null) {
                        pairP = com.tencent.qqmusic.business.playernew.fxeffect.o.a.i();
                    }
                } else {
                    pairP = com.tencent.qqmusic.business.playernew.fxeffect.o.a.p(bitmap);
                }
                Log.i("CustomLESurfaceView", "[onImageLoaded] color pair: " + pairP);
                CustomLightEffectSurfaceView.this.r(pairP);
                Function1<? super Pair<Integer, Integer>, Unit> function12 = this.b;
                if (function12 != null) {
                    function12.invoke(pairP);
                }
            }
        }

        public void b(Bitmap bitmap) {
            byte[] bArr = SwordSwitches.switches6;
            if (bArr == null || ((bArr[1339] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(bitmap, this, 66717).isSupported) {
                Intrinsics.checkNotNullParameter(bitmap, "bitmap");
            }
        }
    }

    public CustomLightEffectSurfaceView(@Nullable Context context) {
        super(context);
        this.j = new float[16];
        this.l = new b();
        this.p = com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.t.a();
        f();
    }

    private final void f() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1344] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66754).isSupported) {
            getHolder().setFormat(-3);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void s(CustomLightEffectSurfaceView customLightEffectSurfaceView, Pair pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1365] >> 2) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{customLightEffectSurfaceView, pair}, (Object) null, 66923).isSupported) {
            Intrinsics.checkNotNullParameter(customLightEffectSurfaceView, "this$0");
            Intrinsics.checkNotNullParameter(pair, "$magicColorPair");
            customLightEffectSurfaceView.q = pair;
            com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar = customLightEffectSurfaceView.o;
            if (bVar != null) {
                bVar.L(pair);
            }
        }
    }

    public static /* synthetic */ void setFillBgColor$default(CustomLightEffectSurfaceView customLightEffectSurfaceView, int i, boolean z, int i2, Object obj) {
        if ((i2 & 2) != 0) {
            z = true;
        }
        customLightEffectSurfaceView.setFillBgColor(i, z);
    }

    public static /* synthetic */ void v(CustomLightEffectSurfaceView customLightEffectSurfaceView, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            z = true;
        }
        customLightEffectSurfaceView.u(z);
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.CustomRenderSurfaceView
    public void g() {
        com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar;
        byte[] bArr = SwordSwitches.switches6;
        if ((bArr == null || ((bArr[1350] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66808).isSupported) && (bVar = this.o) != null) {
            com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a aVar = this.m;
            if (aVar != null) {
                aVar.a();
            }
            bVar.K(this.j);
            bVar.O(this.m);
            e eVar = this.n;
            if (eVar != null) {
                eVar.a(this.m);
            }
            e eVar2 = this.n;
            if (eVar2 != null) {
                eVar2.d();
            }
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.CustomRenderSurfaceView
    public void h(int i, int i2) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1352] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}, this, 66821).isSupported) {
            Log.i("CustomLESurfaceView", "[onSurfaceChanged] width: " + i + ", height: " + i2);
            Matrix.setIdentityM(this.j, 0);
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.CustomRenderSurfaceView
    public void i() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1349] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66795).isSupported) {
            com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar = this.o;
            if (bVar == null) {
                throw new RuntimeException("[onSurfaceCreated] has not set shader");
            }
            this.m = new com.tencent.qqmusic.business.playernew.fxeffect.custom.programs.a(bVar.C(), bVar.w());
            this.n = new e(2, 2);
            Pair<Integer, Integer> pair = this.q;
            if (pair != null) {
                r(pair);
            }
        }
    }

    @Override // com.tencent.qqmusic.business.playernew.fxeffect.custom.CustomRenderSurfaceView
    public void j() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1354] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66833).isSupported) {
            v(this, false, 1, null);
            v vVar = this.t;
            if (vVar != null) {
                n1.a.E(vVar);
            }
            super.j();
            Log.i("CustomLESurfaceView", "[release]");
        }
    }

    public final void q(@Nullable SongInfo songInfo, @Nullable Function1<? super Pair<Integer, Integer>, Unit> function1) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1355] >> 6) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{songInfo, function1}, this, 66847).isSupported) {
            v vVar = this.t;
            if (vVar != null) {
                n1.a.E(vVar);
            }
            c cVar = new c(function1);
            this.t = cVar;
            n1.a.o(cVar);
            n1.a.w(songInfo);
        }
    }

    public final void r(@NotNull final Pair<Integer, Integer> pair) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1357] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(pair, this, 66861).isSupported) {
            Intrinsics.checkNotNullParameter(pair, "magicColorPair");
            queueEvent(new Runnable() { // from class: com.tencent.qqmusic.business.playernew.fxeffect.custom.a
                @Override // java.lang.Runnable
                public final void run() {
                    CustomLightEffectSurfaceView.s(CustomLightEffectSurfaceView.this, pair);
                }
            });
        }
    }

    public final void setEffectShader(@NotNull com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1345] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(bVar, this, 66765).isSupported) {
            Intrinsics.checkNotNullParameter(bVar, "shader");
            this.o = bVar;
        }
    }

    public final void setFillBgColor(@ColorInt int i, boolean z) {
        byte[] bArr = SwordSwitches.switches6;
        if ((bArr == null || ((bArr[1346] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Boolean.valueOf(z)}, this, 66773).isSupported) && i != this.p) {
            Log.i("CustomLESurfaceView", "[setFillBgColor] bgColor " + i);
            this.p = i;
            com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar = this.o;
            if (bVar != null) {
                bVar.F(i, z);
            }
        }
    }

    public final void setMagicColorGenerator(@Nullable Function1<? super Bitmap, Pair<Integer, Integer>> function1) {
        this.s = function1;
    }

    public final void setPicLoadListener(@Nullable v vVar) {
        this.r = vVar;
    }

    public final void t() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1359] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66878).isSupported) {
            if (!isAttachedToWindow()) {
                Log.w("CustomLESurfaceView", "[start] View not attached to window yet, skip");
            }
            this.l.removeCallbacksAndMessages(null);
            this.l.sendEmptyMessage(0);
            Log.d("CustomLESurfaceView", "start\nCustomLightEffectSurfaceView=  " + hashCode() + " \nFrameHandler = " + this.l.hashCode() + "\nRenderMode = " + getRenderMode() + "\nisAttachedToWindow = " + isAttachedToWindow());
            com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar = this.o;
            if (bVar != null) {
                bVar.M();
            }
        }
    }

    public final void u(boolean z) {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1362] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 66902).isSupported) {
            if (z) {
                this.l.removeCallbacksAndMessages(null);
            } else {
                this.l.removeMessages(1);
                this.l.sendEmptyMessageDelayed(1, 2000L);
            }
            Log.i("CustomLESurfaceView", "[stop] stopImmediately=" + z);
            com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b bVar = this.o;
            if (bVar != null) {
                bVar.N(z);
            }
        }
    }

    public CustomLightEffectSurfaceView(@Nullable Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.j = new float[16];
        this.l = new b();
        this.p = com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.b.t.a();
        f();
    }
}
