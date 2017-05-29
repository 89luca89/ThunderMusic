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
import android.os.Environment;
import android.view.View;
import android.widget.RemoteViews;

import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.OnlineActivity;
import com.luca89.thundermusic.R;

/**
 * Simple widget to show currently playing album art along with play/pause and
 * next track buttons.
 */
public class MediaAppWidgetProvider2x1_Light extends AppWidgetProvider {
    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate";
    private static MediaAppWidgetProvider2x1_Light sInstance;

    public static synchronized MediaAppWidgetProvider2x1_Light getInstance() {
        if (sInstance == null) {
            sInstance = new MediaAppWidgetProvider2x1_Light();
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
                MediaAppWidgetProvider2x1_Light.CMDAPPWIDGETUPDATE);
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
                R.layout.album_appwidget2x1_light);

        views.setViewVisibility(R.id.title, View.GONE);
        views.setTextViewText(R.id.artist,
                res.getText(R.string.widget_initial_text));
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
                    || MediaPlaybackService.PLAYSTATE_CHANGED.equals(what)) {
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
                R.layout.album_appwidget2x1_light);

        CharSequence titleName = service.getTrackName();
        CharSequence artistName = service.getArtistName();
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
        } else if (titleName == null) {
            errorState = res.getText(R.string.emptyplaylist);
        }

        if (errorState != null) {
            // Show error state to user
            views.setViewVisibility(R.id.title, View.GONE);
            views.setTextViewText(R.id.artist, errorState);
        } else {
            // No error, so show normal titles and artwork
            views.setViewVisibility(R.id.title, View.VISIBLE);
            views.setTextViewText(R.id.title, titleName);
            views.setTextViewText(R.id.artist, artistName);
        }

        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play,
                    R.drawable.btn_playback_pause_black);
        } else {
            views.setImageViewResource(R.id.control_play,
                    R.drawable.btn_playback_play_black);
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
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);

        intent = new Intent(MediaPlaybackService.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);

        intent = new Intent(MediaPlaybackService.PREVIOUS_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_prev, pendingIntent);

        intent = new Intent(MediaPlaybackService.NEXT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
    }
}
