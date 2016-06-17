package com.mainmethod.premofm.parse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * Created by evan on 11/5/14.
 */
public class DateParser {

    private SimpleDateFormat dateFormat;

    public DateParser() {

    }

    private void initFormatter(String sampleDate) {

        for (int i = 0; i < DateFormat.FORMATS.length; i++) {

            try {
                dateFormat = new SimpleDateFormat(DateFormat.FORMATS[i], Locale.getDefault());
                dateFormat.setTimeZone(TimeZone.getDefault());
                dateFormat.parse(sampleDate);
                break;
            } catch (Exception e) {
                Timber.d("Pattern doesn't match pattern: %s", DateFormat.FORMATS[i]);
                dateFormat = null;
            }
        }

        if (dateFormat == null) {
            Timber.w("Unable to parse dates that look like: %s", sampleDate);
        }
    }

    /**
     * Parses a string date into a Java Date object
     * @param dateStr
     * @return
     */
    public Date parseDate(String dateStr) {
        return parseDate(dateStr, true);
    }

    /**
     * Parses a string date into a Java Date object
     * @param dateStr
     * @return
     */
    private Date parseDate(String dateStr, boolean tryAgain) {
        Date date = new Date();

        if (dateFormat == null) {
            initFormatter(dateStr);
        }

        if (dateFormat != null) {

            try {
                date = dateFormat.parse(dateStr);
            } catch (ParseException e) {
                Timber.d("ParseException parsing date: %s", dateStr);

                if (tryAgain) {
                    Timber.d("ParseException encountered, re-initializing the date parser");
                    dateFormat = null;
                    parseDate(dateStr, false);
                }
            }
        }
        return date;
    }

    /**
     * Parses a duration string into milliseconds
     * @param durationStr
     * @return
     */
    public static long parseDuration(String durationStr) {
        long duration = -1;
        String[] componentArr = durationStr.split(":");

        switch (componentArr.length) {
            case 1:
                duration = parseDurationFromSeconds(durationStr);
                break;
            case 2:
            case 3:
                durationStr = ensureSegmentedDuration(durationStr);
                duration = parseDurationFromString(durationStr);
                break;
            default:
                break;
        }

        return duration;
    }

    public static String sanitizeDuration(String durationStr) {
        StringBuilder builder = new StringBuilder();
        String[] componentArr = durationStr.split(":");

        for (int i = 0; i < componentArr.length; i++) {
            builder.append(String.format("%02d", Integer.parseInt(componentArr[i])));

            if (i < componentArr.length - 1) {
                builder.append(":");
            }
        }
        return builder.toString();
    }

    public static String ensureSegmentedDuration(String timeStr) {

        if (timeStr == null) {
            throw new IllegalArgumentException("Input is null");
        }

        String[] componentArr = timeStr.split(":");
        int hour;
        int min;
        int sec;

        switch (componentArr.length) {
            case 2:
                hour = 0;
                min = Integer.parseInt(componentArr[0].trim());
                sec = Integer.parseInt(componentArr[1].trim());
                break;
            case 3:
                hour = Integer.parseInt(componentArr[0].trim());
                min = Integer.parseInt(componentArr[1].trim());
                sec = Integer.parseInt(componentArr[2].trim());
                break;
            default:
                throw new IllegalArgumentException("Input contains an incorrect number of segments");
        }

        if (sec >= 60) {
            min += sec / 60;
            sec = sec % 60;
        }

        if (min >= 60) {
            hour += min / 60;
            min = min % 60;
        }
        return String.format("%02d:%02d:%02d", hour, min, sec);
    }

    /**
     * Parses the length of the episode in milliseonds from a string resembling ssss
     * @param durationSeconds
     * @return
     */
    private static long parseDurationFromSeconds(String durationSeconds) {
        long duration = 0;

        try {
            duration = Long.parseLong(durationSeconds);
            duration = duration * 1_000;
        } catch (NumberFormatException e) {
            // TODO
        }
        return duration;
    }

    /**
     * Parses the length of the episode in milliseconds from a string resembling HH:mm:ss or mm:ss
     * @param durationStr
     * @return
     */
    private static long parseDurationFromString(String durationStr) {
        long duration = 0;
        durationStr = sanitizeDuration(durationStr);
        String[] segments = durationStr.split(":");

        try {
            duration += Integer.parseInt(segments[0]) * 3_600_000 +
                    Integer.parseInt(segments[1]) * 60_000 +
                    Integer.parseInt(segments[2]) * 1_000;
        } catch (Exception e) {
            Timber.e(e, "Unable to parse a duration");
        }
        return duration;
    }
}