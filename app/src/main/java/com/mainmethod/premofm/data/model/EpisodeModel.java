/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.data.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.DatabaseOpenHelper;
import com.mainmethod.premofm.data.LoadListCallback;
import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Collection;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.object.Filter;
import com.mainmethod.premofm.service.DeleteEpisodeService;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Manages episode based interaction with the database as well as convenience functinos.
 * Created by evan on 4/14/15.
 */
public class EpisodeModel {

    private static final String TAG = EpisodeModel.class.getSimpleName();
    public static final String PARAM_EPISODE_STATUS     = "episodeStatus";
    public static final String PARAM_EPISODE_PROGRESS   = "episodeProgress";
    public static final String PARAM_EPISODE_DURATION   = "episodeDuration";
    public static final String PARAM_DOWNLOAD_STATUS    = "downloadStatus";
    public static final String PARAM_MANUAL_DOWNLOAD    = "manualDownload";
    public static final String PARAM_TOGGLE_FAVORITE    = "toggleFavorite";
    public static final String PARAM_SIZE               = "size";
    public static final String PARAM_LOCAL_MEDIA_URL    = "localMediaUrl";
    public static final String PARAM_DOWNLOADED_SIZE    = "downloadedSize";
    public static final String PARAM_MANUALLY_ADDED     = "manuallyAdded";

    public static final int UPDATE = 0;
    public static final int ADD    = 1;

    private static final String CHANNEL_EPISODE_QUERY =
            PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID + " = ?";

    private static final String DOWNLOAD_QUEUE_EPISODE_QUERY =
            PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + " = " + DownloadStatus.QUEUED;

    private static final String DOWNLOAD_QUEUE_EPISODE_SORT =
            PremoContract.EpisodeEntry.MANUAL_DOWNLOAD + " DESC";

    private static final String EPISODE_ID_QUERY = PremoContract.EpisodeEntry._ID + " = ?";

    private static final String EPISODE_SERVER_ID_QUERY =
            PremoContract.EpisodeEntry.GENERATED_ID + " = ?";

    private static final String EPISODE_SERVER_ID_MANUAL_DOWNLOAD_QUERY =
            PremoContract.EpisodeEntry.GENERATED_ID + " = ? " +
                    "AND " + PremoContract.EpisodeEntry.CHANNEL_IS_SUBSCRIBED + " = 0 " +
                    "AND " + PremoContract.EpisodeEntry.MANUALLY_ADDED + " = 1 ";

    private static final String EPISODE_IS_SUBSCRIBED_OR_MANUALLY_ADDED_QUERY =
                    PremoContract.EpisodeEntry.CHANNEL_IS_SUBSCRIBED + " = 1 " +
                    " OR " + PremoContract.EpisodeEntry.MANUALLY_ADDED + " = 1 ";

    public static final String DEFAULT_EPISODE_SORT =
            PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS + " DESC";

    private static final String SUBSCRIBED_CHANNELS_EPISODE_QUERY =
            PremoContract.EpisodeEntry.CHANNEL_IS_SUBSCRIBED + " = 1 ";

    private static final String REQUESTED_EPISODE_QUERY =
            PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + " = '" + DownloadStatus.REQUESTED +
                    "' AND " + PremoContract.EpisodeEntry.MANUAL_DOWNLOAD + " = '1'";

    /**
     * Returns a cursor for use on the home fragment
     * @param context
     * @param filter
     * @return
     */
    public static Loader<Cursor> getHomeCursorLoader(Context context, Filter filter) {
        String query = FilterModel.buildFilterQuery(context, filter);
        String defaultSort = DEFAULT_EPISODE_SORT;

        if (filter.getCollectionId() > -1) {
            String sort = FilterModel.buildFilterCollectionSort(context, filter);

            if (sort != null) {
                defaultSort = sort;
            }
        }
        // filter out episodes from the home screen who are from channels that aren't subscribed
        if (!filter.isEpisodesManuallyAdded()) {

            if (query.length() > 0) {
                query += " AND ";
            }
            query += SUBSCRIBED_CHANNELS_EPISODE_QUERY;
        }

        Uri episodeUri;
        if (filter.getEpisodesPerChannel() > -1) {
            episodeUri = PremoContract.EpisodeEntry.GROUPED_CONTENT_URI.buildUpon()
                    .appendEncodedPath(String.valueOf(filter.getEpisodesPerChannel())).build();
        } else {
            episodeUri = PremoContract.EpisodeEntry.CONTENT_URI;
        }

        return new CursorLoader(context,
                episodeUri,
                null,
                query,
                null,
                defaultSort);
    }

    /**
     * Returns a cursor to be used by the channel profile
     * @param context
     * @param channel
     * @return
     */
    public static Loader<Cursor> getChannelProfileCursorLoader(Context context, Channel channel) {
        return new CursorLoader(context,
                PremoContract.EpisodeEntry.CONTENT_URI,
                null,
                CHANNEL_EPISODE_QUERY,
                new String[]{ channel.getGeneratedId() },
                DEFAULT_EPISODE_SORT);
    }

