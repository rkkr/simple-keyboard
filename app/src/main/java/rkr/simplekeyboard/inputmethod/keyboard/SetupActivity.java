package rkr.simplekeyboard.inputmethod.keyboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsActivity;

public class SetupActivity extends SettingsActivity {
    private static final String TAG = SetupActivity.class.getSimpleName();

    @Override
    protected void onStart() {
        super.onStart();

        boolean enabled = false;
        try {
            InputMethodManager immService = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            RichInputMethodManager.InputMethodInfoCache inputMethodInfoCache = new RichInputMethodManager.InputMethodInfoCache(immService, getPackageName());
            enabled = inputMethodInfoCache.isInputMethodOfThisImeEnabled();
        }
        catch (Exception e) {
            Log.e(TAG, "Exception in check if input method is enabled", e);
        }

        if (!enabled) {
            final Context context = this;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.setup_message);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            builder.setCancelable(false);

            builder.create().show();
        }
    }
}
