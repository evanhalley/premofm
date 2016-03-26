/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */
package com.mainmethod.premofm.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.mainmethod.premofm.BuildConfig;
import com.mainmethod.premofm.R;
import com.mainmethod.premofm.api.ApiException;
import com.mainmethod.premofm.api.ApiHelper;
import com.mainmethod.premofm.api.ApiManager;
import com.mainmethod.premofm.api.PostApiResponse;
import com.mainmethod.premofm.data.PremoContract;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.data.model.CollectionModel;
import com.mainmethod.premofm.data.model.EpisodeModel;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.DatetimeHelper;
import com.mainmethod.premofm.helper.NotificationHelper;
import com.mainmethod.premofm.helper.PackageHelper;
import com.mainmethod.premofm.helper.ResourceHelper;
import com.mainmethod.premofm.helper.UserPrefHelper;
import com.mainmethod.premofm.helper.billing.Purchase;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.object.Collection;
import com.mainmethod.premofm.object.Credential;
import com.mainmethod.premofm.object.DownloadStatus;
import com.mainmethod.premofm.object.Episode;
import com.mainmethod.premofm.object.EpisodeStatus;
import com.mainmethod.premofm.object.SyncStatus;
import com.mainmethod.premofm.object.User;
import com.mainmethod.premofm.service.job.DownloadJobService;
import com.mainmethod.premofm.ui.dialog.ReauthUserDialogFragment;
import com.mainmethod.premofm.util.IOUtil;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The service that runs when a user signs in/up
 * 1) Get and store the user profile
 * 2) Get and store the subscribed channels
 * 3) Get and store the episodes on the timeline
 * Created by evan on 1/30/15.
 */
public class ApiService extends IntentService {

    private static final String TAG = ApiService.class.getSimpleName();

    public static final String ACTION_SIGN_IN                   = "com.mainmethod.premofm.sync.signIn";
    public static final String ACTION_SIGN_UP                   = "com.mainmethod.premofm.sync.signUp";
    public static final String ACTION_SYNC_CHANNELS             = "com.mainmethod.premofm.sync.channels";
    public static final String ACTION_SYNC_EPISODE_CHANGES      = "com.mainmethod.premofm.sync.episodeChanges";
    public static final String ACTION_REGISTER_GCM              = "com.mainmethod.premofm.sync.registerGcm";
    public static final String ACTION_PUSH_LOCAL_CHANGES        = "com.mainmethod.premofm.sync.pushLocalChanges";
    public static final String ACTION_SUBSCRIBE_CHANNEL         = "com.mainmethod.premofm.sync.subscribeChannel";
    public static final String ACTION_UNSUBSCRIBE_CHANNEL       = "com.mainmethod.premofm.sync.unsubscribeChannel";
    public static final String ACTION_CHANGE_EMAIL              = "com.mainmethod.premofm.sync.changeEmail";
    public static final String ACTION_CHANGE_PASSWORD           = "com.mainmethod.premofm.sync.changePassword";
    public static final String ACTION_CHANGE_NICKNAME           = "com.mainmethod.premofm.sync.changeNickname";
    public static final String ACTION_SYNC_USER_PROFILE         = "com.mainmethod.premofm.sync.userProfile";
    public static final String ACTION_REAUTHENTICATE            = "com.mainmethod.premofm.sync.reauthenticate";
    public static final String ACTION_PUSH_COLLECTION_CHANGES   = "com.mainmethod.premofm.sync.pushCollectionChanges";
    public static final String ACTION_GET_COLLECTIONS           = "com.mainmethod.premofm.sync.getCollectionChanges";
    public static final String ACTION_GET_CHANNEL_EPISODES_OLDER_THAN = "com.mainmethod.premofm.sync.getChannelEpisodesOlderThan";
    public static final String ACTION_GET_CHANNEL_EPISODES_NEWER_THAN = "com.mainmethod.premofm.sync.getChannelEpisodesNewerThan";
    public static final String ACTION_ADD_PURCHASE              = "com.mainmethod.premofm.sync.addPurchase";
    public static final String ACTION_SETUP_ACCOUNT             = "com.mainmethod.premofm.sync.setupAccount";

