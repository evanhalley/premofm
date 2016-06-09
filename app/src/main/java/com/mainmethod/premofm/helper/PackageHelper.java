/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import com.mainmethod.premofm.BuildConfig;

/**
 * Simplifies retrieving metadata values from the AndroidManifest file
 * Created by evan on 3/29/15.
 */
public class PackageHelper {

    private static final String TAG = PackageHelper.class.getSimpleName();

    public static int getVersionCode(Context context) {
        int versionCode = -1;
        try {
            versionCode = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Error getting version code", e);
        }
        return versionCode;
    }

    /**
     * Returns a hashed version of the Android ID
     * @param context
     * @return
     * @throws Exception
     */
    public static String getDeviceId(Context context) throws Exception {
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return HashHelper.generateMd5Hash(androidId);
    }
}
