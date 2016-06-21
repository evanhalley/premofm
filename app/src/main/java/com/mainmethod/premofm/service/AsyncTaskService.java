package com.mainmethod.premofm.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.util.IOUtil;

import java.util.List;

import timber.log.Timber;

/**
 * Services for executing tasks on background threads
 * Created by evanhalley on 6/13/16.
 */

public class AsyncTaskService extends IntentService {

    private static final String ACTION_DELETE_CHANNEL = "com.mainmethod.premofm.deleteChannel";
    private static final String ACTION_SUBSCRIBE_TO_CHANNEL = "com.mainmethod.premofm.subscribeToChannel";
    private static final String ACTION_UNSUBSCRIBE_FROM_CHANNEL = "com.mainmethod.premofm.unsubscribeFromChannel";
    private static final String ACTION_OPML_IMPORT = "com.mainmethod.premofm.opmlImport";
    private static final String ACTION_OPML_EXPORT = "com.mainmethod.premofm.opmlExport";
    private static final String PARAM_GENERATED_ID = "channelId";
    private static final String PARAM_OPML_URI = "opmlUri";

    public static void opmlExport(Context context, Uri uri) {
        Bundle params = new Bundle();
        params.putParcelable(PARAM_OPML_URI, uri);
        startService(context, ACTION_OPML_EXPORT, params);
    }

    public static void opmlImport(Context context, Uri uri) {
        Bundle params = new Bundle();
        params.putParcelable(PARAM_OPML_URI, uri);
        startService(context, ACTION_OPML_IMPORT, params);
    }

    public static void deleteChannel(Context context, String generatedId) {
        Bundle params = new Bundle();
        params.putString(PARAM_GENERATED_ID, generatedId);
        startService(context, ACTION_DELETE_CHANNEL, params);
    }

    public static void subscribeToChannel(Context context, String generatedId) {
        Bundle params = new Bundle();
        params.putString(PARAM_GENERATED_ID, generatedId);
        startService(context, ACTION_SUBSCRIBE_TO_CHANNEL, params);
    }

    public static void unsubscribeFromChannel(Context context, String generatedId) {
        Bundle params = new Bundle();
        params.putString(PARAM_GENERATED_ID, generatedId);
        startService(context, ACTION_UNSUBSCRIBE_FROM_CHANNEL, params);
    }

    private static void startService(Context context, String action, Bundle params) {
        Intent intent = new Intent(context, AsyncTaskService.class);
        intent.setAction(action);
        intent.putExtras(params);
        context.startService(intent);
    }

    public AsyncTaskService() {
        super("AsyncTaskService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null && intent.getAction() != null) {
            Timber.d("Executing action %s", intent.getAction());
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
                case ACTION_OPML_IMPORT:
                    String opmlData = IOUtil.readTextFromUri(this, intent.getParcelableExtra(PARAM_OPML_URI));
                    List<Channel> channels = null;
                    boolean importSuccess = false;

                    if (opmlData.length() > 0) {
                         channels = ChannelModel.getChannelsFromOpml(opmlData);
                    }

                    // TODO filter out possible duplicates

                    if (channels != null && channels.size() > 0) {
                        ChannelModel.storeImportedChannels(this, channels);
                        importSuccess = true;
                    }
                    BroadcastHelper.broadcastOpmlProcessFinish(this, importSuccess);
                    PodcastSyncService.syncAllPodcasts(this, false);
                    break;
                case ACTION_OPML_EXPORT:
                    ChannelModel.exportChannelsToOpml(this, intent.getParcelableExtra(PARAM_OPML_URI));
                    BroadcastHelper.broadcastOpmlProcessFinish(this, true);
                    break;
            }
        }
    }
}
