/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.object;

/**
 * Created by evan on 6/7/15.
 */
public class Category {

    public static final int CATEGORY_ID_ART = 1;
    public static final int CATEGORY_ID_BUSINESS = 2;
    public static final int CATEGORY_ID_COMEDY = 3;
    public static final int CATEGORY_ID_EDUCATION = 4;
    public static final int CATEGORY_ID_GAMES = 5;
    public static final int CATEGORY_ID_GOVERNMENTS = 6;
    public static final int CATEGORY_ID_HEALTH = 7;
    public static final int CATEGORY_ID_FAMILY = 8;
    public static final int CATEGORY_ID_MUSIC = 9;
    public static final int CATEGORY_ID_NEWS = 10;
    public static final int CATEGORY_ID_SPIRITUALITY = 11;
    public static final int CATEGORY_ID_SCIENCE = 12;
    public static final int CATEGORY_ID_SOCIETY = 13;
    public static final int CATEGORY_ID_SPORTS = 14;
    public static final int CATEGORY_ID_TV_FILM = 15;
    public static final int CATEGORY_ID_TECHNOLOGY = 16;

    private final int mId;
    private final String mName;

    public Category(int id, String name) {
        mId = id;
        mName = name;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }
}
