package com.tencent.qqmusic.business.playernew.fxeffect.custom.shader;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes4.dex */
@SourceDebugExtension({"SMAP\nAudioDataMockManager.kt\nKotlin\n*S Kotlin\n*F\n+ 1 AudioDataMockManager.kt\ncom/tencent/qqmusic/business/playernew/fxeffect/custom/shader/AudioDataMockManager\n+ 2 Scalar.kt\ncom/google/android/filament/utils/ScalarKt\n*L\n1#1,113:1\n34#2:114\n34#2:115\n34#2:116\n*S KotlinDebug\n*F\n+ 1 AudioDataMockManager.kt\ncom/tencent/qqmusic/business/playernew/fxeffect/custom/shader/AudioDataMockManager\n*L\n68#1:114\n72#1:115\n104#1:116\n*E\n"})
public final class a {

    @NotNull
    public static final C0060a d = new C0060a(null);

    @Nullable
    private final b a;

    @NotNull
    private float[] b = f.D.a();

    @NotNull
    private c c = new c(com.tencent.biz.common.handler.c.a("RealTime_HandlerThread").getLooper());

    /* JADX INFO: renamed from: com.tencent.qqmusic.business.playernew.fxeffect.custom.shader.a$a, reason: collision with other inner class name */
    public static final class C0060a {
        private C0060a() {
        }

        public /* synthetic */ C0060a(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    public interface b {
        void c(@NotNull float[] fArr);
    }

    public final class c extends Handler {
        c(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            byte[] bArr = SwordSwitches.switches6;
            if (bArr == null || ((bArr[1328] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(message, this, 66629).isSupported) {
                Intrinsics.checkNotNullParameter(message, "msg");
                int i = message.what;
                if (i == 1) {
                    removeMessages(2);
                    removeMessages(1);
                    a.this.e();
                    sendEmptyMessageDelayed(1, 166L);
                    sendEmptyMessageDelayed(2, 33L);
                    return;
                }
                if (i == 2) {
                    removeMessages(2);
                    a.this.c();
                    sendEmptyMessageDelayed(2, 33L);
                }
            }
        }
    }

    public a(@Nullable b bVar) {
        this.a = bVar;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void c() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1340] >> 0) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66721).isSupported) {
            float[] fArr = this.b;
            float[] fArr2 = new float[16];
            for (int i = 0; i < 16; i++) {
                float f = fArr[i] - 0.05f;
                if (f < 0.005f) {
                    f = 0.005f;
                } else if (f > 1.0f) {
                    f = 1.0f;
                }
                fArr2[i] = f;
            }
            this.b = fArr2;
            b bVar = this.a;
            if (bVar != null) {
                bVar.c(fArr2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void e() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1336] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66696).isSupported) {
            float[] fArr = this.b;
            float[] fArr2 = new float[16];
            for (int i = 0; i < 16; i++) {
                float fNextFloat = Random.Default.nextFloat();
                if (fNextFloat < 0.1f) {
                    fNextFloat = 0.1f;
                } else if (fNextFloat > 0.83f) {
                    fNextFloat = 0.83f;
                }
                float fD = d(fNextFloat);
                float f = fArr[i];
                if (fD >= f) {
                    fArr2[i] = fD;
                } else {
                    float f2 = f - 0.05f;
                    if (f2 < 0.005f) {
                        f2 = 0.005f;
                    } else if (f2 > 1.0f) {
                        f2 = 1.0f;
                    }
                    fArr2[i] = f2;
                }
            }
            this.b = fArr2;
            b bVar = this.a;
            if (bVar != null) {
                bVar.c(fArr2);
            }
        }
    }

    public float d(float f) {
        if (f < 0.5688889f) {
            f = 0.5688889f;
        } else if (f > 0.8888889f) {
            f = 0.8888889f;
        }
        float f2 = (f - 0.5688889f) / 0.32f;
        return (0.995f * f2 * f2) + 0.005f;
    }

    public final void f() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1334] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66680).isSupported) {
            this.c.sendEmptyMessage(1);
        }
    }

    public final void g() {
        byte[] bArr = SwordSwitches.switches6;
        if (bArr == null || ((bArr[1335] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg((Object) null, this, 66686).isSupported) {
            this.c.removeMessages(1);
            this.c.removeMessages(2);
        }
    }
}
