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

import org.jsoup.parser.Parser;

import java.io.IOException;

public class BandcampSearchTask implements Runnable {
    private final Handler replyTo;
    private final String mSearch;
    private final Context context;
    private final String name;
    private Library mLib;

    public BandcampSearchTask(Handler replyTo, String search,
                              Context context, String name, Library lib) {
        this.replyTo = replyTo;
        this.mSearch = search;
        this.context = context;
        this.name = name;
        this.mLib = lib;
    }

    /**
     * This will first fetch the html page from BandCamp
     * Then scrap the result to take the data we want
     * Follow the steps as commented with the html page
     * to see how i'm moving
     *
     * @param lib
     * @return
     */
    private Library getLibrary(Library lib) {

        String search = mSearch;
        search = search.replace(" ", "%20");
        search = search.replace("'", "");
        search = search.replace("!", "%21");
        search = search.replace("`", "%60");
        String html;
        String title, artUrl, artist, album, url;
        String nextPageID = lib.getNextPage();

        if (nextPageID == null) {
            html = StreamUtils.convertToString("https://bandcamp.com/search?q=" + search);
        } else {
            html = StreamUtils.convertToString("https://bandcamp.com/search?q=" + search + "&page=" + nextPageID);
        }
        if (nextPageID == null) {
            nextPageID = "2";
            lib.setNextPage(nextPageID);
        } else {
            int a = Integer.parseInt(nextPageID);
            a++;
            lib.setNextPage(Integer.toString(a));
        }

        if (html.isEmpty()) {
            return lib;
        }
        // Strart scraping the web page
        html = html.substring(html.indexOf("<ul class=\"result-items\">"));
        String[] results = html.split("<li class=\"searchresult track\">");
        for (String entry : results) {
            // Here I have exaclty one search entry
            // Let's start scraping the data

            if (entry.contains("TRACK")) {

                artUrl = entry.substring(entry.indexOf("<img src=\"") + 10);
                artUrl = artUrl.substring(0, artUrl.indexOf("\">"));
                artUrl = artUrl.replace("_7.", "_16.");
                // Art and Thumb url done

                title = entry.substring(entry.indexOf("<div class=\"heading\">") + 21);
                title = title.substring(title.indexOf(">") + 1);
                title = title.substring(0, title.indexOf("</a>"));
                title = title.trim();
                title = title.replace("\n", "");
                title = title.replace("\t", "");
                title = Parser.unescapeEntities(title, false);
                // Title done

                String subhead = entry.substring(entry.indexOf("<div class=\"subhead\">") + 21);
                subhead = subhead.substring(0, subhead.indexOf("</div>"));
                subhead = subhead.trim();
                subhead = subhead.replace("\n", "");
                subhead = subhead.replace("\t", "");

                if (subhead.contains("from")) {
                    album = subhead.substring(subhead.indexOf("from ") + 5, subhead.indexOf("by"));
                    album = Parser.unescapeEntities(album, false);
                    album = album.trim();
                } else {
                    album = "BandCamp";
                }

                artist = subhead.substring(subhead.indexOf("by") + 3);
                artist = Parser.unescapeEntities(artist, false);
                artist = artist.trim();
                // Album and Artist done

                url = entry.substring(entry.indexOf("<div class=\"itemurl\">") + 21);
                url = url.substring(url.indexOf(">") + 1, url.indexOf("</a>"));
                String link = url;
                // Url done

                if (artUrl.isEmpty()) {
                    artUrl = "https://dl.dropboxusercontent.com/u/25106916/Assets/bandcamp_logo.jpg";
                }

                lib.getVideos().add(
                        new OnlineTrack(title, album, artist, url, link, artUrl, 0, 0, "BandCamp"));
            }

        }

        return lib;
        // We don't do any error catching, just nothing will happen if this
        // task falls over
        // an idea would be to reply to the handler with a different message
        // so your Activity can act accordingly
    }


    @Override
    public void run() {
        try {
            mLib = getLibrary(mLib);
            mLib = getLibrary(mLib);
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(0);
        } catch (IOException e) {
        }
    }
}