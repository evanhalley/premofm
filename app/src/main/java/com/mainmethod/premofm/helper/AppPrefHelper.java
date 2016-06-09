/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Set;
import java.util.TreeSet;

/**
 * Manages storing and retrieval of Preferences
 * Created by evan on 8/24/14.
 */
public class AppPrefHelper {

    private static final String PROPERTY_REG_ID                  = "RegistrationId";
    private static final String PROPERTY_APP_VERSION             = "AppVersion";
    private static final String PROPERTY_LAST_PLAYED_EPISODE     = "LastPlayedEpisode";
    private static final String PROPERTY_LAST_EPISODE_SYNC_PUSH  = "LastEpisodeSyncPush";
    private static final String PROPERTY_LAST_EPISODE_SYNC_PULL  = "LastEpisodeSyncPull";
    private static final String PROPERTY_PLAYLIST                = "Playlist2";
    private static final String PROPERTY_SLEEP_TIMER             = "SleepTimer";
    private static final String PROPERTY_ASKED_FOR_RATING       = "AskedForRating";
    private static final String PROPERTY_USER_HAS_ONBOARDED     = "UserHasOnboarded";
    public static final String PROPERTY_FIRST_BOOT              = "FirstBoot";
    public static final String PROPERTY_EPISODE_NOTIFICATIONS   = "EpisodeNotifications";
    public static final String PROPERTY_DOWNLOAD_NOTIFICATIONS  = "DownloadNotifications";

    private static final String PROPERTY_VIEWED_FILTER_SHOWCASE  = "ViewedFilterShowcase";
    private static final String PROPERTY_VIEWED_COLLECTION_SHOWCASE  = "ViewedCollectionShowcase";
    private static final String PROPERTY_VIEWED_CATEGORY_SHOWCASE  = "ViewedCategoryShowcase";
    private static final String PROPERTY_VIEWED_NOW_PLAYING_SHOWCASE  = "ViewedNowPlayingShowcase";

    private static final String PROPERTY_PLAYBACK_SPEED         = "PlaybackSpeed_";

    private static final String PREFERENCES = "PremoPreferences";

    private SharedPreferences mPreferences;

    private static AppPrefHelper sInstance;

    public static AppPrefHelper getInstance(Context context) {

        if (sInstance == null) {
            sInstance = new AppPrefHelper(context);
        }
        return sInstance;
    }

    /**
     * Creates a new PreferenceManager
     * @param context context
     */
    private AppPrefHelper(Context context) {
        mPreferences = context.getSharedPreferences(PREFERENCES, 0);

    }

    /**
     * Removes the property with the specified key
     * @param key
     */
    public void remove(String key) {
        mPreferences.edit().remove(key).apply();
    }

    /**
     * Deletes all preference data
     */
    public void clear() {
        mPreferences.edit().clear().apply();
    }

    public float getPlaybackSpeed(String serverId) {
        float speed = mPreferences.getFloat(PROPERTY_PLAYBACK_SPEED + serverId, -1.0f);

        if (speed < MediaHelper.MIN_PLAYBACK_SPEED || speed > MediaHelper.MAX_PLAYBACK_SPEED) {
            speed = MediaHelper.DEFAULT_PLAYBACK_SPEED;
            setPlaybackSpeed(serverId, speed);
        }
        return speed;
    }

    public String getPlaybackSpeedLabel(String serverId) {
        float speed = getPlaybackSpeed(serverId);
        return MediaHelper.formatSpeed(speed);
    }

    public void setPlaybackSpeed(String serverId, float speed) {
        mPreferences.edit().putFloat(PROPERTY_PLAYBACK_SPEED + serverId, speed).apply();
    }

    public boolean hasUserOnboarded() {
        return mPreferences.getBoolean(PROPERTY_USER_HAS_ONBOARDED, false);
    }

    public void setUserHasOnboarded() {
        mPreferences.edit().putBoolean(PROPERTY_USER_HAS_ONBOARDED, true).apply();
    }

    public boolean hasAskedForRating() {
        return mPreferences.getBoolean(PROPERTY_ASKED_FOR_RATING, false);
    }

    public void setAskedForRating() {
        mPreferences.edit().putBoolean(PROPERTY_ASKED_FOR_RATING, true).apply();
    }

    public long getFirstBoot() {
        return mPreferences.getLong(PROPERTY_FIRST_BOOT, -1L);
    }

    public long getSleepTimer() {
        return mPreferences.getLong(PROPERTY_SLEEP_TIMER, -1);
    }

    public void removeSleepTimer() {
        mPreferences.edit().remove(PROPERTY_SLEEP_TIMER).apply();
    }

    public void setSleepTimer(long sleepTime) {
        mPreferences.edit().putLong(PROPERTY_SLEEP_TIMER, sleepTime).apply();
    }

    public String getPlaylist() {
        return mPreferences.getString(PROPERTY_PLAYLIST, null);
    }

    public void setPlaylist(String playlist) {
        mPreferences.edit().putString(PROPERTY_PLAYLIST, playlist).apply();
    }

    public void removePlaylist() {
        mPreferences.edit().remove(PROPERTY_PLAYLIST).apply();
    }

    public long getLastEpisodeSyncPush() {
        return mPreferences.getLong(PROPERTY_LAST_EPISODE_SYNC_PUSH, -1);
    }

    public void setLastEpisodeSyncPush(long syncTime) {
        mPreferences.edit().putLong(PROPERTY_LAST_EPISODE_SYNC_PUSH, syncTime).apply();
    }

    public long getLastEpisodeSyncPull() {
        return mPreferences.getLong(PROPERTY_LAST_EPISODE_SYNC_PULL, -1);
    }

