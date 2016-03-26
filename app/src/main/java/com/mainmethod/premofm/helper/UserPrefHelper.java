/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Encapsulation of the SharedPreferences used in the apps settings UI
 * Created by evan on 2/8/15.
 */
public class UserPrefHelper {

    private final SharedPreferences mPreferences;
    private final Context mContext;

    public static UserPrefHelper get(Context context) {
        return new UserPrefHelper(context);
    }

    /**
     * Creates a new PreferenceManager
     * @param context context
     */
    public UserPrefHelper(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;
    }

    /**
     * Returns a string setting
     * @param stringResId
     * @return
     */
    public String getString(int stringResId) {
        return getString(mContext.getString(stringResId));
    }

    /**
     * Returns a string setting
     * @param key
     * @return
     */
    public String getString(String key) {
        return mPreferences.getString(key, null);
    }

    public int getStringAsInt(String key) {

        int value = -1;
        String valueStr = getString(key);

        if (valueStr != null && valueStr.length() > 0) {
            value = Integer.parseInt(valueStr);
        }
        return value;
    }

    public int getStringAsInt(int stringResId) {
        return getStringAsInt(mContext.getString(stringResId));
    }

    public boolean getBoolean(int stringResId) {
        return getBoolean(mContext.getString(stringResId));
    }

    public boolean getBoolean(String key) {
        return mPreferences.getBoolean(key, false);
    }

    public void putString(String key, String value) {
        mPreferences.edit().putString(key, value).apply();
    }

    public void removeServerId(int keyResId, String serverId) {
        String key = mContext.getString(keyResId);
        String serverIdStr = mPreferences.getString(key, "");
        List<String> serverIds = new ArrayList<>(Arrays.asList(TextUtils.split(serverIdStr, ",")));
        int indexToRemove = -1;

        for (int i = 0; i < serverIds.size(); i++) {

            if (serverIds.get(i).contentEquals(serverId)) {
                indexToRemove = i;
                break;
            }
        }
        serverIds.remove(indexToRemove);
        putString(key, TextUtils.join(",", serverIds));
    }

    public void addServerId(int keyResId, String serverId) {
        String key = mContext.getString(keyResId);
        String serverIdStr = mPreferences.getString(key, "");

        if (isServerIdAdded(keyResId, serverId)) {
            return;
        }
        List<String> serverIds = new ArrayList<>(Arrays.asList(TextUtils.split(serverIdStr, ",")));
        serverIds.add(serverId);
        putString(key, TextUtils.join(",", serverIds));
    }

    public boolean isServerIdAdded(int keyResId, String serverId) {
        String key = mContext.getString(keyResId);
        String serverIdStr = mPreferences.getString(key, "");

        HashSet<String> serverIds = new HashSet<>(Arrays.asList(TextUtils.split(serverIdStr, ",")));
        return serverIds.contains(serverId);
    }
}
