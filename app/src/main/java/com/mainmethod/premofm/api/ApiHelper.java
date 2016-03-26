/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.api;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.mainmethod.premofm.data.LoadCallback;
import com.mainmethod.premofm.data.LoadListCallback;
import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.object.Category;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Collection;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.object.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Convenience functions for accessing the API
 * Created by evan on 4/26/15.
 */
public class ApiHelper {

    private static final String TAG = ApiHelper.class.getSimpleName();

    /**
     *
     * @param jsonObject
     * @return
     */
    public static Collection toCollection(JSONObject jsonObject) throws JSONException {

        if (jsonObject == null) {
            throw new IllegalArgumentException("Cannot process null JSON object");
        }
        Collection collection = new Collection();
        collection.setServerId(jsonObject.getString(ApiManager.ID));
        collection.setName(jsonObject.getString(ApiManager.NAME));
        collection.setDescription(jsonObject.getString(ApiManager.DESCRIPTION));
        collection.setType(jsonObject.getInt(ApiManager.TYPE));

        switch (collection.getType()) {
            case Collection.COLLECTION_TYPE_CHANNEL:
                int numChannelIds = jsonObject.getJSONArray(ApiManager.CHANNEL_IDS).length();
                List<String> channelIds = new ArrayList<>(numChannelIds);

                for (int i = 0; i < numChannelIds; i++) {
                    channelIds.add(jsonObject.getJSONArray(ApiManager.CHANNEL_IDS).getString(i));
                }
                collection.setCollectedServerIds(channelIds);
                break;
            case Collection.COLLECTION_TYPE_EPISODE:
                int numEpisodeIds = jsonObject.getJSONArray(ApiManager.EPISODE_IDS).length();
                List<String> episodeIds = new ArrayList<>(numEpisodeIds);

                for (int i = 0; i < numEpisodeIds; i++) {
                    episodeIds.add(jsonObject.getJSONArray(ApiManager.EPISODE_IDS).getString(i));
                }
                collection.setCollectedServerIds(episodeIds);
                break;
            default:
                break;
        }
        return collection;
    }

    /**
     * Converts JSON from the API into a Channel
     * @param jsonObject json
     * @return channel
     * @throws JSONException
     */
    public static Channel toChannel(JSONObject jsonObject) throws JSONException {

        if (jsonObject == null) {
            throw new IllegalArgumentException("Cannot process null JSON object");
        }
        Channel channel = new Channel();
        channel.setServerId(jsonObject.getString(ApiManager.ID));
        channel.setTitle(jsonObject.getString(ApiManager.TITLE));
        channel.setAuthor(jsonObject.getString(ApiManager.AUTHOR));
        channel.setDescription(jsonObject.getString(ApiManager.DESCRIPTION));
        channel.setSiteUrl(jsonObject.getString(ApiManager.SITE_URL));
        channel.setFeedUrl(jsonObject.getString(ApiManager.FEED_URL));
        channel.setArtworkUrl(jsonObject.getString(ApiManager.ARTWORK_URL));
        channel.setNetwork(jsonObject.optString(ApiManager.NETWORK, ""));
        channel.setTags(jsonObject.optString(ApiManager.TAGS, ""));
        return channel;
    }

    /**
     * Converts JSON from the API into an Episode
     * @param jsonObject json data to parse
     * @param parseChannelObject if true, parse the attached channel data
     * @return episode
     * @throws JSONException
     * @throws ParseException
     */
    public static Episode toEpisode(JSONObject jsonObject, boolean parseChannelObject) throws JSONException, ParseException {

        if (jsonObject == null) {
            throw new IllegalArgumentException("Cannot process null JSON object");
        }
        Episode episode = new Episode();
        episode.setServerId(jsonObject.getString(ApiManager.ID));
        episode.setGuid(jsonObject.getString(ApiManager.GUID));
        episode.setTitle(jsonObject.getString(ApiManager.TITLE));
        episode.setPublishedAt(DatetimeHelper.stringToDate(jsonObject.getString(ApiManager.PUBLISHED_AT)));
        episode.setDuration(jsonObject.getInt(ApiManager.DURATION));
        episode.setProgress(jsonObject.optInt(ApiManager.PROGRESS));
        episode.setUrl(jsonObject.getString(ApiManager.URL));
        episode.setRemoteMediaUrl(jsonObject.getString(ApiManager.MEDIA_URL));
        episode.setSize(jsonObject.getInt(ApiManager.SIZE));
        episode.setMimeType(jsonObject.getString(ApiManager.MIME_TYPE));
        episode.setEpisodeStatus(jsonObject.optInt(ApiManager.STATUS, EpisodeStatus.NEW));
        episode.setFavorite(jsonObject.optBoolean(ApiManager.FAVORITE, false));

        String descriptionHtml = jsonObject.getString(ApiManager.DESCRIPTION);

        if(descriptionHtml != null) {
            episode.setDescription(descriptionHtml, true);
            episode.setDescriptionHtml(descriptionHtml);
        }

        if (parseChannelObject) {
            JSONObject channelData = jsonObject.getJSONObject("channel");
            episode.setChannelTitle(channelData.getString("title"));
            episode.setChannelArtworkUrl(channelData.getString("artworkUrl"));
            episode.setChannelServerId(channelData.getString("id"));
            episode.setChannelAuthor(channelData.getString("author"));
        } else {
            episode.setChannelServerId(jsonObject.getString(ApiManager.CHANNEL_ID));
        }
        return episode;
    }

