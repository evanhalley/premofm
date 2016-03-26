/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.service.job;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.service.ApiService;

/**
 * Schedules a task to update episodes
 * Created by evan on 4/24/15.
 */
public class PushCollectionChangesJobService extends PremoJobService {

    private static final String TAG = PushCollectionChangesJobService.class.getSimpleName();
    private static final int JOB_ID = PushCollectionChangesJobService.class.hashCode();
    private static final int OVERRIDE_DEADLINE = 300_000;

    private JobParameters mJobParameters;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            jobFinished(mJobParameters, intent.getBooleanExtra(
                    BroadcastHelper.EXTRA_IS_SUCCESSFUL, false));
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }
    };

    @Override
    public boolean onStartJob(JobParameters params) {
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(BroadcastHelper.INTENT_PUSH_COLLECTION_SERVICE_FINISHED));
        mJobParameters = params;
        ApiService.start(this, ApiService.ACTION_PUSH_COLLECTION_CHANGES);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        return true;
    }

    public static void schedule(Context context) {

        if (isJobScheduled(context, JOB_ID)) {
            Log.d(TAG, "This job has already been scheduled, skipping");
            return;
        }

        Log.d(TAG, "Scheduling job service for pushing collection changes");
        ComponentName comp = new ComponentName(context, PushCollectionChangesJobService.class);
        Log.d(TAG, String.format("Scheduling new job, override deadline: %d",
                OVERRIDE_DEADLINE));
        JobInfo job = new JobInfo.Builder(JOB_ID, comp)
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
}