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

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.luca89.thundermusic.R;
import com.luca89.utils.MusicUtils;

import java.io.File;

public class OnlineSearchClearHistory extends Activity {
    private TextView mPrompt;
    private Button mButton;
    private Activity activity;
    private View.OnClickListener mButtonClicked = new View.OnClickListener() {
        public void onClick(View v) {
            // delete the selected item(s)
            String path = (getBaseContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.online/search")
                    .toString();
            File pathFile = new File(path);

            if (pathFile.exists()) {
                pathFile.delete();
            }
            MusicUtils.resetArrayPref(activity.getBaseContext(), "history");
            MusicUtils.resetArrayPref(activity.getBaseContext(), "radiohistory");
            finish();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.confirm_delete);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        activity = this;
        mPrompt = (TextView) findViewById(R.id.prompt);
        mButton = (Button) findViewById(R.id.delete);
        mButton.setOnClickListener(mButtonClicked);

        (findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        mPrompt.setText(getString(R.string.clear_online_dialog));
    }
}
