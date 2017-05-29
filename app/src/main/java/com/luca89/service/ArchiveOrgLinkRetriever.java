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

public class ArchiveOrgLinkRetriever {
    /**
     * Necessary to retrieve the stream link from the url
     * we gather from the search
     *
     * @param video_id
     * @return
     */
    public static String getARLink(String video_id) {

        try {
            String html = StreamUtils
                    .convertToString(video_id);

            String prop = "<meta property=\"og:video\" content=\"";
            html = html.substring(html.indexOf(prop) + prop.length());
            html = html.substring(0, html.indexOf("\"/>"));

            return html;
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        return null;
    }
}
