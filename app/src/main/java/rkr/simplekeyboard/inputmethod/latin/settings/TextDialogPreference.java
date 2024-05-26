package rkr.simplekeyboard.inputmethod.latin.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import rkr.simplekeyboard.inputmethod.R;

public class TextDialogPreference extends DialogPreference  {

    private EditText mEditTextView;
    public TextDialogPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.edit_text_dialog);
    }

    @Override
    protected View onCreateDialogView() {
        final View view = super.onCreateDialogView();
        mEditTextView = (EditText) view.findViewById(R.id.dialog_edit_text);
        final SharedPreferences prefs = getSharedPreferences();
        String string = prefs.getString(getKey(), "");
        mEditTextView.setText(string);
        return view;
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        super.onClick(dialog, which);
        final String key = getKey();

        final SharedPreferences prefs = getSharedPreferences();

        if (which == DialogInterface.BUTTON_POSITIVE) {
            prefs.edit().putString(key,mEditTextView.getText().toString()).apply();
        }
    }
}
