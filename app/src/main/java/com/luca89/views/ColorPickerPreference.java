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

package com.luca89.views;


import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class ColorPickerPreference extends DialogPreference {

    public static final int DEFAULT_COLOR = Color.WHITE;
    private static TextView currentColor;
    private int selectedColor;
    private ColorPicker colorPickerView;

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static void setColorText(String color) {
        currentColor.setText((color));
    }

    @Override
    protected View onCreateDialogView() {

        RelativeLayout relativeLayout = new RelativeLayout(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        layoutParams.addRule(RelativeLayout.BELOW, 2);


        LayoutParams layoutParamsText = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParamsText.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParamsText.addRule(RelativeLayout.CENTER_HORIZONTAL);

        colorPickerView = new ColorPicker(getContext());
        colorPickerView.setId(1);

        currentColor = new TextView(getContext());
        currentColor.setTextSize(16);
        currentColor.setId(2);

        relativeLayout.addView(colorPickerView, layoutParams);
        relativeLayout.addView(currentColor, layoutParamsText);

        return relativeLayout;

    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        colorPickerView.setColor(selectedColor);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(null); // remove dialog title to get more space for color picker
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && shouldPersist()) {
            if (callChangeListener(colorPickerView.getColor())) {
                selectedColor = colorPickerView.getColor();
                persistInt(selectedColor);
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        selectedColor = restoreValue ? getPersistedInt(DEFAULT_COLOR) : (Integer) defaultValue;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_COLOR);
    }

}
