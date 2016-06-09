/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */
package com.mainmethod.premofm.task;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.mainmethod.premofm.PremoApp;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.NotificationHelper;
import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.http.HttpHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.service.DeleteEpisodeService;
import com.mainmethod.premofm.util.DownloadQueue;
import com.mainmethod.premofm.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * AsyncTask that downloads episodes and stores them on the device
 * Created by evan on 1/22/15.
 */
public class DownloadEpisodesTask extends AsyncTask<Void, Integer, Boolean> {
    private final static String TAG = DownloadEpisodesTask.class.getSimpleName();
    private final static int MODE_STARTING_DOWNLOAD = 0;
    private final static int MODE_DOWNLOADING = 1;
    private final static int BUFFER_SIZE = 8_192;
    private final static int MAX_REDIRECTS = 8;

    private static final int DOWNLOAD_ICON_LEVEL_MIN = 0;
    private static final int DOWNLOAD_ICON_LEVEL_MAX = 5;

    private final Context mContext;
    private final OnDownloadEpisodesTaskListener mListener;
    private final DownloadQueue mDownloadQueue;

    private Episode mCurrentEpisode;
    private boolean mCancelCurrentEpisode;
    private int mDownloadLevel;
    private Bundle mCachedBundle;

    public DownloadEpisodesTask(Context context, OnDownloadEpisodesTaskListener listener) {
        mContext = context;
        mListener = listener;
        mDownloadQueue = new DownloadQueue(context);
        mContext.getContentResolver().registerContentObserver(
                PremoContract.EpisodeEntry.CONTENT_URI,
                true,
                mDownloadQueue);
        mCachedBundle = new Bundle();
    }

    /**
     * Cancels the episode with the ID in the download queue
     * @param episodeId
     */
    public synchronized void cancelDownload(int episodeId) {

        // cancel the in progress download
        if (mCurrentEpisode != null && mCurrentEpisode.getId() == episodeId) {
            mCancelCurrentEpisode = true;
        }

        // unqueue the episode with episodeId
        else {
            EpisodeModel.markEpisodeDeleted(mContext, episodeId);
        }
    }

    private synchronized boolean isCurrentDownloadCancelled() {
        return mCancelCurrentEpisode;
    }

    private synchronized void resetCurrentDownloadCancelled() {
        mCancelCurrentEpisode = false;
    }

    @Override
    protected Boolean doInBackground(Void... p) {
        List<Integer> episodeIds = getRequestedEpisodes();

        if (episodeIds.size() == 0) {
            episodeIds = getEpisodesToDownload();
        }

        if (episodeIds != null && episodeIds.size() > 0) {
            EpisodeModel.markEpisodesAsQueued(mContext, episodeIds);
        }

        try {
            downloadEpisodes();
        } catch (Exception e) {
            Log.e(TAG, "Error occurred in DownloadEpisodesTask: ", e);
        }
        mContext.getContentResolver().unregisterContentObserver(mDownloadQueue);
        return null;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        // remove the notification
        NotificationHelper.dismissNotification(mContext,
                NotificationHelper.NOTIFICATION_ID_DOWNLOADING);
        mListener.onFinish();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int mode = values[0];

        switch (mode) {
            case MODE_STARTING_DOWNLOAD:
                NotificationHelper.showDownloadStartingNotification(mContext, mCurrentEpisode,
                        mDownloadQueue.size(), DOWNLOAD_ICON_LEVEL_MIN);
                break;
            case MODE_DOWNLOADING:
                int downloaded = values[1];
                int totalSize = values[2];
                adjustDownloadIconLevel();
                NotificationHelper.showDownloadStartedNotification(mContext, mCurrentEpisode,
                        mDownloadQueue.size(), mDownloadLevel, downloaded, totalSize);
                break;
        }
    }

    private void adjustDownloadIconLevel() {

        if (mDownloadLevel >= DOWNLOAD_ICON_LEVEL_MAX) {
            mDownloadLevel = DOWNLOAD_ICON_LEVEL_MIN;
        } else {
            mDownloadLevel++;
        }
    }

