package com.ximalaya.ting.android.host.xnps;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.mengzhen.app.R;

/** Ximalaya 9.5.1.4 XNpsScoreView, with only private utility calls replaced. */
public final class XNpsScoreView extends ConstraintLayout {
    private View mContentView;
    private Context mContext;
    private IOnXNpsChangeListener mOnXNpsChangeListener;
    private LinearLayout mScoreLayout;
    private View mScoreTopLayout;
    private View mSelectedScoreView;

    public XNpsScoreView(Context context) {
        this(context, null, 0);
    }

    public XNpsScoreView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XNpsScoreView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContentView = LayoutInflater.from(context).inflate(R.layout.host_view_xnps_score, this, true);
        mScoreLayout = mContentView.findViewById(R.id.host_layout_score);
        mScoreTopLayout = mContentView.findViewById(R.id.host_layout_score_top);
        mContext = context;
        addScoreTv(context, mScoreLayout);
    }

    public View getMSelectedScoreView() {
        return mSelectedScoreView;
    }

    public void setMSelectedScoreView(View view) {
        mSelectedScoreView = view;
    }

    public IOnXNpsChangeListener getMOnXNpsChangeListener() {
        return mOnXNpsChangeListener;
    }

    public void setMOnXNpsChangeListener(IOnXNpsChangeListener listener) {
        mOnXNpsChangeListener = listener;
    }

    public void resetView() {
        addScoreTv(mContext, mScoreLayout);
    }

    private void addScoreTv(Context context, LinearLayout layout) {
        if (layout == null || context == null) {
            return;
        }
        layout.removeAllViews();
        layout.setVisibility(VISIBLE);
        int width = context.getResources().getDisplayMetrics().widthPixels;
        int scoreWidth = (width - dp(context, 32)) / 11;
        for (int score = 0; score < 11; score++) {
            final TextView textView = new TextView(context);
            int background;
            if (score == 0) {
                background = R.drawable.xm_feedback_xnps_score_start;
            } else if (score == 7 || score == 8) {
                background = R.drawable.xm_feedback_xnps_score_neutral;
            } else if (score == 9) {
                background = R.drawable.xm_feedback_xnps_score_willing;
            } else if (score == 10) {
                background = R.drawable.xm_feedback_xnps_score_end;
            } else {
                background = R.drawable.xm_feedback_xnps_score_reluctant;
            }
            textView.setBackgroundResource(background);
            textView.setTextSize(14.0f);
            textView.setWidth(scoreWidth);
            textView.setHeight(scoreWidth);
            textView.setGravity(Gravity.CENTER);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setMaxLines(1);
            textView.setTextColor(ContextCompat.getColorStateList(context, R.color.xm_feedback_xnps_score_text));
            textView.setText(String.valueOf(score));
            textView.setOnClickListener(view -> onScoreClick(textView, view));
            layout.addView(textView, new LinearLayout.LayoutParams(scoreWidth, scoreWidth));
        }
    }

    private void onScoreClick(TextView scoreView, View view) {
        if (mSelectedScoreView != null) {
            mSelectedScoreView.setSelected(false);
            ((TextView) mSelectedScoreView).setTypeface(Typeface.DEFAULT);
        }
        view.setSelected(!view.isSelected());
        mSelectedScoreView = scoreView;
        TextView selected = (TextView) view;
        selected.setTypeface(selected.isSelected() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        if (mOnXNpsChangeListener != null) {
            int score = Integer.parseInt(selected.getText().toString());
            mOnXNpsChangeListener.a(selected.getText().toString(), score);
        }
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
