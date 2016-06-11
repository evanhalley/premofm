package com.mainmethod.premofm.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import com.mainmethod.premofm.R;

/**
 * Created by WillowTree, Inc on 6/10/16.
 */

public class AddFeedDialog extends DialogFragment implements Dialog.OnClickListener {

    private EditText feedUrl;

    public static void show(AppCompatActivity activity) {
        AddFeedDialog d = new AddFeedDialog();
        d.show(activity.getSupportFragmentManager(), "SPEED");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setView(R.layout.dialog_add_rss)
                .setPositiveButton(R.string.dialog_add, this)
                .setNegativeButton(R.string.dialog_cancel, this)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        feedUrl = (EditText) dialog.findViewById(R.id.url);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                break;
        }

    }
}
