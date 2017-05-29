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

import android.content.Context;

import java.io.InputStream;

public class Decrypt {
    public static final String SHOUTCAST_API_KEY = "shoutcast";
    public static final String SOUNDCLOUD_API_KEY = "soundcloud";
    public static final String SOUNDCLOUD_API_KEY_2 = "soundcloud2";
    public static final String YT_API_KEY = "youtube";
    public static final String YT_API_KEY_2 = "youtube2";
    private static final char[] map1 = new char[64];
    private static final byte[] map2 = new byte[128];

    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) map1[i++] = c;
        for (char c = 'a'; c <= 'z'; c++) map1[i++] = c;
        for (char c = '0'; c <= '9'; c++) map1[i++] = c;
        map1[i++] = '+';
        map1[i++] = '/';
    }

    static {
        for (int i = 0; i < map2.length; i++) map2[i] = -1;
        for (int i = 0; i < 64; i++) map2[map1[i]] = (byte) i;
    }

    public static String getKey(String provider, Context context) {
        try {
            InputStream stream = context.getAssets().open("asset.bin");

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();

            String total = "";
            char nextChar;
            String s = new String(buffer);
            s = s.substring(0, s.length() - 4);
            s = kD(s);

            for (int i = 0; i <= s.length() - 8; i += 9) {
                nextChar = (char) Integer.parseInt(s.substring(i, i + 8), 2);
                total += nextChar;
            }
            String pass[] = total.split(";");
            if (provider.equals(SHOUTCAST_API_KEY)) {
                return kD(pass[0]);
            } else if (provider.equals(SOUNDCLOUD_API_KEY)) {
                return kD(pass[1]);
            } else if (provider.equals(SOUNDCLOUD_API_KEY)) {
                return kD(pass[2]);
            } else if (provider.equals(YT_API_KEY)) {
                return kD(pass[3]);
            } else if (provider.equals(YT_API_KEY_2)) {
                return kD(pass[4]);
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static String kD(String a) {
        a = new String(decode(a));
        a = new StringBuilder(a).reverse().toString();
        String a1 = a.substring(0, a.length() / 2 + 1);
        String a2 = a.substring(a.length() / 2 + 1, a.length());
        a = a2 + a1;
        return a;
    }

    private static byte[] decode(String s) {

        char[] in = s.toCharArray();
        int iOff = 0;
        int iLen = in.length;
        if (iLen % 4 != 0)
            throw new IllegalArgumentException();
        while (iLen > 0 && in[iOff + iLen - 1] == '=') iLen--;
        int oLen = (iLen * 3) / 4;
        byte[] out = new byte[oLen];
        int ip = iOff;
        int iEnd = iOff + iLen;
        int op = 0;
        while (ip < iEnd) {
            int i0 = in[ip++];
            int i1 = in[ip++];
            int i2 = ip < iEnd ? in[ip++] : 'A';
            int i3 = ip < iEnd ? in[ip++] : 'A';
            if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127)
                throw new IllegalArgumentException();
            int b0 = map2[i0];
            int b1 = map2[i1];
            int b2 = map2[i2];
            int b3 = map2[i3];
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
                throw new IllegalArgumentException();
            int o0 = (b0 << 2) | (b1 >>> 4);
            int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
            int o2 = ((b2 & 3) << 6) | b3;
            out[op++] = (byte) o0;
            if (op < oLen) out[op++] = (byte) o1;
            if (op < oLen) out[op++] = (byte) o2;
        }
        return out;
    }
}
