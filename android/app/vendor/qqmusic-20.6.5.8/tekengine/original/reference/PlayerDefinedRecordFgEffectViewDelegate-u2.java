package com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef;

import TekEngineLib.Interface.ITekEffectInterface;
import TekEngineLib.Interface.ITekEffectStateListener;
import TekEngineLib.Interface.TekEffectImplement;
import TekEngineLib.Interface.UsualParamInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import com.tencent.qqmusic.business.ad.common.AdLiveData;
import com.tencent.qqmusic.business.playernew.interactor.playerstyle.PlayerStyle;
import com.tencent.qqmusic.business.playernew.view.BaseViewDelegate;
import com.tencent.qqmusic.business.playernew.viewmodel.FoldingScreenState;
import com.tencent.qqmusic.share.sharedialog.ShareSongDialogActivity;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusiccommon.util.GlobalLifeCycleManager;
import com.tencent.qqmusiccommon.util.MLog;
import java.util.HashMap;
import kotlin.Function;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.FunctionAdapter;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.jvm.internal.SourceDebugExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes18.dex */
@SourceDebugExtension({"SMAP\nPlayerDefinedRecordFgEffectViewDelegate.kt\nKotlin\n*S Kotlin\n*F\n+ 1 PlayerDefinedRecordFgEffectViewDelegate.kt\ncom/tencent/qqmusic/business/playernew/view/playersong/definedplayer/recorddef/PlayerDefinedRecordFgEffectViewDelegate\n+ 2 ViewEx.kt\ncom/tencent/qqmusiccommon/util/kotlinex/ViewExKt\n*L\n1#1,355:1\n197#2,2:356\n197#2,2:358\n*S KotlinDebug\n*F\n+ 1 PlayerDefinedRecordFgEffectViewDelegate.kt\ncom/tencent/qqmusic/business/playernew/view/playersong/definedplayer/recorddef/PlayerDefinedRecordFgEffectViewDelegate\n*L\n212#1:356,2\n218#1:358,2\n*E\n"})
public final class u2 extends BaseViewDelegate {

    @NotNull
    public static final a x = new a(null);

    @NotNull
    private final com.tencent.qqmusic.business.playernew.viewmodel.n i;

    @NotNull
    private final View j;

    @NotNull
    private ViewGroup l;

    @Nullable
    private ITekEffectInterface m;

    @Nullable
    private View n;
    private boolean o;

    @Nullable
    private com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.shader.a p;

    @Nullable
    private PlayerStyle q;

    @Nullable
    private com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.model.i r;

    @Nullable
    private Runnable s;

    @Nullable
    private Runnable t;
    private boolean u;

    @NotNull
    private Handler v;

    @NotNull
    private final Observer<Boolean> w;

    public static final class a {
        private a() {
        }

        public /* synthetic */ a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    static final class b implements Observer, FunctionAdapter {
        private final /* synthetic */ Function1 b;

        b(Function1 function1) {
            Intrinsics.checkNotNullParameter(function1, "function");
            this.b = function1;
        }

        public final boolean equals(@Nullable Object obj) {
            byte[] bArr = SwordSwitches.switches8;
            if (bArr != null && ((bArr[566] >> 2) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(obj, this, 82931);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
                }
            }
            if ((obj instanceof Observer) && (obj instanceof FunctionAdapter)) {
                return Intrinsics.areEqual(getFunctionDelegate(), ((FunctionAdapter) obj).getFunctionDelegate());
            }
            return false;
        }

        @NotNull
        public final Function<?> getFunctionDelegate() {
            return this.b;
        }

        public final int hashCode() {
            byte[] bArr = SwordSwitches.switches8;
            if (bArr != null && ((bArr[567] >> 2) & 1) > 0) {
                SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg((Object) null, this, 82939);
                if (swordProxyResultProxyOneArg.isSupported) {
                    return ((Integer) swordProxyResultProxyOneArg.result).intValue();
                }
            }
            return getFunctionDelegate().hashCode();
        }

        public final /* synthetic */ void onChanged(Object obj) {
            this.b.invoke(obj);
        }
    }

    public u2(@NotNull com.tencent.qqmusic.business.playernew.viewmodel.n nVar, @NotNull View view) {
        Intrinsics.checkNotNullParameter(nVar, "viewModel");
        Intrinsics.checkNotNullParameter(view, "container");
        this.i = nVar;
        this.j = view;
        View viewFindViewById = view.findViewById(2131310899);
        Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
        this.l = (ViewGroup) viewFindViewById;
        this.v = new Handler(Looper.getMainLooper());
        this.w = new Observer() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.t2
            public final void onChanged(Object obj) {
                u2.h2(this.b, ((Boolean) obj).booleanValue());
            }
        };
    }

