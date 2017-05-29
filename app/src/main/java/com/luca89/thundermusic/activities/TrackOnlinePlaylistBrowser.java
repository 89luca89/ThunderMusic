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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.luca89.adapters.VideosAdapter;
import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.R;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.ThemeUtils;
import com.luca89.utils.activities.CreatePlaylist;
import com.luca89.utils.dataset.Library;
import com.luca89.views.TouchInterceptor;
import com.melnykov.fab.FloatingActionButton;

import java.io.IOException;
import java.util.Collections;

public class TrackOnlinePlaylistBrowser extends Activity implements
        MusicUtils.Defs, OnSharedPreferenceChangeListener, ServiceConnection {
    // A reference to our list that will hold the video details
    private static final int ABOUT = 0;
    private static final int SEARCH = CHILD_MENU_BASE + 6;
    private static int mSelectedPosition;

    private static ListView listView;
    private static Library mLib;
    private static Activity activity;
    private static String mPlaylist;
    private static boolean mCenter;
    private MusicUtils.ServiceToken mToken;
    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {

            ((VideosAdapter) listView.getAdapter()).notifyDataSetChanged();
            Collections.swap(mLib.getVideos(), from, to);
            // Update the service only if manipulating the queue!
            if (mPlaylist.equalsIgnoreCase("queue")) {
                try {
                    MusicUtils.writeAdapter(activity, mLib, "queue");
                    MusicUtils.sService.setOnlineLibrary("queue", -1);
                } catch (IOException e) {
                    // 
                    e.printStackTrace();
                } catch (RemoteException e) {
                    // 
                    e.printStackTrace();
                }
            }
            try {
                MusicUtils.writeAdapter(activity, mLib, mPlaylist);
            } catch (IOException e) {
                // 
                e.printStackTrace();
            }
            listView.invalidate();
        }
    };
    private TouchInterceptor.RemoveListener mRemoveListener = new TouchInterceptor.RemoveListener() {
        public void remove(int which) {
            ((VideosAdapter) listView.getAdapter()).notifyDataSetChanged();
            mLib.getVideos().remove(which);
            // Update the service only if manipulating the queue!
            if (mPlaylist.equalsIgnoreCase("queue")) {
                try {
                    MusicUtils.writeAdapter(activity, mLib, "queue");
                    MusicUtils.sService.setOnlineLibrary("queue", -1);
                } catch (IOException e) {
                    // 
                    e.printStackTrace();
                } catch (RemoteException e) {
                    // 
                    e.printStackTrace();
                }
            }
            try {
                MusicUtils.writeAdapter(activity, mLib, mPlaylist);
            } catch (IOException e) {
                // 
                e.printStackTrace();
            }
            listView.invalidate();
        }
    };
    private TextView mTxt;
    private boolean changed = false;
    private boolean called;
    private View.OnClickListener mFABlistener = new View.OnClickListener() {
        public void onClick(View v) {
            MusicUtils.execute(false, new clickerTask(0), true);
        }
    };
    private BroadcastReceiver mNowPlayingListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            listView.invalidateViews();
            try {
                if (MusicUtils.sService != null)
                    if (MusicUtils.sService.getTrackTrack() != null)
                        listView.setSelection(Integer
                                .parseInt(MusicUtils.sService.getTrackTrack()));
            } catch (RemoteException e) {
                // 
            }

        }
    };

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * This method retrieves the Library of videos from the task and passes them
     * to our ListView
     */
    private static void populateListWithVideos(Activity activity, Context context) {

        Library tmp;
        try {
            tmp = MusicUtils.readAdapter(context, mPlaylist);
            if (tmp != null) {
                mLib = tmp;
                VideosAdapter adapter = new VideosAdapter(activity,
                        tmp.getVideos(), true, "");
                listView.setAdapter(adapter);
            }
        } catch (ClassNotFoundException e) {
            // 
            e.printStackTrace();
        } catch (IOException e) {
            // 
            e.printStackTrace();
        }
    }

    public static void showUpPopup(final Activity context, View v,
                                   final int position) {
        PopupMenu popup = new PopupMenu(context, v);

        Menu menu = popup.getMenu();
        /** Adding menu items to the popumenu */

        mSelectedPosition = position;
        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0,
                R.string.add_to_playlist);
        MusicUtils.makePlaylistMenuOnline(context, sub);
        menu.add(0, SEARCH, 0, R.string.search_internet_menu_short);
        menu.add(0, DELETE_ITEM, 0, R.string.remove_from_playlist);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case QUEUE:
                        MusicUtils.addToExistingPlaylist(context, "queue", mLib
                                .getVideos().get(mSelectedPosition));
                        return true;
                    case NEW_PLAYLIST:

                        Bundle data = new Bundle();
                        data.putSerializable("TRACK",
                                mLib.getVideos().get(mSelectedPosition));
                        Intent intent = new Intent(context, CreatePlaylist.class);
                        intent.putExtra("online", true);
                        intent.putExtra("track", data);
                        context.startActivity(intent);

                        return true;
                    case PLAYLIST_SELECTED:
                        String playlist = item.getIntent().getStringExtra("name");
                        MusicUtils.addToExistingPlaylist(context, playlist, mLib
                                .getVideos().get(mSelectedPosition));
                        return true;
                    case DELETE_ITEM:
                        MusicUtils.removeFromPlaylist(mPlaylist, position, context);
                        populateListWithVideos(activity, activity);
                        return true;
                }
                return true;
            }
        });
        /** Showing the popup menu */
        popup.show();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        Intent intent = getIntent();
        if (savedInstanceState != null) {
            mPlaylist = savedInstanceState.getString("playlist");
            called = savedInstanceState.getBoolean("called");
            mCenter = savedInstanceState.getBoolean("center");
        } else {
            mPlaylist = intent.getStringExtra("playlist");
            called = intent.getBooleanExtra("called", true);
            mCenter = intent.getBooleanExtra("center", false);
        }

        getWindow().setUiOptions(
                ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        if (!called)
            ThemeUtils.getAppTheme(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.media_picker_activity_playlist);
        if (!called) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
            getActionBar().setDisplayShowHomeEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
            // Initialize part of the UI only if we are not in the
            // Nowplaying activity
            if (!mPlaylist.equalsIgnoreCase("queue")) {
                getActionBar().setTitle(mPlaylist);

                InterfaceUtils.setUpFAB(this,
                        R.drawable.music_playall_holo_dark, (FloatingActionButton) findViewById(R.id.FAB), mFABlistener);
            } else
                getActionBar().setTitle(R.string.nowplaying_title);

        }

        activity = this;
        mTxt = (TextView) findViewById(R.id.nointernet);
        listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {

                if (!isNetworkAvailable(getBaseContext())) {
                    listView.setEnabled(false);
                    mTxt.setVisibility(View.VISIBLE);
                } else
                    MusicUtils.execute(false, new clickerTask(arg2), true);

            }
        });
        if (!isNetworkAvailable(this)) {
            listView.setEnabled(false);
            mTxt.setVisibility(View.VISIBLE);
        }

        if (!called)
            ((TouchInterceptor) listView).setDropListener(mDropListener);
        ((TouchInterceptor) listView).setRemoveListener(mRemoveListener);

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        registerReceiver(mNowPlayingListener, new IntentFilter(f));
        mNowPlayingListener.onReceive(this, new Intent(
                MediaPlaybackService.META_CHANGED));
        mToken = MusicUtils.bindToService(this, this);

    }

    @Override
    protected void onDestroy() {
        // Make sure we null our handler when the activity has stopped
        // because who cares if we get a callback once the activity has stopped?
        // not me!
        MusicUtils.unbindFromService(mToken);
        unregisterReceiver(mNowPlayingListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        // 
        ((TouchInterceptor) listView).setCenterGravity(mCenter);
        if (changed == true && !called) {
            changed = false;
            recreate();
        }
        populateListWithVideos(activity, activity);
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("playlist", mPlaylist);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater2 = getMenuInflater();
        inflater2.inflate(R.menu.play, menu);

        MenuInflater inflater7 = getMenuInflater();
        inflater7.inflate(R.menu.save, menu);

        MenuInflater inflater4 = getMenuInflater();
        inflater4.inflate(R.menu.discard, menu);
        MenuInflater inflater6 = getMenuInflater();
        inflater6.inflate(R.menu.settings, menu);
        menu.add(1, ABOUT, 0, R.string.about_menu_short);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                finish();
                return true;

            case ABOUT:
                InterfaceUtils.showAbout(this);
                break;

            case R.id.menu_save:

                Bundle data = new Bundle();
                data.putSerializable("LIBRARY", mLib);
                Intent intent = new Intent(this, CreatePlaylist.class);
                intent.putExtra("online", true);
                intent.putExtra("track", data);
                startActivity(intent);
                break;

            case R.id.menu_play:
                Intent intent7 = new Intent();
                intent7.setClass(this, MediaPlaybackActivity.class);
                intent7.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent7);
                break;

            case R.id.menu_discard:
                // We only clear the current playlist
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        MusicUtils.getPlaylistiD(mPlaylist));
                getContentResolver().delete(uri, null, null);
                MusicUtils.deleteOnlinePlaylist(this, mPlaylist);
                finish();
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // 
        if (key.equals(PreferencesActivity.KEY_TITLE)
                || key.equals(PreferencesActivity.KEY_THEME)
                || key.equals(PreferencesActivity.KEY_START_SCREEN)
                || key.equals(PreferencesActivity.GRID_MODE))
            changed = true;

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        finish();
    }

    class clickerTask extends AsyncTask<Boolean, Integer, Void> {

        int arg2;

        /**
         * Constructor of <code>FetchLyrics</code>
         */
        public clickerTask(int click) {
            arg2 = click;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Void doInBackground(Boolean... params) {
            try {
                MusicUtils.writeAdapter(activity, mLib, "queue");
                MusicUtils.sService.setOnlineLibrary("queue", arg2);
                MusicUtils.sService.openOnline(mLib.getVideos().get(arg2)
                        .getUrl());
            } catch (RemoteException e) {
                // 
                e.printStackTrace();
            } catch (IOException e) {
                // 
                e.printStackTrace();
            }
            if (!InterfaceUtils.getTabletMode(activity)) {
                Intent intent = new Intent(
                        "com.luca89.thundermusic.PLAYBACK_VIEWER")
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(intent);
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(final Void result) {
        }

    }
}
