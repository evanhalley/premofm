/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.util;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.object.Episode;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Manages the queue of queued episodes that are pending download
 * Created by evan on 2/19/15.
 */
public class DownloadQueue extends ContentObserver {

    private static final String TAG = DownloadQueue.class.getSimpleName();
    private final Context mContext;
    private final LinkedBlockingDeque<Episode> mQueuedEpisodes;

    public DownloadQueue(Context context) {
        super(new Handler());
        mContext = context;
        mQueuedEpisodes = new LinkedBlockingDeque<>();
    }

    public int size() {
        return mQueuedEpisodes.size();
    }

    public Episode next() {
        Episode episode = null;

        try {
            episode = mQueuedEpisodes.poll(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted: ", e);
        }
        return episode;
    }

    private void refreshQueue() {
        mQueuedEpisodes.clear();
        mQueuedEpisodes.addAll(EpisodeModel.getDownloadQueueEpisodes(mContext));
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        onChange(selfChange);
    }

    @Override
    public void onChange(boolean selfChange) {
        refreshQueue();
    }
}
