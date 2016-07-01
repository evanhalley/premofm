package com.mainmethod.premofm.parse;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

import timber.log.Timber;

/**
 * Created by evan on 11/5/14.
 */
public class DateParser {


    private DateTimeFormatter mDateTimeFormatter;

    public DateParser() {

    }

    private void initFormatter(String sampleDate) {

        for (int i = 0; i < DateFormat.FORMATS.length; i++) {

            try {
                mDateTimeFormatter = DateTimeFormatter.ofPattern(DateFormat.FORMATS[i]);
                mDateTimeFormatter.parse(sampleDate);
                break;
            } catch (Exception e) {
                Timber.d("Pattern doesn't match pattern: " + DateFormat.FORMATS[i]);
                mDateTimeFormatter = null;
            }
        }

        if (mDateTimeFormatter == null) {
            Timber.w(String.format("Unable to parse dates that look like: %s", sampleDate));
        }
    }

    /**
     * Parses a string date into a Java Date object
     * @param dateStr
     * @return
     */
    public LocalDateTime parseDate(String dateStr) {
        return parseDate(dateStr, true);
    }

    /**
     * Parses a string date into a Java Date object
     * @param dateStr
     * @return
     */
    private LocalDateTime parseDate(String dateStr, boolean tryAgain) {
        LocalDateTime date = LocalDateTime.now(ZoneId.of("GMT"));

        if (mDateTimeFormatter == null) {
            initFormatter(dateStr);
        }

        if (mDateTimeFormatter != null) {

            try {
                date = LocalDateTime.parse(dateStr, mDateTimeFormatter);
            } catch (DateTimeParseException e) {
                Timber.d(String.format("ParseException parsing date: %s", dateStr));

                if (tryAgain) {
                    Timber.d("ParseException encountered, re-initializing the date parser");
                    mDateTimeFormatter = null;
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
            Timber.w("Error in parseDuration");
            Timber.w(e.getMessage());
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

    public static long getTimeInMillis(LocalDateTime localDateTime) {
        return localDateTime.toEpochSecond(ZoneOffset.UTC) * 1_000;
    }
}