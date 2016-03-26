package com.mainmethod.premofm.service;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.mainmethod.premofm.helper.UpdateHelper;
import com.mainmethod.premofm.service.job.SyncEpisodesJobService;

/**
 * Handles messages from google cloud messenger
 * Created by evanhalley on 12/29/15.
 */
public class PremoGcmListenerService extends GcmListenerService {

    private static final String TAG = "PremoGcmListenerService";
    public static final String TYPE_UNKNOWN = "UNKNOWN";
    public static final String TYPE_NEW_EPISODES = "NEW_EPISODES";
    public static final String TYPE_UPDATED_COLLECTION = "UPDATED_COLLECTION";
    public static final String PARAM_PUSH_TYPE = "pushType";

    @Override
    public void onMessageReceived(String from, Bundle data) {

        if (UpdateHelper.wasUpdated(this)) {
            // a migration is needed, so ignore pushes until the app is migrated
            return;
        }

        String pushType = data.getString(PARAM_PUSH_TYPE, TYPE_UNKNOWN);
        Log.d(TAG, "Push message is " + pushType);

        switch (pushType) {
            case TYPE_NEW_EPISODES:
                // let's run the ApiService
                SyncEpisodesJobService.schedule(this);
                break;
            case TYPE_UPDATED_COLLECTION:
                break;
            case TYPE_UNKNOWN:
            default:
                break;
        }
    }
}
