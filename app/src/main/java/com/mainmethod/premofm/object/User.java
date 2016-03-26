/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.object;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mainmethod.premofm.helper.AppPrefHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a Premo user
 * Created by evan on 7/26/14.
 */
public class User {

    private static final String ID = "id";
    private static final String EMAIL = "email";
    private static final String NICKNAME = "nickname";
    private static final String AUTH_TOKEN = "authToken";
    private static final String IS_PREMO_LISTENER = "isPremoListener";
    private static final String IS_TEMP_USER = "isTempUser";
    private static final String IS_CURATOR = "isCurator";
    private static final String LISTENING_TIME_MS = "listeningTime";

    private String mId;
    private String mEmail;
    private String mNickname;
    private boolean mIsPremoListener;
    private boolean mIsTempUser;
    private boolean mIsCurator;
    private Credential mCredential;

    private long mListeningTime;

    /**
     * Creates a new, empty User
     */
    public User() {

    }

    public static User load(Context context) {

        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        User user = null;
        AppPrefHelper helper = AppPrefHelper.getInstance(context);
        String userJson = helper.getString(AppPrefHelper.PROPERTY_USER_PROFILE);

        try {

            if (!TextUtils.isEmpty(userJson)) {
                user = User.fromJson(userJson);
            }
        } catch (JSONException e) {
            Log.e("User", "Error in getUser");
            Log.e("User", e.toString());
        }
        return user;
    }

    public static void save(Context context, User user) {
        AppPrefHelper.getInstance(context)
                .putStringNow(AppPrefHelper.PROPERTY_USER_PROFILE, user.toJsonString());
    }

    /**
     * Creates a User, populating it with JSON data
     * @param jsonStr json to populate the new user with
     * @throws JSONException
     */
    public static User fromJson(String jsonStr) throws JSONException {
        User user = new User();
        JSONObject json = new JSONObject(jsonStr);
        user.setId(json.getString(ID));
        user.setEmail(json.getString(EMAIL));
        user.setNickname(json.optString(NICKNAME));
        user.setCredential(new Credential(json.getString(ID), json.getString(AUTH_TOKEN)));
        user.setIsPremoListener(json.optBoolean(IS_PREMO_LISTENER, false));
        user.setIsTempUser(json.optBoolean(IS_TEMP_USER, false));
        user.setIsCurator(json.optBoolean(IS_CURATOR, false));
        user.setListeningTime(json.optLong(LISTENING_TIME_MS, 0));
        return user;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getNickname() {
        return mNickname;
    }

    public void setNickname(String nickname) {
        mNickname = nickname;
    }

    public Credential getCredential() {
        return mCredential;
    }

    public void setCredential(Credential credential) {
        mCredential = credential;
    }

    public boolean getIsPremium() {
        return mIsPremoListener;
    }

    public void setIsPremoListener(boolean isPremoListener) {
        mIsPremoListener = isPremoListener;
    }

    public boolean isTempUser() {
        return mIsTempUser;
    }

    public void setIsTempUser(boolean isTempUser) {
        mIsTempUser = isTempUser;
    }

    public boolean isCurator() {
        return mIsCurator;
    }

    public void setIsCurator(boolean isCurator) {
        mIsCurator = isCurator;
    }

    public long getListeningTime() {
        return mListeningTime;
    }

    public void setListeningTime(long listeningTime) {
        this.mListeningTime = listeningTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "mId='" + mId + '\'' +
                ", mEmail='" + mEmail + '\'' +
                ", mCredential=" + mCredential +
                '}';
    }

    /**
     * Serializes the User to a JSON string
     * @return
     */
    public String toJsonString() {
        try {
            JSONObject json = new JSONObject();
            json.put(ID, mId);
            json.put(EMAIL, mEmail);
            json.put(NICKNAME, mNickname);
            json.put(AUTH_TOKEN, mCredential.getAuthToken());
            json.put(IS_PREMO_LISTENER, mIsPremoListener);
            json.put(IS_TEMP_USER, mIsTempUser);
            json.put(IS_CURATOR, mIsCurator);
            json.put(LISTENING_TIME_MS, mListeningTime);
            return json.toString();
        } catch (Exception e) {
            return null;
        }

    }
}
