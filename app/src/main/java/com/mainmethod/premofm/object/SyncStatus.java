/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.object;

/**
 * Created by evan on 7/16/15.
 */
public class SyncStatus {

    // nothing to do
    public static final int NONE            = 0;

    // need to create on the server
    public static final int PENDING_CREATE  = 1;

    // need to update on the server
    public static final int PENDING_UPDATE  = 2;

    // need to delete on the server
    public static final int PENDING_DELETE  = 3;
}
