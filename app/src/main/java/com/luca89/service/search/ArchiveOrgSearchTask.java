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

import com.luca89.utils.MusicUtils;
import com.luca89.utils.StreamUtils;
import com.luca89.utils.dataset.Library;
import com.luca89.utils.dataset.OnlineTrack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ArchiveOrgSearchTask implements Runnable {
    private final Handler replyTo;
    private final String mSearch;
    private final Context context;
    private final String name;
    private Library mLib;

    public ArchiveOrgSearchTask(Handler replyTo, String search,
                                Context context, String name, Library lib) {
        this.replyTo = replyTo;
        this.mSearch = search;
        this.context = context;
        this.name = name;
        this.mLib = lib;
    }

    /**
     * This will first fetch the JSon from archive.org
     * Then scrap the result to take the data we want
     * Very simple, if you want examinate the json from archive.org to have
     * better idea of how it is organized
     *
     * @param lib
     * @return
     */
    private Library getLibrary(Library lib) {
        try {

            String search = mSearch;
            search = search.replace(" ", "+");
            search = search.replace("'", "");
            search = search.replace("!", "%21");
            search = search.replace("`", "%60");
            String jsonString;
            String title, link, artUrl, artist;
            String nextPageID = lib.getNextPage();

            if (nextPageID == null) {
                jsonString = StreamUtils.convertToString("https://archive.org/advancedsearch.php?q=(" + search + ")+AND+mediatype%3A(audio)&fl[]=identifier&fl[]=description&fl[]=title&rows=50&page=1&output=json");
            } else {
                jsonString = StreamUtils.convertToString("https://archive.org/advancedsearch.php?q=(" + search + ")+AND+mediatype%3A(audio)&fl[]=identifier&fl[]=description&fl[]=title&rows=50&page=" + nextPageID + "&output=json");
            }
            JSONObject json = new JSONObject(jsonString).getJSONObject("response");
            JSONArray jsonArray = json.getJSONArray("docs");

            if (nextPageID == null) {
                nextPageID = "2";
                lib.setNextPage(nextPageID);
            } else {
                int a = Integer.parseInt(nextPageID);
                a++;
                lib.setNextPage(Integer.toString(a));
            }
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonMainObject = jsonArray.getJSONObject(i);

                try {
                    title = jsonMainObject.getString("title");
                } catch (JSONException e) {
                    title = "";
                }
                try {
                    link = "http://www.archive.org/details/"
                            + jsonMainObject.getString("identifier");
                } catch (JSONException e) {
                    link = "";
                }
                try {
                    artist = jsonMainObject.getString("description");
                } catch (JSONException e) {
                    artist = "";
                }
                artUrl = "https://dl.dropboxusercontent.com/u/25106916/Assets/Internet_Archive_artwork.png";

                lib.getVideos().add(
                        new OnlineTrack(title, "Archive.Org", artist, "archive.org", link, artUrl, 0, 0, "Archive.Org"));
            }


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
            mLib = getLibrary(mLib);
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(0);
        } catch (IOException e) {
        }
    }

}