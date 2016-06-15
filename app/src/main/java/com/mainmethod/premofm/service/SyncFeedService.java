package com.mainmethod.premofm.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.sync.SyncManager;

import timber.log.Timber;

/**\
 * Created by evanhalley on 6/10/16.
 */
public class SyncFeedService extends IntentService {

    private static final String ACTION_ADD_FEED = "com.mainmethod.premofm.addFeed";
    private static final String ACTION_REFRESH_FEED = "com.mainmethod.premofm.refreshFeed";
    private static final String ACTION_REFRESH_ALL_FEEDS = "com.mainmethod.premofm.refreshAllFeeds";
    private static final String PARAM_CHANNEL_GENERATED_ID = "channelGeneratedId";
    private static final String PARAM_FEED_URL = "feedUrl";
    private static final String PARAM_DO_NOTIFY = "doNotify";
    private SyncManager syncManager;

    public SyncFeedService() {
        super("SyncFeedService");
    }

    public static void addFeed(Context context, String feedUrl) {
        Intent intent = new Intent(context, SyncFeedService.class);
        intent.setAction(ACTION_ADD_FEED);
        intent.putExtra(PARAM_FEED_URL, feedUrl);
        context.startService(intent);
    }

    public static void syncFeed(Context context, String generatedId) {
        Intent intent = new Intent(context, SyncFeedService.class);
        intent.setAction(ACTION_REFRESH_FEED);
        intent.putExtra(PARAM_CHANNEL_GENERATED_ID, generatedId);
        context.startService(intent);
    }

    public static void syncAllFeeds(Context context, boolean doNotify) {
        Intent intent = new Intent(context, SyncFeedService.class);
        intent.setAction(ACTION_REFRESH_ALL_FEEDS);
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
                    case ACTION_ADD_FEED:
                        String feedUrl = intent.getStringExtra(PARAM_FEED_URL);
                        channel = new Channel();
                        channel.setFeedUrl(feedUrl);
                        channel.setGeneratedId(TextHelper.generateMD5(feedUrl));
                        syncManager = new SyncManager(this, channel);
                        break;
                    case ACTION_REFRESH_FEED:
                        channel = ChannelModel.getChannelByGeneratedId(this,
                                intent.getStringExtra(PARAM_CHANNEL_GENERATED_ID));
                        syncManager = new SyncManager(this, channel);
                        break;
                    case ACTION_REFRESH_ALL_FEEDS:
                        syncManager = new SyncManager(this, intent.getBooleanExtra(PARAM_DO_NOTIFY, false));
                        break;
                }

                if (syncManager != null) {
                    Thread thread = new Thread(syncManager);
                    thread.start();
                    thread.join();
                }
            } catch (Exception e) {
                Timber.e(e, "Error in onHandleIntent");
            }
        }
    }
}