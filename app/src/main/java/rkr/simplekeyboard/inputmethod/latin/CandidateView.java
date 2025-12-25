package rkr.simplekeyboard.inputmethod.latin;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class CandidateView extends HorizontalScrollView {
    private LinearLayout mContainer;
    private OnCandidateClickListener mListener;

    public interface OnCandidateClickListener {
        void onCandidateClick(String candidate);
    }

    public CandidateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContainer = new LinearLayout(context);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        addView(mContainer, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        // Default visibility gone until we have candidates
        this.setVisibility(View.GONE);
    }

    public void setListener(OnCandidateClickListener listener) {
        mListener = listener;
    }

    public void setCandidates(List<String> candidates) {
        mContainer.removeAllViews();
        if (candidates == null || candidates.isEmpty()) {
            this.setVisibility(View.GONE);
            return;
        }
        this.setVisibility(View.VISIBLE);

        for (String candidate : candidates) {
            TextView tv = new TextView(getContext());
            tv.setText(candidate);
            tv.setTextSize(22);
            tv.setPadding(40, 20, 40, 20);
            tv.setGravity(Gravity.CENTER);
            tv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onCandidateClick(candidate);
                    }
                }
            });
            mContainer.addView(tv);
        }
    }

    public void clear() {
        mContainer.removeAllViews();
        this.setVisibility(View.GONE);
    }
}
