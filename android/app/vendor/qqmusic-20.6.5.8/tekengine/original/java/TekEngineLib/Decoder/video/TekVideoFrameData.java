package TekEngineLib.Decoder.video;

import TekEngineLib.State.TekProxyLog;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekVideoFrameData {
    private static final String TAG = "TekVideoFrameData";
    public float[] colorMatrix;
    public float colorOffsetU;
    public float colorOffsetV;
    public int width = 0;
    public int height = 0;
    public int alignedWidth = 0;
    public int bytesPerRow = 0;
    public int uvBytesPerRow = 0;
    public int pixelFormat = 2;
    public int status = 0;
    public Object nativeHandle = null;
    public ByteBuffer yData = null;
    public ByteBuffer uvData = null;
    private Runnable releaseFunction = null;
    private volatile boolean isReleased = false;
    public int uvWidth = 0;
    public int uvHeight = 0;

    public TekVideoFrameData() {
        float[] fArr = {1.0f, 0.0f, 1.57481f, 1.0f, -0.18732f, -0.46813f, 1.0f, 1.8556f, 0.0f};
        this.colorMatrix = fArr;
        fArr[0] = 1.0f;
        fArr[1] = 0.0f;
        fArr[2] = 1.57481f;
        fArr[3] = 1.0f;
        fArr[4] = -0.18732f;
        fArr[5] = -0.46813f;
        fArr[6] = 1.0f;
        fArr[7] = 1.8556f;
        fArr[8] = 0.0f;
        this.colorOffsetU = 0.5f;
        this.colorOffsetV = 0.5f;
    }

    public String getInfo() {
        Object[] objArr = new Object[7];
        objArr[0] = Integer.valueOf(this.width);
        objArr[1] = Integer.valueOf(this.height);
        objArr[2] = Integer.valueOf(this.pixelFormat);
        objArr[3] = Integer.valueOf(this.status);
        objArr[4] = Integer.valueOf(this.alignedWidth);
        objArr[5] = Integer.valueOf(this.bytesPerRow);
        objArr[6] = this.nativeHandle != null ? "valid" : "null";
        return String.format("Frame[size=%dx%d, format=%d, status=%d, aligned=%d, bytesPerRow=%d, nativeHandle=%s]", objArr);
    }

    public boolean isValid() {
        return this.width > 0 && this.height > 0 && this.yData != null && !(this.pixelFormat == 0 && this.uvData == null);
    }

    public void release() {
        if (this.isReleased) {
            TekProxyLog.d(TAG, "TekVideoFrameData already released");
            return;
        }
        this.isReleased = true;
        Runnable runnable = this.releaseFunction;
        if (runnable != null) {
            try {
                runnable.run();
            } catch (Exception e) {
                TekProxyLog.w(TAG, "Error in releaseFunction: " + e.getMessage());
            }
        }
        try {
            ByteBuffer byteBuffer = this.yData;
            if (byteBuffer != null) {
                byteBuffer.clear();
                this.yData = null;
            }
            ByteBuffer byteBuffer2 = this.uvData;
            if (byteBuffer2 != null) {
                byteBuffer2.clear();
                this.uvData = null;
            }
            int i = 0;
            this.width = 0;
            this.height = 0;
            this.alignedWidth = 0;
            this.bytesPerRow = 0;
            this.uvBytesPerRow = 0;
            this.uvWidth = 0;
            this.uvHeight = 0;
            this.pixelFormat = 0;
            this.status = 0;
            if (this.colorMatrix != null) {
                while (true) {
                    float[] fArr = this.colorMatrix;
                    if (i >= fArr.length) {
                        break;
                    }
                    fArr[i] = 0.0f;
                    i++;
                }
            }
            this.colorOffsetU = 0.0f;
            this.colorOffsetV = 0.0f;
        } catch (Exception e2) {
            TekProxyLog.w(TAG, "Error releasing frame data: " + e2.getMessage());
        }
    }

    public void setReleaseFunction(Runnable runnable) {
        this.releaseFunction = runnable;
    }
}
