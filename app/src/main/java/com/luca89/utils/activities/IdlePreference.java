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
import android.content.res.Resources;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.luca89.thundermusic.R;

/**
 * A preference that provides a dialog with a slider for idle time.
 * <p>
 * The slider produces a value in seconds from 60 (1 minute) to 21600
 * (6 hours). The values range on an approximately exponential scale.
 */
public class IdlePreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final int DEFAULT_VALUE = 3600;
    private static final int MIN = 60;
    private static final int MAX = 21600;

    /**
     * The current idle timeout displayed on the slider. Will not be persisted
     * until the dialog is closed.
     */
    private static int mValue;
    /**
     * The view in which the value is displayed.
     */
    private TextView mValueText;

    public IdlePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        Context context = getContext();
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mValue = getPersistedInt(DEFAULT_VALUE);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(params);

        mValueText = new TextView(context);
        mValueText.setGravity(Gravity.RIGHT);
        mValueText.setPadding(20, 0, 20, 0);
        layout.addView(mValueText);

        SeekBar seekBar = new SeekBar(context);
        seekBar.setPadding(20, 0, 20, 20);
        seekBar.setLayoutParams(params);
        seekBar.setMax(1000);
        seekBar.setProgress((int) (Math.pow((float) (mValue - MIN) / (MAX - MIN), 0.25f) * 1000));
        seekBar.setOnSeekBarChangeListener(this);
        layout.addView(seekBar);

        updateText();

        builder.setView(layout);
    }

    /**
     * Update the text view with the current value.
     */
    private void updateText() {
        mValueText.setText(formatTime(mValue));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && shouldPersist())
            persistInt(mValue);
        notifyChanged();
    }

    @Override
    public CharSequence getSummary() {
        return formatTime(getPersistedInt(DEFAULT_VALUE));
    }

    /**
     * Format seconds into a human-readable time description.
     *
     * @param value The time, in seconds.
     * @return A human-readable string, such as "1 hour, 21 minutes"
     */
    private String formatTime(int value) {
        Resources res = getContext().getResources();
        StringBuilder text = new StringBuilder();
        if (value >= 3600) {
            int hours = value / 3600;
            text.append(res.getQuantityString(R.plurals.hours, hours, hours));
            text.append(", ");
            int minutes = value / 60 - hours * 60;
            text.append(res.getQuantityString(R.plurals.minutes, minutes, minutes));
        } else {
            int minutes = value / 60;
            text.append(res.getQuantityString(R.plurals.minutes, minutes, minutes));
        }
        return text.toString();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Approximate an exponential curve with x^4. Produces a value from 60-10860.
        if (fromUser) {
            float value = seekBar.getProgress() / 1000.0f;
            value *= value;
            value *= value;
            mValue = (int) (value * (MAX - MIN)) + MIN;
            updateText();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
