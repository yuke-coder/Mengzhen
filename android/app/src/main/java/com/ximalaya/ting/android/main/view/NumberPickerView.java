package com.ximalaya.ting.android.main.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;
import androidx.core.view.MotionEventCompat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class NumberPickerView extends View {
    private int A;
    private int B;
    private int C;
    private int D;
    private String E;
    private String F;
    private String G;
    private String H;
    private float I;
    private float J;
    private float K;
    private float L;
    private boolean M;
    private boolean N;
    private boolean O;
    private boolean P;
    private boolean Q;
    private boolean R;
    private boolean S;
    private boolean T;
    private Scroller U;
    private VelocityTracker V;
    private Paint W;

    private int f120471a;
    private float aA;
    private float aB;
    private int aC;
    private int aD;
    private int aE;
    private int aF;
    private int aG;
    private TextPaint aa;
    private Paint ab;
    private String[] ac;
    private CharSequence[] ad;
    private CharSequence[] ae;
    private HandlerThread af;
    private Handler ag;
    private Handler ah;
    private Map<String, Integer> ai;
    private d aj;
    private b ak;
    private a al;
    private c am;
    private int an;
    private int ao;
    private int ap;
    private int aq;
    private int ar;
    private float as;
    private float at;
    private float au;
    private boolean av;
    private int aw;
    private int ax;
    private int ay;
    private float az;

    private int f120472b;

    private int f120473c;

    private int f120474d;

    private int f120475e;

    private int f120476f;

    private int f120477g;
    private int h;
    private int i;
    private int j;
    private int k;
    private int l;
    private int m;
    private int n;
    private int o;
    private int p;
    private int q;
    private int r;
    private int s;
    private int t;
    private int u;
    private int v;
    private int w;
    private int x;
    private int y;
    private int z;

    public interface a {
        void a(NumberPickerView numberPickerView, int i);
    }

    public interface b {
        void onValueChange(NumberPickerView numberPickerView, int i, int i2);
    }

    public interface c {
        void onValueChangeInScrolling(NumberPickerView numberPickerView, int i, int i2);
    }

    public interface d {
        void a(NumberPickerView numberPickerView, int i, int i2, String[] strArr);
    }

    private float a(float f2, float f3, float f4) {
        return f3 + ((f4 - f3) * f2);
    }

    private int a(float f2, int i, int i2) {
        int i3 = (i & (-16777216)) >>> 24;
        int i4 = (i & 16711680) >>> 16;
        int i5 = (i & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >>> 8;
        int i6 = (i & 255) >>> 0;
        return ((int) (i6 + ((((i2 & 255) >>> 0) - i6) * f2))) | (((int) (i3 + (((((-16777216) & i2) >>> 24) - i3) * f2))) << 24) | (((int) (i4 + ((((16711680 & i2) >>> 16) - i4) * f2))) << 16) | (((int) (i5 + ((((65280 & i2) >>> 8) - i5) * f2))) << 8);
    }

    public NumberPickerView(Context context) {
        super(context);
        this.f120471a = -13421773;
        this.f120472b = -695533;
        this.f120473c = -695533;
        this.f120474d = 0;
        this.f120475e = 0;
        this.f120476f = 0;
        this.f120477g = 0;
        this.h = 0;
        this.i = 0;
        this.j = 0;
        this.k = 0;
        this.l = 0;
        this.m = -695533;
        this.n = 2;
        this.o = 0;
        this.p = 0;
        this.q = 3;
        this.r = 0;
        this.s = 0;
        this.t = -1;
        this.u = -1;
        this.v = 0;
        this.w = 0;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.A = 0;
        this.B = 0;
        this.C = 150;
        this.D = 8;
        this.I = 1.0f;
        this.J = 0.0f;
        this.K = 0.0f;
        this.L = 0.0f;
        this.M = true;
        this.N = true;
        this.O = false;
        this.P = false;
        this.Q = true;
        this.R = false;
        this.S = false;
        this.T = true;
        this.W = new Paint();
        this.aa = new TextPaint();
        this.ab = new Paint();
        this.ai = new ConcurrentHashMap();
        this.an = 0;
        this.as = 0.0f;
        this.at = 0.0f;
        this.au = 0.0f;
        this.av = false;
        this.aC = 0;
        this.aD = 0;
        this.aE = 0;
        this.aF = 0;
        this.aG = 0;
        a(context);
    }

    public NumberPickerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.f120471a = -13421773;
        this.f120472b = -695533;
        this.f120473c = -695533;
        this.f120474d = 0;
        this.f120475e = 0;
        this.f120476f = 0;
        this.f120477g = 0;
        this.h = 0;
        this.i = 0;
        this.j = 0;
        this.k = 0;
        this.l = 0;
        this.m = -695533;
        this.n = 2;
        this.o = 0;
        this.p = 0;
        this.q = 3;
        this.r = 0;
        this.s = 0;
        this.t = -1;
        this.u = -1;
        this.v = 0;
        this.w = 0;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.A = 0;
        this.B = 0;
        this.C = 150;
        this.D = 8;
        this.I = 1.0f;
        this.J = 0.0f;
        this.K = 0.0f;
        this.L = 0.0f;
        this.M = true;
        this.N = true;
        this.O = false;
        this.P = false;
        this.Q = true;
        this.R = false;
        this.S = false;
        this.T = true;
        this.W = new Paint();
        this.aa = new TextPaint();
        this.ab = new Paint();
        this.ai = new ConcurrentHashMap();
        this.an = 0;
        this.as = 0.0f;
        this.at = 0.0f;
        this.au = 0.0f;
        this.av = false;
        this.aC = 0;
        this.aD = 0;
        this.aE = 0;
        this.aF = 0;
        this.aG = 0;
        a(context, attributeSet);
        a(context);
    }

    public NumberPickerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.f120471a = -13421773;
        this.f120472b = -695533;
        this.f120473c = -695533;
        this.f120474d = 0;
        this.f120475e = 0;
        this.f120476f = 0;
        this.f120477g = 0;
        this.h = 0;
        this.i = 0;
        this.j = 0;
        this.k = 0;
        this.l = 0;
        this.m = -695533;
        this.n = 2;
        this.o = 0;
        this.p = 0;
        this.q = 3;
        this.r = 0;
        this.s = 0;
        this.t = -1;
        this.u = -1;
        this.v = 0;
        this.w = 0;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.A = 0;
        this.B = 0;
        this.C = 150;
        this.D = 8;
        this.I = 1.0f;
        this.J = 0.0f;
        this.K = 0.0f;
        this.L = 0.0f;
        this.M = true;
        this.N = true;
        this.O = false;
        this.P = false;
        this.Q = true;
        this.R = false;
        this.S = false;
        this.T = true;
        this.W = new Paint();
        this.aa = new TextPaint();
        this.ab = new Paint();
        this.ai = new ConcurrentHashMap();
        this.an = 0;
        this.as = 0.0f;
        this.at = 0.0f;
        this.au = 0.0f;
        this.av = false;
        this.aC = 0;
        this.aD = 0;
        this.aE = 0;
        this.aF = 0;
        this.aG = 0;
        a(context, attributeSet);
        a(context);
    }

    private void a(Context context, AttributeSet attributeSet) {
        if (attributeSet == null) {
            return;
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, new int[]{com.mengzhen.app.R.attr.npv_AlternativeHint, com.mengzhen.app.R.attr.npv_AlternativeTextArrayWithMeasureHint, com.mengzhen.app.R.attr.npv_AlternativeTextArrayWithoutMeasureHint, com.mengzhen.app.R.attr.npv_DividerColor, com.mengzhen.app.R.attr.npv_DividerHeight, com.mengzhen.app.R.attr.npv_DividerMarginLeft, com.mengzhen.app.R.attr.npv_DividerMarginRight, com.mengzhen.app.R.attr.npv_EmptyItemHint, com.mengzhen.app.R.attr.npv_HintText, com.mengzhen.app.R.attr.npv_ItemPaddingHorizontal, com.mengzhen.app.R.attr.npv_ItemPaddingVertical, com.mengzhen.app.R.attr.npv_MarginEndOfHint, com.mengzhen.app.R.attr.npv_MarginStartOfHint, com.mengzhen.app.R.attr.npv_MaxValue, com.mengzhen.app.R.attr.npv_MinValue, com.mengzhen.app.R.attr.npv_RespondChangeInMainThread, com.mengzhen.app.R.attr.npv_RespondChangeOnDetached, com.mengzhen.app.R.attr.npv_ShowDivider, com.mengzhen.app.R.attr.npv_ShownCount, com.mengzhen.app.R.attr.npv_TextArray, com.mengzhen.app.R.attr.npv_TextColorHint, com.mengzhen.app.R.attr.npv_TextColorNormal, com.mengzhen.app.R.attr.npv_TextColorSelected, com.mengzhen.app.R.attr.npv_TextEllipsize, com.mengzhen.app.R.attr.npv_TextSizeHint, com.mengzhen.app.R.attr.npv_TextSizeNormal, com.mengzhen.app.R.attr.npv_TextSizeSelected, com.mengzhen.app.R.attr.npv_WrapSelectorWheel});
        int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            int index = typedArrayObtainStyledAttributes.getIndex(i);
            if (index == 18) {
                this.q = typedArrayObtainStyledAttributes.getInt(index, 3);
            } else if (index == 3) {
                this.m = typedArrayObtainStyledAttributes.getColor(index, -695533);
            } else if (index == 4) {
                this.n = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 2);
            } else if (index == 5) {
                this.o = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
            } else if (index == 6) {
                this.p = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 0);
            } else if (index == 19) {
                this.ac = a(typedArrayObtainStyledAttributes.getTextArray(index));
            } else if (index == 21) {
                this.f120471a = typedArrayObtainStyledAttributes.getColor(index, -13421773);
            } else if (index == 22) {
                this.f120472b = typedArrayObtainStyledAttributes.getColor(index, -695533);
            } else if (index == 20) {
                this.f120473c = typedArrayObtainStyledAttributes.getColor(index, -695533);
            } else if (index == 25) {
                this.f120474d = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, a(context, 14.0f));
            } else if (index == 26) {
                this.f120475e = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, a(context, 16.0f));
            } else if (index == 24) {
                this.f120476f = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, a(context, 14.0f));
            } else if (index == 14) {
                this.t = typedArrayObtainStyledAttributes.getInteger(index, 0);
            } else if (index == 13) {
                this.u = typedArrayObtainStyledAttributes.getInteger(index, 0);
            } else if (index == 27) {
                this.N = typedArrayObtainStyledAttributes.getBoolean(index, true);
            } else if (index == 17) {
                this.M = typedArrayObtainStyledAttributes.getBoolean(index, true);
            } else if (index == 8) {
                this.E = typedArrayObtainStyledAttributes.getString(index);
            } else if (index == 0) {
                this.H = typedArrayObtainStyledAttributes.getString(index);
            } else if (index == 7) {
                this.G = typedArrayObtainStyledAttributes.getString(index);
            } else if (index == 12) {
                this.i = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, b(context, 8.0f));
            } else if (index == 11) {
                this.j = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, b(context, 8.0f));
            } else if (index == 10) {
                this.k = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, b(context, 2.0f));
            } else if (index == 9) {
                this.l = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, b(context, 5.0f));
            } else if (index == 1) {
                this.ad = typedArrayObtainStyledAttributes.getTextArray(index);
            } else if (index == 2) {
                this.ae = typedArrayObtainStyledAttributes.getTextArray(index);
            } else if (index == 16) {
                this.S = typedArrayObtainStyledAttributes.getBoolean(index, false);
            } else if (index == 15) {
                this.T = typedArrayObtainStyledAttributes.getBoolean(index, true);
            } else if (index == 23) {
                this.F = typedArrayObtainStyledAttributes.getString(index);
            }
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    private void a(Context context) {
        this.U = new Scroller(context);
        this.C = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
        this.D = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        if (this.f120474d == 0) {
            this.f120474d = a(context, 14.0f);
        }
        if (this.f120475e == 0) {
            this.f120475e = a(context, 16.0f);
        }
        if (this.f120476f == 0) {
            this.f120476f = a(context, 14.0f);
        }
        if (this.i == 0) {
            this.i = b(context, 8.0f);
        }
        if (this.j == 0) {
            this.j = b(context, 8.0f);
        }
        this.W.setColor(this.m);
        this.W.setAntiAlias(true);
        this.W.setStyle(Paint.Style.STROKE);
        this.W.setStrokeWidth(this.n);
        this.aa.setColor(this.f120471a);
        this.aa.setAntiAlias(true);
        this.aa.setTextAlign(Paint.Align.CENTER);
        this.ab.setColor(this.f120473c);
        this.ab.setAntiAlias(true);
        this.ab.setTextAlign(Paint.Align.CENTER);
        this.ab.setTextSize(this.f120476f);
        int i = this.q;
        if (i % 2 == 0) {
            this.q = i + 1;
        }
        if (this.t == -1 || this.u == -1) {
            l();
        }
        b();
    }

    private void b() {
        HandlerThread handlerThread = new HandlerThread("HandlerThread-For-Refreshing");
        this.af = handlerThread;
        handlerThread.start();
        this.ag = new Handler(this.af.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                int iC;
                Message messageA;
                Handler handler;
                long j;
                int i;
                NumberPickerView numberPickerView;
                int i2;
                int i3 = message.what;
                if (i3 != 1) {
                    if (i3 != 2) {
                        return;
                    }
                    NumberPickerView.this.a(message.arg1, message.arg2, message.obj);
                    return;
                }
                int i4 = 0;
                if (!NumberPickerView.this.U.isFinished()) {
                    if (NumberPickerView.this.an == 0) {
                        NumberPickerView.this.b(1);
                    }
                    handler = NumberPickerView.this.ag;
                    messageA = NumberPickerView.this.a(1, 0, 0, message.obj);
                    j = 32;
                } else {
                    if (NumberPickerView.this.aD != 0) {
                        if (NumberPickerView.this.an == 0) {
                            NumberPickerView.this.b(1);
                        }
                        if (NumberPickerView.this.aD < (-NumberPickerView.this.ay) / 2) {
                            i = (int) (((NumberPickerView.this.ay + NumberPickerView.this.aD) * 300.0f) / NumberPickerView.this.ay);
                            NumberPickerView.this.U.startScroll(0, NumberPickerView.this.aE, 0, NumberPickerView.this.aD + NumberPickerView.this.ay, i * 3);
                            numberPickerView = NumberPickerView.this;
                            i2 = numberPickerView.aE + NumberPickerView.this.ay;
                        } else {
                            i = (int) (((-NumberPickerView.this.aD) * 300.0f) / NumberPickerView.this.ay);
                            NumberPickerView.this.U.startScroll(0, NumberPickerView.this.aE, 0, NumberPickerView.this.aD, i * 3);
                            numberPickerView = NumberPickerView.this;
                            i2 = numberPickerView.aE;
                        }
                        iC = numberPickerView.c(i2 + NumberPickerView.this.aD);
                        i4 = i;
                        NumberPickerView.this.postInvalidate();
                    } else {
                        NumberPickerView.this.b(0);
                        NumberPickerView numberPickerView2 = NumberPickerView.this;
                        iC = numberPickerView2.c(numberPickerView2.aE);
                    }
                    NumberPickerView numberPickerView3 = NumberPickerView.this;
                    messageA = numberPickerView3.a(2, numberPickerView3.B, iC, message.obj);
                    handler = NumberPickerView.this.T ? NumberPickerView.this.ah : NumberPickerView.this.ag;
                    j = i4 * 2;
                }
                handler.sendMessageDelayed(messageA, j);
            }
        };
        this.ah = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                int i = message.what;
                if (i == 2) {
                    NumberPickerView.this.a(message.arg1, message.arg2, message.obj);
                    return false;
                }
                if (i != 3) {
                    return false;
                }
                NumberPickerView.this.requestLayout();
                return false;
            }
        });
    }

    private void b(int i, int i2) {
        this.am.onValueChangeInScrolling(this, i, i2);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        a(false);
        setMeasuredDimension(f(i), g(i2));
    }

    /* JADX WARN: Code duplicated, block: B:10:0x003d  */
    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        int value;
        super.onSizeChanged(i, i2, i3, i4);
        this.aw = i;
        this.ax = i2;
        this.ay = i2 / this.q;
        this.aB = ((i + getPaddingLeft()) - getPaddingRight()) / 2.0f;
        boolean z = false;
        if (getOneRecycleSize() <= 1) {
            value = 0;
        } else if (this.P) {
            value = getValue() - this.v;
        } else if (this.O) {
            value = this.aC + ((this.q - 1) / 2);
        } else {
            value = 0;
        }
        if (this.N && this.Q) {
            z = true;
        }
        b(value, z);
        e();
        f();
        d();
        this.P = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        HandlerThread handlerThread = this.af;
        if (handlerThread == null || !handlerThread.isAlive()) {
            b();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.af.quit();
        if (this.ay == 0) {
            return;
        }
        if (!this.U.isFinished()) {
            this.U.abortAnimation();
            this.aE = this.U.getCurrY();
            g();
            int i = this.aD;
            if (i != 0) {
                int i2 = this.ay;
                if (i < (-i2) / 2) {
                    this.aE = this.aE + i2 + i;
                } else {
                    this.aE += i;
                }
                g();
            }
            b(0);
        }
        int iC = c(this.aE);
        int i3 = this.B;
        if (iC != i3 && this.S) {
            try {
                b bVar = this.ak;
                if (bVar != null) {
                    int i4 = this.v;
                    bVar.onValueChange(this, i3 + i4, i4 + iC);
                }
                d dVar = this.aj;
                if (dVar != null) {
                    dVar.a(this, this.B, iC, this.ac);
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        this.B = iC;
    }

    public int getOneRecycleSize() {
        return (this.u - this.t) + 1;
    }

    public int getRawContentSize() {
        String[] strArr = this.ac;
        if (strArr != null) {
            return strArr.length;
        }
        return 0;
    }

    public void a(String[] strArr, int i, boolean z) {
        a();
        if (strArr == null) {
            throw new IllegalArgumentException("newDisplayedValues should not be null.");
        }
        if (i < 0) {
            throw new IllegalArgumentException("pickedIndex should not be negative, now pickedIndex is " + i);
        }
        a(strArr);
        a(true);
        f();
        k();
        this.B = this.t + i;
        b(i, this.N && this.Q);
        if (z) {
            this.ag.sendMessageDelayed(h(1), 0L);
            postInvalidate();
        }
    }

    public void a(String[] strArr, boolean z) {
        a(strArr, 0, z);
    }

    public void setDisplayedValues(String[] strArr) {
        o();
        a();
        if (strArr == null) {
            throw new IllegalArgumentException("newDisplayedValues should not be null.");
        }
        if ((this.w - this.v) + 1 <= strArr.length) {
            a(strArr);
            a(true);
            this.B = this.t + 0;
            b(0, this.N && this.Q);
            postInvalidate();
            this.ah.sendEmptyMessage(3);
            return;
        }
        throw new IllegalArgumentException("mMaxValue - mMinValue + 1 should not be greater than mDisplayedValues.length, now ((mMaxValue - mMinValue + 1) is " + ((this.w - this.v) + 1) + " newDisplayedValues.length is " + strArr.length + ", you need to set MaxValue and MinValue before setDisplayedValues(String[])");
    }

    public String[] getDisplayedValues() {
        return this.ac;
    }

    public void setWrapSelectorWheel(boolean z) {
        if (this.N != z) {
            if (z) {
                this.N = z;
                n();
                postInvalidate();
            } else if (this.an == 0) {
                c();
            } else {
                this.R = true;
            }
        }
    }

    public void a(int i, int i2, Object obj) {
        b(0);
        if (i != i2 && (obj == null || !(obj instanceof Boolean) || ((Boolean) obj).booleanValue())) {
            b bVar = this.ak;
            if (bVar != null) {
                int i3 = this.v;
                bVar.onValueChange(this, i + i3, i3 + i2);
            }
            d dVar = this.aj;
            if (dVar != null) {
                dVar.a(this, i, i2, this.ac);
            }
        }
        this.B = i2;
        if (this.R) {
            this.R = false;
            c();
        }
    }

    private void a(int i) {
        a(i, true);
    }

    private void a(int i, boolean z) {
        int pickedIndexRelativeToRaw;
        int pickedIndexRelativeToRaw2;
        int i2;
        int i3;
        if ((!this.N || !this.Q) && ((pickedIndexRelativeToRaw2 = (pickedIndexRelativeToRaw = getPickedIndexRelativeToRaw()) + i) > (i2 = this.u) || pickedIndexRelativeToRaw2 < (i2 = this.t))) {
            i = i2 - pickedIndexRelativeToRaw;
        }
        int i4 = this.aD;
        int i5 = this.ay;
        if (i4 < (-i5) / 2) {
            int i6 = i5 + i4;
            int i7 = (int) (((i4 + i5) * 300.0f) / i5);
            i3 = i < 0 ? (-i7) - (i * 300) : i7 + (i * 300);
            i4 = i6;
        } else {
            int i8 = (int) (((-i4) * 300.0f) / i5);
            int i9 = i * 300;
            i3 = i < 0 ? i8 - i9 : i8 + i9;
        }
        int i10 = i4 + (i * i5);
        if (i3 < 300) {
            i3 = 300;
        }
        if (i3 > 600) {
            i3 = 600;
        }
        this.U.startScroll(0, this.aE, 0, i10, i3);
        if (z) {
            this.ag.sendMessageDelayed(h(1), i3 / 4);
        } else {
            this.ag.sendMessageDelayed(a(1, 0, 0, Boolean.valueOf(z)), i3 / 4);
        }
        postInvalidate();
    }

    public int getMinValue() {
        return this.v;
    }

    public int getMaxValue() {
        return this.w;
    }

    public void setMinValue(int i) {
        this.v = i;
        this.t = 0;
        f();
    }

    public void setMaxValue(int i) {
        String[] strArr = this.ac;
        Objects.requireNonNull(strArr, "mDisplayedValues should not be null");
        int i2 = this.v;
        if ((i - i2) + 1 > strArr.length) {
            throw new IllegalArgumentException("(maxValue - mMinValue + 1) should not be greater than mDisplayedValues.length now  (maxValue - mMinValue + 1) is " + ((i - this.v) + 1) + " and mDisplayedValues.length is " + this.ac.length);
        }
        this.w = i;
        int i3 = this.t;
        int i4 = (i - i2) + i3;
        this.u = i4;
        a(i3, i4);
        f();
    }

    public void setValue(int i) {
        int i2 = this.v;
        if (i < i2) {
            throw new IllegalArgumentException("should not set a value less than mMinValue, value is " + i);
        }
        if (i <= this.w) {
            setPickedIndexRelativeToRaw(i - i2);
            return;
        }
        throw new IllegalArgumentException("should not set a value greater than mMaxValue, value is " + i);
    }

    public int getValue() {
        return getPickedIndexRelativeToRaw() + this.v;
    }

    public String getContentByCurrValue() {
        return this.ac[getValue() - this.v];
    }

    public boolean getWrapSelectorWheel() {
        return this.N;
    }

    public boolean getWrapSelectorWheelAbsolutely() {
        return this.N && this.Q;
    }

    public void setHintText(String str) {
        if (a(this.E, str)) {
            return;
        }
        this.E = str;
        this.L = a(this.ab.getFontMetrics());
        this.f120477g = a(this.E, this.ab);
        this.ah.sendEmptyMessage(3);
    }

    public void setPickedIndexRelativeToMin(int i) {
        if (i < 0 || i >= getOneRecycleSize()) {
            return;
        }
        this.B = this.t + i;
        b(i, this.N && this.Q);
        postInvalidate();
    }

    public void setNormalTextColor(int i) {
        if (this.f120471a == i) {
            return;
        }
        this.f120471a = i;
        postInvalidate();
    }

    public void setSelectedTextColor(int i) {
        if (this.f120472b == i) {
            return;
        }
        this.f120472b = i;
        postInvalidate();
    }

    public void setHintTextColor(int i) {
        if (this.f120473c == i) {
            return;
        }
        this.f120473c = i;
        this.ab.setColor(i);
        postInvalidate();
    }

    public void setDividerColor(int i) {
        if (this.m == i) {
            return;
        }
        this.m = i;
        this.W.setColor(i);
        postInvalidate();
    }

    public void setPickedIndexRelativeToRaw(int i) {
        int i2 = this.t;
        if (i2 <= -1 || i2 > i || i > this.u) {
            return;
        }
        Handler handler = this.ah;
        if (handler != null) {
            handler.removeMessages(2);
        }
        Handler handler2 = this.ag;
        if (handler2 != null) {
            handler2.removeMessages(2);
        }
        this.B = i;
        b(i - this.t, this.N && this.Q);
        postInvalidate();
    }

    public int getPickedIndexRelativeToRaw() {
        int i = this.aD;
        if (i == 0) {
            return c(this.aE);
        }
        int i2 = this.ay;
        return i < (-i2) / 2 ? c(this.aE + i2 + i) : c(this.aE + i);
    }

    public void a(int i, int i2) {
        a(i, i2, true);
    }

    public void a(int i, int i2, boolean z) {
        if (i > i2) {
            throw new IllegalArgumentException("minShowIndex should be less than maxShowIndex, minShowIndex is " + i + ", maxShowIndex is " + i2 + ".");
        }
        String[] strArr = this.ac;
        if (strArr == null) {
            throw new IllegalArgumentException("mDisplayedValues should not be null, you need to set mDisplayedValues first.");
        }
        if (i < 0) {
            throw new IllegalArgumentException("minShowIndex should not be less than 0, now minShowIndex is " + i);
        }
        if (i > strArr.length - 1) {
            throw new IllegalArgumentException("minShowIndex should not be greater than (mDisplayedValues.length - 1), now (mDisplayedValues.length - 1) is " + (this.ac.length - 1) + " minShowIndex is " + i);
        }
        if (i2 < 0) {
            throw new IllegalArgumentException("maxShowIndex should not be less than 0, now maxShowIndex is " + i2);
        }
        if (i2 > strArr.length - 1) {
            throw new IllegalArgumentException("maxShowIndex should not be greater than (mDisplayedValues.length - 1), now (mDisplayedValues.length - 1) is " + (this.ac.length - 1) + " maxShowIndex is " + i2);
        }
        this.t = i;
        this.u = i2;
        if (z) {
            this.B = i + 0;
            b(0, this.N && this.Q);
            postInvalidate();
        }
    }

    public void setFriction(float f2) {
        if (f2 > 0.0f) {
            ViewConfiguration.get(getContext());
            this.I = ViewConfiguration.getScrollFriction() / f2;
        } else {
            throw new IllegalArgumentException("you should set a a positive float friction, now friction is " + f2);
        }
    }

    public void b(int i) {
        if (this.an == i) {
            return;
        }
        this.an = i;
        a aVar = this.al;
        if (aVar != null) {
            aVar.a(this, i);
        }
    }

    public void setOnScrollListener(a aVar) {
        this.al = aVar;
    }

    public void setOnValueChangedListener(b bVar) {
        this.ak = bVar;
    }

    public void setOnValueChangedListenerRelativeToRaw(d dVar) {
        this.aj = dVar;
    }

    public void setOnValueChangeListenerInScrolling(c cVar) {
        this.am = cVar;
    }

    public void setContentTextTypeface(Typeface typeface) {
        this.aa.setTypeface(typeface);
    }

    public void setHintTextTypeface(Typeface typeface) {
        this.ab.setTypeface(typeface);
    }

    public int c(int i) {
        int i2 = this.ay;
        boolean z = false;
        if (i2 == 0) {
            return 0;
        }
        int i3 = (i / i2) + (this.q / 2);
        int oneRecycleSize = getOneRecycleSize();
        if (this.N && this.Q) {
            z = true;
        }
        int iB = b(i3, oneRecycleSize, z);
        if (iB >= 0 && iB < getOneRecycleSize()) {
            return iB + this.t;
        }
        throw new IllegalArgumentException("getWillPickIndexByGlobalY illegal index : " + iB + " getOneRecycleSize() : " + getOneRecycleSize() + " mWrapSelectorWheel : " + this.N);
    }

    private int b(int i, int i2, boolean z) {
        if (i2 <= 0) {
            return 0;
        }
        if (!z) {
            return i;
        }
        int i3 = i % i2;
        return i3 < 0 ? i3 + i2 : i3;
    }

    private void c() {
        b(getPickedIndexRelativeToRaw() - this.t, false);
        this.N = false;
        postInvalidate();
    }

    private void d() {
        int i = this.q;
        int i2 = i / 2;
        this.r = i2;
        int i3 = i2 + 1;
        this.s = i3;
        int i4 = this.ax;
        this.az = (i2 * i4) / i;
        this.aA = (i3 * i4) / i;
        if (this.o < 0) {
            this.o = 0;
        }
        if (this.p < 0) {
            this.p = 0;
        }
        if (this.o + this.p != 0 && getPaddingLeft() + this.o >= (this.aw - getPaddingRight()) - this.p) {
            int paddingLeft = getPaddingLeft() + this.o + getPaddingRight();
            int i5 = this.p;
            int i6 = (paddingLeft + i5) - this.aw;
            int i7 = this.o;
            float f2 = i6;
            int i8 = (int) (i7 - ((i7 * f2) / (i7 + i5)));
            this.o = i8;
            this.p = (int) (i5 - ((f2 * i5) / (i8 + i5)));
        }
    }

    private void e() {
        int i = this.f120474d;
        int i2 = this.ay;
        if (i > i2) {
            this.f120474d = i2;
        }
        if (this.f120475e > i2) {
            this.f120475e = i2;
        }
        Paint paint = this.ab;
        if (paint == null) {
            throw new IllegalArgumentException("mPaintHint should not be null.");
        }
        paint.setTextSize(this.f120476f);
        this.L = a(this.ab.getFontMetrics());
        this.f120477g = a(this.E, this.ab);
        TextPaint textPaint = this.aa;
        if (textPaint == null) {
            throw new IllegalArgumentException("mPaintText should not be null.");
        }
        textPaint.setTextSize(this.f120475e);
        this.K = a(this.aa.getFontMetrics());
        this.aa.setTextSize(this.f120474d);
        this.J = a(this.aa.getFontMetrics());
    }

    private void f() {
        this.aq = 0;
        this.ar = (-this.q) * this.ay;
        if (this.ac != null) {
            int oneRecycleSize = getOneRecycleSize();
            int i = this.q;
            int i2 = this.ay;
            this.aq = ((oneRecycleSize - (i / 2)) - 1) * i2;
            this.ar = (-(i / 2)) * i2;
        }
    }

    private int d(int i) {
        if (this.N && this.Q) {
            return i;
        }
        int i2 = this.ar;
        return (i >= i2 && i <= (i2 = this.aq)) ? i : i2;
    }

    /* JADX WARN: Code duplicated, block: B:24:0x005c  */
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.ay == 0) {
            return true;
        }
        if (this.V == null) {
            this.V = VelocityTracker.obtain();
        }
        this.V.addMovement(motionEvent);
        this.au = motionEvent.getY();
        int action = motionEvent.getAction();
        if (action == 0) {
            this.av = true;
            this.ag.removeMessages(1);
            a();
            this.at = this.au;
            this.as = this.aE;
            b(0);
            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (action != 1) {
            if (action == 2) {
                float f2 = this.at - this.au;
                if (this.av) {
                    int i = this.D;
                    if ((-i) >= f2 || f2 >= i) {
                        this.av = false;
                        this.aE = d((int) (this.as + f2));
                        g();
                        invalidate();
                    }
                } else {
                    this.av = false;
                    this.aE = d((int) (this.as + f2));
                    g();
                    invalidate();
                }
                b(1);
            } else if (action == 3) {
                this.as = this.aE;
                a();
                this.ag.sendMessageDelayed(h(1), 0L);
            }
        } else if (this.av) {
            a(motionEvent);
        } else {
            VelocityTracker velocityTracker = this.V;
            velocityTracker.computeCurrentVelocity(1000);
            int yVelocity = (int) (velocityTracker.getYVelocity() * this.I);
            if (Math.abs(yVelocity) > this.C) {
                this.U.fling(0, this.aE, 0, -yVelocity, Integer.MIN_VALUE, Integer.MAX_VALUE, d(Integer.MIN_VALUE), d(Integer.MAX_VALUE));
                invalidate();
                b(2);
            }
            this.ag.sendMessageDelayed(h(1), 0L);
            h();
        }
        return true;
    }

    private void a(MotionEvent motionEvent) {
        float y = motionEvent.getY();
        for (int i = 0; i < this.q; i++) {
            int i2 = this.ay;
            if (i2 * i <= y && y < i2 * (i + 1)) {
                e(i);
                return;
            }
        }
    }

    private void e(int i) {
        int i2;
        if (i < 0 || i >= (i2 = this.q)) {
            return;
        }
        a(i - (i2 / 2));
    }

    private float a(Paint.FontMetrics fontMetrics) {
        if (fontMetrics == null) {
            return 0.0f;
        }
        return Math.abs(fontMetrics.top + fontMetrics.bottom) / 2.0f;
    }

    private void b(int i, boolean z) {
        int i2 = i - ((this.q - 1) / 2);
        this.aC = i2;
        int iB = b(i2, getOneRecycleSize(), z);
        this.aC = iB;
        int i3 = this.ay;
        if (i3 == 0) {
            this.O = true;
            return;
        }
        this.aE = i3 * iB;
        int i4 = iB + (this.q / 2);
        this.ao = i4;
        int oneRecycleSize = i4 % getOneRecycleSize();
        this.ao = oneRecycleSize;
        if (oneRecycleSize < 0) {
            this.ao = oneRecycleSize + getOneRecycleSize();
        }
        this.ap = this.ao;
        g();
    }

    @Override
    public void computeScroll() {
        if (this.ay != 0 && this.U.computeScrollOffset()) {
            this.aE = this.U.getCurrY();
            g();
            postInvalidate();
        }
    }

    private void g() {
        int iFloor = (int) Math.floor(this.aE / this.ay);
        this.aC = iFloor;
        int i = this.aE;
        int i2 = this.ay;
        int i3 = -(i - (iFloor * i2));
        this.aD = i3;
        if (this.am != null) {
            if ((-i3) > i2 / 2) {
                iFloor++;
            }
            this.ap = iFloor + (this.q / 2);
            int oneRecycleSize = this.ap % getOneRecycleSize();
            this.ap = oneRecycleSize;
            if (oneRecycleSize < 0) {
                this.ap = oneRecycleSize + getOneRecycleSize();
            }
            int i4 = this.ao;
            int i5 = this.ap;
            if (i4 != i5) {
                int i6 = this.v;
                b(i4 + i6, i5 + i6);
            }
            this.ao = this.ap;
        }
    }

    private void h() {
        VelocityTracker velocityTracker = this.V;
        if (velocityTracker != null) {
            velocityTracker.clear();
            this.V.recycle();
            this.V = null;
        }
    }

    private void a(boolean z) {
        i();
        j();
        if (z) {
            if (this.aF == Integer.MIN_VALUE || this.aG == Integer.MIN_VALUE) {
                this.ah.sendEmptyMessage(3);
            }
        }
    }

    private int f(int i) {
        int mode = View.MeasureSpec.getMode(i);
        this.aF = mode;
        int size = View.MeasureSpec.getSize(i);
        if (mode == 1073741824) {
            return size;
        }
        int paddingLeft = getPaddingLeft() + getPaddingRight() + Math.max(this.z, Math.max(this.x, this.A) + (((Math.max(this.f120477g, this.h) != 0 ? this.i : 0) + Math.max(this.f120477g, this.h) + (Math.max(this.f120477g, this.h) == 0 ? 0 : this.j) + (this.l * 2)) * 2));
        return mode == Integer.MIN_VALUE ? Math.min(paddingLeft, size) : paddingLeft;
    }

    private int g(int i) {
        int mode = View.MeasureSpec.getMode(i);
        this.aG = mode;
        int size = View.MeasureSpec.getSize(i);
        if (mode == 1073741824) {
            return size;
        }
        int paddingTop = getPaddingTop() + getPaddingBottom() + (this.q * (this.y + (this.k * 2)));
        return mode == Integer.MIN_VALUE ? Math.min(paddingTop, size) : paddingTop;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        a(canvas);
        b(canvas);
        c(canvas);
    }

    private void a(Canvas canvas) {
        float fA;
        float fA2;
        float f2;
        int iA;
        String string;
        float f3 = 0.0f;
        int i = 0;
        while (i < this.q + 1) {
            float f4 = this.aD + (this.ay * i);
            int iB = b(this.aC + i, getOneRecycleSize(), this.N && this.Q);
            int i2 = this.q;
            if (i == i2 / 2) {
                int i3 = this.ay;
                f2 = (this.aD + i3) / i3;
                iA = a(f2, this.f120471a, this.f120472b);
                fA = a(f2, this.f120474d, this.f120475e);
                fA2 = a(f2, this.J, this.K);
            } else if (i == (i2 / 2) + 1) {
                float f5 = 1.0f - f3;
                int iA2 = a(f5, this.f120471a, this.f120472b);
                float fA3 = a(f5, this.f120474d, this.f120475e);
                float fA4 = a(f5, this.J, this.K);
                f2 = f3;
                iA = iA2;
                fA = fA3;
                fA2 = fA4;
            } else {
                int i4 = this.f120471a;
                fA = this.f120474d;
                fA2 = this.J;
                f2 = f3;
                iA = i4;
            }
            this.aa.setColor(iA);
            this.aa.setTextSize(fA);
            if (iB < 0 || iB >= getOneRecycleSize()) {
                if (!TextUtils.isEmpty(this.G)) {
                    string = this.G;
                }
                i++;
                f3 = f2;
                continue;
            } else {
                CharSequence charSequenceEllipsize = this.ac[iB + this.t];
                if (this.F != null) {
                    charSequenceEllipsize = TextUtils.ellipsize(charSequenceEllipsize, this.aa, getWidth() - (this.l * 2), getEllipsizeType());
                }
                string = charSequenceEllipsize.toString();
            }
            canvas.drawText(string, this.aB, f4 + (this.ay / 2) + fA2, this.aa);
            i++;
            f3 = f2;
        }
    }

    private TextUtils.TruncateAt getEllipsizeType() {
        String str = this.F;
        str.hashCode();
        switch (str) {
            case "middle":
                return TextUtils.TruncateAt.MIDDLE;
            case "end":
                return TextUtils.TruncateAt.END;
            case "start":
                return TextUtils.TruncateAt.START;
            default:
                throw new IllegalArgumentException("Illegal text ellipsize type.");
        }
    }

    private void b(Canvas canvas) {
        if (this.M) {
            canvas.drawLine(getPaddingLeft() + this.o, this.az, (this.aw - getPaddingRight()) - this.p, this.az, this.W);
            canvas.drawLine(getPaddingLeft() + this.o, this.aA, (this.aw - getPaddingRight()) - this.p, this.aA, this.W);
        }
    }

    private void c(Canvas canvas) {
        if (TextUtils.isEmpty(this.E)) {
            return;
        }
        canvas.drawText(this.E, this.aB + ((this.x + this.f120477g) / 2) + this.i, ((this.az + this.aA) / 2.0f) + this.L, this.ab);
    }

    private void i() {
        float textSize = this.aa.getTextSize();
        this.aa.setTextSize(this.f120475e);
        this.x = a(this.ac, this.aa);
        this.z = a(this.ad, this.aa);
        this.A = a(this.ae, this.aa);
        this.aa.setTextSize(this.f120476f);
        this.h = a(this.H, this.aa);
        this.aa.setTextSize(textSize);
    }

    private int a(CharSequence[] charSequenceArr, Paint paint) {
        if (charSequenceArr == null) {
            return 0;
        }
        int iMax = 0;
        for (CharSequence charSequence : charSequenceArr) {
            if (charSequence != null) {
                iMax = Math.max(a(charSequence, paint), iMax);
            }
        }
        return iMax;
    }

    private int a(CharSequence charSequence, Paint paint) {
        Integer num;
        if (TextUtils.isEmpty(charSequence)) {
            return 0;
        }
        String string = charSequence.toString();
        if (this.ai.containsKey(string) && (num = this.ai.get(string)) != null) {
            return num.intValue();
        }
        int iMeasureText = (int) (paint.measureText(string) + 0.5f);
        this.ai.put(string, Integer.valueOf(iMeasureText));
        return iMeasureText;
    }

    private void j() {
        float textSize = this.aa.getTextSize();
        this.aa.setTextSize(this.f120475e);
        this.y = (int) (((double) (this.aa.getFontMetrics().bottom - this.aa.getFontMetrics().top)) + 0.5d);
        this.aa.setTextSize(textSize);
    }

    private void a(String[] strArr) {
        this.ac = strArr;
        n();
    }

    private void k() {
        m();
        n();
        this.t = 0;
        this.u = this.ac.length - 1;
    }

    private void l() {
        m();
        n();
        if (this.t == -1) {
            this.t = 0;
        }
        if (this.u == -1) {
            this.u = this.ac.length - 1;
        }
        a(this.t, this.u, false);
    }

    private void m() {
        if (this.ac == null) {
            this.ac = new String[]{"0"};
        }
    }

    private void n() {
        this.Q = this.ac.length > this.q;
    }

    private void o() {
        Handler handler = this.ag;
        if (handler != null) {
            handler.removeMessages(1);
        }
    }

    public void a() {
        Scroller scroller = this.U;
        if (scroller == null || scroller.isFinished()) {
            return;
        }
        Scroller scroller2 = this.U;
        scroller2.startScroll(0, scroller2.getCurrY(), 0, 0, 1);
        this.U.abortAnimation();
        postInvalidate();
    }

    private Message h(int i) {
        return a(i, 0, 0, (Object) null);
    }

    public Message a(int i, int i2, int i3, Object obj) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = i3;
        messageObtain.obj = obj;
        return messageObtain;
    }

    private boolean a(String str, String str2) {
        if (str == null) {
            return str2 == null;
        }
        return str.equals(str2);
    }

    private int a(Context context, float f2) {
        return (int) ((f2 * context.getResources().getDisplayMetrics().scaledDensity) + 0.5f);
    }

    private int b(Context context, float f2) {
        return (int) ((f2 * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private String[] a(CharSequence[] charSequenceArr) {
        if (charSequenceArr == null) {
            return null;
        }
        String[] strArr = new String[charSequenceArr.length];
        for (int i = 0; i < charSequenceArr.length; i++) {
            strArr[i] = charSequenceArr[i].toString();
        }
        return strArr;
    }
}
