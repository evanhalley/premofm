/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.service;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.gms.cast.CastDevice;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.data.model.PlaylistModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.helper.MediaHelper;
import com.mainmethod.premofm.helper.PendingIntentHelper;
import com.mainmethod.premofm.helper.PlaybackButtonHelper;
import com.mainmethod.premofm.helper.SleepTimerHelper;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.media.CastMediaPlayer;
import com.mainmethod.premofm.media.LocalMediaPlayer;
import com.mainmethod.premofm.media.MediaNotificationManager;
import com.mainmethod.premofm.media.MediaPlayer;
import com.mainmethod.premofm.media.MediaPlayerState;
import com.mainmethod.premofm.media.ProgressUpdateListener;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.object.Playlist;
import com.mainmethod.premofm.ui.view.RoundedCornersTransformation;
import com.mainmethod.premofm.ui.widget.WidgetProvider;

import org.parceler.Parcels;

/**
 * Plays podcasts in the background
 * Created by evan on 12/17/14.
 */
public class PodcastPlayerService extends Service implements AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.PremoMediaPlayerListener, ProgressUpdateListener {

    private static final String TAG = PodcastPlayerService.class.getSimpleName();

    private static final int MS_TO_REVERSE_ON_PAUSE         = 0;
    private static final float AUDIO_DUCK                   = 0.8f;
    private static final int AUTO_COMPLETE_EPISODE          = 15_000;
    private static final long MEDIA_SESSION_ACTIONS =
            PlaybackState.ACTION_FAST_FORWARD |
            PlaybackState.ACTION_REWIND |
            PlaybackState.ACTION_SEEK_TO |
            PlaybackState.ACTION_SKIP_TO_NEXT |
            PlaybackState.ACTION_SKIP_TO_PREVIOUS;

    public static final String ACTION_PLAY_QUEUE        = "com.mainmethod.premofm.playQueue";
    public static final String ACTION_PLAY_EPISODE      = "com.mainmethod.premofm.playNew";
    public static final String ACTION_PLAY_PLAYLIST     = "com.mainmethod.premofm.playPlaylist";
    public static final String ACTION_RESUME_PLAYBACK   = "com.mainmethod.premofm.play";
    public static final String ACTION_PAUSE             = "com.mainmethod.premofm.pause";
    public static final String ACTION_SEEK_FORWARD      = "com.mainmethod.premofm.seekForward";
    public static final String ACTION_SEEK_BACKWARD     = "com.mainmethod.premofm.seekBackward";
    public static final String ACTION_SEEK_TO           = "com.mainmethod.premofm.seekTo";
    public static final String ACTION_STOP_SERVICE      = "com.mainmethod.premofm.stopService";
    public static final String ACTION_UPDATE_WIDGET     = "com.mainmethod.premofm.updateWidget";
    public static final String ACTION_SLEEP_TIMER       = "com.mainmethod.premofm.sleepTimer";
    public static final String ACTION_START_CAST        = "com.mainmethod.premofm.startCast";
    public static final String ACTION_END_CAST          = "com.mainmethod.premofm.endCast";
    public static final String ACTION_SET_SPEED         = "com.mainmethod.premofm.setPlaybackSpeed";

    public static final String PARAM_PLAYLIST           = "playlist";
    public static final String PARAM_EPISODE_ID         = "episodeId";
    public static final String PARAM_SEEK_MS            = "seekMs";
    public static final String PARAM_CAST_DEVICE        = "castDevice";
    public static final String PARAM_PLAYBACK_SPEED     = "playbackSpeed";

    // media related objects
    private MediaPlayer mMediaPlayer;
    private MediaSession mMediaSession;
    private AudioManager mAudioManager;
    private WifiManager.WifiLock mWifiLock;
    private MediaNotificationManager mMediaNotificationManager;
    private PlaybackState mPlaybackState;
    private int mMediaPlayerState;
    private Episode mCurrentEpisode;
    private UpdateEpisodeProgressTask mUpdateTask;
    private boolean mPlayingBeforeFocusChange;
    private boolean mReceiverRegistered;
    private int mStreamVolume = -1;
    private boolean mServiceBound = false;
    private HeadsetReceiver mHeadsetReceiver;
    private EpisodeDownloadedReceiver mEpisodeDownloadedReceiver;

