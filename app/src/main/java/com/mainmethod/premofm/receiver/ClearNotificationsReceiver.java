/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mainmethod.premofm.helper.AppPrefHelper;

/**
 * Created by evan on 9/22/15.
 */
public class ClearNotificationsReceiver extends BroadcastReceiver {

    public static final String CLEAR_EPISODE_NOTIFICATIONS = "com.mainmethod.premo.clearEpisodeNotifications";
    public static final String CLEAR_DOWNLOAD_NOTIFICATIONS = "com.mainmethod.premo.clearDownloadNotifications";

    @Override
    public void onReceive(Context context, Intent intent) {

        switch (intent.getAction()) {
            case CLEAR_DOWNLOAD_NOTIFICATIONS:
                AppPrefHelper.getInstance(context).remove(AppPrefHelper.PROPERTY_DOWNLOAD_NOTIFICATIONS);
                break;
            case CLEAR_EPISODE_NOTIFICATIONS:
                AppPrefHelper.getInstance(context).remove(AppPrefHelper.PROPERTY_EPISODE_NOTIFICATIONS);
                break;
        }

    }
}
