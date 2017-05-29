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

import android.text.TextUtils;

import com.luca89.utils.StreamUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by luca-linux on 6/3/16.
 */
public class LyricSearchTask {


    public static String getLyrics(String artist, String song) {
        return getLyricsSongLyrics(artist, song);
    }

    // Get from SongLyrics, fallback to LyriksWiki.com
    public static String getLyricsSongLyrics(String artist, String song) {
        if (artist == null || song == null) {
            return null;
        }
        String html = null;
        artist = artist.replace(" ", "-");
        song = song.replace(" ", "-");
        song = song.replace("(", "-");
        song = song.replace(")", "-");
        song = song.replace(" ", "+");
        song = song.replace("'", "");
        song = song.replace("!", "%21");
        song = song.replace("`", "%60");
        try {
            // Get the lyrics URL
            html = StreamUtils.convertToString("http://www.songlyrics.com/" + artist + "/" + song + "-lyrics/");

            if (html.isEmpty())
                return getLyricsWikia(artist, song);
            if (html.contains("Sorry"))
                return getLyricsWikia(artist, song);

            // Reach the line where the lyrics are interpreted
            String head = "<p id=\"songLyricsDiv\"  class=\"songLyricsV14 iComment-text\">";
            html = html.substring(html.indexOf(head) + head.length());
            // This is the lyrics, just have to clean them a little
            html = html.substring(0, html.indexOf("</p>"));

            // Now remove the junk
            String junk = "";
            Pattern pat = Pattern.compile("(<img src=.*><br />)");
            Matcher mat = pat.matcher(html);
            if (mat.find()) {
                junk = mat.group(1);
            }
            html = html.replace(junk, "");
            // Replace new line html with characters
            html = html.replace("<br />", "\n");
        } catch (final NumberFormatException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(html)) {
            return getLyricsWikia(artist, song);
        } else {
            return html;
        }
    }


    // Get from LyricsWiki, fallback to AZLyrics
    public static String getLyricsWikia(String artist, String song) {
        if (artist == null || song == null) {
            return null;
        }
        String ret = null;
        artist = artist.replace(" ", "%20");
        song = song.replace(" ", "%20");
        song = song.replace("(", "%20");
        song = song.replace(")", "%20");
        song = song.replace(" ", "+");
        song = song.replace("'", "");
        song = song.replace("!", "%21");
        song = song.replace("`", "%60");
        try {
            // Get the lyrics URL
            final String urlString = StreamUtils.convertToString("http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=" + artist + "&song=" + song);
            String songURL = new JSONObject(urlString.replace("song = ", ""))
                    .getString("url");
            songURL = songURL.replace("&amp;action=edit", "");

            // And now get the full lyrics
            String html = StreamUtils.convertToString(songURL);

            if (html.isEmpty())
                return getLyricsAZLyrics(artist, song);
            // Reach the line where the lyrics are interpreted
            if (!html.contains("<div class='lyricbox'>")) {
                return getLyricsAZLyrics(artist, song);
            }
            html = html.substring(html.indexOf("<div class='lyricbox'>"));
            // Cut from the line all the JS code
            html = html.substring(html.indexOf("</script>") + 9);
            // This is the lyrics, just have to clean them a little
            html = html.substring(0, html.indexOf("<!"));
            // Replace new line html with characters
            html = html.replace("<br />", "\n;");
            // Now parse the html entities

            final String[] htmlChars = html.split(";");
            final StringBuilder builder = new StringBuilder();
            String code;
            char character;
            for (final String s : htmlChars) {
                if (s.equals("\n")) {
                    builder.append(s);
                } else {
                    code = s.replaceAll("&#", "");
                    character = (char) Integer.valueOf(code).intValue();
                    builder.append(character);
                }
            }
            // And that's it
            ret = builder.toString();
        } catch (final JSONException e) {
			e.printStackTrace();
        } catch (final NumberFormatException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(ret)) {
            return getLyricsAZLyrics(artist, song);
        } else {
            return ret;
        }
    }

    // Get from AZ Lyrics,
    public static String getLyricsAZLyrics(String artist, String song) {
        if (artist == null || song == null) {
            return null;
        }
        String html = null;
        artist = artist.replace(" ", "");
        song = song.replace(" ", "");
        song = song.replace("(", "");
        song = song.replace(")", "");
        song = song.replace(" ", "+");
        song = song.replace("'", "");
        song = song.replace("!", "%21");
        song = song.replace("`", "%60");
        try {
            // Get the lyrics URL
            artist = artist.toLowerCase();
            song = song.toLowerCase();
            html = StreamUtils.convertToString("http://www.azlyrics.com/lyrics/" + artist + "/" + song + ".html");

            if (html.isEmpty())
                return "";
            if (!html.contains("<div class=\"lyricsh\">"))
                return "";
            // Reach the line where the lyrics are interpreted
            String head = "<div class=\"lyricsh\">";
            html = html.substring(html.indexOf(head) + head.length());
            html = html.substring(html.indexOf("<div>") + 5);
            // This is the lyrics, just have to clean them a little
            if (html.contains("-->"))
                html = html.substring(html.indexOf("-->") + 3);
            html = html.substring(0, html.indexOf("</div>"));

            // Replace new line html with characters
            html = html.replace("<br>", "\n");
            html = html.replace("&quot;", "");

        } catch (final NumberFormatException e) {
            e.printStackTrace();
        }
        return html;
    }

}
