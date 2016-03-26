/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.config;

import android.content.Context;

/**
 * Provides functions for easily accessing configuration settings in XML files
 * Created by evan on 7/27/14.
 */
public class ConfigurationManager {

    private static ConfigurationManager sInstance;

    private Context mContext;

    /**
     * Returns an instance of the ConfigurationManager
     * Should be used the very first time so the context is initialized
     * @param context Context to use
     * @return ConfigurationManager
     */
    public static ConfigurationManager getInstance(Context context){

        if(sInstance == null) {
            sInstance = new ConfigurationManager(context);
        }
        return sInstance;
    }

    /**
     * Returns an instance of the ConfigurationManager
     * @return ConfigurationManager
     */
    public static ConfigurationManager getInstance(){
        return sInstance;
    }

    private ConfigurationManager(Context context) {
        mContext = context;
    }

    /**
     * Returns a string value from n config XML file
     * @param stringResId resource ID if the string property
     * @return string value
     */
    public String getString(int stringResId) {
        return mContext.getString(stringResId);
    }

    /**
     * Returns an integer value from a config XML file
     * @param intResId resource ID of the integer property
     * @return integer value
     */
    public int getInt(int intResId) {
        return mContext.getResources().getInteger(intResId);
    }

}
