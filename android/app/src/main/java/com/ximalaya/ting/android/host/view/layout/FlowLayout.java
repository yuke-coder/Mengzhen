package com.ximalaya.ting.android.host.view.layout;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import androidx.core.internal.view.SupportMenu;
import androidx.core.view.GravityCompat;
import androidx.core.view.InputDeviceCompat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes14.dex */
public class FlowLayout extends ViewGroup {

    /* renamed from: a, reason: collision with root package name */
    List<b> f52847a;

    /* renamed from: b, reason: collision with root package name */
    private final LayoutConfiguration f52848b;

    /* renamed from: c, reason: collision with root package name */
    private int f52849c;

    /* renamed from: d, reason: collision with root package name */
    private int f52850d;

    /* renamed from: e, reason: collision with root package name */
    private a f52851e;

    /* renamed from: f, reason: collision with root package name */
    private boolean f52852f;
    private boolean g;
    private boolean h;

    public interface a {
        void a(int i, View view, b bVar);

        boolean a(int i, b bVar, int i2);
    }

    public FlowLayout(Context context) {
        super(context);
        this.f52847a = new ArrayList();
        this.f52850d = Integer.MAX_VALUE;
        this.f52852f = false;
        this.f52848b = new LayoutConfiguration(context, null);
    }

