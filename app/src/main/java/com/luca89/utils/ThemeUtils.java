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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.luca89.thundermusic.R;
import com.luca89.thundermusic.activities.MediaPlaybackActivity;
import com.luca89.thundermusic.activities.PreferencesActivity;
import com.readystatesoftware.systembartint.SystemBarTintManager;

/**
 * Created by luca-linux on 6/3/16.
 */
public class ThemeUtils {
    /**
     * Manipulate the application theme:
     * Based on the corresponding theme (1-7) this method will:
     * set the theme
     * colorize the actionbar accordinlgy
     * adjust the padding of the icon
     * colorize (if >lollipop) the multitask
     * colorize (if>KK) the statusbar
     *
     * @param context
     */
    public static void getAppTheme(Activity context) {
        int Theme =
                Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(
                        PreferencesActivity.KEY_THEME, "3"));
        Drawable mActionBarBackgroundDrawable;

        mActionBarBackgroundDrawable = getThemeDrawable(context, Theme);

        if (Theme == 4) {
            context.setTheme(R.style.BlackTheme);
        } else if (Theme == 1 || Theme == 7) {
            context.setTheme(R.style.CustomTheme);

            context.getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
            context.getActionBar().setSplitBackgroundDrawable(mActionBarBackgroundDrawable);
            context.getActionBar().setStackedBackgroundDrawable(mActionBarBackgroundDrawable);

        } else {
            context.setTheme(R.style.ThunderMusic);

            context.getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
            context.getActionBar().setSplitBackgroundDrawable(mActionBarBackgroundDrawable);
            context.getActionBar().setStackedBackgroundDrawable(mActionBarBackgroundDrawable);
        }
        context.getActionBar().setIcon(R.drawable.ic_action_back);

        ImageView view = (ImageView) context.findViewById(android.R.id.home);

