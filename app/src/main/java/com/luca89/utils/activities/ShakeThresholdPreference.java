/*
 * Copyright (C) 2016 Luca Di Maio <luca.dimaio1@gmail.com>
 *
 * This file is part of ThunderMusic Player.
 *
 * ThunderMusic is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * ThunderMusic is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.luca89.utils.activities;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * SeekBar preference to set the shake force threshold.
 */
public class ShakeThresholdPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static int mValue;
    /**
     * TextView to display current threshold.
     */
    private TextView mValueText;

    public ShakeThresholdPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Create the summary for the given value.
     *
     * @param value The force threshold.
     * @return A string representation of the threshold.
     */
    private static String getSummary(int value) {
        return String.valueOf(value / 10.0f);
    }

    @Override
    public CharSequence getSummary() {
        return getSummary(-getPersistedInt(95));
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        Context context = getContext();
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mValue = getPersistedInt(95);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(params);

        mValueText = new TextView(context);
        mValueText.setGravity(Gravity.RIGHT);
        mValueText.setPadding(20, 0, 20, 0);
        mValueText.setText(getSummary(-mValue));
        layout.addView(mValueText);

        SeekBar seekBar = new SeekBar(context);
        seekBar.setPadding(20, 0, 20, 20);
        seekBar.setLayoutParams(params);
        seekBar.setMax(150);
        seekBar.setProgress(mValue);
        seekBar.setOnSeekBarChangeListener(this);
        layout.addView(seekBar);
        builder.setView(layout);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && shouldPersist())
            persistInt(mValue);
        notifyChanged();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mValueText.setText(getSummary(-progress));
            mValue = progress;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
