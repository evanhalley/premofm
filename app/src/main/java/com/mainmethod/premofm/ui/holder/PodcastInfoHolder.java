/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mainmethod.premofm.R;

/**
 * Created by evan on 8/20/15.
 */
public class PodcastInfoHolder extends RecyclerView.ViewHolder {

    public TextView title;
    public TextView description;
    public TextView author;
    public Button subscribe;
    public Button website;
    public EpisodeHolder episodeHolder;

    public PodcastInfoHolder(View itemView, EpisodeHolder.PinClickListener listener) {
        super(itemView);
        title = (TextView) itemView.findViewById(R.id.channel_title);
        description = (TextView) itemView.findViewById(R.id.channel_description);
        website = (Button) itemView.findViewById(R.id.website);
        author = (TextView) itemView.findViewById(R.id.channel_author);
        subscribe = (Button) itemView.findViewById(R.id.subscribe);
        episodeHolder = new EpisodeHolder(itemView, null, listener);
    }
}
