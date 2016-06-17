/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.media;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.NotificationHelper;
import com.mainmethod.premofm.helper.PendingIntentHelper;
import com.mainmethod.premofm.helper.PlaybackButtonHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.PodcastPlayerService;

import timber.log.Timber;

/**
 * Manages the notification used to control podcast playback
 * Created by evan on 3/14/15.
 */
public class MediaNotificationManager {

    private final NotificationManager notificationManager;
    private final PodcastPlayerService podcastPlayerService;

    public MediaNotificationManager(PodcastPlayerService playService) {
        podcastPlayerService = playService;
        notificationManager = (NotificationManager) playService.getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_PLAYER);
    }

    /**
     * Starts a notification
     * @param episode
     * @param session
     * @param channelArt
     */
    public void startNotification(Episode episode,
                                  MediaSession session, Bitmap channelArt) {
        Notification notification = createNotification(episode, session, channelArt);

        if (notification != null) {

            if (podcastPlayerService.getPlaybackState() == PlaybackState.STATE_PLAYING) {
                podcastPlayerService.startForeground(NotificationHelper.NOTIFICATION_ID_PLAYER,
                        notification);
            } else {
                podcastPlayerService.stopForeground(false);
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID_PLAYER,
                        notification);
            }
        }
    }

    /**
     * Stops a notification
     */
    public void stopNotification() {
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_PLAYER);
        podcastPlayerService.stopForeground(true);
    }

    /**
     * Creates a notification
     * @param episode
     * @param session
     * @param channelArt
     * @return
     */
    private Notification createNotification(Episode episode,
            MediaSession session, Bitmap channelArt) {

        if (episode == null){
            return null;
        }

        int state = podcastPlayerService.getPlaybackState();
        Timber.d("Creating notification for state: %d", state);
        boolean isPlaying = state == PlaybackState.STATE_PLAYING;

        // build the notification
        Notification.Builder builder = new Notification.Builder(podcastPlayerService)
                .setShowWhen(false)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setLargeIcon(channelArt)
                .setSmallIcon(R.drawable.ic_stat_playing)
                .setContentTitle(episode.getTitle())
                .setContentText(episode.getChannelTitle())
                .setContentIntent(PendingIntentHelper.getOpenNowPlayingIntent(podcastPlayerService, episode))
                .setDeleteIntent(PendingIntentHelper.getStopServiceIntent(podcastPlayerService))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .addAction(R.drawable.ic_notification_action_rewind, "Skip Backward",
                        PendingIntentHelper.getSeekBackwardIntent(podcastPlayerService))
                .addAction(PlaybackButtonHelper.getNotificationPlaybackButtonResId(state),
                        isPlaying ? "Pause" : "Play",
                        PendingIntentHelper.getPlayOrPauseIntent(podcastPlayerService, state))
                .addAction(R.drawable.ic_notification_action_forward, "Skip Ahead",
                        PendingIntentHelper.getSeekForwardIntent(podcastPlayerService))
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(session.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(isPlaying);

        return builder.build();
    }
}
