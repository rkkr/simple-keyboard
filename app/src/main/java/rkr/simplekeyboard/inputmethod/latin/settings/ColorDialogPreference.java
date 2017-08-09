/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.latin.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import rkr.simplekeyboard.inputmethod.R;

public final class ColorDialogPreference extends DialogPreference
        implements SeekBar.OnSeekBarChangeListener {
    public interface ValueProxy {
        int readValue(final String key);
        void writeDefaultValue(final String key);
        void writeValue(final int value, final String key);
    }

    private TextView mValueView;
    private SeekBar mSeekBarRed;
    private SeekBar mSeekBarGreen;
    private SeekBar mSeekBarBlue;

    private ValueProxy mValueProxy;

    public ColorDialogPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.color_dialog);
    }

    public void setInterface(final ValueProxy proxy) {
        mValueProxy = proxy;
    }

    @Override
    protected View onCreateDialogView() {
        final View view = super.onCreateDialogView();
        mSeekBarRed = (SeekBar)view.findViewById(R.id.seek_bar_dialog_bar_red);
        mSeekBarRed.setMax(255);
        mSeekBarRed.setOnSeekBarChangeListener(this);
        mSeekBarRed.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        mSeekBarRed.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        mSeekBarGreen = (SeekBar)view.findViewById(R.id.seek_bar_dialog_bar_green);
        mSeekBarGreen.setMax(255);
        mSeekBarGreen.setOnSeekBarChangeListener(this);
        mSeekBarGreen.getThumb().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
        mSeekBarGreen.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
        mSeekBarBlue = (SeekBar)view.findViewById(R.id.seek_bar_dialog_bar_blue);
        mSeekBarBlue.setMax(255);
        mSeekBarBlue.setOnSeekBarChangeListener(this);
        mSeekBarBlue.getThumb().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
        mSeekBarBlue.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
        mValueView = (TextView)view.findViewById(R.id.seek_bar_dialog_value);
        return view;
    }

    @Override
    protected void onBindDialogView(final View view) {
        final int color = mValueProxy.readValue(getKey());
        mSeekBarRed.setProgress(Color.red(color));
        mSeekBarGreen.setProgress(Color.green(color));
        mSeekBarBlue.setProgress(Color.blue(color));
        setHeaderText(color);
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        builder.setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setNeutralButton(R.string.button_default, this);
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        super.onClick(dialog, which);
        final String key = getKey();
        if (which == DialogInterface.BUTTON_POSITIVE) {
            super.onClick(dialog, which);
            final int value = Color.rgb(
                    mSeekBarRed.getProgress(),
                    mSeekBarGreen.getProgress(),
                    mSeekBarBlue.getProgress());
            mValueProxy.writeValue(value, key);
            return;
        }
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            super.onClick(dialog, which);
            mValueProxy.writeDefaultValue(key);
            return;
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress,
                                  final boolean fromUser) {
        int color = Color.rgb(
                mSeekBarRed.getProgress(),
                mSeekBarGreen.getProgress(),
                mSeekBarBlue.getProgress());
        setHeaderText(color);
        if (!fromUser) {
            seekBar.setProgress(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void setHeaderText(int color) {
        mValueView.setText(getValueText(color));
        boolean bright = Color.red(color) + Color.green(color) + Color.blue(color) > 128 * 3;
        mValueView.setTextColor(bright ? Color.BLACK : Color.WHITE);
        mValueView.setBackgroundColor(color);
    }

    private String getValueText(final int value) {
        String temp = Integer.toHexString(value);
        for (; temp.length() < 8; temp = "0" + temp);
        return temp.substring(2).toUpperCase();
    }
}
