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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.luca89.thundermusic.R;
import com.luca89.thundermusic.activities.TrackOnlinePlaylistBrowser;
import com.luca89.thundermusic.fragments.online.ArchiveOrgFragment;
import com.luca89.thundermusic.fragments.online.BandcampFragment;
import com.luca89.thundermusic.fragments.online.FreeMusicArchiveOrgFragment;
import com.luca89.thundermusic.fragments.online.OnlineSearchAllFragment;
import com.luca89.thundermusic.fragments.online.SoundcloudFragment;
import com.luca89.thundermusic.fragments.online.YoutubeFragment;
import com.luca89.utils.InterfaceUtils;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.ThemeUtils;
import com.luca89.utils.dataset.OnlineTrack;

import java.util.List;

public class VideosAdapter extends BaseAdapter {
    // The list of videos to display
    private static List<OnlineTrack> videos;
    // An inflator to use when creating rows
    private LayoutInflater mInflater;
    private Activity mActivity;
    private String mCaller;
    private boolean mPlaylist;
    private View.OnClickListener mButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            // mActivity.sho
            if (mPlaylist)
                TrackOnlinePlaylistBrowser.showUpPopup(mActivity, v,
                        (Integer) v.getTag());
            else {
                if (mCaller.equalsIgnoreCase("onlinetotal")) {
                    OnlineSearchAllFragment.showUpPopup(mActivity, v,
                            (Integer) v.getTag());
                } else if (videos.get((Integer) v.getTag()).getProvider().equalsIgnoreCase("soundcloud")) {
                    SoundcloudFragment.showUpPopup(mActivity, v,
                            (Integer) v.getTag());
                } else if (videos.get((Integer) v.getTag()).getProvider().equalsIgnoreCase("archive.org")) {
                    ArchiveOrgFragment.showUpPopup(mActivity, v,
                            (Integer) v.getTag());
                } else if (videos.get((Integer) v.getTag()).getProvider().equalsIgnoreCase("freemusicarchive.org")) {
                    FreeMusicArchiveOrgFragment.showUpPopup(mActivity, v,
                            (Integer) v.getTag());
                } else if (videos.get((Integer) v.getTag()).getProvider().equalsIgnoreCase("bandcamp")) {
                    BandcampFragment.showUpPopup(mActivity, v,
                            (Integer) v.getTag());
                } else if (videos.get((Integer) v.getTag()).getProvider().equalsIgnoreCase("youtube")) {
                    YoutubeFragment.showUpPopup(mActivity, v,
                            (Integer) v.getTag());
                }
            }
        }
    };

    /**
     * @param videos this is a list of videos to display
     */
    public VideosAdapter(Activity activity, List<OnlineTrack> videos, boolean playlist, String caller) {
        this.videos = videos;
        this.mInflater = LayoutInflater.from(activity);
        this.mActivity = activity;
        this.mPlaylist = playlist;
        this.mCaller = caller;
    }

    public void updateAdapter(List<OnlineTrack> newvideos) {
        videos = newvideos;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return videos.size();
    }

    @Override
    public Object getItem(int position) {
        return videos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("DefaultLocale")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // If convertView wasn't null it means we have already set it to our
        // list_item_user_video so no need to do it again
        ViewHolder vh;
        OnlineTrack video = videos.get(position);
        if (convertView == null) {
            // This is the layout we are using for each row in our list
            // anything you declare in this layout can then be referenced below
            convertView = mInflater.inflate(
                    mPlaylist ? R.layout.edit_track_list_item
                            : ThemeUtils.getAppTheme2(mActivity) == 3 ? R.layout.track_list_item_online_card_grid
                            : R.layout.track_list_item_online_card_grid_dark, parent, false);
            vh = new ViewHolder();
            vh.thumb = (ImageView) convertView.findViewById(R.id.icon);
            vh.title = (TextView) convertView
                    .findViewById(R.id.line1);
            vh.artist = (TextView) convertView.findViewById(R.id.line2);
            vh.menu = (ImageView) convertView.findViewById(R.id.menu_button);
            vh.playindicator = (ImageView) convertView
                    .findViewById(R.id.play_indicator);

            vh.provider = (TextView) convertView.findViewById(R.id.line3);
            vh.duration = (TextView) convertView.findViewById(R.id.line4);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        vh.menu.setTag(position);
        vh.menu.setOnClickListener(mButtonListener);
        String url = "";
        if (MusicUtils.sService != null) {
            try {
                url = MusicUtils.sService.getTrackLink();
            } catch (RemoteException ex) {
            }
        }
        if (video.getLink().equals(url) && mPlaylist) {
            vh.playindicator
                    .setImageResource(R.drawable.indicator_ic_mp_playing_list);
            vh.playindicator.setVisibility(View.VISIBLE);
        } else {
            vh.playindicator.setVisibility(View.GONE);
        }
        if (!mPlaylist) {
            vh.thumb.setImageResource(InterfaceUtils.getOnlineDefaultArtworkRes(video.getProvider()));
        } else {
            vh.thumb.setImageDrawable(ThemeUtils.colorizeResourceDrawable(
                    R.drawable.ic_mp_playlist_list, mActivity));
        }
        if (video.getDuration() != 0) {
            vh.duration.setVisibility(View.VISIBLE);
            vh.duration.setText(video.getDurationString(mActivity));
        } else {
            vh.duration.setVisibility(View.GONE);
        }

        // Set the title for the list item
        vh.title.setText(video.getTitle());
        vh.artist.setText(video.getArtist());
        vh.menu.setImageDrawable(ThemeUtils.colorizeResourceDrawable(
                R.drawable.ic_action_overflow, mActivity));
        vh.provider.setText(video.getProvider());
        vh.title.setTextColor(ThemeUtils.getTextColor(mActivity));
        vh.duration.setTextColor(ThemeUtils.getTextColor(mActivity));
        vh.provider.setTextColor(ThemeUtils.getTextColor(mActivity));
        vh.artist.setTextColor(ThemeUtils.getTextColor(mActivity));
        return convertView;
    }

    static class ViewHolder {
        ImageView thumb;
        TextView title;
        TextView artist;
        TextView duration;
        TextView provider;
        ImageView playindicator;
        ImageView menu;
    }
}
