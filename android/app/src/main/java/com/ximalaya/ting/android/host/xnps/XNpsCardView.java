package com.ximalaya.ting.android.host.xnps;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.mengzhen.app.R;
import com.ximalaya.ting.android.host.model.CustomButtonModel;
import com.ximalaya.ting.android.host.model.QuestionnaireFormTag;
import com.ximalaya.ting.android.host.model.QuestionnaireFormText;
import com.ximalaya.ting.android.host.model.XNpsQueryModel;
import com.ximalaya.ting.android.host.model.XNpsQuestionnaireFormScore;
import com.ximalaya.ting.android.host.view.layout.FlowLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ximalaya 9.5.1.4 XNpsCardView.
 *
 * The original private analytics/network calls are removed. The source submit listener is the
 * single bridge used by Mengzhen; layout, state transitions and questionnaire model stay intact.
 */
public final class XNpsCardView extends LinearLayout {
    private View mBtnLayout;
    private IOnXNpsCloseListener mCloseListener;
    private View mContentView;
    private Context mContext;
    private TextView mCountTv;
    private ConstraintLayout mCslTitleContainer;
    private String mCurrPage;
    private EditText mEditText;
    private View mEtLayout;
    private boolean mIsCarStyle;
    private ImageView mIvClose;
    private final IOnXNpsChangeListener mOnXNpsChangeListener;
    private View mReasonLayout;
    private TextView mReasonTitle;
    private View mRootView;
    private SatisfactionView mSatisfactionView;
    private XNpsQuestionnaireFormScore mSelectXNpsQuestionnaireFormScore;
    private int mSelectedScore;
    private Set<TextView> mSelectedTagList;
    private boolean mShowBtnInit;
    private boolean mShowTagAndEt;
    private TextView mSubmitLeft;
    private IOnXNpsSubmitListener mSubmitListener;
    private TextView mSubmitRight;
    private FlowLayout mTagLayout;
    private TextView mTitleView;
    private XNpsQueryModel mXNpsQueryModel;
    private XNpsScoreView mXNpsScoreView;
    private String mXmRequestId;

    // Mengzhen reads the source button action after IOnXNpsSubmitListener fires.
    private int mSelectedAction = 1;

    public XNpsCardView(Context context) {
        this(context, null, 0);
    }

