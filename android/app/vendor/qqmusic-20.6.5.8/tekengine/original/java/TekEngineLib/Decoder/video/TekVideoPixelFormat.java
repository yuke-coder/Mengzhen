package TekEngineLib.Decoder.video;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekVideoPixelFormat {
    public static final int NV12 = 0;
    public static final int RGBA = 1;
    public static final int UNKNOWN = 2;

    private TekVideoPixelFormat() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String getName(int i) {
        if (i != 0) {
            return i != 1 ? "Unknown" : "RGBA";
        }
        return "NV12";
    }

    public static boolean isRGB(int i) {
        return i == 1;
    }

    public static boolean isYUV(int i) {
        return i == 0;
    }
}
