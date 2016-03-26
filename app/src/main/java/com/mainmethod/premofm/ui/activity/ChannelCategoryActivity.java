/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.api.ApiHelper;
import com.mainmethod.premofm.data.LoadListCallback;
import com.mainmethod.premofm.object.Category;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.ui.adapter.ExploreChannelAdapter;
import com.mainmethod.premofm.ui.view.ListScrollListener;

import java.util.List;

/**
 * Created by evan on 10/4/15.
 */
public class ChannelCategoryActivity extends MiniPlayerActivity implements ListScrollListener.OnLoadMoreListener {

    private static final int MODE_CATEGORIES = 1;
    private static final int MODE_CHANNELS = 2;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private ListScrollListener mListScrollListener;
    private int mMode = MODE_CATEGORIES;
    private int mResultsPage = 0;
    private String mCategory;

    public static void start(Context context) {
        Intent intent = new Intent(context, ChannelCategoryActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_channel_list;
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.menu_channel_category_activity;
    }

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        super.onCreateBase(savedInstanceState);
        setHomeAsUpEnabled(true);
        mRecyclerView = (RecyclerView) findViewById(R.id.channel_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addOnScrollListener(new ListScrollListener(this));
        mAdapter = new ExploreChannelAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mListScrollListener = new ListScrollListener(this);
        populateList();
    }

    @Override
    public void load() {
        if (mMode == MODE_CHANNELS) {
            retrieveChannels();
        }
    }

    @Override
    protected void onPodcastPlayerServiceBound() {

    }

    private void populateList() {
        switch (mMode) {
            case MODE_CATEGORIES:
                mRecyclerView.removeOnScrollListener(mListScrollListener);
                ApiHelper.getCategoriesAsync(this, categoryList -> {
                    mResultsPage = 0;
                    mAdapter = new ExploreCategoryAdapter(categoryList);
                    mRecyclerView.setAdapter(mAdapter);
                });
                break;
            case MODE_CHANNELS:
                retrieveChannels();
                break;
        }
    }

    public void retrieveChannels() {

        if (mCategory == null) {
            return;
        }
        getSupportActionBar().setSubtitle(mCategory);

        ApiHelper.getChannelsByCategoryAsync(this, mCategory, mResultsPage,
                new LoadListCallback<Channel>() {
                    @Override
                    public void onListLoaded(List<Channel> channelList) {

                        if (channelList == null) {
                            return;
                        }

                        if (channelList.size() > 0) {
                            mResultsPage++;
                        }

                        if (!(mAdapter instanceof ExploreChannelAdapter)) {
                            mAdapter = new ExploreChannelAdapter();
                            mRecyclerView.setAdapter(mAdapter);
                            mRecyclerView.addOnScrollListener(mListScrollListener);
                        }
                        ((ExploreChannelAdapter) mAdapter).addChannels(channelList);
                    }
                });
    }

    @Override
    protected int getHomeContentDescriptionResId() {
        return R.string.back;
    }

    @Override
    public void onBackPressed() {

        if (mMode == MODE_CHANNELS) {
            mMode = MODE_CATEGORIES;
            populateList();
            getSupportActionBar().setSubtitle(null);
        } else {
            finish();
        }
    }

    private class ExploreCategoryAdapter extends RecyclerView.Adapter<ExploreCategoryViewHolder> {

        private List<Category> mCategories;

        public ExploreCategoryAdapter(List<Category> categories) {
            mCategories = categories;
        }

        @Override
        public ExploreCategoryViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_category, viewGroup, false);
            ExploreCategoryViewHolder viewHolder = new ExploreCategoryViewHolder(itemView);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ExploreCategoryViewHolder holder, final int position) {
            holder.categoryText.setText(mCategories.get(position).getName());
            holder.categoryIcon.setImageResource(getCategoryIconResId(mCategories.get(position)));
            holder.setOnClickListener(v -> {
                mMode = MODE_CHANNELS;
                mCategory = mCategories.get(position).getName();
                populateList();
            });
        }

        private int getCategoryIconResId(Category category) {
            int iconResId = R.drawable.ic_add;

            switch (category.getId()) {
                case Category.CATEGORY_ID_ART:
                    iconResId = R.drawable.ic_category_art;
                    break;
                case Category.CATEGORY_ID_BUSINESS:
                    iconResId = R.drawable.ic_category_business;
                    break;
                case Category.CATEGORY_ID_COMEDY:
                    iconResId = R.drawable.ic_category_comedy;
                    break;
                case Category.CATEGORY_ID_EDUCATION:
                    iconResId = R.drawable.ic_category_education;
                    break;
                case Category.CATEGORY_ID_GAMES:
                    iconResId = R.drawable.ic_category_games;
                    break;
                case Category.CATEGORY_ID_GOVERNMENTS:
                    iconResId = R.drawable.ic_category_government;
                    break;
                case Category.CATEGORY_ID_HEALTH:
                    iconResId = R.drawable.ic_category_health;
                    break;
                case Category.CATEGORY_ID_FAMILY:
                    iconResId = R.drawable.ic_category_family_kids;
                    break;
                case Category.CATEGORY_ID_MUSIC:
                    iconResId = R.drawable.ic_category_music;
                    break;
                case Category.CATEGORY_ID_NEWS:
                    iconResId = R.drawable.ic_category_news;
                    break;
                case Category.CATEGORY_ID_SCIENCE:
                    iconResId = R.drawable.ic_category_science;
                    break;
                case Category.CATEGORY_ID_SPIRITUALITY:
                    iconResId = R.drawable.ic_category_spirituality;
                    break;
                case Category.CATEGORY_ID_SPORTS:
                    iconResId = R.drawable.ic_category_sports;
                    break;
                case Category.CATEGORY_ID_SOCIETY:
                    iconResId = R.drawable.ic_category_culture;
                    break;
                case Category.CATEGORY_ID_TV_FILM:
                    iconResId = R.drawable.ic_category_tv_film;
                    break;
                case Category.CATEGORY_ID_TECHNOLOGY:
                    iconResId = R.drawable.ic_category_technology;
                    break;
            }
            return iconResId;
        }

        @Override
        public int getItemCount() {
            return mCategories == null ? 0 : mCategories.size();
        }
    }

    private static class ExploreCategoryViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        TextView categoryText;
        ImageView categoryIcon;

        public ExploreCategoryViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            categoryText = (TextView) itemView.findViewById(R.id.category_text);
            categoryIcon = (ImageView) itemView.findViewById(R.id.category_icon);
        }

        public void setOnClickListener(View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
        }
    }
}
