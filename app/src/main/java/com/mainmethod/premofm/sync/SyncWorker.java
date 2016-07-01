package com.mainmethod.premofm.sync;

import android.content.Context;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.NotificationHelper;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.http.HttpHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.parse.Feed;
import com.mainmethod.premofm.parse.FeedHandler;
import com.mainmethod.premofm.parse.SAXFeedHandler;
import com.mainmethod.premofm.service.job.DownloadJobService;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import timber.log.Timber;

/**
 * Created by evanhalley on 6/14/16.
 */
public class SyncWorker implements Runnable {

    private final Context context;
    private final Channel channel;
    private final boolean doNotify;

    public SyncWorker(Context context, Channel channel, boolean doNotify) {
        this.context = context;
        this.channel = channel;
        this.doNotify = doNotify;
    }

    @Override
    public void run() {

        try {
            processChannel();
        } catch (Exception e) {
            Timber.e(e, "Error in run");
        }
    }

    private void processChannel() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Timber.d("Processing channel %s", channel.getFeedUrl());

        // get the xml data and process it into a Feed object 1116303051
        try {

            if (!HttpHelper.hasInternetConnection(context)) {
                return;
            }
            String xmlData = HttpHelper.getXmlData(channel, false);

            if (xmlData != null) {
                FeedHandler feedHandler = new SAXFeedHandler(channel);
                Feed feed = feedHandler.processXml(xmlData);

                if (feed != null && feed.getEpisodeList() != null && feed.getEpisodeList().size() > 0) {
                    // determine what's new or updated
                    List<Episode> newEpisodes = EpisodeModel.getNewEpisodesFromFeed(context, feed);

                    // save the episodes
                    EpisodeModel.bulksInsertEpisodes(context, newEpisodes);

                    // handle new episode notifications
                    if (doNotify) {
                        showNewEpisodesNotification(newEpisodes);
                    }
                }
            }
        } catch (HttpHelper.XmlDataException e) {
            Timber.e(e, "Error in doInBackground");
            channel.setLastSyncSuccessful(false);
        }
        // save the channel
        channel.setLastSyncSuccessful(true);
        channel.setLastSyncTime(DatetimeHelper.getTimestamp());

        if (channel.getId() == -1) {
            int id = ChannelModel.insertChannel(context, channel);

            if (id != -1) {
                UserPrefHelper.get(context).addGeneratedId(R.string.pref_key_notification_channels, channel.getGeneratedId());
                BroadcastHelper.broadcastPodcastProcessed(context, channel, true);
            } else {
                BroadcastHelper.broadcastPodcastProcessed(context, channel, false);
            }
        } else {
            ChannelModel.updateChannel(context, channel);
        }

        // trigger the download service if applicable
        if (doNotify) {
            DownloadJobService.scheduleEpisodeDownloadNow(context);
        }
    }

    private void showNewEpisodesNotification(List<Episode> newEpisodes) {

        if (UserPrefHelper.get(context).getBoolean(R.string.pref_key_enable_notifications)) {
            // add new episodes to the episode server id set
            Set<String> episodeServerIds = new TreeSet<>();
            String channelsToNotifyServerIds = UserPrefHelper.get(context).getString(
                    R.string.pref_key_notification_channels);

            for (int i = 0; i < newEpisodes.size(); i++) {

                if (channelsToNotifyServerIds.contains(newEpisodes.get(i).getChannelGeneratedId())) {
                    episodeServerIds.add(newEpisodes.get(i).getGeneratedId());
                }
            }
            // add them to the preferences
            AppPrefHelper.getInstance(context).addToStringSet(
                    AppPrefHelper.PROPERTY_EPISODE_NOTIFICATIONS, episodeServerIds);
            // show the notification
            NotificationHelper.showNewEpisodeNotification(context);
        }
    }
}