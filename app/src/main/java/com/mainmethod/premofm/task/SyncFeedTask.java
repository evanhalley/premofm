package com.mainmethod.premofm.task;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import timber.log.Timber;

/**
 * Created by evan on 6/10/16.
 */
public class SyncFeedTask extends AsyncTask<Bundle, Integer, Void> {

    public static final String PARAM_MODE = "mode";
    public static final String PARAM_FEED_URL = "feedUrl";
    public static final String PARAM_GENERATED_IDS = "generatedIds";
    public static final int MODE_ADD_FEED = 0;
    public static final int MODE_SYNC_FEED = 1;
    private final Context context;

    public SyncFeedTask(Context context) {
        this.context = context;
    }

    @Override
    protected final Void doInBackground(Bundle... bundles) {
        Bundle params = bundles[0];

        switch (params.getInt(PARAM_MODE)) {
            case MODE_ADD_FEED:
                String feedUrl = params.getString(PARAM_FEED_URL);
                Channel channel = new Channel();
                channel.setFeedUrl(feedUrl);
                processChannel(channel, false);
                break;
            case MODE_SYNC_FEED:
                syncFeed(params.getStringArrayList(PARAM_GENERATED_IDS));
                break;
        }

        return null;
    }

    private void syncFeed(List<String> generatedIds) {
        for (int i = 0; i < generatedIds.size(); i++) {
            // get channel from the database
            Channel channel = ChannelModel.getChannelByGeneratedId(context, generatedIds.get(i));

            if (channel == null) {
                continue;
            }
            processChannel(channel, true);
        }
    }

    private void processChannel(Channel channel, boolean doNotify) {

        // get the xml data and process it into a Feed object
        try {

            if (!HttpHelper.hasInternetConnection(context)) {
                return;
            }
            String xmlData = HttpHelper.getXmlData(channel);

            if (xmlData != null) {
                FeedHandler feedHandler = new SAXFeedHandler();
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
            BroadcastHelper.broadcastChannelAdded(context, id);
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
