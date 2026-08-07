package TekEngineLib.Lyric;

import com.kugou.framework.lyric.LyricData;
import java.util.ArrayList;
import org.json.JSONArray;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekLyricParam {
    public static final int ALIGNMENT_CENTRE = 0;
    public static final int ALIGNMENT_LEFT = 1;
    public int _alignment;
    public float _blurRadius;
    public TekLyricBreakLineMode _breakMode;
    public long _endPoint;
    public TekFontParam _fontParam;
    public TekTextParam _headline;
    public float _headlineDrawWidth;
    public boolean _isAddSpace;
    public boolean _isLrc;
    public boolean _isNeedString;
    public boolean _isRemoveEnd;
    public boolean _isRemovePrelude;
    public float _kernAdjust;
    public LyricData _krcData;
    public int _krcTimestampOffset;
    public int _lyricAlignmentParam;
    public int _lyricGroupSize;
    public float _lyricMaxDrawHeight;
    public float _lyricMaxDrawWidth;
    public int _maxDownLine;
    public int _maxUpLine;
    public ArrayList _parsedLyricArray;
    public long _preludePoint;
    public JSONArray _qrcData;
    public float _sentenceDrawWidth;
    public float _sentenceLineSpacing;
    public float _sentenceSpacing;
    public TekTextParam _singer;
    public long _songId;
    public TekLyricSubRowSplit _subRowSplit;

    public TekLyricParam createNewParam() {
        TekLyricParam tekLyricParam = new TekLyricParam();
        tekLyricParam._songId = this._songId;
        tekLyricParam._isLrc = this._isLrc;
        tekLyricParam._krcData = this._krcData;
        tekLyricParam._qrcData = this._qrcData;
        tekLyricParam._preludePoint = this._preludePoint;
        tekLyricParam._isRemovePrelude = this._isRemovePrelude;
        tekLyricParam._endPoint = this._endPoint;
        tekLyricParam._isRemoveEnd = this._isRemoveEnd;
        tekLyricParam._breakMode = this._breakMode;
        tekLyricParam._fontParam = this._fontParam;
        tekLyricParam._headline = this._headline;
        tekLyricParam._singer = this._singer;
        tekLyricParam._krcTimestampOffset = this._krcTimestampOffset;
        tekLyricParam._isAddSpace = this._isAddSpace;
        tekLyricParam._kernAdjust = this._kernAdjust;
        tekLyricParam._maxUpLine = this._maxUpLine;
        tekLyricParam._maxDownLine = this._maxDownLine;
        tekLyricParam._isNeedString = this._isNeedString;
        tekLyricParam._subRowSplit = this._subRowSplit;
        tekLyricParam._parsedLyricArray = this._parsedLyricArray;
        tekLyricParam._alignment = this._alignment;
        tekLyricParam._lyricAlignmentParam = this._lyricAlignmentParam;
        tekLyricParam._lyricGroupSize = this._lyricGroupSize;
        tekLyricParam._lyricMaxDrawWidth = this._lyricMaxDrawWidth;
        tekLyricParam._lyricMaxDrawHeight = this._lyricMaxDrawHeight;
        tekLyricParam._sentenceDrawWidth = this._sentenceDrawWidth;
        tekLyricParam._sentenceSpacing = this._sentenceSpacing;
        tekLyricParam._sentenceLineSpacing = this._sentenceLineSpacing;
        tekLyricParam._blurRadius = this._blurRadius;
        tekLyricParam._headlineDrawWidth = this._headlineDrawWidth;
        return tekLyricParam;
    }
}