    public static final String PARAM_FROM_JOB_SERVICE       = "fromJobService";
    public static final String PARAM_CHANNEL                = "channel";
    public static final String PARAM_CHANNEL_SERVER_ID      = "channelServerId";
    public static final String PARAM_TIMESTAMP              = "timestamp";
    public static final String PARAM_EMAIL                  = "email";
    public static final String PARAM_OLD_PASSWORD           = "oldPassword";
    public static final String PARAM_NEW_PASSWORD           = "newPassword";
    public static final String PARAM_NICKNAME               = "nickname";
    public static final String PARAM_PASSWORD               = "password";
    public static final String PARAM_PURCHASE               = "purchase";
    public static final String PARAM_IS_TEMP_ACCOUNT        = "isTempAccount";

    public ApiService() {
        super(TAG);
    }

    /**
     * Convenience function for starting API service actions related to an apps
     * first sign in
     * @param context
     */
    public static void initiateFirstSignInProcess(Context context) {
        ApiService.start(context, ApiService.ACTION_SYNC_CHANNELS);
        ApiService.start(context, ApiService.ACTION_SYNC_EPISODE_CHANGES);
        ApiService.start(context, ApiService.ACTION_GET_COLLECTIONS);
    }

    /**
     * Helper function created for easily starting the sync service
     * @param context
     * @param action
     */
    public static void start(Context context, String action) {
        Log.d(TAG, "Starting the API service: " + action);
        Intent intent = new Intent(context, ApiService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    /**
     * Helper function created for easily starting the sync service
     * @param context
     * @param action
     */
    public static void start(Context context, String action, Bundle args) {
        Log.d(TAG, "Starting the API service: " + action);
        Intent intent = new Intent(context, ApiService.class);
        intent.setAction(action);
        intent.putExtras(args);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "ApiService called with action: " + action);

            switch (action) {
                case ACTION_SIGN_IN:
                    signIn(intent.getStringExtra(PARAM_EMAIL),
                            intent.getStringExtra(PARAM_PASSWORD));
                    break;
                case ACTION_SIGN_UP:
                    signUp(intent.getStringExtra(PARAM_EMAIL),
                            intent.getStringExtra(PARAM_PASSWORD),
                            intent.getBooleanExtra(PARAM_IS_TEMP_ACCOUNT, false));
                    break;
                case ACTION_SYNC_CHANNELS:
                    syncChannels();
                    break;
                case ACTION_SYNC_EPISODE_CHANGES:
                    pushLocalEpisodeChanges();
                    boolean isSuccessful = pullServerEpisodeChanges();
                    BroadcastHelper.broadcastEpisodeSyncFinished(this, isSuccessful);
                    break;
                case ACTION_REGISTER_GCM:
                    registerGcmRegistrationId();
                    break;
                case ACTION_PUSH_LOCAL_CHANGES:
                    pushLocalEpisodeChanges();
                    break;
                case ACTION_SUBSCRIBE_CHANNEL:
                    subscribeToChannel(intent.getStringExtra(PARAM_CHANNEL_SERVER_ID));
                    break;
                case ACTION_UNSUBSCRIBE_CHANNEL:
                    unsubscribeFromChannel(intent.getStringExtra(PARAM_CHANNEL_SERVER_ID));
                    break;
                case ACTION_CHANGE_EMAIL:
                    boolean emailChangeResult = changeEmail(intent.getStringExtra(PARAM_EMAIL));
                    BroadcastHelper.broadcastAccountChange(this, emailChangeResult);
                    break;
                case ACTION_CHANGE_NICKNAME:
                    boolean nicknameChangeResult = changeNickname(intent.getStringExtra(PARAM_NICKNAME));
                    BroadcastHelper.broadcastAccountChange(this, nicknameChangeResult);
                    break;
                case ACTION_CHANGE_PASSWORD:
                    boolean passwordChangeResult = changePassword(intent.getStringExtra(PARAM_OLD_PASSWORD),
                            intent.getStringExtra(PARAM_NEW_PASSWORD));
                    BroadcastHelper.broadcastAccountChange(this, passwordChangeResult);
                    break;
                case ACTION_SETUP_ACCOUNT:
                    String email = intent.getStringExtra(PARAM_EMAIL);
                    String password = intent.getStringExtra(PARAM_PASSWORD);
                    String oldPassword = intent.getStringExtra(PARAM_OLD_PASSWORD);

                    boolean setupAccountResult = changeEmail(email);

                    if (setupAccountResult) {
                       setupAccountResult = changePassword(oldPassword, password);

                        if (setupAccountResult) {
                            User user = User.load(this);
                            user.setIsTempUser(false);
                            User.save(this, user);
                        }
                    }
                    BroadcastHelper.broadcastAccountChange(this, setupAccountResult);
                    break;
                case ACTION_SYNC_USER_PROFILE:
                    syncUserProfile();
                    break;
                case ACTION_REAUTHENTICATE:
                    reauthenticate(intent.getStringExtra(PARAM_PASSWORD));
                    break;
                case ACTION_PUSH_COLLECTION_CHANGES:
                    boolean isPushedSuccessful = pushCollectionChanges();
                    BroadcastHelper.broadcastCollectionPushFinished(this, isPushedSuccessful);
                    break;
                case ACTION_GET_COLLECTIONS:
                    getCollections();
                    break;
                case ACTION_GET_CHANNEL_EPISODES_OLDER_THAN:
                    getChannelEpisodesOlderThan(Parcels.unwrap(intent.getParcelableExtra(PARAM_CHANNEL)),
                            intent.getLongExtra(PARAM_TIMESTAMP, -1));
                    break;
                case ACTION_GET_CHANNEL_EPISODES_NEWER_THAN:
                    getChannelEpisodesNewerThan(Parcels.unwrap(intent.getParcelableExtra(PARAM_CHANNEL)),
                            intent.getLongExtra(PARAM_TIMESTAMP, -1));
                    break;
                case ACTION_ADD_PURCHASE:
                    addPurchases(Parcels.unwrap(intent.getParcelableExtra(PARAM_PURCHASE)));
                    break;
                default:
                    Log.w(TAG, "Unknown action encountered: " + action);
            }
        }
    }

