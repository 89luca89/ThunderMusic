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

package com.luca89.service.search;

import android.content.Context;
import android.os.Handler;

import com.luca89.utils.Decrypt;
import com.luca89.utils.MusicUtils;
import com.luca89.utils.StreamUtils;
import com.luca89.utils.dataset.Library;
import com.luca89.utils.dataset.OnlineTrack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

public class SoundcloudSearchTask implements Runnable {
    private final Handler replyTo;
    private final String mSearch;
    private final Context context;
    private final String name;
    private Library mLib;

    public SoundcloudSearchTask(Handler replyTo, String username,
                                Context context, String name, Library lib) {
        this.replyTo = replyTo;
        this.mSearch = username;
        this.context = context;
        this.name = name;
        this.mLib = lib;
    }

    /**
     * This will first fetch the JSon result from Soundcloud
     * Then scrap the result to take the data we want
     * <p/>
     * Soundcloud does NOT provide a simple way to order results
     * so firts i'm fetching all results and then sort them by view_count
     * <p/>
     * Little tricky but necessary to have decent results
     *
     * @param lib
     * @return
     */
    private Library getLibrary(Library lib, String key) {
        try {

            String search = mSearch;
            search = search.replace(" ", "%20");
            search = search.replace("'", "");
            search = search.replace("!", "%21");
            search = search.replace("`", "%60");
            String jsonString;
            String title, link, artUrl, artist, urlhq;
            int duration, views;

            jsonString = StreamUtils.convertToString("http://api.soundcloud.com/tracks.json?client_id=" + key + "&q=" + search + "&limit=100");

            // Fallback to key2 only if we are using key1
            // If we are using key2 already, just fail gracefully
            if (jsonString == "") {
                if (key.equals(Decrypt.SOUNDCLOUD_API_KEY))
                    return getLibrary(lib, Decrypt.getKey(Decrypt.SOUNDCLOUD_API_KEY_2, context));
                else return lib;
            }

            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonMainObject = jsonArray.getJSONObject(i);

                try {
                    duration = Integer.parseInt(jsonMainObject.getString("duration")) / 1000;
                } catch (JSONException e) {
                    duration = 0;
                }
                try {
                    link = jsonMainObject.getString("permalink_url");
                } catch (JSONException e) {
                    link = "";
                }
                try {
                    title = jsonMainObject.getString("title");
                } catch (JSONException e) {
                    title = "";
                }
                try {
                    artist = jsonMainObject.getJSONObject("user").getString("username");
                } catch (JSONException e) {
                    artist = "";
                }

                try {
                    views = jsonMainObject.getInt("playback_count");
                } catch (JSONException e) {
                    views = 0;
                }

                try {
                    artUrl = jsonMainObject.getString("artwork_url").replace("large", "t500x500");
                } catch (JSONException e) {
                    artUrl = "https://dl.dropboxusercontent.com/u/25106916/Assets/soundcloud_logo.jpeg";
                }

                if (artUrl.equals("null"))
                    artUrl = "https://dl.dropboxusercontent.com/u/25106916/Assets/soundcloud_logo.jpeg";

                try {
                    urlhq = jsonMainObject.getString("stream_url") + "?client_id=" + Decrypt.getKey(Decrypt.SOUNDCLOUD_API_KEY, context);
                } catch (JSONException e) {
                    urlhq = "stream_url";
                }

                lib.getVideos().add(
                        new OnlineTrack(title, "Soundcloud", artist, urlhq, link, artUrl, duration, views, "Soundcloud"));
            }

            // Order by playback_count
            Collections.sort(lib.getVideos(), new Comparator<OnlineTrack>() {
                public int compare(OnlineTrack s1, OnlineTrack s2) {
                    return s2.getViews() - (s1.getViews());
                }
            });
            return lib;
            // We don't do any error catching, just nothing will happen if this
            // task falls over
            // an idea would be to reply to the handler with a different message
            // so your Activity can act accordingly
        } catch (JSONException e) {
        }

        return null;
    }

    @Override
    public void run() {
        try {
            mLib = getLibrary(mLib, Decrypt.getKey(Decrypt.SOUNDCLOUD_API_KEY, context));
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(0);
        } catch (IOException e) {
        }
    }
}