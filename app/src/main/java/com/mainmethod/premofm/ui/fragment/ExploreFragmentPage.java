/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.api.ApiHelper;
import com.mainmethod.premofm.data.LoadListCallback;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.ui.adapter.ExploreChannelAdapter;
import com.mainmethod.premofm.ui.adapter.ExploreCollectionAdapter;
import com.mainmethod.premofm.ui.adapter.ExploreEpisodeAdapter;

import java.util.List;

/**
 * Shows UI in the explore interface
 * Created by evan on 6/2/15.
 */
public class ExploreFragmentPage extends Fragment {

    private static final String PARAM_EXPLORE_FRAGMENT_TYPE = "fragmentType";
    private int mFragmentType;
    private RecyclerView mRecyclerView;

    private LoadListCallback<Episode> mEpisodeLoadCallback = new LoadListCallback<Episode>() {
        @Override
        public void onListLoaded(List<Episode> episodes) {
            ((ExploreEpisodeAdapter) mRecyclerView.getAdapter()).addEpisodes(episodes);
        }
    };

    private LoadListCallback<Channel> mChannelsLoadCalblack = new LoadListCallback<Channel>() {
        @Override
        public void onListLoaded(List<Channel> channels) {
            ((ExploreChannelAdapter) mRecyclerView.getAdapter()).addChannels(channels);
        }
    };

    public static Fragment newInstance(int exploreFragmentType) {
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_EXPLORE_FRAGMENT_TYPE, exploreFragmentType);
        ExploreFragmentPage tabFragment = new ExploreFragmentPage();
        tabFragment.setArguments(bundle);
        return tabFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore_page, container, false);
        mFragmentType = getArguments().getInt(PARAM_EXPLORE_FRAGMENT_TYPE);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.explore_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setItemAnimator(null);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupUi();
    }

    private void setupUi() {

        switch (mFragmentType) {
            case ExploreFragment.EXPLORE_TOP_CHANNELS:
                mRecyclerView.setAdapter(new ExploreChannelAdapter());
                ApiHelper.getTopChannelsAsync(getActivity(), mChannelsLoadCalblack);
                break;
            case ExploreFragment.EXPLORE_TRENDING_CHANNELS:
                mRecyclerView.setAdapter(new ExploreChannelAdapter());
                ApiHelper.getTrendingChannelsAsync(getActivity(), mChannelsLoadCalblack);
                break;
            case ExploreFragment.EXPLORE_TRENDING_EPISODES:
                mRecyclerView.setAdapter(new ExploreEpisodeAdapter(getActivity()));
                ApiHelper.getTrendingEpisodesAsync(getActivity(), mEpisodeLoadCallback);
                break;
            case ExploreFragment.EXPLORE_TOP_COLLECTIONS:
                mRecyclerView.setAdapter(new ExploreCollectionAdapter(getActivity()));
                break;
        }
    }
}