    /**
     * Returns episodes that were requested for download
     * @param context
     * @return
     */
    public static List<Episode> getRequestedEpisodes(Context context) {
        List<Episode> episodes = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(
                    PremoContract.EpisodeEntry.CONTENT_URI,
                    null,
                    REQUESTED_EPISODE_QUERY,
                    null,
                    DOWNLOAD_QUEUE_EPISODE_SORT);

            while (cursor != null && cursor.moveToNext()) {
                episodes.add(toEpisode(cursor));
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return episodes;
    }

    public static LinkedBlockingDeque<Episode> getDownloadQueueEpisodes(Context context) {
        LinkedBlockingDeque<Episode> queue = new LinkedBlockingDeque<>();
        Cursor cursor = null;
        ContentResolver resolver = context.getContentResolver();

        try {
            cursor = resolver.query(PremoContract.EpisodeEntry.CONTENT_URI, null,
                    DOWNLOAD_QUEUE_EPISODE_QUERY,
                    null,
                    DOWNLOAD_QUEUE_EPISODE_SORT);

            if (cursor != null) {

                while (cursor.moveToNext()) {
                    Episode episode = EpisodeModel.toEpisode(cursor);

                    if (episode.isManualDownload()) {
                        queue.addFirst(episode);
                    } else {
                        queue.add(episode);
                    }
                }
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return queue;
    }

    public static void manuallyAddEpisode(Context context, int episodeId) {
        Bundle params = new Bundle();
        params.putBoolean(EpisodeModel.PARAM_MANUALLY_ADDED, true);
        EpisodeModel.updateEpisodeAsync(context, episodeId, params);
    }

    public static void updateEpisodeAsync(final Context context, final int episodeId,
                                          final Bundle parameters) {
        updateEpisodeAsync(context, episodeId, parameters, null);
    }

    public static void updateEpisodeAsync(final Context context, final int episodeId,
                                          final Bundle parameters,
                                          final EpisodeUpdateFinishedListener listener) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                updateEpisode(context, episodeId, parameters);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                if (listener != null) {
                    listener.onUpdateFinished();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static boolean updateEpisode(Context context, int episodeId, Bundle parameters) {
        ContentValues record = new ContentValues();

        if (parameters.containsKey(PARAM_EPISODE_PROGRESS)) {
            record.put(PremoContract.EpisodeEntry.PROGRESS,
                    parameters.getLong(PARAM_EPISODE_PROGRESS));
        }

        if (parameters.containsKey(PARAM_EPISODE_DURATION)) {
            record.put(PremoContract.EpisodeEntry.DURATION,
                    parameters.getLong(PARAM_EPISODE_DURATION));
        }

        if (parameters.containsKey(PARAM_EPISODE_STATUS)) {
            record.put(PremoContract.EpisodeEntry.EPISODE_STATUS_ID,
                    parameters.getInt(PARAM_EPISODE_STATUS));
        }

        if (parameters.containsKey(PARAM_DOWNLOAD_STATUS)) {
            record.put(PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID,
                    parameters.getInt(PARAM_DOWNLOAD_STATUS));
        }

        if (parameters.containsKey(PARAM_MANUAL_DOWNLOAD)) {
            record.put(PremoContract.EpisodeEntry.MANUAL_DOWNLOAD,
                    parameters.getInt(PARAM_MANUAL_DOWNLOAD));
        }

        if (parameters.containsKey(PARAM_TOGGLE_FAVORITE)) {
            record.put(PremoContract.EpisodeEntry.FAVORITE,
                    parameters.getInt(PARAM_TOGGLE_FAVORITE));
        }

        if (parameters.containsKey(PARAM_LOCAL_MEDIA_URL)) {
            record.put(PremoContract.EpisodeEntry.LOCAL_MEDIA_URL,
                    parameters.getString(PARAM_LOCAL_MEDIA_URL));
        }

        if (parameters.containsKey(PARAM_DOWNLOADED_SIZE)) {
            record.put(PremoContract.EpisodeEntry.DOWNLOADED_SIZE,
                    parameters.getInt(PARAM_DOWNLOADED_SIZE));
        }

        if (parameters.containsKey(PARAM_SIZE)) {
            record.put(PremoContract.EpisodeEntry.SIZE,
                    parameters.getInt(PARAM_SIZE));
        }

        if (parameters.containsKey(PARAM_MANUALLY_ADDED)) {
            record.put(PremoContract.EpisodeEntry.MANUALLY_ADDED,
                    parameters.getBoolean(PARAM_MANUALLY_ADDED) ? 1 : 0);
        }

        int rowsUpdated = context.getContentResolver().update(
                PremoContract.EpisodeEntry.CONTENT_URI,
                record,
                EPISODE_ID_QUERY,
                new String[]{String.valueOf(episodeId)});
        return rowsUpdated == 1;
    }

    /**
     * Converts an Episode into a database record
     * @return record
     */
    public static ContentValues fromEpisode(Episode episode) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.EpisodeEntry.GENERATED_ID, episode.getGeneratedId());
        record.put(PremoContract.EpisodeEntry.TITLE, episode.getTitle());
        record.put(PremoContract.EpisodeEntry.DESCRIPTION, episode.getDescription());
        record.put(PremoContract.EpisodeEntry.DESCRIPTION_HTML, episode.getDescriptionHtml());
        record.put(PremoContract.EpisodeEntry.FAVORITE, episode.isFavorite() ? 1 : 0);
        record.put(PremoContract.EpisodeEntry.PUBLISHED_AT, DatetimeHelper.dateToString(
                episode.getPublishedAt()));
        record.put(PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS, episode.getPublishedAt().getTime());
        record.put(PremoContract.EpisodeEntry.DURATION, episode.getDuration());
        record.put(PremoContract.EpisodeEntry.PROGRESS, episode.getProgress());
        record.put(PremoContract.EpisodeEntry.URL, episode.getUrl());
        record.put(PremoContract.EpisodeEntry.REMOTE_MEDIA_URL, episode.getRemoteMediaUrl());
        record.put(PremoContract.EpisodeEntry.LOCAL_MEDIA_URL, episode.getLocalMediaUrl());
        record.put(PremoContract.EpisodeEntry.SIZE, episode.getSize());
        record.put(PremoContract.EpisodeEntry.DOWNLOADED_SIZE, episode.getDownloadedSize());
        record.put(PremoContract.EpisodeEntry.MIME_TYPE, episode.getMimeType());
        record.put(PremoContract.EpisodeEntry.EPISODE_STATUS_ID, episode.getEpisodeStatus());
        record.put(PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID, episode.getDownloadStatus());
        record.put(PremoContract.EpisodeEntry.MANUAL_DOWNLOAD, episode.isManualDownload() ? 1 : 0);
        record.put(PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID, episode.getChannelGeneratedId());
        record.put(PremoContract.EpisodeEntry.CHANNEL_TITLE, episode.getChannelTitle());
        record.put(PremoContract.EpisodeEntry.CHANNEL_AUTHOR, episode.getChannelAuthor());
        record.put(PremoContract.EpisodeEntry.CHANNEL_ARTWORK_URL, episode.getChannelArtworkUrl());
        record.put(PremoContract.EpisodeEntry.CHANNEL_IS_SUBSCRIBED, episode.isChannelSubscribed() ? 1 : 0);
        record.put(PremoContract.EpisodeEntry.MANUALLY_ADDED, episode.isManuallyAdded() ? 1 : 0);
        return record;
    }

    /**
     * Converts the record referenced by the cursor to an Episode
     * @param cursor Cursor
     * @return Episode
     */
    public static Episode toEpisode(Cursor cursor) {

        if (cursor == null || cursor.isClosed()) {
            throw new IllegalArgumentException("Cannot process null or closed cursor");
        }
        Episode episode = new Episode();
        episode.setId(cursor.getInt(cursor.getColumnIndex(PremoContract.EpisodeEntry._ID)));
        episode.setChannelGeneratedId(cursor.getString(cursor.getColumnIndex(
                PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID)));
        episode.setGeneratedId(cursor.getString(cursor.getColumnIndex(PremoContract.EpisodeEntry.GENERATED_ID)));
        episode.setTitle(cursor.getString(cursor.getColumnIndex(PremoContract.EpisodeEntry.TITLE)));
        episode.setDescription(cursor.getString(cursor.getColumnIndex(PremoContract.EpisodeEntry.DESCRIPTION)), false);
        episode.setDescriptionHtml(cursor.getString(cursor.getColumnIndex(PremoContract.EpisodeEntry.DESCRIPTION_HTML)));
        episode.setDuration(cursor.getLong(cursor.getColumnIndex(PremoContract.EpisodeEntry.DURATION)));
        episode.setProgress(cursor.getLong(cursor.getColumnIndex(PremoContract.EpisodeEntry.PROGRESS)));
        episode.setUrl(cursor.getString(cursor.getColumnIndex(PremoContract.EpisodeEntry.URL)));
        episode.setRemoteMediaUrl(cursor.getString(cursor.getColumnIndex(PremoContract.EpisodeEntry.REMOTE_MEDIA_URL)));
        episode.setLocalMediaUrl(cursor.getString(cursor.getColumnIndex(PremoContract.EpisodeEntry.LOCAL_MEDIA_URL)));
        episode.setSize(cursor.getInt(cursor.getColumnIndex(PremoContract.EpisodeEntry.SIZE)));
        episode.setDownloadedSize(cursor.getInt(cursor.getColumnIndex(PremoContract.EpisodeEntry.DOWNLOADED_SIZE)));
        episode.setMimeType(cursor.getString(cursor.getColumnIndex(PremoContract.EpisodeEntry.MIME_TYPE)));
        episode.setEpisodeStatus(
                cursor.getInt(cursor.getColumnIndex(PremoContract.EpisodeEntry.EPISODE_STATUS_ID)));
        episode.setDownloadStatus(
                cursor.getInt(cursor.getColumnIndex(PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID)));
        episode.setManualDownload(cursor.getInt(
                cursor.getColumnIndex(PremoContract.EpisodeEntry.MANUAL_DOWNLOAD)) == 1);
        episode.setFavorite(cursor.getInt(
                cursor.getColumnIndex(PremoContract.EpisodeEntry.FAVORITE)) == 1);
        episode.setChannelTitle(cursor.getString(
                cursor.getColumnIndex(PremoContract.EpisodeEntry.CHANNEL_TITLE)));
        episode.setChannelAuthor(cursor.getString
                (cursor.getColumnIndex(PremoContract.EpisodeEntry.CHANNEL_AUTHOR)));
        episode.setChannelArtworkUrl(cursor.getString(
                cursor.getColumnIndex(PremoContract.EpisodeEntry.CHANNEL_ARTWORK_URL)));
        episode.setChannelIsSubscribed(cursor.getInt(
                cursor.getColumnIndex(PremoContract.EpisodeEntry.CHANNEL_IS_SUBSCRIBED)) == 1);
        episode.setManuallyAdded(cursor.getInt(
                cursor.getColumnIndex(PremoContract.EpisodeEntry.MANUALLY_ADDED)) == 1);

        try {
            episode.setPublishedAt(DatetimeHelper.stringToDate(cursor.getString(cursor.getColumnIndex(
                    PremoContract.EpisodeEntry.PUBLISHED_AT))));
        } catch (ParseException e) {
            episode.setPublishedAt(new Date());
        }
        return episode;
    }

    /**
     * Returns an Episode item, populating only the items needed for syncing
     * @param cursor
     * @return
     */
    public static Episode toEpisodeForSync(Cursor cursor) {

        if (cursor == null || cursor.isClosed()) {
            throw new IllegalArgumentException("Cannot process null or closed cursor");
        }
        Episode episode = new Episode();
        episode.setChannelGeneratedId(cursor.getString(cursor.getColumnIndex(
                PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID)));
        episode.setGeneratedId(cursor.getString(cursor.getColumnIndex(
                PremoContract.EpisodeEntry.GENERATED_ID)));
        episode.setProgress(cursor.getLong(cursor.getColumnIndex(
                PremoContract.EpisodeEntry.PROGRESS)));
        episode.setEpisodeStatus(cursor.getInt(cursor.getColumnIndex(
                PremoContract.EpisodeEntry.EPISODE_STATUS_ID)));
        episode.setFavorite(cursor.getInt(
                cursor.getColumnIndex(PremoContract.EpisodeEntry.FAVORITE)) == 1);
        return episode;
    }

    public static Episode getEpisodeById(Context context, int episodeId) {
        return getEpisode(context, PremoContract.EpisodeEntry._ID + " = " + episodeId);
    }

    public static List<Episode> getEpisodesByChannel(Context context, Channel channel) {
        return getEpisodes(context, PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID + " = ?",
                new String[] { channel.getGeneratedId() },
                PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS + " DESC");
    }

    public static Episode getEpisodeByServerId(Context context, String episodeServerId) {
        return getEpisode(context, PremoContract.EpisodeEntry.GENERATED_ID + " = '" + episodeServerId + "'");
    }

    public static List<Episode> getEpisodesByServerId(Context context, Set<String> episodeServerId) {
        String[] episodeServerIdsArr = new String[episodeServerId.size()];
        return getEpisodes(context,
                PremoContract.EpisodeEntry.GENERATED_ID +
                        " IN (" + TextHelper.joinStrings(episodeServerId.toArray(episodeServerIdsArr), true) + ")", null);
    }

    public static Episode getLatestEpisodeByChannelServerId(Context context, String channelServerId) {
        List<Episode> episodes = getEpisodes(context,
                PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID +
                        " = '" + channelServerId + "'",
                PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS + " DESC LIMIT 1");

        if (episodes == null || episodes.size() == 0) {
            return null;
        } else {
            return episodes.get(0);
        }
    }

    public static List<Episode> getEpisodesByCollection(Context context, Collection collection) {
        List<Episode> episodes = new ArrayList<>(collection.getCollectedServerIds().size());
        StringBuilder inClause = new StringBuilder(512);

        if (collection.getType() == Collection.COLLECTION_TYPE_EPISODE) {

            for (int i = 0; i < collection.getCollectedServerIds().size(); i++) {
                inClause.append("'").append(collection.getCollectedServerIds().get(i)).append("'");

                if (i < collection.getCollectedServerIds().size() - 1) {
                    inClause.append(",");
                }
            }

            List<Episode> collectionEpisodes = getEpisodes(context,
                    new StringBuilder(64)
                            .append(PremoContract.EpisodeEntry.GENERATED_ID)
                            .append(" IN (")
                            .append(inClause.toString())
                            .append(")").toString(),
                    null,
                    null);

            // order the episodes in the order of the server IDs in the collection
            for (int i = 0; i < collection.getCollectedServerIds().size(); i++) {

                for (int j = 0; j < collectionEpisodes.size(); j++) {

                    if (collection.getCollectedServerIds().get(i).contentEquals(
                            collectionEpisodes.get(j).getGeneratedId())) {
                        episodes.add(collectionEpisodes.get(j));
                        break;
                    }
                }
            }
        }

        return episodes;
    }

    public static void getEpisodeByCollectionAsync(final Context context,
                                                   final Collection collection,
                                                   final LoadListCallback<Episode> listener) {
        new AsyncTask<Void, Void, List<Episode>>() {
            @Override
            protected List<Episode> doInBackground(Void... params) {
                return getEpisodesByCollection(context, collection);
            }

            @Override
            protected void onPostExecute(List<Episode> episodes) {
                listener.onListLoaded(episodes);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static List<Episode> searchForEpisodes(Context context, String query) {
        Cursor cursor = null;
        ContentResolver contentResolver = context.getContentResolver();
        List<Episode> episodeList = new ArrayList<>();

        String selection = PremoContract.EpisodeEntry.TITLE + " LIKE '%" + query + "%' AND " +
                EPISODE_IS_SUBSCRIBED_OR_MANUALLY_ADDED_QUERY;
        String sortOrder = PremoContract.EpisodeEntry.TITLE + " ASC LIMIT 10";

        try {
            cursor = contentResolver.query(PremoContract.EpisodeEntry.CONTENT_URI, null, selection,
                    null, sortOrder);

            while (cursor != null && cursor.moveToNext()) {
                episodeList.add(EpisodeModel.toEpisode(cursor));
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return episodeList;
    }

    public static void searchForEpisodesAsync(final Context context,
                                              final String query,
                                              final LoadListCallback<Episode> listener) {
        new AsyncTask<Void, Void, List<Episode>>() {

            @Override
            protected List<Episode> doInBackground(Void... params) {
                return searchForEpisodes(context, query);
            }

            @Override
            protected void onPostExecute(List<Episode> episodeList) {

                if (listener != null) {
                    listener.onListLoaded(episodeList);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Helper function to send a query to the DB and get a list of episodes
     * @param context
     * @param sortOrder
     * @return
     */
    public static List<Episode> getEpisodes(Context context, String selection,
                                            String[] selectionArgs,  String sortOrder) {
        Cursor cursor = null;
        ContentResolver contentResolver = context.getContentResolver();
        List<Episode> episodeList = null;

        try {
            cursor = contentResolver.query(PremoContract.EpisodeEntry.CONTENT_URI,
                    null, selection, selectionArgs, sortOrder);
            episodeList = new ArrayList<>();

            while (cursor != null && cursor.moveToNext()) {
                episodeList.add(EpisodeModel.toEpisode(cursor));
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return episodeList;
    }

    /**
     * Helper function to send a query to the DB and get a list of episodes
     * @param context
     * @param query
     * @param sortOrder
     * @return
     */
    public static List<Episode> getEpisodes(Context context, String query, String sortOrder) {
        Cursor cursor = null;
        ContentResolver contentResolver = context.getContentResolver();
        List<Episode> episodeList = null;

        try {
            cursor = contentResolver.query(PremoContract.EpisodeEntry.CONTENT_URI, null, query, null, sortOrder);
            episodeList = new ArrayList<>();

            while (cursor != null && cursor.moveToNext()) {
                episodeList.add(EpisodeModel.toEpisode(cursor));
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return episodeList;
    }

    /**
     * Helper function to send a query to the DB and get an episode
     * @param context
     * @param query
     * @return
     */
    public static Episode getEpisode(Context context, String query) {
        Cursor cursor = null;
        Episode episode = null;

        try {
            cursor = context.getContentResolver().query(PremoContract.EpisodeEntry.CONTENT_URI, null, query,
                    null, null);

            if (cursor != null && cursor.moveToFirst()) {
                episode = EpisodeModel.toEpisode(cursor);
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return episode;
    }

    /**
     * Inserts a single episode
     * @param context
     * @param episode
     * @return
     */
    public static boolean insertEpisode(Context context, Episode episode) {
        ContentValues values = EpisodeModel.fromEpisode(episode);
        Uri uri = context.getContentResolver().insert(PremoContract.EpisodeEntry.CONTENT_URI, values);
        return uri != null;
    }

    public static void insertEpisodes(Context context, Channel channel, List<Episode> episodeList)
            throws RemoteException, OperationApplicationException {

        if (episodeList != null && episodeList.size() > 0) {
            ContentResolver contentResolver = context.getContentResolver();
            ArrayList<ContentProviderOperation> operations = new ArrayList<>(episodeList.size());

            for (int i = 0; i < episodeList.size(); i++) {

                if (!doesEpisodeExist(context, episodeList.get(i).getGeneratedId())) {

                    if (channel != null) {
                        episodeList.get(i).setChannelTitle(channel.getTitle());
                        episodeList.get(i).setChannelAuthor(channel.getAuthor());
                        episodeList.get(i).setChannelArtworkUrl(channel.getArtworkUrl());
                    }
                    operations.add(ContentProviderOperation.newInsert(
                            PremoContract.EpisodeEntry.CONTENT_URI).withValues(
                            EpisodeModel.fromEpisode(episodeList.get(i))).build());
                }
            }

            if (operations.size() > 0) {
                contentResolver.applyBatch(PremoContract.CONTENT_AUTHORITY, operations);
            }
        }
    }

    public static long getNumberOfEpisodesForChannel(Context context, String channelServerId) {
        SQLiteDatabase db = null;
        long count = -1;

        try {
            db = new DatabaseOpenHelper(context).getReadableDatabase();
            count = DatabaseUtils.queryNumEntries(db,
                    PremoContract.EpisodeEntry.TABLE_NAME,
                    CHANNEL_EPISODE_QUERY,
                    new String[]{channelServerId});
        } finally {
            ResourceHelper.closeResource(db);
        }
        return count;
    }

    /**
     * Marks episodes as their channel is unsubscribed
     * @param context
     * @param channelServerId
     */
    public static void markEpisodesAsChannelUnsubscribed(Context context, String channelServerId) {
        ContentValues values = new ContentValues();
        values.put(PremoContract.EpisodeEntry.CHANNEL_IS_SUBSCRIBED, 0);
        context.getContentResolver().update(
                PremoContract.EpisodeEntry.CONTENT_URI,
                values,
                PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID + " = ? ",
                new String[]{channelServerId});
    }

    /**
     * Marks episodes as their channel is subscribed
     * @param context
     * @param channelServerId
     */
    public static void markEpisodesAsChannelSubscribed(Context context, String channelServerId) {
        ContentValues values = new ContentValues();
        values.put(PremoContract.EpisodeEntry.CHANNEL_IS_SUBSCRIBED, 1);
        context.getContentResolver().update(
                PremoContract.EpisodeEntry.CONTENT_URI,
                values,
                PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID + " = ? ",
                new String[]{ channelServerId} );
    }

    /**
     * Returns episodes that aren't in the episodeList but are in the database
     * @param context
     * @param channel
     * @param newEpisodeList
     * @return
     */
    public static List<Episode> returnNewEpisodes(Context context, Channel channel,
                                                  List<Episode> newEpisodeList) {
        List<Episode> newEpisodes = new ArrayList<>();

        // get existing episodes
        List<Episode> existingEpisodeList = getEpisodesByChannel(context, channel);
        Map<String, Episode> existingEpisodeMap = new ArrayMap<>(existingEpisodeList.size());

        // add existing episode list to map for easy lookup
        for (int i = 0; i < existingEpisodeList.size(); i++) {
            Episode episode = existingEpisodeList.get(i);
            existingEpisodeMap.put(episode.getGeneratedId(), episode);
        }

        // add episode to new list if it's not in the existing episodes list
        for (int i = 0; i < newEpisodeList.size(); i++) {
            Episode episode = newEpisodeList.get(i);

            if (!existingEpisodeMap.containsKey(episode.getGeneratedId())) {
                newEpisodes.add(episode);
            }
        }
        return newEpisodes;
    }

    /**
     * Helper function to send a query to the DB and get a list of episodes
     * @param context
     * @return
     */
    public static List<Episode> getAllEpisodes(Context context) {
        Cursor cursor = null;
        ContentResolver contentResolver = context.getContentResolver();
        List<Episode> episodeList = null;

        try {
            cursor = contentResolver.query(PremoContract.EpisodeEntry.CONTENT_URI, null, null, null,
                    null);
            episodeList = new ArrayList<>();

            while (cursor != null && cursor.moveToNext()) {
                episodeList.add(EpisodeModel.toEpisode(cursor));
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return episodeList;
    }

    /**
     * Executes channel operations against the local database
     * @param context
     * @param operationType
     * @param episodeList
     * @throws android.os.RemoteException
     * @throws android.content.OperationApplicationException
     */
    public static void storeEpisodes(Context context, int operationType, List<Episode> episodeList)
            throws RemoteException, OperationApplicationException {

        if (episodeList != null && episodeList.size() > 0) {
            ContentResolver contentResolver = context.getContentResolver();
            ArrayList<ContentProviderOperation> operations = new ArrayList<>(episodeList.size());
            Map<String, Channel> channelMap = ChannelModel.getChannelMap(context);

            for (Episode episode : episodeList) {
                // Join some channel metadata to the episode for convenience sake (and because I HATE joins)
                Channel channel = channelMap.get(episode.getChannelGeneratedId());

                if (channel == null) {
                    continue;
                }
                episode.setChannelTitle(channel.getTitle());
                episode.setChannelAuthor(channel.getAuthor());
                episode.setChannelArtworkUrl(channel.getArtworkUrl());
                ContentProviderOperation operation = null;

                switch (operationType) {
                    case UPDATE:
                        operation = ContentProviderOperation.newUpdate(PremoContract.EpisodeEntry.CONTENT_URI)
                                .withValues(EpisodeModel.fromEpisode(episode))
                                .withSelection(PremoContract.EpisodeEntry._ID +
                                        " = ?", new String[]{String.valueOf(episode.getId())}).build();
                        break;
                    case ADD:
                        operation = ContentProviderOperation.newInsert(PremoContract.EpisodeEntry.CONTENT_URI)
                                .withValues(EpisodeModel.fromEpisode(episode)).build();
                        break;
                }

                if (operation != null) {
                    operations.add(operation);
                }
            }

            if (operations.size() > 0) {
                contentResolver.applyBatch(PremoContract.CONTENT_AUTHORITY, operations);
            }
        }
    }

    /**
     * Compares episodes stored locally to episodes from the server, and returns
     *  whats new & whats changed
     * @param localEpisodes
     * @param serverEpisodes
     * @return
     */
    public static List<List<Episode>> compareEpisodes(List<Episode> localEpisodes,
                                                      List<Episode> serverEpisodes) {

        // convert each list to maps for easy comparison
        Map<String, Episode> localEpisodeMap = convertListToMap(localEpisodes);
        Map<String, Episode> serverEpisodeMap = convertListToMap(serverEpisodes);

        List<Episode> episodesToAdd = getNewEpisodes(localEpisodeMap, serverEpisodeMap);
        List<Episode> episodesToUpdate = getAlteredEpisodes(localEpisodeMap, serverEpisodeMap);

        // package it up and return it
        List<List<Episode>> episodeComparison = new ArrayList<>();
        Log.d(TAG, "Number of channels to update: " + episodesToUpdate.size());
        episodeComparison.add(UPDATE, episodesToUpdate);
        Log.d(TAG, "Number of channels to add: " + episodesToAdd.size());
        episodeComparison.add(ADD, episodesToAdd);
        return episodeComparison;
    }

    /**
     * Compares a map of episodes from the server with episodes from the devices, returns a list
     * of episodes that don't exist on the device
     * @param localEpisodes
     * @param serverEpisodes
     * @return
     */
    public static List<Episode> getNewEpisodes(Map<String, Episode> localEpisodes,
                                               Map<String, Episode> serverEpisodes) {
        List<Episode> episodeList = new ArrayList<>();

        // what episodes are new?
        for (Episode episode : serverEpisodes.values()) {

            if (!localEpisodes.containsKey(episode.getGeneratedId())) {
                episodeList.add(episode);
            }
        }
        return episodeList;
    }

    public static List<Episode> getAlteredEpisodes(Map<String, Episode> localEpisodes,
                                                   Map<String, Episode> serverEpisodes) {
        List<Episode> episodeList = new ArrayList<>();

        // what channels should we update
        for (Episode episode : serverEpisodes.values()) {
            Episode localEpisode = localEpisodes.get(episode.getGeneratedId());

            // skip updating episodes that are currently playing
            if (localEpisode != null && localEpisode.getEpisodeStatus()
                    != EpisodeStatus.IN_PROGRESS) {

                // is the channel metadata different?
                if (!localEpisode.metadataEquals(episode)) {
                    // transfer the local db id to the server channel
                    episode.setId(localEpisode.getId());
                    episodeList.add(episode);
                }
            }
        }
        return episodeList;
    }

    /**
     * Returns true if the episode with the server ID is already local on the device
     * @param context
     * @param serverId
     * @return
     */
    public static boolean doesEpisodeExist(Context context, String serverId) {
        SQLiteDatabase db = null;
        boolean exists = false;

        try {
            db = new DatabaseOpenHelper(context).getReadableDatabase();
            exists = DatabaseUtils.queryNumEntries(db,
                    PremoContract.EpisodeEntry.TABLE_NAME,
                    EPISODE_SERVER_ID_QUERY,
                    new String[] { serverId }) > 0;
        } finally {
            ResourceHelper.closeResource(db);
        }
        return exists;
    }

    /**
     * Returns true if the episode with the server ID is already local on the device
     * @param context
     * @param serverId
     * @return
     */
    public static boolean doesEpisodeExistManually(Context context, String serverId) {
        SQLiteDatabase db = null;
        boolean exists = false;

        try {
            db = new DatabaseOpenHelper(context).getReadableDatabase();
            exists = DatabaseUtils.queryNumEntries(db,
                    PremoContract.EpisodeEntry.TABLE_NAME,
                    EPISODE_SERVER_ID_MANUAL_DOWNLOAD_QUERY,
                    new String[] { serverId }) > 0;
        } finally {
            ResourceHelper.closeResource(db);
        }
        return exists;
    }

    public static void markEpisodesCompletedByChannelAsync(final Context context, final String channelServerId) {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                markEpisodesCompletedByChannel(context, channelServerId);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static boolean markEpisodesCompletedByChannel(Context context, String channelServerId) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.EpisodeEntry.EPISODE_STATUS_ID, EpisodeStatus.COMPLETED);
        int updated = context.getContentResolver().update(PremoContract.EpisodeEntry.CONTENT_URI, record,
                PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID + " = ?",
                new String[] { channelServerId });
        return updated == 1;
    }

    public static boolean markEpisodeDeleted(Context context, int episodeId) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID, DownloadStatus.NOT_DOWNLOADED);
        record.put(PremoContract.EpisodeEntry.LOCAL_MEDIA_URL, "");
        record.put(PremoContract.EpisodeEntry.DOWNLOADED_SIZE, 0);
        record.put(PremoContract.EpisodeEntry.MANUAL_DOWNLOAD, 0);
        int updated = context.getContentResolver().update(PremoContract.EpisodeEntry.CONTENT_URI, record,
                PremoContract.EpisodeEntry._ID + " = ?",
                new String[] { String.valueOf(episodeId) });
        return updated == 1;
    }

    public static boolean markEpisodeDeleted(Context context, String localMediaUrl) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID, DownloadStatus.NOT_DOWNLOADED);
        record.put(PremoContract.EpisodeEntry.LOCAL_MEDIA_URL, "");
        record.put(PremoContract.EpisodeEntry.DOWNLOADED_SIZE, 0);
        record.put(PremoContract.EpisodeEntry.MANUAL_DOWNLOAD, 0);
        int updated = context.getContentResolver().update(PremoContract.EpisodeEntry.CONTENT_URI, record,
                PremoContract.EpisodeEntry.LOCAL_MEDIA_URL + " = ?",
                new String[] { localMediaUrl });
        return updated == 1;
    }

    public static void markEpisodeCompleted(Context context, int episodeId, boolean isDownloaded) {
        Bundle params = new Bundle();
        params.putLong(EpisodeModel.PARAM_EPISODE_PROGRESS, 0);
        params.putInt(EpisodeModel.PARAM_EPISODE_STATUS, EpisodeStatus.COMPLETED);
        EpisodeModel.updateEpisodeAsync(context, episodeId, params);

        if (isDownloaded && UserPrefHelper.get(context).getBoolean(R.string.pref_key_delete_completed_episodes)) {
            DeleteEpisodeService.deleteEpisode(context, episodeId);
        }
    }

    public static void unpinEpisode(Context context, int episodeId) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(EpisodeModel.PARAM_MANUALLY_ADDED, false);
        EpisodeModel.updateEpisode(context, episodeId, bundle);
    }

    public static void deleteEpisode(Context context, int episodeId) {
        context.getContentResolver().delete(
                PremoContract.EpisodeEntry.CONTENT_URI,
                PremoContract.EpisodeEntry._ID + " = ?",
                new String[]{String.valueOf(episodeId)});
    }

        /**
         * Marks episods with the IDs as queued in the database
         * @param context
         * @param episodeIds
         */
    public static void markEpisodesAsQueued(Context context, List<Integer> episodeIds) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID, DownloadStatus.QUEUED);
        int updated = context.getContentResolver().update(PremoContract.EpisodeEntry.CONTENT_URI, record,
                PremoContract.EpisodeEntry._ID + " IN (" + TextUtils.join(",", episodeIds) + ")",
                null);
        Log.d(TAG, "Episodes marked as queue: " + updated);
    }

    /**
     * Marks any queued/requested/downloading episodes as NOT DOWNLOADED
     * @param context
     */
    public static void markQueuedEpisodeNotDownloaded(Context context) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID, DownloadStatus.NOT_DOWNLOADED);
        record.put(PremoContract.EpisodeEntry.LOCAL_MEDIA_URL, "");
        record.put(PremoContract.EpisodeEntry.DOWNLOADED_SIZE, 0);
        record.put(PremoContract.EpisodeEntry.MANUAL_DOWNLOAD, 0);

        int updated = context.getContentResolver().update(PremoContract.EpisodeEntry.CONTENT_URI, record,
                PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + " = ? OR " +
                        PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + " = ? OR " +
                        PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + " = ?",
                new String[] { String.valueOf(DownloadStatus.REQUESTED),
                    String.valueOf(DownloadStatus.QUEUED),
                        String.valueOf(DownloadStatus.DOWNLOADING)});
        Log.d(TAG, "markQueueEpisodeNotDownloaded: " + updated);
    }

    /**
     * Converts a list of collectables to a HashMap, using the server ID as the key
     * @param episodeList
     * @return
     */
    public static Map<String, Episode> convertListToMap(List<Episode> episodeList) {

        if (episodeList == null) {
            throw new IllegalArgumentException("Collectables in list cannot be null");
        }
        Map<String, Episode> collectableMap = new ArrayMap<>(episodeList.size());

        for (int i = 0; i < episodeList.size(); i++) {
            Episode episode = episodeList.get(i);
            collectableMap.put(episode.getGeneratedId(), episode) ;
        }
        return collectableMap;
    }

    /**
     * Bulk inserts a list of episodes into the database
     * @param context
     * @param episodeList
     * @return
     */
    public static boolean bulksInsertEpisodes(Context context, List<Episode> episodeList) {
        Log.d(TAG, "Number episode to bulk insert: " + episodeList.size());
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues[] records = new ContentValues[episodeList.size()];

        for (int i = 0; i < records.length; i++) {
            records[i] = EpisodeModel.fromEpisode(episodeList.get(i));
        }
        return contentResolver.bulkInsert(PremoContract.EpisodeEntry.CONTENT_URI, records) > 0;
    }

    private static boolean toggleFavorite(Context context, int episodeId) {
        boolean favorite = false;
        Cursor cursor = null;

        try {
            // get the favorite status from the DB
            cursor = context.getContentResolver().query(PremoContract.EpisodeEntry.CONTENT_URI,
                    new String[]{PremoContract.EpisodeEntry.FAVORITE}, PremoContract.EpisodeEntry._ID + " = ?",
                    new String[]{String.valueOf(episodeId)}, null);

            if (cursor == null) {
                return false;
            }

            cursor.moveToFirst();
            favorite = cursor.getInt(cursor.getColumnIndex(PremoContract.EpisodeEntry.FAVORITE)) == 1;

            // toggle the status
            favorite = !favorite;
            ContentValues record = new ContentValues();
            record.put(PremoContract.EpisodeEntry.FAVORITE, favorite ? 1 : 0);
            context.getContentResolver().update(PremoContract.EpisodeEntry.CONTENT_URI, record,
                    "_id = ?", new String[]{String.valueOf(episodeId)});
        } finally {
            ResourceHelper.closeResource(cursor);
        }

        return favorite;
    }

    public static void toggleFavoriteAsync(final Context context, final int episodeId,
                                           final OnToggleFavoriteEpisodeListener listener) {

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return toggleFavorite(context, episodeId);
            }

            @Override
            protected void onPostExecute(Boolean isFavorite) {

                if (listener != null) {
                    listener.onFavoriteToggled(isFavorite);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public interface EpisodeUpdateFinishedListener {
        void onUpdateFinished();
    }

    public interface OnToggleFavoriteEpisodeListener {
        void onFavoriteToggled(boolean isFavorite);
    }
}
