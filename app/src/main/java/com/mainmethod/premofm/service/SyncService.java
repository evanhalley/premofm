package com.mainmethod.premofm.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Episode;

import java.util.List;

import rx.Observable;
import timber.log.Timber;

/**
 * Created by evanhalley on 6/10/16.
 */

public class SyncService extends Service {

    public static final String ACTION_REFRESH_FEED = "com.mainmethod.premofm.refreshFeed";
    public static final String ACTION_REFRESH_ALL_FEEDS = "com.mainmethod.premofm.refreshAllFeeds";
    public static final String PARAM_CHANNEL_ID = "channelId";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Timber.d("Handling action %s", action);

            switch (action) {
                case ACTION_REFRESH_FEED:
                    break;
                case ACTION_REFRESH_ALL_FEEDS:
                    break;
            }
        }
        return START_STICKY;
    }

    private void refreshFeeds(List<Channel> channelList) {
        Observable.just(channelList)
                .flatMap(this::createNotification)
                .flatMapIterable(channels -> channelList)
                .flatMap(this::updateNotification)
                .flatMap(SyncHelper::getXmlData)
                .flatMap(SyncHelper::processXmlData)
                .flatMap(SyncHelper::processFeed)
                .flatMap(this::showNewEpisodesNotification);
    }

    private Observable<List<Channel>> createNotification(List<Channel> channelList) {
        return Observable.just(channelList);
    }

    private Observable<Channel> updateNotification(Channel channel) {
        return Observable.just(channel);
    }

    private Observable<Void> showNewEpisodesNotification(List<Episode> episodeList) {
        return null;
    }
}
