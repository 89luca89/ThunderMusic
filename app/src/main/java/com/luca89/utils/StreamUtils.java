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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StreamUtils {

    /**
     * A helper method to convert an InputStream into a String
     *
     * @return the String or a blank string if the IS was null
     * @throws IOException
     */
    public static String convertToString(String url) {
        if (url != null) {
            try {
                HttpClient client = new DefaultHttpClient();
                client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64; rv:47.0) Gecko/20100101 Firefox/47.0");
                HttpUriRequest request = new HttpGet(url);
                HttpResponse response = client.execute(request);

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        response.getEntity().getContent()));
                StringBuilder total = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    total.append(inputLine);
                }
                in.close();

                return total.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }

}
