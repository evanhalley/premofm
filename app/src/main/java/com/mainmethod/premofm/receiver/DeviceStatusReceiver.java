/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mainmethod.premofm.service.DownloadService;
import com.mainmethod.premofm.service.job.DownloadJobService;
import com.mainmethod.premofm.service.job.PremoJobService;

/**
 * Created by evan on 9/15/15.
 */
public class DeviceStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // this method is hit if something changes with wifi connectivity and or charging requirements

        // can we run the downloader
        boolean downloadPermitted = DownloadService.canRunDownloadService(context);

        if (downloadPermitted) {

            if (!PremoJobService.isJobScheduled(context, DownloadJobService.JOB_ID)) {
                DownloadJobService.scheduleEpisodeDownload(context);
            }
        }

        // no downloads
        else {
            DownloadJobService.cancelScheduledEpisodeDownload(context);
            Intent cancelIntent = new Intent(context, DownloadService.class);
            cancelIntent.setAction(DownloadService.ACTION_CANCEL_SERVICE);
            context.startService(cancelIntent);
        }
    }
}
