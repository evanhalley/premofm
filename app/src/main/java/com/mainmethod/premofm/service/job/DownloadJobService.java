/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.service.job;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.service.DownloadService;

/**
 * Created by evan on 1/22/15.
 */
public class DownloadJobService extends PremoJobService {

    private static final String TAG = DownloadJobService.class.getSimpleName();
    public static final int JOB_ID = DownloadJobService.class.hashCode();
    private static final int LATER = 120_000;
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Starting download service");
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(DownloadService.ACTION_AUTO_DOWNLOAD);
        startService(intent);
        jobFinished(params, false);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void scheduleEpisodeDownloadNow(Context context) {

        if (UserPrefHelper.get(context).getBoolean(R.string.pref_key_enable_auto_download)) {
            scheduleEpisodeDownload(context, -1);
        }
    }

    public static void scheduleEpisodeDownload(Context context) {

        if (UserPrefHelper.get(context).getBoolean(R.string.pref_key_enable_auto_download)) {
            scheduleEpisodeDownload(context, LATER);
        }
    }

    /**
     * Schedules the download job service which kicks off the download service for caching episodes
     */
    private static void scheduleEpisodeDownload(Context context, int minimumLatency) {

        if (isJobScheduled(context, JOB_ID)) {
            Log.d(TAG, "This job has already been scheduled, skipping");
            return;
        }
        Log.d(TAG, "Scheduling");
        UserPrefHelper helper = UserPrefHelper.get(context);
        boolean onlyDownloadOnWifi = helper.getBoolean(context.getString(
                R.string.pref_key_auto_download_only_on_wifi));
        boolean requiresCharging = helper.getBoolean(context.getString(
                R.string.pref_key_auto_download_charging_only));
        ComponentName comp = new ComponentName(context, DownloadJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, comp)
                .setPersisted(true)
                .setRequiresDeviceIdle(false)
                .setRequiredNetworkType(onlyDownloadOnWifi ? JobInfo.NETWORK_TYPE_UNMETERED :
                        JobInfo.NETWORK_TYPE_ANY)
                .setRequiresCharging(requiresCharging);

        if (minimumLatency > -1) {
            builder.setMinimumLatency(BuildConfig.DEBUG ? 1_000 : minimumLatency);
        }
        JobInfo job = builder.build();
        JobScheduler scheduler = (JobScheduler) context.getSystemService(
                Context.JOB_SCHEDULER_SERVICE);
        int result = scheduler.schedule(job);
        Log.d(TAG, result + " scheduler result");
    }

    /**
     * Cancels this job service if it's currently schedule or running
     * @param context
     */
    public static void cancelScheduledEpisodeDownload(Context context) {

        if (!isJobScheduled(context, JOB_ID)) {
            Log.d(TAG, "This job hasn't already been scheduled, no need to cancel");
            return;
        }

        JobScheduler scheduler = (JobScheduler) context.getSystemService(
                Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(JOB_ID);
    }
}