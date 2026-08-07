package com.tencent.qqmusic.supersound;

import android.util.Log;

/** JNI surface used by QQ Music's real-time healing PCM generator. */
public final class SuperSoundJni {
    static {
        System.loadLibrary("SuperSound3");
    }

    private SuperSoundJni() {}

    public static native boolean ss_bs_check_resource(String root);

    public static native long ss_bs_create_inst(
            String root,
            int mode,
            int scene,
            float dayTime
    );

    public static native void ss_bs_destroy_inst(long instance);

    public static native int ss_bs_process_out(
            long instance,
            float[] output,
            int size,
            int[] written
    );

    public static native int ss_bs_update_params(
            long instance,
            String config,
            String[] outputConfig
    );

    public static void superSoundLog(int level, String message) {
        Log.d("SuperSound", level + ": " + message);
    }
}
