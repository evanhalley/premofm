package com.mainmethod.premofm.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;

import com.mainmethod.premofm.task.SyncFeedTask;

import java.util.ArrayList;

import timber.log.Timber;

/**\
 * Created by evanhalley on 6/10/16.
 */
public class SyncFeedService extends Service {

    private static final String ACTION_ADD_FEED = "com.mainmethod.premofm.addFeed";
    private static final String ACTION_REFRESH_FEED = "com.mainmethod.premofm.refreshFeed";
    private static final String ACTION_REFRESH_ALL_FEEDS = "com.mainmethod.premofm.refreshAllFeeds";
    private static final String PARAM_CHANNEL_GENERATED_ID = "channelGeneratedId";
    private static final String PARAM_FEED_URL = "feedUrl";
    private SyncFeedTask task;

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

    public static void syncAllFeeds(Context context) {
        Intent intent = new Intent(context, SyncFeedService.class);
        intent.setAction(ACTION_REFRESH_ALL_FEEDS);
        context.startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            return START_STICKY;
        }

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Timber.d("Handling action %s", action);
            task = new SyncFeedTask(this);
            Bundle params = new Bundle();

            switch (action) {
                case ACTION_ADD_FEED:
                    params.putInt(SyncFeedTask.PARAM_MODE, SyncFeedTask.MODE_ADD_FEED);
                    params.putString(SyncFeedTask.PARAM_FEED_URL,
                            intent.getStringExtra(PARAM_FEED_URL));
                    break;
                case ACTION_REFRESH_FEED:
                    ArrayList<String> id = new ArrayList<>(1);
                    id.add(intent.getStringExtra(PARAM_CHANNEL_GENERATED_ID));
                    params.putInt(SyncFeedTask.PARAM_MODE, SyncFeedTask.MODE_SYNC_FEED);
                    params.putStringArrayList(SyncFeedTask.PARAM_GENERATED_IDS, id);
                    break;
                case ACTION_REFRESH_ALL_FEEDS:
                    params.putInt(SyncFeedTask.PARAM_MODE, SyncFeedTask.MODE_SYNC_FEED);
                    break;
                default:
                    return START_STICKY;
            }
            AsyncTaskCompat.executeParallel(task, params);
        }
        return START_STICKY;
    }
}