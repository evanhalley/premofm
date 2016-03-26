/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.media.session.PlaybackState;
import android.os.Bundle;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.ui.view.MiniPlayer;

/**
 * An activity that has a mini player
 * Created by evan on 8/8/15.
 */
public abstract class MiniPlayerActivity
        extends PlayableActivity {

    // mini player views
    private MiniPlayer mMiniPlayer;

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        setupMiniPlayer();
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
}