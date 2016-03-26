/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.adapter;

import android.content.Context;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.object.EpisodeStatus;

/**
 * Created by evan on 10/17/15.
 */
public class AdapterHelper {

    public static String buildDurationString(Context context, int status, long duration, long progress) {
        long timeLeft = duration;
        String string = null;

        switch (status) {
            case EpisodeStatus.NEW:
            case EpisodeStatus.IN_PROGRESS:
            case EpisodeStatus.PLAYED:
                if (progress > -1 && progress <= duration) {
                    timeLeft -= progress;
                    string = String.format(context.getString(R.string.duration_in_progress),
                            DatetimeHelper.convertSecondsToReadableDuration(timeLeft));
                } else {
                    string = String.format(context.getString(R.string.duration_in_progress),
                            DatetimeHelper.convertSecondsToReadableDuration(timeLeft));
                }
                break;
            case EpisodeStatus.COMPLETED:
                string = String.format(context.getString(R.string.duration_finished),
                        DatetimeHelper.convertSecondsToReadableDuration(timeLeft));
                break;
        }
        return string;
    }

}
