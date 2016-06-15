package com.mainmethod.premofm.helper;

import com.mainmethod.premofm.parse.DateParser;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Created by evanhalley on 6/15/16.
 */
@RunWith(RobolectricTestRunner.class)
public class DateParserTest {

    @Test
    public void parseDuration() {
        Assert.assertEquals(3_600_000, DateParser.parseDuration("01:00:00"));
        Assert.assertEquals(99 * 3_600_000, DateParser.parseDuration("99:00:00"));
        Assert.assertEquals(0, DateParser.parseDuration("00:00:00"));
        Assert.assertEquals(60_000, DateParser.parseDuration("01:00"));
        Assert.assertEquals(99 * 60_000, DateParser.parseDuration("99:00"));
        Assert.assertEquals(1_000, DateParser.parseDuration("1"));
        Assert.assertEquals(0, DateParser.parseDuration("0"));
    }

}
