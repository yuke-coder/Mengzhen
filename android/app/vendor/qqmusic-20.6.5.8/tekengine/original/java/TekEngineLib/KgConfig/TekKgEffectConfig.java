package TekEngineLib.KgConfig;

import TekEngineLib.Lyric.TekFontParam;
import TekEngineLib.Lyric.TekLyricBreakLineMode;
import TekEngineLib.Lyric.TekLyricBreakLineType;
import TekEngineLib.Lyric.TekLyricParam;
import TekEngineLib.Lyric.TekLyricSubRowSplit;
import TekEngineLib.Lyric.TekTextParam;
import android.graphics.Typeface;
import com.kugou.framework.lyric.LyricData;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekKgEffectConfig {
    public static final int KG_EFFECT_END = 3;
    public static final int KG_EFFECT_LETTER = 1;
    public static final int KG_EFFECT_TRAIN = 2;
    private static TekLyricSubRowSplit _trainEffectSplit;

    public static TekLyricParam paramFromType(int i, LyricData lyricData, Typeface typeface, String str, String str2, String str3) {
        if (i < 1 || i >= 3 || lyricData == null) {
            return null;
        }
        TekLyricParam tekLyricParam = new TekLyricParam();
        tekLyricParam._krcData = lyricData;
        if (i == 1) {
            TekFontParam tekFontParam = new TekFontParam();
            tekLyricParam._fontParam = tekFontParam;
            tekFontParam._tf = typeface;
            if (typeface == null) {
                tekFontParam._tf = Typeface.DEFAULT;
            }
            tekFontParam._fontSize = 110;
            TekLyricBreakLineMode tekLyricBreakLineMode = new TekLyricBreakLineMode();
            tekLyricParam._breakMode = tekLyricBreakLineMode;
            tekLyricBreakLineMode._type = TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_BY_LENGTH;
            tekLyricParam._isNeedString = true;
            if (lyricData.getLyricType() == 2) {
                tekLyricParam._breakMode._param = 1300;
                tekLyricParam._isLrc = true;
            } else {
                tekLyricParam._breakMode._param = 1600;
                tekLyricParam._isAddSpace = true;
            }
        } else if (i == 2) {
            if (str == null) {
                return null;
            }
            TekFontParam tekFontParam2 = new TekFontParam();
            tekLyricParam._fontParam = tekFontParam2;
            tekFontParam2._tf = typeface;
            if (typeface == null) {
                tekFontParam2._tf = Typeface.DEFAULT;
            }
            tekFontParam2._fontSize = 84;
            TekLyricBreakLineMode tekLyricBreakLineMode2 = new TekLyricBreakLineMode();
            tekLyricParam._breakMode = tekLyricBreakLineMode2;
            tekLyricBreakLineMode2._type = TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_BY_LENGTH;
            tekLyricParam._isNeedString = true;
            if (lyricData.getLyricType() == 2) {
                TekLyricBreakLineMode tekLyricBreakLineMode3 = tekLyricParam._breakMode;
                tekLyricBreakLineMode3._param = 1800;
                tekLyricBreakLineMode3._param2 = 800.0f;
                tekLyricParam._isLrc = true;
            } else {
                TekLyricBreakLineMode tekLyricBreakLineMode4 = tekLyricParam._breakMode;
                tekLyricBreakLineMode4._param = 1800;
                tekLyricBreakLineMode4._param2 = 800.0f;
            }
            if (str2 != null) {
                TekTextParam tekTextParam = new TekTextParam();
                tekLyricParam._headline = tekTextParam;
                tekTextParam._fontParam = new TekFontParam();
                TekTextParam tekTextParam2 = tekLyricParam._headline;
                TekFontParam tekFontParam3 = tekTextParam2._fontParam;
                tekFontParam3._fontSize = 63;
                tekFontParam3._tf = typeface;
                tekTextParam2._text = str2;
                tekTextParam2._alignment = 1;
                tekTextParam2._showWidth = 900.0f;
            }
            if (str3 != null) {
                TekTextParam tekTextParam3 = new TekTextParam();
                tekLyricParam._singer = tekTextParam3;
                tekTextParam3._fontParam = new TekFontParam();
                TekTextParam tekTextParam4 = tekLyricParam._singer;
                TekFontParam tekFontParam4 = tekTextParam4._fontParam;
                tekFontParam4._fontSize = 36;
                tekFontParam4._tf = typeface;
                tekTextParam4._text = str3;
                tekTextParam4._alignment = 1;
                tekTextParam4._showWidth = 900.0f;
            }
            TekKgTrainEffectSubRowSplit tekKgTrainEffectSubRowSplit = new TekKgTrainEffectSubRowSplit();
            tekKgTrainEffectSubRowSplit.jiebaPath = str;
            tekLyricParam._subRowSplit = tekKgTrainEffectSubRowSplit;
        }
        return tekLyricParam;
    }
}
