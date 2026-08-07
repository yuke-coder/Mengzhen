package TekEngineLib.Interface;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public interface ITekEffectStateListener {
    public static final int BACKGROUND_ENCODE_ERROR = 9;
    public static final int ENCODE_FINISH = 8;
    public static final int ENCOING_ERROR = 7;
    public static final int FIRST_FRAME = 3;
    public static final int INIT_GL_FAILED = 2;
    public static final int INIT_GL_SUCCESS = 16;
    public static final int MESSAGE = 15;
    public static final int OPEN_ENCODER_FAILED = 6;
    public static final int OPEN_ENCODER_SUCCESS = 5;
    public static final int PLAY_OVER = 4;
    public static final int RENDER_ERROR = 13;
    public static final int RENDER_STATISTIC = 11;
    public static final int UPDATE_DATASOURCE_FAILED = 1;
    public static final int UPDATE_DATASOURCE_SUCCESS = 0;
    public static final int UPDATE_KRC_SUCCESS = 14;
    public static final int UPDATE_LYRIC_ERROR = 10;
    public static final int VIDEO_PLAY_OVER = 12;

    void onStateChange(int i, int i2, Object obj);
}
