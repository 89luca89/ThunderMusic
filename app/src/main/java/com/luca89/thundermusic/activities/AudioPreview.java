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
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.luca89.thundermusic.R;
import com.luca89.utils.ThemeUtils;

import java.io.IOException;

/**
 * Dialog that comes up in response to various music-related VIEW intents.
 */
public class AudioPreview extends Activity implements OnPreparedListener, OnErrorListener, OnCompletionListener {
    private static final int OPEN_IN_MUSIC = 1;
    private static boolean mSeeking = false;
    private static int mDuration;
    private static Uri mUri;
    private static long mMediaId = -1;
    private static boolean mPausedByTransientLossOfFocus;
    private PreviewPlayer mPlayer;
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mSeeking = true;
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            // Protection for case of simultaneously tapping on seek bar and exit	336
            if (mPlayer == null) {
                return;
            }
            mPlayer.seekTo(progress);
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mSeeking = false;
        }
    };
    private TextView mTextLine1;
    private TextView mTextLine2;
    private TextView mLoadingText;
    private SeekBar mSeekBar;
    private Handler mProgressRefresher;
    private AudioManager mAudioManager;
    private ImageButton mPlayPause;
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (mPlayer == null) {
                // this activity has handed its MediaPlayer off to the next activity
                // (e.g. portrait/landscape switch) and should abandon its focus
                mAudioManager.abandonAudioFocus(this);
                return;
            }
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    mPausedByTransientLossOfFocus = false;
                    mPlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (mPlayer.isPlaying()) {
                        mPausedByTransientLossOfFocus = true;
                        mPlayer.pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mPausedByTransientLossOfFocus) {
                        mPausedByTransientLossOfFocus = false;
                        start();
                    }
                    break;
            }
            updatePlayPause();
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        mUri = intent.getData();
        if (mUri == null) {
            finish();
            return;
        }
        String scheme = mUri.getScheme();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.audiopreview);

        mTextLine1 = (TextView) findViewById(R.id.line1);
        mTextLine2 = (TextView) findViewById(R.id.line2);
        mLoadingText = (TextView) findViewById(R.id.loading);
        if (scheme.equals("http")) {
            String msg = getString(R.string.streamloadingtext, mUri.getHost());
            mLoadingText.setText(msg);
        } else {
            mLoadingText.setVisibility(View.GONE);
        }
        mSeekBar = (SeekBar) findViewById(R.id.progress);
        mProgressRefresher = new Handler();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mPlayPause = (ImageButton) findViewById(R.id.playpause);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                } else {
                    mPlayer.start();
                }
                updatePlayPause();
            }
        });
        PreviewPlayer player = (PreviewPlayer) getLastNonConfigurationInstance();
        if (player == null) {
            mPlayer = new PreviewPlayer();
            mPlayer.setActivity(this);
            try {
                mPlayer.setDataSourceAndPrepare(mUri);
            } catch (Exception ex) {
                // catch generic Exception, since we may be called with a media
                // content URI, another content provider's URI, a file URI,
                // an http URI, and there are different exceptions associated
                // with failure to open each of those.
                Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            mPlayer = player;
            mPlayer.setActivity(this);
            if (mPlayer.isPrepared()) {
                showPostPrepareUI();
            }
        }

        AsyncQueryHandler mAsyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()) {

                    int titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int displaynameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (idIdx >= 0) {
                        mMediaId = cursor.getLong(idIdx);
                    }

                    if (titleIdx >= 0) {
                        String title = cursor.getString(titleIdx);
                        mTextLine1.setText(title);
                        if (artistIdx >= 0) {
                            String artist = cursor.getString(artistIdx);
                            mTextLine2.setText(artist);
                        }
                    } else if (displaynameIdx >= 0) {
                        String name = cursor.getString(displaynameIdx);
                        mTextLine1.setText(name);
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
                setNames();
            }
        };

        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            if (mUri.getAuthority() == MediaStore.AUTHORITY) {
                // try to get title and artist from the media content provider
                mAsyncQueryHandler.startQuery(0, null, mUri, new String[]{
                                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                        null, null, null);
            } else {
                // Try to get the display name from another content provider.
                // Don't specifically ask for the display name though, since the
                // provider might not actually support that column.
                mAsyncQueryHandler.startQuery(0, null, mUri, null, null, null, null);
            }
        } else if (scheme.equals("file")) {
            // check if this file is in the media database (clicking on a download
            // in the download manager might follow this path
            String path = mUri.getPath();
            mAsyncQueryHandler.startQuery(0, null, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.DATA + "=?", new String[]{path}, null);
        } else {
            // We can't get metadata from the file/stream itself yet, because
            // that API is hidden, so instead we display the URI being played
            if (mPlayer.isPrepared()) {
                setNames();
            }
        }
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        mSeekBar.setThumb(ThemeUtils.colorizeDrawable(getResources().getDrawable(R.drawable.thumb_seek), this));
        mSeekBar.setProgressDrawable(ThemeUtils.colorizeDrawable(
                mSeekBar.getProgressDrawable(), this));
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        PreviewPlayer player = mPlayer;
        mPlayer = null;
        return player;
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        super.onDestroy();
    }

    private void stopPlayback() {
        if (mProgressRefresher != null) {
            mProgressRefresher.removeCallbacksAndMessages(null);
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }
    }

    @Override
    public void onUserLeaveHint() {
        stopPlayback();
        finish();
        super.onUserLeaveHint();
    }

    public void onPrepared(MediaPlayer mp) {
        if (isFinishing()) return;
        mPlayer = (PreviewPlayer) mp;
        setNames();
        mPlayer.start();
        showPostPrepareUI();
    }

    private void showPostPrepareUI() {
        ProgressBar pb = (ProgressBar) findViewById(R.id.spinner);
        pb.setVisibility(View.GONE);
        mDuration = mPlayer.getDuration();
        if (mDuration != 0) {
            mSeekBar.setMax(mDuration);
            mSeekBar.setVisibility(View.VISIBLE);
        }
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        mLoadingText.setVisibility(View.GONE);
        View v = findViewById(R.id.titleandbuttons);
        v.setVisibility(View.VISIBLE);
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
        updatePlayPause();
    }

    private void start() {
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mPlayer.start();
        mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
    }

    public void setNames() {
        if (TextUtils.isEmpty(mTextLine1.getText())) {
            mTextLine1.setText(mUri.getLastPathSegment());
        }
        if (TextUtils.isEmpty(mTextLine2.getText())) {
            mTextLine2.setVisibility(View.GONE);
        } else {
            mTextLine2.setVisibility(View.VISIBLE);
        }
    }

    private void updatePlayPause() {
        if (mPlayPause != null) {
            if (mPlayer.isPlaying()) {
                mPlayPause.setImageResource(R.drawable.btn_playback_pause_black);
            } else {
                mPlayPause.setImageResource(R.drawable.btn_playback_play_black);
                mProgressRefresher.removeCallbacksAndMessages(null);
            }
        }
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
        finish();
        return true;
    }

    public void onCompletion(MediaPlayer mp) {
        mSeekBar.setProgress(mDuration);
        updatePlayPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, OPEN_IN_MUSIC, 0, "open in music");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(OPEN_IN_MUSIC);
        if (mMediaId >= 0) {
            item.setVisible(true);
            return true;
        }
        item.setVisible(false);
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                } else {
                    start();
                }
                updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                start();
                updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                }
                updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_BACK:
                stopPlayback();
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /*
     * Wrapper class to help with handing off the MediaPlayer to the next instance
     * of the activity in case of orientation change, without losing any state.
     */
    private static class PreviewPlayer extends MediaPlayer implements OnPreparedListener {
        AudioPreview mActivity;
        boolean mIsPrepared = false;

        public void setActivity(AudioPreview activity) {
            mActivity = activity;
            setOnPreparedListener(this);
            setOnErrorListener(mActivity);
            setOnCompletionListener(mActivity);
        }

        public void setDataSourceAndPrepare(Uri uri) throws IllegalArgumentException,
                SecurityException, IllegalStateException, IOException {
            setDataSource(mActivity, uri);
            prepareAsync();
        }

        /* (non-Javadoc)
         * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
         */
        @Override
        public void onPrepared(MediaPlayer mp) {
            mIsPrepared = true;
            mActivity.onPrepared(mp);
        }

        boolean isPrepared() {
            return mIsPrepared;
        }
    }

    class ProgressRefresher implements Runnable {

        public void run() {
            if (mPlayer != null && !mSeeking && mDuration != 0) {
                mSeekBar.setProgress(mPlayer.getCurrentPosition());
            }
            mProgressRefresher.removeCallbacksAndMessages(null);
            mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
        }
    }

}
