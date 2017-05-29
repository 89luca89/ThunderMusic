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
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.luca89.thundermusic.R;
import com.luca89.thundermusic.activities.LyricsActivity;

public class SearchPopup extends Activity {
    private static String mArtist;
    private static String mAlbum;
    private static String mTrack;
    private static boolean mAlbumArt;
    private EditText mEditTitle;
    private EditText mEditAlbum;
    private EditText mEditArtist;
    private Button mButton;
    private Activity activity;
    private View.OnClickListener mOpenClicked = new View.OnClickListener() {
        public void onClick(View v) {
            if (mTrack != null) {
                Intent intent21 = new Intent();
                intent21.setClass(activity, LyricsActivity.class);
                intent21.putExtra("track", mEditTitle.getText().toString());
                intent21.putExtra("trackor", mTrack);
                intent21.putExtra("artist", mEditArtist.getText().toString());
                intent21.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent21);
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {

        Intent intent = getIntent();
        if (icicle != null) {
            mAlbum = icicle.getString("album");
            mArtist = icicle.getString("artist");
            mTrack = icicle.getString("song");
            mAlbumArt = icicle.getBoolean("artwork", true);
        } else {
            mAlbum = intent.getStringExtra("album");
            mArtist = intent.getStringExtra("artist");
            mTrack = intent.getStringExtra("song");
            mAlbumArt = intent.getBooleanExtra("artwork", true);
        }
        super.onCreate(icicle);
        activity = this;
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.search_layout);

        mEditTitle = (EditText) findViewById(R.id.editTextTitle);
        mEditAlbum = (EditText) findViewById(R.id.editTextAlbum);
        mEditArtist = (EditText) findViewById(R.id.editTextArtist);
        mButton = (Button) findViewById(R.id.create);
        mButton.setOnClickListener(mOpenClicked);

        findViewById(R.id.cancel)
                .setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        finish();
                    }
                });
        if (!mAlbumArt) {
            mEditTitle.setText(mTrack);
            mEditAlbum.setVisibility(View.GONE);
        } else {
            mEditAlbum.setText(mAlbum);
            mEditTitle.setVisibility(View.GONE);
        }
        mEditArtist.setText(mArtist);
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("song", mEditTitle.getText().toString());
        outcicle.putString("album", mEditAlbum.getText().toString());
        outcicle.putString("artist", mEditArtist.getText().toString());
        outcicle.putBoolean("artwork", mAlbumArt);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
