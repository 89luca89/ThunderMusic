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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import com.luca89.thundermusic.MediaPlaybackService;
import com.luca89.thundermusic.activities.PreferencesActivity;

/**
 *
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver {

    static final  int MAX_DELAY = 1200;
    static long firstElapsed = -1;
    static long currentElapsed;
    static int count = 0;

    @Override
    public void onReceive(final Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            Intent i = new Intent(context, MediaPlaybackService.class);
            i.setAction(MediaPlaybackService.SERVICECMD);
            i.putExtra(MediaPlaybackService.CMDNAME,
                    MediaPlaybackService.CMDPAUSE);
            context.startService(i);
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = intent
                    .getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            int buttonId = intent.getIntExtra(MediaPlaybackService.CMDNOTIF, 0);

            // single quick press: pause/resume.
            // double press: next track
            // long press: start auto-shuffle mode.
            final SharedPreferences mSettings = PreferenceManager
                    .getDefaultSharedPreferences(context);

            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = MediaPlaybackService.CMDSTOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = MediaPlaybackService.CMDTOGGLEPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = MediaPlaybackService.CMDNEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = MediaPlaybackService.CMDPREVIOUS;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    command = MediaPlaybackService.CMDPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    command = MediaPlaybackService.CMDPLAY;
                    break;
            }

            if (command != null) {
                // only consider the first event in a sequence, not the
                // repeat events,
                // so that we don't trigger in cases where the first
                // event went to
                // a different app (e.g. when the user ends a phone call
                // by
                // long pressing the headset button)

                // The service may or may not be running, but we need to
                // send it
                // a command.
                final Intent i = new Intent(context, MediaPlaybackService.class);
                i.setAction(MediaPlaybackService.SERVICECMD);
                i.putExtra(MediaPlaybackService.CMDNOTIF, buttonId);

                if (keycode == KeyEvent.KEYCODE_HEADSETHOOK // COMANDI DA CUFFIE
                        && mSettings.getBoolean("enable_headsethook", true) == true) {

                    if (firstElapsed == -1) {
                        firstElapsed = System.currentTimeMillis();
                        count = 0;
                    }
                    currentElapsed = System.currentTimeMillis();

                    long duration = currentElapsed - firstElapsed;
                    if (duration > 0 && duration < MAX_DELAY
                            && keycode == KeyEvent.KEYCODE_HEADSETHOOK)
                        count++;

                    final ToneGenerator tg = new ToneGenerator(
                            AudioManager.STREAM_NOTIFICATION, 100);

                    new CountDownTimer(mSettings.getBoolean(
                            PreferencesActivity.KEY_4_CLICK, false) ? MAX_DELAY
                            : 1000, // 1 second countdown
                            9999) {
                        public void onTick(long millisUntilFinished) {
                            // Not used
                        }

                        public void onFinish() {
                            if (count == 1) {
                                i.putExtra(
                                        MediaPlaybackService.CMDNAME,
                                        getCommand(Integer.valueOf(mSettings
                                                .getString("click_action", "1"))));
                                context.startService(i);
                                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                                count = 0;
                                firstElapsed = -1;
                            } else if (count <= 3 && count > 1) {
                                i.putExtra(
                                        MediaPlaybackService.CMDNAME,
                                        getCommand(Integer.valueOf(mSettings
                                                .getString("click_action2", "2"))));
                                context.startService(i);
                                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                                count = 0;
                                firstElapsed = -1;
                            } else if (count <= 5 && count > 3) {
                                i.putExtra(
                                        MediaPlaybackService.CMDNAME,
                                        getCommand(Integer.valueOf(mSettings
                                                .getString("click_action3", "3"))));
                                context.startService(i);
                                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                                count = 0;
                                firstElapsed = -1;
                            } else if (count <= 8 && count > 5) {
                                if (mSettings.getBoolean(
                                        PreferencesActivity.KEY_4_CLICK, false)) {
                                    i.putExtra(
                                            MediaPlaybackService.CMDNAME,
                                            getCommand(Integer.valueOf(mSettings
                                                    .getString("click_action4",
                                                            "7"))));
                                    context.startService(i);
                                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                                }
                                count = 0;
                                firstElapsed = -1;
                            } else {
                                firstElapsed = -1;
                                count = 0;
                            }
                        }
                    }.start();

                } else if (action == KeyEvent.ACTION_DOWN) { //Notification command
                    i.putExtra(MediaPlaybackService.CMDNAME, command);
                    context.startService(i);
                }
            }
        }

        if (isOrderedBroadcast()) {
            abortBroadcast();
        }
    }


    private String getCommand(int action) {
        String command = null;
        switch (action) {
            case 0:
                break;
            case 1: {
                command = MediaPlaybackService.CMDTOGGLEPAUSE;
                break;
            }
            case 2: {
                command = MediaPlaybackService.CMDNEXT;
                break;
            }
            case 3: {
                command = MediaPlaybackService.CMDPREVIOUS;
                break;
            }
            case 4: {
                command = MediaPlaybackService.CMDNEXT3;
                break;
            }
            case 5: {
                command = MediaPlaybackService.CMDNEXT2;
                break;
            }
            case 6: {
                command = MediaPlaybackService.CMDPREVIOUS2;
                break;
            }
            case 7: {
                command = MediaPlaybackService.CMDSEEK;
                break;
            }
            case 8: {
                command = MediaPlaybackService.CMDSEEK2;
                break;
            }
            case 9: {
                command = MediaPlaybackService.CMDSEEK3;
                break;
            }
            case 10: {
                command = MediaPlaybackService.CMDSEEK4;
                break;
            }
            case 11: {
                command = MediaPlaybackService.CMDVOLUP;
                break;
            }
            case 12: {
                command = MediaPlaybackService.CMDVOLDOWN;
                break;
            }
            case 13: {
                command = MediaPlaybackService.CMDSTOP;
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }
        return command;
    }

}
