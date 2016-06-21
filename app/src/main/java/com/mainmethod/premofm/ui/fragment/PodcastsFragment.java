/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.fragment;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.ui.activity.BaseActivity;
import com.mainmethod.premofm.ui.activity.ChannelProfileActivity;
import com.mainmethod.premofm.ui.activity.PremoActivity;
import com.mainmethod.premofm.ui.adapter.CursorRecyclerViewAdapter;
import com.mainmethod.premofm.ui.dialog.AddPodcastDialog;

import timber.log.Timber;

/**
 * Created by evan on 12/3/14.
 */
public class PodcastsFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener {

    private static final int NUMBER_COLUMNS_PORTRAIT = 3;

    private RecyclerView mRecyclerView;
    private PodcastAdapter mAdapter;
    private View mEmptyListView;

    /**
     * Creates a new instance of this fragment
     * @return
     */
    public static PodcastsFragment newInstance(Bundle args) {
        PodcastsFragment fragment = new PodcastsFragment();

        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.podcasts_fragment;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_podcasts;
    }

    @Override
    protected int getFragmentTitleResourceId() {
        return R.string.title_fragment_subscriptions;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mEmptyListView = view.findViewById(R.id.empty_list);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.channel_list);
        mEmptyListView.findViewById(R.id.button_empty_list).setOnClickListener(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), NUMBER_COLUMNS_PORTRAIT));
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels / NUMBER_COLUMNS_PORTRAIT;
        mAdapter = new PodcastAdapter(getActivity(), null, screenWidth);
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_add_podcast:
                AddPodcastDialog.show(getBaseActivity());
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.button_empty_list:
                AddPodcastDialog.show(getBaseActivity());
                break;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(ChannelModel.LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ChannelModel.getCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor != null && cursor.moveToFirst()) {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyListView.setVisibility(View.INVISIBLE);

            switch (loader.getId()) {
                case ChannelModel.LOADER_ID:
                    mAdapter.changeCursor(cursor);
                    break;
            }
        } else {
            // hide the recycler view
            mRecyclerView.setVisibility(View.INVISIBLE);
            mEmptyListView.setVisibility(View.VISIBLE);
            ((TextView) mEmptyListView.findViewById(R.id.empty_list_title)).setText(
                    R.string.no_channels_title);
            ((TextView) mEmptyListView.findViewById(R.id.empty_list_message)).setText(
                    R.string.no_channels_message);
            ((Button) mEmptyListView.findViewById(R.id.button_empty_list)).setText(
                    R.string.button_add_podcast);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    /**
     * Created by evan on 12/7/14.
     */
    private static class PodcastAdapter extends
            CursorRecyclerViewAdapter<PodcastAdapter.PodcastViewHolder> {

        private Context mContext;
        private final int mScreenWidth;

        public PodcastAdapter(Context context, Cursor cursor, int screenWidth) {
            super(cursor);
            mContext = context;
            mScreenWidth = screenWidth;
        }

        @Override
        public void onBindViewHolder(final PodcastViewHolder viewHolder, Cursor cursor, int position) {
            final Channel channel = ChannelModel.toChannel(cursor);
            ImageLoadHelper.loadImageIntoView(mContext, channel.getArtworkUrl(),
                    viewHolder.channelArt, mScreenWidth, mScreenWidth);
            viewHolder.channelId = channel.getId();
            viewHolder.setOnClickListener(v -> ChannelProfileActivity.openChannelProfile( (BaseActivity) mContext,
                    channel, viewHolder.channelArt, false));
        }

        @Override
        public PodcastViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_channel, viewGroup, false);
            return new PodcastViewHolder(itemView);
        }

        /**
         * Created by evan on 1/4/15.
         */
        public class PodcastViewHolder extends RecyclerView.ViewHolder {

            int channelId;
            ImageView channelArt;

            public PodcastViewHolder(View view) {
                super(view);
                channelArt = (ImageView) view.findViewById(R.id.channel_art);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mScreenWidth, mScreenWidth);
                channelArt.setLayoutParams(params);
            }

            public void setOnClickListener(View.OnClickListener listener) {
                channelArt.setOnClickListener(listener);
            }
        }
    }
}