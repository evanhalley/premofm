/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */
package com.mainmethod.premofm.ui.activity;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.IntentHelper;
import com.mainmethod.premofm.helper.NotificationHelper;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.service.SyncFeedService;
import com.mainmethod.premofm.service.job.DownloadJobService;
import com.mainmethod.premofm.ui.fragment.BaseFragment;
import com.mainmethod.premofm.ui.fragment.CollectionsFragment;
import com.mainmethod.premofm.ui.fragment.EpisodesFragment;
import com.mainmethod.premofm.ui.fragment.PodcastsFragment;

/**
 * Frame of the basic application
 * @author evan
 */
public class PremoActivity
        extends MiniPlayerActivity
        implements DrawerLayout.DrawerListener,
        View.OnClickListener,
        DialogInterface.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener,
        TabLayout.OnTabSelectedListener {

    private static final String TAG = PremoActivity.class.getSimpleName();

    private static final int FEEDBACK_OPTION_PROBLEM    = 0;
    private static final int FEEDBACK_OPTION_IDEA       = 1;
    private static final int FEEDBACK_RATE_APP          = 2;

    private static final String PARAM_CURRENT_FRAGMENT = "currentFragment";
    public static final String PARAM_EPISODE_ID = "episodeId";

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BaseFragment mActiveFragment;
    private int selectedDrawerItem;
    private TabLayout tabLayout;
    private AppBarLayout appBarLayout;
    private CoordinatorLayout coordinatorLayout;
    private View navHeader;

    private BroadcastReceiver rssRefreshFinished = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSyncTime();
        }
    };

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        super.onCreateBase(savedInstanceState);
        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        setupNavDrawer();
        loadUI(savedInstanceState);
        DownloadJobService.scheduleEpisodeDownload(this);

        // dismiss any new episode or new download notifications
        NotificationHelper.dismissNotification(this, NotificationHelper.NOTIFICATION_ID_NEW_EPISODES);
        NotificationHelper.dismissNotification(this, NotificationHelper.NOTIFICATION_ID_DOWNLOADED);
        sendBroadcast(IntentHelper.getClearEpisodeNotificationsIntent(this));
        sendBroadcast(IntentHelper.getClearDownloadNotificationsIntent(this));
        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey(PARAM_EPISODE_ID)) {
            Episode episode = EpisodeModel.getEpisodeById(this, extras.getInt(PARAM_EPISODE_ID));
            showEpisodeInformation(this, episode);
            getIntent().removeExtra(PARAM_EPISODE_ID);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(rssRefreshFinished,
                new IntentFilter(BroadcastHelper.INTENT_RSS_REFRESH_FINISH));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(rssRefreshFinished);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tabLayout.removeOnTabSelectedListener(this);
        drawerLayout.removeDrawerListener(this);
        tabLayout = null;
    }

    @Override
    public void onBackPressed() {

        if (!mActiveFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPodcastPlayerServiceBound() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mActiveFragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.navigation_header:
                drawerLayout.closeDrawers();
                break;
            case R.id.last_sync_time:
                SyncFeedService.syncAllFeeds(this, true);
                break;
        }
    }

    public void setViewPager(ViewPager viewPager) {
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(new TabViewPagerOnTabSelectedListener(viewPager));
    }


    private void loadUI(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            onDrawerItemSelected(R.id.action_home);
        } else {
            int itemId = savedInstanceState.getInt(PARAM_CURRENT_FRAGMENT, R.id.action_home);
            onDrawerItemSelected(itemId);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(PARAM_CURRENT_FRAGMENT, selectedDrawerItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        mActiveFragment.onTabSelected(tab);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        mActiveFragment.onTabReselected(tab);
    }

    private void setupNavDrawer() {
        // configure the action bar and drawer
        toolbar.setNavigationIcon(R.drawable.ic_action_drawer);
        drawerLayout.addDrawerListener(this);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navHeader = navigationView.inflateHeaderView(R.layout.nav_header);
        navHeader.findViewById(R.id.navigation_header).setOnClickListener(this);
        navHeader.findViewById(R.id.last_sync_time).setOnClickListener(this);
        updateSyncTime();

        if (BuildConfig.DEBUG) {
            navigationView.getMenu().findItem(R.id.action_debug).setVisible(true);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        expandToolbar();
        drawerLayout.closeDrawers();
        onDrawerItemSelected(menuItem);
        return true;
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(View drawerView) {
        updateSyncTime();
    }

    @Override
    public void onDrawerClosed(View drawerView) {

    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_premo;
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.premo_activity;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void expandToolbar(){
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();

        if (behavior != null) {
            behavior.setTopAndBottomOffset(0);
            behavior.onNestedPreScroll(coordinatorLayout, appBarLayout, null, 0, 1, new int[2]);
        }
    }

    private void onDrawerItemSelected(int itemId) {
        onDrawerItemSelected(navigationView.getMenu().findItem(itemId));
    }

    private void onDrawerItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        switch (itemId) {
            case R.id.action_home:
            case R.id.action_channels:
            case R.id.action_collections:
                startFragment(itemId);
                menuItem.setChecked(true);
                break;
            case R.id.action_settings:
                startPremoActivity(SettingsActivity.class, null, "", -1, null);
                menuItem.setChecked(false);
                break;
            case R.id.action_feedback:
                showFeedbackDialog();
                menuItem.setChecked(false);
                break;
            case R.id.action_about:
                SettingsActivity.start(this, SettingsActivity.SHOW_ABOUT);
                menuItem.setChecked(false);
                break;
            case R.id.action_debug:

                if (BuildConfig.DEBUG) {
                    startPremoActivity(DebugActivity.class);
                }
                menuItem.setChecked(false);
                break;
            default:
                break;
        }
    }

    private void startFragment(int itemId) {
        FragmentManager fragmentManager = getFragmentManager();
        // let's look for the fragment first
        BaseFragment fragment = (BaseFragment) fragmentManager
                .findFragmentByTag(String.valueOf(itemId));

        if (fragment == null) {

            switch (itemId) {
                case R.id.action_home:
                    fragment = EpisodesFragment.newInstance(null);
                    break;
                case R.id.action_channels:
                    fragment =  PodcastsFragment.newInstance(null);
                    break;
                case R.id.action_collections:
                    fragment = CollectionsFragment.newInstance();
                    break;
                default:
                    throw new IllegalArgumentException(
                            String.format("Invalid item ID encountered %d", itemId));
            }
        } else if (fragment == mActiveFragment) {
            // we are already at the active fragment, do nothing
            return;
        }
        Log.d(TAG, "Switching to Fragment: " + fragment.getClass().getSimpleName());

        selectedDrawerItem = itemId;
        mActiveFragment = fragment;
        fragmentManager.beginTransaction().replace(R.id.fragment_container,
                mActiveFragment, String.valueOf(itemId)).commit();

        // configure the tabs
        tabLayout.removeAllTabs();
        AppBarLayout.LayoutParams params =
                (AppBarLayout.LayoutParams) getToolbar().getLayoutParams();

        if (mActiveFragment.hasTabs()) {
            // only scroll the toolbar if the fragment uses the tab layout
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
            tabLayout.setVisibility(View.VISIBLE);
        } else {
            params.setScrollFlags(-1);
            tabLayout.setVisibility(View.GONE);
        }
    }

    private void showFeedbackDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_feedback_title)
                .setItems(R.array.dialog_feedback_options, this)
                .setNeutralButton(R.string.dialog_close, null)
                .create().show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.d(TAG, which + "");

        switch (which) {
            case FEEDBACK_OPTION_PROBLEM:
                // TODO write debug file for support email
                IntentHelper.sendSupportEmail(this, null);
                break;
            case FEEDBACK_OPTION_IDEA:
                IntentHelper.sendIdeaEmail(this);
                break;
            case FEEDBACK_RATE_APP:
                IntentHelper.openAppListing(this);
                break;
        }
    }

    private void updateSyncTime() {
        long lastSyncTime = AppPrefHelper.getInstance(this).getLastEpisodeSync();

        if (lastSyncTime > -1) {
            ((TextView) navHeader.findViewById(R.id.last_sync_time)).setText(getString(R.string.last_sync_label,
                    DateUtils.getRelativeTimeSpanString(lastSyncTime, DatetimeHelper.getTimestamp(),
                            DateUtils.MINUTE_IN_MILLIS)));
        }
    }

    private void showSnackbarMessage(int messageResId, int actionTextResId, View.OnClickListener clickListener) {
        Snackbar.make(findViewById(R.id.assistant_message), messageResId, Snackbar.LENGTH_INDEFINITE)
                .setAction(actionTextResId, clickListener)
                .setActionTextColor(getResources().getColor(R.color.primary))
                .show();
    }

    private class TabViewPagerOnTabSelectedListener extends TabLayout.ViewPagerOnTabSelectedListener{

        public TabViewPagerOnTabSelectedListener(ViewPager viewPager) {
            super(viewPager);
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            super.onTabReselected(tab);
            PremoActivity.this.onTabReselected(tab);
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            super.onTabSelected(tab);
            PremoActivity.this.onTabSelected(tab);
        }
    }
}