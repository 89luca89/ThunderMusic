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

import java.io.IOException;

public class FreeMusicArchiveOrgSearchTask implements Runnable {
    private final Handler replyTo;
    private final String mSearch;
    private final Context context;
    private final String name;
    private Library mLib;

    public FreeMusicArchiveOrgSearchTask(Handler replyTo, String search,
                                         Context context, String name, Library lib) {
        this.replyTo = replyTo;
        this.mSearch = search;
        this.context = context;
        this.name = name;
        this.mLib = lib;
    }

    /**
     * This will first fetch the html page from Freemusicarchive
     * Then scrap the result to take the data we want
     * Follow the steps as commented with the html page
     * to see how i'm moving
     *
     * @param lib
     * @return
     */
    private Library getLibrary(Library lib) {
        String search = mSearch;
        search = search.replace(" ", "+");
        search = search.replace("'", "");
        search = search.replace("!", "%21");
        search = search.replace("`", "%60");
        String html;
        String title = "", artUrl, artist = "", album = "", url = "", link = "";
        String nextPageID = lib.getNextPage();

        if (nextPageID == null) {
            html = StreamUtils.convertToString("http://freemusicarchive.org/search/?quicksearch=" + search + "&page=1&per_page=50&sort=track_interest");
        } else {
            html = StreamUtils.convertToString("http://freemusicarchive.org/search/?quicksearch=" + search + "&page=" + nextPageID + "&per_page=50&sort=track_interest");
        }
        if (nextPageID == null) {
            nextPageID = "2";
            lib.setNextPage(nextPageID);
        } else {
            int a = Integer.parseInt(nextPageID);
            a++;
            lib.setNextPage(Integer.toString(a));
        }
        // Check if no results
        if (html.contains("No results")) {
            return lib;
        }
        if (html.isEmpty()) {
            return lib;
        }
        // Strart scraping the web page
        html = html.substring(html.indexOf("<div class=\"play-item gcol gid-electronic tid-"));
        html = html.replace("<div class=\"play-item gcol gid-electronic tid-", "MARKER");
        html = html.replace("\t", " ");
        String[] results = html.split("MARKER");
        for (String entry : results) {
            if (entry.contains("<div")) {
                // Clean up the entry
                entry = entry.substring(entry.indexOf("<div"));
                if (entry.contains("<span class=\"ptxt-artist\">\"")) {
                    entry = entry.substring(entry.indexOf(" <span class=\"ptxt-artist\">"));
                    // Take artist name
                    try {
                        artist = entry.substring(entry.indexOf("<a href="), entry.indexOf("</a>"));
                        artist = artist.substring(artist.indexOf(">") + 1);
                    } catch (StringIndexOutOfBoundsException e) {
                        artist = "";
                    }
                }
                if (entry.contains("<span class=\"ptxt-track\">")) {
                    // Artist span is gone, go to the next span
                    entry = entry.substring(entry.indexOf("<span class=\"ptxt-track\">"));
                    entry = entry.substring(entry.indexOf("<a href=\"") + 9);
                    link = entry.substring(0, entry.indexOf("\"><b>\""));
                    try {
                        title = entry.substring(entry.indexOf("<b>\"") + 4, entry.indexOf("\"</b>"));
                    } catch (StringIndexOutOfBoundsException e) {
                        title = "";
                    }
                }
                // Title span is gone, go to the next span
                if (entry.contains("<span class=\"ptxt-album\">")) {
                    entry = entry.substring(entry.indexOf("<span class=\"ptxt-album\">"));

                    try {
                        album = entry.substring(entry.indexOf("<b>\"") + 4, entry.indexOf("\"</b>"));
                    } catch (StringIndexOutOfBoundsException e) {
                        album = "";
                    }
                }
                // Album span is gone, go to the URL and end
                if (entry.contains("<span class=\"playicn\">")) {
                    entry = entry.substring(entry.indexOf("<span class=\"playicn\">"));

                    try {
                        url = entry.substring(entry.indexOf("<a href=\"") + 9);
                        url = url.substring(0, url.indexOf("\" "));
                    } catch (StringIndexOutOfBoundsException e) {
                        url = "";
                    }
                }

                artUrl = "https://dl.dropboxusercontent.com/u/25106916/Assets/fma_logo.jpg";

                String albumArtist;
                if (album.isEmpty()) {
                    albumArtist = artist;
                } else if (artist.isEmpty()) {
                    albumArtist = album;
                } else {
                    albumArtist = album + "-" + artist;
                }
                lib.getVideos().add(
                        new OnlineTrack(title, "FreeMusicArchive.Org", albumArtist, url, link, artUrl, 0, 0, "FreeMusicArchive.Org"));
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
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(0);
        } catch (IOException e) {
        }
    }
}
