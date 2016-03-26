/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.object;

/**
 * Allowable download states of a podcast episode
 * Created by evan on 12/4/14.
 */
public class DownloadStatus {
    public static final int NOT_DOWNLOADED          = 1;
    public static final int REQUESTED               = 2;
    public static final int QUEUED                  = 3;
    public static final int DOWNLOADING             = 4;
    public static final int DOWNLOADED              = 5;
    public static final int PARTIALLY_DOWNLOADED    = 6;
}