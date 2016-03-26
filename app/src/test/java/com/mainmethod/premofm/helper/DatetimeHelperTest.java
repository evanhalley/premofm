/*
 * Copyright (c) 2014.
 * Main Method Incorporated.
 */

package com.mainmethod.premofm.helper;

import android.content.Context;

import com.mainmethod.premofm.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by evan on 6/26/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DatetimeHelperTest {

    @Mock
    Context mMockContext;

    @Test
    public void getTimestampTest() {
        long timestamp = DatetimeHelper.getTimestamp();
        assertEquals(true, timestamp > 0);
    }

    @Test
    public void dateToStringTest() {
        String dateStr = DatetimeHelper.dateToString(new Date());
        assertNotNull(dateStr);
    }

    @Test
    public void stringToDateTest() throws ParseException {
        Date date = DatetimeHelper.stringToDate("1970-01-01T00:00:00.0Z");
        assertEquals(date.getTime(), 0);
    }

    @Test
    public void convertSecondsToReadableDurationTest() {
        assertEquals(" < 1m", DatetimeHelper.convertSecondsToReadableDuration(-1_000));
        assertEquals(" < 1m", DatetimeHelper.convertSecondsToReadableDuration(1_000));
        assertEquals("5m", DatetimeHelper.convertSecondsToReadableDuration(300_000));
        assertEquals("10m", DatetimeHelper.convertSecondsToReadableDuration(600_000));
        assertEquals("1h 0m", DatetimeHelper.convertSecondsToReadableDuration(3_600_000));
        assertEquals("1h 0m", DatetimeHelper.convertSecondsToReadableDuration(3_620_000));
    }

    @Test
    public void convertSecondsToDurationTest() {
        assertEquals("00:00", DatetimeHelper.convertSecondsToDuration(-1_000));
        assertEquals("00:01", DatetimeHelper.convertSecondsToDuration(1_000));
        assertEquals("05:00", DatetimeHelper.convertSecondsToDuration(300_000));
        assertEquals("1:00:00", DatetimeHelper.convertSecondsToDuration(3_600_000));
        assertEquals("1:00:20", DatetimeHelper.convertSecondsToDuration(3_620_000));
    }
}

