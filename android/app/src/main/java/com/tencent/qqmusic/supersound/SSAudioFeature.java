package com.tencent.qqmusic.supersound;

import java.io.Serializable;

/* JADX INFO: loaded from: classes7.dex */
public class SSAudioFeature implements Serializable {
    public static int ORG_SPECTRUM_BANDS = 1024;
    public static int TYPE_LOUDNESS = 1 << 1;
    public static int TYPE_SPECTRUM = 1;
    public float leftLoundess;
    public float[] leftSpectrumValues;
    public float rightLoudness;
    public float[] rightSpectrumValue;
    public int sampleRate;
    public int spectrumBands;
    public float[] spectrumFreqs;
    public long time;

    public SSAudioFeature(long j, int i, int i2, float[] fArr, float[] fArr2, float[] fArr3, float f, float f2) {
        this.time = j;
        this.sampleRate = i;
        this.spectrumBands = i2;
        this.spectrumFreqs = fArr;
        this.leftSpectrumValues = fArr2;
        this.rightSpectrumValue = fArr3;
        this.leftLoundess = f;
        this.rightLoudness = f2;
    }
}
