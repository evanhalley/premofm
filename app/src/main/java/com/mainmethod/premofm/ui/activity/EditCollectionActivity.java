/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.LoadListCallback;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.CollectionModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.data.model.FilterModel;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Collectable;
import com.mainmethod.premofm.object.Collection;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.object.Filter;
import com.mainmethod.premofm.ui.view.ItemTouchHelperAdapter;
import com.mainmethod.premofm.ui.view.RoundedCornersTransformation;
import com.mainmethod.premofm.ui.view.SimpleItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Permits the user to create/edit podcast collections
 * Created by evan on 2/14/15.
 */
public class EditCollectionActivity
        extends BaseActivity
        implements View.OnClickListener,
        DialogInterface.OnClickListener,
        LoadListCallback<Episode> {

    public static final String PARAM_NAME               = "name";
    public static final String PARAM_DESCRIPTION        = "description";
    public static final String PARAM_COLLECTION_ID      = "collectionId";
    public static final String PARAM_COLLECTION_TYPE    = "collectionType";
    public static final String PARAM_CREATE_FILTER      = "createFilter";
    public static final String PARAM_IS_PUBLIC          = "isPublic";

    private static final int MINIMUM_SEARCH_LENGTH      = 3;

    private CollectionItemsAdapter mAdapter;

    private List<Collectable> mCollectableList;
    private Collection mCollection;
    private boolean mCreateFilter = false;

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        setHomeAsUpEnabled(true);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.playlist);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        getToolbar().setOnClickListener(this);
        Bundle extras = getIntent().getExtras();

        mCollectableList = new ArrayList<>();
        mAdapter = new CollectionItemsAdapter(this, R.layout.item_collectable);
        recyclerView.setAdapter(mAdapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(mAdapter));
        touchHelper.attachToRecyclerView(recyclerView);

        if (extras.containsKey(PARAM_COLLECTION_ID)) {
            loadCollection();
        } else {
            // we are creating a new collection, let's set the title and such using values from the user
            mCollection = new Collection();
            mCollection.setName(extras.getString(PARAM_NAME));
            mCollection.setDescription(extras.getString(PARAM_DESCRIPTION));
            mCollection.setType(extras.getInt(PARAM_COLLECTION_TYPE));
            mCollection.setIsPublic(extras.getBoolean(PARAM_IS_PUBLIC));
            mCreateFilter = extras.getBoolean(PARAM_CREATE_FILTER);
            populateNameAndDescription(mCollection.getName(), mCollection.getDescription());

            // add the channels in the collection to the list view adapter
            if (mCollection.getType() == Collection.COLLECTION_TYPE_CHANNEL) {
                loadChannels();
            }
        }
    }

    private void loadCollection() {
        // load the collection with this ID from the database
        int id = getIntent().getExtras().getInt(PARAM_COLLECTION_ID);

        mCollection = CollectionModel.getCollectionById(this, id);
        populateNameAndDescription(mCollection.getName(), mCollection.getDescription());

        if (mCollection.getCollectedServerIds() == null) {
            return;
        }

        // add the channels in the collection to the list view adapter
        if (mCollection.getType() == Collection.COLLECTION_TYPE_CHANNEL) {
            loadChannels();
        }

        // load the episodes from the collection and add them to the adapter
        else if (mCollection.getType() == Collection.COLLECTION_TYPE_EPISODE) {
            EpisodeModel.getEpisodeByCollectionAsync(EditCollectionActivity.this,
                    mCollection, EditCollectionActivity.this);
        }
    }

    @Override
    public void onListLoaded(List<Episode> episodes) {
        mAdapter.addCollectables(new ArrayList<>(episodes));
    }

    public void loadChannels() {
        List<Channel> channels = ChannelModel.getChannels(this);
        mCollectableList = new ArrayList<>(channels.size());
        Map<String, Collectable> collectableMap = new HashMap<>();

        for (int i = 0; i < channels.size(); i++) {
            Collectable channel = channels.get(i);
            collectableMap.put(channel.getServerId(), channel);
            mCollectableList.add(channel);
        }

        List<Collectable> collectables = new ArrayList<>(mCollection.getCollectedServerIds().size());

        for (int i = 0; i < mCollection.getCollectedServerIds().size(); i++) {
            String serverId = mCollection.getCollectedServerIds().get(i);

            if (collectableMap.get(serverId) != null) {
                collectables.add(collectableMap.get(serverId));
            }
        }
        mAdapter.addCollectables(collectables);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.toolbar:
                showRenameCollectionDialog();
                break;
        }
    }

    public void handleAddChannelRequest() {
        // we only need to make unselected (for the collection) channels available for selection
        List<String> collectableTitles = new ArrayList<>();
        final List<Collectable> availableChannels = new ArrayList<>();

        for (int i = 0; i < mCollectableList.size(); i++) {

            if (!mAdapter.getCollectables().contains(mCollectableList.get(i))) {
                collectableTitles.add(mCollectableList.get(i).getTitle());
                availableChannels.add(mCollectableList.get(i));
            }
        }
        final boolean[] selectedItems = new boolean[collectableTitles.size()];
        CharSequence[] titles = new CharSequence[collectableTitles.size()];
        collectableTitles.toArray(titles);

        // build and show the channel selection dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.dialog_save, (dialog, which) -> {
                    List<Collectable> selectedChannels = new ArrayList<>();

                    // add the selected channels to the recycler view
                    for (int i = 0; i < availableChannels.size(); i++) {

                        if (selectedItems[i]) {
                            selectedChannels.add(availableChannels.get(i));
                        }
                    }
                    mAdapter.addCollectables(selectedChannels);
                    mAdapter.notifyDataSetChanged();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .setMultiChoiceItems(titles, null, (dialog, which, isChecked) -> {
                    selectedItems[which] = isChecked;
                });
        builder.create().show();
    }

    private void handleAddEpisodeRequest() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this)
                .setNeutralButton(R.string.dialog_close, null)
                .setView(R.layout.dialog_add_episode);
        Dialog dialog = alertBuilder.create();
        dialog.show();
        RecyclerView recyclerView = (RecyclerView) dialog.findViewById(
                R.id.collection_episode_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        final CollectionItemsAdapter adapter = new CollectionItemsAdapter(this,
                R.layout.item_collection_episode_result);
        adapter.setOnCollectableItemClickListener(position -> {
            Collectable collectable = adapter.getCollectables().get(position);
            Toast.makeText(EditCollectionActivity.this,
                    String.format(getString(R.string.collection_episode_added),
                            collectable.getTitle()),
                    Toast.LENGTH_SHORT).show();
            mAdapter.addCollectable(collectable);
        });
        recyclerView.setAdapter(adapter);
        ((EditText) dialog.findViewById(R.id.collection_episode_search)).addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                        if (s.length() > MINIMUM_SEARCH_LENGTH) {
                            // async task to search episodes
                            EpisodeModel.searchForEpisodesAsync(EditCollectionActivity.this, s.toString(),
                                    results -> {
                                        Log.d("EditCollectionActiivty", "Results Returened: " + results.size());
                                        adapter.clear();
                                        adapter.addCollectables(new ArrayList<>(results));
                                        adapter.notifyDataSetChanged();
                                    });
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_edit_collection;
    }

    @Override
    protected int getMenuResourceId() {
        return R.menu.menu_edit_collection_activity;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                saveCollection();
                return true;
            case R.id.action_delete_collection:
                showDeleteConfirmation();
                return true;
            case R.id.action_add_collectable:
                // show a dialog that allows the user to pick multiple channels to add
                switch (mCollection.getType()) {
                    case Collection.COLLECTION_TYPE_CHANNEL:
                        handleAddChannelRequest();
                        break;
                    case Collection.COLLECTION_TYPE_EPISODE:
                        handleAddEpisodeRequest();
                        break;
                    default:
                        break;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        saveCollection();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }

    /**
     * Shows a dialog that allows the user to rename the collection
     */
    private void showRenameCollectionDialog() {
        // show dialog that let's the user enter a name and description for their new playlist
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.dialog_save, this)
                .setNegativeButton(R.string.dialog_cancel, this)
                .setView(R.layout.dialog_name_collection);
        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        final EditText nameView = (EditText) dialog.findViewById(R.id.name);
        final EditText descriptionView = (EditText) dialog.findViewById(R.id.description);
        dialog.findViewById(R.id.collection_type).setVisibility(View.GONE);
        dialog.findViewById(R.id.create_filter).setVisibility(View.GONE);
        dialog.findViewById(R.id.collection_type_label).setVisibility(View.GONE);
        dialog.findViewById(R.id.publish_collection).setVisibility(View.GONE);
        nameView.setText(mCollection.getName());
        descriptionView.setText(mCollection.getDescription());

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameView.getText().toString();
            String description = descriptionView.getText().toString();

            if (!TextUtils.isEmpty(name)) {
                // open the create new playlist activity
                mCollection.setName(name);
                mCollection.setDescription(description);
                populateNameAndDescription(name, description);
                dialog.dismiss();
            } else {
                nameView.setError(getString(R.string.error_no_collection_name));
            }
        });
    }

    private void populateNameAndDescription(String name, String description) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(name);

        if (description != null ) {
            actionBar.setSubtitle(description);
        }
    }

    /**
     * Shows a delete confirmation
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.dialog_delete_collection_confirmation)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    deleteCollection();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /**
     * Initiates saving a collection to the database
     */
    private void saveCollection() {
        // save the collection
        List<Collectable> collectables = mAdapter.getCollectables();
        List<String> serverIds = new ArrayList<>(collectables.size());

        for (Collectable collectable : collectables) {
            serverIds.add(collectable.getServerId());
        }
        mCollection.setCollectedServerIds(serverIds);
        int collectionId = CollectionModel.saveCollection(this, mCollection, true);

        if (mCreateFilter && collectionId > -1) {
            List<Filter> filters = FilterModel.getFilters(this);
            Filter filter = new Filter();
            filter.setCollectionId(collectionId);
            filter.setName(mCollection.getName());
            filter.setEpisodeStatusIds(new Integer[]{EpisodeStatus.NEW, EpisodeStatus.PLAYED});
            filter.setDownloadStatusIds(new Integer[0]);
            filter.setUserCreated(true);
            filter.setOrder(filters != null ? filters.size() : 0);
            filter.setEpisodesPerChannel(2);
            filter.setDaysSincePublished(FilterModel.DISABLED);
            filter.setEpisodesManuallyAdded(false);
            FilterModel.insertFilter(this, filter);
        }
        finish();
    }

    /**
     * Initiates deleting a collection from the database
     */
    private void deleteCollection() {

        // delete the collection
        if (mCollection.getId() > -1) {
            CollectionModel.markCollectionToDelete(this, mCollection.getId());
        }
        finish();
    }

    private static class CollectionItemsAdapter extends
            RecyclerView.Adapter<CollectionItemsAdapter.PlaylistChannelHolder> implements ItemTouchHelperAdapter {

        private final Context mContext;
        private final List<Collectable> mCollectablesList;
        private final int mLayoutResId;
        private OnCollectableItemClickListener mListener;

        public CollectionItemsAdapter(Context context, int layoutResId) {
            super();
            setHasStableIds(true);
            mContext = context;
            mCollectablesList = new ArrayList<>();
            mLayoutResId = layoutResId;
        }

        public void setOnCollectableItemClickListener(OnCollectableItemClickListener listener) {
            mListener = listener;
        }

        public void addCollectables(List<Collectable> collectables) {
            int start = mCollectablesList.size();
            mCollectablesList.addAll(collectables);
            notifyItemRangeInserted(start, collectables.size());
        }

        public void addCollectable(Collectable collectable) {
            mCollectablesList.add(collectable);
            notifyItemInserted(mCollectablesList.size() - 1);
        }

        public void clear() {
            mCollectablesList.clear();
        }

        public List<Collectable> getCollectables() {
            return mCollectablesList;
        }

        @Override
        public void onItemDismiss(int position) {
            mCollectablesList.remove(position);
            notifyItemRemoved(position);
        }

        @Override
        public void onItemMove(int fromPosition, int toPosition) {
            Collections.swap(mCollectablesList, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public long getItemId(int position) {
            return mCollectablesList.get(position).getId();
        }

        @Override
        public PlaylistChannelHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(mLayoutResId, parent, false);
            return new PlaylistChannelHolder(itemView);
        }

        @Override
        public void onBindViewHolder(PlaylistChannelHolder holder, int position) {
            final Collectable collectable = mCollectablesList.get(position);
            holder.position = position;
            holder.title.setText(collectable.getTitle());
            holder.subtitle.setText(collectable.getSubtitle());

            if (holder.remove != null) {
                holder.remove.setOnClickListener(v -> {
                    mCollectablesList.remove(collectable);
                    notifyDataSetChanged();
                });
            }
            ImageLoadHelper.loadImageIntoView(mContext, collectable.getArtworkUrl(),
                    holder.channelArt, new RoundedCornersTransformation(mContext));
        }

        @Override
        public int getItemCount() {
            return mCollectablesList.size();
        }

        public class PlaylistChannelHolder extends RecyclerView.ViewHolder {

            public int position;
            public TextView title;
            public TextView subtitle;
            public ImageView channelArt;
            public ImageButton remove;

            public PlaylistChannelHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.title);
                subtitle = (TextView) itemView.findViewById(R.id.subtitle);
                channelArt = (ImageView) itemView.findViewById(R.id.channel_art);

                itemView.setOnClickListener(v -> {

                    if (mListener != null) {
                        mListener.onClick(position);
                    }
                });
            }
        }

        public interface OnCollectableItemClickListener {
            void onClick(int position);
        }
    }
}
