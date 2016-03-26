package com.mainmethod.premofm.helper;

import android.net.Uri;

import java.util.List;

/**
 * Created by evanhalley on 12/23/15.
 */
public class LinkHelper {

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
