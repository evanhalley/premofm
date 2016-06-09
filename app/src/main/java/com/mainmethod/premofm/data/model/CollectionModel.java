/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.data.model;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.object.Collectable;
import com.mainmethod.premofm.object.Collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Collection *pun* of convenient functions for interacting with Collection objects in the DB
 * Created by evan on 4/19/15.
 */
public class CollectionModel {

    public static final int LOADER_ID = 0;

    public static Loader<Cursor> getCursorLoader(Context context) {
        return new CursorLoader(context, PremoContract.CollectionEntry.CONTENT_URI,
                null, null, null, PremoContract.CollectionEntry.NAME + " ASC");
    }

    public static ContentValues fromCollection(Collection collection) {
        ContentValues record = new ContentValues();

        if (collection.getId() > 0) {
            record.put(PremoContract.CollectionEntry._ID, collection.getId());
        }
        record.put(PremoContract.CollectionEntry.NAME, collection.getName());
        record.put(PremoContract.CollectionEntry.DESCRIPTION, collection.getDescription());
        record.put(PremoContract.CollectionEntry.PARAMS, TextUtils.join(",",
                collection.getParameters()));
        record.put(PremoContract.CollectionEntry.COLLECTED_GENERATED_IDS, TextUtils.join(",",
                collection.getCollectedServerIds()));
        record.put(PremoContract.CollectionEntry.COLLECTION_TYPE, collection.getType());
        return record;
    }

    public static Collection toCollection(Cursor cursor) {

        if (cursor == null || cursor.isClosed()) {
            throw new IllegalArgumentException("Cannot process null or closed cursor");
        }
        Collection collection = new Collection();
        collection.setId(cursor.getInt(cursor.getColumnIndex(PremoContract.CollectionEntry._ID)));
        collection.setName(cursor.getString(cursor.getColumnIndex(PremoContract.CollectionEntry.NAME)));
        collection.setDescription(cursor.getString(cursor.getColumnIndex(PremoContract.CollectionEntry.DESCRIPTION)));
        collection.setType(cursor.getInt(cursor.getColumnIndex(PremoContract.CollectionEntry.COLLECTION_TYPE)));

        String parameterStr = cursor.getString(
                cursor.getColumnIndex(PremoContract.CollectionEntry.PARAMS));
        List<String> parameters = new ArrayList<>(Arrays.asList(TextUtils.split(parameterStr, ",")));
        collection.setParameters(parameters);

        String collectedServerIdStr = cursor.getString( cursor.getColumnIndex(PremoContract.CollectionEntry.COLLECTED_GENERATED_IDS));
        List<String> collectedServerIds = new ArrayList<>(Arrays.asList(TextUtils.split(collectedServerIdStr, ",")));
        collection.setCollectedServerIds(collectedServerIds);
        return collection;
    }

    public static List<String> getCollectableServerIds(List collectables) {
        List<String> serverIds = new ArrayList<>(collectables.size());

        for (int i = 0; i < collectables.size(); i++) {
            serverIds.add(((Collectable) collectables.get(i)).getGeneratedId());
        }
        return serverIds;
    }

    /**
     * Finds channel collections containing the channel and removes it
     * @param context
     * @param channelServerId
     */
    public static void removeChannelFromCollections(Context context, String channelServerId) {
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(PremoContract.CollectionEntry.CONTENT_URI,
                    null,
                    PremoContract.CollectionEntry.COLLECTION_TYPE + " == ? AND " +
                        PremoContract.CollectionEntry.COLLECTED_GENERATED_IDS + " LIKE '%" + channelServerId +  "%'",
                    new String[]{String.valueOf(Collection.COLLECTION_TYPE_CHANNEL)},
                    null);

            if (cursor == null) {
                return;
            }
            ArrayList<Collection> collections = new ArrayList<>();

            while (cursor.moveToNext()) {
                collections.add(toCollection(cursor));
            }

            for (int i = 0; i < collections.size(); i++) {
                int foundChannelIdx = -1;

                for (int j = 0; j < collections.get(i).getCollectedServerIds().size(); j++) {

                    if (collections.get(i).getCollectedServerIds().get(j).contentEquals(channelServerId)) {
                        foundChannelIdx = j;
                        break;
                    }
                }

                if (foundChannelIdx > -1) {
                    collections.get(i).getCollectedServerIds().remove(foundChannelIdx);
                    saveCollection(context, collections.get(i), true);
                }
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }

    }

