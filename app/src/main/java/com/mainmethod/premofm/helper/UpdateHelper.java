/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.DatabaseOpenHelper;
import com.mainmethod.premofm.data.model.FilterModel;
import com.mainmethod.premofm.service.job.DownloadJobService;
import com.mainmethod.premofm.service.job.PodcastSyncJobService;

import timber.log.Timber;

/**
 * Manages the upgrade process as the device is updated from the Play Store
 *   -database and preference migrations, etc
 * Created by evan on 4/21/15.
 */
public class UpdateHelper {

    /**
     * Returns true if the app was updated
     * @param context
     * @return
     */
    public static boolean wasUpdated(Context context) {
        int oldVersionCode = AppPrefHelper.getInstance(context).getAppVersion();
        int newVersionCode = PackageHelper.getVersionCode(context);
        return newVersionCode > oldVersionCode;
    }

    public static void executeUpdateAsync(final Context context,
                                          final OnUpdateCompleteListener listener) {

        new AsyncTask<Void, Void, Void>() {
            Dialog dialog;

            @Override
            protected void onPreExecute() {
                dialog = ProgressDialog.show(context,
                        context.getString(R.string.dialog_updating_title),
                        context.getString(R.string.dialog_updating_message),
                        true,
                        false);
            }

            @Override
            protected Void doInBackground(Void... params) {
                executeUpdate(context);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                dialog.dismiss();
                listener.onUpdateComplete();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void executeUpdate(Context context) {
        int oldVersionCode = AppPrefHelper.getInstance(context).getAppVersion();
        int newVersionCode = PackageHelper.getVersionCode(context);

        Timber.d("Old version code: %d, New version code: %d", oldVersionCode, newVersionCode);

        // force the onUpgrade function to run and return a writable database
        new DatabaseOpenHelper(context).getWritableDatabase();

        // probably the first time we've started the app
        if (oldVersionCode == -1) {
            // set the default preferences
            // because -> https://code.google.com/p/android/issues/detail?id=6641
            PreferenceManager.setDefaultValues(context, R.xml.settings, false);

            // create a couple filters
            FilterModel.createSampleFilters(context);
        }

        if (oldVersionCode != -1) {
            // anytime we do any upgrade, remove cached episode notifications
            AppPrefHelper.getInstance(context).remove(AppPrefHelper.PROPERTY_EPISODE_NOTIFICATIONS);
            AppPrefHelper.getInstance(context).remove(AppPrefHelper.PROPERTY_DOWNLOAD_NOTIFICATIONS);
        }

        // save the new version code
        AppPrefHelper.getInstance(context).setAppVersion(newVersionCode);
        PodcastSyncJobService.schedule(context);
        DownloadJobService.scheduleEpisodeDownload(context);
    }

    public interface OnUpdateCompleteListener {
        void onUpdateComplete();
    }
}