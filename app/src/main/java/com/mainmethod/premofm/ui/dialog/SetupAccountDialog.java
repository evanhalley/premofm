/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.ui.view.AuthenticationWidget;

/**
 * Created by evan on 9/27/15.
 */
public class SetupAccountDialog extends DialogFragment implements Dialog.OnClickListener {

    private AuthenticationWidget mAuthenticationWidget;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_setup_account, null, false);
        mAuthenticationWidget = new AuthenticationWidget(view);
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

        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                try {
                    mAuthenticationWidget.startAccountSetup();
                } catch (Exception e) {

                }
                break;

        }

    }
}
