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
import android.util.Log;

import com.mainmethod.premofm.service.ApiService;

/**
 * Created by evan on 3/3/15.
 */
public class PushChangesJobService extends PremoJobService {

    private static final String TAG = PushChangesJobService.class.getSimpleName();
    private static final int JOB_ID = PushChangesJobService.class.hashCode();
    private static final int MINIMUM_LATENCY = 900_000;
    private static final int OVERRIDE_DEADLINE = 1_800_000;

    @Override
    public boolean onStartJob(JobParameters params) {
        ApiService.start(this, ApiService.ACTION_PUSH_LOCAL_CHANGES);
        jobFinished(params, false);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void schedule(Context context) {

        if (isJobScheduled(context, JOB_ID)) {
            Log.d(TAG, "This job has already been scheduled, skipping");
            return;
        }
        Log.d(TAG, "Scheduling job service for pushing local changes");
        ComponentName comp = new ComponentName(context, PushChangesJobService.class);
        JobInfo job = new JobInfo.Builder(JOB_ID, comp)
                .setMinimumLatency(MINIMUM_LATENCY)
                .setOverrideDeadline(OVERRIDE_DEADLINE)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();

        JobScheduler scheduler = (JobScheduler) context.getSystemService(
                Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(job);
    }

    public static void unschedule(Context context) {
        Log.d(TAG, "Unscheduling job service for pushing local changes");
        JobScheduler scheduler = (JobScheduler) context.getSystemService(
                Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(JOB_ID);
    }
}
