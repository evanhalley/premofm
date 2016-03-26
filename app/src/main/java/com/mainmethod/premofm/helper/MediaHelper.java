package com.mainmethod.premofm.helper;

import java.text.NumberFormat;

/**
 * Created by evanhalley on 2/8/16.
 */
public class MediaHelper {

    private static NumberFormat sFormatter;
    public static final float DEFAULT_PLAYBACK_SPEED = 1.0f;
    public static final float MIN_PLAYBACK_SPEED = 0.5f;
    public static final float MAX_PLAYBACK_SPEED = 2.0f;
    public static final float UNIT = 0.1f;

    private static NumberFormat getFormatter() {

        if (sFormatter == null) {
            sFormatter = NumberFormat.getNumberInstance();
            sFormatter.setMinimumFractionDigits(0);
            sFormatter.setMaximumFractionDigits(1);
            sFormatter.setMaximumIntegerDigits(1);
            sFormatter.setMinimumIntegerDigits(1);
        }
        return sFormatter;
    }

    public static String formatSpeed(float speed) {
        return getFormatter().format(speed) + "x";
    }

    public static int getMax() {
        return (int) ((MAX_PLAYBACK_SPEED - MIN_PLAYBACK_SPEED) / UNIT);
    }

    public static float progressToSpeed(int progress) {
        return MIN_PLAYBACK_SPEED + progress * UNIT;
    }

    public static int speedToProgress(float speed) {
        return (int) ((speed - MIN_PLAYBACK_SPEED) / UNIT);
    }

}
