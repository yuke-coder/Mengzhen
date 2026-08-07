package com.ximalaya.ting.android.main.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewNoFocus extends RecyclerView {
    public RecyclerViewNoFocus(Context context) {
        this(context, null);
    }

    public RecyclerViewNoFocus(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RecyclerViewNoFocus(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setFocusableInTouchMode(false);
        setFocusable(false);
    }
}
