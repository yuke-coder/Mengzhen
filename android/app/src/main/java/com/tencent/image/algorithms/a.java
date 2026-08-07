package com.tencent.image.algorithms;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import com.tencent.qqmusiccommon.util.MLog;
import java.lang.reflect.Array;

/* loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes12.dex */
public class a {
    private static void a(int[] iArr, int[] iArr2, int i, int i2, float f) {
        int i3 = i - 1;
        int i4 = (int) f;
        int i5 = (i4 * 2) + 1;
        int i6 = i5 * 256;
        int[] iArr3 = new int[i6];
        int i7 = 0;
        for (int i8 = 0; i8 < i6; i8++) {
            iArr3[i8] = i8 / i5;
        }
        int i9 = 0;
        int i10 = 0;
        while (i9 < i2) {
            int i11 = i7;
            int i12 = i11;
            int i13 = i12;
            int i14 = i13;
            for (int i15 = -i4; i15 <= i4; i15++) {
                int i16 = iArr[j(i15, i7, i3) + i10];
                i11 += (i16 >> 24) & 255;
                i12 += (i16 >> 16) & 255;
                i13 += (i16 >> 8) & 255;
                i14 += i16 & 255;
            }
            int i17 = i9;
            int i18 = i7;
            while (i18 < i) {
                iArr2[i17] = (iArr3[i11] << 24) | (iArr3[i12] << 16) | (iArr3[i13] << 8) | iArr3[i14];
                int i19 = i18 + i4 + 1;
                if (i19 > i3) {
                    i19 = i3;
                }
                int i20 = i18 - i4;
                if (i20 < 0) {
                    i20 = i7;
                }
                int i21 = iArr[i19 + i10];
                int i22 = iArr[i20 + i10];
                i11 += ((i21 >> 24) & 255) - ((i22 >> 24) & 255);
                i12 += ((i21 & 16711680) - (16711680 & i22)) >> 16;
                i13 += ((i21 & 65280) - (65280 & i22)) >> 8;
                i14 += (i21 & 255) - (i22 & 255);
                i17 += i2;
                i18++;
                i3 = i3;
                i7 = 0;
            }
            i10 += i;
            i9++;
            i7 = 0;
        }
    }

    public static Bitmap b(Bitmap bitmap, int i, int i2) {
        return c(bitmap, i, i2, 5, 5);
    }

    public static Bitmap c(Bitmap bitmap, int i, int i2, int i3, int i4) {
        if (bitmap == null) {
            return null;
        }
        try {
            Bitmap bitmapM = m(bitmap, i, i2);
            Bitmap bitmapI = i(bitmapM, 3, i3, i4);
            if (bitmapM != bitmap && bitmapM != null && !bitmapM.isRecycled()) {
                bitmapM.recycle();
            }
            if (bitmapI == null) {
                return null;
            }
            int width = (int) (bitmapI.getWidth() * 0.03f);
            int height = (int) (bitmapI.getHeight() * 0.03f);
            if (height <= 0) {
                return bitmapI;
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmapI, width, height, bitmapI.getWidth() - (width << 1), bitmapI.getHeight() - (height << 1));
            if (!bitmapI.isRecycled()) {
                bitmapI.recycle();
            }
            return bitmapCreateBitmap;
        } catch (Throwable th) {
            MLog.e("BitmapAlgorithms", th);
            return null;
        }
    }

