package com.mainmethod.premofm.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.IntentHelper;
import com.mainmethod.premofm.object.Channel;
import com.mainmethod.premofm.service.AsyncTaskService;
import com.mainmethod.premofm.service.PodcastSyncService;
import com.mainmethod.premofm.ui.activity.BaseActivity;
import com.mainmethod.premofm.ui.activity.ChannelProfileActivity;

import org.parceler.Parcels;

/**
 * Created by evanhalley on 6/10/16.
 */
public class AddPodcastDialog extends DialogFragment implements Dialog.OnClickListener {

    private EditText feedUrl;
    private ProgressBar progress;

    private BroadcastReceiver podcastAddedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progress.setVisibility(View.GONE);

            switch (intent.getAction()) {
                case BroadcastHelper.INTENT_PODCAST_PROCESSED:
                    if (intent.getBooleanExtra(BroadcastHelper.EXTRA_SUCCESS, false)) {
                        Channel channel = Parcels.unwrap(intent.getParcelableExtra(BroadcastHelper.EXTRA_CHANNEL));
                        ChannelProfileActivity.openChannelProfile((BaseActivity) getActivity(),
                                channel, null, false);
                        AddPodcastDialog.this.dismiss();
                    } else {

                    }
                    break;
                case BroadcastHelper.INTENT_OPML_PROCESS_FINISH:

                    if (intent.getBooleanExtra(BroadcastHelper.EXTRA_SUCCESS, false)) {
                        AddPodcastDialog.this.dismiss();
                    } else {

                    }
                    break;
            }
        }
    };

    public static void show(AppCompatActivity activity) {
        AddPodcastDialog d = new AddPodcastDialog();
        d.show(activity.getFragmentManager(), "ADD_PODCAST");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setView(R.layout.dialog_add_podcast)
                .setPositiveButton(R.string.dialog_add, (dialog, which) -> { /*nothing*/ })
                .setNeutralButton(R.string.dialog_opml_import, (dialog, which) -> { /*nothing*/ })
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
                AddPodcastDialog.this.onClick(dialog, AlertDialog.BUTTON_POSITIVE));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                AddPodcastDialog.this.onClick(dialog, AlertDialog.BUTTON_NEUTRAL));
    }

    @Override
    public void onResume() {
        super.onResume();
        BroadcastHelper.registerReceiver(getActivity(), podcastAddedReceiver,
                BroadcastHelper.INTENT_PODCAST_PROCESSED, BroadcastHelper.INTENT_OPML_PROCESS_FINISH);
    }

    @Override
    public void onPause() {
        super.onPause();
        BroadcastHelper.unregisterReceiver(getActivity(), podcastAddedReceiver);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                String url = feedUrl.getText().toString();

                if (TextUtils.isEmpty(url)) {

                } else if (!URLUtil.isValidUrl(url)) {

                } else {
                    PodcastSyncService.addPodcastFromUrl(getActivity(), url);
                    progress.setVisibility(View.VISIBLE);
                }
                break;
            case Dialog.BUTTON_NEUTRAL:
                IntentHelper.openOpmlFileChooser(this);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (resultCode != Activity.RESULT_OK || intent == null || intent.getData() == null) {
            return;
        }

        if (requestCode == IntentHelper.REQUEST_CODE_OPEN_OPML_FILE && intent.getData() != null) {
            progress.setVisibility(View.VISIBLE);
            AsyncTaskService.opmlImport(getActivity(), intent.getData());
        }
    }
}
