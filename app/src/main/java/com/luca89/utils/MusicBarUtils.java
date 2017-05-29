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

package com.luca89.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.audiofx.AudioEffect;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.R;
import com.luca89.thundermusic.activities.MediaPlaybackActivity;

/**
 * Created by luca-linux on 6/3/16.
 */
public class MusicBarUtils {
    private static final int REFRESH = 1;
    public static TextView mCurrentTime;
    public static TextView mTotalTime;
    public static ProgressBar mProgress;
    public static ImageButton mPrevButton;
    public static ImageButton mPauseButton;
    public static ImageButton mNextButton;
    public static ImageView mAlbum;
    private static ImageButton mRepeatButton;
    private static ImageButton mShuffleButton;
    private static ImageButton mEqButton;
    private static long mDuration;
    private static Context mContext;
    private static long mPosOverride = -1;
    private static final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    long next = MusicBarUtils.refreshNow(mContext);
                    MusicBarUtils.queueNextRefresh(next);
                    break;

                default:
                    break;
            }
        }
    };
    private static boolean mFromTouch = false;
    private static SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mFromTouch = true;
        }

        public void onProgressChanged(SeekBar bar, int progress,
                                      boolean fromuser) {
            if (!fromuser || (MusicUtils.sService == null))
                return;
            mPosOverride = mDuration * progress / 1000;
            try {
                MusicUtils.sService.seek(mPosOverride);
            } catch (RemoteException ex) {
            }

            // trackball event, allow progress updates
            MusicBarUtils.refreshNow(mContext);
            if (!mFromTouch) {
                MusicBarUtils.refreshNow(mContext);
                mPosOverride = -1;
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mPosOverride = -1;
            mFromTouch = false;
        }
    };

    public static void updateMusicBar(final Activity a,
                                      RelativeLayout nowPlayingView) {
        updateMusicBar(a, true, nowPlayingView);
    }

    /**
     * Utility to initialize and manage the bottom music bar
     * it also distinguishes from phone and tablet mode
     * to act accordingly and load the more appropriate layout
     *
     * @param a
     * @param visible
     * @param nowPlayingView
     */
    public static void updateMusicBar(final Activity a, boolean visible,
                                      RelativeLayout nowPlayingView) {
        nowPlayingView.bringToFront();
        nowPlayingView.invalidate();
        ThemeUtils.getBarTheme(a, nowPlayingView);
        try {
            if (MusicUtils.sService != null) {

                if (MusicUtils.sService.getQueue().length == 0)
                    return;

                TextView title = (TextView) nowPlayingView
                        .findViewById(R.id.title);
                TextView album = (TextView) nowPlayingView
                        .findViewById(R.id.albumname);
                mContext = a.getApplicationContext();
                mPauseButton = (ImageButton) nowPlayingView
                        .findViewById(R.id.pause);
                mPrevButton = (ImageButton) nowPlayingView
                        .findViewById(R.id.prev);
                mNextButton = (ImageButton) nowPlayingView
                        .findViewById(R.id.next);
                mAlbum = (ImageView) nowPlayingView
                        .findViewById(R.id.album);

                title.setTextColor(ThemeUtils.getTextColor(a));
                album.setTextColor(ThemeUtils.getTextColor(a));

                if (ThemeUtils.getAppTheme2(a) == 3) {
                    mPrevButton.setImageResource(R.drawable.btn_playback_previous_black);
                    mNextButton.setImageResource(R.drawable.btn_playback_next_black);
                } else {
                    mPrevButton.setImageResource(R.drawable.btn_playback_previous);
                    mNextButton.setImageResource(R.drawable.btn_playback_next);
                }

                if (InterfaceUtils.getTabletMode(a)) {
                    mCurrentTime = (TextView) nowPlayingView
                            .findViewById(R.id.currenttime);
                    mTotalTime = (TextView) nowPlayingView
                            .findViewById(R.id.totaltime);
                    mProgress = (ProgressBar) nowPlayingView
                            .findViewById(android.R.id.progress);
                    mDuration = MusicUtils.sService.duration();
                    mTotalTime.setText(MusicUtils.makeTimeString(a,
                            mDuration / 1000));
                    mShuffleButton = ((ImageButton) nowPlayingView
                            .findViewById(R.id.shuffle));
                    mRepeatButton = ((ImageButton) nowPlayingView
                            .findViewById(R.id.repeat));
                    mEqButton = ((ImageButton) nowPlayingView
                            .findViewById(R.id.eqbutton));
                    if (ThemeUtils.getAppTheme2(a) == 3)
                        mEqButton
                                .setImageResource(R.drawable.music_eualizer_holo_light);
                    Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                    if (a.getPackageManager().resolveActivity(i, 0) != null) {
                        mEqButton.setVisibility(View.GONE);
                    }
                    mEqButton.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                            try {
                                i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicUtils.sService.getAudioSessionId());
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            a.startActivityForResult(i, 13);
                        }
                    });
                    mShuffleButton
                            .setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {

                                    if (MusicUtils.sService == null) {
                                        return;
                                    }
                                    try {
                                        int shuffle = MusicUtils.sService
                                                .getShuffleMode();
                                        if (shuffle == MediaPlaybackService.SHUFFLE_NONE) {
                                            MusicUtils.sService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
                                            if (MusicUtils.sService.getRepeatMode() == MediaPlaybackService.REPEAT_CURRENT) {
                                                MusicUtils.sService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                                                setRepeatButtonImage(a);
                                            }
                                            InterfaceUtils.showToast(
                                                    a,
                                                    R.string.shuffle_on_notif);
                                        } else if (shuffle == MediaPlaybackService.SHUFFLE_NORMAL) {
                                            MusicUtils.sService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                                            InterfaceUtils.showToast(
                                                    a,
                                                    R.string.shuffle_off_notif);
                                        } else {
                                        }
                                        setShuffleButtonImage(a);
                                    } catch (RemoteException ex) {
                                    }

                                }
                            });
                    mRepeatButton
                            .setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    if (MusicUtils.sService == null) {
                                        return;
                                    }
                                    try {
                                        int mode = MusicUtils.sService.getRepeatMode();
                                        if (mode == MediaPlaybackService.REPEAT_NONE) {
                                            MusicUtils.sService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                                            InterfaceUtils.showToast(
                                                    a,
                                                    R.string.repeat_all_notif);
                                        } else if (mode == MediaPlaybackService.REPEAT_ALL) {
                                            MusicUtils.sService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
                                            if (MusicUtils.sService.getShuffleMode() != MediaPlaybackService.SHUFFLE_NONE) {
                                                MusicUtils.sService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                                                setShuffleButtonImage(a);
                                            }
                                            InterfaceUtils.showToast(
                                                    a,
                                                    R.string.repeat_current_notif);
                                        } else {
                                            MusicUtils.sService.setRepeatMode(MediaPlaybackService.REPEAT_NONE);
                                            InterfaceUtils.showToast(
                                                    a,
                                                    R.string.repeat_off_notif);
                                        }
                                        setRepeatButtonImage(a);
                                    } catch (RemoteException ex) {
                                    }

                                }
                            });
                    if (mProgress instanceof SeekBar) {
                        SeekBar seeker = (SeekBar) mProgress;
                        seeker.setOnSeekBarChangeListener(mSeekListener);
                        seeker.setThumb(ThemeUtils.colorizeDrawable(a.getResources().getDrawable(R.drawable.thumb_seek), a));

                        seeker.setProgressDrawable(ThemeUtils.colorizeDrawable(
                                seeker.getProgressDrawable(), a));
                    }
                    mProgress.setMax(1000);
                    long next = refreshNow(mContext);
                    queueNextRefresh(next);
                    setRepeatButtonImage(a);
                    setShuffleButtonImage(a);

                    mTotalTime.setTextColor(ThemeUtils.getTextColor(a));
                    mCurrentTime.setTextColor(ThemeUtils.getTextColor(a));
                }
                title.setText(MusicUtils.sService.getTrackName());
                String albumName = MusicUtils.sService.getAlbumName();

                Drawable bm = ImageUtils.getArtwork(a,
                        MusicUtils.sService.getAlbumId());
                if (bm == null) {
                    bm = ImageUtils.getDefaultArtwork(a.getApplicationContext());
                }
                setPauseButtonImage(a);
                mAlbum.setImageDrawable(bm);
                if (MediaStore.UNKNOWN_STRING.equals(albumName)) {
                    albumName = a.getString(R.string.unknown_album_name);
                }
                album.setText(albumName);

                if (visible)
                    nowPlayingView.setVisibility(View.VISIBLE);
                else
                    nowPlayingView.setVisibility(View.GONE);

                nowPlayingView
                        .setOnClickListener(new View.OnClickListener() {

                            public void onClick(View v) {
                                Intent intent = new Intent(a,
                                        MediaPlaybackActivity.class);
                                a.startActivity(intent);
                            }
                        });
                mPauseButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            if (MusicUtils.sService != null) {
                                if (MusicUtils.sService.isPlaying()) {
                                    MusicUtils.sService.pause();

                                    if (InterfaceUtils.getTabletMode(a))
                                        refreshNow(mContext);
                                    setPauseButtonImage(a);
                                } else {
                                    MusicUtils.sService.play();

                                    if (InterfaceUtils.getTabletMode(a))
                                        refreshNow(mContext);
                                    setPauseButtonImage(a);
                                }
                            }
                        } catch (RemoteException ex) {
                        }
                    }
                });
                mPrevButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (MusicUtils.sService == null)
                            return;
                        try {
                            if (MusicUtils.sService.position() < 2000) {
                                MusicUtils.sService.prev();
                                if (InterfaceUtils.getTabletMode(a))
                                    refreshNow(mContext);
                                setPauseButtonImage(a);
                            } else {
                                MusicUtils.sService.seek(0);
                                MusicUtils.sService.play();
                                if (InterfaceUtils.getTabletMode(a))
                                    refreshNow(mContext);
                                setPauseButtonImage(a);
                            }
                        } catch (RemoteException ex) {
                        }
                    }
                });
                mNextButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (MusicUtils.sService == null)
                            return;
                        try {
                            MusicUtils.sService.next();
                            if (InterfaceUtils.getTabletMode(a))
                                refreshNow(mContext);
                            setPauseButtonImage(a);
                        } catch (RemoteException ex) {
                        }
                    }
                });
                return;
            }
        } catch (RemoteException ex) {
        }

    }

    @SuppressWarnings("deprecation")
    static void queueNextRefresh(long delay) {
        PowerManager powerManager = (PowerManager) mContext
                .getSystemService(Context.POWER_SERVICE);
        if (powerManager.isScreenOn()) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    static long refreshNow(Context context) {
        if (MusicUtils.sService == null)
            return 500;
        try {
            long pos = mPosOverride < 0 ? MusicUtils.sService.position() : mPosOverride;
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(MusicUtils.makeTimeString(context,
                        pos / 1000));
                int progress = (int) (1000 * pos / mDuration);
                mProgress.setProgress(progress);

                if (MusicUtils.sService.isPlaying()) {
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

    private static void setPauseButtonImage(Context a) {
        try {
            if (MusicUtils.sService != null && MusicUtils.sService.isPlaying()) {
                if (ThemeUtils.getAppTheme2(a) == 3) {
                    mPauseButton.setImageResource(R.drawable.btn_playback_pause_black);
                } else {
                    mPauseButton.setImageResource(R.drawable.btn_playback_pause);
                }
            } else {
                if (ThemeUtils.getAppTheme2(a) == 3) {
                    mPauseButton.setImageResource(R.drawable.btn_playback_play_black);
                } else {
                    mPauseButton.setImageResource(R.drawable.btn_playback_play);
                }
            }
        } catch (RemoteException ex) {
        }
    }

    public static void setRepeatButtonImage(Activity a) {
        if (MusicUtils.sService == null)
            return;
        try {
            switch (MusicUtils.sService.getRepeatMode()) {
                case MediaPlaybackService.REPEAT_ALL:

                    mRepeatButton.setImageDrawable(ThemeUtils.colorizeDrawable(
                            a.getResources().getDrawable(
                                    R.drawable.ic_mp_repeat_all_btn), a));
                    break;
                case MediaPlaybackService.REPEAT_CURRENT:

                    mRepeatButton.setImageDrawable(ThemeUtils.colorizeDrawable(
                            a.getResources().getDrawable(
                                    R.drawable.ic_mp_repeat_once_btn), a));
                    break;
                default:
                    mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_off_btn);
                    break;
            }
        } catch (RemoteException ex) {
        }
    }

    private static void setShuffleButtonImage(Activity a) {
        if (MusicUtils.sService == null)
            return;
        try {
            switch (MusicUtils.sService.getShuffleMode()) {
                case MediaPlaybackService.SHUFFLE_NONE:
                    mShuffleButton
                            .setImageResource(R.drawable.ic_mp_shuffle_off_btn);
                    break;
                default:

                    mShuffleButton.setImageDrawable(ThemeUtils.colorizeDrawable(
                            a.getResources().getDrawable(
                                    R.drawable.ic_mp_shuffle_on_btn), a));
                    break;
            }
        } catch (RemoteException ex) {
        }
    }
}
