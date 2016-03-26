/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.api.ApiHelper;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AnalyticsHelper;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.DialogHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.PodcastPlayerService;
import com.mainmethod.premofm.ui.dialog.QueueDialogFragment;

import java.lang.ref.WeakReference;

/**
 * Handles functionality for controlling audio from an activity
 * Created by evan on 12/28/14.
 */
public abstract class PlayableActivity extends BaseActivity {

    private static final String TAG = PlayableActivity.class.getSimpleName();

    private PodcastPlayerService mPodcastPlayerService;
    private MediaController mMediaController;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouterCallback mMediaRouterCallback;
    private MediaControllerCallback mMediaControllerCallback;
    private PlayerConnection mConnection;

    protected abstract void onPodcastPlayerServiceBound();

    protected void onPodcastPlayerServiceBoundBase() {
        mMediaController = new MediaController(getApplicationContext(), mPodcastPlayerService.getMediaSessionToken());
        mMediaController.registerCallback(mMediaControllerCallback);
        onStateChanged(getState(), getEpisode());
        onPodcastPlayerServiceBound();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaRouter = MediaRouter.getInstance(this);
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(BuildConfig.CAST_APP_ID))
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // create the case button
        MenuItem item = menu.add(Menu.NONE, R.string.action_cast, 1, R.string.action_cast);
        MediaRouteActionProvider mediaRouteActionProvider = new MediaRouteActionProvider(
                new ContextThemeWrapper(this, R.style.AppTheme_CastIcon));
        MenuItemCompat.setActionProvider(item, mediaRouteActionProvider);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouterCallback = new MediaRouterCallback(this);
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        Intent intent = new Intent(this, PodcastPlayerService.class);
        startService(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
        getApplicationContext().unbindService(mConnection);
        mConnection = null;
        mPodcastPlayerService = null;
        mMediaControllerCallback = null;
        mMediaController = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaControllerCallback = new MediaControllerCallback(this);
        bindToPodcastPlayService();
    }

    protected final boolean isStreaming() {
        return mPodcastPlayerService != null && mPodcastPlayerService.isStreaming();
    }

    protected int getState() {
        int state = PlaybackState.STATE_NONE;

        if (mMediaController != null && mMediaController.getPlaybackState() != null) {
            state = mMediaController.getPlaybackState().getState();
        }
        return state;
    }

    protected Episode getEpisode() {
        Episode episode = null;
        final AppPrefHelper appPrefHelper = AppPrefHelper.getInstance(this);
        int episodeId = -1;

        if (mMediaController != null && mMediaController.getExtras() != null &&
                mMediaController.getExtras().containsKey(PodcastPlayerService.PARAM_EPISODE_ID)) {
            episodeId = mMediaController.getExtras().getInt(PodcastPlayerService.PARAM_EPISODE_ID);
        }  else if (appPrefHelper.getLastPlayedEpisodeId() != -1) {
            episodeId = appPrefHelper.getLastPlayedEpisodeId();
        }

        if (episodeId != -1) {
            episode = EpisodeModel.getEpisodeById(this, episodeId);
        }
        return episode;
    }

    public abstract void onStateChanged(int state, Episode episode);

    private void bindToPodcastPlayService() {
        Log.d(TAG, "Binding to PlayService");
        mConnection = new PlayerConnection(this);
        Intent intent = new Intent(this, PodcastPlayerService.class);
        boolean bound = getApplicationContext().bindService(intent, mConnection, 0);
        Log.d(TAG, "Bound: " + bound);
    }

    public void onPlayEpisode() {
        Episode episode = getEpisode();

        if (episode != null) {
            Bundle extras = new Bundle();
            extras.putInt(PodcastPlayerService.PARAM_EPISODE_ID, episode.getId());
            mMediaController.getTransportControls().playFromSearch(null, extras);
        }
    }

    public void onResumePlayback() { mMediaController.getTransportControls().play(); }

    public void onPauseEpisode() {
        mMediaController.getTransportControls().pause();
    }

    protected void onSeekForward() {
        mMediaController.getTransportControls().fastForward();
    }

    protected void onSeekBackward() {
        mMediaController.getTransportControls().rewind();
    }

    protected void onSeekTo(int seekTo) {
        mMediaController.getTransportControls().seekTo(seekTo);
    }

    protected void showPlayQueue() {
        QueueDialogFragment fragment = new QueueDialogFragment();
        fragment.setEpisode(getEpisode());
        fragment.show(getFragmentManager(), "Queue");
    }

    protected void favoriteEpisode(ApiHelper.OnToggleFavoriteEpisodeListener listener) {

        if (getEpisode() != null) {
            ApiHelper.toggleFavoriteAsync(this, getEpisode().getId(), listener);
        }
    }

    protected void showEpisodeInformation() {

        if (getEpisode() != null) {
            showEpisodeInformation(this, getEpisode());
        }
    }

    public static void showEpisodeInformation(final Context context, final Episode episode) {
        DialogHelper.openWebviewDialog(context, episode.getTitle(), episode.getUrl(), episode.getDescriptionHtml());
    }

    private static class PlayerConnection implements ServiceConnection {

        private WeakReference<PlayableActivity> mActivity;

        public PlayerConnection(PlayableActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected: " + name.flattenToString());
            PlayableActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }

            if (service instanceof PodcastPlayerService.ServiceBinder) {
                activity.mPodcastPlayerService = ((PodcastPlayerService.ServiceBinder) service).getService();
                activity.onPodcastPlayerServiceBoundBase();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected: " + name.flattenToString());
        }
    }

    private static class MediaControllerCallback extends MediaController.Callback {

        private WeakReference<PlayableActivity> mActivity;

        public MediaControllerCallback(PlayableActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            onStateChanged();
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            onStateChanged();
        }

        private void onStateChanged() {
            PlayableActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }

            activity.onStateChanged(activity.getState(), activity.getEpisode());
        }
    }

    private static class MediaRouterCallback extends MediaRouter.Callback {

        private final WeakReference<Context> mContext;

        private MediaRouterCallback(Context context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
            PodcastPlayerService.startCast(mContext.get(), castDevice);
            AnalyticsHelper.sendEvent(mContext.get(),
                    AnalyticsHelper.CATEGORY_CAST,
                    AnalyticsHelper.ACTION_CLICK,
                    null);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            PodcastPlayerService.endCast(mContext.get());
        }
    }
}