/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mainmethod.premofm.service.PodcastPlayerService;

import java.util.Calendar;

/**
 * Helper functions for setting and cancelling the sleep timer
 * Created by evan on 3/12/15.
 */
public class SleepTimerHelper {

    private static final String TAG = SleepTimerHelper.class.getSimpleName();

    public static boolean timerIsActive(Context context) {
        return AppPrefHelper.getInstance(context).getSleepTimer() != -1;
    }

    /**
     * Set's the timer to stop the podcast service
     * @param context
     * @param minutes
     */
    public static void setTimer(Context context, int minutes) {
        Log.d(TAG, String.format("Setting alarm to ring %d minute(s) from now", minutes));

        if (minutes < 0) {
            return;
        }
        // set up the alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // send an intent to stop playback
        Intent intent = new Intent(context.getApplicationContext(), PodcastPlayerService.class);
        intent.setAction(PodcastPlayerService.ACTION_SLEEP_TIMER);

        // record the set time so we can cancel it if we need to
        long timeToAlarm = Calendar.getInstance().getTimeInMillis() + (minutes * 60 * 1000);
        AppPrefHelper.getInstance(context).setSleepTimer(timeToAlarm);

        // set the alarm
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                timeToAlarm,
                PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static void setTimerChoice(Context context, int choice) {
        int minutes = -1;

        switch (choice) {
            case 0:
                minutes = 5;
                break;
            case 1:
                minutes = 10;
                break;
            case 2:
                minutes = 15;
                break;
            case 3:
                minutes = 30;
                break;
            case 4:
                minutes = 60;
                break;
        }

        if (minutes == -1) {
            return;
        }
        setTimer(context, minutes);
    }

    /**
     * Cancels an alarm that stops the podcast service
     * @param context
     */
    public static void cancelTimer(Context context) {
        Log.d(TAG, "Cancelling timers");
        // set up the alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // send an intent to stop playback
        Intent intent = new Intent(context.getApplicationContext(), PodcastPlayerService.class);
        intent.setAction(PodcastPlayerService.ACTION_SLEEP_TIMER);

        // cancel the alarm
        alarmManager.cancel(PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT));

        // erase the stored sleep timer
        AppPrefHelper.getInstance(context).removeSleepTimer();
    }
}