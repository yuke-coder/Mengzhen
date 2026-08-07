package com.tencent.qqmusicplayerprocess.audio.playlist;

import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import kotlin.jvm.internal.DefaultConstructorMarker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: classes20.dex */
public final class y {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    private int f37638a;
    private int b;

    /* JADX WARN: Illegal instructions before constructor call */
    public y() {

        this(0, 0, 3, null);
    }

    public y(int i, int i2) {
        this.f37638a = i;
        this.b = i2;
    }

    public final int a() {
        return this.f37638a;
    }

    public final int b() {
        return this.b;
    }

    public boolean equals(@Nullable Object obj) {
        if (obj instanceof y) {
            y yVar = (y) obj;
            if (yVar.f37638a == this.f37638a && yVar.b == this.b) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        byte[] bArr = SwordSwitches.switches14;
        if (bArr != null && ((bArr[1065] >> 1) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(null, this, 154122);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Integer) swordProxyResultProxyOneArg.result).intValue();
            }
        }
        return y.class.hashCode();
    }

    @NotNull
    public String toString() {
        byte[] bArr = SwordSwitches.switches14;
        if (bArr != null && ((bArr[1066] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(null, this, 154129);
            if (swordProxyResultProxyOneArg.isSupported) {
                return (String) swordProxyResultProxyOneArg.result;
            }
        }
        return "oneShotMultiRepeatCount = " + this.f37638a + " oneShotMultiRepeatRemainCount = " + this.b;
    }

    public /* synthetic */ y(int i, int i2, int i3, DefaultConstructorMarker defaultConstructorMarker) {
        this((i3 & 1) != 0 ? -1 : i, (i3 & 2) != 0 ? -1 : i2);
    }
}