    private List<Integer> getRequestedEpisodes() {
        List<Integer> episodeIds = new ArrayList<>();
        List<Episode> episodes = EpisodeModel.getRequestedEpisodes(mContext);

        for (int i = 0; i < episodes.size(); i++) {
            episodeIds.add(episodes.get(i).getId());
        }
        return episodeIds;
    }

    /**
     * Returns a list of episode IDs that are eligible to download
     * @return
     */
    private List<Integer> getEpisodesToDownload() {
        List<Channel> channels = ChannelModel.getChannels(mContext);
        List<Integer> episodeIds = new ArrayList<>();

        int numEpisodesToCache = UserPrefHelper.get(mContext).getStringAsInt(
                R.string.pref_key_episode_cache_limit);

        // get episodes to cache from preferences
        String serverIdStr = UserPrefHelper.get(mContext)
                .getString(R.string.pref_key_auto_download_channels);

        if (serverIdStr == null || serverIdStr.length() == 0) {
            return episodeIds;
        }
        String[] serverIdArr = TextUtils.split(serverIdStr, ",");
        HashSet<String> serverIdSet = new HashSet<>(Arrays.asList(serverIdArr));

        // loop over each channel
        for (int i = 0; i < channels.size(); i++) {

            if (serverIdSet.contains(channels.get(i).getGeneratedId())) {
                // get all channel's episodes
                List<Episode> episodes =  EpisodeModel.getEpisodesByChannel(mContext,
                        channels.get(i));
                int downloadCount = 0;

                for (int j = 0; j < episodes.size(); j++) {

                    // we only want to download the latest X, not completed, not downloaded, queued episodes of a channel
                    // determine which non downloaded episodes to download
                    // we encounter not downloaded episode and we still under the number to cache
                    if ((episodes.get(j).getDownloadStatus() == DownloadStatus.NOT_DOWNLOADED ||
                            episodes.get(j).getDownloadStatus() == DownloadStatus.QUEUED) &&
                            downloadCount < numEpisodesToCache) {
                        downloadCount++;

                        // only add the episode if it's not completed or downlaoded
                        if (episodes.get(j).getEpisodeStatus() != EpisodeStatus.COMPLETED &&
                                episodes.get(j).getEpisodeStatus() != EpisodeStatus.DELETED) {
                            episodeIds.add(episodes.get(j).getId());
                        }
                    }

                    // we encounter a downloaded episode and still under the number to cache
                    else if (episodes.get(j).getDownloadStatus() == DownloadStatus.DOWNLOADED &&
                            !episodes.get(j).isManualDownload()) {
                        downloadCount++;
                    }

                    if (downloadCount == numEpisodesToCache) {
                        break;
                    }
                }
            }
        }
        return episodeIds;
    }

