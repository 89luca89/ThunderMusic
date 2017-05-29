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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.R;
import com.luca89.utils.ImageUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.MusicUtils.ServiceToken;
import com.luca89.utils.ThemeUtils;

import thundermusic.IMediaPlaybackService;

@TargetApi(19)
@SuppressLint("DefaultLocale")
public class MediaPlaybackCarModeActivity extends Activity implements
        MusicUtils.Defs, OnSharedPreferenceChangeListener {

    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    private static final int GET_ALBUM_ART = 3;
    private static final int ALBUM_ART_DECODED = 4;
    private static SharedPreferences mSettings;
    private static Toast mToast;
    private static boolean mIntentDeRegistered = false;
    private static boolean shortPress = false;
    private static AudioManager audio;
    private static Activity activity;
    private static ImageView mAlbum;
    private static final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALBUM_ART_DECODED:
                    mAlbum.setImageDrawable((Drawable) msg.obj);
                    break;

                case REFRESH:

                    break;

                case QUIT:
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(R.string.service_start_error_button,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int whichButton) {
                                            activity.finish();
                                        }
                                    }).setCancelable(false).show();
                    break;

                default:
                    break;
            }
        }
    };
    private IMediaPlaybackService mService = null;
    private Button mExitButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private ImageButton mPrevButton;
    private ImageButton mPauseButton;
    private ImageButton mNextButton;
    private Worker mAlbumArtWorker;
    private AlbumArtHandler mAlbumArtHandler;
    private ServiceToken mToken;
    private RelativeLayout mPlayPause;
    private boolean changed = false;
    private View.OnClickListener mShuffleListener = new View.OnClickListener() {
        public void onClick(View v) {
            toggleShuffle();
        }
    };
    private View.OnClickListener mRepeatListener = new View.OnClickListener() {
        public void onClick(View v) {
            cycleRepeat();
        }
    };
    private TextView mTrackName;
    private TextView mAlbumName;
    private TextView mArtistName;
    private ServiceConnection osc = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mService = IMediaPlaybackService.Stub.asInterface(obj);
            startPlayback();
            try {
                // Assume something is playing when the service says it is,
                // but also if the audio ID is valid but the service is paused.
                if (mService.getAudioId() >= 0 || mService.isPlaying()
                        || mService.getPath() != null) {
                    // something is playing now, we're done
                    mRepeatButton.setVisibility(View.VISIBLE);
                    mShuffleButton.setVisibility(View.VISIBLE);
                    setRepeatButtonImage();
                    setShuffleButtonImage();
                    setPauseButtonImage();
                    return;
                }
            } catch (RemoteException ex) {
            }
            // Service is dead or not playing anything. If we got here as part
            // of a "play this file" Intent, exit. Otherwise go to the Music
            // app start screen.
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlaybackService.META_CHANGED)) {
                // redraw the artist/title info and
                // set new max for progress bar
                updateTrackInfo();
                setPauseButtonImage();
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
            } else if (action.equals(MediaPlaybackService.START_BUFFERING)) {
            } else if (action.equals(MediaPlaybackService.END_BUFFERING)) {
                updateTrackInfo();
            }
        }
    };
    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mService == null)
                return;
            try {
                if (mService.position() < 2000) {
                    mService.prev();
                } else {
                    mService.seek(0);
                    mService.play();
                }
            } catch (RemoteException ex) {
            }
        }
    };
    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mService == null)
                return;
            try {
                mService.next();
            } catch (RemoteException ex) {
            }
        }
    };

    public MediaPlaybackCarModeActivity() {
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        activity = this;
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings.registerOnSharedPreferenceChangeListener(this);
        if (mSettings.getBoolean("use_as_lock", false)) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initViews();
    }

    private void initViews() {
        mAlbumArtWorker = new Worker("album art worker");
        mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());
        setContentView(R.layout.carmode);

        mAlbum = (ImageView) findViewById(R.id.album);
        mTrackName = (TextView) findViewById(R.id.trackname);
        mAlbumName = (TextView) findViewById(R.id.albumname);
        mArtistName = (TextView) findViewById(R.id.artistname);
        mPrevButton = (ImageButton) findViewById(R.id.prev);
        mNextButton = (ImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);
        mPrevButton.setOnClickListener(mPrevListener);
        mExitButton = (Button) findViewById(R.id.exit);
        mExitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 
                finish();
            }
        });

        mPlayPause = (RelativeLayout) findViewById(R.id.infos);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPauseResume();
            }
        });

        mShuffleButton = ((ImageButton) findViewById(R.id.shuffle));
        mRepeatButton = ((ImageButton) findViewById(R.id.repeat));
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mRepeatButton.setOnClickListener(mRepeatListener);
        mShuffleButton.setOnClickListener(mShuffleListener);
        mPrevButton.setImageResource(R.drawable.btn_playback_previous);
        mNextButton.setImageResource(R.drawable.btn_playback_next);


        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mTrackName.setSingleLine(false);
        } else {
            mTrackName.setSingleLine(true);
        }

        setRepeatButtonImage();
        setShuffleButtonImage();
        setPauseButtonImage();
    }

    @Override
    public void onStop() {
        if (!mIntentDeRegistered) {
            mHandler.removeMessages(REFRESH);
            unregisterReceiver(mStatusListener);
        }
        MusicUtils.unbindFromService(mToken);
        mService = null;
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();

        mToken = MusicUtils.bindToService(this, osc);
        if (mToken == null) {
            // something went wrong
            mHandler.sendEmptyMessage(QUIT);
        }

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.START_BUFFERING);
        f.addAction(MediaPlaybackService.END_BUFFERING);
        f.addAction(Intent.ACTION_HEADSET_PLUG);
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mStatusListener, new IntentFilter(f));

        IntentFilter s = new IntentFilter();
        s.addAction(Intent.ACTION_SCREEN_ON);
        s.addAction(Intent.ACTION_SCREEN_OFF);
        s.addAction(Intent.ACTION_HEADSET_PLUG);

        updateTrackInfo();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (changed == true) {
            changed = false;
            recreate();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateTrackInfo();
        setPauseButtonImage();
    }

    @Override
    public void onDestroy() {
        mAlbumArtWorker.quit();
        super.onDestroy();
    }

    private void doPauseResume() {
        try {
            if (mService != null) {
                if (mService.isPlaying()) {
                    mService.pause();
                } else {
                    mService.play();
                }

                setPauseButtonImage();
            }
        } catch (RemoteException ex) {
        }
    }

    private void toggleShuffle() {
        if (mService == null) {
            return;
        }
        try {
            int shuffle = mService.getShuffleMode();
            if (shuffle == MediaPlaybackService.SHUFFLE_NONE) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
                if (mService.getRepeatMode() == MediaPlaybackService.REPEAT_CURRENT) {
                    mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                    setRepeatButtonImage();
                }
                showToast(R.string.shuffle_on_notif);
            } else if (shuffle == MediaPlaybackService.SHUFFLE_NORMAL) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                showToast(R.string.shuffle_off_notif);
            }
            setShuffleButtonImage();
        } catch (RemoteException ex) {
        }
    }

    private void cycleRepeat() {
        if (mService == null) {
            return;
        }
        try {
            int mode = mService.getRepeatMode();
            if (mode == MediaPlaybackService.REPEAT_NONE) {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                showToast(R.string.repeat_all_notif);
            } else if (mode == MediaPlaybackService.REPEAT_ALL) {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
                if (mService.getShuffleMode() != MediaPlaybackService.SHUFFLE_NONE) {
                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    setShuffleButtonImage();
                }
                showToast(R.string.repeat_current_notif);
            } else {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_NONE);
                showToast(R.string.repeat_off_notif);
            }
            setRepeatButtonImage();
        } catch (RemoteException ex) {
        }

    }

    private void showToast(int resid) {
        if (mToast == null) {
            mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }
        mToast.setText(resid);
        mToast.show();
    }

    private void startPlayback() {

        if (mService == null)
            return;
        updateTrackInfo();

    }

    public void setRepeatButtonImage() {
        if (mService == null)
            return;
        try {
            switch (mService.getRepeatMode()) {
                case MediaPlaybackService.REPEAT_ALL:

                    mRepeatButton.setImageDrawable(ThemeUtils.colorizeLockDrawable(
                            getResources().getDrawable(
                                    R.drawable.ic_mp_repeat_all_btn), this));
                    break;
                case MediaPlaybackService.REPEAT_CURRENT:

                    mRepeatButton.setImageDrawable(ThemeUtils.colorizeLockDrawable(
                            getResources().getDrawable(
                                    R.drawable.ic_mp_repeat_once_btn), this));
                    break;
                default:
                    mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_off_btn);
                    break;
            }
        } catch (RemoteException ex) {
        }
    }

    private void setShuffleButtonImage() {
        if (mService == null)
            return;
        try {
            switch (mService.getShuffleMode()) {
                case MediaPlaybackService.SHUFFLE_NONE:
                    mShuffleButton
                            .setImageResource(R.drawable.ic_mp_shuffle_off_btn);
                    break;
                default:

                    mShuffleButton
                            .setImageDrawable(ThemeUtils.colorizeLockDrawable(
                                    getResources().getDrawable(
                                            R.drawable.ic_mp_shuffle_on_btn), this));
                    break;
            }
        } catch (RemoteException ex) {
        }
    }

    private void setPauseButtonImage() {
        try {
            if (mService != null && mService.isPlaying()) {
                mPauseButton
                        .setImageResource(android.R.drawable.ic_media_pause);
            } else {
                mPauseButton.setImageResource(android.R.drawable.ic_media_play);
            }
        } catch (RemoteException ex) {
        }
    }

    @Override
    public void onBackPressed() {
    }

    private void updateTrackInfo() {
        if (mService == null) {
            return;
        }
        try {
            String path = mService.getPath();
            if (path == null) {
                return;
            }

            String artistName = mService.getArtistName();
            if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
                artistName = getString(R.string.unknown_artist_name);
            }
            String albumName = mService.getAlbumName();
            long albumid = mService.getAlbumId();
            if (MediaStore.UNKNOWN_STRING.equals(albumName)) {
                albumName = getString(R.string.unknown_album_name);
                albumid = -1;
            }
            mArtistName.setText(artistName);
            mAlbumName.setText(albumName);
            mTrackName.setText(mService.getTrackName());
            mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
            mAlbumArtHandler.obtainMessage(GET_ALBUM_ART,
                    new AlbumSongIdWrapper(albumid)).sendToTarget();
            mAlbum.setVisibility(View.VISIBLE);

        } catch (RemoteException ex) {
        }
    }

    /**
     * Here and in the other key management methods
     * we handle the Volume rocker longpress
     * <p/>
     * Longpress vol+ for skip
     * Longpress vol- for rewind
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            shortPress = false;

            try {
                if (mService.position() < 2000) {
                    mService.prev();
                } else {
                    mService.seek(0);
                    mService.play();
                }
            } catch (RemoteException e) {
                // 
                e.printStackTrace();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            shortPress = false;
            try {
                mService.next();
            } catch (RemoteException e) {
                // 
                e.printStackTrace();
            }

            return true;
        }
        // Just return false because the super call does always the same
        // (returning false)
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                event.startTracking();
                if (event.getRepeatCount() == 0) {
                    shortPress = true;
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (shortPress) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_VIBRATE);
            } else {
                // Don't handle longpress here, because the user will have
                // to
                // get his finger back up first
            }
            shortPress = false;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (shortPress) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_VIBRATE);
            } else {
                // Don't handle longpress here, because the user will have
                // to
                // get his finger back up first
            }
            shortPress = false;
            return true;

        }
        return super.onKeyUp(keyCode, event);
    }

    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // 
        if (key.equals(PreferencesActivity.KEY_TITLE)
                || key.equals(PreferencesActivity.KEY_START_SCREEN)
                || key.equals(PreferencesActivity.GRID_MODE))
            changed = true;
    }

    private static class AlbumSongIdWrapper {
        public long albumid;

        AlbumSongIdWrapper(long aid) {
            albumid = aid;
        }
    }

    private static class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;

        /**
         * Creates a worker thread with the given name. The thread then runs a
         * {@link Looper}.
         *
         * @param name A name for the new thread
         */
        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        public Looper getLooper() {
            return mLooper;
        }

        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }

        public void quit() {
            mLooper.quit();
        }
    }

    public class AlbumArtHandler extends Handler {
        private long mAlbumId = -1;

        public AlbumArtHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            long albumid = ((AlbumSongIdWrapper) msg.obj).albumid;
            if (msg.what == GET_ALBUM_ART
                    && (mAlbumId != albumid || albumid < 0)) {
                // while decoding the new image, show the default album art
                Message numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, null);
                mHandler.removeMessages(ALBUM_ART_DECODED);
                mHandler.sendMessageDelayed(numsg, 300);
                // Don't allow default artwork here, because we want to fall
                // back to song-specific
                // album art if we can't find anything for the album.
                Drawable bm = ImageUtils.getArtwork(
                        MediaPlaybackCarModeActivity.this, albumid,
                        false);
                if (bm == null) {
                    bm = ImageUtils.getArtwork(
                            MediaPlaybackCarModeActivity.this, -1);
                    albumid = -1;
                }
                if (bm != null) {
                    numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, bm);
                    mHandler.removeMessages(ALBUM_ART_DECODED);
                    mHandler.sendMessage(numsg);
                }
                mAlbumId = albumid;
            }
        }
    }
}
