/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Toast;

import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.NotificationHelper;
import com.mainmethod.premofm.helper.XORHelper;
import com.mainmethod.premofm.helper.billing.IabHelper;
import com.mainmethod.premofm.helper.billing.IabResult;
import com.mainmethod.premofm.helper.billing.Purchase;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.User;
import com.mainmethod.premofm.service.ApiService;
import com.mainmethod.premofm.service.DownloadService;
import com.mainmethod.premofm.service.PodcastPlayerService;
import com.mainmethod.premofm.ui.dialog.ReauthUserDialogFragment;

import org.parceler.Parcels;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created by evan on 10/14/14.
 */
public class DebugActivity extends BaseActivity implements View.OnClickListener,
        IabHelper.OnIabSetupFinishedListener, IabHelper.OnIabPurchaseFinishedListener {
    private IabHelper mIabHelper;

    private final BroadcastReceiver mReauthReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // show toast
            boolean isReauthenticated = intent.getBooleanExtra(
                    BroadcastHelper.EXTRA_IS_REAUTHENTICATED, false);
            int toastResId = isReauthenticated ? R.string.toast_login_successful :
                    R.string.toast_login_not_successful;
            Toast.makeText(DebugActivity.this, toastResId, Toast.LENGTH_SHORT).show();

            if (!isReauthenticated) {
                ReauthUserDialogFragment.showReauthNotification(context);
            }
        }
    };

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        setHomeAsUpEnabled(true);

        if (!BuildConfig.DEBUG) {
            finish();
            return;
        }
        setupIabHelper();
        findViewById(R.id.trigger_download_service).setOnClickListener(this);
        findViewById(R.id.simulate_sleep_timer_expiration).setOnClickListener(this);
        findViewById(R.id.test_iab_purchase).setOnClickListener(this);
        findViewById(R.id.reauthenticate).setOnClickListener(this);
        findViewById(R.id.notify_to_reauth).setOnClickListener(this);
        findViewById(R.id.single_episode_notification).setOnClickListener(this);
        findViewById(R.id.push_collection_changes).setOnClickListener(this);
        findViewById(R.id.insufficient_space_notification).setOnClickListener(this);
        findViewById(R.id.push_episode_changes).setOnClickListener(this);
        findViewById(R.id.multiple_episode_notification).setOnClickListener(this);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_debug;
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.menu_settings;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.trigger_download_service:
                Intent downloadIntent  = new Intent(this, DownloadService.class);
                downloadIntent.setAction(DownloadService.ACTION_AUTO_DOWNLOAD);
                startService(downloadIntent);
                break;
            case R.id.simulate_sleep_timer_expiration:
                PodcastPlayerService.sendIntent(this, PodcastPlayerService.ACTION_SLEEP_TIMER, -1);
                break;
            case R.id.test_iab_purchase:
                User user = User.load(this);
                mIabHelper.launchPurchaseFlow(this, "premofm_listener.test", 123, this, user.getId());
                break;
            case R.id.reauthenticate:
                ReauthUserDialogFragment.showDialog(this);
                break;
            case R.id.notify_to_reauth:
                ReauthUserDialogFragment.showReauthNotification(this);
                break;
            case R.id.single_episode_notification:
                Episode episode = EpisodeModel.getEpisodeById(this, 1);
                Set<String> episodeIds = new TreeSet<>();
                episodeIds.add(episode.getServerId());
                AppPrefHelper.getInstance(this).addToStringSet(
                        AppPrefHelper.PROPERTY_EPISODE_NOTIFICATIONS, episodeIds);
                NotificationHelper.showNewEpisodeNotification(this);
                break;
            case R.id.multiple_episode_notification:
                Set<String> episodeIds1 = new TreeSet<>();
                episodeIds1.add(EpisodeModel.getEpisodeById(this, 1).getServerId());
                episodeIds1.add(EpisodeModel.getEpisodeById(this, 2).getServerId());
                episodeIds1.add(EpisodeModel.getEpisodeById(this, 3).getServerId());
                episodeIds1.add(EpisodeModel.getEpisodeById(this, 4).getServerId());
                episodeIds1.add(EpisodeModel.getEpisodeById(this, 5).getServerId());
                AppPrefHelper.getInstance(this).addToStringSet(
                        AppPrefHelper.PROPERTY_EPISODE_NOTIFICATIONS, episodeIds1);
                NotificationHelper.showNewEpisodeNotification(this);
                break;
            case R.id.push_collection_changes:
                ApiService.start(this, ApiService.ACTION_PUSH_COLLECTION_CHANGES);
                break;
            case R.id.insufficient_space_notification:
                NotificationHelper.showErrorNotification(this,
                        R.string.notification_title_insufficient_space,
                        R.string.notification_content_insufficient_space);
                break;
            case R.id.push_episode_changes:
                ApiService.start(this, ApiService.ACTION_PUSH_LOCAL_CHANGES);
                break;
        }
    }

    @Override
    public void onIabSetupFinished(IabResult result) {

    }

    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase info) {

        if (info != null && result.getResponse() == IabHelper.BILLING_RESPONSE_RESULT_OK) {
            Bundle args = new Bundle();
            args.putParcelable(ApiService.PARAM_PURCHASE, Parcels.wrap(info));
            ApiService.start(this, ApiService.ACTION_ADD_PURCHASE, args);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mIabHelper != null) {
            mIabHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    private void setupIabHelper() {
        String key = XORHelper.decode(getString(R.string.google_play_license), 27);
        mIabHelper = new IabHelper(this, key);
        mIabHelper.enableDebugLogging(BuildConfig.DEBUG);
        mIabHelper.startSetup(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mReauthReceiver, new IntentFilter(BroadcastHelper.INTENT_REAUTHENTICATION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReauthReceiver);
    }
}