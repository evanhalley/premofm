/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.mainmethod.premofm.BuildConfig;

/**
 * Created by evan on 3/4/15.
 */
public class PremoContract {

    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".data";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // paths
    public static final String PATH_EPISODE = "episode";
    public static final String PATH_GROUPED_EPISODES = "groupedEpisodes";
    public static final String PATH_CHANNEL = "channel";
    public static final String PATH_COLLECTION = "collection";
    public static final String PATH_FILTER = "filter";

    public static final class EpisodeEntry implements BaseColumns {

        // content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_EPISODE).build();

        public static final Uri GROUPED_CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_GROUPED_EPISODES).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_EPISODE;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_EPISODE;

        /** episode schema **/
        public static final String TABLE_NAME = "episode";
        public static final String GENERATED_ID = "generatedId";
        public static final String CHANNEL_GENERATED_ID = "channelGeneratedId";
        public static final String EPISODE_STATUS_ID = "episodeStatusId";
        public static final String DOWNLOAD_STATUS_ID = "downloadStatusId";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String DESCRIPTION_HTML = "descriptionHtml";
        public static final String PUBLISHED_AT = "publishedAt";
        public static final String PUBLISHED_AT_MILLIS = "publishedAtMillis";
        public static final String DURATION = "duration";
        public static final String URL = "url";
        public static final String REMOTE_MEDIA_URL = "remoteMediaUrl";
        public static final String LOCAL_MEDIA_URL = "localMediaUrl";
        public static final String SIZE = "size";
        public static final String DOWNLOADED_SIZE = "downloadedSize";
        public static final String MIME_TYPE = "mimeType";
        public static final String FAVORITE = "favorite";
        public static final String PROGRESS = "progress";
        public static final String MANUAL_DOWNLOAD = "manualDownload";
        public static final String UPDATED_AT = "updatedAt";
        public static final String CHANNEL_TITLE = "channelTitle";
        public static final String CHANNEL_AUTHOR = "channelAuthor";
        public static final String CHANNEL_ARTWORK_URL = "channelArtworkUrl";
        public static final String CHANNEL_IS_SUBSCRIBED = "channelIsSubscribed";
        public static final String MANUALLY_ADDED = "manuallyAdded";

        public static final String CREATE_SQL = "" +
                "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GENERATED_ID + " TEXT NOT NULL," +
                CHANNEL_GENERATED_ID + " TEXT NOT NULL," +
                EPISODE_STATUS_ID + " INTEGER NOT NULL," +
                DOWNLOAD_STATUS_ID + " INTEGER NOT NULL," +
                MANUAL_DOWNLOAD + " INTEGER NOT NULL," +
                TITLE + " TEXT NOT NULL," +
                DESCRIPTION + " TEXT," +
                DESCRIPTION_HTML + " TEXT," +
                CHANNEL_TITLE + " TEXT NOT NULL," +
                CHANNEL_AUTHOR + " TEXT NOT NULL," +
                CHANNEL_ARTWORK_URL + " TEXT NOT NULL," +
                CHANNEL_IS_SUBSCRIBED + " INTEGER NOT NULL DEFAULT 1," +
                MANUALLY_ADDED + " INTEGER NOT NULL DEFAULT 0," +
                FAVORITE + " INTEGER NOT NULL," +
                PUBLISHED_AT + " TEXT NOT NULL," +
                PUBLISHED_AT_MILLIS + " INTEGER NOT NULL," +
                DURATION + " INTEGER NOT NULL," +
                PROGRESS + " INTEGER NOT NULL," +
                URL + " TEXT," +
                REMOTE_MEDIA_URL + " TEXT NOT NULL," +
                LOCAL_MEDIA_URL + " TEXT," +
                SIZE + " INTEGER," +
                DOWNLOADED_SIZE + " INTEGER," +
                MIME_TYPE + " TEXT," +
                UPDATED_AT + " INTEGER);";

