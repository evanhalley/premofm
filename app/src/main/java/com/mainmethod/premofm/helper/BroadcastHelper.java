/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.mainmethod.premofm.object.Channel;

import org.parceler.Parcels;

import timber.log.Timber;

/**
 * Created by evan on 4/26/15.
 */
public class BroadcastHelper {

    public static final String INTENT_SUBSCRIPTION_CHANGE = "com.mainmethod.premofm.subscriptionChange";
    public static final String INTENT_EPISODE_DOWNLOADED = "com.mainmethod.premofm.episodeDownloaded";
    public static final String INTENT_DOWNLOAD_SERVICE_FINISHED = "com.mainmethod.premofm.downloadServiceFinished";
    public static final String INTENT_PLAYER_STATE_CHANGE = "com.mainmethod.premofm.playerStateChange";
    public static final String INTENT_PROGRESS_UPDATE = "com.mainmethod.premofm.progressUpdate";
    public static final String INTENT_CHANNEL_PROCESSED = "com.mainmethod.premofm.channelProcessed";
    public static final String INTENT_OPML_PROCESS_FINISH = "com.mainmethod.premofm.opmlProcessFinish";

    public static final String EXTRA_IS_SUBSCRIBED = "isSubscribed";
    public static final String EXTRA_CHANNEL_SERVER_ID = "channelServerId";
    public static final String EXTRA_EPISODE_ID = "episodeId";
    public static final String EXTRA_EPISODE_SERVER_ID = "episodeServerId";
    public static final String EXTRA_PLAYER_STATE = "playerStateId";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_BUFFERED_PROGRESS = "bufferedProgress";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_CHANNEL = "channel";
    public static final String EXTRA_SUCCESS = "success";

    public static void broadcastPlayerStateChange(Context context, int playerStateId,
                                                  String episodeServerId) {
        Intent intent = new Intent(INTENT_PLAYER_STATE_CHANGE);
        intent.putExtra(EXTRA_EPISODE_SERVER_ID, episodeServerId);
        intent.putExtra(EXTRA_PLAYER_STATE, playerStateId);
        sendBroadcast(context, intent);
    }

    public static void broadcastSubscriptionChange(Context context, boolean isSubscribed,
                                                   String generatedId) {
        Intent intent = new Intent(INTENT_SUBSCRIPTION_CHANGE);
        intent.putExtra(EXTRA_IS_SUBSCRIBED, isSubscribed);
        intent.putExtra(EXTRA_CHANNEL_SERVER_ID, generatedId);
        sendBroadcast(context, intent);
    }

    public static void broadcastDownloadServiceFinished(Context context) {
        sendBroadcast(context, new Intent(INTENT_DOWNLOAD_SERVICE_FINISHED));
    }

    public static void broadcastEpisodeDownloaded(Context context, int episodeId) {
        Intent intent = new Intent(INTENT_EPISODE_DOWNLOADED);
        intent.putExtra(EXTRA_EPISODE_ID, episodeId);
        sendBroadcast(context, intent);
    }

    public static void broadcastProgressUpdate(Context context, long progress, long bufferedProgress,
                                               long duration) {
        Intent intent = new Intent(INTENT_PROGRESS_UPDATE);
        intent.putExtra(EXTRA_PROGRESS, progress);
        intent.putExtra(EXTRA_BUFFERED_PROGRESS, bufferedProgress);
        intent.putExtra(EXTRA_DURATION, duration);
        sendBroadcast(context, intent);
    }

    public static void broadcastChannelAdded(Context context, Channel channel) {
        Intent intent = new Intent(INTENT_CHANNEL_PROCESSED);
        intent.putExtra(EXTRA_CHANNEL, Parcels.wrap(channel));
        sendBroadcast(context, intent);
    }

    public static void broadcastOpmlProcessFinish(Context context, boolean success) {
        Intent intent = new Intent(INTENT_OPML_PROCESS_FINISH);
        intent.putExtra(EXTRA_SUCCESS, success);
        sendBroadcast(context, intent);
    }

    private static void sendBroadcast(Context context, Intent intent) {

        if (!intent.getAction().contentEquals(INTENT_PROGRESS_UPDATE)) {
            Timber.d("Broadcasting intent: %s", intent.getAction());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
