/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */
package com.mainmethod.premofm.ui.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.ColorHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.helper.IntentHelper;
import com.mainmethod.premofm.helper.MediaHelper;
import com.mainmethod.premofm.helper.NotificationHelper;
import com.mainmethod.premofm.helper.PaletteHelper;
import com.mainmethod.premofm.helper.PlaybackButtonHelper;
import com.mainmethod.premofm.helper.ShowcaseHelper;
import com.mainmethod.premofm.helper.SleepTimerHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.PodcastPlayerService;
import com.mainmethod.premofm.ui.dialog.PlaybackSpeedDialog;
import com.mainmethod.premofm.ui.view.BlurTransformation;

import java.lang.ref.WeakReference;
import java.util.Calendar;

/**
 * Shows the user the now playing user experience
 * Created by evan on 12/16/14.
 */
public class NowPlayingActivity extends PlayableActivity implements
        View.OnClickListener,
        EpisodeModel.OnToggleFavoriteEpisodeListener,
        OnSeekBarChangeListener {

    public static final String PARAM_EPISODE_ID             = "episodeId";
    public static final String PARAM_PRIMARY_COLOR          = "primaryColor";
    public static final String PARAM_TEXT_COLOR             = "textColor";

    private static final float DEGREES_IN_CIRCLE            = 360.0f;
    private static final float MS_IN_RECORD_ROTATION        = 1800.0f;

    private SeekBar mSeekBar;
    private TextView mEpisodeTitle;
    private TextView mChannelTitle;
    private TextView mTimeElapsed;
    private TextView mTimeLeft;
    private ImageButton mPlayButton;
    private ImageButton mFavoriteButton;
    private ImageButton mSeekBackward;
    private ImageButton mSeekForward;
    private ImageButton mInfo;
    private ImageView mStreaming;
    private ImageView mChannelArt;
    private ImageView mBackground;
    private MenuItem mSleepMenuItem;
    private MenuItem mSleepTimerMenuItem;
    private MenuItem mPlaybackSpeedItem;
    private int mPrimaryColor = -1;
    private int mTextColor = -1;
    private Episode mEpisode;
    private ProgressUpdateReceiver mProgressUpdateReceiver;
    private CountDownTimer mCountDownTimer;

    public static void start(BaseActivity activity, int primaryColor, int textColor) {
        Bundle args = new Bundle();
        args.putInt(PARAM_PRIMARY_COLOR, primaryColor);
        args.putInt(PARAM_TEXT_COLOR, textColor);
        activity.startSlideUpPremoActivity(NowPlayingActivity.class, -1, args);
    }

    private final PaletteHelper.OnPaletteLoaded mOnPaletteLoaded = this::themeUI;

    @Override
    protected int getHomeContentDescriptionResId() {
        return R.string.back;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_now_playing;
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.menu_now_playing_activity;
    }

    @Override
    public void onCreateBase(Bundle savedInstanceState) {
        setHomeAsUpEnabled(true);
        mEpisodeTitle = ((TextView) findViewById(R.id.episode_title));
        mChannelTitle = ((TextView) findViewById(R.id.channel_title));
        mTimeLeft = (TextView) findViewById(R.id.time_left);
        mTimeElapsed = (TextView) findViewById(R.id.time_elapsed);
        mChannelArt = (ImageView) findViewById(R.id.channel_art);
        mChannelArt.setOnClickListener(this);
        mPlayButton = (ImageButton) findViewById(R.id.play);
        mPlayButton.setOnClickListener(this);
        mFavoriteButton = (ImageButton) findViewById(R.id.favorite);
        mFavoriteButton.setOnClickListener(this);
        mBackground = (ImageView) findViewById(R.id.play_background);
        mSeekBackward = (ImageButton) findViewById(R.id.seek_backward);
        mSeekBackward.setOnClickListener(this);
        mSeekForward = (ImageButton) findViewById(R.id.seek_forward);
        mSeekForward.setOnClickListener(this);
        mInfo = (ImageButton) findViewById(R.id.episode_info);
        mInfo.setOnClickListener(this);
        mStreaming = (ImageView) findViewById(R.id.streaming);

        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);

        if (getIntent().getIntExtra(PARAM_EPISODE_ID, -1) != -1) {
            PodcastPlayerService.sendIntent(this, PodcastPlayerService.ACTION_PLAY_EPISODE,
                    getIntent().getExtras().getInt(PARAM_EPISODE_ID));
            NotificationHelper.dismissNotification(this,
                    NotificationHelper.NOTIFICATION_ID_NEW_EPISODES);
        }

        mPrimaryColor = getIntent().getIntExtra(PARAM_PRIMARY_COLOR, -1);
        mTextColor = getIntent().getIntExtra(PARAM_TEXT_COLOR, -1);

        if (mPrimaryColor != -1  && mTextColor != -1) {
            themeUI(mPrimaryColor, mTextColor);
        }
        mProgressUpdateReceiver = new ProgressUpdateReceiver(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            onSeekTo(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.channel_art:
            case R.id.play:

                switch (getState()) {
                    case PlaybackState.STATE_PLAYING:
                        onPauseEpisode();
                        break;
                    case PlaybackState.STATE_STOPPED:
                    case PlaybackState.STATE_NONE:
                        onPlayEpisode();
                        break;
                    case PlaybackState.STATE_PAUSED:
                        onResumePlayback();
                        break;
                }
                break;
            case R.id.seek_backward:
                onSeekBackward();
                break;
            case R.id.seek_forward:
                onSeekForward();
                break;
            case R.id.episode_info:
                showEpisodeInformation();
                break;
            case R.id.favorite:
                favoriteEpisode(this);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean hasMenu = super.onCreateOptionsMenu(menu);
        mSleepMenuItem = menu.findItem(R.id.action_sleep);
        mSleepTimerMenuItem = menu.findItem(R.id.action_sleep_timer);
        mPlaybackSpeedItem = menu.findItem(R.id.action_playback_speed);
        updatePlaybackSpeed();
        initSleepTimerUi();
        return hasMenu;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        animateExit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                animateExit();
                return true;
            case R.id.action_sleep_timer:
                SleepTimerHelper.cancelTimer(this);
                initSleepTimerUi();
                endTimer();
                return true;
            case R.id.action_sleep:
                showSleepTimerDialog();
                return true;
            case R.id.action_share_episode:

                if (mEpisode != null) {
                    IntentHelper.shareEpisode(this, EpisodeModel.getEpisodeByGeneratedId(this,
                            mEpisode.getGeneratedId()));
                }
                return true;
            case R.id.action_play_queue:
                showPlayQueue();
                return true;
            case R.id.action_playback_speed:

                if (mEpisode != null) {
                    PlaybackSpeedDialog.show(this, mEpisode.getChannelGeneratedId());
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onFavoriteToggled(boolean isFavorite) {
        mFavoriteButton.setImageResource(isFavorite ? R.drawable.ic_favorite :
                R.drawable.ic_favorite_outline);
    }

    private void animateExit() {
        overridePendingTransition(R.anim.do_nothing, R.anim.slide_down);
    }

    protected void showSleepTimerDialog() {

        new AlertDialog.Builder(this)
            .setTitle(R.string.sleep_timer_title)
            .setSingleChoiceItems(R.array.sleep_timer_labels, -1, (dialog, which) -> {
                dialog.dismiss();
                SleepTimerHelper.setTimerChoice(NowPlayingActivity.this, which);
                initSleepTimerUi();
                startTimer();
            })
            .setNegativeButton(R.string.dialog_cancel_timer, (dialog, which) -> {
                SleepTimerHelper.cancelTimer(NowPlayingActivity.this);
                initSleepTimerUi();
                endTimer();
            })
            .setNeutralButton(R.string.dialog_close, null)
            .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mProgressUpdateReceiver,
                new IntentFilter(BroadcastHelper.INTENT_PROGRESS_UPDATE));
        initSleepTimerUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mProgressUpdateReceiver);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (menu.findItem(R.string.action_cast).isVisible()) {
            ShowcaseHelper.showNowPlayingShowcase(this);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPodcastPlayerServiceBound() {}

    @Override
    public void onStateChanged(int state, Episode episode) {

        if (mStreaming != null) {
            mStreaming.setVisibility(isStreaming() ? View.VISIBLE : View.INVISIBLE);
        }

        if (episode == null) {
            int episodeId = AppPrefHelper.getInstance(this).getLastPlayedEpisodeId();

            if (episodeId > -1) {
                episode = EpisodeModel.getEpisodeById(this, episodeId);
            }
        }

        if (episode != null && state != PlaybackState.STATE_STOPPED) {

            if (!episode.equals(mEpisode)) {
                // a new episode has started
                mEpisode = episode;
                mPrimaryColor = -1;
                mTextColor = -1;
            }

            if (mPrimaryColor == -1 && mTextColor == -1) {
                String channelServerId = episode.getChannelGeneratedId();

                ImageLoadHelper.loadImageAsync(this, episode.getChannelArtworkUrl(), new ImageLoadHelper.OnImageLoaded() {
                    @Override
                    public void imageLoaded(Bitmap bitmap) {

                        if (bitmap != null) {
                            mChannelArt.setImageBitmap(bitmap);
                            PaletteHelper.get(NowPlayingActivity.this)
                                    .getChannelColors(channelServerId, bitmap, mOnPaletteLoaded);
                        }
                    }

                    @Override
                    public void imageFailed() {
                        mChannelArt.setImageDrawable(
                                new BitmapDrawable(NowPlayingActivity.this.getResources(), BitmapFactory.decodeResource(
                                        NowPlayingActivity.this.getResources(), R.drawable.default_channel_art)));
                    }
                });
            } else {
                ImageLoadHelper.loadImageIntoView(this, episode.getChannelArtworkUrl(), mChannelArt);
            }
            ImageLoadHelper.loadImageIntoView(this, episode.getChannelArtworkUrl(), mBackground,
                    new BlurTransformation(this));

            // set the channel and episode titles
            mEpisodeTitle.setText(episode.getTitle());
            mChannelTitle.setText(episode.getChannelTitle());
            updatePlaybackStats(episode.getProgress(), episode.getDuration());
            updatePlaybackSpeed();
            onFavoriteToggled(episode.isFavorite());
        } else {
            resetUI();
        }
        mPlayButton.setImageResource(PlaybackButtonHelper.getPlayerPlaybackButtonResId(state));
    }

    private void updatePlaybackSpeed() {

        if (mPlaybackSpeedItem != null) {

            String title;
            if (mEpisode != null) {
                title = AppPrefHelper.getInstance(this).getPlaybackSpeedLabel(
                                mEpisode.getChannelGeneratedId());
            } else {
                title = MediaHelper.formatSpeed(MediaHelper.DEFAULT_PLAYBACK_SPEED);
            }
            mPlaybackSpeedItem.setTitle(title);
        }

    }

    private void initSleepTimerUi() {
        if (mSleepMenuItem == null || mSleepTimerMenuItem == null) {
            return;
        }

        if (SleepTimerHelper.timerIsActive(this)) {
            mSleepTimerMenuItem.setVisible(true);
            mSleepMenuItem.setVisible(false);
            startTimer();
        } else {
            mSleepTimerMenuItem.setVisible(false);
            mSleepMenuItem.setVisible(true);
            endTimer();
        }
    }

    private void startTimer() {
        long timeToAlarm = AppPrefHelper.getInstance(this).getSleepTimer();

        mCountDownTimer = new CountDownTimer(timeToAlarm, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateSleepTimer();
            }

            @Override
            public void onFinish() {
                endTimer();
            }
        }.start();
    }

    private void endTimer() {

        if (mCountDownTimer == null) {
            return;
        }
        mCountDownTimer.cancel();
        mCountDownTimer = null;
    }

    /**
     * Updates the sleep timer
     */
    private void updateSleepTimer() {

        if (mSleepMenuItem == null || mSleepTimerMenuItem == null) {
            return;
        }

        long now = Calendar.getInstance().getTimeInMillis();
        long timeToAlarm = AppPrefHelper.getInstance(this).getSleepTimer();
        mSleepTimerMenuItem.setTitle(DatetimeHelper.convertSecondsToDuration(timeToAlarm - now));
    }

    /**
     * Themes the Now Playing UI with the appropriate colors
     */
    private void themeUI(int primaryColor, int seekBarColor) {
        findViewById(R.id.media_controls).setBackgroundColor(primaryColor);
        getWindow().setStatusBarColor(ColorHelper.getStatusBarColor(primaryColor));
        mToolbar.setBackgroundColor(primaryColor);
        int textColor = ColorHelper.getTextColor(primaryColor);
        mSeekBackward.setImageTintList(ColorStateList.valueOf(textColor));
        mSeekForward.setImageTintList(ColorStateList.valueOf(textColor));
        mPlayButton.setImageTintList(ColorStateList.valueOf(textColor));
        mInfo.setImageTintList(ColorStateList.valueOf(textColor));
        mFavoriteButton.setImageTintList(ColorStateList.valueOf(textColor));
        mSeekBar.setProgressBackgroundTintList(ColorStateList.valueOf(seekBarColor));
        mSeekBar.setBackgroundTintList(ColorStateList.valueOf(seekBarColor));
        mSeekBar.setProgressTintList(ColorStateList.valueOf(primaryColor));
        mSeekBar.setSecondaryProgressTintList(ColorStateList.valueOf(ColorHelper.darkenColor(primaryColor)));
        mSeekBar.setThumbTintList(ColorStateList.valueOf(primaryColor));
    }

    /**
     * Resets the user interface
     */
    private void resetUI() {
        mSeekBar.setEnabled(false);
        mSeekBar.setMax(100);
        mSeekBar.setProgress(0);
        mSeekBar.setSecondaryProgress(0);
        mEpisodeTitle.setText("");
        mChannelTitle.setText("");
        mTimeElapsed.setText(R.string.empty_time);
        mTimeLeft.setText(R.string.empty_time);
        mBackground.setImageDrawable(null);
        mChannelArt.setImageResource(R.drawable.default_channel_art);
        themeUI(getResources().getColor(R.color.primary),
                getResources().getColor(R.color.primary));
    }

    private void updatePlaybackStats(long progress, long duration) {
        mChannelArt.setRotation(((progress / MS_IN_RECORD_ROTATION) * DEGREES_IN_CIRCLE)
                % DEGREES_IN_CIRCLE);
        mSeekBar.setProgress((int) progress);
        mSeekBar.setMax((int) duration);
        long left = duration - progress;

        if (left == 0 && duration == 0) {
            mTimeElapsed.setText(R.string.empty_time);
            mTimeLeft.setText(R.string.empty_time);
        } else {
            mTimeElapsed.setText(DatetimeHelper.convertSecondsToDuration(progress));
            mTimeLeft.setText(getString(R.string.time_left, DatetimeHelper.convertSecondsToDuration(left)));
        }
    }

    private static class ProgressUpdateReceiver extends BroadcastReceiver {

        private final WeakReference<NowPlayingActivity> mActivity;

        public ProgressUpdateReceiver(NowPlayingActivity context) {
            mActivity = new WeakReference<>(context);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            NowPlayingActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }
            long progress = intent.getLongExtra(BroadcastHelper.EXTRA_PROGRESS, 0L);
            long bufferedProgress = intent.getLongExtra(BroadcastHelper.EXTRA_BUFFERED_PROGRESS, 0L);
            long duration = intent.getLongExtra(BroadcastHelper.EXTRA_DURATION, 0L);
            activity.mSeekBar.setSecondaryProgress((int) bufferedProgress);
            activity.updatePlaybackStats(progress, duration);
        }
    }
}