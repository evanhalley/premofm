/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.object.Filter;
import com.mainmethod.premofm.ui.adapter.EpisodeAdapter;
import com.mainmethod.premofm.ui.dialog.AddFeedDialog;

import org.parceler.Parcels;

/**
 * Created by evan on 6/14/15.
 */
public class EpisodesFragmentPage extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener {

    public static final String PARAM_FILTER = "filter";

    private Filter mFilter;
    private int mLoaderId;
    private EpisodeAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private View mEmptyListView;

    public static Fragment newInstance(Filter filter) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(PARAM_FILTER, Parcels.wrap(filter));
        EpisodesFragmentPage tabFragment = new EpisodesFragmentPage();
        tabFragment.setArguments(bundle);
        return tabFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mFilter = Parcels.unwrap(getArguments().getParcelable(PARAM_FILTER));
        View view = inflater.inflate(R.layout.fragment_episodes_page, container, false);
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
        mEmptyListView = view.findViewById(R.id.empty_list);
        mEmptyListView.findViewById(R.id.button_empty_list).setOnClickListener(this);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.episode_list);
        mRecyclerView.setItemAnimator(itemAnimator);
        mAdapter = new EpisodeAdapter(getActivity(), null, mFilter);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);
        mLoaderId = mFilter.hashCode();
        return view;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.button_empty_list:
                AddFeedDialog.show((AppCompatActivity) getActivity());
                break;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(mLoaderId, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return EpisodeModel.getHomeCursorLoader(getActivity(), mFilter);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor != null && cursor.moveToFirst()) {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyListView.setVisibility(View.INVISIBLE);
            mAdapter.changeCursor(cursor);
        } else {
            // hide the recycler view
            mRecyclerView.setVisibility(View.INVISIBLE);
            mEmptyListView.setVisibility(View.VISIBLE);
            ((TextView) mEmptyListView.findViewById(R.id.empty_list_title)).setText(
                    R.string.no_episodes_title);
            ((TextView) mEmptyListView.findViewById(R.id.empty_list_message)).setText(
                    R.string.no_episodes_message);
            ((Button) mEmptyListView.findViewById(R.id.button_empty_list)).setText(
                    R.string.button_add_feed);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }
}
