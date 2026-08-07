package com.tencent.biz.common.handler;

/* loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes12.dex */
public class d extends b {
    private a b;

    public d(String str, int i) {
        super(str, i);
    }

    public synchronized void a(Runnable runnable) {
        if (this.b == null) {
            this.b = new a(getLooper());
        }
        this.b.post(runnable);
    }

    public synchronized void b(Runnable runnable, long j) {
        if (this.b == null) {
            this.b = new a(getLooper());
        }
        this.b.postDelayed(runnable, j);
    }

    public synchronized void c(Runnable runnable) {
        if (this.b == null) {
            this.b = new a(getLooper());
        }
        this.b.removeCallbacks(runnable);
    }

    public synchronized void d(Runnable runnable) {
        if (this.b == null) {
            this.b = new a(getLooper());
        }
        this.b.removeCallbacksAndMessages(runnable);
    }
}
