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

package com.luca89.service;

import com.luca89.utils.StreamUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 * Created by luca-linux on 4/4/16.
 */
public class BandcampLinkRetriever {
    /**
     * Necessary to retrieve the stream url from the
     * link we gather from the search
     *
     * @param video_url
     * @return
     */
    public static String getBCLink(String video_url) {

        try {
            String html = StreamUtils
                    .convertToString(video_url);

            html = html.substring(html.indexOf("\"file\":"));
            html = html.substring(html.indexOf("//") + 2, html.indexOf("\"},"));
            html = "http://" + html;
            html = getFinalURL(html);
            return html;
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        return null;
    }

    public static String getFinalURL(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setInstanceFollowRedirects(false);
        con.connect();
        con.getInputStream();

        if (con.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM || con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
            String redirectUrl = con.getHeaderField("Location");
            return getFinalURL(redirectUrl);
        }
        return url;
    }
}
