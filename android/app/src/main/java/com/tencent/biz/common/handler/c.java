package com.tencent.biz.common.handler;

import java.util.HashMap;
import java.util.Map;

/* loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes12.dex */
public class c {

    /* renamed from: a, reason: collision with root package name */
    private static Map<String, d> f1054a = new HashMap();

    public static d a(String str) {
        return b(str, c(str));
    }

    public static d b(String str, int i) {
        d dVar = f1054a.get(str);
        if (dVar == null) {
            d dVar2 = new d(str, i);
            dVar2.start();
            f1054a.put(str, dVar2);
            return dVar2;
        }
        if (dVar.isAlive()) {
            return dVar;
        }
        try {
            dVar.start();
            return dVar;
        } catch (Throwable unused) {
            return dVar;
        }
    }

    private static int c(String str) {
        if ("BackGround_HandlerThread".equalsIgnoreCase(str)) {
            return 10;
        }
        return (!"Normal_HandlerThread".equalsIgnoreCase(str) && "RealTime_HandlerThread".equalsIgnoreCase(str)) ? -2 : 0;
    }
}
