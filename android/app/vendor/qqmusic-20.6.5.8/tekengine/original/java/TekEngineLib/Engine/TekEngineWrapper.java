package TekEngineLib.Engine;

import TekEngineLib.Interface.UsualParamInfo;
import TekEngineLib.Lyric.TekAndroidLyricCache;
import TekEngineLib.Lyric.TekFontParam;
import TekEngineLib.Lyric.TekLyricParam;
import TekEngineLib.Lyric.TekLyricParser;
import TekEngineLib.Lyric.TekLyricRowParseResult;
import TekEngineLib.Lyric.TekLyricSubRowParseResult;
import TekEngineLib.Lyric.TekLyricWordParseResult;
import TekEngineLib.Lyric.TekTextParam;
import TekEngineLib.Render.TekBaseShader;
import TekEngineLib.State.TekAdditionImageRef;
import TekEngineLib.State.TekCustomLayer;
import TekEngineLib.State.TekEffectConfig;
import TekEngineLib.State.TekEffectLayerType;
import TekEngineLib.State.TekErrorCode;
import TekEngineLib.State.TekLog;
import TekEngineLib.State.TekProxyLog;
import android.graphics.Bitmap;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Map;
import okhttp3.HttpUrl;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekEngineWrapper {
    private static final String LOGTAG = "TEK TekEngineWrapper";
    public static int TEK_EFFECT_FILL_MODE_ASPECT_FILL = 0;
    public static int TEK_EFFECT_FILL_MODE_ASPECT_FIT = 2;
    public static int TEK_EFFECT_FILL_MODE_CENTER = 3;
    public static int TEK_EFFECT_FILL_MODE_NONE = 4;
    public static int TEK_EFFECT_FILL_MODE_SCALE_TO_FIT = 1;
    private long _nativePointer;

    public TekEngineWrapper() {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "construct.");
            this._nativePointer = TekNativeInterface.createEngine();
        }
    }

    public void addInputImage(String str) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "addInputImage:" + str);
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "addInputImage _nativePointer == 0");
            } else {
                TekNativeInterface.addInputImage(j, str);
            }
        }
    }

    public void addInputImageRefs(ArrayList<TekAdditionImageRef> arrayList) {
        String str;
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "addInputImageRefs");
            long j = this._nativePointer;
            if (j == 0) {
                str = "addInputImageRefs _nativePointer == 0";
            } else {
                TekNativeInterface.cleanInputImage(j);
                if (arrayList != null) {
                    for (int i = 0; i < arrayList.size(); i++) {
                        TekAdditionImageRef tekAdditionImageRef = arrayList.get(i);
                        TekNativeInterface.addInputImageRef(this._nativePointer, tekAdditionImageRef._imagePath, tekAdditionImageRef._fillMode.ordinal(), tekAdditionImageRef._x, tekAdditionImageRef._y, tekAdditionImageRef._width, tekAdditionImageRef._height);
                    }
                    return;
                }
                str = "addInputImageRefs inputImageRef == null";
            }
            TekLog.write(LOGTAG, str);
        }
    }

    public void addInputImages(ArrayList<String> arrayList) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "addInputImages");
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "addInputImages _nativePointer == 0");
                return;
            }
            TekNativeInterface.cleanInputImage(j);
            for (int i = 0; i < arrayList.size(); i++) {
                TekNativeInterface.addInputImage(this._nativePointer, arrayList.get(i));
            }
        }
    }

    public void addInputTexture(int i, int i2, int i3, int i4) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "addInputTexture:" + i + ":" + i2 + ":" + i3 + ":" + i4);
            long j = this._nativePointer;
            if (j != 0) {
                TekNativeInterface.addInputTexture(j, i, i2, i3, i4);
                return;
            }
            TekLog.write(LOGTAG, "addInputTexture _nativePointer == 0:" + i + ":" + i2 + ":" + i3 + ":" + i4);
        }
    }

    public void addLayer(String str) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "addLayer:" + str);
            long j = this._nativePointer;
            if (j != 0) {
                TekNativeInterface.addLayer(str, j);
                return;
            }
            TekLog.write(LOGTAG, "addLayer:" + str);
        }
    }

    public void addUserDefineLayerImageRef(String str, int i) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "addUserDefineLayerImageRef:" + str + ":" + i);
            long j = this._nativePointer;
            if (j != 0) {
                TekNativeInterface.addUserDefineLayerImageRef(j, str, i);
                return;
            }
            TekLog.write(LOGTAG, "addUserDefineLayerImageRef _nativePointer == 0:" + str + ":" + i);
        }
    }

    public void addUserDefineLayerImageRef2(String str, String str2, int i) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "addUserDefineLayerImageRef2:" + str + ":" + str2 + ":" + i);
            long j = this._nativePointer;
            if (j != 0) {
                TekNativeInterface.addUserDefineLayerImageRef2(j, str, str2, i);
                return;
            }
            TekLog.write(LOGTAG, "addUserDefineLayerImageRef2 _nativePointer == 0:" + str + ":" + str2 + ":" + i);
        }
    }

    public void addUserDefineOrderLayer(int i) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "addUserDefineOrderLayer:" + i);
            long j = this._nativePointer;
            if (j != 0) {
                TekNativeInterface.addUserDefineOrderLayer(j, i);
                return;
            }
            TekLog.write(LOGTAG, "addUserDefineOrderLayer _nativePointer == 0:" + i);
        }
    }

    public void appendInputImage(String str) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "appendInputImage:" + str);
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "appendInputImage _nativePointer == 0");
            } else {
                TekNativeInterface.appendInputImage(j, str);
            }
        }
    }

    public void cleanAllCache() {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekProxyLog.i(LOGTAG, "cleanAllCache");
            long j = this._nativePointer;
            if (j == 0) {
                TekProxyLog.e(LOGTAG, "_nativePointer == 0");
            } else {
                TekNativeInterface.cleanAllCache(j);
            }
        }
    }

    public void cleanInputImage() {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "cleanInputImage");
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "cleanInputImage _nativePointer == 0");
            } else {
                TekNativeInterface.cleanInputImage(j);
            }
        }
    }

    public void cleanLyric() {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "cleanLyric");
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "_nativePointer == 0");
                return;
            }
            TekNativeInterface.cleanLyricRows(j);
            TekNativeInterface.cleanReuseTextureCache(this._nativePointer);
            TekAndroidLyricCache.getInstance().clean(Long.valueOf(this._nativePointer));
        }
    }

    public void cleanParam(String str, String str2) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "cleanParam:");
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "cleanParam: _nativePointer == 0");
            } else {
                TekNativeInterface.cleanParam(j, str, str2);
            }
        }
    }

    public void cleanTextureCache() {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "cleanTextureCache");
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "_nativePointer == 0");
            } else {
                TekNativeInterface.cleanTextureCache(j);
            }
        }
    }

    public void cleanUserDefineOrderLayers() {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "cleanUserDefineOrderLayers");
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "cleanUserDefineOrderLayers _nativePointer == 0");
            } else {
                TekNativeInterface.cleanUserDefineOrderLayers(j);
            }
        }
    }

    public void configAnimationRows() {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "configAnimationRows");
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "configAnimationRows _nativePointer == 0");
            } else {
                TekNativeInterface.configAnimationRows(j);
            }
        }
    }

    public void destory() {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekProxyLog.i(LOGTAG, "destory.");
            if (this._nativePointer != 0) {
                cleanAllCache();
                TekNativeInterface.releaseEngine(this._nativePointer);
                TekAndroidLyricCache.getInstance().clean(Long.valueOf(this._nativePointer));
                this._nativePointer = 0L;
            }
        }
    }

    public int getContentHeight() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getContentHeight(j);
        }
        TekLog.write(LOGTAG, "getContentHeight _nativePointer == 0:");
        return 0;
    }

    public int getContentWidth() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getContentWidth(j);
        }
        TekLog.write(LOGTAG, "getContentWidth _nativePointer == 0:");
        return 0;
    }

    public int getEndFrame() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j == 0) {
            TekLog.write(LOGTAG, "getEndFrame _nativePointer == 0:");
            return 0;
        }
        int endFrame = TekNativeInterface.getEndFrame(j);
        TekLog.write(LOGTAG, "getEndFrame:" + endFrame);
        return endFrame;
    }

    public int getFramerate() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getFramerate(j);
        }
        TekLog.write(LOGTAG, "getFramerate _nativePointer == 0:");
        return 0;
    }

    public long getImageData() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0L;
        }
        TekLog.write(LOGTAG, "getImageData");
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getImageData(j);
        }
        TekLog.write(LOGTAG, "getImageData _nativePointer == 0");
        return 0L;
    }

    public int getInputContainerSize() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j == 0) {
            TekLog.write(LOGTAG, "getInputContainerSize _nativePointer == 0:");
            return 0;
        }
        int inputContainerSize = TekNativeInterface.getInputContainerSize(j);
        TekLog.write(LOGTAG, "getInputContainerSize:" + inputContainerSize);
        return inputContainerSize;
    }

    public boolean getIsDirectRenderView() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return false;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getIsDirectRenderView(j) == 1;
        }
        TekLog.write(LOGTAG, "getIsDirectRenderView _nativePointer == 0:");
        return false;
    }

    public String getLayeAudio(int i) {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return HttpUrl.FRAGMENT_ENCODE_SET;
        }
        long j = this._nativePointer;
        return j == 0 ? HttpUrl.FRAGMENT_ENCODE_SET : TekNativeInterface.getLayerAudio(j, i);
    }

    public int getLayerCount() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j == 0) {
            TekLog.write(LOGTAG, "getLayerCount _nativePointer == 0");
            return 0;
        }
        int layerCount = TekNativeInterface.getLayerCount(j);
        TekLog.write(LOGTAG, "getLayerCount:" + layerCount);
        return layerCount;
    }

    public int getLayerDuration(int i) {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j == 0) {
            TekLog.write(LOGTAG, "getLayerDuration:");
            return 0;
        }
        int layerDuration = TekNativeInterface.getLayerDuration(j, i);
        TekLog.write(LOGTAG, "getLayerDuration:" + layerDuration);
        return layerDuration;
    }

    public boolean getLayerHasKeyPoints(int i) {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return false;
        }
        long j = this._nativePointer;
        return j != 0 && TekNativeInterface.getLayerHasKeyPoints(j, i) > 0;
    }

    public String getLayerName(int i) {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return HttpUrl.FRAGMENT_ENCODE_SET;
        }
        long j = this._nativePointer;
        return j == 0 ? HttpUrl.FRAGMENT_ENCODE_SET : TekNativeInterface.getLayerName(j, i);
    }

    public TekEffectLayerType getLayerType(int i) {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return TekEffectLayerType.TEK_INFO_LAYER_TYPE_COVER;
        }
        long j = this._nativePointer;
        if (j == 0) {
            return TekEffectLayerType.TEK_INFO_LAYER_TYPE_COVER;
        }
        return TekEffectLayerType.values()[TekNativeInterface.getLayerType(j, i)];
    }

    public long getNativePointer() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0L;
        }
        TekLog.write(LOGTAG, "getNativePointer");
        long j = this._nativePointer;
        if (j != 0) {
            return j;
        }
        TekLog.write(LOGTAG, "getNativePointer _nativePointer == 0");
        return 0L;
    }

    public int getOutputTexture() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getOutputTexture(j);
        }
        TekLog.write(LOGTAG, "getOutputTexture _nativePointer == 0:");
        return 0;
    }

    public int getProgress() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getProgress(j);
        }
        TekLog.write(LOGTAG, "getProgress _nativePointer == 0:");
        return 0;
    }

    public int getViewHeight() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getViewHeight(j);
        }
        TekLog.write(LOGTAG, "getViewHeight _nativePointer == 0:");
        return 0;
    }

    public int getViewWidth() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getViewWidth(j);
        }
        TekLog.write(LOGTAG, "getViewWidth _nativePointer == 0:");
        return 0;
    }

    public int getX() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getX(j);
        }
        TekLog.write(LOGTAG, "getX _nativePointer == 0:");
        return 0;
    }

    public int getY() {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.getY(j);
        }
        TekLog.write(LOGTAG, "getY _nativePointer == 0:");
        return 0;
    }

    public int initWithCustomLayer(TekCustomLayer tekCustomLayer) {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return -1;
        }
        TekLog.write(LOGTAG, "initWithCustomLayer:");
        long j = this._nativePointer;
        if (j == 0) {
            TekLog.write(LOGTAG, "initWithCustomLayer: _nativePointer == 0");
            return TekErrorCode.ENGINEWRAPPER_INITJSON_POINTER_NULL;
        }
        int iStartInitCustom = TekNativeInterface.startInitCustom(j, tekCustomLayer._name, tekCustomLayer._resourcePath);
        if (iStartInitCustom != 0) {
            return iStartInitCustom;
        }
        TekNativeInterface.updateParam(this._nativePointer, UsualParamInfo.kTargetLayerAll, "width", tekCustomLayer._width);
        TekNativeInterface.updateParam(this._nativePointer, UsualParamInfo.kTargetLayerAll, "height", tekCustomLayer._height);
        Map<String, Float> map = tekCustomLayer._params1;
        if (map != null) {
            for (Map.Entry<String, Float> entry : map.entrySet()) {
                TekNativeInterface.updateParam(this._nativePointer, UsualParamInfo.kTargetLayerAll, entry.getKey(), entry.getValue().floatValue());
            }
        }
        Map<String, String> map2 = tekCustomLayer._params2;
        if (map2 != null) {
            for (Map.Entry<String, String> entry2 : map2.entrySet()) {
                TekNativeInterface.updateParam2(this._nativePointer, UsualParamInfo.kTargetLayerAll, entry2.getKey(), entry2.getValue());
            }
        }
        TekNativeInterface.endInitCustom(this._nativePointer);
        return 0;
    }

    public int initWithJSON(String str) {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return -1;
        }
        TekLog.write(LOGTAG, "initWihtJSON:" + str);
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.initWithJSON(j, str);
        }
        TekLog.write(LOGTAG, "initWihtJSON: _nativePointer == 0");
        return TekErrorCode.ENGINEWRAPPER_INITJSON_POINTER_NULL;
    }

    public void setCryptKey(String str) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "setCryptKey:" + str);
            if (this._nativePointer == 0) {
                TekLog.write(LOGTAG, "setCryptKey: _nativePointer == 0");
            } else {
                if (TextUtils.isEmpty(str)) {
                    return;
                }
                TekNativeInterface.setCryptKey(this._nativePointer, str);
            }
        }
    }

    public void setDelegate(Object obj) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "setDelegate.");
            if (obj == null) {
                return;
            }
            TekNativeInterface.setJavaEngineDelegate(this._nativePointer, obj);
        }
    }

    public void setIsDirectRenderView(int i) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "setIsDirectRenderView _nativePointer == 0:");
            } else {
                TekNativeInterface.setIsDirectRenderView(j, i);
            }
        }
    }

    public void setUUID(String str) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "setUUID:" + str);
            long j = this._nativePointer;
            if (j != 0) {
                TekNativeInterface.setUUID(j, str);
                return;
            }
            TekLog.write(LOGTAG, "setUUID _nativePointer == 0:" + str);
        }
    }

    public void updateConfig(TekEffectConfig tekEffectConfig) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "updateConfig");
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "updateConfig _nativePointer == 0");
            } else {
                if (tekEffectConfig == null) {
                    return;
                }
                TekNativeInterface.setInputImageCacheSize(j, tekEffectConfig._inputImageCacheSize);
            }
        }
    }

    public void updateEffectFillMode(int i) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "updateFFTData _nativePointer == 0");
            } else {
                TekNativeInterface.updateEffectFillMode(j, i);
            }
        }
    }

    public void updateFFTData(byte[] bArr, int i) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "updateFFTData _nativePointer == 0");
            } else {
                TekNativeInterface.updateFFTData(j, bArr, bArr.length, i);
            }
        }
    }

    public int updateLyricParam(TekLyricParam tekLyricParam) {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return -40;
        }
        TekLog.write(LOGTAG, "updateLyricParam");
        long j = this._nativePointer;
        if (j == 0) {
            TekLog.write(LOGTAG, "updateLyricParam _nativePointer == 0");
            return -41;
        }
        TekNativeInterface.cleanLyricRows(j);
        TekNativeInterface.cleanReuseTextureCache(this._nativePointer);
        long j2 = this._nativePointer;
        TekNativeInterface.setRenderId(j2, j2);
        TekNativeInterface.setSongId(this._nativePointer, tekLyricParam._songId);
        int iCheckLyricParam = TekLyricParser.checkLyricParam(tekLyricParam);
        if (iCheckLyricParam < 0) {
            return iCheckLyricParam;
        }
        ArrayList<TekLyricRowParseResult> arrayList = tekLyricParam._parsedLyricArray;
        if (arrayList == null) {
            arrayList = TekLyricParser.parse(tekLyricParam);
        }
        if (arrayList == null) {
            TekLog.write(LOGTAG, "updateLyricParam lyricArray == null");
            return -49;
        }
        if (arrayList.isEmpty()) {
            TekLog.write(LOGTAG, "updateLyricParam lyricArray isEmpty");
        }
        TekAndroidLyricCache tekAndroidLyricCache = TekAndroidLyricCache.getInstance();
        tekAndroidLyricCache.clean(Long.valueOf(this._nativePointer));
        tekAndroidLyricCache.addKrcRows(arrayList, Long.valueOf(this._nativePointer));
        tekAndroidLyricCache.setLyricParam(tekLyricParam, Long.valueOf(this._nativePointer));
        tekAndroidLyricCache.setFontParam(tekLyricParam._fontParam, Long.valueOf(this._nativePointer));
        TekTextParam tekTextParam = tekLyricParam._headline;
        if (tekTextParam != null && tekTextParam.isValidate()) {
            tekAndroidLyricCache.setHeadline(tekLyricParam._headline, Long.valueOf(this._nativePointer));
            long j3 = this._nativePointer;
            TekTextParam tekTextParam2 = tekLyricParam._headline;
            TekNativeInterface.setKrcHeadline(j3, j3, tekTextParam2._showWidth, tekTextParam2._x, tekTextParam2._y, tekTextParam2._alignment, tekTextParam2._shadow);
        }
        TekTextParam tekTextParam3 = tekLyricParam._singer;
        if (tekTextParam3 != null && tekTextParam3.isValidate()) {
            tekAndroidLyricCache.setSinger(tekLyricParam._singer, Long.valueOf(this._nativePointer));
            long j4 = this._nativePointer;
            TekTextParam tekTextParam4 = tekLyricParam._singer;
            TekNativeInterface.setKrcSinger(j4, j4, tekTextParam4._showWidth, tekTextParam4._x, tekTextParam4._y, tekTextParam4._alignment, tekTextParam4._shadow);
        }
        for (int i = 0; i < arrayList.size(); i++) {
            TekLyricRowParseResult tekLyricRowParseResult = arrayList.get(i);
            TekNativeInterface.addOneRow(this._nativePointer, tekLyricRowParseResult._orignRowIndex, tekLyricRowParseResult._startTime, tekLyricRowParseResult._endTime, tekLyricRowParseResult._rowLength);
            if (tekLyricRowParseResult._wordArray != null) {
                for (int i2 = 0; i2 < tekLyricRowParseResult._wordArray.size(); i2++) {
                    TekLyricWordParseResult tekLyricWordParseResult = tekLyricRowParseResult._wordArray.get(i2);
                    if (tekLyricParam._isNeedString) {
                        TekNativeInterface.addOneWord2(this._nativePointer, i, tekLyricWordParseResult._startTime, tekLyricWordParseResult._endTime, tekLyricWordParseResult._wordLength, tekLyricParam._kernAdjust, tekLyricWordParseResult._str);
                    } else {
                        TekNativeInterface.addOneWord(this._nativePointer, i, tekLyricWordParseResult._startTime, tekLyricWordParseResult._endTime, tekLyricWordParseResult._wordLength, tekLyricParam._kernAdjust);
                    }
                }
            }
            if (tekLyricRowParseResult._subRowArray != null) {
                for (int i3 = 0; i3 < tekLyricRowParseResult._subRowArray.size(); i3++) {
                    TekLyricSubRowParseResult tekLyricSubRowParseResult = tekLyricRowParseResult._subRowArray.get(i3);
                    TekNativeInterface.addOneSubRow(this._nativePointer, i, tekLyricSubRowParseResult._startTime, tekLyricSubRowParseResult._endTime, tekLyricSubRowParseResult._textureLength, tekLyricSubRowParseResult._wordStartIndex, tekLyricSubRowParseResult._wordEndIndex);
                }
            }
        }
        if (tekLyricParam._isLrc) {
            TekNativeInterface.setIsLrc(this._nativePointer, 1);
        } else {
            TekNativeInterface.setIsLrc(this._nativePointer, 0);
        }
        TekNativeInterface.updateParam3(this._nativePointer, UsualParamInfo.kTargetLayerAll, "KrcAlignment", tekLyricParam._alignment);
        TekNativeInterface.setPreludePoint(this._nativePointer, tekLyricParam._preludePoint);
        TekNativeInterface.setKrcTimestampOffset(this._nativePointer, tekLyricParam._krcTimestampOffset);
        int i4 = tekLyricParam._lyricGroupSize;
        long j5 = this._nativePointer;
        if (i4 != 0) {
            TekNativeInterface.updateParam3(j5, UsualParamInfo.kTargetLayerAll, "LyricGroupSize", i4);
        } else {
            TekNativeInterface.cleanParam(j5, UsualParamInfo.kTargetLayerAll, "LyricGroupSize");
        }
        TekNativeInterface.configAnimationRows(this._nativePointer);
        TekNativeInterface.setKrcLineRange(this._nativePointer, tekLyricParam._maxUpLine, tekLyricParam._maxDownLine);
        TekNativeInterface.clearFFTData(this._nativePointer);
        return 0;
    }

    public void updateParam(String str, Map<String, Object> map) {
        Bitmap bitmap;
        int iLoadTextureWithBitmap;
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "updateParam:");
            if (this._nativePointer == 0) {
                TekLog.write(LOGTAG, "updateParam: _nativePointer == 0");
                return;
            }
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object value = entry.getValue();
                String key = entry.getKey();
                if (value instanceof String) {
                    TekNativeInterface.updateParam2(this._nativePointer, str, entry.getKey(), (String) value);
                }
                if (value instanceof Float) {
                    TekNativeInterface.updateParam(this._nativePointer, str, entry.getKey(), ((Float) value).floatValue());
                }
                if (value instanceof Double) {
                    TekNativeInterface.updateParam(this._nativePointer, str, entry.getKey(), ((Double) value).floatValue());
                }
                if (value instanceof Integer) {
                    TekNativeInterface.updateParam3(this._nativePointer, str, entry.getKey(), ((Integer) value).intValue());
                }
                if ((value instanceof Bitmap) && (bitmap = (Bitmap) value) != null && (iLoadTextureWithBitmap = TekAndroidTextureCreate.loadTextureWithBitmap(bitmap)) > 0) {
                    TekNativeInterface.updateParam4(this._nativePointer, str, entry.getKey(), iLoadTextureWithBitmap, bitmap.getWidth(), bitmap.getHeight(), 6408);
                }
                if (key != null && (value instanceof TekFontParam) && key.equals("FontParam")) {
                    TekAndroidLyricCache tekAndroidLyricCache = TekAndroidLyricCache.getInstance();
                    TekFontParam tekFontParam = (TekFontParam) value;
                    if (tekFontParam != null) {
                        long fontParam = tekAndroidLyricCache.setFontParam(tekFontParam, Long.valueOf(tekFontParam.hashCode()));
                        if (fontParam != 0) {
                            TekNativeInterface.updateParam5(this._nativePointer, str, key, fontParam);
                        }
                    }
                }
            }
        }
    }

    public void updateParmaMap(Map<String, Object> map) {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekProxyLog.f(LOGTAG, "updateParmaMap:");
            long j = this._nativePointer;
            if (j == 0) {
                TekProxyLog.e(LOGTAG, "updateParmaMap: _nativePointer == 0");
            } else {
                TekNativeInterface.updateParamMap(j, map);
            }
        }
    }

    public void updateRunningLayerInfo() {
        if (TekNativeInterface.LoadLibrarySuccess) {
            TekLog.write(LOGTAG, "updateRunningLayerInfo");
            long j = this._nativePointer;
            if (j == 0) {
                TekLog.write(LOGTAG, "updateRunningLayerInfo _nativePointer == 0");
            } else {
                TekNativeInterface.updateRunningLayerInfo(j);
            }
        }
    }

    public void updateUpdaterImpl(TekBaseShader tekBaseShader) {
        TekProxyLog.i(LOGTAG, "updateUpdaterImpl. shader" + tekBaseShader);
        TekNativeInterface.updateUpdaterImpl(this._nativePointer, tekBaseShader.getClassSimpleName());
    }

    public void updateWithTimestamp(float f) {
        updateWithTimestampWithRet(f);
    }

    public int updateWithTimestampWithRet(float f) {
        if (!TekNativeInterface.LoadLibrarySuccess) {
            return 0;
        }
        long j = this._nativePointer;
        if (j != 0) {
            return TekNativeInterface.updateWithTimestamp(j, f);
        }
        TekLog.write(LOGTAG, "updateWithTimestamp _nativePointer == 0:" + f);
        return 0;
    }
}
