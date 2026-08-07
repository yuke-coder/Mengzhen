package TekEngineLib.State;

import android.util.Log;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekProxyLog {
    public static TekLogProxy tekLogProxy;

    public interface TekLogProxy {
        void d(String str, String str2);

        void e(String str, String str2);

        void f(String str, String str2);

        void i(String str, String str2);

        void w(String str, String str2);
    }

    public static void d(String str, String str2) {
        try {
            TekLogProxy tekLogProxy2 = tekLogProxy;
            if (tekLogProxy2 != null) {
                tekLogProxy2.d(str, str2);
            } else {
                Log.d(str, str2);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static void e(String str, String str2) {
        try {
            TekLogProxy tekLogProxy2 = tekLogProxy;
            if (tekLogProxy2 != null) {
                tekLogProxy2.e(str, str2);
            } else {
                Log.e(str, str2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void f(String str, String str2) {
        try {
            TekLogProxy tekLogProxy2 = tekLogProxy;
            if (tekLogProxy2 != null) {
                tekLogProxy2.f(str, str2);
            } else {
                Log.i(str, str2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String str, String str2) {
        try {
            TekLogProxy tekLogProxy2 = tekLogProxy;
            if (tekLogProxy2 != null) {
                tekLogProxy2.i(str, str2);
            } else {
                Log.i(str, str2);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static void setLogProxy(TekLogProxy tekLogProxy2) {
        tekLogProxy = tekLogProxy2;
    }

    public static void w(String str, String str2) {
        try {
            TekLogProxy tekLogProxy2 = tekLogProxy;
            if (tekLogProxy2 != null) {
                tekLogProxy2.w(str, str2);
            } else {
                Log.w(str, str2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
