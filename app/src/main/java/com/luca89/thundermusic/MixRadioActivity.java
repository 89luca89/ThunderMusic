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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.luca89.adapters.ShoutcastRadioAdapter;
import com.luca89.service.search.OnlineRadioSearchTask;
import com.luca89.thundermusic.activities.PreferencesActivity;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicBarUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.ThemeUtils;
import com.luca89.utils.dataset.Library;
import com.luca89.utils.dataset.OnlineTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class MixRadioActivity extends Activity implements ServiceConnection,
        OnSharedPreferenceChangeListener, ActionBar.OnNavigationListener {
    public static boolean changed = false;
    private static MusicUtils.ServiceToken mToken;
    private static GridView lv;
    private static String[] array;
    private static Activity activity;
    private static AutoCompleteTextView mEditTitle;
    private static ProgressDialog mProgressDialog;
    private static RelativeLayout nowPlayingView;
    private Set<String> mSuggestionAdapter;
    private radioTask mTask;
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicBarUtils.updateMusicBar(MixRadioActivity.this,
                    nowPlayingView);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (mToken == null) {
            mToken = MusicUtils.bindToService(this, this);
        }
        getActionBar().setSelectedNavigationItem(1);
        activity = this;

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        registerReceiver(mTrackListListener, new IntentFilter(f));
        MusicBarUtils.updateMusicBar(this, nowPlayingView);
        if (changed == true) {
            changed = false;
            recreate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater6 = getMenuInflater();
        inflater6.inflate(R.menu.settings, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // 
        MusicBarUtils.updateMusicBar(this, nowPlayingView);

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // 
        finish();
    }

    @Override
    protected void onDestroy() {
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
        }
        unregisterReceiver(mTrackListListener);
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle icicle) {
        MusicUtils.setIntPref(this, "activesection", 2);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        ThemeUtils.getAppTheme(this);
        super.onCreate(icicle);
        activity = this;

        setContentView(R.layout.media_picker_activity_list);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mToken = MusicUtils.bindToService(this, this);

        if (InterfaceUtils.getTabletMode(this))
            nowPlayingView = (RelativeLayout) findViewById(R.id.nowplaying_actionbar_tablet);
        else
            nowPlayingView = (RelativeLayout) findViewById(R.id.nowplaying_actionbar);

        lv = (GridView) findViewById(android.R.id.list);
        lv.setFastScrollEnabled(false);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    final int arg2, long arg3) {
                if (isNetworkAvailable()) {
                    // 

                    if (arg2 == 0) { // SEARCH
                        final Dialog dialog = new Dialog(activity);
                        dialog.setContentView(R.layout.search_layout);
                        dialog.findViewById(R.id.editTextAlbum).setVisibility(
                                View.GONE);
                        dialog.findViewById(R.id.editTextArtist).setVisibility(
                                View.GONE);
                        dialog.findViewById(R.id.editTextTitle).setVisibility(
                                View.GONE);
                        dialog.setTitle("");
                        mEditTitle = (AutoCompleteTextView) dialog
                                .findViewById(R.id.editTextRadio);
                        mEditTitle.setVisibility(View.VISIBLE);
                        mEditTitle.setHint(R.string.search_hint);
                        mEditTitle.setDropDownWidth(-1);
                        mEditTitle.setDropDownBackgroundResource(android.R.color.white);
                        mEditTitle
                                .setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View arg0,
                                                           MotionEvent arg1) {
                                        // 
                                        mEditTitle.showDropDown();
                                        return false;
                                    }
                                });
                        mEditTitle
                                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                                    @Override
                                    public boolean onEditorAction(
                                            TextView arg0, int arg1,
                                            KeyEvent arg2) {
                                        // 
                                        if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
                                            MusicUtils.setArrayPref(activity,
                                                    "radiohistory", mEditTitle
                                                            .getText()
                                                            .toString());

                                            mTask = new radioTask(mEditTitle
                                                    .getText().toString(), 0,
                                                    activity, true);

                                            MusicUtils.execute(false, mTask,
                                                    true);
                                            setAdapter();
                                            dialog.dismiss();
                                            return true;
                                        }
                                        return false;
                                    }
                                });
                        setAdapter();

                        dialog.findViewById(R.id.cancel)
                                .setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View v) {
                                        if (mTask != null)
                                            mTask.cancel(true);
                                        dialog.dismiss();
                                    }
                                });
                        dialog.findViewById(R.id.create)
                                .setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View v) {
                                        // CREO RADIO SU RICERCA
                                        MusicUtils.setArrayPref(activity,
                                                "radiohistory", mEditTitle
                                                        .getText().toString());

                                        mTask = new radioTask(mEditTitle
                                                .getText().toString(), 0,
                                                activity, true);

                                        MusicUtils.execute(false, mTask, true);
                                        setAdapter();
                                        dialog.dismiss();
                                    }
                                });
                        dialog.show();

                    } else if (arg2 == 1) { // TOP 500
                        mTask = new radioTask(array[arg2], arg2, activity,
                                false);
                        MusicUtils.execute(false, mTask, true);
                    } else if (arg2 == 2) { // RANDOM STATIONS
                        mTask = new radioTask(array[arg2], arg2, activity,
                                false);
                        MusicUtils.execute(false, mTask, true);
                    } else {
                        GenreDialog task = new GenreDialog(activity,
                                array[arg2], arg2);
                        MusicUtils.execute(false, task, true);
                    }
                } else {
                    new AlertDialog.Builder(activity,
                            ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT)
                            .setTitle("")
                            .setMessage(R.string.no_internet_available)
                            .setPositiveButton(android.R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                        }
                                    }).show();
                }
            }

        });

        if (!InterfaceUtils.getTabletMode(activity)) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                lv.setNumColumns(1);
            else
                lv.setNumColumns(2);
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                lv.setNumColumns(2);
            } else {
                lv.setNumColumns(3);
            }
        }

        array = getResources().getStringArray(R.array.mixradio_entry);
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(array));

        lv.setAdapter(new ShoutcastRadioAdapter(activity, list));
        MusicBarUtils.updateMusicBar(this, nowPlayingView);

        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setHomeButtonEnabled(false);

        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ArrayList<String> itemList = new ArrayList<String>();
        itemList.add(getString(R.string.onlineall_nav));
        itemList.add(getString(R.string.onlineradio_nav));
        itemList.add(getString(R.string.playlists_nav));
        ArrayAdapter<String> aAdpt = new ArrayAdapter<String>(this, R.layout.simple_item_list_1, android.R.id.text1, itemList);
        getActionBar().setListNavigationCallbacks(aAdpt, this);

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        registerReceiver(mTrackListListener, new IntentFilter(f));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void setAdapter() {
        mSuggestionAdapter = MusicUtils.getArrayPref(activity, "radiohistory");
        mEditTitle.setAdapter(new ArrayAdapter<String>(activity,
                R.layout.search_suggest_item, mSuggestionAdapter.toArray(new String[0])));

    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // 
        if (key.equals(PreferencesActivity.KEY_TITLE)
                || key.equals(PreferencesActivity.KEY_THEME)
                || key.equals(PreferencesActivity.KEY_START_SCREEN)
                || key.equals(PreferencesActivity.GRID_MODE)
                || key.equals(PreferencesActivity.KEY_CUSTOM_THEME))
            changed = true;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Intent intent = new Intent();

        switch (itemPosition) {
            case 0:
                intent.setClass(this, OnlineActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                this.finish();
                break;
            case 1:
                break;
            case 2:
                intent.setClass(this, PlaylistBrowserActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                this.finish();
                break;
        }

        return false;
    }

    class radioTask extends AsyncTask<Boolean, Integer, Void> {

        String search;
        String id;
        Context context;
        int position;
        boolean mIsSearch;

        public radioTask(String what, int pos, Context context, boolean isSearch) {
            this.search = what;
            this.id = null;
            this.context = context;
            this.position = pos;
            this.mIsSearch = isSearch;
        }

        public radioTask(String what, String id, int pos, Context context,
                         boolean isSearch) {
            this.search = what;
            this.id = id;
            this.context = context;
            this.position = pos;
            this.mIsSearch = isSearch;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // 
            mProgressDialog.setProgress(values[0] + 1);
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPreExecute() {
            // 

            mProgressDialog = new ProgressDialog(context,
                    ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
            mProgressDialog.setTitle("");
            mProgressDialog.setMessage(activity
                    .getString(R.string.mixradio_dialog));

            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    // 
                    mTask.cancel(true);
                }
            });
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Boolean... params) {
            try {
                Library lib = new Library(search, new ArrayList<OnlineTrack>(), null);

                if (mIsSearch) {
                    if (!isCancelled()) {
                        lib = OnlineRadioSearchTask.getLibrarySearched(lib, search, context);
                    }
                } else {
                    if (!isCancelled()) {
                        if (position == 1) { // TOP 500
                            lib = OnlineRadioSearchTask.getLibraryTOP500(lib, context);
                        } else if (position == 2) { // RANDOM
                            lib = OnlineRadioSearchTask.getLibraryRandom(lib, context);
                        } else { // NORMAL GENRE
                            lib = OnlineRadioSearchTask.getLibraryGenre(lib,
                                    Integer.parseInt(id), context);
                        }
                    }
                }

                MusicUtils.writeAdapter(activity, lib, "mixradio");
                if (!isCancelled()) {
                    List<OnlineTrack> videos = MusicUtils.readAdapter(activity, "mixradio")
                            .getVideos();
                    MusicUtils.writeAdapter(activity, lib, "queue");

                    if (MusicUtils.sService != null) {
                        MusicUtils.sService.setOnlineLibrary("queue", 0);
                        MusicUtils.sService.openOnline(videos.get(0).getUrl());
                    }
                    if (!InterfaceUtils.getTabletMode(activity)) {
                        Intent intent = new Intent(
                                "com.luca89.thundermusic.PLAYBACK_VIEWER")
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        activity.startActivity(intent);
                    }
                }
            } catch (RemoteException e) {
                // 
                e.printStackTrace();
            } catch (IOException e) {
                // 
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // 
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // 

            if (mProgressDialog != null)
                mProgressDialog.dismiss();
            super.onPostExecute(result);
        }
    }

    class GenreDialog extends
            AsyncTask<Boolean, Integer, LinkedHashMap<String, String>> {

        String genrename;
        int position;
        Context context;

        public GenreDialog(Context context, String what, int pos) {
            this.genrename = what;
            this.position = pos;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            // 
            mProgressDialog = new ProgressDialog(context,
                    ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
            mProgressDialog.setTitle("");
            mProgressDialog.setMessage(activity
                    .getString(R.string.mixradio_dialog));

            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected LinkedHashMap<String, String> doInBackground(
                Boolean... params) {
            // 

            return OnlineRadioSearchTask.getSubGenreList(genrename, context);
        }

        @Override
        protected void onPostExecute(final LinkedHashMap<String, String> result) {
            // 
            // SHOW GENRE POPUP
            if (mProgressDialog != null)
                mProgressDialog.dismiss();
            if (result != null) {

                final List<String> array = new ArrayList<String>(
                        result.keySet());

                AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                        activity, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        activity, R.layout.search_suggest_item, array);
                builderSingle.setTitle("");
                builderSingle.setNegativeButton("cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        });

                builderSingle.setAdapter(arrayAdapter,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                mTask = new radioTask(array.get(which), result
                                        .get(array.get(which)), position,
                                        activity, false);
                                MusicUtils.execute(false, mTask, true);
                            }
                        });
                builderSingle.show();
            }
            super.onPostExecute(result);
        }

    }

}
