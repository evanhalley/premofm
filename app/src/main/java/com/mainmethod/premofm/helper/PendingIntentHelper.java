/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.PlaybackState;

import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.receiver.RemoteControlReceiver;
import com.mainmethod.premofm.service.PodcastPlayerService;
import com.mainmethod.premofm.ui.activity.NowPlayingActivity;
import com.mainmethod.premofm.ui.activity.PremoActivity;

/**
 * Collection of static functions for building pending intents
 * Created by evan on 9/22/15.
 */
public class PendingIntentHelper {

    public static final int REQ_CODE_NEW_EPISODE_DELETE = 1;
    public static final int REQ_CODE_PLAY_EPISODE = 2;
    public static final int REQ_CODE_OPEN_PREMO_ACTIVITY = 3;
    public static final int REQ_CODE_STOP_SERVICE = 4;
    public static final int REQ_CODE_SEEK_FORWARD = 5;
    public static final int REQ_CODE_SEEK_BACKWARD = 6;
    public static final int REQ_CODE_PLAY_OR_PAUSE = 7;
    public static final int REQ_CODE_OPEN_NOW_PLAYING_ACTIVITY = 8;
    public static final int REQ_CODE_NEW_DOWNLOAD_DELETE = 9;
    public static final int REQ_CODE_MEDIA_BUTTON_RECEIVER = 10;
    public static final int REQ_CODE_SHOW_EPISODE_INFO = 11;


    public static PendingIntent getNewEpisodeDeleteIntent(Context context) {
        return PendingIntent.getBroadcast(context, REQ_CODE_NEW_EPISODE_DELETE,
                IntentHelper.getClearEpisodeNotificationsIntent(context),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent getNewDownloadsDeleteIntent(Context context) {
        return PendingIntent.getBroadcast(context, REQ_CODE_NEW_DOWNLOAD_DELETE,
                IntentHelper.getClearDownloadNotificationsIntent(context),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent getPlayEpisodeIntent(Context context, int episodeId) {
        // this is so when the user presses back, we go back to the last activity (or app)
        Intent premoIntent = new Intent(context, PremoActivity.class);
        Intent playIntent = new Intent(context, NowPlayingActivity.class);
        playIntent.putExtra(NowPlayingActivity.PARAM_EPISODE_ID, episodeId);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(premoIntent);
        stackBuilder.addNextIntent(playIntent);
        return stackBuilder.getPendingIntent(REQ_CODE_PLAY_EPISODE,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getOpenPremoActivityIntent(Context context) {
        Intent resultIntent = new Intent(context, PremoActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(PremoActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        return stackBuilder.getPendingIntent(REQ_CODE_OPEN_PREMO_ACTIVITY,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getShowEpisodeInfoIntent(Context context, int episodeId) {
        Intent resultIntent = new Intent(context, PremoActivity.class);
        resultIntent.putExtra(PremoActivity.PARAM_EPISODE_ID, episodeId);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(PremoActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        return stackBuilder.getPendingIntent(REQ_CODE_SHOW_EPISODE_INFO,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getStopServiceIntent(Context context) {
        return PendingIntent.getService(context, REQ_CODE_STOP_SERVICE,
                new Intent(context, PodcastPlayerService.class)
                        .setAction(PodcastPlayerService.ACTION_STOP_SERVICE),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public static PendingIntent getSeekForwardIntent(Context context) {
        return PendingIntent.getService(context, REQ_CODE_SEEK_FORWARD,
                new Intent(context, PodcastPlayerService.class)
                        .setAction(PodcastPlayerService.ACTION_SEEK_FORWARD),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getSeekBackwardIntent(Context context) {
        return PendingIntent.getService(context, REQ_CODE_SEEK_BACKWARD,
                new Intent(context, PodcastPlayerService.class)
                        .setAction(PodcastPlayerService.ACTION_SEEK_BACKWARD),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getPlayOrPauseIntent(Context context, int state) {
        String playOrPauseAction;

        if (state == PlaybackState.STATE_PLAYING) {
            playOrPauseAction = PodcastPlayerService.ACTION_PAUSE;
        } else {
            playOrPauseAction = PodcastPlayerService.ACTION_RESUME_PLAYBACK;
        }

        return PendingIntent.getService(context, REQ_CODE_PLAY_OR_PAUSE,
                new Intent(context, PodcastPlayerService.class).setAction(playOrPauseAction),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getOpenNowPlayingIntent(Context context, Episode episode) {
        // this is so when the user presses back, we go back to the last activity (or app)
        Intent premoIntent = new Intent(context, PremoActivity.class);
        Intent playIntent = new Intent(context, NowPlayingActivity.class);
        PaletteHelper paletteHelper = PaletteHelper.get(context);

        if (episode != null) {
            playIntent.putExtra(NowPlayingActivity.PARAM_PRIMARY_COLOR, paletteHelper.getPrimaryColor(
                    episode.getChannelServerId()));
            playIntent.putExtra(NowPlayingActivity.PARAM_TEXT_COLOR, paletteHelper.getTextColor(
                    episode.getChannelServerId()));
        }
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(premoIntent);
        stackBuilder.addNextIntent(playIntent);
        return stackBuilder.getPendingIntent(REQ_CODE_OPEN_NOW_PLAYING_ACTIVITY,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getMediaButtonReceiverIntent(Context context) {
        ComponentName mediaButtonReceiver = new ComponentName(context, RemoteControlReceiver.class);
        return PendingIntent.getBroadcast(context, REQ_CODE_MEDIA_BUTTON_RECEIVER,
                new Intent().setComponent(mediaButtonReceiver), PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
