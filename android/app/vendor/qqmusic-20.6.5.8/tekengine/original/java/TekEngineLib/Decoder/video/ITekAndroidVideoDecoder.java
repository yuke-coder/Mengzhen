package TekEngineLib.Decoder.video;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public interface ITekAndroidVideoDecoder {
    void close();

    double getDuration();

    int getNextFrame(TekVideoFrameData tekVideoFrameData);

    boolean isEndOfStream();

    boolean isOpen();

    boolean open(String str);

    boolean open(String str, double d, double d2);

    void reset();
}
