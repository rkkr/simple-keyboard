package rkr.simplekeyboard.inputmethod.keyboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsActivity;

public class SetupActivity extends SettingsActivity {

    @Override
    protected void onStart() {
        super.onStart();

        RichInputMethodManager.init(this);

        if (!RichInputMethodManager.getInstance().isInputMethodOfThisImeEnabled()) {
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
