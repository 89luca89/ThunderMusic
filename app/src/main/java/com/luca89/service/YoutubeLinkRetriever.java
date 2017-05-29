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

import com.Ostermiller.util.CGIParser;
import com.luca89.utils.StreamUtils;

import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by luca-linux on 6/2/16.
 */
public class YoutubeLinkRetriever {


    public static String getYtLink(String link) {

        try {

            int fmt;
            String encoded_fmt_stream_map, adaptive_fmts_stream_map;
            String html5player;
            String result;

            String ytPage = StreamUtils
                    .convertToString(link
                            + "&el=detailpage&ps=default&eurl=&gl=US&hl=en");

            String jsonString = ytPage.substring(
                    ytPage.indexOf("ytplayer.config = ")
                            + "ytplayer.config = ".length(),
                    ytPage.indexOf(";ytplayer.load ="));

            JSONObject json = new JSONObject(jsonString);

            html5player = json.getJSONObject("assets").getString("js");
            html5player = "https:" + html5player;

            // Retrieve normal stream map (mp4 videos and flash)
            encoded_fmt_stream_map = json.getJSONObject("args").getString(
                    "url_encoded_fmt_stream_map");
            encoded_fmt_stream_map = URLDecoder.decode(encoded_fmt_stream_map, "utf-8");

            LinkedHashMap<Integer, String> links = getFMTLinkMap(encoded_fmt_stream_map);

            // Retrieve adaptive stream map (DASH audio and video)
            adaptive_fmts_stream_map = json.getJSONObject("args").getString(
                    "adaptive_fmts");
            adaptive_fmts_stream_map = URLDecoder.decode(adaptive_fmts_stream_map, "utf-8");

            links.putAll(getFMTLinkMap(adaptive_fmts_stream_map));

            /**
             * Quality order
             * 251  webm/audio best
             * 171  webm/audio
             * 140  mp4/audio
             * 250  webm/audio medium
             * 22   mp4/video best
             * 43   mp4/video 480p
             * 18   mp4/video 360p
             * 17   mp4/video 144p
             */
            if (links.get(140) != null) {
                result = links.get(140);
            } else if (links.get(251) != null) {
                result = links.get(251);
            } else if (links.get(250) != null) {
                result = links.get(250);
            } else if (links.get(22) != null) {
                result = links.get(22);
            } else if (links.get(18) != null) {
                result = links.get(18);
            } else if (links.get(36) != null) {
                result = links.get(36);
            } else {
                result = links.get(17);
            }

            if (result.contains("&s=")) {

                /** It's ciphered
                 *  act accordingly:
                 *  find the ciphered key
                 *  decipher it
                 *  swap it in the url changing the parameter from
                 *  &s to &signature
                 * **/
                String key = result.substring(result.indexOf("&s=") + 3, result.indexOf("&sparams"));
                String newkey = decipherKey(key, html5player);
                result = result.replace("&s=" + key, "&signature=" + newkey);
            }

            return result;
        } catch (Exception e4) {
            e4.printStackTrace();
            return "";
        }
    }


    /**
     * Parse the result from the jSon using CGIParser and regenerate links
     * dinamically using params as described by the sparams parameter
     *
     * @param tmpstr
     * @return
     * @throws UnsupportedEncodingException
     */
    private static LinkedHashMap<Integer, String> getFMTLinkMap(String tmpstr)
            throws UnsupportedEncodingException {

        LinkedHashMap<Integer, String> result = new LinkedHashMap<Integer, String>();

        String[] tmp;
        tmp = tmpstr.split(",");
        CGIParser tags;
        String link = "";
        int itag;
        for (int i = 0; i < tmp.length; i++) {

            tags = new CGIParser(org.jsoup.parser.Parser.unescapeEntities(tmp[i], true), "utf-8");

            if (tags.getParameter("url") != null && tags.getParameter("itag") != null) {

                itag = Integer.parseInt(tags.getParameter("itag"));
                link = tags.getParameter("url");

                String[] sparmams = tags.getParameter("sparams").split(",");
                for (String par : sparmams) {
                    if (tags.getParameter(par) != null)
                        link = link + "&" + par + "=" + tags.getParameter(par);
                }

                if (!link.contains("upn")) {
                    link = link + "&upn=" + tags.getParameter("upn");
                }
                if (!link.contains("key")) {
                    link = link + "&key=" + tags.getParameter("key");
                }
                if (!link.contains("sver")) {
                    link = link + "&sver=" + tags.getParameter("sver");
                }
                if (!link.contains("fexp")) {
                    link = link + "&fexp=" + tags.getParameter("fexp");
                }
                if (tags.getParameter("signature") != null) {
                    link = link + "&signature=" + tags.getParameter("signature");
                }
                if (tags.getParameter("s") != null) {
                    link = link + "&s=" + tags.getParameter("s");
                }
                link = link + "&sparams=" + tags.getParameter("sparams");

                result.put(itag, link);
            }

        }
        return result;
    }

