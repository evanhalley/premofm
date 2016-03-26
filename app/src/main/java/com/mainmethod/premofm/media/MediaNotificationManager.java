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
import android.util.Log;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.NotificationHelper;
import com.mainmethod.premofm.helper.PendingIntentHelper;
import com.mainmethod.premofm.helper.PlaybackButtonHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.PodcastPlayerService;

/**
 * Manages the notification used to control podcast playback
 * Created by evan on 3/14/15.
 */
public class MediaNotificationManager {

    private static final String TAG = MediaNotificationManager.class.getSimpleName();

    private final NotificationManager mNotificationManager;
    private final PodcastPlayerService mPodcastPlayerService;

    public MediaNotificationManager(PodcastPlayerService playService) {
        mPodcastPlayerService = playService;
        mNotificationManager = (NotificationManager) playService.getSystemService(
                Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NotificationHelper.NOTIFICATION_ID_PLAYER);
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

            if (mPodcastPlayerService.getPlaybackState() == PlaybackState.STATE_PLAYING) {
                mPodcastPlayerService.startForeground(NotificationHelper.NOTIFICATION_ID_PLAYER,
                        notification);
            } else {
                mPodcastPlayerService.stopForeground(false);
                mNotificationManager.notify(NotificationHelper.NOTIFICATION_ID_PLAYER,
                        notification);
            }
        }
    }

    /**
     * Stops a notification
     */
    public void stopNotification() {
        mNotificationManager.cancel(NotificationHelper.NOTIFICATION_ID_PLAYER);
        mPodcastPlayerService.stopForeground(true);
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

        int state = mPodcastPlayerService.getPlaybackState();
        Log.d(TAG, "Creating notification for state: " + state);
        boolean isPlaying = state == PlaybackState.STATE_PLAYING;

        // build the notification
        Notification.Builder builder = new Notification.Builder(mPodcastPlayerService)
                .setShowWhen(false)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setLargeIcon(channelArt)
                .setSmallIcon(R.drawable.ic_stat_playing)
                .setContentTitle(episode.getTitle())
                .setContentText(episode.getChannelTitle())
                .setContentIntent(PendingIntentHelper.getOpenNowPlayingIntent(mPodcastPlayerService, episode))
                .setDeleteIntent(PendingIntentHelper.getStopServiceIntent(mPodcastPlayerService))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .addAction(R.drawable.ic_notification_action_rewind, "Skip Backward",
                        PendingIntentHelper.getSeekBackwardIntent(mPodcastPlayerService))
                .addAction(PlaybackButtonHelper.getNotificationPlaybackButtonResId(state),
                        isPlaying ? "Pause" : "Play",
                        PendingIntentHelper.getPlayOrPauseIntent(mPodcastPlayerService, state))
                .addAction(R.drawable.ic_notification_action_forward, "Skip Ahead",
                        PendingIntentHelper.getSeekForwardIntent(mPodcastPlayerService))
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(session.getSessionToken())
                        .setShowActionsInCompactView(1, 2))
                .setOngoing(isPlaying);

        return builder.build();
    }
}
