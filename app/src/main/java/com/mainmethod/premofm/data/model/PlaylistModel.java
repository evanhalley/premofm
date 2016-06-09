/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.data.model;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.mainmethod.premofm.data.LoadListCallback;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.object.Collection;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.Playlist;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by evan on 11/4/15.
 */
public class PlaylistModel {

    private static final String TAG = PlaylistModel.class.getSimpleName();

    public static void deletePlaylist(Context context) {
        AppPrefHelper.getInstance(context).removePlaylist();
    }

    public static void savePlaylist(Context context, Playlist playlist) {
        Log.d(TAG, "Saving playlist: " + playlist.toString());
        AppPrefHelper.getInstance(context).setPlaylist(playlist.toJsonString());
    }

    public static Playlist getPlaylist(Context context) {
        return new Playlist(AppPrefHelper.getInstance(context).getPlaylist());
    }

    public static void addEpisodeToPlaylist(Context context, String episodeServerId) {
        Playlist playlist = getPlaylist(context);
        playlist.addToEnd(episodeServerId);
        savePlaylist(context, playlist);
    }

    public static Playlist buildPlaylistFromCollection(Context context, int collectionId) {
        Collection collection = CollectionModel.getCollectionById(context, collectionId);
        List<String> episodeServerIds = null;

        // load the episode ids
        switch (collection.getType()) {
            case Collection.COLLECTION_TYPE_CHANNEL:
                episodeServerIds = getCollectionEpisodeServerIds(context, collection);
                break;
            case Collection.COLLECTION_TYPE_EPISODE:
                episodeServerIds = collection.getCollectedServerIds();
                break;
        }

        if (episodeServerIds == null) {
            return new Playlist();
        }
        return new Playlist(episodeServerIds);
    }

    private static List<String> getCollectionEpisodeServerIds(Context context, Collection collection) {
        List<String> episodeServerIds = new ArrayList<>();

        for (String serverId : collection.getCollectedServerIds()) {
            Episode episode = EpisodeModel.getLatestEpisodeByChannelServerId(context, serverId);

            if (episode == null) {
                continue;
            }
            episodeServerIds.add(episode.getGeneratedId());
        }
        return episodeServerIds;
    }

    private static List<Episode> getPlaylistEpisodes(Context context) {
        List<Episode> episodeList = null;
        List<String> episodeIds = getPlaylist(context).getEpisodeServerIds();

        if (episodeIds != null && episodeIds.size() > 0) {
            episodeList = new ArrayList<>(episodeIds.size());

            for (int i = 0; i < episodeIds.size(); i++) {
                Episode episode = EpisodeModel.getEpisodeByServerId(context, episodeIds.get(i));

                if (episode != null) {
                    episodeList.add(episode);
                }
            }
        }
        return episodeList;
    }

    public static void loadPlaylistEpisodesAsync(final Context context,
                                                 final LoadListCallback<Episode> callback) {
        new AsyncTask<Void, Void, List<Episode>>() {


            @Override
            protected List<Episode> doInBackground(Void... params) {
                return getPlaylistEpisodes(context);
            }

            @Override
            protected void onPostExecute(List<Episode> episodes) {
                callback.onListLoaded(episodes);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
