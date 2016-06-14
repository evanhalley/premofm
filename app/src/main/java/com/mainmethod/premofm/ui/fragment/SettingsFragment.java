/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.IntentHelper;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.service.AsyncTaskService;
import com.mainmethod.premofm.service.DeleteEpisodeService;
import com.mainmethod.premofm.service.job.DownloadJobService;
import com.mainmethod.premofm.service.job.SyncFeedJobService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * UI for changing user preferences
 * Created by evan on 10/14/14.
 */
public class SettingsFragment extends PreferenceFragment implements
        DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnClickListener,
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private UserPrefHelper mUserPrefHelper;
    private List<Channel> mChannels;
    private boolean[] mSelectedItems;

    private Dialog mDeleteConfirmation;
    private Dialog mDownloadChannelsChooser;
    private Dialog mNotificationChannelsChooser;

    private Preference mNotificationChannels;
    private Preference mAutoDownloadChannels;

    private ProgressDialog dialog;

    private BroadcastReceiver opmlProcessFinished = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        }
    };

    public SettingsFragment() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // get channels from the cache
        mChannels = ChannelModel.getChannels(getActivity());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        mUserPrefHelper = UserPrefHelper.get(getActivity());

        Preference skipForward = findPreference(getString(R.string.pref_key_skip_forward));
        Preference skipBackward = findPreference(getString(R.string.pref_key_skip_backward));
        mNotificationChannels = findPreference(getString(R.string.pref_key_notification_channels));
        mAutoDownloadChannels = findPreference(getString(R.string.pref_key_auto_download_channels));
        Preference episodeCacheLimit = findPreference(getString(R.string.pref_key_episode_cache_limit));
        Preference syncPeriod = findPreference(getString(R.string.pref_key_syncing_period));

        refreshPreferenceSummary(skipBackward);
        refreshPreferenceSummary(skipForward);
        refreshPreferenceSummary(mAutoDownloadChannels);
        refreshPreferenceSummary(episodeCacheLimit);
        refreshPreferenceSummary(mNotificationChannels);
        refreshPreferenceSummary(syncPeriod);

        syncPeriod.setOnPreferenceChangeListener(this);
        skipForward.setOnPreferenceChangeListener(this);
        skipBackward.setOnPreferenceChangeListener(this);
        mNotificationChannels.setOnPreferenceClickListener(this);
        episodeCacheLimit.setOnPreferenceChangeListener(this);
        mAutoDownloadChannels.setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_delete_all_episodes)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_import_opml)).setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_export_opml)).setOnPreferenceClickListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        // if any job is schedule cancel it and re-schedule, because the user may have disabled
        //   auto download or change the parameters in which we can download
        DownloadJobService.cancelScheduledEpisodeDownload(getActivity());
        DownloadJobService.scheduleEpisodeDownload(getActivity());
        SyncFeedJobService.schedule(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(opmlProcessFinished,
                new IntentFilter(BroadcastHelper.INTENT_OPML_PROCESS_FINISH));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(opmlProcessFinished);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (key.contentEquals(getString(R.string.pref_key_skip_backward))) {
            preference.setSummary(String.format(getString(R.string.pref_summary_skip_backward),
                    Integer.parseInt((String) newValue)));
        } else if (key.contentEquals(getString(R.string.pref_key_skip_forward))) {
            preference.setSummary(String.format(getString(R.string.pref_summary_skip_forward),
                    Integer.parseInt((String) newValue)));
        }

        else if (key.contentEquals(getString(R.string.pref_key_notification_channels))) {
            int numberOfChannels = (int) newValue;
            int stringResId;

            if (numberOfChannels == 1) {
                stringResId = R.string.pref_summary_notification_channel;
            } else {
                stringResId = R.string.pref_summary_notification_channels;
            }
            preference.setSummary(String.format(getString(stringResId), numberOfChannels));
        }

        else if (key.contentEquals(getString(R.string.pref_key_auto_download_channels))) {
            int numberOfChannels = (int) newValue;
            int stringResId;

            if (numberOfChannels == 1) {
                stringResId = R.string.pref_summary_auto_download_channel;
            } else {
                stringResId = R.string.pref_summary_auto_download_channels;
            }
            preference.setSummary(String.format(getString(stringResId), numberOfChannels));
        }

        else if (key.contentEquals(getString(R.string.pref_key_episode_cache_limit))) {
            int numberOfEpisodes = Integer.parseInt((String) newValue);
            int stringResId;

            if (numberOfEpisodes > 1) {
                stringResId = R.string.pref_summary_episodes_cache_limit;
            } else {
                stringResId = R.string.pref_summary_episode_cache_limit;
            }
            preference.setSummary(String.format(getString(stringResId), numberOfEpisodes));
        }

        else if (key.contentEquals(getString(R.string.pref_key_syncing_period))) {
            int minutes = Integer.parseInt((String) newValue);

            if (minutes < 60) {
                preference.setSummary(getString(R.string.pref_minutes, minutes));
            } else if (minutes == 60) {
                preference.setSummary(getString(R.string.pref_hour));
            } else if (minutes > 60 && minutes < 1440) {
                preference.setSummary(getString(R.string.pref_hours, minutes / 60));
            } else {
                preference.setSummary(getString(R.string.pref_day));
            }
        }

        return true;
    }

    private void refreshPreferenceSummary(Preference preference) {
        String key = preference.getKey();

        if (key.contentEquals(getString(R.string.pref_key_skip_backward))) {
            preference.setSummary(String.format(getString(R.string.pref_summary_skip_backward),
                    mUserPrefHelper.getStringAsInt(key)));
        } else if (key.contentEquals(getString(R.string.pref_key_skip_forward))) {
            preference.setSummary(String.format(getString(R.string.pref_summary_skip_forward),
                    mUserPrefHelper.getStringAsInt(key)));
        } else if (key.contentEquals(getString(R.string.pref_key_notification_channels))) {
            String value =  mUserPrefHelper.getString(R.string.pref_key_notification_channels);
            int numberOfChannels;

            if (TextUtils.isEmpty(value)) {
                numberOfChannels = 0;
            } else {
                numberOfChannels = value.split(",").length;
            }
            int stringResId;

            if (numberOfChannels == 1) {
                stringResId = R.string.pref_summary_notification_channel;
            } else {
                stringResId = R.string.pref_summary_notification_channels;
            }
            preference.setSummary(String.format(getString(stringResId), numberOfChannels));
        }

        else if (key.contentEquals(getString(R.string.pref_key_auto_download_channels))) {
            String value =  mUserPrefHelper.getString(R.string.pref_key_auto_download_channels);
            int numberOfChannels;

            if (TextUtils.isEmpty(value)) {
                numberOfChannels = 0;
            } else {
                numberOfChannels = value.split(",").length;
            }
            int stringResId;

            if (numberOfChannels == 1) {
                stringResId = R.string.pref_summary_auto_download_channel;
            } else {
                stringResId = R.string.pref_summary_auto_download_channels;
            }
            preference.setSummary(String.format(getString(stringResId), numberOfChannels));
        }

        else if (key.contentEquals(getString(R.string.pref_key_episode_cache_limit))) {
            int numberOfEpisodes = mUserPrefHelper.getStringAsInt(R.string.pref_key_episode_cache_limit);
            int stringResId;

            if (numberOfEpisodes > 1) {
                stringResId = R.string.pref_summary_episodes_cache_limit;
            } else {
                stringResId = R.string.pref_summary_episode_cache_limit;
            }
            preference.setSummary(String.format(getString(stringResId), numberOfEpisodes));
        }

        else if (key.contentEquals(getString(R.string.pref_key_syncing_period))) {
            int minutes = mUserPrefHelper.getStringAsInt(getString(R.string.pref_key_syncing_period));

            if (minutes < 60) {
                preference.setSummary(getString(R.string.pref_minutes, minutes));
            } else if (minutes == 60) {
                preference.setSummary(getString(R.string.pref_hour));
            } else if (minutes > 60 && minutes < 1440) {
                preference.setSummary(getString(R.string.pref_hours, minutes / 60));
            } else {
                preference.setSummary(getString(R.string.pref_day));
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.contentEquals(getString(R.string.pref_key_auto_download_channels))) {
            showDownloadChannelSelectionDialog();
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_delete_all_episodes))) {
            showDeleteAllEpisodesDialog();
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_notification_channels))) {
            showNotificationChannelSelectionDialog();
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_import_opml))) {
            IntentHelper.openOpmlFileChooser(this);
            return true;
        }

        else if (key.contentEquals(getString(R.string.pref_key_export_opml))) {
            IntentHelper.openOpmlFileExporter(this);
            return true;
        }

        return false;
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

        if (dialog.equals(mDownloadChannelsChooser)) {
            mSelectedItems[which] = isChecked;
        } else if (dialog.equals(mNotificationChannelsChooser)) {
            mSelectedItems[which] = isChecked;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        if (dialog.equals(mDownloadChannelsChooser)) {
            saveChannelDownloadPreference(which);
        } else if (dialog.equals(mDeleteConfirmation)) {
            deleteEpisodes(which);
        } else if (dialog.equals(mNotificationChannelsChooser)) {
            saveChannelNotificationPreference(which);
        }
    }

    private void deleteEpisodes(int which) {

        if (which == Dialog.BUTTON_POSITIVE) {
            DeleteEpisodeService.deleteAllEpisodes(getActivity());
            Toast.makeText(getActivity(), R.string.toast_deleting_all_episodes,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void saveChannelDownloadPreference(int which) {
        List<String> serverIdList = new ArrayList<>();

        if (which == Dialog.BUTTON_POSITIVE) {
            // save the preferences

            for (int i = 0; i < mSelectedItems.length; i++) {

                if (mSelectedItems[i]) {
                    serverIdList.add(mChannels.get(i).getGeneratedId());
                }
            }
        }

        else if (which == Dialog.BUTTON_NEUTRAL) {

            for (int i = 0; i < mChannels.size(); i++) {
                serverIdList.add(mChannels.get(i).getGeneratedId());
            }
        }
        String serverIdStr = TextUtils.join(",", serverIdList);
        getPreferenceManager().getSharedPreferences().edit()
                .putString(getString(R.string.pref_key_auto_download_channels), serverIdStr).apply();
        onPreferenceChange(mAutoDownloadChannels, serverIdList.size());
    }

    private void saveChannelNotificationPreference(int which) {
        List<String> serverIdList = new ArrayList<>();

        if (which == Dialog.BUTTON_POSITIVE) {
            // save the preferences

            for (int i = 0; i < mSelectedItems.length; i++) {

                if (mSelectedItems[i]) {
                    serverIdList.add(mChannels.get(i).getGeneratedId());
                }
            }
        }

        else if (which == Dialog.BUTTON_NEUTRAL) {

            for (int i = 0; i < mChannels.size(); i++) {
                serverIdList.add(mChannels.get(i).getGeneratedId());
            }
        }
        String serverIdStr = TextUtils.join(",", serverIdList);
        getPreferenceManager().getSharedPreferences().edit()
                .putString(getString(R.string.pref_key_notification_channels), serverIdStr).apply();
        onPreferenceChange(mNotificationChannels, serverIdList.size());
    }

    private void showDownloadChannelSelectionDialog() {

        // load previous selections from preferences
        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        String serverIdStr = preferences.getString(getString(
                R.string.pref_key_auto_download_channels), "");

        // load each channel title into the channel titles array
        CharSequence[] channelTitles = new CharSequence[mChannels.size()];
        mSelectedItems = new boolean[mChannels.size()];
        List<String> serverIds = Arrays.asList(TextUtils.split(serverIdStr, ","));

        for (int i = 0; i < channelTitles.length; i++) {
            channelTitles[i] = mChannels.get(i).getTitle();
            mSelectedItems[i] = serverIds.contains(mChannels.get(i).getGeneratedId());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setNeutralButton(R.string.dialog_select_all, this)
                .setPositiveButton(R.string.dialog_save, this)
                .setNegativeButton(R.string.dialog_clear, this)
                .setMultiChoiceItems(channelTitles, mSelectedItems, this);
        mDownloadChannelsChooser = builder.create();
        mDownloadChannelsChooser.show();
    }

    private void showNotificationChannelSelectionDialog() {

        // load previous selections from preferences
        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        String serverIdStr = preferences.getString(getString(
                R.string.pref_key_notification_channels), "");

        // load each channel title into the channel titles array
        CharSequence[] channelTitles = new CharSequence[mChannels.size()];
        mSelectedItems = new boolean[mChannels.size()];
        List<String> serverIds = Arrays.asList(TextUtils.split(serverIdStr, ","));

        for (int i = 0; i < channelTitles.length; i++) {
            channelTitles[i] = mChannels.get(i).getTitle();
            mSelectedItems[i] = serverIds.contains(mChannels.get(i).getGeneratedId());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setNeutralButton(R.string.dialog_select_all, this)
                .setPositiveButton(R.string.dialog_save, this)
                .setNegativeButton(R.string.dialog_clear, this)
                .setMultiChoiceItems(channelTitles, mSelectedItems, this);
        mNotificationChannelsChooser = builder.create();
        mNotificationChannelsChooser.show();
    }

    private void showDeleteAllEpisodesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setMessage(R.string.dialog_delete_downloaded_episodes_confirmation)
                .setPositiveButton(R.string.dialog_delete, this)
                .setNegativeButton(R.string.dialog_cancel, this);
        mDeleteConfirmation = builder.create();
        mDeleteConfirmation.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (resultCode != Activity.RESULT_OK || intent == null || intent.getData() == null) {
            return;
        }

        if (requestCode == IntentHelper.REQUEST_CODE_OPEN_OPML_FILE) {
            executeOpmlImport(intent.getData());
        } else if (requestCode == IntentHelper.REQUEST_CODE_SAVE_OPML_FILE) {
            executeOpmlExport(intent.getData());
        }
    }

    private void executeOpmlImport(Uri uri) {
        dialog = ProgressDialog.show(getActivity(),
                getString(R.string.dialog_please_wait),
                getString(R.string.importing_opml),
                true);
        AsyncTaskService.opmlImport(getActivity(), uri);
    }

    private void executeOpmlExport(Uri uri) {
        dialog = ProgressDialog.show(getActivity(),
                getString(R.string.dialog_please_wait),
                getString(R.string.exporting_opml),
                true);
        AsyncTaskService.opmlExport(getActivity(), uri);
    }
}