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

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class SummariedListPreference extends ListPreference {
    public SummariedListPreference(Context context, AttributeSet set) {
        super(context, set);
    }


    @Override
    public void setValue(String value) {
        super.setValue(value);

        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();
        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i].equals(value)) {
                setSummary(entries[i]);
                break;
            }
        }
    }


    public void refreshFromPreference() {
        onSetInitialValue(true, null);
    }
}