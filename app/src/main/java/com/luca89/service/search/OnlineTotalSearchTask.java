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
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OnlineTotalSearchTask implements Runnable {
    private final Handler replyTo;
    private final String mSearch;
    private final Context context;
    private final String name;
    private Library mLib;

    /**
     * This is only a gather of all the searches, look at the single ones
     * to understand better
     * <p/>
     * they are only limited to 10 results for gathering purposes
     *
     * @param lib
     * @return
     */
    public OnlineTotalSearchTask(Handler replyTo, String search,
                                 Context context, String name, Library lib) {
        this.replyTo = replyTo;
        this.mSearch = search;
        this.context = context;
        this.name = name;
        this.mLib = lib;
    }

    private int convertTime(String time) {
        PeriodFormatter formatter = ISOPeriodFormat.standard();
        Period p = formatter.parsePeriod(time);
        Seconds s = p.toStandardSeconds();

        return s.getSeconds();
    }

    private Library getLibraryYT(Library lib, String nextPage, String key) {
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
                jsonString = StreamUtils.convertToString("https://www.googleapis.com/youtube/v3/search?q=" + search + "&type=video&part=snippet&maxResults=15&key=" + key);
            } else {
                jsonString = StreamUtils.convertToString("https://www.googleapis.com/youtube/v3/search?q=" + search + "&type=video&part=snippet&maxResults=15&key=" + key + "&pageToken=" + nextPage);

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
                return getLibraryYT(lib, nextPageID, key);
            } else {
                return lib;
            }
            // We don't do any error catching, just nothing will happen if this
            // task falls over
            // an idea would be to reply to the handler with a different message
            // so your Activity can act accordingly
        } catch (JSONException e) {
        }
        return getLibraryYT(lib, null, Decrypt.getKey(Decrypt.YT_API_KEY_2, context));
    }

    private Library getLibrarySoundcloud(Library lib, String key) {
        try {

            String search = mSearch;
            search = search.replace(" ", "%20");
            search = search.replace("'", "");
            search = search.replace("!", "%21");
            search = search.replace("`", "%60");
            String jsonString;
            String title, link, artUrl, artist, urlhq;
            int duration, views;


            List<OnlineTrack> videos = new ArrayList<OnlineTrack>();

            jsonString = StreamUtils.convertToString("http://api.soundcloud.com/tracks.json?client_id=" + key + "&q=" + search + "&limit=100");

            // Fallback to key2 only if we are using key1
            // If we are using key2 already, just fail gracefully
            if (jsonString == "") {
                if (key.equals(Decrypt.SOUNDCLOUD_API_KEY))
                    return getLibrarySoundcloud(lib, Decrypt.getKey(Decrypt.SOUNDCLOUD_API_KEY_2, context));
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

                videos.add(
                        new OnlineTrack(title, "Soundcloud", artist, urlhq, link, artUrl, duration, views, "Soundcloud"));
            }

            // Order by playback_count using a second list
            Collections.sort(videos, new Comparator<OnlineTrack>() {
                public int compare(OnlineTrack s1, OnlineTrack s2) {
                    return s2.getViews() - (s1.getViews());
                }
            });
            // Add only the first results to the library
            // This is not meant to be a complete search, only a peek at top results
            for (int i = 0; i < Math.min(15, videos.size()); i++) {
                lib.getVideos().add(videos.get(i));
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

    public Library getLibraryBandcamp(Library lib) {
        try {
            String search = mSearch;
            search = search.replace(" ", "%20");
            search = search.replace("'", "");
            search = search.replace("!", "%21");
            search = search.replace("`", "%60");
            String html;
            String title, artUrl, artist, album, url;
            String nextPageID = lib.getNextPage();

            int limit = 0;

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
                // No result or error
                return lib;
            }
            // Strart scraping the web page
            html = html.substring(html.indexOf("<ul class=\"result-items\">"));
            String[] results = html.split("<li class=\"searchresult track\">");
            for (String entry : results) {
                // Here I have exaclty one search entry
                // Let's start scraping the data
                if (limit == 10)
                    break;
                if (entry.contains("TRACK")) {
                    limit++;
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lib;
        // We don't do any error catching, just nothing will happen if this
        // task falls over
        // an idea would be to reply to the handler with a different message
        // so your Activity can act accordingly
    }

    private Library getLibraryArchive(Library lib) {
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
                jsonString = StreamUtils.convertToString("https://archive.org/advancedsearch.php?q=(" + search + ")+AND+mediatype%3A(audio)&fl[]=identifier&fl[]=description&fl[]=title&rows=15&page=1&output=json");
            } else {
                jsonString = StreamUtils.convertToString("https://archive.org/advancedsearch.php?q=(" + search + ")+AND+mediatype%3A(audio)&fl[]=identifier&fl[]=description&fl[]=title&rows=15&page=" + nextPageID + "&output=json");
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

        return lib;
    }

    public Library getLibraryFma(Library lib) {
        try {
            String search = mSearch;
            search = search.replace(" ", "+");
            search = search.replace("'", "");
            search = search.replace("!", "%21");
            search = search.replace("`", "%60");
            String html;
            String title = "", artUrl, artist = "", album = "", url = "", link = "";
            String nextPageID = lib.getNextPage();

            if (nextPageID == null) {
                html = StreamUtils.convertToString("http://freemusicarchive.org/search/?quicksearch=" + search + "&page=1&per_page=15&sort=track_interest");
            } else {
                html = StreamUtils.convertToString("http://freemusicarchive.org/search/?quicksearch=" + search + "&page=" + nextPageID + "&per_page=15&sort=track_interest");
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
                // No result or error
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
        } catch (Exception e) {
            e.printStackTrace();
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
            mLib = getLibraryYT(mLib, null, Decrypt.getKey(Decrypt.YT_API_KEY, context));
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(0);

            mLib = getLibrarySoundcloud(mLib, Decrypt.getKey(Decrypt.SOUNDCLOUD_API_KEY, context));
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(1);

            mLib = getLibraryBandcamp(mLib);
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(1);


            mLib = getLibraryArchive(mLib);
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(1);


            mLib = getLibraryFma(mLib);
            MusicUtils.writeAdapter(context, mLib, name);
            replyTo.sendEmptyMessage(1);

        } catch (IOException e) {
        }
    }
}
