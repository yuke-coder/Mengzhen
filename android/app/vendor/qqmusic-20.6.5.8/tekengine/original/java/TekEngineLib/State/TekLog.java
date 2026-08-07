package TekEngineLib.State;

import TekEngineLib.Engine.TekNativeInterface;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekLog {
    private static boolean _isEnablePrint = true;
    private static final Map<String, Long> _logTimeMap = new ConcurrentHashMap();

    public static void addFilter(String str) {
        TekNativeInterface.logAddFilter(str);
    }

    public static void enablePrint() {
        _isEnablePrint = true;
        TekNativeInterface.logEnablePrint();
    }

    public static void enableWriteFile() {
        TekNativeInterface.logEnableWriteFile();
    }

    public static boolean isEnablePrint() {
        return _isEnablePrint;
    }

    public static void setPath(String str) {
        TekNativeInterface.logSetPath(str);
    }

    public static void write(String str, String str2) {
        if (_isEnablePrint) {
            TekNativeInterface.logWrite(str + ":" + str2);
        }
    }

    public static void write(String str, String str2, String str3) {
        if (_isEnablePrint) {
            TekNativeInterface.logWrite(str + ":" + str2 + ".  " + str3);
        }
    }
}
