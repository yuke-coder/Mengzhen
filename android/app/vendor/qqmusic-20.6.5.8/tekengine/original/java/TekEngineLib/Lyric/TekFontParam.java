package TekEngineLib.Lyric;

import android.graphics.Typeface;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekFontParam {
    public boolean _blurOptimize;
    public List<TekFontShadow> _blurShadows;
    public int _fontSize;
    public TekFontShadow _shadow;
    public Typeface _tf;
    public TekFontShadow _unplayShadow;
    public int _color = -1;
    public int[] _alternateColors = null;
    public int _unplayedColor = -1;
    public boolean _isItalic = false;
    public int _widthPadding = 0;
    public int _heightPadding = 0;
    public float _letterSpacing = 0.0f;
    public boolean _isBlurLight = false;
    public boolean _isShadowLight = false;
    public float _scale = -1.0f;

    public TekFontParam copy() {
        TekFontParam tekFontParam = new TekFontParam();
        tekFontParam._tf = this._tf;
        tekFontParam._fontSize = this._fontSize;
        tekFontParam._color = this._color;
        tekFontParam._alternateColors = this._alternateColors;
        tekFontParam._unplayedColor = this._unplayedColor;
        tekFontParam._isItalic = this._isItalic;
        tekFontParam._widthPadding = this._widthPadding;
        tekFontParam._heightPadding = this._heightPadding;
        tekFontParam._letterSpacing = this._letterSpacing;
        tekFontParam._isBlurLight = this._isBlurLight;
        tekFontParam._isShadowLight = this._isShadowLight;
        tekFontParam._blurShadows = this._blurShadows;
        tekFontParam._shadow = this._shadow;
        tekFontParam._unplayShadow = this._unplayShadow;
        tekFontParam._blurOptimize = this._blurOptimize;
        tekFontParam._scale = this._scale;
        return tekFontParam;
    }

    public boolean equals(@Nullable Object obj) {
        if (obj == null || !(obj instanceof TekFontParam)) {
            return super.equals(obj);
        }
        TekFontParam tekFontParam = (TekFontParam) obj;
        return this._tf == tekFontParam._tf && this._fontSize == tekFontParam._fontSize && this._color == tekFontParam._color && Arrays.equals(this._alternateColors, tekFontParam._alternateColors) && this._unplayedColor == tekFontParam._unplayedColor && this._isItalic == tekFontParam._isItalic && this._widthPadding == tekFontParam._widthPadding && this._heightPadding == tekFontParam._heightPadding && this._letterSpacing == tekFontParam._letterSpacing && this._isBlurLight == tekFontParam._isBlurLight && this._isShadowLight == tekFontParam._isShadowLight && Objects.equals(this._blurShadows, tekFontParam._blurShadows) && this._shadow == tekFontParam._shadow && this._unplayShadow == tekFontParam._unplayShadow && this._blurOptimize == tekFontParam._blurOptimize && this._scale == tekFontParam._scale;
    }

    public boolean isValidate() {
        return this._tf != null && this._fontSize >= 1;
    }
}
