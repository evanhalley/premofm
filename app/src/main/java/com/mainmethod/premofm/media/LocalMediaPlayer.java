package com.mainmethod.premofm.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.mainmethod.premofm.PremoApp;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.Episode;

/**
 * Plays media playback on the device audio output
 * Created by evanhalley on 11/18/15.
 */
public class LocalMediaPlayer extends MediaPlayer implements ExoPlayer.EventListener,
        ProgressUpdateListener {

    private static final String TAG = LocalMediaPlayer.class.getSimpleName();
    private static final int ALLOCATION_SIZE = 65_535;
    private static final int BUFFER_SIZE = 10 * 1_024 * 1_024;

    private final Context mContext;
    private final ExoPlayer mMediaPlayer;
    private PodcastAudioRenderer mTrackRenderer;
    private Episode mEpisode;
    private boolean mIsStreaming;
    private int mMediaPlayerState;
    private int mStateBeforeLoading;

    public LocalMediaPlayer(PremoMediaPlayerListener mediaPlayerListener,
                            ProgressUpdateListener progressUpdateListener, Context context) {
        super(mediaPlayerListener, progressUpdateListener);
        mContext = context;
        TrackSelection.Factory trackSelection = new FixedTrackSelection.Factory();
        TrackSelector
        mMediaPlayer = ExoPlayerFactory.newInstance()
        mMediaPlayer.addListener(this);
        mMediaPlayerState = MediaPlayerState.STATE_IDLE;
    }

    @Override
    public int getState() {
        return mMediaPlayerState;
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        return mMediaPlayer.getBufferedPosition();
    }

    @Override
    public boolean isStreaming() {
        return mIsStreaming;
    }

    @Override
    public void loadEpisode(Episode episode) {
        mEpisode = episode;
    }

    @Override
    public void startPlayback(boolean playImmediately) {

        if (mEpisode.getProgress() > -1) {
            mMediaPlayer.seekTo(mEpisode.getProgress());
        } else {
            mMediaPlayer.seekTo(0);
        }
        mTrackRenderer = buildAudioRenderer();
        mMediaPlayer.prepare(mTrackRenderer);
        mMediaPlayer.setPlayWhenReady(playImmediately);
    }

    @Override
    public void resumePlayback() {
        mMediaPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pausePlayback() {
        mMediaPlayer.setPlayWhenReady(false);
    }

    @Override
    public void stopPlayback() {
        mMediaPlayer.stop();
        mIsStreaming = false;
        mEpisode = null;
    }

    @Override
    public void seekTo(long position) {
        mMediaPlayer.seekTo(position);
    }

    @Override
    public void tearDown() {
        Log.d(TAG, "Tearing down");
        super.tearDown();
        mMediaPlayer.release();
        mMediaPlayer.removeListener(this);
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        if (isLoading) {
            mStateBeforeLoading = mMediaPlayerState;
            mMediaPlayerState = MediaPlayerState.STATE_CONNECTING;
        } else {
            mMediaPlayerState = mStateBeforeLoading;
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        String playbackStateStr;

        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                mMediaPlayerState = MediaPlayerState.STATE_CONNECTING;
                playbackStateStr = "Buffering";
                break;
            case ExoPlayer.STATE_ENDED:
                mMediaPlayerState = MediaPlayerState.STATE_ENDED;
                playbackStateStr = "Ended";
                break;
            case ExoPlayer.STATE_IDLE:
                mMediaPlayerState = MediaPlayerState.STATE_IDLE;
                playbackStateStr = "Idle";
                break;
            case ExoPlayer.STATE_READY:
                mMediaPlayerState = playWhenReady ? MediaPlayerState.STATE_PLAYING :
                        MediaPlayerState.STATE_PAUSED;
                playbackStateStr = "Ready";
                break;
            default:
                mMediaPlayerState = MediaPlayerState.STATE_IDLE;
                playbackStateStr = "Unknown";
                break;
        }
        mMediaPlayerListener.onStateChanged(mMediaPlayerState);
        Log.d(TAG, String.format("ExoPlayer state changed: %s, Play When Ready: %s",
                playbackStateStr,
                String.valueOf(playWhenReady)));
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.w(TAG, "Player error encountered", error);
        stopPlayback();
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onProgressUpdate(long progress, long bufferedProgress, long duration) {
        mProgressUpdateListener.onProgressUpdate(progress,
                isStreaming() ? bufferedProgress : duration, duration);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void setPlaybackSpeed(float speed) {

        if (mTrackRenderer != null) {

            if (mTrackRenderer instanceof PodcastAudioRendererV21) {
                ((PodcastAudioRendererV21) mTrackRenderer).setSpeed(speed);
            } else {
                mMediaPlayer.sendMessage(mTrackRenderer,
                        MediaCodecAudioTrackRenderer.MSG_SET_PLAYBACK_PARAMS,
                        new PlaybackParams().setSpeed(speed));
            }
        }
    }

    private PodcastAudioRenderer buildAudioRenderer() {
        PodcastAudioRenderer trackRenderer = null;
        Uri uri = null;

        // return the uri to play
        switch (mEpisode.getDownloadStatus()) {
            case DownloadStatus.DOWNLOADED:
                uri = Uri.parse(mEpisode.getLocalMediaUrl());
                mIsStreaming = false;
                break;
            case DownloadStatus.DOWNLOADING:
            case DownloadStatus.NOT_DOWNLOADED:
                uri = Uri.parse(mEpisode.getRemoteMediaUrl());
                mIsStreaming = true;
                break;
        }

        if (uri != null) {
            Log.d(TAG, "Playing from URI " + uri);
            Allocator allocator = new DefaultAllocator(ALLOCATION_SIZE);
            DataSource dataSource = new DefaultUriDataSource(mContext, null,
                    String.format(mContext.getString(R.string.user_agent),
                            PremoApp.getVersionName()), true);
            ExtractorSampleSource sampleSource = new ExtractorSampleSource(
                    uri, dataSource, allocator, BUFFER_SIZE,
                    new Mp3Extractor(),
                    new Mp4Extractor());

            if (Util.SDK_INT >= Build.VERSION_CODES.M) {
                trackRenderer = new PodcastAudioRenderer(sampleSource);
            } else {
                trackRenderer = new PodcastAudioRendererV21(sampleSource);
            }
            trackRenderer.setProgressUpdateListener(this);
        } else {
            Log.w(TAG, "Uri is null");
        }
        return trackRenderer;
    }
}
