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

package com.luca89.thundermusic.fragments.online;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.luca89.adapters.VideosAdapter;
import com.luca89.service.search.BandcampSearchTask;
import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.OnlineActivity;
import com.luca89.thundermusic.R;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.ThemeUtils;
import com.luca89.utils.activities.CreatePlaylist;
import com.luca89.utils.dataset.Library;
import com.luca89.utils.dataset.OnlineTrack;
import com.luca89.views.AnimatedLayout;
import com.melnykov.fab.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BandcampFragment extends Fragment implements MusicUtils.Defs {
    // A reference to our list that will hold the video details
    private static final int SEARCH = CHILD_MENU_BASE + 6;
    private static GridView listView;
    private static Activity activity;
    private static String search;
    private static Library mLib;
    private static Set<String> mSuggestionAdapter;
    private static ProgressBar prog;
    private static TextView mTxt;
    private static AnimatedLayout mSearchLayout;
    private static int mSelectedPosition;
    private static VideosAdapter mAdapter;
    private static boolean scrolling = false;
    static Handler responseHandler = new Handler() {
        public void handleMessage(Message msg) {
            populateListWithVideos(activity, activity);
        }
    };
    private static FloatingActionButton mFAB;
    private static ImageButton searchBtn;
    private static ImageButton playBtn;
    private static ImageButton saveBtn;
    private static AutoCompleteTextView mEditTitle;
    private static int myLastVisiblePos = Integer.MAX_VALUE;

    public static void clickSearchBtn(final Context context) {

        if (mEditTitle.getText().toString().isEmpty()) {
            mEditTitle.post(new Runnable() {
                public void run() {
                    mEditTitle.requestFocusFromTouch();
                    InputMethodManager lManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    lManager.showSoftInput(mEditTitle, 0);
                }
            });
            mEditTitle.showDropDown();
        } else {
            // 
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mEditTitle.getWindowToken(), 0);

            search = mEditTitle.getText().toString();

            MusicUtils.setArrayPref(context, "history", search);

            if (!isNetworkAvailable(context)) {
                mTxt.setVisibility(View.VISIBLE);
            } else if (!search.isEmpty()) {
                MusicUtils.execute(false, new Task(search, "search", false), true);
                setAdapter();
            }
        }
    }

    private static void setAdapter() {
        mSuggestionAdapter = MusicUtils.getArrayPref(activity, "history");
        mEditTitle.setAdapter(new ArrayAdapter<String>(activity,
                R.layout.search_suggest_item, mSuggestionAdapter.toArray(new String[0])));

    }

    /**
     * activity method retrieves the Library of videos from the task and passes
     * them to our ListView
     *
     * @param
     */
    public static void populateListWithVideos(Activity activity,
                                              Context context) {

        if (search != null && !search.isEmpty()) {
            Library tmp;
            try {
                tmp = MusicUtils.readAdapter(context, "search");
                if (tmp != null) {
                    mLib = tmp;
                    scrolling = false;
                    mAdapter = new VideosAdapter(activity, tmp.getVideos(),
                            false, "bandcamp");
                    listView.setAdapter(mAdapter);
                    listView.setVisibility(View.VISIBLE);
                }
            } catch (ClassNotFoundException e) {
                // 
                e.printStackTrace();
            } catch (IOException e) {
                // 
                e.printStackTrace();
            }
        }
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void showUpPopup(final Activity context, View v,
                                   final int position) {
        PopupMenu popup = new PopupMenu(context, v);

        Menu menu = popup.getMenu();
        /** Adding menu items to the popumenu */

        mSelectedPosition = position;
        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0,
                R.string.add_to_playlist);
        MusicUtils.makePlaylistMenuOnline(activity, sub);
        menu.add(0, SEARCH, 0, R.string.search_internet_menu_short);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case PLAY_SELECTION:
                        if (isNetworkAvailable(activity)) {
                            BandcampFragment bcf = new BandcampFragment();
                            MusicUtils.execute(false, bcf.new clickerTask(position,
                                    false), true);
                        }
                        break;

                    case QUEUE:
                        MusicUtils.addToExistingPlaylist(activity, "queue", mLib
                                .getVideos().get(mSelectedPosition));
                        return true;
                    case NEW_PLAYLIST:

                        Bundle data = new Bundle();
                        data.putSerializable("TRACK",
                                mLib.getVideos().get(mSelectedPosition));
                        Intent intent = new Intent(activity, CreatePlaylist.class);
                        intent.putExtra("online", true);
                        intent.putExtra("track", data);
                        context.startActivity(intent);
                        return true;
                    case PLAYLIST_SELECTED:
                        String playlist = item.getIntent().getStringExtra("name");
                        MusicUtils.addToExistingPlaylist(activity, playlist, mLib
                                .getVideos().get(mSelectedPosition), 0);
                        return true;
                    case SEARCH:
                        context.startActivity(
                                new Intent(Intent.ACTION_VIEW, Uri
                                        .parse(mLib.getVideos().get(position)
                                                .getLink())));
                        break;
                }
                return true;
            }
        });
        /** Showing the popup menu */
        popup.show();
    }

    public static boolean getScrollingState() {
        return scrolling;
    }

    public static void setScrollingState(boolean state) {
        scrolling = state;
    }

    private static void setSearch(String mSuggestionAdapter) {
        search = mSuggestionAdapter;
    }

    @Override
    public void onResume() {
        super.onResume();
        activity = getActivity();
        setAdapter();

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("search", search);
        super.onSaveInstanceState(outState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            search = savedInstanceState.getString("search");
        } else {
            search = getActivity().getIntent().getStringExtra("search");
        }

        if (search == null) {
            search = "";
        }

        super.onCreate(savedInstanceState);
        // 

        RelativeLayout main = (RelativeLayout) inflater.inflate(R.layout.media_picker_activity_online, container, false);
        activity = getActivity();
        mSearchLayout = (AnimatedLayout) main.findViewById(R.id.search_layout);

        mEditTitle = (AutoCompleteTextView) main.findViewById(R.id.search_bar);
        mEditTitle.setDropDownBackgroundResource(R.drawable.abc_menu_dropdown_panel_holo_light);

        mEditTitle
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView arg0, int arg1,
                                                  KeyEvent arg2) {
                        // 
                        if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
                            clickSearchBtn(activity);
                        }
                        return false;
                    }
                });
        mEditTitle.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 
                mEditTitle.showDropDown();
                return false;
            }
        });
        setAdapter();

        ThemeUtils.getNowDrawerTheme(activity, mSearchLayout);
        mFAB = (FloatingActionButton) main.findViewById(R.id.FAB);
        InterfaceUtils.setUpFAB(activity, R.drawable.music_search_holo_dark, mFAB, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickSearchBtn(activity);
            }
        });
        listView = (GridView) main.findViewById(R.id.videosListView);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                if (!isNetworkAvailable(activity)) {
                    mTxt.setVisibility(View.VISIBLE);
                } else
                    MusicUtils.execute(false, new clickerTask(arg2, false),
                            true);

            }
        });

        myLastVisiblePos = listView.getFirstVisiblePosition();

        if (!InterfaceUtils.getTabletMode(activity)) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                listView.setNumColumns(1);
            else
                listView.setNumColumns(2);

        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                listView.setNumColumns(2);
            } else {
                listView.setNumColumns(3);
            }
        }

        mTxt = (TextView) main.findViewById(R.id.nointernet);
        searchBtn = (ImageButton) main.findViewById(R.id.search_btn);
        searchBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 
                clickSearchBtn(activity);
            }
        });

        playBtn = (ImageButton) main.findViewById(R.id.play_all);
        playBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 
                if (mLib != null && mLib.getVideos().size() > 1) {
                    BandcampFragment bcf = new BandcampFragment();
                    MusicUtils.execute(false, bcf.new clickerTask(0, true), true);
                }
            }
        });
        saveBtn = (ImageButton) main.findViewById(R.id.save_playlist);
        saveBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (mLib != null && mLib.getVideos().size() > 1) {
                    Bundle data = new Bundle();
                    data.putSerializable("LIBRARY", mLib);
                    Intent intent = new Intent(activity, CreatePlaylist.class);
                    intent.putExtra("online", true);
                    intent.putExtra("track", data);
                    startActivity(intent);
                }
            }
        });
        prog = (ProgressBar) main.findViewById(R.id.progressBar1);
        // setAdapter(getBaseContext());
        if (!isNetworkAvailable(activity)) {
            mTxt.setTextColor(ThemeUtils.getTextColor(activity));
            mTxt.setVisibility(View.VISIBLE);
        }

        if (!search.isEmpty())
            populateListWithVideos(activity, activity);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            private int previousFirstVisibleItem = 0;
            private long previousEventTime = 0;
            private double speed = 0;

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                int currentFirstVisPos = view.getFirstVisiblePosition();
                if (currentFirstVisPos > myLastVisiblePos) {
                    if (mSearchLayout.getVisibility() == View.VISIBLE) {
                        mSearchLayout.setVisibility(View.GONE, true,
                                R.anim.fade_out);
                    }
                    OnlineActivity.setTabHide(BandcampFragment.this, mFAB);

                }
                if (currentFirstVisPos < myLastVisiblePos) {
                    if (mSearchLayout.getVisibility() != View.VISIBLE) {
                        mSearchLayout.setVisibility(View.VISIBLE, true,
                                R.anim.fade_in);
                    }
                    OnlineActivity.setTabShow(BandcampFragment.this, mFAB);
                }
                myLastVisiblePos = currentFirstVisPos;

                if (previousFirstVisibleItem != firstVisibleItem) {
                    long currTime = System.currentTimeMillis();
                    long timeToScrollOneElement = currTime - previousEventTime;
                    speed = ((double) 1 / timeToScrollOneElement) * 1000;

                    previousFirstVisibleItem = firstVisibleItem;
                    previousEventTime = currTime;


                    if (speed > 15)
                        setScrollingState(true);
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // 
                if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    if (getScrollingState() == true)
                        mAdapter.notifyDataSetChanged();
                    setScrollingState(false);
                }
            }
        });

        playBtn.setImageDrawable(ThemeUtils.colorizeResourceDrawable(R.drawable.btn_playback_play, activity));
        searchBtn.setImageDrawable(ThemeUtils.colorizeResourceDrawable(R.drawable.mixradio_search_holo_dark, activity));
        saveBtn.setImageDrawable(ThemeUtils.colorizeResourceDrawable(R.drawable.music_save_holo_dark, activity));
        mEditTitle.setTextColor(ThemeUtils.getTextColor(activity));
        mEditTitle.setHintTextColor(ThemeUtils.getTextColor(activity));
        return main;
    }

    //

    /**
     * TODO
     * MusicUtils.execute(false, new Task(search, "search", true), true); To go to next page if needed
     * not used but here for future reference
     * //
     **/
    static class Task extends AsyncTask<Boolean, Integer, Void> {

        String name;
        String search;
        boolean nextpage;

        public Task(String search, String name, boolean nextpage) {
            setSearch(search);
            this.search = search;
            this.name = name;
            this.nextpage = nextpage;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPreExecute() {
            listView.setVisibility(View.GONE);
            prog.setVisibility(View.VISIBLE);

        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Void doInBackground(Boolean... params) {
            if (!nextpage)
                mLib = new Library(search, new ArrayList<OnlineTrack>(), null);
            new BandcampSearchTask(responseHandler, search, activity,
                    name, mLib).run();

            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(final Void result) {

            prog.setVisibility(View.GONE);
        }

    }

    class clickerTask extends AsyncTask<Boolean, Integer, Void> {

        int arg2;
        boolean playall;

        public clickerTask(int click, boolean playall) {
            arg2 = click;
            this.playall = playall;
        }

        @Override
        protected Void doInBackground(Boolean... params) {
            try {
                List<OnlineTrack> videos = new ArrayList<OnlineTrack>();
                if (mLib != null && !mLib.getVideos().isEmpty()) {
                    if (!playall)
                        videos.add(mLib.getVideos().get(arg2));
                    else {
                        videos = mLib.getVideos();
                    }
                    Library lib = new Library("queue", videos, null);
                    MusicUtils.writeAdapter(activity, lib, "queue");
                    MusicUtils.sService.setOnlineLibrary("queue", 0);
                    if (!playall)
                        MusicUtils.sService.openOnline(mLib.getVideos()
                                .get(arg2).getUrl());
                    else {
                        MusicUtils.sService.openOnline(mLib.getVideos()
                                .get(0).getUrl());
                    }
                }
                if (!InterfaceUtils.getTabletMode(activity)) {
                    Intent intent = new Intent(
                            "com.luca89.thundermusic.PLAYBACK_VIEWER")
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    activity.startActivity(intent);
                }
            } catch (RemoteException e) {
                // 
                e.printStackTrace();
            } catch (IOException e) {
                // 
                e.printStackTrace();
            }
            return null;
        }
    }
}
