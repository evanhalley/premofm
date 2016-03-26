/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.api;

import com.mainmethod.premofm.http.Response;

import java.net.HttpURLConnection;

/**
 * Get API response handles encapsulating data from a HTTP POST Premo API Call
 * Created by evan on 7/26/14.
 */
public class PostApiResponse extends BaseApiResponse {

    public PostApiResponse(Response response) throws ApiException {
        super(response);
    }

    public PostApiResponse() {

    }

    public boolean isSuccessful() {

        if (getResponseCode() == HttpURLConnection.HTTP_OK) {
            return getBoolean("success");
        }else {
            return false;
        }
    }

    public String getMessage() {
        return getString("message");
    }
}