        public static Uri buildUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class ChannelEntry implements BaseColumns {

        // content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_CHANNEL).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CHANNEL;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CHANNEL;

        public static final String TABLE_NAME = "channel";
        public static final String GENERATED_ID = "generatedId";
        public static final String TITLE = "title";
        public static final String AUTHOR = "author";
        public static final String DESCRIPTION = "description";
        public static final String SITE_URL = "siteUrl";
        public static final String FEED_URL = "feedUrl";
        public static final String ARTWORK_URL = "artworkUrl";
        public static final String IS_SUBSCRIBED = "isSubscribed";
        public static final String ETAG = "eTag";
        public static final String LAST_MODIFIED = "lastModified";
        public static final String MD5 = "md5";
        public static final String LAST_SYNC_TIME = "lastSyncTime";
        public static final String LAST_SYNC_SUCCESSFUL = "lastSyncSuccessful";

        public static final String CREATE_SQL = "" +
                "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GENERATED_ID + " TEXT NOT NULL," +
                TITLE + " TEXT NOT NULL," +
                AUTHOR + " TEXT," +
                DESCRIPTION + " TEXT," +
                SITE_URL + " TEXT," +
                FEED_URL + " TEXT NOT NULL," +
                ARTWORK_URL + " TEXT," +
                IS_SUBSCRIBED + " INTEGER DEFAULT 0," +
                ETAG + " TEXT," +
                LAST_MODIFIED + " TEXT," +
                MD5 + " TEXT," +
                LAST_SYNC_TIME + " INTEGER," +
                LAST_SYNC_SUCCESSFUL + " INTEGER)";

        public static Uri buildUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class CollectionEntry implements BaseColumns {

        // content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_COLLECTION).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_COLLECTION;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_COLLECTION;

        public static final String TABLE_NAME = "collection";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String COLLECTION_TYPE = "collectionType";
        public static final String COLLECTED_GENERATED_IDS = "collectedGeneratedIds";
        public static final String PARAMS = "params";

        public static final String CREATE_SQL = "" +
                "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                NAME + " TEXT NOT NULL," +
                DESCRIPTION + " TEXT," +
                COLLECTION_TYPE + " INTEGER NOT NULL," +
                PARAMS + " TEXT NOT NULL," +
                COLLECTED_GENERATED_IDS + " TEXT);";

        public static Uri buildUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class FilterEntry implements BaseColumns {

        // content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_FILTER).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FILTER;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FILTER;

        public static final String TABLE_NAME = "filter";
        public static final String NAME = "name";
        public static final String EPISODE_STATUS_IDS = "episodeStatusIds";
        public static final String DOWNLOAD_STATUS_IDS = "downloadStatusIds";
        public static final String COLLECTION_ID = "collectionId";
        public static final String USER_CREATED = "userCreated";
        public static final String FAVORITE = "favorite";
        public static final String FILTER_ORDER = "filterOrder";
        public static final String EPISODES_PER_CHANNEL = "episodesPerChannel";
        public static final String DAYS_SINCE_PUBLISHED = "daysSincePublished";
        public static final String EPISODES_MANUALLY_ADDED = "episodesManuallyAdded";

        public static final String CREATE_SQL = "" +
                "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                NAME + " TEXT NOT NULL," +
                EPISODE_STATUS_IDS + " TEXT NOT NULL," +
                DOWNLOAD_STATUS_IDS + " TEXT NOT NULL," +
                COLLECTION_ID + " TEXT NOT NULL," +
                FAVORITE + " INTEGER NOT NULL," +
                USER_CREATED + " INTEGER NOT NULL," +
                FILTER_ORDER + " INTEGER NOT NULL," +
                EPISODES_PER_CHANNEL + " INTEGER NOT NULL DEFAULT -1," +
                DAYS_SINCE_PUBLISHED + " INTEGER NOT NULL DEFAULT -1," +
                EPISODES_MANUALLY_ADDED + " INTEGER NOT NULL DEFAULT 0);";

        public static Uri buildUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
