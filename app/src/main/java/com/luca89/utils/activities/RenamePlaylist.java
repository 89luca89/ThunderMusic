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
import android.content.ContentValues;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.luca89.thundermusic.R;
import com.luca89.utils.MusicUtils;

import java.io.File;

public class RenamePlaylist extends Activity {
    private static long mRenameId;
    private static String mOriginalName;
    private EditText mPlaylist;
    private View.OnClickListener mOpenClicked = new View.OnClickListener() {
        public void onClick(View v) {
            String name = mPlaylist.getText().toString();
            if (name != null && name.length() > 0) {
                ContentResolver resolver = getContentResolver();
                ContentValues values = new ContentValues(1);
                values.put(MediaStore.Audio.Playlists.NAME, name);
                resolver.update(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        values, MediaStore.Audio.Playlists._ID + "=?",
                        new String[]{Long.valueOf(mRenameId).toString()});

                if (MusicUtils.getPlaylistiD(mOriginalName) == mRenameId) {
                    ContentValues values1 = new ContentValues(1);
                    values1.put(MediaStore.Audio.Playlists._ID,
                            MusicUtils.getPlaylistiD(name));
                    resolver.update(
                            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                            values1, MediaStore.Audio.Playlists._ID + "=?",
                            new String[]{Long.valueOf(mRenameId).toString()});
                    String path = (getBaseContext()
                            .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.online");
                    File oldfile = new File(path, mOriginalName);
                    File newfile = new File(path, name);
                    oldfile.renameTo(newfile);

                }

                setResult(RESULT_OK);
                Toast.makeText(RenamePlaylist.this,
                        R.string.playlist_renamed_message, Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        }
    };
    private TextView mPrompt;
    private Button mSaveButton;
    TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // don't care about this one
        }

        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            // check if playlist with current name exists already, and warn the
            // user if so.
            setSaveButton();
        }

        public void afterTextChanged(Editable s) {
            // don't care about this one
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
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

        mRenameId = icicle != null ? icicle.getLong("rename") : getIntent()
                .getLongExtra("rename", -1);
        mOriginalName = nameForId(mRenameId);
        String defaultname = icicle != null ? icicle.getString("defaultname")
                : mOriginalName;

        if (mRenameId < 0 || mOriginalName == null || defaultname == null) {
            finish();
            return;
        }

        String promptformat;
        if (mOriginalName.equals(defaultname)) {
            promptformat = getString(R.string.rename_playlist_same_prompt);
        } else {
            promptformat = getString(R.string.rename_playlist_diff_prompt);
        }

        String prompt = String.format(promptformat, mOriginalName, defaultname);
        mPrompt.setText(prompt);
        mPlaylist.setText(defaultname);
        mPlaylist.setSelection(defaultname.length());
        mPlaylist.addTextChangedListener(mTextWatcher);
        setSaveButton();
    }

    private void setSaveButton() {
        String typedname = mPlaylist.getText().toString();
        if (typedname.trim().length() == 0) {
            mSaveButton.setEnabled(false);
        } else {
            mSaveButton.setEnabled(true);
            if (idForplaylist(typedname) >= 0
                    && !mOriginalName.equals(typedname)) {
                mSaveButton.setText(R.string.create_playlist_overwrite_text);
            } else {
                mSaveButton.setText(R.string.create_playlist_create_text);
            }
        }

    }

    private int idForplaylist(String name) {
        Cursor c = MusicUtils.query(this,
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Playlists._ID},
                MediaStore.Audio.Playlists.NAME + "=?", new String[]{name},
                MediaStore.Audio.Playlists.NAME);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
        }
        c.close();
        return id;
    }

    private String nameForId(long id) {
        Cursor c = MusicUtils.query(this,
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Playlists.NAME},
                MediaStore.Audio.Playlists._ID + "=?", new String[]{Long
                        .valueOf(id).toString()},
                MediaStore.Audio.Playlists.NAME);
        String name = null;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                name = c.getString(0);
            }
        }
        c.close();
        return name;
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("defaultname", mPlaylist.getText().toString());
        outcicle.putLong("rename", mRenameId);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
