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

import org.joda.time.Period;
import org.joda.time.Seconds;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YoutubeSearch implements Runnable {
    private final Handler replyTo;
    private final String mSearch;
    private final Context context;
    private final String name;
    private Library mLib;

    public YoutubeSearch(Handler replyTo, String username,
                         Context context, String name, Library lib) {
        this.replyTo = replyTo;
        this.mSearch = username;
        this.context = context;
        this.name = name;
        this.mLib = lib;
    }

    private Library getLibrary(Library lib, String nextPage, String key) {
        try {

            String search = mSearch;
            search = search.replace(" ", "+");
            search = search.replace("'", "");
            search = search.replace("!", "%21");
            search = search.replace("`", "%60");
            String jsonString;
            String title, id, thumbUrl, artUrl, artist, urlhq = "";
            int duration, views;
            List<String> videoIDS = new ArrayList<String>();
            String videoIDSurl = "";
            String nextPageID = "";

            if (nextPage == null) {
                jsonString = StreamUtils.convertToString("https://www.googleapis.com/youtube/v3/search?q=" + search + "&type=video&part=snippet&maxResults=50&key=" + key);
            } else {
                jsonString = StreamUtils.convertToString("https://www.googleapis.com/youtube/v3/search?q=" + search + "&type=video&part=snippet&maxResults=50&key=" + key + "&pageToken=" + nextPage);

            }
            JSONObject json = new JSONObject(jsonString);
            JSONArray jsonArray = json.getJSONArray("items");

            if (nextPage == null) {
                nextPageID = json.getString("nextPageToken");
            }

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonMainObject = jsonArray.getJSONObject(i);
                try {
                    videoIDS.add(jsonMainObject.getJSONObject("id").getString(
                            "videoId"));
                } catch (JSONException e) {
                }
            }

            /** Creo stringa unica di IDS per avere i dati **/
            for (int i = 0; i < videoIDS.size(); i++) {
                if (i + 1 < videoIDS.size())
                    videoIDSurl = videoIDSurl + videoIDS.get(i) + "%2C";
                else
                    videoIDSurl = videoIDSurl + videoIDS.get(i);
            }


            /** Cerco tutti i dati **/
            jsonString = StreamUtils
                    .convertToString("https://www.googleapis.com/youtube/v3/videos?id=" + videoIDSurl + "&key=" + key + "&part=contentDetails,statistics,snippet");
            json = new JSONObject(jsonString);
            jsonArray = json.getJSONArray("items");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonMainObject = jsonArray.getJSONObject(i);

                JSONObject jsonSnippetObject = jsonMainObject.getJSONObject("snippet");
                JSONObject jsonDetailstObject = jsonMainObject.getJSONObject("contentDetails");
                JSONObject jsonStatisticstObject = jsonMainObject.getJSONObject("statistics");
                JSONObject jsonThumbnailObject = jsonSnippetObject.getJSONObject("thumbnails");
                try {
                    duration = convertTime(jsonDetailstObject
                            .getString("duration"));
                } catch (JSONException e) {
                    duration = 0;
                }
                try {
                    id = jsonMainObject.getString("id");
                } catch (JSONException e) {
                    id = "";
                }
                try {
                    title = jsonSnippetObject.getString("title");
                } catch (JSONException e) {
                    title = "";
                }
                try {
                    artist = jsonSnippetObject.getString("channelTitle");
                } catch (JSONException e) {
                    artist = "";
                }

                try {
                    views = jsonStatisticstObject.getInt("viewCount");
                } catch (JSONException e) {
                    views = 0;
                }
                try {
                    thumbUrl = jsonThumbnailObject.getJSONObject("default").getString("url");
                } catch (JSONException e) {
                    thumbUrl = "";
                }
                try {
                    artUrl = jsonThumbnailObject.getJSONObject("maxres").getString("url");
                } catch (JSONException e) {
                    try {
                        artUrl = jsonThumbnailObject.getJSONObject("standard").getString("url");
                    } catch (JSONException e1) {
                        try {
                            artUrl = jsonThumbnailObject.getJSONObject("high").getString("url");
                        } catch (JSONException e2) {
                            try {
                                artUrl = jsonThumbnailObject.getJSONObject("medium").getString("url");
                            } catch (JSONException e3) {
                                artUrl = thumbUrl;
                            }
                        }
                    }
                }


                lib.getVideos().add(
                        new OnlineTrack(title, "YouTube", artist, urlhq, "http://www.youtube.com/watch?v=" + id, artUrl, duration, views, "Youtube"));
            }

            if (nextPage == null) {
                return getLibrary(lib, nextPageID, key);
            } else {
                return lib;
            }
            // We don't do any error catching, just nothing will happen if this
            // task falls over
            // an idea would be to reply to the handler with a different message
            // so your Activity can act accordingly
        } catch (JSONException e) {
        }

        return getLibrary(lib, nextPage, Decrypt.getKey(Decrypt.YT_API_KEY_2, context));
    }


    @Override
    public void run() {
        try {
            mLib = getLibrary(mLib, null, Decrypt.getKey(Decrypt.YT_API_KEY, context));
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(0);
        } catch (IOException e) {
        }
    }

    private int convertTime(String time) {
        PeriodFormatter formatter = ISOPeriodFormat.standard();
        Period p = formatter.parsePeriod(time);
        Seconds s = p.toStandardSeconds();

        return s.getSeconds();
    }
}