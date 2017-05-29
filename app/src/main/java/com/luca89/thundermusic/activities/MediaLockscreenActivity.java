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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.OnlineActivity;
import com.luca89.thundermusic.R;
import com.luca89.utils.ImageUtils;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.MusicUtils.ServiceToken;
import com.luca89.utils.ThemeUtils;
import com.melnykov.fab.FloatingActionButton;

import thundermusic.IMediaPlaybackService;

@TargetApi(19)
@SuppressLint("DefaultLocale")
public class MediaLockscreenActivity extends Activity implements
        SensorEventListener, MusicUtils.Defs, OnSharedPreferenceChangeListener, GestureDetector.OnGestureListener {

    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    private static final int GET_ALBUM_ART = 3;
    private static final int ALBUM_ART_DECODED = 4;
    private static SharedPreferences mSettings;
    private static IMediaPlaybackService mService = null;
    private static Toast mToast;
    private static boolean mIntentDeRegistered = false;
    private static boolean shortPress = false;
    private static AudioManager audio;
    private static SensorManager mSensorManager;
    private static Sensor mProximity;
    private static float previousScreenBrightness;
    private static Activity activity;
    private static ImageView mAlbum;
    private static TextView mCurrentTime;
    private static TextView mTotalTime;
    private static TextView mTrackName;
    private static TextView mAlbumName;
    private static ProgressBar mProgress;
    private static long mPosOverride = -1;
    private static boolean mFromTouch = false;
    private static long mDuration;
    private static boolean paused;
    private static final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALBUM_ART_DECODED:
                    mAlbum.setImageDrawable((Drawable) msg.obj);
                    break;

                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;

                case QUIT:
                    // This can be moved back to onCreate once the bug that prevents
                    // Dialogs from being started from onCreate/onResume is fixed.
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
    private static float mDistanceY;
    GestureDetector mGestureScanner;
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
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mFromTouch = true;
        }

        public void onProgressChanged(SeekBar bar, int progress,
                                      boolean fromuser) {
            if (!fromuser || (mService == null))
                return;
            mPosOverride = mDuration * progress / 1000;
            try {
                mService.seek(mPosOverride);
            } catch (RemoteException ex) {
            }

            // trackball event, allow progress updates
            refreshNow();
            if (!mFromTouch) {
                refreshNow();
                mPosOverride = -1;
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mPosOverride = -1;
            mFromTouch = false;
        }
    };
    private ImageButton mPrevButton;
    private FloatingActionButton mPauseButton;
    private ImageButton mNextButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private ImageButton mCamera;
    private Worker mAlbumArtWorker;
    private AlbumArtHandler mAlbumArtHandler;
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
            if (getIntent().getData() == null) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(MediaLockscreenActivity.this,
                        OnlineActivity.class);
                startActivity(intent);
            }
            finish();
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };
    private ServiceToken mToken;
    private RelativeLayout mMainWindows;
    private boolean changed = false;
    private View.OnClickListener mShuffleListener = new View.OnClickListener() {
        public void onClick(View v) {
            toggleShuffle();
        }
    };
    private View.OnLongClickListener mCameraLongListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View arg0) {
            // 

            if (mSettings.getBoolean("use_as_lock_main", false)) {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            }
            Intent i = new Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE);
            PackageManager pm = getPackageManager();

            final ResolveInfo mInfo = pm.resolveActivity(i, 0);

            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    mInfo.activityInfo.packageName, mInfo.activityInfo.name));
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            startActivity(intent);
            return true;
        }
    };
    private View.OnClickListener mCameraListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            // 
            showToast(R.string.camera_lockscreen);
        }
    };
    private View.OnClickListener mRepeatListener = new View.OnClickListener() {
        public void onClick(View v) {
            cycleRepeat();
        }
    };
    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mProgress.setSecondaryProgress(0);
            if (action.equals(MediaPlaybackService.META_CHANGED)) {
                // redraw the artist/title info and
                // set new max for progress bar
                updateTrackInfo();
                setPauseButtonImage();
                queueNextRefresh(1);
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
            } else if (action.equals(MediaPlaybackService.UPDATE_BUFFERING)) {
                try {
                    mProgress.setSecondaryProgress(mService.getBufferState());
                } catch (RemoteException e) {
                    // 
                    e.printStackTrace();
                }
            } else if (action.equals(MediaPlaybackService.START_BUFFERING)) {
                mPauseButton.setEnabled(false);
                mPauseButton.setClickable(false);
                mNextButton.setEnabled(false);
                mNextButton.setClickable(false);
                mPrevButton.setEnabled(false);
                mPrevButton.setClickable(false);
                mProgress.setEnabled(false);
                mProgress.setClickable(false);
            } else if (action.equals(MediaPlaybackService.END_BUFFERING)) {
                mPauseButton.setEnabled(true);
                mPauseButton.setClickable(true);
                mNextButton.setEnabled(true);
                mNextButton.setClickable(true);
                mPrevButton.setEnabled(true);
                mPrevButton.setClickable(true);
                mProgress.setEnabled(true);
                mProgress.setClickable(true);
                updateTrackInfo();
            }
        }
    };

    public MediaLockscreenActivity() {
    }

    private static void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private static long refreshNow() {
        if (mService == null)
            return 500;
        try {
            long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(MusicUtils.makeTimeString(activity,
                        pos / 1000));
                int progress = (int) (1000 * pos / mDuration);
                mProgress.setProgress(progress);

                if (mService.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    mCurrentTime
                            .setVisibility(vis == View.INVISIBLE ? View.VISIBLE
                                    : View.INVISIBLE);
                    return 500;
                }
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            long remaining = 1000 - (pos % 1000);

            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mProgress.getWidth();
            if (width == 0)
                width = 320;
            long smoothrefreshtime = mDuration / width;

            if (smoothrefreshtime > remaining)
                return remaining;
            if (smoothrefreshtime < 20)
                return 20;
            return smoothrefreshtime;
        } catch (RemoteException ex) {
        }
        return 500;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        activity = this;
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        previousScreenBrightness = lp.screenBrightness;

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

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PreferencesActivity.KEY_MUSMART_LOCK, true)) {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        initViews();
    }

    private void initViews() {
        if (InterfaceUtils.getTabletMode(this) == false)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mAlbumArtWorker = new Worker("album art worker");
        mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());
        setContentView(R.layout.lockscreen);


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            final View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        mAlbum = (ImageView) findViewById(R.id.album);
        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mTotalTime = (TextView) findViewById(R.id.totaltime);
        mProgress = (ProgressBar) findViewById(android.R.id.progress);
        mTrackName = (TextView) findViewById(R.id.trackname);
        mAlbumName = (TextView) findViewById(R.id.albumname);

        mCurrentTime.setTextColor(getResources().getColor(R.color.dark_text_color));
        mTotalTime.setTextColor(getResources().getColor(R.color.dark_text_color));
        mTrackName.setTextColor(getResources().getColor(R.color.dark_text_color));
        mAlbumName.setTextColor(getResources().getColor(R.color.dark_text_color));

        mPrevButton = (ImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(mPrevListener);
        mPauseButton = (FloatingActionButton) findViewById(R.id.FAB);
        InterfaceUtils.setUpFAB(this,
                R.drawable.music_playlist_holo_dark, mPauseButton, mPauseListener);

        mNextButton = (ImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);

        mPrevButton.setImageResource(R.drawable.btn_playback_previous_black);
        mNextButton.setImageResource(R.drawable.btn_playback_next_black);

        mShuffleButton = ((ImageButton) findViewById(R.id.shuffle));
        mShuffleButton.setOnClickListener(mShuffleListener);
        mRepeatButton = ((ImageButton) findViewById(R.id.repeat));
        mRepeatButton.setOnClickListener(mRepeatListener);
        mCamera = ((ImageButton) findViewById(R.id.camera));
        mCamera.setOnClickListener(mCameraListener);
        mCamera.setOnLongClickListener(mCameraLongListener);

        mMainWindows = (RelativeLayout) findViewById(R.id.window);
        if (mProgress instanceof SeekBar) {
            SeekBar seeker = (SeekBar) mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
            seeker.setThumb(ThemeUtils.colorizeDrawable(getResources().getDrawable(R.drawable.thumb_seek), this));
            seeker.setProgressDrawable(ThemeUtils.colorizeDrawable(
                    seeker.getProgressDrawable(), this));
        }
        mGestureScanner = new GestureDetector(this);
        mProgress.setMax(1000);
        setRepeatButtonImage();
        setShuffleButtonImage();
        setPauseButtonImage();
        if (MusicUtils.getBooleanPref(this, "radiomode", false)) {
            mProgress.setVisibility(View.INVISIBLE);
            mCurrentTime.setVisibility(View.INVISIBLE);
            mTotalTime.setVisibility(View.INVISIBLE);
            mRepeatButton.setVisibility(View.INVISIBLE);
            mShuffleButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onStop() {
        paused = true;
        if (!mIntentDeRegistered) {
            mHandler.removeMessages(REFRESH);
            unregisterReceiver(mStatusListener);
        }
        MusicUtils.unbindFromService(mToken);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PreferencesActivity.KEY_MUSMART_LOCK, true)) {
            mSensorManager.unregisterListener(this);
        }
        mService = null;
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        paused = false;

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
        f.addAction(MediaPlaybackService.UPDATE_BUFFERING);
        f.addAction(Intent.ACTION_HEADSET_PLUG);
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mStatusListener, new IntentFilter(f));

        IntentFilter s = new IntentFilter();
        s.addAction(Intent.ACTION_SCREEN_ON);
        s.addAction(Intent.ACTION_SCREEN_OFF);
        s.addAction(Intent.ACTION_HEADSET_PLUG);

        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (changed == true) {
            changed = false;
            recreate();
        }
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PreferencesActivity.KEY_MUSMART_LOCK, true)) {

            mSensorManager.registerListener(this, mProximity,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        updateTrackInfo();
        if (mSettings.getBoolean("use_as_lock", false)) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }

        if (mIntentDeRegistered) {
            paused = false;
        }
        setPauseButtonImage();
    }

    @Override
    public void onDestroy() {
        mAlbumArtWorker.quit();
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PreferencesActivity.KEY_MUSMART_LOCK, true)) {

            mSensorManager.unregisterListener(this);
        }
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
                refreshNow();
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
        long next = refreshNow();
        queueNextRefresh(next);
    }

    public void setRepeatButtonImage() {
        if (mService == null)
            return;
        try {
            switch (mService.getRepeatMode()) {
                case MediaPlaybackService.REPEAT_ALL:

                    mRepeatButton.setImageDrawable(ThemeUtils.colorizeDrawable(
                            getResources().getDrawable(
                                    R.drawable.ic_mp_repeat_all_btn), this));
                    break;
                case MediaPlaybackService.REPEAT_CURRENT:

                    mRepeatButton.setImageDrawable(ThemeUtils.colorizeDrawable(
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
                            .setImageDrawable(ThemeUtils.colorizeDrawable(
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
                mPauseButton.setImageDrawable(ThemeUtils.colorizeLockDrawable(getResources().getDrawable(R.drawable.btn_playback_pause), this));
            } else {
                mPauseButton.setImageDrawable(ThemeUtils.colorizeLockDrawable(getResources().getDrawable(R.drawable.btn_playback_play), this));
            }
        } catch (RemoteException ex) {
        }
    }

    private void updateTrackInfo() {
        if (mService == null) {
            return;
        }
        try {
            String path = mService.getPath();
            if (path == null) {
                finish();
                return;
            }

            long songid = mService.getAudioId();
            if (songid < 0 && path.toLowerCase().startsWith("http://")) {
                // Once we can get album art and meta data from MediaPlayer, we
                // can show that info again when streaming.
                mAlbum.setVisibility(View.INVISIBLE);
                mAlbumName.setVisibility(View.INVISIBLE);
                mTrackName.setText(" " + path);
                mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
                mAlbumArtHandler.obtainMessage(GET_ALBUM_ART,
                        new AlbumSongIdWrapper(-1)).sendToTarget();
            } else {
                mAlbumName.setVisibility(View.VISIBLE);
                String albumName = mService.getAlbumName();
                long albumid = mService.getAlbumId();
                if (MediaStore.UNKNOWN_STRING.equals(albumName)) {
                    albumName = getString(R.string.unknown_album_name);
                    albumid = -1;
                }
                mAlbumName.setText(albumName);
                mTrackName.setText(" " + mService.getTrackName());
                mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
                mAlbumArtHandler.obtainMessage(GET_ALBUM_ART,
                        new AlbumSongIdWrapper(albumid)).sendToTarget();
                mAlbum.setVisibility(View.VISIBLE);
            }
            mDuration = mService.duration();
            mTotalTime.setText(MusicUtils
                    .makeTimeString(this, mDuration / 1000));
        } catch (RemoteException ex) {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
    }
//
//    RIFARE ANCHE LA LOCK TABLET
//    RIVEDERE INOLTRE IL NOWPLAYING TABLET E NEL CASO ANCHE SMARTPHONE
//            YOUTUBE E DAILYMOTION RELATED???

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {

        if (mSettings.getBoolean("longpress_to_skip", false)) {
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
        }
        // Just return false because the super call does always the same
        // (returning false)
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (mSettings.getBoolean("longpress_to_skip", false)) {
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
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (mSettings.getBoolean("longpress_to_skip", false)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (shortPress) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI);
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
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI);
                } else {
                    // Don't handle longpress here, because the user will have
                    // to
                    // get his finger back up first
                }
                shortPress = false;
                return true;

            }
        }
        return super.onKeyUp(keyCode, event);
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

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // 

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // 
        float distance = event.values[0];
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (distance < mProximity.getMaximumRange()) {
            lp.screenBrightness = 0f;
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            getWindow().setAttributes(lp);
            setContentView(R.layout.lockscreen_black);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LOW_PROFILE);
            } else {
                final View decorView = getWindow().getDecorView();
                decorView
                        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LOW_PROFILE);
            } else {
                final View decorView = getWindow().getDecorView();
                decorView
                        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            initViews();
            updateTrackInfo();
            setRepeatButtonImage();
            setShuffleButtonImage();
            lp.screenBrightness = previousScreenBrightness;
            getWindow().setAttributes(lp);

        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mMainWindows
                .getLayoutParams();

        if (distanceY > 0) {
            mDistanceY = distanceY;
            if (mSettings.getBoolean("use_as_lock_main", false)) {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            }
            layoutParams.topMargin = (int) (layoutParams.topMargin - distanceY);
            mMainWindows.setLayoutParams(layoutParams);
        } else {
            snapBack();
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (velocityY < -4000) {
            if (mDistanceY > 15) {
                Unlock();
            } else {
                snapBack();
            }
        } else {
            snapBack();
        }
        return false;
    }

    private void snapBack() {
        if (mMainWindows.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            final RelativeLayout.LayoutParams marginLayoutParams = (RelativeLayout.LayoutParams) mMainWindows.getLayoutParams();

            final int startValueX = marginLayoutParams.leftMargin;
            final int startValueY = marginLayoutParams.topMargin;
            final int endValueX = 0;
            final int endValueY = 0;

            mMainWindows.clearAnimation();

            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    int leftMarginInterpolatedValue = (int) (startValueX + (endValueX - startValueX) * interpolatedTime);
                    marginLayoutParams.leftMargin = leftMarginInterpolatedValue;

                    int topMarginInterpolatedValue = (int) (startValueY + (endValueY - startValueY) * interpolatedTime);
                    marginLayoutParams.topMargin = topMarginInterpolatedValue;

                    mMainWindows.requestLayout();
                }
            };
            animation.setDuration(400);
            animation.setInterpolator(new DecelerateInterpolator());
            mMainWindows.startAnimation(animation);
        }
    }

    private void Unlock() {
        if (mMainWindows.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            final RelativeLayout.LayoutParams marginLayoutParams = (RelativeLayout.LayoutParams) mMainWindows.getLayoutParams();

            final int startValueX = marginLayoutParams.leftMargin;
            final int startValueY = marginLayoutParams.topMargin;
            final int endValueX = 0;
            final int endValueY = -6000;

            mMainWindows.clearAnimation();

            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    int leftMarginInterpolatedValue = (int) (startValueX + (endValueX - startValueX) * interpolatedTime);
                    marginLayoutParams.leftMargin = leftMarginInterpolatedValue;

                    int topMarginInterpolatedValue = (int) (startValueY + (endValueY - startValueY) * interpolatedTime);
                    marginLayoutParams.topMargin = topMarginInterpolatedValue;

                    mMainWindows.requestLayout();
                }
            };
            animation.setDuration(300);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    activity.finish();
                    activity.overridePendingTransition(0, 0);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mMainWindows.startAnimation(animation);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 
        return mGestureScanner.onTouchEvent(event);
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
                Drawable bm = ImageUtils.getArtwork(MediaLockscreenActivity.this, albumid, false);
                if (bm == null) {
                    bm = ImageUtils.getArtwork(MediaLockscreenActivity.this, -1);
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