    /**
     * Downloads a list of episodes
     * @throws Exception
     */
    private void downloadEpisodes() throws Exception {
        Episode episode;
        List<Episode> downloadedEpisodes = new ArrayList<>();
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
        
        while ((episode = mDownloadQueue.next()) != null) {
            Log.d(TAG, "Downloading episode: " + episode);
            lock.acquire();

            try {
                // if the episode is not downloaded, download it
                mCurrentEpisode = episode;
                publishProgress(MODE_STARTING_DOWNLOAD, mDownloadQueue.size());
                String localMediaUrl = downloadEpisode();

                // finish downloading and mark it as downloaded, and save it's local location
                if (localMediaUrl != null) {
                    Log.d(TAG, "Local media URI: " + localMediaUrl);
                    downloadedEpisodes.add(episode);
                } else {
                    Log.w(TAG, "No local media url returned, error encountered");
                }

            } catch (DownloadInterruptedException e) {
                Log.d(TAG, "Download interrupted", e);

                switch (e.getInterruptionType()) {
                    case CANCEL_ALL:
                        NotificationHelper.dismissNotification(mContext,
                                NotificationHelper.NOTIFICATION_ID_DOWNLOADING);
                        EpisodeModel.markQueuedEpisodeNotDownloaded(mContext);
                        throw e;
                    case CANCEL_EPISODE:
                        resetCurrentDownloadCancelled();
                        DeleteEpisodeService.deleteEpisode(mContext, mCurrentEpisode.getId());
                        mCurrentEpisode = null;
                        break;
                    case INSUFFICIENT_SPACE:
                        NotificationHelper.dismissNotification(mContext,
                                NotificationHelper.NOTIFICATION_ID_DOWNLOADING);
                        EpisodeModel.markQueuedEpisodeNotDownloaded(mContext);
                        NotificationHelper.showErrorNotification(mContext,
                                R.string.notification_title_insufficient_space,
                                R.string.notification_content_insufficient_space);
                        throw e;
                    case CONNECTION_ERROR:
                        NotificationHelper.dismissNotification(mContext,
                                NotificationHelper.NOTIFICATION_ID_DOWNLOADING);
                        EpisodeModel.markQueuedEpisodeNotDownloaded(mContext);
                        NotificationHelper.showErrorNotification(mContext,
                                mContext.getString(R.string.notification_title_connection_error),
                                String.format(mContext.getString(
                                        R.string.notification_content_connection_error),
                                        mCurrentEpisode.getTitle()));
                        break;
                    default:
                       throw e;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in DownloadEpisodeTask", e);
                // an error occurred, mark the episode as not downloaded
                try {
                    DeleteEpisodeService.deleteEpisode(mContext, mCurrentEpisode.getId());
                    updateEpisodeDownloadStatus(DownloadStatus.NOT_DOWNLOADED, -1, null);
                } catch (Exception e1) {
                    throw e1;
                }
            } finally {

                if (lock.isHeld()) {
                    lock.release();
                }
            }
        }

        if (downloadedEpisodes.size() > 0) {

            if (UserPrefHelper.get(mContext).getBoolean(
                    R.string.pref_key_enable_notifications)) {
                // add new episodes to the episode server id set
                Set<String> episodeServerIds = new TreeSet<>();

                for (int i = 0; i < downloadedEpisodes.size(); i++) {
                    episodeServerIds.add(downloadedEpisodes.get(i).getGeneratedId());
                }
                // add them to the preferences
                AppPrefHelper.getInstance(mContext).addToStringSet(
                        AppPrefHelper.PROPERTY_DOWNLOAD_NOTIFICATIONS, episodeServerIds);
                // show the notification
                NotificationHelper.showEpisodesDownloadedNotification(mContext);
            }
        }
    }

    /**
     * Helper function for updating an episodes download status
     * @param status
     * @param localMediaUrl
     * @throws Exception
     */
    private void updateEpisodeDownloadStatus(int status,
                                            int totalBytesDownloaded,
                                            String localMediaUrl) throws Exception {
        switch (status) {
            case DownloadStatus.NOT_DOWNLOADED:
                mCurrentEpisode.setLocalMediaUrl(null);
                mCurrentEpisode.setDownloadedSize(0);
                mCurrentEpisode.setDownloadStatus(status);
                break;
            case DownloadStatus.DOWNLOADING:
                mCurrentEpisode.setLocalMediaUrl(localMediaUrl);
                mCurrentEpisode.setDownloadedSize(totalBytesDownloaded);
                mCurrentEpisode.setDownloadStatus(status);
                break;
            case DownloadStatus.DOWNLOADED:
                mCurrentEpisode.setDownloadedSize(totalBytesDownloaded);
                mCurrentEpisode.setLocalMediaUrl(localMediaUrl);
                mCurrentEpisode.setDownloadStatus(status);
                break;
            case DownloadStatus.PARTIALLY_DOWNLOADED:
                mCurrentEpisode.setDownloadedSize(totalBytesDownloaded);
                mCurrentEpisode.setDownloadStatus(status);
                break;
        }
        updateEpisode();
    }

    private void updateEpisode() {
        mCachedBundle.clear();
        mCachedBundle.putInt(EpisodeModel.PARAM_SIZE, mCurrentEpisode.getSize());
        mCachedBundle.putString(EpisodeModel.PARAM_LOCAL_MEDIA_URL, mCurrentEpisode.getLocalMediaUrl());
        mCachedBundle.putInt(EpisodeModel.PARAM_DOWNLOADED_SIZE, mCurrentEpisode.getDownloadedSize());
        mCachedBundle.putInt(EpisodeModel.PARAM_DOWNLOAD_STATUS, mCurrentEpisode.getDownloadStatus());

        EpisodeModel.updateEpisodeAsync(mContext, mCurrentEpisode.getId(), mCachedBundle,
                () -> {

                    if (mCachedBundle.getInt(EpisodeModel.PARAM_DOWNLOAD_STATUS) ==
                            DownloadStatus.DOWNLOADED) {
                        BroadcastHelper.broadcastEpisodeDownloaded(mContext,
                                mCurrentEpisode.getId());
                    }
                });
    }

    private String buildFilename(Episode episode) {
        // build the filename
        String filename = episode.getTitle().hashCode() + "-" + episode.getGeneratedId();
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                episode.getMimeType());

        if (extension == null || extension.length() == 0) {
            extension = MimeTypeMap.getFileExtensionFromUrl(episode.getRemoteMediaUrl());
        }

        if (extension == null) {
            throw new RuntimeException("Unable to generate the extension for episode: " +
                    episode.toString());
        }
        return IOUtil.PATH_PODCAST_DIRECTORY + "/" + filename + "." + extension;
    }

