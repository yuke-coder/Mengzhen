package TekEngineLib.Engine;

import android.util.Log;
import java.util.Map;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekNativeInterface {
    public static volatile boolean LoadLibrarySuccess;

    static {
        try {
            System.loadLibrary("TekEngineLib");
            LoadLibrarySuccess = true;
            Log.i("TEK TekNativeInterface", "System.loadLibrary TekEngineLib:" + Thread.currentThread().getName());
        } catch (Exception e) {
            Log.i("TEK TekEngineLib Load", e.getMessage());
            LoadLibrarySuccess = false;
        }
    }

    public static native void addInputImage(long j, String str);

    public static native void addInputImageRef(long j, String str, int i, int i2, int i3, int i4, int i5);

    public static native void addInputTexture(long j, int i, int i2, int i3, int i4);

    public static native void addLayer(String str, long j);

    public static native void addOneRow(long j, int i, long j2, long j3, float f);

    public static native void addOneSubRow(long j, int i, long j2, long j3, float f, int i2, int i3);

    public static native void addOneWord(long j, int i, long j2, long j3, float f, float f2);

    public static native void addOneWord2(long j, int i, long j2, long j3, float f, float f2, String str);

    public static native void addPaddingToTextureWithJNI(long j, int i, int i2);

    public static native void addScaleToTextureWithJNI(long j, float f);

    public static native void addTextScaleToTextureWithJNI(long j, float f);

    public static native void addUserDefineLayerImageRef(long j, String str, int i);

    public static native void addUserDefineLayerImageRef2(long j, String str, String str2, int i);

    public static native void addUserDefineOrderLayer(long j, int i);

    public static native void addWordLengthToTexture(long j, float f);

    public static native void addWordPositionToTexture(long j, float f, float f2);

    public static native void appendInputImage(long j, String str);

    public static native void cleanAllCache(long j);

    public static native void cleanInputImage(long j);

    public static native void cleanLyricRows(long j);

    public static native void cleanParam(long j, String str, String str2);

    public static native void cleanReuseTextureCache(long j);

    public static native void cleanTextureCache(long j);

    public static native void cleanUserDefineOrderLayers(long j);

    public static native void clearFFTData(long j);

    public static native void configAnimationRows(long j);

    public static native long createEngine();

    public static native void endInitCustom(long j);

    public static native int getContentHeight(long j);

    public static native int getContentWidth(long j);

    public static native int getEndFrame(long j);

    public static native int getFramerate(long j);

    public static native long getImageData(long j);

    public static native int getInputContainerSize(long j);

    public static native int getIsDirectRenderView(long j);

    public static native String getLayerAudio(long j, int i);

    public static native int getLayerCount(long j);

    public static native int getLayerDuration(long j, int i);

    public static native int getLayerHasKeyPoints(long j, int i);

    public static native String getLayerName(long j, int i);

    public static native int getLayerType(long j, int i);

    public static native int getOutputTexture(long j);

    public static native int getProgress(long j);

    public static native int getViewHeight(long j);

    public static native int getViewWidth(long j);

    public static native int getX(long j);

    public static native int getY(long j);

    public static native void hardVideoDecodeOnFrameAvailable(long j);

    public static native void hardVideoDecoderSetTextureId(long j, long j2);

    public static native void initTextureWithJNI(long j, int i, int i2, int i3, int i4);

    public static native int initWithJSON(long j, String str);

    public static native String[] jiebaCut(String str);

    public static native int jiebaInit(String str, String str2, String str3);

    public static native void logAddFilter(String str);

    public static native void logEnablePrint();

    public static native void logEnableWriteFile();

    public static native void logSetPath(String str);

    public static native void logWrite(String str);

    public static native void releaseEngine(long j);

    public static native void setCryptKey(long j, String str);

    public static native void setInputImageCacheSize(long j, int i);

    public static native void setIsDirectRenderView(long j, int i);

    public static native void setIsLrc(long j, int i);

    public static native void setJavaEngineDelegate(long j, Object obj);

    public static native void setKrcHeadline(long j, long j2, float f, float f2, float f3, int i, boolean z);

    public static native void setKrcLineRange(long j, int i, int i2);

    public static native void setKrcSinger(long j, long j2, float f, float f2, float f3, int i, boolean z);

    public static native void setKrcTimestampOffset(long j, int i);

    public static native void setPreludePoint(long j, long j2);

    public static native void setRenderId(long j, long j2);

    public static native void setSongId(long j, long j2);

    public static native void setUUID(long j, String str);

    public static native int startInitCustom(long j, String str, String str2);

    public static native void updateEffectFillMode(long j, int i);

    public static native void updateFFTData(long j, byte[] bArr, int i, int i2);

    public static native void updateGyroscopeData(long j, double d, double d2, double d3, double d4);

    public static native void updateParam(long j, String str, String str2, float f);

    public static native void updateParam2(long j, String str, String str2, String str3);

    public static native void updateParam3(long j, String str, String str2, int i);

    public static native void updateParam4(long j, String str, String str2, int i, int i2, int i3, int i4);

    public static native void updateParam5(long j, String str, String str2, long j2);

    public static native void updateParamMap(long j, Map<String, Object> map);

    public static native void updateRunningLayerInfo(long j);

    public static native boolean updateTextureDataWithJNI(long j, int i, int i2, int i3, byte[] bArr);

    public static native void updateUpdaterImpl(long j, String str);

    public static native int updateWithTimestamp(long j, float f);
}
