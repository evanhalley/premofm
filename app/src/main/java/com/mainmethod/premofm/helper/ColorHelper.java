/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.graphics.ColorUtils;

/**
 * Color convenience functions
 * Created by evan on 4/26/15.
 */
public class ColorHelper {

    public static int getColor(Context context, int colorResId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(colorResId);
        } else {
            //noinspection deprecation
            return context.getResources().getColor(colorResId);
        }
    }

    public static int getTextColor(int rgb) {
        // Counting the perceptive luminance - human eye favors green color
        double luminance = 0.299 * Color.red(rgb) + 0.587 * Color.green(rgb) +
                0.114 * Color.blue(rgb);
        int textColor;

        if (luminance > 186) {
            textColor = Color.BLACK; // bright colors - black font
        } else {
            textColor = Color.WHITE;
        }
        return textColor;
    }

    public static int darkenColor(int color) {
        return Color.argb(120, (color & 0xff0000) >> 16, (color & 0x00ff00) >> 8, (color & 0x0000ff));
    }

    /**
     * Returns a color suitable for the application status bar
     * @param color base color
     * @return status bar color
     */
    public static int getStatusBarColor(int color) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        hsl[2] = 0.85f * hsl[2];
        return ColorUtils.HSLToColor(hsl);
    }
}
