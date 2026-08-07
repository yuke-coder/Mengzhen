package com.ximalaya.ting.android.main.view;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class LinearItemDecoration extends RecyclerView.ItemDecoration {

    private int f120397a;

    private int f120398b;

    public LinearItemDecoration() {
    }

    public LinearItemDecoration(int i, int i2) {
        this.f120397a = i / 2;
        this.f120398b = i2;
    }

    public void a(int i) {
        this.f120397a = i;
    }

    public void b(int i) {
        this.f120398b = i;
    }

    @Override
    public void getItemOffsets(Rect rect, View view, RecyclerView recyclerView, RecyclerView.State state) {
        super.getItemOffsets(rect, view, recyclerView, state);
        int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
        rect.left = this.f120397a;
        rect.right = this.f120397a;
        if (childAdapterPosition == 0) {
            rect.left = this.f120398b;
        } else if (childAdapterPosition == recyclerView.getAdapter().getItemCount() - 1) {
            rect.right = this.f120398b;
        }
    }
}
