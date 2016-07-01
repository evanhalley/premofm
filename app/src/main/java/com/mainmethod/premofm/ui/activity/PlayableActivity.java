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
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.DialogHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.PodcastPlayerService;
import com.mainmethod.premofm.ui.dialog.QueueDialogFragment;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Handles functionality for controlling audio from an activity
 * Created by evan on 12/28/14.
 */
public abstract class PlayableActivity extends BaseActivity {

    private PodcastPlayerService podcastPlayerService;
    private MediaController mediaController;
    private MediaRouter mediaRouter;
    private MediaRouteSelector mediaRouteSelector;
    private MediaRouterCallback mediaRouterCallback;
    private MediaControllerCallback mediaControllerCallback;
    private PlayerConnection playerConnection;

    protected abstract void onPodcastPlayerServiceBound();

    protected void onPodcastPlayerServiceBoundBase() {
        mediaController = new MediaController(getApplicationContext(), podcastPlayerService.getMediaSessionToken());
        mediaController.registerCallback(mediaControllerCallback);
        onStateChanged(getState(), getEpisode());
        onPodcastPlayerServiceBound();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaRouter = MediaRouter.getInstance(this);
        mediaRouteSelector = new MediaRouteSelector.Builder()
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
        mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaRouterCallback = new MediaRouterCallback(this);
        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        Intent intent = new Intent(this, PodcastPlayerService.class);
        startService(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mediaRouter.removeCallback(mediaRouterCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mediaController != null) {
            mediaController.unregisterCallback(mediaControllerCallback);
        }
        getApplicationContext().unbindService(playerConnection);
        playerConnection = null;
        podcastPlayerService = null;
        mediaControllerCallback = null;
        mediaController = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaControllerCallback = new MediaControllerCallback(this);
        bindToPodcastPlayService();
    }

    protected final boolean isStreaming() {
        return podcastPlayerService != null && podcastPlayerService.isStreaming();
    }

    protected int getState() {
        int state = PlaybackState.STATE_NONE;

        if (mediaController != null && mediaController.getPlaybackState() != null) {
            state = mediaController.getPlaybackState().getState();
        }
        return state;
    }

    protected Episode getEpisode() {
        Episode episode = null;
        final AppPrefHelper appPrefHelper = AppPrefHelper.getInstance(this);
        int episodeId = -1;

        if (mediaController != null && mediaController.getExtras() != null &&
                mediaController.getExtras().containsKey(PodcastPlayerService.PARAM_EPISODE_ID)) {
            episodeId = mediaController.getExtras().getInt(PodcastPlayerService.PARAM_EPISODE_ID);
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
        Timber.d("Binding to PlayService");
        playerConnection = new PlayerConnection(this);
        Intent intent = new Intent(this, PodcastPlayerService.class);
        boolean bound = getApplicationContext().bindService(intent, playerConnection, 0);
        Timber.d("Bound: %s", String.valueOf(bound));
    }

    public void onPlayEpisode() {
        Episode episode = getEpisode();

        if (episode != null) {
            Bundle extras = new Bundle();
            extras.putInt(PodcastPlayerService.PARAM_EPISODE_ID, episode.getId());
            mediaController.getTransportControls().playFromSearch(null, extras);
        }
    }

    public void onResumePlayback() { mediaController.getTransportControls().play(); }

    public void onPauseEpisode() {
        mediaController.getTransportControls().pause();
    }

    protected void onSeekForward() {
        mediaController.getTransportControls().fastForward();
    }

    protected void onSeekBackward() {
        mediaController.getTransportControls().rewind();
    }

    protected void onSeekTo(int seekTo) {
        mediaController.getTransportControls().seekTo(seekTo);
    }

    protected void showPlayQueue() {
        QueueDialogFragment fragment = new QueueDialogFragment();
        fragment.setEpisode(getEpisode());
        fragment.show(getFragmentManager(), "Queue");
    }

    protected void favoriteEpisode(EpisodeModel.OnToggleFavoriteEpisodeListener listener) {

        if (getEpisode() != null) {
            EpisodeModel.toggleFavoriteAsync(this, getEpisode().getId(), listener);
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
            Timber.d("Service connected: %s", name.flattenToString());
            PlayableActivity activity = mActivity.get();

            if (activity == null) {
                return;
            }

            if (service instanceof PodcastPlayerService.ServiceBinder) {
                activity.podcastPlayerService = ((PodcastPlayerService.ServiceBinder) service).getService();
                activity.onPodcastPlayerServiceBoundBase();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.d("Service disconnected: %s", name.flattenToString());
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
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            PodcastPlayerService.endCast(mContext.get());
        }
    }
}