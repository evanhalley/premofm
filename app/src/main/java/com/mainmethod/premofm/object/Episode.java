/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.object;

import com.mainmethod.premofm.helper.TextHelper;

import org.parceler.Parcel;

import java.util.Date;

/**
 * Created by evan on 12/3/14.
 */
@Parcel(Parcel.Serialization.BEAN)
public class Episode implements Collectable {

    private int     mId;
    private String  mGeneratedId;
    private String  mTitle;
    private String  mDescriptionHtml;
    private String  mDescription;
    private Date    mPublishedAt;
    private long    mDuration;
    private long    mProgress;
    private String  mUrl;
    private String  mRemoteMediaUrl;
    private String  mLocalMediaUrl;
    private int     mSize;
    private int     mDownloadedSize;
    private String  mMimeType;
    private boolean mFavorite;
    private long    mUpdatedAt;
    private boolean mManuallyAdded;

    // channel data
    private String mChannelGeneratedId;
    private String mChannelTitle;
    private String mChannelArtworkUrl;
    private String mChannelAuthor;
    private boolean mChannelIsSubscribed;

    private int     mEpisodeStatus;
    private int     mDownloadStatus = DownloadStatus.NOT_DOWNLOADED;
    private boolean mManualDownload = false;

    public Episode() {

    }

    /**
     * Constructor that copies a subset of metadata to the new Episode
     * @param episode
     */
    public Episode(Episode episode) {
        mTitle = episode.getTitle();
        mChannelAuthor = episode.getChannelAuthor();
        mChannelTitle = episode.getChannelTitle();
        mChannelArtworkUrl = episode.getArtworkUrl();
        mMimeType = episode.getMimeType();
        mDuration = episode.getDuration();
        mProgress = episode.getProgress();
        mRemoteMediaUrl = episode.getRemoteMediaUrl();
        mLocalMediaUrl = episode.getLocalMediaUrl();
        mDownloadStatus = episode.getDownloadStatus();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getChannelGeneratedId() {
        return mChannelGeneratedId;
    }

    public void setChannelGeneratedId(String channelGeneratedId) {
        mChannelGeneratedId = channelGeneratedId;
    }

    public String getGeneratedId() {
        return mGeneratedId;
    }

    public void setGeneratedId(String serverId) {
        mGeneratedId = serverId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description, boolean extractFromHtml) {

        if (extractFromHtml) {
            mDescription = TextHelper.getTextFromHtml(description);
        } else {
            mDescription = description;
        }
    }

    public String getDescriptionHtml() {
        return mDescriptionHtml;
    }

    public void setDescriptionHtml(String descriptionHtml) {
        mDescriptionHtml = descriptionHtml;
    }

    public Date getPublishedAt() {
        return mPublishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        mPublishedAt = publishedAt;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    public long getProgress() {
        return mProgress;
    }

    public void setProgress(long progress) {
        mProgress = progress;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getRemoteMediaUrl() {
        return mRemoteMediaUrl;
    }

    public void setRemoteMediaUrl(String remoteMediaUrl) {
        mRemoteMediaUrl = remoteMediaUrl;
    }

    public String getLocalMediaUrl() {
        return mLocalMediaUrl;
    }

    public void setLocalMediaUrl(String localMediaUrl) {
        mLocalMediaUrl = localMediaUrl;
    }

    public int getSize() {
        return mSize;
    }

    public void setSize(int size) {
        mSize = size;
    }

    public int getDownloadedSize() {
        return mDownloadedSize;
    }

    public void setDownloadedSize(int downloadedSize) {
        mDownloadedSize = downloadedSize;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    public String getChannelTitle() {
        return mChannelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        mChannelTitle = channelTitle;
    }

    public String getChannelArtworkUrl() {
        return mChannelArtworkUrl;
    }

    public void setChannelArtworkUrl(String channelArtworkUrl) {
        mChannelArtworkUrl = channelArtworkUrl;
    }

    public boolean isChannelSubscribed() {
        return mChannelIsSubscribed;
    }

    public Episode setChannelIsSubscribed(boolean channelIsSubscribed) {
        mChannelIsSubscribed = channelIsSubscribed;
        return this;
    }

    public int getEpisodeStatus() {
        return mEpisodeStatus;
    }

    public void setEpisodeStatus(int episodeStatus) {
        mEpisodeStatus = episodeStatus;
    }

    public int getDownloadStatus() {
        return mDownloadStatus;
    }

    public void setDownloadStatus(int downloadStatus) {
        mDownloadStatus = downloadStatus;
    }

    public boolean isManualDownload() {
        return mManualDownload;
    }

    public void setManualDownload(boolean manualDownload) {
        mManualDownload = manualDownload;
    }

    public Boolean isFavorite() {
        return mFavorite;
    }

    public void setFavorite(Boolean favorite) {
        mFavorite = favorite;
    }

    public long getUpdatedAt() {
        return mUpdatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        mUpdatedAt = updatedAt;
    }

    public boolean isManuallyAdded() {
        return mManuallyAdded;
    }

    public void setManuallyAdded(boolean manuallyAdded) {
        mManuallyAdded = manuallyAdded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Episode episode = (Episode) o;

        if (!mGeneratedId.equals(episode.mGeneratedId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return mGeneratedId.hashCode();
    }

    public boolean metadataEquals(Episode episode) {
        // TODO
        return false;
    }

    @Override
    public String toString() {
        return "Episode{" +
                "mId=" + mId +
                ", mGeneratedId='" + mGeneratedId + '\'' +
                ", mChannelGeneratedId='" + mChannelGeneratedId + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mPublishedAt=" + mPublishedAt +
                ", mDuration=" + mDuration +
                ", mProgress=" + mProgress +
                ", mUrl='" + mUrl + '\'' +
                ", mRemoteMediaUrl='" + mRemoteMediaUrl + '\'' +
                ", mLocalMediaUrl='" + mLocalMediaUrl + '\'' +
                ", mSize=" + mSize +
                ", mDownloadedSize=" + mDownloadedSize +
                ", mMimeType='" + mMimeType + '\'' +
                ", mChannelTitle='" + mChannelTitle + '\'' +
                ", mChannelArtworkUrl='" + mChannelArtworkUrl + '\'' +
                ", mEpisodeStatus=" + mEpisodeStatus +
                ", mDownloadStatus=" + mDownloadStatus +
                ", mManualDownload=" + mManualDownload +
                '}';
    }

    @Override
    public String getSubtitle() {
        return mChannelTitle;
    }

    @Override
    public String getArtworkUrl() {
        return mChannelArtworkUrl;
    }

    public String getChannelAuthor() {
        return mChannelAuthor;
    }

    public void setChannelAuthor(String channelAuthor) {
        mChannelAuthor = channelAuthor;
    }
}
