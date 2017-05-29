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

import com.luca89.utils.Decrypt;
import com.luca89.utils.StreamUtils;
import com.luca89.utils.dataset.Library;
import com.luca89.utils.dataset.OnlineTrack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class OnlineRadioSearchTask {

    private static String getSearch(String search) {
        search = search.replace(" ", "+");
        search = search.replace("'", "");
        search = search.replace("!", "%21");
        search = search.replace("`", "%60");
        return search;
    }

    /**
     * This will first fetch the XML result from shoutcast API
     * Then scrap the result to take the data we want
     * <p/>
     * Other instances of GetLibrary are only a variation of this
     * to take different results
     *
     * @param lib
     * @return
     */
    public static Library getLibrarySearched(Library lib, String search, Context context) {

        String newsearch = getSearch(search);
        String page = StreamUtils
                .convertToString("http://api.shoutcast.com/legacy/stationsearch?k="
                        + Decrypt.getKey(Decrypt.SHOUTCAST_API_KEY, context) + "&search=" + newsearch);

        if (page.isEmpty()) {
            return lib;
        }
        String[] results = page.split("/>");
        String title, id, genre, quality, url, artUrl;
        artUrl = "https://dl.dropboxusercontent.com/u/25106916/Assets/shoutcast_logo.jpg";
        for (int i = 0; i < results.length; i++) {
            if (results[i].indexOf("name=") >= 0) {
                title = results[i].substring(
                        results[i].indexOf("name=") + 6);
                title = title.substring(0, title.indexOf('\"'));
                id = results[i].substring(results[i].indexOf("id=") + 4);
                id = id.substring(0, id.indexOf('\"'));
                quality = results[i].substring(
                        results[i].indexOf("br=") + 4);
                quality = quality.substring(0, quality.indexOf('\"'));
                genre = results[i].substring(
                        results[i].indexOf("genre=") + 7);
                genre = genre.substring(0, genre.indexOf('\"'));
                url = "http://yp.shoutcast.com/sbin/tunein-station.pls?id="
                        + id;

                title = title.replaceAll("&amp;", "&");
                lib.getVideos().add(
                        new OnlineTrack(title, search, genre, url, id,
                                artUrl, 0, Integer.parseInt(quality), "Shoutcast", true));
            }
        }
        if (lib.getVideos().isEmpty()) {
            for (int i = 0; i < 2; i++) {
                lib = getLibraryTOP500(lib, context); //FALLBACK
            }
        }
        return lib;
    }

    public static LinkedHashMap<String, String> getSubGenreList(String genrename, Context context) {

        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        String genrelist = StreamUtils
                .convertToString("http://api.shoutcast.com/genre/primary?k="
                        + Decrypt.getKey(Decrypt.SHOUTCAST_API_KEY, context) + "&f=xml");

        if (genrelist.isEmpty()) {
            return null;
        }
        genrename = genrename.replace("&", "&amp;");
        String[] genreresults = genrelist.split("/>");
        String id = null;
        for (int i = 0; i < genreresults.length; i++) {
            if (genreresults[i].indexOf("name=") >= 0) {
                if (genreresults[i].contains(genrename)) {
                    id = genreresults[i].substring(
                            genreresults[i].indexOf("id=") + 4,
                            genreresults[i].indexOf("parentid=") - 2);
                }
            }
        }

        String subgenrelist = StreamUtils
                .convertToString("http://api.shoutcast.com/genre/secondary?parentid="
                        + id + "&k=" + Decrypt.getKey(Decrypt.SHOUTCAST_API_KEY, context) + "&f=xml");

        String[] subgenreresults = subgenrelist.split("/>");
        for (int i = 0; i < subgenreresults.length; i++) {
            if (subgenreresults[i].indexOf("name=") >= 0) {
                String title = subgenreresults[i].substring(
                        subgenreresults[i].indexOf("name=") + 6,
                        subgenreresults[i].indexOf("id=") - 2);
                String subid = subgenreresults[i].substring(
                        subgenreresults[i].indexOf("id=") + 4,
                        subgenreresults[i].indexOf("parentid=") - 2);

                title = title.replace("&amp;", "&");
                result.put(title, subid);
            }
        }
        return result;
    }

    public static Library getLibraryGenre(Library lib,
                                          int genid, Context context) {
        String page = StreamUtils
                .convertToString("http://api.shoutcast.com/station/advancedsearch?genre_id="
                        + genid + "&f=xml&k=" + Decrypt.getKey(Decrypt.SHOUTCAST_API_KEY, context));

        if (page.isEmpty()) {
            return null;
        }
        String[] results = page.split("/>");
        String title, id, genre, quality, url, artUrl;
        artUrl = "https://dl.dropboxusercontent.com/u/25106916/Assets/shoutcast_logo.jpg";

        for (int i = 0; i < results.length; i++) {
            if (results[i].indexOf("name=") >= 0) {
                title = results[i].substring(
                        results[i].indexOf("name=") + 6);
                title = title.substring(0, title.indexOf('\"'));
                id = results[i].substring(results[i].indexOf("id=") + 4);
                id = id.substring(0, id.indexOf('\"'));
                quality = results[i].substring(
                        results[i].indexOf("br=") + 4);
                quality = quality.substring(0, quality.indexOf('\"'));
                genre = results[i].substring(
                        results[i].indexOf("genre=") + 7);
                genre = genre.substring(0, genre.indexOf('\"'));
                url = "http://yp.shoutcast.com/sbin/tunein-station.pls?id="
                        + id;
                title = title.replaceAll("&amp;", "&");
                lib.getVideos().add(
                        new OnlineTrack(title, "ShoutCast Radio", genre, url, id,
                                artUrl, 0, Integer.parseInt(quality), "Shoutcast", true));
            }
        }
        return lib;
    }

    public static Library getLibraryTOP500(Library lib, Context context) {

        String page = StreamUtils
                .convertToString("http://api.shoutcast.com/legacy/Top500?k="
                        + Decrypt.getKey(Decrypt.SHOUTCAST_API_KEY, context));

        if (page.isEmpty()) {
            return null;
        }
        String[] results = page.split("/>");
        String title, id, genre, quality, url, artUrl;
        artUrl = "https://dl.dropboxusercontent.com/u/25106916/Assets/shoutcast_logo.jpg";
        for (int i = 0; i < results.length; i++) {
            if (results[i].indexOf("name=") >= 0) {
                title = results[i].substring(
                        results[i].indexOf("name=") + 6);
                title = title.substring(0, title.indexOf('\"'));
                id = results[i].substring(results[i].indexOf("id=") + 4);
                id = id.substring(0, id.indexOf('\"'));
                quality = results[i].substring(
                        results[i].indexOf("br=") + 4);
                quality = quality.substring(0, quality.indexOf('\"'));
                genre = results[i].substring(
                        results[i].indexOf("genre=") + 7);
                genre = genre.substring(0, genre.indexOf('\"'));
                url = "http://yp.shoutcast.com/sbin/tunein-station.pls?id="
                        + id;

                title = title.replaceAll("&amp;", "&");

                lib.getVideos()
                        .add(new OnlineTrack(title, "ShoutCast Radio", genre, url,
                                id, artUrl, 0, Integer.parseInt(quality), "Shoutcast", true));
            }
        }
        return lib;
    }

    public static Library getLibraryRandom(Library lib, Context context) {

        String page = StreamUtils
                .convertToString("http://api.shoutcast.com/station/randomstations?f=xml&k="
                        + Decrypt.getKey(Decrypt.SHOUTCAST_API_KEY, context));

        if (page.isEmpty()) {
            return null;
        }
        String[] results = page.split("/>");
        String title, id, genre, quality, url, artUrl;
        artUrl = "https://dl.dropboxusercontent.com/u/25106916/Assets/shoutcast_logo.jpg";
        for (int i = 0; i < results.length; i++) {
            if (results[i].indexOf("name=") >= 0) {
                title = results[i].substring(
                        results[i].indexOf("name=") + 6);
                title = title.substring(0, title.indexOf('\"'));
                id = results[i].substring(results[i].indexOf("id=") + 4);
                id = id.substring(0, id.indexOf('\"'));
                quality = results[i].substring(
                        results[i].indexOf("br=") + 4);
                quality = quality.substring(0, quality.indexOf('\"'));
                genre = results[i].substring(
                        results[i].indexOf("genre=") + 7);
                genre = genre.substring(0, genre.indexOf('\"'));
                url = "http://yp.shoutcast.com/sbin/tunein-station.pls?id="
                        + id;

                title = title.replaceAll("&amp;", "&");
                lib.getVideos()
                        .add(new OnlineTrack(title, "ShoutCast Radio", genre, url,
                                id, artUrl, 0, Integer.parseInt(quality), "Shoutcast", true));
            }
        }
        return lib;
    }

    /**
     * This method is to extract the Stream URL
     * from the PLS file we take from XML Shoutcast API
     * <p/>
     * Simply follow the redirection and print the url we find
     *
     * @param url
     * @return
     */

    public static String getURLfromPLS(String url) {

        try {
            URLConnection mUrl = new URL(url).openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    mUrl.getInputStream()));
            LinkedList<String> murls = new LinkedList<String>();
            while (true) {
                try {
                    String line = br.readLine();

                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.indexOf("http") >= 0) {
                        line = line.substring(line.indexOf("http"));
                    }
                    if (line != null && !line.isEmpty()
                            && line.contains("http")) {
                        murls.add(line);

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if( !murls.isEmpty() )
                return murls.get(0);
            else
                return null;
        } catch (IOException e) {
            // 
            e.printStackTrace();
        }

        return null;
    }
}