    public static Bitmap d(Context context, Bitmap bitmap, float f, float f2) {
        Bitmap bitmapM;
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int i = (int) (width / f2);
            int i2 = (int) (height / f2);
            Bitmap bitmapM2 = m(bitmap, (i - (i % 4)) + 4, (i2 - (i2 % 4)) + 4);
            if (bitmapM2 != null) {
                Bitmap.Config config = bitmapM2.getConfig();
                Bitmap.Config config2 = Bitmap.Config.ARGB_8888;
                if (config != config2) {
                    bitmapM2 = bitmapM2.copy(config2, false);
                }
                try {
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    bitmapM2 = g(context, bitmapM2, f);
                    MLog.d("BitmapAlgorithms", "[blurRs] blurBitmap Time:" + (System.currentTimeMillis() - jCurrentTimeMillis));
                } catch (RSRuntimeException unused) {
                    long jCurrentTimeMillis2 = System.currentTimeMillis();
                    bitmapM2 = h(bitmapM2, (int) f, true);
                    MLog.d("BitmapAlgorithms", "[blurStack] blurBitmap Time:" + (System.currentTimeMillis() - jCurrentTimeMillis2));
                }
                bitmapM = m(bitmapM2, width, height);
            } else {
                bitmapM = null;
            }
            return bitmapM;
        } catch (Throwable th) {
            MLog.e("BitmapAlgorithms", th);
            return bitmap;
        }
    }

    public static Bitmap e(Context context, Bitmap bitmap, float f, float f2, float f3, float f4) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        float f5 = f3 < 0.0f ? 0.0f : f3;
        float fMin = f4 > 1.0f ? 1.0f : f4;
        if (fMin <= f5) {
            fMin = Math.min(1.0f, 1.0E-4f + f5);
        }
        Bitmap bitmapD = null;
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            bitmapD = d(context, bitmap, f, f2);
            if (bitmapD == null) {
                return null;
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            canvas.drawBitmap(bitmapD, 0.0f, 0.0f, new Paint(3));
            float f6 = width;
            float f7 = f6 * 0.5f;
            float f8 = height;
            float f9 = f8 * 0.5f;
            float fSqrt = (float) Math.sqrt((f7 * f7) + (f9 * f9));
            RadialGradient radialGradient = new RadialGradient(f7, f9, fSqrt <= 0.0f ? 1.0f : fSqrt, new int[]{-1, -1, 16777215}, new float[]{f5, f5, fMin}, Shader.TileMode.CLAMP);
            Paint paint = new Paint(1);
            paint.setShader(radialGradient);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.drawRect(0.0f, 0.0f, f6, f8, paint);
            paint.setXfermode(null);
            return bitmapCreateBitmap;
        } catch (OutOfMemoryError e) {
            MLog.e("BitmapAlgorithms", e);
            return null;
        } catch (Throwable th) {
            MLog.e("BitmapAlgorithms", th);
            return null;
        } finally {
            if (bitmapD != null && bitmapD != bitmap && !bitmapD.isRecycled()) {
                try {
                    bitmapD.recycle();
                } catch (Throwable unused) {
                }
            }
        }
    }

    private static void f(int[] iArr, int[] iArr2, int i, int i2, float f) {
        int i3;
        float f2 = f - ((int) f);
        float f3 = 1.0f / ((2.0f * f2) + 1.0f);
        char c2 = 0;
        int i4 = 0;
        int i5 = 0;
        while (i4 < i2) {
            iArr2[i4] = iArr[c2];
            int i6 = i4 + i2;
            int i7 = 1;
            int i8 = 1;
            i3 = i - 1;
            while (i8 < i3) {
                int i9 = i5 + i8;
                int i10 = iArr[i9 - 1];
                int i11 = iArr[i9];
                int i12 = iArr[i9 + i7];
                int i13 = (i11 >> 24) & 255;
                int i14 = (i11 >> 8) & 255;
                iArr2[i6] = (((int) ((((i11 >> 16) & 255) + ((int) ((((i10 >> 16) & 255) + ((i12 >> 16) & 255)) * f2))) * f3)) << 16) | (((int) ((i13 + ((int) ((((i10 >> 24) & 255) + ((i12 >> 24) & 255)) * f2))) * f3)) << 24) | (((int) ((i14 + ((int) ((((i10 >> 8) & 255) + ((i12 >> 8) & 255)) * f2))) * f3)) << 8) | ((int) (((i11 & 255) + ((int) (((i10 & 255) + (i12 & 255)) * f2))) * f3));
                i6 += i2;
                i8++;
                i4 = i4;
                i5 = i5;
                i7 = 1;
            }
            iArr2[i6] = iArr[i3];
            i5 += i;
            i4++;
            c2 = 0;
        }
    }

    private static Bitmap g(Context context, Bitmap bitmap, float f) throws Throwable {
        RenderScript renderScript = null;
        Allocation allocationCreateFromBitmap = null;
        Allocation allocationCreateTyped = null;
        ScriptIntrinsicBlur blur = null;
        try {
            renderScript = RenderScript.create(context);
            allocationCreateFromBitmap = Allocation.createFromBitmap(
                    renderScript,
                    bitmap,
                    Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT);
            allocationCreateTyped = Allocation.createTyped(
                    renderScript,
                    allocationCreateFromBitmap.getType());
            blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            blur.setInput(allocationCreateFromBitmap);
            blur.setRadius(f);
            blur.forEach(allocationCreateTyped);
            allocationCreateTyped.copyTo(bitmap);
            return bitmap;
        } finally {
            if (blur != null) {
                blur.destroy();
            }
            if (allocationCreateTyped != null) {
                allocationCreateTyped.destroy();
            }
            if (allocationCreateFromBitmap != null) {
                allocationCreateFromBitmap.destroy();
            }
            if (renderScript != null) {
                renderScript.destroy();
            }
        }
    }

    private static Bitmap h(Bitmap bitmap, int i, boolean z) {
        int[] iArr;
        int i2 = i;
        Bitmap bitmapCopy = z ? bitmap : bitmap.copy(bitmap.getConfig(), true);
        if (i2 < 1) {
            return bitmap;
        }
        int width = bitmapCopy.getWidth();
        int height = bitmapCopy.getHeight();
        int i3 = width * height;
        int[] iArr2 = new int[i3];
        bitmapCopy.getPixels(iArr2, 0, width, 0, 0, width, height);
        int i4 = width - 1;
        int i5 = height - 1;
        int i6 = i2 + i2 + 1;
        int[] iArr3 = new int[i3];
        int[] iArr4 = new int[i3];
        int[] iArr5 = new int[i3];
        int[] iArr6 = new int[Math.max(width, height)];
        int i7 = (i6 + 1) >> 1;
        int i8 = i7 * i7;
        int i9 = i8 * 256;
        int[] iArr7 = new int[i9];
        for (int i10 = 0; i10 < i9; i10++) {
            iArr7[i10] = i10 / i8;
        }
        int[][] iArr8 = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, i6, 3);
        int i11 = i2 + 1;
        int i12 = 0;
        int i13 = 0;
        int i14 = 0;
        while (i12 < height) {
            Bitmap bitmap2 = bitmapCopy;
            int i15 = height;
            int i16 = 0;
            int i17 = 0;
            int i18 = 0;
            int i19 = 0;
            int i20 = 0;
            int i21 = 0;
            int i22 = 0;
            int i23 = 0;
            int i24 = -i2;
            int i25 = 0;
            while (i24 <= i2) {
                int i26 = i5;
                int[] iArr9 = iArr6;
                int i27 = iArr2[i13 + Math.min(i4, Math.max(i24, 0))];
                int[] iArr10 = iArr8[i24 + i2];
                iArr10[0] = (i27 & 16711680) >> 16;
                iArr10[1] = (i27 & 65280) >> 8;
                iArr10[2] = i27 & 255;
                int iAbs = i11 - Math.abs(i24);
                int i28 = iArr10[0];
                i25 += i28 * iAbs;
                int i29 = iArr10[1];
                i16 += i29 * iAbs;
                int i30 = iArr10[2];
                i17 += iAbs * i30;
                if (i24 > 0) {
                    i21 += i28;
                    i22 += i29;
                    i23 += i30;
                } else {
                    i18 += i28;
                    i19 += i29;
                    i20 += i30;
                }
                i24++;
                i5 = i26;
                iArr6 = iArr9;
            }
            int i31 = i5;
            int[] iArr11 = iArr6;
            int i32 = i2;
            int i33 = i25;
            int i34 = 0;
            while (i34 < width) {
                iArr3[i13] = iArr7[i33];
                iArr4[i13] = iArr7[i16];
                iArr5[i13] = iArr7[i17];
                int i35 = i33 - i18;
                int i36 = i16 - i19;
                int i37 = i17 - i20;
                int[] iArr12 = iArr8[((i32 - i2) + i6) % i6];
                int i38 = i18 - iArr12[0];
                int i39 = i19 - iArr12[1];
                int i40 = i20 - iArr12[2];
                if (i12 == 0) {
                    iArr = iArr7;
                    iArr11[i34] = Math.min(i34 + i2 + 1, i4);
                } else {
                    iArr = iArr7;
                }
                int i41 = iArr2[i14 + iArr11[i34]];
                int i42 = (i41 & 16711680) >> 16;
                iArr12[0] = i42;
                int i43 = (i41 & 65280) >> 8;
                iArr12[1] = i43;
                int i44 = i41 & 255;
                iArr12[2] = i44;
                int i45 = i21 + i42;
                int i46 = i22 + i43;
                int i47 = i23 + i44;
                i33 = i35 + i45;
                i16 = i36 + i46;
                i17 = i37 + i47;
                i32 = (i32 + 1) % i6;
                int[] iArr13 = iArr8[i32 % i6];
                int i48 = iArr13[0];
                i18 = i38 + i48;
                int i49 = iArr13[1];
                i19 = i39 + i49;
                int i50 = iArr13[2];
                i20 = i40 + i50;
                i21 = i45 - i48;
                i22 = i46 - i49;
                i23 = i47 - i50;
                i13++;
                i34++;
                iArr7 = iArr;
            }
            i14 += width;
            i12++;
            bitmapCopy = bitmap2;
            height = i15;
            i5 = i31;
            iArr6 = iArr11;
        }
        Bitmap bitmap3 = bitmapCopy;
        int i51 = i5;
        int[] iArr14 = iArr6;
        int i52 = height;
        int[] iArr15 = iArr7;
        int i53 = 0;
        while (i53 < width) {
            int i54 = -i2;
            int i55 = i6;
            int[] iArr16 = iArr2;
            int i56 = 0;
            int i57 = 0;
            int i58 = 0;
            int i59 = 0;
            int i60 = 0;
            int i61 = 0;
            int i62 = 0;
            int i63 = i54;
            int i64 = i54 * width;
            int i65 = 0;
            int i66 = 0;
            while (i63 <= i2) {
                int i67 = width;
                int iMax = Math.max(0, i64) + i53;
                int[] iArr17 = iArr8[i63 + i2];
                iArr17[0] = iArr3[iMax];
                iArr17[1] = iArr4[iMax];
                iArr17[2] = iArr5[iMax];
                int iAbs2 = i11 - Math.abs(i63);
                i65 += iArr3[iMax] * iAbs2;
                i66 += iArr4[iMax] * iAbs2;
                i56 += iArr5[iMax] * iAbs2;
                if (i63 > 0) {
                    i60 += iArr17[0];
                    i61 += iArr17[1];
                    i62 += iArr17[2];
                } else {
                    i57 += iArr17[0];
                    i58 += iArr17[1];
                    i59 += iArr17[2];
                }
                int i68 = i51;
                if (i63 < i68) {
                    i64 += i67;
                }
                i63++;
                i51 = i68;
                width = i67;
            }
            int i69 = width;
            int i70 = i51;
            int i71 = i2;
            int i72 = i53;
            int i73 = i66;
            int i74 = i52;
            int i75 = i65;
            int i76 = 0;
            while (i76 < i74) {
                iArr16[i72] = (iArr16[i72] & (-16777216)) | (iArr15[i75] << 16) | (iArr15[i73] << 8) | iArr15[i56];
                int i77 = i75 - i57;
                int i78 = i73 - i58;
                int i79 = i56 - i59;
                int[] iArr18 = iArr8[((i71 - i2) + i55) % i55];
                int i80 = i57 - iArr18[0];
                int i81 = i58 - iArr18[1];
                int i82 = i59 - iArr18[2];
                if (i53 == 0) {
                    iArr14[i76] = Math.min(i76 + i11, i70) * i69;
                }
                int i83 = iArr14[i76] + i53;
                int i84 = iArr3[i83];
                iArr18[0] = i84;
                int i85 = iArr4[i83];
                iArr18[1] = i85;
                int i86 = iArr5[i83];
                iArr18[2] = i86;
                int i87 = i60 + i84;
                int i88 = i61 + i85;
                int i89 = i62 + i86;
                i75 = i77 + i87;
                i73 = i78 + i88;
                i56 = i79 + i89;
                i71 = (i71 + 1) % i55;
                int[] iArr19 = iArr8[i71];
                int i90 = iArr19[0];
                i57 = i80 + i90;
                int i91 = iArr19[1];
                i58 = i81 + i91;
                int i92 = iArr19[2];
                i59 = i82 + i92;
                i60 = i87 - i90;
                i61 = i88 - i91;
                i62 = i89 - i92;
                i72 += i69;
                i76++;
                i2 = i;
            }
            i53++;
            i2 = i;
            i51 = i70;
            i52 = i74;
            i6 = i55;
            iArr2 = iArr16;
            width = i69;
        }
        int i93 = width;
        bitmap3.setPixels(iArr2, 0, i93, 0, 0, i93, i52);
        return bitmap3;
    }

    public static Bitmap i(Bitmap bitmap, int i, int i2, int i3) {
        Bitmap bitmapCreateBitmap = null;
        if (bitmap == null) {
            return null;
        }
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int i4 = width * height;
            int[] iArr = new int[i4];
            int[] iArr2 = new int[i4];
            bitmapCreateBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.getPixels(iArr, 0, width, 0, 0, width, height);
            for (int i5 = 0; i5 < i; i5++) {
                a(iArr, iArr2, width, height, i2);
                a(iArr2, iArr, height, width, i3);
            }
            f(iArr, iArr2, width, height, i2);
            f(iArr2, iArr, height, width, i3);
            bitmapCreateBitmap.setPixels(iArr, 0, width, 0, 0, width, height);
        } catch (OutOfMemoryError e) {
            MLog.e("BitmapAlgorithms", e);
        } catch (Error e2) {
            MLog.e("BitmapAlgorithms", e2);
        } catch (Exception e3) {
            MLog.e("BitmapAlgorithms", e3);
        }
        return bitmapCreateBitmap;
    }

    private static int j(int i, int i2, int i3) {
        return i < i2 ? i2 : i > i3 ? i3 : i;
    }

    public static Bitmap k(Bitmap bitmap, int i, int i2, int i3, int i4) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (i < 0 || i2 < 0 || i3 <= 0 || i4 <= 0 || i + i3 > width || i2 + i4 > height) {
            return null;
        }
        try {
            return Bitmap.createBitmap(bitmap, i, i2, i3, i4);
        } catch (Exception e) {
            MLog.e("BitmapAlgorithms", e);
            return null;
        } catch (OutOfMemoryError e2) {
            MLog.e("BitmapAlgorithms", e2);
            return null;
        } catch (Throwable th) {
            MLog.e("BitmapAlgorithms", th);
            return null;
        }
    }

    public static Bitmap l(Bitmap bitmap, int i, int i2) {
        int i3;
        float f;
        if (bitmap != null && i > 0 && i2 > 0) {
            try {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                if (width == i && height == i2) {
                    return bitmap;
                }
                Matrix matrix = new Matrix();
                int i4 = 0;
                if (width * i2 > i * height) {
                    f = (i2 * 1.0f) / height;
                    i3 = (int) ((width - (i / f)) * 0.5f);
                } else {
                    float f2 = (i * 1.0f) / width;
                    i3 = 0;
                    i4 = (int) (height - (i2 / f2));
                    f = f2;
                }
                matrix.setScale(f, f);
                return Bitmap.createBitmap(bitmap, i3, 0, width - i3, height - i4, matrix, true);
            } catch (Throwable th) {
                MLog.e("BitmapAlgorithms", th);
            }
        }
        return bitmap;
    }

    public static Bitmap m(Bitmap bitmap, int i, int i2) {
        if (bitmap == null) {
            return null;
        }
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float f = i / width;
            float f2 = i2 / height;
            if (f == 1.0f && f2 == 1.0f) {
                return bitmap;
            }
            Matrix matrix = new Matrix();
            matrix.postScale(f, f2);
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        } catch (OutOfMemoryError e) {
            MLog.e("BitmapAlgorithms", e);
            return null;
        } catch (Error e2) {
            MLog.e("BitmapAlgorithms", e2);
            return null;
        } catch (Exception e3) {
            MLog.e("BitmapAlgorithms", e3);
            return null;
        }
    }
}
