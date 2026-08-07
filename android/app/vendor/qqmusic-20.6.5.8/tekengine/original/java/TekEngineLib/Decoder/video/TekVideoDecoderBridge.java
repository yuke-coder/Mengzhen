package TekEngineLib.Decoder.video;

import TekEngineLib.State.TekProxyLog;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekVideoDecoderBridge {
    private static final String TAG = "TekVideoDecoderBridge";
    private static ITekVideoDecoderFactory sDecoderFactory;

    public interface ITekVideoDecoderFactory {
        ITekAndroidVideoDecoder create();
    }

    class a implements ITekVideoDecoderFactory {
        a() {
        }

        @Override // TekEngineLib.Decoder.video.TekVideoDecoderBridge.ITekVideoDecoderFactory
        public ITekAndroidVideoDecoder create() {
            return new TekAndroidVideoDecoderDefImpl();
        }
    }

    class b implements ITekVideoDecoderFactory {

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        final /* synthetic */ Class f1a;

        b(Class cls) {
            this.f1a = cls;
        }

        @Override // TekEngineLib.Decoder.video.TekVideoDecoderBridge.ITekVideoDecoderFactory
        public ITekAndroidVideoDecoder create() {
            try {
                return (ITekAndroidVideoDecoder) this.f1a.newInstance();
            } catch (Exception e) {
                TekProxyLog.e(TekVideoDecoderBridge.TAG, "Failed to create custom decoder: " + e.getMessage());
                return null;
            }
        }
    }

    public static ITekAndroidVideoDecoder createDecoder() {
        ITekVideoDecoderFactory iTekVideoDecoderFactory = sDecoderFactory;
        if (iTekVideoDecoderFactory == null) {
            return null;
        }
        return iTekVideoDecoderFactory.create();
    }

    public static ITekAndroidVideoDecoder createDecoder(Class<? extends ITekAndroidVideoDecoder> cls) {
        try {
            return cls.newInstance();
        } catch (Exception unused) {
            return null;
        }
    }

    public static void destroy() {
    }

    public static ITekAndroidVideoDecoder getCurrentDecoder() {
        ITekVideoDecoderFactory iTekVideoDecoderFactory = sDecoderFactory;
        if (iTekVideoDecoderFactory == null) {
            return null;
        }
        return iTekVideoDecoderFactory.create();
    }

    public static ITekVideoDecoderFactory getDecoderFactory() {
        return sDecoderFactory;
    }

    public static boolean isDecoderRegistered() {
        return sDecoderFactory != null;
    }

    public static void registerCustomDecoder(Class<? extends ITekAndroidVideoDecoder> cls) {
        setDecoderFactory(new b(cls));
        TekProxyLog.i(TAG, "Custom video decoder registered: " + cls.getSimpleName());
    }

    public static void registerDefaultDecoderFactory() {
        TekProxyLog.i(TAG, "registerDefaultDecoderFactory");
        sDecoderFactory = new a();
    }

    public static void setDecoderFactory(ITekVideoDecoderFactory iTekVideoDecoderFactory) {
        sDecoderFactory = iTekVideoDecoderFactory;
    }
}
