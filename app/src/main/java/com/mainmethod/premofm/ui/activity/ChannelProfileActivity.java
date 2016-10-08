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
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.helper.PaletteHelper;
import com.mainmethod.premofm.helper.PaletteHelper.OnPaletteLoaded;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.AsyncTaskService;
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

    private Channel mChannel;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ImageView mChannelArt;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private boolean mVisitingFromExplore;
    private EpisodeAdapter mAdapter;
    private SubscriptionBroadcastReceiver mSubscriptionBroadcastReceiver;

    public static void openChannelProfile(BaseActivity activity, Channel channel, View channelArt,
                                          boolean visitingFromExplore) {
        Bundle args = new Bundle();
        args.putBoolean(PARAM_VISITING_FROM_EXPLORE, visitingFromExplore);
        args.putParcelable(ChannelProfileActivity.PARAM_CHANNEL, Parcels.wrap(channel));
        args.putLong(PARAM_NUM_STORED_EPISODES,
                EpisodeModel.getNumberOfEpisodesForChannel(activity, channel.getGeneratedId()));
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
            int channelId = ChannelModel.channelIsSubscribed(this, mChannel.getGeneratedId());

            if (channelId != -1) {
                mChannel.setId(channelId);
            }
        }
        setupHeader();
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
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSubscriptionBroadcastReceiver = new SubscriptionBroadcastReceiver(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mSubscriptionBroadcastReceiver,
                new IntentFilter(BroadcastHelper.INTENT_SUBSCRIPTION_CHANGE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSubscriptionBroadcastReceiver);
        mRecyclerView.clearOnScrollListeners();
        mSubscriptionBroadcastReceiver = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overrideTransition();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!mChannel.isSubscribed()) {
            AsyncTaskService.deleteChannel(this, mChannel.getGeneratedId());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        UserPrefHelper prefHelper = UserPrefHelper.get(this);
        MenuItem downloadItem = menu.findItem(R.id.action_enable_auto_download);
        MenuItem notifyItem = menu.findItem(R.id.action_enable_notifications);

        if (mChannel.isSubscribed()) {
            downloadItem.setChecked(prefHelper.isServerIdAdded(R.string.pref_key_auto_download_channels,
                    mChannel.getGeneratedId()));
            notifyItem.setChecked(prefHelper.isServerIdAdded(R.string.pref_key_notification_channels,
                    mChannel.getGeneratedId()));
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
                startChannelShare(mChannel);
                return true;
            case R.id.action_mark_all_channels_completed:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.dialog_mark_channel_completed)
                        .setPositiveButton(R.string.dialog_mark_completed, (dialog, which) -> {
                            EpisodeModel.markEpisodesCompletedByChannelAsync(ChannelProfileActivity.this,
                                    mChannel.getGeneratedId());
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
                return true;
            case R.id.action_enable_auto_download:

                if (item.isChecked()) {
                    item.setChecked(false);
                    UserPrefHelper.get(this).removeServerId(R.string.pref_key_auto_download_channels,
                            mChannel.getGeneratedId());
                } else {
                    item.setChecked(true);
                    UserPrefHelper.get(this).addGeneratedId(R.string.pref_key_auto_download_channels,
                            mChannel.getGeneratedId());
                }
                return true;
            case R.id.action_enable_notifications:

                if (item.isChecked()) {
                    item.setChecked(false);
                    UserPrefHelper.get(this).removeServerId(R.string.pref_key_notification_channels,
                            mChannel.getGeneratedId());
                } else {
                    item.setChecked(true);
                    UserPrefHelper.get(this).addGeneratedId(R.string.pref_key_notification_channels,
                            mChannel.getGeneratedId());
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
        return R.menu.channel_profile_activity;
    }

    @Override
    protected void onPodcastPlayerServiceBound() {

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
                        activity.mChannel.getGeneratedId(), bitmap, new PaletteLoaded(activity));
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

    private static class PaletteLoaded implements OnPaletteLoaded {

        private WeakReference<ChannelProfileActivity> mActivity;

        public PaletteLoaded(ChannelProfileActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void loaded(int primaryColor, int textColor) {

            if (primaryColor == -1 || textColor == -1) {
                return;
            }

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
                activity.mChannel = ChannelModel.getChannelByGeneratedId(activity,
                        activity.mChannel.getGeneratedId());
            } else {
                activity.mChannel.setId(-1);
                activity.mChannel.setSubscribed(false);
            }
            activity.mAdapter.setChannel(activity.mChannel);
            activity.mAdapter.notifyDataSetChanged();
        }
    }
}