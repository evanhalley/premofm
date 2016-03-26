/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.api.ApiHelper;
import com.mainmethod.premofm.helper.AnalyticsHelper;
import com.mainmethod.premofm.ui.adapter.ExploreChannelAdapter;
import com.mainmethod.premofm.ui.view.ListScrollListener;

/**
 * Created by evan on 10/4/15.
 */
public class
        ChannelSearchActivity extends MiniPlayerActivity implements SearchView.OnQueryTextListener,
        ListScrollListener.OnLoadMoreListener {

    private RecyclerView mChannelList;
    private View mEmptyListView;
    private String mQuery;
    private ExploreChannelAdapter mAdapter;
    private int mPage;

    public static void start(Context context) {
        Intent intent = new Intent(context, ChannelSearchActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_channel_list;
    }

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        super.onCreateBase(savedInstanceState);
        setHomeAsUpEnabled(true);
        mEmptyListView = findViewById(R.id.empty_list);
        ((Button) mEmptyListView.findViewById(R.id.button_empty_list)).setText(R.string.button_try_again);
        mChannelList = (RecyclerView) findViewById(R.id.channel_list);
        mChannelList.setHasFixedSize(true);
        mChannelList.setLayoutManager(new LinearLayoutManager(this));
        mChannelList.addOnScrollListener(new ListScrollListener(this));
        mAdapter = new ExploreChannelAdapter();
        mChannelList.setAdapter(mAdapter);
    }

    @Override
    protected int getHomeContentDescriptionResId() {
        return R.string.back;
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.menu_search_activity;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mQuery = null;
        mAdapter.removeChannels();
        mPage = 0;

        if (query != null && query.trim().length() > 0) {
            mQuery = query.trim();

            // send the query to analytics
            AnalyticsHelper.sendEvent(this,
                    AnalyticsHelper.CATEGORY_EXPLORE_SEARCH,
                    AnalyticsHelper.ACTION_INPUT,
                    mQuery);

            // hide the keyboard once the query has been submitted
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(mChannelList.getWindowToken(), 0);
            getSearchResults();
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);

        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();

            if (searchView != null) {
                searchItem.setVisible(true);
                searchView.setOnQueryTextListener(this);
                searchView.setIconified(false);
                searchView.findViewById(android.support.v7.appcompat.R.id.search_plate)
                        .setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
                searchView.findViewById(android.support.v7.appcompat.R.id.submit_area)
                        .setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
            }
        }
        return true;
    }

    @Override
    public void load() {

        if (mQuery != null) {
            getSearchResults();
        }
    }

    private void getSearchResults() {
        ApiHelper.searchServerAsync(this, mQuery, mPage, channelList -> {

            if (channelList == null || channelList.size() == 0) {

                // no existing channels in the list, show empty list
                if (mAdapter.getItemCount() == 0) {
                    showEmptyListView(mQuery);
                }
            } else {
                mPage++;
                hideEmptyListView();
                mAdapter.addChannels(channelList);
            }
        });
    }

    @Override
    protected void onPodcastPlayerServiceBound() {

    }

    private void showEmptyListView(String searchQuery) {
        ((TextView) mEmptyListView.findViewById(R.id.empty_list_title)).setText(
                getString(R.string.no_results_found_title));
        ((TextView) mEmptyListView.findViewById(R.id.empty_list_message)).setText(
                String.format(getString(R.string.no_results_found_message), searchQuery));
        mEmptyListView.setVisibility(View.VISIBLE);
    }

    private void hideEmptyListView() {
        mEmptyListView.setVisibility(View.INVISIBLE);
    }
}
