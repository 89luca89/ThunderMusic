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

package com.luca89.utils.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.luca89.thundermusic.MixRadioActivity;
import com.luca89.thundermusic.OnlineActivity;
import com.luca89.thundermusic.PlaylistBrowserActivity;
import com.luca89.utils.MusicUtils;

/**
 * Necessary on Android 6.0 and above to ask for
 * storage permission because...well...
 * we need it!
 */
public class Permission extends Activity {
    private static Activity mContext;

    private void startThunderMusic() {
        int section = MusicUtils.getIntPref(this, "activesection", 1);
        Intent intent;
        switch (section) {
            case 1:
                intent = new Intent(this, OnlineActivity.class);
                break;
            case 2:
                intent = new Intent(this, MixRadioActivity.class);
                break;
            case 3:
                intent = new Intent(this, PlaylistBrowserActivity.class);
                break;
            default:
                intent = new Intent(this, OnlineActivity.class);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
        this.finish();
    }

    @Override
    public void onCreate(Bundle icicle) {
        mContext = this;

        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(mContext,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_SETTINGS}, 0);

            } else {
                startThunderMusic();
            }
        } else {
            startThunderMusic();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    startThunderMusic();
                } else {
                    finish();
                }
                return;
            }
        }
    }
}
