/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.http;

import java.util.List;
import java.util.Map;

/**
 * Represents a response from a network connection
 * Created by evan on 9/30/14.
 */
public class Response {

    private int mResponseCode;
    private Map<String, List<String>> mHeaderFields;
    private String mResponseBody;

    public int getResponseCode() {
        return mResponseCode;
    }

    public void setResponseCode(int responseCode) {
        mResponseCode = responseCode;
    }

    public String getResponseBody() {
        return mResponseBody;
    }

    public void setResponseBody(String responseBody) {
        mResponseBody = responseBody;
    }

    public Map<String, List<String>> getHeaderFields() {
        return mHeaderFields;
    }

    public void setHeaderFields(Map<String, List<String>> headerFields) {
        mHeaderFields = headerFields;
    }
}
