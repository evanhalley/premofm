/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.object;

import org.parceler.Parcel;

/**
 * Created by evan on 12/3/14.
 */
@Parcel(Parcel.Serialization.BEAN)
public class Channel implements Collectable {

    private int mId = -1;
    private String mGeneratedId;
    private String mTitle;
    private String mAuthor;
    private String mDescription;
    private String mSiteUrl;
    private String mFeedUrl;
    private String mArtworkUrl;
    private boolean isSubscribed;

    private long mLastSyncTime;
    private boolean mLastSyncSuccessful;
    private long mLastModified;
    private String mETag;
    private String mDataMd5;

    public Channel() {

    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getGeneratedId() {
        return mGeneratedId;
    }

    public void setGeneratedId(String generatedId) {
        mGeneratedId = generatedId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getSiteUrl() {
        return mSiteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        mSiteUrl = siteUrl;
    }

    public String getFeedUrl() {
        return mFeedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        mFeedUrl = feedUrl;
    }

    @Override
    public String getArtworkUrl() {
        return mArtworkUrl;
    }

    public void setArtworkUrl(String artworkUrl) {
        mArtworkUrl = artworkUrl;
    }

    public boolean isSubscribed() {
        return isSubscribed;
    }

    public void setSubscribed(boolean subscribed) {
        isSubscribed = subscribed;
    }

    public String getETag() {
        return mETag;
    }

    public void setETag(String eTag) {
        this.mETag = eTag;
    }

    public long getLastModified() {
        return mLastModified;
    }

    public void setLastModified(long lastModified) {
        this.mLastModified = lastModified;
    }

    public String getDataMd5() {
        return mDataMd5;
    }

    public void setDataMd5(String dataMd5) {
        this.mDataMd5 = dataMd5;
    }

    public boolean isLastSyncSuccessful() {
        return mLastSyncSuccessful;
    }

    public void setLastSyncSuccessful(boolean lastSyncSuccessful) {
        this.mLastSyncSuccessful = lastSyncSuccessful;
    }

    public long getLastSyncTime() {
        return mLastSyncTime;
    }

    public void setLastSyncTime(long lastSyncTime) {
        this.mLastSyncTime = lastSyncTime;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "mId=" + mId +
                ", mGeneratedId='" + mGeneratedId + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mAuthor='" + mAuthor + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mSiteUrl='" + mSiteUrl + '\'' +
                ", mFeedUrl='" + mFeedUrl + '\'' +
                ", mArtworkUrl='" + mArtworkUrl + '\'' +
                '}';
    }

    public boolean metadataEquals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Channel channel = (Channel) o;

        if (!mArtworkUrl.equals(channel.mArtworkUrl)) return false;
        if (!mAuthor.equals(channel.mAuthor)) return false;
        if (!mDescription.equals(channel.mDescription)) return false;
        if (!mFeedUrl.equals(channel.mFeedUrl)) return false;
        if (!mSiteUrl.equals(channel.mSiteUrl)) return false;
        if (!mTitle.equals(channel.mTitle)) return false;

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Channel channel = (Channel) o;

        if (!mArtworkUrl.equals(channel.mArtworkUrl)) return false;
        if (!mAuthor.equals(channel.mAuthor)) return false;
        if (!mDescription.equals(channel.mDescription)) return false;
        if (!mFeedUrl.equals(channel.mFeedUrl)) return false;
        if (!mGeneratedId.equals(channel.mGeneratedId)) return false;
        if (!mSiteUrl.equals(channel.mSiteUrl)) return false;
        if (!mTitle.equals(channel.mTitle)) return false;

        return true;
    }

    @Override
    public String getSubtitle() {
        return mAuthor;
    }
}
