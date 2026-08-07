package com.tencent.qqmusic.ui;

public interface OnSurfaceChangeListener {
    void onDrawFrame();
    void onSurfaceChanged(int width, int height);
    void onSurfaceCreated();
}
