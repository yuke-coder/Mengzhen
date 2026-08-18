package com.mengzhen.app.util

/**
 * 喜马拉雅 Android 9.5.4.7 星座算法直接迁移。
 *
 * 源码对照：com.ximalaya.ting.android.main.util.ui.a.a(int, int)（classes7.dex）。
 * 该方法 switch(month+1)，按各月边界日判断星座，未命中（非法 month）返回空串。
 * 边界日序列：20/19/21/20/21/22/23/23/23/24/23/22（1-12 月）。
 *
 * 参数语义与原版一致：
 * - [month]：0 基月份（DatePicker 语义，0=1 月 … 11=12 月）
 * - [day]：当月日期（1 基）
 */
object ConstellationUtils {

    fun calculate(month: Int, day: Int): String = when (month + 1) {
        1 -> if (day >= 20) "水瓶座" else "摩羯座"
        2 -> if (day >= 19) "双鱼座" else "水瓶座"
        3 -> if (day >= 21) "白羊座" else "双鱼座"
        4 -> if (day >= 20) "金牛座" else "白羊座"
        5 -> if (day >= 21) "双子座" else "金牛座"
        6 -> if (day >= 22) "巨蟹座" else "双子座"
        7 -> if (day >= 23) "狮子座" else "巨蟹座"
        8 -> if (day >= 23) "处女座" else "狮子座"
        9 -> if (day >= 23) "天秤座" else "处女座"
        10 -> if (day >= 24) "天蝎座" else "天秤座"
        11 -> if (day >= 23) "射手座" else "天蝎座"
        12 -> if (day >= 22) "摩羯座" else "射手座"
        else -> ""
    }
}