    private HttpURLConnection getConnection() throws IOException {
        boolean isRedirecting;
        int numRedirects = 0;
        String url = mCurrentEpisode.getRemoteMediaUrl();
        String userAgent = String.format(mContext.getString(R.string.user_agent),
                PremoApp.getVersionName());
        HttpURLConnection connection;

        do {
            connection = HttpHelper.getConnection(url);
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setRequestProperty("Accept-Encoding", "gzip,deflate");

            // if the episode has started downloading before, let's reattempt downloading
            if (mCurrentEpisode.getDownloadedSize() > 0) {
                Log.d(TAG, "Attempting to resume download from: " + mCurrentEpisode.getDownloadedSize());
                connection.setRequestProperty("Range", "bytes=" + mCurrentEpisode.getDownloadedSize() + "-");
            }
            int responseCode = connection.getResponseCode();

            switch (responseCode) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    HttpURLConnection oldConnection = connection;
                    url = connection.getHeaderField("Location");
                    Log.d(TAG, "Redirecting to : " + url);
                    isRedirecting = true;
                    numRedirects++;
                    ResourceHelper.closeResource(oldConnection);
                    continue;
                case HttpURLConnection.HTTP_PARTIAL:
                case HttpURLConnection.HTTP_OK:
                    isRedirecting = false;
                    break;
                default:
                    connection = null;
                    isRedirecting = false;
                    break;
            }
        } while (isRedirecting && numRedirects < MAX_REDIRECTS);