    public static void searchServerAsync(final Context context, final String query, final int page,
                                         final LoadListCallback<Channel> listener) {

        // first load the channel map so we can tell whether or not the user is already subscribed
        ChannelModel.getChannelMapAsync(context, channelMap -> new AsyncTask<Void, Void, List<Channel>>() {

            @Override
            protected List<Channel> doInBackground(Void... params) {
                return searchServer(context, query, page);
            }

            @Override
            protected void onPostExecute(List<Channel> channelList) {

                if (channelList == null) {
                    channelList = new ArrayList<>(0);
                }

                for (int i = 0; i < channelList.size(); i++) {

                    if (channelMap.containsKey(channelList.get(i).getServerId())) {
                        channelList.get(i).setId(
                                channelMap.get(channelList.get(i).getServerId()).getId());
                    }
                }

                listener.onListLoaded(channelList);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
    }

    private static List<Channel> searchServer(Context context, String query, int page) {
        List<Channel> results = null;
        ApiManager apiManager = ApiManager.getInstance(context);
        User user = User.load(context);

        try {
            results = apiManager.search(user.getCredential(), query, page);
        } catch (ApiException e) {
            Log.e(TAG, "Error in doInBackground");
            Log.e(TAG, e.toString());
        }
        return results;
    }

    public static List<Episode> getRemoteEpisodesByChannelOlderThan(Context context, Channel channel,
                                                                    long publishedBefore) {
        List<Episode> episodes = new ArrayList<>(0);
        User user = User.load(context);

        try {
            Log.d(TAG, String.format("Getting episodes for channel %s", channel.getServerId()));
            episodes = ApiManager.getInstance(context).getEpisodes(user.getCredential(),
                    channel.getServerId(), publishedBefore, channel.isSubscribed());
        } catch (ApiException e) {
            Log.w(TAG, "Error retrieving episodes", e);
        }
        return episodes;
    }

    public static List<Episode> getRemoteEpisodesByChannelNewerThan(Context context, Channel channel,
                                                                    long publishedBefore) {
        List<Episode> episodes = new ArrayList<>(0);
        User user = User.load(context);

        try {
            Log.d(TAG, String.format("Getting episodes for channel %s", channel.getServerId()));
            episodes = ApiManager.getInstance(context).getChannelEpisodesNewerThan(user.getCredential(),
                    channel.getServerId(), publishedBefore);
        } catch (ApiException e) {
            Log.w(TAG, "Error retrieving episodes", e);
        }
        return episodes;
    }

    public static void getTrendingChannelsAsync(final Context context, final LoadListCallback<Channel> listener) {

        // first load the channel map so we can tell whether or not the user is already subscribed
        ChannelModel.getChannelMapAsync(context, channelMap -> new AsyncTask<Void, Void, List<Channel>>() {

            @Override
            protected List<Channel> doInBackground(Void... params) {
                List<Channel> channelList = new ArrayList<>(0);
                User user = User.load(context);
                ApiManager apiManager = ApiManager.getInstance(context);

                try {
                    channelList = apiManager.getTrendingChannels(user.getCredential());
                } catch (ApiException e) {
                    Log.e(TAG, "Error occurred GetChannelFromServerTask");
                    Log.e(TAG, e.toString());
                }
                return channelList;
            }

            @Override
            protected void onPostExecute(List<Channel> channels) {
                listener.onListLoaded(channels);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
    }

    public static void getTrendingEpisodesAsync(final Context context, final LoadListCallback<Episode> listener) {

        new AsyncTask<Void, Void, List<Episode>>() {

            @Override
            protected List<Episode> doInBackground(Void... params) {
                List<Episode> episodeList = new ArrayList<>(0);
                User user = User.load(context);
                ApiManager apiManager = ApiManager.getInstance(context);

                try {
                    episodeList = apiManager.getTrendingEpisodes(user.getCredential());
                } catch (ApiException e) {
                    Log.e(TAG, "Error occurred getTrendingEpisodesAsync", e);
                }
                return episodeList;
            }

            @Override
            protected void onPostExecute(List<Episode> episodes) {
                listener.onListLoaded(episodes);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getTopChannelsAsync(final Context context, final LoadListCallback<Channel> listener) {

        // first load the channel map so we can tell whether or not the user is already subscribed
        ChannelModel.getChannelMapAsync(context, channelMap -> new AsyncTask<Void, Void, List<Channel>>() {

            @Override
            protected List<Channel> doInBackground(Void... params) {
                List<Channel> channelList = new ArrayList<>(0);
                User user = User.load(context);
                ApiManager apiManager = ApiManager.getInstance(context);

                try {
                    channelList = apiManager.getTopChannels(user.getCredential());
                } catch (ApiException e) {
                    Log.e(TAG, "Error occurred GetChannelFromServerTask");
                    Log.e(TAG, e.toString());
                }
                return channelList;
            }

            @Override
            protected void onPostExecute(List<Channel> channels) {
                listener.onListLoaded(channels);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
    }

    public static void getChannelAsync(final Context context, final String channelServerId,
                                       final LoadCallback<Channel> listener) {

        new AsyncTask<Void, Void, Channel>() {

            @Override
            protected Channel doInBackground(Void... params) {
                return getChannel(context, channelServerId);
            }

            @Override
            protected void onPostExecute(Channel channel) {
                listener.onLoaded(channel);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    public static Channel getChannel(Context context, String channelServerId) {
        Channel channel = null;

        try {
            channel = ApiManager.getInstance(context).getChannel(
                    User.load(context).getCredential(),
                    channelServerId);
        } catch (ApiException e) {
            Log.e(TAG, "Error occurred getChannel", e);
        }
        return channel;
    }

    private static List<Channel> getChannelsByCategory(Context context, String category, int page) {
        List<Channel> channelList = null;
        User user = User.load(context);
        ApiManager apiManager = ApiManager.getInstance(context);

        try {
            channelList = apiManager.getChannelsByCategory(user.getCredential(), category, page);
        } catch (ApiException e) {
            Log.e(TAG, "Error occurred getChannelsByCategory", e);
            Log.e(TAG, e.toString());
        }
        return channelList;
    }

    public static void getChannelsByCategoryAsync(final Context context, final String category,
                                            final int page, final LoadListCallback<Channel> listener) {

        // first load the channel map so we can tell whether or not the user is already subscribed
        ChannelModel.getChannelMapAsync(context, channelMap -> new AsyncTask<Void, Void, List<Channel>>() {

            @Override
            protected List<Channel> doInBackground(Void... params) {
                return getChannelsByCategory(context, category, page);
            }

            @Override
            protected void onPostExecute(List<Channel> channelList) {

                if (channelList == null) {
                    channelList = new ArrayList<>(0);
                }

                for (int i = 0; i < channelList.size(); i++) {

                    if (channelMap.containsKey(channelList.get(i).getServerId())) {
                        channelList.get(i).setId(
                                channelMap.get(channelList.get(i).getServerId()).getId());
                    }
                }

                listener.onListLoaded(channelList);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
    }

    private static List<Category> getCategories(Context context) {
        List<Category> categoryList = null;
        User user = User.load(context);
        ApiManager apiManager = ApiManager.getInstance(context);

        try {
            categoryList = apiManager.getCategories(user.getCredential());
        } catch (ApiException e) {
            Log.e(TAG, "Error occurred getCategories");
            Log.e(TAG, e.toString());
        }
        return categoryList;
    }

    public static void getCategoriesAsync(final Context context, final LoadListCallback<Category> listener) {

        new AsyncTask<Void, Void, List<Category>>() {

            @Override
            protected List<Category> doInBackground(Void... params) {
                return getCategories(context);
            }

            @Override
            protected void onPostExecute(List<Category> categoryList) {
                listener.onListLoaded(categoryList);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    public static boolean addPurchase(Context context, String productId, String orderId,
                                      String developerPayload, String signature, String signedData) {
        User user = User.load(context);
        ApiManager apiManager = ApiManager.getInstance(context);
        boolean purchaseAdded = false;

        try {
            PostApiResponse response = apiManager.addPurchase(user.getCredential(), productId,
                    orderId, developerPayload, signature, signedData);
            purchaseAdded = response.isSuccessful();
        } catch (ApiException e) {
            Log.e(TAG, "Error occurred getCategories");
            Log.e(TAG, e.toString());
        }
        return purchaseAdded;
    }

    public static void addPurchaseAsync(final Context context, final String productId,
                                        final String orderId, final String developerPayload,
                                        final String signature, final String signedData,
                                        final OnPurchaseAddedListener purchaseAddedListener) {

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return addPurchase(context, productId, orderId, developerPayload, signature,
                        signedData);
            }

            @Override
            protected void onPostExecute(Boolean purchaseAdded) {
                purchaseAddedListener.onPurchaseAdded(purchaseAdded);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getChannelByItunesIdAsync(final Context context, final String iTunesId,
                                                 final LoadCallback<Channel> callback) {
        Observable.create(
            (Observable.OnSubscribe<Channel>) subscriber -> {
                    User user = User.load(context);
                    Channel channel = null;

                    try {
                        channel = ApiManager.getInstance(context).getChannelByITunesId(
                                user.getCredential(), iTunesId);
                    } catch (ApiException e) {
                        subscriber.onError(e);
                    }
                    subscriber.onNext(channel);
                }
            )
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Channel>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "Error getting channel", e);
                    callback.onLoaded(null);
                }

                @Override
                public void onNext(Channel channel) {
                    callback.onLoaded(channel);
                }
            });
    }

    public interface OnToggleFavoriteEpisodeListener {
        void onFavoriteToggled(boolean isFavorite);
    }

    public interface OnPurchaseAddedListener {
        void onPurchaseAdded(boolean purchaseAdded);
    }
}