package com.tencent.qqmusic.ui.dialog;

public interface j {
    long getDialogResourceId();
    d getDialogShowExtraInfo();
    String getResourceTraceId();
    void innerShow(boolean animated);
    void setDialogResourceId(long id);
    void setDialogShowExtraInfo(d info);
    void setResourceTraceId(String id);
}
