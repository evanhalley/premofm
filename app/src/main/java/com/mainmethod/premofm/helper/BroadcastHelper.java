/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by evan on 4/26/15.
 */
public class BroadcastHelper {

    private static final String TAG = "BroadcastHelper";

    public static final String INTENT_SUBSCRIPTION_CHANGE = "com.mainmethod.premofm.subscriptionChange";
    public static final String INTENT_ACCOUNT_CHANGE = "com.mainmethod.premofm.accountChange";
    public static final String INTENT_REAUTHENTICATION = "com.mainmethod.premofm.reauthenticate";
    public static final String INTENT_EPISODE_DOWNLOADED = "com.mainmethod.premofm.episodeDownloaded";
    public static final String INTENT_DOWNLOAD_SERVICE_FINISHED = "com.mainmethod.premofm.downloadServiceFinished";
    public static final String INTENT_EPISODE_SYNC_FINISHED = "com.mainmethod.premofm.episodeSyncFinished";
    public static final String INTENT_PUSH_COLLECTION_SERVICE_FINISHED = "com.mainmethod.premofm.pushCollectionFinished";
    public static final String INTENT_EPISODES_LOADED_FROM_SERVER = "com.mainmethod.premofm.episodesLoadedFromServer";
    public static final String INTENT_USER_AUTH_RESULT = "com.mainmethod.premofm.userAuthResult";
    public static final String INTENT_PURCHASE_STORED  = "com.mainmethod.premofm.purchaseStored";
    public static final String INTENT_PLAYER_STATE_CHANGE = "com.mainmethod.premofm.playerStateChange";
    public static final String INTENT_PROGRESS_UPDATE = "com.mainmethod.premofm.progressUpdate";
    public static final String INTENT_CHANNEL_PROCESSED = "com.mainmethod.premofm.channelProcessed";

    public static final String EXTRA_IS_SUCCESSFUL = "isSuccessfull";
    public static final String EXTRA_IS_SUBSCRIBED = "isSubscribed";
    public static final String EXTRA_CHANNEL_SERVER_ID = "channelServerId";
    public static final String EXTRA_CHANNEL_ID = "channelId";
    public static final String EXTRA_EPISODE_ID = "episodeId";
    public static final String EXTRA_EPISODE_SERVER_ID = "episodeServerId";
    public static final String EXTRA_NUMBER_EPISODES_LOADED = "numberEpisodesLoaded";
    public static final String EXTRA_PLAYER_STATE = "playerStateId";

    public static final String EXTRA_IS_ACCOUNT_CHANGED = "isEmailChanged";
    public static final String EXTRA_IS_REAUTHENTICATED = "isReauthentication";
    public static final String EXTRA_IS_AUTHENTICATED = "isAuthenticated";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_PURCHASE_STORED = "isPurchaseStored";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_BUFFERED_PROGRESS = "bufferedProgress";
    public static final String EXTRA_DURATION = "duration";

    public static void broadcastPlayerStateChange(Context context, int playerStateId,
                                                  String episodeServerId) {
        Intent intent = new Intent(INTENT_PLAYER_STATE_CHANGE);
        intent.putExtra(EXTRA_EPISODE_SERVER_ID, episodeServerId);
        intent.putExtra(EXTRA_PLAYER_STATE, playerStateId);
        sendBroadcast(context, intent);
    }

    public static void broadcastPurchaseStored(Context context, boolean isPurchaseStored) {
        Intent intent = new Intent(INTENT_PURCHASE_STORED);
        intent.putExtra(EXTRA_PURCHASE_STORED, isPurchaseStored);
        sendBroadcast(context, intent);
    }

    public static void broadcastSubscriptionChange(Context context, boolean isSubscribed,
                                                   int channelId, String channelServerId) {
        Intent intent = new Intent(INTENT_SUBSCRIPTION_CHANGE);
        intent.putExtra(EXTRA_IS_SUBSCRIBED, isSubscribed);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_CHANNEL_SERVER_ID, channelServerId);
        sendBroadcast(context, intent);
    }

    public static void broadcastReauthentication(Context context, boolean successful) {
        Intent intent = new Intent(INTENT_REAUTHENTICATION);
        intent.putExtra(EXTRA_IS_REAUTHENTICATED, successful);
        sendBroadcast(context, intent);
    }

    public static void broadcastDownloadServiceFinished(Context context) {
        sendBroadcast(context, new Intent(INTENT_DOWNLOAD_SERVICE_FINISHED));
    }

    public static void broadcastEpisodeSyncFinished(Context context, boolean isSuccessful) {
        Intent intent = new Intent(INTENT_EPISODE_SYNC_FINISHED);
        intent.putExtra(EXTRA_IS_SUCCESSFUL, isSuccessful);
        sendBroadcast(context, intent);
    }

    public static void broadcastCollectionPushFinished(Context context, boolean isSuccessful) {
        Intent intent = new Intent(INTENT_PUSH_COLLECTION_SERVICE_FINISHED);
        intent.putExtra(EXTRA_IS_SUCCESSFUL, isSuccessful);
        sendBroadcast(context, intent);
    }

    public static void broadcastAccountChange(Context context, boolean accountChanged) {
        Intent intent = new Intent(INTENT_ACCOUNT_CHANGE);
        intent.putExtra(EXTRA_IS_ACCOUNT_CHANGED, accountChanged);
        sendBroadcast(context, intent);
    }

    public static void broadcastEpisodeDownloaded(Context context, int episodeId) {
        Intent intent = new Intent(INTENT_EPISODE_DOWNLOADED);
        intent.putExtra(EXTRA_EPISODE_ID, episodeId);
        sendBroadcast(context, intent);
    }

    public static void broadcastEpisodesLoadedFromServer(Context context, String channelServerId,
                                                         int numEpisodesLoaded) {
        Intent intent = new Intent(INTENT_EPISODES_LOADED_FROM_SERVER);
        intent.putExtra(EXTRA_CHANNEL_SERVER_ID, channelServerId);
        intent.putExtra(EXTRA_NUMBER_EPISODES_LOADED, numEpisodesLoaded);
        sendBroadcast(context, intent);
    }

    public static void broadcastAuthenticationResult(Context context, boolean authSuceeded, String message) {
        Intent intent = new Intent(INTENT_USER_AUTH_RESULT);
        intent.putExtra(EXTRA_IS_AUTHENTICATED, authSuceeded);
        intent.putExtra(EXTRA_MESSAGE, message);
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

    public static void broadcastChannelAdded(Context context, int channelId) {
        Intent intent = new Intent(INTENT_CHANNEL_PROCESSED);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        sendBroadcast(context, intent);
    }

    private static void sendBroadcast(Context context, Intent intent) {
        //Log.d(TAG, String.format("Broadcasting intent: %s", intent.getAction()));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
