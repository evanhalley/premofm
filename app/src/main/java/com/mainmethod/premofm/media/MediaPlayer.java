package com.mainmethod.premofm.media;

import com.mainmethod.premofm.object.Episode;

/**
 * Interface for interacting with a media player
 * Created by evanhalley on 11/18/15.
 */
public abstract class MediaPlayer {

    protected PremoMediaPlayerListener mMediaPlayerListener;
    protected ProgressUpdateListener mProgressUpdateListener;

    public MediaPlayer(PremoMediaPlayerListener mediaPlayerListener,
                       ProgressUpdateListener progressUpdateListener) {
        mMediaPlayerListener = mediaPlayerListener;
        mProgressUpdateListener = progressUpdateListener;
    }

    public void tearDown() {
        mMediaPlayerListener = null;
        mProgressUpdateListener = null;
    }

    public abstract void loadEpisode(Episode episode);

    public abstract void startPlayback(boolean playImmediately);

    public abstract void resumePlayback();

    public abstract void pausePlayback();

    public abstract void stopPlayback();

    public abstract void seekTo(long position);

    public abstract boolean isStreaming();

    public abstract int getState();

    public abstract long getCurrentPosition();

    public abstract long getDuration();

    public abstract long getBufferedPosition();

    public abstract void setPlaybackSpeed(float speed);

    /**
     * Used to listen for media player state changes
     * Created by evanhalley on 11/18/15.
     */
    public interface PremoMediaPlayerListener {

        void onStateChanged(int state);

    }
}