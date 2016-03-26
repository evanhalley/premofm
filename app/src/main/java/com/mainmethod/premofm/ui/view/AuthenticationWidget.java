/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.ui.view;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.mainmethod.premofm.R;
import com.mainmethod.premofm.helper.AnalyticsHelper;
import com.mainmethod.premofm.helper.PackageHelper;
import com.mainmethod.premofm.helper.TextHelper;
import com.mainmethod.premofm.service.ApiService;

/**
 * Created by evan on 9/27/15.
 */
public class AuthenticationWidget implements View.OnClickListener {

    private Context mContext;
    private EditText mEmail;
    private EditText mPassword;
    private TextInputLayout mEmailInputLayout;
    private TextInputLayout mPasswordInputLayout;
    private ImageButton mShowPassword;
    private boolean mIsShowingPassword;

    public AuthenticationWidget(View parentView) {
        mContext = parentView.getContext();
        mEmail = (EditText) parentView.findViewById(R.id.email);
        mEmailInputLayout = (TextInputLayout) parentView.findViewById(R.id.email_text_input_layout);
        mPassword = (EditText) parentView.findViewById(R.id.password);
        mPasswordInputLayout = (TextInputLayout) parentView.findViewById(R.id.password_text_input_layout);
        mShowPassword = (ImageButton) parentView.findViewById(R.id.show_password);
        mShowPassword.setOnClickListener(this);
        parentView.findViewById(R.id.help_email).setOnClickListener(this);
    }

    public void setEmailError(String error) {
        mEmailInputLayout.setErrorEnabled(true);
        mEmailInputLayout.setError(error);
    }

    public void setPasswordError(String error) {
        mPasswordInputLayout.setErrorEnabled(true);
        mPasswordInputLayout.setError(error);
    }

    public void clearErrors() {
        mEmailInputLayout.setErrorEnabled(false);
        mPasswordInputLayout.setErrorEnabled(false);
    }

    public String getEmail() {
        return mEmail.getText().toString();
    }

    public String getPassword() {
        return mPassword.getText().toString();
    }

    public void startAuthentication(boolean isSigningUp) {
        clearErrors();
        boolean cancel = false;
        String emailVal = getEmail();
        String passwordVal = getPassword();

        // Check for a valid email address.
        if (!TextHelper.isValidEmail(emailVal)) {
            setEmailError(mContext.getString(R.string.error_invalid_email));
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(passwordVal) && !TextHelper.isPasswordValid(passwordVal)) {
            setPasswordError(mContext.getString(R.string.error_invalid_password));
            cancel = true;
        }

        if (!cancel) {
            AnalyticsHelper.sendEvent(mContext,
                    isSigningUp ? AnalyticsHelper.CATEGORY_SIGN_UP : AnalyticsHelper.CATEGORY_SIGN_IN,
                    AnalyticsHelper.ACTION_CLICK, "");
            doAuthentication(emailVal, passwordVal, isSigningUp, false);
        }
    }

    private void doAuthentication(String email, String password, boolean isSigningUp, boolean isTempUser) {
        Bundle args = new Bundle();
        args.putString(ApiService.PARAM_EMAIL, email);
        args.putString(ApiService.PARAM_PASSWORD, password);
        args.putBoolean(ApiService.PARAM_IS_TEMP_ACCOUNT, isTempUser);
        String action = isSigningUp ? ApiService.ACTION_SIGN_UP : ApiService.ACTION_SIGN_IN;
        ApiService.start(mContext, action, args);
    }

    public void startAccountSetup() throws Exception {
        clearErrors();
        boolean cancel = false;
        String emailVal = getEmail();
        String passwordVal = getPassword();
        String oldPassword = PackageHelper.getDeviceId(mContext);

        // Check for a valid email address.
        if (!TextHelper.isValidEmail(emailVal)) {
            setEmailError(mContext.getString(R.string.error_invalid_email));
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(passwordVal) && !TextHelper.isPasswordValid(passwordVal)) {
            setPasswordError(mContext.getString(R.string.error_invalid_password));
            cancel = true;
        }

        if (!cancel) {
            AnalyticsHelper.sendEvent(mContext, AnalyticsHelper.CATEGORY_SETUP_ACCOUNT, AnalyticsHelper.ACTION_CLICK, "");
            Bundle args = new Bundle();
            args.putString(ApiService.PARAM_EMAIL, emailVal);
            args.putString(ApiService.PARAM_PASSWORD, passwordVal);
            args.putString(ApiService.PARAM_OLD_PASSWORD, oldPassword);
            ApiService.start(mContext, ApiService.ACTION_SETUP_ACCOUNT, args);
        }
    }

    /**
     * Creates a temporary user for trialing the app
     */
    public void startNewTempUser() {

        try {
            String email = PackageHelper.getDeviceId(mContext) + Math.random() + "@" +
                    mContext.getString(R.string.temp_user_email_domain);
            String password = PackageHelper.getDeviceId(mContext);
            doAuthentication(email, password, true, true);
        } catch (Exception e) {
            Toast.makeText(mContext, R.string.error_general, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.show_password:

                if (mIsShowingPassword) {
                    mIsShowingPassword = false;
                    mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mShowPassword.setImageResource(R.drawable.ic_show_password);
                } else {
                    mIsShowingPassword = true;
                    mPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    mShowPassword.setImageResource(R.drawable.ic_hide_password);
                }
                break;
            case R.id.help_email:
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.onboarding_email_explanation_title)
                        .setMessage(R.string.onboarding_email_explanation_message)
                        .setPositiveButton(R.string.dialog_got_it, null)
                        .show();
                break;
        }
    }
}
