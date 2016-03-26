/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;
import com.mainmethod.premofm.PremoApp;

/**
 * Created by evan on 7/8/15.
 */
public class AnalyticsHelper {

    public static final String CATEGORY_DRAWER = "Drawer";
    public static final String CATEGORY_FILTER = "Filter";
    public static final String CATEGORY_CREATE_FILTER = "CreateFilter";
    public static final String CATEGORY_EPISODE_ACTION = "EpisodeShare";
    public static final String CATEGORY_CHANNEL_ACTION = "ChannelShare";
    public static final String CATEGORY_EXPLORE_SEARCH = "ExploreSearch";
    public static final String CATEGORY_SIGN_IN = "SignIn";
    public static final String CATEGORY_SIGN_UP = "SignUp";
    public static final String CATEGORY_SETUP_ACCOUNT = "SetupAccount";
    public static final String CATEGORY_TRY_IT = "TryIt";
    public static final String CATEGORY_ONBOARDING = "Onboarding";
    public static final String CATEGORY_PURCHASE = "Purchase";
    public static final String CATEGORY_EPISODE_DOWNLOAD = "EpisodeDownload";
    public static final String CATEGORY_RATE_PREMOFM = "RateApp";
    public static final String CATEGORY_SUPPORT_PREMOFM = "SupportApp";
    public static final String CATEGORY_TWITTER = "Twitter";
    public static final String CATEGORY_VIEW_FAQ = "ViewFaq";
    public static final String CATEGORY_GO_TO_RALEIGH = "GoToRaleigh";
    public static final String CATEGORY_VIEW_TOS = "ViewTos";
    public static final String CATEGORY_VIEW_PRIVACY = "ViewPrivacy";
    public static final String CATEGORY_VIEW_LICENSES = "ViewLicenses";
    public static final String CATEGORY_VIEW_PLAYLIST = "ViewPlaylist";
    public static final String CATEGORY_VIEW_TRANSLATIONS = "Translations";
    public static final String CATEGORY_SLEEP_TIMER = "SleepTimer";
    public static final String CATEGORY_CAST = "Cast";
    public static final String CATEGORY_ITUNES_LINK = "iTunesLink";
    public static final String CATEGORY_PIN = "Pin";
    public static final String CATEGORY_OPML_IMPORT = "OpmlImport";
    public static final String CATEGORY_OPML_EXPORT = "OpmlExport";
    public static final String CATEGORY_PLAYBACK_SPEED = "PlaybackSpeed";

    public static final String ACTION_CLICK = "click";
    public static final String ACTION_INPUT = "input";
    public static final String ACTION_VIEW  = "view";

    public static void sendEvent(Context context, String category, String action, String label) {

        if (context == null) {
            return;
        }

        ((PremoApp) context.getApplicationContext()).getTracker().send(
                new HitBuilders.EventBuilder()
                        .setCategory(category)
                        .setAction(action)
                        .setLabel(label)
                        .build());
    }
}