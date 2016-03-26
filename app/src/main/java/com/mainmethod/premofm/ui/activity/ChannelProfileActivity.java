/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.ColorHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.helper.IntentHelper;
import com.mainmethod.premofm.helper.PaletteHelper;
import com.mainmethod.premofm.helper.PaletteHelper.OnPaletteLoaded;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.ApiService;
import com.mainmethod.premofm.ui.adapter.EpisodeAdapter;

import org.parceler.Parcels;

import java.lang.ref.WeakReference;


/**
 * Displays a channels profile
 * Created by evan on 12/29/14.
 */
public class ChannelProfileActivity
        extends MiniPlayerActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String PARAM_CHANNEL = "channel";
    public static final String PARAM_NUM_STORED_EPISODES = "numberOfStoredEpisodes";
    public static final String PARAM_VISITING_FROM_EXPLORE = "visitingFromExplore";

    private static final int LOADER_ID = ChannelProfileActivity.class.hashCode();
    private static final int VISIBLE_THRESHOLD = 1;

    private Channel mChannel;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ImageView mChannelArt;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private boolean mVisitingFromExplore;

    private boolean mLoadingFromServer;
    private long mOldestTimestamp = DatetimeHelper.getTimestamp();
    private long mNewestTimestamp = -1;
    private boolean mKeepLoadingChannels = true;
    private EpisodeAdapter mAdapter;
    private SubscriptionBroadcastReceiver mSubscriptionBroadcastReceiver;
    private EpisodesLoadedBroadcastReceiver mEpisodesLoadedReceiver;

    public static void openChannelProfile(BaseActivity activity, Channel channel, View channelArt,
                                          boolean visitingFromExplore) {
        Bundle args = new Bundle();
        args.putBoolean(PARAM_VISITING_FROM_EXPLORE, visitingFromExplore);
        args.putParcelable(ChannelProfileActivity.PARAM_CHANNEL, Parcels.wrap(channel));
        args.putLong(PARAM_NUM_STORED_EPISODES,
                EpisodeModel.getNumberOfEpisodesForChannel(activity, channel.getServerId()));
        activity.startPremoActivity(ChannelProfileActivity.class, channelArt,
                "channel_art", -1, args);
    }

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        super.onCreateBase(savedInstanceState);
        setHomeAsUpEnabled(true);
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
        mRecyclerView = (RecyclerView) findViewById(R.id.channel_details);
        mRecyclerView.setItemAnimator(itemAnimator);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        Bundle extras = getIntent().getExtras();
        mChannel = Parcels.unwrap(extras.getParcelable(PARAM_CHANNEL));
        mVisitingFromExplore = extras.getBoolean(PARAM_VISITING_FROM_EXPLORE, false);
        mAdapter = new EpisodeAdapter(this, null, mChannel);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mChannelArt = (ImageView) findViewById(R.id.big_channel_art);

        // channel passed in as unsubscribed, check the db
        if (!mChannel.isSubscribed()) {
            int channelId = ChannelModel.channelIsSubscribed(this, mChannel.getServerId());

            if (channelId != -1) {
                mChannel.setId(channelId);
            }
        }
        setupHeader();

        if (extras.getLong(PARAM_NUM_STORED_EPISODES) == 0) {
            getEpisodesFromServer();
        }
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    private void setupHeader() {
        mCollapsingToolbarLayout.setTitle("    ");
        ImageLoadHelper.loadImageAsync(this, mChannel.getArtworkUrl(),
                new ImageLoadedListener(this));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return EpisodeModel.getChannelProfileCursorLoader(this, mChannel);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int position = cursor.getPosition();

        // get the newest timestampt
        if (mNewestTimestamp == -1) {

            if (cursor.moveToFirst()) {
                Episode episode = EpisodeModel.toEpisode(cursor);
                mNewestTimestamp = episode.getPublishedAt().getTime();
                Bundle args = new Bundle();
                args.putParcelable(ApiService.PARAM_CHANNEL, Parcels.wrap(mChannel));
                args.putLong(ApiService.PARAM_TIMESTAMP, mNewestTimestamp);
                ApiService.start(this, ApiService.ACTION_GET_CHANNEL_EPISODES_NEWER_THAN, args);
            }
        }
        
        // no channels returned and the channel is unsubscribed, get more from the server
        if (cursor.moveToLast()) {
            // get the last episodes published time, use it for the timestamp
            Episode episode = EpisodeModel.toEpisode(cursor);
            mOldestTimestamp = episode.getPublishedAt().getTime();
        }
        cursor.moveToPosition(position);
        mAdapter.changeCursor(cursor);
        mRecyclerView.addOnScrollListener(new ScrollListener(this));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSubscriptionBroadcastReceiver = new SubscriptionBroadcastReceiver(this);
        mEpisodesLoadedReceiver = new EpisodesLoadedBroadcastReceiver(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mSubscriptionBroadcastReceiver,
                new IntentFilter(BroadcastHelper.INTENT_SUBSCRIPTION_CHANGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mEpisodesLoadedReceiver,
                new IntentFilter(BroadcastHelper.INTENT_EPISODES_LOADED_FROM_SERVER));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSubscriptionBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mEpisodesLoadedReceiver);
        mRecyclerView.clearOnScrollListeners();
        mSubscriptionBroadcastReceiver = null;
        mEpisodesLoadedReceiver = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overrideTransition();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        UserPrefHelper prefHelper = UserPrefHelper.get(this);
        MenuItem downloadItem = menu.findItem(R.id.action_enable_auto_download);
        MenuItem notifyItem = menu.findItem(R.id.action_enable_notifications);

        if (mChannel.isSubscribed()) {
            downloadItem.setChecked(prefHelper.isServerIdAdded(R.string.pref_key_auto_download_channels,
                    mChannel.getServerId()));
            notifyItem.setChecked(prefHelper.isServerIdAdded(R.string.pref_key_notification_channels,
                    mChannel.getServerId()));
        } else {
            downloadItem.setVisible(false);
            notifyItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                overrideTransition();
                return true;
            case R.id.action_share_channel:
                IntentHelper.shareChannel(this, mChannel);
                return true;
            case R.id.action_mark_all_channels_completed:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.dialog_mark_channel_completed)
                        .setPositiveButton(R.string.dialog_mark_completed, (dialog, which) -> {
                            EpisodeModel.markEpisodesCompletedByChannelAsync(ChannelProfileActivity.this,
                                    mChannel.getServerId());
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
                return true;
            case R.id.action_enable_auto_download:

                if (item.isChecked()) {
                    item.setChecked(false);
                    UserPrefHelper.get(this).removeServerId(R.string.pref_key_auto_download_channels,
                            mChannel.getServerId());
                } else {
                    item.setChecked(true);
                    UserPrefHelper.get(this).addServerId(R.string.pref_key_auto_download_channels,
                            mChannel.getServerId());
                }
                return true;
            case R.id.action_enable_notifications:

                if (item.isChecked()) {
                    item.setChecked(false);
                    UserPrefHelper.get(this).removeServerId(R.string.pref_key_notification_channels,
                            mChannel.getServerId());
                } else {
                    item.setChecked(true);
                    UserPrefHelper.get(this).addServerId(R.string.pref_key_notification_channels,
                            mChannel.getServerId());
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getHomeContentDescriptionResId() {
        return R.string.back;
    }

    private void overrideTransition() {
        if (mChannel.isSubscribed() || mVisitingFromExplore) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    @Override
    public void onStateChanged(int state, Episode episode) {
        super.onStateChanged(state, episode);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_channel_profile;
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.menu_channel_profile_activity;
    }

    @Override
    protected void onPodcastPlayerServiceBound() {

    }

    private void getEpisodesFromServer() {
        mLoadingFromServer = true;
        Bundle args = new Bundle();
        args.putParcelable(ApiService.PARAM_CHANNEL, Parcels.wrap(mChannel));
        args.putLong(ApiService.PARAM_TIMESTAMP, mOldestTimestamp);
        ApiService.start(ChannelProfileActivity.this,
                ApiService.ACTION_GET_CHANNEL_EPISODES_OLDER_THAN,
                args);
    }

    private static class ImageLoadedListener implements ImageLoadHelper.OnImageLoaded {

        private WeakReference<ChannelProfileActivity> mActivity;

        public ImageLoadedListener(ChannelProfileActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void imageLoaded(Bitmap bitmap) {
            ChannelProfileActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }

            if (bitmap != null) {
                activity.mChannelArt.setImageDrawable(
                        new BitmapDrawable(activity.getResources(), bitmap));

                PaletteHelper.get(activity).getChannelColors(
                        activity.mChannel.getServerId(), bitmap, new PaletteLoaded(activity));
            }
        }

        @Override
        public void imageFailed() {
            ChannelProfileActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }
            activity.mChannelArt.setImageDrawable(
                    new BitmapDrawable(activity.getResources(), BitmapFactory.decodeResource(
                            activity.getResources(), R.drawable.default_channel_art)));
        }
    }

    private static class ScrollListener extends RecyclerView.OnScrollListener {

        private WeakReference<ChannelProfileActivity> mActivity;

        public ScrollListener(ChannelProfileActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            ChannelProfileActivity activity = mActivity.get();

            if(activity == null) {
                return;
            }

            if (!activity.mKeepLoadingChannels) {
                return;
            }

            if (activity.mLoadingFromServer) {
                return;
            }

            int mVisibleItemCount = activity.mRecyclerView.getChildCount();
            int mTotalItemCount = activity.mLayoutManager.getItemCount();
            int mFirstVisibleItem = activity.mLayoutManager.findFirstVisibleItemPosition();

            if (!activity.mLoadingFromServer && ((mTotalItemCount - mVisibleItemCount) <=
                    (mFirstVisibleItem + VISIBLE_THRESHOLD))) {
                // we are at the end, let's get more from the server
                activity.getEpisodesFromServer();
            }
        }
    }

    private static class PaletteLoaded implements OnPaletteLoaded {

        private WeakReference<ChannelProfileActivity> mActivity;

        public PaletteLoaded(ChannelProfileActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void loaded(int primaryColor, int textColor) {
            ChannelProfileActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }
            activity.mCollapsingToolbarLayout.setBackgroundColor(primaryColor);
            activity.mCollapsingToolbarLayout.setContentScrimColor(primaryColor);
            activity.mCollapsingToolbarLayout.setStatusBarScrimColor(ColorHelper.getStatusBarColor(primaryColor));
        }
    }

    private static class SubscriptionBroadcastReceiver extends BroadcastReceiver {

        private WeakReference<ChannelProfileActivity> mActivity;

        public SubscriptionBroadcastReceiver(ChannelProfileActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ChannelProfileActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }
            boolean isSubscribed = intent.getBooleanExtra(BroadcastHelper.EXTRA_IS_SUBSCRIBED, false);

            if (isSubscribed) {
                activity.mChannel = ChannelModel.getChannelByServerId(activity,
                        activity.mChannel.getServerId());
            } else {
                activity.mChannel.setId(-1);
            }
            activity.mAdapter.setChannel(activity.mChannel);
            activity.mAdapter.notifyDataSetChanged();
        }
    }

    private static class EpisodesLoadedBroadcastReceiver extends BroadcastReceiver {

        private WeakReference<ChannelProfileActivity> mActivity;

        public EpisodesLoadedBroadcastReceiver(ChannelProfileActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ChannelProfileActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }

            if (activity.mChannel == null) {
                return;
            }

            if (activity.mChannel.getServerId().contentEquals(intent.getStringExtra(BroadcastHelper.EXTRA_CHANNEL_SERVER_ID))) {
                activity.mLoadingFromServer = false;

                if (intent.getIntExtra(BroadcastHelper.EXTRA_NUMBER_EPISODES_LOADED, 0) == 0) {
                    activity.mKeepLoadingChannels = false;
                }
            }
        }
    }
}