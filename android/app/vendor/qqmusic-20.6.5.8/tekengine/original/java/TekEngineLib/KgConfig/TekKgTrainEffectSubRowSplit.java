package TekEngineLib.KgConfig;

import TekEngineLib.Engine.TekNativeInterface;
import TekEngineLib.Lyric.TekLyricRowParseResult;
import TekEngineLib.Lyric.TekLyricSubRowParseResult;
import TekEngineLib.Lyric.TekLyricSubRowSplit;
import TekEngineLib.Lyric.TekLyricWordParseResult;
import TekEngineLib.State.TekLog;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekKgTrainEffectSubRowSplit implements TekLyricSubRowSplit {
    static final String LOGTAG = "TekKgTrainEffectSubRowSplit";
    static Boolean hasJiebaInit = Boolean.FALSE;
    public String jiebaPath;

    static TekLyricSubRowParseResult createSubRowFromWordArrayInfo(ArrayList<TekLyricWordParseResult> arrayList) {
        if (arrayList == null || arrayList.size() == 0) {
            return null;
        }
        TekLog.write(LOGTAG, "createSubRowFromWordArrayInfo");
        TekLyricSubRowParseResult tekLyricSubRowParseResult = new TekLyricSubRowParseResult();
        tekLyricSubRowParseResult._wordArray = arrayList;
        tekLyricSubRowParseResult._startTime = arrayList.get(0)._startTime;
        tekLyricSubRowParseResult._endTime = arrayList.get(arrayList.size() - 1)._endTime;
        tekLyricSubRowParseResult._wordStartIndex = arrayList.get(0)._wordIndex;
        tekLyricSubRowParseResult._wordEndIndex = arrayList.get(arrayList.size() - 1)._wordIndex;
        float f = 0.0f;
        for (int i = 0; i < arrayList.size(); i++) {
            f += arrayList.get(i)._wordLength;
        }
        tekLyricSubRowParseResult._textureLength = f;
        return tekLyricSubRowParseResult;
    }

    public static boolean jiebaInit(String str) {
        if (hasJiebaInit.booleanValue()) {
            return true;
        }
        String str2 = str + "/jieba.dict.utf8";
        String str3 = str + "/hmm_model.utf8";
        String str4 = str + "/user.dict.utf8";
        try {
            if (!new File(str2).exists() || !new File(str3).exists() || !new File(str4).exists()) {
                return false;
            }
            hasJiebaInit = TekNativeInterface.jiebaInit(str2, str3, str4) == 0 ? Boolean.TRUE : Boolean.FALSE;
            return hasJiebaInit.booleanValue();
        } catch (Exception unused) {
            return false;
        }
    }

    /* JADX WARN: Code duplicated, block: B:75:0x0184  */
    @Override // TekEngineLib.Lyric.TekLyricSubRowSplit
    public int createSubRowInfo(TekLyricRowParseResult tekLyricRowParseResult) {
        String str;
        String str2 = this.jiebaPath;
        if (str2 == null) {
            return -50;
        }
        if (!jiebaInit(str2)) {
            return -51;
        }
        if (tekLyricRowParseResult == null || tekLyricRowParseResult._wordArray == null || (str = tekLyricRowParseResult._str) == null || str.length() == 0 || tekLyricRowParseResult._wordArray.size() <= 0) {
            return -52;
        }
        ArrayList arrayList = new ArrayList();
        ArrayList<TekLyricWordParseResult> arrayList2 = tekLyricRowParseResult._wordArray;
        TekLog.write(LOGTAG, "createSubRowInfo:" + tekLyricRowParseResult._str);
        tekLyricRowParseResult._subRowArray = new ArrayList<>();
        String[] strArrJiebaCut = TekNativeInterface.jiebaCut(tekLyricRowParseResult._str);
        if (strArrJiebaCut == null || strArrJiebaCut.length == 0) {
            return -53;
        }
        ArrayList arrayList3 = new ArrayList(Arrays.asList(strArrJiebaCut));
        String str3 = null;
        ArrayList arrayList4 = null;
        String str4 = null;
        int i = 0;
        int i2 = 0;
        char c = 0;
        while (i < arrayList3.size() && i2 < arrayList2.size()) {
            if (c == 0) {
                str3 = (String) arrayList3.get(i);
                String str5 = arrayList2.get(i2)._str;
                ArrayList arrayList5 = new ArrayList();
                arrayList5.add(arrayList2.get(i2));
                str4 = str5;
                arrayList4 = arrayList5;
            } else if (c == 1) {
                str4 = str4 + arrayList2.get(i2)._str;
                arrayList4.add(arrayList2.get(i2));
            } else {
                str3 = str3 + ((String) arrayList3.get(i));
            }
            if (str3.equalsIgnoreCase(str4)) {
                TekLyricSubRowParseResult tekLyricSubRowParseResultCreateSubRowFromWordArrayInfo = createSubRowFromWordArrayInfo(arrayList4);
                tekLyricSubRowParseResultCreateSubRowFromWordArrayInfo._str = str4;
                arrayList.add(tekLyricSubRowParseResultCreateSubRowFromWordArrayInfo);
                i++;
                i2++;
                c = 0;
            } else if (str3.toLowerCase().contains(str4.toLowerCase())) {
                i2++;
                c = 1;
            } else {
                i++;
                c = 2;
            }
        }
        float f = tekLyricRowParseResult._rowLength;
        int i3 = f < 500.0f ? 1 : f < 800.0f ? 2 : 3;
        ArrayList<TekLyricSubRowParseResult> arrayList6 = new ArrayList<>();
        TekLyricSubRowParseResult tekLyricSubRowParseResult = new TekLyricSubRowParseResult();
        tekLyricSubRowParseResult.appendSubRow((TekLyricSubRowParseResult) arrayList.get(0));
        arrayList6.add(tekLyricSubRowParseResult);
        for (int i4 = 1; i4 < arrayList.size(); i4++) {
            TekLyricSubRowParseResult tekLyricSubRowParseResult2 = (TekLyricSubRowParseResult) arrayList.get(i4);
            int size = arrayList6.size();
            if (size >= i3) {
                tekLyricSubRowParseResult.appendSubRow(tekLyricSubRowParseResult2);
            } else {
                float f2 = 1.0f;
                if (i3 == 3) {
                    f2 = (size != 1 && size == 2) ? 0.2f : 0.4f;
                } else if (i3 == 2) {
                    if (size == 1) {
                        f2 = 0.7f;
                    } else if (size == 2) {
                        f2 = 0.3f;
                    }
                }
                float f3 = f2 * tekLyricRowParseResult._rowLength;
                float f4 = tekLyricSubRowParseResult._textureLength;
                if (f4 <= f3 && (tekLyricSubRowParseResult2._textureLength + f4) - f3 <= f3 - f4) {
                    tekLyricSubRowParseResult.appendSubRow(tekLyricSubRowParseResult2);
                } else {
                    tekLyricSubRowParseResult = new TekLyricSubRowParseResult();
                    tekLyricSubRowParseResult.appendSubRow(tekLyricSubRowParseResult2);
                    arrayList6.add(tekLyricSubRowParseResult);
                }
            }
        }
        tekLyricRowParseResult._subRowArray = arrayList6;
        return 0;
    }
}
