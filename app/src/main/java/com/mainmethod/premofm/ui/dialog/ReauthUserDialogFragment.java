/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.service.ApiService;
import com.mainmethod.premofm.ui.activity.UserProfileActivity;

/**
 * Created by evan on 6/29/15.
 */
public class ReauthUserDialogFragment extends DialogFragment implements Dialog.OnClickListener {

    private static final int NOTIFICATION_ID = 1002;

    private EditText mPassword;

    public static void showReauthNotification(Context context) {
        Intent resultIntent = new Intent(context, UserProfileActivity.class);
        resultIntent.putExtra(UserProfileActivity.PARAM_SHOW_PASSWORD_DIALOG, true);
        PendingIntent contentIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setCategory(Notification.CATEGORY_STATUS)
                .setContentTitle(context.getString(R.string.notification_title_enter_password))
                .setContentText(context.getString(R.string.notification_content_enter_password))
                .setContentIntent(contentIntent)
                .setShowWhen(false)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
        NotificationManager manager = (NotificationManager) context.getApplicationContext().getSystemService(
                Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    public static void showDialog(Activity activity) {
        ReauthUserDialogFragment dialog = new ReauthUserDialogFragment();
        dialog.show(activity.getFragmentManager(), "reauth");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_reauthenticate,
                null, false);
        mPassword = (EditText) view.findViewById(R.id.password);
        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(false)
                .setView(view)
                .setPositiveButton(R.string.dialog_save, this)
                .setNegativeButton(R.string.dialog_cancel, this).create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        if (which == Dialog.BUTTON_POSITIVE) {
            String password = mPassword.getText().toString();

            if (TextHelper.isPasswordValid(password)) {
                Bundle bundle = new Bundle();
                bundle.putString(ApiService.PARAM_PASSWORD, password);
                ApiService.start(getActivity(), ApiService.ACTION_REAUTHENTICATE, bundle);
            } else {
                Toast.makeText(getActivity(), R.string.error_invalid_password,
                        Toast.LENGTH_SHORT).show();
                showDialog(getActivity());
            }
        }
    }
}
