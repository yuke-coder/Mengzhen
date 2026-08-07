package com.tencent.qqmusic.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;
import com.tencent.qqmusic.ui.RoundedConstraintLayout;

/* JADX INFO: Access modifiers changed from: private */
/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes7.dex */
public final class RoundedConstraintLayout$b implements RoundedConstraintLayout.d {
    private final Path a;
    private final Path b;
    private final Paint c;
    private final RectF d;
    final /* synthetic */ RoundedConstraintLayout e;

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    RoundedConstraintLayout$b(RoundedConstraintLayout roundedConstraintLayout) {
        this.e = roundedConstraintLayout;
        this.a = new Path();
        this.b = new Path();
        this.c = new Paint(1);
        this.d = new RectF();
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public void a(Canvas canvas, boolean z) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[973] >> 0) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{canvas, Boolean.valueOf(z)}, this, 164585).isSupported) {
            if (z || RoundedConstraintLayout.M(this.e)) {
                if (Build.VERSION.SDK_INT <= 26) {
                    canvas.drawPath(this.a, this.c);
                    canvas.restore();
                    return;
                }
                this.b.reset();
                this.b.addRect(0.0f, 0.0f, (int) this.d.width(), (int) this.d.height(), Path.Direction.CW);
                this.b.op(this.a, Path.Op.DIFFERENCE);
                canvas.drawPath(this.b, this.c);
                canvas.restore();
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public void b(Canvas canvas, boolean z) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[972] >> 6) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{canvas, Boolean.valueOf(z)}, this, 164583).isSupported) {
            if (z || RoundedConstraintLayout.M(this.e)) {
                canvas.saveLayer(this.d, null, 31);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public void c(int i, int i2) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[975] >> 2) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}, this, 164603).isSupported) {
            this.d.set(0.0f, 0.0f, i, i2);
            this.a.reset();
            float[] fArrN = RoundedConstraintLayout.N(this.e);
            if (fArrN == null) {
                this.a.addRoundRect(this.d, RoundedConstraintLayout.O(this.e), RoundedConstraintLayout.O(this.e), Path.Direction.CW);
            } else {
                this.a.addRoundRect(this.d, fArrN, Path.Direction.CW);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public void d(float f, float f2, float f3, float f4) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[971] >> 5) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3), Float.valueOf(f4)}, this, 164574).isSupported) {
            if (Build.VERSION.SDK_INT <= 26) {
                this.c.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            } else {
                this.c.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            }
            this.e.postInvalidate();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public void setCornerRadius(float f) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[971] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(Float.valueOf(f), this, 164571).isSupported) {
            if (Build.VERSION.SDK_INT <= 26) {
                this.c.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            } else {
                this.c.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            }
            this.e.postInvalidate();
        }
    }
}


