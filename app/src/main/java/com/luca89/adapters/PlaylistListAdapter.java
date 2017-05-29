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

package com.luca89.adapters;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.luca89.thundermusic.PlaylistBrowserActivity;
import com.luca89.thundermusic.R;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.ThemeUtils;

@SuppressWarnings("deprecation")
public class PlaylistListAdapter extends SimpleCursorAdapter {
    static int mTitleIdx;
    static int mIdIdx;
    private static AsyncQueryHandler mQueryHandler;
    private static String mConstraint = null;
    private static boolean mConstraintIsValid = false;
    private PlaylistBrowserActivity mActivity = null;
    private View.OnClickListener mButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            PlaylistBrowserActivity.showUpPopup(mActivity, v,
                    (Integer) v.getTag());
        }
    };

    public PlaylistListAdapter(Context context,
                               PlaylistBrowserActivity currentactivity, int layout, Cursor cursor,
                               String[] from, int[] to) {
        super(context, layout, cursor, from, to);
        mActivity = currentactivity;
        getColumnIndices(cursor);
        mQueryHandler = new QueryHandler(context.getContentResolver());
    }

    private void getColumnIndices(Cursor cursor) {
        if (cursor != null) {
            mTitleIdx = cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME);
            mIdIdx = cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID);
        }
    }

    public void setActivity(PlaylistBrowserActivity newactivity) {
        mActivity = newactivity;
    }

    public AsyncQueryHandler getQueryHandler() {
        return mQueryHandler;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        String name = cursor.getString(mTitleIdx);
        long id = cursor.getLong(mIdIdx);
        TextView tv = (TextView) view.findViewById(R.id.line1);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tv
                .getLayoutParams();
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL,
                RelativeLayout.TRUE);
        tv.setPadding(0, 0, 0, 0);
        tv.setLayoutParams(layoutParams);

        tv.setText(name);

        ImageView iv = (ImageView) view.findViewById(R.id.icon);
        if (name.equalsIgnoreCase(context
                .getString(R.string.playlist_favouritesstations)))
            iv.setImageDrawable(ThemeUtils.colorizeResourceDrawable(
                    R.drawable.mixradio_holo_dark, context));
        else
            iv.setImageDrawable(ThemeUtils.colorizeResourceDrawable(
                    R.drawable.you_tube_icon, context));

        ImageView menu = (ImageView) view.findViewById(R.id.menu_button);
        menu.setOnClickListener(mButtonListener);
        menu.setImageDrawable(ThemeUtils.colorizeResourceDrawable(
                R.drawable.ic_action_overflow, mActivity));
        menu.setTag(cursor.getPosition());

        iv = (ImageView) view.findViewById(R.id.play_indicator);
        iv.setVisibility(View.GONE);
        tv.setTextColor(ThemeUtils.getTextColor(context));
    }

    @Override
    public void changeCursor(Cursor cursor) {
        if (mActivity.isFinishing() && cursor != null) {
            cursor.close();
            cursor = null;
        }
        if (cursor != PlaylistBrowserActivity.mPlaylistCursor) {
            getColumnIndices(cursor);
            PlaylistBrowserActivity.mPlaylistCursor = cursor;
            super.changeCursor(cursor);
        }
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        String s = constraint.toString();
        if (mConstraintIsValid
                && ((s == null && mConstraint == null) || (s != null && s
                .equals(mConstraint)))) {
            return getCursor();
        }
        Cursor c = PlaylistBrowserActivity.getPlaylistCursor(null, s,
                mActivity);
        mConstraint = s;
        mConstraintIsValid = true;


        String name = c.getString(mTitleIdx);
        long id = c.getLong(mIdIdx);
        if (id == MusicUtils.getPlaylistiD(name)) {
            return c;
        } else
            return null;
    }

    class QueryHandler extends AsyncQueryHandler {
        QueryHandler(ContentResolver res) {
            super(res);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null) {
                cursor = PlaylistBrowserActivity.filterCursor(cursor);
            }
            mActivity.init(cursor);
        }
    }
}
