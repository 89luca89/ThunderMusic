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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.luca89.thundermusic.R;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.dataset.Library;
import com.luca89.utils.dataset.OnlineTrack;

import java.io.IOException;

public class CreatePlaylist extends Activity {
    private static Library mLib;
    private EditText mPlaylist;
    private TextView mPrompt;
    private Button mSaveButton;
    TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // don't care about this one
        }

        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            String newText = mPlaylist.getText().toString();
            if (newText.trim().length() == 0) {
                mSaveButton.setEnabled(false);
            } else {
                mSaveButton.setEnabled(true);
                // check if playlist with current name exists already, and warn
                // the user if so.
                if (MusicUtils.idForplaylist(newText, getBaseContext()) >= 0) {
                    mSaveButton
                            .setText(R.string.create_playlist_overwrite_text);
                } else {
                    mSaveButton.setText(R.string.create_playlist_create_text);
                }
            }
        }

        public void afterTextChanged(Editable s) {
            // don't care about this one
        }
    };
    private boolean online;
    private OnlineTrack mTrack = new OnlineTrack("", "", "", "",
            "", "", 0, 0, "", false);
    private View.OnClickListener mOpenClicked = new View.OnClickListener() {
        public void onClick(View v) {
            if (online) {
                String name = mPlaylist.getText().toString();
                if (name != null && name.length() > 0) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues values = new ContentValues(2);
                    values.put(MediaStore.Audio.Playlists.NAME, name);
                    values.put(MediaStore.Audio.Playlists._ID,
                            MusicUtils.getPlaylistiD(name));
                    resolver.insert(
                            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                            values);
                }
                if (mLib != null) {
                    try {
                        MusicUtils.addToPlaylistLib(CreatePlaylist.this, name,
                                mLib);
                    } catch (IOException e) {
                        // 
                        e.printStackTrace();
                    }
                } else {
                    MusicUtils.addToPlaylist(CreatePlaylist.this, name, mTrack);
                }
                finish();
            } else {
                String name = mPlaylist.getText().toString();
                if (name != null && name.length() > 0) {
                    ContentResolver resolver = getContentResolver();
                    int id = MusicUtils.idForplaylist(name, getBaseContext());
                    Uri uri;
                    if (id >= 0) {
                        uri = ContentUris
                                .withAppendedId(
                                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                        id);
                    } else {
                        ContentValues values = new ContentValues(1);
                        values.put(MediaStore.Audio.Playlists.NAME, name);
                        uri = resolver
                                .insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                        values);
                    }
                    setResult(RESULT_OK, (new Intent()).setData(uri));
                    finish();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        Intent intent = getIntent();
        if (icicle != null) {
            online = icicle.getBoolean("online", false);
            Bundle tmp = icicle.getBundle("track");
            if (tmp != null) {
                mLib = (Library) tmp.get("LIBRARY");
                mTrack = (OnlineTrack) tmp.get("TRACK");
            }
        } else {
            online = intent.getBooleanExtra("online", false);
            Bundle tmp = intent.getBundleExtra("track");
            if (tmp != null) {
                mLib = (Library) tmp.get("LIBRARY");
                mTrack = (OnlineTrack) tmp.get("TRACK");
            }
        }
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.create_playlist);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

        mPrompt = (TextView) findViewById(R.id.prompt);
        mPlaylist = (EditText) findViewById(R.id.playlist);
        mSaveButton = (Button) findViewById(R.id.create);
        mSaveButton.setOnClickListener(mOpenClicked);

        findViewById(R.id.cancel)
                .setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        finish();
                    }
                });

        String defaultname = icicle != null ? icicle.getString("defaultname")
                : makePlaylistName();
        if (defaultname == null) {
            finish();
            return;
        }
        String promptformat = getString(R.string.create_playlist_create_text_prompt);
        String prompt = String.format(promptformat, defaultname);
        mPrompt.setText(prompt);
        mPlaylist.setText(defaultname);
        mPlaylist.setSelection(defaultname.length());
        mPlaylist.addTextChangedListener(mTextWatcher);
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("defaultname", mPlaylist.getText().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private String makePlaylistName() {

        String template = getString(R.string.new_playlist_name_template);
        int num = 1;

        String[] cols = new String[]{MediaStore.Audio.Playlists.NAME};
        ContentResolver resolver = getContentResolver();
        String whereclause = MediaStore.Audio.Playlists.NAME + " != ''";
        Cursor c = resolver.query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols,
                whereclause, null, MediaStore.Audio.Playlists.NAME);

        if (c == null) {
            return null;
        }

        String suggestedname;
        suggestedname = String.format(template, num++);

        // Need to loop until we've made 1 full pass through without finding a
        // match.
        // Looping more than once shouldn't happen very often, but will happen
        // if
        // you have playlists named "New Playlist 1"/10/2/3/4/5/6/7/8/9, where
        // making only one pass would result in "New Playlist 10" being
        // erroneously
        // picked for the new name.
        boolean done = false;
        while (!done) {
            done = true;
            c.moveToFirst();
            while (!c.isAfterLast()) {
                String playlistname = c.getString(0);
                if (playlistname.compareToIgnoreCase(suggestedname) == 0) {
                    suggestedname = String.format(template, num++);
                    done = false;
                }
                c.moveToNext();
            }
        }
        c.close();
        return suggestedname;
    }
}
