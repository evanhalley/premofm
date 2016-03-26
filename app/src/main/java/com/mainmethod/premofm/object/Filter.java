/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.object;

import com.mainmethod.premofm.data.model.FilterModel;
import com.mainmethod.premofm.helper.TextHelper;

import org.parceler.Parcel;

import java.util.Arrays;

/**
 * Created by evan on 6/14/15.
 */
@Parcel(Parcel.Serialization.BEAN)
public class Filter {

    private int mId;
    private String mName;
    private Integer[] mEpisodeStatusIds;
    private Integer[] mDownloadStatusIds;
    private boolean mFavorite;
    private int mCollectionId = FilterModel.DISABLED;
    private boolean mUserCreated;
    private int mFilterOrder;
    private int mEpisodesPerChannel = FilterModel.DISABLED;
    private int mDaysSincePublished = FilterModel.DISABLED;
    private boolean mEpisodesManuallyAdded;

    public Filter() {

    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public void setFavorite(boolean favorite) {
        mFavorite = favorite;
    }

    public Integer[] getEpisodeStatusIds() {
        return mEpisodeStatusIds;
    }

    public String getEpisodeStatusIdsStr() {
        return TextHelper.joinIntegers(mEpisodeStatusIds);
    }

    public void setEpisodeStatusIds(Integer[] episodeStatusIds) {
        mEpisodeStatusIds = episodeStatusIds;
    }

    public void setEpisodeStatusIdsFromStr(String episodeStatusIdsStr) {
        mEpisodeStatusIds = TextHelper.splitToIntArray(episodeStatusIdsStr);
    }

    public Integer[] getDownloadStatusIds() {
        return mDownloadStatusIds;
    }

    public String getDownloadStatusIdsStr() {
        return TextHelper.joinIntegers(mDownloadStatusIds);
    }

    public void setDownloadStatusIds(Integer[] downloadStatusIds) {
        mDownloadStatusIds = downloadStatusIds;
    }

    public void setDownloadStatusIdsFromStr(String downloadStatusIdsStr) {
        mDownloadStatusIds = TextHelper.splitToIntArray(downloadStatusIdsStr);
    }

    public int getCollectionId() {
        return mCollectionId;
    }

    public void setCollectionId(int collectionId) {
        mCollectionId = collectionId;
    }

    public boolean isUserCreated() {
        return mUserCreated;
    }

    public void setUserCreated(boolean userCreated) {
        mUserCreated = userCreated;
    }

    public int getOrder() {
        return mFilterOrder;
    }

    public void setOrder(int order) {
        mFilterOrder = order;
    }

    public int getEpisodesPerChannel() {
        return mEpisodesPerChannel;
    }

    public void setEpisodesPerChannel(int episodesPerChannel) {
        mEpisodesPerChannel = episodesPerChannel;
    }

    public int getDaysSincePublished() {
        return mDaysSincePublished;
    }

    public void setDaysSincePublished(int daysSincePublished) {
        mDaysSincePublished = daysSincePublished;
    }

    public boolean isEpisodesManuallyAdded() {
        return mEpisodesManuallyAdded;
    }

    public void setEpisodesManuallyAdded(boolean episodesManuallyAdded) {
        mEpisodesManuallyAdded = episodesManuallyAdded;
    }

    @Override
    public int hashCode() {
        return mId;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "mId=" + mId +
                ", mName='" + mName + '\'' +
                ", mEpisodeStatusIds=" + Arrays.toString(mEpisodeStatusIds) +
                ", mDownloadStatusIds=" + Arrays.toString(mDownloadStatusIds) +
                ", mFavorite=" + mFavorite +
                ", mCollectionId=" + mCollectionId +
                ", mUserCreated=" + mUserCreated +
                ", mFilterOrder=" + mFilterOrder +
                ", mEpisodesPerChannel=" + mEpisodesPerChannel +
                '}';
    }
}
