/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.data;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.object.SyncStatus;

/**
 * Our database maintenance class
 * Created by evan on 12/3/14.
 */
public class DatabaseOpenHelper extends SQLiteOpenHelper {

    private static final String TAG                     = DatabaseOpenHelper.class.getSimpleName();
    private static final int    DATABASE_VERSION        = 11;
    private static final String DATABASE_NAME           = "premo.db";

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        try {
            db.execSQL(PremoContract.ChannelEntry.CREATE_SQL);
            db.execSQL(PremoContract.EpisodeEntry.CREATE_SQL);
            db.execSQL(PremoContract.CollectionEntry.CREATE_SQL);
            db.execSQL(PremoContract.FilterEntry.CREATE_SQL);
            createIndexes(db);
        } catch (SQLException e) {
            Log.e(TAG, "Error in onCreate");
            Log.e(TAG, e.toString());
            throw e;
        }
    }

    private void createIndexes(SQLiteDatabase db) {
        // channel indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS channel_serverId_Idx ON " +
                PremoContract.ChannelEntry.TABLE_NAME + " (" +
                PremoContract.ChannelEntry.SERVER_ID + ")");

        // episode indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS episode_serverId_Idx ON " +
                PremoContract.EpisodeEntry.TABLE_NAME + " (" +
                PremoContract.EpisodeEntry.SERVER_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS episode_channelServerId_Idx ON " +
                PremoContract.EpisodeEntry.TABLE_NAME + " (" +
                PremoContract.EpisodeEntry.CHANNEL_SERVER_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS episode_title_Idx ON " +
                PremoContract.EpisodeEntry.TABLE_NAME + " (" +
                PremoContract.EpisodeEntry.TITLE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS episode_publishedAtMillis_Idx ON " +
                PremoContract.EpisodeEntry.TABLE_NAME + " (" +
                PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS episode_downloadStatusId_Idx ON " +
                PremoContract.EpisodeEntry.TABLE_NAME + " (" +
                PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS episode_episodeStatusId_Idx ON " +
                PremoContract.EpisodeEntry.TABLE_NAME + " (" +
                PremoContract.EpisodeEntry.EPISODE_STATUS_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS episode_favorite_Idx ON " +
                PremoContract.EpisodeEntry.TABLE_NAME + " (" +
                PremoContract.EpisodeEntry.FAVORITE + ")");

        // collection indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS collection_serverId_Idx ON " +
                PremoContract.CollectionEntry.TABLE_NAME + " (" +
                PremoContract.CollectionEntry.SERVER_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS collection_collectedServerIds_Idx ON " +
                PremoContract.CollectionEntry.TABLE_NAME + " (" +
                PremoContract.CollectionEntry.COLLECTED_SERVER_IDS + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database, old version:  " + oldVersion +", new version: " + newVersion);

        if (oldVersion == 1 && newVersion >= 2) {
            long updatedAt = DatetimeHelper.getTimestamp();

            /**** Migrate Collection Table ****/

            // rename collection column
            db.execSQL("ALTER TABLE collection RENAME TO collection_old");

            // create the new collection table
            db.execSQL(PremoContract.CollectionEntry.CREATE_SQL);

            // transfer the data to the new table
            db.execSQL(
                    "INSERT INTO collection(_id, serverId, name, description, collectionType, collectedServerIds, params, updatedAt) " +
                    "SELECT _id, serverId, name, description, 0, channelServerIds, '', " + updatedAt + " FROM collection_old");

            // drop the old collection table
            db.execSQL("DROP TABLE collection_old");

            /**** Migrate Episode Table ****/

            // rename the episode collection
            db.execSQL("ALTER TABLE episode RENAME TO episode_old");

            // create the new episode table
            db.execSQL(PremoContract.EpisodeEntry.CREATE_SQL);

            // transfer the data to the new table
            db.execSQL(
                    "INSERT INTO episode(_id, serverId, channelServerId, episodeStatusId, downloadStatusId, manualDownload, guid, " +
                            "title, description, descriptionHtml, channelTitle, channelAuthor, channelArtworkUrl, favorite, " +
                            "publishedAt, publishedAtMillis, duration, progress, url, remoteMediaUrl, localMediaUrl, size, " +
                            "downloadedSize, mimeType, updatedAt) " +
                    "SELECT _id, serverId, channelServerId, episodeStatusId, channelStatusId, manualDownload, guid, " +
                            "title, description, descriptionHtml, '', '', '', favorite, " +
                            "publishedAt, publishedAtMillis, duration, progress, url, mediaUrl, localMediaUrl, size, " +
                            "downloadedSize, mimeType, updatedAt " +
                    "FROM episode_old");

            // drop the old collection table
            db.execSQL("DROP TABLE episode_old");
        }

        if (oldVersion == 2 && newVersion >= 2) {
            // create the database
            db.execSQL(PremoContract.FilterEntry.CREATE_SQL);
        }

        if (oldVersion < 4 && newVersion >= 4) {
            // rename collection column
            db.execSQL("ALTER TABLE " + PremoContract.CollectionEntry.TABLE_NAME +
                    " ADD COLUMN " + PremoContract.CollectionEntry.SYNC_STATUS +
                    " INTEGER NOT NULL DEFAULT " + SyncStatus.PENDING_CREATE + ";");
        }

        if (oldVersion < 5 && newVersion >= 5) {
            // rename collection column
            db.execSQL("ALTER TABLE " + PremoContract.FilterEntry.TABLE_NAME +
                    " ADD COLUMN " + PremoContract.FilterEntry.EPISODES_PER_CHANNEL +
                    " INTEGER NOT NULL DEFAULT 2;");
        }

        if (oldVersion < 6 && newVersion >= 6) {
            createIndexes(db);
        }

        if (oldVersion < 7 && newVersion >= 7) {
            // channel is subscribed column
            db.execSQL("ALTER TABLE " + PremoContract.EpisodeEntry.TABLE_NAME +
                    " ADD COLUMN " + PremoContract.EpisodeEntry.CHANNEL_IS_SUBSCRIBED +
                    " INTEGER NOT NULL DEFAULT 1;");
        }

        if (oldVersion < 8 && newVersion >= 8) {
            db.execSQL("ALTER TABLE " + PremoContract.FilterEntry.TABLE_NAME +
                    " ADD COLUMN " + PremoContract.FilterEntry.DAYS_SINCE_PUBLISHED +
                    " INTEGER NOT NULL DEFAULT -1;");
        }

        if (oldVersion < 9 && newVersion >= 9) {
            db.execSQL("ALTER TABLE " + PremoContract.CollectionEntry.TABLE_NAME +
                    " ADD COLUMN " + PremoContract.CollectionEntry.AUTHOR_SERVER_ID +
                    " TEXT;");
        }

        if (oldVersion < 10 && newVersion >= 10) {
            db.execSQL("ALTER TABLE " + PremoContract.EpisodeEntry.TABLE_NAME +
                    " ADD COLUMN " + PremoContract.EpisodeEntry.MANUALLY_ADDED +
                    " INTEGER NOT NULL DEFAULT 0;");
        }

        if (oldVersion < 11 && newVersion >= 11) {
            db.execSQL("ALTER TABLE " + PremoContract.FilterEntry.TABLE_NAME +
                    " ADD COLUMN " + PremoContract.FilterEntry.EPISODES_MANUALLY_ADDED +
                    " INTEGER NOT NULL DEFAULT 0;");
        }
    }
}
