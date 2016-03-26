/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.LoadListCallback;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.data.model.PlaylistModel;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.Playlist;
import com.mainmethod.premofm.service.PodcastPlayerService;
import com.mainmethod.premofm.ui.view.ItemTouchHelperAdapter;
import com.mainmethod.premofm.ui.view.SimpleItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allows the user to interact with the playlist
 * Created by evan on 3/9/15.
 */
public class QueueDialogFragment
        extends DialogFragment
        implements DialogInterface.OnClickListener,
        LoadListCallback<Episode> {

    private Episode mEpisode;
    private RecyclerView mRecyclerView;
    private PlaylistAdapter mAdapter;

    private BroadcastReceiver mPlayerStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BroadcastHelper.EXTRA_PLAYER_STATE, -1);
            String episodeServerId = intent.getStringExtra(BroadcastHelper.EXTRA_EPISODE_SERVER_ID);

            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED) {
                mEpisode = EpisodeModel.getEpisodeByServerId(context, episodeServerId);
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    public void setEpisode(Episode episode) {
        mEpisode = episode;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_play_queue, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setPositiveButton(R.string.dialog_save, this);
        builder.setNegativeButton(R.string.dialog_cancel, this);
        Dialog dialog = builder.create();

        mRecyclerView = (RecyclerView) view.findViewById(R.id.queue);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(false);
        PlaylistModel.loadPlaylistEpisodesAsync(getActivity(), this);
        return dialog;
    }

    @Override
    public void onListLoaded(List<Episode> list) {
        mAdapter = new PlaylistAdapter(list);
        mRecyclerView.setAdapter(mAdapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(mAdapter));
        touchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        if (which == AlertDialog.BUTTON_POSITIVE) {
            List<String> episodeServerIds = new ArrayList<>(mAdapter.getItemCount());
            List<Episode> episodes = mAdapter.getItems();

            if (episodes != null) {

                for (Episode episode : episodes) {
                    episodeServerIds.add(episode.getServerId());
                }
            }
            Playlist playlist = new Playlist(episodeServerIds);
            PlaylistModel.savePlaylist(getActivity(), playlist);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mPlayerStateChangeReceiver,
                new IntentFilter(BroadcastHelper.INTENT_PLAYER_STATE_CHANGE));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mPlayerStateChangeReceiver);
    }

    public class PlaylistAdapter extends RecyclerView.Adapter<ViewHolder> implements ItemTouchHelperAdapter {

        private List<Episode> mPlaylist;

        public PlaylistAdapter(List<Episode> playlist) {
            mPlaylist = playlist;
        }

        public List<Episode> getItems() {
            return mPlaylist;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_playlist_episode, parent, false);
           return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Episode episode = mPlaylist.get(position);
            ImageLoadHelper.loadImageIntoView(getActivity(), episode.getArtworkUrl(), holder.channelArt);
            holder.episodeServerId = episode.getServerId();
            holder.episodeTitle.setText(episode.getTitle());
            holder.channelTitle.setText(episode.getChannelTitle());

            if (episode.getServerId().contentEquals(mEpisode.getServerId())) {
                holder.episodeTitle.setTextAppearance(getActivity(), R.style.Playlist_Text_NowPlayingTitle);
                holder.channelTitle.setTextAppearance(getActivity(), R.style.Playlist_Text_NowPlayingTitle);
            } else {
                holder.episodeTitle.setTextAppearance(getActivity(), R.style.Playlist_Text_Title);
                holder.channelTitle.setTextAppearance(getActivity(), R.style.Playlist_Text_Title);
            }
        }

        @Override
        public int getItemCount() {
            return (mPlaylist != null ? mPlaylist.size() : 0);
        }

        @Override
        public void onItemDismiss(int position) {
            mPlaylist.remove(position);
            notifyItemRemoved(position);
        }

        @Override
        public void onItemMove(int fromPosition, int toPosition) {
            Collections.swap(mPlaylist, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public String episodeServerId;
        public TextView episodeTitle;
        public TextView channelTitle;
        public ImageView channelArt;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            episodeTitle = (TextView) itemView.findViewById(R.id.episode_title);
            channelTitle = (TextView) itemView.findViewById(R.id.channel_title);
            channelArt = (ImageView) itemView.findViewById(R.id.channel_art);
        }

        @Override
        public void onClick(View v) {
            Playlist playlist = PlaylistModel.getPlaylist(itemView.getContext());

            if (playlist.moveToEpisode(episodeServerId)) {
                PodcastPlayerService.playPlaylist(itemView.getContext(), playlist);
            }
        }
    }
}
