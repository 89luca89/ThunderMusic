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

package com.luca89.utils.dataset;

import java.io.Serializable;
import java.util.List;

/**
 * This is the 'library' of all the users videos
 *
 * @author paul.blundell
 */
public class Library implements Serializable {
    // The username of the owner of the library
    private String query;
    // A list of videos that the user owns
    private List<OnlineTrack> videos;
    // A token for next page
    private String nextPage;

    public Library(String query, List<OnlineTrack> videos, String nextPage) {
        this.query = query;
        this.videos = videos;
        this.nextPage = nextPage;
    }

    /**
     * @return the user name
     */
    public String getQuery() {
        return query;
    }


    /**
     * @return the user name
     */
    public String getNextPage() {
        return nextPage;
    }

    /**
     * @return the user name
     */
    public void setNextPage(String nextPage) {
        this.nextPage = nextPage;
    }

    /**
     * @return the videos
     */
    public List<OnlineTrack> getVideos() {
        return videos;
    }
}