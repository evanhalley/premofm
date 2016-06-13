package com.mainmethod.premofm.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.service.SyncFeedService;
import com.mainmethod.premofm.ui.activity.BaseActivity;
import com.mainmethod.premofm.ui.activity.ChannelProfileActivity;

import org.parceler.Parcels;

/**
 * Created by WillowTree, Inc on 6/10/16.
 */

public class AddFeedDialog extends DialogFragment implements Dialog.OnClickListener {

    private EditText feedUrl;
    private ProgressBar progress;

    private BroadcastReceiver feedAddedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progress.setVisibility(View.GONE);
            Channel channel = Parcels.unwrap(intent.getParcelableExtra(BroadcastHelper.EXTRA_CHANNEL));
            ChannelProfileActivity.openChannelProfile((BaseActivity) getActivity(),
                    channel, null, false);
            AddFeedDialog.this.dismiss();
        }
    };

    public static void show(AppCompatActivity activity) {
        AddFeedDialog d = new AddFeedDialog();
        d.show(activity.getSupportFragmentManager(), "ADD_FEED");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setView(R.layout.dialog_add_feed)
                .setPositiveButton(R.string.dialog_add, (dialog, which) -> {
                    // do nothing
                })
                .setNegativeButton(R.string.dialog_cancel, this)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        feedUrl = (EditText) dialog.findViewById(R.id.url);
        progress = (ProgressBar) dialog.findViewById(R.id.progress);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
                AddFeedDialog.this.onClick(dialog, AlertDialog.BUTTON_POSITIVE));
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(feedAddedReceiver,
                new IntentFilter(BroadcastHelper.INTENT_CHANNEL_PROCESSED));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(feedAddedReceiver);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                String url = feedUrl.getText().toString();

                if (TextUtils.isEmpty(url)) {

                } else if (!URLUtil.isValidUrl(url)) {

                } else {
                    SyncFeedService.addFeed(getActivity(), url);
                    progress.setVisibility(View.VISIBLE);
                }
                break;
        }
    }
}
