/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.holder;

import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.data.model.PlaylistModel;
import com.mainmethod.premofm.helper.IntentHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.service.DeleteEpisodeService;
import com.mainmethod.premofm.service.DownloadService;
import com.mainmethod.premofm.service.PodcastPlayerService;
import com.mainmethod.premofm.ui.activity.BaseActivity;
import com.mainmethod.premofm.ui.activity.ChannelProfileActivity;
import com.mainmethod.premofm.ui.activity.PlayableActivity;

/**
 * Data model for episode items in list views
 * Created by evan on 8/20/15.
 */
public class EpisodeHolder extends RecyclerView.ViewHolder implements RecyclerView.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    public static final int MODE_WILL_PLAY = 0;
    public static final int MODE_WILL_PAUSE = 1;

    public int episodeId = -1;
    public int downloadStatusId = -1;
    public String episodeServerId;
    public View publishedData;
    public TextView episodeTitle;
    public TextView channelTitle;
    public TextView description;
    public TextView publishedAt;
    public TextView duration;
    public ProgressBar downloadProgress;
    public ImageView channelArt;
    public ImageView favorite;
    public ImageView downloaded;
    public ImageView pinned;
    public boolean isChannelSubscribed;

    public ImageButton pin;
    public ImageButton play;
    public ImageButton more;

    public PopupMenu popupMenu;

    public MenuItem queueMenu;
    public MenuItem completedMenu;
    public MenuItem favoriteMenu;
    public MenuItem downloadMenu;
    public MenuItem deleteDownloadMenu;
    public MenuItem cancelMenu;
    public MenuItem deleteEpisode;

    public String channelServerId;
    public long updatedAt;
    private final PlayClickListener mPlayClickListener;
    private final PinClickListener mPinClickListener;

    public EpisodeHolder(View view, PlayClickListener playClickListener, PinClickListener pinClickListener) {
        super(view);
        mPlayClickListener = playClickListener;
        mPinClickListener = pinClickListener;
        view.setOnClickListener(this);
        episodeTitle = (TextView) view.findViewById(R.id.episode_title);
        channelTitle = (TextView) view.findViewById(R.id.channel_title);
        description = (TextView) view.findViewById(R.id.description);
        description.setOnClickListener(this);
        publishedAt = (TextView) view.findViewById(R.id.published_at);
        duration = (TextView) view.findViewById(R.id.duration);
        channelArt = (ImageView) view.findViewById(R.id.channel_art);
        favorite = (ImageView) view.findViewById(R.id.favorite);
        downloaded = (ImageView) view.findViewById(R.id.downloaded);
        pinned = (ImageView) view.findViewById(R.id.pinned);
        downloadProgress = (ProgressBar) view.findViewById(R.id.download_progress);
        publishedData = view.findViewById(R.id.published_data);

        if (publishedData != null) {
            publishedData.setOnClickListener(this);
            channelArt.setOnClickListener(this);
        }
        // our action buttons
        pin = (ImageButton) view.findViewById(R.id.pin);
        pin.setOnClickListener(this);
        play = (ImageButton) view.findViewById(R.id.play);
        play.setOnClickListener(this);
        more = (ImageButton) view.findViewById(R.id.card_more);
        more.setOnClickListener(this);

        // configure the item overflow menu
        popupMenu = new PopupMenu(more.getContext(), more);
        popupMenu.getMenuInflater().inflate(R.menu.menu_home_episode_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(EpisodeHolder.this);
        Menu menu = popupMenu.getMenu();
        queueMenu = menu.findItem(R.id.action_add_to_queue);
        completedMenu = menu.findItem(R.id.action_mark_completed);
        favoriteMenu = menu.findItem(R.id.action_favorite);
        downloadMenu = menu.findItem(R.id.action_download);
        deleteDownloadMenu = menu.findItem(R.id.action_delete);
        cancelMenu = menu.findItem(R.id.action_cancel_download);
        deleteEpisode = menu.findItem(R.id.action_unpin);
    }

    @Override
    public void onClick(final View v) {

        switch (v.getId()) {
            case R.id.description:

                if (episodeId != -1) {
                    PlayableActivity.showEpisodeInformation(v.getContext(),
                            EpisodeModel.getEpisodeById(v.getContext(), episodeId));
                }
                break;
            case R.id.play:
                int tag = (int) play.getTag();
                String action = tag == MODE_WILL_PLAY ?
                    PodcastPlayerService.ACTION_PLAY_EPISODE : PodcastPlayerService.ACTION_PAUSE;

                if (mPlayClickListener != null) {
                    mPlayClickListener.onPlayClick(episodeId, action);
                } else {
                    PodcastPlayerService.sendIntent(v.getContext(), action, episodeId);
                }

                if (!isChannelSubscribed) {
                    EpisodeModel.manuallyAddEpisode(itemView.getContext(), episodeId);
                }
                break;
            case R.id.card_more:
                popupMenu.show();
                break;
            case R.id.channel_art:
            case R.id.published_data:
                if (channelServerId != null) {
                    Channel channel = ChannelModel.getChannelByGeneratedId(publishedData.getContext(),
                            channelServerId);

                    if (channel != null) {
                        ChannelProfileActivity.openChannelProfile(
                                (BaseActivity) publishedData.getContext(),
                                channel, channelArt, false);
                    }
                }
                break;
            case R.id.pin:

                if (mPinClickListener != null) {
                    mPinClickListener.onPinClick(getAdapterPosition());
                }
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_delete:
                DeleteEpisodeService.deleteEpisode(itemView.getContext(), episodeId);
                return true;
            case R.id.action_download:
                DownloadService.downloadEpisode(itemView.getContext(), episodeId);

                if (!isChannelSubscribed) {
                    EpisodeModel.manuallyAddEpisode(itemView.getContext(), episodeId);
                }
                return true;
            case R.id.action_cancel_download:
                DownloadService.cancelEpisode(itemView.getContext(), episodeId);
                return true;
            case R.id.action_share:
                IntentHelper.shareEpisode(itemView.getContext(),
                        EpisodeModel.getEpisodeById(itemView.getContext(), episodeId));
                return true;
            case R.id.action_favorite:
                EpisodeModel.toggleFavoriteAsync(itemView.getContext(), episodeId, null);
                return true;
            case R.id.action_add_to_queue:
                PlaylistModel.addEpisodeToPlaylist(itemView.getContext(),
                        EpisodeModel.getEpisodeById(itemView.getContext(), episodeId).getGeneratedId());

                if (!isChannelSubscribed) {
                    EpisodeModel.manuallyAddEpisode(itemView.getContext(), episodeId);
                }
                return true;
            case R.id.action_mark_completed:
                EpisodeModel.markEpisodeCompleted(itemView.getContext(), episodeId,
                        downloadStatusId == DownloadStatus.DOWNLOADED);
                return true;
            case R.id.action_unpin:
                EpisodeModel.unpinEpisode(itemView.getContext(), episodeId);
                return true;
            default:
                return false;
        }
    }

    public interface PlayClickListener {
        void onPlayClick(int episodeId, String action);
    }

    public interface PinClickListener {
        void onPinClick(int index);
    }
}