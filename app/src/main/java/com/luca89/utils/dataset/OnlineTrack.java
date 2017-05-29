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

import android.content.Context;

import com.luca89.utils.MusicUtils;

import java.io.Serializable;


/**
 * This simple class is to unify all the online providers data
 * in one single Track-like object
 */
public class OnlineTrack implements Serializable {

    protected String mTitle;
    protected String mArtist;
    protected String mLink;
    protected String mUrl;
    protected int mDuration;
    protected int mViews;
    protected String mAlbum;
    protected String mArtUrl;
    protected boolean isRadio;
    protected String mProvider;

    public OnlineTrack(String title, String album, String artist, String url, String link, String artUrl,
                       int duration, int Views, String provider) {
        this.mTitle = title;
        this.mArtist = artist;
        this.mUrl = url;
        this.mLink = link;
        this.mDuration = duration;
        this.mViews = Views;
        this.mArtUrl = artUrl;
        this.isRadio = false;
        this.mAlbum = album;
        this.mProvider = provider;

    }

    public OnlineTrack(String title, String album, String artist, String url, String link, String artUrl,
                       int duration, int Views, String provider, boolean radio) {
        // 
        this.mTitle = title;
        this.mArtist = artist;
        this.mUrl = url;
        this.mLink = link;
        this.mDuration = duration;
        this.mViews = Views;
        this.mArtUrl = artUrl;
        this.isRadio = radio;
        this.mAlbum = album;
        this.mProvider = provider;
    }

    public String getLink() {
        return mLink;
    }

    public void setLink(String link) {
        this.mLink = link;
    }

    public int getViews() {
        return mViews;
    }

    public void setViews(int views) {
        this.mViews = views;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public void setAlbum(String album) {
        mAlbum = album;
    }

    public String getArtUrl() {
        return mArtUrl;
    }

    public void setArtUrl(String artwurl) {
        mArtUrl = artwurl;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String artist) {
        this.mArtist = artist;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public boolean getRadiomode() {
        return isRadio;
    }

    public void setRadiomode(boolean radio) {
        this.isRadio = radio;
    }

    public String getDurationString(Context context) {
        return MusicUtils.makeTimeString(context, mDuration);
    }

    public String getProvider() {
        return mProvider;
    }
}
