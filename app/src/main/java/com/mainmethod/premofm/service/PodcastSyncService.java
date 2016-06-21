package com.mainmethod.premofm.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.sync.SyncManager;

import timber.log.Timber;

/**\
 * Created by evanhalley on 6/10/16.
 */
public class PodcastSyncService extends IntentService {

    private static final String ACTION_ADD_PODCAST_FROM_URL = "com.mainmethod.premofm.addPodcastFromUrl";
    private static final String ACTION_ADD_PODCAST_FROM_DIRECTORY = "com.mainmethod.premofm.addPodcastFromDirectory";
    private static final String ACTION_REFRESH_PODCAST = "com.mainmethod.premofm.refreshPodcast";
    private static final String ACTION_REFRESH_ALL_PODCASTS = "com.mainmethod.premofm.refreshAllPodcasts";
    private static final String PARAM_CHANNEL_GENERATED_ID = "channelGeneratedId";
    private static final String PARAM_FEED_URL = "feedUrl";
    private static final String PARAM_DIRECTORY_ID = "directoryId";
    private static final String PARAM_DIRECTORY_TYPE = "directoryType";
    private static final String PARAM_DO_NOTIFY = "doNotify";
    private SyncManager syncManager;

    public PodcastSyncService() {
        super("PodcastSyncService");
    }

    public static void addPodcastFromUrl(Context context, String feedUrl) {
        Intent intent = new Intent(context, PodcastSyncService.class);
        intent.setAction(ACTION_ADD_PODCAST_FROM_URL);
        intent.putExtra(PARAM_FEED_URL, feedUrl);
        context.startService(intent);
    }

    public static void addPodcastFromDirectory(Context context, int directoryType, String directoryId) {
        Intent intent = new Intent(context, PodcastSyncService.class);
        intent.setAction(ACTION_ADD_PODCAST_FROM_DIRECTORY);
        intent.putExtra(PARAM_DIRECTORY_ID, directoryId);
        intent.putExtra(PARAM_DIRECTORY_TYPE, directoryType);
        context.startService(intent);
    }

    public static void syncPodcast(Context context, String generatedId) {
        Intent intent = new Intent(context, PodcastSyncService.class);
        intent.setAction(ACTION_REFRESH_PODCAST);
        intent.putExtra(PARAM_CHANNEL_GENERATED_ID, generatedId);
        context.startService(intent);
    }

    public static void syncAllPodcasts(Context context, boolean doNotify) {
        Intent intent = new Intent(context, PodcastSyncService.class);
        intent.setAction(ACTION_REFRESH_ALL_PODCASTS);
        intent.putExtra(PARAM_DO_NOTIFY, doNotify);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Timber.d("Handling action %s", action);
            Channel channel;

            try {
                switch (action) {
                    case ACTION_ADD_PODCAST_FROM_URL:
                        String feedUrl = intent.getStringExtra(PARAM_FEED_URL);
                        channel = ChannelModel.getChannelByFeedUrl(this, feedUrl);

                        if (channel != null) {
                            BroadcastHelper.broadcastPodcastProcessed(this, channel, true);
                        } else {
                            channel = new Channel();
                            channel.setFeedUrl(feedUrl);
                            channel.setGeneratedId(TextHelper.generateMD5(feedUrl));
                        }
                        syncManager = new SyncManager(this, channel);
                        break;
                    case ACTION_ADD_PODCAST_FROM_DIRECTORY:
                        String directoryId = intent.getStringExtra(PARAM_DIRECTORY_ID);
                        int directoryType = intent.getIntExtra(PARAM_DIRECTORY_TYPE, -1);
                        syncManager = new SyncManager(this, directoryType, directoryId);
                        break;
                    case ACTION_REFRESH_PODCAST:
                        channel = ChannelModel.getChannelByGeneratedId(this,
                                intent.getStringExtra(PARAM_CHANNEL_GENERATED_ID));
                        syncManager = new SyncManager(this, channel);
                        break;
                    case ACTION_REFRESH_ALL_PODCASTS:
                        syncManager = new SyncManager(this, intent.getBooleanExtra(PARAM_DO_NOTIFY, false));
                        break;
                }

                if (syncManager != null) {
                    Thread thread = new Thread(syncManager);
                    thread.start();
                    thread.join();

                    if (action.contentEquals(ACTION_REFRESH_ALL_PODCASTS)) {
                        AppPrefHelper.getInstance(this).setLastEpisodeSync(DatetimeHelper.getTimestamp());
                    }
                }
                BroadcastHelper.broadcastRssRefreshFinish(this, true);
            } catch (Exception e) {
                Timber.e(e, "Error in onHandleIntent");
                BroadcastHelper.broadcastRssRefreshFinish(this, false);
            }
        }
    }
}