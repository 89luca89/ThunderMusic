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

package com.luca89.thundermusic.activities;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.luca89.thundermusic.R;
import com.luca89.utils.InterfaceUtils;

/**
 * The preferences activity in which one can change application preferences.
 */
public class PreferencesActivity extends PreferenceActivity {
    /**
     * Initialize the activity, loading the preference specifications.
     */
    public static final String GRID_MODE = "grid_mode";
    public static final String KEY_ENABLE_FLIP = "enable_flip";
    public static final String KEY_DUCK_ATTENUATION = "duck_attenuation";
    public static final String KEY_START_SCREEN = "start_screen";
    public static final String KEY_CUSTOM_THEME = "custom_theme";
    public static final String KEY_THEME = "theme";
    public static final String KEY_TITLE = "title";
    public static final String KEY_NOTIFICATION = "notification";
    public static final String KEY_NOW_PLAYING_EXPANDED = "now_playing_in_title";
    public static final String KEY_MUSMART_LOCK = "smart_lock";
    public static final String KEY_AOSP_LOCK = "aosp_lock";
    public static final String KEY_4_CLICK = "four_click_enable";
    public static final String SAVE_LYRICS = "save_lyrics";
    public static final String POPUP_ON = "open_popup";
    public static final String NOTIFICAION_ON = "open_notification";


    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(R.style.SettingsTheme);

        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setIcon(R.drawable.null_icon);
        if (InterfaceUtils.getTabletMode(this)) {
            addPreferencesFromResource(R.xml.preferences_tab);
        } else {
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}