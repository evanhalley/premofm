package com.mainmethod.premofm.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.AppPrefHelper;
import com.mainmethod.premofm.helper.MediaHelper;
import com.mainmethod.premofm.service.PodcastPlayerService;

/**
 * Allows the user to change playback speed for the channel with the server ID
 * Created by evanhalley on 2/8/16.
 */
public class PlaybackSpeedDialog extends DialogFragment implements Dialog.OnClickListener,
        SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private static final String PARAM_CHANNEL_SERVER_ID = "channelServerId";

    private SeekBar mPlaybackSpeed;
    private TextView mPlaybackSpeedLabel;
    private float mSpeed;
    private String mChannelServerId;

    public static void show(AppCompatActivity activity, String channelServerId) {
        Bundle args = new Bundle();
        args.putString(PARAM_CHANNEL_SERVER_ID, channelServerId);
        PlaybackSpeedDialog d = new PlaybackSpeedDialog();
        d.setArguments(args);
        d.show(activity.getSupportFragmentManager(), "SPEED");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mChannelServerId = getArguments().getString(PARAM_CHANNEL_SERVER_ID);
        mSpeed = AppPrefHelper.getInstance(getActivity()).getPlaybackSpeed(mChannelServerId);
        return new AlertDialog.Builder(getActivity())
                .setView(R.layout.dialog_playback_speed)
                .setPositiveButton(R.string.dialog_save, this)
                .setNegativeButton(R.string.dialog_cancel, this)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        mPlaybackSpeed = (SeekBar) dialog.findViewById(R.id.playback_speed);
        mPlaybackSpeed.setMax(MediaHelper.getMax());
        mPlaybackSpeed.setProgress(MediaHelper.speedToProgress(mSpeed));
        mPlaybackSpeed.setOnSeekBarChangeListener(this);
        mPlaybackSpeedLabel = (TextView) dialog.findViewById(R.id.current_playback_speed);
        mPlaybackSpeedLabel.setText(getString(R.string.normal_playback_speed, MediaHelper.formatSpeed(mSpeed)));
        dialog.findViewById(R.id.max_playback_speed).setOnClickListener(this);
        dialog.findViewById(R.id.min_playback_speed).setOnClickListener(this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                AppPrefHelper.getInstance(getActivity()).setPlaybackSpeed(mChannelServerId, mSpeed);
                PodcastPlayerService.changePlaybackSpeed(getActivity(), mSpeed);
                break;
            default:
                // do nothing
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mSpeed = MediaHelper.progressToSpeed(progress);
        mPlaybackSpeedLabel.setText(getString(R.string.normal_playback_speed, MediaHelper.formatSpeed(mSpeed)));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.min_playback_speed:

                if (mSpeed > MediaHelper.MIN_PLAYBACK_SPEED) {
                    mPlaybackSpeed.setProgress(MediaHelper.speedToProgress(mSpeed - MediaHelper.UNIT));
                }
                break;
            case R.id.max_playback_speed:

                if (mSpeed < MediaHelper.MAX_PLAYBACK_SPEED) {
                    mPlaybackSpeed.setProgress(MediaHelper.speedToProgress(mSpeed + MediaHelper.UNIT));
                }
                break;
        }
    }
}