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
import android.text.TextUtils;
import android.util.Log;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.DatabaseOpenHelper;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.CollectionModel;
import com.mainmethod.premofm.data.model.FilterModel;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.User;
import com.mainmethod.premofm.service.ApiService;
import com.mainmethod.premofm.service.job.SyncEpisodesJobService;

import java.util.List;

/**
 * Manages the upgrade process as the device is updated from the Play Store
 *   -database and preference migrations, etc
 * Created by evan on 4/21/15.
 */
public class UpdateHelper {

    private static final String TAG = UpdateHelper.class.getSimpleName();

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

        Log.d(TAG, String.format("Old version code: %d, New version code: %d", oldVersionCode,
                newVersionCode));

        // force the onUpgrade function to run and return a writable database
        new DatabaseOpenHelper(context).getWritableDatabase();

        // probably the first time we've started the app
        if (oldVersionCode == -1) {

            // set the default preferences
            // because -> https://code.google.com/p/android/issues/detail?id=6641
            PreferenceManager.setDefaultValues(context, R.xml.settings, false);

            String registrationId = AppPrefHelper.getInstance(context).getRegistrationId();

            if (registrationId == null) {
                Log.d(TAG, "Registration ID is null, retrieving a new one");

                // start the process to retrieve a GCM registration ID
                ApiService.start(context, ApiService.ACTION_REGISTER_GCM);
            }

            // create a couple filters
            FilterModel.createSampleFilters(context);
        }

        if (oldVersionCode != -1) {

            // update registration ID
            ApiService.start(context, ApiService.ACTION_REGISTER_GCM);

            // anytime we do any upgrade, resync the user profile?
            ApiService.start(context, ApiService.ACTION_SYNC_USER_PROFILE);

            // anytime we do any upgrade, remove cached episode notifications
            AppPrefHelper.getInstance(context).remove(AppPrefHelper.PROPERTY_EPISODE_NOTIFICATIONS);
            AppPrefHelper.getInstance(context).remove(AppPrefHelper.PROPERTY_DOWNLOAD_NOTIFICATIONS);

            // schedule an episode update
            SyncEpisodesJobService.schedule(context);

            // more updates here
            if (oldVersionCode <= 100001 && newVersionCode >= 100002) {
                // upgraded playlists, remove current playlist data
                AppPrefHelper.getInstance(context).removePlaylist();
                AppPrefHelper.getInstance(context).removeLastPlayedEpisodeId();
            }

            // more updates here
            if (oldVersionCode <= 100003 && newVersionCode >= 100003) {
                // opt all channels into notifications
                List<Channel> channels = ChannelModel.getChannels(context);
                List<String> serverIds = CollectionModel.getCollectableServerIds(channels);
                String serverIdStr = TextUtils.join(",", serverIds);
                UserPrefHelper.get(context).putString(
                        context.getString(R.string.pref_key_notification_channels), serverIdStr);
            }

            if (oldVersionCode <= 100020 && newVersionCode >= 100021) {
                User user = User.load(context);
                user.setListeningTime(0);
                User.save(context, user);
            }
        }

        // save the new version code
        AppPrefHelper.getInstance(context).setAppVersion(newVersionCode);
    }

    public interface OnUpdateCompleteListener {
        void onUpdateComplete();
    }
}