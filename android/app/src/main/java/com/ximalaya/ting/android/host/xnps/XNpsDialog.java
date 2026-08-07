package com.ximalaya.ting.android.host.xnps;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.mengzhen.app.R;
import com.ximalaya.ting.android.host.model.XNpsQueryModel;

/** Ximalaya 9.5.1.4 XNpsDialog with BaseDialogFragment replaced by DialogFragment. */
public final class XNpsDialog extends DialogFragment {
    private static final String ARGUMENT_KEY_CURRPAGE = "nps_currPage";
    private static final String ARGUMENT_KEY_MODEL = "nps_model";

    private XNpsCardView xNpsCardView;
    private IOnXNpsSubmitListener submitListener;

    public static XNpsDialog newInstance(String currPage, XNpsQueryModel model) {
        Bundle bundle = new Bundle();
        bundle.putString(ARGUMENT_KEY_CURRPAGE, currPage);
        bundle.putSerializable(ARGUMENT_KEY_MODEL, model);
        XNpsDialog dialog = new XNpsDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    public String getDialogSource() {
        return "do_you_satisfy";
    }

    public void setSubmitListener(IOnXNpsSubmitListener listener) {
        submitListener = listener;
        if (xNpsCardView != null) {
            xNpsCardView.setSubmitListener(listener);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        configureDialogStyle();
        return inflater.inflate(R.layout.host_dialog_fra_xnps, container, false);
    }

    private void configureDialogStyle() {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setWindowAnimations(R.style.XmFeedbackXnpsDialogAnimation);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        xNpsCardView = view.findViewById(R.id.host_xnps_card_view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            XNpsQueryModel model = (XNpsQueryModel) arguments.getSerializable(ARGUMENT_KEY_MODEL);
            xNpsCardView.setInitData(
                    false,
                    true,
                    arguments.getString(ARGUMENT_KEY_CURRPAGE),
                    model,
                    this::dismiss
            );
            xNpsCardView.setSubmitListener(submitListener);
            xNpsCardView.exploreTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        Window window = dialog == null ? null : dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setAttributes(window.getAttributes());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (xNpsCardView != null) {
            xNpsCardView.onDestroy();
        }
        xNpsCardView = null;
    }
}
