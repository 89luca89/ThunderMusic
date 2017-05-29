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
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.luca89.adapters.PlaylistListAdapter;
import com.luca89.thundermusic.activities.PreferencesActivity;
import com.luca89.thundermusic.activities.TrackOnlinePlaylistBrowser;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicBarUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.ThemeUtils;
import com.luca89.utils.activities.RenamePlaylist;
import com.luca89.views.WeekSelector;

import java.text.Collator;
import java.util.ArrayList;

public class PlaylistBrowserActivity extends Activity implements ServiceConnection, MusicUtils.Defs,
        SharedPreferences.OnSharedPreferenceChangeListener, ActionBar.OnNavigationListener {
    private static final int DELETE_PLAYLIST = CHILD_MENU_BASE + 1;
    private static final int EDIT_PLAYLIST = CHILD_MENU_BASE + 2;
    private static final int RENAME_PLAYLIST = CHILD_MENU_BASE + 3;
    private static final int CHANGE_WEEKS = CHILD_MENU_BASE + 4;
    private static final long RECENTLY_ADDED_PLAYLIST = -1;
    public static Activity activity;
    public static Cursor mPlaylistCursor;
    static String[] mCols = new String[]{MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME};
    private static RelativeLayout nowPlayingView;
    private static boolean changed = false;
    private static MusicUtils.ServiceToken mToken;
    private static PlaylistListAdapter mAdapter;

    private static int mLastListPosCourse = -1;
    private static GridView lv = null;

    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicBarUtils.updateMusicBar(PlaylistBrowserActivity.this,
                    nowPlayingView);
        }
    };

    public static void showUpPopup(final Activity context, View v,
                                   final int position) {
        PopupMenu popup = new PopupMenu(context, v);

        Menu menu = popup.getMenu();
        /** Adding menu items to the popumenu */

        if (MusicUtils.getPlaylistiD(mPlaylistCursor.getString(mPlaylistCursor
                .getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME))) != lv
                .getItemIdAtPosition(position))

            if (lv.getItemIdAtPosition(position) >= 0 /* || mi.id == PODCASTS_PLAYLIST */) {
                menu.add(0, DELETE_PLAYLIST, 0, R.string.delete_playlist_menu);
            }

        if (lv.getItemIdAtPosition(position) == RECENTLY_ADDED_PLAYLIST) {
            menu.add(0, EDIT_PLAYLIST, 0, R.string.edit_playlist_menu);
        }

        if (lv.getItemIdAtPosition(position) >= 0) {
            menu.add(0, RENAME_PLAYLIST, 0, R.string.rename_playlist_menu);
        }

        mPlaylistCursor.moveToPosition(position);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 

                switch (item.getItemId()) {
                    case DELETE_PLAYLIST:
                        Uri uri = ContentUris.withAppendedId(
                                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                lv.getItemIdAtPosition(position));
                        activity.getContentResolver().delete(uri, null, null);
                        Toast.makeText(activity, R.string.playlist_deleted_message,
                                Toast.LENGTH_SHORT).show();
                        if (MusicUtils.getPlaylistiD(mPlaylistCursor.getString(mPlaylistCursor
                                .getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME))) == lv
                                .getItemIdAtPosition(position)) {
                            MusicUtils.deleteOnlinePlaylist(
                                    activity,
                                    mPlaylistCursor.getString(mPlaylistCursor
                                            .getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)));
                        }
                        activity.recreate();
                        break;
                    case EDIT_PLAYLIST:
                        if (lv.getItemIdAtPosition(position) == RECENTLY_ADDED_PLAYLIST) {
                            Intent intent = new Intent();
                            intent.setClass(activity, WeekSelector.class);
                            context.startActivityForResult(intent, CHANGE_WEEKS);
                            return true;
                        };
                    case RENAME_PLAYLIST:
                        Intent intent = new Intent();
                        intent.setClass(activity, RenamePlaylist.class);
                        intent.putExtra("rename", lv.getItemIdAtPosition(position));
                        context.startActivityForResult(intent, RENAME_PLAYLIST);
                        break;
                }
                return true;

            }
        });
        /** Showing the popup menu */
        popup.show();
    }


    public static Cursor getPlaylistCursor(AsyncQueryHandler async,
                                           String filterstring, Context context) {

        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Playlists.NAME + " != ''");

        // Add in the filtering constraints
        String[] keywords = null;
        if (filterstring != null) {
            String[] searchWords = filterstring.split(" ");
            keywords = new String[searchWords.length];
            Collator col = Collator.getInstance();
            col.setStrength(Collator.PRIMARY);
            for (int i = 0; i < searchWords.length; i++) {
                keywords[i] = '%' + searchWords[i] + '%';
            }
            for (int i = 0; i < searchWords.length; i++) {
                where.append(" AND ");
                where.append(MediaStore.Audio.Playlists.NAME + " LIKE ?");
            }
        }

        String whereclause = where.toString();

        if (async != null) {
            async.startQuery(0, null,
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mCols,
                    whereclause, keywords, MediaStore.Audio.Playlists.NAME);
            return null;
        }
        Cursor c;
        c = MusicUtils.query(context,
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mCols,
                whereclause, keywords, MediaStore.Audio.Playlists.NAME);

        return filterCursor(c);
    }

    public static boolean isHidden(Cursor cc) {
        int mTitleIdx = cc
                .getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME);
        int mIdIdx = cc
                .getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID);
        String name = cc.getString(mTitleIdx);
        long id = cc.getLong(mIdIdx);
        return id != MusicUtils.getPlaylistiD(name);

    }

    public static Cursor filterCursor(Cursor cursor) {
        MatrixCursor newCursor = new MatrixCursor(mCols);
        if (cursor.moveToFirst()) {
            do {
                if (isHidden(cursor)) continue;
                newCursor.addRow(new Object[]{cursor.getString(0), cursor.getString(1)});
            } while (cursor.moveToNext());
        }
        return newCursor;
    }

    @Override
    public void onResume() {
        // 
        super.onResume();
        if (mToken == null) {
            mToken = MusicUtils.bindToService(this, this);
        }
        getActionBar().setSelectedNavigationItem(2);
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
    protected void onCreate(Bundle savedInstanceState) {
        // 
        MusicUtils.setIntPref(this, "activesection", 3);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        activity = this;
        ThemeUtils.getAppTheme(PlaylistBrowserActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_picker_activity_list);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mToken = MusicUtils.bindToService(this, this);

        if (InterfaceUtils.getTabletMode(this))
            nowPlayingView = (RelativeLayout) findViewById(R.id.nowplaying_actionbar_tablet);
        else
            nowPlayingView = (RelativeLayout) findViewById(R.id.nowplaying_actionbar);
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

        lv = (GridView) findViewById(android.R.id.list);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                // 
                if (MusicUtils.getPlaylistiD(mPlaylistCursor.getString(mPlaylistCursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME))) == arg3) {

                    Intent intent = new Intent(activity, TrackOnlinePlaylistBrowser.class)
                            .setAction(Intent.ACTION_EDIT);
                    ;
                    intent.putExtra("playlist",
                            mPlaylistCursor.getString(mPlaylistCursor
                                    .getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)));
                    intent.putExtra("called", false);
                    startActivity(intent);
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

        if (mAdapter == null) {

            mAdapter = new PlaylistListAdapter(activity, this,
                    R.layout.track_list_item, mPlaylistCursor,
                    new String[]{MediaStore.Audio.Playlists.NAME},
                    new int[]{android.R.id.text1});
            lv.setAdapter(mAdapter);
            getPlaylistCursor(mAdapter.getQueryHandler(), null, activity);
        } else {
            mAdapter.setActivity(this);
            lv.setAdapter(mAdapter);
            mPlaylistCursor = mAdapter.getCursor();
            // If mPlaylistCursor is null, this can be because it doesn't have
            // a cursor yet (because the initial query that sets its cursor
            // is still in progress), or because the query failed.
            // In order to not flash the error dialog at the user for the
            // first case, simply retry the query when the cursor is null.
            // Worst case, we end up doing the same query twice.
            if (mPlaylistCursor != null) {
                init(mPlaylistCursor);
            } else {
                getPlaylistCursor(mAdapter.getQueryHandler(), null, activity);
            }
        }

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addDataScheme("file");
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        registerReceiver(mTrackListListener, new IntentFilter(f));
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
    public void onDestroy() {
        if (lv != null) {
            mLastListPosCourse = lv.getFirstVisiblePosition();
        }

        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
        }
        unregisterReceiver(mTrackListListener);
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.
        lv.setAdapter(null);
        mAdapter = null;
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void init(Cursor cursor) {

        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(cursor);

        if (mPlaylistCursor == null) {
            return;
        }

        // restore previous position
        if (mLastListPosCourse >= 0) {
            lv.setSelection(mLastListPosCourse);
            mLastListPosCourse = -1;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_DONE:
                if (resultCode != Activity.RESULT_CANCELED) {
                    getPlaylistCursor(mAdapter.getQueryHandler(), null, activity);
                }
                recreate();
                break;
        }
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
                intent.setClass(this, MixRadioActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                this.finish();
                break;
            case 2:
                break;
        }

        return false;
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
}
