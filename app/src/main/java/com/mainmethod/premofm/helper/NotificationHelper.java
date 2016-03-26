/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.DownloadService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convenience functions for displaying notifications
 * Created by evan on 7/15/15.
 */
public class NotificationHelper {

    private static final String TAG = NotificationHelper.class.getSimpleName();
    public static final int NOTIFICATION_ID_NEW_EPISODES        = 111;
    public static final int NOTIFICATION_ID_INSUFFICIENT_SPACE  = 222;
    public static final int NOTIFICATION_ID_DOWNLOADING         = 333;
    public static final int NOTIFICATION_ID_DOWNLOADED          = 444;
    public static final int NOTIFICATION_ID_PLAYER              = 555;

    private static final int MAX_EPISODES_FOR_NOTIFICATION      = 3;
    private static final int DESCRIPTION_MAX_LENGTH             = 200;

    public static void dismissNotification(Context context, int notificationId) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(notificationId);
    }

    public static NotificationManager getNotificationManager(Context context) {
        return ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
    }

    public static void showNewEpisodeNotification(Context context) {
        Log.d(TAG, "Showing notification");

        // let's get episodes to notify
        Set<String> episodeServerIds = AppPrefHelper.getInstance(context)
                .getStringSet(AppPrefHelper.PROPERTY_EPISODE_NOTIFICATIONS);

        if (episodeServerIds == null || episodeServerIds.size() == 0) {
            return;
        }
        List<Episode> episodeList = EpisodeModel.getEpisodesByServerId(context, episodeServerIds);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Notification.Builder builder = new Notification.Builder(context);

        // configure the notification's appearance
        builder.setSmallIcon(R.drawable.ic_stat_logo);
        builder.setPriority(Notification.PRIORITY_DEFAULT);

        if (preferences.getBoolean(context.getString(R.string.pref_key_notification_sound), false)) {
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        }

        if (preferences.getBoolean(context.getString(R.string.pref_key_notification_light), false)) {
            builder.setLights(Color.argb(255, 255, 101, 10), 600, 400);
        }

        if (preferences.getBoolean(context.getString(R.string.pref_key_notification_vibrate), false)) {
            builder.setVibrate(new long[] { 0, 75, 115, 75 });
        }

        if (episodeList.size() == 1) {
            Episode episode = EpisodeModel.getEpisodeByServerId(context,
                    episodeList.get(0).getServerId());
            String title = context.getString(R.string.notification_new_episode,
                    episode.getChannelTitle());
            builder.setContentTitle(title);

            if (!TextUtils.isEmpty(episode.getDescription())) {
                Spanned description;

                if (episode.getDescription().length() > DESCRIPTION_MAX_LENGTH) {
                    description = Html.fromHtml("<b>" + episode.getTitle() + "</b><br/>" +
                            episode.getDescription().substring(0, DESCRIPTION_MAX_LENGTH) + "...");
                } else {
                    description = SpannableString.valueOf(episode.getDescription());
                }
                Notification.BigTextStyle style = new Notification.BigTextStyle();
                style.setBigContentTitle(title);
                style.bigText(description);
                builder.setStyle(style);
            }
            builder.addAction(R.drawable.ic_notification_action_play, context.getString(R.string.notification_play_episode),
                    PendingIntentHelper.getPlayEpisodeIntent(context, episode.getId()));
            builder.addAction(R.drawable.ic_notification_action_info, context.getString(R.string.notification_show_notes),
                    PendingIntentHelper.getShowEpisodeInfoIntent(context, episode.getId()));
        } else {
            String title = String.format(context.getString(R.string.notification_new_episodes), episodeList.size());
            builder.setContentTitle(title);
            builder.setStyle(buildEpisodesInboxStyle(context, title, episodeList));
        }
        builder.setContentIntent(PendingIntentHelper.getOpenPremoActivityIntent(context));
        builder.setContentText(buildNotificationContent(episodeList));
        builder.setDeleteIntent(PendingIntentHelper.getNewEpisodeDeleteIntent(context));
        builder.setAutoCancel(true);
        Notification notification = builder.build();
        getNotificationManager(context).notify(NOTIFICATION_ID_NEW_EPISODES, notification);
    }

    public static void showErrorNotification(Context context, int titleResId, int messageResId) {
        showErrorNotification(context, context.getString(titleResId),
                context.getString(messageResId));
    }

    public static void showErrorNotification(Context context, String title, String content) {
        Notification.BigTextStyle style = new Notification.BigTextStyle();
        style.setBigContentTitle(title);
        style.bigText(content);
        Notification notification = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(PendingIntentHelper.getOpenPremoActivityIntent(context))
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setStyle(style)
                .build();
        getNotificationManager(context).notify(NOTIFICATION_ID_INSUFFICIENT_SPACE, notification);
    }

    private static Notification.InboxStyle buildEpisodesInboxStyle(Context context,
                                                                   String title, List<Episode> episodeList) {
        Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
        inboxStyle.setBigContentTitle(title);
        int numRemainingEpisodes = -1;

        for (int i = 0; i < episodeList.size(); i++) {

            if (i >= MAX_EPISODES_FOR_NOTIFICATION) {
                numRemainingEpisodes = episodeList.size() - MAX_EPISODES_FOR_NOTIFICATION;
                break;
            }
            String line;

            if (episodeList.get(i).getChannelTitle() != null) {
                line = "<b>" + episodeList.get(i).getChannelTitle() + "</b> " + episodeList.get(i).getTitle();
            } else {
                line = episodeList.get(i).getTitle();
            }
            inboxStyle.addLine(Html.fromHtml(line));
        }
        if (numRemainingEpisodes != -1) {
            inboxStyle.setSummaryText("+" + numRemainingEpisodes + " " + context.getString(R.string.more));
        }
        return inboxStyle;
    }

    private static String buildNotificationContent(List<Episode> episodeList) {
        String content;

        if (episodeList.size() == 1) {
            content = episodeList.get(0).getTitle();
        } else {
            StringBuilder episodeTitles = new StringBuilder();
            HashSet<String> channelTitles = new HashSet<>();

            for (int i = 0; i < episodeList.size(); i++) {

                if (episodeList.get(i).getChannelTitle() != null) {
                    channelTitles.add(episodeList.get(i).getChannelTitle());
                }
            }

            for (String title : channelTitles) {
                episodeTitles.append(title).append(", ");
            }
            content = episodeTitles.toString().substring(0, episodeTitles.length() - 2);
        }
        return content;
    }

    /**
     * Creates the notification to track episodes being downloaded
     * @param bytesDownloaded
     * @param totalBytes
     */
    public static void showDownloadStartedNotification(Context context,
                                                       Episode episode,
                                                       int queueSize,
                                                       int level,
                                                       int bytesDownloaded,
                                                       int totalBytes) {
        Notification.Builder builder = getBaseDownloadNotification(context,
                episode, queueSize, level);
        builder.setProgress(totalBytes, bytesDownloaded, false);
        Notification notification = builder.build();
        getNotificationManager(context).notify(NOTIFICATION_ID_DOWNLOADING, notification);
    }

    /**
     * Creates the notification the signifies the download mechanism queuing up
     */
    public static void showDownloadStartingNotification(Context context,
                                                        Episode episode,
                                                        int queueSize,
                                                        int level) {
        Notification.Builder builder = getBaseDownloadNotification(context, episode, queueSize, level);
        builder.setProgress(0, 0, true);
        Notification notification = builder.build();
        getNotificationManager(context).notify(NOTIFICATION_ID_DOWNLOADING, notification);
    }

    /**
     * Returns a base download notification
     * @param context
     * @param episode
     * @param queueSize
     * @param level
     * @return
     */
    public static Notification.Builder getBaseDownloadNotification(Context context,
                                                                   Episode episode,
                                                                   int queueSize,
                                                                   int level) {
        Notification.Builder builder = new Notification.Builder(context);
        Intent cancelIntent = new Intent(context, DownloadService.class);
        cancelIntent.setAction(DownloadService.ACTION_CANCEL_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, cancelIntent, 0);

        if (episode != null) {
            queueSize++;
        }

        if (queueSize == 1) {

            // single episode download, show the channel and episode title
            builder.setContentTitle(episode.getChannelTitle());
            builder.setContentText(context.getString(R.string.notification_downloading_episode,
                    episode.getTitle()));
            builder.addAction(R.drawable.ic_notification_action_cancel,
                    context.getString(R.string.notification_cancel_download), pendingIntent);
        } else {
            // numerous episodes to download, show number of downloads and pod cast channels
            builder.setContentTitle(context.getString(
                    R.string.notification_downloading_episodes, queueSize));
            builder.setContentText(context.getString(R.string.notification_downloading_episode,
                    episode.getTitle()));
            builder.addAction(R.drawable.ic_notification_action_cancel,
                    context.getString(R.string.notification_cancel_all_downloads), pendingIntent);
        }
        builder.setSmallIcon(R.drawable.ic_stat_download, level);
        builder.setPriority(Notification.PRIORITY_DEFAULT);
        builder.setOngoing(true);
        builder.setShowWhen(false);
        return builder;
    }

    /**
     * Shows a notification alerting the user that episodes have downloaded
     * @param context
     */
    public static void showEpisodesDownloadedNotification(Context context) {
        Notification.Builder builder = new Notification.Builder(context);

        // let's get episodes to notify
        Set<String> episodeServerIds = AppPrefHelper.getInstance(context)
                .getStringSet(AppPrefHelper.PROPERTY_DOWNLOAD_NOTIFICATIONS);

        if (episodeServerIds == null || episodeServerIds.size() == 0) {
            return;
        }
        List<Episode> episodeList = EpisodeModel.getEpisodesByServerId(context, episodeServerIds);

        if (episodeList.size() == 0) {
            return;
        } else if (episodeList.size() == 1) {
            builder.setContentTitle(context.getString(
                    R.string.notification_title_episode_downloaded));
        } else if (episodeList.size() > 1) {
            String title = String.format(
                    context.getString(R.string.notification_title_episodes_downloaded),
                    episodeList.size());
            builder.setContentTitle(title);
            builder.setStyle(buildEpisodesInboxStyle(context, title, episodeList));
        }
        builder.setContentText(Html.fromHtml(String.format("<b>%s</b> %s",
                episodeList.get(0).getChannelTitle(), episodeList.get(0).getTitle())));
        builder.setContentIntent(PendingIntentHelper.getOpenPremoActivityIntent(context));
        builder.setDeleteIntent(PendingIntentHelper.getNewDownloadsDeleteIntent(context));
        builder.setSmallIcon(R.drawable.ic_stat_download);
        builder.setPriority(Notification.PRIORITY_DEFAULT);
        builder.setOngoing(false);
        builder.setShowWhen(true);
        builder.setAutoCancel(true);
        getNotificationManager(context).notify(NOTIFICATION_ID_DOWNLOADED, builder.build());
    }
}
