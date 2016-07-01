package com.mainmethod.premofm.helper;

import android.net.Uri;

import com.mainmethod.premofm.object.Channel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

/**
 * Created by evanhalley on 6/21/16.
 */
public class PodcastDirectoryHelper {

    private static final String ITUNES_DIRECTORY_LOOKUP_URL = "https://itunes.apple.com/lookup?id=%1$s";
    private static final String ITUNES_HOST = "itunes.apple.com";
    public static final int DIRECTORY_TYPE_ITUNES = 10000;

    // keys for parsing things from iTunes directory JSON
    private static final String RESULTS = "results";
    private static final String RESULT_COUNT = "resultCount";
    private static final String FEED_URL = "feedUrl";
    private static final String TRACK_NAME = "trackName";
    private static final String ARTIST_NAME = "artistName";
    private static final String ARTWORK_URL_1600 = "artworkUrl1600";

    public static Channel getChannelFromITunesJson(String jsonStr) throws JSONException,
            UnsupportedEncodingException, NoSuchAlgorithmException {
        Channel channel = null;
        JSONObject json = new JSONObject(jsonStr);

        if (json.has(RESULTS) && json.has(RESULT_COUNT) && json.getInt(RESULT_COUNT) > 0) {
            JSONObject result = json.getJSONArray(RESULTS).getJSONObject(0);
            channel = new Channel();
            String feedUrl = result.getString(FEED_URL);
            channel.setFeedUrl(feedUrl);
            channel.setGeneratedId(TextHelper.generateMD5(feedUrl));
            channel.setTitle(result.optString(TRACK_NAME));
            channel.setAuthor(result.optString(ARTIST_NAME));
            channel.setArtworkUrl(result.optString(ARTWORK_URL_1600));
        }
        return channel;
    }

    public static String getDirectoryUrl(int directoryType, String directoryId) {

        switch (directoryType) {
            case DIRECTORY_TYPE_ITUNES:
                return String.format(ITUNES_DIRECTORY_LOOKUP_URL, directoryId);
            default:
                throw new IllegalArgumentException(String.format(Locale.getDefault(),
                        "Unknown directory type %d", directoryType));
        }
    }

    public static boolean containsITunesHost(Uri uri) {
        return uri != null && uri.getHost().contains(PodcastDirectoryHelper.ITUNES_HOST);
    }

    /**
     * Returns the iTunes ID from a url that looks like
     * https://itunes.apple.com/us/podcast/the-vergecast/id430333725?mt=2
     * @param uri
     * @return
     */
    public static String getITunesId(Uri uri) {

        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }

        String id = "";
        List<String> segments = uri.getPathSegments();

        for (int i = 0; segments != null && i < segments.size(); i++) {

            if (segments.get(i).startsWith("id")) {
                id = segments.get(i).replace("id", "");
                break;
            }
        }
        return id;
    }
}