    private void signIn(String email, String password) {
        String error = null;
        boolean succeeded = false;

        try {
            ApiManager apiManager = ApiManager.getInstance(this);
            Credential credential = apiManager.authenticateUser(email, password);

            if (credential != null) {
                User user = apiManager.getUserProfile(credential);

                if (user != null) {
                    user.setCredential(credential);
                    User.save(this, user);
                    succeeded = true;
                } else {
                    error = getString(R.string.error_api_cannot_retrieve_profile);
                }
            } else {
                error = getString(R.string.error_incorrect_password);
            }
        } catch (ApiException e) {
            Log.e(TAG, "Error logging in", e);
            error = getString(R.string.error_api_unavailable);
        }
        BroadcastHelper.broadcastAuthenticationResult(this, succeeded, error);
    }

    private void signUp(String email, String password, boolean isTempAccount) {
        String error = null;
        boolean succeeded = false;

        try {
            ApiManager apiManager = ApiManager.getInstance(this);
            PostApiResponse response = apiManager.createUser(email, password);

            if(response != null) {

                if(response.isSuccessful()) {

                    // authenticate the user and get the auth token
                    Credential credential = apiManager.authenticateUser(email, password);

                    if (credential != null) {
                        User user = apiManager.getUserProfile(credential);

                        if (user != null) {
                            user.setCredential(credential);
                            user.setIsTempUser(isTempAccount);
                            User.save(this, user);
                            succeeded = true;
                        } else {
                            error = getString(R.string.error_api_cannot_retrieve_profile);
                        }
                    } else {
                        error = getString(R.string.error_api_cannot_retrieve_profile);
                    }
                } else {
                    error = response.getMessage();
                }
            } else {
                error = getString(R.string.error_api_no_response);
            }
        } catch (ApiException e) {
            Log.e(TAG, "Error creating user", e);
            error = getString(R.string.error_api_unavailable);
        }
        BroadcastHelper.broadcastAuthenticationResult(this, succeeded, error);
    }

    private void getCollections() {
        User user = User.load(this);

        try {
            // get collections from the server
            List<Collection> collections = ApiManager.getInstance(this)
                    .getCollections(user.getCredential());

            if (collections == null || collections.size() == 0) {
                return;
            }
            // get the collections on the device
            Map<String, Collection> syncedCollections = CollectionModel.getServerCollectionsMap(this);

            for (int i = 0; i < collections.size(); i++) {
                Collection collection = collections.get(i);

                // if this is an existing collection, transfer the ID so it runs as an update
                if (syncedCollections.containsKey(collection.getServerId())) {
                    collection.setId(syncedCollections.get(collection.getServerId()).getId());
                }

                // save the collection
                CollectionModel.saveCollection(this, collection, false);
            }
        } catch (ApiException e) {
            Log.w(TAG, "Error occurred in getCollections", e);
        }
    }

