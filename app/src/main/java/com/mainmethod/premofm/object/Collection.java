/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.object;

import java.util.ArrayList;
import java.util.List;

/**
 * A Collection is a grouping of channels
 * Created by evan on 2/15/15.
 */
public class Collection {

    public static final int COLLECTION_TYPE_CHANNEL     = 0;
    public static final int COLLECTION_TYPE_EPISODE     = 1;

    private int mId = -1;
    private String mServerId;
    private String mName;
    private String mDescription;
    private int mType;
    private List<String> mParameters = new ArrayList<>();
    private List<String> mCollectedServerIds = new ArrayList<>();
    private int mSyncStatus;
    private int mCreatedAt;
    private boolean mIsPublic;
    private String mAuthorServerId;

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

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public void setParameters(List<String> parameters) {
        mParameters = parameters;
    }

    public List<String> getParameters() {
        return mParameters;
    }

    public List<String> getCollectedServerIds() {
        return mCollectedServerIds;
    }

    public void setCollectedServerIds(List<String> collectedServerIds) {
        mCollectedServerIds = collectedServerIds;
    }

    public String getServerId() {
        return mServerId;
    }

    public void setServerId(String mServerId) {
        this.mServerId = mServerId;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public int getSyncStatus() {
        return mSyncStatus;
    }

    public void setSyncStatus(int syncStatus) {
        mSyncStatus = syncStatus;
    }

    public boolean isPublic() {
        return mIsPublic;
    }

    public void setIsPublic(boolean isPublic) {
        mIsPublic = isPublic;
    }

    public String getAuthorServerId() {
        return mAuthorServerId;
    }

    public void setAuthorServerId(String authorServerId) {
        mAuthorServerId = authorServerId;
    }

    public int getCreatedAt() {
        return mCreatedAt;
    }

    public void setCreatedAt(int createdAt) {
        mCreatedAt = createdAt;
    }

    @Override
    public String toString() {
        return "Collection{" +
                "mId=" + mId +
                ", mServerId='" + mServerId + '\'' +
                ", mName='" + mName + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mType=" + mType +
                ", mParameters=" + mParameters +
                '}';
    }
}