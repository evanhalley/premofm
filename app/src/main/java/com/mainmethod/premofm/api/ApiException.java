/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.api;

/**
 * Created by evan on 7/26/14.
 */
public class ApiException extends Exception {

    public enum Error {
        OTHER,
        AUTHENTICATION,
        API_UNAVAILABLE,
        INTERNAL_ERROR
    }

    private Error mError;

    public ApiException(String message, Error error) {
        super(message);
        mError = error;
    }

    @Override
    public String toString() {
        return "ApiException{" +
                "mError=" + mError +
                ", Exception=" + super.toString() +
                '}';
    }
}