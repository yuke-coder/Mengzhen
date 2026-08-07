package com.ximalaya.ting.android.host.xnps;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.mengzhen.app.R;
import com.ximalaya.ting.android.host.model.XNpsQuestionnaireFormScore;

import java.util.List;

/** Ximalaya 9.5.1.4 SatisfactionView, with XmLottieAnimationView replaced by LottieAnimationView. */
public final class SatisfactionView extends ConstraintLayout {
    private View mContentView;
    private IOnXNpsChangeListener mOnXNpsChangeListener;
    private LottieAnimationView mSatisfactionIv1;
    private LottieAnimationView mSatisfactionIv2;
    private LottieAnimationView mSatisfactionIv3;
    private LottieAnimationView mSatisfactionIv4;
    private LottieAnimationView mSatisfactionIv5;
    private TextView mSatisfactionTv1;
    private TextView mSatisfactionTv2;
    private TextView mSatisfactionTv3;
    private TextView mSatisfactionTv4;
    private TextView mSatisfactionTv5;
    private View mSelectedTv;

    public SatisfactionView(Context context) {
        this(context, null, 0);
    }

    public SatisfactionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SatisfactionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContentView = LayoutInflater.from(context).inflate(R.layout.host_view_satisfaction, this, true);
        mSatisfactionIv1 = mContentView.findViewById(R.id.host_satisfaction_iv1);
        mSatisfactionIv2 = mContentView.findViewById(R.id.host_satisfaction_iv2);
        mSatisfactionIv3 = mContentView.findViewById(R.id.host_satisfaction_iv3);
        mSatisfactionIv4 = mContentView.findViewById(R.id.host_satisfaction_iv4);
        mSatisfactionIv5 = mContentView.findViewById(R.id.host_satisfaction_iv5);
        mSatisfactionIv1.setAnimationFromUrl("https://aod.cos.tx.xmcdn.com/storages/29f0-audiofreehighqps/86/86/GAqhp50L27qhAACR5gOcHwdG.json");
        mSatisfactionIv2.setAnimationFromUrl("https://aod.cos.tx.xmcdn.com/storages/4c2d-audiofreehighqps/E7/BF/GAqh9sAL27qhAACIGQOcHweh.json");
        mSatisfactionIv3.setAnimationFromUrl("https://aod.cos.tx.xmcdn.com/storages/fd37-audiofreehighqps/B0/6C/GKwRIW4L27qgAABpJQOcHwbM.json");
        mSatisfactionIv4.setAnimationFromUrl("https://aod.cos.tx.xmcdn.com/storages/c912-audiofreehighqps/E0/9E/GAqhF9kL27qiAACFQwOcHwhH.json");
        mSatisfactionIv5.setAnimationFromUrl("https://aod.cos.tx.xmcdn.com/storages/b751-audiofreehighqps/A0/41/GKwRIRwL27qhAAEL5AOcHwfz.json");
        mSatisfactionTv1 = mContentView.findViewById(R.id.host_satisfaction_tv1);
        mSatisfactionTv2 = mContentView.findViewById(R.id.host_satisfaction_tv2);
        mSatisfactionTv3 = mContentView.findViewById(R.id.host_satisfaction_tv3);
        mSatisfactionTv4 = mContentView.findViewById(R.id.host_satisfaction_tv4);
        mSatisfactionTv5 = mContentView.findViewById(R.id.host_satisfaction_tv5);

        View.OnClickListener listener = this::onSatisfactionClick;
        mSatisfactionIv1.setOnClickListener(listener);
        mSatisfactionIv2.setOnClickListener(listener);
        mSatisfactionIv3.setOnClickListener(listener);
        mSatisfactionIv4.setOnClickListener(listener);
        mSatisfactionIv5.setOnClickListener(listener);
        mSatisfactionTv1.setOnClickListener(listener);
        mSatisfactionTv2.setOnClickListener(listener);
        mSatisfactionTv3.setOnClickListener(listener);
        mSatisfactionTv4.setOnClickListener(listener);
        mSatisfactionTv5.setOnClickListener(listener);
    }

    public IOnXNpsChangeListener getMOnXNpsChangeListener() { return mOnXNpsChangeListener; }
    public void setMOnXNpsChangeListener(IOnXNpsChangeListener listener) { mOnXNpsChangeListener = listener; }
    public View getMSelectedTv() { return mSelectedTv; }
    public void setMSelectedTv(View view) { mSelectedTv = view; }

    private void onSatisfactionClick(View view) {
        if (view.isSelected()) {
            return;
        }
        resetTvStatus();
        doVibratorAction();
        mSelectedTv = view;
        if (view == mSatisfactionTv1 || view == mSatisfactionIv1) {
            select(mSatisfactionTv1, mSatisfactionIv1, 1);
        } else if (view == mSatisfactionTv2 || view == mSatisfactionIv2) {
            select(mSatisfactionTv2, mSatisfactionIv2, 2);
        } else if (view == mSatisfactionTv3 || view == mSatisfactionIv3) {
            select(mSatisfactionTv3, mSatisfactionIv3, 3);
        } else if (view == mSatisfactionTv4 || view == mSatisfactionIv4) {
            select(mSatisfactionTv4, mSatisfactionIv4, 4);
        } else if (view == mSatisfactionTv5 || view == mSatisfactionIv5) {
            select(mSatisfactionTv5, mSatisfactionIv5, 5);
        }
    }

    private void select(TextView textView, LottieAnimationView animationView, int score) {
        textView.setSelected(true);
        animationView.setSelected(true);
        textView.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        animationView.playAnimation();
        if (mOnXNpsChangeListener != null) {
            mOnXNpsChangeListener.a(String.valueOf(textView.getText()), score);
        }
    }

    public void initData(List<XNpsQuestionnaireFormScore> dataList) {
        if (dataList.size() == 5) {
            mSatisfactionTv1.setText(dataList.get(0).getName());
            mSatisfactionTv2.setText(dataList.get(1).getName());
            mSatisfactionTv3.setText(dataList.get(2).getName());
            mSatisfactionTv4.setText(dataList.get(3).getName());
            mSatisfactionTv5.setText(dataList.get(4).getName());
        }
    }

    public void resetTvStatus() {
        reset(mSatisfactionTv1, mSatisfactionIv1);
        reset(mSatisfactionTv2, mSatisfactionIv2);
        reset(mSatisfactionTv3, mSatisfactionIv3);
        reset(mSatisfactionTv4, mSatisfactionIv4);
        reset(mSatisfactionTv5, mSatisfactionIv5);
    }

    private static void reset(TextView textView, LottieAnimationView animationView) {
        if (textView.isSelected()) {
            animationView.setProgress(0.0f);
        }
        animationView.cancelAnimation();
        textView.setSelected(false);
        animationView.setSelected(false);
        textView.setTypeface(Typeface.DEFAULT);
    }

    public void doVibratorAction() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(50L);
        }
    }
}
