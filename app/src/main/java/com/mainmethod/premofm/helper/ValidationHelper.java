/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * Contains functions for validating input
 * Created by evan on 7/24/14.
 */
public class ValidationHelper {

    /**
     * Returns true if the email address is in the right format
     * @param emailAddress
     * @return
     */
    public static boolean isValidEmail(String emailAddress) {
        if(TextUtils.isEmpty(emailAddress)) {
            return false;
        } else {
            return Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches();
        }
    }

    /**
     * Returns true if the phone number is a valid format
     * @param phoneNumber
     * @return
     */
    public static boolean isValidPhone(String phoneNumber) {
        if(TextUtils.isEmpty(phoneNumber)) {
            return false;
        } else {
            return Patterns.PHONE.matcher(phoneNumber).matches();
        }
    }

    /**
     * Returns true if the url is a valid url
     * @param url
     * @return
     */
    public static boolean isValidUrl(String url) {
        if(TextUtils.isEmpty(url)) {
            return false;
        } else {
            return Patterns.WEB_URL.matcher(url).matches();
        }
    }

}