    public void setLastEpisodeSyncPull(long syncTime) {
        mPreferences.edit().putLong(PROPERTY_LAST_EPISODE_SYNC_PULL, syncTime).apply();
    }

    public int getLastPlayedEpisodeId() {
        return mPreferences.getInt(PROPERTY_LAST_PLAYED_EPISODE, -1);
    }

    public void setLastPlayedEpisodeId(int episodeId) {
        mPreferences.edit().putInt(PROPERTY_LAST_PLAYED_EPISODE, episodeId).apply();
    }

    public void removeLastPlayedEpisodeId() {
        mPreferences.edit().remove(PROPERTY_LAST_PLAYED_EPISODE).apply();
    }

    public String getRegistrationId() {
        return mPreferences.getString(PROPERTY_REG_ID, null);
    }

    public void setRegistrationId(String registrationId) {
        mPreferences.edit().putString(PROPERTY_REG_ID, registrationId).apply();
    }

    public int getAppVersion() {
        return mPreferences.getInt(PROPERTY_APP_VERSION, -1);
    }

    public void setAppVersion(int appVersion) {
        mPreferences.edit().putInt(PROPERTY_APP_VERSION, appVersion).apply();
    }

    public boolean hasViewedFilterShowcase() {
        return mPreferences.getBoolean(PROPERTY_VIEWED_FILTER_SHOWCASE, false);
    }

    public void setViewedFilterShowcase(boolean hasViewed) {
        mPreferences.edit().putBoolean(PROPERTY_VIEWED_FILTER_SHOWCASE, hasViewed).apply();
    }

    public boolean hasViewedCollectionShowcase() {
        return mPreferences.getBoolean(PROPERTY_VIEWED_COLLECTION_SHOWCASE, false);
    }

    public void setViewedCollectionShowcase(boolean hasViewed) {
        mPreferences.edit().putBoolean(PROPERTY_VIEWED_COLLECTION_SHOWCASE, hasViewed).apply();
    }

    public boolean hasViewedCategoryShowcase() {
        return mPreferences.getBoolean(PROPERTY_VIEWED_CATEGORY_SHOWCASE, false);
    }

    public void setViewedCategoryShowcase(boolean hasViewed) {
        mPreferences.edit().putBoolean(PROPERTY_VIEWED_CATEGORY_SHOWCASE, hasViewed).apply();
    }

    public boolean hasViewedNowPlayingShowcase() {
        return mPreferences.getBoolean(PROPERTY_VIEWED_NOW_PLAYING_SHOWCASE, false);
    }

    public void setViewedNowPlayingShowcase(boolean hasViewed) {
        mPreferences.edit().putBoolean(PROPERTY_VIEWED_NOW_PLAYING_SHOWCASE, hasViewed).apply();
    }


    /**
     * Returns true if preferences contains a value for the key
     * @param key
     * @return
     */
    public boolean contains(String key) {
        return mPreferences.contains(key);
    }

    /**
     * Retrieves a string from preferences
     * @param key key of the value to retrieve
     * @return string value
     */
    public String getString(String key) {
        return mPreferences.getString(key, null);
    }

    /**
     * Retrieves a string from preferences
     * @param key key of the value to retrieve
     * @return string value
     */
    public String getString(String key, String def) {
        return mPreferences.getString(key, def);
    }

    /**
     * Stores a string in preferences
     * @param key key of the value to store
     * @param value value to store
     * @return true if the string was stored
     */
    public void putString(String key, String value) {

        if (key != null && value != null) {
            mPreferences.edit().putString(key, value).apply();
        }
    }

    /**
     * Stores a string in preferences
     * @param key key of the value to store
     * @param value value to store
     * @return true if the string was stored
     */
    public void putStringNow(String key, String value) {

        if (key != null && value != null) {
            mPreferences.edit().putString(key, value).commit();
        }
    }

    /**
     * Retrieves a int from preferences
     * @param key key of the value to retrieve
     * @return int value
     */
    public int getInt(String key) {
        return mPreferences.getInt(key, -1);
    }

    /**
     * Stores a int in preferences
     * @param key key of the value to store
     * @param value value to store
     */
    public void putInt(String key, int value) {

        if (key != null && value > -1) {
            mPreferences.edit().putInt(key, value).apply();
        }
    }

    /**
     * Retrieves a int from preferences
     * @param key key of the value to retrieve
     * @return int value
     */
    public long getLong(String key) {
        return mPreferences.getLong(key, -1);
    }

    /**
     * Stores a int in preferences
     * @param key key of the value to store
     * @param value value to store
     */
    public void putLong(String key, long value) {

        if(key != null && value > -1) {
            mPreferences.edit().putLong(key, value).apply();
        }
    }

    /**
     * Retrieves a int from preferences
     * @param key key of the value to retrieve
     * @return int value
     */
    public boolean getBool(String key) {
        return mPreferences.getBoolean(key, false);
    }

    /**
     * Stores a int in preferences
     * @param key key of the value to store
     * @param value value to store
     * @return true if the int value was stored
     */
    public void putBool(String key, boolean value) {

        if (key != null) {
            mPreferences.edit().putBoolean(key, value).apply();
        }
    }

    public void putStringSet(String key, Set<String> stringList) {

        if (TextUtils.isEmpty(key) || stringList == null) {
            return;
        }

        mPreferences.edit().putStringSet(key, stringList).apply();

    }

    public Set<String> getStringSet(String key) {

        if (TextUtils.isEmpty(key)) {
            return null;
        }
        return mPreferences.getStringSet(key, null);
    }

    public void addToStringSet(String key, Set<String> stringSet) {
        Set<String> strings = new TreeSet<>();

        if (mPreferences.contains(key)) {
            strings.addAll(getStringSet(key));
        }
        strings.addAll(stringSet);
        putStringSet(key, strings);
    }
}
