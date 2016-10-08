/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.LoadCallback;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.receiver.ClearNotificationsReceiver;

/**
 * Convenience functions for sending intents
 * Created by evan on 2/25/15.
 */
public class IntentHelper {

    public static final int REQUEST_CODE_OPEN_OPML_FILE = 1234;
    public static final int REQUEST_CODE_SAVE_OPML_FILE = 4321;

    private static void sendIntent(Context context, Intent intent) {

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    public static Intent getClearEpisodeNotificationsIntent(Context context) {
        return new Intent(context, ClearNotificationsReceiver.class)
                .setAction(ClearNotificationsReceiver.CLEAR_EPISODE_NOTIFICATIONS);
    }

    public static Intent getClearDownloadNotificationsIntent(Context context) {
        return new Intent(context, ClearNotificationsReceiver.class)
                .setAction(ClearNotificationsReceiver.CLEAR_DOWNLOAD_NOTIFICATIONS);
    }

    /**
     * Sends an email to hello@mainmethod.co with an optional attachment
     * @param context
     * @param pathToDebugAttachment
     */
    public static void sendSupportEmail(Context context, String pathToDebugAttachment) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]
                { context.getString(R.string.feedback_email) });
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_support_subject));
        intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.feedback_support_message));

        if (pathToDebugAttachment != null && pathToDebugAttachment.length() > 0) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(pathToDebugAttachment));
        }
        sendIntent(context, intent);
    }

    /**
     * Sends a suggestion or idea in an email
     * @param context
     */
    public static void sendIdeaEmail(Context context) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]
                { context.getString(R.string.feedback_email) });
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_idea_subject));
        intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.feedback_idea_message));
        sendIntent(context, intent);
    }

    /**
     * Opens the app's Play Store listing
     * @param context
     */
    public static void openAppListing(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                context.getString(R.string.app_list_url)));
        sendIntent(context, intent);
    }

    /**
     * Opens the specified URL in a browser
     * @param context
     * @param url
     */
    public static void openBrowser(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        sendIntent(context, intent);
    }

    /**
     * Shares a channel to the Android app of choice
     * @param context
     * @param channel
     */
    public static void shareChannel(Context context, Channel channel) {
        String shareText = String.format(
                context.getString(R.string.share_channel_text), channel.getTitle(),
                channel.getSiteUrl());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        Intent chooser = Intent.createChooser(intent, context.getString(R.string.share_with));

        // Verify the intent will resolve to at least one activity
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(chooser);
        }
    }

    /**
     * Shares an episode to the Android app of choice
     * @param context
     * @param episode
     */
    public static void shareEpisode(Context context, Episode episode) {
        ImageLoadHelper.saveImage(context, episode.getArtworkUrl(),
                uri -> shareEpisode(context, episode, uri));
    }

    private static void shareEpisode(Context context, Episode episode, Uri bitmapUri) {
        String channelTitle = episode.getChannelTitle();

        String shareUrl = episode.getUrl() != null ? episode.getUrl() : episode.getRemoteMediaUrl();
        int messageResId = episode.getEpisodeStatus() == EpisodeStatus.IN_PROGRESS ?
                R.string.share_listening_episode_text : R.string.share_episode_text;

        String shareText = String.format(
                context.getString(messageResId),
                episode.getTitle(),
                channelTitle,
                shareUrl);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);

        if (bitmapUri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
        }
        Intent chooser = Intent.createChooser(intent, context.getString(R.string.share_with));

        // Verify the intent will resolve to at least one activity
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(chooser);
        }
    }

    public static void openOpmlFileExporter(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, "PremoFM_export.opml");
        fragment.startActivityForResult(intent, REQUEST_CODE_SAVE_OPML_FILE);
    }

    /**
     * Opens a file chooser
     * @param fragment
     */
    public static void openOpmlFileChooser(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        fragment.startActivityForResult(intent, REQUEST_CODE_OPEN_OPML_FILE);
    }
}
