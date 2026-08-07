package com.tencent.qqmusic.ui;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusiccommon.util.MLog;

public class RepeatingImageButton extends ImageButton {
    private long b;
    private int d;
    private RepeatListener e;
    private long f;
    private boolean g;
    private final Runnable h;

    public interface RepeatListener {
        void a(View view, long j, int i, boolean z);
    }

    class a implements Runnable {
        @Override
        public void run() {
            byte[] bArr = SwordSwitches.switches15;
            if (bArr == null || ((bArr[1007] >> 7) & 1) <= 0 ||
                    !SwordProxy.proxyOneArg(null, this, 164864).isSupported) {
                try {
                    RepeatingImageButton.this.c(false);
                    if (RepeatingImageButton.this.isPressed()) {
                        RepeatingImageButton repeatingImageButton = RepeatingImageButton.this;
                        repeatingImageButton.postDelayed(this, repeatingImageButton.f);
                    }
                } catch (Exception exception) {
                    MLog.e("RepeatingImageButton run", exception);
                }
            }
        }
    }

    class b implements Runnable {
        @Override
        public void run() {
            RepeatingImageButton.this.setFocusable(true);
            RepeatingImageButton.this.setLongClickable(true);
        }
    }

    public RepeatingImageButton(Context context) {
        this(context, null);
    }

    private void c(boolean z) {
        int i;
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[846] >> 2) & 1) <= 0 ||
                !SwordProxy.proxyOneArg(Boolean.valueOf(z), this, 163571).isSupported) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            RepeatListener repeatListener = this.e;
            if (repeatListener != null) {
                long elapsed = elapsedRealtime - this.b;
                if (z) {
                    i = -1;
                } else {
                    i = this.d;
                    this.d = i + 1;
                }
                repeatListener.a(this, elapsed, i, z);
            }
        }
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr != null && ((bArr[844] >> 5) & 1) > 0) {
            SwordProxyResult result = SwordProxy.proxyMoreArgs(
                    new Object[]{Integer.valueOf(i), keyEvent}, this, 163558);
            if (result.isSupported) {
                return ((Boolean) result.result).booleanValue();
            }
        }
        if (!this.g) {
            return super.onKeyUp(i, keyEvent);
        }
        if (i == KeyEvent.KEYCODE_DPAD_CENTER || i == KeyEvent.KEYCODE_ENTER) {
            removeCallbacks(this.h);
            if (this.b != 0) {
                c(true);
                this.b = 0L;
            }
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr != null && ((bArr[843] >> 0) & 1) > 0) {
            SwordProxyResult result = SwordProxy.proxyOneArg(motionEvent, this, 163545);
            if (result.isSupported) {
                return ((Boolean) result.result).booleanValue();
            }
        }
        if (!this.g) {
            return super.onTouchEvent(motionEvent);
        }
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            removeCallbacks(this.h);
            if (this.b != 0) {
                c(true);
                this.b = 0L;
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public boolean performLongClick() {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr != null && ((bArr[842] >> 0) & 1) > 0) {
            SwordProxyResult result = SwordProxy.proxyOneArg(null, this, 163537);
            if (result.isSupported) {
                return ((Boolean) result.result).booleanValue();
            }
        }
        if (!this.g) {
            return super.performLongClick();
        }
        this.b = SystemClock.elapsedRealtime();
        this.d = 0;
        post(this.h);
        return false;
    }

    public void setFunctionEnable(boolean z) {
        this.g = z;
    }

    public void setRepeatListener(RepeatListener repeatListener, long j) {
        this.e = repeatListener;
        this.f = j;
    }

    public RepeatingImageButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.imageButtonStyle);
    }

    public RepeatingImageButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.f = 500L;
        this.g = false;
        this.h = new a();
        post(new b());
    }
}
