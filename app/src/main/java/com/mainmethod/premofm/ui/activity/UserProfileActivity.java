/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.activity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.BroadcastHelper;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.object.User;
import com.mainmethod.premofm.service.ApiService;
import com.mainmethod.premofm.ui.dialog.ReauthUserDialogFragment;

/**
 * Created by evan on 6/16/15.
 */
public class UserProfileActivity extends BaseActivity implements View.OnClickListener {

    public static final String PARAM_SHOW_PASSWORD_DIALOG = "showPasswordDialog";

    private User mUser;
    private TextView mEmail;
    private TextView mNickname;
    private TextView mNumberOfSubscriptions;
    private TextView mNumberOfListens;
    private TextView mTimeListened;
    private Dialog mDialog;

    private final BroadcastReceiver mAccountChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUser();
            populateUserInterface();
            dismissDialog();
        }
    };

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_user_profile;
    }

    @Override
    protected void onCreateBase(Bundle savedInstanceState) {
        setHomeAsUpEnabled(true);

        // get the user object
        mUser = User.load(this);

        // handles to the user interface
        mEmail = (TextView) findViewById(R.id.email);
        mNickname = (TextView) findViewById(R.id.nickname);
        mNumberOfSubscriptions = (TextView) findViewById(R.id.number_of_subscriptions);
        mNumberOfListens = (TextView) findViewById(R.id.number_of_episode_listens);
        mTimeListened = (TextView) findViewById(R.id.time_of_podcast_listening);

        populateUserInterface();

        // add listeners to different components for changing username, passwords, and logging out
        findViewById(R.id.email_content).setOnClickListener(this);
        findViewById(R.id.nickname_content).setOnClickListener(this);
        findViewById(R.id.change_password).setOnClickListener(this);
        findViewById(R.id.logout).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mAccountChangedReceiver, new IntentFilter(BroadcastHelper.INTENT_ACCOUNT_CHANGE));

        Bundle args = getIntent().getExtras();

        if (args != null && args.containsKey(PARAM_SHOW_PASSWORD_DIALOG)) {
            ReauthUserDialogFragment.showDialog(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAccountChangedReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // so the back button works
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.email_content:
                showChangeEmail();
                break;
            case R.id.nickname_content:
                showChangeNickname();
                break;
            case R.id.logout:
                showLogoutConfirmationDialog();
                break;
            case R.id.change_password:
                showChangePasswordDialog();
                break;
        }
    }

    private void refreshUser() {
        mUser = User.load(this);
    }

    private void showLogoutConfirmationDialog() {
        mDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.button_logout, (dialog, which) -> {
                    boolean erased = ((ActivityManager) getSystemService(ACTIVITY_SERVICE))
                            .clearApplicationUserData();

                    if (erased) {
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
                    // DO NOTHING
                }).create();
        mDialog.show();
    }

    private void showChangePasswordDialog() {
        mDialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_change_password)
                .setPositiveButton(R.string.dialog_save, (dialog, which) -> {
                    initiatePasswordChange();
                })
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
                   // DO NOTHING
                })
                .create();
        mDialog.show();
    }

    private void showChangeNickname() {
        mDialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_change_nickname)
                .setPositiveButton(R.string.dialog_save, (dialog, which) -> {
                    initiateNicknameChange();
                })
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
                    // DO NOTHING
                })
                .create();
        mDialog.show();

        if (mUser.getNickname() != null && mUser.getNickname().length() > 0) {
            ((EditText) mDialog.findViewById(R.id.nickname)).setText(mUser.getNickname());
        }
    }

    private void showChangeEmail() {
        mDialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog_change_email)
                .setPositiveButton(R.string.dialog_save, (dialogInterface, which) -> {
                    initiateEmailChange();
                })
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
                    // DO NOTHING
                })
                .create();
        mDialog.show();
        ((EditText) mDialog.findViewById(R.id.email)).setText(mUser.getEmail());
    }

    private void initiateEmailChange() {
        showProgressDialog(R.string.dialog_please_wait, R.string.dialog_one_moment);
        String email = ((EditText) mDialog.findViewById(R.id.email)).getText().toString();

        if(TextHelper.isValidEmail(email)) {
            Bundle bundle = new Bundle();
            bundle.putString(ApiService.PARAM_EMAIL, email);
            ApiService.start(this, ApiService.ACTION_CHANGE_EMAIL, bundle);
        } else {
            Toast.makeText(this, R.string.error_invalid_email, Toast.LENGTH_SHORT).show();
        }
    }

    private void initiatePasswordChange() {
        showProgressDialog(R.string.dialog_please_wait, R.string.dialog_one_moment);
        String oldPassword = ((EditText) mDialog.findViewById(R.id.old_password)).getText().toString();
        String newPassword = ((EditText) mDialog.findViewById(R.id.new_password)).getText().toString();
        String confirmedPassword = ((EditText) mDialog.findViewById(R.id.new_password_confirm)).getText().toString();

        if (!newPassword.contentEquals(confirmedPassword)) {
            Toast.makeText(this, R.string.error_non_matching_password, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextHelper.isPasswordValid(newPassword)) {
            Bundle bundle = new Bundle();
            bundle.putString(ApiService.PARAM_OLD_PASSWORD, oldPassword);
            bundle.putString(ApiService.PARAM_NEW_PASSWORD, newPassword);
            ApiService.start(this, ApiService.ACTION_CHANGE_PASSWORD, bundle);
        } else {
            Toast.makeText(this, R.string.error_invalid_password, Toast.LENGTH_SHORT).show();
        }
    }

    private void initiateNicknameChange() {
        showProgressDialog(R.string.dialog_please_wait, R.string.dialog_one_moment);
        String nickname = ((EditText) mDialog.findViewById(R.id.nickname)).getText().toString();

        if (TextHelper.isValidNickname(nickname)) {
            Bundle bundle = new Bundle();
            bundle.putString(ApiService.PARAM_NICKNAME, nickname);
            ApiService.start(this, ApiService.ACTION_CHANGE_NICKNAME, bundle);
        } else {
            Toast.makeText(this, R.string.error_invalid_password, Toast.LENGTH_SHORT).show();
        }
    }

    private void populateUserInterface() {
        // populate the user interface with data from the user object
        mEmail.setText(mUser.getEmail());

        if (mUser.getNickname() == null || mUser.getNickname().length() == 0) {
            mNickname.setText(getString(R.string.user_profile_add_nickname));
        } else {
            mNickname.setText(mUser.getNickname());
        }
        // TODO podcast stats section
    }
}
