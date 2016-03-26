package com.mainmethod.premofm.media;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;
import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.object.Episode;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Controls playback through a Google Cast device
 * Created by evan on 11/19/15.
 */
public class CastMediaPlayer extends MediaPlayer implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, RemoteMediaPlayer.OnStatusUpdatedListener {

    private static final String TAG = CastMediaPlayer.class.getSimpleName();

    private GoogleApiClient mApiClient;
    private CastDevice mCastDevice;
    private boolean mCastStarted;
    private boolean mWaitingForReconnect;
    private boolean mPlayWhenConnected;
    private RemoteMediaPlayer mMediaPlayer;
    private Episode mEpisode;
    private int mMediaPlayerState;
    private boolean mPlayImmediately;
    private boolean isUpdatingProgress;
    private ProgressUpdater mProgressUpdater;
    private Handler mHandler;

    public CastMediaPlayer(PremoMediaPlayerListener mediaPlayerListener,
                           ProgressUpdateListener progressUpdateListener,
                           Context context, CastDevice castDevice) {
        super(mediaPlayerListener, progressUpdateListener);
        mHandler = new Handler();
        mProgressUpdater = new ProgressUpdater();
        mCastDevice = castDevice;
        mMediaPlayer = new RemoteMediaPlayer();
        mMediaPlayer.setOnStatusUpdatedListener(this);

        Cast.CastOptions castOptions = Cast.CastOptions
                .builder(mCastDevice, new CastListener(this))
                .setVerboseLoggingEnabled(BuildConfig.DEBUG)
                .build();
        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(Cast.API, castOptions)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient.connect();
    }

    @Override
    public void onStatusUpdated() {

        if (mMediaPlayer == null) {
            return;
        }
        String playbackStr;
        int mediaStatus = MediaStatus.PLAYER_STATE_IDLE;
        MediaStatus status = mMediaPlayer.getMediaStatus();

        if (status != null) {
            mediaStatus = status.getPlayerState();
        }

        switch (mediaStatus) {
            case MediaStatus.PLAYER_STATE_IDLE:
                stopProgressUpdater();
                long position = mMediaPlayer.getApproximateStreamPosition();

                if (position == 0 && mMediaPlayerState == MediaPlayerState.STATE_PLAYING) {
                    playbackStr = "Ended";
                    mMediaPlayerState = MediaPlayerState.STATE_ENDED;
                } else {
                    playbackStr = "Idle";
                    mMediaPlayerState = MediaPlayerState.STATE_IDLE;
                }
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                playbackStr = "Buffering";
                mMediaPlayerState = MediaPlayerState.STATE_CONNECTING;
                break;
            case MediaStatus.PLAYER_STATE_PLAYING:
                startProgressUpdater();
                playbackStr = "Playing";
                mMediaPlayerState = MediaPlayerState.STATE_PLAYING;
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:

                if (mMediaPlayerState == MediaPlayerState.STATE_PLAYING) {
                    stopProgressUpdater();
                    playbackStr = "Paused";
                    mMediaPlayerState = MediaPlayerState.STATE_PAUSED;
                } else {
                    playbackStr = "Idle";
                    mMediaPlayerState = MediaPlayerState.STATE_IDLE;
                }
                break;
            case MediaStatus.PLAYER_STATE_UNKNOWN:
            default:
                playbackStr = "Unknown";
                mMediaPlayerState = MediaPlayerState.STATE_IDLE;
                break;
        }
        mMediaPlayerListener.onStateChanged(mMediaPlayerState);
        Log.d(TAG, "RemoteMediaPlayer state changed: " + playbackStr);
    }

