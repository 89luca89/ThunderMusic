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

import android.app.ListActivity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.luca89.thundermusic.R;
import com.luca89.utils.MusicUtils;

public class WeekSelector extends ListActivity {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.weekpicker);
        ListView mWeeks = getListView();
        mWeeks.setTextFilterEnabled(true);
        setTitle(R.string.weekpicker_title);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        int numweeks = position + 2;
        MusicUtils.setIntPref(WeekSelector.this, "numweeks", numweeks);
        setResult(RESULT_OK);
        finish();
    }
}
