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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.luca89.thundermusic.R;
import com.melnykov.fab.FloatingActionButton;


/**
 * Created by luca-linux on 6/3/16.
 */
public class InterfaceUtils {

    /**
     * Method to calculate screen width and determine screen size (<7 or >7)
     * <p/>
     * It does also compensate for the ~50dp reduction in landscape
     * for the navigation bar
     *
     * @param activity
     * @return
     */
    public static boolean getTabletMode(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        float scaleFactor = metrics.density;
        float widthDp = widthPixels / scaleFactor;
        float heightDp = heightPixels / scaleFactor;
        float smallestWidth = Math.min(widthDp, heightDp);

        // Compensate navigation bar in case of landscape
        if (activity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT)
            smallestWidth += 50;

        if (smallestWidth >= 720) {
            //Device is a 10" or 9" tablet
            return true;
        } else if (smallestWidth >= 600) {
            //Device is a 7" tablet
            return true;
        } else {
            //Device is a phone
            return false;
        }

    }

    public static void showToast(Activity a, int resid) {
        Toast mToast = Toast.makeText(a, "", Toast.LENGTH_SHORT);
        mToast.setText(resid);
        mToast.show();
    }

    /**
     * Show webview with about screen
     * html is taken from assets
     *
     * @param context
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void showAbout(Context context) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        WebView wv = new WebView(context);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.loadUrl("file:///android_asset/about.html");
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);

                return true;
            }
        });
        alert.setView(wv);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alert.show();
    }

    /**
     * Setup utility for the FAB
     *
     * @param context
     * @param icon         the icon to apply
     * @param button       the FAB button
     * @param mFABlistener the listener to apply
     */
    public static void setUpFAB(Activity context, int icon, FloatingActionButton button, View.OnClickListener mFABlistener) {
        button.setVisibility(View.VISIBLE);
        button.setType(FloatingActionButton.TYPE_NORMAL);
        if (ThemeUtils.getAppTheme3(context) < 6) {
            button.setColorNormal(context.getResources().getColor(
                    ThemeUtils.getDrawableColor(ThemeUtils.getAppTheme3(context), context)));
            button.setColorPressed(context.getResources().getColor(
                    ThemeUtils.getDrawableColor(ThemeUtils.getAppTheme3(context), context)));
        } else {
            button.setColorNormal(ThemeUtils.getDrawableColor(ThemeUtils.getAppTheme3(context), context));
            button.setColorPressed(ThemeUtils.getDrawableColor(ThemeUtils.getAppTheme3(context), context));
        }
        button.setImageDrawable(context.getResources().getDrawable(icon));
        button.setOnClickListener(mFABlistener);
    }

    /**
     * Based on the provider
     * this utiliy returns the correct icon
     * via id
     *
     * @param provider
     * @return
     */
    public static int getOnlineDefaultArtworkRes(String provider) {
        if (provider.equalsIgnoreCase("soundcloud"))
            return R.drawable.logo_soundcloud;
        else if (provider.equalsIgnoreCase("bandcamp"))
            return R.drawable.logo_bandcamp;
        else if (provider.equalsIgnoreCase("freemusicarchive.org"))
            return R.drawable.logo_fma;
        else if (provider.equalsIgnoreCase("archive.org"))
            return R.drawable.logo_archive;
        else if (provider.equalsIgnoreCase("youtube"))
            return R.drawable.logo_youtube;
        else
            return 0;
    }
}
