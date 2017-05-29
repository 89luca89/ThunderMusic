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

package com.luca89.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import com.luca89.thundermusic.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by luca-linux on 6/3/16.
 */
public class ImageUtils {
    /**
     * Returns a downsized image
     * it is a wrapper to return the right image for
     * downloaded artwork or embeeded ones
     *
     * @param context
     * @param album_id
     * @return
     */
    public static Bitmap getArtworkQuick1(Context context, long album_id) {
        Bitmap b;

        try {
            b = getArtworkDownloadedResized(context, album_id);
            if (b != null) {
                return b;
            } else {
                return getArtworkQuick_Base1(context, album_id);
            }
        } catch (FileNotFoundException e) {

            if (album_id != -2)
                return getArtworkQuick_Base1(context, album_id);
            else
                return null;
        }

    }

    public static Drawable getArtwork(Context context, long album_id) {
        return getArtwork(context, album_id, true);
    }

    /**
     * Return artwork
     * Downloaded is exists or embeeded one
     *
     * @param context
     * @param album_id
     * @param allowdefault
     * @return
     */
    public static Drawable getArtwork(Context context, long album_id, boolean allowdefault) {

        Drawable b;
        b = getArtworkDownloaded(context, album_id);
        if (b != null) {
            return b;
        } else {
            return getArtwork_Base(context, album_id, album_id, allowdefault);
        }

    }

    public static Drawable getArtworkFromFile(Context context, long albumid) {
        Drawable b;
        b = getArtworkDownloaded(context, albumid);
        if (b != null) {
            return b;
        } else {
            return getArtworkFromFile_Base(context, albumid, albumid);
        }

    }

    /**
     * Return album artwork given the id
     * if nothing is found, we try a downloaded one
     *
     * @param context
     * @param album_id
     * @return
     */
    public static Bitmap getArtworkQuick_Base1(Context context, long album_id) {
        if (album_id != -1) {
            ContentResolver res = context.getContentResolver();
            Uri uri = ContentUris.withAppendedId(MusicUtils.sArtworkUri, album_id);
            if (uri != null) {
                // ParcelFileDescriptor fd = null;
                try {

                    ParcelFileDescriptor fd = res.openFileDescriptor(uri, "r");

                    BitmapFactory.Options o2 = new BitmapFactory.Options();

                    o2.inSampleSize = 2;
                    o2.inDither = false;
                    o2.inPreferredConfig = Bitmap.Config.RGB_565;

                    Bitmap b = BitmapFactory.decodeFileDescriptor(
                            fd.getFileDescriptor(), null, o2);

                    if (b != null) {
                        return b;
                    } else {
                        b = getArtworkDownloadedResized(context, album_id);
                        if (b != null) {
                            return b;
                        } else {
                            return null;
                        }
                    }
                } catch (FileNotFoundException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Get album art for specified album. You should not pass in the album id
     * for the "unknown" album here (use -1 instead)
     *
     * @param context
     * @param song_id
     * @param album_id
     * @param allowdefault
     * @return
     */
    public static Drawable getArtwork_Base(Context context, long song_id,
                                           long album_id, boolean allowdefault) {

        if (album_id < 0) {
            // This is something that is not in the database, so get the album
            // art directly
            // from the file.
            if (song_id >= 0) {
                Drawable bm = getArtworkFromFile(context, -1);
                if (bm != null) {
                    return bm;
                }
            }
            if (allowdefault) {
                return getDefaultArtwork(context);
            }
            return null;
        }

        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(MusicUtils.sArtworkUri, album_id);
        if (uri != null) {
            InputStream in = null;
            try {
                in = res.openInputStream(uri);
                Drawable tmp = Drawable.createFromStream(in, uri.toString());
                return tmp;
            } catch (FileNotFoundException ex) {
                // The album art thumbnail does not actually exist. Maybe the
                // user deleted it, or
                // maybe it never existed to begin with.
                Drawable bm = getArtworkFromFile(context, album_id);

                if (bm == null && allowdefault) {
                    bm = getDefaultArtwork(context);
                }
                return bm;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                }
            }
        }

        return null;
    }

    public static Drawable getArtworkFromFile_Base(Context context,
                                                   long songid, long albumid) {
        Drawable bm = null;
        if (albumid < 0 && songid < 0) {
            throw new IllegalArgumentException(
                    "Must specify an album or a song id");
        }

        try {
            if (albumid < 0) {
                Uri uri = Uri.parse("content://media/external/audio/media/"
                        + songid + "/albumart");

                InputStream is = context.getContentResolver().openInputStream(
                        uri);
                bm = Drawable.createFromStream(is, uri.toString());

            } else {
                Uri uri = ContentUris.withAppendedId(MusicUtils.sArtworkUri, albumid);
                InputStream is = context.getContentResolver().openInputStream(
                        uri);
                bm = Drawable.createFromStream(is, uri.toString());
            }
        } catch (IllegalStateException ex) {
        } catch (FileNotFoundException ex) {
        }
        if (bm != null) {
            return bm;
        } else {
            return getArtworkDownloaded(context, albumid);
        }
    }

    /**
     * Return the default artwork for unknown album or absent artwork
     *
     * @param context
     * @return
     */
    public static BitmapDrawable getDefaultArtwork(Context context) {
        BitmapDrawable tmp = (BitmapDrawable) context.getResources()
                .getDrawable(R.drawable.albumart_mp_unknown);
        if (tmp != null) {
            return tmp;
        } else {
            return null;
        }
    }

    /**
     * Similar to setDefaultArtwork
     * Used to set the online thumbnail for online tracks
     *
     * @param bitmap
     * @param context
     * @param album_id
     * @throws IOException
     */
    public static void setThumbArtwork(Bitmap bitmap, Context context,
                                       long album_id) throws IOException {

        MusicUtils.createPathScheme(context);
        String path = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music");
        OutputStream fOut;

        File file = new File(path, Long.toString(album_id) + ".jpg");
        fOut = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
        fOut.flush();
        fOut.close();
        bitmap = null;
    }

    /**
     * Return the artwork saved previously
     * Retrieve it from sdcard with the name bounded to the albumid
     *
     * @param context
     * @param albumid
     * @return
     */
    public static Drawable getArtworkDownloaded(Context context, long albumid) {
        Drawable bm;
        String path = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/.Thunder_Music/" + Long.toString(albumid) + ".jpg");
        bm = Drawable.createFromPath(path);
        return bm;
    }

    /**
     * Return the artwork saved previously DOWNSIZED
     * Retrieve it from sdcard with the name bounded to the albumid
     * Handy for thumbnails
     *
     * @param context
     * @param albumid
     * @return
     */
    public static Bitmap getArtworkDownloadedResized(Context context,
                                                     long albumid) throws FileNotFoundException {

        String path = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/.Thunder_Music/" + Long.toString(albumid) + ".jpg");

        BitmapFactory.Options o2 = new BitmapFactory.Options();

        o2.inSampleSize = 2;
        o2.inDither = false;
        o2.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap tmp = BitmapFactory.decodeStream(new FileInputStream(new File(
                path)), null, o2);
        return tmp;
    }
}
