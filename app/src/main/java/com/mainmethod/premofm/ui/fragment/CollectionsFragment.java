/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.fragment;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.CollectionModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.data.model.PlaylistModel;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.helper.ShowcaseHelper;
import com.mainmethod.premofm.object.Collectable;
import com.mainmethod.premofm.object.Collection;
import com.mainmethod.premofm.object.Playlist;
import com.mainmethod.premofm.service.PodcastPlayerService;
import com.mainmethod.premofm.ui.activity.BaseActivity;
import com.mainmethod.premofm.ui.activity.EditCollectionActivity;
import com.mainmethod.premofm.ui.adapter.CursorRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows collections of podcasts created by the user
 * Created by evan on 12/3/14.
 */
public class CollectionsFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>,
       DialogInterface.OnClickListener, View.OnClickListener {

    private RecyclerView mRecyclerView;
    private CollectionCursorAdapter mAdapter;
    private View mEmptyListView;

    /**
     * Creates a new instance of this fragment
     * @return
     */
    public static CollectionsFragment newInstance() {
        CollectionsFragment fragment = new CollectionsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mEmptyListView = view.findViewById(R.id.empty_list);
        mEmptyListView.findViewById(R.id.button_empty_list).setOnClickListener(this);
        mAdapter = new CollectionCursorAdapter(getActivity(), null);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.playlist);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getResources().getInteger(
                R.integer.collection_fragment_columns)));
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(CollectionModel.LOADER_ID, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_collection:
                showAddCollectionDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.button_empty_list:
                showAddCollectionDialog();
                break;
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        ShowcaseHelper.showCollectionShowcase(getBaseActivity());
    }

    private void showAddCollectionDialog() {
        // show dialog that let's the user enter a name and description for their new playlist
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.dialog_create, this)
                .setNegativeButton(R.string.dialog_cancel, this)
                .setView(R.layout.dialog_name_collection);
        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        final EditText nameView = (EditText) dialog.findViewById(R.id.name);
        final EditText descriptionView = (EditText) dialog.findViewById(R.id.description);
        final Switch createFilter = (Switch) dialog.findViewById(R.id.create_filter);
        final Switch publish = (Switch) dialog.findViewById(R.id.publish_collection);
        final RadioButton channelType = (RadioButton) dialog.findViewById(R.id.channels_selection);
        final RadioButton episodeType = (RadioButton) dialog.findViewById(R.id.episodes_selection);

        channelType.setOnCheckedChangeListener((buttonView, isChecked) -> episodeType.setChecked(!isChecked));

        episodeType.setOnCheckedChangeListener((buttonView, isChecked) -> channelType.setChecked(!isChecked));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameView.getText().toString();
            String description = descriptionView.getText().toString();
            int collectionType = channelType.isChecked() ? Collection.COLLECTION_TYPE_CHANNEL :
                    Collection.COLLECTION_TYPE_EPISODE;

            if (TextUtils.isEmpty(name)) {
                nameView.setError(getString(R.string.error_no_collection_name));
                return;
            }

            // open the create new playlist activity
            Bundle extras = new Bundle();
            extras.putString(EditCollectionActivity.PARAM_NAME, name);
            extras.putString(EditCollectionActivity.PARAM_DESCRIPTION, description);
            extras.putInt(EditCollectionActivity.PARAM_COLLECTION_TYPE, collectionType);
            extras.putBoolean(EditCollectionActivity.PARAM_CREATE_FILTER, createFilter.isChecked());
            getBaseActivity().startPremoActivity(EditCollectionActivity.class, -1, extras);
            dialog.dismiss();
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return CollectionModel.getCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor != null && cursor.moveToFirst()) {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyListView.setVisibility(View.INVISIBLE);

            switch (loader.getId()) {
                case CollectionModel.LOADER_ID:
                    mAdapter.changeCursor(cursor);
                    break;
            }
        } else {
            // hide the recycler view
            mRecyclerView.setVisibility(View.INVISIBLE);
            mEmptyListView.setVisibility(View.VISIBLE);
            ((TextView) mEmptyListView.findViewById(R.id.empty_list_title)).setText(
                    R.string.no_collections_title);
            ((TextView) mEmptyListView.findViewById(R.id.empty_list_message)).setText(
                    R.string.no_collections_message);
            ((Button) mEmptyListView.findViewById(R.id.button_empty_list)).setText(
                    R.string.button_create_collection);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // do nothing, override it so it doesn't close
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_collections;
    }

    @Override
    protected int getFragmentTitleResourceId() {
        return R.string.title_fragment_collections;
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.collections_fragment;
    }

    /**
     * Shows the collections on the device
     */
    private static class CollectionCursorAdapter extends CursorRecyclerViewAdapter<CollectionHolder> {

        private final Context mContext;

        public CollectionCursorAdapter(Context context, Cursor cursor) {
            super(cursor);
            mContext = context;
        }

        @Override
        public void onBindViewHolder(CollectionHolder viewHolder, Cursor cursor, int position) {
            final Collection collection = CollectionModel.toCollection(cursor);
            viewHolder.name.setText(collection.getName());
            viewHolder.collectionId = collection.getId();
            List<String> serverIds = collection.getCollectedServerIds();

            switch (collection.getType()) {
                case Collection.COLLECTION_TYPE_CHANNEL:

                    if (collection.getCollectedServerIds().size() == 1) {
                        viewHolder.count.setText(String.format(mContext.getString(R.string.channel_label), 1));
                    } else {
                        viewHolder.count.setText(String.format(mContext.getString(R.string.channels_label),
                                collection.getCollectedServerIds().size()));
                    }
                    break;
                case Collection.COLLECTION_TYPE_EPISODE:
                    if (collection.getCollectedServerIds().size() == 1) {
                        viewHolder.count.setText(String.format(mContext.getString(R.string.episode_label), 1));
                    } else {
                        viewHolder.count.setText(String.format(mContext.getString(R.string.episodes_label),
                                collection.getCollectedServerIds().size()));
                    }
                    break;
            }

            // set new images
            List<Collectable> collectables = new ArrayList<>(serverIds.size());

            for (int i = 0; i < serverIds.size(); i++) {
                Collectable collectable;

                if (collection.getType() == Collection.COLLECTION_TYPE_CHANNEL) {
                    collectable = ChannelModel.getChannelByGeneratedId(mContext, serverIds.get(i));
                } else {
                    collectable = EpisodeModel.getEpisodeByGeneratedId(mContext, serverIds.get(i));
                }

                if (collectable != null) {
                    collectables.add(collectable);
                }
            }

            // how many columns
            viewHolder.gridView.setNumColumns((int) Math.ceil(Math.sqrt(serverIds.size())));
            viewHolder.gridView.setAdapter(new ChannelArtAdapter(mContext, collectables));
        }

        @Override
        public CollectionHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View itemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_collection, viewGroup, false);
            return new CollectionHolder(itemView);
        }
    }

    /**
     * Holder for each collection
     */
    private static class CollectionHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener {

        private int collectionId;
        private TextView name;
        private TextView count;
        private GridView gridView;
        private ImageButton more;

        private PopupMenu popupMenu;

        public CollectionHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.collection_name);
            count = (TextView) itemView.findViewById(R.id.collection_item_count);
            gridView = (GridView) itemView.findViewById(R.id.channel_art_gallery);
            more = (ImageButton) itemView.findViewById(R.id.card_more);

            itemView.findViewById(R.id.collection);
            itemView.setOnClickListener(this);
            gridView.setOnItemClickListener(this);
            more.setOnClickListener(this);

            popupMenu = new PopupMenu(more.getContext(), more);
            popupMenu.getMenuInflater().inflate(R.menu.collection_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {

            switch (item.getItemId()) {
                case R.id.action_delete:
                    CollectionModel.markCollectionToDelete(itemView.getContext(), collectionId);
                    return true;
                case R.id.action_play_collection:
                    Playlist playlist = PlaylistModel.buildPlaylistFromCollection(
                            itemView.getContext(), collectionId);
                    PodcastPlayerService.playPlaylist(itemView.getContext(), playlist);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            loadCollection();
        }

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.channel_art_gallery:
                case R.id.item_collection:
                case R.id.collection:
                    loadCollection();
                    break;
                case R.id.card_more:
                    popupMenu.show();
                    break;

            }
        }

        private void loadCollection() {
            Bundle extras = new Bundle();
            extras.putInt(EditCollectionActivity.PARAM_COLLECTION_ID, collectionId);
            ((BaseActivity) itemView.getContext()).startPremoActivity(EditCollectionActivity.class, -1, extras);
        }
    }

    /**
     * Adapter for the channel art gallery in each collection item
     */
    private static class ChannelArtAdapter extends ArrayAdapter<Collectable> {

        private LayoutInflater mLayoutInflater;

        public ChannelArtAdapter(Context context, List<Collectable> collectables) {
            super(context, -1, collectables);
            mLayoutInflater = LayoutInflater.from(context);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.item_channel_art, parent, false);
            }
            Collectable collectable = getItem(position);
            ImageLoadHelper.loadImageIntoView(
                    mLayoutInflater.getContext(),
                    collectable.getArtworkUrl(),
                    (ImageView) convertView);
            return convertView;
        }
    }
}