    public FlowLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.f52847a = new ArrayList();
        this.f52850d = Integer.MAX_VALUE;
        this.f52852f = false;
        this.f52848b = new LayoutConfiguration(context, attributeSet);
    }

    public FlowLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.f52847a = new ArrayList();
        this.f52850d = Integer.MAX_VALUE;
        this.f52852f = false;
        this.f52848b = new LayoutConfiguration(context, attributeSet);
    }

    public void setLine(int i) {
        this.f52850d = i;
    }

    public int getLine() {
        return this.f52850d;
    }

    public int getValideViewNum() {
        return this.f52849c;
    }

    public void setFLowListener(a aVar) {
        this.f52851e = aVar;
    }

    public void setNeedDelete(boolean z) {
        this.f52852f = z;
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        int i3;
        int i4;
        int measuredWidth;
        a aVar;
        int size = (View.MeasureSpec.getSize(i) - getPaddingRight()) - getPaddingLeft();
        int size2 = (View.MeasureSpec.getSize(i2) - getPaddingTop()) - getPaddingBottom();
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int i5 = this.f52848b.a() == 0 ? size : size2;
        if (this.f52848b.a() == 0) {
            size = size2;
        }
        if (this.f52848b.a() != 0) {
            mode = mode2;
        }
        this.f52848b.a();
        this.f52847a.clear();
        int iMax = 0;
        this.f52849c = 0;
        b bVar = new b(i5, this.f52848b);
        this.f52847a.add(bVar);
        int childCount = getChildCount();
        int i6 = 0;
        while (true) {
            if (i6 >= childCount) {
                break;
            }
            View childAt = getChildAt(i6);
            if (childAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                childAt.measure(getChildMeasureSpec(i, getPaddingLeft() + getPaddingRight(), layoutParams.width), getChildMeasureSpec(i2, getPaddingTop() + getPaddingBottom(), layoutParams.height));
                layoutParams.e(this.f52848b.a());
                if (this.f52848b.a() == 0) {
                    layoutParams.b(childAt.getMeasuredWidth());
                    measuredWidth = childAt.getMeasuredHeight();
                } else {
                    layoutParams.b(childAt.getMeasuredHeight());
                    measuredWidth = childAt.getMeasuredWidth();
                }
                layoutParams.c(measuredWidth);
                boolean z = layoutParams.f52858a || !(mode == 0 || bVar.b(childAt));
                if (z) {
                    if (this.f52847a.size() < this.f52850d) {
                        bVar = new b(i5, this.f52848b);
                        if (this.f52848b.a() == 1 && this.f52848b.e() == 1) {
                            this.f52847a.add(0, bVar);
                        } else {
                            this.f52847a.add(bVar);
                        }
                        if (z && (aVar = this.f52851e) != null && aVar.a(this.f52847a.size(), bVar, i6)) {
                            break;
                        }
                    } else {
                        a aVar2 = this.f52851e;
                        if (aVar2 != null) {
                            aVar2.a(i6, childAt, bVar);
                        }
                        if (this.f52850d == 1 && this.f52852f) {
                            removeView(childAt);
                        }
                    }
                }
                if (this.f52848b.a() == 0 && this.f52848b.e() == 1) {
                    bVar.a(0, childAt);
                } else {
                    bVar.a(childAt);
                }
            }
            i6++;
        }
        a(this.f52847a);
        Iterator<b> it = this.f52847a.iterator();
        while (it.hasNext()) {
            iMax = Math.max(iMax, it.next().e());
        }
        int iC = bVar.c() + bVar.d();
        a(this.f52847a, a(mode, i5, iMax), a(mode2, size, iC));
        for (b bVar2 : this.f52847a) {
            b(bVar2);
            a(bVar2);
        }
        int paddingLeft = getPaddingLeft() + getPaddingRight();
        int paddingBottom = getPaddingBottom() + getPaddingTop();
        if (this.f52848b.a() == 0) {
            i3 = paddingLeft + iMax;
            i4 = paddingBottom + iC;
        } else {
            i3 = paddingLeft + iC;
            i4 = paddingBottom + iMax;
        }
        Iterator<b> it2 = this.f52847a.iterator();
        while (it2.hasNext()) {
            this.f52849c += it2.next().g().size();
        }
        setMeasuredDimension(resolveSize(i3, i), resolveSize(i4, i2));
    }

    private int a(int i, int i2, int i3) {
        if (i != Integer.MIN_VALUE) {
            return i != 1073741824 ? i3 : i2;
        }
        return Math.min(i3, i2);
    }

    private void a(List<b> list) {
        int iD = 0;
        for (b bVar : list) {
            bVar.c(iD);
            iD += bVar.d();
            Iterator<View> it = bVar.g().iterator();
            int iD2 = 0;
            while (it.hasNext()) {
                LayoutParams layoutParams = (LayoutParams) it.next().getLayoutParams();
                layoutParams.a(iD2);
                iD2 += layoutParams.d() + layoutParams.g();
            }
        }
    }

    private void a(b bVar) {
        int iMakeMeasureSpec;
        int iD;
        for (View view : bVar.g()) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            if (this.f52848b.a() == 0) {
                layoutParams.a(getPaddingLeft() + bVar.f() + layoutParams.c(), getPaddingTop() + bVar.c() + layoutParams.f());
                iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(layoutParams.d(), 1073741824);
                iD = layoutParams.e();
            } else {
                layoutParams.a(getPaddingLeft() + bVar.c() + layoutParams.f(), getPaddingTop() + bVar.f() + layoutParams.c());
                iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(layoutParams.e(), 1073741824);
                iD = layoutParams.d();
            }
            view.measure(iMakeMeasureSpec, View.MeasureSpec.makeMeasureSpec(iD, 1073741824));
        }
    }

    private void a(List<b> list, int i, int i2) {
        int size = list.size();
        if (size <= 0) {
            return;
        }
        b bVar = list.get(size - 1);
        int iD = i2 - (bVar.d() + bVar.c());
        int i3 = 0;
        for (b bVar2 : list) {
            int gravity = getGravity();
            int iRound = Math.round((iD * 1) / size);
            int iE = bVar2.e();
            int iD2 = bVar2.d();
            Rect rect = new Rect();
            rect.top = i3;
            rect.left = 0;
            rect.right = i;
            rect.bottom = iD2 + iRound + i3;
            Rect rect2 = new Rect();
            Gravity.apply(gravity, iE, iD2, rect, rect2);
            i3 += iRound;
            bVar2.d(rect2.left);
            bVar2.c(rect2.top);
            bVar2.b(rect2.width());
            bVar2.a(rect2.height());
        }
    }

    private void b(b bVar) {
        int size = bVar.g().size();
        if (size <= 0) {
            return;
        }
        float fB = 0.0f;
        Iterator<View> it = bVar.g().iterator();
        while (it.hasNext()) {
            fB += b((LayoutParams) it.next().getLayoutParams());
        }
        LayoutParams layoutParams = (LayoutParams) bVar.g().get(size - 1).getLayoutParams();
        int iE = bVar.e() - (layoutParams.d() + layoutParams.c());
        Iterator<View> it2 = bVar.g().iterator();
        int i = 0;
        while (it2.hasNext()) {
            LayoutParams layoutParams2 = (LayoutParams) it2.next().getLayoutParams();
            float fB2 = b(layoutParams2);
            int iA = a(layoutParams2);
            int iRound = Math.round((iE * fB2) / fB);
            int iD = layoutParams2.d() + layoutParams2.g();
            int iE2 = layoutParams2.e() + layoutParams2.h();
            Rect rect = new Rect();
            rect.top = 0;
            rect.left = i;
            rect.right = iD + iRound + i;
            rect.bottom = bVar.d();
            Rect rect2 = new Rect();
            Gravity.apply(iA, iD, iE2, rect, rect2);
            i += iRound;
            layoutParams2.a(rect2.left + layoutParams2.c());
            layoutParams2.d(rect2.top);
            layoutParams2.b(rect2.width() - layoutParams2.g());
            layoutParams2.c(rect2.height() - layoutParams2.h());
        }
    }

    public List<b> getCurrentLines() {
        return this.f52847a;
    }

    private int a(LayoutParams layoutParams) {
        return layoutParams.a() ? layoutParams.f52859b : this.f52848b.d();
    }

    private float b(LayoutParams layoutParams) {
        return layoutParams.b() ? layoutParams.f52860c : this.f52848b.c();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int childCount = getChildCount();
        for (int i5 = 0; i5 < childCount && i5 < this.f52849c; i5++) {
            View childAt = getChildAt(i5);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            childAt.layout(layoutParams.j + layoutParams.leftMargin, layoutParams.k + layoutParams.topMargin, layoutParams.j + layoutParams.leftMargin + childAt.getMeasuredWidth(), layoutParams.k + layoutParams.topMargin + childAt.getMeasuredHeight());
        }
        if (this.f52849c >= childCount || !b()) {
            return;
        }
        for (int i6 = this.f52849c; i6 < childCount; i6++) {
            View childAt2 = getChildAt(i6);
            if (childAt2 != null) {
                childAt2.layout(0, 0, 0, 0);
            }
        }
    }

    private boolean b() {
        if (this.h) {
            return this.g;
        }
        boolean zA = true;
        this.g = zA;
        this.h = true;
        return zA;
    }

    @Override // android.view.ViewGroup
    protected boolean drawChild(Canvas canvas, View view, long j) {
        boolean zDrawChild = super.drawChild(canvas, view, j);
        a(canvas, view);
        return zDrawChild;
    }

    @Override // android.view.ViewGroup
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.ViewGroup
    /* renamed from: a, reason: merged with bridge method [inline-methods] */
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override // android.view.ViewGroup
    /* renamed from: a, reason: merged with bridge method [inline-methods] */
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.ViewGroup
    /* renamed from: a, reason: merged with bridge method [inline-methods] */
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    private void a(Canvas canvas, View view) {
        float top;
        float f2;
        float left;
        Canvas canvas2;
        float f3;
        if (this.f52848b.b()) {
            Paint paintA = a(InputDeviceCompat.SOURCE_ANY);
            Paint paintA2 = a(SupportMenu.CATEGORY_MASK);
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            if (layoutParams.rightMargin > 0) {
                float right = view.getRight();
                float top2 = view.getTop() + (view.getHeight() / 2.0f);
                canvas.drawLine(right, top2, right + layoutParams.rightMargin, top2, paintA);
                canvas.drawLine((layoutParams.rightMargin + right) - 4.0f, top2 - 4.0f, right + layoutParams.rightMargin, top2, paintA);
                canvas.drawLine((layoutParams.rightMargin + right) - 4.0f, top2 + 4.0f, right + layoutParams.rightMargin, top2, paintA);
            }
            if (layoutParams.leftMargin > 0) {
                float left2 = view.getLeft();
                float top3 = view.getTop() + (view.getHeight() / 2.0f);
                canvas.drawLine(left2, top3, left2 - layoutParams.leftMargin, top3, paintA);
                canvas.drawLine((left2 - layoutParams.leftMargin) + 4.0f, top3 - 4.0f, left2 - layoutParams.leftMargin, top3, paintA);
                canvas.drawLine((left2 - layoutParams.leftMargin) + 4.0f, top3 + 4.0f, left2 - layoutParams.leftMargin, top3, paintA);
            }
            if (layoutParams.bottomMargin > 0) {
                float left3 = view.getLeft() + (view.getWidth() / 2.0f);
                float bottom = view.getBottom();
                canvas.drawLine(left3, bottom, left3, bottom + layoutParams.bottomMargin, paintA);
                canvas.drawLine(left3 - 4.0f, (layoutParams.bottomMargin + bottom) - 4.0f, left3, bottom + layoutParams.bottomMargin, paintA);
                canvas.drawLine(left3 + 4.0f, (layoutParams.bottomMargin + bottom) - 4.0f, left3, bottom + layoutParams.bottomMargin, paintA);
            }
            if (layoutParams.topMargin > 0) {
                float left4 = view.getLeft() + (view.getWidth() / 2.0f);
                float top4 = view.getTop();
                canvas.drawLine(left4, top4, left4, top4 - layoutParams.topMargin, paintA);
                canvas.drawLine(left4 - 4.0f, (top4 - layoutParams.topMargin) + 4.0f, left4, top4 - layoutParams.topMargin, paintA);
                canvas.drawLine(left4 + 4.0f, (top4 - layoutParams.topMargin) + 4.0f, left4, top4 - layoutParams.topMargin, paintA);
            }
            if (layoutParams.f52858a) {
                if (this.f52848b.a() == 0) {
                    left = view.getLeft();
                    float top5 = view.getTop() + (view.getHeight() / 2.0f);
                    f3 = top5 - 6.0f;
                    top = top5 + 6.0f;
                    canvas2 = canvas;
                    f2 = left;
                } else {
                    float left5 = view.getLeft() + (view.getWidth() / 2.0f);
                    top = view.getTop();
                    f2 = left5 - 6.0f;
                    left = left5 + 6.0f;
                    canvas2 = canvas;
                    f3 = top;
                }
                canvas2.drawLine(f2, f3, left, top, paintA2);
            }
        }
    }

    private Paint a(int i) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(i);
        paint.setStrokeWidth(2.0f);
        return paint;
    }

    public int getOrientation() {
        return this.f52848b.a();
    }

    public void setOrientation(int i) {
        this.f52848b.a(i);
        requestLayout();
    }

    public void setDebugDraw(boolean z) {
        this.f52848b.a(z);
        invalidate();
    }

    public float getWeightDefault() {
        return this.f52848b.c();
    }

    public void setWeightDefault(float f2) {
        this.f52848b.a(f2);
        requestLayout();
    }

    public int getGravity() {
        return this.f52848b.d();
    }

    public void setGravity(int i) {
        this.f52848b.b(i);
        requestLayout();
    }

    @Override // android.view.View, android.view.ViewParent
    public int getLayoutDirection() {
        LayoutConfiguration layoutConfiguration = this.f52848b;
        if (layoutConfiguration == null) {
            return 0;
        }
        return layoutConfiguration.e();
    }

    @Override // android.view.View
    public void setLayoutDirection(int i) {
        this.f52848b.c(i);
        requestLayout();
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /* renamed from: a, reason: collision with root package name */
        public boolean f52858a;

        /* renamed from: b, reason: collision with root package name */
        @ViewDebug.ExportedProperty
        public int f52859b;

        /* renamed from: c, reason: collision with root package name */
        public float f52860c;

        /* renamed from: d, reason: collision with root package name */
        private int f52861d;

        /* renamed from: e, reason: collision with root package name */
        private int f52862e;

        /* renamed from: f, reason: collision with root package name */
        private int f52863f;
        private int g;
        private int h;
        private int i;
        private int j;
        private int k;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.f52858a = false;
            this.f52859b = 0;
            this.f52860c = -1.0f;
            a(context, attributeSet);
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
            this.f52858a = false;
            this.f52859b = 0;
            this.f52860c = -1.0f;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.f52858a = false;
            this.f52859b = 0;
            this.f52860c = -1.0f;
        }

        public boolean a() {
            return this.f52859b != 0;
        }

        public boolean b() {
            return this.f52860c >= 0.0f;
        }

        private void a(Context context, AttributeSet attributeSet) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, new int[]{R.attr.layout_gravity});
            try {
                this.f52858a = false;
                this.f52859b = typedArrayObtainStyledAttributes.getInt(0, 0);
                this.f52860c = -1.0f;
            } finally {
                typedArrayObtainStyledAttributes.recycle();
            }
        }

        void a(int i, int i2) {
            this.j = i;
            this.k = i2;
        }

        int c() {
            return this.f52863f;
        }

        void a(int i) {
            this.f52863f = i;
        }

        int d() {
            return this.g;
        }

        void b(int i) {
            this.g = i;
        }

        int e() {
            return this.h;
        }

        void c(int i) {
            this.h = i;
        }

        int f() {
            return this.i;
        }

        void d(int i) {
            this.i = i;
        }

        int g() {
            return this.f52861d;
        }

        int h() {
            return this.f52862e;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void e(int i) {
            int i2;
            int i3;
            if (i == 0) {
                this.f52861d = this.leftMargin + this.rightMargin;
                i2 = this.topMargin;
                i3 = this.bottomMargin;
            } else {
                this.f52861d = this.topMargin + this.bottomMargin;
                i2 = this.leftMargin;
                i3 = this.rightMargin;
            }
            this.f52862e = i2 + i3;
        }

        public void a(int i, View view) {
            int measuredWidth;
            if (view != null) {
                e(i);
                if (i == 0) {
                    b(view.getMeasuredWidth());
                    measuredWidth = view.getMeasuredHeight();
                } else {
                    b(view.getMeasuredHeight());
                    measuredWidth = view.getMeasuredWidth();
                }
                c(measuredWidth);
            }
        }
    }

    public static class b {

        /* renamed from: b, reason: collision with root package name */
        private final LayoutConfiguration f52865b;

        /* renamed from: c, reason: collision with root package name */
        private final int f52866c;

        /* renamed from: d, reason: collision with root package name */
        private int f52867d;

        /* renamed from: e, reason: collision with root package name */
        private int f52868e;

        /* renamed from: f, reason: collision with root package name */
        private int f52869f;
        private int g;
        private int i;
        private int j;

        /* renamed from: a, reason: collision with root package name */
        private final List<View> f52864a = new ArrayList();
        private int h = 0;
        private int k = 0;

        public b(int i, LayoutConfiguration layoutConfiguration) {
            this.f52866c = i;
            this.f52865b = layoutConfiguration;
        }

        public void a(View view) {
            a(this.f52864a.size(), view);
        }

        public LayoutConfiguration a() {
            return this.f52865b;
        }

        public void a(int i, View view) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            this.f52864a.add(i, view);
            int iD = this.f52869f + layoutParams.d();
            this.f52867d = iD;
            this.f52869f = iD + layoutParams.g();
            this.j = this.f52868e;
            int i2 = this.g;
            this.i = i2;
            this.g = Math.max(i2, layoutParams.e() + layoutParams.h());
            this.f52868e = Math.max(this.f52868e, layoutParams.e());
        }

        public boolean b(View view) {
            return this.f52869f + (this.f52865b.a() == 0 ? view.getMeasuredWidth() : view.getMeasuredHeight()) <= this.f52866c;
        }

        public int b() {
            return this.f52866c - this.f52869f;
        }

        public int c() {
            return this.h;
        }

        public int d() {
            return this.g;
        }

        public int e() {
            return this.f52867d;
        }

        public int f() {
            return this.k;
        }

        public List<View> g() {
            return this.f52864a;
        }

        public void a(int i) {
            int i2 = this.g - this.f52868e;
            this.g = i;
            this.f52868e = i - i2;
        }

        public void b(int i) {
            int i2 = this.f52869f - this.f52867d;
            this.f52867d = i;
            this.f52869f = i + i2;
        }

        public void c(int i) {
            this.h += i;
        }

        public void d(int i) {
            this.k += i;
        }

        public View e(int i) {
            LayoutParams layoutParams;
            if (i < 0 || this.f52864a.size() <= i) {
                return null;
            }
            View viewRemove = this.f52864a.remove(i);
            if (viewRemove != null && (layoutParams = (LayoutParams) viewRemove.getLayoutParams()) != null) {
                int iD = this.f52869f - layoutParams.d();
                this.f52867d = iD;
                this.f52869f = iD - layoutParams.g();
                if (layoutParams.e() + layoutParams.h() == this.g) {
                    this.g = this.i;
                }
                if (this.f52868e == layoutParams.e()) {
                    this.f52868e = this.j;
                }
            }
            return viewRemove;
        }
    }

    public static class LayoutConfiguration {

        /* renamed from: a, reason: collision with root package name */
        private int f52853a = 0;

        /* renamed from: b, reason: collision with root package name */
        private boolean f52854b = false;

        /* renamed from: c, reason: collision with root package name */
        private float f52855c = 0.0f;

        /* renamed from: d, reason: collision with root package name */
        private int f52856d = 8388659;

        /* renamed from: e, reason: collision with root package name */
        private int f52857e = 0;

        public LayoutConfiguration(Context context, AttributeSet attributeSet) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, new int[]{R.attr.gravity, R.attr.orientation});
            try {
                a(typedArrayObtainStyledAttributes.getInteger(1, 0));
                a(false);
                a(0.0f);
                b(typedArrayObtainStyledAttributes.getInteger(0, 0));
                c(0);
            } finally {
                typedArrayObtainStyledAttributes.recycle();
            }
        }

        public int a() {
            return this.f52853a;
        }

        public void a(int i) {
            if (i != 1) {
                i = 0;
            }
            this.f52853a = i;
        }

        public boolean b() {
            return this.f52854b;
        }

        public void a(boolean z) {
            this.f52854b = z;
        }

        public float c() {
            return this.f52855c;
        }

        public void a(float f2) {
            this.f52855c = Math.max(0.0f, f2);
        }

        public int d() {
            return this.f52856d;
        }

        public void b(int i) {
            if ((i & 7) == 0) {
                i |= GravityCompat.START;
            }
            if ((i & 112) == 0) {
                i |= 48;
            }
            this.f52856d = i;
        }

        public int e() {
            return this.f52857e;
        }

        public void c(int i) {
            if (i != 1) {
                i = 0;
            }
            this.f52857e = i;
        }
    }
}
