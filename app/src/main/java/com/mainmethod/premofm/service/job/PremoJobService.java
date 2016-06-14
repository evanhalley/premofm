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
        return getJobInfo(context, jobId) != null;
    }

    public static JobInfo getJobInfo(Context context, int jobId) {
        List<JobInfo> jobs = ((JobScheduler) context.getSystemService(
                Context.JOB_SCHEDULER_SERVICE)).getAllPendingJobs();

        if (jobs != null) {

            for (int i = 0; i < jobs.size(); i++) {

                if (jobs.get(i).getId() == jobId) {
                    return jobs.get(i);
                }
            }
        }
        return null;
    }
}