/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.media.session.PlaybackState;

import com.mainmethod.premofm.R;

/**
 * Created by evan on 8/27/15.
 */
public class PlaybackButtonHelper {

    public static int getNotificationPlaybackButtonResId(int state) {

        // set up the appropriate button configuration
        switch (state) {
            case PlaybackState.STATE_PLAYING:
                return R.drawable.ic_notification_action_pause;
            case PlaybackState.STATE_BUFFERING:
            case PlaybackState.STATE_CONNECTING:
                return R.drawable.ic_action_buffering;
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
                return R.drawable.ic_notification_action_play;
            default:
                return -1;
        }
    }

    public static int getWidgetPlaybackButtonResId(int state) {

        // set up the appropriate button configuration
        switch (state) {
            case PlaybackState.STATE_PLAYING:
                return R.drawable.ic_notification_action_pause;
            case PlaybackState.STATE_BUFFERING:
            case PlaybackState.STATE_CONNECTING:
                return R.drawable.ic_notification_action_buffering;
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
                return R.drawable.ic_notification_action_play;
            default:
                return -1;
        }
    }

    public static int getPlayerPlaybackButtonResId(int state) {

        // set up the appropriate button configuration
        switch (state) {
            case PlaybackState.STATE_CONNECTING:
            case PlaybackState.STATE_BUFFERING:
                return R.drawable.ic_action_buffering;
            case PlaybackState.STATE_PLAYING:
                return R.drawable.ic_action_pause;
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
                return R.drawable.ic_action_play;
            default:
                return -1;
        }
    }

}
