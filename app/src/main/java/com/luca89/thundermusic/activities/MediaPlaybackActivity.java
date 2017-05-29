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
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.R;
import com.luca89.thundermusic.fragments.NowplayingOnlineFragment;
import com.luca89.utils.ImageUtils;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.MusicUtils.ServiceToken;
import com.luca89.utils.ThemeUtils;
import com.luca89.utils.activities.CreatePlaylist;
import com.luca89.utils.activities.SearchPopup;
import com.luca89.utils.dataset.Library;
import com.luca89.utils.dataset.OnlineTrack;
import com.luca89.views.AnimatedLayout;
import com.melnykov.fab.FloatingActionButton;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import thundermusic.IMediaPlaybackService;

@SuppressLint({"DefaultLocale", "HandlerLeak", "InlinedApi"})
@SuppressWarnings("deprecation")
public class MediaPlaybackActivity extends Activity implements MusicUtils.Defs,
        OnSharedPreferenceChangeListener {
    private static final int ABOUT = 23;
    private static final int SEARCH_INTERNET = 20;
    private static final int SEARCH_LYRICS = 21;
    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    private static final int GET_ALBUM_ART = 3;
    private static final int ALBUM_ART_DECODED = 4;
    private static Toast mToast;
    private static boolean mIntentDeRegistered = false;
    private static AnimatedLayout mNowList;
    private static ImageView mAlbum;
    public Activity activity;
    private SharedPreferences mSettings;
    private IMediaPlaybackService mService = null;
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
    // private Drawable mActionBarBackgroundDrawable;
    private ImageButton mPrevButton;
    private ImageButton mPauseButton;
    private ImageButton mNextButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private RelativeLayout mInfoLayout;
    private Worker mAlbumArtWorker;
    private AlbumArtHandler mAlbumArtHandler;
    private ServiceToken mToken;
    private ActionBar actionBar;
    private boolean changed = false;
    private SystemBarTintManager tintManager;
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
    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private TextView mAlbumArtistName;
    private TextView mTrackNumber;
    private TextView mTrackName;
    private ProgressBar mProgress;
    private long mPosOverride = -1;
    private boolean mFromTouch = false;
    private long mDuration;

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
    private boolean paused;
    private final Handler mHandler = new Handler() {
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
                    new AlertDialog.Builder(MediaPlaybackActivity.this)
                            .setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(R.string.service_start_error_button,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int whichButton) {
                                            finish();
                                        }
                                    }).setCancelable(false).show();
                    break;

                default:
                    break;
            }
        }
    };
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

                    if (!MusicUtils.getBooleanPref(getApplicationContext(), "radiomode", false)) {
                        mRepeatButton.setVisibility(View.VISIBLE);
                        mShuffleButton.setVisibility(View.VISIBLE);
                    }
                    setRepeatButtonImage();
                    setShuffleButtonImage();
                    setPauseButtonImage();
                    return;
                }
            } catch (RemoteException ex) {
            }
            // Service is
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
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
                invalidateOptionsMenu();
                updateTrackInfo();
                setPauseButtonImage();
                queueNextRefresh(1);
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
            } else if (action.equals(MediaPlaybackService.UPDATE_BUFFERING)) {
                try {
                    if (mProgress != null)
                        if (mService != null)
                            mProgress.setSecondaryProgress(mService
                                    .getBufferState());
                } catch (RemoteException e) {

                    e.printStackTrace();
                }
            } else if (action.equals(MediaPlaybackService.START_BUFFERING)) {
                mPauseButton.setEnabled(false);
                mPauseButton.setClickable(false);
                mNextButton.setEnabled(false);
                mNextButton.setClickable(false);
                mPrevButton.setEnabled(false);
                mPrevButton.setClickable(false);
            } else if (action.equals(MediaPlaybackService.END_BUFFERING)) {
                mPauseButton.setEnabled(true);
                mPauseButton.setClickable(true);
                mNextButton.setEnabled(true);
                mNextButton.setClickable(true);
                mPrevButton.setEnabled(true);
                mPrevButton.setClickable(true);
                updateTrackInfo();
            }
        }
    };
    private BroadcastReceiver mScreenTimeoutListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if (mIntentDeRegistered) {
                    IntentFilter f = new IntentFilter();
                    f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
                    f.addAction(MediaPlaybackService.META_CHANGED);
                    f.addAction(Intent.ACTION_SCREEN_ON);
                    f.addAction(Intent.ACTION_SCREEN_OFF);
                    registerReceiver(mStatusListener, new IntentFilter(f));
                    mIntentDeRegistered = false;
                }
                updateTrackInfo();

                long next = refreshNow();
                queueNextRefresh(next);
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                paused = true;

                if (!mIntentDeRegistered) {
                    mHandler.removeMessages(REFRESH);
                    unregisterReceiver(mStatusListener);
                    mIntentDeRegistered = true;
                }
            }
        }
    };
    private View.OnClickListener mFABlistener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mNowList.getVisibility() == View.GONE)
                mNowList.setVisibility(View.VISIBLE, true, R.anim.fade_in);
            else
                mNowList.setVisibility(View.GONE, true, R.anim.fade_out);
        }
    };

    /**
     * Called when the activity is first created.
     */

    public MediaPlaybackActivity() {
    }

    @Override
    public void onBackPressed() {
        if (mNowList.getVisibility() == View.VISIBLE)
            mNowList.setVisibility(View.GONE, true, R.anim.fade_out);
        else
            super.onBackPressed();
    }

    @TargetApi(19)
    @Override
    public void onCreate(Bundle icicle) {

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings.registerOnSharedPreferenceChangeListener(this);
        ThemeUtils.getAppThemeExpanded(this, new ColorDrawable());

        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mAlbumArtWorker = new Worker("album art worker");
        mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());
        setContentView(R.layout.audio_player);

        mNowList = (AnimatedLayout) findViewById(R.id.playlist);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);

            if (ThemeUtils.getAppTheme3(this) < 6)
                tintManager.setTintColor(getResources().getColor(
                        ThemeUtils.getThemeColor(this,
                                ThemeUtils.getAppTheme3(this))));
            else
                tintManager.setTintColor(ThemeUtils.getThemeColor(this,
                        ThemeUtils.getAppTheme3(this)));
            tintManager.setStatusBarAlpha(0);
            mNowList.setPadding(0, tintManager.getConfig().getStatusBarHeight()
                    + tintManager.getConfig().getActionBarHeight(), 0, 0);

        }

        if (mNowList.getChildCount() == 0)
            setNowDrawer();
        ThemeUtils.getNowDrawerTheme(this, mNowList);

        activity = this;
        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class
                    .getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        if (mSettings.getBoolean("stay_on", false)) {
            getWindow()
                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mTotalTime = (TextView) findViewById(R.id.totaltime);
        mProgress = (ProgressBar) findViewById(android.R.id.progress);
        mAlbum = (ImageView) findViewById(R.id.album);
        mInfoLayout = (RelativeLayout) findViewById(R.id.discrete_infos);
        mAlbumArtistName = (TextView) findViewById(R.id.album_artist_name);
        mTrackNumber = (TextView) findViewById(R.id.number_track);
        mTrackName = (TextView) findViewById(R.id.trackname);
        mPrevButton = (ImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(mPrevListener);
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        mNextButton = (ImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);

        ThemeUtils.getBarTheme(this, mInfoLayout);

        mShuffleButton = ((ImageButton) findViewById(R.id.shuffle));
        mShuffleButton.setOnClickListener(mShuffleListener);
        mRepeatButton = ((ImageButton) findViewById(R.id.repeat));
        mRepeatButton.setOnClickListener(mRepeatListener);

        mAlbumArtistName.setTextColor(ThemeUtils.getTextColor(this));
        mTrackName.setTextColor(ThemeUtils.getTextColor(this));
        mTrackNumber.setTextColor(ThemeUtils.getTextColor(this));
        mCurrentTime.setTextColor(ThemeUtils.getTextColor(this));
        mTotalTime.setTextColor(ThemeUtils.getTextColor(this));


        InterfaceUtils.setUpFAB(this,
                R.drawable.music_playlist_holo_dark, (FloatingActionButton) findViewById(R.id.FAB), mFABlistener);
        findViewById(R.id.FAB).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!MusicUtils.getBooleanPref(activity, "radiomode", false)) {
                    Intent intent = new Intent(activity, TrackOnlinePlaylistBrowser.class);
                    intent.putExtra("playlist", "queue");
                    intent.putExtra("called", false);
                    startActivity(intent);
                }
                return false;
            }
        });

        if (ThemeUtils.getAppTheme2(this) == 3) {
            mPrevButton.setImageResource(R.drawable.btn_playback_previous_black);
            mNextButton.setImageResource(R.drawable.btn_playback_next_black);
        } else {
            mPrevButton.setImageResource(R.drawable.btn_playback_previous);
            mNextButton.setImageResource(R.drawable.btn_playback_next);
        }

        mNowList.setPadding(0, 0, 0, 0);

        if (mProgress instanceof SeekBar) {
            SeekBar seeker = (SeekBar) mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
            seeker.setThumb(ThemeUtils.colorizeDrawable(getResources().getDrawable(R.drawable.thumb_seek), this));
            seeker.setProgressDrawable(ThemeUtils.colorizeDrawable(
                    seeker.getProgressDrawable(), this));

            if (!MusicUtils.getBooleanPref(this, "radiomode", false)) {
                seeker.setEnabled(true);
            } else {
                seeker.setEnabled(false);
            }
        }

        mProgress.setMax(1000);
        if (MusicUtils.getBooleanPref(this, "radiomode", false)) {
            mProgress.setVisibility(View.GONE);
            mCurrentTime.setVisibility(View.GONE);
            mTotalTime.setVisibility(View.GONE);
            mRepeatButton.setVisibility(View.GONE);
            mShuffleButton.setVisibility(View.GONE);
        }
    }

    public boolean SearchInternet() {

        if (checkPackage()) {
            Intent intent = new Intent("android.intent.action.SEARCH");
            intent.setPackage("com.google.android.youtube");
            try {
                intent.putExtra("query", mService.getTrackName());
            } catch (RemoteException remoteexception1) {
                remoteexception1.printStackTrace();
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } else {
            String site = "http://youtube.com/results?search_query=";
            String site2 = "&submit=Search";
            String song;
            try {
                song = mService.getTrackName();
                song = song.replace(" ", "+");
                song = song.replace("'", "");
                song = song.replace("!", "%21");
                song = song.replace("`", "%60");
                URL url = null;
                try {
                    url = new URL(String.format(site, song));
                } catch (MalformedURLException e) {

                    e.printStackTrace();
                }
                String urlString = url.toString() + song + site2;
                try {
                    url = new URL(String.format(urlString));
                } catch (MalformedURLException e) {

                    e.printStackTrace();
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()));
                startActivity(intent);
            } catch (RemoteException e) {

                e.printStackTrace();
            }
            return true;
        }
    }

    private boolean checkPackage() {
        boolean flag1 = false;
        Iterator iterator = getPackageManager().getInstalledApplications(0).iterator();
        boolean flag;
        do {
            flag = flag1;
            if (!iterator.hasNext()) {
                break;
            }
            if (!((ApplicationInfo) iterator.next()).packageName.equals("com.google.android.youtube")) {
                continue;
            }
            flag = true;
            break;
        } while (true);
        return flag;
    }

    @Override
    public void onStop() {
        paused = true;
        if (!mIntentDeRegistered) {
            mHandler.removeMessages(REFRESH);
            unregisterReceiver(mStatusListener);
        }
        unregisterReceiver(mScreenTimeoutListener);
        MusicUtils.unbindFromService(mToken);
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
        s.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mScreenTimeoutListener, new IntentFilter(s));

        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTrackInfo();
        if (changed == true) {
            changed = false;
            recreate();
        }

        if (mSettings.getBoolean("stay_on", false)) {
            getWindow()
                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (mIntentDeRegistered) {
            paused = false;
        }
        setPauseButtonImage();
    }

    @Override
    public void onDestroy() {
        if (mAlbumArtWorker != null)
            mAlbumArtWorker.quit();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.starred, menu);

        MenuInflater inflater2 = getMenuInflater();
        inflater2.inflate(R.menu.share, menu);

        Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        if (getPackageManager().resolveActivity(i, 0) != null) {
            MenuInflater inflater1 = getMenuInflater();
            inflater1.inflate(R.menu.equalizer, menu);
        }

        MenuInflater inflater3 = getMenuInflater();
        inflater3.inflate(R.menu.settings, menu);
        if (!MusicUtils.getBooleanPref(this, "radiomode", false)) {
            SubMenu menu1 = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0,
                    R.string.add_to_playlist);
            if (!MusicUtils.getBooleanPref(this, "radiomode", false)) {
                MusicUtils.makePlaylistMenuOnline(this, menu1);
            }
            menu.add(1, SEARCH_LYRICS, 0, R.string.search_lyrics_menu_short);
        }
        menu.add(1, ABOUT + 2, 0, R.string.carmode_menu_short);

        if (mSettings.getBoolean(PreferencesActivity.POPUP_ON, false))
            menu.add(1, ABOUT + 1, 0, R.string.go_popup);
        menu.add(1, ABOUT, 0, R.string.about_menu_short);
        menu.add(1, EXIT, 0, R.string.exit_menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        try {
            switch (item.getItemId()) {

                case android.R.id.home:
                    finish();
                    break;
                case R.id.menu_starred:
                    ContentResolver resolver = getContentResolver();
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Playlists.NAME,
                            getString(R.string.playlist_favourites));
                    values.put(
                            MediaStore.Audio.Playlists._ID,
                            MusicUtils
                                    .getPlaylistiD(getString(R.string.playlist_favourites)) - 1);
                    resolver.insert(
                            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);

                    ContentValues values2 = new ContentValues(3);
                    values2.put(MediaStore.Audio.Playlists.NAME,
                            getString(R.string.playlist_favouritesstations));
                    values2.put(
                            MediaStore.Audio.Playlists._ID,
                            MusicUtils
                                    .getPlaylistiD(getString(R.string.playlist_favouritesstations)));
                    resolver.insert(
                            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                            values2);

                    ContentValues values3 = new ContentValues(3);
                    values3.put(MediaStore.Audio.Playlists.NAME,
                            getString(R.string.playlist_favouritesonline));
                    values3.put(
                            MediaStore.Audio.Playlists._ID,
                            MusicUtils
                                    .getPlaylistiD(getString(R.string.playlist_favouritesonline)));
                    resolver.insert(
                            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                            values3);
                    try {
                        MusicUtils
                                .readAdapter(
                                        this,
                                        getString(R.string.playlist_favouritesstations));

                    } catch (IOException e) {

                        List<OnlineTrack> videos = new ArrayList<OnlineTrack>();
                        MusicUtils
                                .writeAdapter(
                                        this,
                                        new Library(
                                                getString(R.string.playlist_favouritesstations),
                                                videos, null),
                                        getString(R.string.playlist_favouritesstations));
                    }
                    try {
                        MusicUtils
                                .readAdapter(
                                        this,
                                        getString(R.string.playlist_favouritesonline));

                    } catch (IOException e) {

                        List<OnlineTrack> videos = new ArrayList<OnlineTrack>();
                        MusicUtils
                                .writeAdapter(
                                        this,
                                        new Library(
                                                getString(R.string.playlist_favouritesonline),
                                                videos, null),
                                        getString(R.string.playlist_favouritesonline));
                    }
                    if (MusicUtils.getBooleanPref(this, "radiomode", false)) {

                        Library mLib = MusicUtils.readAdapter(this, "queue");
                        MusicUtils
                                .addToExistingPlaylist(
                                        this,
                                        getString(R.string.playlist_favouritesstations),
                                        mLib.getVideos().get(
                                                mService.getQueuePosition()), 0);
                    } else {
                        Library mLib = MusicUtils.readAdapter(this, "queue");
                        MusicUtils
                                .addToExistingPlaylist(
                                        this,
                                        getString(R.string.playlist_favouritesonline),
                                        mLib.getVideos().get(
                                                mService.getQueuePosition()), 0);
                    }
                    break;

                case ABOUT:
                    InterfaceUtils.showAbout(this);
                    break;

                case ABOUT + 2:
                    Intent carmode = new Intent(activity,
                            MediaPlaybackCarModeActivity.class);
                    carmode.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(carmode);
                    break;

                case ABOUT + 1:
                    mService.startPopup();
                    break;

                case SEARCH_INTERNET:
                    SearchInternet();
                    break;

                case SEARCH_LYRICS:
                    // SearchInternet();
                    Intent intent21 = new Intent();
                    intent21.setClass(this, SearchPopup.class);
                    intent21.putExtra("artwork", false);
                    intent21.putExtra("song", mService.getTrackName());
                    intent21.putExtra("artist", mService.getArtistName());
                    intent21.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    this.startActivity(intent21);
                    break;

                case EXIT:
                    mService.pause();
                    mService.exit();
                    finish();

                    break;

                case PLAYLIST_SELECTED:
                    Library mLib1 = MusicUtils.readAdapter(this, "queue");
                    String playlist = item.getIntent().getStringExtra("name");
                    MusicUtils.addToExistingPlaylist(this, playlist, mLib1
                            .getVideos().get(mService.getQueuePosition()), 0);
                    return true;

                case QUEUE:
                    Library mLib2 = MusicUtils.readAdapter(this, "queue");
                    MusicUtils.addToExistingPlaylist(this, "queue", mLib2
                            .getVideos().get(mService.getQueuePosition()));
                    return true;

                case NEW_PLAYLIST:
                    Library mLib3 = MusicUtils.readAdapter(this, "queue");
                    Bundle data = new Bundle();
                    data.putSerializable("TRACK",
                            mLib3.getVideos().get(mService.getQueuePosition()));
                    Intent intent9 = new Intent(this, CreatePlaylist.class);
                    intent9.putExtra("online", true);
                    intent9.putExtra("track", data);
                    startActivity(intent9);
                    return true;

                case R.id.menu_share:
                    String share;
                    Intent sharingIntent = new Intent(
                            Intent.ACTION_SEND);

                    String artist = mService.getArtistName();
                    share = getResources().getString(R.string.share_string);
                    if (artist == null
                            || artist.equals(MediaStore.UNKNOWN_STRING)) {
                        share = share + "\n" + "#" + mService.getTrackName().replace(" ", "")
                                + "\n#" + mService.getAlbumName().replace(" ", "");
                    } else {
                        share = share + "\n" + "#" + artist.replace(" ", "")
                                + "\n#" + mService.getTrackName().replace(" ", "");
                    }
                    share = share + "\non #ThunderMusic" + " :";
                    share = share + "\n" + getResources().getString(R.string.playstore_string);

                    share = share + "\nTrack :";
                    share = share + "\n" + mService.getTrackLink();

                    sharingIntent.setType("text/plain");
                    // Add data to the intent, the receiving app will decide
                    // what to do with it.
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, mService.getTrackName());
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, share);
                    startActivity(Intent.createChooser(sharingIntent, "Share"));
                    break;

                case R.id.menu_settings:
                    startActivity(new Intent(this, PreferencesActivity.class));
                    return true;

                case R.id.menu_equalizer:
                    Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                    i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                            mService.getAudioSessionId());
                    startActivityForResult(i, EFFECTS_PANEL);
                    return true;
            }
        } catch (RemoteException ex) {
        } catch (IllegalStateException e) {

            e.printStackTrace();
        } catch (ClassNotFoundException e1) {

            e1.printStackTrace();
        } catch (IOException e1) {

            e1.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }

    private void doPauseResume() {
        try {
            if (mService != null) {
                if (mService.isPlaying()) {
                    mService.pause();
                } else {
                    mService.play();
                }
                /*
                 * refreshNow(); setPauseButtonImage();
	 */
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
            } else {
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
                    mRepeatButton.setImageDrawable(ThemeUtils.colorizeResourceDrawable(R.drawable.ic_mp_repeat_off_btn, this));

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

                    mShuffleButton.setImageDrawable(ThemeUtils.colorizeResourceDrawable(R.drawable.ic_mp_shuffle_off_btn, this));

                    break;
                default:

                    mShuffleButton.setImageDrawable(ThemeUtils.colorizeDrawable(
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
                if (ThemeUtils.getAppTheme2(this) == 3) {
                    mPauseButton.setImageResource(R.drawable.btn_playback_pause_black);
                } else {
                    mPauseButton.setImageResource(R.drawable.btn_playback_pause);
                }
            } else {
                if (ThemeUtils.getAppTheme2(this) == 3) {
                    mPauseButton.setImageResource(R.drawable.btn_playback_play_black);
                } else {
                    mPauseButton.setImageResource(R.drawable.btn_playback_play);
                }
            }
        } catch (RemoteException ex) {
        }
    }

    private void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        if (mService == null)
            return 500;
        try {
            long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(MusicUtils
                        .makeTimeString(this, pos / 1000));
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
            mAlbumArtistName.setText(artistName + " - " + albumName);
            actionBar.setTitle(R.string.nowplaying_title);
            mTrackName.setText(mService.getTrackName());
            mTrackNumber.setText((Integer.toString(mService.getQueuePosition() + 1)) + " / " + Integer.toString(mService.getQueue().length));
            mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
            mAlbumArtHandler.obtainMessage(GET_ALBUM_ART,
                    new AlbumSongIdWrapper(albumid)).sendToTarget();
            mAlbum.setVisibility(View.VISIBLE);

            mDuration = mService.getDuration();
            mTotalTime.setText(" / " + MusicUtils
                    .makeTimeString(this, mDuration / 1000));

        } catch (RemoteException ex) {
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        if (key.equals(PreferencesActivity.KEY_TITLE)
                || key.equals(PreferencesActivity.KEY_THEME)
                || key.equals(PreferencesActivity.KEY_START_SCREEN)
                || key.equals(PreferencesActivity.GRID_MODE)
                || key.equals(PreferencesActivity.KEY_ENABLE_FLIP)
                || key.equals(PreferencesActivity.KEY_NOW_PLAYING_EXPANDED)
                || key.equals(PreferencesActivity.KEY_CUSTOM_THEME)
                || key.equals("radiomode")) {
            changed = true;
        }
    }

    private void setNowDrawer() {
        mNowList.removeAllViews();
        Fragment frag;
        frag = new NowplayingOnlineFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(mNowList.getId(), frag).commit();
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
                Drawable bm = ImageUtils.getArtwork(activity, albumid);
                if (bm == null) {
                    bm = ImageUtils.getArtwork(activity, -1);
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