    private boolean pushCollectionChanges() {
        boolean isSuccessful = true;
        List<Collection> pendingCollections = CollectionModel.getPendingCollections(this);
        ApiManager apiManager = ApiManager.getInstance(this);
        User user = User.load(this);

        if (pendingCollections == null) {
            return isSuccessful;
        }
        List<Collection> collectionsToCreate = new ArrayList<>();
        List<Collection> collectionsToUpdate = new ArrayList<>();
        List<Collection> collectionsToDelete = new ArrayList<>();

        for (int i = 0; i< pendingCollections.size(); i++) {

            switch (pendingCollections.get(i).getSyncStatus()) {
                case SyncStatus.PENDING_CREATE:
                    collectionsToCreate.add(pendingCollections.get(i));
                    break;
                case SyncStatus.PENDING_UPDATE:
                    collectionsToUpdate.add(pendingCollections.get(i));
                    break;
                case SyncStatus.PENDING_DELETE:
                    String serverId = pendingCollections.get(i).getServerId();

                    if (!TextUtils.isEmpty(serverId)) {
                        collectionsToDelete.add(pendingCollections.get(i));
                    } else {
                        CollectionModel.deleteCollection(this, pendingCollections.get(i).getId());
                    }
                    break;
            }
        }

        try {
            for (int i = 0; i < collectionsToCreate.size(); i++) {
                PostApiResponse response = apiManager.upsertCollection(
                        user.getCredential(), collectionsToCreate.get(i), false);

                if (response != null && response.isSuccessful()) {
                    String id = response.getString(ApiManager.ID);
                    collectionsToCreate.get(i).setServerId(id);
                    CollectionModel.saveCollection(this, collectionsToCreate.get(i), false);
                }
            }

            for (int i = 0; i < collectionsToUpdate.size(); i++) {
                PostApiResponse response = apiManager.upsertCollection(
                        user.getCredential(), collectionsToUpdate.get(i), true);

                if (response != null && response.isSuccessful()) {
                    CollectionModel.saveCollection(this, collectionsToUpdate.get(i), false);
                }
            }

            for (int i = 0; i < collectionsToDelete.size(); i++) {
                PostApiResponse response = apiManager.deleteCollection(
                        user.getCredential(), collectionsToDelete.get(i).getServerId());

                if (response != null && response.isSuccessful()) {
                    CollectionModel.deleteCollection(this, collectionsToDelete.get(i).getId());
                }
            }
        } catch (ApiException e) {
            Log.w(TAG, "Error occurred in pushCollectionChanges", e);
            isSuccessful = false;
        }
        return isSuccessful;
    }

