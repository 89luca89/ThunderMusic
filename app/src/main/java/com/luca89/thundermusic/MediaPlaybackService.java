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

package com.luca89.thundermusic;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.media.audiofx.AudioEffect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.luca89.service.ArchiveOrgLinkRetriever;
import com.luca89.service.BandcampLinkRetriever;
import com.luca89.service.MediaButtonIntentReceiver;
import com.luca89.service.YoutubeLinkRetriever;
import com.luca89.service.search.OnlineRadioSearchTask;
import com.luca89.thundermusic.activities.MediaLockscreenActivity;
import com.luca89.thundermusic.activities.MediaPlaybackActivity;
import com.luca89.thundermusic.activities.PreferencesActivity;
import com.luca89.thundermusic.widgets.MediaAppWidgetProvider2x1;
import com.luca89.thundermusic.widgets.MediaAppWidgetProvider2x1_Light;
import com.luca89.thundermusic.widgets.MediaAppWidgetProvider4x1;
import com.luca89.thundermusic.widgets.MediaAppWidgetProvider4x1_Light;
import com.luca89.thundermusic.widgets.MediaAppWidgetProvider4x2;
import com.luca89.thundermusic.widgets.MediaAppWidgetProvider4x2_Light;
import com.luca89.thundermusic.widgets.MediaAppWidgetProvider4x4;
import com.luca89.thundermusic.widgets.MediaAppWidgetProvider4x4_Light;
import com.luca89.utils.ImageUtils;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.ThemeUtils;
import com.luca89.utils.dataset.Library;
import com.luca89.utils.dataset.OnlineTrack;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import thundermusic.IMediaPlaybackService;

/**
 * Provides "background" audio playback capabilities, allowing the user to
 * switch between activities without stopping playback.
 */
