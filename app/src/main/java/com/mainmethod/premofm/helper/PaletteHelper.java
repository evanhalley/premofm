/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;

/**
 * Created by evan on 9/11/15.
 */
public class PaletteHelper {

    private static final String PREF_NAME = "channelColors";
    private static final String PRIMARY_COLOR_KEY_PREFIX = "primary_";
    private static final String TEXT_COLOR_KEY_PREFIX = "text_";
    private static PaletteHelper sInstance;

    private final SharedPreferences mSharedPreferences;

    public static PaletteHelper get(Context context) {

        if (sInstance == null) {
            sInstance = new PaletteHelper(context);
        }
        return sInstance;
    }

    private PaletteHelper(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREF_NAME, 0);
    }

    public int getPrimaryColor(String channelServerId) {
        int color = -1;

        if (mSharedPreferences.contains(getPrimaryColorKey(channelServerId))) {
            color = mSharedPreferences.getInt(getPrimaryColorKey(channelServerId), -1);
        }
        return color;
    }

    public int getTextColor(String channelServerId) {
        int color = -1;

        if (mSharedPreferences.contains(getTextColorKey(channelServerId))) {
            color = mSharedPreferences.getInt(getTextColorKey(channelServerId), -1);
        }
        return color;
    }

    public void getChannelColors(final String channelServerId, Bitmap bitmap,
                                 final OnPaletteLoaded onPaletteLoaded) {

        // do a lookup for the channel's primary color
        if (mSharedPreferences.contains(getPrimaryColorKey(channelServerId)) &&
                mSharedPreferences.contains(getTextColorKey(channelServerId))) {
            onPaletteLoaded.loaded(mSharedPreferences.getInt(getPrimaryColorKey(channelServerId), 0),
                    mSharedPreferences.getInt(getTextColorKey(channelServerId), 0));
        }

        // let's calculate the color using Palette and the bitmap
        else {
            Palette.from(bitmap).generate(palette -> {
                Palette.Swatch swatch = palette.getVibrantSwatch();

                if (swatch == null) {
                    swatch = palette.getDarkVibrantSwatch();
                }

                if (swatch == null && palette.getSwatches().size() > 0) {
                    swatch = palette.getSwatches().get(0);
                }

                if (swatch == null) {
                    onPaletteLoaded.loaded(-1, -1);
                    return;
                }

                // store the new colors
                storeNewColors(channelServerId, swatch.getRgb(), swatch.getTitleTextColor());
                onPaletteLoaded.loaded(swatch.getRgb(), swatch.getTitleTextColor());
            });
        }
    }

    private void storeNewColors(String channelServerId, int primaryColor, int textColor) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(getPrimaryColorKey(channelServerId), primaryColor);
        editor.putInt(getTextColorKey(channelServerId), textColor);
        editor.apply();
    }

    private static String getPrimaryColorKey(String channelServerId) {
        return PRIMARY_COLOR_KEY_PREFIX + channelServerId;
    }

    private static String getTextColorKey(String channelServerId) {
        return TEXT_COLOR_KEY_PREFIX + channelServerId;
    }

    public interface OnPaletteLoaded {
        void loaded(int primaryColor, int textColor);
    }
}
