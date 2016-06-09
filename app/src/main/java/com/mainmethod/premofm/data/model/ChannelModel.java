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
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.mainmethod.premofm.data.LoadMapCallback;
import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.object.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by evan on 4/15/15.
 */
public class ChannelModel {

    public static final int LOADER_ID = 2;
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
        channel.setServerId(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.GENERATED_ID)));
        channel.setTitle(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.TITLE)));
        channel.setAuthor(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.AUTHOR)));
        channel.setDescription(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.DESCRIPTION)));
        channel.setSiteUrl(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.SITE_URL)));
        channel.setFeedUrl(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.FEED_URL)));
        channel.setArtworkUrl(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.ARTWORK_URL)));
        channel.setNetwork(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.NETWORK)));
        channel.setTags(cursor.getString(cursor.getColumnIndex(PremoContract.ChannelEntry.TAGS)));
        return channel;
    }

    /**
     * Converts an Episode into a database record
     * @return record
     */
    public static ContentValues fromChannel(Channel channel) {
        ContentValues record = new ContentValues();
        record.put(PremoContract.ChannelEntry.GENERATED_ID, channel.getServerId());
        record.put(PremoContract.ChannelEntry.TITLE, channel.getTitle());
        record.put(PremoContract.ChannelEntry.DESCRIPTION, channel.getDescription());
        record.put(PremoContract.ChannelEntry.AUTHOR, channel.getAuthor());
        record.put(PremoContract.ChannelEntry.SITE_URL, channel.getSiteUrl());
        record.put(PremoContract.ChannelEntry.FEED_URL, channel.getFeedUrl());
        record.put(PremoContract.ChannelEntry.ARTWORK_URL, channel.getArtworkUrl());
        record.put(PremoContract.ChannelEntry.NETWORK, channel.getNetwork());
        record.put(PremoContract.ChannelEntry.TAGS, channel.getTags());
        return record;
    }

    public static Channel getChannelByServerId(Context context, String serverId) {

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
            channelMap.put(channelList.get(i).getServerId(), channelList.get(i));
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
            localChannelMap.put(channel.getServerId(), channel);
        }

        for (Channel channel : serverChannels) {
            serverChannelMap.put(channel.getServerId(), channel);
        }

        // what channels are new?
        for (Channel channel : serverChannelMap.values()) {

            if (!localChannelMap.containsKey(channel.getServerId())) {
                channelsToAdd.add(channel);
            }
        }

        // what channels should we delete
        for (Channel channel : localChannelMap.values()) {

            if (!serverChannelMap.containsKey(channel.getServerId())) {
                channelsToDelete.add(channel);
            }
        }

        // what channels should we update
        for (Channel channel : serverChannelMap.values()) {
            Channel localChannel = localChannelMap.get(channel.getServerId());

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
    public static void storeChannels(Context context, int operationType, List<Channel> channelList)
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
}