    public XNpsCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XNpsCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mShowTagAndEt = true;
        mSelectedScore = -1;
        mShowBtnInit = true;
        mOnXNpsChangeListener = this::onXNpsChange;
        mContentView = LayoutInflater.from(context).inflate(R.layout.host_view_card_xnps, this, true);
        mContext = context;
        mTitleView = mContentView.findViewById(R.id.host_tv_title);
        mIvClose = mContentView.findViewById(R.id.host_iv_close);
        mCslTitleContainer = mContentView.findViewById(R.id.main_csl_xnps_title);
        mSatisfactionView = mContentView.findViewById(R.id.host_satisfaction_view_layout);
        mSatisfactionView.setMOnXNpsChangeListener(mOnXNpsChangeListener);
        mXNpsScoreView = mContentView.findViewById(R.id.host_xnps_scoreview_layout);
        mXNpsScoreView.setMOnXNpsChangeListener(mOnXNpsChangeListener);
        mReasonLayout = mContentView.findViewById(R.id.host_reason_layout);
        mReasonTitle = mContentView.findViewById(R.id.host_tv_reason_title);
        mTagLayout = mContentView.findViewById(R.id.host_reason_flowlayout);
        mEditText = mContentView.findViewById(R.id.host_reason_et);
        mCountTv = mContentView.findViewById(R.id.host_tv_reason_count);
        mSubmitLeft = mContentView.findViewById(R.id.host_tv_submit_white);
        mSubmitRight = mContentView.findViewById(R.id.host_tv_submit_feedback);
        mEtLayout = mContentView.findViewById(R.id.host_et_layout);
        mBtnLayout = mContentView.findViewById(R.id.host_btn_layout);
        mRootView = mContentView.findViewById(R.id.host_root_layout);
        setClickListener();
    }

    private void onXNpsChange(String value, int score) {
        List<XNpsQuestionnaireFormScore> scores = mXNpsQueryModel == null
                ? null : mXNpsQueryModel.getQuestionnaireFormScores();
        if (scores != null && !scores.isEmpty()) {
            for (XNpsQuestionnaireFormScore item : scores) {
                if (TextUtils.equals(item.getName(), value)) {
                    mSelectedScore = -1;
                    mSelectXNpsQuestionnaireFormScore = item;
                    QuestionnaireFormTag formTag = item.getQuestionnaireFormTag();
                    if (mShowTagAndEt && formTag != null && formTag.getTags() != null
                            && !TextUtils.isEmpty(formTag.getTitle())) {
                        mReasonLayout.setVisibility(VISIBLE);
                        setTags(mContext, mTagLayout, formTag.getTags());
                        mReasonTitle.setText(formTag.getTitle());
                    } else {
                        mReasonLayout.setVisibility(GONE);
                    }
                    break;
                }
            }
        } else {
            mSelectedScore = score;
            mReasonLayout.setVisibility(GONE);
        }
        changeCanSubmitStatus();
    }

    public void setInitData(
            boolean cardStyle,
            boolean showTagAndEt,
            String currPage,
            XNpsQueryModel model,
            IOnXNpsCloseListener closeListener
    ) {
        mCurrPage = currPage;
        mShowTagAndEt = showTagAndEt;
        mXNpsQueryModel = model;
        mCloseListener = closeListener;
        mIsCarStyle = cardStyle;
        mShowBtnInit = showTagAndEt;
        if (!showTagAndEt) {
            mBtnLayout.setVisibility(GONE);
        }
        handleData();
    }

    public void setSearchTitleStyle() {
        mTitleView.setTextSize(16.0f);
    }

    public void setSubmitListener(IOnXNpsSubmitListener submitListener) {
        mSubmitListener = submitListener;
    }

    private void handleData() {
        if (mXNpsQueryModel == null) {
            return;
        }
        mTitleView.setText(mXNpsQueryModel.getTitle());
        if (mXNpsQueryModel.getType() == 1) {
            mXNpsScoreView.setVisibility(VISIBLE);
            mSatisfactionView.setVisibility(GONE);
        } else {
            mSatisfactionView.setVisibility(VISIBLE);
            mXNpsScoreView.setVisibility(GONE);
            List<XNpsQuestionnaireFormScore> scores = mXNpsQueryModel.getQuestionnaireFormScores();
            if (scores != null && !scores.isEmpty()) {
                mSatisfactionView.initData(scores);
            }
        }
        mSubmitRight.setVisibility(GONE);
        mSubmitLeft.setVisibility(GONE);
        List<CustomButtonModel> buttons = mXNpsQueryModel.getCustomButtons();
        if (buttons != null && !buttons.isEmpty()) {
            if (buttons.size() == 2 && buttons.get(0).getColorStyle() != 1
                    && buttons.get(1).getColorStyle() == 1) {
                dealStyleForBtn(mSubmitLeft, buttons.get(1));
                dealStyleForBtn(mSubmitRight, buttons.get(0));
                mSubmitLeft.setVisibility(VISIBLE);
                mSubmitRight.setVisibility(VISIBLE);
            } else {
                dealStyleForBtn(mSubmitLeft, buttons.get(0));
                mSubmitLeft.setVisibility(VISIBLE);
                if (buttons.size() >= 2) {
                    dealStyleForBtn(mSubmitRight, buttons.get(1));
                    mSubmitRight.setVisibility(VISIBLE);
                }
            }
        }
        mEtLayout.setVisibility(mXNpsQueryModel.getQuestionnaireFormText() != null ? VISIBLE : GONE);
        changeCanSubmitStatus();
    }

    public void setXmRequestId(String xmRequestId) {
        mXmRequestId = xmRequestId;
    }

    public void exploreTrace() {
        // Source analytics endpoint belongs to Ximalaya and is intentionally not called by Mengzhen.
    }

    public String getViewStyle() {
        return mIsCarStyle ? "track" : "dialogView";
    }

    private void dealStyleForBtn(TextView btn, CustomButtonModel customButtonModel) {
        if (customButtonModel == null || btn == null) {
            return;
        }
        btn.setTag(R.id.host_tv_submit_white, customButtonModel);
        btn.setText(customButtonModel.getLabel());
        if (customButtonModel.getColorStyle() == 2) {
            btn.setBackgroundResource(R.drawable.xm_feedback_xnps_button_filled);
            btn.setTextColor(ContextCompat.getColor(mContext, R.color.xm_feedback_xnps_white));
        } else {
            btn.setBackgroundResource(R.drawable.xm_feedback_xnps_button_plain);
            btn.setTextColor(ContextCompat.getColor(mContext, R.color.xm_feedback_xnps_disabled));
        }
    }

    private void setClickListener() {
        View.OnClickListener listener = view -> {
            if (view == mIvClose) {
                if (mCloseListener != null) {
                    mCloseListener.a();
                }
                return;
            }
            if (view == mSubmitLeft || view == mSubmitRight) {
                Object tag = view.getTag(R.id.host_tv_submit_white);
                submitXNps(tag instanceof CustomButtonModel ? (CustomButtonModel) tag : null);
            }
        };
        mSubmitRight.setOnClickListener(listener);
        mSubmitLeft.setOnClickListener(listener);
        mIvClose.setOnClickListener(listener);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable editable) {
                if (editable != null) {
                    updateByTextChanged(editable.toString());
                }
            }
        });
    }

    public int getMSelectedScore() {
        return mSelectedScore;
    }

    private void submitXNps(CustomButtonModel customButtonModel) {
        int action = customButtonModel == null ? 1 : customButtonModel.getAction();
        if (mXNpsQueryModel == null) {
            return;
        }
        int score = mSelectedScore;
        if (score == -1 && mSelectXNpsQuestionnaireFormScore == null) {
            return;
        }
        if (mSelectXNpsQuestionnaireFormScore != null) {
            score = mSelectXNpsQuestionnaireFormScore.getScore();
        }
        if (mustSelectTagNotValid()) {
            return;
        }
        mSelectedAction = action;
        QuestionnaireFormTag formTag = mSelectXNpsQuestionnaireFormScore == null
                ? null : mSelectXNpsQuestionnaireFormScore.getQuestionnaireFormTag();
        if (formTag != null) {
            formTag.setTags(getSelectedTags());
            formTag.setTitle(null);
        }
        QuestionnaireFormText formText = mXNpsQueryModel.getQuestionnaireFormText();
        if (formText != null) {
            formText.setContent(String.valueOf(mEditText.getText()));
        }
        if (mSubmitListener != null) {
            mSubmitListener.a(score);
        }
    }

    private boolean mustSelectTagNotValid() {
        if (mXNpsQueryModel != null && Boolean.TRUE.equals(mXNpsQueryModel.getMustSelect())) {
            if (mSelectedTagList == null || mSelectedTagList.isEmpty()) {
                return TextUtils.isEmpty(mEditText.getText());
            }
        }
        return false;
    }

    private void updateByTextChanged(String value) {
        mCountTv.setText(String.valueOf(value.length()));
        int color = value.length() >= 300
                ? R.color.xm_feedback_xnps_accent : R.color.xm_feedback_xnps_counter;
        mCountTv.setTextColor(ContextCompat.getColor(mContext, color));
        changeCanSubmitStatus();
    }

    private void setTags(Context context, FlowLayout layout, List<String> tags) {
        if (mSelectedTagList != null) {
            mSelectedTagList.clear();
        }
        if (layout == null || context == null) {
            return;
        }
        if (tags == null || tags.isEmpty()) {
            layout.setVisibility(GONE);
            return;
        }
        if (mSelectedTagList == null) {
            mSelectedTagList = new LinkedHashSet<>();
        }
        layout.removeAllViews();
        layout.setVisibility(VISIBLE);
        int count = Math.min(tags.size(), 6);
        for (int index = 0; index < count; index++) {
            final TextView textView = new TextView(context);
            textView.setBackgroundResource(R.drawable.xm_feedback_xnps_tag);
            textView.setTextSize(13.0f);
            textView.setMinHeight(dp(context, 28));
            textView.setGravity(Gravity.CENTER);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setMaxLines(1);
            textView.setTextColor(ContextCompat.getColorStateList(context, R.color.xm_feedback_xnps_tag_text));
            textView.setText(tags.get(index));
            int horizontal = dp(context, 12);
            int vertical = dp(context, 5);
            textView.setPadding(horizontal, vertical, horizontal, vertical);
            FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(-2, -2);
            int margin = dp(context, 8);
            params.setMargins(0, margin, margin, 0);
            textView.setOnClickListener(view -> {
                textView.setSelected(!textView.isSelected());
                textView.setTypeface(textView.isSelected() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                if (mSelectedTagList.contains(textView)) {
                    mSelectedTagList.remove(textView);
                } else {
                    mSelectedTagList.add(textView);
                }
                changeCanSubmitStatus();
            });
            layout.addView(textView, params);
        }
    }

    public void changeCanSubmitStatus() {
        boolean validSelection;
        if (mXNpsScoreView.getVisibility() == VISIBLE) {
            validSelection = mXNpsScoreView.getMSelectedScoreView() != null;
        } else if (mSatisfactionView.getVisibility() == VISIBLE) {
            validSelection = mSatisfactionView.getMSelectedTv() != null;
        } else {
            validSelection = false;
        }
        boolean canSubmit = validSelection && !mustSelectTagNotValid();
        mSubmitLeft.setSelected(canSubmit);
        mSubmitRight.setSelected(canSubmit);
        applyButtonTextColor(mSubmitLeft, canSubmit);
        applyButtonTextColor(mSubmitRight, canSubmit);
    }

    private void applyButtonTextColor(TextView button, boolean canSubmit) {
        Object value = button.getTag(R.id.host_tv_submit_white);
        CustomButtonModel model = value instanceof CustomButtonModel ? (CustomButtonModel) value : null;
        if (model != null && model.getColorStyle() == 2) {
            button.setTextColor(ContextCompat.getColor(mContext, R.color.xm_feedback_xnps_white));
        } else {
            button.setTextColor(ContextCompat.getColor(
                    mContext,
                    canSubmit ? R.color.xm_feedback_xnps_accent : R.color.xm_feedback_xnps_disabled
            ));
        }
    }

    public void resetView() {
        mSatisfactionView.resetTvStatus();
        mSatisfactionView.setMSelectedTv(null);
        mXNpsScoreView.setMSelectedScoreView(null);
        mXNpsScoreView.resetView();
        mEditText.setText(null);
        mSelectXNpsQuestionnaireFormScore = null;
        if (mSelectedTagList != null) {
            mSelectedTagList.clear();
        }
        mSelectedScore = -1;
        mReasonLayout.setVisibility(GONE);
        mBtnLayout.setVisibility(mShowBtnInit ? VISIBLE : GONE);
        changeCanSubmitStatus();
    }

    public void onDestroy() {
        mSatisfactionView.setMOnXNpsChangeListener(null);
        mXNpsScoreView.setMOnXNpsChangeListener(null);
    }

    public int getSelectedAction() { return mSelectedAction; }

    private List<String> getSelectedTags() {
        if (mSelectedTagList == null || mSelectedTagList.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tags = new ArrayList<>(mSelectedTagList.size());
        for (TextView textView : mSelectedTagList) {
            if (!TextUtils.isEmpty(textView.getText())) {
                tags.add(textView.getText().toString());
            }
        }
        return tags;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
