package TekEngineLib.Interface;

import TekEngineLib.Render.TekBaseShader;
import TekEngineLib.Render.a;
import TekEngineLib.State.TekGyroscopeData;
import android.content.Context;
import android.view.View;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public interface ITekEffectInterface {
    public static final int STATE_DESTORING = 4;
    public static final int STATE_IDLING = 0;
    public static final int STATE_PAUSING = 3;
    public static final int STATE_READY = 1;
    public static final int STATE_RENDERING = 2;

    void cleanCache();

    void cleanParam(String str, String str2);

    void clearView();

    void closeEffect();

    View createView();

    View createView(int i, int i2);

    void destory();

    long getAudioTimestamp();

    int getCurrentState();

    int getOutputTexture();

    Object getParam(String str);

    TekBaseShader getShader();

    void pause();

    void postAsync(Runnable runnable);

    void postToRenderThread(Runnable runnable);

    void renderOneFrame();

    void seekTo(long j);

    void setContext(Context context);

    void setCryptKey(String str);

    void setEventListener(TekEventListener tekEventListener);

    void setFrameUpdateListener(a aVar);

    void setIsDirectRenderView(boolean z);

    void setParam(String str, Object obj);

    void setShader(TekBaseShader tekBaseShader);

    void setStateListener(ITekEffectStateListener iTekEffectStateListener);

    void setTimestampGetter(ITekEffectTimestampGetter iTekEffectTimestampGetter);

    boolean start();

    void updateAudioPlaying(boolean z);

    void updateAudioTimestamp(long j);

    void updateEffect(String str);

    void updateEffect(String str, String str2);

    void updateEffectFillMode(int i);

    void updateFFTData(byte[] bArr, int i);

    void updateGyroscopeData(TekGyroscopeData tekGyroscopeData);

    void updatePlaySpeed(float f);

    void updateSpeed(float f);

    void updateWaveData(byte[] bArr, int i);
}
