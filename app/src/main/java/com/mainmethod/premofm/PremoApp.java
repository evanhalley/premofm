/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */
package com.mainmethod.premofm;

import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;

import com.mainmethod.premofm.config.ConfigurationManager;

import timber.log.Timber;

/**
 * Created by evan on 12/1/14.
 */
public class PremoApp extends Application {

    private static final String TAG = PremoApp.class.getSimpleName();
    public static final String FLAG_IS_FIRST_SIGN_IN = "isFirstSignIn";

    public static String mVersionName;

    public static String getVersionName() {
        return mVersionName;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // configuration manager
        ConfigurationManager.getInstance(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        try {
            mVersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error retrieving app version name: ", e);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
