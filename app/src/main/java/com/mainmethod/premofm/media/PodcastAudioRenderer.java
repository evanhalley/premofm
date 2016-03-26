package com.mainmethod.premofm.media;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;

/**
 * Custom renderer used for PremoFM
 * Created by evanhalley on 12/4/15.
 */
public class PodcastAudioRenderer extends MediaCodecAudioTrackRenderer {

    private static final long US_IN_MS = 1_000;

    private ProgressUpdateListener mProgressUpdateListener;
    private long mProgress;

    public PodcastAudioRenderer(SampleSource source) {
        super(source, MediaCodecSelector.DEFAULT);
    }

    public void setProgressUpdateListener(ProgressUpdateListener progressUpdateListener) {
        mProgressUpdateListener = progressUpdateListener;
    }

    @Override
    protected void doSomeWork(long positionUs, long elapsedRealtimeUs, boolean sourceIsReady) throws ExoPlaybackException {
        super.doSomeWork(positionUs, elapsedRealtimeUs, sourceIsReady);
        long progress = positionUs / US_IN_MS;
        long duration = getDurationUs() / US_IN_MS;
        long bufferedProgress = getBufferedPositionUs();

        // if the end of the track has been buffered, set progress to duration
        if (bufferedProgress == TrackRenderer.END_OF_TRACK_US) {
            bufferedProgress = duration;
        } else {
            bufferedProgress = bufferedProgress / US_IN_MS;
        }

        if (mProgress != progress) {
            mProgress = progress;

            if (mProgressUpdateListener != null) {
                mProgressUpdateListener.onProgressUpdate(progress, bufferedProgress, duration);
            }
        }
    }
}