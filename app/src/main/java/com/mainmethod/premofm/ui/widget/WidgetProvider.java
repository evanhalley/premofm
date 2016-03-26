/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.media.session.PlaybackState;
import android.util.Log;
import android.widget.RemoteViews;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.PendingIntentHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.PodcastPlayerService;

/**
 * Widget provider updates the homescreen play controls when it's added to the screen
 * The PodcastPlayService updates the widget while a podcast is playing
 * Created by evan on 2/19/15.
 */
public class WidgetProvider extends AppWidgetProvider {

    private static final String TAG = WidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called");

        // assign the pending intent to our remote views
        RemoteViews remoteViews = configureWidgetIntents(context, null);
        remoteViews.setOnClickPendingIntent(R.id.play, PendingIntentHelper.getPlayOrPauseIntent(
                context, PlaybackState.STATE_PAUSED));

        // iterate over all of the widgets on the screen
        for (int appWidgetId : appWidgetIds) {
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }

        // send intent to update widget
        context.startService(new Intent(context, PodcastPlayerService.class)
                .setAction(PodcastPlayerService.ACTION_UPDATE_WIDGET));
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDeleted called");
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled called");
        super.onEnabled(context);
    }

    public static RemoteViews configureWidgetIntents(Context context, Episode episode) {
        // assign the pending intent to our remote views
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.homescreen_widget);
        remoteViews.setOnClickPendingIntent(R.id.widget, PendingIntentHelper.getOpenNowPlayingIntent(context, episode));
        remoteViews.setOnClickPendingIntent(R.id.seek_backward,
                PendingIntentHelper.getSeekBackwardIntent(context));
        remoteViews.setOnClickPendingIntent(R.id.seek_forward,
                PendingIntentHelper.getSeekForwardIntent(context));
        remoteViews.setOnClickPendingIntent(R.id.channel_art,
                PendingIntentHelper.getOpenNowPlayingIntent(context, episode));
        return remoteViews;
    }
}
