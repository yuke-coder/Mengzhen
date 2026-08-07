package TekEngineLib.Render;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.ColorInt;
import java.util.Map;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public abstract class TekBaseShader {
    private static final int DEFAULT_BG_COLOR = 0;
    private static final Pair<Integer, Integer> DEFAULT_COLOR_PAIR = new Pair<>(-16777216, -1);
    public static final float FPS = 30.0f;
    public static final float FRAME_INTERVAL = 33.333332f;
    private static final int MSG_BG_COLOR_CHANGE = 2;
    private static final int MSG_COLOR_CHANGE = 1;
    public static final float PERCENT_DELTA = 0.030000001f;
    private static final String TAG = "TekBaseShader";
    protected int mIsForSurfaceView = 0;
    protected float aspectRatio = 1.0f;
    protected Pair<Integer, Integer> magicColorPair = DEFAULT_COLOR_PAIR;
    private Pair<Integer, Integer> targetMagicColorPair = null;
    private boolean isChangingMagicColor = false;
    private float currentColorChangeTimePercent = 0.0f;
    private int mOriginBgColor = 0;
    protected int mBgColor = 0;
    private Integer mTargetBgColor = null;
    private boolean mIsChangingBgColor = false;
    private float mCurrentBgColorChangeTimePercent = 0.0f;
    protected boolean isStart = false;
    private Handler mColorChangeHandler = new a(Looper.getMainLooper());

    class a extends Handler {
        a(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            int i = message.what;
            int i2 = 1;
            if (i != 1) {
                i2 = 2;
                if (i != 2 || TekBaseShader.this.mTargetBgColor == null) {
                    return;
                }
                TekBaseShader tekBaseShader = TekBaseShader.this;
                tekBaseShader.mCurrentBgColorChangeTimePercent = Math.min(tekBaseShader.mCurrentBgColorChangeTimePercent + 0.030000001f, 1.0f);
                if (TekBaseShader.this.mCurrentBgColorChangeTimePercent < 1.0f) {
                    TekBaseShader tekBaseShader2 = TekBaseShader.this;
                    tekBaseShader2.mBgColor = tekBaseShader2.getCurrentChangeMagicColor(tekBaseShader2.mOriginBgColor, TekBaseShader.this.mTargetBgColor.intValue(), TekBaseShader.this.mCurrentBgColorChangeTimePercent);
                } else {
                    TekBaseShader tekBaseShader3 = TekBaseShader.this;
                    tekBaseShader3.mBgColor = tekBaseShader3.mTargetBgColor.intValue();
                    TekBaseShader.this.mIsChangingBgColor = false;
                    TekBaseShader.this.mTargetBgColor = null;
                    TekBaseShader tekBaseShader4 = TekBaseShader.this;
                    tekBaseShader4.mOriginBgColor = tekBaseShader4.mBgColor;
                }
                if (!TekBaseShader.this.mIsChangingBgColor) {
                    return;
                }
            } else {
                if (TekBaseShader.this.targetMagicColorPair == null) {
                    return;
                }
                TekBaseShader tekBaseShader5 = TekBaseShader.this;
                tekBaseShader5.currentColorChangeTimePercent = Math.min(tekBaseShader5.currentColorChangeTimePercent + 0.030000001f, 1.0f);
                if (TekBaseShader.this.currentColorChangeTimePercent < 1.0f) {
                    TekBaseShader tekBaseShader6 = TekBaseShader.this;
                    tekBaseShader6.magicColorPair = tekBaseShader6.getCurrentChangeMagicColor(tekBaseShader6.magicColorPair, tekBaseShader6.targetMagicColorPair, TekBaseShader.this.currentColorChangeTimePercent);
                } else {
                    TekBaseShader tekBaseShader7 = TekBaseShader.this;
                    tekBaseShader7.magicColorPair = tekBaseShader7.targetMagicColorPair;
                    TekBaseShader.this.isChangingMagicColor = false;
                    TekBaseShader.this.targetMagicColorPair = null;
                }
                if (!TekBaseShader.this.isChangingMagicColor) {
                    return;
                }
            }
            sendEmptyMessageDelayed(i2, 33L);
        }
    }

    public static float[] toBgColorFloats(@ColorInt int i) {
        return new float[]{Color.red(i) / 255.0f, Color.green(i) / 255.0f, Color.blue(i) / 255.0f, Color.alpha(i) / 255.0f};
    }

    public static float[] toColorFloats(@ColorInt int i) {
        return new float[]{Color.red(i) / 255.0f, Color.green(i) / 255.0f, Color.blue(i) / 255.0f, 1.0f};
    }

    public void clearGLSLSelf() {
        resetMagicColor();
    }

    public String getClassSimpleName() {
        return getClass().getSimpleName();
    }

    protected int getCurrentChangeMagicColor(int i, int i2, float f) {
        return Color.argb((int) (Color.alpha(i) + ((Color.alpha(i2) - Color.alpha(i)) * f)), (int) (Color.red(i) + ((Color.red(i2) - Color.red(i)) * f)), (int) (Color.green(i) + ((Color.green(i2) - Color.green(i)) * f)), (int) (Color.blue(i) + ((Color.blue(i2) - Color.blue(i)) * f)));
    }

    protected Pair<Integer, Integer> getCurrentChangeMagicColor(Pair<Integer, Integer> pair, Pair<Integer, Integer> pair2, float f) {
        return new Pair<>(Integer.valueOf(getCurrentChangeMagicColor(((Integer) pair.first).intValue(), ((Integer) pair2.first).intValue(), f)), Integer.valueOf(getCurrentChangeMagicColor(((Integer) pair.second).intValue(), ((Integer) pair2.second).intValue(), f)));
    }

    public abstract Map<String, Object> getParamMap();

    public void resetMagicColor() {
        this.mColorChangeHandler.removeCallbacksAndMessages(null);
        this.magicColorPair = DEFAULT_COLOR_PAIR;
        this.mBgColor = 0;
    }

    public void setAspectRadio(float f) {
        this.aspectRatio = f;
    }

    public void setBgColor(@ColorInt int i, boolean z) {
        Log.i(TAG, "[setBgColor] mBgColor=" + Integer.toHexString(this.mBgColor) + ", bgColor=" + Integer.toHexString(i));
        this.mIsForSurfaceView = 1;
        if (this.mIsChangingBgColor) {
            Integer num = this.mTargetBgColor;
            if (num != null) {
                this.mBgColor = num.intValue();
                this.mCurrentBgColorChangeTimePercent = 1.0f;
                this.mColorChangeHandler.removeMessages(2);
                this.mIsChangingBgColor = false;
            }
            this.mTargetBgColor = null;
            this.mOriginBgColor = this.mBgColor;
        }
        int i2 = this.mBgColor;
        if (i2 == i) {
            return;
        }
        if (i2 == 0 || !z) {
            this.mBgColor = i;
            return;
        }
        this.mOriginBgColor = i2;
        this.mTargetBgColor = Integer.valueOf(i);
        this.mIsChangingBgColor = true;
        this.mCurrentBgColorChangeTimePercent = 0.0f;
        this.mColorChangeHandler.removeMessages(2);
        this.mColorChangeHandler.sendEmptyMessage(2);
    }

    public void setMagicColor(Pair<Integer, Integer> pair) {
        if (this.magicColorPair == DEFAULT_COLOR_PAIR) {
            this.magicColorPair = pair;
            return;
        }
        this.targetMagicColorPair = pair;
        this.isChangingMagicColor = true;
        this.currentColorChangeTimePercent = 0.0f;
        this.mColorChangeHandler.removeMessages(1);
        this.mColorChangeHandler.sendEmptyMessage(1);
    }

    public void setMagicColor(Pair<Integer, Integer> pair, boolean z) {
        if (z) {
            setMagicColor(pair);
            return;
        }
        this.magicColorPair = pair;
        this.targetMagicColorPair = null;
        this.currentColorChangeTimePercent = 0.0f;
        this.isChangingMagicColor = false;
        this.mColorChangeHandler.removeMessages(1);
    }

    public void start() {
        this.isStart = true;
    }

    public void stop(boolean z) {
        this.isStart = false;
    }
}
