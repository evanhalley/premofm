/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.service.job;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;

import java.util.List;

/**
 * Created by evan on 4/24/15.
 */
public abstract class PremoJobService extends JobService {

    public static boolean isJobScheduled(Context context, int jobId) {
        boolean isScheduled = false;
        JobScheduler scheduler = (JobScheduler) context.getSystemService(
                Context.JOB_SCHEDULER_SERVICE);

        // only schedule the job if it hasn't already been scheduled
        List<JobInfo> jobs = scheduler.getAllPendingJobs();

        if (jobs != null) {

            for (JobInfo job : jobs) {

                if (job.getId() == jobId) {
                    isScheduled = true;
                    break;
                }
            }
        }
        return isScheduled;
    }
}