    /**
     * Starts the process of reauthenticating a user and persisting the new credentials
     * @param password
     */
    private void reauthenticate(String password) {
        User user = User.load(this);

        try {
            Credential credential = ApiManager.getInstance(this).authenticateUser(user.getEmail(),
                    password);

            if (credential != null) {
                user.setCredential(credential);
                User.save(this, user);
                BroadcastHelper.broadcastReauthentication(this, true);
            } else {
                BroadcastHelper.broadcastReauthentication(this, false);
                ReauthUserDialogFragment.showReauthNotification(this);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error occurred in reauthenticate", e);
        }
    }

    /**
     * Retrieves and stores the late user profile from the server
     */
    private void syncUserProfile() {
        User user = User.load(this);

        try {
            User newUser = ApiManager.getInstance(this).getUserProfile(user.getCredential());

            if (newUser != null) {
                newUser.setCredential(user.getCredential());
                User.save(this, user);
                BroadcastHelper.broadcastAccountChange(this, true);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error occurred in syncUserProfile", e);
        }
    }

    /**
     * Initiates the email address change of the logged in user
     * @param email
     */
    private boolean changeEmail(String email) {
        boolean result = false;
        User user = User.load(this);

        try {
            PostApiResponse response = ApiManager.getInstance(this).updateEmail(
                    user.getCredential(), email);

            if (response.isSuccessful()) {
                user.setEmail(email);
                User.save(this, user);
                result = true;
            }
            BroadcastHelper.broadcastAccountChange(this, response.isSuccessful());
        } catch (Exception e) {
            Log.w(TAG, "Error occurred in changeEmail", e);
        }
        return result;
    }

    /**
     * Initiates the nickname change of the logged in user
     * @param nickname
     */
    private boolean changeNickname(String nickname) {
        boolean result = false;
        User user = User.load(this);

        try {
            PostApiResponse response = ApiManager.getInstance(this).updateNickname(
                    user.getCredential(), nickname);

            if (response.isSuccessful()) {
                user.setNickname(nickname);
                User.save(this, user);
                result = true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error occurred in changeNickname", e);
        }
        return result;
    }

    /**
     * Initiates the password change of the logged in user
     * @param oldPassword
     * @param newPassword
     */
    private boolean changePassword(String oldPassword, String newPassword) {
        boolean result = false;
        User user = User.load(this);

        try {
            PostApiResponse response = ApiManager.getInstance(this).updatePassword(
                    user.getCredential(), oldPassword, newPassword);
            result = response.isSuccessful();
        } catch (Exception e) {
            Log.w(TAG, "Error occurred in changePassword", e);
        }
        return result;
    }

    /**
     * Push local episode changes to the server
     */
    private boolean pushLocalEpisodeChanges() {
        boolean isSuccessful = true;
        Log.d(TAG, "Pushing local episode changes");
        List<Episode> episodeList = new ArrayList<>();
        Cursor cursor = null;

        // get time last synced episode progress
        AppPrefHelper prefs = AppPrefHelper.getInstance(this);
        long time = prefs.getLastEpisodeSyncPush();
        String selection;
        String[] args;
        Log.d(TAG, "Last episode sync time: " + time);

        // first time syncing, get any episodes that have progress > 0, status > NEW, favorite = true
        if (time == -1) {
            selection = PremoContract.EpisodeEntry.FAVORITE + " = 1 OR " +
                    PremoContract.EpisodeEntry.PROGRESS + " > 0 OR " +
                    PremoContract.EpisodeEntry.EPISODE_STATUS_ID + " >= ?";
            args = new String[] { String.valueOf(EpisodeStatus.IN_PROGRESS) };
        }

        // we've done this before
        else {
            selection = PremoContract.EpisodeEntry.UPDATED_AT + " >= ?";
            args = new String[]{ String.valueOf(time) };
        }

        // get the episodes from the database that have changed since then
        String[] projection = new String[] {
                PremoContract.EpisodeEntry.CHANNEL_SERVER_ID,
                PremoContract.EpisodeEntry.SERVER_ID,
                PremoContract.EpisodeEntry.PROGRESS,
                PremoContract.EpisodeEntry.EPISODE_STATUS_ID,
                PremoContract.EpisodeEntry.FAVORITE
        };


        try {
            // get episodes to sync from the database
            cursor = getContentResolver().query(PremoContract.EpisodeEntry.CONTENT_URI, projection,
                    selection,
                    args, null);

            if (cursor == null) {
                return isSuccessful;
            }

            while (cursor.moveToNext()) {
                Episode episode = EpisodeModel.toEpisodeForSync(cursor);

                // if an episode is playing, sync to server as played
                if (episode.getEpisodeStatus() == EpisodeStatus.IN_PROGRESS) {
                    episode.setEpisodeStatus(EpisodeStatus.PLAYED);
                }
                episodeList.add(episode);
            }
            Log.d(TAG, "Number of local episodes with changes to push: " + episodeList.size());

            if (episodeList.size() == 0) {
                return isSuccessful;
            }
            User user = User.load(this);
            PostApiResponse response = ApiManager.getInstance(this).syncEpisodes(user.getCredential(), episodeList);

            if (response != null && response.isSuccessful()) {
                Log.d(TAG, "Local changes pushed successfully");
                prefs.setLastEpisodeSyncPush(DatetimeHelper.getTimestamp());
            } else {
                Log.w(TAG, "There was an issue syncing with the API server");
                isSuccessful = false;
            }
        } catch(ApiException e) {
            Log.w(TAG, "There was an issue syncing with the API server: ", e);
            isSuccessful = false;
        } finally {
            ResourceHelper.closeResource(cursor);
        }
        return isSuccessful;
    }

    /**
     * Registers the GCM registration ID with the server
     */
    private void registerGcmRegistrationId() {

        try {
            // register with GCM
            InstanceID instanceID = InstanceID.getInstance(this);
            String registrationId = instanceID.getToken(BuildConfig.GCM_SENDER_ID,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            String deviceId = PackageHelper.getDeviceId(this);

            // send registration ID to API server
            User user = User.load(this);
            ApiManager api = ApiManager.getInstance(this);
            PostApiResponse response = api.registerDevice(user.getCredential(), registrationId, deviceId);

            if (response != null) {

                if (response.isSuccessful()) {
                    // persist the registration ID
                    AppPrefHelper.getInstance(this).setRegistrationId(registrationId);
                } else {
                    // failed registration, lock out mode
                    instanceID.deleteToken(BuildConfig.GCM_SENDER_ID,
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                    Log.w(TAG, getString(R.string.error_api_device_not_registered));
                }
            } else {
                instanceID.deleteToken(BuildConfig.GCM_SENDER_ID,
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                Log.w(TAG, getString(R.string.error_api_no_response));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during GCM Registration");
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Retrieves new and updated episodes from the server
     */
    private boolean pullServerEpisodeChanges() {
        boolean isSuccessful = true;
        Log.d(TAG, "Getting episodes new episodes from the API");
        ApiManager apiManager = ApiManager.getInstance(this);
        User user = User.load(this);

        long timestamp = AppPrefHelper.getInstance(this).getLastEpisodeSyncPull();
        List<Episode> serverEpisodes;

        try {
            // 1) Get episodes from the server
            if (timestamp != -1) {
                serverEpisodes = apiManager.getNewTimelineEpisodes(user.getCredential(), timestamp);
            } else {
                serverEpisodes = apiManager.getEpisodes(user.getCredential());
            }

            // 2) Get episodes already on the device
            List<Episode> localEpisodes = EpisodeModel.getAllEpisodes(this);

            // 3) Determine what's new and what updated
            List<List<Episode>> episodeComparison = EpisodeModel.compareEpisodes(localEpisodes,
                    serverEpisodes);

            // 4) Save changes
            EpisodeModel.storeEpisodes(this, EpisodeModel.UPDATE,
                    episodeComparison.get(EpisodeModel.UPDATE));
            EpisodeModel.storeEpisodes(this, EpisodeModel.ADD,
                    episodeComparison.get(EpisodeModel.ADD));

            // 5) Store last sync time if there were updates or inserts
            if ((episodeComparison.get(EpisodeModel.UPDATE) != null &&
                    episodeComparison.get(EpisodeModel.UPDATE).size() > 0)
                    || (episodeComparison.get(EpisodeModel.ADD) != null &&
                    episodeComparison.get(EpisodeModel.ADD).size() > 0)) {
                AppPrefHelper.getInstance(this).setLastEpisodeSyncPull(DatetimeHelper.getTimestamp());
            }

            // 6) Notify, if there are new episodes and this isn't a get all episodes (timestamp > -1)
            List<Episode> newEpisodes = episodeComparison.get(EpisodeModel.ADD);

            if (newEpisodes != null && newEpisodes.size() > 0 && timestamp > -1) {

                if (UserPrefHelper.get(this).getBoolean(R.string.pref_key_enable_notifications)) {
                    // add new episodes to the episode server id set
                    Set<String> episodeServerIds = new TreeSet<>();
                    String channelsToNotifyServerIds = UserPrefHelper.get(this).getString(
                            R.string.pref_key_notification_channels);

                    for (int i = 0; i < newEpisodes.size(); i++) {

                        if (channelsToNotifyServerIds.contains(newEpisodes.get(i).getChannelServerId())) {
                            episodeServerIds.add(newEpisodes.get(i).getServerId());
                        }
                    }
                    // add them to the preferences
                    AppPrefHelper.getInstance(this).addToStringSet(
                            AppPrefHelper.PROPERTY_EPISODE_NOTIFICATIONS, episodeServerIds);
                    // show the notification
                    NotificationHelper.showNewEpisodeNotification(this);
                }
            }
            DownloadJobService.scheduleEpisodeDownloadNow(this);
        } catch (Exception e) {
            Log.e(TAG, "Error in pullServerEpisodeChanges");
            Log.e(TAG, e.toString());
            isSuccessful = false;
        }
        return isSuccessful;
    }

    /**
     * Syncs channels with the server
     */
    private void syncChannels() {
        Log.d(TAG, "Executing the initial sync of user profile, channel, and episode data");
        ApiManager apiManager = ApiManager.getInstance(this);
        User user = User.load(this);

        try {
            // 1) Get the subscribed channels for this user from the server
            List<Channel> serverChannels = apiManager.getChannelSubscriptions(user.getCredential());

            // 2) Get channels already on the device
            List<Channel> localChannels = ChannelModel.getChannels(this);

            // 3) Determine what's new and what updated
            List<List<Channel>> channelComparison = ChannelModel.compareChannels(localChannels, serverChannels);

            // 4) Save changes
            ChannelModel.storeChannels(this, ChannelModel.UPDATE,
                    channelComparison.get(ChannelModel.UPDATE));
            ChannelModel.storeChannels(this, ChannelModel.ADD,
                    channelComparison.get(ChannelModel.ADD));
            ChannelModel.storeChannels(this, ChannelModel.DELETE,
                    channelComparison.get(ChannelModel.DELETE));

            // 5) Opt channels for notifications that were added
            List<Channel> addedChannels = channelComparison.get(ChannelModel.ADD);
            List<String> serverIds = CollectionModel.getCollectableServerIds(addedChannels);

            for (int i = 0; i < serverIds.size(); i++) {
                UserPrefHelper.get(this).addServerId(R.string.pref_key_notification_channels, serverIds.get(i));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in syncChannels");
            Log.e(TAG, e.toString());
        }
    }

    private void subscribeToChannel(String channelServerId) {
        ApiManager apiManager = ApiManager.getInstance(this);
        User user = User.load(this);

        try {
            PostApiResponse response = apiManager.subscribeToChannel(user.getCredential(),
                    channelServerId);

            if (response == null || !response.isSuccessful()) {
                //return;
            }
        } catch (ApiException e) {
            Log.e(TAG, "Error occurred in subscribing/unsubscribing from channel");
            Log.e(TAG, e.toString());
        }

        try {
            Channel channel = apiManager.getChannel(user.getCredential(), channelServerId);

            // insert the channel
            int channelId = ChannelModel.insertChannel(this, channel);

            UserPrefHelper.get(this).addServerId(R.string.pref_key_notification_channels,
                    channel.getServerId());

            // mark any existing stored channels are subscribed
            EpisodeModel.markEpisodesAsChannelSubscribed(this, channelServerId);

            // retrieve episodes and insert them if they don't exist
            List<Episode> episodes = apiManager.getEpisodes(user.getCredential(),
                    channel.getServerId(), DatetimeHelper.getTimestamp(), false);

            // filter these episodes down to ones that aren't already stored locally
            episodes = EpisodeModel.returnNewEpisodes(this, channel, episodes);

            // add channel metadata to channels
            for (int i = 0; i < episodes.size(); i++) {
                episodes.get(i).setChannelIsSubscribed(true);
                episodes.get(i).setChannelTitle(channel.getTitle());
                episodes.get(i).setChannelAuthor(channel.getAuthor());
                episodes.get(i).setChannelArtworkUrl(channel.getArtworkUrl());
            }

            EpisodeModel.bulksInsertEpisodes(this, episodes);

            // broadcast the subscription change
            BroadcastHelper.broadcastSubscriptionChange(this, true, channelId, channelServerId);
        } catch (ApiException e) {
            Log.e(TAG, "Error retrieving channel from API");
            Log.e(TAG, e.toString());
        }
    }

    private void unsubscribeFromChannel(String channelServerId) {
        // unsubscribe from the channel via the API
        ApiManager apiManager = ApiManager.getInstance(this);
        User user = User.load(this);

        try {
            PostApiResponse response = apiManager.unsubscribeFromChannel(user.getCredential(),
                    channelServerId);

            if (response == null || !response.isSuccessful()) {
                return;
            }
        } catch (ApiException e) {
            Log.e(TAG, "Error occurred in subscribing/unsubscribing from channel");
            Log.e(TAG, e.toString());
        }

        // remove the channel from the collection
        CollectionModel.removeChannelFromCollections(this, channelServerId);

        // clean up cached episodes
        Cursor cursor = null;

        try {

            Log.d(TAG, "Deleting unsubscribed channel");
            getContentResolver().delete(PremoContract.ChannelEntry.CONTENT_URI,
                    PremoContract.ChannelEntry.SERVER_ID + " = ?", new String[] { channelServerId });

            // get downloaded episodes belong to the channel with the server id
            cursor = getContentResolver().query(PremoContract.EpisodeEntry.CONTENT_URI,
                    new String[]{PremoContract.EpisodeEntry.LOCAL_MEDIA_URL},
                    PremoContract.EpisodeEntry.DOWNLOAD_STATUS_ID + " = ? AND " +
                            PremoContract.EpisodeEntry.CHANNEL_SERVER_ID + " = ?",
                    new String[]{String.valueOf(DownloadStatus.DOWNLOADED),
                            channelServerId}, null);
            ArrayList<String> filenames = new ArrayList<>();

            while (cursor.moveToNext()) {
                filenames.add(cursor.getString(cursor.getColumnIndex(
                        PremoContract.EpisodeEntry.LOCAL_MEDIA_URL)));
            }
            String[] filenamesArr = new String[filenames.size()];
            IOUtil.deleteFiles(filenames.toArray(filenamesArr));

            // mark episodes in the database as channel is unsubscribed
            EpisodeModel.markEpisodesAsChannelUnsubscribed(this, channelServerId);

            // broadcast the subscription change
            BroadcastHelper.broadcastSubscriptionChange(this, false, -1, channelServerId);
        } finally {
            ResourceHelper.closeResource(cursor);
        }
    }

    /**
     * Calls the API to load more servers for the channel published before the timestamp
     * @param channel
     * @param olderThanTimestamp
     */
    private void getChannelEpisodesOlderThan(Channel channel, long olderThanTimestamp) {
        List<Episode> episodes = ApiHelper.getRemoteEpisodesByChannelOlderThan(this, channel,
                olderThanTimestamp);
        int numberOfEpisodes;

        if (episodes != null) {
            numberOfEpisodes = episodes.size();

            // mark these episodes as unsubscribed
            for (int i = 0; i < episodes.size(); i++) {
                episodes.get(i).setChannelIsSubscribed(channel.isSubscribed());
            }

            // store the channels
            try {
                EpisodeModel.insertEpisodes(this, channel, episodes);
            } catch (Exception e) {
                numberOfEpisodes = 0;
                Log.w(TAG, "Error in getChannelEpisodesOlderThan", e);
            }
        } else {
            numberOfEpisodes = 0;
        }

        // broadcast the conclusion
        BroadcastHelper.broadcastEpisodesLoadedFromServer(this, channel.getServerId(),
                numberOfEpisodes);
    }

    /**
     * Calls the API to load more servers for the channel published before the timestamp
     * @param channel
     * @param newThanTimestamp
     */
    private void getChannelEpisodesNewerThan(Channel channel, long newThanTimestamp) {
        List<Episode> episodes = ApiHelper.getRemoteEpisodesByChannelNewerThan(this, channel,
                newThanTimestamp);
        int numberOfEpisodes;

        if (episodes != null) {
            numberOfEpisodes = episodes.size();

            // mark these episodes as unsubscribed
            for (int i = 0; i < episodes.size(); i++) {
                episodes.get(i).setChannelIsSubscribed(channel.isSubscribed());
            }

            // store the channels
            try {
                EpisodeModel.insertEpisodes(this, channel, episodes);
            } catch (Exception e) {
                numberOfEpisodes = 0;
                Log.w(TAG, "Error in getChannelEpisodesNewerThan", e);
            }
        } else {
            numberOfEpisodes = 0;
        }

        // broadcast the conclusion
        BroadcastHelper.broadcastEpisodesLoadedFromServer(this, channel.getServerId(),
                numberOfEpisodes);
    }

    /**
     * Adds a purchase to a users account
     * @param purchase
     */
    private void addPurchases(Purchase purchase) {
        String signature = purchase.getSignature();
        String orderId = purchase.getOrderId();
        String signedData = purchase.getOriginalJson();
        String productId = purchase.getSku();
        String developerPayload = purchase.getDeveloperPayload();

        // 1) add the purchase to the user's profile
        boolean purchaseAdded = ApiHelper.addPurchase(this, productId, orderId, developerPayload,
                signature, signedData);
        Log.d(TAG, "Purchase added: " + purchaseAdded);

        // 2) Resync the user profile
        syncUserProfile();

        // 3) If we aren't in touch anymore (locked out), re-register GCM
        registerGcmRegistrationId();

        // 4) Broadcast the change
        BroadcastHelper.broadcastPurchaseStored(ApiService.this, purchaseAdded);
    }
}