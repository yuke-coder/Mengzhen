package TekEngineLib.Record;

import TekEngineLib.Interface.TekEffectImplement;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekRecordConfig {
    private final long durationMs;
    private final int frameRate;
    private final int height;
    private final TekEffectImplement mTekEffectImplement;
    private final long startTimeMs;
    private final int width;

    public TekRecordConfig(int i, int i2, int i3, long j, long j2, TekEffectImplement tekEffectImplement) {
        this.width = i;
        this.height = i2;
        this.frameRate = i3;
        this.durationMs = j;
        this.startTimeMs = j2;
        this.mTekEffectImplement = tekEffectImplement;
    }

    public long getDurationMs() {
        return this.durationMs;
    }

    public int getFrameRate() {
        return this.frameRate;
    }

    public int getHeight() {
        return this.height;
    }

    public long getStartTimeMs() {
        return this.startTimeMs;
    }

    public TekEffectImplement getTekEffectImplement() {
        return this.mTekEffectImplement;
    }

    public int getWidth() {
        return this.width;
    }
}
