package TekEngineLib.Lyric;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekTextParam {
    public int _alignment;
    public TekFontParam _fontParam;
    public float _hPadding;
    public boolean _shadow;
    public float _showWidth;
    public String _text;
    public float _vPadding;
    public float _x;
    public float _y;

    public boolean isValidate() {
        TekFontParam tekFontParam;
        if (this._text == null || (tekFontParam = this._fontParam) == null) {
            return false;
        }
        return tekFontParam.isValidate();
    }
}
