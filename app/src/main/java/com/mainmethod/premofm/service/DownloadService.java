/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.task.DownloadEpisodesTask;

import timber.log.Timber;

/**
 * Downloads episodes in the background
 * The service will be passed a list of episodes to download and will download them
 * Created by evan on 1/21/15.
 */
public class DownloadService extends Service implements
        DownloadEpisodesTask.OnDownloadEpisodesTaskListener {

    public static final String ACTION_MANUAL_DOWNLOAD   = "com.mainmethod.premofm.manualDownload";
    public static final String ACTION_AUTO_DOWNLOAD     = "com.mainmethod.premofm.autoDownload";
    public static final String ACTION_CANCEL_SERVICE    = "com.mainmethod.premofm.cancelService";
    public static final String ACTION_CANCEL_DOWNLOAD   = "com.mainmethod.premofm.cancelDownload";
    public static final String PARAM_EPISODE_ID         = "episodeId";

    private DownloadEpisodesTask mTask;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
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
            Timber.d("Handling action %s", action);

            switch (action) {
                case ACTION_MANUAL_DOWNLOAD:
                    handleManualDownloadAction(intent.getIntExtra(PARAM_EPISODE_ID, -1));
                    break;
                case ACTION_AUTO_DOWNLOAD:
                    handleAutoDownloadAction();
                    break;
                case ACTION_CANCEL_SERVICE:

                    // if the task is running, let's cancel it
                    if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
                        mTask.cancel(false);
                    }
                    break;
                case ACTION_CANCEL_DOWNLOAD:
                    handleCancelDownloadAction(intent.getIntExtra(PARAM_EPISODE_ID, -1));
                    break;
            }
        }
        return START_STICKY;
    }

    /******************************
     * Jump off points for downloading episodes
     ******************************/

    private void handleAutoDownloadAction() {

        // only run the auto download if the task isn't already running
        if (mTask == null || mTask.getStatus() != AsyncTask.Status.RUNNING) {
            startDownloadTask();
        }
    }

    private void handleManualDownloadAction(int episodeId) {

        if (episodeId == -1) {
            return;
        }
        Bundle params = new Bundle();

        // if the task is running, mark episode as queued so it's picked up by the download queue
        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
            params.putInt(EpisodeModel.PARAM_DOWNLOAD_STATUS, DownloadStatus.QUEUED);
            EpisodeModel.updateEpisodeAsync(this, episodeId, params);
        }

        // else mark it was requested and start the download task
        else {
            params.putInt(EpisodeModel.PARAM_DOWNLOAD_STATUS, DownloadStatus.REQUESTED);
            params.putInt(EpisodeModel.PARAM_MANUAL_DOWNLOAD, 1);
            EpisodeModel.updateEpisodeAsync(this, episodeId, params, () -> startDownloadTask());
        }
    }

    private void handleCancelDownloadAction(int episodeId) {

        if (episodeId == -1) {
            return;
        }

        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
            mTask.cancelDownload(episodeId);
        } else {
            EpisodeModel.markEpisodeDeleted(this, episodeId);
        }
    }

    private void startDownloadTask() {
        mTask = new DownloadEpisodesTask(this, this);
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /******************************
     * Service bind functions
     ******************************/

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.d("Unbinded from service");
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /******************************
     * Listener functions
     ******************************/

    @Override
    public void onFinish() {
        BroadcastHelper.broadcastDownloadServiceFinished(this);
        Intent intent = new Intent(this, DeleteEpisodeService.class);
        intent.setAction(DeleteEpisodeService.ACTION_ELIGIBLE_EPISODES);
        startService(intent);
        stopSelf();
    }

    /******************************
     * Helper functions for sending
     * Download Service intents
     ******************************/

    public static void downloadEpisode(final Context context, int episodeId) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_MANUAL_DOWNLOAD);
        intent.putExtra(PARAM_EPISODE_ID, episodeId);
        context.startService(intent);
    }

    public static void cancelEpisode(final Context context, int episodeId) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_CANCEL_DOWNLOAD);
        intent.putExtra(PARAM_EPISODE_ID, episodeId);
        context.startService(intent);
    }

    public static boolean canRunDownloadService(Context context) {
        boolean downloadPermitted = false;
        UserPrefHelper helper = UserPrefHelper.get(context);
        // get user preferences
        boolean onlyDownloadOnWifi = helper.getBoolean(context.getString(
                R.string.pref_key_auto_download_only_on_wifi));
        boolean requiresCharging = helper.getBoolean(context.getString(
                R.string.pref_key_auto_download_charging_only));

        // get the battery status
        Intent batteryStatus = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = -1;

        if (batteryStatus != null) {
            status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, status);
        }
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        Timber.d("Device is charging: " + isCharging);

        // get connectivity status
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean wifiConnected = wifiInfo != null && wifiInfo.isConnected();
        boolean mobileConnected = mobileInfo != null && mobileInfo.isConnected();

        Timber.d("WiFi connected: " + wifiConnected);
        Timber.d("Mobile connected: " + mobileConnected);

        // we are charging and charging is required or charging is not required
        if ((isCharging && requiresCharging) || !requiresCharging)  {

            // if only download on wifi and wifi connected or download on wifi/mobile and mobile connected
            if ((onlyDownloadOnWifi && wifiConnected) || !onlyDownloadOnWifi) {

                if (wifiConnected || mobileConnected) {
                    downloadPermitted = true;
                } else {
                    Timber.d("No mobile or wifi connection");
                }
            } else {
                Timber.d("Connectivity state prevents download");
            }
        } else {
            Timber.d("Charge state prevents download");
        }
        Timber.d("Download permitted: %s", downloadPermitted);
        return downloadPermitted;
    }
}