@SuppressWarnings("deprecation")
@SuppressLint({"NewApi", "HandlerLeak", "InflateParams"})
public class MediaPlaybackService
        extends Service implements OnSharedPreferenceChangeListener, SensorEventListener {
    /**
     * used to specify whether enqueue() should start playing the new list of
     * files right away, next or once all the currently queued files have been
     * played
     */
    public static final int PLAYBACKSERVICE_STATUS = 1;

    public static final int SHUFFLE_NONE = 0;
    public static final int SHUFFLE_NORMAL = 1;
    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_CURRENT = 1;
    public static final int REPEAT_ALL = 2;
    public static final String LUCA89_PACKAGE_NAME = "com.luca89.thundermusic";
    public static final String MUSIC_PACKAGE_NAME = "com.android.music";
    public static final String PLAYSTATE_CHANGED = "com.luca89.thundermusic.playstatechanged";
    public static final String META_CHANGED = "com.luca89.thundermusic.metachanged";
    public static final String TAG_CHANGED = "com.luca89.thundermusic.tagchanged";
    public static final String START_BUFFERING = "com.luca89.thundermusic.startbuffer";
    public static final String END_BUFFERING = "com.luca89.thundermusic.endbuffer";
    public static final String UPDATE_BUFFERING = "com.luca89.thundermusic.updatebuffer";
    public static final String QUEUE_CHANGED = "com.luca89.thundermusic.queuechanged";
    public static final String REPEATMODE_CHANGED = "com.luca89.thundermusic.repeatmodechanged";
    public static final String SHUFFLEMODE_CHANGED = "com.luca89.thundermusic.shufflemodechanged";
    public static final String SERVICECMD = "com.luca89.thundermusic.musicservicecommand";
    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPLAY = "play";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDPREVIOUS2 = "previous2";
    public static final String CMDNEXT3 = "next3";
    public static final String CMDNEXT2 = "next2";
    public static final String CMDNEXT = "next";
    public static final String CMDSEEK = "seek";
    public static final String CMDSEEK2 = "seek2";
    public static final String CMDSEEK3 = "seek3";
    public static final String CMDSEEK4 = "seek4";
    public static final String CMDVOLUP = "volup";
    public static final String CMDVOLDOWN = "voldown";
    public static final String CMDCYCLEREPEAT = "cyclerepeat";
    public static final String CMDTOGGLESHUFFLE = "toggleshuffle";
    public static final String CMDNOTIF = "buttonId";
    public static final String CMDUNPAUSE = "unpause";
    public static final String TOGGLEPAUSE_ACTION =
            "com.luca89.thundermusic.musicservicecommand.togglepause";
    public static final String PAUSE_ACTION = "com.luca89.thundermusic.musicservicecommand.pause";
    public static final String PREVIOUS_ACTION =
            "com.luca89.thundermusic.musicservicecommand.previous";
    public static final String PREVIOUS_ACTION2 =
            "com.luca89.thundermusic.musicservicecommand.previous2";
    public static final String NEXT_ACTION = "com.luca89.thundermusic.musicservicecommand.next";
    public static final String NEXT_ACTION2 = "com.luca89.thundermusic.musicservicecommand.next2";
    public static final String NEXT_ACTION3 = "com.luca89.thundermusic.musicservicecommand.next3";
    public static final String SEEK_ACTION = "com.luca89.thundermusic.musicservicecommand.seek";
    public static final String SEEK_ACTION2 = "com.luca89.thundermusic.musicservicecommand.seek2";
    public static final String SEEK_ACTION3 = "com.luca89.thundermusic.musicservicecommand.seek3";
    public static final String SEEK_ACTION4 = "com.luca89.thundermusic.musicservicecommand.seek4";
    public static final String VOLUME_ACTION = "com.luca89.thundermusic.musicservicecommand.volume";
    public static final String VOLUME_ACTION2 =
            "com.luca89.thundermusic.musicservicecommand.volume2";
    public static final String CYCLEREPEAT_ACTION =
            "com.luca89.thundermusic.musicservicecommand.cyclerepeat";
    public static final String TOGGLESHUFFLE_ACTION =
            "com.luca89.thundermusic.musicservicecommand.toggleshuffle";
    private static final int TRACK_ENDED = 1;
    private static final int RELEASE_WAKELOCK = 2;
    private static final int SERVER_DIED = 3;
    private static final int FADEDOWN = 5;
    private static final int FADEUP = 6;
    private static final int FADEIN_FROM_DUCK = 10;
    private static final int MIN_SHAKE_PERIOD = 1000;
    //    private static KeyguardManager manager;
    //    private static KeyguardManager.KeyguardLock lock;
    //    private static boolean isKeyguardEnabled = true;
    //    private static String LOG_lock = "com.luca89.thundermusic.keyguard";
    private static final int TRACK_WENT_TO_NEXT = 7;
    private static final int IDLE_TIMEOUT = 8;
    private static MediaAppWidgetProvider4x1 mAppWidgetProvider4x1 =
            MediaAppWidgetProvider4x1.getInstance();
    private static MediaAppWidgetProvider4x2 mAppWidgetProvider4x2 =
            MediaAppWidgetProvider4x2.getInstance();
    private static MediaAppWidgetProvider2x1 mAppWidgetProvider2x1 =
            MediaAppWidgetProvider2x1.getInstance();
    private static MediaAppWidgetProvider4x4 mAppWidgetProvider4x4 =
            MediaAppWidgetProvider4x4.getInstance();
    private static MediaAppWidgetProvider4x1_Light mAppWidgetProvider4x1_Light =
            MediaAppWidgetProvider4x1_Light.getInstance();
    private static MediaAppWidgetProvider4x2_Light mAppWidgetProvider4x2_Light =
            MediaAppWidgetProvider4x2_Light.getInstance();
    private static MediaAppWidgetProvider2x1_Light mAppWidgetProvider2x1_Light =
            MediaAppWidgetProvider2x1_Light.getInstance();
    private static MediaAppWidgetProvider4x4_Light mAppWidgetProvider4x4_Light =
            MediaAppWidgetProvider4x4_Light.getInstance();
    private static OnlineTrack mOnlineTrack;
    private static Library mOnlineLibrary;
    private static int mOnlinePosition;
    private static int mOnlineDuration = 0;
    private static ImageView mAlbum;
    private static TextView title;
    private static TextView album;
    private static boolean moved = false;
    private static boolean open = false;
    private static Thread artwork;
    private static Boolean mIsInProximity = false;
    private static Sensor mProximity;
    private static boolean mScreenOFF = false;
    final Handler handler = new Handler();
    private final String LOGTAG = "MediaPlaybackService";
    private final Shuffler mRand = new Shuffler();
    private final int IDLE_DELAY = 150000;
    private final IBinder mBinder = new ServiceStub(this);
    private int mShuffleMode = SHUFFLE_NONE;
    private int mRepeatMode = REPEAT_NONE;
    private Notification status;
    private Notification buffering;
    private NotificationManager mnotif;
    private boolean mPlugInitialized;
    private boolean mPlugInitializedPlayer;
    private SensorManager mSensorManager;
    private boolean Flip = false;
    /**
     * Magnitude of last sensed acceleration.
     */
    private float mAccelLast;
    /**
     * Filtered acceleration used for shake detection.
     */
    private float mAccelFiltered;
    /**
     * Elapsed realtime of last shake action.
     */
    private long mLastShakeTime;
    /**
     * Minimum jerk required for shake.
     */
    private float mShakeThreshold;
    private int mShakeAction;
    private MultiPlayer mPlayer;
    private String mFileToPlay;
    private int mMediaMountedCount = 0;
    // interval after which we stop the service when idle
    private int mPlayListLen = 1;
    private int mPlayPos = -1;
    private WakeLock mWakeLock;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mIsSupposedToBePlaying = false;
    private AudioManager mAudioManager;
    // used to track what type of audio focus loss caused the playback to pause
    private boolean mPausedByTransientLossOfFocus = false;
    private float mTransientDuckVolume = 1.0f;
    // We use this to distinguish between different cards when saving/restoring
    // playlists.
    // This will have to change if we want to support multiple simultaneous
    // cards.
    private int mNextPlayPos = -1;
    /**
     * The time to wait before considering the player idle.
     */
    private int mIdleTimeout;
    /**
     * The volume set by the user in the preferences.
     */
    private float mUserVolume = 1.0f;
    /**
     * The actual volume of the media player. Will differ from the user volume
     * when fading the volume.
     */
    private float mCurrentVolume = 1.0f;
    private int bufferPercent = 0;
    private SharedPreferences mSettings;
    private Handler mHandler;
    private RemoteControlClient mRemoteControlClient;
    private WindowManager windowManager;
    private FrameLayout chatHead;
    private RelativeLayout mWidget;
    private ImageButton mPlay;
    private ImageButton mNext;
    private ImageButton mPrev;
    private ImageButton mClose;
    private Vibrator mVibrator;
    Runnable mLongPressed = new Runnable() {
        public void run() {
            moved = true;
            mVibrator.vibrate(100);
        }
    };
    private SensorEventListener ProximityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent se) {
            // Code here
            float distance = se.values[0];
            mIsInProximity = distance < mProximity.getMaximumRange();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    };
    /**
     * Perform actions based on Audio focus changes
     */
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                if (isPlaying()) {
                    int attenuation = getTransientDuckAttenuation();
                    if (attenuation < 0) {
                        // Pause
                        mPausedByTransientLossOfFocus = true;
                        pause();
                    } else if (attenuation > 0) {
                        // 0 Means no ducking wanted so do nothing in that case
                        // setVolume wants a scalar value, not logarithmic
                        mTransientDuckVolume = (float) Math.pow(10, -attenuation / 20f);
                        mPlayer.setVolume(mTransientDuckVolume);
                    }
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                if (!isPlaying() && mPausedByTransientLossOfFocus) {
                    mPausedByTransientLossOfFocus = false;
                    mCurrentVolume = 1.0f;
                    mPlayer.setVolume(mCurrentVolume);
                    play(); // also queues a fade-in
                } else if (isPlaying()) {
                    mCurrentMediaPlayerHandler.sendEmptyMessageDelayed(FADEUP, 10);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                if (isPlaying()) {
                    mPausedByTransientLossOfFocus = false;
                }
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                if (isPlaying()) {
                    mPausedByTransientLossOfFocus = true;
                }
                pause();
            }
        }
    };
    private Handler mCurrentMediaPlayerHandler = new Handler() {
        float mCurrentVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADEIN_FROM_DUCK:
                    if (isPlaying()) {
                        mTransientDuckVolume = 1.0f;
                        mPlayer.setVolume(mTransientDuckVolume);
                        mCurrentVolume = mTransientDuckVolume;
                    } else {
                        mTransientDuckVolume = 1.0f;
                        break;
                    }
                case FADEDOWN:
                    mCurrentVolume -= .05f;
                    if (mCurrentVolume > .2f) {
                        mCurrentMediaPlayerHandler.sendEmptyMessageDelayed(FADEDOWN, 10);
                    } else {
                        mCurrentVolume = .2f;
                    }
                    mPlayer.setVolume(mCurrentVolume);
                    break;
                case FADEUP:
                    mCurrentVolume += .01f;
                    if (mCurrentVolume < 1.0f) {
                        mCurrentMediaPlayerHandler.sendEmptyMessageDelayed(FADEUP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    mPlayer.setVolume(mCurrentVolume);
                    break;
                case SERVER_DIED:
                    if (mIsSupposedToBePlaying) {
                        gotoNext(true);
                    }
                    break;
                case IDLE_TIMEOUT:
                    mHandler.sendMessage(mHandler.obtainMessage(FADEDOWN, 100));
                    userActionTriggered();
                    pause();
                    stop();
                    break;
                case TRACK_WENT_TO_NEXT:
                    if (mNextPlayPos >= 0 && mOnlineLibrary != null) {
                        mPlayPos = mNextPlayPos;
                        notifyChange(META_CHANGED);
                        updateTotalNotification(false, getBaseContext());
                    }
                    break;
                case TRACK_ENDED:
                    if (mRepeatMode == REPEAT_CURRENT) {
                        seek(0);
                        play();
                    } else {
                        gotoNext(false);
                    }
                    break;
                case RELEASE_WAKELOCK:
                    try {
                        mWakeLock.release();
                    } catch (Exception e) {
                        // Wakelock release fail
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (isPlaying() || mPausedByTransientLossOfFocus || mServiceInUse
                    || mCurrentMediaPlayerHandler.hasMessages(TRACK_ENDED)) {
                return;
            }
            // save the queue again, because it might have changed
            // since the user exited the music app (because of
            // party-shuffle or because the play-position changed)
            stopSelf(mServiceStartId);
        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                gotoNext(true);
            } else if (CMDVOLUP.equals(cmd) || VOLUME_ACTION.equals(action)) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            } else if (CMDVOLDOWN.equals(cmd) || VOLUME_ACTION2.equals(action)) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            } else if (CMDNEXT2.equals(cmd) || NEXT_ACTION2.equals(action)) {
                gotoNext(true);
                gotoNext(true);

            } else if (CMDNEXT3.equals(cmd) || NEXT_ACTION3.equals(action)) {
                int shuffle = getShuffleMode();
                setShuffleMode(SHUFFLE_NORMAL);
                gotoNext(true);
                setShuffleMode(shuffle);
            } else if (CMDSEEK.equals(cmd) || SEEK_ACTION.equals(action)) {
                seek(mPlayer.position() + 30000);
            } else if (CMDSEEK2.equals(cmd) || SEEK_ACTION2.equals(action)) {
                seek(mPlayer.position() + 60000);
            } else if (CMDSEEK3.equals(cmd) || SEEK_ACTION3.equals(action)) {
                seek(mPlayer.position() - 30000);
            } else if (CMDSEEK4.equals(cmd) || SEEK_ACTION4.equals(action)) {
                seek(mPlayer.position() - 60000);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (CMDPREVIOUS2.equals(cmd) || PREVIOUS_ACTION2.equals(action)) {
                prev();
                prev();
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDPLAY.equals(cmd)) {
                play();
            } else if (CMDSTOP.equals(cmd)) {
                pause();

                mPausedByTransientLossOfFocus = false;
                seek(0);
            } else if (CMDUNPAUSE.equals(cmd)) {
                if (!isPlaying()) {
                    play();
                }
            } else if (CMDCYCLEREPEAT.equals(cmd) || CYCLEREPEAT_ACTION.equals(action)) {
                cycleRepeat();
            } else if (CMDTOGGLESHUFFLE.equals(cmd) || TOGGLESHUFFLE_ACTION.equals(action)) {
                toggleShuffle();
            } else if (MediaAppWidgetProvider4x1.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets,
                // probably
                // because they were just added.
                int[] appWidgetURLs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider4x1.performUpdate(MediaPlaybackService.this, appWidgetURLs);
            } else if (MediaAppWidgetProvider4x2.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets,
                // probably
                // because they were just added.
                int[] appWidgetURLs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider4x2.performUpdate(MediaPlaybackService.this, appWidgetURLs);
            } else if (MediaAppWidgetProvider2x1.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets,
                // probably
                // because they were just added.
                int[] appWidgetURLs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider2x1.performUpdate(MediaPlaybackService.this, appWidgetURLs);
            } else if (MediaAppWidgetProvider4x4.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets,
                // probably
                // because they were just added.
                int[] appWidgetURLs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider4x4.performUpdate(MediaPlaybackService.this, appWidgetURLs);
            } else if (MediaAppWidgetProvider4x1_Light.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets,
                // probably
                // because they were just added.
                int[] appWidgetURLs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider4x1_Light.performUpdate(MediaPlaybackService.this, appWidgetURLs);
            } else if (MediaAppWidgetProvider4x2_Light.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets,
                // probably
                // because they were just added.
                int[] appWidgetURLs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider4x2_Light.performUpdate(MediaPlaybackService.this, appWidgetURLs);
            } else if (MediaAppWidgetProvider2x1_Light.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets,
                // probably
                // because they were just added.
                int[] appWidgetURLs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider2x1_Light.performUpdate(MediaPlaybackService.this, appWidgetURLs);
            } else if (MediaAppWidgetProvider4x4_Light.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets,
                // probably
                // because they were just added.
                int[] appWidgetURLs = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider4x4_Light.performUpdate(MediaPlaybackService.this, appWidgetURLs);
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                mScreenOFF = false;
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mScreenOFF = true;

                if (mSettings.getBoolean("use_as_lock", false))
                    new CountDownTimer(2000, // 1 second countdown
                            9999) {
                        @Override
                        public void onTick(long millisUntilFinished) {}

                        @Override
                        public void onFinish() {
                            if (isPlaying() && mScreenOFF == true) {
                                StartLock();
                            }
                        }
                    }
                            .start();
            } else if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                if (mSettings.getBoolean("unpause_on_headset_plug", false) && mPlugInitialized
                        && intent.getIntExtra("state", 0) == 1) {
                    Log.d(getClass().getSimpleName(), "Headset connected, resuming playback");
                    if (!isPlaying()) {
                        play();
                    }
                } else if (!mPlugInitialized) {
                    mPlugInitialized = true;
                }
                if (mSettings.getBoolean("launch_on_headset_plug", false) && mPlugInitializedPlayer
                        && intent.getIntExtra("state", 0) == 1) {
                    Log.d(getClass().getSimpleName(), "Headset connected, resuming playback");

                    StartPlayer();
                } else if (!mPlugInitializedPlayer) {
                    mPlugInitializedPlayer = true;
                }
            }
        }
    };

    /**
     * Download image from internet
     *
     * @param urlStr
     * @return
     * @throws IOException
     */
    private static BitmapDrawable getDrawableFromUrl(final String urlStr) throws IOException {
        URL url = new URL(urlStr);
        final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setUseCaches(true);
        httpURLConnection.connect();
        return (BitmapDrawable) Drawable.createFromStream(
                httpURLConnection.getInputStream(), "name");
    }

    /**
     * Setup the artwork for the online reproducion
     * First check if is using shoutcast
     * then retrieve the correct artwork for the provider
     * <p>
     * if no artowrk is found, try to download it from internet
     *
     * @param context
     */
    private static void setOnlineArtwork(final Context context) {
        artwork = new Thread(new Runnable() {
            public void run() {
                // do stuff that doesn't touch the UI here
                Bitmap bm;
                try {
                    if (mOnlineTrack.getRadiomode()) {
                        bm = ((BitmapDrawable) context.getResources().getDrawable(
                                      R.drawable.logo_shoutcast))
                                     .getBitmap();
                    } else {
                        int id = InterfaceUtils.getOnlineDefaultArtworkRes(
                                mOnlineTrack.getProvider());
                        if (id != 0) {
                            bm = ((BitmapDrawable) context.getResources().getDrawable(id))
                                         .getBitmap();
                        } else {
                            bm = getDrawableFromUrl(mOnlineTrack.getArtUrl()).getBitmap();
                        }
                    }
                    ImageUtils.setThumbArtwork(bm, context, -2);
                    MusicUtils.sService.notifyChange(META_CHANGED);
                } catch (Exception e) {
                }
            }
        });
        artwork.setPriority(Thread.MAX_PRIORITY);
        artwork.start();
    }

    private int getTransientDuckAttenuation() {
        return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString(
                PreferencesActivity.KEY_DUCK_ATTENUATION, "10"));
    }

    public void StartLock() {
        Intent intent = new Intent();
        intent.setClass(this, MediaLockscreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_FROM_BACKGROUND
                | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void StartPlayer() {
        Intent intent = new Intent();
        intent.setClass(this, MediaPlaybackActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mOnlineTrack = new OnlineTrack("", "", "", "", "", "", 0, 0, "", false);
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        ComponentName rec =
                new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(rec);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(rec);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
        setRemoteControlClient();
        // mAudioManager.registerRemoteControlClient(mRemoteControlClient);
        mnotif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;
        mRemoteControlClient.setTransportControlFlags(flags);
        // Needs to be done in this thread, since otherwise
        // ApplicationContext.getPowerManager() crashes.
        mPlayer = new MultiPlayer();
        mPlayer.setHandler(mCurrentMediaPlayerHandler);
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(NEXT_ACTION2);
        commandFilter.addAction(NEXT_ACTION3);
        commandFilter.addAction(VOLUME_ACTION);
        commandFilter.addAction(VOLUME_ACTION2);
        commandFilter.addAction(PREVIOUS_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION2);
        commandFilter.addAction(SEEK_ACTION);
        commandFilter.addAction(SEEK_ACTION2);
        commandFilter.addAction(SEEK_ACTION3);
        commandFilter.addAction(SEEK_ACTION4);
        commandFilter.addAction(CYCLEREPEAT_ACTION);
        commandFilter.addAction(TOGGLESHUFFLE_ACTION);
        commandFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        commandFilter.addAction(Intent.ACTION_SCREEN_ON);
        commandFilter.addAction(Intent.ACTION_SCREEN_OFF);
        commandFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
        registerReceiver(mIntentReceiver, commandFilter);

        if (mSettings == null) mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings.registerOnSharedPreferenceChangeListener(this);
        mIdleTimeout = mSettings.getBoolean("use_idle_timeout", false)
                ? mSettings.getInt("idle_timeout", 3600)
                : 0;
        if (mSettings.getBoolean("enable_shake", false)) {
            mShakeAction = Integer.valueOf(mSettings.getString("shake_action", "2"));
        }
        mShakeThreshold = mSettings.getInt("shake_threshold", 80) / 10.0f;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that
        // case.
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);

        mAccelFiltered = 0.0f;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        setupSensor();
    }

    /**
     * Setup the accelerometer.
     * Credits to Christopher Eby <kreed@kreed.org> for base code!
     */
    private void setupSensor() {
        if (!mSettings.getBoolean("enable_shake", false)
                && !mSettings.getBoolean(PreferencesActivity.KEY_ENABLE_FLIP, false)) {
            mSensorManager.unregisterListener(this);
        } else {
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(ProximityListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * Accellerometer and timeout preferences
     * <p>
     * Credits to Christopher Eby <kreed@kreed.org> for base code!
     *
     * @param key
     */
    private void loadPreference(String key) {
        if ("use_idle_timeout".equals(key) || "idle_timeout".equals(key)) {
            mIdleTimeout = mSettings.getBoolean("use_idle_timeout", false)
                    ? mSettings.getInt("idle_timeout", 3600)
                    : 0;
            userActionTriggered();
        } else if ("enable_shake".equals(key) || "shake_action".equals(key)) {
            mShakeAction = mSettings.getBoolean("enable_shake", false)
                    ? Integer.valueOf(mSettings.getString("shake_action", "2"))
                    : 2;
        } else if ("shake_threshold".equals(key)) {
            mShakeThreshold = mSettings.getInt("shake_threshold", 80) / 10.0f;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        this.stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        // Check that we're not being destroyed while something is still
        // playing.
        if (chatHead != null) windowManager.removeView(chatHead);
        if (isPlaying()) {
            Log.e(LOGTAG, "Service being destroyed while still playing.");
        }

        if (mShakeAction != 0) mSensorManager.unregisterListener(this);
        // release all MediaPlayer resources, including the native player and
        // wakelocks

        Intent i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(i);
        pause();
        mPlayer.release();
        mPlayer = null;

        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        setRemoteControlClient();

        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mCurrentMediaPlayerHandler.removeCallbacksAndMessages(null);

        unregisterReceiver(mIntentReceiver);
        dismissAllNotifications();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                gotoNext(true);
            } else if (CMDVOLUP.equals(cmd) || VOLUME_ACTION.equals(action)) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            } else if (CMDVOLDOWN.equals(cmd) || VOLUME_ACTION2.equals(action)) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            } else if (CMDNEXT2.equals(cmd) || NEXT_ACTION2.equals(action)) {
                gotoNext(true);
                gotoNext(true);
            } else if (CMDNEXT3.equals(cmd) || NEXT_ACTION3.equals(action)) {
                int shuffle = getShuffleMode();
                setShuffleMode(SHUFFLE_NORMAL);
                gotoNext(true);
                setShuffleMode(shuffle);
            } else if (CMDSEEK.equals(cmd) || SEEK_ACTION.equals(action)) {
                seek(mPlayer.position() + 30000);
            } else if (CMDSEEK2.equals(cmd) || SEEK_ACTION2.equals(action)) {
                seek(mPlayer.position() + 60000);
            } else if (CMDSEEK3.equals(cmd) || SEEK_ACTION3.equals(action)) {
                seek(mPlayer.position() - 30000);
            } else if (CMDSEEK4.equals(cmd) || SEEK_ACTION4.equals(action)) {
                seek(mPlayer.position() - 60000);
            } else if (CMDPREVIOUS2.equals(cmd) || PREVIOUS_ACTION2.equals(action)) {
                prev();
                prev();
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                if (position() < 5000) {
                    prev();
                } else {
                    seek(0);
                    play();
                }
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDPLAY.equals(cmd)) {
                play();
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                if (intent.getIntExtra(CMDNOTIF, 0) == 3) {
                    stopForeground(true);
                }
                mPausedByTransientLossOfFocus = false;
                seek(0);
            } else if (CMDCYCLEREPEAT.equals(cmd) || CYCLEREPEAT_ACTION.equals(action)) {
                cycleRepeat();
            } else if (CMDTOGGLESHUFFLE.equals(cmd) || TOGGLESHUFFLE_ACTION.equals(action)) {
                toggleShuffle();
            }
        }

        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;

        if (isPlaying() || mPausedByTransientLossOfFocus) {
            // something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop
            // the service now.
            return true;
        }

        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between
        // tracks.
        if (mPlayListLen > 0 || mCurrentMediaPlayerHandler.hasMessages(TRACK_ENDED)) {
            Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }

        // No active playlist, OK to stop the service right now
        stopSelf(mServiceStartId);
        return true;
    }

    /**
     * Notify the change-receivers that something has changed. The intent that
     * is sent contains the following data for the currently playing track: "id"
     * - Integer: the database row ID "artist" - String: the name of the artist
     * "album" - String: the name of the album "track" - String: the name of the
     * track The intent has an action that is one of
     * "com.luca89.thundermusic.metachanged" "com.luca89.thundermusic.queuechanged",
     * "com.luca89.thundermusic.playbackcomplete"
     * "com.luca89.thundermusic.playstatechanged" respectively indicating that a
     * new track has started playing, that the playback queue has changed, that
     * playback has stopped because the last file in the list has been played,
     * or that the play-state changed (paused/resumed).
     */
    public void notifyChange(String what) {
        Intent i = new Intent(what);
        i.putExtra("id", Long.valueOf(getAudioId()));
        i.putExtra("artist", getArtistName());
        i.putExtra("album", getAlbumName());
        i.putExtra("track", getTrackName());
        i.putExtra("playing", isPlaying());
        sendStickyBroadcast(i);

        i = new Intent(i);
        i.setAction(what.replace(LUCA89_PACKAGE_NAME, MUSIC_PACKAGE_NAME));
        sendStickyBroadcast(i);

        if (what.equals(PLAYSTATE_CHANGED)) {
            if (mSettings.getBoolean(PreferencesActivity.KEY_AOSP_LOCK, true))
                mRemoteControlClient.setPlaybackState(isPlaying()
                                ? RemoteControlClient.PLAYSTATE_PLAYING
                                : RemoteControlClient.PLAYSTATE_PAUSED);
            if (chatHead != null) setPauseButtonImage();
        } else if (what.equals(META_CHANGED)) {
            if (mSettings.getBoolean(PreferencesActivity.KEY_AOSP_LOCK, true)) {
                MetadataEditor ed = mRemoteControlClient.editMetadata(true);
                ed.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getTrackName());
                ed.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getAlbumName());
                ed.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getArtistName());
                ed.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration());
                BitmapDrawable b =
                        ((BitmapDrawable) ImageUtils.getArtwork(this, getAlbumId(), true));

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    if (b != null) ed.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, b.getBitmap());
                    ed.apply();
                } else {
                    Bitmap art = null;
                    if (b != null) art = b.getBitmap().copy(b.getBitmap().getConfig(), true);
                    if (art != null) {
                        ed.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, art);
                    }
                    ed.apply();
                    art.recycle();
                }
            }
            if (chatHead != null) {
                chatHead.post(new Runnable() {
                    public void run() {
                        updateMeta(getBaseContext());
                        setPauseButtonImage();
                    }
                });
            }
        } else if (what.equals(START_BUFFERING)) {
            if (!isNetworkAvailable()) {
                stop();
                buffering = new Builder(this)
                                    .setSmallIcon(R.drawable.indicator_ic_mp_playing_list)
                                    .setTicker(getString(R.string.no_internet_available))
                                    .setContentTitle(getString(R.string.no_internet_available))
                                    .setContentText(getString(R.string.no_internet_available))
                                    .build();

                buffering.contentIntent = PendingIntent.getActivity(this, 0,
                        new Intent("com.luca89.thundermusic.PLAYBACK_VIEWER")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        0);
                mnotif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mnotif.notify(PLAYBACKSERVICE_STATUS, buffering);
            } else {
                buffering = new Builder(this)
                                    .setSmallIcon(R.drawable.indicator_ic_mp_playing_list)
                                    .setTicker("Loading " + getTrackName())
                                    .setContentTitle("Loading " + getTrackName())
                                    .setContentText("Buffering...")
                                    .build();

                buffering.contentIntent = PendingIntent.getActivity(this, 0,
                        new Intent("com.luca89.thundermusic.PLAYBACK_VIEWER")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        0);
                mnotif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mnotif.notify(PLAYBACKSERVICE_STATUS, buffering);
            }

        } else if (what.equals(END_BUFFERING)) {
            updateTotalNotification(false, this);
            play();
        } else if (what.equals(TAG_CHANGED)) {
            notifyChange(META_CHANGED);
        }
        setupSensor();

        // Share this notification directly with our widgets

        mAppWidgetProvider4x1.notifyChange(this, what);
        mAppWidgetProvider4x2.notifyChange(this, what);
        mAppWidgetProvider2x1.notifyChange(this, what);
        mAppWidgetProvider4x4.notifyChange(this, what);
        mAppWidgetProvider4x1_Light.notifyChange(this, what);
        mAppWidgetProvider4x2_Light.notifyChange(this, what);
        mAppWidgetProvider2x1_Light.notifyChange(this, what);
        mAppWidgetProvider4x4_Light.notifyChange(this, what);
    }

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {
        mAudioManager.requestAudioFocus(
                mAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mPlayer.setVolume(1.0f);
        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(
                this.getPackageName(), MediaButtonIntentReceiver.class.getName()));

        if (mPlayer.isInitialized()) {
            // if we are at the end of the song, go to the next song first
            long duration = mPlayer.duration();
            if (mRepeatMode != REPEAT_CURRENT && duration > 2000
                    && mPlayer.position() >= duration - 2000) {
                gotoNext(true);
            }

            mPlayer.start();
            // make sure we fade in, in case a previous fadein was stopped
            // because
            // of another focus loss
            mCurrentMediaPlayerHandler.removeMessages(FADEDOWN);
            mCurrentMediaPlayerHandler.sendEmptyMessage(FADEUP);

            updateTotalNotification(false, this);
        }
        mIsSupposedToBePlaying = true;
        notifyChange(PLAYSTATE_CHANGED);
    }

    private void stop(boolean remove_status_icon) {
        if (mPlayer != null)
            if (mPlayer.isInitialized()) {
                mPlayer.stop();
            }
        mFileToPlay = null;
        if (remove_status_icon) {
            gotoIdleState();
        }
        if (remove_status_icon) {
            mIsSupposedToBePlaying = false;
        }
    }

    /**
     * Stops playback.
     */
    public void stop() {
        stop(true);
    }

    /**
     * Pauses playback (call play() to resume)
     */
    public void pause() {
        pause(true);
    }

    public void pause(boolean notif) {
        synchronized (this) {
            mCurrentMediaPlayerHandler.removeMessages(FADEUP);
            mCurrentMediaPlayerHandler.removeMessages(FADEUP);
            if (isPlaying()) {
                mPlayer.pause();
                gotoIdleState();
                if (notif) {
                    updateTotalNotification(true, this);
                    mIsSupposedToBePlaying = false;
                    notifyChange(PLAYSTATE_CHANGED);
                }
                userActionTriggered();
            }
        }
    }

    /**
     * Returns whether something is currently playing
     *
     * @return true if something is playing (or will be playing shortly, in case
     * we're currently transitioning between tracks), false if not.
     */
    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    public void prev() {
        synchronized (this) {
            if (MusicUtils.getBooleanPref(this, "radiomode", false))
                return;
            else {
                stop(false);
                if (mOnlinePosition > 0) {
                    mOnlinePosition--;
                } else {
                    mOnlinePosition = mOnlineLibrary.getVideos().size() - 1;
                }
                mOnlineTrack = mOnlineLibrary.getVideos().get(mOnlinePosition);
                openOnline(mOnlineTrack.getUrl());
            }
        }
    }

    public void gotoNext(boolean force) {
        synchronized (this) {
            if (mPlayListLen <= 0) {
                Log.d(LOGTAG, "No play queue");
                return;
            }
            int pos = getOnlineNextPosition(force);
            if (pos < 0) {
                gotoIdleState();
                if (mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = false;
                    notifyChange(PLAYSTATE_CHANGED);
                }
                return;
            }
            stop(false);
            mOnlinePosition = pos;
            mOnlineTrack = mOnlineLibrary.getVideos().get(mOnlinePosition);
            openOnline(mOnlineTrack.getUrl());
        }
    }

    public void cycleRepeat() {
        if (mRepeatMode == REPEAT_NONE) {
            setRepeatMode(REPEAT_ALL);
        } else if (mRepeatMode == REPEAT_ALL) {
            setRepeatMode(REPEAT_CURRENT);
            if (mShuffleMode != SHUFFLE_NONE) {
                setShuffleMode(SHUFFLE_NONE);
            }
        } else {
            setRepeatMode(REPEAT_NONE);
        }
    }

    public void toggleShuffle() {
        if (mShuffleMode == SHUFFLE_NONE) {
            setShuffleMode(SHUFFLE_NORMAL);
            if (mRepeatMode == REPEAT_CURRENT) {
                setRepeatMode(REPEAT_ALL);
            }
        } else if (mShuffleMode == SHUFFLE_NORMAL) {
            setShuffleMode(SHUFFLE_NONE);
        } else {
            Log.e("MediaPlaybackService", "Invalid shuffle mode: " + mShuffleMode);
        }
    }

    private void gotoIdleState() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        stopForeground(false);
    }

    public int getShuffleMode() {
        return mShuffleMode;
    }

    public void setShuffleMode(int shufflemode) {
        synchronized (this) {
            if (mShuffleMode == shufflemode && mPlayListLen > 0) {
                return;
            }
            mShuffleMode = shufflemode;
            notifyChange(SHUFFLEMODE_CHANGED);
        }
    }

    public int getRepeatMode() {
        return mRepeatMode;
    }

    public void setRepeatMode(int repeatmode) {
        synchronized (this) {
            mRepeatMode = repeatmode;
            notifyChange(REPEATMODE_CHANGED);
        }
    }

    public int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    /**
     * Returns the path of the currently playing file, or null if no file is
     * currently playing.
     */
    public String getPath() {
        return mFileToPlay;
    }

    /**
     * Returns the rowid of the currently playing file, or -1 if no file is
     * currently playing.
     */
    public long getAudioId() {
        synchronized (this) {
            if (mPlayer != null)
                if (mPlayPos >= 0 && mPlayer.isInitialized()) {
                    return mOnlineLibrary.getVideos().get(mPlayPos).hashCode();
                }
        }
        return -1;
    }

    public String getAudioUrl() {
        synchronized (this) {
            if (mPlayer != null)
                if (mOnlineTrack != null) {
                    return mOnlineTrack.getLink();
                }
        }
        return null;
    }

    /**
     * Returns the current play list
     *
     * @return An array of integers containing the IDs of the tracks in the play
     * list
     */
    public long[] getQueue() {
        synchronized (this) {
            if (mOnlineLibrary == null) return new long[0];
            int len = mPlayListLen;
            long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                list[i] = mOnlineLibrary.getVideos().get(i).hashCode();
            }
            return list;
        }
    }

    /**
     * Returns the position in the queue
     *
     * @return the position in the queue
     */
    public int getQueuePosition() {
        synchronized (this) {
            return mOnlinePosition;
        }
    }

    /**
     * Starts playing the track at the given position in the queue.
     *
     * @param pos The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int pos) {
        synchronized (this) {
            stop(false);
            mPlayPos = pos;
            play();
            notifyChange(META_CHANGED);
        }
    }

    public String getArtistName() {
        synchronized (this) {
            if (mOnlineTrack != null)
                return mOnlineTrack.getArtist();
            else
                return "";
        }
    }

    public String getAlbumName() {
        synchronized (this) {
            if (mOnlineTrack != null)
                return mOnlineTrack.getAlbum();
            else
                return "";
        }
    }

    public long getAlbumId() {
        synchronized (this) {
            return -2;
        }
    }

    public String getTrackTrack() {
        synchronized (this) {
            return Integer.toString(mOnlinePosition);
        }
    }

    public String getTrackLink() {
        return mOnlineTrack.getLink();
    }

    public String getTrackName() {
        synchronized (this) {
            if (mOnlineTrack != null)
                return mOnlineTrack.getTitle();
            else
                return null;
        }
    }

    public int getDuration() {
        synchronized (this) {
            int tmp = mOnlineTrack.getDuration() * 1000;
            if (tmp == 0)
                return mOnlineDuration;
            else
                return tmp;
        }
    }

    /**
     * Returns the duration of the file in milliseconds. Currently this method
     * returns -1 for the duration of MIDI files.
     */
    public long duration() {
        if (mPlayer != null)
            if (mPlayer.isInitialized()) {
                return mPlayer.duration();
            }
        return -1;
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (mPlayer != null)
            if (mPlayer.isInitialized()) {
                return mPlayer.position();
            }
        return -1;
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     */
    public long seek(long pos) {
        if (mPlayer != null)
            if (mPlayer.isInitialized()) {
                if (pos < 0) pos = 0;
                if (pos > mPlayer.duration()) pos = mPlayer.duration();
                return mPlayer.seek(pos);
            }
        return -1;
    }

    /**
     * Returns the audio session ID.
     */
    public int getAudioSessionId() {
        synchronized (this) {
            return mPlayer.getAudioSessionId();
        }
    }

    /**
     * Resets the idle timeout countdown. Should be called by a user action has
     * been trigger (new song chosen or playback toggled).
     * <p>
     * If an idle fade out is actually in progress, aborts it and resets the
     * volume.
     */

    private void userActionTriggered() {
        mHandler.removeMessages(FADEDOWN);
        mHandler.removeMessages(IDLE_TIMEOUT);
        if (mIdleTimeout != 0) mHandler.sendEmptyMessageDelayed(IDLE_TIMEOUT, mIdleTimeout * 1000);

        if (mCurrentVolume != mUserVolume) {
            mCurrentVolume = mUserVolume;
            mPlayer.setVolume(mCurrentVolume);
        }
    }

    /**
     * This section is for the accellerometer actions
     * <p>
     * Credits to Christopher Eby <kreed@kreed.org> for the base code!
     *
     * @param se
     */
    @Override
    public void onSensorChanged(SensorEvent se) {
        double x = se.values[0];
        double y = se.values[1];
        double z = se.values[2];

        float accel = (float) Math.sqrt(x * x + y * y + z * z);
        float delta = accel - mAccelLast;
        mAccelLast = accel;

        float filtered = mAccelFiltered * 0.9f + delta;
        mAccelFiltered = filtered;

        if (mSettings.getBoolean("enable_shake", false)) {
            if (filtered > mShakeThreshold) {
                long now = SystemClock.elapsedRealtime();
                if (now - mLastShakeTime > MIN_SHAKE_PERIOD) {
                    mLastShakeTime = now;

                    if (mSettings.getBoolean("shake_from_paused", false)) {
                        if (!mIsInProximity) performAction_free(mShakeAction);
                    } else {
                        if (!mIsInProximity) performAction(mShakeAction);
                    }
                }
            }
        }
        if (mSettings.getBoolean(PreferencesActivity.KEY_ENABLE_FLIP, false)) {
            if (!mIsInProximity)
                if (z >= mShakeThreshold && x < 2 && x > -2 && y < 2.5 && y > -2.5) {
                    if (Flip == true) {
                        if (!isPlaying()) {
                            play();
                            Flip = false;
                        }
                    }
                } else if (z <= -mShakeThreshold && x < 2 && x > -2 && y < 2.5 && y > -2.5) {
                    if (Flip == false) {
                        if (isPlaying()) {
                            pause();
                            Flip = true;
                        }
                    }
                }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /**
     * Perform an action based on user preference
     * even if not playing music
     *
     * @param action
     */
    private void performAction_free(int action) {
        switch (action) {
            case 0:
                break;
            case 1: {
                if (isPlaying()) {
                    pause();
                } else {
                    play();
                }
                break;
            }
            case 2: {
                gotoNext(true);
                break;
            }
            case 3: {
                prev();
                break;
            }
            case 4: {
                int shuffle = getShuffleMode();
                setShuffleMode(SHUFFLE_NORMAL);
                gotoNext(true);
                setShuffleMode(shuffle);
                break;
            }
            case 5: {
                gotoNext(true);
                gotoNext(true);

                break;
            }
            case 6: {
                prev();
                prev();
                break;
            }
            case 7: {
                seek(mPlayer.position() + 30000);
                break;
            }
            case 8: {
                seek(mPlayer.position() + 60000);
                break;
            }
            case 9: {
                seek(mPlayer.position() - 30000);
                break;
            }
            case 10: {
                seek(mPlayer.position() - 60000);
                break;
            }
            case 11: {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                break;
            }
            case 12: {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                break;
            }
            case 13: {
                pause();
                stop();
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }
    }

    /**
     * Perform an action based on user preference
     * only if music is already playing
     *
     * @param action
     */
    private void performAction(int action) {
        if (isPlaying()) {
            switch (action) {
                case 0:
                    break;
                case 1: {
                    if (isPlaying()) {
                        pause();
                    } else {
                        play();
                    }
                    break;
                }
                case 2: {
                    gotoNext(true);
                    break;
                }
                case 3: {
                    prev();
                    break;
                }
                case 4: {
                    int shuffle = getShuffleMode();
                    setShuffleMode(SHUFFLE_NORMAL);
                    gotoNext(true);
                    setShuffleMode(shuffle);
                    break;
                }
                case 5: {
                    gotoNext(true);
                    gotoNext(true);
                    break;
                }
                case 6: {
                    prev();
                    prev();
                    break;
                }
                case 7: {
                    seek(mPlayer.position() + 30000);
                    break;
                }
                case 8: {
                    seek(mPlayer.position() + 60000);
                    break;
                }
                case 9: {
                    seek(mPlayer.position() - 30000);
                    break;
                }
                case 10: {
                    seek(mPlayer.position() - 60000);
                    break;
                }
                case 11: {
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    break;
                }
                case 12: {
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    break;
                }
                case 13: {
                    pause();
                    stop();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid action: " + action);
            }
        }
    }

    /**
     * Setup the BT/Aosp lockscreen controls
     */
    private void setRemoteControlClient() {
        if (mSettings.getBoolean(PreferencesActivity.KEY_AOSP_LOCK, true)) {
            mAudioManager.registerRemoteControlClient(mRemoteControlClient);
            mRemoteControlClient.setPlaybackState(isPlaying()
                            ? RemoteControlClient.PLAYSTATE_PLAYING
                            : RemoteControlClient.PLAYSTATE_PAUSED);
        } else {
            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
            mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean isPlaying = isPlaying();
        loadPreference(key);

        if (key.equals(PreferencesActivity.KEY_AOSP_LOCK)) {
            setRemoteControlClient();
        }
        if ((key.equals(PreferencesActivity.KEY_NOTIFICATION)
                    || key.equals(PreferencesActivity.NOTIFICAION_ON)
                    || key.equals(PreferencesActivity.KEY_ENABLE_FLIP))
                && mPlayListLen > 0) {
            updateTotalNotification(!isPlaying(), this);
        }
        if (key.equals(PreferencesActivity.POPUP_ON)) {
            if (mSettings.getBoolean(PreferencesActivity.POPUP_ON, false)) {
                startPopup();
            } else {
                closePopup();
            }
        }
        if (key.equals(PreferencesActivity.KEY_ENABLE_FLIP) || key.equals("enable_shake"))
            setupSensor();
        if (isPlaying() && (key.equals(PreferencesActivity.KEY_AOSP_LOCK)))
            notifyChange(META_CHANGED);
    }

    private OnlineTrack getOnlineTrack(int position) {
        return mOnlineLibrary.getVideos().get(position);
    }

    /**
     * Sets the online queue
     * retrieve it from file and load in memory
     * <p>
     * see the explanation in readAdapter for further infos
     *
     * @param name
     * @param position
     */
    public void setOnlineLibrary(String name, int position) {
        try {
            mOnlineLibrary = MusicUtils.readAdapter(this, name);
            if (!mOnlineLibrary.getVideos().isEmpty()) {
                mPlayListLen = mOnlineLibrary.getVideos().size();
                if (position != -1) {
                    mOnlineTrack = getOnlineTrack(position);
                    mOnlinePosition = position;
                } else
                    mOnlineTrack = getOnlineTrack(mOnlinePosition);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getOnlineNextPosition(boolean force) {
        if (mRepeatMode == REPEAT_CURRENT) {
            if (mOnlinePosition < 0) return 0;
            return mOnlinePosition;
        } else if (mShuffleMode == SHUFFLE_NORMAL) {
            int numTracks = mOnlineLibrary.getVideos().size();
            if (numTracks == 1) return 0;
            int[] tracks = new int[numTracks];
            for (int i = 0; i < numTracks; i++) {
                tracks[i] = i;
            }
            int skip = mRand.nextInt(mOnlineLibrary.getVideos().size());
            int cnt = -1;
            while (true) {
                while (tracks[++cnt] < 0)
                    ;
                skip--;
                if (skip < 0) {
                    break;
                }
            }
            mOnlinePosition = cnt;
            return mOnlinePosition;
        } else {
            if (mOnlinePosition >= mOnlineLibrary.getVideos().size() - 1) {
                // we're at the end of the list
                if (mRepeatMode == REPEAT_NONE && !force) {
                    // all done
                    return -1;
                } else if (mRepeatMode == REPEAT_ALL || force) {
                    return 0;
                }
                return -1;
            } else {
                return mOnlinePosition + 1;
            }
        }
    }

    private void setVideoUrl(final String url) {
        if (mPlayer != null) {
            synchronized (this) {
                if (isNetworkAvailable()) {
                    mPlayer.stop();
                    mPlayer.setDataSource(url);
                } else {
                    Toast.makeText(this, R.string.no_internet_available, Toast.LENGTH_LONG).show();
                    stop();
                }
            }
        }
    }

    /**
     * Open an online track
     * Given the track, check the provider and act accordingly
     * for example, bandcamp and archive.org do not have a directly playable url
     * so is shoutcas
     * <p>
     * for others setVideoUrl is enough
     *
     * @param url
     */
    public void openOnline(String url) {
        synchronized (this) {
            stop(false);
            mFileToPlay = url;
            MusicUtils.setBooleanPref(getBaseContext(), "radiomode", mOnlineTrack.getRadiomode());
            setOnlineArtwork(this);
            if (mOnlineTrack.getRadiomode()) {
                MusicUtils.execute(false, new ShoutCastPlayTask(), true);
            } else {
                if (mOnlineTrack.getProvider().equalsIgnoreCase("soundcloud"))
                    setVideoUrl(mOnlineTrack.getUrl());
                else if (mOnlineTrack.getProvider().equalsIgnoreCase("freemusicarchive.org"))
                    setVideoUrl(mOnlineTrack.getUrl());
                else if (mOnlineTrack.getProvider().equalsIgnoreCase("archive.org"))
                    MusicUtils.execute(false, new ArchiveOrgPlayTask(mOnlineTrack.getLink()), true);
                else if (mOnlineTrack.getProvider().equalsIgnoreCase("bandcamp"))
                    MusicUtils.execute(false, new BandcampPlayTask(mOnlineTrack.getLink()), true);
                else if (mOnlineTrack.getProvider().equalsIgnoreCase("youtube"))
                    MusicUtils.execute(false, new YoutubePlayTask(mOnlineTrack.getLink()), true);
                else
                    setVideoUrl(url);
            }
        }
    }

    private void exit() {
        stopSelf(mServiceStartId);
    }

    private int getBufferState() {
        return bufferPercent;
    }

    private void dismissAllNotifications() {
        stopForeground(true);
        mnotif.cancelAll();
    }

    /**
     * Check network connectivity
     *
     * @return
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Little wrapper to call notifications
     * This is sensible to the notification preference
     * called in a thread to mantain the UI smooth
     *
     * @param stop
     * @param context
     */
    private void updateTotalNotification(final boolean stop, final Context context) {
        synchronized (this) {
            new Thread(new Runnable() {
                public void run() {
                    if (mSettings.getBoolean(PreferencesActivity.NOTIFICAION_ON, true)) {
                        int Notification = Integer.valueOf(
                                PreferenceManager.getDefaultSharedPreferences(context).getString(
                                        PreferencesActivity.KEY_NOTIFICATION, "1"));
                        if (Notification == 1) {
                            try {
                                updateNotification1(stop);
                            } catch (final IllegalStateException e) {
                            }
                        } else if (Notification == 2) {
                            try {
                                updateNotification2(stop);
                            } catch (final IllegalStateException e) {
                            }
                        }

                    } else {
                        stopForeground(true);
                    }
                }
            })
                    .start();
        }
    }

    /**
     * Small legacy stile notificatoin
     * based on a custom layout this is not expandable
     *
     * @param stop
     * @throws IllegalStateException
     */
    private void updateNotification2(boolean stop) throws IllegalStateException {
        mAudioManager.requestAudioFocus(
                mAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(
                this.getPackageName(), MediaButtonIntentReceiver.class.getName()));
        if (mPlayer != null)
            if (mPlayer.isInitialized()) {
                // if we are at the end of the song, go to the next song first
                long duration = mPlayer.duration();
                if (mRepeatMode != REPEAT_CURRENT && duration > 2000
                        && mPlayer.position() >= duration - 2000) {
                    gotoNext(true);
                }
                // make sure we fade in, in case a previous fadein was stopped
                // because
                // of another focus loss
                mCurrentMediaPlayerHandler.removeMessages(FADEDOWN);
                mCurrentMediaPlayerHandler.sendEmptyMessage(FADEUP);

                RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_simple);

                Bitmap tmp = ImageUtils.getArtworkQuick1(getBaseContext(), getAlbumId());
                if (tmp == null) tmp = ImageUtils.getDefaultArtwork(getBaseContext()).getBitmap();

                views.setImageViewBitmap(R.id.icon, tmp);
                ComponentName rec = new ComponentName(
                        getPackageName(), MediaButtonIntentReceiver.class.getName());
                Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                mediaButtonIntent.putExtra(CMDNOTIF, 1);
                mediaButtonIntent.setComponent(rec);
                KeyEvent mediaKey =
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
                PendingIntent mediaPendingIntent =
                        PendingIntent.getBroadcast(getApplicationContext(), 1, mediaButtonIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.status_bar_play, mediaPendingIntent);
                mediaButtonIntent.putExtra(CMDNOTIF, 2);
                mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
                mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
                mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2,
                        mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.skip, mediaPendingIntent);
                mediaButtonIntent.putExtra(CMDNOTIF, 3);
                mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
                mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 3,
                        mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.prev, mediaPendingIntent);

                if (stop)
                    views.setImageViewResource(
                            R.id.status_bar_play, ThemeUtils.getResourceDrawableNotification(3));
                else
                    views.setImageViewResource(
                            R.id.status_bar_play, ThemeUtils.getResourceDrawableNotification(2));

                views.setImageViewResource(
                        R.id.prev, ThemeUtils.getResourceDrawableNotification(4));
                views.setImageViewResource(
                        R.id.skip, ThemeUtils.getResourceDrawableNotification(1));

                views.setTextColor(R.id.trackname, ThemeUtils.getTextColorNotification(this));
                views.setTextColor(R.id.artistalbum, ThemeUtils.getTextColorNotification(this));

                if (getAudioId() < 0) {
                    // streaming
                    views.setTextViewText(R.id.trackname, getPath());
                    views.setTextViewText(R.id.artistalbum, null);
                } else {
                    String artist = getArtistName();
                    views.setTextViewText(R.id.trackname, getTrackName());
                    if (artist == null || artist.equals(MediaStore.UNKNOWN_STRING)) {
                        artist = getString(R.string.unknown_artist_name);
                    }
                    String album = getAlbumName();
                    if (album == null || album.equals(MediaStore.UNKNOWN_STRING)) {
                        album = getString(R.string.unknown_album_name);
                    }

                    views.setTextViewText(R.id.artistalbum,
                            getString(R.string.notification_artist_album, artist, album));
                }

                Notification status = new Notification();
                status.contentView = views;
                if (!stop)
                    status.flags |= Notification.FLAG_ONGOING_EVENT;
                else
                    status.flags |= Notification.FLAG_AUTO_CANCEL;
                status.icon = stop ? R.drawable.null_icon : R.drawable.now_playing_holo_dark;
                status.contentIntent = PendingIntent.getActivity(this, 0,
                        new Intent("com.luca89.thundermusic.PLAYBACK_VIEWER")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        0);
                if (!stop) {
                    startForeground(PLAYBACKSERVICE_STATUS, status);
                } else {
                    stopForeground(true);
                    mnotif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mnotif.notify(PLAYBACKSERVICE_STATUS, status);
                }
                mIsSupposedToBePlaying = !stop;
            }
    }

    /**
     * Expanded Media notification
     * based on bigpicture
     * <p>
     * shrinked notificaiton is a custom layout
     *
     * @param stop
     * @throws IllegalStateException
     */
    private void updateNotification1(boolean stop) throws IllegalStateException {
        if (mPlayer != null) {
            ComponentName rec1 =
                    new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
            Intent mediaButtonIntent1 = new Intent(Intent.ACTION_MEDIA_BUTTON);

            mediaButtonIntent1.putExtra(CMDNOTIF, 1);
            mediaButtonIntent1.setComponent(rec1);
            KeyEvent mediaKey1 =
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            mediaButtonIntent1.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey1);
            PendingIntent mediaPendingIntent1 = PendingIntent.getBroadcast(getApplicationContext(),
                    1, mediaButtonIntent1, PendingIntent.FLAG_UPDATE_CURRENT);

            ComponentName rec2 =
                    new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
            Intent mediaButtonIntent2 = new Intent(Intent.ACTION_MEDIA_BUTTON);

            mediaButtonIntent2.putExtra(CMDNOTIF, 2);
            mediaButtonIntent2.setComponent(rec2);
            KeyEvent mediaKey2 = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
            mediaButtonIntent2.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey2);
            PendingIntent mediaPendingIntent2 = PendingIntent.getBroadcast(getApplicationContext(),
                    2, mediaButtonIntent2, PendingIntent.FLAG_UPDATE_CURRENT);

            ComponentName rec3 =
                    new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
            Intent mediaButtonIntent3 = new Intent(Intent.ACTION_MEDIA_BUTTON);

            mediaButtonIntent3.putExtra(CMDNOTIF, 3);
            mediaButtonIntent3.setComponent(rec3);
            KeyEvent mediaKey3 =
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            mediaButtonIntent3.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey3);
            PendingIntent mediaPendingIntent3 = PendingIntent.getBroadcast(getApplicationContext(),
                    3, mediaButtonIntent3, PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap tmp = ImageUtils.getArtworkQuick1(getBaseContext(), getAlbumId());
            if (tmp == null) tmp = ImageUtils.getDefaultArtwork(getBaseContext()).getBitmap();

            RemoteViews mNotificationTemplate =
                    new RemoteViews(this.getPackageName(), R.layout.notification_simple);
            mNotificationTemplate.setOnClickPendingIntent(R.id.prev, mediaPendingIntent3);
            mNotificationTemplate.setOnClickPendingIntent(R.id.skip, mediaPendingIntent2);
            mNotificationTemplate.setOnClickPendingIntent(
                    R.id.status_bar_play, mediaPendingIntent1);
            mNotificationTemplate.setTextViewText(R.id.trackname, getTrackName());
            mNotificationTemplate.setTextViewText(R.id.artistalbum,
                    getString(R.string.notification_artist_album, getArtistName(), getAlbumName()));
            mNotificationTemplate.setImageViewBitmap(R.id.icon, tmp);
            if (stop)
                mNotificationTemplate.setImageViewResource(
                        R.id.status_bar_play, ThemeUtils.getResourceDrawableNotification(3));
            else
                mNotificationTemplate.setImageViewResource(
                        R.id.status_bar_play, ThemeUtils.getResourceDrawableNotification(2));

            mNotificationTemplate.setImageViewResource(
                    R.id.prev, ThemeUtils.getResourceDrawableNotification(4));
            mNotificationTemplate.setImageViewResource(
                    R.id.skip, ThemeUtils.getResourceDrawableNotification(1));

            mNotificationTemplate.setTextColor(
                    R.id.trackname, ThemeUtils.getTextColorNotification(this));
            mNotificationTemplate.setTextColor(
                    R.id.artistalbum, ThemeUtils.getTextColorNotification(this));

            // NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();
            // style.setShowActionsInCompactView(0, 1, 2);
            status = new Notification.Builder(this)
                             .setContentTitle(getTrackName())
                             .setContentText(getAlbumName())
                             .setSubText(getArtistName())
                             .setPriority(
                                     !stop ? Notification.PRIORITY_MAX : Notification.PRIORITY_LOW)
                             .setOngoing(stop ? false : true)
                             .setLargeIcon(tmp)
                             .setContent(mNotificationTemplate)
                             .setSmallIcon(R.drawable.now_playing_holo_dark)
                             .addAction(ThemeUtils.getResourceDrawableNotification(4),
                                     ThemeUtils.getResourceTextNotification(4, getBaseContext()),
                                     mediaPendingIntent3)
                             .addAction(stop ? ThemeUtils.getResourceDrawableNotification(3)
                                             : ThemeUtils.getResourceDrawableNotification(2),
                                     stop ? ThemeUtils.getResourceTextNotification(
                                                    3, getBaseContext())
                                          : ThemeUtils.getResourceTextNotification(
                                                    2, getBaseContext()),
                                     mediaPendingIntent1)
                             .addAction(ThemeUtils.getResourceDrawableNotification(1),
                                     ThemeUtils.getResourceTextNotification(1, getBaseContext()),
                                     mediaPendingIntent2)

                             //.setStyle(style)

                             .setStyle(new Notification.BigPictureStyle().bigPicture(tmp))

                             .build();

            status.icon = stop ? R.drawable.null_icon : R.drawable.now_playing_holo_dark;
            status.contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent("com.luca89.thundermusic.PLAYBACK_VIEWER")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    0);
            if (!stop) {
                startForeground(PLAYBACKSERVICE_STATUS, status);
            } else {
                stopForeground(true);
                mnotif = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mnotif.notify(PLAYBACKSERVICE_STATUS, status);
            }
            mIsSupposedToBePlaying = !stop;
        }
    }

    public void startPopup() {
        if (!MusicUtils.checkSystemAlertWindowPermission(this)) {
            return;
        }
        if (open == false) {
            open = true;
            windowManager = (WindowManager) getSystemService(Service.WINDOW_SERVICE);

            chatHead = new FrameLayout(this);
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 100;

            windowManager.addView(chatHead, params);

            LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

            View view = vi.inflate(R.layout.audio_player_popup, null);
            chatHead.addView(view, 0,
                    new ViewGroup.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT));
            mPlay = (ImageButton) chatHead.findViewById(R.id.control_play);
            mPrev = (ImageButton) chatHead.findViewById(R.id.control_prev);
            mNext = (ImageButton) chatHead.findViewById(R.id.control_next);
            mClose = (ImageButton) chatHead.findViewById(R.id.control_close);
            mAlbum = (ImageView) chatHead.findViewById(R.id.albumart);
            mWidget = (RelativeLayout) chatHead.findViewById(R.id.widget);
            title = (TextView) chatHead.findViewById(R.id.title);
            album = (TextView) chatHead.findViewById(R.id.artist);

            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            mClose.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mWidget.setVisibility(View.GONE);
                    closePopup();
                }
            });
            mPlay.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (isPlaying()) {
                        pause();
                        setPauseButtonImage();
                    } else {
                        play();
                        setPauseButtonImage();
                    }
                }
            });
            mPrev.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (position() < 2000) {
                        prev();
                        setPauseButtonImage();
                    } else {
                        seek(0);
                        play();
                        setPauseButtonImage();
                    }
                }
            });
            mNext.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    gotoNext(true);
                    setPauseButtonImage();
                }
            });
            mWidget.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mWidget.setVisibility(View.GONE);
                    startActivity(new Intent("com.luca89.thundermusic.PLAYBACK_VIEWER")
                                          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            });
            chatHead.setOnTouchListener(new View.OnTouchListener() {
                private WindowManager.LayoutParams paramsF = params;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            // Get current time in nano seconds.
                            initialX = paramsF.x;
                            initialY = paramsF.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            handler.postDelayed(mLongPressed, 500);
                            break;
                        case MotionEvent.ACTION_UP:
                            handler.removeCallbacks(mLongPressed);
                            if (!moved) {
                                if (mWidget.getVisibility() == View.GONE)
                                    mWidget.setVisibility(View.VISIBLE);
                                else
                                    mWidget.setVisibility(View.GONE);
                            }
                            moved = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (moved == true) {
                                paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                                paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                                windowManager.updateViewLayout(chatHead, paramsF);
                            }
                            break;
                    }
                    return false;
                }
            });
            setPauseButtonImage();
            updateMeta(this);
            if (!MusicUtils.startupPopup(this)) {
                Toast.makeText(this, R.string.go_advanced_popup, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void closePopup() {
        if (chatHead != null && open == true) windowManager.removeView(chatHead);
        open = false;
    }

    private void updateMeta(Context context) {
        album.setText(getAlbumName());
        title.setText(getTrackName());

        Bitmap bm = ImageUtils.getArtworkQuick1(context.getApplicationContext(), getAlbumId());
        if (bm == null) {
            bm = ImageUtils.getDefaultArtwork(getBaseContext()).getBitmap();
        }
        mAlbum.setImageBitmap(bm);
    }

    private void setPauseButtonImage() {
        if (isPlaying()) {
            mPlay.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            mPlay.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    // A simple variation of Random that makes sure that the
    // value it returns is not equal to the value it returned
    // previously, unless the interval is 1.
    private static class Shuffler {
        private int mPrevious;
        private Random mRandom = new Random();

        public int nextInt(int interval) {
            int ret;
            do {
                ret = mRandom.nextInt(interval);
            } while (ret == mPrevious && interval > 1);
            mPrevious = ret;
            return ret;
        }
    }

    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still has
     * a remote reference to the stub.
     */
    static class ServiceStub extends IMediaPlaybackService.Stub {
        WeakReference<MediaPlaybackService> mService;

        ServiceStub(MediaPlaybackService service) {
            mService = new WeakReference<MediaPlaybackService>(service);
        }

        public int getBufferState() {
            return mService.get().getBufferState();
        }

        public void exit() {
            mService.get().exit();
        }

        public void openOnline(String url) {
            mService.get().openOnline(url);
        }

        public void setOnlineLibrary(String name, int position) {
            mService.get().setOnlineLibrary(name, position);
        }

        public long[] getQueue() {
            return mService.get().getQueue();
        }

        public int getQueuePosition() {
            return mService.get().getQueuePosition();
        }

        public void setQueuePosition(int index) {
            mService.get().setQueuePosition(index);
        }

        public boolean isPlaying() {
            return mService.get().isPlaying();
        }

        public void stop() {
            mService.get().stop();
        }

        public void pause() {
            mService.get().pause();
        }

        public void play() {
            mService.get().play();
        }

        public void prev() {
            mService.get().prev();
        }

        public void next() {
            mService.get().gotoNext(true);
        }

        public void cycleRepeat() {
            mService.get().cycleRepeat();
        }

        public void toggleShuffle() {
            mService.get().toggleShuffle();
        }

        public String getTrackName() {
            return mService.get().getTrackName();
        }

        public String getAlbumName() {
            return mService.get().getAlbumName();
        }

        public long getAlbumId() {
            return mService.get().getAlbumId();
        }

        public String getArtistName() {
            return mService.get().getArtistName();
        }

        public String getPath() {
            return mService.get().getPath();
        }

        public String getAudioUrl() {
            return mService.get().getAudioUrl();
        }

        public long getAudioId() {
            return mService.get().getAudioId();
        }

        public long position() {
            return mService.get().position();
        }

        public long duration() {
            return mService.get().duration();
        }

        public long seek(long pos) {
            return mService.get().seek(pos);
        }

        public int getShuffleMode() {
            return mService.get().getShuffleMode();
        }

        public void setShuffleMode(int shufflemode) {
            mService.get().setShuffleMode(shufflemode);
        }

        public int getRepeatMode() {
            return mService.get().getRepeatMode();
        }

        public void setRepeatMode(int repeatmode) {
            mService.get().setRepeatMode(repeatmode);
        }

        public int getMediaMountedCount() {
            return mService.get().getMediaMountedCount();
        }

        public int getAudioSessionId() {
            return mService.get().getAudioSessionId();
        }

        public String getTrackTrack() {
            return mService.get().getTrackTrack();
        }

        public String getTrackLink() {
            return mService.get().getTrackLink();
        }

        public int getDuration() {
            return mService.get().getDuration();
        }

        public void startPopup() {
            mService.get().startPopup();
        }

        public void closePopup() {
            mService.get().closePopup();
        }

        public void notifyChange(String what) {
            mService.get().notifyChange(what);
        }

        public void dismissAllNotifications() {
            mService.get().dismissAllNotifications();
        }
    }

    /**
     * Provides a unified interface for dealing with midi files and other media
     * files.
     */
    private class MultiPlayer {
        private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();
        private MediaPlayer mNextMediaPlayer;
        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
                    mCurrentMediaPlayer.release();
                    mCurrentMediaPlayer = mNextMediaPlayer;
                    mNextMediaPlayer = null;
                    mHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
                } else {
                    // Acquire a temporary wakelock, since when we return from
                    // this callback the MediaPlayer will release its wakelock
                    // and allow the device to go to sleep.
                    // This temporary wakelock is released when the
                    // RELEASE_WAKELOCK
                    // message is processed, but just in case, put a timeout on
                    // it.
                    mWakeLock.acquire(1000);
                    mHandler.sendEmptyMessage(TRACK_ENDED);
                    mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
                }
            }
        };
        private boolean mIsInitialized = false;
        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        mIsInitialized = false;
                        mCurrentMediaPlayer.release();
                        // Creating a new MediaPlayer and settings its wakemode does
                        // not
                        // require the media service, so it's OK to do this now,
                        // while the
                        // service is still being restarted
                        mCurrentMediaPlayer = new MediaPlayer();
                        mCurrentMediaPlayer.setWakeMode(
                                MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                        return true;
                    default:
                        Log.d("MultiPlayer", "Error: " + what + "," + extra);
                        if (what == -1) {
                            gotoNext(true);
                            break;
                        }
                }
                Log.d("MultiPlayer", "Error: " + what + "," + extra);
                return false;
            }
        };

        public MultiPlayer() {
            mCurrentMediaPlayer.setWakeMode(
                    MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }

        public void setOnInfoListener() {
            mCurrentMediaPlayer.setOnBufferingUpdateListener(
                    new MediaPlayer.OnBufferingUpdateListener() {

                        @Override
                        public void onBufferingUpdate(MediaPlayer mp, int percent) {
                            bufferPercent = (percent * 10);
                            notifyChange(UPDATE_BUFFERING);
                        }
                    });
            mCurrentMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                        notifyChange(START_BUFFERING);
                    } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                        notifyChange(END_BUFFERING);
                    }
                    return false;
                }
            });
        }

        public void setDataSource(String path) {
            try {
                mOnlineDuration = 0;
                mCurrentMediaPlayer.reset();
                if (path.startsWith("content://")) {
                    mCurrentMediaPlayer.setDataSource(MediaPlaybackService.this, Uri.parse(path));
                } else {
                    mCurrentMediaPlayer.setDataSource(path);
                }
                mCurrentMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                mCurrentMediaPlayer.prepareAsync();
                mCurrentMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mOnlineDuration = mCurrentMediaPlayer.getDuration();
                        mp.setOnCompletionListener(listener);
                        mp.setOnErrorListener(errorListener);
                        setOnInfoListener();

                        Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
                        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                        sendBroadcast(i);
                        mIsInitialized = true;
                        notifyChange(META_CHANGED);
                        notifyChange(END_BUFFERING);
                    }
                });
            } catch (IOException ex) {
                mIsInitialized = false;
                return;
            }
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void start() {
            mCurrentMediaPlayer.start();
        }

        public void stop() {
            mCurrentMediaPlayer.reset();
            mIsInitialized = false;
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            mCurrentMediaPlayer.release();
        }

        public void pause() {
            if (mCurrentMediaPlayer.isPlaying()) mCurrentMediaPlayer.pause();
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        public long duration() {
            return mCurrentMediaPlayer.getDuration();
        }

        public long position() {
            return mCurrentMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            mCurrentMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public void setVolume(float vol) {
            mCurrentMediaPlayer.setVolume(vol, vol);
        }

        public int getAudioSessionId() {
            return mCurrentMediaPlayer.getAudioSessionId();
        }
    }

    /**
     * This, BandcampPlayTask and ArchiveOrgPlayTask are only an async task to
     * retrieve the correct link to play
     */
    private class ShoutCastPlayTask extends AsyncTask<Boolean, Void, String> {
        /**
         * Constructor
         */
        public ShoutCastPlayTask() {}

        @Override
        protected String doInBackground(Boolean... arg0) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            return OnlineRadioSearchTask.getURLfromPLS(mOnlineTrack.getUrl());
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.isEmpty()) {
                setVideoUrl(result);
            } else
                gotoNext(true);
        }
    }

    private class BandcampPlayTask extends AsyncTask<Boolean, Void, String> {
        private String mVideoUrl;

        /**
         * Constructor
         */
        public BandcampPlayTask(String videoUrl) {
            this.mVideoUrl = videoUrl;
        }

        @Override
        protected String doInBackground(Boolean... arg0) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            // return mOnlineTrack.getUrl();
            return BandcampLinkRetriever.getBCLink(mOnlineTrack.getLink());
        }

        @Override
        protected void onPostExecute(String result) {
            if (mVideoUrl.equalsIgnoreCase(mOnlineTrack.getLink()) && result != null
                    && !result.isEmpty()) {
                setVideoUrl(result);
            } else
                gotoNext(true);
        }
    }

    private class ArchiveOrgPlayTask extends AsyncTask<Boolean, Void, String> {
        private String mVideoId;

        /**
         * Constructor
         */
        public ArchiveOrgPlayTask(String videoId) {
            this.mVideoId = videoId;
        }

        @Override
        protected String doInBackground(Boolean... arg0) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            // return mOnlineTrack.getUrl();
            return ArchiveOrgLinkRetriever.getARLink(mOnlineTrack.getLink());
        }

        @Override
        protected void onPostExecute(String result) {
            if (mVideoId.equalsIgnoreCase(mOnlineTrack.getLink()) && result != null
                    && !result.isEmpty()) {
                setVideoUrl(result);
            } else
                gotoNext(true);
        }
    }

    private class YoutubePlayTask extends AsyncTask<Boolean, Void, String> {
        private String mVideoUrl;

        /**
         * Constructor
         */
        public YoutubePlayTask(String videoUrl) {
            this.mVideoUrl = videoUrl;
        }

        @Override
        protected String doInBackground(Boolean... arg0) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            // return mOnlineTrack.getUrl();
            return YoutubeLinkRetriever.getYtLink(mOnlineTrack.getLink());
        }

        @Override
        protected void onPostExecute(String result) {
            if (mVideoUrl.equalsIgnoreCase(mOnlineTrack.getLink()) && result != null
                    && !result.isEmpty()) {
                setVideoUrl(result);
            } else
                gotoNext(true);
        }
    }
}
