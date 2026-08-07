package TekEngineLib.Lyric;

import TekEngineLib.State.TekLog;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import com.kugou.framework.lyric.LyricData;
import java.util.ArrayList;
import java.util.List;
import okhttp3.HttpUrl;
import org.json.JSONArray;
import org.json.JSONException;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekLyricParser {
    private static String LOGTAG = "TEK TekLyricParser";
    public static int errorCode;

    private static ArrayList<TekLyricRowParseResult> addSpace(ArrayList<TekLyricRowParseResult> arrayList, TekLyricParam tekLyricParam, Paint paint) {
        float fMeasureText = paint.measureText(" ");
        tekLyricParam._kernAdjust = fMeasureText;
        for (int i = 0; i < arrayList.size(); i++) {
            TekLyricRowParseResult tekLyricRowParseResult = arrayList.get(i);
            float f = 0.0f;
            String str = HttpUrl.FRAGMENT_ENCODE_SET;
            for (int i2 = 0; i2 < tekLyricRowParseResult._wordArray.size(); i2++) {
                TekLyricWordParseResult tekLyricWordParseResult = tekLyricRowParseResult._wordArray.get(i2);
                if (tekLyricWordParseResult._str != null) {
                    str = str + tekLyricWordParseResult._str + " ";
                    f = f + fMeasureText + tekLyricWordParseResult._wordLength;
                }
            }
            tekLyricRowParseResult._str = str;
            tekLyricRowParseResult._rowLength = f;
        }
        return arrayList;
    }

    private static void checkLongWord(TekLyricRowParseResult tekLyricRowParseResult, TekLyricParam tekLyricParam, Paint paint) {
        TekFontParam tekFontParam;
        ArrayList<TekLyricWordParseResult> arrayList;
        if (paint == null || tekLyricRowParseResult == null || tekLyricParam == null || tekLyricParam._breakMode == null || (tekFontParam = tekLyricParam._fontParam) == null) {
            return;
        }
        boolean z = true;
        if (tekFontParam._fontSize < 1 || (arrayList = tekLyricRowParseResult._wordArray) == null || arrayList.isEmpty()) {
            return;
        }
        TekLyricBreakLineMode tekLyricBreakLineMode = tekLyricParam._breakMode;
        if (tekLyricBreakLineMode._type != TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_BY_LENGTH || tekLyricBreakLineMode._param < 1) {
            return;
        }
        int i = 0;
        while (true) {
            if (i >= tekLyricRowParseResult._wordArray.size()) {
                z = false;
                break;
            }
            float f = tekLyricRowParseResult._wordArray.get(i)._wordLength;
            TekLyricBreakLineMode tekLyricBreakLineMode2 = tekLyricParam._breakMode;
            if (f > tekLyricBreakLineMode2._param) {
                break;
            }
            float f2 = tekLyricBreakLineMode2._param2;
            if (f2 > 1.0f && f > f2) {
                break;
            } else {
                i++;
            }
        }
        if (z) {
            ArrayList<TekLyricWordParseResult> arrayList2 = new ArrayList<>();
            for (int i2 = 0; i2 < tekLyricRowParseResult._wordArray.size(); i2++) {
                partitionWordStrIntoArray(arrayList2, tekLyricRowParseResult._wordArray.get(i2), tekLyricParam, paint);
            }
            tekLyricRowParseResult._wordArray = arrayList2;
        }
    }

    public static int checkLyricParam(TekLyricParam tekLyricParam) {
        if (tekLyricParam == null) {
            return -42;
        }
        TekFontParam tekFontParam = tekLyricParam._fontParam;
        if (tekFontParam == null) {
            return -43;
        }
        LyricData lyricData = tekLyricParam._krcData;
        if (lyricData == null && tekLyricParam._qrcData == null) {
            return -44;
        }
        if (tekFontParam._tf == null) {
            return -45;
        }
        if (tekFontParam._fontSize < 1) {
            return -46;
        }
        if (lyricData == null) {
            return 0;
        }
        String[][] words = lyricData.getWords();
        long[] rowBeginTime = lyricData.getRowBeginTime();
        long[] rowDelayTime = lyricData.getRowDelayTime();
        long[][] wordBeginTime = lyricData.getWordBeginTime();
        long[][] wordDelayTime = lyricData.getWordDelayTime();
        if (words == null || rowBeginTime == null || rowDelayTime == null || wordBeginTime == null || wordDelayTime == null) {
            return -47;
        }
        return (words.length > rowBeginTime.length || words.length > rowDelayTime.length || words.length > wordBeginTime.length || words.length > wordDelayTime.length) ? -48 : 0;
    }

    static int findPartitionPosition(ArrayList<TekLyricWordParseResult> arrayList) {
        if (arrayList == null || arrayList.size() < 2 || arrayList.size() == 2) {
            return 0;
        }
        return arrayList.size() / 2;
    }

    static boolean hasContentSymbol(String str) {
        return str.contains("，") || str.contains("；") || str.contains("、") || str.contains("：") || str.contains(",") || str.contains(".") || str.contains(":");
    }

    static void innerPartitionOneRow(TekLyricRowParseResult tekLyricRowParseResult, ArrayList<TekLyricRowParseResult> arrayList, TekLyricBreakLineMode tekLyricBreakLineMode) {
        String str;
        if (!isNeedPartition(tekLyricRowParseResult, tekLyricBreakLineMode)) {
            arrayList.add(tekLyricRowParseResult);
            return;
        }
        TekLyricBreakLineType tekLyricBreakLineType = tekLyricBreakLineMode._type;
        TekLyricBreakLineType tekLyricBreakLineType2 = TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_AVERAGE_CHARACTER;
        long j = 0;
        int i = 0;
        if (tekLyricBreakLineType != tekLyricBreakLineType2 && tekLyricBreakLineType != TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_AVERAGE_LENGTH && tekLyricBreakLineType != TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_AVERAGE_LENGTH_MORE_LINE) {
            TekLyricRowParseResult tekLyricRowParseResult2 = new TekLyricRowParseResult();
            tekLyricRowParseResult2._wordArray = new ArrayList<>();
            TekLyricRowParseResult tekLyricRowParseResult3 = new TekLyricRowParseResult();
            tekLyricRowParseResult3._wordArray = new ArrayList<>();
            tekLyricRowParseResult2._orignRowIndex = tekLyricRowParseResult._orignRowIndex;
            tekLyricRowParseResult3._orignRowIndex = tekLyricRowParseResult._orignRowIndex;
            int iFindPartitionPosition = findPartitionPosition(tekLyricRowParseResult._wordArray);
            while (i <= iFindPartitionPosition) {
                TekLyricWordParseResult tekLyricWordParseResult = tekLyricRowParseResult._wordArray.get(i);
                if (i == 0) {
                    j = tekLyricWordParseResult._startTime;
                    long j2 = tekLyricRowParseResult._startTime;
                    tekLyricRowParseResult2._startTime = j2;
                    tekLyricRowParseResult2._endTime = j2;
                    str = tekLyricWordParseResult._str;
                } else {
                    str = tekLyricRowParseResult2._str + tekLyricWordParseResult._str;
                }
                tekLyricRowParseResult2._str = str;
                long j3 = tekLyricRowParseResult2._endTime;
                long j4 = tekLyricWordParseResult._endTime;
                long j5 = tekLyricWordParseResult._startTime;
                tekLyricRowParseResult2._endTime = j3 + (j4 - j5);
                tekLyricRowParseResult2._rowLength += tekLyricWordParseResult._wordLength;
                tekLyricRowParseResult2._characterCount += tekLyricWordParseResult._characterCount;
                tekLyricWordParseResult._startTime = j5 - j;
                tekLyricWordParseResult._endTime = j4 - j;
                tekLyricWordParseResult._wordIndex = tekLyricRowParseResult2._wordArray.size();
                tekLyricRowParseResult2._wordArray.add(tekLyricWordParseResult);
                i++;
                iFindPartitionPosition = iFindPartitionPosition;
            }
            int i2 = iFindPartitionPosition + 1;
            for (int i3 = i2; i3 < tekLyricRowParseResult._wordArray.size(); i3++) {
                TekLyricWordParseResult tekLyricWordParseResult2 = tekLyricRowParseResult._wordArray.get(i3);
                if (i3 == i2) {
                    j = tekLyricWordParseResult2._startTime;
                    tekLyricRowParseResult3._str = tekLyricWordParseResult2._str;
                    tekLyricRowParseResult3._startTime = tekLyricRowParseResult._startTime + j;
                } else {
                    tekLyricRowParseResult3._str += tekLyricWordParseResult2._str;
                }
                if (i3 == tekLyricRowParseResult._wordArray.size() - 1) {
                    tekLyricRowParseResult3._endTime = tekLyricRowParseResult._endTime;
                }
                tekLyricRowParseResult3._rowLength += tekLyricWordParseResult2._wordLength;
                tekLyricRowParseResult3._characterCount += tekLyricWordParseResult2._characterCount;
                tekLyricWordParseResult2._startTime -= j;
                tekLyricWordParseResult2._endTime -= j;
                tekLyricWordParseResult2._wordIndex = tekLyricRowParseResult3._wordArray.size();
                tekLyricRowParseResult3._wordArray.add(tekLyricWordParseResult2);
            }
            innerPartitionOneRow(tekLyricRowParseResult2, arrayList, tekLyricBreakLineMode);
            innerPartitionOneRow(tekLyricRowParseResult3, arrayList, tekLyricBreakLineMode);
            return;
        }
        ArrayList arrayList2 = new ArrayList(tekLyricRowParseResult._wordArray.size());
        ArrayList arrayList3 = new ArrayList(tekLyricBreakLineMode._param);
        TekLyricBreakLineType tekLyricBreakLineType3 = tekLyricBreakLineMode._type;
        if (tekLyricBreakLineType3 == tekLyricBreakLineType2) {
            int i4 = tekLyricBreakLineMode._param;
            int i5 = 0;
            for (TekLyricWordParseResult tekLyricWordParseResult3 : tekLyricRowParseResult._wordArray) {
                arrayList2.add(Integer.valueOf(tekLyricWordParseResult3._characterCount));
                i5 += tekLyricWordParseResult3._characterCount;
            }
            float f = (i5 * 1.0f) / i4;
            for (int i6 = 0; i6 < i4; i6++) {
                arrayList3.add(Float.valueOf(f));
            }
        } else {
            float f2 = 0.0f;
            if (tekLyricBreakLineType3 == TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_AVERAGE_LENGTH) {
                for (TekLyricWordParseResult tekLyricWordParseResult4 : tekLyricRowParseResult._wordArray) {
                    arrayList2.add(Integer.valueOf((int) Math.ceil(tekLyricWordParseResult4._wordLength)));
                    f2 += tekLyricWordParseResult4._wordLength;
                }
                int iMin = Math.min(Math.max(1, (int) Math.ceil(f2 / tekLyricBreakLineMode._param2)), tekLyricBreakLineMode._param);
                float f3 = f2 / iMin;
                for (int i7 = 0; i7 < iMin; i7++) {
                    arrayList3.add(Float.valueOf(f3));
                }
            } else if (tekLyricBreakLineType3 == TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_AVERAGE_LENGTH_MORE_LINE) {
                for (TekLyricWordParseResult tekLyricWordParseResult5 : tekLyricRowParseResult._wordArray) {
                    arrayList2.add(Integer.valueOf((int) Math.ceil(tekLyricWordParseResult5._wordLength)));
                    f2 += tekLyricWordParseResult5._wordLength;
                }
                float fMax = Math.max(1, Math.min(tekLyricBreakLineMode._param, tekLyricRowParseResult._wordArray.size()));
                arrayList3.add(Float.valueOf(fMax));
                arrayList3.add(Float.valueOf(f2 / fMax));
            }
        }
        List<Integer> listCalcLyric = TekLyricDivider.calcLyric(arrayList2, arrayList3);
        int i8 = -1;
        TekLyricRowParseResult tekLyricRowParseResult4 = null;
        while (i < listCalcLyric.size()) {
            TekLyricWordParseResult tekLyricWordParseResult6 = tekLyricRowParseResult._wordArray.get(i);
            int iIntValue = listCalcLyric.get(i).intValue();
            if (i8 != iIntValue) {
                if (i8 != iIntValue - 1) {
                    throw new AssertionError("Row indexes are not consecutive");
                }
                long j6 = tekLyricWordParseResult6._startTime;
                TekLyricRowParseResult tekLyricRowParseResult5 = new TekLyricRowParseResult();
                tekLyricRowParseResult5._orignRowIndex = tekLyricRowParseResult._orignRowIndex;
                tekLyricRowParseResult5._wordArray = new ArrayList<>();
                long j7 = tekLyricRowParseResult._startTime + tekLyricWordParseResult6._startTime;
                tekLyricRowParseResult5._startTime = j7;
                tekLyricRowParseResult5._endTime = j7;
                tekLyricRowParseResult5._str = HttpUrl.FRAGMENT_ENCODE_SET;
                arrayList.add(tekLyricRowParseResult5);
                tekLyricRowParseResult4 = tekLyricRowParseResult5;
                j = j6;
                i8 = iIntValue;
            }
            if (tekLyricRowParseResult4 == null) {
                throw new AssertionError("Previous row info is null");
            }
            tekLyricRowParseResult4._str += tekLyricWordParseResult6._str;
            long j8 = tekLyricRowParseResult4._endTime;
            long j9 = tekLyricWordParseResult6._endTime;
            long j10 = tekLyricWordParseResult6._startTime;
            tekLyricRowParseResult4._endTime = j8 + (j9 - j10);
            tekLyricRowParseResult4._rowLength += tekLyricWordParseResult6._wordLength;
            tekLyricRowParseResult4._characterCount += tekLyricWordParseResult6._characterCount;
            tekLyricWordParseResult6._startTime = j10 - j;
            tekLyricWordParseResult6._endTime = j9 - j;
            tekLyricRowParseResult4._wordArray.add(tekLyricWordParseResult6);
            i++;
        }
    }

    static boolean isNeedPartition(TekLyricRowParseResult tekLyricRowParseResult, TekLyricBreakLineMode tekLyricBreakLineMode) {
        ArrayList<TekLyricWordParseResult> arrayList;
        if (tekLyricBreakLineMode != null && tekLyricRowParseResult != null && (arrayList = tekLyricRowParseResult._wordArray) != null && arrayList.size() > 1) {
            TekLyricBreakLineType tekLyricBreakLineType = tekLyricBreakLineMode._type;
            if (tekLyricBreakLineType == TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_BY_LENGTH && tekLyricRowParseResult._rowLength > tekLyricBreakLineMode._param) {
                return true;
            }
            if (tekLyricBreakLineType == TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_BY_WORDCOUNT && tekLyricRowParseResult._wordArray.size() > tekLyricBreakLineMode._param) {
                return true;
            }
            TekLyricBreakLineType tekLyricBreakLineType2 = tekLyricBreakLineMode._type;
            if (tekLyricBreakLineType2 == TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_AVERAGE_CHARACTER && tekLyricBreakLineMode._param > 1 && tekLyricRowParseResult._characterCount > tekLyricBreakLineMode._param2) {
                return true;
            }
            if (tekLyricBreakLineType2 == TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_AVERAGE_LENGTH && tekLyricBreakLineMode._param > 1 && tekLyricRowParseResult._rowLength > tekLyricBreakLineMode._param2) {
                return true;
            }
            if (tekLyricBreakLineType2 == TekLyricBreakLineType.TEK_LYRIC_BREAK_LINE_AVERAGE_LENGTH_MORE_LINE && tekLyricRowParseResult._wordArray.size() > 1 && tekLyricBreakLineMode._param > 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWordInfoTooLong(float f, TekLyricParam tekLyricParam) {
        TekLyricBreakLineMode tekLyricBreakLineMode = tekLyricParam._breakMode;
        if (f <= tekLyricBreakLineMode._param) {
            float f2 = tekLyricBreakLineMode._param2;
            if (f2 <= 0.0f || f <= f2) {
                return false;
            }
        }
        return true;
    }

    public static ArrayList<TekLyricRowParseResult> parse(TekLyricParam tekLyricParam) {
        TekFontParam tekFontParam;
        errorCode = 0;
        if (tekLyricParam != null && (tekFontParam = tekLyricParam._fontParam) != null && tekFontParam._tf != null && tekFontParam._fontSize >= 1) {
            if (tekLyricParam._krcData != null) {
                return parseKrc(tekLyricParam);
            }
            if (tekLyricParam._qrcData != null) {
                return parseQrc(tekLyricParam);
            }
        }
        return null;
    }

    /* JADX WARN: Code duplicated, block: B:38:0x0096  */
    /* JADX WARN: Code duplicated, block: B:39:0x00a4  */
    /* JADX WARN: Code duplicated, block: B:41:0x00a8  */
    public static ArrayList<TekLyricRowParseResult> parseKrc(TekLyricParam tekLyricParam) {
        TekFontParam tekFontParam;
        LyricData lyricData;
        Typeface typeface;
        String[] strArr;
        long[][] jArr;
        String[][] strArr2;
        long[] jArr2;
        long[][] jArr3;
        Paint paint;
        int i;
        long[][] jArr4;
        long[] jArr5;
        long[][] jArr6;
        Paint paint2;
        long[] jArr7;
        long j;
        if (tekLyricParam == null || (tekFontParam = tekLyricParam._fontParam) == null || (lyricData = tekLyricParam._krcData) == null || (typeface = tekFontParam._tf) == null || tekFontParam._fontSize < 1) {
            return null;
        }
        Paint paint3 = new Paint();
        paint3.setStyle(Paint.Style.FILL);
        paint3.setAntiAlias(true);
        paint3.setTextSize(tekLyricParam._fontParam._fontSize);
        paint3.setColor(Color.argb(1, 1, 1, 1));
        paint3.setTypeface(typeface);
        String[][] words = lyricData.getWords();
        long[] rowBeginTime = lyricData.getRowBeginTime();
        long[] rowDelayTime = lyricData.getRowDelayTime();
        long[][] wordBeginTime = lyricData.getWordBeginTime();
        long[][] wordDelayTime = lyricData.getWordDelayTime();
        if (words == null || rowBeginTime == null || rowDelayTime == null || wordBeginTime == null || wordDelayTime == null || words.length > rowBeginTime.length || words.length > rowDelayTime.length || words.length > wordBeginTime.length || words.length > wordDelayTime.length) {
            return null;
        }
        ArrayList<TekLyricRowParseResult> arrayList = new ArrayList<>();
        int i2 = 0;
        while (i2 < words.length) {
            TekLyricRowParseResult tekLyricRowParseResult = new TekLyricRowParseResult();
            long j2 = rowBeginTime[i2];
            tekLyricRowParseResult._startTime = j2;
            Paint paint4 = paint3;
            long j3 = j2 + rowDelayTime[i2];
            tekLyricRowParseResult._endTime = j3;
            long[] jArr8 = rowBeginTime;
            int i3 = i2;
            if (!tekLyricParam._isRemovePrelude) {
                if (tekLyricParam._isRemoveEnd) {
                    j = tekLyricParam._endPoint;
                    if (j <= 0) {
                    }
                }
                tekLyricRowParseResult._wordArray = new ArrayList<>();
                strArr = words[i3];
                long[] jArr9 = wordBeginTime[i3];
                long[] jArr10 = wordDelayTime[i3];
                return strArr != null ? null : null;
            }
            long j4 = tekLyricParam._preludePoint;
            if (j4 <= 0 || j3 > j4) {
                if (tekLyricParam._isRemoveEnd) {
                    j = tekLyricParam._endPoint;
                    if (j <= 0 && j2 >= j) {
                        jArr = wordDelayTime;
                        strArr2 = words;
                        jArr2 = rowDelayTime;
                        jArr3 = wordBeginTime;
                        paint = paint4;
                        i = i3;
                    }
                }
                tekLyricRowParseResult._wordArray = new ArrayList<>();
                strArr = words[i3];
                long[] jArr11 = wordBeginTime[i3];
                long[] jArr12 = wordDelayTime[i3];
                if (strArr != null || jArr11 == null || jArr12 == null || strArr.length > jArr11.length || strArr.length > jArr12.length) {
                    return null;
                }
                float f = 0.0f;
                String str = HttpUrl.FRAGMENT_ENCODE_SET;
                long j5 = 0;
                int i4 = 0;
                while (i4 < strArr.length) {
                    str = str + strArr[i4];
                    TekLyricWordParseResult tekLyricWordParseResult = new TekLyricWordParseResult();
                    long j6 = jArr11[i4];
                    tekLyricWordParseResult._startTime = j6;
                    tekLyricWordParseResult._endTime = jArr12[i4] + j6;
                    if (i4 < strArr.length - 1) {
                        tekLyricWordParseResult._endTime = jArr11[i4 + 1];
                    }
                    String str2 = strArr[i4];
                    tekLyricWordParseResult._str = str2;
                    if (TextUtils.isEmpty(str2)) {
                        jArr4 = wordDelayTime;
                        words = words;
                        jArr5 = rowDelayTime;
                        jArr6 = wordBeginTime;
                        paint2 = paint4;
                        jArr7 = jArr11;
                    } else {
                        paint2 = paint4;
                        float fMeasureText = paint2.measureText(tekLyricWordParseResult._str);
                        tekLyricWordParseResult._wordLength = fMeasureText;
                        f += fMeasureText;
                        tekLyricWordParseResult._wordIndex = tekLyricRowParseResult._wordArray.size();
                        tekLyricRowParseResult._wordArray.add(tekLyricWordParseResult);
                        jArr7 = jArr11;
                        if (i4 == 0) {
                            long j7 = tekLyricRowParseResult._startTime;
                            jArr4 = wordDelayTime;
                            long j8 = tekLyricWordParseResult._startTime;
                            tekLyricRowParseResult._startTime = j7 + j8;
                            j5 = j8;
                        } else {
                            jArr4 = wordDelayTime;
                        }
                        long j9 = tekLyricWordParseResult._startTime - j5;
                        tekLyricWordParseResult._startTime = j9;
                        long j10 = tekLyricWordParseResult._endTime - j5;
                        tekLyricWordParseResult._endTime = j10;
                        jArr5 = rowDelayTime;
                        jArr6 = wordBeginTime;
                        if (j9 < 0) {
                            tekLyricWordParseResult._startTime = 0L;
                        }
                        if (j10 < 0) {
                            tekLyricWordParseResult._endTime = 0L;
                        }
                        if (i4 == strArr.length - 1) {
                            long j11 = tekLyricRowParseResult._startTime;
                            long j12 = tekLyricWordParseResult._endTime;
                            tekLyricRowParseResult._endTime = j11 + j12;
                            if (j12 - tekLyricWordParseResult._startTime > 160) {
                                TekLog.write(LOGTAG, "wordInfo:" + str + ",d:" + ((tekLyricWordParseResult._endTime - tekLyricWordParseResult._startTime) / 40));
                            }
                        }
                    }
                    i4++;
                    jArr11 = jArr7;
                    words = words;
                    wordDelayTime = jArr4;
                    rowDelayTime = jArr5;
                    wordBeginTime = jArr6;
                    paint4 = paint2;
                }
                jArr = wordDelayTime;
                strArr2 = words;
                jArr2 = rowDelayTime;
                jArr3 = wordBeginTime;
                paint = paint4;
                tekLyricRowParseResult._rowLength = f;
                tekLyricRowParseResult._str = str;
                tekLyricRowParseResult._orignRowIndex = arrayList.size();
                arrayList.add(tekLyricRowParseResult);
                String str3 = LOGTAG;
                StringBuilder sb = new StringBuilder();
                sb.append("parse:rowInfo:");
                i = i3;
                sb.append(i);
                sb.append(",str:");
                sb.append(tekLyricRowParseResult._str);
                sb.append(",start:");
                sb.append(tekLyricRowParseResult._startTime);
                sb.append(",end:");
                sb.append(tekLyricRowParseResult._endTime);
                TekLog.write(str3, sb.toString());
            } else {
                jArr = wordDelayTime;
                strArr2 = words;
                jArr2 = rowDelayTime;
                jArr3 = wordBeginTime;
                paint = paint4;
                i = i3;
            }
            i2 = i + 1;
            paint3 = paint;
            rowBeginTime = jArr8;
            words = strArr2;
            wordDelayTime = jArr;
            rowDelayTime = jArr2;
            wordBeginTime = jArr3;
        }
        Paint paint5 = paint3;
        if (tekLyricParam._breakMode == null) {
            if (tekLyricParam._isAddSpace && !tekLyricParam._isLrc) {
                arrayList = addSpace(arrayList, tekLyricParam, paint5);
            }
            if (tekLyricParam._subRowSplit != null && arrayList != null) {
                for (int i5 = 0; i5 < arrayList.size(); i5++) {
                    tekLyricParam._subRowSplit.createSubRowInfo(arrayList.get(i5));
                }
            }
            return arrayList;
        }
        ArrayList<TekLyricRowParseResult> arrayListPartitionKrcRows = partitionKrcRows(arrayList, tekLyricParam, paint5);
        if (tekLyricParam._isAddSpace && !tekLyricParam._isLrc) {
            arrayListPartitionKrcRows = addSpace(arrayListPartitionKrcRows, tekLyricParam, paint5);
        }
        if (tekLyricParam._subRowSplit != null && arrayListPartitionKrcRows != null) {
            for (int i6 = 0; i6 < arrayListPartitionKrcRows.size(); i6++) {
                int iCreateSubRowInfo = tekLyricParam._subRowSplit.createSubRowInfo(arrayListPartitionKrcRows.get(i6));
                if (iCreateSubRowInfo < 0) {
                    errorCode = iCreateSubRowInfo;
                    return null;
                }
            }
        }
        return arrayListPartitionKrcRows;
    }

    /* JADX WARN: Code duplicated, block: B:31:0x008b A[Catch: JSONException -> 0x00bc, TryCatch #0 {JSONException -> 0x00bc, blocks: (B:22:0x0055, B:24:0x006f, B:28:0x0080, B:29:0x0083, B:31:0x008b, B:32:0x0093, B:34:0x00a0, B:35:0x00a8, B:25:0x0075, B:27:0x007b), top: B:51:0x0055 }] */
    /* JADX WARN: Code duplicated, block: B:34:0x00a0 A[Catch: JSONException -> 0x00bc, TryCatch #0 {JSONException -> 0x00bc, blocks: (B:22:0x0055, B:24:0x006f, B:28:0x0080, B:29:0x0083, B:31:0x008b, B:32:0x0093, B:34:0x00a0, B:35:0x00a8, B:25:0x0075, B:27:0x007b), top: B:51:0x0055 }] */
    public static ArrayList<TekLyricRowParseResult> parseQrc(TekLyricParam tekLyricParam) {
        TekFontParam tekFontParam;
        JSONArray jSONArray;
        Typeface typeface;
        String str;
        String str2;
        ArrayList<TekLyricWordParseResult> qrcWordArray;
        String string;
        if (tekLyricParam == null || (tekFontParam = tekLyricParam._fontParam) == null || (jSONArray = tekLyricParam._qrcData) == null || (typeface = tekFontParam._tf) == null || tekFontParam._fontSize < 1) {
            return null;
        }
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setTextSize(tekLyricParam._fontParam._fontSize);
        paint.setColor(Color.argb(1, 1, 1, 1));
        paint.setTypeface(typeface);
        ArrayList<TekLyricRowParseResult> arrayList = new ArrayList<>();
        for (int i = 0; i < jSONArray.length(); i++) {
            try {
                JSONArray jSONArray2 = jSONArray.getJSONArray(i);
                if (jSONArray2 == null || jSONArray2.length() != 4) {
                    TekLog.write(LOGTAG, "jsonRow not right");
                } else {
                    TekLyricRowParseResult tekLyricRowParseResult = new TekLyricRowParseResult();
                    try {
                        tekLyricRowParseResult._orignRowIndex = i;
                        tekLyricRowParseResult._startTime = jSONArray2.getInt(0);
                        tekLyricRowParseResult._endTime = ((long) jSONArray2.getInt(1)) + tekLyricRowParseResult._startTime;
                        JSONArray jSONArray3 = jSONArray2.getJSONArray(2);
                        if (jSONArray3 == null) {
                            str = LOGTAG;
                            str2 = "parseQrc wordArray == null";
                        } else {
                            if (jSONArray3.length() == 0) {
                                str = LOGTAG;
                                str2 = "parseQrc wordArray isEmpty";
                            }
                            qrcWordArray = parseQrcWordArray(jSONArray3, paint);
                            tekLyricRowParseResult._wordArray = qrcWordArray;
                            if (qrcWordArray == null) {
                                TekLog.write(LOGTAG, "parseQrc _wordArray == null");
                            }
                            string = jSONArray2.getString(3);
                            tekLyricRowParseResult._str = string;
                            if (TextUtils.isEmpty(string)) {
                                TekLog.write(LOGTAG, "parseQrc _str == null");
                            }
                            tekLyricRowParseResult._rowLength = paint.measureText(tekLyricRowParseResult._str);
                            tekLyricRowParseResult._characterCount = tekLyricRowParseResult._str.length();
                            arrayList.add(tekLyricRowParseResult);
                        }
                        TekLog.write(str, str2);
                        qrcWordArray = parseQrcWordArray(jSONArray3, paint);
                        tekLyricRowParseResult._wordArray = qrcWordArray;
                        if (qrcWordArray == null) {
                            TekLog.write(LOGTAG, "parseQrc _wordArray == null");
                        }
                        string = jSONArray2.getString(3);
                        tekLyricRowParseResult._str = string;
                        if (TextUtils.isEmpty(string)) {
                            TekLog.write(LOGTAG, "parseQrc _str == null");
                        }
                        tekLyricRowParseResult._rowLength = paint.measureText(tekLyricRowParseResult._str);
                        tekLyricRowParseResult._characterCount = tekLyricRowParseResult._str.length();
                        arrayList.add(tekLyricRowParseResult);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            } catch (JSONException e2) {
                e2.printStackTrace();
                return null;
            }
        }
        return tekLyricParam._breakMode != null ? partitionKrcRows(arrayList, tekLyricParam, paint) : arrayList;
    }

    public static ArrayList<TekLyricWordParseResult> parseQrcWordArray(JSONArray jSONArray, Paint paint) {
        if (paint == null || jSONArray == null || jSONArray.length() == 0) {
            return null;
        }
        ArrayList<TekLyricWordParseResult> arrayList = new ArrayList<>();
        for (int i = 0; i < jSONArray.length(); i++) {
            try {
                JSONArray jSONArray2 = jSONArray.getJSONArray(i);
                if (jSONArray2 != null && jSONArray2.length() == 3) {
                    TekLyricWordParseResult tekLyricWordParseResult = new TekLyricWordParseResult();
                    tekLyricWordParseResult._startTime = jSONArray2.getInt(0);
                    tekLyricWordParseResult._endTime = i < jSONArray.length() - 1 ? jSONArray.getJSONArray(i + 1).getInt(0) : ((long) jSONArray2.getInt(1)) + tekLyricWordParseResult._startTime;
                    String string = jSONArray2.getString(2);
                    tekLyricWordParseResult._str = string;
                    if (TextUtils.isEmpty(string)) {
                        tekLyricWordParseResult._wordLength = 0.0f;
                        tekLyricWordParseResult._characterCount = 0;
                    } else {
                        tekLyricWordParseResult._wordLength = paint.measureText(tekLyricWordParseResult._str);
                        tekLyricWordParseResult._characterCount = tekLyricWordParseResult._str.length();
                    }
                    arrayList.add(tekLyricWordParseResult);
                }
                return null;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }

    public static ArrayList<TekLyricRowParseResult> partitionKrcRows(ArrayList<TekLyricRowParseResult> arrayList, TekLyricParam tekLyricParam, Paint paint) {
        ArrayList<TekLyricRowParseResult> arrayList2 = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            TekLyricRowParseResult tekLyricRowParseResult = arrayList.get(i);
            checkLongWord(tekLyricRowParseResult, tekLyricParam, paint);
            innerPartitionOneRow(tekLyricRowParseResult, arrayList2, tekLyricParam._breakMode);
        }
        return arrayList2;
    }

    private static void partitionWordStrIntoArray(ArrayList<TekLyricWordParseResult> arrayList, TekLyricWordParseResult tekLyricWordParseResult, TekLyricParam tekLyricParam, Paint paint) {
        TekFontParam tekFontParam;
        int length;
        if (paint == null || arrayList == null || tekLyricWordParseResult == null || tekLyricParam == null || (tekFontParam = tekLyricParam._fontParam) == null || tekFontParam._fontSize < 1 || arrayList.size() > 30) {
            return;
        }
        if (!isWordInfoTooLong(tekLyricWordParseResult._wordLength, tekLyricParam)) {
            tekLyricWordParseResult._wordIndex = arrayList.size();
            arrayList.add(tekLyricWordParseResult);
            return;
        }
        String str = tekLyricWordParseResult._str;
        if (TextUtils.isEmpty(str)) {
            return;
        }
        int i = 1;
        while (true) {
            if (i > str.length()) {
                length = 0;
                break;
            }
            String strSubstring = str.substring(0, i);
            if (isWordInfoTooLong(TextUtils.isEmpty(strSubstring) ? 0.0f : paint.measureText(strSubstring), tekLyricParam)) {
                length = i - 1;
                break;
            }
            i++;
        }
        if (length <= 1 || length > str.length() - 2) {
            length = str.length() / 2;
        }
        if (length <= 0) {
            return;
        }
        TekLyricWordParseResult tekLyricWordParseResult2 = new TekLyricWordParseResult();
        String strSubstring2 = str.substring(0, length);
        tekLyricWordParseResult2._str = strSubstring2;
        if (!TextUtils.isEmpty(strSubstring2)) {
            tekLyricWordParseResult2._wordLength = paint.measureText(tekLyricWordParseResult2._str);
            tekLyricWordParseResult2._characterCount = tekLyricWordParseResult2._str.length();
            long j = tekLyricWordParseResult._startTime;
            tekLyricWordParseResult2._startTime = j;
            tekLyricWordParseResult2._endTime = (long) (j + (((tekLyricWordParseResult._endTime - tekLyricWordParseResult._startTime) * tekLyricWordParseResult2._wordLength) / tekLyricWordParseResult._wordLength));
            tekLyricWordParseResult2._wordIndex = arrayList.size();
            arrayList.add(tekLyricWordParseResult2);
        }
        TekLyricWordParseResult tekLyricWordParseResult3 = new TekLyricWordParseResult();
        String strSubstring3 = str.substring(length);
        tekLyricWordParseResult3._str = strSubstring3;
        if (TextUtils.isEmpty(strSubstring3)) {
            return;
        }
        tekLyricWordParseResult3._wordLength = paint.measureText(tekLyricWordParseResult3._str);
        tekLyricWordParseResult3._characterCount = tekLyricWordParseResult3._str.length();
        tekLyricWordParseResult3._startTime = tekLyricWordParseResult2._endTime;
        tekLyricWordParseResult3._endTime = tekLyricWordParseResult._endTime;
        partitionWordStrIntoArray(arrayList, tekLyricWordParseResult3, tekLyricParam, paint);
    }

    static int spaceCountInStringTail(String str) {
        int i = 0;
        for (int length = str.length() - 1; length >= 1 && str.charAt(length) == ' '; length--) {
            i++;
        }
        return i;
    }
}
