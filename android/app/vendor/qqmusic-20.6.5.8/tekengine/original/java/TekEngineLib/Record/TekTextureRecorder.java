package TekEngineLib.Record;

import TekEngineLib.State.TekProxyLog;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.view.Surface;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekTextureRecorder {
    private static final float RATIO_VIDEO_OUTPUT = 0.8f;
    private static final String TAG = "TekTextureRecorder";
    private Callback callback;
    private final TekRecordConfig mConfig;
    private Surface mEncodeInputSurface;
    private MediaMuxer mMuxer;
    private final File mSaveFile;
    private MediaCodec mSurfaceEncoder;
    private HandlerThread mSurfaceEncoderThread;
    private final File outputFile;
    private TekTextureRenderer tekTextureRenderer;
    private final Object mViewEncoderLock = new Object();
    private volatile boolean running = false;
    private int mViewMuxerIndex = -1;

    public interface Callback {
        void onCancelled();

        void onError(Throwable th);

        void onFirstFrameAvailable(Bitmap bitmap);

        void onProgress(int i);

        void onStarted();

        void onSucceed(File file);

        void renderFrame(long j, float f);
    }

    class a implements TekTextureRenderer.Callback {
        a() {
        }

        @Override // TekEngineLib.Record.TekTextureRenderer.Callback
        public void onEnd() {
            synchronized (TekTextureRecorder.this.mViewEncoderLock) {
                try {
                    if (TekTextureRecorder.this.mSurfaceEncoder != null) {
                        TekTextureRecorder.this.mSurfaceEncoder.signalEndOfInputStream();
                    }
                    TekTextureRecorder.this.mSurfaceEncoder = null;
                } catch (Exception e) {
                    TekProxyLog.e(TekTextureRecorder.TAG, e.toString());
                }
            }
        }

        @Override // TekEngineLib.Record.TekTextureRenderer.Callback
        public void onError(Throwable th) {
            if (TekTextureRecorder.this.callback != null) {
                TekTextureRecorder.this.callback.onError(th);
            }
        }

        @Override // TekEngineLib.Record.TekTextureRenderer.Callback
        public void onFirstFrameAvailable(Bitmap bitmap) {
            if (TekTextureRecorder.this.callback != null) {
                TekTextureRecorder.this.callback.onFirstFrameAvailable(bitmap);
            }
        }

        @Override // TekEngineLib.Record.TekTextureRenderer.Callback
        public void onProgress(int i) {
            if (TekTextureRecorder.this.callback != null) {
                TekTextureRecorder.this.callback.onProgress(i);
            }
        }

        @Override // TekEngineLib.Record.TekTextureRenderer.Callback
        public void onStarted() {
            if (TekTextureRecorder.this.callback != null) {
                TekTextureRecorder.this.callback.onStarted();
            }
            if (TekTextureRecorder.this.mSurfaceEncoder != null) {
                TekTextureRecorder.this.mSurfaceEncoder.start();
            }
        }

        @Override // TekEngineLib.Record.TekTextureRenderer.Callback
        public void renderFrame(long j, float f) {
            if (TekTextureRecorder.this.callback != null) {
                TekTextureRecorder.this.callback.renderFrame(j, f);
            }
        }
    }

    class b extends MediaCodec.Callback {
        b() {
        }

        @Override // android.media.MediaCodec.Callback
        public void onError(MediaCodec mediaCodec, MediaCodec.CodecException codecException) {
            if (TekTextureRecorder.this.callback != null) {
                TekTextureRecorder.this.callback.onError(codecException);
            }
        }

        @Override // android.media.MediaCodec.Callback
        public void onInputBufferAvailable(MediaCodec mediaCodec, int i) {
        }

        @Override // android.media.MediaCodec.Callback
        public void onOutputBufferAvailable(MediaCodec mediaCodec, int i, MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
            if (outputBuffer == null) {
                TekProxyLog.e(TekTextureRecorder.TAG, "[onOutputBufferAvailable] getOutputBuffer is null");
            } else {
                TekTextureRecorder.this.tryConsumeOutputBuffer(outputBuffer, bufferInfo);
            }
            mediaCodec.releaseOutputBuffer(i, true);
            if ((bufferInfo.flags & 4) == 4) {
                TekProxyLog.i(TekTextureRecorder.TAG, "[onOutputBufferAvailable] BUFFER_FLAG_END_OF_STREAM");
                try {
                    mediaCodec.release();
                } catch (Exception e) {
                    TekProxyLog.e(TekTextureRecorder.TAG, "[onOutputBufferAvailable] failed to release encoder" + e);
                }
                TekTextureRecorder.this.releaseEncodeStaff();
                TekTextureRecorder.this.mConfig.getTekEffectImplement().setFrameUpdateListener(null);
                TekTextureRecorder.this.mConfig.getTekEffectImplement().pause();
                TekTextureRecorder.this.mConfig.getTekEffectImplement().destory();
                if (TekTextureRecorder.this.callback != null) {
                    TekTextureRecorder.this.callback.onSucceed(TekTextureRecorder.this.outputFile);
                }
            }
        }

        @Override // android.media.MediaCodec.Callback
        public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {
            TekTextureRecorder.this.prepareMuxer(mediaFormat);
        }
    }

    public TekTextureRecorder(TekRecordConfig tekRecordConfig, File file, Callback callback) {
        this.mConfig = tekRecordConfig;
        this.mSaveFile = file;
        this.callback = callback;
        this.outputFile = new File(file.getAbsolutePath());
    }

    private void doPrepareEncoder(int i, int i2) {
        MediaCodec mediaCodecCreateEncoderByType;
        MediaFormat mediaFormat = Build.VERSION.SDK_INT >= 29 ? getMediaFormat(i, i2, 8, 512) : getMediaFormat(i, i2, 0, 0);
        try {
            String strFindEncoderForFormat = new MediaCodecList(0).findEncoderForFormat(mediaFormat);
            if (TextUtils.isEmpty(strFindEncoderForFormat)) {
                throw new IllegalArgumentException("can't find supported encoder for format: " + mediaFormat);
            }
            mediaCodecCreateEncoderByType = MediaCodec.createByCodecName(strFindEncoderForFormat);
            this.mSurfaceEncoder = mediaCodecCreateEncoderByType;
            try {
                mediaCodecCreateEncoderByType.configure(mediaFormat, (Surface) null, (MediaCrypto) null, 1);
            } catch (Exception e) {
                TekProxyLog.e(TAG, "[doPrepareEncoder] MedieCodec configure Exception. use default format" + e);
                try {
                    mediaCodecCreateEncoderByType.configure(getMediaFormat(i, i2, 0, 0), (Surface) null, (MediaCrypto) null, 1);
                } catch (Exception e2) {
                    TekProxyLog.e(TAG, "[doPrepareEncoder] failed to configure MedieCodec" + e2);
                    throw new RuntimeException("没有对应的编码器");
                }
            }
            this.mEncodeInputSurface = mediaCodecCreateEncoderByType.createInputSurface();
            mediaCodecCreateEncoderByType.setCallback(new b());
        } catch (Exception e3) {
            TekProxyLog.e(TAG, "[doPrepareEncoder] failed to create encoder. try again." + e3);
            try {
                mediaCodecCreateEncoderByType = MediaCodec.createEncoderByType("video/avc");
            } catch (Exception e4) {
                TekProxyLog.e(TAG, "[doPrepareEncoder] failed to create encoder" + e4);
                throw new RuntimeException("没有对应的编码器");
            }
        }
    }

    private boolean ensureDir(File file) {
        String str;
        if (file.isDirectory()) {
            return true;
        }
        if (file.isFile() && !file.delete()) {
            str = "[ensureDir] cannot delete previous file";
        } else {
            if (file.exists() || file.mkdirs()) {
                return true;
            }
            str = "[ensureDir] cannot mkdirs";
        }
        TekProxyLog.e(TAG, str);
        return false;
    }

    private MediaFormat getMediaFormat(int i, int i2, int i3, int i4) {
        MediaFormat mediaFormatCreateVideoFormat = MediaFormat.createVideoFormat("video/avc", this.mConfig.getWidth(), this.mConfig.getHeight());
        mediaFormatCreateVideoFormat.setInteger("bitrate", i);
        mediaFormatCreateVideoFormat.setInteger("frame-rate", i2);
        mediaFormatCreateVideoFormat.setInteger("i-frame-interval", 1);
        mediaFormatCreateVideoFormat.setInteger("color-format", 2130708361);
        if (i3 != 0) {
            mediaFormatCreateVideoFormat.setInteger("profile", 8);
        }
        if (i4 != 0) {
            mediaFormatCreateVideoFormat.setInteger("level", 512);
        }
        return mediaFormatCreateVideoFormat;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$prepareEncoder$0(int i, int i2, CountDownLatch countDownLatch) {
        try {
            doPrepareEncoder(i, i2);
        } catch (Exception e) {
            TekProxyLog.e(TAG, "[prepareEncoder] failed:" + e.toString());
            stop();
            if (this.callback != null) {
                this.callback.onError(e);
            }
        }
        countDownLatch.countDown();
    }

    private void prepareEncoder(final int i, final int i2) {
        TekProxyLog.i(TAG, "[prepareEncoder] bitrate: " + i + ", frameRate: " + i2);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        HandlerThread handlerThread = new HandlerThread("TextureRecorder.SurfaceEncoder");
        this.mSurfaceEncoderThread = handlerThread;
        handlerThread.start();
        new Handler(this.mSurfaceEncoderThread.getLooper()).post(new Runnable() { // from class: TekEngineLib.Record.a
            @Override // java.lang.Runnable
            public final void run() {
                this.b.lambda$prepareEncoder$0(i, i2, countDownLatch);
            }
        });
        TekProxyLog.i(TAG, "[prepareEncoder] done");
        try {
            countDownLatch.await();
        } catch (InterruptedException unused) {
            TekProxyLog.e(TAG, "[prepareEncoder] latch await interrupted");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareMuxer(MediaFormat mediaFormat) {
        if (this.mMuxer == null) {
            TekFileUtil.deleteGeneralFile(this.outputFile.getAbsolutePath());
            if (ensureDir(this.outputFile.getParentFile())) {
                try {
                    MediaMuxer mediaMuxer = new MediaMuxer(this.outputFile.getAbsolutePath(), 0);
                    this.mMuxer = mediaMuxer;
                    this.mViewMuxerIndex = mediaMuxer.addTrack(mediaFormat);
                    this.mMuxer.start();
                } catch (IOException e) {
                    TekProxyLog.w(TAG, "[prepareMuxer] failed to create MediaMuxer" + e);
                    this.callback.onError(new IllegalStateException("[prepareMuxer] failed to create MediaMuxer"));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseEncodeStaff() {
        Surface surface = this.mEncodeInputSurface;
        if (surface != null) {
            surface.release();
        }
        try {
            MediaMuxer mediaMuxer = this.mMuxer;
            if (mediaMuxer != null) {
                mediaMuxer.release();
            }
        } catch (Exception unused) {
        }
        HandlerThread handlerThread = this.mSurfaceEncoderThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryConsumeOutputBuffer(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        int i;
        MediaMuxer mediaMuxer = this.mMuxer;
        if (mediaMuxer == null || (i = this.mViewMuxerIndex) == -1) {
            Callback callback = this.callback;
            if (callback != null) {
                callback.onError(new IllegalStateException("call prepareMuxer first!"));
                return;
            }
            return;
        }
        if (bufferInfo.size > 0) {
            try {
                mediaMuxer.writeSampleData(i, byteBuffer, bufferInfo);
            } catch (Exception e) {
                Callback callback2 = this.callback;
                if (callback2 != null) {
                    callback2.onError(e);
                }
            }
        }
    }

    public void release() {
        this.callback = null;
        TekTextureRenderer tekTextureRenderer = this.tekTextureRenderer;
        if (tekTextureRenderer != null) {
            tekTextureRenderer.stop();
        }
        stop();
    }

    public void start() {
        prepareEncoder((((this.mConfig.getWidth() * 32) * this.mConfig.getHeight()) * this.mConfig.getFrameRate()) / 100, this.mConfig.getFrameRate());
        TekTextureRenderer tekTextureRenderer = new TekTextureRenderer(this.mEncodeInputSurface, this.mConfig, new a());
        this.tekTextureRenderer = tekTextureRenderer;
        tekTextureRenderer.start();
        this.running = true;
    }

    public void stop() {
        synchronized (this.mViewEncoderLock) {
            try {
                MediaCodec mediaCodec = this.mSurfaceEncoder;
                if (mediaCodec != null) {
                    mediaCodec.signalEndOfInputStream();
                }
                this.mSurfaceEncoder = null;
            } catch (Exception e) {
                TekProxyLog.e(TAG, e.toString());
            }
        }
        if (this.running) {
            this.running = false;
        }
    }
}
