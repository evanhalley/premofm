/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.EpisodeStatus;

/**
 * Created by evan on 4/12/15.
 */
public class ResourceValuesHelper {

    public static String getDownloadStatusLabel(Context context, int downloadStatusId) {
        int statusResId;

        switch (downloadStatusId) {
            case DownloadStatus.REQUESTED:
                statusResId = R.string.download_status_requested;
                break;
            case DownloadStatus.QUEUED:
                statusResId = R.string.download_status_queued;
                break;
            case DownloadStatus.NOT_DOWNLOADED:
                statusResId = R.string.download_status_not_downloaded;
                break;
            case DownloadStatus.DOWNLOADING:
                statusResId = R.string.download_status_downloading;
                break;
            case DownloadStatus.DOWNLOADED:
                statusResId = R.string.download_status_downloaded;
                break;
            case DownloadStatus.PARTIALLY_DOWNLOADED:
                statusResId = R.string.download_status_partially_downloaded;
                break;
            default:
                throw new IllegalArgumentException("Invalid download status ID: " +
                        downloadStatusId);
        }

        return context.getString(statusResId);
    }

    public static String getEpisodeStatusLabel(Context context, int episodeStatus) {
        int statusResId;

        switch (episodeStatus) {
            case EpisodeStatus.NEW:
                statusResId = R.string.episode_status_new;
                break;
            case EpisodeStatus.PLAYED:
                statusResId = R.string.episode_status_played;
                break;
            case EpisodeStatus.IN_PROGRESS:
                statusResId = R.string.episode_status_in_progress;
                break;
            case EpisodeStatus.COMPLETED:
                statusResId = R.string.episode_status_completed;
                break;
            case EpisodeStatus.DELETED:
                statusResId = R.string.episode_status_deleted;
                break;
            default:
                throw new IllegalArgumentException("Invalid episode status ID: " + episodeStatus);
        }

        return context.getString(statusResId);
    }

}