        int paddingRight;
        paddingRight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 22, context.getResources().getDisplayMetrics());

        int paddingLeft = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
        view.setPadding(paddingLeft, 0, paddingRight, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SystemBarTintManager tintManager = new SystemBarTintManager(context);
            tintManager.setStatusBarTintEnabled(true);
            if (Theme < 6)
                tintManager.setTintColor(
                        context.getResources().getColor(getThemeColor(context, Theme)));
            else
                tintManager.setTintColor(getThemeColor(context, Theme));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Theme < 6)
                context.setTaskDescription(new ActivityManager.TaskDescription(
                        context.getString(R.string.musicbrowserlabel),
                        BitmapFactory.decodeResource(context.getResources(), R.drawable.app_music),
                        context.getResources().getColor(getThemeColor(context, Theme))));
            else
                context.setTaskDescription(new ActivityManager.TaskDescription(
                        context.getString(R.string.musicbrowserlabel),
                        BitmapFactory.decodeResource(context.getResources(), R.drawable.app_music),
                        getThemeColor(context, Theme)));

            Window window = context.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(context.getResources().getColor(R.color.transparent6));
        }
    }

    /**
     * Manipulate the application theme with TRASLUCENT actionbar:
     * Based on the corresponding theme (1-7) this method will:
     * set the theme
     * colorize the actionbar accordinlgy
     * adjust the padding of the icon
     * colorize (if >lollipop) the multitask
     * colorize (if>KK) the statusbar
     * <p/>
     * this returns a drawable that corresponds to the actionbar background
     * later to be manipulated by the corresponding activity
     *
     * @param context
     */
    public static Drawable getAppThemeExpanded(
            final Activity context, Drawable mActionBarBackgroundDrawable) {
        int Theme =
                Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(
                        PreferencesActivity.KEY_THEME, "3"));

        mActionBarBackgroundDrawable = getThemeDrawable(context, Theme);

        if (Theme == 4) {
            context.setTheme(R.style.BlackTheme55);
        } else if (Theme == 1 || Theme == 7) {
            context.setTheme(R.style.CustomTheme55);
            context.getActionBar().setSplitBackgroundDrawable(mActionBarBackgroundDrawable);
            context.getActionBar().setStackedBackgroundDrawable(mActionBarBackgroundDrawable);
        } else {
            context.setTheme(R.style.ThunderMusic55);
            context.getActionBar().setSplitBackgroundDrawable(mActionBarBackgroundDrawable);
            context.getActionBar().setStackedBackgroundDrawable(mActionBarBackgroundDrawable);
        }

        ImageView view = (ImageView) context.findViewById(android.R.id.home);
        int paddingRight;

        if (context.getClass() == MediaPlaybackActivity.class) {
            paddingRight = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
        } else {
            paddingRight = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 24, context.getResources().getDisplayMetrics());
        }

        int paddingLeft = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 21, context.getResources().getDisplayMetrics());
        view.setPadding(paddingLeft, 0, paddingRight, 0);

        mActionBarBackgroundDrawable.setAlpha(0);
        context.getActionBar().setIcon(R.drawable.ic_action_back);
        context.getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Theme < 6)
                context.setTaskDescription(new ActivityManager.TaskDescription(
                        context.getString(R.string.musicbrowserlabel),
                        BitmapFactory.decodeResource(context.getResources(), R.drawable.app_music),
                        context.getResources().getColor(getThemeColor(context, Theme))));
            else
                context.setTaskDescription(new ActivityManager.TaskDescription(
                        context.getString(R.string.musicbrowserlabel),
                        BitmapFactory.decodeResource(context.getResources(), R.drawable.app_music),
                        getThemeColor(context, Theme)));
        }
        return mActionBarBackgroundDrawable;
    }

    /**
     * Return an integer
     * representing if using a LIGHT(3) or DARK(other) theme
     *
     * @param context
     */
    public static int getAppTheme2(Context context) {
        int Theme =
                Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(
                        PreferencesActivity.KEY_THEME, "3"));
        if (Theme != 7 && Theme != 1 && Theme != 4)
            return 3;
        else
            return Theme;
    }

    /**
     * Return an integer
     * Representing the theme in use (1-7)
     *
     * @param context
     * @return
     */
    public static int getAppTheme3(Context context) {
        return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(
                PreferencesActivity.KEY_THEME, "3"));
    }

    /**
     * Return a color based on the theme in use
     *
     * @param context
     */
    public static void getBarTheme(Context context, View view1) {
        int Theme =
                Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(
                        PreferencesActivity.KEY_THEME, "3"));
        if (Theme == 1 || Theme == 7) {
            view1.setBackgroundResource(R.color.background_dark);
        } else if (Theme == 4 || Theme == 5) {
        } else {
            view1.setBackgroundResource(android.R.color.white);
        }
    }

    /**
     * Return a color based on the theme in use
     *
     * @param context
     * @param lv
     */
    public static void getNowDrawerTheme(Activity context, View lv) {
        int Theme =
                Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(
                        PreferencesActivity.KEY_THEME, "3"));
        if (Theme == 1 || Theme == 7) {
            lv.setBackgroundResource(R.color.bar_background);
        } else if (Theme == 4) {
            lv.setBackgroundResource(android.R.color.black);
        } else {
            lv.setBackgroundResource(android.R.color.white);
        }
    }

    /**
     * Return the color code corresponding to the current theme
     * <p/>
     * id if a preset
     * color_Hex if a custom color
     *
     * @param context
     * @param Theme
     * @return
     */
    public static int getThemeColor(Context context, int Theme) {
        switch (Theme) {
            case 3:
                return R.color.google_dark_blue;
            case 1:
                return R.color.google_dark_blue;
            case 4:
                return android.R.color.black;
            case 5:
                return android.R.color.black;
            case 6:
                return PreferenceManager.getDefaultSharedPreferences(context).getInt(
                        PreferencesActivity.KEY_CUSTOM_THEME, 0xff4285f4);
            case 7:
                return PreferenceManager.getDefaultSharedPreferences(context).getInt(
                        PreferencesActivity.KEY_CUSTOM_THEME, 0xff4285f4);
        }
        return android.R.color.transparent;
    }

    /**
     * As above but returning a ColorDrawable
     * Handy for actionbar for example
     *
     * @param context
     * @param Theme
     * @return
     */
    public static Drawable getThemeDrawable(Context context, int Theme) {
        switch (Theme) {
            case 3:
                return new ColorDrawable(context.getResources().getColor(R.color.google_dark_blue));
            case 1:
                return new ColorDrawable(context.getResources().getColor(R.color.google_dark_blue));
            case 4:
                return new ColorDrawable(context.getResources().getColor(android.R.color.black));
            case 5:
                return new ColorDrawable(context.getResources().getColor(android.R.color.black));
            case 6:
                return new ColorDrawable(Color.parseColor("#"
                        + Integer.toHexString(PreferenceManager.getDefaultSharedPreferences(context)
                                                      .getInt(PreferencesActivity.KEY_CUSTOM_THEME,
                                                              0xff4285f4))
                                  .substring(2)));
            case 7:
                return new ColorDrawable(Color.parseColor("#"
                        + Integer.toHexString(PreferenceManager.getDefaultSharedPreferences(context)
                                                      .getInt(PreferencesActivity.KEY_CUSTOM_THEME,
                                                              0xff4285f4))
                                  .substring(2)));
        }
        return new ColorDrawable(context.getResources().getColor(android.R.color.transparent));
    }

    /**
     * Handles the dark or light notifications
     * from previous version of android
     * <p/>
     * The icons are not automatically colored from the system
     * so I have to set them dark or light depending on the version
     *
     * @param button
     * @return
     */
    public static int getResourceDrawableNotification(int button) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            switch (button) {
                case 1:
                    return R.drawable.btn_playback_next;
                case 2:
                    return R.drawable.btn_playback_pause;
                case 3:
                    return R.drawable.btn_playback_play;
                case 4:
                    return R.drawable.btn_playback_previous;
            }
        } else {
            switch (button) {
                case 1:
                    return R.drawable.btn_playback_next_black;
                case 2:
                    return R.drawable.btn_playback_pause_black;
                case 3:
                    return R.drawable.btn_playback_play_black;
                case 4:
                    return R.drawable.btn_playback_previous_black;
            }
        }
        return 0;
    }
    public static String getResourceTextNotification(int button, Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return "";
        } else {
            switch (button) {
                case 1:
                    return context.getResources().getString(R.string.next_selection);
                case 2:
                    return context.getResources().getString(R.string.pause_selection);
                case 3:
                    return context.getResources().getString(R.string.play_selection);
                case 4:
                    return context.getResources().getString(R.string.prev_selection);
            }
        }
        return "";
    }
    /**
     * Similar to above but for textColor
     *
     * @param context
     * @return
     */
    public static int getTextColorNotification(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getColor(android.R.color.white);
        } else {
            return context.getResources().getColor(R.color.google_grey);
        }
    }

    /**
     * Returns a color to colorize the drawables
     * FAB or button for example
     *
     * @param Theme
     * @param context
     * @return
     */
    static int getDrawableColor(int Theme, Context context) {
        switch (Theme) {
            case 1:
                return R.color.google_dark_blue_fab;
            case 3:
                return R.color.google_dark_blue_fab;
            case 6:
                return PreferenceManager.getDefaultSharedPreferences(context).getInt(
                        PreferencesActivity.KEY_CUSTOM_THEME, 0xff4285f4);
            case 7:
                return PreferenceManager.getDefaultSharedPreferences(context).getInt(
                        PreferencesActivity.KEY_CUSTOM_THEME, 0xff4285f4);
        }
        return R.color.holo;
    }

    public static Drawable colorizeDrawable(Drawable drawable, Context context) {
        return colorizeDrawable(
                drawable, context, getDrawableColor(getAppTheme3(context), context));
    }

    /**
     * Colorizing drawables in white for the lockscreen
     *
     * @param drawable
     * @param context
     * @return
     */
    public static Drawable colorizeLockDrawable(Drawable drawable, Context context) {
        int color = context.getResources().getColor(android.R.color.white);
        drawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    /**
     * Depending on Light or Dark theme
     * One may need a darker or lighter icon
     * <p/>
     * This methods colorizes the icons accordingly
     *
     * @param drawable
     * @param context
     * @param colorRes
     * @return
     */
    public static Drawable colorizeDrawable(Drawable drawable, Context context, int colorRes) {
        int color;
        if (getAppTheme3(context) < 6)
            color = context.getResources().getColor(colorRes);
        else
            color = colorRes;

        drawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    /**
     * Return a textColor according to the light or dark theme
     *
     * @param context
     * @return
     */
    public static int getTextColor(Context context) {
        if (getAppTheme2(context) == 3) {
            return context.getResources().getColor(R.color.dark_text_color);
        } else {
            return context.getResources().getColor(android.R.color.white);
        }
    }

    /**
     * Similar to other colorize methods
     * but takes a resId instead of a drawable
     *
     * @param mResId
     * @param context
     * @return
     */
    public static Drawable colorizeResourceDrawable(int mResId, Context context) {
        Drawable draw = context.getResources().getDrawable(mResId);
        // Add the second layer to the transiation drawable
        // Assuming "color" is your target color
        int color;
        if (getAppTheme2(context) == 3) {
            color = context.getResources().getColor(R.color.dark_text_color);
        } else {
            color = context.getResources().getColor(android.R.color.white);
        }
        draw.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);

        return draw;
    }

    public static Drawable colorizeResourceDrawableInvert(int mResId, Context context) {
        Drawable draw = context.getResources().getDrawable(mResId);
        // Add the second layer to the transiation drawable
        // Assuming "color" is your target color
        int color;
        if (getAppTheme2(context) != 3) {
            color = context.getResources().getColor(R.color.background_dark);
        } else {
            color = context.getResources().getColor(android.R.color.white);
        }
        float r = Color.red(color) / 255f;
        float g = Color.green(color) / 255f;
        float b = Color.blue(color) / 255f;

        ColorMatrix cm = new ColorMatrix(new float[] {
                // Change red channel
                r,
                0,
                0,
                0,
                0,
                // Change green channel
                0,
                g,
                0,
                0,
                0,
                // Change blue channel
                0,
                0,
                b,
                0,
                0,
                // Keep alpha channel
                0,
                0,
                0,
                1,
                0,
        });
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(cm);
        draw.setColorFilter(cf);
        return draw;
    }
}
