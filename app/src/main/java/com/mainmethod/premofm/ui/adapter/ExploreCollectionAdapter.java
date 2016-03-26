/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.object.Collection;

import java.util.List;

/**
 * Created by evan on 6/5/15.
 */
public class ExploreCollectionAdapter extends RecyclerView.Adapter<ExploreCollectionAdapter.ExploreCollectionViewHolder> {

    private Context mContext;
    private List<Collection> mCollectionList;

    public ExploreCollectionAdapter(Context context) {
        mContext = context;
    }

    public void setCollections(List<Collection> channelList) {
        mCollectionList = channelList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public void onBindViewHolder(final ExploreCollectionViewHolder viewHolder, int position) {

    }


    @Override
    public ExploreCollectionViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_explore_collection, viewGroup, false);
        ExploreCollectionViewHolder viewHolder = new ExploreCollectionViewHolder(itemView);
        return viewHolder;
    }

    /**
     * Created by evan on 1/4/15.
     */
    static class ExploreCollectionViewHolder extends RecyclerView.ViewHolder {

        public TextView comingSoon;

        public ExploreCollectionViewHolder(View view) {
            super(view);
        }
    }
}
