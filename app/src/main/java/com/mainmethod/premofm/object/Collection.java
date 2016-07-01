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
    private String mGeneratedId;
    private String mName;
    private String mDescription;
    private int mType;
    private List<String> mParameters = new ArrayList<>();
    private List<String> mCollectedServerIds = new ArrayList<>();

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

    public String getGeneratedId() {
        return mGeneratedId;
    }

    public void setGeneratedId(String generatedId) {
        this.mGeneratedId = generatedId;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    @Override
    public String toString() {
        return "Collection{" +
                "mId=" + mId +
                ", mGeneratedId='" + mGeneratedId + '\'' +
                ", mName='" + mName + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mType=" + mType +
                ", mParameters=" + mParameters +
                '}';
    }
}