    /**
     * Credits to NewPipe project thanks to Christian Schabesberger
     * This parser for finding the youtube decryption key
     * is derived from the  NewPipe code
     * https://github.com/TeamNewPipe/NewPipe/blob/master/app/src/main/java/org/schabi/newpipe/extractor/services/youtube/YoutubeStreamExtractor.java
     * <p>
     * This code consists in finding the decrypting code in the
     * html5 player code
     * to ensure its working, we also find for additional helper functions
     * in the code
     *
     * @param playerUrl
     */

    private static String loadFunction(String playerUrl) {
        String decryptionFuncName = "";
        String decryptionFunc = "";
        String helperObjectName = "";
        String helperObject = "";
        String callerFunc = "function " + "decrypt" + "(a){return %%(a);}";
        String decipherFunc = "";

        try {
            if (!playerUrl.contains("https://youtube.com")) {
                //sometimes the https://youtube.com part does not get send with
                //than we have to add it by hand
                playerUrl = playerUrl.replace("https:", "");
                playerUrl = "https://youtube.com" + playerUrl;
            }
            String playerCode = StreamUtils.convertToString(playerUrl);

            decryptionFuncName =
                    Parser.matchGroup("([\"\\'])signature\\1\\s*,\\s*([a-zA-Z0-9$]+)\\(", playerCode, 2);

            String functionPattern = "("
                    + decryptionFuncName.replace("$", "\\$")
                    + "=function\\([a-zA-Z0-9_]+\\)\\{.+?\\})";
            decryptionFunc = "var " + Parser.matchGroup1(functionPattern, playerCode) + ";";

            helperObjectName = Parser
                    .matchGroup1(";([A-Za-z0-9_\\$]{2})\\...\\(", decryptionFunc);

            String helperPattern = "(var "
                    + helperObjectName.replace("$", "\\$") + "=\\{.+?\\}\\};)";
            helperObject = Parser.matchGroup1(helperPattern, playerCode);


            callerFunc = callerFunc.replace("%%", decryptionFuncName);

            decipherFunc = helperObject + decryptionFunc + callerFunc;
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
        return decipherFunc;
    }

    /**
     * After finding the decrypted code in the js html5 player code
     * run the code passing the encryptedSig parameter
     *
     * @param encryptedSig
     * @param html5player
     * @return
     * @throws Exception
     */
    private static String decipherKey(String encryptedSig, String html5player)
            throws Exception {
        String decipherFunc = loadFunction(html5player);
        Context context = Context.enter();
        // Rhino interpreted mode
        context.setOptimizationLevel(-1);
        Object result = null;
        try {
            ScriptableObject scope = context.initStandardObjects();
            context.evaluateString(scope, decipherFunc, "decipherFunc", 1, null);
            Function decryptionFunc = (Function) scope.get("decrypt", scope);
            result = decryptionFunc.call(context, scope, scope, new Object[]{encryptedSig});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Context.exit();
        }
        if (result == null) {
            return "";
        } else {
            return result.toString();
        }
    }
}

/**
 * All credits to NewPipe project thanks to Christian Schabesberger
 * This parser for finding the youtube decryption key
 * is derived from the  NewPipe code
 **/
class Parser {

    private Parser() {
    }

    public static String matchGroup1(String pattern, String input) throws RegexException {
        return matchGroup(pattern, input, 1);
    }

    public static String matchGroup(String pattern, String input, int group) throws RegexException {
        Pattern pat = Pattern.compile(pattern);
        Matcher mat = pat.matcher(input);
        boolean foundMatch = mat.find();
        if (foundMatch) {
            return mat.group(group);
        } else {
            if (input.length() > 1024) {
                throw new RegexException("failed to find pattern \"" + pattern);
            } else {
                throw new RegexException("failed to find pattern \"" + pattern + " inside of " + input + "\"");
            }
        }
    }

    public static class RegexException extends Exception {
        public RegexException(String message) {
            super(message);
        }
    }
}
