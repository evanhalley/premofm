/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;

import com.mainmethod.premofm.R;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by evan on 8/6/14.
 */
public class DatetimeHelper {

    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.S'Z'";
    private static final int MINUTES_IN_AN_HOUR = 60;
    private static final int SECONDS_IN_A_MINUTE = 60;

    public static long getTimestamp() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        return cal.getTimeInMillis();
    }

    /**
     * Converts a date object to a ISO Date string
     * @param date Date to convert
     * @return ISO Date string
     */
    public static String dateToString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(ISO_DATE_FORMAT);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter.format(date);
    }

    /**
     * Converts a ISO Date String to a Date Object
     * @param dateText ISO Date String
     * @return Date
     * @throws ParseException
     */
    public static Date stringToDate(String dateText) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(ISO_DATE_FORMAT);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter.parse(dateText);
    }

    /**
     * Converts a date to human readable format
     * @param date date to convert
     * @return human readable string
     */
    public static String dateToShortReadableString(Context context, Date date) {
        StringBuilder readable = new StringBuilder(32);
        Locale locale = Locale.getDefault();
        Calendar gmtCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        gmtCal.setTime(date);
        Calendar localCal = GregorianCalendar.getInstance(TimeZone.getDefault());
        localCal.setTimeInMillis(gmtCal.getTimeInMillis());

        Calendar now = Calendar.getInstance();

        // if date is today
        if (localCal.get(Calendar.DAY_OF_YEAR) >= now.get(Calendar.DAY_OF_YEAR) &&
                localCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            readable.append(context.getString(R.string.today));
        }

        // if the date is yesterday
        else if (localCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1) {
            readable.append(context.getString(R.string.yesterday));
        }

        // TODO, if within the last week, show days ago

        // if week of the month, show the day of the week
        else if (localCal.get(Calendar.WEEK_OF_MONTH) == now.get(Calendar.WEEK_OF_MONTH) &&
                localCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)) {
            readable.append(localCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale));
        }

        // if the same year, show the Month + Day
        else if (localCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            readable.append(localCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale))
                    .append(" ")
                    .append(localCal.get(Calendar.DAY_OF_MONTH));
        }

        // else show the date in MM/DD/YYYY
        else {
            readable.append(localCal.get(Calendar.MONTH) + 1)
                    .append("/")
                    .append(localCal.get(Calendar.DAY_OF_MONTH))
                    .append("/")
                    .append(localCal.get(Calendar.YEAR));
        }
        return readable.toString();
    }

    public static String convertSecondsToReadableDuration(long totalMilliseconds) {

        if (totalMilliseconds < 0) {
            return " < 1 min";
        }

        long totalSeconds = totalMilliseconds / 1000;
        long totalMinutes = totalSeconds / SECONDS_IN_A_MINUTE;
        long minutes = totalMinutes % MINUTES_IN_AN_HOUR;
        long hours = totalMinutes / MINUTES_IN_AN_HOUR;
        StringBuilder durationStr = new StringBuilder();

        if (hours > 0) {
            durationStr.append(hours).append(" hr ");
        }

        if (hours < 1 && minutes < 1) {
            durationStr.append(" < 1 min");
        } else {
            durationStr.append(minutes).append(" min");
        }
        return durationStr.toString();
    }

    public static String convertSecondsToDuration(long totalMilliseconds) {

        if (totalMilliseconds < 0) {
            return "00:00";
        }

        if (totalMilliseconds < 3_600_000) {
            return DurationFormatUtils.formatDuration(totalMilliseconds, "mm:ss", true);
        } else {
            return DurationFormatUtils.formatDuration(totalMilliseconds, "H:mm:ss", true);
        }
    }

    public static String formatRFC822Date(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yy HH:mm:ss Z", Locale.US);
        return format.format(date);
    }
}