/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.object;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by evan on 11/4/15.
 */
@Parcel(Parcel.Serialization.BEAN)
public class Playlist {

    private static final String TAG = Playlist.class.getSimpleName();
    private static final String EPISODE_IDS = "episodeIds";
    private static final String CURRENT_INDEX = "currentIndex";

    private final LinkedList<String> mEpisodeServerIds;
    private int mCurrentIndex = -1;

    @ParcelConstructor
    public Playlist(int currentIndex, LinkedList<String> episodeServerIds) {
        mCurrentIndex = currentIndex;
        mEpisodeServerIds = episodeServerIds;
    }

    public Playlist() {
        mEpisodeServerIds = new LinkedList<>();
    }

    public Playlist(List<String> episodeServerIds) {
        mEpisodeServerIds = new LinkedList<>(episodeServerIds);

        if (mEpisodeServerIds.size() > 0) {
            mCurrentIndex = 0;
        }
    }

    public Playlist(String jsonStr) {
        mEpisodeServerIds = new LinkedList<>();

        if (TextUtils.isEmpty(jsonStr)) {
            return;
        }

        try {
            JSONObject json = new JSONObject(jsonStr);
            mCurrentIndex = json.getInt(CURRENT_INDEX);
            JSONArray jsonArray = json.getJSONArray(EPISODE_IDS);

            for (int i = 0; i < jsonArray.length(); i++) {
                mEpisodeServerIds.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            Log.w(TAG, e);
        }
    }

    public void addToEnd(String episodeServerId) {
        mEpisodeServerIds.addLast(episodeServerId);

        if (mCurrentIndex == -1) {
            mCurrentIndex = 0;
        }
    }

    public void addToBeginning(String episodeServerId) {
        mEpisodeServerIds.addFirst(episodeServerId);

        if (mCurrentIndex == -1) {
            mCurrentIndex = 0;
        }
    }

    public String getCurrentEpisodeServerId() {

        if (mCurrentIndex != -1  && mCurrentIndex + 1  <= mEpisodeServerIds.size()) {
            return mEpisodeServerIds.get(mCurrentIndex);
        }
        return null;
    }

    public boolean previous() {

        if (mCurrentIndex - 1 >= 0) {
            mCurrentIndex--;
            return true;
        }
        return false;
    }

    public boolean next() {

        if (mCurrentIndex + 1 < mEpisodeServerIds.size()) {
            mCurrentIndex++;
            return true;
        }
        return false;
    }

    public boolean moveToEpisode(String episodeServerId) {

        for (int i = 0; i < mEpisodeServerIds.size(); i++) {

            if (mEpisodeServerIds.get(i).contentEquals(episodeServerId)) {
                mCurrentIndex = i;
                return true;
            }
        }
        return false;
    }

    public List<String> getEpisodeServerIds() {
        return mEpisodeServerIds;
    }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    public String toJsonString() {
        JSONObject json = new JSONObject();
        JSONArray episodeIdArr = new JSONArray();

        try {
            for (int i = 0; i < mEpisodeServerIds.size(); i++) {
                episodeIdArr.put(i, mEpisodeServerIds.get(i));
            }
            json.put(EPISODE_IDS, episodeIdArr);
            json.put(CURRENT_INDEX, mCurrentIndex);
        } catch (JSONException e) {
            Log.w(TAG, e);
        }
        return json.toString();
    }

    @Override
    public String toString() {
        return toJsonString();
    }

}