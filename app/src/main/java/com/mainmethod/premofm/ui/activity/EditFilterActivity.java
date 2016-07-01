/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.CollectionModel;
import com.mainmethod.premofm.data.model.FilterModel;
import com.mainmethod.premofm.object.Collection;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.object.Filter;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows user to create, edit, and delete filters
 * Created by evan on 10/19/15.
 */
public class EditFilterActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener,
        FilterModel.FilterChangedListener,
        FilterModel.FilterDeletedListener,
        FilterModel.FilterCreatedListener {

    public static final String PARAM_FILTER     = "filter";
    public static final String PARAM_SORT_ORDER = "sortOrder";
    public static final int REQUEST_CODE        = 7921;

    private Filter mFilter;
    private List<Collection> mCollectionList;
    private View mLimitSubscriptionsPanel;
    private View mLimitByPublishedDatePanel;
    private View mLimitByCollectionsPanel;
    private Switch mLimitSubscriptionsEnabled;
    private Switch mLimitByPublishedDateEnabled;
    private Switch mLimitByCollectionEnabled;
    private TextView mFilterName;
    private boolean mNewFilter;
    private Dialog mDialog;

    public static void createNewFilter(Activity activity, int sortOrder) {
        Intent intent = new Intent(activity, EditFilterActivity.class);
        intent.putExtra(PARAM_SORT_ORDER, sortOrder);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    public static void editFilter(Activity activity, Filter filter, int sortOrder) {
        Intent intent = new Intent(activity, EditFilterActivity.class);
        intent.putExtra(PARAM_FILTER, Parcels.wrap(filter));
        intent.putExtra(PARAM_SORT_ORDER, sortOrder);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_edit_filter;
    }

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        setHomeAsUpEnabled(true);
        mCollectionList = CollectionModel.getCollections(this);
        mFilterName = ((TextView) findViewById(R.id.name));
        mLimitByCollectionEnabled = ((Switch) findViewById(R.id.limit_by_collections_enabled));
        mLimitByCollectionEnabled.setOnCheckedChangeListener(this);
        mLimitSubscriptionsEnabled = ((Switch) findViewById(R.id.enable_episodes_per_subscription_limit));
        mLimitSubscriptionsEnabled.setOnCheckedChangeListener(this);
        mLimitByPublishedDateEnabled = ((Switch) findViewById(R.id.enable_episodes_published_since));
        mLimitByPublishedDateEnabled.setOnCheckedChangeListener(this);
        mLimitSubscriptionsPanel = findViewById(R.id.episodes_per_subscription_panel);
        mLimitByPublishedDatePanel = findViewById(R.id.episodes_by_date_panel);
        mLimitByCollectionsPanel = findViewById(R.id.episodes_by_collections_panel);
        mLimitSubscriptionsPanel.setOnClickListener(this);
        mLimitByPublishedDatePanel.setOnClickListener(this);
        mLimitByCollectionsPanel.setOnClickListener(this);
        findViewById(R.id.filter_name_panel).setOnClickListener(this);

        int sortOrder = getIntent().getIntExtra(PARAM_SORT_ORDER, -1);
        mFilter = Parcels.unwrap(getIntent().getParcelableExtra(PARAM_FILTER));

        if (mFilter != null) {
            populateUi();
            setTitle(R.string.filter_edit_title);
        } else {
            mFilter = new Filter();
            mFilter.setOrder(sortOrder + 1);
            mFilter.setUserCreated(true);
            setTitle(R.string.filter_create_title);
            mFilterName.setText(R.string.enter_a_name);
            mNewFilter = true;
        }
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.edit_filter_activity;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_delete:

                if (mFilter != null) {

                    new AlertDialog.Builder(this)
                            .setMessage(R.string.delete_filter)
                            .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                                deleteFilter();
                            })
                            .setNegativeButton(R.string.dialog_cancel, null)
                            .show();
                }
                return true;
            case R.id.action_save:
                saveFilter();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFilterChanged(boolean saved) {
        Intent intent = new Intent();
        intent.putExtra(PARAM_SORT_ORDER, mFilter.getOrder());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onFilterCreated(boolean saved) {
        Intent intent = new Intent();
        intent.putExtra(PARAM_SORT_ORDER, mFilter.getOrder());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onFilterDeleted(boolean saved) {
        Intent intent = new Intent();
        intent.putExtra(PARAM_SORT_ORDER, mFilter.getOrder() - 1);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.filter_name_panel:
                mDialog = new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                        .setView(R.layout.dialog_filter_name)
                        .setPositiveButton(R.string.dialog_save, (dialog, which) -> {

                            if (mDialog != null && which == Dialog.BUTTON_POSITIVE) {
                                String name = ((EditText) mDialog.findViewById(R.id.name)).getText().toString();
                                mFilter.setName(name);
                            }
                            populateFilterName();
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();

                if (!TextUtils.isEmpty(mFilter.getName())) {
                    ((EditText) mDialog.findViewById(R.id.name)).setText(mFilter.getName());
                }
                break;
            case R.id.episodes_by_collections_panel:
                int collectionIndex = getCollectionIndex(mFilter.getCollectionId());

                // add collections
                String[] collectionNames = new String[mCollectionList.size()];

                for (int i = 0; i < mCollectionList.size(); i++) {
                    collectionNames[i] = mCollectionList.get(i).getName();
                }
                new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.collection)
                        .setSingleChoiceItems(collectionNames, collectionIndex,
                                (dialog, which) -> {
                                    mFilter.setCollectionId(mCollectionList.get(which).getId());
                                })
                        .setPositiveButton(R.string.dialog_save, (dialog, which) -> {

                            // set the default choice
                            if (mFilter.getCollectionId() == FilterModel.DISABLED) {
                                mFilter.setCollectionId(mCollectionList.get(0).getId());
                            }

                            populateCollectionConfig();
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
                break;
            case R.id.episodes_by_date_panel:
                int selected = getDaysSinceIndex(mFilter.getDaysSincePublished());

                new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.filter_episode_newer_than)
                        .setSingleChoiceItems(R.array.filter_date_options, selected,
                                (dialog, which) -> {
                                    mFilter.setDaysSincePublished(getDaysSinceValue(which));
                                })
                        .setPositiveButton(R.string.dialog_save, (dialog, which) -> {

                            if (mFilter.getDaysSincePublished() == FilterModel.DISABLED) {
                                mFilter.setDaysSincePublished(getDaysSinceValue(0));
                            }
                            populatePublishedSinceConfig();
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
                break;
            case R.id.episodes_per_subscription_panel:
                int selectedEpisodesPerChannel = mFilter.getEpisodesPerChannel() == FilterModel.DISABLED ? 0 :
                        mFilter.getEpisodesPerChannel() - 1;

                new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                        .setTitle(R.string.filter_episodes_per_channel)
                        .setSingleChoiceItems(R.array.filter_episodes_per_options, selectedEpisodesPerChannel,
                                (dialog, which) -> {
                                    mFilter.setEpisodesPerChannel(which + 1);
                                })
                        .setPositiveButton(R.string.dialog_save, (dialog, which) -> {

                            if (mFilter.getEpisodesPerChannel() == FilterModel.DISABLED) {
                                mFilter.setEpisodesPerChannel(1);
                            }
                            populateEpisodesPerPodcastConfig();
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
                break;
        }
    }

    private Collection getCollection(int collectionId) {
        Collection collection = null;

        for (int i = 0; i < mCollectionList.size(); i++) {

            if (mCollectionList.get(i).getId() == collectionId) {
                collection = mCollectionList.get(i);
                break;
            }
        }

        return collection;
    }

    private int getCollectionIndex(int collectionId) {
        int index = 0;

        for (int i = 0; i < mCollectionList.size(); i++) {

            if (mCollectionList.get(i).getId() == collectionId) {
                index = i;
                break;
            }
        }
        return index;
    }

    private static int getDaysSinceIndex(int daysSincePublished) {

        switch (daysSincePublished) {
            case FilterModel.DAYS_SINCE_PUBLISHED_YESTERDAY:
                return 0;
            case FilterModel.DAYS_SINCE_PUBLISHED_LAST_WEEK:
                return 1;
            case FilterModel.DAYS_SINCE_PUBLISHED_LAST_MONTH:
                return 2;
            case FilterModel.DAYS_SINCE_PUBLISHED_LAST_SIX_MONTHS:
                return 3;
            default:
                return 0;
        }
    }

    private static int getDaysSinceValue(int daySinceIndex) {

        switch (daySinceIndex) {
            case 0:
                return FilterModel.DAYS_SINCE_PUBLISHED_YESTERDAY;
            case 1:
                return FilterModel.DAYS_SINCE_PUBLISHED_LAST_WEEK;
            case 2:
                return FilterModel.DAYS_SINCE_PUBLISHED_LAST_MONTH;
            case 3:
                return FilterModel.DAYS_SINCE_PUBLISHED_LAST_SIX_MONTHS;
            default:
                return FilterModel.DISABLED;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        switch (buttonView.getId()) {
            case R.id.enable_episodes_per_subscription_limit:
                mLimitSubscriptionsPanel.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                if (!isChecked) {
                    mFilter.setEpisodesPerChannel(FilterModel.DISABLED);
                }
                break;
            case R.id.enable_episodes_published_since:
                mLimitByPublishedDatePanel.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                if (!isChecked) {
                    mFilter.setDaysSincePublished(FilterModel.DISABLED);
                }

                break;
            case R.id.limit_by_collections_enabled:

                if (mCollectionList.size() > 0) {
                    mLimitByCollectionsPanel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(this, R.string.please_create_collection, Toast.LENGTH_SHORT).show();
                }

                if (!isChecked) {
                    mFilter.setCollectionId(FilterModel.DISABLED);
                }
                break;
        }
    }

    private void populateFilterName() {

        if (TextUtils.isEmpty(mFilter.getName())) {
            mFilterName.setText(R.string.enter_a_name);
        } else {
            mFilterName.setText(mFilter.getName());
        }
    }

    private void populateEpisodesPerPodcastConfig() {

        if (mFilter.getEpisodesPerChannel() > FilterModel.DISABLED) {
            mLimitSubscriptionsEnabled.setChecked(true);
            ((TextView) findViewById(R.id.episodes_per_subscription)).setText(String.valueOf(mFilter.getEpisodesPerChannel()));
            mLimitSubscriptionsPanel.setVisibility(View.VISIBLE);
        } else {
            mLimitSubscriptionsEnabled.setChecked(false);
            mLimitSubscriptionsPanel.setVisibility(View.GONE);
        }
    }

    private void populatePublishedSinceConfig() {

        if (mFilter.getDaysSincePublished() > FilterModel.DISABLED) {
            mLimitByPublishedDatePanel.setVisibility(View.VISIBLE);
            mLimitByPublishedDateEnabled.setChecked(true);
            ((TextView) findViewById(R.id.published_since)).setText(
                    getResources().getStringArray(R.array.filter_date_options)
                            [getDaysSinceIndex(mFilter.getDaysSincePublished())]);
        } else {
            mLimitByPublishedDateEnabled.setChecked(false);
            mLimitByPublishedDatePanel.setVisibility(View.GONE);
        }
    }

    private void populateCollectionConfig() {

        if (mFilter.getCollectionId() > FilterModel.DISABLED) {
            mLimitByCollectionsPanel.setVisibility(View.VISIBLE);
            mLimitByCollectionEnabled.setChecked(true);
            String collectionName = getCollection(mFilter.getCollectionId()).getName();
            ((TextView) findViewById(R.id.collection_name)).setText(collectionName);
        } else {
            mLimitByCollectionsPanel.setVisibility(View.GONE);
            mLimitByCollectionEnabled.setChecked(false);
        }
    }

    private void populateUi() {
        String episodeStatusIdsStr = mFilter.getEpisodeStatusIdsStr();
        ((Switch) findViewById(R.id.filter_new_episodes)).setChecked(
                episodeStatusIdsStr.contains(String.valueOf(EpisodeStatus.NEW)));
        ((Switch) findViewById(R.id.filter_played_episodes)).setChecked(
                episodeStatusIdsStr.contains(String.valueOf(EpisodeStatus.PLAYED)));
        ((Switch) findViewById(R.id.filter_completed_episodes)).setChecked(
                episodeStatusIdsStr.contains(String.valueOf(EpisodeStatus.COMPLETED)));

        String downloadStatusIdsStr = mFilter.getDownloadStatusIdsStr();
        ((Switch) findViewById(R.id.filter_downloaded_episodes)).setChecked(
                downloadStatusIdsStr.contains(String.valueOf(DownloadStatus.DOWNLOADED)));
        ((Switch) findViewById(R.id.filter_not_downloaded_episodes)).setChecked(
                downloadStatusIdsStr.contains(String.valueOf(DownloadStatus.NOT_DOWNLOADED)));

        ((Switch) findViewById(R.id.filter_favorited)).setChecked(mFilter.isFavorite());

        ((Switch) findViewById(R.id.filter_episodes_pinned)).setChecked(mFilter.isEpisodesManuallyAdded());

        populateFilterName();
        populateEpisodesPerPodcastConfig();
        populatePublishedSinceConfig();
        populateCollectionConfig();
    }

    private void deleteFilter() {
        FilterModel.deleteFilterAsync(this, mFilter.getId(), this);
    }

    private void saveFilter() {

        // save the favorite
        mFilter.setFavorite(((Switch) findViewById(R.id.filter_favorited)).isChecked());

        // set the episode status IDs
        List<Integer> episodeStatusIds = new ArrayList<>();

        if (((Switch) findViewById(R.id.filter_new_episodes)).isChecked()) {
            episodeStatusIds.add(EpisodeStatus.NEW);
        }

        if (((Switch) findViewById(R.id.filter_played_episodes)).isChecked()) {
            episodeStatusIds.add(EpisodeStatus.PLAYED);
        }

        if (((Switch) findViewById(R.id.filter_completed_episodes)).isChecked()) {
            episodeStatusIds.add(EpisodeStatus.COMPLETED);
        }

        // if we are filtering on episode status, always include episodes currently playing
        if (episodeStatusIds.size() > 0) {
            episodeStatusIds.add(EpisodeStatus.IN_PROGRESS);
        }

        Integer[] episodeIdsArr = new Integer[episodeStatusIds.size()];
        mFilter.setEpisodeStatusIds(episodeStatusIds.toArray(episodeIdsArr));

        // set the download status IDs
        List<Integer> downloadStatusIds = new ArrayList<>();

        if (((Switch) findViewById(R.id.filter_downloaded_episodes)).isChecked()) {
            downloadStatusIds.add(DownloadStatus.DOWNLOADED);
        }

        if (((Switch) findViewById(R.id.filter_not_downloaded_episodes)).isChecked()) {
            downloadStatusIds.add(DownloadStatus.NOT_DOWNLOADED);
        }

        Integer[] downloadIdsArr = new Integer[downloadStatusIds.size()];
        mFilter.setDownloadStatusIds(downloadStatusIds.toArray(downloadIdsArr));
        mFilter.setEpisodesManuallyAdded(((Switch) findViewById(R.id.filter_episodes_pinned)).isChecked());

        if (mNewFilter) {
            FilterModel.insertFilterAsync(this, mFilter, this);
        } else {
            FilterModel.updateFilterAsync(this, mFilter, this);
        }
    }
}