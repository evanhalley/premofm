package com.mainmethod.premofm.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.BroadcastHelper;

/**
 * Created by evanhalley on 6/13/16.
 */

public class AsyncJobService extends IntentService {

    private static final String ACTION_DELETE_CHANNEL = "com.mainmethod.premofm.deleteChannel";
    private static final String ACTION_SUBSCRIBE_TO_CHANNEL = "com.mainmethod.premofm.subscribeToChannel";
    private static final String ACTION_UNSUBSCRIBE_FROM_CHANNEL = "com.mainmethod.premofm.unsubscribeFromChannel";
    private static final String PARAM_GENERATED_ID = "channelId";

    public static void deleteChannel(Context context, String generatedId) {
        Intent intent = new Intent(context, AsyncJobService.class);
        intent.setAction(ACTION_DELETE_CHANNEL);
        intent.putExtra(PARAM_GENERATED_ID, generatedId);
        context.startService(intent);
    }

    public static void subscribeToChannel(Context context, String generatedId) {
        Intent intent = new Intent(context, AsyncJobService.class);
        intent.setAction(ACTION_SUBSCRIBE_TO_CHANNEL);
        intent.putExtra(PARAM_GENERATED_ID, generatedId);
        context.startService(intent);
    }

    public static void unsubscribeFromChannel(Context context, String generatedId) {
        Intent intent = new Intent(context, AsyncJobService.class);
        intent.setAction(ACTION_UNSUBSCRIBE_FROM_CHANNEL);
        intent.putExtra(PARAM_GENERATED_ID, generatedId);
        context.startService(intent);
    }

    public AsyncJobService() {
        super("AsyncJobService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null && intent.getAction() != null) {
            String generatedId;

            switch (intent.getAction()) {
                case ACTION_DELETE_CHANNEL:
                    generatedId = intent.getStringExtra(PARAM_GENERATED_ID);
                    EpisodeModel.deleteEpisodes(this, generatedId);
                    ChannelModel.deleteChannel(this, generatedId);
                    break;
                case ACTION_SUBSCRIBE_TO_CHANNEL:
                    generatedId = intent.getStringExtra(PARAM_GENERATED_ID);
                    ChannelModel.changeSubscription(this, generatedId, true);
                    EpisodeModel.markEpisodesAsChannelSubscribed(this, generatedId);
                    BroadcastHelper.broadcastSubscriptionChange(this, true, generatedId);
                    break;
                case ACTION_UNSUBSCRIBE_FROM_CHANNEL:
                    generatedId = intent.getStringExtra(PARAM_GENERATED_ID);
                    ChannelModel.changeSubscription(this, generatedId, false);
                    EpisodeModel.markEpisodesAsChannelUnsubscribed(this, generatedId);
                    BroadcastHelper.broadcastSubscriptionChange(this, false, generatedId);
                    break;
            }
        }
    }
}
