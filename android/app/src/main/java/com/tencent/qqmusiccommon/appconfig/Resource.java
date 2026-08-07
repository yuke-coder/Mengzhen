package com.tencent.qqmusiccommon.appconfig;

import android.content.res.ColorStateList;
import android.graphics.Color;

/** Minimal host bridge for the only source string read by QQ Music WheelView. */
public final class Resource {
    private static final int PICKER_SELECTED_DESCRIPTION = 0x7f110f01;

    private Resource() {}

    public static String getString(int resourceId) {
        if (resourceId == PICKER_SELECTED_DESCRIPTION) {
            return "列表，已选中";
        }
        return "";
    }

    public static ColorStateList c(int resourceId) {
        return ColorStateList.valueOf(resourceId == 0x7f0607cf ? 0xff00cc70 : Color.BLACK);
    }
}