    private final void B2() {
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[574] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 83000).isSupported) && !this.u) {
            this.l.setVisibility(0);
            C2();
        }
    }

    private final void C2() {
        ITekEffectInterface iTekEffectInterface;
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[569] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 82960).isSupported) && A1() && (iTekEffectInterface = this.m) != null) {
            if (iTekEffectInterface != null) {
                iTekEffectInterface.start();
            }
            E2();
        }
    }

    private final void D2() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[679] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 83834).isSupported) {
            C2();
            this.j.removeCallbacks(this.s);
            this.j.removeCallbacks(this.t);
            this.i.n2(true);
            this.q = (PlayerStyle) this.i.V4().getValue();
        }
    }

    private final void E2() {
        com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.shader.a aVar;
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[571] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 82975).isSupported) && A1() && this.m != null) {
            if ((this.o || this.i.J2()) && (aVar = this.p) != null) {
                aVar.start();
            }
        }
    }

    private final void G2(boolean z) {
        ITekEffectInterface iTekEffectInterface;
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[570] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 82968).isSupported) && (iTekEffectInterface = this.m) != null) {
            if (iTekEffectInterface != null) {
                iTekEffectInterface.pause();
            }
            J2(z);
        }
    }

    static /* synthetic */ void H2(u2 u2Var, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            z = false;
        }
        u2Var.G2(z);
    }

    private final void J2(boolean z) {
        com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.shader.a aVar;
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[572] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 82979).isSupported) && (aVar = this.p) != null) {
            aVar.stop(z);
        }
    }

    static /* synthetic */ void K2(u2 u2Var, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            z = false;
        }
        u2Var.J2(z);
    }

    private final void L2(int i) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[676] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 83809).isSupported) {
            HashMap map = new HashMap();
            map.put("BlendMode", Integer.valueOf(i));
            ITekEffectInterface iTekEffectInterface = this.m;
            if (iTekEffectInterface != null) {
                iTekEffectInterface.setParam("UsualParam", new UsualParamInfo("TargetLayerAll", map));
            }
        }
    }

    private final void M2(boolean z) {
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[678] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 83828).isSupported) && this.n != null) {
            if (com.tencent.qqmusic.business.ad.media.c0.u(this.i.getCurrentSong())) {
                View view = this.n;
                if (view != null) {
                    view.setVisibility(8);
                }
                MLog.d("PlayerDefinedRecordFgEffectViewDelegate", "[updateLightEffectView]--long audio ad showing,return");
                return;
            }
            this.l.setVisibility(0);
            View view2 = this.n;
            if (view2 != null) {
                view2.setVisibility(0);
            }
            if (this.i.w().n()) {
                D2();
            }
        }
    }

    static /* synthetic */ void N2(u2 u2Var, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            z = false;
        }
        u2Var.M2(z);
    }

    private final void O2(com.tencent.qqmusic.business.playernew.viewmodel.p0 p0Var, boolean z) {
        com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.shader.a aVar;
        byte[] bArr = SwordSwitches.switches8;
        if ((bArr == null || ((bArr[577] >> 2) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{p0Var, Boolean.valueOf(z)}, this, 83019).isSupported) && (aVar = this.p) != null) {
            float width = this.l.getWidth();
            float height = this.l.getHeight();
            if (width <= 0.0f || height <= 0.0f) {
                return;
            }
            aVar.setCurrentTime(System.currentTimeMillis() / 1000.0f);
            aVar.setViewSize(width, height);
            aVar.setAspectRadio(width / height);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void h2(u2 u2Var, boolean z) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[679] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{u2Var, Boolean.valueOf(z)}, (Object) null, 83837).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            MLog.i("PlayerDefinedRecordFgEffectViewDelegate", "[adShowObserver] show=" + z);
            if (z) {
                K2(u2Var, false, 1, null);
                return;
            }
            Boolean bool = (Boolean) u2Var.i.Y3().getValue();
            if (bool != null ? bool.booleanValue() : false) {
                K2(u2Var, false, 1, null);
            } else {
                u2Var.E2();
            }
        }
    }

    private final void i2(com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.model.c cVar) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[578] >> 1) & 1) <= 0 || !SwordProxy.proxyOneArg(cVar, this, 83026).isSupported) {
            com.tencent.qqmusic.business.playernew.view.playersong.vinyl.w1 w1VarY = cVar.y();
            String strB = w1VarY != null ? w1VarY.b() : null;
            if (!(strB == null || strB.length() == 0)) {
                com.tencent.qqmusic.business.playernew.view.playersong.vinyl.w1 w1VarY2 = cVar.y();
                String strD = w1VarY2 != null ? w1VarY2.d() : null;
                if (!(strD == null || strD.length() == 0)) {
                    this.l.setVisibility(0);
                    if (this.m == null) {
                        TekEffectImplement tekEffectImplement = new TekEffectImplement();
                        tekEffectImplement.setContext(this.j.getContext());
                        tekEffectImplement.setStateListener(new ITekEffectStateListener() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.j2
                            public final void onStateChange(int i, int i2, Object obj) {
                                u2.j2(this.b, i, i2, obj);
                            }
                        });
                        com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.shader.a aVar = new com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.shader.a();
                        float width = this.l.getWidth();
                        float height = this.l.getHeight();
                        aVar.setCurrentTime(System.currentTimeMillis() / 1000.0f);
                        aVar.setViewSize(width, height);
                        aVar.setAspectRadio(width / height);
                        this.p = aVar;
                        tekEffectImplement.setShader(aVar);
                        this.m = tekEffectImplement;
                        View viewCreateView = tekEffectImplement.createView();
                        this.n = viewCreateView;
                        if (viewCreateView != null) {
                            viewCreateView.setId(2131315338);
                        }
                        ITekEffectInterface iTekEffectInterface = this.m;
                        if (iTekEffectInterface != null) {
                            iTekEffectInterface.setCryptKey("c!sUm_qQ");
                        }
                        ITekEffectInterface iTekEffectInterface2 = this.m;
                        if (iTekEffectInterface2 != null) {
                            com.tencent.qqmusic.business.playernew.view.playersong.vinyl.w1 w1VarY3 = cVar.y();
                            iTekEffectInterface2.updateEffect(w1VarY3 != null ? w1VarY3.d() : null);
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("[createEffect] hashCode=");
                        sb.append(this.i.A4().hashCode());
                        sb.append(", setDataSource=");
                        com.tencent.qqmusic.business.playernew.view.playersong.vinyl.w1 w1VarY4 = cVar.y();
                        sb.append(w1VarY4 != null ? w1VarY4.d() : null);
                        MLog.i("PlayerDefinedRecordFgEffectViewDelegate", sb.toString());
                        L2(3);
                        this.l.setVisibility(0);
                        View view = this.n;
                        if ((view != null ? view.getParent() : null) == null) {
                            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -1);
                            layoutParams.gravity = 80;
                            View view2 = this.n;
                            TextureView textureView = view2 instanceof TextureView ? (TextureView) view2 : null;
                            if (textureView != null) {
                                textureView.setOpaque(false);
                            }
                            this.l.addView(this.n, layoutParams);
                        }
                        D2();
                    }
                    N2(this, false, 1, null);
                    return;
                }
            }
            MLog.i("PlayerDefinedRecordFgEffectViewDelegate", "[createEffect] url=null");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void j2(final u2 u2Var, int i, int i2, Object obj) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[711] >> 7) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{u2Var, Integer.valueOf(i), Integer.valueOf(i2), obj}, (Object) null, 84096).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            MLog.i("PlayerDefinedRecordFgEffectViewDelegate", "[createEffect] onStateChange=" + i + ", errorCode=" + i2);
            if (i2 < 0) {
                u2Var.v.post(new Runnable() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.k2
                    @Override // java.lang.Runnable
                    public final void run() {
                        u2.k2(this.b);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void k2(u2 u2Var) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[711] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(u2Var, (Object) null, 84089).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            u2Var.u = true;
            u2Var.m2();
        }
    }

    private final void l2() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[677] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 83821).isSupported) {
            G2(true);
            View view = this.n;
            if ((view != null ? view.getParent() : null) != null) {
                this.l.removeView(this.n);
            }
            this.n = null;
            ITekEffectInterface iTekEffectInterface = this.m;
            if (iTekEffectInterface != null) {
                iTekEffectInterface.destory();
            }
            this.m = null;
            this.v.removeCallbacksAndMessages(null);
        }
    }

    private final void m2() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[575] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 83007).isSupported) {
            this.l.setVisibility(8);
            H2(this, false, 1, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit n2(Ref.BooleanRef booleanRef, MediatorLiveData mediatorLiveData, Ref.BooleanRef booleanRef2, Boolean bool) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr != null && ((bArr[709] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{booleanRef, mediatorLiveData, booleanRef2, bool}, (Object) null, 84073);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Unit) swordProxyResultProxyMoreArgs.result;
            }
        }
        Intrinsics.checkNotNullParameter(booleanRef, "$showAdVipAd");
        Intrinsics.checkNotNullParameter(mediatorLiveData, "$this_apply");
        Intrinsics.checkNotNullParameter(booleanRef2, "$showAd");
        booleanRef.element = bool.booleanValue();
        p2(mediatorLiveData, booleanRef2, booleanRef);
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final Unit o2(Ref.BooleanRef booleanRef, MediatorLiveData mediatorLiveData, Ref.BooleanRef booleanRef2, Boolean bool) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr != null && ((bArr[708] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{booleanRef, mediatorLiveData, booleanRef2, bool}, (Object) null, 84066);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return (Unit) swordProxyResultProxyMoreArgs.result;
            }
        }
        Intrinsics.checkNotNullParameter(booleanRef, "$showAd");
        Intrinsics.checkNotNullParameter(mediatorLiveData, "$this_apply");
        Intrinsics.checkNotNullParameter(booleanRef2, "$showAdVipAd");
        booleanRef.element = bool.booleanValue();
        p2(mediatorLiveData, booleanRef, booleanRef2);
        return Unit.INSTANCE;
    }

    private static final void p2(MediatorLiveData<Boolean> mediatorLiveData, Ref.BooleanRef booleanRef, Ref.BooleanRef booleanRef2) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[707] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{mediatorLiveData, booleanRef, booleanRef2}, (Object) null, 84061).isSupported) {
            mediatorLiveData.setValue(Boolean.valueOf(booleanRef.element || booleanRef2.element));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void s2(final u2 u2Var, com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.model.i iVar) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[680] >> 7) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{u2Var, iVar}, (Object) null, 83848).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            if (!iVar.n() || Intrinsics.areEqual(u2Var.r, iVar)) {
                return;
            }
            u2Var.l2();
            final com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.model.c cVarB = iVar.b();
            if (cVarB != null) {
                Runnable runnable = u2Var.s;
                if (runnable != null) {
                    u2Var.j.removeCallbacks(runnable);
                }
                Runnable runnable2 = new Runnable() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.i2
                    @Override // java.lang.Runnable
                    public final void run() {
                        u2.t2(this.b, cVarB);
                    }
                };
                u2Var.s = runnable2;
                u2Var.j.post(runnable2);
            }
            u2Var.r = iVar;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void t2(u2 u2Var, com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.model.c cVar) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[680] >> 5) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{u2Var, cVar}, (Object) null, 83846).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            u2Var.i2(cVar);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void u2(u2 u2Var, com.tencent.qqmusic.business.playernew.viewmodel.i.a aVar) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[681] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{u2Var, aVar}, (Object) null, 83853).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            if (aVar instanceof com.tencent.qqmusic.business.playernew.viewmodel.i.a.b) {
                u2Var.M2(true);
            } else {
                N2(u2Var, false, 1, null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void v2(u2 u2Var, Integer num) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[682] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{u2Var, num}, (Object) null, 83861).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            Intrinsics.checkNotNull(num);
            if (com.tencent.qqmusiccommon.util.music.j.o(num.intValue())) {
                u2Var.o = true;
                u2Var.E2();
            } else {
                u2Var.o = false;
                K2(u2Var, false, 1, null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void w2(u2 u2Var, com.tencent.qqmusic.business.playernew.viewmodel.p0 p0Var) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[683] >> 5) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{u2Var, p0Var}, (Object) null, 83870).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            Intrinsics.checkNotNull(p0Var);
            u2Var.O2(p0Var, u2Var.i.t4());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void x2(u2 u2Var, Float f) {
        com.tencent.qqmusic.business.playernew.customlyric.entity.h hVarA;
        byte[] bArr = SwordSwitches.switches8;
        boolean z = false;
        if (bArr == null || ((bArr[684] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{u2Var, f}, (Object) null, 83877).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            if (u2Var.i.t4()) {
                return;
            }
            Integer num = (Integer) u2Var.i.W4().getValue();
            if (num != null && num.intValue() == 2) {
                if (f.floatValue() >= 0.8f) {
                    com.tencent.qqmusic.business.playernew.customlyric.h0 h0Var = (com.tencent.qqmusic.business.playernew.customlyric.h0) u2Var.i.s1().getValue();
                    if (h0Var != null && (hVarA = h0Var.a()) != null && hVarA.h()) {
                        z = true;
                    }
                    if (!z) {
                        u2Var.m2();
                        return;
                    }
                }
                u2Var.B2();
                return;
            }
            Integer num2 = (Integer) u2Var.i.W4().getValue();
            if (num2 != null && num2.intValue() == 0) {
                u2Var.B2();
                return;
            }
            Integer num3 = (Integer) u2Var.i.W4().getValue();
            if (num3 != null && num3.intValue() == 1 && f.floatValue() < 0.8f) {
                u2Var.B2();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void y2(u2 u2Var, FoldingScreenState foldingScreenState) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[706] >> 3) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{u2Var, foldingScreenState}, (Object) null, 84052).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            com.tencent.qqmusic.business.playernew.viewmodel.p0 p0Var = (com.tencent.qqmusic.business.playernew.viewmodel.p0) u2Var.i.g().getValue();
            if (p0Var != null) {
                u2Var.O2(p0Var, u2Var.i.t4());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void z2(u2 u2Var) {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[710] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg(u2Var, (Object) null, 84081).isSupported) {
            Intrinsics.checkNotNullParameter(u2Var, "this$0");
            if (GlobalLifeCycleManager.INSTANCE.getTopActivity() instanceof ShareSongDialogActivity) {
                return;
            }
            u2Var.m2();
        }
    }

    @NotNull
    public BaseViewDelegate.MeditateMode D1() {
        return BaseViewDelegate.MeditateMode.YES;
    }

    public void E1() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[568] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 82947).isSupported) {
            this.i.A4().observe(this, new Observer() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.l2
                public final void onChanged(Object obj) {
                    u2.s2(this.b, (com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.model.i) obj);
                }
            });
            this.i.i().observe(this, new Observer() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.m2
                public final void onChanged(Object obj) {
                    u2.u2(this.b, (com.tencent.qqmusic.business.playernew.viewmodel.i.a) obj);
                }
            });
            this.i.c().observe(this, new Observer() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.n2
                public final void onChanged(Object obj) {
                    u2.v2(this.b, (Integer) obj);
                }
            });
            this.i.g().observe(this, new Observer() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.o2
                public final void onChanged(Object obj) {
                    u2.w2(this.b, (com.tencent.qqmusic.business.playernew.viewmodel.p0) obj);
                }
            });
            this.i.l4().observe(this, new Observer() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.p2
                public final void onChanged(Object obj) {
                    u2.x2(this.b, (Float) obj);
                }
            });
            this.i.B1().observe(this, new Observer() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.q2
                public final void onChanged(Object obj) {
                    u2.y2(this.b, (FoldingScreenState) obj);
                }
            });
            final MediatorLiveData mediatorLiveData = new MediatorLiveData();
            final Ref.BooleanRef booleanRef = new Ref.BooleanRef();
            final Ref.BooleanRef booleanRef2 = new Ref.BooleanRef();
            mediatorLiveData.addSource(AdLiveData.a.s(), new b(new Function1() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.r2
                public final Object invoke(Object obj) {
                    return u2.o2(booleanRef, mediatorLiveData, booleanRef2, (Boolean) obj);
                }
            }));
            mediatorLiveData.addSource(com.tencent.qqmusic.advip.mgr.h.b.h(), new b(new Function1() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.s2
                public final Object invoke(Object obj) {
                    return u2.n2(booleanRef2, mediatorLiveData, booleanRef, (Boolean) obj);
                }
            }));
            mediatorLiveData.observe(this, this.w);
        }
    }

    public void G1() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[573] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 82989).isSupported) {
            super.G1();
            Runnable runnable = this.t;
            if (runnable != null) {
                this.j.removeCallbacks(runnable);
            }
            Runnable runnable2 = new Runnable() { // from class: com.tencent.qqmusic.business.playernew.view.playersong.definedplayer.recorddef.h2
                @Override // java.lang.Runnable
                public final void run() {
                    u2.z2(this.b);
                }
            };
            this.t = runnable2;
            this.j.postDelayed(runnable2, 200L);
        }
    }

    public void H1() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[576] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 83012).isSupported) {
            super.H1();
            MLog.i("PlayerDefinedRecordFgEffectViewDelegate", "[onUnbind]");
            l2();
            this.u = false;
        }
    }

    public void I1() {
        byte[] bArr = SwordSwitches.switches8;
        if (bArr == null || ((bArr[573] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 82987).isSupported) {
            super.I1();
            B2();
        }
    }
}
