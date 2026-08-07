package com.tencent.qqmusic.business.playernew.fxeffect.custom.util;

import android.opengl.GLES20;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordProxyResult;
import com.tencent.qqmusic.sword.SwordSwitches;
import android.util.Log;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes19.dex */
public class b {
    public static int a(String str, String str2) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr != null && ((bArr[1302] >> 4) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{str, str2}, (Object) null, 178421);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Integer) swordProxyResultProxyMoreArgs.result).intValue();
            }
        }
        int iE = e(d(str), b(str2));
        f(iE);
        return iE;
    }

    public static int b(String str) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr != null && ((bArr[1294] >> 5) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(str, (Object) null, 178358);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Integer) swordProxyResultProxyOneArg.result).intValue();
            }
        }
        return c(35632, str);
    }

    private static int c(int i, String str) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr != null && ((bArr[1295] >> 5) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), str}, (Object) null, 178366);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Integer) swordProxyResultProxyMoreArgs.result).intValue();
            }
        }
        int iGlCreateShader = GLES20.glCreateShader(i);
        if (iGlCreateShader == 0) {
            Log.w("ShaderHelper", "Could not create new shader.");
            return 0;
        }
        GLES20.glShaderSource(iGlCreateShader, str);
        GLES20.glCompileShader(iGlCreateShader);
        int[] iArr = new int[1];
        GLES20.glGetShaderiv(iGlCreateShader, 35713, iArr, 0);
        Log.v("ShaderHelper", "Results of compiling source:\n" + str + "\n:" + GLES20.glGetShaderInfoLog(iGlCreateShader));
        if (iArr[0] != 0) {
            return iGlCreateShader;
        }
        GLES20.glDeleteShader(iGlCreateShader);
        Log.w("ShaderHelper", "Compilation of shader failed.");
        return 0;
    }

    public static int d(String str) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr != null && ((bArr[1292] >> 6) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(str, (Object) null, 178343);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Integer) swordProxyResultProxyOneArg.result).intValue();
            }
        }
        return c(35633, str);
    }

    public static int e(int i, int i2) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr != null && ((bArr[1297] >> 7) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyMoreArgs = SwordProxy.proxyMoreArgs(new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}, (Object) null, 178384);
            if (swordProxyResultProxyMoreArgs.isSupported) {
                return ((Integer) swordProxyResultProxyMoreArgs.result).intValue();
            }
        }
        int iGlCreateProgram = GLES20.glCreateProgram();
        if (iGlCreateProgram == 0) {
            Log.w("ShaderHelper", "Could not create new program");
            return 0;
        }
        GLES20.glAttachShader(iGlCreateProgram, i);
        GLES20.glAttachShader(iGlCreateProgram, i2);
        GLES20.glLinkProgram(iGlCreateProgram);
        int[] iArr = new int[1];
        GLES20.glGetProgramiv(iGlCreateProgram, 35714, iArr, 0);
        Log.v("ShaderHelper", "Results of linking program:\n" + GLES20.glGetProgramInfoLog(iGlCreateProgram));
        if (iArr[0] != 0) {
            return iGlCreateProgram;
        }
        GLES20.glDeleteProgram(iGlCreateProgram);
        Log.w("ShaderHelper", "Linking of program failed.");
        return 0;
    }

    public static boolean f(int i) {
        byte[] bArr = SwordSwitches.switches16;
        if (bArr != null && ((bArr[1300] >> 5) & 1) > 0) {
            SwordProxyResult swordProxyResultProxyOneArg = SwordProxy.proxyOneArg(Integer.valueOf(i), (Object) null, 178406);
            if (swordProxyResultProxyOneArg.isSupported) {
                return ((Boolean) swordProxyResultProxyOneArg.result).booleanValue();
            }
        }
        GLES20.glValidateProgram(i);
        int[] iArr = new int[1];
        GLES20.glGetProgramiv(i, 35715, iArr, 0);
        Log.v("ShaderHelper", "Results of validating program: " + iArr[0] + "\nLog:" + GLES20.glGetProgramInfoLog(i));
        return iArr[0] != 0;
    }
}
