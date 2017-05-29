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

package com.luca89.thundermusic.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.luca89.adapters.VideosAdapter;
import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.R;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.dataset.Library;
import com.luca89.views.TouchInterceptor;

import java.io.IOException;
import java.util.Collections;

public class NowplayingOnlineFragment extends Fragment implements
        MusicUtils.Defs, ServiceConnection {
    // A reference to our list that will hold the video details

    private static ListView listView;
    private static Library mLib;
    private static Activity activity;
    private static String mPlaylist;
    private MusicUtils.ServiceToken mToken;
    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {

            ((VideosAdapter) listView.getAdapter()).notifyDataSetChanged();
            Collections.swap(mLib.getVideos(), from, to);
            // Update the service only if manipulating the queue!
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {

        mPlaylist = "queue";


        RelativeLayout main = (RelativeLayout) inflater.inflate(
                R.layout.media_picker_activity_playlist, container, false);

        activity = getActivity();
        mTxt = (TextView) main.findViewById(R.id.nointernet);
        listView = (ListView) main.findViewById(android.R.id.list);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {

                if (!isNetworkAvailable(getActivity())) {
                    listView.setEnabled(false);
                    mTxt.setVisibility(View.VISIBLE);
                } else
                    MusicUtils.execute(false, new clickerTask(arg2), true);

            }
        });
        if (!isNetworkAvailable(activity)) {
            listView.setEnabled(false);
            mTxt.setVisibility(View.VISIBLE);
        }

        ((TouchInterceptor) listView).setDropListener(mDropListener);
        ((TouchInterceptor) listView).setRemoveListener(mRemoveListener);

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        activity.registerReceiver(mNowPlayingListener, new IntentFilter(f));
        mNowPlayingListener.onReceive(activity, new Intent(
                MediaPlaybackService.META_CHANGED));
        mToken = MusicUtils.bindToService(activity, this);

        return main;
    }

    @Override
    public void onDestroy() {
        // Make sure we null our handler when the activity has stopped
        // because who cares if we get a callback once the activity has stopped?
        // not me!
        MusicUtils.unbindFromService(mToken);
        activity.unregisterReceiver(mNowPlayingListener);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        // 
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
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        activity.finish();
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
