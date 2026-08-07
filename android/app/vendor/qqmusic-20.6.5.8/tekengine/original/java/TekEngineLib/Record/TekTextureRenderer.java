package TekEngineLib.Record;

import TekEngineLib.State.TekProxyLog;
import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekTextureRenderer implements Handler.Callback {
    private static final int DEFAULT_FRAME_RATE = 30;
    private static final int MSG_RELEASE = 2;
    private static final int MSG_START = 0;
    private static final int MSG_UPDATE = 1;
    private static final String TAG = "TekTextureRenderer";
    private final Callback callback;
    private long currentAudioPositionMs;
    private TekUpdateTextureFilter filter;
    private final TekRecordConfig mConfig;
    private EGLContext mSavedEglContext;
    private EGLDisplay mSavedEglDisplay;
    private EGLSurface mSavedEglDrawSurface;
    private EGLSurface mSavedEglReadSurface;
    private TekInputSurface mWorkingTekInputSurface;
    private Handler rendererHandler;
    private HandlerThread rendererThread;
    private final Surface surface;
    private volatile boolean running = false;
    private volatile int currentFrame = 0;
    private int totalFrames = 0;
    private long lastRenderTimeStamp = 0;
    private long firstFameTimeStamp = 0;
    private float currentProgress = 0.0f;
    private long lastUpdateTime = 0;

    public interface Callback {
        void onEnd();

        void onError(Throwable th);

        void onFirstFrameAvailable(Bitmap bitmap);

        void onProgress(int i);

        void onStarted();

        void renderFrame(long j, float f);
    }

    class a implements TekEngineLib.Render.a {
        a() {
        }

        @Override // TekEngineLib.Render.a
        public void a(long j) {
        }

        @Override // TekEngineLib.Render.a
        public void a(long j, int i, int i2, int i3, boolean z) {
            if (TekTextureRenderer.this.mWorkingTekInputSurface == null) {
                TekTextureRenderer tekTextureRenderer = TekTextureRenderer.this;
                tekTextureRenderer.initGlContext(tekTextureRenderer.surface);
            }
            if (TekTextureRenderer.this.mWorkingTekInputSurface == null) {
                TekProxyLog.w(TekTextureRenderer.TAG, "mWorkingInputSurface still null");
                TekTextureRenderer.this.callback.onError(new IllegalStateException("mWorkingInputSurface still null"));
                return;
            }
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (TekTextureRenderer.this.currentFrame == 0) {
                TekTextureRenderer.this.firstFameTimeStamp = jCurrentTimeMillis;
                TekTextureRenderer.this.lastUpdateTime = jCurrentTimeMillis;
            }
            try {
                if (i > 0) {
                    GLES20.glFinish();
                    TekTextureRenderer.this.saveRenderState();
                    TekTextureRenderer.this.mWorkingTekInputSurface.makeCurrent();
                    TekTextureRenderer tekTextureRenderer2 = TekTextureRenderer.this;
                    TekTextureRenderer.this.flush(i, i2, i3, tekTextureRenderer2.computePresentationTime(tekTextureRenderer2.currentFrame).longValue());
                    GLES20.glFinish();
                    TekTextureRenderer.this.restoreRenderState();
                    TekTextureRenderer.access$408(TekTextureRenderer.this);
                    TekTextureRenderer tekTextureRenderer3 = TekTextureRenderer.this;
                    tekTextureRenderer3.currentProgress = (tekTextureRenderer3.currentFrame * 1.0f) / TekTextureRenderer.this.totalFrames;
                    TekTextureRenderer tekTextureRenderer4 = TekTextureRenderer.this;
                    tekTextureRenderer4.currentAudioPositionMs = tekTextureRenderer4.mConfig.getStartTimeMs() + ((long) (TekTextureRenderer.this.mConfig.getDurationMs() * ((TekTextureRenderer.this.currentFrame * 1.0f) / TekTextureRenderer.this.totalFrames)));
                    TekTextureRenderer.this.callback.onProgress((int) ((TekTextureRenderer.this.currentFrame * 100.0f) / TekTextureRenderer.this.totalFrames));
                    TekTextureRenderer.this.rendererHandler.sendMessage(Message.obtain(TekTextureRenderer.this.rendererHandler, 1));
                    TekTextureRenderer.this.lastUpdateTime = jCurrentTimeMillis;
                } else {
                    TekTextureRenderer.this.callback.onError(new IllegalStateException("render error1"));
                }
            } catch (Exception e) {
                TekTextureRenderer.this.callback.onError(new IllegalStateException("render error2 " + e.getMessage()));
            }
        }
    }

    public TekTextureRenderer(Surface surface, TekRecordConfig tekRecordConfig, Callback callback) {
        this.surface = surface;
        this.mConfig = tekRecordConfig;
        this.callback = callback;
        this.currentAudioPositionMs = tekRecordConfig.getStartTimeMs();
    }

    static /* synthetic */ int access$408(TekTextureRenderer tekTextureRenderer) {
        int i = tekTextureRenderer.currentFrame;
        tekTextureRenderer.currentFrame = i + 1;
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flush(int i, int i2, int i3, long j) {
        this.filter.flush(i, i2, i3, j);
        this.mWorkingTekInputSurface.setPresentationTime(j);
        this.mWorkingTekInputSurface.swapBuffers();
        try {
            TekUpdateTextureFilter.checkGLError(TAG, "mWorkingInputSurface flush");
        } catch (Exception e) {
            Log.e(TAG, "flush error: " + e.getMessage());
        }
    }

    private void handleStart() {
        try {
            this.currentFrame = 0;
            this.totalFrames = Math.round(((this.mConfig.getDurationMs() * 1.0f) / 1000.0f) * 30.0f);
            this.mConfig.getTekEffectImplement().setFrameUpdateListener(new a());
            this.mConfig.getTekEffectImplement().pause();
            this.callback.onStarted();
            this.callback.onProgress(0);
            Handler handler = this.rendererHandler;
            handler.sendMessage(Message.obtain(handler, 1));
        } catch (Throwable th) {
            this.callback.onError(th);
        }
    }

    private void handleUpdate() {
        if (!this.running || this.currentFrame > this.totalFrames) {
            return;
        }
        if (this.currentFrame != this.totalFrames) {
            this.callback.renderFrame(this.currentAudioPositionMs, this.currentProgress);
        } else {
            this.callback.onEnd();
            this.running = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initGlContext(Surface surface) {
        if (!surface.isValid()) {
            TekProxyLog.i(TAG, "[initGlContext]Surface is not valid");
            return;
        }
        this.mWorkingTekInputSurface = new TekInputSurface(surface);
        TekUpdateTextureFilter tekUpdateTextureFilter = new TekUpdateTextureFilter();
        this.filter = tekUpdateTextureFilter;
        tekUpdateTextureFilter.initGLContext();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreRenderState() {
        if (EGL14.eglMakeCurrent(this.mSavedEglDisplay, this.mSavedEglDrawSurface, this.mSavedEglReadSurface, this.mSavedEglContext)) {
            return;
        }
        Log.e(TAG, "eglMakeCurrent failed");
    }

    private void saveBitmap2Png(Bitmap bitmap, String str) {
        if (bitmap == null || str.isEmpty()) {
            Log.e("saveBitmap2Png", "Bitmap or save path is null/empty");
            return;
        }
        File file = new File(str);
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(str);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            Log.d("saveBitmap2Png", "Bitmap saved successfully to " + str);
        } catch (IOException e) {
            Log.e("saveBitmap2Png", "Failed to save bitmap: " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveRenderState() {
        this.mSavedEglDisplay = EGL14.eglGetCurrentDisplay();
        this.mSavedEglDrawSurface = EGL14.eglGetCurrentSurface(12377);
        this.mSavedEglReadSurface = EGL14.eglGetCurrentSurface(12378);
        this.mSavedEglContext = EGL14.eglGetCurrentContext();
    }

    protected Long computePresentationTime(int i) {
        return Long.valueOf((((long) i) * 1000000000) / ((long) this.mConfig.getFrameRate()));
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message message) {
        int i = message.what;
        if (i == 0) {
            handleStart();
        } else if (i == 1) {
            handleUpdate();
        } else if (i == 2) {
            release();
        }
        return true;
    }

    public void release() {
        this.running = false;
        TekUpdateTextureFilter tekUpdateTextureFilter = this.filter;
        if (tekUpdateTextureFilter != null) {
            tekUpdateTextureFilter.release();
        }
        Handler handler = this.rendererHandler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        HandlerThread handlerThread = this.rendererThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        TekInputSurface tekInputSurface = this.mWorkingTekInputSurface;
        if (tekInputSurface != null) {
            tekInputSurface.release();
            this.mWorkingTekInputSurface = null;
        }
    }

    public void start() {
        this.running = true;
        HandlerThread handlerThread = new HandlerThread("TextureRenderer", 10);
        this.rendererThread = handlerThread;
        handlerThread.start();
        Handler handler = new Handler(this.rendererThread.getLooper(), this);
        this.rendererHandler = handler;
        handler.sendEmptyMessage(0);
    }

    public void stop() {
        this.running = false;
        this.rendererHandler.sendEmptyMessage(2);
    }
}
