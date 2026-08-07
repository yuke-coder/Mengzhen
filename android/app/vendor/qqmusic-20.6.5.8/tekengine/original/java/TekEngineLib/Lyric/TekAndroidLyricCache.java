package TekEngineLib.Lyric;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekAndroidLyricCache {
    private static volatile TekAndroidLyricCache _instance;
    private Map<Long, ArrayList<TekLyricRowParseResult>> _rowCaches = new HashMap();
    private Map<Long, TekLyricParam> _lyricParams = new HashMap();
    private Map<Long, TekFontParam> _fontParams = new HashMap();
    private Map<Long, TekTextParam> _headlines = new HashMap();
    private Map<Long, TekTextParam> _singers = new HashMap();

    private TekAndroidLyricCache() {
    }

    public static TekAndroidLyricCache getInstance() {
        if (_instance == null) {
            synchronized (TekAndroidLyricCache.class) {
                _instance = new TekAndroidLyricCache();
            }
        }
        return _instance;
    }

    public void addKrcRows(ArrayList<TekLyricRowParseResult> arrayList, Long l) {
        synchronized (this) {
            this._rowCaches.put(l, arrayList);
        }
    }

    public void clean(Long l) {
        synchronized (this) {
            this._rowCaches.remove(l);
            this._fontParams.remove(l);
            this._headlines.remove(l);
            this._singers.remove(l);
        }
    }

    public TekFontParam getFontParam(Long l) {
        TekFontParam tekFontParam;
        synchronized (this) {
            tekFontParam = this._fontParams.get(l);
        }
        return tekFontParam;
    }

    public TekTextParam getHeadline(Long l) {
        TekTextParam tekTextParam;
        synchronized (this) {
            tekTextParam = this._headlines.get(l);
        }
        return tekTextParam;
    }

    public ArrayList<TekLyricRowParseResult> getKrcRows(Long l) {
        ArrayList<TekLyricRowParseResult> arrayList;
        synchronized (this) {
            arrayList = this._rowCaches.get(l);
        }
        return arrayList;
    }

    public TekLyricParam getLyricParam(Long l) {
        TekLyricParam tekLyricParam;
        synchronized (this) {
            tekLyricParam = this._lyricParams.get(l);
        }
        return tekLyricParam;
    }

    public TekTextParam getSinger(Long l) {
        TekTextParam tekTextParam;
        synchronized (this) {
            tekTextParam = this._singers.get(l);
        }
        return tekTextParam;
    }

    public String getText(int i, int i2, Long l) {
        ArrayList<TekLyricRowParseResult> arrayList;
        synchronized (this) {
            arrayList = this._rowCaches.get(l);
        }
        if (arrayList == null || i >= arrayList.size()) {
            return null;
        }
        TekLyricRowParseResult tekLyricRowParseResult = arrayList.get(i);
        if (i2 < 0) {
            return tekLyricRowParseResult._str;
        }
        ArrayList<TekLyricWordParseResult> arrayList2 = tekLyricRowParseResult._wordArray;
        if (arrayList2 == null || i2 >= arrayList2.size()) {
            return null;
        }
        return tekLyricRowParseResult._wordArray.get(i2)._str;
    }

    public List<List<String>> getTexts(List<List<Integer>> list, Long l) {
        ArrayList<TekLyricRowParseResult> arrayList;
        synchronized (this) {
            arrayList = this._rowCaches.get(l);
        }
        if (arrayList == null) {
            return null;
        }
        ArrayList arrayList2 = new ArrayList(list.size());
        for (List<Integer> list2 : list) {
            ArrayList arrayList3 = new ArrayList();
            Iterator<Integer> it = list2.iterator();
            while (it.hasNext()) {
                String str = arrayList.get(it.next().intValue())._str;
                if (!TextUtils.isEmpty(str)) {
                    arrayList3.add(str);
                }
            }
            if (!arrayList3.isEmpty()) {
                arrayList2.add(arrayList3);
            }
        }
        return arrayList2;
    }

    public long setFontParam(TekFontParam tekFontParam, Long l) {
        if (tekFontParam == null || tekFontParam._tf == null) {
            return 0L;
        }
        boolean z = true;
        if (tekFontParam._fontSize < 1) {
            return 0L;
        }
        long jLongValue = l.longValue();
        synchronized (this) {
            Iterator<Long> it = this._fontParams.keySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    z = false;
                    break;
                }
                Long next = it.next();
                TekFontParam tekFontParam2 = this._fontParams.get(next);
                if (tekFontParam2 != null && tekFontParam.equals(tekFontParam2)) {
                    jLongValue = next.longValue();
                    break;
                }
            }
            if (!z) {
                this._fontParams.put(l, tekFontParam);
            }
        }
        return jLongValue;
    }

    public void setHeadline(TekTextParam tekTextParam, Long l) {
        synchronized (this) {
            this._headlines.put(l, tekTextParam);
        }
    }

    public void setLyricParam(TekLyricParam tekLyricParam, Long l) {
        synchronized (this) {
            this._lyricParams.put(l, tekLyricParam);
        }
    }

    public void setSinger(TekTextParam tekTextParam, Long l) {
        synchronized (this) {
            this._singers.put(l, tekTextParam);
        }
    }
}
