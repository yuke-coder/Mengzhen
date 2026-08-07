package com.tencent.qqmusic.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: classes20.dex */
public class PopupWindow extends android.widget.PopupWindow {
    public PopupWindow(@Nullable Context context, @Nullable AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        setTouchInterceptor(new View.OnTouchListener() { // from class: com.tencent.qqmusic.ui.u4
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return PopupWindow.d(PopupWindow.this, view, motionEvent);
            }
        });
        setWidth(-2);
        setHeight(-2);
        setBackgroundDrawable(new ColorDrawable());
        setTouchable(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean d(PopupWindow this$0, View view, MotionEvent motionEvent) {
        byte[] bArr = SwordSwitches.switches37;
        if (bArr != null && ((bArr[1045] >> 0) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{this$0, view, motionEvent}, null, 411561);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Boolean) swordProxyResultProxyMoreArgs.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (!this$0.isOutsideTouchable() || !this$0.isFocusable()) {
            return false;
        }
        if (motionEvent.getX() > 0.0f && motionEvent.getY() > 0.0f && motionEvent.getX() < ((float) view.getWidth()) && motionEvent.getY() < ((float) view.getHeight())) {
            return false;
        }
        this$0.dismiss();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean e(PopupWindow this$0, View view, MotionEvent motionEvent) {
        byte[] bArr = SwordSwitches.switches37;
        if (bArr != null && ((bArr[1046] >> 5) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{this$0, view, motionEvent}, null, 411574);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Boolean) swordProxyResultProxyMoreArgs.result).booleanValue();
            }
        }
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (!this$0.isOutsideTouchable() || !this$0.isFocusable()) {
            return false;
        }
        if (motionEvent.getX() > 0.0f && motionEvent.getY() > 0.0f && motionEvent.getX() < ((float) view.getWidth()) && motionEvent.getY() < ((float) view.getHeight())) {
            return false;
        }
        this$0.dismiss();
        return true;
    }

    public PopupWindow(@Nullable Context context) {
        this(context, (AttributeSet) null, 0, 0);
    }

    public PopupWindow(@Nullable Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public PopupWindow(@Nullable Context context, @Nullable AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public PopupWindow() {
        this((View) null, -2, -2);
    }

    public PopupWindow(@Nullable View view, int i, int i2) {
        this(view, i, i2, false);
    }

    public PopupWindow(@Nullable View view, int i, int i2, boolean z) {
        super(view, i, i2, z);
        setTouchInterceptor(new View.OnTouchListener() { // from class: com.tencent.qqmusic.ui.t4
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view2, MotionEvent motionEvent) {
                return PopupWindow.e(PopupWindow.this, view2, motionEvent);
            }
        });
        setBackgroundDrawable(new ColorDrawable());
        setTouchable(true);
    }
}

