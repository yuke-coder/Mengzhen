package TekEngineLib.Lyric;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekLyricDivider {
    public static List<Integer> calcLyric(List<Integer> list, int i) {
        Iterator<Integer> it = list.iterator();
        int iIntValue = 0;
        while (it.hasNext()) {
            iIntValue += it.next().intValue();
        }
        float f = (iIntValue * 1.0f) / i;
        ArrayList arrayList = new ArrayList(i);
        for (int i2 = 0; i2 < i; i2++) {
            arrayList.add(Float.valueOf(f));
        }
        return calcLyric(list, arrayList);
    }

    public static List<Integer> calcLyric(List<Integer> list, List<Float> list2) {
        ArrayList arrayList = new ArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            arrayList.add(0);
        }
        if (!list.isEmpty() && !list2.isEmpty()) {
            int size = list.size();
            int size2 = list2.size();
            if (size <= size2) {
                for (int i2 = 0; i2 < size; i2++) {
                    arrayList.set(i2, Integer.valueOf(i2));
                }
                return arrayList;
            }
            int i3 = size2 + 1;
            int i4 = size + 1;
            float[][] fArr = (float[][]) Array.newInstance((Class<?>) Float.TYPE, i3, i4);
            int[][] iArr = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, i3, i4);
            for (int i5 = 0; i5 <= size2; i5++) {
                for (int i6 = 0; i6 <= size; i6++) {
                    fArr[i5][i6] = 1.7014117E38f;
                }
            }
            float fIntValue = 0.0f;
            for (int i7 = 1; i7 <= size; i7++) {
                fIntValue += list.get(i7 - 1).intValue();
                fArr[1][i7] = Math.abs(list2.get(0).floatValue() - fIntValue);
                iArr[1][i7] = 1;
            }
            for (int i8 = 2; i8 <= size2; i8++) {
                int i9 = i8 - 1;
                float fFloatValue = list2.get(i9).floatValue();
                for (int i10 = i8; i10 <= size; i10++) {
                    float fIntValue2 = 0.0f;
                    for (int i11 = i10; i11 >= i8; i11--) {
                        int i12 = i11 - 1;
                        fIntValue2 += list.get(i12).intValue();
                        float fAbs = fArr[i9][i12] + Math.abs(fFloatValue - fIntValue2);
                        float[] fArr2 = fArr[i8];
                        if (fAbs < fArr2[i10]) {
                            fArr2[i10] = fAbs;
                            iArr[i8][i10] = i11;
                        }
                    }
                }
            }
            while (size > 0 && size2 > 0) {
                int i13 = iArr[size2][size];
                for (int i14 = i13; i14 <= size; i14++) {
                    arrayList.set(i14 - 1, Integer.valueOf(size2 - 1));
                }
                size = i13 - 1;
                size2--;
            }
        }
        return arrayList;
    }
}
