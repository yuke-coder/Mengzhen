package com.tencent.qqmusiccommon.util;

import android.os.Handler;
import android.view.View;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;

/* JADX INFO: loaded from: classes2.dex */
public abstract class l0 implements View.OnClickListener {
    private long CLICK_OFFSET;
    private boolean isBusy;
    private Object isBusyLock;
    private Handler mHandler;

    class a implements Runnable {
        a() {
        }

        @Override // java.lang.Runnable
        public void run() {
            l0.this.isBusy = false;
        }
    }

    public l0() {
        this.CLICK_OFFSET = 500L;
        this.isBusy = false;
        this.isBusyLock = new Object();
        this.mHandler = new Handler();
    }

    public void clear() {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr == null || ((bArr[45] >> 3) & 1) <= 0 || !SwordProxy.proxyOneArg(null, this, 414764).isSupported) {
            this.mHandler.removeCallbacksAndMessages(null);
        }
    }

    public abstract void fastOnClick(View view);

    @Override // android.view.View.OnClickListener
    public final void onClick(View view) {
        byte[] bArr = SwordSwitches.switches38;
        if (bArr == null || ((bArr[43] >> 4) & 1) <= 0 || !SwordProxy.proxyOneArg(view, this, 414749).isSupported) {
            synchronized (this.isBusyLock) {
                if (this.isBusy) {
                    MLog.d("FastOnClickListener", "fast click return");
                    return;
                }
                this.isBusy = true;
                MLog.d("FastOnClickListener", "fast click action");
                this.mHandler.postDelayed(new a(), this.CLICK_OFFSET);
                fastOnClick(view);
            }
        }
    }

    public l0(long j) {
        this.CLICK_OFFSET = 500L;
        this.isBusy = false;
        this.isBusyLock = new Object();
        this.mHandler = new Handler();
        if (j >= 0) {
            this.CLICK_OFFSET = j;
        }
    }
}