    /**
     * Get Collections that are marked as pending sync
     * @param context
     * @return
     */
    public static List<Collection> getPendingCollections(Context context) {
        ArrayList<Collection> collections = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(PremoContract.CollectionEntry.CONTENT_URI,
                null, null, null, null);

        if (cursor != null) {

            try {

                while (cursor.moveToNext()) {
                    collections.add(toCollection(cursor));
                }
            } finally {
                ResourceHelper.closeResource(cursor);
            }
        }
        return collections;
    }

    public static Collection getCollectionById(Context context, int collectionId) {
        Collection collection = null;
        Cursor cursor = context.getContentResolver().query(PremoContract.CollectionEntry.CONTENT_URI,
                null, PremoContract.ChannelEntry._ID + " = " + collectionId, null, null);

        try {

            if (cursor != null) {
                cursor.moveToFirst();
                collection = toCollection(cursor);
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return collection;
    }

    public static List<Collection> getCollections(Context context) {
        List<Collection> collections = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(PremoContract.CollectionEntry.CONTENT_URI,
                null, null, null, PremoContract.CollectionEntry.NAME + " ASC");

        try {

            while (cursor != null && cursor.moveToNext()) {
                collections.add(toCollection(cursor));
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return collections;
    }

    public static Map<String, Collection> getServerCollectionsMap(Context context) {
        Map<String, Collection> collections = new ArrayMap<>();
        Cursor cursor = context.getContentResolver().query(PremoContract.CollectionEntry.CONTENT_URI,
                null, null, null, PremoContract.CollectionEntry.NAME + " ASC");

        try {

            while (cursor != null && cursor.moveToNext()) {
                Collection collection = toCollection(cursor);

                if (!(TextUtils.isEmpty(collection.getGeneratedId()))) {
                    collections.put(collection.getGeneratedId(), collection);
                }
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return collections;
    }

    public static void markCollectionToDelete(Context context, int collectionId) {
        ContentValues contentValues = new ContentValues();
        context.getContentResolver().update(PremoContract.CollectionEntry.CONTENT_URI,
                contentValues,
                PremoContract.CollectionEntry._ID + " = ?",
                new String[]{String.valueOf(collectionId)});
    }

    public static void deleteCollection(Context context, int collectionId) {
        context.getContentResolver().delete(PremoContract.CollectionEntry.CONTENT_URI,
                PremoContract.CollectionEntry._ID + " = ?",
                new String[]{String.valueOf(collectionId)});
        FilterModel.removeCollectionFromFilter(context, collectionId);
    }

    public static void deleteCollectionAsync(final Context context, final int collectionId,
                                           final OnCollectionDeletedListener listener) {

        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                deleteCollection(context, collectionId);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (listener != null) {
                    listener.onCollectedDeleted();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static int saveCollection(Context context, Collection collection, boolean pushToServer) {
        ContentValues values = fromCollection(collection);
        int collectionId = -1;

        // this is an existing collection
        if (collection.getId() > -1) {
            context.getContentResolver().update(PremoContract.CollectionEntry.CONTENT_URI,
                    values,
                    PremoContract.CollectionEntry._ID + " = ?",
                    new String[]{String.valueOf(collection.getId())});
        } else {
            Uri uri = context.getContentResolver().insert(
                    PremoContract.CollectionEntry.CONTENT_URI,
                    values);
            collectionId = (int) ContentUris.parseId(uri);
        }
        return collectionId;
    }

    public interface OnCollectionDeletedListener {
        void onCollectedDeleted();
    }
}