        return connection;
    }

    /**
     * Downloads a podcast episode media file to the local file system
     */
    private String downloadEpisode() {
        int totalBytesRead = 0;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        RandomAccessFile output = null;
        boolean revertEpisodeValues = false;

        // previous values
        int oldDownloadStatus = mCurrentEpisode.getDownloadStatus();

        if (oldDownloadStatus == DownloadStatus.QUEUED || oldDownloadStatus == DownloadStatus.REQUESTED) {
            oldDownloadStatus = DownloadStatus.NOT_DOWNLOADED;
        }

        String oldLocalMediaUrl = mCurrentEpisode.getLocalMediaUrl();
        int oldDownloadedSize = mCurrentEpisode.getDownloadedSize();

        IOUtil.createDirectory(mContext, IOUtil.PATH_PODCAST_DIRECTORY);
        File file = new File(mContext.getExternalFilesDir(null),  buildFilename(mCurrentEpisode));
        URI fileUri = file.toURI();
        String filename = fileUri.toString();
        Log.d(TAG, "Proposed file and url: " + fileUri);

        try {
            // open the file output stream for file writing
            output = new RandomAccessFile(file, "rw");
            connection = getConnection();

            if (connection == null) {
                revertEpisodeValues = true;
                throw new DownloadInterruptedException(DownloadInterruptedException.InterruptionType.CONNECTION_ERROR,
                        String.format("Unable to download episode %s", mCurrentEpisode.getTitle()));
            }
            Log.d(TAG, "Response code: " + connection.getResponseCode());
            Log.d(TAG, "Content-Length: " + connection.getContentLength());
            Log.d(TAG, "Content-Encoding: " + connection.getContentEncoding());

            // check for free space on the device
            if (!IOUtil.spaceAvailable(mContext, connection.getContentLength())) {
                throw new DownloadInterruptedException(
                        DownloadInterruptedException.InterruptionType.INSUFFICIENT_SPACE,
                        String.format("Insufficient space storing %d bytes",
                                connection.getContentLength()));
            }
            inputStream = HttpHelper.getInputStream(connection);

            int contentLength = connection.getContentLength();

            // we couldn't get content length from the connection, use the size from the episode
            if (contentLength == -1) {
                contentLength = mCurrentEpisode.getSize();
            }

            // content length has a value, update the episode size
            else if (mCurrentEpisode.getSize() == 0) {
                mCurrentEpisode.setSize(contentLength);
            }

            float percent = contentLength * 0.05f;
            int fractionBytesRead = 0;
            int bytesRead;
            byte[] data = new byte[BUFFER_SIZE];

            String connectionField = connection.getHeaderField("content-range");

            if (connectionField != null) {
                String[] connectionRanges = connectionField.substring("bytes=".length()).split("-");
                totalBytesRead = Integer.parseInt(connectionRanges[0]);
                contentLength = mCurrentEpisode.getSize();
                Log.d(TAG, "Resuming download from: " + totalBytesRead);
            }

            if (connectionField == null && mCurrentEpisode.getDownloadedSize() > 0) {
                Log.d(TAG, "Deleting existing file because we can't resume this download");
                IOUtil.deleteFiles(fileUri.toString());
            }
            output.seek(totalBytesRead);
            updateEpisodeDownloadStatus(DownloadStatus.DOWNLOADING, totalBytesRead, filename);

            // get the data from the input stream and write it to the output stream (file)
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                output.write(data, 0, bytesRead);
                fractionBytesRead += bytesRead;
                totalBytesRead += bytesRead;

                if (isCancelled()) {
                    // task was cancelled, save the progress
                    updateEpisodeDownloadStatus(DownloadStatus.PARTIALLY_DOWNLOADED,
                            totalBytesRead, null);
                    throw new DownloadInterruptedException(
                            DownloadInterruptedException.InterruptionType.CANCEL_ALL,
                            "Task was cancelled");
                }

                if (isCurrentDownloadCancelled()) {
                    // requested to cancel the current download
                    throw new DownloadInterruptedException(
                            DownloadInterruptedException.InterruptionType.CANCEL_EPISODE,
                            "Current episode cancelled");
                }

                // update the database and UI every one percent
                if (fractionBytesRead >= percent) {
                    fractionBytesRead = 0;
                    updateEpisodeDownloadStatus(DownloadStatus.DOWNLOADING, totalBytesRead, filename);
                    publishProgress(MODE_DOWNLOADING, totalBytesRead, contentLength);
                }
            }
            // done, mark episode as downloaded
            updateEpisodeDownloadStatus(DownloadStatus.DOWNLOADED, totalBytesRead, filename);
        } catch (DownloadInterruptedException e) {
            Log.d(TAG, "Download interrupted", e);
            revertEpisodeValues = true;
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Error in downloadEpisode", e);
            revertEpisodeValues = true;
            throw new DownloadInterruptedException(
                    DownloadInterruptedException.InterruptionType.CONNECTION_ERROR,
                    "Error connecting to download url");
        } finally {
            ResourceHelper.closeResources(new Object[]{ connection, inputStream, output });

            if (revertEpisodeValues) {
                mCurrentEpisode.setDownloadStatus(oldDownloadStatus);
                mCurrentEpisode.setDownloadedSize(oldDownloadedSize);
                mCurrentEpisode.setLocalMediaUrl(oldLocalMediaUrl);
                filename = null;
                updateEpisode();
            }
        }
        return filename;
    }

    public interface OnDownloadEpisodesTaskListener {
        void onFinish();
    }

    private static class DownloadInterruptedException extends RuntimeException {
        enum InterruptionType {
            CANCEL_ALL,
            CANCEL_EPISODE,
            INSUFFICIENT_SPACE,
            CONNECTION_ERROR
        }

        private InterruptionType mInterruptionType;

        private DownloadInterruptedException(InterruptionType type, String detailMessage) {
            super(detailMessage);
            mInterruptionType = type;
        }

        public InterruptionType getInterruptionType() {
            return mInterruptionType;
        }
    }
}