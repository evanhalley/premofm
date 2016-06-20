package com.mainmethod.premofm.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.data.model.ChannelModel;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.LinkHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.service.SyncFeedService;

import org.parceler.Parcels;

/**
 * Forwards incoming intents to the correct activity
 * Created by evanhalley on 12/23/15.
 */
public class LinkActivity extends BaseActivity {

    private BroadcastReceiver podcastAddedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Channel channel = Parcels.unwrap(intent.getParcelableExtra(BroadcastHelper.EXTRA_CHANNEL));
            ChannelProfileActivity.openChannelProfile(LinkActivity.this, channel, null, false);
            finish();
        }
    };

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        Intent intent = getIntent();

        if (intent == null) {
            showFailureToast();
            return;
        }

        Uri uri = intent.getData();

        if (uri == null) {
            showFailureToast();
            return;
        }

        String id = LinkHelper.getITunesId(uri);

        if (TextUtils.isEmpty(id)) {
            showFailureToast();
            return;
        }
        SyncFeedService.addFeedFromDirectory(this, ChannelModel.DIRECTORY_TYPE_ITUNES, id);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(podcastAddedReceiver);
    }


    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(podcastAddedReceiver,
                new IntentFilter(BroadcastHelper.INTENT_PODCAST_PROCESSED));
    }

    @Override
    protected int getLayoutResourceId() {
        return -1;
    }

    private void showFailureToast() {
        Toast.makeText(LinkActivity.this, R.string.error_cannot_load_channel,
                Toast.LENGTH_SHORT).show();
        finish();
    }
}