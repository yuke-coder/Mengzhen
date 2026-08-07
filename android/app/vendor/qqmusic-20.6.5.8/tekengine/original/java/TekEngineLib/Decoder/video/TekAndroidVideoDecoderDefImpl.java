package TekEngineLib.Decoder.video;

import TekEngineLib.State.TekProxyLog;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCrypto;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Iterator;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekAndroidVideoDecoderDefImpl implements ITekAndroidVideoDecoder {
    private static final int COLOR_STANDARD_BT2020 = 3;
    private static final int COLOR_STANDARD_BT601 = 1;
    private static final int COLOR_STANDARD_BT709 = 2;
    private static final int MAX_QUEUE_SIZE = 2;
    private static final String TAG = "TekAndroidVideoDecoderDefImpl";
    private static final double TIME_US_PER_SEC = 1000000.0d;
    private int mColorStandard;
    private MediaCodec mDecoder;
    private MediaExtractor mExtractor;
    private String mVideoPath;
    private double mStartSec = 0.0d;
    private double mEndSec = 0.0d;
    private boolean mIsOpen = false;
    private boolean mIsEndOfStream = false;
    private double mDuration = 0.0d;
    private int mColorFormat = -1;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private int mVideoTrackIndex = -1;
    private long mVideoDurationUs = 0;
    private int alignWidth = 0;
    private int alignHeight = 0;
    private boolean mInputEOS = false;
    private boolean mOutputEOS = false;
    private long lastSampleTime = 0;
    private boolean useBT601 = false;
    private long frameRate = 0;
    private ArrayDeque<TekVideoFrameData> frameQueue = new ArrayDeque<>(2);
    private final Object queueLock = new Object();
    private boolean isProducing = false;
    private boolean isConsuming = false;
    private TekVideoFrameData mCurFrameData = null;
    private HandlerThread producerThread = null;
    private Handler producerHandler = null;

    class a implements Runnable {
        a() {
        }

        /* JADX WARN: Code duplicated, block: B:51:0x00f8  */
        /* JADX WARN: Code duplicated, block: B:60:? A[RETURN, SYNTHETIC] */
        @Override // java.lang.Runnable
        public void run() {
            if (TekAndroidVideoDecoderDefImpl.this.isProducing) {
                synchronized (TekAndroidVideoDecoderDefImpl.this.queueLock) {
                    if (TekAndroidVideoDecoderDefImpl.this.isProducing) {
                        if (TekAndroidVideoDecoderDefImpl.this.frameQueue.size() >= 2) {
                            try {
                                TekAndroidVideoDecoderDefImpl.this.queueLock.wait();
                                if (TekAndroidVideoDecoderDefImpl.this.isProducing) {
                                    TekAndroidVideoDecoderDefImpl.this.producerHandler.post(this);
                                }
                            } catch (InterruptedException e) {
                                TekProxyLog.e(TekAndroidVideoDecoderDefImpl.TAG, "Producer thread interrupted: " + e.getMessage());
                                return;
                            }
                        }
                        try {
                            TekVideoFrameData tekVideoFrameData = new TekVideoFrameData();
                            int iDecodeNextFrame = TekAndroidVideoDecoderDefImpl.this.decodeNextFrame(tekVideoFrameData);
                            if (iDecodeNextFrame == 0 && tekVideoFrameData.yData != null && tekVideoFrameData.uvData != null) {
                                TekAndroidVideoDecoderDefImpl.this.frameQueue.addLast(tekVideoFrameData);
                                TekAndroidVideoDecoderDefImpl.this.queueLock.notifyAll();
                            } else {
                                if (iDecodeNextFrame == 1) {
                                    if (tekVideoFrameData.yData != null && tekVideoFrameData.uvData != null) {
                                        TekAndroidVideoDecoderDefImpl.this.frameQueue.addLast(tekVideoFrameData);
                                    }
                                    TekAndroidVideoDecoderDefImpl.this.isProducing = false;
                                    TekAndroidVideoDecoderDefImpl.this.queueLock.notifyAll();
                                    return;
                                }
                                if (iDecodeNextFrame == 3) {
                                    TekAndroidVideoDecoderDefImpl tekAndroidVideoDecoderDefImpl = TekAndroidVideoDecoderDefImpl.this;
                                    if (!tekAndroidVideoDecoderDefImpl.isMediaCodecSafe(tekAndroidVideoDecoderDefImpl.mDecoder)) {
                                        TekAndroidVideoDecoderDefImpl.this.isProducing = false;
                                        TekAndroidVideoDecoderDefImpl.this.queueLock.notifyAll();
                                        return;
                                    }
                                }
                            }
                        } catch (Exception e2) {
                            TekProxyLog.e(TekAndroidVideoDecoderDefImpl.TAG, "Error in producer thread: " + e2.getMessage());
                            TekAndroidVideoDecoderDefImpl tekAndroidVideoDecoderDefImpl2 = TekAndroidVideoDecoderDefImpl.this;
                            if (!tekAndroidVideoDecoderDefImpl2.isMediaCodecSafe(tekAndroidVideoDecoderDefImpl2.mDecoder)) {
                                TekProxyLog.e(TekAndroidVideoDecoderDefImpl.TAG, "[${hashCode()}]decodeNextFrame error isMediaCodecSafe=false do relase");
                                TekAndroidVideoDecoderDefImpl.this.close();
                            }
                        }
                        if (TekAndroidVideoDecoderDefImpl.this.isProducing) {
                            TekAndroidVideoDecoderDefImpl.this.producerHandler.post(this);
                        }
                    }
                }
            }
        }
    }

    class b implements Runnable {
        final /* synthetic */ int b;

        b(int i) {
            this.b = i;
        }

        @Override // java.lang.Runnable
        public void run() {
            TekAndroidVideoDecoderDefImpl.this.mDecoder.releaseOutputBuffer(this.b, false);
        }
    }

    private void copyFrameData(TekVideoFrameData tekVideoFrameData, TekVideoFrameData tekVideoFrameData2) {
        tekVideoFrameData2.width = tekVideoFrameData.width;
        tekVideoFrameData2.height = tekVideoFrameData.height;
        tekVideoFrameData2.alignedWidth = tekVideoFrameData.alignedWidth;
        tekVideoFrameData2.bytesPerRow = tekVideoFrameData.bytesPerRow;
        tekVideoFrameData2.uvBytesPerRow = tekVideoFrameData.uvBytesPerRow;
        tekVideoFrameData2.pixelFormat = tekVideoFrameData.pixelFormat;
        tekVideoFrameData2.status = tekVideoFrameData.status;
        tekVideoFrameData2.uvWidth = tekVideoFrameData.uvWidth;
        tekVideoFrameData2.uvHeight = tekVideoFrameData.uvHeight;
        float[] fArr = tekVideoFrameData.colorMatrix;
        System.arraycopy(fArr, 0, tekVideoFrameData2.colorMatrix, 0, fArr.length);
        tekVideoFrameData2.colorOffsetU = tekVideoFrameData.colorOffsetU;
        tekVideoFrameData2.colorOffsetV = tekVideoFrameData.colorOffsetV;
        ByteBuffer byteBuffer = tekVideoFrameData.yData;
        if (byteBuffer != null) {
            tekVideoFrameData2.yData = byteBuffer;
        }
        ByteBuffer byteBuffer2 = tekVideoFrameData.uvData;
        if (byteBuffer2 != null) {
            tekVideoFrameData2.uvData = byteBuffer2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int decodeNextFrame(TekVideoFrameData tekVideoFrameData) {
        MediaCodec mediaCodec;
        String str;
        int iDequeueInputBuffer;
        ByteBuffer inputBuffer;
        String string;
        if (!this.mIsOpen || (mediaCodec = this.mDecoder) == null) {
            return 3;
        }
        if (isMediaCodecReleased(mediaCodec)) {
            TekProxyLog.w(TAG, "[" + hashCode() + "]Decoder isMediaCodecReleased");
            return 3;
        }
        if (isEndOfStream() && !this.frameQueue.isEmpty()) {
            TekProxyLog.d(TAG, "[" + hashCode() + "]decodeNextFrame: frameQueue is not empty");
            return 1;
        }
        try {
            if (this.mInputEOS || (iDequeueInputBuffer = this.mDecoder.dequeueInputBuffer(0L)) < 0 || (inputBuffer = this.mDecoder.getInputBuffer(iDequeueInputBuffer)) == null) {
                str = "[";
            } else {
                int sampleData = this.mExtractor.readSampleData(inputBuffer, 0);
                if (sampleData < 0) {
                    this.mInputEOS = true;
                    this.mDecoder.queueInputBuffer(iDequeueInputBuffer, 0, 0, 0L, 4);
                    string = "[" + hashCode() + "]Input EOS signaled";
                    str = "[";
                } else {
                    long sampleTime = this.mExtractor.getSampleTime();
                    double d = this.mEndSec;
                    if (d <= 0.0d || sampleTime / TIME_US_PER_SEC < d) {
                        str = "[";
                        this.mDecoder.queueInputBuffer(iDequeueInputBuffer, 0, sampleData, sampleTime, 0);
                        this.mExtractor.advance();
                    } else {
                        this.mInputEOS = true;
                        this.mDecoder.queueInputBuffer(iDequeueInputBuffer, 0, 0, 0L, 4);
                        StringBuilder sb = new StringBuilder();
                        str = "[";
                        sb.append(str);
                        sb.append(hashCode());
                        sb.append("]Reached end of playback interval");
                        string = sb.toString();
                    }
                }
                TekProxyLog.d(TAG, string);
            }
            if (!isMediaCodecSafe(this.mDecoder)) {
                TekProxyLog.w(TAG, str + hashCode() + "]MediaCodec is not safe for output operations");
                return 3;
            }
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int iDequeueOutputBuffer = this.mDecoder.dequeueOutputBuffer(bufferInfo, 10000L);
            if (iDequeueOutputBuffer == -2) {
                TekProxyLog.d(TAG, str + hashCode() + "]Output format changed");
                this.mColorFormat = this.mDecoder.getOutputFormat().getInteger("color-format");
                MediaFormat outputFormat = this.mDecoder.getOutputFormat();
                try {
                    int integer = outputFormat.getInteger("stride");
                    int integer2 = outputFormat.getInteger("slice-height");
                    if (integer > 0 && integer2 > 0) {
                        this.alignWidth = integer;
                        this.alignHeight = integer2;
                        TekProxyLog.d(TAG, str + hashCode() + "]ColorFormat " + this.mColorFormat + " alignWidth " + this.alignWidth + " alignHeight " + this.alignHeight);
                    }
                } catch (Exception e) {
                    TekProxyLog.d(TAG, str + hashCode() + "]Output format changed error: " + e.getMessage());
                }
                updateUseBT601ColorMatrix(outputFormat);
                return decodeNextFrame(tekVideoFrameData);
            }
            if (iDequeueOutputBuffer == -1) {
                return this.mInputEOS ? 2 : 0;
            }
            if (iDequeueOutputBuffer < 0) {
                return 3;
            }
            try {
                if ((bufferInfo.flags & 4) != 0) {
                    this.mOutputEOS = true;
                    this.mIsEndOfStream = true;
                    TekProxyLog.d(TAG, str + hashCode() + "]bufferIndex=" + iDequeueOutputBuffer + "`   EOS reached");
                    this.mDecoder.releaseOutputBuffer(iDequeueOutputBuffer, false);
                    return 1;
                }
                if (bufferInfo.size > 0) {
                    fillFrameData(tekVideoFrameData, iDequeueOutputBuffer, bufferInfo);
                    return (tekVideoFrameData.yData == null || tekVideoFrameData.uvData == null) ? 3 : 0;
                }
                TekProxyLog.d(TAG, str + hashCode() + "]bufferInfo.size<=0 outputBufferIndex = " + iDequeueOutputBuffer);
                this.mDecoder.releaseOutputBuffer(iDequeueOutputBuffer, false);
                return 2;
            } catch (Throwable th) {
                TekProxyLog.e(TAG, str + hashCode() + "]Frame processing error " + th);
                if (iDequeueOutputBuffer < 0) {
                    return 3;
                }
                try {
                    if (isMediaCodecReleased(this.mDecoder)) {
                        return 3;
                    }
                    TekProxyLog.d(TAG, str + hashCode() + "]Failed to release buffer outputBufferIndex=" + iDequeueOutputBuffer);
                    this.mDecoder.releaseOutputBuffer(iDequeueOutputBuffer, false);
                    return 3;
                } catch (Exception unused) {
                    TekProxyLog.e(TAG, "[${hashCode()}] 2 Failed to release buffer outputBufferIndex=$outputBufferIndex: $e");
                    return 3;
                }
            }
            TekProxyLog.e(TAG, "Error in decodeNextFrame: " + e.getMessage());
            return 3;
        } catch (Exception e2) {
            TekProxyLog.e(TAG, "Error in decodeNextFrame: " + e2.getMessage());
            return 3;
        }
    }

    private void fillFrameData(TekVideoFrameData tekVideoFrameData, int i, MediaCodec.BufferInfo bufferInfo) {
        try {
            ByteBuffer outputBuffer = this.mDecoder.getOutputBuffer(i);
            if (outputBuffer == null) {
                TekProxyLog.d(TAG, "[" + hashCode() + "]outputBuffer==null outputBufferIndex = " + i);
                this.mDecoder.releaseOutputBuffer(i, false);
                return;
            }
            int i2 = this.mVideoWidth;
            tekVideoFrameData.width = i2;
            int i3 = this.mVideoHeight;
            tekVideoFrameData.height = i3;
            int i4 = this.alignWidth;
            if (i4 > 0) {
                i2 = i4;
            }
            int i5 = this.alignHeight;
            if (i5 > 0) {
                i3 = i5;
            }
            tekVideoFrameData.alignedWidth = i2;
            tekVideoFrameData.bytesPerRow = i2;
            tekVideoFrameData.uvBytesPerRow = i2;
            tekVideoFrameData.pixelFormat = 0;
            tekVideoFrameData.status = 0;
            tekVideoFrameData.uvWidth = i2 / 2;
            tekVideoFrameData.uvHeight = i3 / 2;
            int i6 = i2 * i3;
            int i7 = bufferInfo.size - i6;
            tekVideoFrameData.setReleaseFunction(new b(i));
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
            ByteBuffer byteBufferSlice = outputBuffer.slice();
            byteBufferSlice.limit(i6);
            ByteBuffer byteBufferSlice2 = outputBuffer.slice();
            byteBufferSlice2.position(i6);
            if (i6 > 0 && i7 > 0 && byteBufferSlice.remaining() == i6 && byteBufferSlice2.remaining() == i7) {
                tekVideoFrameData.yData = byteBufferSlice.slice();
                tekVideoFrameData.uvData = byteBufferSlice2.slice();
                updateColorMatrix(tekVideoFrameData);
                return;
            }
            TekProxyLog.e(TAG, "[" + hashCode() + "] Error filling frame data: invalid buffer sizes ySize=" + i6 + ", uvSize=" + i7 + ", yData remaining=" + byteBufferSlice.remaining() + ", uvData remaining=" + byteBufferSlice2.remaining() + "bufferIndex+=" + i);
            this.mDecoder.releaseOutputBuffer(i, false);
        } catch (Exception e) {
            TekProxyLog.e(TAG, "[" + hashCode() + "]bufferIndex=" + i + " Error filling frame data: " + e.getMessage());
            if (i >= 0) {
                try {
                    if (isMediaCodecReleased(this.mDecoder)) {
                        return;
                    }
                    this.mDecoder.releaseOutputBuffer(i, false);
                } catch (Exception e2) {
                    TekProxyLog.e(TAG, "[" + hashCode() + "] 2 Failed to release buffer bufferIndex=" + i + ": " + e2);
                }
            }
        }
    }

    private int findVideoTrack() {
        if (this.mExtractor == null) {
            return -1;
        }
        for (int i = 0; i < this.mExtractor.getTrackCount(); i++) {
            String string = this.mExtractor.getTrackFormat(i).getString("mime");
            if (string != null && string.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }

    private boolean isMediaCodecReleased(MediaCodec mediaCodec) {
        if (mediaCodec == null) {
            return true;
        }
        try {
            mediaCodec.getCodecInfo();
            return false;
        } catch (IllegalStateException e) {
            TekProxyLog.d(TAG, "[" + hashCode() + "]MediaCodec is in illegal state (likely released): " + e);
            return true;
        } catch (Exception e2) {
            TekProxyLog.w(TAG, "Unexpected exception when checking MediaCodec state: " + e2);
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isMediaCodecSafe(MediaCodec mediaCodec) {
        if (mediaCodec == null || isMediaCodecReleased(mediaCodec)) {
            return false;
        }
        try {
            mediaCodec.getOutputFormat();
            return true;
        } catch (Exception e) {
            TekProxyLog.w(TAG, "[" + hashCode() + "]MediaCodec is not safe to use: " + e);
            return false;
        }
    }

    private void releaseFrameData(TekVideoFrameData tekVideoFrameData) {
        if (tekVideoFrameData == null) {
            return;
        }
        tekVideoFrameData.release();
    }

    private void startProducing() {
        if (this.producerThread == null) {
            HandlerThread handlerThread = new HandlerThread("VideoFrameProducer");
            this.producerThread = handlerThread;
            handlerThread.start();
            this.producerHandler = new Handler(this.producerThread.getLooper());
        }
        if (this.isProducing) {
            return;
        }
        this.isProducing = true;
        this.producerHandler.post(new a());
    }

    private void stopProducing() {
        synchronized (this.queueLock) {
            this.isProducing = false;
            Iterator<TekVideoFrameData> it = this.frameQueue.iterator();
            while (it.hasNext()) {
                releaseFrameData(it.next());
            }
            this.frameQueue.clear();
            this.queueLock.notifyAll();
        }
        HandlerThread handlerThread = this.producerThread;
        if (handlerThread != null) {
            handlerThread.quitSafely();
            this.producerThread = null;
            this.producerHandler = null;
        }
    }

    private void updateColorMatrix(TekVideoFrameData tekVideoFrameData) {
        int i = this.mColorStandard;
        if (i == 1) {
            float[] fArr = tekVideoFrameData.colorMatrix;
            fArr[0] = 1.0f;
            fArr[1] = 0.0f;
            fArr[2] = 1.4f;
            fArr[3] = 1.0f;
            fArr[4] = -0.343f;
            fArr[5] = -0.711f;
            fArr[6] = 1.0f;
            fArr[7] = 1.765f;
            fArr[8] = 0.0f;
        } else if (i == 3) {
            float[] fArr2 = tekVideoFrameData.colorMatrix;
            fArr2[0] = 1.0f;
            fArr2[1] = 0.0f;
            fArr2[2] = 1.4746f;
            fArr2[3] = 1.0f;
            fArr2[4] = -0.16455f;
            fArr2[5] = -0.57135f;
            fArr2[6] = 1.0f;
            fArr2[7] = 1.8814f;
            fArr2[8] = 0.0f;
        } else {
            float[] fArr3 = tekVideoFrameData.colorMatrix;
            fArr3[0] = 1.0f;
            fArr3[1] = 0.0f;
            fArr3[2] = 1.57481f;
            fArr3[3] = 1.0f;
            fArr3[4] = -0.18732f;
            fArr3[5] = -0.46813f;
            fArr3[6] = 1.0f;
            fArr3[7] = 1.8556f;
            fArr3[8] = 0.0f;
        }
        tekVideoFrameData.colorOffsetU = 0.5f;
        tekVideoFrameData.colorOffsetV = 0.5f;
    }

    private void updateUseBT601ColorMatrix(MediaFormat mediaFormat) {
        if (this.mVideoWidth <= 0 || this.mVideoHeight <= 0) {
            TekProxyLog.w(TAG, "Invalid video dimensions, defaulting to BT.709");
            this.mColorStandard = 2;
            return;
        }
        if (mediaFormat != null) {
            try {
                if (Build.VERSION.SDK_INT >= 24) {
                    if (mediaFormat.containsKey("color-transfer")) {
                        TekProxyLog.d(TAG, "[" + hashCode() + "]Color transfer: " + mediaFormat.getInteger("color-transfer"));
                    }
                    if (mediaFormat.containsKey("color-range")) {
                        TekProxyLog.d(TAG, "[" + hashCode() + "]Color range: " + mediaFormat.getInteger("color-range"));
                    }
                    if (mediaFormat.containsKey("color-standard")) {
                        int integer = mediaFormat.getInteger("color-standard");
                        if (integer != 2 && integer != 4) {
                            if (integer == 1) {
                                TekProxyLog.d(TAG, "[" + hashCode() + "]Using BT.709 color matrix based on MediaFormat COLOR_STANDARD: " + integer);
                                this.mColorStandard = 2;
                                return;
                            }
                            if (integer == 6) {
                                TekProxyLog.w(TAG, "[" + hashCode() + "] COLOR_STANDARD_BT2020 detected: need BT2020 matrix (not 601/709)");
                                this.mColorStandard = 3;
                            }
                        }
                        TekProxyLog.d(TAG, "[" + hashCode() + "]Using BT.601 color matrix based on MediaFormat COLOR_STANDARD: " + integer);
                        this.mColorStandard = 1;
                        return;
                    }
                }
            } catch (Exception e) {
                TekProxyLog.w(TAG, "Error reading color space from MediaFormat: " + e.getMessage());
            }
        }
        int iMin = Math.min(this.mVideoWidth, this.mVideoHeight);
        if (iMin <= 576) {
            this.mColorStandard = 1;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(hashCode());
        sb.append("]Video resolution: ");
        sb.append(this.mVideoWidth);
        sb.append("x");
        sb.append(this.mVideoHeight);
        sb.append(", height: ");
        sb.append(iMin);
        sb.append(", using color space: ");
        sb.append(this.useBT601 ? "BT.601" : "BT.709");
        sb.append(" (resolution-based)");
        TekProxyLog.d(TAG, sb.toString());
    }

    @Override // TekEngineLib.Decoder.video.ITekAndroidVideoDecoder
    public void close() {
        StringBuilder sb;
        TekProxyLog.i(TAG, "[" + hashCode() + "]Closing decoder");
        try {
            stopProducing();
            try {
                MediaCodec mediaCodec = this.mDecoder;
                if (mediaCodec != null) {
                    try {
                        try {
                            if (!isMediaCodecReleased(mediaCodec)) {
                                try {
                                    this.mDecoder.flush();
                                } catch (Exception e) {
                                    TekProxyLog.w(TAG, "[" + hashCode() + "]Failed to flush decoder: " + e);
                                }
                                try {
                                    MediaCodecInfo codecInfo = this.mDecoder.getCodecInfo();
                                    if (codecInfo != null && codecInfo.isEncoder()) {
                                        this.mDecoder.signalEndOfInputStream();
                                    }
                                } catch (Exception e2) {
                                    TekProxyLog.w(TAG, "[" + hashCode() + "]Failed to signal end of input stream: " + e2);
                                }
                                try {
                                    this.mDecoder.stop();
                                } catch (Exception e3) {
                                    TekProxyLog.w(TAG, "[" + hashCode() + "]Failed to stop decoder: " + e3);
                                }
                            }
                            try {
                                this.mDecoder.release();
                            } catch (Exception e4) {
                                sb = new StringBuilder();
                                sb.append("[");
                                sb.append(hashCode());
                                sb.append("]Failed to release decoder: ");
                                sb.append(e4);
                                TekProxyLog.w(TAG, sb.toString());
                            }
                        } catch (Exception e5) {
                            TekProxyLog.w(TAG, "[" + hashCode() + "]Error during decoder cleanup: " + e5);
                            try {
                                this.mDecoder.release();
                            } catch (Exception e6) {
                                sb = new StringBuilder();
                                sb.append("[");
                                sb.append(hashCode());
                                sb.append("]Failed to release decoder: ");
                                sb.append(e6);
                                TekProxyLog.w(TAG, sb.toString());
                            }
                        }
                    } catch (Throwable th) {
                        try {
                            this.mDecoder.release();
                        } catch (Exception e7) {
                            TekProxyLog.w(TAG, "[" + hashCode() + "]Failed to release decoder: " + e7);
                        }
                        throw th;
                    }
                }
            } catch (Exception e8) {
                TekProxyLog.w(TAG, "[" + hashCode() + "]Failed to release decoder: " + e8);
            }
            try {
                MediaExtractor mediaExtractor = this.mExtractor;
                if (mediaExtractor != null) {
                    mediaExtractor.release();
                }
            } catch (Exception e9) {
                TekProxyLog.w(TAG, "[" + hashCode() + "]Failed to release extractor: " + e9);
            }
            this.mDecoder = null;
            this.mExtractor = null;
            this.mIsOpen = false;
            this.mIsEndOfStream = false;
            this.mVideoPath = null;
        } catch (Exception e10) {
            TekProxyLog.e(TAG, "Error closing decoder: " + e10.getMessage());
        }
    }

    @Override // TekEngineLib.Decoder.video.ITekAndroidVideoDecoder
    public double getDuration() {
        return this.mDuration;
    }

    @Override // TekEngineLib.Decoder.video.ITekAndroidVideoDecoder
    public int getNextFrame(TekVideoFrameData tekVideoFrameData) {
        if (!this.mIsOpen || this.mDecoder == null) {
            return 3;
        }
        synchronized (this.queueLock) {
            if (this.frameQueue.isEmpty()) {
                if (!this.isProducing) {
                    TekVideoFrameData tekVideoFrameData2 = this.mCurFrameData;
                    if (tekVideoFrameData2 == null) {
                        return 2;
                    }
                    copyFrameData(tekVideoFrameData2, tekVideoFrameData);
                    TekProxyLog.d(TAG, "[" + hashCode() + "]Using current frame (producer stopped)");
                    return 0;
                }
                this.isConsuming = true;
                try {
                    this.queueLock.wait(20L);
                    if (this.frameQueue.isEmpty()) {
                        TekProxyLog.d(TAG, "[" + hashCode() + "]No frames available after waiting");
                        return 2;
                    }
                } catch (InterruptedException e) {
                    TekProxyLog.e(TAG, "Consumer thread interrupted: " + e.getMessage());
                    return 3;
                }
            }
            this.isConsuming = true;
            TekVideoFrameData tekVideoFrameDataRemoveFirst = this.frameQueue.removeFirst();
            TekVideoFrameData tekVideoFrameData3 = this.mCurFrameData;
            if (tekVideoFrameData3 != null) {
                releaseFrameData(tekVideoFrameData3);
            }
            this.mCurFrameData = tekVideoFrameDataRemoveFirst;
            copyFrameData(tekVideoFrameDataRemoveFirst, tekVideoFrameData);
            this.queueLock.notifyAll();
            return (isEndOfStream() && this.frameQueue.isEmpty()) ? 1 : 0;
        }
    }

    @Override // TekEngineLib.Decoder.video.ITekAndroidVideoDecoder
    public boolean isEndOfStream() {
        return this.mIsEndOfStream;
    }

    @Override // TekEngineLib.Decoder.video.ITekAndroidVideoDecoder
    public boolean isOpen() {
        return this.mIsOpen;
    }

    @Override // TekEngineLib.Decoder.video.ITekAndroidVideoDecoder
    public boolean open(String str) {
        return open(str, 0.0d, 0.0d);
    }

    @Override // TekEngineLib.Decoder.video.ITekAndroidVideoDecoder
    public boolean open(String str, double d, double d2) {
        TekProxyLog.i(TAG, "[" + hashCode() + "]open video: " + str + " [" + d + "-" + d2 + "]");
        try {
            if (!new File(str).exists()) {
                TekProxyLog.e(TAG, "Video file not exists: " + str);
                return false;
            }
            MediaExtractor mediaExtractor = new MediaExtractor();
            this.mExtractor = mediaExtractor;
            mediaExtractor.setDataSource(str);
            int iFindVideoTrack = findVideoTrack();
            this.mVideoTrackIndex = iFindVideoTrack;
            if (iFindVideoTrack < 0) {
                TekProxyLog.e(TAG, "No video track found");
                close();
                return false;
            }
            this.mExtractor.selectTrack(iFindVideoTrack);
            MediaFormat trackFormat = this.mExtractor.getTrackFormat(this.mVideoTrackIndex);
            this.mVideoWidth = trackFormat.getInteger("width");
            this.mVideoHeight = trackFormat.getInteger("height");
            this.mVideoDurationUs = trackFormat.getLong("durationUs");
            long integer = trackFormat.getInteger("frame-rate");
            this.frameRate = integer;
            double d3 = this.mVideoDurationUs;
            this.mDuration = d3 / TIME_US_PER_SEC;
            TekProxyLog.i(TAG, "[" + hashCode() + "]Video total frames: " + ((long) (d3 / (TIME_US_PER_SEC / integer))));
            this.mStartSec = Math.max(0.0d, d);
            this.mEndSec = d2 > 0.0d ? Math.min(this.mDuration, d2) : this.mDuration;
            updateUseBT601ColorMatrix(this.mExtractor.getTrackFormat(this.mVideoTrackIndex));
            this.mDecoder = MediaCodec.createDecoderByType(trackFormat.getString("mime"));
            trackFormat.setInteger("color-format", 21);
            this.mDecoder.configure(trackFormat, (Surface) null, (MediaCrypto) null, 0);
            this.mDecoder.start();
            this.mExtractor.seekTo((long) (this.mStartSec * TIME_US_PER_SEC), 2);
            this.mVideoPath = str;
            this.mIsOpen = true;
            this.mIsEndOfStream = false;
            this.mInputEOS = false;
            this.mOutputEOS = false;
            TekProxyLog.i(TAG, "[" + hashCode() + "]Video opened successfully: mVideoPath=" + this.mVideoPath + " " + this.mVideoWidth + "x" + this.mVideoHeight + ", duration: " + this.mDuration + "s");
            startProducing();
            return true;
        } catch (Exception e) {
            TekProxyLog.e(TAG, "Failed to open video: " + e.getMessage());
            close();
            return false;
        }
    }

    @Override // TekEngineLib.Decoder.video.ITekAndroidVideoDecoder
    public void reset() {
        if (this.mIsOpen) {
            synchronized (this.queueLock) {
                try {
                    this.mDecoder.flush();
                    this.isProducing = false;
                    Iterator<TekVideoFrameData> it = this.frameQueue.iterator();
                    while (it.hasNext()) {
                        releaseFrameData(it.next());
                    }
                    this.frameQueue.clear();
                    this.mExtractor.seekTo((long) (this.mStartSec * TIME_US_PER_SEC), 2);
                    this.mInputEOS = false;
                    this.mOutputEOS = false;
                    this.mIsEndOfStream = false;
                    this.queueLock.notifyAll();
                    startProducing();
                    TekProxyLog.d(TAG, "[" + hashCode() + "]Decoder reset to start position: " + this.mStartSec + "s");
                } catch (Exception e) {
                    TekProxyLog.e(TAG, "Error resetting decoder: " + e.getMessage());
                }
            }
        }
    }
}
