package com.mainmethod.premofm.service;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Handles ensuring the instance id is updated if necessary
 * Created by evanhalley on 12/29/15.
 */
public class PremoInstanceIdListenerService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        ApiService.start(this, ApiService.ACTION_REGISTER_GCM);
    }
}