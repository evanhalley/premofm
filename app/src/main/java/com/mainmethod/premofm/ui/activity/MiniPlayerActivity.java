/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.ui.view.MiniPlayer;

import java.lang.ref.WeakReference;
import java.util.Date;

import timber.log.Timber;

/**
 * An activity that has a mini player
 * Created by evan on 8/8/15.
 */
public abstract class MiniPlayerActivity
        extends PlayableActivity {

    // mini player views
    private MiniPlayer mMiniPlayer;
    private ProgressUpdateReceiver mProgressUpdateReceiver;

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        setupMiniPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mProgressUpdateReceiver = new ProgressUpdateReceiver(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mProgressUpdateReceiver,
                new IntentFilter(BroadcastHelper.INTENT_PROGRESS_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mProgressUpdateReceiver);
    }

    @Override
    public void onStateChanged(int state, Episode episode) {

        if (mMiniPlayer == null) {
            return;
        }

        if (episode != null && state != PlaybackState.STATE_STOPPED) {
            mMiniPlayer.setEpisode(episode, state);
        } else {
            mMiniPlayer.clearEpisode();
        }
    }

    /**
     * Configures the mini player
     */
    private void setupMiniPlayer() {
        mMiniPlayer = new MiniPlayer(findViewById(R.id.mini_player), this);
    }

    private static class ProgressUpdateReceiver extends BroadcastReceiver {
        private static final long UPDATE_INTERVAL_MS = 15_000;
        private final WeakReference<MiniPlayerActivity> mActivity;
        private long mLastUpdate = -1;

        public ProgressUpdateReceiver(MiniPlayerActivity context) {
            mActivity = new WeakReference<>(context);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MiniPlayerActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }
            long timestamp = DatetimeHelper.getTimestamp();

            if (mLastUpdate == -1 || (timestamp - mLastUpdate) > UPDATE_INTERVAL_MS) {
                Timber.d("Updating mini player time left");
                mLastUpdate = timestamp;
                mActivity.get().mMiniPlayer.setDuration(
                        intent.getLongExtra(BroadcastHelper.EXTRA_DURATION, 0L),
                        intent.getLongExtra(BroadcastHelper.EXTRA_PROGRESS, 0L));
            }
        }
    }
}