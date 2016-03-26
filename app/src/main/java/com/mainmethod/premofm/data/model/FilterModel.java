/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.data.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.object.Collection;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.object.Filter;
import com.mainmethod.premofm.util.StringUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by evan on 6/14/15.
 */
public class FilterModel {

    public static final int DISABLED = -1;
    public static final int DAYS_SINCE_PUBLISHED_YESTERDAY = 1;
    public static final int DAYS_SINCE_PUBLISHED_LAST_WEEK = 7;
    public static final int DAYS_SINCE_PUBLISHED_LAST_MONTH = 30;
    public static final int DAYS_SINCE_PUBLISHED_LAST_SIX_MONTHS = 180;

    /**
     * Function will correctly sort filters that have collections.  Items in a collection have an order
     *   and this function builds a sort order for that purpose;
     *
     *   ORDER BY
     *   CASE ID
     *   WHEN 4 THEN 0
     *   WHEN 3 THEN 1
     *   WHEN 1 THEN 2
     *   WHEN 5 THEN 3
     *   WHEN 6 THEN 4
     *   END
     *
     * @param context
     * @param filter
     * @return
     */
    public static String buildFilterCollectionSort(Context context, Filter filter) {
        String sort = null;

        if (filter.getCollectionId() > -1) {
            Collection collection = CollectionModel.getCollectionById(context,
                    filter.getCollectionId());

            if (collection.getCollectedServerIds().size() == 0) {
                return sort;
            }

            String columnToSort;

            switch (collection.getType()) {
                case Collection.COLLECTION_TYPE_CHANNEL:
                    columnToSort = PremoContract.EpisodeEntry.CHANNEL_SERVER_ID;
                    break;
                case Collection.COLLECTION_TYPE_EPISODE:
                    columnToSort = PremoContract.EpisodeEntry.SERVER_ID;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown collection type: " +
                            collection.getType());
            }
            StringBuilder sortBuilder = new StringBuilder(StringUtil.DEFAULT_STRING_BUILDER_SIZE)
                    .append("CASE ")
                    .append(columnToSort);

            for (int i = 0; i < collection.getCollectedServerIds().size(); i++) {
                sortBuilder.append(" WHEN ")
                        .append("'")
                        .append(collection.getCollectedServerIds().get(i))
                        .append("'")
                        .append(" THEN ")
                        .append(i);
            }
            sort = sortBuilder.append(" END ")
                    .append(", ")
                    .append(PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS)
                    .append(" DESC").toString();
        }
        return sort;
    }

    public static String buildFilterQuery(Context context, Filter filter) {
        StringBuilder episodeWhere =  new StringBuilder(StringUtil.DEFAULT_STRING_BUILDER_SIZE);

        if (filter.getEpisodeStatusIds() != null && filter.getEpisodeStatusIds().length > 0) {
            String episodeStatusIds = filter.getEpisodeStatusIdsStr();

            if (episodeStatusIds.contains(String.valueOf(EpisodeStatus.PLAYED))) {
                episodeStatusIds += "," + EpisodeStatus.IN_PROGRESS;
            }

            episodeWhere.append(PremoContract.EpisodeEntry.EPISODE_STATUS_ID)
                    .append(" IN ")
                    .append("(")
                    .append(episodeStatusIds)
                    .append(")");
        }

        StringBuilder downloadWhere =  new StringBuilder();

        if (filter.getDownloadStatusIds() != null && filter.getDownloadStatusIds().length > 0) {
            downloadWhere.append(PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID)
                    .append(" IN ")
                    .append("(")
                    .append(filter.getDownloadStatusIdsStr())
                    .append(")");
        }

        StringBuilder query = new StringBuilder(StringUtil.DEFAULT_STRING_BUILDER_SIZE)
                .append(episodeWhere.toString())
                .append(episodeWhere.length() > 0 && downloadWhere.length() > 0 ? " AND " : "")
                .append(downloadWhere.toString());

        if (filter.isFavorite()) {
            query.append(query.length() > 0 ? " AND " : "")
                    .append(PremoContract.EpisodeEntry.FAVORITE)
                    .append(" = '1'");
        }

        if (filter.getCollectionId() > -1) {
            Collection collection = CollectionModel.getCollectionById(context,
                    filter.getCollectionId());

            switch (collection.getType()) {
                case Collection.COLLECTION_TYPE_CHANNEL:
                    query.append(query.length() > 0 ? " AND " : "")
                            .append(generateChannelWhere(collection));
                    break;
                case Collection.COLLECTION_TYPE_EPISODE:
                    query.append(query.length() > 0 ? " AND " : "")
                            .append(generateEpisodeWhere(collection));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown collection type: " +
                            collection.getType());
            }
        }

        if (filter.getDaysSincePublished() > -1) {
            Calendar publishedDateLimit = Calendar.getInstance();
            publishedDateLimit.add(Calendar.DAY_OF_YEAR, -1 * filter.getDaysSincePublished());
            query.append(query.length() > 0 ? " AND " : "")
                    .append(PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS)
                    .append(" > ")
                    .append(publishedDateLimit.getTimeInMillis());
        }

        if (filter.isEpisodesManuallyAdded()) {
            query.append(query.length() > 0 ? " AND " : "")
                    .append(PremoContract.EpisodeEntry.MANUALLY_ADDED)
                    .append(" = 1");
        } else {
            query.append(query.length() > 0 ? " AND " : "")
                    .append(PremoContract.EpisodeEntry.CHANNEL_IS_SUBSCRIBED)
                    .append(" = 1 ");
        }

        return query.toString();
    }

    /**
     * Returns a list containing episode server IDs of episodes to add to the now playing queue
     * @param context
     * @param filter
     * @param num
     * @param episodeId
     * @return
     */
    public static ArrayList<String> getNextEpisodesForPlaylist(Context context, Filter filter, int num,
                                                          int episodeId) {
        Cursor cursor = null;
        ArrayList<String> episodeServerIds = new ArrayList<>(num);

        try {
            cursor = context.getContentResolver().query(PremoContract.EpisodeEntry.CONTENT_URI, null,
                    buildFilterQuery(context, filter),
                    null,
                    EpisodeModel.DEFAULT_EPISODE_SORT);
            int count = 0;
            boolean startCollecting = false;

            while (cursor != null && cursor.moveToNext() && count < num) {
                int id = cursor.getInt(
                        cursor.getColumnIndex(PremoContract.EpisodeEntry._ID));
                String serverId = cursor.getString(
                        cursor.getColumnIndex(PremoContract.EpisodeEntry.SERVER_ID));

                if (id == episodeId) {
                    startCollecting = true;
                }

                if (startCollecting) {
                    episodeServerIds.add(serverId);
                    count++;
                }
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return episodeServerIds;
    }

    private static String generateChannelWhere(Collection collection) {
        StringBuilder serverIdsStr = new StringBuilder();

        for (int i = 0; i < collection.getCollectedServerIds().size(); i++) {
            serverIdsStr.append("'").append(collection.getCollectedServerIds().get(i)).append("'");

            if (i < collection.getCollectedServerIds().size() - 1) {
                serverIdsStr.append(",");
            }
        }
        return PremoContract.EpisodeEntry.CHANNEL_SERVER_ID + " IN (" + serverIdsStr.toString() + ") ";
    }

    private static String generateEpisodeWhere(Collection collection) {
        StringBuilder serverIdsStr = new StringBuilder();

        for (int i = 0; i < collection.getCollectedServerIds().size(); i++) {
            serverIdsStr.append("'").append(collection.getCollectedServerIds().get(i)).append("'");

            if (i < collection.getCollectedServerIds().size() - 1) {
                serverIdsStr.append(",");
            }
        }
        return PremoContract.EpisodeEntry.SERVER_ID + " IN (" + serverIdsStr.toString() + ") ";
    }

    public static void createSampleFilters(Context context) {
        Filter whatsNewFilter = new Filter();
        whatsNewFilter.setName(context.getString(R.string.filter_name_latest));
        whatsNewFilter.setCollectionId(DISABLED);
        whatsNewFilter.setFavorite(false);
        whatsNewFilter.setUserCreated(false);
        whatsNewFilter.setDownloadStatusIds(new Integer[0]);
        whatsNewFilter.setEpisodeStatusIds(new Integer[]{EpisodeStatus.NEW, EpisodeStatus.PLAYED});
        whatsNewFilter.setOrder(0);
        whatsNewFilter.setEpisodesPerChannel(1);
        whatsNewFilter.setEpisodesManuallyAdded(false);
        whatsNewFilter.setDaysSincePublished(DAYS_SINCE_PUBLISHED_LAST_MONTH);

        Filter downloadedFilter = new Filter();
        downloadedFilter.setName(context.getString(R.string.filter_name_downloaded));
        downloadedFilter.setCollectionId(DISABLED);
        downloadedFilter.setFavorite(false);
        downloadedFilter.setUserCreated(false);
        downloadedFilter.setDownloadStatusIds(new Integer[]{DownloadStatus.DOWNLOADED});
        downloadedFilter.setEpisodeStatusIds(new Integer[0]);
        downloadedFilter.setOrder(1);
        downloadedFilter.setEpisodesPerChannel(DISABLED);
        downloadedFilter.setEpisodesManuallyAdded(false);
        downloadedFilter.setDaysSincePublished(DISABLED);

        Filter pinnedFilter = new Filter();
        pinnedFilter.setName(context.getString(R.string.filter_name_pinned));
        pinnedFilter.setCollectionId(DISABLED);
        pinnedFilter.setFavorite(false);
        pinnedFilter.setUserCreated(false);
        pinnedFilter.setDownloadStatusIds(new Integer[0]);
        pinnedFilter.setEpisodeStatusIds(new Integer[0]);
        pinnedFilter.setOrder(2);
        pinnedFilter.setEpisodesPerChannel(DISABLED);
        pinnedFilter.setEpisodesManuallyAdded(true);
        pinnedFilter.setDaysSincePublished(DISABLED);

        FilterModel.insertFilter(context, whatsNewFilter);
        FilterModel.insertFilter(context, downloadedFilter);
        FilterModel.insertFilter(context, pinnedFilter);
    }

    public static List<Filter> getFilters(Context context) {
        Cursor cursor = null;
        ContentResolver contentResolver = context.getContentResolver();
        List<Filter> filterList = null;

        try {
            cursor = contentResolver.query(PremoContract.FilterEntry.CONTENT_URI,
                    null, null, null, PremoContract.FilterEntry.FILTER_ORDER + " ASC");
            filterList = new ArrayList<>();

            while (cursor != null && cursor.moveToNext()) {
                filterList.add(FilterModel.toFilter(cursor));
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return filterList;
    }

    public static void deleteFilterAsync(final Context context, final int filterId,
                                         final FilterDeletedListener listener) {

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return deleteFilter(context, filterId);
            }

            @Override
            protected void onPostExecute(Boolean saved) {
                listener.onFilterDeleted(true);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    public static boolean deleteFilter(Context context, int filterId) {
        context.getContentResolver().delete(PremoContract.FilterEntry.CONTENT_URI,
                PremoContract.FilterEntry._ID + " = " + filterId, null);
        return true;
    }

    public static void updateFilterAsync(final Context context, final Filter filter,
                                         final FilterChangedListener listener) {

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return updateFilter(context, filter);
            }

            @Override
            protected void onPostExecute(Boolean saved) {
                listener.onFilterChanged(true);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    public static boolean updateFilter(Context context, Filter filter) {
        ContentValues record = FilterModel.fromFilter(filter);
        context.getContentResolver().update(PremoContract.FilterEntry.CONTENT_URI, record,
                PremoContract.FilterEntry._ID + " = " + filter.getId(), null);
        return true;
    }

    public static void insertFilterAsync(final Context context, final Filter filter,
                                         final FilterCreatedListener listener) {

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return insertFilter(context, filter);
            }

            @Override
            protected void onPostExecute(Boolean saved) {
                listener.onFilterCreated(true);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    public static boolean insertFilter(Context context, Filter filter) {
        ContentValues record = FilterModel.fromFilter(filter);
        context.getContentResolver().insert(PremoContract.FilterEntry.CONTENT_URI, record);
        return true;
    }

    public static void removeCollectionFromFilter(Context context, int collectionId) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.FilterEntry.COLLECTION_ID, -1);
        context.getContentResolver().update(PremoContract.FilterEntry.CONTENT_URI,
                record, PremoContract.FilterEntry.COLLECTION_ID + " = ?", new String[]{
                        String.valueOf(collectionId) });
    }

    public static ContentValues fromFilter(Filter filter) {
        ContentValues records = new ContentValues();
        records.put(PremoContract.FilterEntry.NAME, filter.getName());
        records.put(PremoContract.FilterEntry.COLLECTION_ID, filter.getCollectionId());
        records.put(PremoContract.FilterEntry.EPISODE_STATUS_IDS, filter.getEpisodeStatusIdsStr());
        records.put(PremoContract.FilterEntry.DOWNLOAD_STATUS_IDS, filter.getDownloadStatusIdsStr());
        records.put(PremoContract.FilterEntry.FAVORITE, filter.isFavorite() ? 1 : 0);
        records.put(PremoContract.FilterEntry.USER_CREATED, filter.isUserCreated() ? 1 : 0);
        records.put(PremoContract.FilterEntry.FILTER_ORDER, filter.getOrder());
        records.put(PremoContract.FilterEntry.EPISODES_PER_CHANNEL, filter.getEpisodesPerChannel());
        records.put(PremoContract.FilterEntry.DAYS_SINCE_PUBLISHED, filter.getDaysSincePublished());
        records.put(PremoContract.FilterEntry.EPISODES_MANUALLY_ADDED, filter.isEpisodesManuallyAdded() ? 1 : 0);
        return records;
    }

    public static Filter toFilter(Cursor cursor) {
        Filter filter = new Filter();
        filter.setId(cursor.getInt(cursor.getColumnIndex((PremoContract.FilterEntry._ID))));
        filter.setName(cursor.getString(cursor.getColumnIndex((PremoContract.FilterEntry.NAME))));
        filter.setCollectionId(cursor.getInt(cursor.getColumnIndex((PremoContract.FilterEntry.COLLECTION_ID))));
        filter.setOrder(cursor.getInt(cursor.getColumnIndex((PremoContract.FilterEntry.FILTER_ORDER))));
        filter.setDownloadStatusIdsFromStr(cursor.getString(cursor.getColumnIndex((PremoContract.FilterEntry.DOWNLOAD_STATUS_IDS))));
        filter.setEpisodeStatusIdsFromStr(cursor.getString(cursor.getColumnIndex((PremoContract.FilterEntry.EPISODE_STATUS_IDS))));
        filter.setUserCreated(cursor.getInt(cursor.getColumnIndex((PremoContract.FilterEntry.USER_CREATED))) == 1);
        filter.setFavorite(cursor.getInt(cursor.getColumnIndex((PremoContract.FilterEntry.FAVORITE))) == 1);
        filter.setEpisodesPerChannel(cursor.getInt(cursor.getColumnIndex((PremoContract.FilterEntry.EPISODES_PER_CHANNEL))));
        filter.setDaysSincePublished(cursor.getInt(cursor.getColumnIndex((PremoContract.FilterEntry.DAYS_SINCE_PUBLISHED))));
        filter.setEpisodesManuallyAdded(cursor.getInt(cursor.getColumnIndex((PremoContract.FilterEntry.EPISODES_MANUALLY_ADDED))) == 1);
        return filter;
    }

    public interface FilterCreatedListener {
        void onFilterCreated(boolean saved);
    }

    public interface FilterChangedListener {
        void onFilterChanged(boolean saved);
    }

    public interface FilterDeletedListener {
        void onFilterDeleted(boolean saved);
    }
}