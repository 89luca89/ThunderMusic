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
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.luca89.thundermusic.R;
import com.luca89.utils.MusicUtils;

import java.io.File;
import java.io.IOException;

public class BackupRestore extends Activity {
    private TextView mPrompt;
    private Button mCancel;
    private Button mBackup;
    private Button mRestore;
    private Context mContext;
    private View.OnClickListener mBackupButtonClicked = new View.OnClickListener() {
        public void onClick(View v) {
            // delete the selected item(s)
            String og_path = (mContext
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music");
            String path = (Environment.getExternalStorageDirectory() + "/Thunder_Music_Backup");

            File og_pathFile = new File(og_path);
            File pathFile = new File(path);

            try {
                MusicUtils.copyDirectory(og_pathFile, pathFile);
            } catch (IOException e) {
                // 
                e.printStackTrace();
            }

            File file = new File(path + "/.online/mixradio");
            file.delete();
            file = new File(path + "/.online/queue");
            file.delete();
            file = new File(path + "/.online/search");
            file.delete();

            Toast.makeText(mContext, R.string.backup_done, Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
    };
    private View.OnClickListener mRestoreButtonClicked = new View.OnClickListener() {
        public void onClick(View v) {
            // delete the selected item(s)
            String path = (mContext
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music");
            String og_path = (Environment.getExternalStorageDirectory() + "/Thunder_Music_Backup");

            File og_pathFile = new File(og_path);

            if (!og_pathFile.exists()) {
                Toast.makeText(mContext, R.string.backup_notfound, Toast.LENGTH_SHORT)
                        .show();
                finish();
            } else {

                String playlist_path = (Environment.getExternalStorageDirectory() + "/Thunder_Music_Backup/.online");

                File playlist_pathFile = new File(playlist_path);
                File pathFile = new File(path);

                try {
                    MusicUtils.copyDirectory(og_pathFile, pathFile);
                } catch (IOException e) {
                    // 
                    e.printStackTrace();
                }

                File[] listOfFiles = playlist_pathFile.listFiles();
                ContentResolver resolver = mContext.getContentResolver();
                ContentValues values2 = new ContentValues(listOfFiles.length);
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile()) {
                        String name = listOfFiles[i].getName();
                        values2.put(MediaStore.Audio.Playlists.NAME, name);
                        values2.put(MediaStore.Audio.Playlists._ID,
                                MusicUtils.getPlaylistiD(name));
                        resolver.insert(
                                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                values2);
                    }
                }

                Toast.makeText(mContext, R.string.backup_done, Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.backup_restore);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        mContext = this;
        mPrompt = (TextView) findViewById(R.id.prompt);
        mBackup = (Button) findViewById(R.id.backup);
        mCancel = (Button) findViewById(R.id.cancel);
        mRestore = (Button) findViewById(R.id.restore);

        mBackup.setOnClickListener(mBackupButtonClicked);
        mRestore.setOnClickListener(mRestoreButtonClicked);

        mPrompt.setText(R.string.backup_files_text);

        mCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }
}
