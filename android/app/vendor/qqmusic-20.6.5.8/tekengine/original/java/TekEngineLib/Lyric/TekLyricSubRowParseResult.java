package TekEngineLib.Lyric;

import java.util.ArrayList;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekLyricSubRowParseResult {
    public String _str;
    public long _startTime = 0;
    public long _endTime = 0;
    public float _textureLength = 0.0f;
    public int _wordStartIndex = 0;
    public int _wordEndIndex = 0;
    public ArrayList<TekLyricWordParseResult> _wordArray = null;

    public void appendSubRow(TekLyricSubRowParseResult tekLyricSubRowParseResult) {
        ArrayList<TekLyricWordParseResult> arrayList;
        if (tekLyricSubRowParseResult == null || tekLyricSubRowParseResult._textureLength <= 1.0f || (arrayList = tekLyricSubRowParseResult._wordArray) == null || arrayList.size() == 0) {
            return;
        }
        int i = 0;
        if (this._wordArray == null) {
            this._wordArray = new ArrayList<>();
            this._wordStartIndex = tekLyricSubRowParseResult._wordStartIndex;
            this._wordEndIndex = tekLyricSubRowParseResult._wordEndIndex;
            this._str = tekLyricSubRowParseResult._str;
            this._textureLength = tekLyricSubRowParseResult._textureLength;
            this._startTime = tekLyricSubRowParseResult._startTime;
            this._endTime = tekLyricSubRowParseResult._endTime;
            while (i < tekLyricSubRowParseResult._wordArray.size()) {
                this._wordArray.add(tekLyricSubRowParseResult._wordArray.get(i));
                i++;
            }
            return;
        }
        this._wordEndIndex = tekLyricSubRowParseResult._wordEndIndex;
        this._endTime = tekLyricSubRowParseResult._endTime;
        this._str += tekLyricSubRowParseResult._str;
        this._textureLength += tekLyricSubRowParseResult._textureLength;
        while (i < tekLyricSubRowParseResult._wordArray.size()) {
            this._wordArray.add(tekLyricSubRowParseResult._wordArray.get(i));
            i++;
        }
    }
}
