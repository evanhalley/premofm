package com.mainmethod.premofm.object;

import android.util.Base64;

/**
 * Created by evan on 12/1/14.
 */
public class Credential {

    private String mUserId;
    private String mAuthToken;

    public Credential(String userId, String authToken) {
        mUserId = userId;
        mAuthToken = authToken;
    }

    public String getUserId() {
        return mUserId;
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    public String getAuthorization() {
        StringBuilder userpass = new StringBuilder();
        StringBuilder authorization = new StringBuilder();
        userpass.append(mUserId).append(":").append(mAuthToken);
        authorization.append("Basic ").append(new String(
                Base64.encode(userpass.toString().getBytes(), Base64.DEFAULT)));
        return authorization.toString();
    }

    @Override
    public String toString() {
        return "Credential{" +
                "mUserId='" + mUserId + '\'' +
                '}';
    }
}
