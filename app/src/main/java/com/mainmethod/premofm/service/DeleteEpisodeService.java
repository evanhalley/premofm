/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */
package com.mainmethod.premofm.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.util.IOUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Intent service that deletes old episodes that are downloaded
 * Created by evan on 2/17/15.
 */

public class DeleteEpisodeService extends IntentService {

    private static final String TAG = DeleteEpisodeService.class.getSimpleName();

    public static final String ACTION_EPISODE               = "episode";
    public static final String ACTION_ELIGIBLE_EPISODES     = "eligibleEpisodes";
    public static final String ACTION_COMPLETED_EPISODES    = "completedEpisodes";
    public static final String ACTION_ALL_EPISODES          = "allEpisodes";
    public static final String PARAM_EPISODE_ID             = "episodeId";

    public DeleteEpisodeService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int episodeId = -1;
        String[] filenameArr = null;
        String action = intent.getAction();
        Bundle options = intent.getExtras();
        Log.d(TAG, "Started service, action: " + action);

        switch (action) {
            case ACTION_EPISODE:
                episodeId = options.getInt(PARAM_EPISODE_ID);
                String filename = getEpisodeToDelete(episodeId);

                if (filename != null && filename.length() > 0) {
                    filenameArr = new String[]{filename};
                }
                break;
            case ACTION_ELIGIBLE_EPISODES:
                filenameArr = getEpisodesToDelete();
                break;
            case ACTION_COMPLETED_EPISODES:
                filenameArr = getCompletedEpisodesToDelete();
                break;
            case ACTION_ALL_EPISODES:
                filenameArr = getAllEpisodesToDelete();
                break;
        }

        if (filenameArr != null && filenameArr.length > 0) {
            boolean result = IOUtil.deleteFiles(filenameArr);
            // mark the episodes as deleted in the database

            for (int i = 0; i <filenameArr.length; i++) {
                boolean updated = EpisodeModel.markEpisodeDeleted(this, filenameArr[i]);
                Log.d(TAG, "Records updated: " + updated);
            }
            Log.d(TAG, "Episode delete succeeded: " + result);
        }

        // we didn't have a legit filename for the episode, let's mark it as not downloaded (cleanup)
        if ((filenameArr == null || filenameArr.length == 0) &&
                action == ACTION_EPISODE && episodeId != -1) {
            boolean deleted = EpisodeModel.markEpisodeDeleted(this, episodeId);
            Log.d(TAG, "Records updated: " + deleted);
        }
    }

    /**
     * Returns the filename of the episode with the specified ID
     * @param episodeId
     * @return
     */
    private String getEpisodeToDelete(int episodeId) {
        String filename = null;
        List<Episode> episodes = EpisodeModel.getEpisodes(this,
                PremoContract.EpisodeEntry._ID + " = ?",
                new String[]{String.valueOf(episodeId)}, null);

        if (episodes != null && episodes.size() == 1) {
            filename = episodes.get(0).getLocalMediaUrl();
        }
        return filename;
    }

    /**
     * Returns a list of filenames of downloaded finished episodes
     * @return
     */
    private String[] getCompletedEpisodesToDelete() {
        List<String> filenameList = new ArrayList<>();

        // 1) Get channels
        Collection<Channel> channels = ChannelModel.getChannels(this);

        // 2) Query the DB, get that channel's DOWNLOADED episodes, order by published_at
        for (Channel channel : channels) {

            // is this channel in a collection

            List<Episode> episodeList = EpisodeModel.getEpisodes(this,
                    PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID + " = ? AND " +
                            PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + " = ? AND " +
                            PremoContract.EpisodeEntry.EPISODE_STATUS_ID + " = ?",
                    new String[]{channel.getGeneratedId(),
                            String.valueOf(DownloadStatus.DOWNLOADED),
                            String.valueOf(EpisodeStatus.COMPLETED)}, null);

            for (int i = 0; i < episodeList.size(); i++) {
                filenameList.add(episodeList.get(i).getLocalMediaUrl());
            }
        }

        Log.d(TAG, "Number of episodes to delete: " + filenameList.size());

        // 3) Return
        String[] filenameArr = new String[filenameList.size()];
        return filenameList.toArray(filenameArr);
    }

    /**
     * Returns a list of filenames of all downloaded episodes
     * @return
     */
    private String[] getAllEpisodesToDelete() {
        List<String> filenameList = new ArrayList<>();

        // 1) get channels that are downloaded, but not currently in progress
        List<Episode> episodeList = EpisodeModel.getEpisodes(this,
                PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + " = ? AND " +
                        PremoContract.EpisodeEntry.EPISODE_STATUS_ID + " != ?",
                new String[]{String.valueOf(DownloadStatus.DOWNLOADED),
                        String.valueOf(EpisodeStatus.IN_PROGRESS)}, null);

        for (int i = 0; i < episodeList.size(); i++) {
            filenameList.add(episodeList.get(i).getLocalMediaUrl());
        }

        Log.d(TAG, "Number of episodes to delete: " + filenameList.size());

        // 2) Return
        String[] filenameArr = new String[filenameList.size()];
        return filenameList.toArray(filenameArr);
    }

    /**
     * Use the user's preferences and get the episodes and their local media urls for
     * deletion
     */
    private String[] getEpisodesToDelete() {
        List<String> filenameList = new ArrayList<>();

        // 1) Get the user's preference on how many episodes to cache per channel
        int cacheLimit = UserPrefHelper.get(this).getStringAsInt(
                R.string.pref_key_episode_cache_limit);

        // 2) Get channels
        Collection<Channel> channels = ChannelModel.getChannels(this);

        // 3) Query the DB, get that channel's DOWNLOADED episodes, order by published_at
        for (Channel channel : channels) {

            // is this channel in a collection

            List<Episode> episodeList = EpisodeModel.getEpisodes(this,
                    PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID + " = ? AND " +
                            PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + " = ? AND " +
                            PremoContract.EpisodeEntry.MANUAL_DOWNLOAD + " != ?",
                    new String[]{channel.getGeneratedId(),
                            String.valueOf(DownloadStatus.DOWNLOADED),
                            String.valueOf(1)},
                    PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS + " ASC");

            // 4) Is the downloaded episodelist greater than the cache limits
            if (episodeList.size() > cacheLimit && cacheLimit > -1) {
                int numToDelete = episodeList.size() - cacheLimit;

                for (int i = 0; i < numToDelete; i++) {
                    filenameList.add(episodeList.get(i).getLocalMediaUrl());
                }
            }
        }

        Log.d(TAG, "Number of episodes to delete: " + filenameList.size());

        // 5) Return
        String[] filenameArr = new String[filenameList.size()];
        return filenameList.toArray(filenameArr);
    }

    public static void deleteEpisode(Context context, int episodeId) {
        Intent intent = new Intent(context, DeleteEpisodeService.class);
        intent.setAction(ACTION_EPISODE);
        intent.putExtra(PARAM_EPISODE_ID, episodeId);
        context.startService(intent);
    }

    public static void deleteAllEpisodes(Context context) {
        Intent intent = new Intent(context, DeleteEpisodeService.class);
        intent.setAction(ACTION_ALL_EPISODES);
        context.startService(intent);
    }
}
