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

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.service.PodcastSyncService;

import timber.log.Timber;

/**
 * Created by evan on 1/22/15.
 */
public class PodcastSyncJobService extends PremoJobService {

    private static final int JOB_ID = PodcastSyncJobService.class.hashCode();

    @Override
    public boolean onStartJob(JobParameters params) {
        Timber.d("Starting podcast sync job service");
        PodcastSyncService.syncAllPodcasts(this, true);
        jobFinished(params, false);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    /**
     * Schedules the podcast sync job service which kicks off the download service for caching episodes
     */
    public static void schedule(Context context) {

        if (isJobScheduled(context, JOB_ID)) {
            Timber.d("This job has already been scheduled, ending");
            JobScheduler scheduler = (JobScheduler) context.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
            scheduler.cancel(JOB_ID);
        }

        if (!UserPrefHelper.get(context).getBoolean(R.string.pref_key_enable_syncing)) {
            return;
        }

        Timber.d("Scheduling");
        UserPrefHelper helper = UserPrefHelper.get(context);
        ComponentName comp = new ComponentName(context, PodcastSyncJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, comp)
                .setPersisted(true)
                .setRequiresDeviceIdle(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresCharging(false)
                .setPeriodic(helper.getStringAsInt(R.string.pref_key_syncing_period) * 60_000);

        JobInfo job = builder.build();
        JobScheduler scheduler = (JobScheduler) context.getSystemService(
                Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(job);
    }

    /**
     * Cancels this job service if it's currently schedule or running
     */
    public static void cancel(Context context) {

        if (!isJobScheduled(context, JOB_ID)) {
            Timber.d("This job hasn't already been scheduled, no need to cancel");
            return;
        }
        JobScheduler scheduler = (JobScheduler) context.getSystemService(
                Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(JOB_ID);
    }
}