    @Override
    public void onConnected(Bundle bundle) {

        if (mWaitingForReconnect) {
            mWaitingForReconnect = false;
            reconnectChannels(bundle);
        } else {

            try {
                Cast.CastApi.launchApplication(mApiClient,
                        BuildConfig.CAST_APP_ID, false).setResultCallback(
                        result -> {
                            Status status = result.getStatus();

                            if (status.isSuccess()) {
                                mCastStarted = true;
                                reconnectChannels(null);

                                if (mPlayWhenConnected) {
                                    startPlayback();
                                    mPlayWhenConnected = false;
                                }
                            } else {
                                tearDown();
                            }
                        });

            } catch (Exception e) {
                Log.e(TAG, "Failed to launch application", e);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mWaitingForReconnect = true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed: " + connectionResult.toString());
        tearDown();
    }

    @Override
    public void loadEpisode(Episode episode) {
        mEpisode = episode;
    }

    @Override
    public void startPlayback(boolean playImmediately) {
        mPlayImmediately = playImmediately;

        if (!mCastStarted || mApiClient == null) {
            mPlayWhenConnected = true;
            return;
        }
        startPlayback();
    }

    @Override
    public void resumePlayback() {
        mMediaPlayer.play(mApiClient);
    }

    @Override
    public void pausePlayback() {
        mMediaPlayer.pause(mApiClient);
    }

    @Override
    public void stopPlayback() {
        mMediaPlayer.stop(mApiClient);
    }

    @Override
    public void seekTo(long position) {
        mMediaPlayer.seek(mApiClient, position);
    }

    @Override
    public void tearDown() {
        Log.d(TAG, "Tearing down");
        super.tearDown();

        if (mApiClient != null) {

            if (mCastStarted) {

                try {
                    Cast.CastApi.stopApplication(mApiClient);

                    if (mMediaPlayer != null) {
                        Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
                                mMediaPlayer.getNamespace());
                        mMediaPlayer = null;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Exception while removing application", e);
                }
                mCastStarted = false;
            }

            if (mApiClient.isConnected()) {
                mApiClient.disconnect();
            }
            mApiClient = null;
        }
        mCastDevice = null;
        stopProgressUpdater();
        mProgressUpdater = null;
    }

    @Override
    public boolean isStreaming() {
        return true;
    }

    @Override
    public int getState() {
        return mMediaPlayerState;
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer.getApproximateStreamPosition();
    }

    @Override
    public long getDuration() {
        return mMediaPlayer.getStreamDuration();
    }

    @Override
    public long getBufferedPosition() {
        return 0;
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        // not going to be able to do it
    }

    private void startPlayback() {
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString(MediaMetadata.KEY_ARTIST, mEpisode.getChannelAuthor());
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, mEpisode.getTitle());
        mediaMetadata.putString(MediaMetadata.KEY_ALBUM_TITLE, mEpisode.getChannelTitle());
        mediaMetadata.addImage(new WebImage(Uri.parse(mEpisode.getArtworkUrl())));

        MediaInfo mediaInfo = new MediaInfo.Builder(mEpisode.getRemoteMediaUrl())
                .setContentType(mEpisode.getMimeType())
                .setStreamDuration(mEpisode.getDuration())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();

        mMediaPlayer.load(mApiClient, mediaInfo, false)
                .setResultCallback(result -> {

                    if (result.getStatus().isSuccess()) {

                        if (mEpisode.getProgress() > 0) {
                            mMediaPlayer.seek(mApiClient, mEpisode.getProgress());
                        }

                        if (mPlayImmediately) {
                            mMediaPlayer.play(mApiClient);
                        }
                    } else {
                        Log.e(TAG, "Media player load failed: " + result.getStatus());
                    }
                });
    }

    private void reconnectChannels(Bundle hint) {

        if ((hint != null) && hint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
            Log.w(TAG, "App is no longer running");
            tearDown();
        } else {

            try {
                Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                        mMediaPlayer.getNamespace(), mMediaPlayer);
            } catch (IOException e) {
                Log.e(TAG, "Exception while creating media channel ", e);
            } catch (NullPointerException e) {
                Log.e(TAG, "Something wasn't reinitialized for reconnectChannels");
            }
        }
    }

    private static class CastListener extends Cast.Listener {

        private final WeakReference<CastMediaPlayer> mMediaPlayer;

        public CastListener(CastMediaPlayer mediaPlayer) {
            mMediaPlayer = new WeakReference<>(mediaPlayer);
        }

        @Override
        public void onApplicationStatusChanged() {

            if (mMediaPlayer.get().mApiClient != null) {
                Log.d(TAG, "onApplicationStatusChanged: " +
                        Cast.CastApi.getApplicationStatus(mMediaPlayer.get().mApiClient));
            }
        }

        @Override
        public void onVolumeChanged() {

            if (mMediaPlayer.get().mApiClient != null) {
                Log.d(TAG, "onVolumeChanged: " +
                        Cast.CastApi.getVolume(mMediaPlayer.get().mApiClient));
            }
        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            Log.d(TAG, "Disconnected code: " + errorCode);
            mMediaPlayer.get().tearDown();
        }
    }

    private void startProgressUpdater() {

        if (!isUpdatingProgress) {
            mProgressUpdater.run();
            isUpdatingProgress = true;
        }
    }

    private void stopProgressUpdater() {

        if (isUpdatingProgress) {
            mHandler.removeCallbacks(mProgressUpdater);
            isUpdatingProgress = false;
        }
    }

    // spins the album art like a record
    private class ProgressUpdater implements Runnable {

        private static final int TIME_UPDATE_MS = 16;

        @Override
        public void run() {
            long progress = mMediaPlayer.getApproximateStreamPosition();
            long duration = mMediaPlayer.getStreamDuration();
            mProgressUpdateListener.onProgressUpdate(progress, 0, duration);
            mHandler.postDelayed(mProgressUpdater, TIME_UPDATE_MS);
        }
    }
}
