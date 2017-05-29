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

package com.luca89.thundermusic.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.View;
import android.widget.RemoteViews;

import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.OnlineActivity;
import com.luca89.thundermusic.R;
import com.luca89.utils.ImageUtils;

/**
 * Simple widget to show currently playing album art along with play/pause and
 * next track buttons.
 */
public class MediaAppWidgetProvider4x2 extends AppWidgetProvider {
    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate4x2";
    private static MediaAppWidgetProvider4x2 sInstance;

    public static synchronized MediaAppWidgetProvider4x2 getInstance() {
        if (sInstance == null) {
            sInstance = new MediaAppWidgetProvider4x2();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);

        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(MediaPlaybackService.SERVICECMD);
        updateIntent.putExtra(MediaPlaybackService.CMDNAME,
                MediaAppWidgetProvider4x2.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    /**
     * Initialize given widgets to default state, where we launch Music on
     * default click and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.album_appwidget4x2);

        views.setViewVisibility(R.id.albumname, View.GONE);
        views.setViewVisibility(R.id.trackname, View.GONE);
        views.setTextViewText(R.id.artistname,
                res.getText(R.string.widget_initial_text));
        views.setImageViewResource(R.id.albumart,
                R.drawable.albumart_mp_unknown);

        linkButtons(context, views);
        pushUpdate(context, appWidgetIds, views);
    }

    private void pushUpdate(Context context, int[] appWidgetIds,
                            RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to
        // all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()),
                    views);
        }
    }

    /**
     * Check against {@link AppWidgetManager} if there are any instances of this
     * widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);
        int[] appWidgetIds = appWidgetManager
                .getAppWidgetIds(new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }

    /**
     * Handle a change notification coming over from
     * {@link MediaPlaybackService}
     */
    public void notifyChange(MediaPlaybackService service, String what) {
        if (hasInstances(service)) {
            if (MediaPlaybackService.META_CHANGED.equals(what)
                    || MediaPlaybackService.PLAYSTATE_CHANGED.equals(what)
                    || MediaPlaybackService.REPEATMODE_CHANGED.equals(what)
                    || MediaPlaybackService.SHUFFLEMODE_CHANGED.equals(what)) {
                performUpdate(service, null);
            }
        }
    }

    /**
     * Update all active widget instances by pushing changes
     */
    public void performUpdate(MediaPlaybackService service, int[] appWidgetIds) {
        final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(),
                R.layout.album_appwidget4x2);

        CharSequence artistName = service.getArtistName();
        CharSequence albumName = service.getAlbumName();
        CharSequence trackName = service.getTrackName();
        long albumId = service.getAlbumId();
        CharSequence errorState = null;

        // Format title string with track number, or show SD card message
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_SHARED)
                || status.equals(Environment.MEDIA_UNMOUNTED)) {
            if (Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_busy_title);
            } else {
                errorState = res.getText(R.string.sdcard_busy_title_nosdcard);
            }
        } else if (status.equals(Environment.MEDIA_REMOVED)) {
            if (Environment.isExternalStorageRemovable()) {
                errorState = res.getText(R.string.sdcard_missing_title);
            } else {
                errorState = res
                        .getText(R.string.sdcard_missing_title_nosdcard);
            }
        } else if (trackName == null) {
            errorState = res.getText(R.string.emptyplaylist);
        }

        if (errorState != null) {
            // Show error state to user
            views.setViewVisibility(R.id.albumname, View.GONE);
            views.setViewVisibility(R.id.trackname, View.GONE);
            views.setTextViewText(R.id.artistname, errorState);
            views.setImageViewResource(R.id.albumart,
                    R.drawable.albumart_mp_unknown);
        } else {
            // No error, so show normal titles and artwork
            views.setViewVisibility(R.id.albumname, View.VISIBLE);
            views.setViewVisibility(R.id.trackname, View.VISIBLE);
            views.setTextViewText(R.id.artistname, artistName);
            views.setTextViewText(R.id.albumname, albumName);
            views.setTextViewText(R.id.trackname, trackName);
            // Set album art
            Bitmap art = ((BitmapDrawable) ImageUtils.getArtwork(service, albumId)).getBitmap();
            Drawable temp = ImageUtils.getArtworkDownloaded(service, albumId);
            if (temp != null) {
                art = ((BitmapDrawable) temp).getBitmap();
            }

            if (art != null) {
                views.setImageViewBitmap(R.id.albumart, art);
            } else {
                views.setImageViewResource(R.id.albumart,
                        R.drawable.albumart_mp_unknown);
            }
        }

        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play,
                    android.R.drawable.ic_media_pause);
        } else {
            views.setImageViewResource(R.id.control_play,
                    android.R.drawable.ic_media_play);
        }

        // Set correct drawable for repeat state
        switch (service.getRepeatMode()) {
            case MediaPlaybackService.REPEAT_ALL:
                views.setImageViewResource(R.id.control_repeat,
                        R.drawable.ic_mp_repeat_all_btn);
                break;
            case MediaPlaybackService.REPEAT_CURRENT:
                views.setImageViewResource(R.id.control_repeat,
                        R.drawable.ic_mp_repeat_once_btn);
                break;
            default:
                views.setImageViewResource(R.id.control_repeat,
                        R.drawable.ic_mp_repeat_off_btn);
                break;
        }

        // Set correct drawable for shuffle state
        switch (service.getShuffleMode()) {
            case MediaPlaybackService.SHUFFLE_NONE:
                views.setImageViewResource(R.id.control_shuffle,
                        R.drawable.ic_mp_shuffle_off_btn);
                break;
            default:
                views.setImageViewResource(R.id.control_shuffle,
                        R.drawable.ic_mp_shuffle_on_btn);
                break;
        }
        // Link actions buttons to intents
        linkButtons(service, views);

        pushUpdate(service, appWidgetIds, views);
    }


    private void linkButtons(Context context, RemoteViews views) {
        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(context,
                MediaPlaybackService.class);

        intent = new Intent(context, OnlineActivity.class);
        pendingIntent = PendingIntent.getActivity(context,
                0 /* no requestCode */, intent, 0 /*
                                                 * no flags
												 */);
        views.setOnClickPendingIntent(R.id.albumart, pendingIntent);
        views.setOnClickPendingIntent(R.id.info, pendingIntent);

        intent = new Intent(MediaPlaybackService.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);

        intent = new Intent(MediaPlaybackService.NEXT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);

        intent = new Intent(MediaPlaybackService.PREVIOUS_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_prev, pendingIntent);

        intent = new Intent(MediaPlaybackService.CYCLEREPEAT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_repeat, pendingIntent);

        intent = new Intent(MediaPlaybackService.TOGGLESHUFFLE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_shuffle, pendingIntent);
    }
}
