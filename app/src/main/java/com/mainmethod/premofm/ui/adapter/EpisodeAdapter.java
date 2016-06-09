/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.data.model.FilterModel;
import com.mainmethod.premofm.helper.AnalyticsHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.ImageLoadHelper;
import com.mainmethod.premofm.helper.IntentHelper;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.object.Filter;
import com.mainmethod.premofm.object.Playlist;
import com.mainmethod.premofm.service.PodcastPlayerService;
import com.mainmethod.premofm.ui.holder.ChannelInfoHolder;
import com.mainmethod.premofm.ui.holder.EpisodeHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Binds episodes and possibly channel information to a recycler view
 * Created by evan on 12/7/14.
 */
public class EpisodeAdapter extends
        CursorRecyclerViewAdapter<RecyclerView.ViewHolder>
    implements View.OnClickListener, EpisodeHolder.PlayClickListener, EpisodeHolder.PinClickListener {

    private static final int ITEM_TYPE_CHANNEL_INFO = 0;
    private static final int ITEM_TYPE_EPISODE = 1;

    private final WeakReference<Context> mContext;

    private Channel mChannel;
    private boolean mChannelSubscriptionChangePending;
    private final Filter mFilter;
    private boolean mShowChannelInfoCard;

    public EpisodeAdapter(Context context, Cursor cursor, Filter filter) {
        super(cursor, PremoContract.EpisodeEntry.UPDATED_AT);
        mContext = new WeakReference<>(context);
        mFilter = filter;
        mChannel = null;
    }

    public EpisodeAdapter(Context context, Cursor cursor, Channel channel) {
        super(cursor, PremoContract.EpisodeEntry.UPDATED_AT);
        mContext = new WeakReference<>(context);
        mChannel = channel;
        mFilter = null;
        mShowChannelInfoCard = true;
    }

    public void setChannel(Channel channel) {
        mChannelSubscriptionChangePending = false;
        mChannel = channel;
    }

    @Override
    public int getItemCount() {

        if (mShowChannelInfoCard) {
            return super.getItemCount();
        } else {
            return super.getItemCount();
        }
    }

    public Episode getEpisode(int position) {
        Episode episode = null;
        Cursor cursor = getCursor();
        int oldPos = cursor.getPosition();

        if (cursor.moveToPosition(position)) {
            episode = EpisodeModel.toEpisode(cursor);
            cursor.moveToPosition(oldPos);
        }
        return episode;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, Cursor cursor, int position) {

        if (viewHolder instanceof ChannelInfoHolder) {
            bindChannelInfoViewToHolder((ChannelInfoHolder) viewHolder, cursor);
        } else if (viewHolder instanceof EpisodeHolder) {
            bindEpisodeViewToHolder((EpisodeHolder) viewHolder, cursor, position);
        }
    }

    @Override
    public int getItemViewType(int position) {

        switch (position) {
            case 0:

                if (mShowChannelInfoCard) {
                    return ITEM_TYPE_CHANNEL_INFO;
                } else {
                    return ITEM_TYPE_EPISODE;
                }
            default:
                return ITEM_TYPE_EPISODE;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        switch (viewType) {
            case ITEM_TYPE_CHANNEL_INFO:
                return new ChannelInfoHolder(
                        LayoutInflater.from(viewGroup.getContext())
                                .inflate(R.layout.item_channel_info, viewGroup, false), this);
            case ITEM_TYPE_EPISODE:
                if (mShowChannelInfoCard) {
                    return new EpisodeHolder(
                            LayoutInflater.from(viewGroup.getContext())
                                    .inflate(R.layout.item_channel_episode, viewGroup, false),
                            this, this);
                } else {
                    return new EpisodeHolder(
                            LayoutInflater.from(viewGroup.getContext())
                                    .inflate(R.layout.item_episode, viewGroup, false),
                            this, null);
                }
            default:
                throw new IllegalArgumentException(String.format("Unknown view type %d", viewType));
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.subscribe:
                //TODO SUBSCRIBE
                break;
            case R.id.website:
                IntentHelper.openBrowser(v.getContext(), mChannel.getSiteUrl());
                break;
        }
    }

    @Override
    public void onPlayClick(int episodeId, String action) {
        Context context = mContext.get();

        if (context == null) {
            return;
        }

        if (action.contentEquals(PodcastPlayerService.ACTION_PAUSE)) {
            PodcastPlayerService.sendIntent(context, action, episodeId);
        } else {
            // add the next 5 episodes to now playing queue if continuous playback is enabled
            if (UserPrefHelper.get(context).getBoolean(R.string.pref_key_continuous_playback)) {
                ArrayList<String> episodeServerIds = FilterModel.getNextEpisodesForPlaylist(context,
                        mFilter, 10, episodeId);
                PodcastPlayerService.playPlaylist(context, new Playlist(episodeServerIds));
            } else {
                PodcastPlayerService.sendIntent(context, action, episodeId);
            }
        }
    }

    @Override
    public void onPinClick(int index) {
        Context context = mContext.get();

        if (context == null) {
            return;
        }
        Episode episode = getEpisode(index);

        if (episode != null && !episode.isManuallyAdded() && !episode.isChannelSubscribed()) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(EpisodeModel.PARAM_MANUALLY_ADDED, true);
            boolean added = EpisodeModel.updateEpisode(context, episode.getId(), bundle);

            if (added) {
                Toast.makeText(context, R.string.episode_pinned, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.episode_not_pinned, Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(context, R.string.episode_pinned, Toast.LENGTH_SHORT).show();
            AnalyticsHelper.sendEvent(context,
                    AnalyticsHelper.CATEGORY_PIN,
                    AnalyticsHelper.ACTION_CLICK,
                    null);
        } else {
            Toast.makeText(context, R.string.episode_already_pinned, Toast.LENGTH_SHORT).show();
        }
    }

    private void bindChannelInfoViewToHolder(ChannelInfoHolder channelInfoHolder, Cursor cursor) {
        Context context = mContext.get();

        if (context == null) {
            return;
        }

        channelInfoHolder.title.setText(mChannel.getTitle());
        channelInfoHolder.author.setText(mChannel.getAuthor());
        channelInfoHolder.website.setOnClickListener(this);
        channelInfoHolder.subscribe.setOnClickListener(this);

        if (TextUtils.isEmpty(mChannel.getDescription()) || mChannel.getDescription().contentEquals("null")) {
            channelInfoHolder.description.setVisibility(View.GONE);
        } else {
            channelInfoHolder.description.setVisibility(View.VISIBLE);
            channelInfoHolder.description.setText(mChannel.getDescription());
        }

        channelInfoHolder.website.setVisibility(TextUtils.isEmpty(mChannel.getSiteUrl()) ?
            View.GONE : View.VISIBLE);

        if (mChannel.isSubscribed()) {
            channelInfoHolder.subscribe.setText(context.getString(mChannelSubscriptionChangePending ?
                    R.string.button_unsubscribing : R.string.button_unsubscribe));
        } else {
            channelInfoHolder.subscribe.setText(context.getString(mChannelSubscriptionChangePending ?
                    R.string.button_subscribing : R.string.button_subscribe));
        }
        channelInfoHolder.subscribe.setEnabled(!mChannelSubscriptionChangePending);
        bindEpisodeViewToHolder(channelInfoHolder.episodeHolder, cursor, 0);
    }

    private void bindEpisodeViewToHolder(EpisodeHolder episodeHolder, Cursor cursor, int position) {
        Context context = mContext.get();

        if (context == null) {
            return;
        }

        Episode episode = EpisodeModel.toEpisode(cursor);
        episodeHolder.episodeId = episode.getId();
        episodeHolder.episodeServerId = episode.getServerId();
        episodeHolder.channelServerId = episode.getChannelServerId();
        episodeHolder.updatedAt = episode.getUpdatedAt();
        episodeHolder.episodeTitle.setText(episode.getTitle());
        episodeHolder.downloadStatusId = episode.getDownloadStatus();
        episodeHolder.isChannelSubscribed = episode.isChannelSubscribed();

        if (!mShowChannelInfoCard) {
            ImageLoadHelper.loadImageIntoView(context, episode.getChannelArtworkUrl(),
                    episodeHolder.channelArt);
            episodeHolder.channelTitle.setText(episode.getChannelTitle());
        }
        configureUiForDownloadStatus(episodeHolder, episode);
        configureUiForEpisodeStatus(episodeHolder, episode);
        configureMenuForEpisodeStatus(episodeHolder, episode);
        configureMenuForDownloadStatus(episodeHolder, episode);

        if (episode.getDescription() == null || episode.getDescription().contentEquals("null") ||
                episode.getDescription().length() == 0) {
            episodeHolder.description.setVisibility(View.GONE);
        } else {
            episodeHolder.description.setVisibility(View.VISIBLE);
            episodeHolder.description.setText(episode.getDescription());
        }
        episodeHolder.publishedAt.setText(DatetimeHelper.dateToShortReadableString(
                context, episode.getPublishedAt()));

        if (mChannel != null && !mChannel.isSubscribed() && !episode.isManuallyAdded()) {
            episodeHolder.pin.setVisibility(View.VISIBLE);
        } else {
            episodeHolder.pin.setVisibility(View.GONE);
        }

        // readable string for the duration
        if (episode.getDuration() > 0) {
            episodeHolder.duration.setVisibility(View.VISIBLE);
            episodeHolder.duration.setText(AdapterHelper.buildDurationString(
                    context,
                    episode.getEpisodeStatus(),
                    episode.getDuration(),
                    episode.getProgress()));
        } else {
            episodeHolder.duration.setVisibility(View.GONE);
        }

        // set the appropriate menu text
        episodeHolder.favoriteMenu.setTitle(episode.isFavorite() ? R.string.action_unfavorite :
                R.string.action_favorite);

        // show or hide the favorite icon
        episodeHolder.favorite.setVisibility(episode.isFavorite() ? View.VISIBLE : View.GONE);

        // show or hide the pinned icon
        episodeHolder.pinned.setVisibility(episode.isManuallyAdded() ? View.VISIBLE : View.GONE);
    }

    private void configureMenuForEpisodeStatus(EpisodeHolder holder, Episode episode) {
        holder.deleteEpisode.setVisible(episode.isManuallyAdded());

        switch (episode.getEpisodeStatus()) {
            case EpisodeStatus.NEW:
                holder.queueMenu.setVisible(true);
                holder.completedMenu.setVisible(true);
                break;
            case EpisodeStatus.IN_PROGRESS:
                holder.queueMenu.setVisible(false);
                holder.completedMenu.setVisible(true);
                break;
            case EpisodeStatus.PLAYED:
                holder.queueMenu.setVisible(true);
                holder.completedMenu.setVisible(true);
                break;
            case EpisodeStatus.COMPLETED:
                holder.queueMenu.setVisible(true);
                holder.completedMenu.setVisible(false);
                break;
            case EpisodeStatus.DELETED:
                holder.queueMenu.setVisible(true);
                holder.completedMenu.setVisible(true);
                break;
        }
    }

    private void configureMenuForDownloadStatus(EpisodeHolder holder, Episode episode) {

        switch (episode.getDownloadStatus()) {
            case DownloadStatus.PARTIALLY_DOWNLOADED:
                holder.deleteDownloadMenu.setVisible(true);
                holder.downloadMenu.setVisible(false);
                holder.cancelMenu.setVisible(true);
                break;
            case DownloadStatus.DOWNLOADED:
                holder.deleteDownloadMenu.setVisible(
                        episode.getEpisodeStatus() != EpisodeStatus.IN_PROGRESS);
                holder.downloadMenu.setVisible(false);
                holder.cancelMenu.setVisible(false);
                break;
            case DownloadStatus.REQUESTED:
            case DownloadStatus.QUEUED:
                holder.deleteDownloadMenu.setVisible(false);
                holder.downloadMenu.setVisible(false);
                holder.cancelMenu.setVisible(true);
                break;
            case DownloadStatus.DOWNLOADING:
                holder.downloadMenu.setVisible(false);
                holder.deleteDownloadMenu.setVisible(false);
                holder.cancelMenu.setVisible(true);
                break;
            case DownloadStatus.NOT_DOWNLOADED:
                holder.deleteDownloadMenu.setVisible(false);
                holder.downloadMenu.setVisible(true);
                holder.cancelMenu.setVisible(false);
                break;
        }
    }

    private void configureUiForEpisodeStatus(EpisodeHolder holder, Episode episode) {
        Context context = mContext.get();

        if (context == null) {
            return;
        }

        switch (episode.getEpisodeStatus()) {
            case EpisodeStatus.NEW:
                holder.episodeTitle.setTextAppearance(context, R.style.Card_Text_Title_New);
                holder.description.setTextAppearance(context, R.style.DescriptionStyle);
                if (!mShowChannelInfoCard) {
                    holder.channelTitle.setTextAppearance(context, R.style.Card_Text_ChannelTitle);
                }
                holder.duration.setTextAppearance(context, R.style.Card_Text_Duration);
                holder.publishedAt.setTextAppearance(context, R.style.Card_Text_PublishedDate);
                holder.play.setImageResource(R.drawable.ic_item_action_play);
                holder.play.setTag(EpisodeHolder.MODE_WILL_PLAY);
                break;
            case EpisodeStatus.IN_PROGRESS:
                holder.episodeTitle.setTextAppearance(context, R.style.Card_Text_Title);
                holder.description.setTextAppearance(context, R.style.DescriptionStyle);
                if (!mShowChannelInfoCard) {
                    holder.channelTitle.setTextAppearance(context, R.style.Card_Text_ChannelTitle);
                }
                holder.duration.setTextAppearance(context, R.style.Card_Text_Duration);
                holder.publishedAt.setTextAppearance(context, R.style.Card_Text_PublishedDate);
                holder.play.setImageResource(R.drawable.ic_item_action_pause);
                holder.play.setTag(EpisodeHolder.MODE_WILL_PAUSE);
                break;
            case EpisodeStatus.PLAYED:
                holder.episodeTitle.setTextAppearance(context, R.style.Card_Text_Title);
                holder.description.setTextAppearance(context, R.style.DescriptionStyle);
                if (!mShowChannelInfoCard) {
                    holder.channelTitle.setTextAppearance(context, R.style.Card_Text_ChannelTitle);
                }
                holder.duration.setTextAppearance(context, R.style.Card_Text_Duration);
                holder.publishedAt.setTextAppearance(context, R.style.Card_Text_PublishedDate);
                holder.play.setImageResource(R.drawable.ic_item_action_play);
                holder.play.setTag(EpisodeHolder.MODE_WILL_PLAY);
                break;
            case EpisodeStatus.COMPLETED:
                holder.episodeTitle.setTextAppearance(context, R.style.Card_Text_Title_Completed);
                holder.description.setTextAppearance(context, R.style.DescriptionStyle_Completed);
                if (!mShowChannelInfoCard) {
                    holder.channelTitle.setTextAppearance(context, R.style.Card_Text_ChannelTitle_Completed);
                }
                holder.duration.setTextAppearance(context, R.style.Card_Text_Duration_Completed);
                holder.publishedAt.setTextAppearance(context, R.style.Card_Text_PublishedDate_Completed);
                holder.play.setImageResource(R.drawable.ic_item_action_play);
                holder.play.setTag(EpisodeHolder.MODE_WILL_PLAY);
                break;
            case EpisodeStatus.DELETED:
                holder.episodeTitle.setTextAppearance(context, R.style.Card_Text_Title_Completed);
                holder.description.setTextAppearance(context, R.style.DescriptionStyle);
                if (!mShowChannelInfoCard) {
                    holder.channelTitle.setTextAppearance(context, R.style.Card_Text_ChannelTitle_Completed);
                }
                holder.duration.setTextAppearance(context, R.style.Card_Text_Duration_Completed);
                holder.publishedAt.setTextAppearance(context, R.style.Card_Text_PublishedDate_Completed);
                holder.play.setImageResource(R.drawable.ic_item_action_play);
                holder.play.setTag(EpisodeHolder.MODE_WILL_PLAY);
                break;
        }
    }

    private void configureUiForDownloadStatus(EpisodeHolder holder, Episode episode) {

        switch (episode.getDownloadStatus()) {
            case DownloadStatus.PARTIALLY_DOWNLOADED:
                holder.downloadProgress.setIndeterminate(false);
                holder.downloadProgress.setVisibility(View.INVISIBLE);
                holder.play.setVisibility(View.INVISIBLE);
                holder.downloaded.setVisibility(View.GONE);
                break;
            case DownloadStatus.DOWNLOADED:
                holder.downloadProgress.setIndeterminate(false);
                holder.downloadProgress.setVisibility(View.INVISIBLE);
                holder.play.setVisibility(View.VISIBLE);
                holder.play.setEnabled(true);
                holder.downloaded.setVisibility(View.VISIBLE);
                break;
            case DownloadStatus.REQUESTED:
            case DownloadStatus.QUEUED:
                holder.downloadProgress.setIndeterminate(true);
                holder.downloadProgress.setVisibility(View.VISIBLE);
                holder.play.setEnabled(false);
                holder.downloaded.setVisibility(View.GONE);
                break;
            case DownloadStatus.DOWNLOADING:
                holder.downloadProgress.setVisibility(View.VISIBLE);
                holder.downloadProgress.setIndeterminate(false);
                holder.downloadProgress.setProgress((int) ((double) episode.getDownloadedSize() /
                        (double) episode.getSize() * 100));
                holder.play.setEnabled(false);
                holder.downloaded.setVisibility(View.GONE);
                break;
            case DownloadStatus.NOT_DOWNLOADED:
                holder.downloadProgress.setIndeterminate(false);
                holder.downloadProgress.setVisibility(View.INVISIBLE);
                holder.play.setVisibility(View.VISIBLE);
                holder.play.setEnabled(true);
                holder.downloaded.setVisibility(View.GONE);
                break;
        }
    }
}