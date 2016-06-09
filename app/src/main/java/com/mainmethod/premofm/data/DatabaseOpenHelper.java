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
                PremoContract.ChannelEntry.GENERATED_ID + ")");

        // episode indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS episode_serverId_Idx ON " +
                PremoContract.EpisodeEntry.TABLE_NAME + " (" +
                PremoContract.EpisodeEntry.GENERATED_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS episode_channelServerId_Idx ON " +
                PremoContract.EpisodeEntry.TABLE_NAME + " (" +
                PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID + ")");
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
                PremoContract.CollectionEntry.COLLECTED_GENERATED_IDS + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS collection_collectedServerIds_Idx ON " +
                PremoContract.CollectionEntry.TABLE_NAME + " (" +
                PremoContract.CollectionEntry.COLLECTED_GENERATED_IDS + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database, old version:  " + oldVersion +", new version: " + newVersion);

    }
}
