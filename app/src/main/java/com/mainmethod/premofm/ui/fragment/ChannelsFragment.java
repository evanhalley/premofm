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

/**
 * Created by evan on 12/3/14.
 */
public class ChannelsFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener {

    private static final int NUMBER_COLUMNS_PORTRAIT = 3;

    private RecyclerView mRecyclerView;
    private ChannelAdapter mAdapter;
    private View mEmptyListView;

    /**
     * Creates a new instance of this fragment
     * @return
     */
    public static ChannelsFragment newInstance(Bundle args) {
        ChannelsFragment fragment = new ChannelsFragment();

        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_channels;
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
        mAdapter = new ChannelAdapter(getActivity(), null, screenWidth);
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.button_empty_list:
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
                    R.string.button_start_exploring);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    /**
     * Created by evan on 12/7/14.
     */
    private static class ChannelAdapter extends
            CursorRecyclerViewAdapter<ChannelAdapter.ChannelViewHolder> {

        private Context mContext;
        private final int mScreenWidth;

        public ChannelAdapter(Context context, Cursor cursor, int screenWidth) {
            super(cursor);
            mContext = context;
            mScreenWidth = screenWidth;
        }

        @Override
        public void onBindViewHolder(final ChannelViewHolder viewHolder, Cursor cursor, int position) {
            final Channel channel = ChannelModel.toChannel(cursor);
            ImageLoadHelper.loadImageIntoView(mContext, channel.getArtworkUrl(),
                    viewHolder.channelArt, mScreenWidth, mScreenWidth);
            viewHolder.channelId = channel.getId();
            viewHolder.setOnClickListener(v -> ChannelProfileActivity.openChannelProfile( (BaseActivity) mContext,
                    channel, viewHolder.channelArt, false));
        }

        @Override
        public ChannelViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_channel, viewGroup, false);
            final ChannelViewHolder viewHolder = new ChannelViewHolder(itemView);
            return viewHolder;
        }

        /**
         * Created by evan on 1/4/15.
         */
        public class ChannelViewHolder extends RecyclerView.ViewHolder {

            int channelId;
            ImageView channelArt;

            public ChannelViewHolder(View view) {
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