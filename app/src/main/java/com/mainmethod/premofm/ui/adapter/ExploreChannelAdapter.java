/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.adapter;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.service.ApiService;
import com.mainmethod.premofm.ui.activity.BaseActivity;
import com.mainmethod.premofm.ui.activity.ChannelProfileActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by evan on 6/5/15.
 */
public class ExploreChannelAdapter extends RecyclerView.Adapter<ExploreChannelAdapter.ExploreChannelViewHolder> {

    private final List<Channel> mChannelList;

    public ExploreChannelAdapter() {
        mChannelList = new ArrayList<>(0);
    }

    public List<Channel> getChannels() {
        return mChannelList;
    }

    public void addChannels(List<Channel> channelList) {
        mChannelList.addAll(channelList);
        notifyDataSetChanged();
    }

    public void removeChannels() {
        int size = mChannelList.size();
        mChannelList.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public int getItemCount() {
        return mChannelList.size();
    }

    @Override
    public void onBindViewHolder(final ExploreChannelViewHolder viewHolder, int position) {
        final Channel channel = mChannelList.get(position);
        ImageLoadHelper.loadImageIntoView(viewHolder.channelArt.getContext(),
                channel.getArtworkUrl(), viewHolder.channelArt);
        viewHolder.channelTitle.setText(channel.getTitle());
        viewHolder.channelAuthor.setText(channel.getAuthor());

        if (TextUtils.isEmpty(channel.getDescription()) || channel.getDescription().contentEquals("null")) {
            viewHolder.channelDescription.setVisibility(View.GONE);
        } else {
            viewHolder.channelDescription.setVisibility(View.VISIBLE);
            viewHolder.channelDescription.setText(channel.getDescription());
        }

        viewHolder.setOnClickListener(v -> ChannelProfileActivity.openChannelProfile((BaseActivity) v.getContext(), channel,
                viewHolder.channelArt, true));
        viewHolder.setOnSubscribeClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(ApiService.PARAM_CHANNEL_SERVER_ID, channel.getServerId());

            if (!channel.isSubscribed()) {
                ApiService.start(v.getContext(),
                        ApiService.ACTION_SUBSCRIBE_CHANNEL, bundle);
            } else {
                ApiService.start(v.getContext(),
                        ApiService.ACTION_UNSUBSCRIBE_CHANNEL, bundle);
            }
        });
    }

    @Override
    public ExploreChannelViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_explore_channel, viewGroup, false);
        return new ExploreChannelViewHolder(itemView);
    }

    /**
     * Created by evan on 1/4/15.
     */
    static class ExploreChannelViewHolder extends RecyclerView.ViewHolder {

        TextView channelTitle;
        TextView channelAuthor;
        TextView channelDescription;
        ImageView channelArt;

        public ExploreChannelViewHolder(View view) {
            super(view);
            channelArt = (ImageView) view.findViewById(R.id.channel_art);
            channelTitle = (TextView) view.findViewById(R.id.primary_title);
            channelAuthor = (TextView) view.findViewById(R.id.secondary_title);
            channelDescription = (TextView) view.findViewById(R.id.description);
        }

        public void setOnClickListener(View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
        }

        public void setOnSubscribeClickListener(View.OnClickListener listener) {
            //subscribe.setOnClickListener(listener);
        }
    }
}
