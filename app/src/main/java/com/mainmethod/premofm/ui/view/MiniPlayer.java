/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.view;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.session.PlaybackState;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.ColorHelper;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.helper.PaletteHelper;
import com.mainmethod.premofm.helper.PlaybackButtonHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.ui.activity.NowPlayingActivity;
import com.mainmethod.premofm.ui.activity.PlayableActivity;

import java.lang.ref.WeakReference;

/**
 * Mini player is the UI widget shown at the bottom of a MiniPlayerActivity
 * Created by evan on 9/18/15.
 */
public class MiniPlayer implements View.OnClickListener, View.OnTouchListener {

    private final WeakReference<PlayableActivity> mActivity;

    private Episode mEpisode;
    private View mMiniPlayer;
    private ImageView mChannelArt;
    private TextView mEpisodeTitle;
    private TextView mChannelTitle;
    private ImageButton mPlay;
    private int mPrimaryColor;
    private int mTextColor;

    public MiniPlayer(View miniPlayer, PlayableActivity baseActivity) {
        mMiniPlayer = miniPlayer;
        mActivity = new WeakReference<>(baseActivity);
        initMiniPlayer();
    }

    private void initMiniPlayer() {
        mMiniPlayer = mMiniPlayer.findViewById(R.id.mini_player);
        mEpisodeTitle = (TextView) mMiniPlayer.findViewById(R.id.episode_title);
        mChannelTitle = (TextView) mMiniPlayer.findViewById(R.id.channel_title);
        mChannelArt = (ImageView) mMiniPlayer.findViewById(R.id.channel_art);
        mPlay = (ImageButton) mMiniPlayer.findViewById(R.id.play);
        mPlay.setOnClickListener(this);
        mMiniPlayer.setOnClickListener(this);
        mMiniPlayer.setOnTouchListener(this);
    }

    private void startNowPlayingActivity() {
        PlayableActivity activity = mActivity.get();

        if (activity == null) {
            return;
        }
        NowPlayingActivity.start(activity, mPrimaryColor, mTextColor);
    }

    @Override
    public void onClick(View v) {
        PlayableActivity activity = mActivity.get();

        if (activity == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.play:
                int state = (int) v.getTag();

                switch (state) {
                    case PlaybackState.STATE_PLAYING:
                        activity.onPauseEpisode();
                        break;
                    case PlaybackState.STATE_PAUSED:
                        activity.onResumePlayback();
                        break;
                    case PlaybackState.STATE_STOPPED:
                    case PlaybackState.STATE_NONE:
                    default:
                        activity.onPlayEpisode();
                        break;
                }
                break;
            case R.id.mini_player:
                startNowPlayingActivity();
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);

        if (action == MotionEvent.ACTION_UP) {
            startNowPlayingActivity();
            return true;
        }
        return false;
    }

    public void setEpisode(Episode episode, int state) {
        mEpisode = episode;
        mPlay.setImageResource(PlaybackButtonHelper.getPlayerPlaybackButtonResId(state));
        mPlay.setTag (state);
        mEpisodeTitle.setText(episode.getTitle());
        mChannelTitle.setText(episode.getChannelTitle());

        // load image into the channel art view
        ImageLoadHelper.loadImageIntoView(mMiniPlayer.getContext(), episode.getChannelArtworkUrl(),
                mChannelArt);

        // load image so that we can extract colors from it
        ImageLoadHelper.loadImageAsync(mMiniPlayer.getContext(), mEpisode.getChannelArtworkUrl(),
                new ImageLoadedListener(this));
        showMiniPlayer();
    }

    public void clearEpisode() {
        hideMiniPlayer();
    }

    public void applyTheme(int primaryColor, int textColor) {
        mPrimaryColor = primaryColor;
        mTextColor = textColor;
        mMiniPlayer.setBackgroundColor(primaryColor);
        mEpisodeTitle.setTextColor(ColorHelper.getTextColor(primaryColor));
        mChannelTitle.setTextColor(ColorHelper.getTextColor(primaryColor));
        mPlay.setImageTintList(ColorStateList.valueOf(ColorHelper.getTextColor(primaryColor)));
    }

    public void hideMiniPlayer() {
        if (mMiniPlayer.getVisibility() != View.GONE) {
            Animation slideDown = AnimationUtils.loadAnimation(mMiniPlayer.getContext(),
                    R.anim.mini_player_slide_down);
            mMiniPlayer.startAnimation(slideDown);
            mMiniPlayer.setVisibility(View.GONE);
        }
    }

    public void showMiniPlayer() {
        if (mMiniPlayer.getVisibility() != View.VISIBLE) {
            Animation slideUp = AnimationUtils.loadAnimation(mMiniPlayer.getContext(),
                    R.anim.mini_player_slide_up);
            mMiniPlayer.startAnimation(slideUp);
            mMiniPlayer.setVisibility(View.VISIBLE);
        }
    }

    private static class ImageLoadedListener implements  ImageLoadHelper.OnImageLoaded {

        private WeakReference<MiniPlayer> mMiniPlayer;

        public ImageLoadedListener(MiniPlayer miniPlayer) {
            mMiniPlayer = new WeakReference<>(miniPlayer);
        }

        @Override
        public void imageLoaded(Bitmap bitmap) {

            if (bitmap != null) {
                mMiniPlayer.get().mChannelArt.setImageBitmap(bitmap);
                PaletteHelper.get(mMiniPlayer.get().mMiniPlayer.getContext()).getChannelColors(
                        mMiniPlayer.get().mEpisode.getChannelGeneratedId(), bitmap,
                        new PaletteLoaded(mMiniPlayer.get()));
            }
        }

        @Override
        public void imageFailed() {
            mMiniPlayer.get().mChannelArt.setImageBitmap(BitmapFactory.decodeResource(
                    mMiniPlayer.get().mMiniPlayer.getContext().getResources(),
                    R.drawable.default_channel_art));
        }
    }

    private static class PaletteLoaded implements PaletteHelper.OnPaletteLoaded {

        private WeakReference<MiniPlayer> mMiniPlayer;

        public PaletteLoaded(MiniPlayer miniPlayer) {
            mMiniPlayer = new WeakReference<>(miniPlayer);
        }

        @Override
        public void loaded(int primaryColor, int textColor) {
            mMiniPlayer.get().applyTheme(primaryColor, textColor);
        }
    }
}
