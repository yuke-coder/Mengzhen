package com.tencent.qqmusic.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

public class ModelDialog extends Dialog implements j {
    private long resourceId;
    private d extraInfo;
    private String traceId;

    public ModelDialog(Context context) {
        super(context);
    }

    public ModelDialog(Context context, int theme) {
        super(context, theme);
    }

    public ModelDialog(Context context, boolean cancelable, DialogInterface.OnCancelListener listener) {
        super(context, cancelable, listener);
    }

    @Override public long getDialogResourceId() { return resourceId; }
    @Override public d getDialogShowExtraInfo() { return extraInfo; }
    @Override public String getResourceTraceId() { return traceId; }
    @Override public void innerShow(boolean animated) { super.show(); }
    @Override public void setDialogResourceId(long id) { resourceId = id; }
    @Override public void setDialogShowExtraInfo(d info) { extraInfo = info; }
    @Override public void setResourceTraceId(String id) { traceId = id; }
}
