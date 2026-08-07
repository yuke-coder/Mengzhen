package com.tencent.qqmusic.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.AttributeSet;
import android.view.View;
import androidx.viewpager.widget.ViewPager;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;


import java.util.ArrayList;
import java.util.Iterator;

/* JADX INFO: loaded from: classes7.dex */
public class ViewPagerCircleIndicator extends View implements ViewPager.OnPageChangeListener {
    private static final int n = com.tencent.qqmusiccommon.util.j0.a(6.0f);
    Paint b;
    Bitmap d;
    Bitmap e;
    int f;
    private float g;
    private float h;
    private Context i;
    private ViewPager j;
    private ArrayList<ViewPager.OnPageChangeListener> l;
    private int m;

    public ViewPagerCircleIndicator(Context context) {
        super(context);
        this.b = new Paint();
        this.d = null;
        this.e = null;
        this.f = 1;
        this.g = 0.0f;
        this.h = 0.0f;
        this.j = null;
        this.m = n;
        b(context);
    }

    public void a(ViewPager.OnPageChangeListener onPageChangeListener) {
        ArrayList<ViewPager.OnPageChangeListener> arrayList;
        byte[] bArr = SwordSwitches.switches15;
        if ((bArr != null && ((bArr[1060] >> 1) & 1) > 0 && SwordProxy.proxyOneArg(onPageChangeListener, this, 165282).isSupported) || (arrayList = this.l) == null || arrayList.contains(onPageChangeListener)) {
            return;
        }
        this.l.add(onPageChangeListener);
    }

    public void b(Context context) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1062] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(context, this, 165299).isSupported) {
            try {
                this.i = context;
                this.d = BitmapFactory.decodeResource(context.getResources(), com.mengzhen.app.R.drawable.pager_selected_for_black);
                this.e = BitmapFactory.decodeResource(this.i.getResources(), com.mengzhen.app.R.drawable.pager_not_selected_for_black);
            } catch (Exception e) {
                android.util.Log.e("ViewPagerCircleIndicator", "decode indicator", e);
                this.d = null;
                this.e = null;
            } catch (OutOfMemoryError unused) {
                this.d = null;
                this.e = null;
            }
        }
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        Bitmap bitmap;
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1064] >> 5) & 1) <= 0 || !SwordProxy.proxyOneArg(canvas, this, 165318).isSupported) {
            super.onDraw(canvas);
            ViewPager viewPager = this.j;
            if (viewPager == null || viewPager.getAdapter().getCount() == 0) {
                return;
            }
            this.g = getWidth();
            this.h = getHeight();
            int currentItem = this.f > 1 ? this.j.getCurrentItem() % this.f : 0;
            if (this.d == null || (bitmap = this.e) == null || bitmap.isRecycled() || this.d.isRecycled()) {
                return;
            }
            float width = this.d.getWidth();
            float height = this.d.getHeight();
            float width2 = this.e.getWidth();
            float f = 1 != this.f ? (this.g - (((this.f - 1) * (this.m + width2)) + width)) * 0.5f : (this.g - width) * 0.5f;
            float f2 = (this.h - height) * 0.5f;
            for (int i = 0; i < this.f; i++) {
                if (i == currentItem) {
                    canvas.drawBitmap(this.d, (i * (this.m + width2)) + f, f2, this.b);
                } else if (i < currentItem) {
                    canvas.drawBitmap(this.e, (i * (this.m + width2)) + f, f2, this.b);
                } else if (i > currentItem) {
                    Bitmap bitmap2 = this.e;
                    int i2 = this.m;
                    canvas.drawBitmap(bitmap2, ((i - 1) * (i2 + width2)) + f + i2 + width, f2, this.b);
                }
            }
        }
    }

    @Override // androidx.viewpager.widget.ViewPager.OnPageChangeListener
    public void onPageScrollStateChanged(int i) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1071] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 165371).isSupported) {
            ArrayList<ViewPager.OnPageChangeListener> arrayList = this.l;
            if (arrayList != null) {
                Iterator<ViewPager.OnPageChangeListener> it = arrayList.iterator();
                while (it.hasNext()) {
                    it.next().onPageScrollStateChanged(i);
                }
            }
            invalidate();
        }
    }

    @Override // androidx.viewpager.widget.ViewPager.OnPageChangeListener
    public void onPageScrolled(int i, float f, int i2) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1069] >> 3) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Float.valueOf(f), Integer.valueOf(i2)}, this, 165356).isSupported) {
            ArrayList<ViewPager.OnPageChangeListener> arrayList = this.l;
            if (arrayList != null) {
                Iterator<ViewPager.OnPageChangeListener> it = arrayList.iterator();
                while (it.hasNext()) {
                    it.next().onPageScrolled(i, f, i2);
                }
            }
            invalidate();
        }
    }

    @Override // androidx.viewpager.widget.ViewPager.OnPageChangeListener
    public void onPageSelected(int i) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1069] >> 6) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 165359).isSupported) {
            ArrayList<ViewPager.OnPageChangeListener> arrayList = this.l;
            if (arrayList != null) {
                Iterator<ViewPager.OnPageChangeListener> it = arrayList.iterator();
                while (it.hasNext()) {
                    it.next().onPageSelected(i);
                }
            }
            invalidate();
        }
    }

    public void setColor(int i) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1074] >> 2) & 1) <= 0 || !SwordProxy.proxyOneArg(Integer.valueOf(i), this, 165395).isSupported) {
            this.b.setColorFilter(new PorterDuffColorFilter(i, PorterDuff.Mode.SRC_ATOP));
            invalidate();
        }
    }

    public void setCount(int i) {
        this.f = i;
    }

    public void setHorizontalSpace(int i) {
        this.m = i;
    }

    public void setImgsBitmap(Bitmap bitmap, Bitmap bitmap2) {
        this.d = bitmap;
        this.e = bitmap2;
    }

    public void setImgsResId(int i, int i2) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1072] >> 7) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}, this, 165384).isSupported) {
            try {
                this.d = BitmapFactory.decodeResource(this.i.getResources(), i);
                this.e = BitmapFactory.decodeResource(this.i.getResources(), i2);
            } catch (Exception e) {
                android.util.Log.e("ViewPagerCircleIndicator", "decode indicator", e);
            } catch (OutOfMemoryError e2) {
                android.util.Log.e("ViewPagerCircleIndicator", "decode indicator", e2);
            }
        }
    }

    public void setViewPager(ViewPager viewPager) {
        byte[] bArr = SwordSwitches.switches15;
        if (bArr == null || ((bArr[1058] >> 7) & 1) <= 0 || !SwordProxy.proxyOneArg(viewPager, this, 165272).isSupported) {
            this.j = viewPager;
            ArrayList<ViewPager.OnPageChangeListener> arrayList = new ArrayList<>();
            this.l = arrayList;
            arrayList.clear();
            viewPager.setOnPageChangeListener(this);
            invalidate();
        }
    }

    public ViewPagerCircleIndicator(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.b = new Paint();
        this.d = null;
        this.e = null;
        this.f = 1;
        this.g = 0.0f;
        this.h = 0.0f;
        this.j = null;
        this.m = n;
        b(context);
    }

    public ViewPagerCircleIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.b = new Paint();
        this.d = null;
        this.e = null;
        this.f = 1;
        this.g = 0.0f;
        this.h = 0.0f;
        this.j = null;
        this.m = n;
        b(context);
    }
}

