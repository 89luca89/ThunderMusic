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
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;

import com.luca89.thundermusic.activities.MediaPlaybackCarModeActivity;
import com.luca89.thundermusic.activities.PreferencesActivity;
import com.luca89.thundermusic.fragments.online.ArchiveOrgFragment;
import com.luca89.thundermusic.fragments.online.BandcampFragment;
import com.luca89.thundermusic.fragments.online.FreeMusicArchiveOrgFragment;
import com.luca89.thundermusic.fragments.online.OnlineSearchAllFragment;
import com.luca89.thundermusic.fragments.online.SoundcloudFragment;
import com.luca89.thundermusic.fragments.online.YoutubeFragment;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicBarUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.MusicUtils.ServiceToken;
import com.luca89.utils.ThemeUtils;
import com.luca89.views.AnimatedLayout;
import com.luca89.views.PagerTabStrip;
import com.melnykov.fab.FloatingActionButton;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("InflateParams")
@SuppressWarnings("deprecation")
public class OnlineActivity extends FragmentActivity implements
        MusicUtils.Defs, OnSharedPreferenceChangeListener, ServiceConnection, OnNavigationListener {

    public static final int TOTAL_INDEX = 0;
    public static final int VK_INDEX = 1;
    public static final int SOUNDCLOUD_INDEX = 2;
    public static final int BANDCAMP_INDEX = 3;
    public static final int ARCHIVE_INDEX = 4;
    public static final int FMA_INDEX = 5;
    private static final int ABOUT = 0;
    public static int Theme;
    public static boolean changed = false;
    public static PagerTabStrip tabs = null;
    public static AnimatedLayout tabsLayout;
    private static ServiceToken mToken;
    private static String TOTAL;
    private static String VK;
    private static String SOUNDCLOUD;
    private static String BANDCAMP;
    private static String ARCHIVE;
    private static String FMA;
    private static ViewPager mViewPager;
    private static MyPagerAdapter myAdapter;
    private static RelativeLayout nowPlayingView;
    private static SharedPreferences mSettings;
    public FragmentActivity activity;
    private int mCurrentTab = TOTAL_INDEX;
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicBarUtils.updateMusicBar(OnlineActivity.this,
                    nowPlayingView);
        }
    };
    private List<Fragment> fragments;

    public OnlineActivity() {
    }

    public static void setTabHide(Fragment caller, FloatingActionButton chatHead) {
        if (myAdapter.getItem(mViewPager.getCurrentItem()).getClass() == caller.getClass()) {
            if (tabsLayout.getVisibility() == View.VISIBLE) {
                tabsLayout.setVisibility(View.GONE, true, R.anim.fade_out);
            }
            if (chatHead.isVisible()) {
                chatHead.hide();
            }
        }
    }

    public static void setTabShow(Fragment caller, FloatingActionButton chatHead) {
        if (myAdapter.getItem(mViewPager.getCurrentItem()).getClass() == caller.getClass()) {
            if (tabsLayout.getVisibility() == View.GONE) {
                tabsLayout.setVisibility(View.VISIBLE, true, R.anim.fade_in);
            }

            if (!chatHead.isVisible()) {
                chatHead.show();
            }
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        MusicUtils.setIntPref(this, "activesection", 1);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mSettings.registerOnSharedPreferenceChangeListener(this);

        Theme = ThemeUtils.getAppTheme3(this);

        ThemeUtils.getAppTheme(this);
        super.onCreate(icicle);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.main);

        if (InterfaceUtils.getTabletMode(this))
            nowPlayingView = (RelativeLayout) findViewById(R.id.nowplaying_actionbar_tablet);
        else
            nowPlayingView = (RelativeLayout) findViewById(R.id.nowplaying_actionbar);

        TOTAL = getString(R.string.onlineall);
        VK = getString(R.string.youtube);
        SOUNDCLOUD = getString(R.string.soundcloud);
        BANDCAMP = getString(R.string.bandcamp);
        ARCHIVE = getString(R.string.archiveorg);
        FMA = getString(R.string.fmarchiveorg);

        activity = this;

        mToken = MusicUtils.bindToService(this, this);

        myAdapter = new MyPagerAdapter(getSupportFragmentManager());
        fragments = new ArrayList<Fragment>();
        if (fragments.isEmpty()) {
            fragments.add(new OnlineSearchAllFragment());
            fragments.add(new YoutubeFragment());
            fragments.add(new SoundcloudFragment());
            fragments.add(new BandcampFragment());
            fragments.add(new ArchiveOrgFragment());
            fragments.add(new FreeMusicArchiveOrgFragment());
            myAdapter.notifyDataSetChanged();
        }

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

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(myAdapter);
        mViewPager.setOffscreenPageLimit(FMA_INDEX + 1);
        mViewPager.setBackgroundDrawable(null);
        mViewPager
                .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        invalidateOptionsMenu();
                    }
                });
        setUpInterface();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActionBar().setSelectedNavigationItem(0);
        if (mToken == null) {
            mToken = MusicUtils.bindToService(this, this);
        }

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        registerReceiver(mTrackListListener, new IntentFilter(f));
        MusicBarUtils.updateMusicBar(this, nowPlayingView);
        int currFrag = mViewPager.getCurrentItem();
        switch (currFrag) {
            case TOTAL_INDEX:
                OnlineSearchAllFragment.populateListWithVideos(this, this);
                break;
            case VK_INDEX:
                YoutubeFragment.populateListWithVideos(this, this);
            case SOUNDCLOUD_INDEX:
                SoundcloudFragment.populateListWithVideos(this, this);
                break;
            case BANDCAMP_INDEX:
                BandcampFragment.populateListWithVideos(this, this);
                break;
            case ARCHIVE_INDEX:
                ArchiveOrgFragment.populateListWithVideos(this, this);
                break;
            case FMA_INDEX:
                FreeMusicArchiveOrgFragment.populateListWithVideos(this, this);
                break;
            default:
                break;
        }
        if (changed == true) {
            changed = false;
            recreate();
        }
    }

    @Override
    public void onDestroy() {
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
        }
        unregisterReceiver(mTrackListListener);
        super.onDestroy();
    }

    private String getStringId(int index) {
        String tabStr = TOTAL;
        switch (index) {
            case TOTAL_INDEX:
                tabStr = TOTAL;
                break;
            case VK_INDEX:
                tabStr = VK;
                break;
            case SOUNDCLOUD_INDEX:
                tabStr = SOUNDCLOUD;
                break;
            case BANDCAMP_INDEX:
                tabStr = BANDCAMP;
                break;
            case ARCHIVE_INDEX:
                tabStr = ARCHIVE;
                break;
            case FMA_INDEX:
                tabStr = FMA;
                break;
            default:
                break;
        }
        return tabStr;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, R.id.menu_settings, 0, R.string.settings);
        menu.add(1, ABOUT + 2, 0, R.string.carmode_menu_short);

        if (mSettings.getBoolean(PreferencesActivity.POPUP_ON, false))
            menu.add(1, ABOUT + 1, 0, R.string.go_popup);
        menu.add(1, ABOUT, 0, R.string.about_menu_short);
        menu.add(1, EXIT, 0, R.string.exit_menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case ABOUT + 1:
                try {
                    MusicUtils.sService.startPopup();
                } catch (RemoteException e) {
                    // 
                    e.printStackTrace();
                }
                break;

            case ABOUT + 2:
                Intent carmode = new Intent(activity,
                        MediaPlaybackCarModeActivity.class);
                carmode.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(carmode);
                break;

            case ABOUT:
                InterfaceUtils.showAbout(this);
                break;

            case R.id.menu_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;

            case EXIT:
                try {
                    MusicUtils.sService.pause();
                    MusicUtils.sService.exit();
                    finish();
                } catch (RemoteException e) {
                    // 
                    e.printStackTrace();
                }

                break;
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
                || key.equals(PreferencesActivity.GRID_MODE)
                || key.equals(PreferencesActivity.KEY_CUSTOM_THEME))
            changed = true;
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

    private void setUpInterface() {
        // mViewPager.setCurrentItem(mCurrentTab );
        tabsLayout = (AnimatedLayout) findViewById(R.id.tabstop_layout);
        tabs = (PagerTabStrip) findViewById(R.id.tabstop);
        tabsLayout.setVisibility(View.VISIBLE);
        tabs.setTabBackground(R.drawable.item_background_holo_light);
        tabs.setTextColorResource(android.R.color.white);
        if (ThemeUtils.getAppTheme2(this) != 3) {
            if (Theme != 4) {
                Drawable mActionBarBackgroundDrawable;

                mActionBarBackgroundDrawable = ThemeUtils
                        .getThemeDrawable(this, Theme);
                tabs.setBackgroundDrawable(mActionBarBackgroundDrawable);
                tabs.setIndicatorColorResource(android.R.color.white);
                if (Theme < 6)
                    tabs.setUnderlineColorResource(ThemeUtils
                            .getThemeColor(this, Theme));
                else
                    tabs.setUnderlineColor(ThemeUtils.getThemeColor(this,
                            Theme));

            } else {
                tabs.setBackgroundResource(android.R.color.black);
            }
            tabs.setIndicatorColorResource(android.R.color.white);
        } else {
            Drawable mActionBarBackgroundDrawable;

            mActionBarBackgroundDrawable = ThemeUtils
                    .getThemeDrawable(this, Theme);
            tabs.setBackgroundDrawable(mActionBarBackgroundDrawable);
            tabs.setIndicatorColorResource(android.R.color.white);
            if (Theme < 6)
                tabs.setUnderlineColorResource(ThemeUtils
                        .getThemeColor(this, Theme));
            else
                tabs.setUnderlineColor(ThemeUtils.getThemeColor(this,
                        Theme));

        }

        mViewPager.setCurrentItem(mCurrentTab, true);
        tabs.setViewPager(mViewPager);
        tabs.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between pages, select the
                // corresponding tab.
                invalidateOptionsMenu();
            }
        });

    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Intent intent = new Intent();

        switch (itemPosition) {
            case 0:
                break;
            case 1:
                intent.setClass(this, MixRadioActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                this.finish();
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

    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getStringId(position);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }
}
