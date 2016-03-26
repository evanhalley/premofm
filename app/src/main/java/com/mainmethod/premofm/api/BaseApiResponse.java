/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.api;

import com.mainmethod.premofm.http.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by evan on 7/26/14.
 */
public class BaseApiResponse {

    private Response mResponse;
    private JSONObject mResponseJson;

    public BaseApiResponse(Response response) throws ApiException {
        mResponse = response;

        if(response.getResponseCode() == HttpsURLConnection.HTTP_OK) {

            try {
                mResponseJson = new JSONObject(response.getResponseBody());
            } catch (JSONException e) {
                throw new ApiException("Error parsing API response body",
                        ApiException.Error.OTHER);
            }
        }
    }

    public BaseApiResponse() {

    }

    public int getResponseCode() {
        return mResponse.getResponseCode();
    }

    private Object getValue(String key) throws JSONException {

        if (mResponse == null || mResponseJson == null)  {
            return null;
        }
        return mResponseJson.get(key);
    }

    public JSONObject getObject(String key) {
        JSONObject jsonObj = null;

        try {
            Object obj = getValue(key);

            if (obj != null && obj instanceof JSONObject) {
                jsonObj = (JSONObject) obj;
            }
        } catch (JSONException e) {
            //Log.w()
        }

        return jsonObj;
    }

    public JSONArray getArray(String key) {
        JSONArray value = null;

        try {
            Object obj = getValue(key);

            if (obj != null && obj instanceof JSONArray) {
                value = (JSONArray) obj;
            }
        } catch (JSONException e) {
            //Log.w()
        }

        return value;
    }

    public Boolean getBoolean(String key) {
        Boolean value = false;

        try {
            Object obj = getValue(key);

            if (obj != null && obj instanceof Boolean) {
                value = (Boolean) obj;
            }
        } catch (JSONException e) {
            //Log.w()
        }

        return value;
    }

    public String getString(String key) {
        String value = null;

        try {
            Object obj = getValue(key);

            if (obj != null && obj instanceof String) {
                value = (String) obj;
            }
        } catch (JSONException e) {
            //Log.w()
        }

        return value;
    }
}
