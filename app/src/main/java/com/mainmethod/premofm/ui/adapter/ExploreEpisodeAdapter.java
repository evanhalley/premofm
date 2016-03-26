/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AnalyticsHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.ui.holder.EpisodeHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by evan on 10/17/15.
 */
public class ExploreEpisodeAdapter extends RecyclerView.Adapter<EpisodeHolder>
        implements EpisodeHolder.PinClickListener {

    private final List<Episode> mEpisodeList;
    private final WeakReference<Context> mContext;

    public ExploreEpisodeAdapter(Context context) {
        mContext = new WeakReference<>(context);
        mEpisodeList = new ArrayList<>(0);
    }

    public void addEpisodes(List<Episode> episodes) {
        int start = mEpisodeList.size();
        mEpisodeList.addAll(episodes);
        notifyItemRangeInserted(start, episodes.size());
    }

    public List<Episode> getEpisodes() {
        return mEpisodeList;
    }

    @Override
    public int getItemCount() {
        return mEpisodeList.size();
    }

    @Override
    public EpisodeHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_episode, viewGroup, false);
        return new EpisodeHolder(itemView, null, this);
    }

    @Override
    public void onBindViewHolder(EpisodeHolder holder, int position) {
        Episode episode = mEpisodeList.get(position);
        ImageLoadHelper.loadImageIntoView(holder.channelArt.getContext(),
                episode.getChannelArtworkUrl(), holder.channelArt);
        holder.channelServerId = episode.getChannelServerId();
        holder.channelTitle.setText(episode.getChannelTitle());
        holder.episodeTitle.setText(episode.getTitle());
        holder.duration.setText(DatetimeHelper.convertSecondsToReadableDuration(episode.getDuration()));
        holder.publishedAt.setText(DatetimeHelper.dateToShortReadableString(
                holder.channelArt.getContext(), episode.getPublishedAt()));
        holder.pin.setVisibility(View.VISIBLE);
        holder.favorite.setVisibility(View.GONE);
        holder.downloaded.setVisibility(View.GONE);
        holder.downloadProgress.setVisibility(View.GONE);
        holder.play.setVisibility(View.GONE);
        holder.more.setVisibility(View.GONE);
        holder.pinned.setVisibility(View.GONE);

        if (episode.getDescription() == null || episode.getDescription().contentEquals("null") ||
                episode.getDescription().length() == 0) {
            holder.description.setVisibility(View.GONE);
        } else {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(episode.getDescription());
        }

        if (episode.getDuration() > 0) {
            holder.duration.setVisibility(View.VISIBLE);
            holder.duration.setText(AdapterHelper.buildDurationString(
                    holder.channelArt.getContext(),
                    episode.getEpisodeStatus(),
                    episode.getDuration(),
                    episode.getProgress()));
        } else {
            holder.duration.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPinClick(int index) {
        boolean added;
        Context context = mContext.get();

        if (context == null) {
            return;
        }
        Episode episode = mEpisodeList.get(index);
        Episode cachedEpisode = EpisodeModel.getEpisodeByServerId(context, episode.getServerId());

        if (cachedEpisode == null) {
            episode.setManuallyAdded(true);
            added = EpisodeModel.insertEpisode(context, episode);

            if (added) {
                AnalyticsHelper.sendEvent(context,
                        AnalyticsHelper.CATEGORY_PIN,
                        AnalyticsHelper.ACTION_CLICK,
                        null);
                Toast.makeText(context, R.string.episode_pinned, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.episode_not_pinned, Toast.LENGTH_SHORT).show();
            }
        } else {

            // update episode to manually added if it's on the device, but the channel isn't subscribed
            if (!cachedEpisode.isManuallyAdded() && !cachedEpisode.isChannelSubscribed()) {
                Bundle bundle = new Bundle();
                bundle.putBoolean(EpisodeModel.PARAM_MANUALLY_ADDED, true);
                added = EpisodeModel.updateEpisode(context, cachedEpisode.getId(), bundle);

                if (added) {
                    AnalyticsHelper.sendEvent(context,
                            AnalyticsHelper.CATEGORY_PIN,
                            AnalyticsHelper.ACTION_CLICK,
                            null);
                    Toast.makeText(context, R.string.episode_pinned, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.episode_not_pinned, Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(context, R.string.episode_pinned, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.episode_already_pinned, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
