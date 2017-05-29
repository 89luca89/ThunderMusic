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

package com.luca89.thundermusic.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.luca89.service.search.LyricSearchTask;
import com.luca89.thundermusic.R;
import com.luca89.utils.MusicUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

public class LyricsActivity extends Activity {

    public static Context mContext;
    private static String mArtist;
    private static String mTrack;
    private static SharedPreferences mSettings;
    public ProgressBar mProgress;
    private String mText;
    private TextView mLyrics;
    private TextView mNotFound;

    public LyricsActivity() {
    }

    private static void writeToFile(String data, Context context, String song) {
        try {
            data = data.replace("\n", ";");
            String path = (context
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.lyrics");
            File pathFile = new File(path);
            pathFile.mkdirs();
            FileWriter fOut;
            OutputStream fOut2;
            File nomedia = new File(path, ".nomedia");
            fOut2 = new FileOutputStream(nomedia);
            File file = new File(path, song);
            fOut = new FileWriter(file);
            fOut.write(data);

            fOut.flush();
            fOut.close();
            fOut2.flush();
            fOut2.close();
        } catch (IOException e) {
        }
    }

    private static String readFromFile(Context context, String song) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        BufferedReader in;
        String path = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.lyrics");
        try {
            in = new BufferedReader(new FileReader(new File(path, song)));
            while ((line = in.readLine()) != null)
                stringBuilder.append(line);
            in.close();

        } catch (Exception e) {
            return null;
        }
        String ret = stringBuilder.toString();
        ret = ret.replace(";", "\n");
        return ret;
    }

    public static void deleteFile(Context context, String song) {
        String path = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.lyrics");
        File file = new File(path, song);
        if (file.exists()) {
            file.delete();
        }

    }

    private static String getSong() {
        return mTrack;
    }

    private static String getSongArtist() {
        return mArtist;
    }

    private static Context getContext() {
        return mContext;
    }

    /**
     * Called when the activity is first created or resumed.
     */
    @Override
    public void onCreate(Bundle icicle) {

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = getIntent();
        if (icicle != null) {
            mTrack = icicle.getString("track");
            mArtist = icicle.getString("artist");
            mText = icicle.getString("lyric");
        } else {
            mTrack = intent.getStringExtra("track");
            mArtist = intent.getStringExtra("artist");
            mText = getIntent().getStringExtra("lyric");
        }
        super.onCreate(icicle);

        setContentView(R.layout.lyrics_layout);
        mContext = this.getBaseContext();
        mLyrics = (TextView) findViewById(R.id.audio_player_lyrics);
        mNotFound = (TextView) findViewById(R.id.audio_player_lyrics2);
        mProgress = (ProgressBar) findViewById(R.id.progressBar1);
        if (mText == null) {
            String text = null;
            if (mSettings.getBoolean(PreferencesActivity.SAVE_LYRICS, true))
                text = readFromFile(mContext, getSong());
            if (text != null) {
                mLyrics.setText(text);
            } else if (isNetworkAvailable()) {
                MusicUtils.execute(false, new FetchLyrics(), false);
            } else {
                mLyrics.setVisibility(View.GONE);
                mNotFound.setVisibility(View.VISIBLE);
                mNotFound.setText(R.string.no_internet_available);
            }

        } else {
            mProgress.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(mText)) {
                // Set the lyrics
                mLyrics.setText(mText);
                // Save the lyrics
                mLyrics.setVisibility(View.VISIBLE);
                mNotFound.setVisibility(View.GONE);
                setTitle(getSong());
            } else {
                mLyrics.setVisibility(View.GONE);
                mNotFound.setVisibility(View.VISIBLE);
                mNotFound.setText(getString(R.string.no_result_lyrics) + "\n"
                        + getSong() + "\n"
                        + getString(R.string.searching_lyrics2) + "\n"
                        + getSongArtist());
            }
        }
    }

    /**
     * Called when the activity going into the background or being destroyed.
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("track", getSong());
        outcicle.putString("artist", getSongArtist());
        outcicle.putString("lyric", mText);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private final class FetchLyrics extends AsyncTask<Boolean, Void, String> {

        private final String mArt;
        private final Context mContext;
        private final String mSong;

        /**
         * Constructor of <code>FetchLyrics</code>
         */
        public FetchLyrics() {
            mArt = LyricsActivity.getSongArtist();
            mSong = LyricsActivity.getSong();
            mContext = LyricsActivity.getContext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPreExecute() {
            // Release the lyrics on track changes
            mLyrics.setText(null);
            mProgress.setVisibility(View.VISIBLE);
            mNotFound.setText(getString(R.string.searching_lyrics) + "\n"
                    + getSong() + "\n" + getString(R.string.searching_lyrics2)
                    + "\n" + getSongArtist());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String doInBackground(final Boolean... force) {
            String lyrics = LyricSearchTask.getLyrics(mArt, mSong);
            mText = lyrics;
            if (!TextUtils.isEmpty(lyrics))
                if (mSettings.getBoolean(PreferencesActivity.SAVE_LYRICS, true))
                    writeToFile(lyrics, mContext, mSong);
            return lyrics;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(final String result) {
            if (!isCancelled()) {
                mProgress.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(result)) {
                    // Set the lyrics
                    mLyrics.setText(result);
                    // Save the lyrics
                    mLyrics.setVisibility(View.VISIBLE);
                    mNotFound.setVisibility(View.GONE);
                    setTitle(getSong());
                } else {
                    mLyrics.setVisibility(View.GONE);
                    mNotFound.setVisibility(View.VISIBLE);
                    mNotFound.setText(getString(R.string.no_result_lyrics)
                            + "\n" + getSong() + "\n"
                            + getString(R.string.searching_lyrics2) + "\n"
                            + getSongArtist());
                    LyricsActivity.deleteFile(mContext, mSong);
                }
            }
        }
    }

}
