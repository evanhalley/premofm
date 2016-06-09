/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.util.StringUtil;

/**
 * Provides are application with data
 * Created by evan on 3/4/15.
 */
public class PremoContentProvider extends ContentProvider {

    public static final String TAG = PremoContentProvider.class.getSimpleName();
    private static final int EPISODES           = 100;
    private static final int EPISODE            = 101;
    private static final int GROUPED_EPISODES   = 102;
    private static final int CHANNELS           = 200;
    private static final int CHANNEL            = 201;
    private static final int COLLECTIONS        = 300;
    private static final int COLLECTION         = 301;
    private static final int FILTERS            = 500;
    private static final int FILTER             = 501;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    // URI matcher
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = PremoContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, PremoContract.PATH_EPISODE, EPISODES);
        matcher.addURI(authority, PremoContract.PATH_EPISODE + "/#", EPISODE);
        matcher.addURI(authority, PremoContract.PATH_GROUPED_EPISODES + "/#", GROUPED_EPISODES);
        matcher.addURI(authority, PremoContract.PATH_CHANNEL, CHANNELS);
        matcher.addURI(authority, PremoContract.PATH_CHANNEL + "/#", CHANNEL);
        matcher.addURI(authority, PremoContract.PATH_COLLECTION, COLLECTIONS);
        matcher.addURI(authority, PremoContract.PATH_COLLECTION + "/#", COLLECTION);
        matcher.addURI(authority, PremoContract.PATH_FILTER, FILTERS);
        matcher.addURI(authority, PremoContract.PATH_FILTER + "/#", FILTER);
        return matcher;
    };

    private DatabaseOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseOpenHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        int match = sUriMatcher.match(uri);

        switch (match) {
            case EPISODES:
                return PremoContract.EpisodeEntry.CONTENT_TYPE;
            case EPISODE:
                return PremoContract.EpisodeEntry.CONTENT_ITEM_TYPE;
            case CHANNELS:
                return PremoContract.ChannelEntry.CONTENT_TYPE;
            case CHANNEL:
                return PremoContract.ChannelEntry.CONTENT_ITEM_TYPE;
            case COLLECTIONS:
                return PremoContract.CollectionEntry.CONTENT_TYPE;
            case COLLECTION:
                return PremoContract.CollectionEntry.CONTENT_ITEM_TYPE;
            case FILTERS:
                return PremoContract.FilterEntry.CONTENT_TYPE;
            case FILTER:
                return PremoContract.FilterEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        String query = null;
        int match = sUriMatcher.match(uri);

        switch (match) {
            case EPISODES:
                queryBuilder.setTables(PremoContract.EpisodeEntry.TABLE_NAME);
                break;
            case EPISODE:
                queryBuilder.setTables(PremoContract.EpisodeEntry.TABLE_NAME);
                queryBuilder.appendWhere(PremoContract.EpisodeEntry._ID + " = " +
                        uri.getLastPathSegment());
                break;
            case GROUPED_EPISODES:
                String episodesPerChannel = uri.getLastPathSegment();

                if (TextUtils.isEmpty(selection)) {
                    selection = " 1=1 ";
                }
                StringBuilder builder = new StringBuilder(StringUtil.DEFAULT_STRING_BUILDER_SIZE);
                builder
                    .append("SELECT * ")
                    .append("FROM ").append(PremoContract.EpisodeEntry.TABLE_NAME).append(" a ")
                    .append("WHERE (")
                    .append("SELECT COUNT (*) ")
                    .append("FROM ").append(PremoContract.EpisodeEntry.TABLE_NAME).append(" b ")
                    .append("WHERE b.").append(PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID).append(" = a.").append(PremoContract.EpisodeEntry.CHANNEL_GENERATED_ID).append(" ")
                    .append("AND b.").append(PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS).append(" > a.").append(PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS).append(" ")
                    .append("AND ").append(selection)
                    .append(") <").append(episodesPerChannel).append(" ")
                    .append("AND ").append(selection).append(" ")
                    .append("ORDER BY ");

                if (sortOrder == null) {
                    builder.append("a.").append(PremoContract.EpisodeEntry.PUBLISHED_AT_MILLIS + " DESC");
                } else {
                    builder.append(sortOrder);
                }
                query = builder.toString();
                break;
            case CHANNELS:
                queryBuilder.setTables(PremoContract.ChannelEntry.TABLE_NAME);
                break;
            case CHANNEL:
                queryBuilder.setTables(PremoContract.ChannelEntry.TABLE_NAME);
                queryBuilder.appendWhere(PremoContract.ChannelEntry._ID + " = " +
                        uri.getLastPathSegment());
                break;
            case COLLECTIONS:
                queryBuilder.setTables(PremoContract.CollectionEntry.TABLE_NAME);
                break;
            case COLLECTION:
                queryBuilder.setTables(PremoContract.CollectionEntry.TABLE_NAME);
                queryBuilder.appendWhere(PremoContract.CollectionEntry._ID + " = " +
                        uri.getLastPathSegment());
                break;
            case FILTERS:
                queryBuilder.setTables(PremoContract.FilterEntry.TABLE_NAME);
                break;
            case FILTER:
                queryBuilder.setTables(PremoContract.FilterEntry.TABLE_NAME);
                queryBuilder.appendWhere(PremoContract.FilterEntry._ID + " = " +
                        uri.getLastPathSegment());
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (query != null) {

            // if the match was on GROUP_EPISODES, make uri point to episode.CONTENT_URI
            if (match == GROUPED_EPISODES) {
                uri = PremoContract.EpisodeEntry.CONTENT_URI;
            }
            cursor = mOpenHelper.getReadableDatabase().rawQuery(query, null);
        } else {
            cursor = queryBuilder.query(mOpenHelper.getReadableDatabase(),
                    projection, selection, selectionArgs, null, null, sortOrder);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Uri returnUri;
        long id;
        int match = sUriMatcher.match(uri);

        switch (match) {
            case EPISODES:
                values.put(PremoContract.EpisodeEntry.UPDATED_AT,
                        DatetimeHelper.getTimestamp());
                id = mOpenHelper.getWritableDatabase().insert(
                        PremoContract.EpisodeEntry.TABLE_NAME, null, values);

                if (id > 0) {
                    returnUri = PremoContract.EpisodeEntry.buildUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case CHANNELS:
                id = mOpenHelper.getWritableDatabase().insert(
                        PremoContract.ChannelEntry.TABLE_NAME, null, values);

                if (id > 0) {
                    returnUri = PremoContract.ChannelEntry.buildUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case COLLECTIONS:
                id = mOpenHelper.getWritableDatabase().insert(
                        PremoContract.CollectionEntry.TABLE_NAME, null, values);

                if (id > 0) {
                    returnUri = PremoContract.CollectionEntry.buildUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case FILTERS:
                values.put(PremoContract.FilterEntry.UPDATED_AT, DatetimeHelper.getTimestamp());
                id = mOpenHelper.getWritableDatabase().insert(
                        PremoContract.FilterEntry.TABLE_NAME, null, values);

                if (id > 0) {
                    returnUri = PremoContract.FilterEntry.buildUri(id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted;
        int match = sUriMatcher.match(uri);

        // this makes delete all rows return the number of rows deleted
        if (selection == null) {
            selection = "1";
        }

        switch (match) {
            case EPISODES:
                rowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        PremoContract.EpisodeEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CHANNELS:
                rowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        PremoContract.ChannelEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case COLLECTIONS:
                rowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        PremoContract.CollectionEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case FILTERS:
                rowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        PremoContract.FilterEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int rowsUpdated;
        int match = sUriMatcher.match(uri);

        switch (match) {
            case EPISODES:
                values.put(PremoContract.EpisodeEntry.UPDATED_AT, DatetimeHelper.getTimestamp());
                rowsUpdated = mOpenHelper.getWritableDatabase().update(
                        PremoContract.EpisodeEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case CHANNELS:
                rowsUpdated = mOpenHelper.getWritableDatabase().update(
                        PremoContract.ChannelEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case COLLECTIONS:
                rowsUpdated = mOpenHelper.getWritableDatabase().update(
                        PremoContract.CollectionEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case FILTERS:
                values.put(PremoContract.FilterEntry.UPDATED_AT, DatetimeHelper.getTimestamp());
                rowsUpdated = mOpenHelper.getWritableDatabase().update(
                        PremoContract.FilterEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null, false);
        }
        return rowsUpdated;
    }
}

