package com.mainmethod.premofm.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.api.ApiHelper;
import com.mainmethod.premofm.helper.AnalyticsHelper;
import com.mainmethod.premofm.helper.LinkHelper;

/**
 * Forwards incoming intents to the correct activity
 * Created by evanhalley on 12/23/15.
 */
public class LinkActivity extends BaseActivity {

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

        ApiHelper.getChannelByItunesIdAsync(this, id, channel -> {

            if (channel != null) {
                AnalyticsHelper.sendEvent(this,
                        AnalyticsHelper.CATEGORY_ITUNES_LINK,
                        AnalyticsHelper.ACTION_CLICK,
                        null);
                ChannelProfileActivity.openChannelProfile(LinkActivity.this, channel, null, true);
                finish();
            } else {
                showFailureToast();
            }
        });
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
