/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.data.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.LoadMapCallback;
import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.helper.opml.OpmlReader;
import com.mainmethod.premofm.helper.opml.OpmlWriter;
import com.mainmethod.premofm.http.HttpHelper;
import com.mainmethod.premofm.object.Channel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by evan on 4/15/15.
 */
public class ChannelModel {

    public static final int LOADER_ID = 2;

    public static final String ITUNES_DIRECTORY_LOOKUP_URL = "https://itunes.apple.com/lookup?id=%1$s";
    public static final int DIRECTORY_TYPE_ITUNES = 10000;
    public static final int UPDATE = 0;
    public static final int ADD    = 1;
    public static final int DELETE = 2;


    public static Loader<Cursor> getCursorLoader(Context context) {
        return new CursorLoader(context, PremoContract.ChannelEntry.CONTENT_URI, null, null, null,
                PremoContract.ChannelEntry.TITLE + " ASC");
    }

    /**
     * Converts the record referenced by the cursor to an Episode
     * @param cursor Cursor
     * @return Episode
     */
    public static Channel toChannel(Cursor cursor) {

        if (cursor == null || cursor.isClosed()) {
            throw new IllegalArgumentException("Cannot process null or closed cursor");
        }
        Channel channel = new Channel();
        channel.setId(cursor.getInt(cursor.getColumnIndex(PremoContract.ChannelEntry._ID)));
        channel.setGeneratedId(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.GENERATED_ID)));
        channel.setTitle(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.TITLE)));
        channel.setAuthor(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.AUTHOR)));
        channel.setDescription(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.DESCRIPTION)));
        channel.setSiteUrl(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.SITE_URL)));
        channel.setFeedUrl(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.FEED_URL)));
        channel.setArtworkUrl(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.ARTWORK_URL)));
        channel.setSubscribed(cursor.getInt(cursor.getColumnIndex(PremoContract.ChannelEntry.IS_SUBSCRIBED)) == 1);
        channel.setETag(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.ETAG)));
        channel.setLastModified(cursor.getLong(cursor.getColumnIndex(PremoContract.ChannelEntry.LAST_MODIFIED)));
        channel.setDataMd5(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.MD5)));
        channel.setLastSyncTime(cursor.getLong(cursor.getColumnIndex(PremoContract.ChannelEntry.LAST_SYNC_TIME)));
        channel.setLastSyncSuccessful(cursor.getInt(cursor.getColumnIndex(PremoContract.ChannelEntry.LAST_SYNC_SUCCESSFUL)) == 1);
        return channel;
    }

    /**
     * Converts an Episode into a database record
     * @return record
     */
    public static ContentValues fromChannel(Channel channel) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.ChannelEntry.GENERATED_ID, channel.getGeneratedId());
        record.put(PremoContract.ChannelEntry.TITLE, channel.getTitle());
        record.put(PremoContract.ChannelEntry.DESCRIPTION, channel.getDescription());
        record.put(PremoContract.ChannelEntry.AUTHOR, channel.getAuthor());
        record.put(PremoContract.ChannelEntry.SITE_URL, channel.getSiteUrl());
        record.put(PremoContract.ChannelEntry.FEED_URL, channel.getFeedUrl());
        record.put(PremoContract.ChannelEntry.ARTWORK_URL, channel.getArtworkUrl());
        record.put(PremoContract.ChannelEntry.IS_SUBSCRIBED, channel.isSubscribed() ? 1 : 0);
        record.put(PremoContract.ChannelEntry.ETAG, channel.getETag());
        record.put(PremoContract.ChannelEntry.LAST_MODIFIED, channel.getLastModified());
        record.put(PremoContract.ChannelEntry.MD5, channel.getDataMd5());
        record.put(PremoContract.ChannelEntry.LAST_SYNC_TIME, channel.getLastSyncTime());
        record.put(PremoContract.ChannelEntry.LAST_SYNC_SUCCESSFUL, channel.isLastSyncSuccessful() ? 1 : 0);
        return record;
    }

    public static Channel getChannelByGeneratedId(Context context, String serverId) {

        if (TextUtils.isEmpty(serverId)) {
            return null;
        }
        Channel channel = null;
        Cursor cursor = context.getContentResolver().query(PremoContract.ChannelEntry.CONTENT_URI,
                null,
                PremoContract.ChannelEntry.GENERATED_ID + " = '" + serverId + "'",
                null,
                null);

        if (cursor == null) {
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                channel = toChannel(cursor);
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return channel;
    }

    /**
     * Returns a map of channels where the key is the server ID
     * @return
     */
    public static Map<String, Channel> getChannelMap(Context context) {
        Map<String, Channel> channelMap = new ArrayMap<>();
        List<Channel> channelList = getChannels(context);

        for (int i = 0; i < channelList.size(); i++) {
            channelMap.put(channelList.get(i).getGeneratedId(), channelList.get(i));
        }

        return channelMap;
    }

    /**
     * Returns a list of channels, asynchronously
     * @param context
     * @param listener
     * @return
     */
    public static void getChannelMapAsync(final Context context, final LoadMapCallback<String, Channel> listener) {

        new AsyncTask<Void, Void, Map<String, Channel>>() {

            @Override
            protected Map<String, Channel> doInBackground(Void... params) {
                return getChannelMap(context);
            }

            @Override
            protected void onPostExecute(Map<String, Channel> channels) {
                listener.onMapLoaded(channels);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static int insertChannel(Context context, Channel channel) {
        ContentValues record = ChannelModel.fromChannel(channel);
        Uri uri = context.getContentResolver().insert(PremoContract.ChannelEntry.CONTENT_URI, record);
        return (int) ContentUris.parseId(uri);
    }

    /**
     * Returns the channel ID if it is subscribed, -1 if it is not subscribed
     * @param context
     * @param serverId
     * @return
     */
    public static int channelIsSubscribed(Context context, String serverId) {
        int channelId = -1;
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(
                    PremoContract.ChannelEntry.CONTENT_URI,
                    null,
                    PremoContract.ChannelEntry.GENERATED_ID + " = ?",
                    new String[]{ serverId },
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                channelId = cursor.getInt(cursor.getColumnIndex(PremoContract.ChannelEntry._ID));
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return channelId;

    }

    /**
     * Compares channels stored locally to channels from the server, and returns
     *  whats new
     *  what to delete
     *  whats changed
     * @param localChannels
     * @param serverChannels
     * @return
     */
    public static List<List<Channel>> compareChannels(List<Channel> localChannels,
                                                       List<Channel> serverChannels) {
        List<Channel> channelsToAdd = new ArrayList<>();
        List<Channel> channelsToDelete = new ArrayList<>();
        List<Channel> channelsToUpdate = new ArrayList<>();

        // convert each list to maps for easy comparison
        Map<String, Channel> localChannelMap = new ArrayMap<>();
        Map<String, Channel> serverChannelMap = new ArrayMap<>();

        for (Channel channel : localChannels) {
            localChannelMap.put(channel.getGeneratedId(), channel);
        }

        for (Channel channel : serverChannels) {
            serverChannelMap.put(channel.getGeneratedId(), channel);
        }

        // what channels are new?
        for (Channel channel : serverChannelMap.values()) {

            if (!localChannelMap.containsKey(channel.getGeneratedId())) {
                channelsToAdd.add(channel);
            }
        }

        // what channels should we delete
        for (Channel channel : localChannelMap.values()) {

            if (!serverChannelMap.containsKey(channel.getGeneratedId())) {
                channelsToDelete.add(channel);
            }
        }

        // what channels should we update
        for (Channel channel : serverChannelMap.values()) {
            Channel localChannel = localChannelMap.get(channel.getGeneratedId());

            if (localChannel != null) {

                // is the channel metadata different?
                if (!localChannel.metadataEquals(channel)) {
                    // transfer the local db id to the server channel
                    channel.setId(localChannel.getId());
                    channelsToUpdate.add(channel);
                }
            }
        }

        // package it up and return it
        List<List<Channel>> channelComparison = new ArrayList<>();
        channelComparison.add(channelsToUpdate);
        channelComparison.add(channelsToAdd);
        channelComparison.add(channelsToDelete);
        return channelComparison;
    }

    /**
     * Executes channel operations against the local database
     * @param operationType
     * @param channelList
     * @throws android.os.RemoteException
     * @throws android.content.OperationApplicationException
     */
    public static void storeImportedChannels(Context context, int operationType, List<Channel> channelList)
            throws RemoteException, OperationApplicationException {

        if (channelList != null && channelList.size() > 0) {
            ContentResolver contentResolver = context.getContentResolver();
            ArrayList<ContentProviderOperation> operations = new ArrayList<>(channelList.size());

            for (Channel channel : channelList) {
                ContentProviderOperation operation = null;

                switch (operationType) {
                    case ChannelModel.UPDATE:
                        operation = ContentProviderOperation.newUpdate(PremoContract.ChannelEntry.CONTENT_URI)
                                .withValues(ChannelModel.fromChannel(channel))
                                .withSelection(PremoContract.ChannelEntry._ID + " = ?",
                                        new String[]{String.valueOf(channel.getId())}).build();
                        break;
                    case ChannelModel.ADD:
                        operation = ContentProviderOperation.newInsert(PremoContract.ChannelEntry.CONTENT_URI)
                                .withValues(ChannelModel.fromChannel(channel)).build();
                        break;
                    case ChannelModel.DELETE:
                        operation = ContentProviderOperation.newDelete(PremoContract.ChannelEntry.CONTENT_URI)
                                .withSelection(PremoContract.ChannelEntry._ID + " = ?",
                                        new String[]{String.valueOf(channel.getId())}).build();
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

    public static List<Channel> getChannels(Context context) {
        List<Channel> channels = null;
        Cursor cursor = null;

        ContentResolver resolver = context.getContentResolver();

        try {
            cursor = resolver.query(PremoContract.ChannelEntry.CONTENT_URI,
                    null, null, null, PremoContract.ChannelEntry.TITLE + " ASC");

            if (cursor != null) {
                channels = new ArrayList<>();

                while (cursor.moveToNext()) {
                    channels.add(ChannelModel.toChannel(cursor));
                }
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return channels;
    }

    public static List<String> getChannelGeneratedIds(Context context) {
        List<String> generatedIds = null;
        Cursor cursor = null;

        ContentResolver resolver = context.getContentResolver();

        try {
            cursor = resolver.query(PremoContract.ChannelEntry.CONTENT_URI,
                    new String[] { PremoContract.ChannelEntry.GENERATED_ID },
                    null, null, PremoContract.ChannelEntry.TITLE + " ASC");

            if (cursor != null) {
                generatedIds = new ArrayList<>();

                while (cursor.moveToNext()) {
                    generatedIds.add(cursor.getString(cursor.getColumnIndex(
                            PremoContract.ChannelEntry.GENERATED_ID)));
                }
            }
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return generatedIds;
    }

    public static void updateChannel(Context context, Channel channel) {
        ContentValues record = ChannelModel.fromChannel(channel);
        context.getContentResolver().update(PremoContract.ChannelEntry.CONTENT_URI,
                record,
                PremoContract.ChannelEntry._ID + " = ?",
                new String[] { String.valueOf(channel.getId())});
    }

    public static void deleteChannel(Context context, String channelGeneratedId) {
        context.getContentResolver().delete(PremoContract.ChannelEntry.CONTENT_URI,
                PremoContract.ChannelEntry.GENERATED_ID + " = ?",
                new String[] { channelGeneratedId });
    }

    public static void changeSubscription(Context context, String channelGeneratedId, boolean doSubscribe) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.ChannelEntry.IS_SUBSCRIBED, doSubscribe ? 1 : 0);
        context.getContentResolver().update(PremoContract.ChannelEntry.CONTENT_URI,
                record,
                PremoContract.ChannelEntry.GENERATED_ID + " = ?",
                new String[] { channelGeneratedId });
    }

    public static List<Channel> getChannelsFromOpml(String opmlData) {
        StringReader reader = null;
        List<Channel> channelList = new ArrayList<>();

        try {
            reader = new StringReader(opmlData);
            OpmlReader opmlReader = new OpmlReader();
            channelList = opmlReader.readDocument(reader);
        } catch (Exception e) {
            Timber.e(e, "Error reading OPML");
            throw new RuntimeException(e);
        } finally {
            ResourceHelper.closeResource(reader);
        }
        return channelList;
    }

    public static void storeImportedChannels(Context context, List<Channel> channels) {

        try {
            ChannelModel.storeImportedChannels(context, ChannelModel.ADD, channels);
            List<String> generatedIds = CollectionModel.getCollectableGeneratedIds(channels);

            for (int i = 0; i < generatedIds.size(); i++) {
                UserPrefHelper.get(context).addGeneratedId(R.string.pref_key_notification_channels,
                        generatedIds.get(i));
            }
        } catch (Exception e) {
            Timber.e(e, "Error storing channels");
            throw new RuntimeException(e);
        }
    }

    public static Uri exportChannelsToOpml(Context context, Uri uri) {
        ParcelFileDescriptor pfd = null;
        FileOutputStream outputStream = null;
        OutputStreamWriter writer = null;

        try {
            List<Channel> channels = ChannelModel.getChannels(context);
            pfd = context.getContentResolver().openFileDescriptor(uri, "w");

            if (pfd == null) {
                throw new IllegalStateException("ParcelFileDescriptor is null");
            }
            outputStream = new FileOutputStream(pfd.getFileDescriptor());
            writer = new OutputStreamWriter(outputStream);
            OpmlWriter opmlWriter = new OpmlWriter();
            opmlWriter.writeDocument(channels, writer);
        } catch (Exception e) {
            Timber.e(e, "Error in exportChannelsToOpml");
            throw new RuntimeException(e);
        } finally {
            ResourceHelper.closeResources(pfd, outputStream, writer);
        }
        return uri;
    }

    public static Channel getChannelFromDirectory(int directoryType, String directoryId) {
        Channel channel = null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        if (directoryType == DIRECTORY_TYPE_ITUNES) {
            try {
                connection =
                        HttpHelper.getConnection(String.format(ITUNES_DIRECTORY_LOOKUP_URL, directoryId));
                inputStream = HttpHelper.getInputStream(connection);
                String data = HttpHelper.readData(inputStream);
                JSONObject json = new JSONObject(data);

                if (json.has("results") && json.has("resultCount") && json.getInt("resultCount") > 0) {
                    JSONObject result = json.getJSONArray("results").getJSONObject(0);
                    channel = new Channel();
                    channel.setFeedUrl(result.getString("feedUrl"));
                    channel.setGeneratedId(TextHelper.generateMD5(result.getString("feedUrl")));
                    channel.setTitle(result.optString("trackName"));
                    channel.setAuthor(result.optString("artistName"));
                    channel.setArtworkUrl(result.optString("artworkUrl1600"));
                }
            } catch (Exception e) {
                Timber.e(e, "Error in getChannelFromDirectory");
            } finally {
                ResourceHelper.closeResources(connection, inputStream);
            }
        }
        return channel;
    }
}