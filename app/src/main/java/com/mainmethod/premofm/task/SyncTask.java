package com.mainmethod.premofm.task;

import android.content.Context;
import android.os.AsyncTask;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
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
public class SyncTask extends AsyncTask<List<String>, Integer, Void> {

    private final Context context;

    public SyncTask(Context context) {
        this.context = context;
    }

    @SafeVarargs
    @Override
    protected final Void doInBackground(List<String>... params) {
        List<String> channelGeneratedIds = params[0];

        for (int i = 0; i < channelGeneratedIds.size(); i++) {
            // get channel from the database
            Channel channel = ChannelModel.getChannelByGeneratedId(context, channelGeneratedIds.get(i));

            if (channel == null) {
                continue;
            }

            // get the xml data and process it into a Feed object
            try {

                if (!HttpHelper.hasInternetConnection(context)) {
                    continue;
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
                        showNewEpisodesNotification(newEpisodes);
                    }
                }

            } catch (HttpHelper.XmlDataException e) {
                Timber.e(e, "Error in doInBackground");
                channel.setLastSyncSuccessful(false);
            }
            // save the channel
            channel.setLastSyncSuccessful(true);
            channel.setLastSyncTime(DatetimeHelper.getTimestamp());

            // trigger the download service if applicable
            DownloadJobService.scheduleEpisodeDownloadNow(context);
        }

        return null;
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
