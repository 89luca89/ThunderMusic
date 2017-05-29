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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.SubMenu;
import android.widget.Toast;

import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.R;
import com.luca89.utils.dataset.Library;
import com.luca89.utils.dataset.OnlineTrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import thundermusic.IMediaPlaybackService;

@SuppressLint({"CutPasteId", "SimpleDateFormat", "InflateParams"})
public class MusicUtils {

    public static final Object[] sTimeArgs = new Object[5];
    public static final Uri sArtworkUri = Uri
            .parse("content://media/external/audio/albumart");
    public static IMediaPlaybackService sService = null;
    public static WeakHashMap<Context, ServiceBinder> sConnectionMap = new WeakHashMap<Context, ServiceBinder>();
    public static StringBuilder sFormatBuilder = new StringBuilder();
    public static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());

    public static void copyDirectory(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]), new File(
                        targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public static ServiceToken bindToService(Activity context,
                                             ServiceConnection callback) {
        Activity realActivity = context.getParent();
        if (realActivity == null) {
            realActivity = context;
        }
        ContextWrapper cw = new ContextWrapper(realActivity);
        cw.startService(new Intent(cw, MediaPlaybackService.class));
        ServiceBinder sb = new ServiceBinder(callback);
        if (cw.bindService(
                (new Intent()).setClass(cw, MediaPlaybackService.class), sb, 0)) {
            sConnectionMap.put(cw, sb);
            return new ServiceToken(cw);
        }
        return null;
    }

    public static void unbindFromService(ServiceToken token) {
        if (token == null) {
            return;
        }
        ContextWrapper cw = token.mWrappedContext;
        ServiceBinder sb = sConnectionMap.remove(cw);
        if (sb == null) {
            return;
        }
        cw.unbindService(sb);
        if (sConnectionMap.isEmpty()) {
            // presumably there is nobody interested in the service at this
            // point,
            // so don't hang on to the ServiceConnection
            sService = null;
        }
    }

    /**
     * Same as makePlaylistMenu, but only for online provides
     *
     * @param context The context to use for creating the menu items
     * @param sub     The submenu to add the items to.
     */
    public static void makePlaylistMenuOnline(Context context, SubMenu sub) {
        String[] cols = new String[]{MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME};
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
        } else {
            String whereclause = MediaStore.Audio.Playlists.NAME + " != ''";
            Cursor cur = resolver.query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols,
                    whereclause, null, MediaStore.Audio.Playlists.NAME);
            sub.clear();
            sub.add(1, Defs.QUEUE, 0, R.string.queue);
            sub.add(1, Defs.NEW_PLAYLIST, 0, R.string.new_playlist);
            if (cur != null && cur.getCount() > 0) {
                // sub.addSeparator(1, 0);
                cur.moveToFirst();
                while (!cur.isAfterLast()) {
                    if (cur.getLong(0) == MusicUtils.getPlaylistiD(cur
                            .getString(1))) {
                        Intent intent = new Intent();
                        intent.putExtra("playlist", cur.getLong(0));
                        intent.putExtra("name", cur.getString(1));
                        // if (cur.getInt(0) == mLastPlaylistSelected) {
                        // sub.add(0, MusicBaseActivity.PLAYLIST_SELECTED,
                        // cur.getString(1)).setIntent(intent);
                        // } else {
                        sub.add(1, Defs.PLAYLIST_SELECTED, 0, cur.getString(1))
                                .setIntent(intent);
                        // }
                    }
                    cur.moveToNext();
                }
            }
            if (cur != null) {
                cur.close();
            }
        }
    }


    public static void addToPlaylist(Context context, String name,
                                     OnlineTrack tmp) {
        try {
            List<OnlineTrack> videos = new ArrayList<OnlineTrack>();
            videos.add(tmp);
            Library lib = new Library(name, videos, null);
            MusicUtils.writeAdapter(context, lib, name);
            String message = context.getResources().getQuantityString(
                    R.plurals.NNNtrackstoplaylist, 1, 1);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
        }

    }

    public static void addToPlaylistLib(Context context, String name,
                                        Library tmp) throws IOException {
        MusicUtils.writeAdapter(context, tmp, name);
        String message = context.getResources().getQuantityString(
                R.plurals.NNNtrackstoplaylist, tmp.getVideos().size(),
                tmp.getVideos().size());
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void removeFromPlaylist(String name, int position,
                                          Context context) {
        try {
            List<OnlineTrack> videos;
            videos = MusicUtils.readAdapter(context, name).getVideos();
            videos.remove(position);
            Library lib = new Library(name, videos, null);
            MusicUtils.writeAdapter(context, lib, name);
            String message = context.getResources().getQuantityString(
                    R.plurals.NNNtracksdeleted, 1, 1);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            // Update the service only if manipulating the queue!
            if (name.equalsIgnoreCase("queue"))
                sService.setOnlineLibrary(name, -1);
        } catch (IOException e) {

        } catch (ClassNotFoundException e) {

        } catch (RemoteException e) {

        }
    }

    public static void addToExistingPlaylist(Context context, String name,
                                             OnlineTrack tmp) {
        addToExistingPlaylist(context, name, tmp, 1);
    }

    public static void addToExistingPlaylist(Context context, String name,
                                             OnlineTrack tmp, int nowPlaying) {
        try {
            Library tmplib;
            tmplib = MusicUtils.readAdapter(context, name);
            List<OnlineTrack> videos;
            videos = tmplib.getVideos();
            videos.add(tmp);

            Library lib = new Library(name, videos, null);
            MusicUtils.writeAdapter(context, lib, name);
            String message = context.getResources().getQuantityString(
                    R.plurals.NNNtrackstoplaylist, 1, 1);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            if (nowPlaying == 1)
                sService.setOnlineLibrary(name, -1);
        } catch (IOException e) {

        } catch (ClassNotFoundException e) {

        } catch (RemoteException e) {

        }

    }

    public static Cursor query(Context context, Uri uri, String[] projection,
                               String selection, String[] selectionArgs, String sortOrder,
                               int limit) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            if (limit > 0) {
                uri = uri.buildUpon().appendQueryParameter("limit", "" + limit)
                        .build();
            }
            return resolver.query(uri, projection, selection, selectionArgs,
                    sortOrder);
        } catch (UnsupportedOperationException ex) {
            return null;
        }

    }

    public static Cursor query(Context context, Uri uri, String[] projection,
                               String selection, String[] selectionArgs, String sortOrder) {
        return query(context, uri, projection, selection, selectionArgs,
                sortOrder, 0);
    }


    public static String makeTimeString(Context context, long secs) {
        String durationformat = context
                .getString(secs < 3600 ? R.string.durationformatshort
                        : R.string.durationformatlong);

		/*
         * Provide multiple arguments so the format can be changed easily by
		 * modifying the xml.
		 */
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;

        return sFormatter.format(durationformat, timeArgs).toString();
    }


    public static int getIntPref(Context context, String name, int def) {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.luca89.thundermusic_preferences", Context.MODE_PRIVATE);
        return prefs.getInt(name, def);
    }

    public static void setIntPref(Context context, String name, int value) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                "com.luca89.thundermusic_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putInt(name, value);
        prefEditor.commit();
    }

    public static void setBooleanPref(Context context, String name,
                                      boolean value) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                "com.luca89.thundermusic_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putBoolean(name, value);
        prefEditor.commit();
    }

    public static boolean getBooleanPref(Context context, String name,
                                         boolean def) {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.luca89.thundermusic_preferences", Context.MODE_PRIVATE);
        return prefs.getBoolean(name, def);
    }


    /**
     * This and readAdapter are how the online search, online play
     * are passed from activities and services
     * <p/>
     * Simply write in a file the serialized Library and retrieve it
     * where needed
     * <p/>
     * Used for "search" "queue" and "playlist_name"
     * where playlist_name is indeed the name of the saved playlist
     * <p/>
     * This way also the playlists are available for manipulation offline too
     * Although connection is still required for playback
     *
     * @param context
     * @param tmp
     * @param name
     * @throws IOException
     */
    public static void writeAdapter(Context context, Library tmp, String name)
            throws IOException {

        MusicUtils.createPathScheme(context);
        String path = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.online");
        File file = new File(path, name);
        FileOutputStream fout = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(tmp);
        oos.close();
    }

    public static Library readAdapter(Context context, String name)
            throws IOException, ClassNotFoundException {
        String path = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.online");
        File file = new File(path, name);
        FileInputStream fin = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fin);
        Library library = (Library) ois.readObject();
        ois.close();

        return library;
    }


    /**
     * Save/Read/Delete the search history as an array
     * Saved to SharedPreferences
     *
     * @param context
     * @param name
     * @param value
     */
    public static void setArrayPref(Context context, String name, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                "com.luca89.search_history", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        Set<String> tmp = sharedPref.getStringSet(name, new HashSet<String>());
        if (!value.isEmpty())
            tmp.add(value);
        prefEditor.remove(name);
        prefEditor.commit();
        prefEditor.putStringSet(name, tmp);
        prefEditor.commit();
    }

    public static Set<String> getArrayPref(Context context, String name) {
        SharedPreferences prefs = context.getSharedPreferences(
                "com.luca89.search_history", Context.MODE_PRIVATE);
        return prefs.getStringSet(name, new HashSet<String>());
    }

    public static void resetArrayPref(Context context, String name) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                "com.luca89.search_history", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.remove(name);
        prefEditor.commit();
    }

    /**
     * From API 23 onward you have to ask for the first time for
     * SYSTEM_ALERT_WINDOW
     * <p/>
     * called in Media PLayback Service
     *
     * @param context
     * @return
     */
    public static boolean checkSystemAlertWindowPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Intent i = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION");
                i.setData(Uri.parse("package:com.luca89.thundermusic"));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * If folders are not already set up it may be a problem
     * so this utility performs the creation of the tree scheme
     * needed by the app
     *
     * @param context
     * @throws IOException
     */
    public static void createPathScheme(Context context) throws IOException {

        String path = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music");
        OutputStream fOut;
        OutputStream fOut2;
        OutputStream fOut3;
        OutputStream fOut4;
        String pathlyrics = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.lyrics");
        String pathcache = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.cache");
        String pathlrucache = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.lrucache");
        String pathonline = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.Thunder_Music/.online");
        File pathFile = new File(path);
        pathFile.mkdirs();
        File pathFileLyrics = new File(pathlyrics);
        pathFileLyrics.mkdirs();
        File pathFileCache = new File(pathcache);
        pathFileCache.mkdirs();
        File pathFileLruCache = new File(pathlrucache);
        pathFileLruCache.mkdirs();
        File pathFileOnline = new File(pathonline);
        pathFileOnline.mkdirs();

        File nomedia = new File(path, ".nomedia");
        File nomedialyrics = new File(pathlyrics, ".nomedia");
        File nomediacache = new File(pathcache, ".nomedia");
        File nomedialrucache = new File(pathlrucache, ".nomedia");

        fOut = new FileOutputStream(nomedia);
        fOut2 = new FileOutputStream(nomedialyrics);
        fOut3 = new FileOutputStream(nomediacache);
        fOut4 = new FileOutputStream(nomedialrucache);

        fOut.flush();
        fOut.close();
        fOut2.flush();
        fOut2.close();
        fOut3.flush();
        fOut3.close();
        fOut4.flush();
        fOut4.close();
    }

    /**
     * Execute given AsyncTask
     * serial or parallel
     *
     * @param forceSerial
     * @param task
     * @param args
     * @param <T>
     */
    public static <T> void execute(final boolean forceSerial,
                                   final AsyncTask<T, ?, ?> task, final T... args) {
        if (forceSerial)
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, args);
        else
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    }


    /**
     * Returns the playlist iD based on the name
     * <p/>
     * It's needed given the methos I use to distinguish a normal playlist
     * from an online one
     * <p/>
     * Online playlist:
     * name.hashCode == iD
     *
     * @param name
     * @return
     */
    public static int getPlaylistiD(String name) {
        int i = name.hashCode();
        if (i > 0)
            return name.hashCode();
        else
            return -name.hashCode();
    }

    public static void deleteOnlinePlaylist(Context context, String name) {
        String path = (context
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/.Thunder_Music/.online/" + name);
        File pathFile = new File(path);

        if (pathFile.exists()) {
            pathFile.delete();
        }
    }

    public static int idForplaylist(String name, Context context) {
        Cursor c = MusicUtils.query(context,
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Playlists._ID},
                MediaStore.Audio.Playlists.NAME + "=?", new String[]{name},
                MediaStore.Audio.Playlists.NAME);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
            c.close();
        }
        return id;
    }

    public static boolean startupPopup(Context context) {
        SharedPreferences runCheck = context.getSharedPreferences(
                "hasRunBeforePopup", 0);
        Boolean hasRun = runCheck.getBoolean("hasRunPopup", false);
        if (!hasRun) {
            SharedPreferences settings = context.getSharedPreferences(
                    "hasRunBeforePopup", 0);
            SharedPreferences.Editor edit = settings.edit();
            edit.putBoolean("hasRunPopup", true); // set to has run
            edit.commit();
            return false;
        } else {
            return true;
        }
    }

    public interface Defs {
        int ADD_TO_PLAYLIST = 1;
        int PLAYLIST_SELECTED = 3;
        int NEW_PLAYLIST = 4;
        int PLAY_SELECTION = 5;
        int DELETE_ITEM = 10;
        int SCAN_DONE = 11;
        int QUEUE = 12;
        int EFFECTS_PANEL = 13;
        int EXIT = 14;
        int CHILD_MENU_BASE = 17;
    }

    public static class ServiceToken {
        ContextWrapper mWrappedContext;

        ServiceToken(ContextWrapper context) {
            mWrappedContext = context;
        }
    }

    public static class ServiceBinder implements ServiceConnection {
        ServiceConnection mCallback;

        ServiceBinder(ServiceConnection callback) {
            mCallback = callback;
        }

        public void onServiceConnected(ComponentName className,
                                       android.os.IBinder service) {
            sService = IMediaPlaybackService.Stub.asInterface(service);
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            sService = null;
        }
    }

}