    // This is the object that receives interactions from clients. See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new ServiceBinder();

    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class ServiceBinder extends Binder {
        public PodcastPlayerService getService() {
            return PodcastPlayerService.this;
        }
    }

    /******************************
     * Service lifecycle methods
     ******************************/

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaNotificationManager = new MediaNotificationManager(this);

        mMediaPlayerState = MediaPlayerState.STATE_IDLE;

        // set default playback state
        mPlaybackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_NONE, 0, 1.0f)
                .build();

        // setup our media session
        mMediaSession = new MediaSession(this, TAG);
        mMediaSession.setMediaButtonReceiver(PendingIntentHelper.getMediaButtonReceiverIntent(this));
        mMediaSession.setCallback(new MediaSessionCallback());
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setActive(true);
        mMediaSession.setPlaybackState(mPlaybackState);

        // set up our broadcast receiver for receiving headset and button events
        mHeadsetReceiver = new HeadsetReceiver();

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "uAmp_lock");

        mEpisodeDownloadedReceiver = new EpisodeDownloadedReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mEpisodeDownloadedReceiver,
                new IntentFilter(BroadcastHelper.INTENT_EPISODE_DOWNLOADED));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying media player object");
        destroyMediaPlayer();
        mMediaNotificationManager.stopNotification();
        mMediaSession.release();
        releaseWifiLock();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mEpisodeDownloadedReceiver);
        super.onDestroy();
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "OnStartCommand intent action: " + action);

            switch (action) {
                case ACTION_PLAY_QUEUE:
                    playNextEpisode();
                    break;
                case ACTION_PLAY_EPISODE:
                    int episodeId = intent.getIntExtra(PARAM_EPISODE_ID, -1);

                    if (episodeId != -1) {
                        play(episodeId);
                    }
                    break;
                case ACTION_PLAY_PLAYLIST:
                    Playlist playlist = Parcels.unwrap(intent.getParcelableExtra(PARAM_PLAYLIST));
                    Episode episode = EpisodeModel.getEpisodeByServerId(this,
                            playlist.getCurrentEpisodeServerId());
                    PlaylistModel.savePlaylist(this, playlist);
                    play(episode, true);
                    break;
                case ACTION_RESUME_PLAYBACK:

                    if (mMediaPlayerState != MediaPlayerState.STATE_PLAYING) {
                        play(-1);
                    }
                    break;
                case ACTION_PAUSE:
                    pause();
                    break;
                case ACTION_SEEK_FORWARD:
                    seekForward();
                    break;
                case ACTION_SEEK_BACKWARD:
                    seekBackward();
                    break;
                case ACTION_SEEK_TO:
                    int seekMs = intent.getIntExtra(PARAM_SEEK_MS, 30);
                    seekTo(seekMs);
                    break;
                case ACTION_STOP_SERVICE:
                    endPlayback(true);

                    if (!mServiceBound) {
                        stopSelf();
                    }
                    break;
                case ACTION_UPDATE_WIDGET:
                    if (mMediaPlayerState == MediaPlayerState.STATE_PLAYING ||
                            mMediaPlayerState == MediaPlayerState.STATE_PAUSED) {
                        updateWidget();
                    }
                    break;
                case ACTION_SLEEP_TIMER:
                    // stop the media player
                    stopPlayback();
                    // clear the timer value
                    AppPrefHelper.getInstance(this).removeSleepTimer();
                    stopSelf();
                    break;
                case ACTION_START_CAST:
                    int beforeState = mMediaPlayerState;
                    destroyMediaPlayer();
                    CastDevice castDevice = intent.getParcelableExtra(PARAM_CAST_DEVICE);
                    mMediaPlayer = new CastMediaPlayer(this, this, this, castDevice);

                    if (beforeState == MediaPlayerState.STATE_PLAYING || beforeState == MediaPlayerState.STATE_PAUSED) {
                        Log.d(TAG, "Restarting episode playback");
                        play(mCurrentEpisode, beforeState == MediaPlayerState.STATE_PLAYING);
                    }
                    break;
                case ACTION_END_CAST:
                    destroyMediaPlayer();
                    break;
                case ACTION_SET_SPEED:
                    float speed = intent.getFloatExtra(PARAM_PLAYBACK_SPEED, MediaHelper.DEFAULT_PLAYBACK_SPEED);
                    changePlaybackSpeed(speed);
                    break;
            }

        }
        return START_NOT_STICKY;
    }

    private void destroyMediaPlayer() {

        if (mMediaPlayer == null) {
            return;
        }
        endUpdateTask();
        endPlayback(true);
        mMediaPlayerState = MediaPlayerState.STATE_IDLE;
        updatePlaybackState(mMediaPlayerState);
        mMediaPlayer.tearDown();
        mMediaPlayer = null;
    }

    /******************************
     * WifiLock Functions
     ******************************/

    private void acquireWifiLock() {

        if (mWifiLock != null && !mWifiLock.isHeld() && mMediaPlayer.isStreaming()) {
            mWifiLock.acquire();
        }
    }

    private void releaseWifiLock() {

        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    /******************************
     * Audio Focus functions
     ******************************/

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "AudioFocusChange, result code: " + focusChange);
        boolean pauseOnNotification = UserPrefHelper.get(this).getBoolean(
                R.string.pref_key_pause_playback_during_notification);

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:

                if (pauseOnNotification) {
                    // record the playing before focus change value
                    mPlayingBeforeFocusChange = getPlaybackState() == MediaPlayerState.STATE_PLAYING;
                    pause();
                } else {
                    mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            (int) (mStreamVolume * AUDIO_DUCK), 0);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // record the playing before focus change value
                mPlayingBeforeFocusChange = mMediaPlayerState == MediaPlayerState.STATE_PLAYING;
                pause();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:

                if (mStreamVolume > -1) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0);
                    mStreamVolume = -1;
                }

                // gained focus, start playback if we were playing before the focus change
                if (mPlayingBeforeFocusChange && pauseOnNotification) {
                    mPlayingBeforeFocusChange = false;
                    play(null, true);
                }
                break;
        }
    }

    public MediaSession.Token getMediaSessionToken() {
        return mMediaSession.getSessionToken();
    }

    /******************************
     * Service bind functions
     ******************************/

    private void registerReceivers() {

        if (!mReceiverRegistered) {
            registerReceiver(mHeadsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            registerReceiver(mHeadsetReceiver, new IntentFilter(
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            mReceiverRegistered = true;
        }
    }

    private void unregisterReceivers() {

        if (mReceiverRegistered) {
            unregisterReceiver(mHeadsetReceiver);
            mReceiverRegistered = false;
        }
    }

    /******************************
     * Service bind functions
     ******************************/

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbinded from service");

        if (mMediaPlayerState == MediaPlayerState.STATE_IDLE && !(mMediaPlayer instanceof CastMediaPlayer)) {
            stopSelf();
        }
        mServiceBound = false;
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binded to service");
        mServiceBound = true;
        return mBinder;
    }

    /******************************
     * MediaPlayer interface callbacks
     ******************************/

    @Override
    public void onProgressUpdate(long progress, long bufferedProgress, long left) {
        BroadcastHelper.broadcastProgressUpdate(this, progress, bufferedProgress, left);
    }

    @Override
    public void onStateChanged(int state) {
        updatePlaybackState(state);
        mMediaPlayerState = state;

        switch (state) {
            case MediaPlayerState.STATE_CONNECTING:
                break;
            case MediaPlayerState.STATE_ENDED:
                endUpdateTask();
                endPlayback(true);
                finishEpisode();
                playNextEpisode();
                break;
            case MediaPlayerState.STATE_IDLE:
                updateEpisode(state);
                endUpdateTask();
                break;
            case MediaPlayerState.STATE_PLAYING:
                updateEpisode(state);
                startNotificationUpdate();
                startUpdateTask();
                break;
            case MediaPlayerState.STATE_PAUSED:
                endUpdateTask();
                
                if (getDurationLeft() < AUTO_COMPLETE_EPISODE) {
                    stopPlayback();
                } else {
                    updateEpisode(state);
                    startNotificationUpdate();
                }
                break;
        }
        updateWidget();
        String serverId = mCurrentEpisode != null ? mCurrentEpisode.getGeneratedId() : "";
        BroadcastHelper.broadcastPlayerStateChange(this, mMediaPlayerState, serverId);
    }

    private void updatePlaybackState(int stateVal) {
        // get current position
        long currentPosition = mMediaPlayer.getCurrentPosition();
        int playbackState;
        long actions = MEDIA_SESSION_ACTIONS;

        switch (stateVal) {
            case MediaPlayerState.STATE_CONNECTING:
                playbackState = PlaybackState.STATE_CONNECTING;
                actions |= PlaybackState.ACTION_PAUSE;
                break;
            case MediaPlayerState.STATE_IDLE:
                playbackState = PlaybackState.STATE_NONE;
                actions |= PlaybackState.ACTION_PLAY;
                break;
            case MediaPlayerState.STATE_ENDED:
                playbackState = PlaybackState.STATE_STOPPED;
                actions |= PlaybackState.ACTION_PLAY;
                break;
            case MediaPlayerState.STATE_PAUSED:
                playbackState = PlaybackState.STATE_PAUSED;
                actions |= PlaybackState.ACTION_PLAY;
                break;
            case MediaPlayerState.STATE_PLAYING:
                playbackState = PlaybackState.STATE_PLAYING;
                actions |= PlaybackState.ACTION_PAUSE;
                break;
            default:
                playbackState = PlaybackState.STATE_NONE;
                actions |= PlaybackState.ACTION_PLAY;
                break;
        }

        // create new playback state and add it to the media session
        mPlaybackState = new PlaybackState.Builder()
                .setState(playbackState, currentPosition, 1.0f)
                .setActions(actions)
                .build();

        // update current episode
        Bundle extras = new Bundle();

        if (mCurrentEpisode != null) {
            extras.putInt(PARAM_EPISODE_ID, mCurrentEpisode.getId());
        }

        // add new properties to the media session
        mMediaSession.setPlaybackState(mPlaybackState);
        mMediaSession.setExtras(extras);
    }

    /******************************
     * Playlist Management
     ******************************/

    private void playNextEpisode() {
        Playlist playlist = PlaylistModel.getPlaylist(this);

        if (playlist.next()) {
            PlaylistModel.savePlaylist(this, playlist);
            Episode episode = EpisodeModel.getEpisodeByServerId(this,
                    playlist.getCurrentEpisodeServerId());
            play(episode, true);
        } else {
            // no more episodes to play, end any possible sleep timers/alarms
            SleepTimerHelper.cancelTimer(this);
        }
    }

    /******************************
     * Media Player controls
     ******************************/

    private void startPlayback(Episode episode, boolean playImmediately) {
        // request audio focus
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentEpisode = episode;
            registerReceivers();
            acquireWifiLock();
            mMediaPlayer.loadEpisode(new Episode(mCurrentEpisode));
            mMediaPlayer.startPlayback(playImmediately);
            mMediaPlayer.setPlaybackSpeed(
                    AppPrefHelper.getInstance(this).getPlaybackSpeed(
                            mCurrentEpisode.getChannelGeneratedId()));
        } else {
            Log.d(TAG, "Audiofocus not granted, result code: " + result);
        }
    }

    private void stopPlayback() {
        mMediaPlayer.stopPlayback();
    }

    private void pausePlayback() {
        mMediaPlayer.pausePlayback();
    }

    private void endPlayback(boolean cancelNotification) {
        updateEpisode(MediaPlayerState.STATE_IDLE);
        unregisterReceivers();

        if (cancelNotification) {
            mMediaNotificationManager.stopNotification();
        }
        mAudioManager.abandonAudioFocus(this);
        releaseWifiLock();
    }

    private void finishEpisode() {
        // mark the episode as finished
        AppPrefHelper.getInstance(this).removeLastPlayedEpisodeId();

        EpisodeModel.markEpisodeCompleted(this, mCurrentEpisode.getId(),
                mCurrentEpisode.getDownloadStatus() == DownloadStatus.DOWNLOADED);

        // clear the episode
        mCurrentEpisode = null;
    }

    private void seekPlayerTo(long seekTo) {
        long currentPosition = mMediaPlayer.getCurrentPosition();

        if (seekTo < 0) {
            seekTo = 0;
        } else if (seekTo > mMediaPlayer.getDuration()) {
            seekTo = mMediaPlayer.getDuration();
        }
        Log.d(TAG, "Current position: " + currentPosition);
        Log.d(TAG, "Seek To: " + seekTo);
        mMediaPlayer.seekTo(seekTo);
    }

    private void seekPlayerBy(long msec) {
        long currentPosition = mMediaPlayer.getCurrentPosition();
        long duration = mMediaPlayer.getDuration();
        long seekTo = currentPosition + msec;

        if (seekTo < 0) {
            seekTo = 0;
        } else if (seekTo > duration) {
            seekTo = duration;
        }
        Log.d(TAG, "Current position: " + currentPosition);
        Log.d(TAG, "Seek To: " + seekTo);
        mMediaPlayer.seekTo(seekTo);
    }

    /******************************
     * Episode updates
     ******************************/

    private void updateEpisode(int state) {
        Log.d(TAG, "Updating episode, state: " + state);

        if (mMediaPlayer == null || mCurrentEpisode == null || mCurrentEpisode.getId() == -1) {
            return;
        }

        switch (state) {
            case MediaPlayerState.STATE_PLAYING:
                // mark episode as playing in DB
                mCurrentEpisode.setEpisodeStatus(EpisodeStatus.IN_PROGRESS);
                break;
            case MediaPlayerState.STATE_PAUSED:
            case MediaPlayerState.STATE_IDLE:
                mCurrentEpisode.setEpisodeStatus(EpisodeStatus.PLAYED);
                mCurrentEpisode.setProgress(mMediaPlayer.getCurrentPosition() - MS_TO_REVERSE_ON_PAUSE);
                break;
            default:
                throw new IllegalArgumentException(
                        "Incorrect state for showing play pause notification");
        }
        AppPrefHelper appPrefHelper = AppPrefHelper.getInstance(this);
        appPrefHelper.setLastPlayedEpisodeId(mCurrentEpisode.getId());

        Bundle params = new Bundle();
        params.putLong(EpisodeModel.PARAM_EPISODE_PROGRESS, mCurrentEpisode.getProgress());
        params.putInt(EpisodeModel.PARAM_EPISODE_STATUS, mCurrentEpisode.getEpisodeStatus());
        EpisodeModel.updateEpisodeAsync(this, mCurrentEpisode.getId(), params);
    }

    /******************************
     * Getters
     ******************************/

    /**
     * Returns true if the media player is streaming audio over the net
     * @return true if media player is streaming over the network
     */
    @SuppressWarnings("unused")
    public boolean isStreaming() {
        return mMediaPlayer != null && mMediaPlayer.isStreaming();
    }

    /**
     * Returns the progress of the player
     * @return progress
     */
    private long getProgress() {
        long progress = -1;

        if (mMediaPlayer == null) {
            return progress;
        }

        switch (mMediaPlayerState) {
            case MediaPlayerState.STATE_PLAYING:
            case MediaPlayerState.STATE_PAUSED:
                progress = mMediaPlayer.getCurrentPosition();
                break;
        }
        return progress;
    }

    /**
     * Returns the length of the track
     * @return length of current track
     */
    private long getDuration() {
        long duration = -1;

        if (mMediaPlayer == null) {
            return duration;
        }

        switch (mMediaPlayerState) {
            case MediaPlayerState.STATE_PLAYING:
            case MediaPlayerState.STATE_PAUSED:
                duration = mMediaPlayer.getDuration();
                break;
        }
        return duration;
    }

    /**
     * Returns the time remaining
     * @return duration left
     */
    private long getDurationLeft() {
        long timeLeft = -1;

        if (mMediaPlayer == null) {
            return timeLeft;
        }

        switch (mMediaPlayerState) {
            case MediaPlayerState.STATE_PLAYING:
            case MediaPlayerState.STATE_PAUSED:
                long duration = getDuration();
                long progress = getProgress();
                timeLeft = duration - progress;
                break;
        }
        return timeLeft;
    }

    public int getPlaybackState() {
        return mPlaybackState.getState();
    }

    /******************************
     * Functions for media player control
     ******************************/

    /**
     * Plays an episode
     * Will resume from pause if state is paused, loads from scratch otherwise
     * @param episodeId id of the episode to play
     */
    private void play(int episodeId) {
       Episode episode = null;

        // load the episode at episode id
        if (episodeId != -1) {
            episode = EpisodeModel.getEpisodeById(this, episodeId);
            // create a playlist and add this episode to it
            Playlist playlist = new Playlist();
            playlist.addToBeginning(episode.getGeneratedId());
            PlaylistModel.savePlaylist(this, playlist);
        }

        // if we aren't playing an episode
        if (episode == null && mCurrentEpisode == null &&
                AppPrefHelper.getInstance(this).getLastPlayedEpisodeId() != -1) {
            episode = EpisodeModel.getEpisodeById(this,
                    AppPrefHelper.getInstance(this).getLastPlayedEpisodeId());
        }
        play(episode, true);
    }

    /**
     * Plays an episode
     * Will resume from pause if episode is null and we are paused
     * @param episode to play
     */
    private void play(Episode episode, boolean playImmediately) {
        Log.d(TAG, "Play called");

        if (mMediaPlayer == null) {
            mMediaPlayer = new LocalMediaPlayer(this, this, this);
        }

        switch (mMediaPlayerState) {
            case MediaPlayerState.STATE_CONNECTING:
            case MediaPlayerState.STATE_PLAYING:
            case MediaPlayerState.STATE_PAUSED:

                // playing an episode, true if the episode to play is a different one
                if (episode != null) {
                    endUpdateTask();
                    endPlayback(false);
                    startPlayback(episode, playImmediately);
                } else if (mMediaPlayerState == MediaPlayerState.STATE_PAUSED) {
                    mMediaPlayer.resumePlayback();
                } else {
                    Log.w(TAG, "Player is playing, episode cannot be null");
                }
                break;
            case MediaPlayerState.STATE_ENDED:
            case MediaPlayerState.STATE_IDLE:
                // stopped or uninitialized, so we need to start from scratch
                if (episode != null) {
                    startPlayback(episode, playImmediately);
                } else {
                    Log.w(TAG, "Player is stopped/uninitialized, episode cannot be null");
                }
                break;
            default:
                Log.w(TAG, "Trying to play an episode, but player is in state: " + mPlaybackState);
                break;
        }
    }

    /**
     * Will pause the player if it's playing
     */
    private void pause() {

        switch (mMediaPlayerState) {
            case MediaPlayerState.STATE_PLAYING:
                // we paused, resume playing state
                pausePlayback();
                break;
            default:
                Log.w(TAG, "Trying to pause an episode, but player is in state: " + mPlaybackState);
                break;
        }
    }

    /**
     * Seek forward by the user's preferred amount
     */
    private void seekForward() {

        switch (mMediaPlayerState) {
            case MediaPlayerState.STATE_PAUSED:
            case MediaPlayerState.STATE_PLAYING:
                long seekTo = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(getString(R.string.pref_key_skip_forward), "30")) * 1000;
                seekPlayerBy(seekTo);
                break;
            default:
                Log.w(TAG, "Trying to play an episode, but player is in state: " + mPlaybackState);
                break;
        }
    }

    /**
     * Seek forward by the user's preferred amount
     */
    private void seekBackward() {

        switch (mMediaPlayerState) {
            case MediaPlayerState.STATE_PAUSED:
            case MediaPlayerState.STATE_PLAYING:
                long seekTo = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(getString(R.string.pref_key_skip_backward), "30")) * 1000;
                seekPlayerBy(-seekTo);
                break;
            default:
                Log.w(TAG, "Trying to play an episode, but player is in state: " + mPlaybackState);
                break;
        }
    }

    private void seekTo(long seekTo) {

        switch (mMediaPlayerState) {
            case MediaPlayerState.STATE_PAUSED:
            case MediaPlayerState.STATE_PLAYING:
                seekPlayerTo(seekTo);
                break;
            default:
                Log.w(TAG, "Trying to play an episode, but player is in state: " + mPlaybackState);
                break;
        }
    }

    private void changePlaybackSpeed(float speed) {

        switch (mMediaPlayerState) {
            case MediaPlayerState.STATE_PAUSED:
            case MediaPlayerState.STATE_PLAYING:
                mMediaPlayer.setPlaybackSpeed(speed);
                onStateChanged(mMediaPlayerState);
                break;
        }
    }

    /******************************
     * Widget update related functions
     ******************************/

    private void updateWidget() {
        RemoteViews remoteViews = WidgetProvider.configureWidgetIntents(this, mCurrentEpisode);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(getApplicationContext(), WidgetProvider.class);
        int playPauseResId = PlaybackButtonHelper.getWidgetPlaybackButtonResId(getPlaybackState());
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        if (allWidgetIds == null || allWidgetIds.length == 0) {
            return;
        }

        if (mCurrentEpisode == null) {
            remoteViews.setTextViewText(R.id.episode_title, "");
            remoteViews.setTextViewText(R.id.channel_title, "");
            remoteViews.setImageViewResource(R.id.play, playPauseResId);
            remoteViews.setImageViewResource(R.id.channel_art, R.drawable.default_channel_art);
        } else {
            ImageLoadHelper.loadImageIntoWidget(this, mCurrentEpisode.getChannelArtworkUrl(), remoteViews,
                    R.id.channel_art, allWidgetIds, new RoundedCornersTransformation(this));
            remoteViews.setOnClickPendingIntent(R.id.play, PendingIntentHelper.getPlayOrPauseIntent(this, getPlaybackState()));
            remoteViews.setTextViewText(R.id.episode_title, mCurrentEpisode.getTitle());
            remoteViews.setTextViewText(R.id.channel_title, mCurrentEpisode.getChannelTitle());
            remoteViews.setImageViewResource(R.id.play, playPauseResId);

        }
        for (int widgetId : allWidgetIds) {
            // assign the pending intent to our remote views
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    /******************************
     * Helper functions
     ******************************/

    public static void playPlaylist(Context context, Playlist playlist) {
        Intent intent = new Intent(context, PodcastPlayerService.class);
        intent.setAction(PodcastPlayerService.ACTION_PLAY_PLAYLIST);
        intent.putExtra(PARAM_PLAYLIST, Parcels.wrap(playlist));
        context.startService(intent);
    }

    public static void startCast(Context context, CastDevice castDevice) {
        Intent intent = new Intent(context, PodcastPlayerService.class);
        intent.setAction(PodcastPlayerService.ACTION_START_CAST);
        intent.putExtra(PARAM_CAST_DEVICE, castDevice);
        context.startService(intent);
    }

    public static void endCast(Context context) {
        Intent intent = new Intent(context, PodcastPlayerService.class);
        intent.setAction(PodcastPlayerService.ACTION_END_CAST);
        context.startService(intent);
    }

    public static void changePlaybackSpeed(Context context, float speed) {
        Intent intent = new Intent(context, PodcastPlayerService.class);
        intent.setAction(ACTION_SET_SPEED);
        intent.putExtra(PARAM_PLAYBACK_SPEED, speed);
        context.startService(intent);
    }

    public static void sendIntent(Context context, String action, int episodeId) {
        Intent intent = new Intent(context, PodcastPlayerService.class);
        intent.setAction(action);

        if (episodeId != -1) {
            intent.putExtra(PARAM_EPISODE_ID, episodeId);
        }
        context.startService(intent);
    }

    private final class MediaSessionCallback extends MediaSession.Callback {

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            int episodeId = extras.getInt(PARAM_EPISODE_ID, -1);

            if (episodeId != -1) {
                play(EpisodeModel.getEpisodeById(PodcastPlayerService.this, episodeId), true);
            }
        }

        @Override
        public void onSeekTo(long pos) {
            seekTo(pos);
        }

        @Override
        public void onPlay() {
            play(-1);
        }

        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onFastForward() {
            seekForward();
        }

        @Override
        public void onRewind() {
            seekBackward();
        }

        @Override
        public void onSkipToNext() {
            onFastForward();
        }

        @Override
        public void onSkipToPrevious() {
            onRewind();
        }
    }

    /******************************
     * Functions for loading podcast artwork
     ******************************/

    private void startNotificationUpdate() {
        ImageLoadHelper.loadImageAsync(this, mCurrentEpisode.getChannelArtworkUrl(), new ImageLoadHelper.OnImageLoaded() {
            @Override
            public void imageLoaded(Bitmap bitmap) {
                updatePlayNotification(bitmap);
            }

            @Override
            public void imageFailed() {
                updatePlayNotification(BitmapFactory.decodeResource(
                        PodcastPlayerService.this.getResources(),
                        R.drawable.default_channel_art));
            }
        });
    }

    private void updatePlayNotification(Bitmap channelArt) {

        if (mCurrentEpisode == null) {
            return;
        }

        mMediaSession.setMetadata(new MediaMetadata.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, channelArt)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, mCurrentEpisode.getChannelAuthor())
                .putString(MediaMetadata.METADATA_KEY_AUTHOR, mCurrentEpisode.getChannelAuthor())
                .putString(MediaMetadata.METADATA_KEY_ALBUM, mCurrentEpisode.getChannelTitle())
                .putString(MediaMetadata.METADATA_KEY_TITLE, mCurrentEpisode.getTitle())
                .putLong(MediaMetadata.METADATA_KEY_DURATION, mCurrentEpisode.getDuration())
                .build());
        mMediaNotificationManager.startNotification(mCurrentEpisode, mMediaSession, channelArt);
    }

    private void startUpdateTask() {

        if (mUpdateTask == null) {
            mUpdateTask = new UpdateEpisodeProgressTask();
            mUpdateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void endUpdateTask() {

        if (mUpdateTask != null) {
            mUpdateTask.cancel(true);
            mUpdateTask = null;
        }
    }

    /**
     * AsyncTask that periodically updates the episodes play progress
     */
    private class UpdateEpisodeProgressTask extends AsyncTask<Void, Void, Void> {

        private static final int UPDATE_DELAY = 20_000;
        private final Bundle mBundle;

        public UpdateEpisodeProgressTask() {
            mBundle = new Bundle();
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "Update task cancelled");
        }

        @Override
        protected Void doInBackground(Void... aVoid) {

            while (!isCancelled()) {

                try {
                    Log.d(TAG, "Updating episode");
                    mCurrentEpisode.setProgress(getProgress());
                    mCurrentEpisode.setDuration(getDuration());
                    mBundle.clear();
                    mBundle.putLong(EpisodeModel.PARAM_EPISODE_PROGRESS, mCurrentEpisode.getProgress());
                    mBundle.putLong(EpisodeModel.PARAM_EPISODE_DURATION, mCurrentEpisode.getDuration());
                    EpisodeModel.updateEpisodeAsync(PodcastPlayerService.this,
                            mCurrentEpisode.getId(), mBundle);
                    Thread.sleep(UPDATE_DELAY);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Update task interrupted");
                }
            }
            return null;
        }
    }

    /**
     * Receives intents related to unplugging of wired and wireless headsets
     */
    private class HeadsetReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (isInitialStickyBroadcast()) {
                return;
            }

            String action = intent.getAction();
            Log.d(TAG, "Action: " + action);

            // physical headphone events
            switch (action) {

                case Intent.ACTION_HEADSET_PLUG:
                    int state = intent.getIntExtra("state", -1);

                    switch (state) {
                        case 0:
                            pause();
                            Log.d(TAG, "Headset unplugged");
                            break;
                    }
                    break;
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    pause();
                    break;
            }
        }
    }

    /**
     * Listens for episode download notifications.  If the episode downloaded is streaming, this will
     * restart playback palying the downloaded episode.
     */
    private class EpisodeDownloadedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null && intent.getAction().contentEquals(
                    BroadcastHelper.INTENT_EPISODE_DOWNLOADED)) {

                int episodeId = intent.getIntExtra(BroadcastHelper.EXTRA_EPISODE_ID, -1);

                if (mCurrentEpisode != null && mCurrentEpisode.getId() == episodeId &&
                        mMediaPlayer instanceof LocalMediaPlayer) {
                    Log.w(TAG, "Streaming episode has downloaded, switching playback to local file");

                    boolean restartPlayback = mMediaPlayerState == MediaPlayerState.STATE_PLAYING;

                    // get the current position
                    long seek = getProgress();

                    // start playing episode
                    play(EpisodeModel.getEpisodeById(context, episodeId), restartPlayback);

                    // set the previous position
                    seekPlayerTo(seek);
                }
            }
        }
    }
}